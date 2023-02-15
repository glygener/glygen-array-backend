package org.glygen.array.controller;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.exception.UserNotFoundException;
import org.glygen.array.persistence.SettingEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.SettingsRepository;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.LinkerClassification;
import org.glygen.array.persistence.rdf.Publication;
import org.glygen.array.persistence.rdf.data.StatisticalMethod;
import org.glygen.array.persistence.rdf.template.DescriptionTemplate;
import org.glygen.array.persistence.rdf.template.DescriptorGroupTemplate;
import org.glygen.array.persistence.rdf.template.MandateGroup;
import org.glygen.array.persistence.rdf.template.MetadataTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplateType;
import org.glygen.array.service.ArrayDatasetRepository;
import org.glygen.array.service.GlycanRepository;
import org.glygen.array.service.LayoutRepository;
import org.glygen.array.service.MetadataRepository;
import org.glygen.array.service.MetadataTemplateRepository;
import org.glygen.array.typeahead.NamespaceHandler;
import org.glygen.array.util.SequenceUtils;
import org.glygen.array.util.UniProtUtil;
import org.glygen.array.util.pubchem.PubChemAPI;
import org.glygen.array.util.pubmed.DTOPublication;
import org.glygen.array.util.pubmed.DTOPublicationAuthor;
import org.glygen.array.util.pubmed.PubmedUtil;
import org.glygen.array.view.EnumerationView;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.glygen.array.view.StatisticsView;
import org.glygen.array.view.User;
import org.glygen.array.view.Version;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/util")
public class UtilityController {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    @Autowired
    MetadataTemplateRepository templateRepository;
    
    @Autowired
    SettingsRepository settingsRepository;
    
    @Autowired
    UserRepository userRepository;
    
    @Autowired
    LayoutRepository layoutRepository;
    
    @Autowired
    ArrayDatasetRepository datasetRepository;
    
    @Autowired
    MetadataRepository metadataRepository;
    
    @Autowired
    GlycanRepository glycanRepository;
    
    static {
        BuilderWorkspace glycanWorkspace = new BuilderWorkspace(new GlycanRendererAWT());
        glycanWorkspace.initData();
    }
	

