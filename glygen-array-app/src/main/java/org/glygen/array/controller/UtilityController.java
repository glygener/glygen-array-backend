package org.glygen.array.controller;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.eurocarbdb.application.glycanbuilder.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.GraphicOptions;
import org.eurocarbdb.application.glycoworkbench.GlycanWorkspace;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.LinkerClassification;
import org.glygen.array.persistence.rdf.Publication;
import org.glygen.array.persistence.rdf.data.StatisticalMethod;
import org.glygen.array.persistence.rdf.template.DescriptionTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplateType;
import org.glygen.array.service.MetadataTemplateRepository;
import org.glygen.array.typeahead.NamespaceHandler;
import org.glygen.array.util.ExtendedGalFileParser;
import org.glygen.array.util.UniProtUtil;
import org.glygen.array.util.pubchem.PubChemAPI;
import org.glygen.array.util.pubmed.DTOPublication;
import org.glygen.array.util.pubmed.DTOPublicationAuthor;
import org.glygen.array.util.pubmed.PubmedUtil;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.grits.toolbox.glycanarray.om.model.UnitOfLevels;
import org.grits.toolbox.glycanarray.om.parser.cfg.CFGMasterListParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping("/util")
public class UtilityController {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    @Autowired
    MetadataTemplateRepository templateRepository;
	
	// needs to be done to initialize static variables to parse glycan sequence
	private static GlycanWorkspace glycanWorkspace = new GlycanWorkspace(null, false, new GlycanRendererAWT());
	
	static {
			// Set orientation of glycan: RL - right to left, LR - left to right, TB - top to bottom, BT - bottom to top
			glycanWorkspace.getGraphicOptions().ORIENTATION = GraphicOptions.RL;
			// Set flag to show information such as linkage positions and anomers
			glycanWorkspace.getGraphicOptions().SHOW_INFO = true;
			// Set flag to show mass
			glycanWorkspace.getGraphicOptions().SHOW_MASSES = false;
			// Set flag to show reducing end
			glycanWorkspace.getGraphicOptions().SHOW_REDEND = true;

	}

