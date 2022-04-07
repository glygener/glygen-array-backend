package org.glygen.array.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.imageio.ImageIO;
import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.eurocarbdb.MolecularFramework.io.SugarImporterException;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarImporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.sugar.Anomer;
import org.eurocarbdb.MolecularFramework.sugar.GlycoconjugateException;
import org.eurocarbdb.MolecularFramework.sugar.Modification;
import org.eurocarbdb.MolecularFramework.sugar.ModificationType;
import org.eurocarbdb.MolecularFramework.sugar.Monosaccharide;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glygen.array.config.SesameTransactionConfig;
import org.glygen.array.config.ValidationConstants;
import org.glygen.array.exception.GlycanExistsException;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.exception.UploadNotFinishedException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.CompoundFeature;
import org.glygen.array.persistence.rdf.ControlFeature;
import org.glygen.array.persistence.rdf.FeatureType;
import org.glygen.array.persistence.rdf.GPLinkedGlycoPeptide;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.GlycanInFeature;
import org.glygen.array.persistence.rdf.GlycanSequenceFormat;
import org.glygen.array.persistence.rdf.GlycanType;
import org.glygen.array.persistence.rdf.GlycoLipid;
import org.glygen.array.persistence.rdf.GlycoPeptide;
import org.glygen.array.persistence.rdf.GlycoProtein;
import org.glygen.array.persistence.rdf.LandingLight;
import org.glygen.array.persistence.rdf.LinkedGlycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.LinkerType;
import org.glygen.array.persistence.rdf.Lipid;
import org.glygen.array.persistence.rdf.MassOnlyGlycan;
import org.glygen.array.persistence.rdf.NegControlFeature;
import org.glygen.array.persistence.rdf.OtherLinker;
import org.glygen.array.persistence.rdf.PeptideLinker;
import org.glygen.array.persistence.rdf.ProteinLinker;
import org.glygen.array.persistence.rdf.ReducingEndConfiguration;
import org.glygen.array.persistence.rdf.ReducingEndType;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.SmallMoleculeLinker;
import org.glygen.array.persistence.rdf.Source;
import org.glygen.array.persistence.rdf.UnknownGlycan;
import org.glygen.array.persistence.rdf.data.ChangeLog;
import org.glygen.array.persistence.rdf.data.ChangeType;
import org.glygen.array.persistence.rdf.data.FileWrapper;
import org.glygen.array.persistence.rdf.data.FutureTaskStatus;
import org.glygen.array.persistence.rdf.metadata.DescriptorGroup;
import org.glygen.array.persistence.rdf.metadata.FeatureMetadata;
import org.glygen.array.persistence.rdf.metadata.SpotMetadata;
import org.glygen.array.persistence.rdf.template.DescriptionTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplateType;
import org.glygen.array.service.AddToRepositoryService;
import org.glygen.array.service.AsyncService;
import org.glygen.array.service.FeatureRepository;
import org.glygen.array.service.GlycanRepository;
import org.glygen.array.service.GlygenArrayRepository;
import org.glygen.array.service.GlygenArrayRepositoryImpl;
import org.glygen.array.service.LayoutRepository;
import org.glygen.array.service.LinkerRepository;
import org.glygen.array.service.MetadataTemplateRepository;
import org.glygen.array.util.ExtendedGalFileParser;
import org.glygen.array.util.GalFileImportResult;
import org.glygen.array.util.GlycanBaseTypeUtil;
import org.glygen.array.util.GlytoucanUtil;
import org.glygen.array.util.ParserConfiguration;
import org.glygen.array.util.SequenceUtils;
import org.glygen.array.util.SpotMetadataConfig;
import org.glygen.array.view.BatchFeatureUploadResult;
import org.glygen.array.view.BatchGlycanFileType;
import org.glygen.array.view.BatchGlycanUploadResult;
import org.glygen.array.view.BatchLinkerUploadResult;
import org.glygen.array.view.BlockLayoutResultView;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.glygen.array.view.FeatureListResultView;
import org.glygen.array.view.GlycanListResultView;
import org.glygen.array.view.LibraryImportInput;
import org.glygen.array.view.LinkerListResultView;
import org.glygen.array.view.ResumableFileInfo;
import org.glygen.array.view.ResumableInfoStorage;
import org.glygen.array.view.SlideLayoutError;
import org.glygen.array.view.SlideLayoutResultView;
import org.glygen.array.view.UploadResult;
import org.grits.toolbox.glycanarray.library.om.ArrayDesignLibrary;
import org.grits.toolbox.glycanarray.library.om.LibraryInterface;
import org.grits.toolbox.glycanarray.library.om.feature.Feature;
import org.grits.toolbox.glycanarray.library.om.feature.GlycanProbe;
import org.grits.toolbox.glycanarray.library.om.feature.Ratio;
import org.grits.toolbox.glycanarray.library.om.layout.Block;
import org.grits.toolbox.glycanarray.library.om.layout.LevelUnit;
import org.grits.toolbox.glycanarray.library.om.layout.Spot;
import org.grits.toolbox.util.structure.glycan.util.FilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Import(SesameTransactionConfig.class)
@RestController
@RequestMapping("/array")
public class GlygenArrayController {
	final static Logger logger = LoggerFactory.getLogger("event-logger");
	
	static BuilderWorkspace glycanWorkspace = new BuilderWorkspace(new GlycanRendererAWT());
	static {       
	        glycanWorkspace.initData();
			// Set orientation of glycan: RL - right to left, LR - left to right, TB - top to bottom, BT - bottom to top
			glycanWorkspace.getGraphicOptions().ORIENTATION = GraphicOptions.RL;
			// Set flag to show information such as linkage positions and anomers
			glycanWorkspace.getGraphicOptions().SHOW_INFO = true;
			// Set flag to show mass
			glycanWorkspace.getGraphicOptions().SHOW_MASSES = false;
			// Set flag to show reducing end
			glycanWorkspace.getGraphicOptions().SHOW_REDEND = true;

			glycanWorkspace.setDisplay(GraphicOptions.DISPLAY_NORMALINFO);
			glycanWorkspace.setNotation(GraphicOptions.NOTATION_SNFG);
	}
	
	@Autowired
	SesameSparqlDAO sparqlDAO;
	
	@Autowired
	@Qualifier("glygenArrayRepositoryImpl")
	GlygenArrayRepository repository;
	
	@Autowired
	GlycanRepository glycanRepository;
	
	@Autowired
	LinkerRepository linkerRepository;
	
	@Autowired
	LayoutRepository layoutRepository;
	
	@Autowired
	FeatureRepository featureRepository;
	
	@Autowired
	UserRepository userRepository;
	
	@Value("${spring.file.imagedirectory}")
	String imageLocation;

	@Value("${spring.file.uploaddirectory}")
	String uploadDir;
	
	@Autowired
	Validator validator;
	
	@Autowired
	ExtendedGalFileParser galFileParser;
	
	@Autowired
    MetadataTemplateRepository templateRepository;
	
	@Autowired
    SpotMetadataConfig metadataConfig;
	
	@Autowired
    AsyncService parserAsyncService;
	
	@Autowired
    AddToRepositoryService addService;
	
	List<Glycan> glycanCache = new ArrayList<Glycan>();
	
	List<Linker> linkerCache = new ArrayList<Linker>();
	
	@ApiOperation(value = "Add an alias to given glycan for the user", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value = "/addAlias/{glycanId}", method = RequestMethod.POST, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Alias added to glycan successfully"), 
			@ApiResponse(code=400, message="Invalid request, alias cannot be empty"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to update glycans"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation addAliasForGlycan(
			@ApiParam(required=true, value="Id of the glycan to add alias for") 
			@PathVariable("glycanId") String glycanId, 
			@ApiParam(required=true, value="alias for the glycan") 
			@RequestBody String alias, Principal principal) throws SQLException {
		try {
			if (alias == null || alias.isEmpty()) {
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
				errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
				errorMessage.addError(new ObjectError("alias", "NoEmpty"));
				throw new IllegalArgumentException("Invalid Input: Not a valid alias", errorMessage);
			}
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			glycanRepository.addAliasForGlycan(glycanId.trim(), alias.trim(), user);
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Error updating glycan with glycanId: " +glycanId);
		}
		return new Confirmation("Glycan updated successfully with new alias", HttpStatus.OK.value());
	}
	
	@ApiOperation(value = "Add given block layout for the user", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/addblocklayout", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Block layout added successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register block layouts"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public String addBlockLayout (
			@ApiParam(required=true, value="Block layout to be added, name, width, height, and spots are required, features should already exist in the repository")
			@RequestBody BlockLayout layout, 
			@ApiParam(required=false, value="true if there is no need to check again if feature exists")
			@RequestParam
			Boolean noFeatureCheck, Principal p) {
	    UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        return addService.addBlockLayout(layout, noFeatureCheck, user);
	}
	

	@ApiOperation(value = "Add given feature for the user", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/addfeature", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added feature"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register features"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public String addFeature (
			@ApiParam(required=true, value="Feature to be added, a linker and an at least one glycan are mandatory") 
			@RequestBody(required=true) org.glygen.array.persistence.rdf.Feature feature, Principal p) {
	    
	    UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
	    return addService.addFeature(feature, user);
	}

  /*  @ApiOperation(value = "Add given feature, provided only with sequence based linker for the user")
	@RequestMapping(value="/addfeatureFromSequence", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added feature"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register features"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public String addFeatureFromLinkerSequence (
			@ApiParam(required=false, value="Feature to be added, "
					+ "a linker is mandatory and should be one of PeptideLinker or a ProteinLinker with a valid sequence") 
			@RequestBody(required=false) org.glygen.array.persistence.rdf.Feature feature, Principal p) {
		if (feature.getLinker() == null || !(feature.getLinker() instanceof SequenceBasedLinker)) {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
			errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
			if (feature.getLinker() == null)
				errorMessage.addError(new ObjectError("linker", "NoEmpty"));
			else 
				errorMessage.addError(new ObjectError("linker", "InvalidSequence"));
			throw new IllegalArgumentException("Invalid Input: Not a valid feature information", errorMessage);
		}
		
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		try {
		    if (feature.getType() == FeatureType.GLYCOPEPTIDE) {
		        if (((GlycoPeptide) feature).getGlycans() == null || ((GlycoPeptide) feature).getGlycans().isEmpty()) {
		            if (feature.getLinker().getType() == LinkerType.PEPTIDE) {
		                Map<Integer, Glycan>  positionMap = ((SequenceBasedLinker)feature.getLinker()).extractGlycans();
	                    Map<String, String> positionMapWithId = new HashMap<>();
	                    for (Integer position: positionMap.keySet()) {
	                        Glycan g = positionMap.get(position);
	                        String seq = ((SequenceDefinedGlycan)g).getSequence();
	                        if (seq != null) {
	                            String existing = glycanRepository.getGlycanBySequence(seq.trim(), user);
	                            if (existing == null) {
	                                // add the glycan
	                                existing = addGlycan(g, p, true);
	                            }
	                            g.setUri(existing);
	                            LinkedGlycan linked = new LinkedGlycan();
	                            linked.addGlycan(g);
	                            ((GlycoPeptide) feature).addGlycan(linked);
	                            positionMapWithId.put(position + "", existing);
	                        } else {
	                            logger.error("Glycan in the feature with the following sequence cannot be located: " + seq);
	                        }
	                        
	                    }
	                    
	                    feature.setPositionMap(positionMapWithId);
		            }
		        }
		        
		    } else if (feature.getType() == FeatureType.GLYCOPROTEIN) {
		        if (((GlycoProtein) feature).getGlycans() == null || ((GlycoProtein) feature).getGlycans().isEmpty()) {
                    if (feature.getLinker().getType() == LinkerType.PROTEIN) {
                        Map<Integer, Glycan>  positionMap = ((SequenceBasedLinker)feature.getLinker()).extractGlycans();
                        Map<String, String> positionMapWithId = new HashMap<>();
                        for (Integer position: positionMap.keySet()) {
                            Glycan g = positionMap.get(position);
                            String seq = ((SequenceDefinedGlycan)g).getSequence();
                            if (seq != null) {
                                String existing = glycanRepository.getGlycanBySequence(seq.trim(), user);
                                if (existing == null) {
                                    // add the glycan
                                    existing = addGlycan(g, p, true);
                                }
                                g.setUri(existing);
                                LinkedGlycan linked = new LinkedGlycan();
                                linked.addGlycan(g);
                                ((GlycoProtein) feature).addGlycan(linked);
                                positionMapWithId.put(position + "", existing);
                            } else {
                                logger.error("Glycan in the feature with the following sequence cannot be located: " + seq);
                            }
                            
                        }
                        
                        feature.setPositionMap(positionMapWithId);
                    }
                }
		    }
			String featureURI = featureRepository.addFeature(feature, user);
			String id = featureURI.substring(featureURI.lastIndexOf("/")+1);
            return id;
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Feature cannot be added for user " + p.getName(), e);
		}		
	}*/
	
	@ApiOperation(value = "Add given glycan for the user", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/addglycan", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="id of the added glycan"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register glycans"),
			@ApiResponse(code=409, message="A glycan with the given sequence already exists!"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public String addGlycan (@RequestBody Glycan glycan, Principal p, 
	        @RequestParam("noGlytoucanRegistration") Boolean noGlytoucanRegistration) {
		if (glycan.getType() == null) {
			// assume sequenceDefinedGlycan
			glycan.setType(GlycanType.SEQUENCE_DEFINED);
		}
		
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		return addService.addGlycan(glycan, user, noGlytoucanRegistration);
	}
	
    @ApiOperation(value = "Register all glycans listed in a file", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value = "/addBatchGlycan", method=RequestMethod.POST, 
			consumes = {"application/json", "application/xml"}, produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Glycans processed successfully"), 
			@ApiResponse(code=400, message="Invalid request if file is not a valid file"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register glycans"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public BatchGlycanUploadResult addGlycanFromFile (
	        @ApiParam(required=true, name="file", value="details of the uploded file") 
	        @RequestBody
	        FileWrapper fileWrapper, Principal p, 
	        @RequestParam Boolean noGlytoucanRegistration,
	        @ApiParam(required=true, name="filetype", value="type of the file", allowableValues="Tab separated Glycan file, Library XML, GlycoWorkbench(.gws), WURCS, CFG IUPAC, Repository Export (.json)") 
	        @RequestParam(required=true, value="filetype") String fileType) {
	    BatchGlycanFileType type = BatchGlycanFileType.forValue(fileType);
	    
	    String fileFolder = uploadDir;
        if (fileWrapper.getFileFolder() != null && !fileWrapper.getFileFolder().isEmpty())
            fileFolder = fileWrapper.getFileFolder();
        File file = new File (fileFolder, fileWrapper.getIdentifier());
        if (!file.exists()) {
            ErrorMessage errorMessage = new ErrorMessage("file is not in the uploads folder");
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("file", "NotFound"));
            throw new IllegalArgumentException("File is not acceptable", errorMessage);
        }
        else {
            byte[] fileContent;
            try {
                fileContent = Files.readAllBytes(file.toPath());
                switch (type) {
                case GWS:
                    return addGlycanFromGWSFile(fileContent, noGlytoucanRegistration, p);
                case XML:
                    return addGlycanFromLibraryFile(fileContent, noGlytoucanRegistration, p);
                case TABSEPARATED:
                    return addGlycanFromCSVFile(fileContent, noGlytoucanRegistration, p);
                case CFG:
                    return addGlycanFromCFGFile(fileContent, noGlytoucanRegistration, p);
                case WURCS:
                    return addGlycanFromWURCSFile(fileContent, noGlytoucanRegistration, p);
                case REPOSITORYEXPORT:
                    return addGlycansFromExportFile (fileContent, noGlytoucanRegistration, p);
                }
                ErrorMessage errorMessage = new ErrorMessage("filetype is not accepted");
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("filetype", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("File is not acceptable", errorMessage);
            } catch (IOException e) {
                ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("file", "NotValid"));
                throw new IllegalArgumentException("File cannot be read", errorMessage);
            }
    	    
        }
	}

    private BatchGlycanUploadResult addGlycansFromExportFile(byte[] contents, Boolean noGlytoucanRegistration,
            Principal p) {
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        BatchGlycanUploadResult result = new BatchGlycanUploadResult();
        try {
            ByteArrayInputStream stream = new   ByteArrayInputStream(contents);
            String fileAsString = IOUtils.toString(stream, StandardCharsets.UTF_8);
            
            boolean isTextFile = Charset.forName("US-ASCII").newEncoder().canEncode(fileAsString);
           /* if (!isTextFile) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("file", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("File is not acceptable", errorMessage);
            }*/
            
            JSONArray inputArray = new JSONArray(fileAsString);
            int countSuccess = 0;
            for (int i=0; i < inputArray.length(); i++) {
                JSONObject jo = inputArray.getJSONObject(i);
                ObjectMapper objectMapper = new ObjectMapper();
                Glycan glycan = objectMapper.readValue(jo.toString(), Glycan.class);
                try {  
                    String id = addGlycan(glycan, p, noGlytoucanRegistration);
                    Glycan addedGlycan = glycanRepository.getGlycanById(id, user);
                    if (addedGlycan instanceof SequenceDefinedGlycan) {
                        byte[] image = getCartoonForGlycan(addedGlycan.getId());
                        addedGlycan.setCartoon(image);
                    }
                    result.getAddedGlycans().add(addedGlycan);
                    countSuccess ++;
                } catch (Exception e) {
                    logger.error ("Exception adding the glycan: " + glycan.getName(), e);
                    if (e.getCause() instanceof ErrorMessage) {
                        if (((ErrorMessage)e.getCause()).toString().contains("Duplicate")) {
                            ErrorMessage error = (ErrorMessage)e.getCause();
                            if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                                ObjectError err = error.getErrors().get(0);
                                if (err.getCodes() != null && err.getCodes().length != 0) {
                                    Glycan duplicateGlycan = new Glycan();
                                    try {
                                        duplicateGlycan = glycanRepository.getGlycanById(err.getCodes()[0], user);
                                        if (duplicateGlycan instanceof SequenceDefinedGlycan) {
                                            byte[] image = getCartoonForGlycan(duplicateGlycan.getId());
                                            duplicateGlycan.setCartoon(image);
                                        }
                                        result.addDuplicateSequence(duplicateGlycan);
                                    } catch (SparqlException | SQLException e1) {
                                        logger.error("Error retrieving duplicate glycan", e1);
                                    }
                                } else {
                                    result.addDuplicateSequence(glycan);
                                }
                            } 
                        } else {
                            result.addWrongSequence(glycan.getName(), i, null, ((ErrorMessage)e.getCause()).toString());
                        }
                    } else { 
                        result.addWrongSequence(glycan.getName(), i, null, e.getMessage());
                    }
                }
            }
         
            result.setSuccessMessage(countSuccess + " out of " + inputArray.length() + " glycans are added");
            return result;
        } catch (IOException | JSONException e) {
            throw new IllegalArgumentException("File is not valid. Reason: " + e.getMessage());
        }
    }
    
    @ApiOperation(value = "Register all linkers listed in a file", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/addBatchLinker", method=RequestMethod.POST, 
            consumes = {"application/json", "application/xml"}, produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Linkers processed successfully"), 
            @ApiResponse(code=400, message="Invalid request if file is not a valid file"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to register linkers"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public BatchLinkerUploadResult addLinkerFromFile (
            @ApiParam(required=true, name="file", value="details of the uploded file") 
            @RequestBody
            FileWrapper fileWrapper, Principal p, 
            @ApiParam(required=true, name="filetype", value="type of the file", allowableValues="Repository Export (.json)") 
            @RequestParam(required=true, value="filetype") String fileType,
            @ApiParam(required=true, value="type of the molecule", allowableValues="SMALLMOLECULE, LIPID, PEPTIDE, PROTEIN, OTHER") 
            @RequestParam("type") String moleculeType) {
        
        LinkerType linkerType = null;
        try {
            linkerType = LinkerType.valueOf(moleculeType);
            if (linkerType == null) {
                ErrorMessage errorMessage = new ErrorMessage("Incorrect molecule type");
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                String[] codes = new String[] {moleculeType};
                errorMessage.addError(new ObjectError("moleculeType", codes, null, "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Incorrect molecule type", errorMessage);
            }
        } catch (Exception e) {
            ErrorMessage errorMessage = new ErrorMessage("Incorrect molecule type");
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            String[] codes = new String[] {moleculeType};
            errorMessage.addError(new ObjectError("moleculeType", codes, null, "NotValid"));
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            throw new IllegalArgumentException("Incorrect molecule type", errorMessage);
        }
        String fileFolder = uploadDir;
        if (fileWrapper.getFileFolder() != null && !fileWrapper.getFileFolder().isEmpty())
            fileFolder = fileWrapper.getFileFolder();
        File file = new File (fileFolder, fileWrapper.getIdentifier());
        if (!file.exists()) {
            ErrorMessage errorMessage = new ErrorMessage("file is not in the uploads folder");
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("file", "NotFound"));
            throw new IllegalArgumentException("File is not acceptable", errorMessage);
        } else {
            byte[] fileContent;
            try {
                fileContent = Files.readAllBytes(file.toPath());
                if (fileType.toLowerCase().contains("export")) {
                    return addLinkersFromExportFile(fileContent, linkerType, p);
                } else {
                    ErrorMessage errorMessage = new ErrorMessage("filetype is not accepted");
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    errorMessage.addError(new ObjectError("filetype", "NotValid"));
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                    throw new IllegalArgumentException("File is not acceptable", errorMessage);
                }
            } catch (IOException e) {
                ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("file", "NotValid"));
                throw new IllegalArgumentException("File cannot be read", errorMessage);
            }
        }
    }
   
    private BatchLinkerUploadResult addLinkersFromExportFile(byte[] contents, LinkerType type, 
            Principal p) {
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        BatchLinkerUploadResult result = new BatchLinkerUploadResult();
        try {
            ByteArrayInputStream stream = new   ByteArrayInputStream(contents);
            String fileAsString = IOUtils.toString(stream, StandardCharsets.UTF_8);
            
            boolean isTextFile = Charset.forName("ISO-8859-1").newEncoder().canEncode(fileAsString);
           /* if (!isTextFile) {
                ErrorMessage errorMessage = new ErrorMessage("The file is not a text file");
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("file", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("File is not acceptable", errorMessage);
            }*/
            
            JSONArray inputArray = new JSONArray(fileAsString);
            int countSuccess = 0;
            for (int i=0; i < inputArray.length(); i++) {
                JSONObject jo = inputArray.getJSONObject(i);
                ObjectMapper objectMapper = new ObjectMapper();
                Linker linker = objectMapper.readValue(jo.toString(), Linker.class);
                if (linker.getType() != type && !linker.getType().name().equals("UNKNOWN_"+type.name())) {
                    // incorrect type
                    ErrorMessage errorMessage = new ErrorMessage("The selected type does not match the file contents");
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    String[] codes = new String[] {"selected type=" + type.name(), "type in file=" + linker.getType().name(), "linker=" + linker.getName()};
                    errorMessage.addError(new ObjectError("type", codes, null, "NotValid"));
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                    result.getErrors().add(errorMessage);
                } else {
                    try {  
                        String id = addLinker(linker, linker.getType().name().contains("UNKNOWN"), p);
                        linker.setId(id);
                        result.getAddedLinkers().add(linker);
                        countSuccess ++;
                    } catch (Exception e) {
                        if (e.getCause() instanceof ErrorMessage) {
                            if (((ErrorMessage)e.getCause()).toString().contains("Duplicate")) {
                                ErrorMessage error = (ErrorMessage)e.getCause();
                                if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                                    ObjectError err = error.getErrors().get(0);
                                    if (err.getCodes() != null && err.getCodes().length != 0) {
                                        try {
                                            Linker duplicate = linkerRepository.getLinkerById(err.getCodes()[0], user);
                                            result.getDuplicateLinkers().add(duplicate);
                                        } catch (SparqlException | SQLException e1) {
                                            logger.error("Error retrieving duplicate linker", e1);
                                        }
                                    } else {
                                        result.getDuplicateLinkers().add(linker);
                                    }
                                } 
                            } else {
                                logger.error ("Exception adding the linker: " + linker.getName(), e);
                                result.getErrors().add((ErrorMessage)e.getCause());
                            }
                        } else { 
                            logger.error ("Exception adding the linker: " + linker.getName(), e);
                            ErrorMessage error = new ErrorMessage(e.getMessage());
                            result.getErrors().add(error);
                        }
                    }
                }
            }
         
            result.setSuccessMessage(countSuccess + " out of " + inputArray.length() + " linkers are added");
            return result;
        } catch (IOException | JSONException e) {
            ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("file", "NotValid"));
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            throw new IllegalArgumentException("File is not acceptable", errorMessage);
        }
    }
    