	@Operation(summary = "Convert given glycan sequence (in NCFG format) into GlycoCT")
	@RequestMapping(value="/parseSequence", method = RequestMethod.GET)
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Successfully converted into GlycoCT"), 
			@ApiResponse(responseCode="400", description="Invalid request, validation error"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public String parseCFGNameString (
			@Parameter(required=true, description="Sequence string (in NCFG format) to be parsed into GlycoCT. Please use a and b for alpha and beta respectively")
			@RequestParam String sequenceString) {
		
		CFGMasterListParser parser = new CFGMasterListParser();
		
		return parser.translateSequence(SequenceUtils.cleanupSequence(sequenceString.trim()));
	}
	
	@Operation(summary = "Retrieve publication details from Pubmed with the given pubmed id")
    @RequestMapping(value="/getPublicationFromPubmed/{pubmedid}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Publication retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Publication with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Publication getPublicationDetailsFromPubMed (
            @Parameter(required=true, description="pubmed id for the publication", example="111") 
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
    
    @Operation(summary = "Retrieve publication details from Pubmed with the given pubmed id")
    @RequestMapping(value="/pubmedToWiki/{pubmedid}", method = RequestMethod.GET, 
            produces={"text/plain"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Publication retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Publication with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String getPublicationTextFromPubMed (
            @Parameter(required=true, description="pubmed id for the publication", example="111") 
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
    
    @Operation(summary = "Retrieve protein sequence from UniProt with the given uniprot id")
    @RequestMapping(value="/getSequenceFromUniprot/{uniprotid}", method = RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Sequence retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Sequence with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String getSequenceFromUniProt (
            @Parameter(required=true, description="uniprotid such as P12345") 
            @PathVariable("uniprotid") String uniprotId) {
        if (uniprotId == null || uniprotId.trim().isEmpty()) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("uniprotId", "NoEmpty"));
            throw new IllegalArgumentException("uniprotId should be provided", errorMessage);
        }
        try {
            String sequence = UniProtUtil.getSequenceFromUniProt(uniprotId.trim());
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
    
    @Operation(summary = "Retrieve linker details from Pubchem with the given pubchem compound id or inchikey")
    @RequestMapping(value="/getlinkerFromPubChem/{pubchemid}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Linker retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Linker details with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Linker getLinkerDetailsFromPubChem (
            @Parameter(required=true, description="pubchemid or the inchikey or the smiles of the linker to retrieve") 
            @PathVariable("pubchemid") String pubchemid) {
        if (pubchemid == null || pubchemid.trim().isEmpty()) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("pubchemid", "NoEmpty"));
            throw new IllegalArgumentException("pubchem id should be provided", errorMessage);
        }
        try {
            Long pubChem = Long.parseLong(pubchemid.trim());
            Linker linker = PubChemAPI.getLinkerDetailsFromPubChem(pubChem);
            if (linker == null) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("pubchemid", "NotFound"));
                throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage); 
            }
            return linker; 
        } catch (NumberFormatException e) {
            try {
                // try using as inchiKey 
                Linker linker = PubChemAPI.getLinkerDetailsFromPubChemByInchiKey(pubchemid.trim());
                if (linker == null) {
                    ErrorMessage errorMessage = new ErrorMessage();
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    errorMessage.addError(new ObjectError("inChiKey", "NotFound"));
                    throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage); 
                }
                return linker;
            } catch (Exception e1) {
                try {
                    // try using as smiles
                    Linker linker = PubChemAPI.getLinkerDetailsFromPubChemBySmiles(pubchemid.trim());
                    if (linker == null) {
                        ErrorMessage errorMessage = new ErrorMessage();
                        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                        errorMessage.addError(new ObjectError("smiles", "NotFound"));
                        throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage); 
                    }
                    return linker;
                } catch (Exception e2) {
                    ErrorMessage errorMessage = new ErrorMessage();
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    errorMessage.addError(new ObjectError("smiles", "NotValid"));
                    throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage); 
                }
            } 
        } catch (Exception e) {  // pubchem retrieval failed
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("pubchemid", "NotValid"));
            throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage); 
        }
    }
    
    @Operation(summary = "retrieves list of possible linker classifications")
    @RequestMapping(value = "/getLinkerClassifications", method = RequestMethod.GET)
    @ApiResponses(value = { @ApiResponse(responseCode= "200", description = "list returned successfully" , content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))}),
            @ApiResponse(responseCode = "415", description = "Media type is not supported"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
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
    
    @Operation(summary="Retrieving Unit of Levels")
    @RequestMapping(value="/unitLevels", method=RequestMethod.GET, 
            produces={"application/json"})
    @ApiResponses(value= {@ApiResponse(responseCode="500", description="Internal Server Error")})
    public List<EnumerationView> getUnitLevels(){

        List<EnumerationView> unitLevels=new ArrayList<EnumerationView>();        
        try {   
            unitLevels.add(new EnumerationView(UnitOfLevels.FMOL.name(), UnitOfLevels.FMOL.getLabel()));
            unitLevels.add(new EnumerationView(UnitOfLevels.MMOL.name(), UnitOfLevels.MMOL.getLabel()));
            unitLevels.add(new EnumerationView(UnitOfLevels.MICROMOL.name(), UnitOfLevels.MICROMOL.getLabel()));
            unitLevels.add(new EnumerationView(UnitOfLevels.MICROML.name(), UnitOfLevels.MICROML.getLabel()));
            unitLevels.add(new EnumerationView(UnitOfLevels.MILLML.name(), UnitOfLevels.MILLML.getLabel()));
            
        }catch(Exception exception) {
            ErrorMessage errorMessage = new ErrorMessage("Error Loading Unit levels");
            errorMessage.setErrorCode(ErrorCodes.INTERNAL_ERROR);
            errorMessage.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            throw errorMessage;
        }
        return unitLevels;
    }
    
    @Operation(summary="Retrieving statistical methods")
    @RequestMapping(value="/statisticalmethods", method=RequestMethod.GET, 
            produces={"application/json"})
    @ApiResponses(value= {@ApiResponse(responseCode="500", description="Internal Server Error")})
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
    
    @Operation(summary="Retrieve supported raw data formats")
    @RequestMapping(value="/supportedrawfileformats", method=RequestMethod.GET, 
            produces={"application/json"})
    @ApiResponses(value= {@ApiResponse(responseCode="500", description="Internal Server Error")})
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
    
    @Operation(summary="Retrieve supported processed data formats")
    @RequestMapping(value="/supportedprocessedfileformats", method=RequestMethod.GET, 
            produces={"application/json"})
    public List<String> getProcessedDataFileFormats(){
        List<String> fileFormats = new ArrayList<String>();
        fileFormats.add("Glygen Array Data File");
        fileFormats.add("CFG_V5.2");
        fileFormats.add("CFG_V5.1");
        fileFormats.add("CFG_V5.0");
        fileFormats.add("CFG_V4.2");
        fileFormats.add("CFG_V4.1");
        fileFormats.add("CFG_V4.0");
        fileFormats.add("CFG_V3.2");
        fileFormats.add("CFG_V3.1");
        fileFormats.add("CFG_V3.0");
        
        return fileFormats;
    }
    
    @Operation(summary = "Retrieve descriptor with the given id")
    @RequestMapping(value="/getDescriptor/{descriptorId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Return the details of the given descriptor"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public DescriptionTemplate getDescriptorTemplate (
            @Parameter(required=true, description="Id if the descriptor or descriptor group") 
            @PathVariable("descriptorId")
            String id) {
        
        try {
            String uri = MetadataTemplateRepository.templatePrefix + id.trim();
            DescriptionTemplate description = templateRepository.getDescriptionFromURI(uri);
            return description;
        } catch (SparqlException e) {
            logger.error("Error retrieving descriptor with given id \" + id", e);
            throw new GlycanRepositoryException("Error retrieving descriptor with given id " + id, e);
        }
    }
    
    @Operation(summary = "Retrieve list of templates for the given type")
    @RequestMapping(value="/listTemplates", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Return a list of metadata templates"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public List<MetadataTemplate> getAllTemplatesByType (
            @Parameter(required=true, description="Type of the metadatatemplate") 
            @RequestParam("type")
            MetadataTemplateType type) {
        
        List<MetadataTemplate> templates;
        try {
            templates = templateRepository.getTemplateByType(type);
            // if it is an assay template, reset the order
           /* if (type == MetadataTemplateType.ASSAY) {
                // reset orders to 0 for optional ones
                for (MetadataTemplate metadata: templates) {
                    List<DescriptionTemplate> allMandatory = new ArrayList<DescriptionTemplate>();
                    for (DescriptionTemplate d: metadata.getDescriptors()) {
                        if (d.isMandatory())
                            allMandatory.add(d);
                        else 
                            d.setOrder(0);
                    }
                    Collections.sort(allMandatory);
                    int i=1;
                    for (DescriptionTemplate d: allMandatory) {
                        d.setOrder(i++);
                    }
                }
            } */
            
            for (MetadataTemplate metadata: templates) {    
                //if (type != MetadataTemplateType.ASSAY) 
                Collections.sort (metadata.getDescriptors());
                processMandateGroups(metadata.getDescriptors());
            }
            
        } catch (SparqlException | SQLException e) {
            logger.error("Error retrieving templates for type\" + type", e);
            throw new GlycanRepositoryException("Error retrieving templates for type" + type, e);
        }
        
        return templates;
    }
        
    void processMandateGroups (List<DescriptionTemplate> descriptors) {
        Map<Integer, MandateGroup> processedGroups = new HashMap<Integer, MandateGroup>();
        // set the first descriptor of a mandate group as the default one
        for (DescriptionTemplate d: descriptors) {
            if (d.getMandateGroup() != null) {
                if (!processedGroups.containsKey(d.getMandateGroup().getId())) {
                    d.getMandateGroup().setDefaultSelection(true);
                    processedGroups.put(d.getMandateGroup().getId(), d.getMandateGroup());
                } else {
                    d.getMandateGroup().setDefaultSelection(false);
                }
            }
            if (d instanceof DescriptorGroupTemplate) {
                Collections.sort(((DescriptorGroupTemplate) d).getDescriptors());
                processMandateGroups(((DescriptorGroupTemplate) d).getDescriptors());
            }
        } 
    }
    
    @Operation(summary = "Retrieve the template by id")
    @RequestMapping(value="/getTemplate/{id}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Return the metadata template, if exists"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public MetadataTemplate getTemplate (
            @Parameter(required=true, description="Id of the metadata template") 
            @PathVariable("id")
            String id) {
        
        try {
            String uri = MetadataTemplateRepository.templatePrefix + id.trim();
            MetadataTemplate metadataTemplate = templateRepository.getTemplateFromURI(uri);
            if (metadataTemplate != null) {
                Collections.sort(metadataTemplate.getDescriptors());
                processMandateGroups(metadataTemplate.getDescriptors());
            }
            if (metadataTemplate == null) {
                ErrorMessage errorMessage = new ErrorMessage("Template not found");
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("template", "NotFound"));
                throw new IllegalArgumentException("Invalid Input: Not a valid template information", errorMessage);
            }
            
            return metadataTemplate;
        } catch (SparqlException e) {
            logger.error("Error retrieving templates for type\" + type", e);
            throw new GlycanRepositoryException("Error retrieving the given templates with id" + id, e);
        }
    }
    
    
    @Operation(summary = "Retrieve type ahead suggestions")
    @RequestMapping(value="/getTypeAhead", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Return the matches, if any"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public List<String> getTypeAheadSuggestions (
            @Parameter(required=true, description="Name of the namespace to retrieve matches "
                    + "(dataset, printedslide, pmid, username, organization, group, lastname, firstname, or any other namespace that is registered in the repository ontology)")
            @RequestParam("namespace")
            String namespace, 
            @Parameter(required=true, description="value to match") 
            @RequestParam("value")
            String key, 
            @Parameter(required=false, description="limit of number of matches", example="10") 
            @RequestParam(name="limit", required=false)
            Integer limit) {
        
        try {
            namespace = namespace.trim();
            PatriciaTrie<String> trie = null;
            if (namespace.equalsIgnoreCase("dataset")) {
                List<String> datasetNames = datasetRepository.getAllPublicDatasetsNames();
                trie = NamespaceHandler.createNamespaceFromList(datasetNames);
                
            } else if (namespace.equalsIgnoreCase("printedslide")) {
                List<String> printedSlideNames = datasetRepository.getAllPublicPrintedSlideNames();
                trie = NamespaceHandler.createNamespaceFromList(printedSlideNames);
                
            } else if (namespace.equalsIgnoreCase("pmid")) {
                List<String> pmids = datasetRepository.getAllPublicPmids();
                trie = NamespaceHandler.createNamespaceFromList(pmids);
                
            } else if (namespace.equalsIgnoreCase("username")) {
                List<UserEntity> userList = userRepository.findAll();
                List<String> userNames = new ArrayList<String>();
                for (UserEntity user: userList) {
                    userNames.add(user.getUsername());
                }
                trie = NamespaceHandler.createNamespaceFromList(userNames);
            } else if (namespace.equalsIgnoreCase("organization")) {
                List<UserEntity> userList = userRepository.findAll();
                List<String> organizationNames = new ArrayList<String>();
                for (UserEntity user: userList) {
                    if (user.getAffiliation() != null && !user.getAffiliation().isEmpty())
                        organizationNames.add(user.getAffiliation());
                }
                trie = NamespaceHandler.createNamespaceFromList(organizationNames);
            } else if (namespace.equalsIgnoreCase("group")) {
                List<UserEntity> userList = userRepository.findAll();
                List<String> groupNames = new ArrayList<String>();
                for (UserEntity user: userList) {
                    if (user.getGroupName() != null && !user.getGroupName().isEmpty())
                        groupNames.add(user.getGroupName());
                }
                trie = NamespaceHandler.createNamespaceFromList(groupNames);
            } else if (namespace.equalsIgnoreCase("lastname")) {
                List<UserEntity> userList = userRepository.findAll();
                List<String> lastNames = new ArrayList<String>();
                for (UserEntity user: userList) {
                    if (user.getLastName() != null && !user.getLastName().isEmpty())
                        lastNames.add(user.getLastName());
                }
                trie = NamespaceHandler.createNamespaceFromList(lastNames);
            } else if (namespace.equalsIgnoreCase("firstname")) {
                List<UserEntity> userList = userRepository.findAll();
                List<String> firstNames = new ArrayList<String>();
                for (UserEntity user: userList) {
                    if (user.getFirstName() != null && !user.getFirstName().isEmpty())
                        firstNames.add(user.getFirstName());
                }
                trie = NamespaceHandler.createNamespaceFromList(firstNames);
            } else {
                // find the exact match if exists and put it as the first proposal
                trie = NamespaceHandler.getTrieForNamespace(namespace);
            }
            return UtilityController.getSuggestions(trie, key, limit);
        } catch (Exception e) {
            throw new GlycanRepositoryException("Type ahead failed", e);
        }
    }
    
    public static List<String> getSuggestions (PatriciaTrie<String> trie, String key, Integer limit) {
        Entry<String, String> entry = trie.select(key.toLowerCase());
        SortedMap<String, String> resultMap = trie.prefixMap(key.toLowerCase());
        List<String> result = new ArrayList<String>();
        int i=0;
       /* if (entry != null && !resultMap.containsValue(entry.getValue())) {
            result.add(entry.getValue());
            i++;
        }
        */   // do not put the best match
        for (Iterator<Entry<String, String>> iterator = resultMap.entrySet().iterator(); iterator.hasNext();) {
            Entry<String, String> match = iterator.next();
            if (limit != null && i >= limit)
                break;
            result.add(match.getValue());
            i++;
        }
        
        return result;
    }
    
    @Operation(summary = "Retrieve the setting for time delay before restarting asyncronous processes (in seconds)")
    @RequestMapping(value = "/delaysetting", method = RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Return the setting (in seconds)"), 
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Long getDelaySetting() {
        SettingEntity entity = settingsRepository.findByName("timeDelay");
        if (entity != null) {
            return Long.parseLong(entity.getValue());
        }
        return 3600L; // default setting is an hour
    } 
    
    
    @RequestMapping(value="/getuserdetails/{userName}", method=RequestMethod.GET, produces={"application/xml", "application/json"})
    @Operation(summary="Retrieve the information for the given user")
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="User retrieved successfully", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))}), 
            @ApiResponse(responseCode="404", description="User with given login name does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public @ResponseBody User getUser (
            @Parameter(required=true, description="login name of the user")
            @PathVariable("userName")
            String userName) {
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(userName.trim());    
        if (user == null) {
            throw new UserNotFoundException ("A user with loginId " + userName + " does not exist");
        }
        
        User userView = new User();
        userView.setAffiliation(user.getAffiliation());
        userView.setGroupName(user.getGroupName());
        userView.setDepartment(user.getDepartment());
        userView.setAffiliationWebsite(user.getAffiliationWebsite());
        userView.setFirstName(user.getFirstName());
        userView.setLastName(user.getLastName());
        userView.setUserName(user.getUsername());
        userView.setUserType(user.getLoginType().name());
        
        return userView;
    }
    
    @RequestMapping(value="/getstatistics", method=RequestMethod.GET, produces={"application/xml", "application/json"})
    @Operation(summary="Retrieve the stats of the repository")
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Stats retrieved successfully", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = StatisticsView.class))}), 
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public @ResponseBody StatisticsView getStatistics () {
        
        StatisticsView stats = new StatisticsView();
        stats.setUserCount(userRepository.count());
        try {
            Version api = new Version();
            api.setComponent("API");
            SettingEntity entity = settingsRepository.findByName("apiVersion");
            if (entity != null) {
                api.setVersion(entity.getValue()); 
            }
            entity = settingsRepository.findByName("apiReleaseDate");
            if (entity != null) {
                api.setReleaseDate(entity.getValue()); 
            } else {
                api.setReleaseDate(new Date().toString());
            }
            stats.addVersion(api);
            
            Version portal = new Version();
            portal.setComponent("Portal");
            stats.addVersion(portal);
            entity = settingsRepository.findByName("portalVersion");
            if (entity != null) {
                portal.setVersion(entity.getValue());
            }
            entity = settingsRepository.findByName("portalReleaseDate");
            if (entity != null) {
                portal.setReleaseDate(entity.getValue()); 
            } else {
                portal.setReleaseDate(new Date().toString());
            }
        } catch (Exception e) {
            logger.warn ("cannot retrieve versions from the database", e);
        }
        try {
            stats.setDatasetCount((long) datasetRepository.getArrayDatasetCountByUser(null, null));
            stats.setSlideCount((long) layoutRepository.getSlideLayoutCountByUser(null, null));
            stats.setSampleCount((long) metadataRepository.getSampleCountByUser(null, null));
            stats.setGlycanCount((long)glycanRepository.getGlycanCountByUser(null, null));
        } catch (SQLException | SparqlException e) {
            throw new GlycanRepositoryException("Cannot retrieve the counts from the repository",e);
        }
        return stats;
    }
}