	@ApiOperation(value = "Convert given glycan sequence (in NCFG format) into GlycoCT")
	@RequestMapping(value="/parseSequence", method = RequestMethod.GET)
	@ApiResponses (value ={@ApiResponse(code=200, message="Successfully converted into GlycoCT"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public String parseCFGNameString (
			@ApiParam(required=true, value="Sequence string (in NCFG format) to be parsed into GlycoCT. Please use a and b for alpha and beta respectively")
			@RequestParam String sequenceString) {
		
		CFGMasterListParser parser = new CFGMasterListParser();
		
		return parser.translateSequence(ExtendedGalFileParser.cleanupSequence(sequenceString));
	}
	
	@ApiOperation(value = "Retrieve publication details from Pubmed with the given pubmed id")
    @RequestMapping(value="/getPublicationFromPubmed/{pubmedid}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Publication retrieved successfully"), 
            @ApiResponse(code=404, message="Publication with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Publication getPublicationDetailsFromPubMed (
            @ApiParam(required=true, value="pubmed id for the publication") 
            @PathVariable("pubmedid") Integer pubmedid) {
        if (pubmedid == null) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("pubmedid", "NotValid"));
            throw new IllegalArgumentException("Invalid Input: Not a valid publication information", errorMessage);
        }
        PubmedUtil util = new PubmedUtil();
        try {
            DTOPublication pub = util.createFromPubmedId(pubmedid);
            return getPublicationFrom(pub);
        } catch (Exception e) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("pubmedid", "NotValid"));
            throw new IllegalArgumentException("Invalid Input: Not a valid publication information", errorMessage);
        }
    }
    
    public static Publication getPublicationFrom (DTOPublication pub) {
        Publication publication = new Publication ();
        publication.setAuthors(pub.getFormattedAuthor());
        publication.setDoiId(pub.getDoiId());
        publication.setEndPage(pub.getEndPage());
        publication.setJournal(pub.getJournal());
        publication.setNumber(pub.getNumber());
        publication.setPubmedId(pub.getPubmedId());
        publication.setStartPage(pub.getStartPage());
        publication.setTitle(pub.getTitle());
        publication.setVolume(pub.getVolume());
        publication.setYear(pub.getYear());
        
        return publication;
    }
    
    @ApiOperation(value = "Retrieve publication details from Pubmed with the given pubmed id")
    @RequestMapping(value="/pubmedToWiki/{pubmedid}", method = RequestMethod.GET, 
            produces={"text/plain"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Publication retrieved successfully"), 
            @ApiResponse(code=404, message="Publication with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String getPublicationTextFromPubMed (
            @ApiParam(required=true, value="pubmed id for the publication") 
            @PathVariable("pubmedid") Integer pubmedid) {
        if (pubmedid == null) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("pubmedid", "NotValid"));
            throw new IllegalArgumentException("Invalid Input: Not a valid publication information", errorMessage);
        }
        PubmedUtil util = new PubmedUtil();
        try {
            DTOPublication pub = util.createFromPubmedId(pubmedid);
            return getWikiTextFromPublication(pub);
        } catch (Exception e) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("pubmedid", "NotValid"));
            throw new IllegalArgumentException("Invalid Input: Not a valid publication information", errorMessage);
        }
    }
    
    
    /*
    <ref name="pmid32075877">{{Cite journal|doi=10.1126/science.abb2507|last1=Wrapp|first1=D.|
            last2=Wang|first2=N.|last3=Corbett|first3=K. S.|last4=Goldsmith|first4=J. A.|last5=Hsieh|first5=C. L.|last6=Abiona|
            first6=O.|last7=Graham|first7=B. S.|last8=McLellan|first8=J. S.|
            title=Cryo-EM structure of the 2019-nCoV spike in the prefusion conformation|journal=Science|pages=1260–1263|year=2020|pmid=32075877}}</ref>
            */
    
    public static String getWikiTextFromPublication (DTOPublication pub) {
        StringBuffer wikiText = new StringBuffer();
        wikiText.append("<ref name=\"pmid" + pub.getPubmedId() + "\">{{Cite journal|");
        /*if (pub.getType() != null) {
            wikiText.append(pub.getType() + "|");
        } else {
            // default
            wikiText.append("Journal Article|");
        }*/
        
        if (pub.getDoiId() != null) {
            wikiText.append("doi=" + pub.getDoiId() + "|");
        }
        
        int i=1;
        for (DTOPublicationAuthor author: pub.getAuthors()) {
            wikiText.append("last" + i + "=" + author.getLastName() + "|");
            wikiText.append("first" + i + "=" + author.getFirstName() + "|");
            i++;
        }
        wikiText.append("title=" + pub.getTitle() + "|");
        if (pub.getJournal() != null) {
            if (pub.getJournal().contains("(")) 
                wikiText.append("journal=" + pub.getJournal().substring(0, pub.getJournal().indexOf("(")).trim() + "|");
            else 
                wikiText.append("journal=" + pub.getJournal() + "|");
        }
        if (pub.getStartPage() != null && pub.getEndPage() != null) {
            wikiText.append("pages=" + pub.getStartPage() + "-" + pub.getEndPage() + "|");
        }
        if (pub.getYear() != null) {
            wikiText.append("year=" + pub.getYear() + "|");
        }
        wikiText.append("pmid=" + pub.getPubmedId() + "}}</ref>");
        
        return wikiText.toString();
    }
    
    @ApiOperation(value = "Retrieve protein sequence from UniProt with the given uniprot id")
    @RequestMapping(value="/getSequenceFromUniprot/{uniprotid}", method = RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(code=200, message="Sequence retrieved successfully"), 
            @ApiResponse(code=404, message="Sequence with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String getSequenceFromUniProt (
            @ApiParam(required=true, value="uniprotid such as P12345") 
            @PathVariable("uniprotid") String uniprotId) {
        if (uniprotId == null || uniprotId.isEmpty()) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("uniprotId", "NoEmpty"));
            throw new IllegalArgumentException("uniprotId should be provided", errorMessage);
        }
        try {
            String sequence = UniProtUtil.getSequenceFromUniProt(uniprotId);
            if (sequence == null) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("uniprotId", "NotValid"));
                throw new IllegalArgumentException("uniprotId does not exist", errorMessage);
            }
            return sequence;
        } catch (Exception e) {
            logger.error("Could not retrieve from uniprot", e);
            throw new GlycanRepositoryException("Failed to retieve from uniprot", e);
        }
    }
    
    @ApiOperation(value = "Retrieve linker details from Pubchem with the given pubchem compound id or inchikey")
    @RequestMapping(value="/getlinkerFromPubChem/{pubchemid}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Linker retrieved successfully"), 
            @ApiResponse(code=404, message="Linker details with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Linker getLinkerDetailsFromPubChem (
            @ApiParam(required=true, value="pubchemid or the inchikey or the smiles of the linker to retrieve") 
            @PathVariable("pubchemid") String pubchemid) {
        if (pubchemid == null || pubchemid.isEmpty()) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("pubchemid", "NoEmpty"));
            throw new IllegalArgumentException("pubchem id should be provided", errorMessage);
        }
        try {
            Long pubChem = Long.parseLong(pubchemid);
            Linker linker = PubChemAPI.getLinkerDetailsFromPubChem(pubChem);
            if (linker == null) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("pubchemid", "NotValid"));
                throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage); 
            }
            return linker; 
        } catch (NumberFormatException e) {
            try {
                // try using as inchiKey 
                Linker linker = PubChemAPI.getLinkerDetailsFromPubChemByInchiKey(pubchemid);
                return linker;
            } catch (Exception e1) {
                // try using as smiles
                Linker linker = PubChemAPI.getLinkerDetailsFromPubChemBySmiles(pubchemid);
                if (linker == null) {
                    ErrorMessage errorMessage = new ErrorMessage();
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    errorMessage.addError(new ObjectError("pubchemid", "NotValid"));
                    throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage); 
                }
                return linker;
            } 
        } catch (Exception e) {  // pubchem retrieval failed
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("pubchemid", "NotValid"));
            throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage); 
        }
    }
    
    @ApiOperation(value = "retrieves list of possible linker classifications", response = List.class)
    @RequestMapping(value = "/getLinkerClassifications", method = RequestMethod.GET)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "list returned successfully"),
            @ApiResponse(code = 415, message = "Media type is not supported"),
            @ApiResponse(code = 500, message = "Internal Server Error") })
    public List<LinkerClassification> getLinkerClassifications () throws SparqlException, SQLException {
        List<LinkerClassification> classificationList = new ArrayList<LinkerClassification>();
        
        try {
            Resource classificationNamespace = new ClassPathResource("linkerclassifications.csv");
            final InputStream inputStream = classificationNamespace.getInputStream();
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length == 2) {
                    Integer chebiID = Integer.parseInt(tokens[0]);
                    String classicationValue = tokens[1];
                    LinkerClassification classification = new LinkerClassification();
                    classification.setChebiId(chebiID);
                    classification.setClassification(classicationValue);
                    classification.setUri(PubChemAPI.CHEBI_URI + chebiID);
                    classificationList.add(classification);
                }
            }
        } catch (Exception e) {
            logger.error("Cannot load linker classification", e);
        }
        
        return classificationList;
        
    }
    
    @ApiOperation(value="Retrieving Unit of Levels")
    @RequestMapping(value="/unitLevels", method=RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses(value= {@ApiResponse(code=500, message="Internal Server Error")})
    public List<String> getUnitLevels(){

        List<String> unitLevels=new ArrayList<String>();        
        try {   
            unitLevels.add(UnitOfLevels.FMOL.getLabel());
            unitLevels.add(UnitOfLevels.MMOL.getLabel());
            unitLevels.add(UnitOfLevels.MICROMOL.getLabel());
            unitLevels.add(UnitOfLevels.MICROML.getLabel());
            unitLevels.add(UnitOfLevels.MILLML.getLabel());
            
        }catch(Exception exception) {
            ErrorMessage errorMessage = new ErrorMessage("Error Loading Unit levels");
            errorMessage.setErrorCode(ErrorCodes.INTERNAL_ERROR);
            errorMessage.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            throw errorMessage;
        }
        return unitLevels;
    }
    
    @ApiOperation(value="Retrieving statistical methods")
    @RequestMapping(value="/statisticalmethods", method=RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses(value= {@ApiResponse(code=500, message="Internal Server Error")})
    public List<StatisticalMethod> getStatisticalMethods(){
        try {
            return templateRepository.getAllStatisticalMethods();
        } catch (SparqlException | SQLException e) {
            ErrorMessage errorMessage = new ErrorMessage("Error retrieving statistical methods from the repository");
            errorMessage.addError(new ObjectError("method", e.getMessage()));
            errorMessage.setErrorCode(ErrorCodes.INTERNAL_ERROR);
            errorMessage.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            throw errorMessage;
        }
    }
    
    @ApiOperation(value="Retrieve supported raw data formats")
    @RequestMapping(value="/supportedrawfileformats", method=RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses(value= {@ApiResponse(code=500, message="Internal Server Error")})
    public List<String> getRawDataFileFormats(){
        List<String> fileFormats = new ArrayList<String>();
        fileFormats.add("GenePix Results 2");
        fileFormats.add("GenePix Results 3");
        fileFormats.add("GenePix Export 3");
        fileFormats.add("GenePix ArrayIt 5.6.1");
        fileFormats.add("GenePix ArrayIt 6.1.0");
        fileFormats.add("Proscan");
        return fileFormats;
    }
    
    @ApiOperation(value="Retrieve supported processed data formats")
    @RequestMapping(value="/supportedprocessedfileformats", method=RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses(value= {@ApiResponse(code=500, message="Internal Server Error")})
    public List<String> getProcessedDataFileFormats(){
        List<String> fileFormats = new ArrayList<String>();
        fileFormats.add("CFG 5.2");
        return fileFormats;
    }
    
    @ApiOperation(value = "Retrieve descriptor with the given id")
    @RequestMapping(value="/getDescriptor/{descriptorId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Return the details of the given descriptor"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public DescriptionTemplate getDescriptorTemplate (
            @ApiParam(required=true, value="Id if the descriptor or descriptor group") 
            @PathVariable("descriptorId")
            String id) {
        
        try {
            String uri = MetadataTemplateRepository.templatePrefix + id;
            DescriptionTemplate description = templateRepository.getDescriptionFromURI(uri);
            return description;
        } catch (SparqlException e) {
            logger.error("Error retrieving descriptor with given id \" + id", e);
            throw new GlycanRepositoryException("Error retrieving descriptor with given id " + id, e);
        }
    }
    
    @ApiOperation(value = "Retrieve list of templates for the given type")
    @RequestMapping(value="/listTemplates", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Return a list of metadata templates"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public List<MetadataTemplate> getAllTemplatesByType (
            @ApiParam(required=true, value="Type of the metadatatemplate") 
            @RequestParam("type")
            MetadataTemplateType type) {
        
        List<MetadataTemplate> templates;
        try {
            templates = templateRepository.getTemplateByType(type);
        } catch (SparqlException | SQLException e) {
            logger.error("Error retrieving templates for type\" + type", e);
            throw new GlycanRepositoryException("Error retrieving templates for type" + type, e);
        }
        
        return templates;
    }
    
    @ApiOperation(value = "Retrieve the template by id")
    @RequestMapping(value="/getTemplate/{id}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Return the metadata template, if exists"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public MetadataTemplate getTemplate (
            @ApiParam(required=true, value="Id of the metadata template") 
            @PathVariable("id")
            String id) {
        
        try {
            String uri = MetadataTemplateRepository.templatePrefix + id;
            MetadataTemplate metadataTemplate = templateRepository.getTemplateFromURI(uri);
            return metadataTemplate;
        } catch (SparqlException e) {
            logger.error("Error retrieving templates for type\" + type", e);
            throw new GlycanRepositoryException("Error retrieving the given templates with id" + id, e);
        }
    }
    
    
    @ApiOperation(value = "Retrieve type ahead suggestions")
    @RequestMapping(value="/getTypeAhead", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Return the matches, if any"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public List<String> getTypeAheadSuggestions (
            @ApiParam(required=true, value="Name of the namespace to retrieve matches") 
            @RequestParam("namespace")
            String namespace, 
            @ApiParam(required=true, value="value to match") 
            @RequestParam("value")
            String key, 
            @ApiParam(required=false, value="limit of number of matches") 
            @RequestParam(name="limit", required=false)
            Integer limit) {
        // find the exact match if exists and put it as the first proposal
        PatriciaTrie<String> trie = NamespaceHandler.getTrieForNamespace(namespace);
        Entry<String, String> entry = trie.select(key.toLowerCase());
        SortedMap<String, String> resultMap = trie.prefixMap(key.toLowerCase());
        List<String> result = new ArrayList<String>();
        int i=0;
        if (entry != null && !resultMap.containsValue(entry.getValue())) {
            result.add(entry.getValue());
            i++;
        }
        
        for (Iterator<Entry<String, String>> iterator = resultMap.entrySet().iterator(); iterator.hasNext();) {
            Entry<String, String> match = iterator.next();
            if (limit != null && i >= limit)
                break;
            result.add(match.getValue());
            i++;
        }
        
        return result;
    }
}