    private BatchFeatureUploadResult addFeaturesFromExportFile(byte[] contents,
            Principal p) {
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        BatchFeatureUploadResult result = new BatchFeatureUploadResult();
        try {
            ByteArrayInputStream stream = new   ByteArrayInputStream(contents);
            String fileAsString = IOUtils.toString(stream, StandardCharsets.UTF_8);
            
            boolean isTextFile = Charset.forName("US-ASCII").newEncoder().canEncode(fileAsString);
           /* if (!isTextFile) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("file", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("File is not acceptable", errorMessage);
            }*/
            
            JSONArray inputArray = new JSONArray(fileAsString);
            int countSuccess = 0;
            for (int i=0; i < inputArray.length(); i++) {
                JSONObject jo = inputArray.getJSONObject(i);
                ObjectMapper objectMapper = new ObjectMapper();
                org.glygen.array.persistence.rdf.Feature feature = objectMapper.readValue(jo.toString(), org.glygen.array.persistence.rdf.Feature.class);
                try {  
                    String id = addFeature(feature, p);
                    feature.setId(id);
                    result.getAddedFeatures().add(feature);
                    countSuccess ++;
                } catch (Exception e) {
                    logger.error ("Exception adding the feature: " + feature.getName(), e);
                    if (e.getCause() instanceof ErrorMessage) {
                        if (((ErrorMessage)e.getCause()).toString().contains("Duplicate")) {
                            ErrorMessage error = (ErrorMessage)e.getCause();
                            if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                                ObjectError err = error.getErrors().get(0);
                                if (err.getCodes() != null && err.getCodes().length != 0) {
                                    try {
                                        org.glygen.array.persistence.rdf.Feature duplicate = featureRepository.getFeatureById(err.getCodes()[0], user);
                                        result.getDuplicateFeatures().add(duplicate);
                                    } catch (SparqlException | SQLException e1) {
                                        logger.error("Error retrieving duplicate feature", e1);
                                    }
                                } else {
                                    result.getDuplicateFeatures().add(feature);
                                }
                            } 
                        } else {
                            result.getErrors().add((ErrorMessage)e.getCause());
                        }
                    } else { 
                        ErrorMessage error = new ErrorMessage(e.getMessage());
                        result.getErrors().add(error);
                    }
                }
            }
         
            result.setSuccessMessage(countSuccess + " out of " + inputArray.length() + " features are added");
            return result;
        } catch (IOException | JSONException e) {
            throw new IllegalArgumentException("File is not valid. Reason: " + e.getMessage());
        }
    }
    
    @ApiOperation(value = "Register all features listed in a file", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/addBatchFeature", method=RequestMethod.POST, 
            consumes = {"application/json", "application/xml"}, produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Features processed successfully"), 
            @ApiResponse(code=400, message="Invalid request if file is not a valid file"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to register linkers"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public BatchFeatureUploadResult addFeatureFromFile (
            @ApiParam(required=true, name="file", value="details of the uploded file") 
            @RequestBody
            FileWrapper fileWrapper, Principal p, 
            @ApiParam(required=true, name="filetype", value="type of the file", allowableValues="Repository Export (.json)") 
            @RequestParam(required=true, value="filetype") String fileType) {
        
        String fileFolder = uploadDir;
        if (fileWrapper.getFileFolder() != null && !fileWrapper.getFileFolder().isEmpty())
            fileFolder = fileWrapper.getFileFolder();
        File file = new File (fileFolder, fileWrapper.getIdentifier());
        if (!file.exists()) {
            ErrorMessage errorMessage = new ErrorMessage("file is not in the uploads folder");
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("file", "NotFound"));
            throw new IllegalArgumentException("File is not acceptable", errorMessage);
        }
        else {
            byte[] fileContent;
            try {
                fileContent = Files.readAllBytes(file.toPath());
                if (fileType.toLowerCase().contains("export")) {
                    return addFeaturesFromExportFile(fileContent, p);
                } else {
                    ErrorMessage errorMessage = new ErrorMessage("filetype is not accepted");
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    errorMessage.addError(new ObjectError("filetype", "NotValid"));
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                    throw new IllegalArgumentException("File is not acceptable", errorMessage);
                }
            } catch (IOException e) {
                ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("file", "NotValid"));
                throw new IllegalArgumentException("File cannot be read", errorMessage);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private BatchGlycanUploadResult addGlycanFromLibraryFile (byte[] contents, Boolean noGlytoucanRegistration, Principal p) {
	    UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
	    BatchGlycanUploadResult result = new BatchGlycanUploadResult();
	    try {
	        ByteArrayInputStream stream = new ByteArrayInputStream(contents);
            InputStreamReader reader2 = new InputStreamReader(stream, "UTF-8");
            List<Class> contextList = new ArrayList<Class>(Arrays.asList(FilterUtils.filterClassContext));
            contextList.add(ArrayDesignLibrary.class);
            JAXBContext context2 = JAXBContext.newInstance(contextList.toArray(new Class[contextList.size()]));
            Unmarshaller unmarshaller2 = context2.createUnmarshaller();
            ArrayDesignLibrary library = (ArrayDesignLibrary) unmarshaller2.unmarshal(reader2);
            List<org.grits.toolbox.glycanarray.library.om.feature.Glycan> glycanList = library.getFeatureLibrary().getGlycan();
            int count = 0;
            int countSuccess = 0;
            for (org.grits.toolbox.glycanarray.library.om.feature.Glycan glycan : glycanList) {
                count++;
                Glycan view = null;
                if (glycan.getSequence() == null) {
                    if (glycan.getOrigSequence() == null && (glycan.getClassification() == null || glycan.getClassification().isEmpty())) {
                        // this is not a glycan, it is either control or a flag
                        // do not create a glycan
                        // give an error message
                        result.addWrongSequence ((glycan.getName() == null ? glycan.getId()+"" : glycan.getName()), count, null, "Not a glycan");
                  
                        continue;
                    } else if (glycan.getOrigSequence() == null || (glycan.getOrigSequence() != null && glycan.getOriginalSequenceType().equalsIgnoreCase("other"))) {  
                        // unknown glycan
                        view = new UnknownGlycan();
                    } 
                    if (glycan.getFilterSetting() != null) {
                        // there is a glycan with composition, TODO we don't handle that right now
                        result.addWrongSequence((glycan.getName() == null ? glycan.getId()+"" : glycan.getName()), count, glycan.getSequence(), "Glycan Type not supported");
                 
                        continue;
                    }
                } else {
                    view = new SequenceDefinedGlycan();
                    ((SequenceDefinedGlycan) view).setGlytoucanId(glycan.getGlyTouCanId());
                    ((SequenceDefinedGlycan) view).setSequence(glycan.getSequence().trim());
                    ((SequenceDefinedGlycan) view).setSequenceType(GlycanSequenceFormat.GLYCOCT);
                }
                if (view == null) {
                    result.addWrongSequence((glycan.getName() == null ? glycan.getId()+"" : glycan.getName()), count , glycan.getSequence(), "Not a glycan");
                    continue;
                }
                try {
                    view.setInternalId(glycan.getId()+ "");
                    view.setName(glycan.getName());
                    view.setDescription(glycan.getComment());   
                    String id = addGlycan(view, p, noGlytoucanRegistration);
                    Glycan addedGlycan = glycanRepository.getGlycanById(id, user);
                    if (addedGlycan instanceof SequenceDefinedGlycan) {
                        byte[] image = getCartoonForGlycan(addedGlycan.getId());
                        addedGlycan.setCartoon(image);
                    }
                    result.getAddedGlycans().add(addedGlycan);
                    countSuccess ++;
                } catch (Exception e) {
                    if (e.getCause() instanceof ErrorMessage) {
                        if (((ErrorMessage)e.getCause()).toString().contains("Duplicate")) {
                            ErrorMessage error = (ErrorMessage)e.getCause();
                            if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                                ObjectError err = error.getErrors().get(0);
                                if (err.getCodes() != null && err.getCodes().length != 0) {
                                    Glycan duplicateGlycan = new Glycan();
                                    try {
                                        duplicateGlycan = glycanRepository.getGlycanById(err.getCodes()[0], user);
                                        if (duplicateGlycan instanceof SequenceDefinedGlycan) {
                                            byte[] image = getCartoonForGlycan(duplicateGlycan.getId());
                                            duplicateGlycan.setCartoon(image);
                                        }
                                        result.addDuplicateSequence(duplicateGlycan);
                                    } catch (SparqlException | SQLException e1) {
                                        logger.error("Error retrieving duplicate glycan", e1);
                                    }
                                }
                            } 
                        } else {
                            result.addWrongSequence((glycan.getName() == null ? glycan.getId()+"" : glycan.getName()), count , glycan.getSequence(), ((ErrorMessage)e.getCause()).toString());
                        }
                    } else { 
                        logger.error ("Exception adding the glycan: " + glycan.getName(), e);
                        result.addWrongSequence((glycan.getName() == null ? glycan.getId()+"" : glycan.getName()), count , glycan.getSequence(), ((ErrorMessage)e.getCause()).toString());
                    }
                } 
            }
            stream.close();
            result.setSuccessMessage(countSuccess + " out of " + count + " glycans are added");
            logger.info("Processed the file. " + countSuccess + " out of " + count + " glycans are added" );
            return result;
	    } catch (Exception e) {
	        ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("file", "NotValid"));
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
	        throw new IllegalArgumentException("File is not valid.", errorMessage);
	    } 
	}
	
	private BatchGlycanUploadResult addGlycanFromCSVFile (byte[] contents, Boolean noGlytoucanRegistration, Principal p) {
	    ParserConfiguration config = new ParserConfiguration();
	    config.setNameColumn(0);
	    config.setIdColumn(1);
	    config.setGlytoucanIdColumn(2);
	    config.setSequenceColumn(3);
	    config.setSequenceTypeColumn(4);
	    config.setMassColumn(5);
	    config.setCommentColumn(6);
	    
	    UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        BatchGlycanUploadResult result = new BatchGlycanUploadResult();
        try {
            ByteArrayInputStream stream = new   ByteArrayInputStream(contents);
            String fileAsString = IOUtils.toString(stream, StandardCharsets.UTF_8);
            Scanner scan = new Scanner(fileAsString);
            int count = 0;
            int countSuccess = 0;
            while(scan.hasNext()){
                String curLine = scan.nextLine();
                String[] splitted = curLine.split("\t");
                if (splitted.length == 0)
                    continue;
                String firstColumn = splitted[0].trim();
                if (firstColumn.equalsIgnoreCase("name")) {
                    continue; // skip the header line
                }
                count++;
                String glycanName = splitted[config.getNameColumn()];
                String glytoucanId = splitted[config.getGlytoucanIdColumn()];
                String sequence = splitted[config.getSequenceColumn()];
                String sequenceType = splitted[config.getSequenceTypeColumn()];
                String mass = splitted[config.getMassColumn()];
                String internalId = splitted[config.getIdColumn()];
                String comments = splitted[config.getCommentColumn()];
                
                Glycan glycan = null;
                if ((glytoucanId == null || glytoucanId.isEmpty()) && (sequence == null || sequence.isEmpty())) {
                    // mass only glycan
                    if (mass != null && !mass.trim().isEmpty()) {
                        glycan = new MassOnlyGlycan(); 
                        try {
                            ((MassOnlyGlycan) glycan).setMass(Double.parseDouble(mass.trim()));
                        } catch (NumberFormatException e) {
                            // add to error list
                            result.addWrongSequence(glycanName, count, null, e.getMessage());
                        }
                    } else {
                        // ERROR
                        result.addWrongSequence(glycanName, count, sequence, "No sequence and mass information found");
                    }
                } else {
                    // sequence defined glycan
                    glycan = new SequenceDefinedGlycan();
                    if (glytoucanId != null && !glytoucanId.trim().isEmpty()) {
                        // use glytoucanid to retrieve the sequence
                        String glycoCT = addService.getSequenceFromGlytoucan(glytoucanId.trim());
                        if (glycoCT != null && glycoCT.startsWith("RES")) {
                            ((SequenceDefinedGlycan) glycan).setSequence(glycoCT.trim());
                            ((SequenceDefinedGlycan) glycan).setSequenceType(GlycanSequenceFormat.GLYCOCT);
                        } else if (glycoCT != null && glycoCT.startsWith("WURCS")){
                            ((SequenceDefinedGlycan) glycan).setSequence(glycoCT.trim());
                            ((SequenceDefinedGlycan) glycan).setSequenceType(GlycanSequenceFormat.WURCS);
                        }
                    } else if (sequence != null && !sequence.trim().isEmpty()) {
                        ((SequenceDefinedGlycan) glycan).setSequence(sequence.trim());
                        ((SequenceDefinedGlycan) glycan).setSequenceType(GlycanSequenceFormat.forValue(sequenceType));
                    }
                }
                if (internalId != null) glycan.setInternalId(internalId.trim());
                if (glycanName != null) glycan.setName(glycanName.trim());
                if (comments != null) glycan.setDescription(comments.trim());
                try {  
                    String id = addGlycan(glycan, p, noGlytoucanRegistration);
                    Glycan addedGlycan = glycanRepository.getGlycanById(id, user);
                    if (addedGlycan instanceof SequenceDefinedGlycan) {
                        byte[] image = getCartoonForGlycan(addedGlycan.getId());
                        addedGlycan.setCartoon(image);
                    }
                    result.getAddedGlycans().add(addedGlycan);
                    countSuccess ++;
                } catch (Exception e) {
                    logger.error ("Exception adding the glycan: " + glycan.getName(), e);
                    if (e.getCause() instanceof ErrorMessage) {
                        if (((ErrorMessage)e.getCause()).toString().contains("Duplicate")) {
                            ErrorMessage error = (ErrorMessage)e.getCause();
                            if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                                ObjectError err = error.getErrors().get(0);
                                if (err.getCodes().length != 0) {
                                    Glycan duplicateGlycan = new Glycan();
                                    try {
                                        duplicateGlycan = glycanRepository.getGlycanById(err.getCodes()[0], user);
                                        if (duplicateGlycan instanceof SequenceDefinedGlycan) {
                                            byte[] image = getCartoonForGlycan(duplicateGlycan.getId());
                                            duplicateGlycan.setCartoon(image);
                                        }
                                        result.addDuplicateSequence(duplicateGlycan);
                                    } catch (SparqlException | SQLException e1) {
                                        logger.error("Error retrieving duplicate glycan", e1);
                                    }
                                }
                            } 
                        } else {
                            result.addWrongSequence(glycan.getName(), count, sequence, ((ErrorMessage)e.getCause()).toString());
                        }
                    } else { 
                        result.addWrongSequence(glycan.getName(), count, sequence, e.getMessage());
                    }
                } 
            }
            scan.close();
            stream.close();
            result.setSuccessMessage(countSuccess + " out of " + count + " glycans are added");
            return result;
        } catch (IOException e) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("file", "NotValid"));
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            throw new IllegalArgumentException("File is not valid.", errorMessage);
        }
    }
	
	private BatchGlycanUploadResult addGlycanFromGWSFile (byte[] contents, Boolean noGlytoucanRegistration, Principal p) {
	    return addGlycanFromTextFile(contents, noGlytoucanRegistration, p, GlycanSequenceFormat.GWS.getLabel(), ";");
	}
	
	private BatchGlycanUploadResult addGlycanFromWURCSFile(byte[] contents, Boolean noGlytoucanRegistration,
            Principal p) {
	    return addGlycanFromTextFile(contents, noGlytoucanRegistration, p, GlycanSequenceFormat.WURCS.getLabel(), "\\n");
    }

    private BatchGlycanUploadResult addGlycanFromCFGFile(byte[] contents, Boolean noGlytoucanRegistration,
            Principal p) {
        return addGlycanFromTextFile(contents, noGlytoucanRegistration, p, GlycanSequenceFormat.IUPAC.getLabel(), "\\n");
    }
	
	private BatchGlycanUploadResult addGlycanFromTextFile (byte[] contents, Boolean noGlytoucanRegistration, Principal p, String format, String delimeter) {
	    UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
	    BatchGlycanUploadResult result = new BatchGlycanUploadResult();
        try {
            ByteArrayInputStream stream = new   ByteArrayInputStream(contents);
            String fileAsString = IOUtils.toString(stream, StandardCharsets.UTF_8);
            
            boolean isTextFile = Charset.forName("US-ASCII").newEncoder().canEncode(fileAsString);
            if (!isTextFile) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("file", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("File is not acceptable", errorMessage);
            }
            
            String[] structures = fileAsString.split(delimeter);
            int count = 0;
            int countSuccess = 0;
            for (String sequence: structures) {
                if (sequence == null || sequence.trim().isEmpty())
                    continue;
                count++;
                try {
                    ErrorMessage e = new ErrorMessage();
                    String glycoCT = SequenceUtils.parseSequence(e, sequence, format);
                    if (glycoCT == null || glycoCT.isEmpty()) {
                        if (e.getErrors() != null && !e.getErrors().isEmpty()) {
                            result.addWrongSequence(null, count, sequence, e.getErrors().get(0).getDefaultMessage());
                        } else {
                            result.addWrongSequence(null, count, sequence, "Cannot parse the sequence");
                        }
                    } else {
                        SequenceDefinedGlycan g = new SequenceDefinedGlycan();
                        g.setSequence(glycoCT);
                        if (glycoCT.startsWith("RES"))
                            g.setSequenceType(GlycanSequenceFormat.GLYCOCT);
                        else 
                            g.setSequenceType(GlycanSequenceFormat.WURCS);
                        
                        String existing = glycanRepository.getGlycanBySequence(glycoCT, user);
                        if (existing != null && !existing.contains("public")) {
                            // duplicate, ignore
                            logger.info("found a duplicate sequence " +  existing);
                            String id = existing.substring(existing.lastIndexOf("/")+1);
                            Glycan glycan = glycanRepository.getGlycanById(id, user);
                            if (glycan instanceof SequenceDefinedGlycan) {
                                byte[] image = getCartoonForGlycan(glycan.getId());
                                glycan.setCartoon(image);
                            }
                            if (glycan != null)
                                result.addDuplicateSequence(glycan);
                            else {
                                logger.warn ("the duplicate glycan cannot be retrieved back: " + id);
                            }
                        } else {
                            String id = addGlycan(g, p, noGlytoucanRegistration);
                            if (id == null) {
                                // cannot be added
                                result.addWrongSequence(null, count, sequence, "Cannot parse the sequence");
                            } else {
                                Glycan addedGlycan = glycanRepository.getGlycanById(id, user);
                                if (addedGlycan instanceof SequenceDefinedGlycan) {
                                    byte[] image = getCartoonForGlycan(addedGlycan.getId());
                                    addedGlycan.setCartoon(image);
                                }
                                if (addedGlycan != null) {
                                    result.getAddedGlycans().add(addedGlycan);
                                    countSuccess ++;
                                } else {
                                    logger.warn ("the added glycan cannot be retrieved back: " + id);
                                }
                            }
                        }
                    }
                }
                catch (SparqlException e) {
                    // cannot add glycan
                    stream.close();
                    throw new GlycanRepositoryException("Glycans cannot be added. Reason: " + e.getMessage());
                } catch (Exception e) {
                    logger.error ("Exception adding the sequence: " + sequence, e);
                    // sequence is not valid
                    result.addWrongSequence(null, count, sequence, e.getMessage());
                }
            }
            stream.close();
            result.setSuccessMessage(countSuccess + " out of " + count + " glycans are added");
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException("File is not valid. Reason: " + e.getMessage());
        }
	}
	
	@ApiOperation(value = "Add given linker for the user", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/addlinker", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added linker"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register linkers"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public String addLinker (
			@ApiParam(required=true, value="Linker to be added, type needs to be set correctly, pubChemId is required for small molecule and lipid, "
			        + "sequence is required for protein and peptide, other fields are optional") 
			@RequestBody Linker linker, 
			@RequestParam(value="unknown", required=false)
			@ApiParam(required=false, value="true, if the linker is of unknown type. The default is false")
			Boolean unknown, Principal p) {
		
		if (linker.getType() == null) {
			// assume OTHER
			linker.setType(LinkerType.OTHER);
		}
		
		if (unknown == null)
		    unknown = false;
		
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		
		return addService.addLinker(linker, unknown, user);
	}

	@ApiOperation(value = "Add given slide layout for the user", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/addslidelayout", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Slide layout added successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register slide layouts"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public String addSlideLayout (
			@ApiParam(required=true, value="Slide Layout to be added, name, width, height and blocks are required")
			@RequestBody SlideLayout layout, Principal p) {
		
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		
		if (layout.getName() == null || layout.getName().trim().isEmpty()) {
			errorMessage.addError(new ObjectError("name", "NoEmpty"));
		} 
		if (layout.getWidth() == null || layout.getHeight() == null)
			errorMessage.addError(new ObjectError(layout.getWidth() == null ? "width" : "height", "NoEmpty"));
		if (layout.getBlocks() == null)
			errorMessage.addError(new ObjectError("blocks", "NoEmpty"));
		
		// validate first
		if (validator != null) {
			if  (layout.getName() != null) {
				Set<ConstraintViolation<SlideLayout>> violations = validator.validateValue(SlideLayout.class, "name", layout.getName());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (layout.getDescription() != null) {
				Set<ConstraintViolation<SlideLayout>> violations = validator.validateValue(SlideLayout.class, "description", layout.getDescription());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("description", "LengthExceeded"));
				}		
			}
			if (layout.getWidth() != null) {
				Set<ConstraintViolation<SlideLayout>> violations = validator.validateValue(SlideLayout.class, "width", layout.getWidth());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("width", "PositiveOnly"));
				}		
			}
			if (layout.getHeight() != null) {
				Set<ConstraintViolation<SlideLayout>> violations = validator.validateValue(SlideLayout.class, "height", layout.getHeight());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("height", "PositiveOnly"));
				}		
			}
			
			
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		try {
			SlideLayout existing = layoutRepository.getSlideLayoutByName(layout.getName(), user);
			if (existing != null) {
				// duplicate
				errorMessage.addError(new ObjectError("name", "Duplicate"));
			}
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Slide layout cannot be added for user " + p.getName(), e);
		}
		
		if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
			throw new IllegalArgumentException("Invalid Input: Not a valid slide layout information", errorMessage);
		
		try {
			String uri = layoutRepository.addSlideLayout(layout, user);
			String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Slide layout cannot be added for user " + p.getName(), e);
		}
	}
	
	
	
	@ApiOperation(value = "Checks whether the given slidelayout name is available to be used (returns true if available, false if alredy in use", 
	        response = Boolean.class, authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value = "/checkSlidelayoutName", method = RequestMethod.GET)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Check performed successfully"),
			@ApiResponse(code = 415, message = "Media type is not supported"),
			@ApiResponse(code = 500, message = "Internal Server Error") })
	public Boolean checkSlidelayoutName(@RequestParam("slidelayoutname") final String slidelayoutname, Principal principal) throws SparqlException, SQLException {

		UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
		
		SlideLayout existing = layoutRepository.getSlideLayoutByName(slidelayoutname.trim(), user);

		if (existing != null) {
			// duplicate
			ErrorMessage errorMessage = new ErrorMessage("Cannot add duplicate slide layout");
			errorMessage.addError(new ObjectError("slidelayoutname", "Duplicate"));
			throw new GlycanExistsException("A slide layout with the same name already exists", errorMessage);
		}

		return true;
	}
	
	@ApiOperation(value = "Delete given block layout", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/deleteblocklayout/{layoutId}", method = RequestMethod.DELETE, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Block Layout deleted successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to delete block layouts"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation deleteBlockLayout (
			@ApiParam(required=true, value="id of the block layout to delete") 
			@PathVariable("layoutId") String blockLayoutId, Principal principal) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			layoutRepository.deleteBlockLayout(blockLayoutId.trim(), user);
			return new Confirmation("Block Layout deleted successfully", HttpStatus.OK.value());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot delete block layout " + blockLayoutId);
		} catch (IllegalArgumentException e) {
		    // in use, we cannot delete
		    ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
		    errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
		    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		    errorMessage.addError(new ObjectError("blockLayout", "InUse"));
		    throw new IllegalArgumentException(e.getMessage(), errorMessage);
		}
	}
	
	@ApiOperation(value = "Delete given feature from the user's list", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/deletefeature/{featureId}", method = RequestMethod.DELETE, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Feature deleted successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to delete linkers"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation deleteFeature (
			@ApiParam(required=true, value="id of the feature to delete") 
			@PathVariable("featureId") String featureId, Principal principal) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			featureRepository.deleteFeature(featureId.trim(), user);
			return new Confirmation("Feature deleted successfully", HttpStatus.OK.value());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot delete feature " + featureId, e);
		} catch (IllegalArgumentException e) {
            // in use, we cannot delete
            ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
            errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("feature", "InUse"));
            throw new IllegalArgumentException(e.getMessage(), errorMessage);
        }
	}
	
	@ApiOperation(value = "Delete given glycan from the user's list", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/delete/{glycanId}", method = RequestMethod.DELETE, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Glycan deleted successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to delete glycans"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation deleteGlycan (
			@ApiParam(required=true, value="id of the glycan to delete") 
			@PathVariable("glycanId") String glycanId, Principal principal) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			glycanRepository.deleteGlycan(glycanId.trim(), user);
			return new Confirmation("Glycan deleted successfully", HttpStatus.OK.value());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot delete glycan " + glycanId, e);
		} catch (IllegalArgumentException e) {
            // in use, we cannot delete
            ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
            errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("glycan", "InUse"));
            throw new IllegalArgumentException(e.getMessage(), errorMessage);
        }
	}
	
	@ApiOperation(value = "Delete given linker from the user's list", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/deletelinker/{linkerId}", method = RequestMethod.DELETE, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Linker deleted successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to delete linkers"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation deleteLinker (
			@ApiParam(required=true, value="id of the linker to delete") 
			@PathVariable("linkerId") String linkerId, Principal principal) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			linkerRepository.deleteLinker(linkerId.trim(), user);
			return new Confirmation("Linker deleted successfully", HttpStatus.OK.value());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot delete linker " + linkerId, e);
		} catch (IllegalArgumentException e) {
            // in use, we cannot delete
            ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
            errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("linker", "InUse"));
            throw new IllegalArgumentException(e.getMessage(), errorMessage);
        }
	}
	
	@ApiOperation(value = "Delete given slide layout", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/deleteslidelayout/{layoutId}", method = RequestMethod.DELETE, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Slide Layout deleted successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to delete slide layouts"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation deleteSlideLayout (
			@ApiParam(required=true, value="id of the block layout to delete") 
			@PathVariable("layoutId") String layoutId, Principal principal) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			layoutRepository.deleteSlideLayout(layoutId.trim(), user);
			return new Confirmation("Slide Layout deleted successfully", HttpStatus.OK.value());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot delete slide layout " + layoutId, e);
		} catch (IllegalArgumentException e) {
            // in use, we cannot delete
            ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
            errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("slideLayout", "InUse"));
            throw new IllegalArgumentException(e.getMessage(), errorMessage);
        }
	}
	
	@ApiOperation(value = "Retrieve block layout with the given id", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/getblocklayout/{layoutId}", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Block Layout retrieved successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list block layouts"),
			@ApiResponse(code=404, message="Block layout with given id does not exist"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public BlockLayout getBlockLayout (
			@ApiParam(required=true, value="id of the block layout to retrieve") 
			@PathVariable("layoutId") String layoutId, 
			@ApiParam (required=false, defaultValue = "true", value="if false, do not load block details. Default is true (to load all)")
			@RequestParam(required=false, defaultValue = "true", value="loadAll") Boolean loadAll, 
			Principal p) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
			BlockLayout layout = layoutRepository.getBlockLayoutById(layoutId.trim(), user, loadAll);
			if (layout == null) {
			    // check if it is from public graph
			    layout = layoutRepository.getBlockLayoutById(layoutId.trim(), null, loadAll);
			    if (layout == null) 
			        throw new EntityNotFoundException("Block layout with id : " + layoutId + " does not exist in the repository");
			}
			
			if (loadAll && layout.getSpots() != null) {        
                for (org.glygen.array.persistence.rdf.Spot s: layout.getSpots()) {
                    if (s.getFeatures() == null) 
                        continue;
                    for (org.glygen.array.persistence.rdf.Feature f: s.getFeatures()) {
                        populateFeatureGlycanImages(f, imageLocation);
                    }
                }
		    }
			return layout;
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Block Layout cannot be retrieved for user " + p.getName(), e);
		}
	}
	
	public static void populateFeatureGlycanImages (org.glygen.array.persistence.rdf.Feature feature, String imageLocation) {
	    List<Glycan> glycanList = null;
        switch (feature.getType()) {
        case LINKEDGLYCAN:
            if (((LinkedGlycan) feature).getGlycans() != null) {
                glycanList = new ArrayList<Glycan>();
                for (GlycanInFeature g: ((LinkedGlycan) feature).getGlycans()) {
                    glycanList.add(g.getGlycan());
                }
            }
            break;
        case GLYCOLIPID:
            if (((GlycoLipid) feature).getGlycans() != null) {
                glycanList = new ArrayList<Glycan>();
                for (LinkedGlycan g: ((GlycoLipid) feature).getGlycans()) {
                    for (GlycanInFeature glycan: ((LinkedGlycan) g).getGlycans()) {
                        glycanList.add(glycan.getGlycan());
                    }
                }
            }
            break;
        case GLYCOPEPTIDE:
            if (((GlycoPeptide) feature).getGlycans() != null) {
                glycanList = new ArrayList<Glycan>();
                for (LinkedGlycan g: ((GlycoPeptide) feature).getGlycans()) {
                    for (GlycanInFeature glycan: ((LinkedGlycan) g).getGlycans()) {
                        glycanList.add(glycan.getGlycan());
                    }
                }
            }
            break;
        case GLYCOPROTEIN:
            if (((GlycoProtein) feature).getGlycans() != null) {
                glycanList = new ArrayList<Glycan>();
                for (LinkedGlycan g: ((GlycoProtein) feature).getGlycans()) {
                    for (GlycanInFeature glycan: ((LinkedGlycan) g).getGlycans()) {
                        glycanList.add(glycan.getGlycan());
                    }
                }
            }
            break;
        case GPLINKEDGLYCOPEPTIDE:
            if (((GPLinkedGlycoPeptide) feature).getPeptides() != null) {
                glycanList = new ArrayList<Glycan>();
                for (GlycoPeptide gp: ((GPLinkedGlycoPeptide) feature).getPeptides()) {
                    for (LinkedGlycan g: gp.getGlycans()) {
                        for (GlycanInFeature glycan: ((LinkedGlycan) g).getGlycans()) {
                            glycanList.add(glycan.getGlycan());
                        }
                    }
                }
            }
            break;
        case LANDING_LIGHT:
            break;
        case NEGATIVE_CONTROL:
        case COMPOUND:
        case CONTROL:
        default:
            break;
        }
        if (glycanList != null) {
            for (Glycan glycan: glycanList) {
                if (glycan.getType().equals(GlycanType.SEQUENCE_DEFINED)) {
                    byte[] image = GlygenArrayController.getCartoonForGlycan(glycan.getId(), imageLocation);
                    if (image == null && ((SequenceDefinedGlycan) glycan).getSequence() != null) {
                        // try to create one
                        BufferedImage t_image = GlygenArrayController.createImageForGlycan((SequenceDefinedGlycan) glycan);
                        if (t_image != null) {
                            String filename = glycan.getId() + ".png";
                            //save the image into a file
                            logger.debug("Adding image to " + imageLocation);
                            File imageFile = new File(imageLocation + File.separator + filename);
                            try {
                                ImageIO.write(t_image, "png", imageFile);
                            } catch (IOException e) {
                                logger.error ("Glycan image cannot be written", e);
                            }
                        }
                        image = GlygenArrayController.getCartoonForGlycan(glycan.getId(), imageLocation);
                    }
                    glycan.setCartoon(image);
                }
            }
        }
	}
	
	private byte[] getCartoonForGlycan (String glycanId) {
	    return getCartoonForGlycan(glycanId, imageLocation);
    }
	
	public static byte[] getCartoonForGlycan (String glycanId, String imageLocation) {
		try {
			File imageFile = new File(imageLocation + File.separator + glycanId + ".png");
			if (imageFile.exists()) {
			    InputStreamResource resource = new InputStreamResource(new FileInputStream(imageFile));
			    return IOUtils.toByteArray(resource.getInputStream());
			}
		} catch (Exception e) {
			logger.warn("Image cannot be retrieved for glycan " + glycanId, e);
			
		}
		return null;
	}
	
	@SuppressWarnings("rawtypes")
    SlideLayout getFullLayoutFromLibrary (File libraryFile, SlideLayout layout) {
		try {
			FileInputStream inputStream2 = new FileInputStream(libraryFile);
	        InputStreamReader reader2 = new InputStreamReader(inputStream2, "UTF-8");
	        List<Class> contextList = new ArrayList<Class>(Arrays.asList(FilterUtils.filterClassContext));
    		contextList.add(ArrayDesignLibrary.class);
	        JAXBContext context2 = JAXBContext.newInstance(contextList.toArray(new Class[contextList.size()]));
	        Unmarshaller unmarshaller2 = context2.createUnmarshaller();
	        ArrayDesignLibrary library = (ArrayDesignLibrary) unmarshaller2.unmarshal(reader2);
	        List<org.grits.toolbox.glycanarray.library.om.layout.SlideLayout> layoutList = 
	        		library.getLayoutLibrary().getSlideLayout();
	        ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
	        for (org.grits.toolbox.glycanarray.library.om.layout.SlideLayout slideLayout : layoutList) {
	        	if (slideLayout.getName().equalsIgnoreCase(layout.getName()) ||
	        	        layout.getId().equals(slideLayout.getId().toString())) {
	        		SlideLayout mySlideLayout = new SlideLayout();
	        		mySlideLayout.setName(slideLayout.getName());
	        		String desc = null;
	        		if (slideLayout.getDescription() != null) {
	        			desc = slideLayout.getDescription();
	        			if (desc.length() >= ValidationConstants.DESCRIPTION_LIMIT) {
	        				desc = desc.substring(0, ValidationConstants.DESCRIPTION_LIMIT-1);
	        			}
	        		}
	        		mySlideLayout.setDescription(desc);
	        		List<org.glygen.array.persistence.rdf.Block> blocks = new ArrayList<>();
	        		int width = 0;
	        		int height = 0;
	        		
	        		// create a SpotMetadata with no information since we don't get the information from the library file
	                SpotMetadata spotMetadata = new SpotMetadata();
	                spotMetadata.setName(slideLayout.getName() + "-spotMetadata");
	                try {
	        	        String uri = templateRepository.getTemplateByName("Default Spot", MetadataTemplateType.SPOT);
	        	        if (uri != null) {
	        	        	MetadataTemplate template = templateRepository.getTemplateFromURI(uri);
	        	        	DescriptionTemplate descT = ExtendedGalFileParser.getKeyFromTemplate("Dispenses", template);
	        	        	DescriptorGroup group = new DescriptorGroup();
	        	            group.setKey(descT);
	        	            group.setNotRecorded(true);
	        	            spotMetadata.setDescriptorGroups(new ArrayList<>());
	        	            spotMetadata.getDescriptorGroups().add(group);
	        	            spotMetadata.setTemplate(template.getName());
	        	        } else {
	        	        	errorMessage.addError(new ObjectError("spot template", "NotFound"));
	        	        }
	                } catch (SparqlException | SQLException e) {
	                	errorMessage.addError(new ObjectError("spot template", "NotValid"));
	                }
	                
	                FeatureMetadata featureMetadata = new FeatureMetadata();
	                featureMetadata.setName(slideLayout.getName() + "-featureMetadata");
	                try {
	        	        String uri = templateRepository.getTemplateByName("Feature Feature", MetadataTemplateType.FEATURE);
	        	        if (uri != null) {
	        	        	MetadataTemplate template = templateRepository.getTemplateFromURI(uri);
	        	        	DescriptionTemplate descT = ExtendedGalFileParser.getKeyFromTemplate("Commercial source", template);
	        	        	DescriptorGroup group = new DescriptorGroup();
	        	            group.setKey(descT);
	        	            group.setNotRecorded(true);
	        	            featureMetadata.setDescriptorGroups(new ArrayList<>());
	        	            featureMetadata.getDescriptorGroups().add(group);
	        	            featureMetadata.setTemplate(template.getName());
	        	        } else {
	        	        	errorMessage.addError(new ObjectError("feature template", "NotFound"));
	        	        }
	                } catch (SparqlException | SQLException e) {
	                	errorMessage.addError(new ObjectError("feature template", "NotValid"));
	                }
	                
	        		for (Block block: slideLayout.getBlock()) {
	        			org.glygen.array.persistence.rdf.Block myBlock = new org.glygen.array.persistence.rdf.Block();
	        			myBlock.setColumn(block.getColumn());
	        			myBlock.setRow(block.getRow());
	        			if (block.getColumn() > width)
	        				width = block.getColumn();
	        			if (block.getRow() > height)
	        				height = block.getRow();
	        			Integer blockLayoutId = block.getLayoutId();
	        			org.grits.toolbox.glycanarray.library.om.layout.BlockLayout blockLayout = LibraryInterface.getBlockLayout(library, blockLayoutId);
	        			if (blockLayout == null) {
	        			    // it should have been in the file
	        	            errorMessage.addError(new ObjectError("blockLayout:" + blockLayoutId, "NotFound"));
	        	            continue;
	        			}
	        			org.glygen.array.persistence.rdf.BlockLayout myLayout = new org.glygen.array.persistence.rdf.BlockLayout();
	        			String name = null;
	        			if (blockLayout.getName() != null) {
	        				name = blockLayout.getName();
	        				if (name.length() >= ValidationConstants.NAME_LIMIT) {
	        					name = name.substring(0, ValidationConstants.NAME_LIMIT-1);
	        				}
	        			}
	        			myLayout.setName(name);
	        			myLayout.setWidth(blockLayout.getColumnNum());
	        			myLayout.setHeight(blockLayout.getRowNum());
	        			String comment = null;
	        			if (blockLayout.getComment() != null) {
	        				comment = blockLayout.getComment().replaceAll("\\r", " ").replaceAll("\\n", " ");
	        				if (comment.length() >= ValidationConstants.DESCRIPTION_LIMIT) {
	        					comment = comment.substring(0, ValidationConstants.DESCRIPTION_LIMIT-1);
	        				}
	        			}
	        			myLayout.setDescription(comment);
	        			myBlock.setBlockLayout(myLayout); 
	        			try {
    	        			List<org.glygen.array.persistence.rdf.Spot> spots = getSpotsFromBlockLayout(library, blockLayout, spotMetadata, featureMetadata);
    	        			//myBlock.setSpots(spots);
    	        			myLayout.setSpots(spots);
    	        			blocks.add(myBlock);
	        			} catch (Exception e) {
	        			    if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
	                            for (ObjectError err: ((ErrorMessage) e.getCause()).getErrors()) {
	                                errorMessage.addError(err);
	                            }
	                        } else {
	                            errorMessage.addError(new ObjectError("internalError", e.getMessage()));
	                        }
	        			}
	        		}
	        		
	        		mySlideLayout.setHeight(slideLayout.getHeight() == null ? height: slideLayout.getHeight());
	        		mySlideLayout.setWidth(slideLayout.getWidth() == null ? width: slideLayout.getWidth());
	        		mySlideLayout.setBlocks(blocks);
	        		
	        		if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
	        		    throw new IllegalArgumentException("Not a valid array library!", errorMessage);
	        		}
	        		return mySlideLayout;
	        	}
	        }
		} catch (JAXBException e) {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
			errorMessage.addError(new ObjectError("file", "NotValid"));
			throw new IllegalArgumentException("Not a valid array library!", errorMessage);
		} catch (IOException e) {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
			errorMessage.addError(new ObjectError("file", "NotValid"));
			throw new IllegalArgumentException("File cannot be found!", errorMessage);
		}
		return null;
	}
	
	@ApiOperation(value = "Import slide layout from uploaded GAL file", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/addSlideLayoutFromGalFile", method=RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added slide layout"), 
            @ApiResponse(code=400, message="Invalid request, file cannot be found"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to register slide layouts"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
	public String addSlideLayoutFromGalFile (
	        @ApiParam(required=true, value="uploaded GAL file information")
	        @RequestBody
            FileWrapper fileWrapper,
	        @ApiParam(required=true, value="name of the slide layout to be created") 
	        @RequestParam("name")
	        String slideLayoutName, 
	        @ApiParam(required=false, value="width of the slide layout", example="1") 
            @RequestParam(required=false, value="width")
	        Integer width,
	        @ApiParam(required=false, value="height of the slide layout", example="1") 
            @RequestParam(required=false, value="height")
	        Integer height,
	        Principal p) {
	    
	    ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
	    if ((height != null && width == null) || (width != null && height == null)) {
	        // either both or none should be provided
	        errorMessage.addError(new ObjectError("dimension", "NotValid")); 
	    }
	    
	    if (fileWrapper != null && fileWrapper.getIdentifier() != null) {
	        //uploadedFileName = moveToTempFile (uploadedFileName);
	        String uploadedFileName = fileWrapper.getIdentifier();
            File galFile = new File(uploadDir, uploadedFileName);
            if (galFile.exists()) {
                
                // check if the name is available
                try {
                    UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
                    SlideLayout existing = layoutRepository.getSlideLayoutByName(slideLayoutName.trim(), user);
                    if (existing != null) {
                        errorMessage = new ErrorMessage("There is already a slide layout with that name");
                        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                        errorMessage.addError(new ObjectError("name", "Duplicate"));
                        throw new IllegalArgumentException("There is already a slide layout with that name", errorMessage);
                    }

                    // need to retrieve full list of linkers first
                    //LinkerListResultView result = listLinkers(0, null, null, null, null, p);
                    GalFileImportResult importResult = galFileParser.parse(galFile.getAbsolutePath(), slideLayoutName.trim(), height, width);
                    
                    errorMessage = new ErrorMessage();
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    if (!importResult.getErrors().isEmpty()) {
                        for (ErrorMessage error: importResult.getErrors()) {
                            for (ObjectError err: error.getErrors()) {
                                errorMessage.addError(err);
                            }
                        }
                    }
                    
                    if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                        throw new IllegalArgumentException("Errors processing the gal file", errorMessage);
                    }
                    
                    
                    SlideLayout layout = importResult.getLayout();
                    if (width != null && height != null) {
                        if (layout.getHeight() == 1 && layout.getWidth() == 1) { // only one block in the GAL file
                            if (layout.getBlocks() != null && layout.getBlocks().size() > 0) {
                                BlockLayout blockLayout = layout.getBlocks().get(0).getBlockLayout();
                                if (blockLayout == null) {
                                    // error
                                    errorMessage = new ErrorMessage();
                                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                                    errorMessage.addError(new ObjectError("blockLayout", "NotFound"));
                                    throw new IllegalArgumentException("Block layout cannot be extracted from the GAL file", errorMessage);
                                }
                                
                                // repeat the same block layout for all blocks
                                if (width > 0 && height > 0) { 
                                    List<org.glygen.array.persistence.rdf.Block> blocks = new ArrayList<>();
                                    for (int i=0; i < width; i++) {
                                        for (int j=0; j < height; j++) {
                                            org.glygen.array.persistence.rdf.Block block = new org.glygen.array.persistence.rdf.Block();
                                            block.setRow(j+1);
                                            block.setColumn(i+1);
                                            block.setBlockLayout(blockLayout);
                                            blocks.add(block);
                                        }
                                    }
                                    layout.setBlocks(blocks);
                                    layout.setHeight(height);
                                    layout.setWidth(width);
                                }
                            } else {
                                // error
                                errorMessage = new ErrorMessage();
                                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                                errorMessage.addError(new ObjectError("blockLayout", "NotFound"));
                                throw new IllegalArgumentException("Block layout cannot be extracted from the GAL file", errorMessage);
                            }
                        } else if (layout.getWidth() != width || layout.getHeight() != height){
                            // error
                            errorMessage = new ErrorMessage();
                            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                            errorMessage.addError(new ObjectError("width", "NotValid"));
                            errorMessage.addError(new ObjectError("height", "NotValid"));
                            throw new IllegalArgumentException("Width and height specification does not match with GAL file", errorMessage);
                        }
                    }
                    
                    // add all new glycans and features and block layouts first
                    try {
                        //for (Glycan g: importResult.getGlycanList()) {
                        //    addGlycan(g, p, true);
                        //}
                        //for (org.glygen.array.persistence.rdf.Feature f: importResult.getFeatureList()) {
                        //    addFeature(f, p);
                        //}
                        for (BlockLayout b: importResult.getLayoutList()) {
                            addBlockLayout(b, false, p);
                        }
                    } catch (IllegalArgumentException e) {
                        // need to ignore duplicate errors
                        if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                            ErrorMessage error = (ErrorMessage) e.getCause();
                            for (ObjectError err: error.getErrors()) {
                                if (err.getDefaultMessage().equalsIgnoreCase("duplicate")) {
                                    // ignore
                                } else {
                                    errorMessage.addError(err);
                                }
                            }
                        } else {
                            throw e;
                        }
                    }
                    if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                        throw new IllegalArgumentException("Errors processing the gal file", errorMessage);
                    }
                    
                    String slideURI = addSlideLayout(importResult.getLayout(), p);
                    String id = slideURI.substring(slideURI.lastIndexOf("/")+1);
                    
                    // save the GAL file with the layout
                    File slideLayoutFolder = new File (uploadDir + File.separator + id);
                    if (!slideLayoutFolder.exists()) {
                        slideLayoutFolder.mkdirs();
                    }
                    File newFile = new File(slideLayoutFolder + File.separator + uploadedFileName);
                    if(galFile.renameTo (newFile)) { 
                        // if file copied successfully then move the original file into temp folder, will be deleted later as part of the cleanup
                        moveToTempFile(uploadedFileName); 
                    } else { 
                        throw new GlycanRepositoryException("File cannot be moved to the dataset folder");
                    }
                    fileWrapper.setFileFolder(uploadDir + File.separator + id);
                    fileWrapper.setFileSize(newFile.length());
                    fileWrapper.setIdentifier(uploadedFileName);
                    fileWrapper.setFileFormat("GAL");    //TODO do we need to standardize this?
                    
                    importResult.getLayout().setFile(fileWrapper);
                    layoutRepository.updateSlideLayout(importResult.getLayout(), user);
                    
                    return id;
                } catch (IllegalArgumentException e) {
                    throw e;
                } catch (IOException | SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("SlideLayout could not be added", e);
                }
            } else {
                errorMessage.addError(new ObjectError("file", "NotValid"));
                throw new IllegalArgumentException("File cannot be found", errorMessage);
            }
	    } else {
            errorMessage.addError(new ObjectError("file", "NotValid"));
            throw new IllegalArgumentException("File cannot be found", errorMessage);
	    }
	}
	
	private String moveToTempFile(String uploadedFileName) {
        if (uploadedFileName.startsWith("tmp"))
            return uploadedFileName;
        File oldFile = new File (uploadDir, uploadedFileName);
        File newFile = new File (uploadDir, "tmp" + uploadedFileName);
        if (newFile.exists()) {
            // already moved to tmp
            return "tmp" + uploadedFileName;
        }
        boolean b = oldFile.renameTo(newFile);
        if (b) return "tmp" + uploadedFileName;
        
        return uploadedFileName;
    }

    @ApiOperation(value = "Import selected slide layouts from uploaded GRITS array library file", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value = "/addSlideLayoutFromLibrary", method=RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Slide layouts imported into repository successfully"), 
			@ApiResponse(code=400, message="Invalid request, file cannot be found"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register slide layouts"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public String addSlideLayoutFromLibrary (
			@ApiParam(required=true, value="uploaded file and the list of slide layouts to be imported, only name is sufficient for a slide layout")
			@RequestBody LibraryImportInput input, 
			Principal p) {
		
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        FileWrapper fileWrapper = input.getFile();
		if (fileWrapper != null && fileWrapper.getIdentifier() != null) {
		    String uploadedFileName = fileWrapper.getIdentifier();
		    String finalFileName = moveToTempFile (uploadedFileName.trim());
			File libraryFile = new File(uploadDir, finalFileName);
			if (libraryFile.exists()) {
				if (input.getSlideLayout() == null) {
					ErrorMessage errorMessage = new ErrorMessage("No slide layout provided");
					errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
					errorMessage.addError(new ObjectError("slideLayout", "NoEmpty"));
					throw new IllegalArgumentException("No slide layout provided", errorMessage);
				}
				
				ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
				// check if already exists before trying to import
			    String searchName = null;
				if (input.getSlideLayout().getName() != null) {
				    searchName = input.getSlideLayout().getName();
					try {
						SlideLayout existing = layoutRepository.getSlideLayoutByName(searchName, user);
						if (existing != null) {
						    SlideLayoutError errorObject = new SlideLayoutError();
                            errorObject.setLayout(createSlideLayoutView(input.getSlideLayout()));
                            errorMessage.addError(new ObjectError("slideLayout", "Duplicate"));
                            
						}
					} catch (Exception e) {
						errorMessage.addError(new ObjectError("internalError", e.getMessage()));
						throw new IllegalArgumentException("Slide layout search failed", errorMessage);
					}
				}
				if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
				    throw new IllegalArgumentException("Slide layout with the given name already exists", errorMessage);
				}
				
				try {
				    SlideLayout slideLayout = getFullLayoutFromLibrary (libraryFile, input.getSlideLayout());
				    if (searchName != null) {
                        slideLayout.setName(searchName);
                    }
                    CompletableFuture<String> slideId = null;
                    if (slideLayout != null) {
                        // save slide layout without its blocks and update its status to "processing"
                        String uri = layoutRepository.addSlideLayout(slideLayout, user, true);  
                        slideLayout.setUri(uri);
                        String id = uri.substring(uri.lastIndexOf("/")+1);
                        if (slideLayout.getError() == null)
                            slideLayout.setStatus(FutureTaskStatus.PROCESSING);
                        else 
                            slideLayout.setStatus(FutureTaskStatus.ERROR);
                        repository.updateStatus (uri, slideLayout, user);
                        
                        try {
                            slideId = parserAsyncService.importSlideLayout(slideLayout, errorMessage, user);
                            // add the slide layout 
                            slideId.whenComplete((uriString, e) -> {
                                try {
                                    if (e != null) {
                                        logger.error(e.getMessage(), e);
                                        slideLayout.setStatus(FutureTaskStatus.ERROR);
                                        if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException && e.getCause().getCause() instanceof ErrorMessage) 
                                            slideLayout.setError((ErrorMessage) e.getCause().getCause());
                                    } else {
                                        slideLayout.setStatus(FutureTaskStatus.DONE);    
                                    }
                                    repository.updateStatus (uriString, slideLayout, user);
                                } catch (SparqlException | SQLException ex) {
                                    throw new GlycanRepositoryException("SlideLayout cannot be added for user " + p.getName(), e);
                                } 
                            });
                            slideId.get(1000, TimeUnit.MILLISECONDS);
                        } catch (IllegalArgumentException e) {
                            slideLayout.setStatus(FutureTaskStatus.ERROR);
                            if (e.getCause() != null && e.getCause() instanceof ErrorMessage)
                                slideLayout.setError((ErrorMessage) e.getCause());
                            repository.updateStatus (uri, slideLayout, user);
                            throw e;
                        } catch (TimeoutException e) {
                            synchronized (this) {
                                if (slideLayout.getError() == null)
                                    slideLayout.setStatus(FutureTaskStatus.PROCESSING);
                                else 
                                    slideLayout.setStatus(FutureTaskStatus.ERROR);
                                repository.updateStatus (uri, slideLayout, user);
                                return id;
                            }
                        }
                        return id;
                    } else {
                        errorMessage.addError(new ObjectError("slideLayout", "NotValid"));
                        throw new IllegalArgumentException("Given slide layout cannot be found in the file", errorMessage);
                    }
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Rawdata cannot be added for user " + p.getName(), e);
                } catch (IllegalArgumentException e) {
                    throw e;
                } catch (Exception e) {
                    logger.error("Error getting slide layout from the library file", e);
                    if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                        for (ObjectError err: ((ErrorMessage) e.getCause()).getErrors()) {
                            errorMessage.addError(err);
                        }
                    } else {
                        errorMessage.addError(new ObjectError("internalError", e.getMessage()));
                    }
                    throw new IllegalArgumentException("Error getting slide layout from the library file", errorMessage);
                }
			} else {
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
				errorMessage.addError(new ObjectError("file", "NotValid"));
				throw new IllegalArgumentException("File cannot be found", errorMessage);
			}
		} else {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
			errorMessage.addError(new ObjectError("file", "NotValid"));
			throw new IllegalArgumentException("File cannot be found", errorMessage);
		}	
	}
	
	SlideLayout createSlideLayoutView (SlideLayout layout) {
	    SlideLayout view = new SlideLayout();
	    view.setId(layout.getId());
	    view.setUri(layout.getUri());
	    view.setIsPublic(layout.getIsPublic());
	    view.setUser(layout.getUser());
	    view.setName(layout.getName());
	    view.setDescription(layout.getDescription());
	    view.setWidth(layout.getWidth());
	    view.setHeight(layout.getHeight());
	    view.setDateCreated(layout.getDateCreated());
	    view.setDateAddedToLibrary(layout.getDateAddedToLibrary());
	    view.setDateModified(layout.getDateModified());
	    
	    return view;
	}
	
	@ApiOperation(value = "Retrieve glycan with the given id", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/getglycan/{glycanId}", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Glycan retrieved successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list glycans"),
			@ApiResponse(code=404, message="Gycan with given id does not exist"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Glycan getGlycan (
			@ApiParam(required=true, value="id of the glycan to retrieve") 
			@PathVariable("glycanId") String glycanId, Principal p) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
			Glycan glycan = glycanRepository.getGlycanById(glycanId.trim(), user);
			if (glycan == null) {
			    glycan = glycanRepository.getGlycanById(glycanId.trim(), null);
			    if (glycan == null) {
			        throw new EntityNotFoundException("Glycan with id : " + glycanId + " does not exist in the repository");
			    }
			}
			if (glycan instanceof SequenceDefinedGlycan) {
			    byte[] image = getCartoonForGlycan(glycanId.trim());
                if (image == null && ((SequenceDefinedGlycan) glycan).getSequence() != null) {
                    // try to create one
                    BufferedImage t_image = createImageForGlycan ((SequenceDefinedGlycan) glycan);
                    if (t_image != null) {
                        String filename = glycan.getId() + ".png";
                        //save the image into a file
                        logger.debug("Adding image to " + imageLocation);
                        File imageFile = new File(imageLocation + File.separator + filename);
                        try {
                            ImageIO.write(t_image, "png", imageFile);
                        } catch (IOException e) {
                            logger.error ("Glycan image cannot be written", e);
                        }
                    }
                    image = getCartoonForGlycan(glycanId);
                }
                glycan.setCartoon(image);
			    
			}
			return glycan;
			
			
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Glycan cannot be retrieved for user " + p.getName(), e);
		}
		
	}
	
	@ApiOperation(value = "Retrieve id for a glycan given the sequence", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/getGlycanBySequence", method = RequestMethod.GET)
	@ApiResponses (value ={@ApiResponse(code=200, message="Glycan id retrieved successfully", response = String.class), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list glycans"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public String getGlycanBySequence (
			@ApiParam(required=true, value="Sequence of the glycan to retrieve (in GlycoCT)") 
			@RequestParam String sequence, Principal principal) {
		if (sequence == null || sequence.isEmpty())
			return null;
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			String seq = URLDecoder.decode(sequence, StandardCharsets.UTF_8.name());
			String glycanURI = glycanRepository.getGlycanBySequence(seq.trim(), user);
			if (glycanURI != null)
				return glycanURI.substring(glycanURI.lastIndexOf("/") + 1);
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("error getting glycan: ", e);
		} catch (UnsupportedEncodingException e) {
			logger.info(e.getMessage());  // ignore, should not happen
		}
		return null;
	}
	
	@ApiOperation(value = "Retrieve sequence (in GlycoCT) from GlyToucan for the glycan with the given glytoucan id (accession number)")
	@RequestMapping(value="/getGlycanFromGlytoucan", method = RequestMethod.GET)
	@ApiResponses (value ={@ApiResponse(code=200, message="Glycan retrieved successfully", response = String.class), 
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public String getGlycanFromGlytoucan (
			@ApiParam(required=true, value="Accession number of the glycan to retrieve (from GlyToucan)") 
			@RequestParam String glytoucanId) {
		if (glytoucanId == null || glytoucanId.trim().isEmpty())
			return null;
		return addService.getSequenceFromGlytoucan(glytoucanId);
	}
	
	@ApiOperation(value = "Retrieve image for given glycan")
	@RequestMapping(value="/getimage/{glycanId}", method = RequestMethod.GET, 
		produces = MediaType.IMAGE_PNG_VALUE )
	@ApiResponses (value ={@ApiResponse(code=200, message="Image retrieved successfully"), 
			@ApiResponse(code=404, message="Image for the given glycan is not available"),
			@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public @ResponseBody byte[] getImageForGlycan (
			@ApiParam(required=true, value="Id of the glycan to retrieve the image for") 
			@PathVariable("glycanId") String glycanId) {
		try {
			File imageFile = new File(imageLocation + File.separator + glycanId.trim() + ".png");
			InputStreamResource resource = new InputStreamResource(new FileInputStream(imageFile));
			return IOUtils.toByteArray(resource.getInputStream());
		} catch (IOException e) {
			logger.error("Image cannot be retrieved", e);
			throw new EntityNotFoundException("Image for glycan " + glycanId + " is not available");
		}
	}
	
	@ApiOperation(value = "Retrieve linker with the given id", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/getlinker/{linkerId}", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Linker retrieved successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list glycans"),
			@ApiResponse(code=404, message="Linker with given id does not exist"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Linker getLinker (
			@ApiParam(required=true, value="id of the linker to retrieve") 
			@PathVariable("linkerId") String linkerId, Principal p) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
			Linker linker = linkerRepository.getLinkerById(linkerId.trim(), user);
			if (linker == null) {
			    linker = linkerRepository.getLinkerById(linkerId.trim(), null);
			    if (linker == null) {
			        throw new EntityNotFoundException("Linker with id : " + linkerId + " does not exist in the repository");
			    }
			}
			
			return linker;
			
			
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Linker cannot be retrieved for user " + p.getName(), e);
		}
		
	}
	
	@ApiOperation(value = "Retrieve slide layout with the given id", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/getslidelayout/{layoutId}", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Slide Layout retrieved successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list slide layouts"),
			@ApiResponse(code=404, message="Slide layout with given id does not exist"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public SlideLayout getSlideLayout (
			@ApiParam(required=true, value="id of the slide layout to retrieve") 
			@PathVariable("layoutId") String layoutId, 
			@ApiParam (required=false, defaultValue = "true", value="if false, do not load slide details. Default is true (to load all)")
			@RequestParam(required=false, defaultValue = "true", value="loadAll") Boolean loadAll, 
			Principal p) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
			SlideLayout layout = layoutRepository.getSlideLayoutById(layoutId.trim(), user, loadAll);
			if (layout == null) {
			    // check the public repository
			    layout = layoutRepository.getSlideLayoutById(layoutId.trim(), null, loadAll);
			    if (layout == null) {
			        throw new EntityNotFoundException("Slide layout with id : " + layoutId + " does not exist in the repository");
			    }
			}
			
			if (loadAll && layout.getBlocks() != null) {
                for (org.glygen.array.persistence.rdf.Block block: layout.getBlocks()) {
                    if (block.getBlockLayout() != null) {
                        if (block.getBlockLayout().getSpots() == null)
                            continue;
                        for (org.glygen.array.persistence.rdf.Spot s: block.getBlockLayout().getSpots()) {
                            if (s.getFeatures() == null)
                                continue;
                            for (org.glygen.array.persistence.rdf.Feature f: s.getFeatures()) {
                                populateFeatureGlycanImages(f, imageLocation);
                            }
                        }
                    }
                }
            }
			return layout;
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Slide Layout cannot be retrieved for user " + p.getName(), e);
		}
	}
	
	@ApiOperation(value = "Retrieve feature with the given id", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/getfeature/{featureId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Feature retrieved successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to retrieve the feature"),
            @ApiResponse(code=404, message="Feature with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public org.glygen.array.persistence.rdf.Feature getFeature (
            @ApiParam(required=true, value="id of the feature to retrieve") 
            @PathVariable("featureId") String featureId, 
            Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            org.glygen.array.persistence.rdf.Feature feature = featureRepository.getFeatureById(featureId.trim(), user);
            if (feature == null) {
                feature = featureRepository.getFeatureById(featureId.trim(), null);
                if (feature == null) {
                    throw new EntityNotFoundException("Feature with id : " + featureId + " does not exist in the repository");
                }
            }
            
            populateFeatureGlycanImages(feature, imageLocation);
            return feature;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Feature cannot be retrieved for user " + p.getName(), e);
        }
    }
	
	/**
	 * the library file should already be uploaded to "uploadDir" before calling this service.
	 * 
	 * @param uploadedFileName the name of the library file already uploaded
	 * @return list of slide layouts in the library
	 */
	@SuppressWarnings("rawtypes")
    @ApiOperation(value = "Retrieve slide layouts from uploaded GRITS array library file", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value = "/getSlideLayoutFromLibrary", method=RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Slide layout retrieved from file successfully"), 
			@ApiResponse(code=400, message="Invalid request, file is not valid or cannot be found"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to retrieve slide layouts"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public List<SlideLayout> getSlideLayoutsFromLibrary(
			@ApiParam(required=true, value="uploaded file with slide layouts")
			@RequestParam("file") String uploadedFileName) {
		List<SlideLayout> layouts = new ArrayList<SlideLayout>();
		if (uploadedFileName != null) {
		    uploadedFileName = moveToTempFile (uploadedFileName.trim());
			File libraryFile = new File(uploadDir, uploadedFileName);
			if (libraryFile.exists()) {
				try {
					FileInputStream inputStream2 = new FileInputStream(libraryFile);
			        InputStreamReader reader2 = new InputStreamReader(inputStream2, "UTF-8");
			        List<Class> contextList = new ArrayList<Class>(Arrays.asList(FilterUtils.filterClassContext));
		    		contextList.add(ArrayDesignLibrary.class);
			        JAXBContext context2 = JAXBContext.newInstance(contextList.toArray(new Class[contextList.size()]));
			        Unmarshaller unmarshaller2 = context2.createUnmarshaller();
			        ArrayDesignLibrary library = (ArrayDesignLibrary) unmarshaller2.unmarshal(reader2);
			        List<org.grits.toolbox.glycanarray.library.om.layout.SlideLayout> layoutList = 
			        		library.getLayoutLibrary().getSlideLayout();
			        for (org.grits.toolbox.glycanarray.library.om.layout.SlideLayout slideLayout : layoutList) {
		        		SlideLayout mySlideLayout = new SlideLayout();
		        		mySlideLayout.setName(slideLayout.getName());
		        		mySlideLayout.setDescription(slideLayout.getDescription());
		        		mySlideLayout.setId(slideLayout.getId().toString());
		        		int width = 0;
		        		int height = 0;
		        		for (Block block: slideLayout.getBlock()) {
		        			org.glygen.array.persistence.rdf.Block myBlock = new org.glygen.array.persistence.rdf.Block();
		        			myBlock.setColumn(block.getColumn());
		        			myBlock.setRow(block.getRow());
		        			if (block.getColumn() > width)
		        				width = block.getColumn();
		        			if (block.getRow() > height)
		        				height = block.getRow();
		        		}
		        		
		        		mySlideLayout.setHeight(slideLayout.getHeight() == null ? height: slideLayout.getHeight());
		        		mySlideLayout.setWidth(slideLayout.getWidth() == null ? width: slideLayout.getWidth());
		        		layouts.add(mySlideLayout);
					}
			        return layouts;
				} catch (JAXBException e) {
					ErrorMessage errorMessage = new ErrorMessage();
					errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
					errorMessage.addError(new ObjectError("file", "NotValid"));
					throw new IllegalArgumentException("Not a valid array library!", errorMessage);
				} catch (IOException e) {
					ErrorMessage errorMessage = new ErrorMessage();
					errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
					errorMessage.addError(new ObjectError("file", "NotValid"));
					throw new IllegalArgumentException("File cannot be found!", errorMessage);
				}
			}
		}
		
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		errorMessage.addError(new ObjectError("file", "NotValid"));
		throw new IllegalArgumentException("File is not valid", errorMessage);
	}
	
	
	List<org.glygen.array.persistence.rdf.Spot> getSpotsFromBlockLayout (ArrayDesignLibrary library, org.grits.toolbox.glycanarray.library.om.layout.BlockLayout blockLayout, SpotMetadata spotMetadata, FeatureMetadata featureMetadata) {
		List<org.glygen.array.persistence.rdf.Spot> spots = new ArrayList<>();
		ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        Source glycanSource = new Source();
        glycanSource.setNotRecorded(true);
    	for (Spot spot: blockLayout.getSpot()) {
    		org.glygen.array.persistence.rdf.Spot s = new org.glygen.array.persistence.rdf.Spot();
    		s.setRow(spot.getY());
    		s.setColumn(spot.getX());
    		s.setGroup(spot.getGroup()+"");
    		//s.setConcentration(spot.getConcentration());
    		Feature feature = LibraryInterface.getFeature(library, spot.getFeatureId());
    		List<org.glygen.array.persistence.rdf.Feature> features = new ArrayList<>();
    		Map<org.glygen.array.persistence.rdf.Feature, Double> ratioMap = new HashMap<>();
    		Map<org.glygen.array.persistence.rdf.Feature, LevelUnit> concentrationMap = new HashMap<>();
    		if (feature != null) {
        		for (Ratio r : feature.getRatio()) {
        			GlycanProbe probe = null;
        			for (GlycanProbe p : library.getFeatureLibrary().getGlycanProbe()) {
        				if (p.getId().equals(r.getItemId())) {
        					probe = p;
        					break;
        				}
        			}
        			if (probe != null) {
        				for (Ratio r1 : probe.getRatio()) {
        					LinkedGlycan myFeature = new LinkedGlycan();
        					myFeature.setGlycans(new ArrayList<GlycanInFeature>());
        					myFeature.setMetadata(featureMetadata);
        					org.grits.toolbox.glycanarray.library.om.feature.Glycan glycan = LibraryInterface.getGlycan(library, r1.getItemId());
        					if (glycan != null) {
		        				Glycan myGlycan = null;
		        				GlycanInFeature glycanFeature = new GlycanInFeature();
		        				//TODO check probe metadata to see if source information is available
                                glycanFeature.setSource(glycanSource);
		        				if (glycan.getSequence() != null) {
		        					myGlycan = new SequenceDefinedGlycan();
		        					((SequenceDefinedGlycan) myGlycan).setSequence(glycan.getSequence().trim());  
		        					((SequenceDefinedGlycan) myGlycan).setGlytoucanId(glycan.getGlyTouCanId());
		        					((SequenceDefinedGlycan) myGlycan).setSequenceType(GlycanSequenceFormat.GLYCOCT);
		        					//determine the reducing end type
		        					ReducingEndConfiguration redEnd = new ReducingEndConfiguration();
	                                redEnd.setType(getReducingEnd (glycan.getSequence().trim()));
	                                glycanFeature.setReducingEndConfiguration(redEnd);
		        				} else {
		        					myGlycan = new UnknownGlycan();
		        				}
		        				glycanFeature.setGlycan(myGlycan);
		        				myGlycan.setName(glycan.getName());
		        				myGlycan.setDescription(glycan.getComment());
		        				myGlycan.setInternalId(glycan.getId() == null ? "" : glycan.getId().toString());
		        				myFeature.getGlycans().add(glycanFeature);
        					} else {
        					    // should have been there
                                errorMessage.addError(new ObjectError("glycan:" + r1.getItemId(), "NotFound"));
        					}
		        			org.grits.toolbox.glycanarray.library.om.feature.Linker linker = LibraryInterface.getLinker(library, probe.getLinker());
		        			if (linker != null) {
		        				Linker myLinker = new SmallMoleculeLinker();
		        				if (linker.getPubChemId() != null) {
		        					((SmallMoleculeLinker) myLinker).setPubChemId(linker.getPubChemId().longValue());
		        					myLinker.setType(LinkerType.SMALLMOLECULE);
		        				} else {
		        					// create unknown linker
		        					myLinker.setType(LinkerType.UNKNOWN_SMALLMOLECULE);
		        				}
		        				myLinker.setName(linker.getName());
		        				myLinker.setDescription(linker.getComment());
		        				myFeature.setLinker(myLinker);
		        			}
		        			else {
		        			    // should have been there
                                errorMessage.addError(new ObjectError("linker:" + probe.getLinker(), "NotFound"));
                            }
		        			myFeature.setName (feature.getName());
		        			myFeature.setInternalId(feature.getName());
		        			myFeature.setMetadata(featureMetadata);
		        			ratioMap.put(myFeature, r1.getItemRatio());
		        			//TODO get the concentration from probe once the library model is updated
		        			concentrationMap.put(myFeature, spot.getConcentration());
		        			features.add(myFeature);
        				}
        			} else {
        			    // should have been there
        			    errorMessage.addError(new ObjectError("probe:" + r.getItemId(), "NotFound"));
        			}
        		}
    		}
    		s.setFeatureRatioMap(ratioMap);
    		s.setFeatureConcentrationMap(concentrationMap);
    		s.setFeatures(features);
    		s.setMetadata(spotMetadata);
    		spots.add(s);
    	}
    	
    	if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty())
    	    throw new IllegalArgumentException("Not a valid array library!", errorMessage);
    	return spots;
	}
	
	@ApiOperation(value = "List all block layouts for the user", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/listBlocklayouts", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Block layouts retrieved successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error for arguments", response = ErrorMessage.class),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list block layouts"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
	public BlockLayoutResultView listBlockLayouts (
			@ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
			@RequestParam("offset") Integer offset,
			@ApiParam(required=false, value="limit of the number of layouts to be retrieved", example="10") 
			@RequestParam(value="limit", required=false) Integer limit, 
			@ApiParam(required=false, value="name of the sort field, defaults to id") 
			@RequestParam(value="sortBy", required=false) String field, 
			@ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
			@RequestParam(value="order", required=false) Integer order, 
			@ApiParam (required=false, defaultValue = "true", value="if false, do not load spot details. Default is true (to load all)")
			@RequestParam(required=false, defaultValue = "true", value="loadAll") Boolean loadAll, 
			@ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue,
			Principal p) {
		BlockLayoutResultView result = new BlockLayoutResultView();
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		try {
			if (offset == null)
				offset = 0;
			if (limit == null)
				limit = -1;
			if (field == null)
				field = "id";
			if (order == null)
				order = 0; // DESC
			
			if (order != 0 && order != 1) {
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
				errorMessage.addError(new ObjectError("order", "NotValid"));
				errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
				throw new IllegalArgumentException("Order should be 0 or 1", errorMessage);
			}
			
			int total = layoutRepository.getBlockLayoutCountByUser (user, searchValue);
			List<BlockLayout> layouts = layoutRepository.getBlockLayoutByUser(user, offset, limit, field, loadAll, order, searchValue);
			result.setRows(layouts);
			result.setTotal(total);
			result.setFilteredTotal(layouts.size());
			
			if (loadAll) {
    			// populate glycan images
    			for (BlockLayout b: layouts) {
    			    if (b.getSpots() == null)
    			        continue;
    			    for (org.glygen.array.persistence.rdf.Spot s: b.getSpots()) {
    			        if (s.getFeatures() == null) 
    			            continue;
    			        for (org.glygen.array.persistence.rdf.Feature f: s.getFeatures()) {
    			            populateFeatureGlycanImages(f, imageLocation);
    			        }
    			    }
    			}
			}
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot retrieve block layouts for user. Reason: " + e.getMessage());
		}
		
		return result;
	}

	@ApiOperation(value = "List all features for the user", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/listFeatures", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Features retrieved successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error for arguments"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list features"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public FeatureListResultView listFeature (
			@ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
			@RequestParam("offset") Integer offset,
			@ApiParam(required=false, value="limit of the number of features to be retrieved", example="10") 
			@RequestParam(value="limit", required=false) Integer limit, 
			@ApiParam(required=false, value="name of the sort field, defaults to id") 
			@RequestParam(value="sortBy", required=false) String field, 
			@ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
			@RequestParam(value="order", required=false) Integer order, 
			@ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue,
			Principal p) {
		FeatureListResultView result = new FeatureListResultView();
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		try {
			if (offset == null)
				offset = 0;
			if (limit == null)
				limit = -1;
			if (field == null)
				field = "id";
			if (order == null)
				order = 0; // DESC
			
			if (order != 0 && order != 1) {
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
				errorMessage.addError(new ObjectError("order", "NotValid"));
				errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
				throw new IllegalArgumentException("Order should be 0 or 1", errorMessage);
			}
			
			int total = featureRepository.getFeatureCountByUser (user, searchValue);
			
			List<org.glygen.array.persistence.rdf.Feature> features = featureRepository.getFeatureByUser(user, offset, limit, field, order, searchValue);
			result.setRows(features);
			result.setTotal(total);
			result.setFilteredTotal(features.size());
			
			// get cartoons for the glycans
			for (org.glygen.array.persistence.rdf.Feature f: features) {
			    populateFeatureGlycanImages(f, imageLocation);
			}
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot retrieve features for user. Reason: " + e.getMessage());
		}
		
		return result;
	}
	
	@ApiOperation(value = "List all features of given type for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/listFeaturesByType", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Features retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to list features"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public FeatureListResultView listFeaturesByType (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of features to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue,
            @ApiParam(required=true, value="type of the molecule", 
            allowableValues="LINKEDGLYCAN, GLYCOLIPID, GLYCOPEPTIDE, "
                    + "GLYCOPROTEIN, GPLINKEDGLYCOPEPTIDE, CONTROL, NEGATIVE_CONTROL, COMPOUND, LANDING_LIGHT") 
            @RequestParam("type") String type,
            Principal p) {
        FeatureListResultView result = new FeatureListResultView();
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        try {
            if (offset == null)
                offset = 0;
            if (limit == null)
                limit = -1;
            if (field == null)
                field = "id";
            if (order == null)
                order = 0; // DESC
            
            if (order != 0 && order != 1) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("order", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Order should be 0 or 1", errorMessage);
            }
            
            FeatureType featureType = FeatureType.valueOf(type);
            if (featureType == null) {
                ErrorMessage errorMessage = new ErrorMessage("Incorrect feature type");
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("type", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Incorrect feature type", errorMessage);
            }
            
            int total = featureRepository.getFeatureCountByUserByType (user, featureType, searchValue);
            
            List<org.glygen.array.persistence.rdf.Feature> features = featureRepository.getFeatureByUser(user, offset, limit, field, order, searchValue, featureType);
            result.setRows(features);
            result.setTotal(total);
            result.setFilteredTotal(features.size());
            
            // get cartoons for the glycans
            for (org.glygen.array.persistence.rdf.Feature f: features) {
                populateFeatureGlycanImages(f, imageLocation);
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve features for user. Reason: " + e.getMessage());
        }
        
        return result;
    }

	@ApiOperation(value = "List all glycans for the user", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/listGlycans", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Glycans retrieved successfully", response = GlycanListResultView.class), 
			@ApiResponse(code=400, message="Invalid request, validation error for arguments"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list glycans"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
	public GlycanListResultView listGlycans (
			@ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
			@RequestParam("offset") Integer offset,
			@ApiParam(required=false, value="limit of the number of glycans to be retrieved", example="10") 
			@RequestParam(value="limit", required=false) Integer limit, 
			@ApiParam(required=false, value="name of the sort field, defaults to id") 
			@RequestParam(value="sortBy", required=false) String field, 
			@ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
			@RequestParam(value="order", required=false) Integer order, 
			@ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, Principal p) {
		GlycanListResultView result = new GlycanListResultView();
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		try {
			if (offset == null)
				offset = 0;
			if (limit == null)
				limit = -1;
			if (field == null)
				field = "id";
			if (order == null)
				order = 0; // DESC
			
			if (order != 0 && order != 1) {
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
				errorMessage.addError(new ObjectError("order", "NotValid"));
				errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
				throw new IllegalArgumentException("Order should be 0 or 1", errorMessage);
			}
			
			int total = glycanRepository.getGlycanCountByUser (user, searchValue);
			
			List<Glycan> glycans = glycanRepository.getGlycanByUser(user, offset, limit, field, order, searchValue);
			for (Glycan glycan : glycans) {
			    if (glycan.getType().equals(GlycanType.SEQUENCE_DEFINED)) {
			        byte[] image = getCartoonForGlycan(glycan.getId());
			        if (image == null && ((SequenceDefinedGlycan) glycan).getSequence() != null) {
                        BufferedImage t_image = createImageForGlycan ((SequenceDefinedGlycan) glycan);
                        if (t_image != null) {
                            String filename = glycan.getId() + ".png";
                            //save the image into a file
                            logger.debug("Adding image to " + imageLocation);
                            File imageFile = new File(imageLocation + File.separator + filename);
                            try {
                                ImageIO.write(t_image, "png", imageFile);
                            } catch (IOException e) {
                                logger.error ("Glycan image cannot be written", e);
                            }
                        }
                        image = getCartoonForGlycan(glycan.getId());
                    }
			        glycan.setCartoon(image);
			    }
			}
			
			result.setRows(glycans);
			result.setTotal(total);
			result.setFilteredTotal(glycans.size());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot retrieve glycans for user. Reason: " + e.getMessage());
		}
		
		return result;
	}
	
	

    @ApiOperation(value = "List all glycans for the user and the public ones", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/listAllGlycans", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Glycans retrieved successfully", response = GlycanListResultView.class), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to list glycans"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public GlycanListResultView listAllGlycans (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of glycans to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, Principal p) {
        GlycanListResultView result = new GlycanListResultView();
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        try {
            if (offset == null)
                offset = 0;
            if (limit == null)
                limit = -1;
            if (field == null)
                field = "id";
            if (order == null)
                order = 0; // DESC
            
            if (order != 0 && order != 1) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("order", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Order should be 0 or 1", errorMessage);
            }
            
            int total = glycanRepository.getGlycanCountByUser(user, searchValue, true);
            List<Glycan> glycans = glycanRepository.getGlycanByUser(user, offset, limit, field, order, searchValue, true);
            List<Glycan> totalResultList = new ArrayList<>();
            totalResultList.addAll(glycans);
            
            /*int totalPublic = glycanRepository.getGlycanCountByUser (null, searchValue);
            
            List<Glycan> publicResultList = glycanRepository.getGlycanByUser(null, offset, limit, field, order, searchValue);
            for (Glycan g1: publicResultList) {
                boolean duplicate = false;
                for (Glycan g2: glycans) {
                    if (g1.getName().equals(g2.getName())) {
                        duplicate = true;
                        totalPublic --;
                    }
                }
                if (!duplicate) {
                    totalResultList.add(g1);
                } 
            }*/
            
            for (Glycan glycan : totalResultList) {
                if (glycan.getType().equals(GlycanType.SEQUENCE_DEFINED)) {
                    byte[] image = getCartoonForGlycan(glycan.getId());
                    if (image == null && ((SequenceDefinedGlycan) glycan).getSequence() != null) {
                        // try to create one
                        BufferedImage t_image = createImageForGlycan ((SequenceDefinedGlycan) glycan);
                        if (t_image != null) {
                            String filename = glycan.getId() + ".png";
                            //save the image into a file
                            logger.debug("Adding image to " + imageLocation);
                            File imageFile = new File(imageLocation + File.separator + filename);
                            try {
                                ImageIO.write(t_image, "png", imageFile);
                            } catch (IOException e) {
                                logger.error ("Glycan image cannot be written", e);
                            }
                        }
                        image = getCartoonForGlycan(glycan.getId());
                    }
                    glycan.setCartoon(image);
                    //glycan.setCartoon(getCartoonForGlycan(glycan.getId(), ((SequenceDefinedGlycan) glycan).getSequence()));
                }
            }
            
            result.setRows(totalResultList);
            //result.setTotal(total+totalPublic);
            result.setTotal(total);
            result.setFilteredTotal(totalResultList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve glycans. Reason: " + e.getMessage());
        }
        
        return result;
    }
	
	@ApiOperation(value = "List all linkers for the user and the public ones", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/listAllLinkers", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Linkers retrieved successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error for arguments"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list linkers"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
	public LinkerListResultView listAllLinkers (
			@ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
			@RequestParam("offset") Integer offset,
			@ApiParam(required=false, value="limit of the number of linkers to be retrieved", example="10") 
			@RequestParam(value="limit", required=false) Integer limit, 
			@ApiParam(required=false, value="name of the sort field, defaults to id") 
			@RequestParam(value="sortBy", required=false) String field, 
			@ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
			@RequestParam(value="order", required=false) Integer order, 
			@ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, Principal p) {
		LinkerListResultView result = new LinkerListResultView();
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		try {
			if (offset == null)
				offset = 0;
			if (limit == null)
				limit = -1;
			if (field == null)
				field = "id";
			if (order == null)
				order = 0; // DESC
			
			if (order != 0 && order != 1) {
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
				errorMessage.addError(new ObjectError("order", "NotValid"));
				errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
				throw new IllegalArgumentException("Order should be 0 or 1", errorMessage);
			}
			
			int total = linkerRepository.getLinkerCountByUser (user, searchValue, true);
			List<Linker> linkers = linkerRepository.getLinkerByUser(user, offset, limit, field, order, searchValue, null, true);
			result.setRows(linkers);
			result.setTotal(total);
			result.setFilteredTotal(linkers.size());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot retrieve linkers for user. Reason: " + e.getMessage());
		}
		
		return result;
	}
	
	@ApiOperation(value = "List all linkers for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/listLinkers", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Linkers retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to list linkers"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public LinkerListResultView listLinkers (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of linkers to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, Principal p) {
        LinkerListResultView result = new LinkerListResultView();
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        try {
            if (offset == null)
                offset = 0;
            if (limit == null)
                limit = -1;
            if (field == null)
                field = "id";
            if (order == null)
                order = 0; // DESC
            
            if (order != 0 && order != 1) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("order", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Order should be 0 or 1", errorMessage);
            }
            
            int total = linkerRepository.getLinkerCountByUser (user, searchValue);
            
            List<Linker> linkers = linkerRepository.getLinkerByUser(user, offset, limit, field, order, searchValue);
            result.setRows(linkers);
            result.setTotal(total);
            result.setFilteredTotal(linkers.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve linkers for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
	
	@ApiOperation(value = "List all linkers of the given type for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/listMoleculesByType", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Linkers retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to list linkers"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public LinkerListResultView listLinkersByType (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of linkers to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @ApiParam(required=true, value="type of the molecule", allowableValues="SMALLMOLECULE, LIPID, PEPTIDE, PROTEIN, OTHER") 
            @RequestParam("type") String moleculeType,
            Principal p) {
        LinkerListResultView result = new LinkerListResultView();
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        try {
            if (offset == null)
                offset = 0;
            if (limit == null)
                limit = -1;
            if (field == null)
                field = "id";
            if (order == null)
                order = 0; // DESC
            
            if (order != 0 && order != 1) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("order", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Order should be 0 or 1", errorMessage);
            }
            
            LinkerType linkerType = LinkerType.valueOf(moleculeType);
            if (linkerType == null) {
                ErrorMessage errorMessage = new ErrorMessage("Incorrect molecule type");
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("moleculeType", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Incorrect molecule type", errorMessage);
            }
            
            int total = linkerRepository.getLinkerCountByUserByType (user, linkerType, searchValue);
            
            List<Linker> linkers = linkerRepository.getLinkerByUser(user, offset, limit, field, order, searchValue, linkerType);
            
            result.setRows(linkers);
            result.setTotal(total);
            result.setFilteredTotal(linkers.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve linkers for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
	
	@ApiOperation(value = "List all linkers of the given type for the user and the public ones", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/listAllMoleculesByType", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Linkers retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to list linkers"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public LinkerListResultView listAllLinkersByType (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of linkers to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @ApiParam(required=true, value="type of the molecule", allowableValues="SMALLMOLECULE, LIPID, PEPTIDE, PROTEIN, OTHER") 
            @RequestParam("type") String moleculeType,
            Principal p) {
        LinkerListResultView result = new LinkerListResultView();
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        try {
            if (offset == null)
                offset = 0;
            if (limit == null)
                limit = -1;
            if (field == null)
                field = "id";
            if (order == null)
                order = 0; // DESC
            
            if (order != 0 && order != 1) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("order", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Order should be 0 or 1", errorMessage);
            }
            
            LinkerType linkerType = LinkerType.valueOf(moleculeType);
            if (linkerType == null) {
                ErrorMessage errorMessage = new ErrorMessage("Incorrect molecule type");
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("moleculeType", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Incorrect molecule type", errorMessage);
            }
            
            int total = linkerRepository.getLinkerCountByUserByType (user, linkerType, searchValue, true);
            List<Linker> linkers = linkerRepository.getLinkerByUser(user, offset, limit, field, order, searchValue, linkerType, true);
            result.setRows(linkers);
            result.setTotal(total);
            result.setFilteredTotal(linkers.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve linkers for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
	
	@ApiOperation(value = "List all slide layouts for the user", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value="/listSlidelayouts", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Slide layouts retrieved successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error for arguments"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list slide layouts"),
			@ApiResponse(code=415, message="Media type is not supported"),
			@ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
	public SlideLayoutResultView listSlideLayouts (
			@ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
			@RequestParam("offset") Integer offset,
			@ApiParam(required=false, value="limit of the number of layouts to be retrieved", example="10") 
			@RequestParam(value="limit", required=false) Integer limit, 
			@ApiParam(required=false, value="name of the sort field, defaults to id") 
			@RequestParam(value="sortBy", required=false) String field, 
			@ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
			@RequestParam(value="order", required=false) Integer order, 
			@ApiParam (required=false, defaultValue = "true", value="if false, do not load block details. Default is true (to load all)")
			@RequestParam(required=false, defaultValue = "true", value="loadAll") Boolean loadAll, 
			@ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue,
			Principal p) {
		SlideLayoutResultView result = new SlideLayoutResultView();
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		try {
			if (offset == null)
				offset = 0;
			if (limit == null)
				limit = -1;
			if (field == null)
				field = "id";
			if (order == null)
				order = 0; // DESC
			
			if (order != 0 && order != 1) {
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
				errorMessage.addError(new ObjectError("order", "NotValid"));
				errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
				throw new IllegalArgumentException("Order should be 0 or 1", errorMessage);
			}
			
			int total = layoutRepository.getSlideLayoutCountByUser (user, searchValue);
			List<SlideLayout> layouts = layoutRepository.getSlideLayoutByUser(user, offset, limit, field, loadAll, order, searchValue);
			result.setRows(layouts);
			result.setTotal(total);
			result.setFilteredTotal(layouts.size());
			
			if (loadAll) {
    			// populate glycan images
    			for (SlideLayout layout: layouts) {
    			    if (layout.getBlocks() != null) {
    			        for (org.glygen.array.persistence.rdf.Block block: layout.getBlocks()) {
    			            if (block.getBlockLayout() != null) {
    			                if (block.getBlockLayout().getSpots() == null)
                                    continue;
                                for (org.glygen.array.persistence.rdf.Spot s: block.getBlockLayout().getSpots()) {
                                    if (s.getFeatures() == null)
                                        continue;
                                    for (org.glygen.array.persistence.rdf.Feature f: s.getFeatures()) {
                                        populateFeatureGlycanImages(f, imageLocation);
                                    }
                                }
    			            }
    			        }
    			    }
                }
			}
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot retrieve slide layouts for user. Reason: " + e.getMessage());
		}
		
		return result;
	}
	
	@ApiOperation(value = "Make given glycan public", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/makeglycanpublic/{glycanId}", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="id of the public glycan"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to modify glycans"),
            @ApiResponse(code=409, message="A glycan with the given name already exists in public repository!"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String makeGlycanPublic (
            @ApiParam(required=true, value="id of the glycan to retrieve") 
            @PathVariable("glycanId") String glycanId, Principal p) {
	    UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        try {
            Glycan glycan = glycanRepository.getGlycanById(glycanId.trim(), user);
            if (glycan == null) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("glycanId", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("There is no glycan with the given id in user's repository", errorMessage);
            }
            
            BufferedImage t_image = null;
            if (glycan.getType() == GlycanType.SEQUENCE_DEFINED) {
                t_image = createImageForGlycan((SequenceDefinedGlycan) glycan);
            }
            String glycanURI = glycanRepository.makePublic (glycan, user);
            if (glycanURI == null) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("name", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Cannot have glycans with the same name", errorMessage);
            }
            String id = glycanURI.substring(glycanURI.lastIndexOf("/")+1);
            if (t_image != null) {
                String filename = id + ".png";
                //save the image into a file
                logger.debug("Adding image to " + imageLocation);
                File imageFile = new File(imageLocation + File.separator + filename);
                ImageIO.write(t_image, "png", imageFile);
            }
            return id;
        } catch (SparqlException e) {
            throw new GlycanRepositoryException("Glycan cannot be made public for user " + p.getName(), e);
        } catch (SQLException e) {
            throw new GlycanRepositoryException("Glycan cannot be made public for user " + p.getName(), e);
        } catch (IOException e) {
            logger.error("Glycan image cannot be generated", e);
            throw new GlycanRepositoryException("Glycan image cannot be generated", e);
        }
	}
	
	@ApiOperation(value = "Make given linker public", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/makelinkerpublic/{linkerId}", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="id of the public linker"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to modify linker"),
            @ApiResponse(code=409, message="A linker with the given name already exists in public repository!"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String makeLinkerPublic (
            @ApiParam(required=true, value="id of the linker to retrieve") 
            @PathVariable("linkerId") String linkerId, Principal p) {
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        try {
            Linker linker = linkerRepository.getLinkerById(linkerId.trim(), user);
            if (linker == null) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("linkerId", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("There is no linker with the given id in user's repository", errorMessage); 
            }
            String linkerURI = linkerRepository.makePublic (linker, user);
            if (linkerURI == null) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("name", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Cannot have linkers with the same name", errorMessage);
            }
            return linkerURI.substring(linkerURI.lastIndexOf("/")+1);
        } catch (SparqlException e) {
            throw new GlycanRepositoryException("Linker cannot be made public for user " + p.getName(), e);
        } catch (SQLException e) {
            throw new GlycanRepositoryException("Linker cannot be made public for user " + p.getName(), e);
        }
    }
	
	@ApiOperation(value = "Make given slide layout public", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/makeslidelayoutpublic/{layoutId}", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="id of the public slide layout"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to modify slide layout"),
            @ApiResponse(code=409, message="A slide layout with the given name already exists in public repository!"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String makeSlideLayoutPublic (
            @ApiParam(required=true, value="id of the slide layout to retrieve") 
            @PathVariable("layoutId") String layoutId, Principal p) {
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        try {
            SlideLayout layout = layoutRepository.getSlideLayoutById(layoutId.trim(), user);
            if (layout == null) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("linkerId", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("There is no slide layout with the given id in user's repository", errorMessage); 
            }
            if (layout.getIsPublic()) {
                // already been made public
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("layoutId", "This slide layout is already public"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("This slide layout is already public!", errorMessage); 
            }
            String layoutURI = layoutRepository.makePublic (layout, user, new HashMap<String, String>()); 
            return layoutURI.substring(layoutURI.lastIndexOf("/")+1);
        } catch (GlycanExistsException e) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("name", "NotValid"));
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            throw new IllegalArgumentException("Cannot have slide layouts with the same name", errorMessage);
        } catch (SparqlException e) {
            throw new GlycanRepositoryException("Slide Layout cannot be made public for user " + p.getName(), e);
        } catch (SQLException e) {
            throw new GlycanRepositoryException("Slide Layout cannot be made public for user " + p.getName(), e);
        }
    }
	
	@ApiOperation(value = "Export glycans into a file", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/exportglycans", method=RequestMethod.GET, 
        produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="confirmation message"), 
            @ApiResponse(code=400, message="Invalid request, file not found, not writable etc."),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to export glycans"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
	public @ResponseBody String exportGlycans (Principal p) {
	    
	    UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
	    try {
            List<Glycan> myGlycans = glycanRepository.getGlycanByUser(user, 0, -1, null, 0, null);
            ObjectMapper mapper = new ObjectMapper();         
            String json = mapper.writeValueAsString(myGlycans);
            return json;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Glycans cannot be retrieved for user " + p.getName(), e);
        } catch (JsonProcessingException e) {
            ErrorMessage errorMessage = new ErrorMessage("Cannot generate the glycan list");
            errorMessage.setStatus(HttpStatus.NOT_FOUND.value());
            errorMessage.addError(new ObjectError("file", "NotValid"));
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            throw new IllegalArgumentException("Cannot generate the glycan list", errorMessage);
        }
	}
	
	@ApiOperation(value = "Export linkers into a file", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/exportlinkers", method=RequestMethod.GET, 
        produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="confirmation message"), 
            @ApiResponse(code=400, message="Invalid request, file not found, not writable etc."),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to export linkers"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public @ResponseBody String exportLinkers (
            @ApiParam(required=true, value="type of the molecule", allowableValues="SMALLMOLECULE, LIPID, PEPTIDE, PROTEIN, OTHER") 
            @RequestParam("type") String moleculeType,
            Principal p) {
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        try {
            
            LinkerType linkerType = LinkerType.valueOf(moleculeType);
            if (linkerType == null) {
                ErrorMessage errorMessage = new ErrorMessage("Incorrect molecule type");
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("moleculeType", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Incorrect molecule type", errorMessage);
            }
            
            List<Linker> myLinkers = linkerRepository.getLinkerByUser(user, 0, -1, null, 0, null, linkerType);
            ObjectMapper mapper = new ObjectMapper();         
            String json = mapper.writeValueAsString(myLinkers);
            return json;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Linkers cannot be retrieved for user " + p.getName(), e);
        } catch (JsonProcessingException e) {
            ErrorMessage errorMessage = new ErrorMessage("Cannot generate the linker list");
            errorMessage.setStatus(HttpStatus.NOT_FOUND.value());
            errorMessage.addError(new ObjectError("file", "NotValid"));
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            throw new IllegalArgumentException("Cannot generate the linker list", errorMessage);
        }
    }
	
	@ApiOperation(value = "Export features into a file", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/exportfeatures", method=RequestMethod.GET, 
        produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="confirmation message"), 
            @ApiResponse(code=400, message="Invalid request, file not found, not writable etc."),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to export features"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public @ResponseBody String exportFeatures (
            @ApiParam(required=true, value="type of the feature", 
            allowableValues="LINKEDGLYCAN, GLYCOLIPID, GLYCOPEPTIDE, "
                    + "GLYCOPROTEIN, GPLINKEDGLYCOPEPTIDE, CONTROL, NEGATIVE_CONTROL, COMPOUND, LANDING_LIGHT") 
            @RequestParam("type") String type,
            Principal p) {
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        try {
            
            FeatureType featureType = FeatureType.valueOf(type);
            if (featureType == null) {
                ErrorMessage errorMessage = new ErrorMessage("Incorrect feature type");
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("type", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Incorrect feature type", errorMessage);
            }
            
            List<org.glygen.array.persistence.rdf.Feature> myFeatures = featureRepository.getFeatureByUser(user, 0, -1, null, 0, null, featureType, false);
            ObjectMapper mapper = new ObjectMapper();         
            String json = mapper.writeValueAsString(myFeatures);
            return json;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Features cannot be retrieved for user " + p.getName(), e);
        } catch (JsonProcessingException e) {
            ErrorMessage errorMessage = new ErrorMessage("Cannot generate the feature list");
            errorMessage.setStatus(HttpStatus.NOT_FOUND.value());
            errorMessage.addError(new ObjectError("file", "NotValid"));
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            throw new IllegalArgumentException("Cannot generate the feature list", errorMessage);
        }
    }
	
	@ApiOperation(value = "Export slide layout in extended GAL format", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/downloadSlideLayout", method=RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(code=200, message="File generated successfully"), 
            @ApiResponse(code=400, message="Invalid request, file cannot be found"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to retrieve slide layout"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public ResponseEntity<Resource> exportSlideLayout (
            @ApiParam(required=true, value="id of the slide layout") 
            @RequestParam("slidelayoutid")
            String slidelayoutid,
            @ApiParam(required=false, value="the name for downloaded file") 
            @RequestParam("filename")
            String fileName,        
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        
        String uri = GlygenArrayRepositoryImpl.uriPrefix + slidelayoutid;
        if (fileName == null || fileName.isEmpty()) {
            fileName = slidelayoutid + ".gal";
        }
        File newFile = new File (uploadDir, "tmp" + fileName);
        
        try {
            SlideLayout layout = layoutRepository.getSlideLayoutFromURI(uri, true, user);
            if (layout == null) {
                // check if it is public
                layout = layoutRepository.getSlideLayoutFromURI(uri, true, null);
                if (layout == null) {
                    errorMessage.addError(new ObjectError("slidelayoutid", "NotFound"));
                }
            }
            if (layout != null) {
                try {
                    galFileParser.exportToFile(layout, newFile.getAbsolutePath());
                    
                } catch (IOException e) {
                    errorMessage.addError(new ObjectError("file", "NotFound"));
                }
            }
            
            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return DatasetController.download (newFile, fileName);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve dataset from the repository", e);
        }
    }
	
	@ApiOperation(value = "Check status for upload file", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value = "/upload", method=RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	public Confirmation resumeUpload (
			@RequestParam("resumableFilename") String resumableFilename,
			@RequestParam ("resumableRelativePath") String resumableRelativePath,
            @RequestParam ("resumableTotalChunks") String resumableTotalChunks,
            @RequestParam("resumableChunkSize") int resumableChunkSize,
            @RequestParam("resumableCurrentChunkSize") int resumableCurrentChunkSize,
            @RequestParam("resumableChunkNumber") int resumableChunkNumber,
            @RequestParam("resumableTotalSize") long resumableTotalSize,
            @RequestParam("resumableType") String resumableType,
            @RequestParam("resumableIdentifier") String resumableIdentifier) {

		
        ResumableFileInfo info = ResumableInfoStorage.getInstance().get(resumableIdentifier);
        if (info == null || !info.valid()) {
        	if (info != null) ResumableInfoStorage.getInstance().remove(info);
        	if (resumableChunkNumber != 1) {
        	    throw new IllegalArgumentException("file identifier is not valid");
        	} else {
        	    throw new UploadNotFinishedException("Not found");  // this will return HttpStatus no_content 204
        	}
        }
        if (info.uploadedChunks.contains(new ResumableFileInfo.ResumableChunkNumber(resumableChunkNumber))) {
        	return new Confirmation ("Upload", HttpStatus.OK.value()); //This Chunk has been Uploaded.
        } else {
            throw new UploadNotFinishedException("Not found");  // this will return HttpStatus no_content 204
        }
    }
	
	@ApiOperation(value = "Update given block layout for the user", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value = "/updateBlockLayout", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Block layout updated successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to update block layout"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation updateBlockLayout(
			@ApiParam(required=true, value="Block layout to be updated, id is required, name and comment can be updated only") 
			@RequestBody BlockLayout layout, Principal principal) throws SQLException {
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		// validate first
		if (validator != null) {
			if (layout.getName() == null || layout.getName().isEmpty()) 
				errorMessage.addError(new ObjectError("name", "NoEmpty"));
			if  (layout.getName() != null) {
				Set<ConstraintViolation<BlockLayout>> violations = validator.validateValue(BlockLayout.class, "name", layout.getName().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (layout.getDescription() != null) {
				Set<ConstraintViolation<BlockLayout>> violations = validator.validateValue(BlockLayout.class, "description", layout.getDescription().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("description", "LengthExceeded"));
				}		
			}
		
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			BlockLayout blockLayout= new BlockLayout();
			blockLayout.setUri(GlygenArrayRepository.uriPrefix + layout.getId());
			blockLayout.setDescription(layout.getDescription() != null ? layout.getDescription().trim() : layout.getDescription());
			blockLayout.setName(layout.getName() != null ? layout.getName().trim() : null);		
			
			BlockLayout local = null;
			// check if name is unique
			if (blockLayout.getName() != null && !blockLayout.getName().isEmpty()) {
				local = layoutRepository.getBlockLayoutByName(blockLayout.getName().trim(), user, false);
				if (local != null && !local.getUri().equals(blockLayout.getUri())) {   // there is another with the same name
					errorMessage.addError(new ObjectError("name", "Duplicate"));
				}
			} 
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid block layout information", errorMessage);
			
			layoutRepository.updateBlockLayout(blockLayout, user);
			return new Confirmation("Block Layout updated successfully", HttpStatus.OK.value());
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Error updating block layout with id: " + layout.getId());
		}
	}
	
	@ApiOperation(value = "Update given glycan for the user", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value = "/updateGlycan", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Glycan updated successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to update glycans"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation updateGlycan(
			@ApiParam(required=true, value="Glycan with updated fields") 
			@RequestBody Glycan glycanView, 
			@RequestParam(value="changeSummary", required=false)
            String changeSummary,
            @ApiParam(required=false, value="field that has changed, can provide multiple") 
            @RequestParam(value="changedField", required=false)
            List<String> changedFields,
			Principal principal) throws SQLException {
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		// validate first
		if (validator != null) {
			if  (glycanView.getName() != null) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "name", glycanView.getName().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (glycanView.getDescription() != null) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "description", glycanView.getDescription().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("description", "LengthExceeded"));
				}		
			}
			if (glycanView.getInternalId() != null && !glycanView.getInternalId().isEmpty()) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "internalId", glycanView.getInternalId().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("internalId", "LengthExceeded"));
				}		
			}
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			Glycan glycan= new Glycan();
			if (glycanView.getUri() != null && !glycanView.getUri().isEmpty()) {
			    glycan.setUri(glycanView.getUri());
			} else {
			    glycan.setUri(GlygenArrayRepository.uriPrefix + glycanView.getId());
			}
			glycan.setInternalId(glycanView.getInternalId() != null ? glycanView.getInternalId().trim(): glycanView.getInternalId());
			glycan.setDescription(glycanView.getDescription() != null ? glycanView.getDescription().trim() : glycanView.getDescription());
			glycan.setName(glycanView.getName() != null ? glycanView.getName().trim() : null);		
			
			Glycan local = null;
			// check if internalid and label are unique
			if (glycan.getInternalId() != null && !glycan.getInternalId().trim().isEmpty()) {
				local = glycanRepository.getGlycanByInternalId(glycan.getInternalId().trim(), user);
				if (local != null && !local.getUri().equals(glycan.getUri())) {   // there is another with the same internal id
					errorMessage.addError(new ObjectError("internalId", "Duplicate"));
				}
			}
			if (glycan.getName() != null && !glycan.getName().trim().isEmpty()) {
				local = glycanRepository.getGlycanByLabel(glycan.getName().trim(), user);
				if (local != null && !local.getUri().equals(glycan.getUri())) {   // there is another with the same name
					errorMessage.addError(new ObjectError("name", "Duplicate"));
				}
			} 
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
			
			ChangeLog changeLog = new ChangeLog();
            changeLog.setUser(principal.getName());
            changeLog.setChangeType(ChangeType.MINOR);
            changeLog.setDate(new Date());
            changeLog.setSummary(changeSummary);
            changeLog.setChangedFields(changedFields);
            glycan.addChange(changeLog);
			glycanRepository.updateGlycan(glycan, user);
			return new Confirmation("Glycan updated successfully", HttpStatus.OK.value());
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Error updating glycan with id: " + glycanView.getId());
		}
	}
	
	@ApiOperation(value = "Update given linker for the user", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value = "/updateLinker", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Linker updated successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to update linkers"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation updateLinker(
			@ApiParam(required=true, value="Linker to be updated, id is required, only name and comment can be updated") 
			@RequestBody Linker linkerView, 
			@ApiParam(required=false, value="summary of the changes") 
            @RequestParam(value="changeSummary", required=false)
            String changeSummary,
            @ApiParam(required=false, value="field that has changed, can provide multiple") 
            @RequestParam(value="changedField", required=false)
            List<String> changedFields,
            @RequestParam(value="unknown", required=false)
            @ApiParam(required=false, value="true, if the linker is of unknown type. The default is false")
            Boolean unknown,
			Principal principal) throws SQLException {
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		
		if (unknown == null) 
		    unknown = false;
		
		// validate first
		if (validator != null) {
			if  (linkerView.getName() != null) {
				Set<ConstraintViolation<Linker>> violations = validator.validateValue(Linker.class, "name", linkerView.getName().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (linkerView.getDescription() != null) {
				Set<ConstraintViolation<Linker>> violations = validator.validateValue(Linker.class, "description", linkerView.getDescription().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("description", "LengthExceeded"));
				}		
			}
		
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			
			if (linkerView.getType() == null) {
			    linkerView.setType(LinkerType.OTHER);
			}
			Linker linker = null;
			switch (linkerView.getType()) {
            case LIPID:
            case UNKNOWN_LIPID:
                linker= new Lipid();
                linker.setType(LinkerType.LIPID);
                break;
            case OTHER:
            case UNKNOWN_OTHER:
                linker= new OtherLinker();
                linker.setType(LinkerType.OTHER);
                break;
            case PEPTIDE:
            case UNKNOWN_PEPTIDE:
                linker = new PeptideLinker();
                linker.setType(LinkerType.PEPTIDE);
                break;
            case PROTEIN:
            case UNKNOWN_PROTEIN:
                linker = new ProteinLinker();
                linker.setType(LinkerType.PROTEIN);
                break;
            case SMALLMOLECULE:
            case UNKNOWN_SMALLMOLECULE:
                linker= new SmallMoleculeLinker();
                linker.setType(LinkerType.SMALLMOLECULE);
                break;
			}
			
			linker.setUri(GlygenArrayRepository.uriPrefix + linkerView.getId());
			//linker.setComment(linkerView.getComment() != null ? linkerView.getComment().trim() : linkerView.getComment());
			linker.setDescription(linkerView.getDescription() != null ? linkerView.getDescription().trim() : linkerView.getDescription());
			linker.setName(linkerView.getName() != null ? linkerView.getName().trim() : null);	
			
			LinkerType unknownType = LinkerType.valueOf("UNKNOWN_" + linker.getType().name());
			Linker local = null;
			// check if name is unique
			if (linker.getName() != null && !linker.getName().isEmpty()) {
				local = linkerRepository.getLinkerByLabel(linker.getName().trim(), linker.getType(), user);
				if (local != null && !local.getUri().equals(linker.getUri())) {   // there is another with the same name
				    if (local.getType() == linker.getType() || local.getType() == unknownType) {
				        errorMessage.addError(new ObjectError("name", "Duplicate"));
				    }
				}
			} 
			
            if (unknown) {
                linker.setType(unknownType);
            }
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage);
			
			ChangeLog changeLog = new ChangeLog();
            changeLog.setUser(principal.getName());
            changeLog.setChangeType(ChangeType.MINOR);
            changeLog.setDate(new Date());
            changeLog.setSummary(changeSummary);
            changeLog.setChangedFields(changedFields);
            linker.addChange(changeLog);
			linkerRepository.updateLinker(linker, user);
			return new Confirmation("Linker updated successfully", HttpStatus.OK.value());
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Error updating linker with id: " + linkerView.getId());
		}
	}
	
	
	@ApiOperation(value = "Update given feature for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/updateFeature", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Feature updated successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to update feature"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation updateFeature(
            @ApiParam(required=true, value="Feature to be updated, id and type are required, only name and internalId can be updated") 
            @RequestBody org.glygen.array.persistence.rdf.Feature feature, 
            @ApiParam(required=false, value="summary of the changes") 
            @RequestParam(value="changeSummary", required=false)
            String changeSummary,
            @ApiParam(required=false, value="field that has changed, can provide multiple") 
            @RequestParam(value="changedField", required=false)
            List<String> changedFields,
            Principal principal) throws SQLException {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        // validate first
        if (validator != null) {
            if  (feature.getName() != null) {
                Set<ConstraintViolation<Feature>> violations = validator.validateValue(Feature.class, "name", feature.getName().trim());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("name", "LengthExceeded"));
                }       
            }
            if (feature.getInternalId() != null) {
                Set<ConstraintViolation<Feature>> violations = validator.validateValue(Feature.class, "internalId", feature.getInternalId().trim());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("internalId", "LengthExceeded"));
                }       
            }
            if (feature.getDescription() != null) {
                Set<ConstraintViolation<Feature>> violations = validator.validateValue(Feature.class, "description", feature.getDescription().trim());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("description", "LengthExceeded"));
                }       
            }
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            
            if (feature.getType() == null) {
                feature.setType(FeatureType.CONTROL);
            }
            org.glygen.array.persistence.rdf.Feature newFeature = null;
            switch (feature.getType()) {
            case COMPOUND:
                newFeature = new CompoundFeature();
                break;
            case CONTROL:
                newFeature = new ControlFeature();
                break;
            case GLYCOLIPID:
                newFeature = new GlycoLipid();
                break;
            case GLYCOPEPTIDE:
                newFeature = new GlycoPeptide();
                break;
            case GLYCOPROTEIN:
                newFeature = new GlycoProtein();
                break;
            case GPLINKEDGLYCOPEPTIDE:
                newFeature = new GPLinkedGlycoPeptide();
                break;
            case LANDING_LIGHT:
                newFeature = new LandingLight();
                break;
            case LINKEDGLYCAN:
                newFeature = new LinkedGlycan();
                break;
            case NEGATIVE_CONTROL:
                newFeature = new NegControlFeature();
                break;
            }
            
            newFeature.setUri(GlygenArrayRepository.uriPrefix + feature.getId());
            newFeature.setInternalId(feature.getInternalId() != null ? feature.getInternalId().trim() : null);
            newFeature.setName(feature.getName() != null ? feature.getName().trim() : null);  
            newFeature.setDescription(feature.getDescription() != null ? feature.getDescription().trim() : null);
            
            // check if name is unique
            if (newFeature.getName() != null && !newFeature.getName().isEmpty()) {
                org.glygen.array.persistence.rdf.Feature local = featureRepository.getFeatureByLabel(newFeature.getName(), user);
                if (local != null && !local.getUri().equals(newFeature.getUri())) {   // there is another with the same name
                    errorMessage.addError(new ObjectError("name", "Duplicate"));
                }
            } 
            
            if (newFeature.getInternalId() != null && !newFeature.getInternalId().trim().isEmpty()) {
                try {
                    org.glygen.array.persistence.rdf.Feature existing = featureRepository.getFeatureByLabel(newFeature.getInternalId(), 
                            "gadr:has_internal_id", user);
                    if (existing != null && !existing.getUri().equals(newFeature.getUri())) {
                        feature.setId(existing.getId());
                        errorMessage.addError(new ObjectError("internalId", "Duplicate"));
                    }
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Could not query existing features", e);
                }
            }
            
            
            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
                throw new IllegalArgumentException("Invalid Input: Not a valid feature information", errorMessage);
            
            ChangeLog changeLog = new ChangeLog();
            changeLog.setUser(principal.getName());
            changeLog.setChangeType(ChangeType.MINOR);
            changeLog.setDate(new Date());
            changeLog.setSummary(changeSummary);
            changeLog.setChangedFields(changedFields);
            feature.addChange(changeLog);
            featureRepository.updateFeature(newFeature, user);
            return new Confirmation("Feature updated successfully", HttpStatus.OK.value());
        } catch (SparqlException e) {
            throw new GlycanRepositoryException("Error updating feature with id: " + feature.getId());
        }
    }
	
	@ApiOperation(value = "Update given slide layout for the user", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value = "/updateSlideLayout", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Slide layout updated successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to update slide layout"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation updateSlideLayout(
			@ApiParam(required=true, value="Slide layout to be updated, id is required, name and comment can be updated only") 
			@RequestBody SlideLayout layout, Principal principal) throws SQLException {
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		// validate first
		if (validator != null) {
			if (layout.getName() == null || layout.getName().isEmpty()) 
				errorMessage.addError(new ObjectError("name", "NoEmpty"));
			if  (layout.getName() != null) {
				Set<ConstraintViolation<SlideLayout>> violations = validator.validateValue(SlideLayout.class, "name", layout.getName().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (layout.getDescription() != null) {
				Set<ConstraintViolation<SlideLayout>> violations = validator.validateValue(SlideLayout.class, "description", layout.getDescription().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("description", "LengthExceeded"));
				}		
			}
		
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			SlideLayout slideLayout= new SlideLayout();
			slideLayout.setUri(GlygenArrayRepository.uriPrefix + layout.getId());
			slideLayout.setDescription(layout.getDescription() != null ? layout.getDescription().trim() : layout.getDescription());
			slideLayout.setName(layout.getName() != null ? layout.getName().trim() : null);		
			
			SlideLayout local = null;
			// check if name is unique
			if (slideLayout.getName() != null && !slideLayout.getName().isEmpty()) {
				local = layoutRepository.getSlideLayoutByName(slideLayout.getName().trim(), user);
				if (local != null && !local.getUri().equals(slideLayout.getUri())) {   // there is another with the same name
					errorMessage.addError(new ObjectError("name", "Duplicate"));
				}
			} 
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid slide layout information", errorMessage);
			
			layoutRepository.updateSlideLayout(slideLayout, user);
			return new Confirmation("Slide Layout updated successfully", HttpStatus.OK.value());
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Error updating slide layout with id: " + layout.getId());
		}
	}
	
	@ApiOperation(value = "Upload file", authorizations = { @Authorization(value="Authorization") })
	@RequestMapping(value = "/upload", method=RequestMethod.POST, 
	        consumes= {"application/octet-stream"},
			produces={"application/json", "application/xml"})
	public UploadResult uploadFile(
	        //@RequestParam("file") MultipartFile file,
	        HttpEntity<byte[]> requestBody,
            @RequestParam("resumableFilename") String resumableFilename,
            @RequestParam ("resumableRelativePath") String resumableRelativePath,
            @RequestParam ("resumableTotalChunks") String resumableTotalChunks,
            @RequestParam("resumableChunkSize") int resumableChunkSize,
            @RequestParam("resumableChunkNumber") int resumableChunkNumber,
            @RequestParam("resumableTotalSize") long resumableTotalSize,
            @RequestParam("resumableIdentifier") String resumableIdentifier
    ) throws IOException, InterruptedException {
		 
        ResumableFileInfo info = ResumableInfoStorage.getInstance().get(resumableIdentifier);
        if (info == null) {
            String extension = resumableFilename.substring(resumableFilename.lastIndexOf(".")+1);
        	String uniqueFileName = System.currentTimeMillis() + "." +  extension;
            String resumableFilePath = new File(uploadDir, uniqueFileName).getAbsolutePath() + ".temp";
        	info = new ResumableFileInfo();
            info.resumableChunkSize = resumableChunkSize;
            info.resumableIdentifier = resumableIdentifier;
            info.resumableTotalSize = resumableTotalSize;
            info.resumableFilename = resumableFilename;
            info.resumableRelativePath = resumableRelativePath;
            info.resumableFilePath = resumableFilePath;
        	ResumableInfoStorage.getInstance().add(info);
        } 
        
		RandomAccessFile raf = new RandomAccessFile(info.resumableFilePath, "rw");

        //Seek to position
        raf.seek((resumableChunkNumber - 1) * (long)resumableChunkSize);
        //byte[] payload = file.getBytes();
        byte[] payload = requestBody.getBody();
        InputStream is = new ByteArrayInputStream(payload);
        long content_length = payload.length;
        long read = 0;
        byte[] bytes = new byte[1024 * 100];
        while(read < content_length) {
            int r = is.read(bytes);
            if (r < 0)  {
                break;
            }
            raf.write(bytes, 0, r);
            read += r;
        }
        raf.close();
        
        info.uploadedChunks.add(new ResumableFileInfo.ResumableChunkNumber(resumableChunkNumber));
        
        UploadResult result = new UploadResult();
        FileWrapper file = new FileWrapper();
        file.setIdentifier(info.resumableFilePath.substring(info.resumableFilePath.lastIndexOf(File.separator) + 1));
        file.setOriginalName(resumableFilename);
        file.setFileFolder(uploadDir);
        file.setFileSize(read);
        result.setFile(file);
        
        if (info.checkIfUploadFinished()) { //Check if all chunks uploaded, and change filename
            ResumableInfoStorage.getInstance().remove(info);
            result.setStatusCode(HttpStatus.OK.value());
            int index = info.resumableFilePath.indexOf(".temp") == -1 ? info.resumableFilePath.length() : info.resumableFilePath.indexOf(".temp");
            file.setIdentifier(info.resumableFilePath.substring(info.resumableFilePath.lastIndexOf(File.separator) + 1, index));
            result.setFile(file);
            return result;
        } else {
        	result.setStatusCode(HttpStatus.ACCEPTED.value());
        	return result;
        }
	}
	
	public static BufferedImage createImageForGlycan(SequenceDefinedGlycan glycan) {
	    BufferedImage t_image = null;
        org.eurocarbdb.application.glycanbuilder.Glycan glycanObject = null;
        try {
            if (glycan.getSequenceType() == GlycanSequenceFormat.GLYCOCT) {
                glycanObject = 
                        org.eurocarbdb.application.glycanbuilder.Glycan.
                        fromGlycoCTCondensed(glycan.getSequence().trim());
                if (glycanObject == null && glycan.getGlytoucanId() != null) {
                    String seq = GlytoucanUtil.getInstance().retrieveGlycan(glycan.getGlytoucanId());
                    if (seq != null) {
                        try {
	                        WURCS2Parser t_wurcsparser = new WURCS2Parser();
	                        glycanObject = t_wurcsparser.readGlycan(seq, new MassOptions());
                        } catch (Exception e) {
                            logger.error ("Glycan image cannot be generated with WURCS sequence", e);
                        }
                    }
                }
                
            } else if (glycan.getSequenceType() == GlycanSequenceFormat.WURCS) {
                WURCS2Parser t_wurcsparser = new WURCS2Parser();
                glycanObject = t_wurcsparser.readGlycan(glycan.getSequence().trim(), new MassOptions());
            }
            if (glycanObject != null) {
                t_image = glycanWorkspace.getGlycanRenderer().getImage(glycanObject, true, false, true, 3.0D);
            } 

        } catch (Exception e) {
            logger.error ("Glycan image cannot be generated", e);
            // check if there is glytoucan id
            if (glycan.getGlytoucanId() != null) {
                String seq = GlytoucanUtil.getInstance().retrieveGlycan(glycan.getGlytoucanId());
                if (seq != null) {
                    WURCS2Parser t_wurcsparser = new WURCS2Parser();
                    try {
                        glycanObject = t_wurcsparser.readGlycan(seq, new MassOptions());
                        if (glycanObject != null) {
                            t_image = glycanWorkspace.getGlycanRenderer().getImage(glycanObject, true, false, true, 3.0D);
                        }
                    } catch (Exception e1) {
                        logger.error ("Glycan image cannot be generated from WURCS", e);
                    }
                }
            }
            
        }
        return t_image;
    }
	
	private ReducingEndType getReducingEnd (String glycoCT) {
	    ReducingEndType type = ReducingEndType.UNKNOWN;
	    try {
	        SugarImporterGlycoCTCondensed importer = new SugarImporterGlycoCTCondensed();
            Sugar sugar = importer.parse(glycoCT);
            if (sugar == null) {
                logger.info("Cannot get Sugar object for sequence: " + glycoCT + " to determine reducing end");
                 
            }
            Monosaccharide root = GlycanBaseTypeUtil.getReducingEnd(sugar);
            if (root.getRingStart() < 1)
            {
                return ReducingEndType.OPENSRING;
            }
            List<Modification> t_modifications = root.getModification();
            for (Modification t_mod : t_modifications)
            {
                if (t_mod.getModificationType().equals(ModificationType.ALDI))
                {
                    return ReducingEndType.OPENSRING;
                }
            }
            if (root.getAnomer() == Anomer.Beta) {
                return ReducingEndType.BETA;
            }
            if (root.getAnomer() == Anomer.Alpha) {
                return ReducingEndType.ALPHA;
            }
        } catch (GlycoconjugateException | SugarImporterException e) {
            logger.info("reducing end cannot be found", e);
        }
	    return type;
	}
}
