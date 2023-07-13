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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.imageio.ImageIO;
import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.codec.digest.DigestUtils;
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
import org.glygen.array.persistence.rdf.data.Checksum;
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
import org.glygen.array.util.SpotMetadataConfig;
import org.glygen.array.view.AsyncBatchUploadResult;
import org.glygen.array.view.BatchGlycanFileType;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

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
	
	@Operation(summary = "Add an alias to given glycan for the user", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value = "/addAlias/{glycanId}", method = RequestMethod.POST, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Alias added to glycan successfully"), 
			@ApiResponse(responseCode="400", description="Invalid request, alias cannot be empty"),
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to update glycans"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public Confirmation addAliasForGlycan(
			@Parameter(required=true, description="Id of the glycan to add alias for") 
			@PathVariable("glycanId") String glycanId, 
			@Parameter(required=true, description="alias for the glycan") 
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
	
	@Operation(summary = "Add given block layout for the user", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/addblocklayout", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Block layout added successfully"), 
			@ApiResponse(responseCode="400", description="Invalid request, validation error"),
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to register block layouts"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public String addBlockLayout (
			@Parameter(required=true, description="Block layout to be added, name, width, height, and spots are required, features should already exist in the repository")
			@RequestBody BlockLayout layout, 
			@Parameter(required=false, description="true if there is no need to check again if feature exists")
			@RequestParam(required=false, value="noFeatureCheck")
			Boolean noFeatureCheck, Principal p) {
	    UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        return addService.addBlockLayout(layout, noFeatureCheck, user);
	}
	

	@Operation(summary = "Add given feature for the user", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/addfeature", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added feature"), 
			@ApiResponse(responseCode="400", description="Invalid request, validation error"),
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to register features"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public String addFeature (
			@Parameter(required=true, description="Feature to be added, a linker and an at least one glycan are mandatory") 
			@RequestBody(required=true) org.glygen.array.persistence.rdf.Feature feature, Principal p) {
	    
	    UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
	    return addService.addFeature(feature, user);
	}

  /*  @Operation(summary = "Add given feature, provided only with sequence based linker for the user")
	@RequestMapping(value="/addfeatureFromSequence", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added feature"), 
			@ApiResponse(responseCode="400", description="Invalid request, validation error"),
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to register features"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public String addFeatureFromLinkerSequence (
			@Parameter(required=false, value="Feature to be added, "
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
	
	@Operation(summary = "Add given glycan for the user", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/addglycan", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="id of the added glycan"), 
			@ApiResponse(responseCode="400", description="Invalid request, validation error"),
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to register glycans"),
			@ApiResponse(responseCode="409", description="A glycan with the given sequence already exists!"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public String addGlycan (@RequestBody Glycan glycan, Principal p, 
	        @Parameter(required=true, name="noGlytoucanRegistration", description="if true, no registration attempt will be made for glytoucan accession number")
	        @RequestParam("noGlytoucanRegistration") Boolean noGlytoucanRegistration,
	        @Parameter(required=false, name="bypassGlytoucanCheck", description="if you already received the sequence from glytoucan, you can set this flag to false") 
            @RequestParam(required=false, value="bypassGlytoucanCheck") Boolean bypassGlytoucanCheck) {
		if (glycan.getType() == null) {
			// assume sequenceDefinedGlycan
			glycan.setType(GlycanType.SEQUENCE_DEFINED);
		}
		
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		return addService.addGlycan(glycan, user, noGlytoucanRegistration, bypassGlytoucanCheck);
	}
	
    @Operation(summary = "Register all glycans listed in a file", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value = "/addBatchGlycan", method=RequestMethod.POST, 
			consumes = {"application/json", "application/xml"}, produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Glycans processed successfully"), 
			@ApiResponse(responseCode="400", description="Invalid request if file is not a valid file"),
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to register glycans"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public AsyncBatchUploadResult addGlycanFromFile (
	        @Parameter(required=true, name="file", description="details of the uploded file") 
	        @RequestBody
	        FileWrapper fileWrapper, Principal p, 
	        @RequestParam Boolean noGlytoucanRegistration,
	        @Parameter(required=true, name="filetype", description="type of the file", schema = @Schema(type = "string", allowableValues= {"Tab separated Glycan file", "Library XML", "GlycoWorkbench(.gws)", "WURCS", "CFG IUPAC", "Repository Export (.json)" })) 
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
                return handleAsyncGlycanUpload(p, fileContent, type, noGlytoucanRegistration);  
            } catch (IOException e) {
                ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("file", "NotValid"));
                throw new IllegalArgumentException("File cannot be read", errorMessage);
            }
    	    
        }
	}
    
    private AsyncBatchUploadResult handleAsyncGlycanUpload (Principal p, byte[] contents, BatchGlycanFileType type, Boolean noGlytoucanRegistration) {
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        
        try {    
            AsyncBatchUploadResult result = new AsyncBatchUploadResult();
            result.setStartDate(new Date());
            result.setStatus(FutureTaskStatus.PROCESSING);
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            result.setError(errorMessage);
            
            String uri = repository.addBatchUpload (result, GlygenArrayRepositoryImpl.batchGlycanTypePredicate, user);
            result.setUri(uri);
            repository.updateStatus (uri, result, user);
            
            CompletableFuture<Confirmation> confirmation = null;
                
            try {
                // process the file and add the glycans 
                switch (type) {
                case REPOSITORYEXPORT: 
                    confirmation = parserAsyncService.addGlycansFromExportFile(contents, noGlytoucanRegistration, user, errorMessage);
                    break;
                case CFG:
                    confirmation = parserAsyncService.addGlycanFromTextFile(contents, noGlytoucanRegistration, user, errorMessage, GlycanSequenceFormat.IUPAC.getLabel(), "\\n");
                    break;
                case TABSEPARATED:
                    ParserConfiguration config = new ParserConfiguration();
                    config.setNameColumn(0);
                    config.setIdColumn(1);
                    config.setGlytoucanIdColumn(2);
                    config.setSequenceColumn(3);
                    config.setSequenceTypeColumn(4);
                    config.setMassColumn(5);
                    config.setCommentColumn(6);
                    confirmation = parserAsyncService.addGlycanFromCSVFile(contents, noGlytoucanRegistration, user, errorMessage, config);
                    break;
                case GWS:
                    confirmation = parserAsyncService.addGlycanFromTextFile(contents, noGlytoucanRegistration, user, errorMessage, GlycanSequenceFormat.GWS.getLabel(), ";");
                    break;
                case WURCS:
                    confirmation = parserAsyncService.addGlycanFromTextFile(contents, noGlytoucanRegistration, user, errorMessage, GlycanSequenceFormat.WURCS.getLabel(), "\\n");
                    break;
                case XML:
                    confirmation = parserAsyncService.addGlycanFromLibraryFile(contents, noGlytoucanRegistration, user, errorMessage);
                    break;
                default:
                    errorMessage.addError(new ObjectError("filetype", "NotValid"));
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                    throw new IllegalArgumentException("File is not acceptable", errorMessage);
                }
                
                if (confirmation != null && confirmation.isCompletedExceptionally()) {
                    logger.error("glycan upload completed with exception!!");
                }
                confirmation.whenComplete((conf, e) -> {
                    try {
                        if (e != null) {
                            logger.error(e.getMessage(), e);
                            result.setStatus(FutureTaskStatus.ERROR);
                            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException && e.getCause().getCause() instanceof ErrorMessage) 
                                result.setError((ErrorMessage) e.getCause().getCause());
                            else {
                                errorMessage.addError(new ObjectError("exception", e.getMessage()));
                                result.setError(errorMessage);
                            }
                        } else {
                            result.setStatus(FutureTaskStatus.DONE);    
                            result.setSuccessMessage(conf.getMessage());
                            repository.updateBatchUpload(result, user);
                        }
                        repository.updateStatus (uri, result, user);
                    } catch (SparqlException | SQLException ex) {
                        throw new GlycanRepositoryException("Glycans cannot be added for user " + p.getName(), e);
                    } 
                });
                confirmation.get(1000, TimeUnit.MILLISECONDS);
            } catch (IllegalArgumentException e) {
                result.setStatus(FutureTaskStatus.ERROR);
                if (e.getCause() != null && e.getCause() instanceof ErrorMessage)
                    result.setError((ErrorMessage) e.getCause());
                repository.updateStatus (uri, result, user);
                throw e;
            } catch (TimeoutException e) {
                synchronized (this) {
                    if (result.getError() == null || (result.getError() != null && (result.getError().getErrors() == null || result.getError().getErrors().isEmpty())))
                        result.setStatus(FutureTaskStatus.PROCESSING);
                    else 
                        result.setStatus(FutureTaskStatus.ERROR);
                    repository.updateStatus (uri, result, user);
                    return result;
                }
            }
            return result;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Glycans cannot be added for user " + p.getName(), e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error uploading glycans from the given file", e);
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                for (ObjectError err: ((ErrorMessage) e.getCause()).getErrors()) {
                    errorMessage.addError(err);
                }
            } else {
                errorMessage.addError(new ObjectError("file", e.getMessage()));
            }
            throw new IllegalArgumentException("Error uploading glycans from the given file", errorMessage);
        }
    }
    
    private AsyncBatchUploadResult handleAsyncUpload (Principal p, byte[] contents, LinkerType linkerType) {
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        boolean linker = linkerType != null;
        try {    
            AsyncBatchUploadResult result = new AsyncBatchUploadResult();
            result.setStartDate(new Date());
            result.setStatus(FutureTaskStatus.PROCESSING);
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            result.setError(errorMessage);
            
            String uploadType = linker ? GlygenArrayRepositoryImpl.batchLinkerTypePredicate + linkerType.name() : GlygenArrayRepositoryImpl.batchFeatureTypePredicate;
            String uri = repository.addBatchUpload (result, uploadType, user);
            result.setUri(uri);
            repository.updateStatus (uri, result, user);
            
            CompletableFuture<Confirmation> confirmation = null;
                
            try {
                if (linkerType != null) {
                    confirmation = parserAsyncService.addLinkersFromExportFile (contents, linkerType, user, errorMessage);
                    
                } else {
                    confirmation = parserAsyncService.addFeaturesFromExportFile(contents, user, errorMessage);
                }
                
                if (confirmation.isCompletedExceptionally()) {
                    logger.error("feature/linker upload completed with exception!!");
                }
                confirmation.whenComplete((conf, e) -> {
                    try {
                        if (e != null) {
                            logger.error(e.getMessage(), e);
                            result.setStatus(FutureTaskStatus.ERROR);
                            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException && e.getCause().getCause() instanceof ErrorMessage) 
                                result.setError((ErrorMessage) e.getCause().getCause());
                            else {
                                errorMessage.addError(new ObjectError("exception", e.getMessage()));
                                result.setError(errorMessage);
                            }
                        } else {
                            result.setStatus(FutureTaskStatus.DONE);   
                            result.setSuccessMessage(conf.getMessage());
                            repository.updateBatchUpload(result, user);
                        }
                        repository.updateStatus (uri, result, user);
                    } catch (SparqlException | SQLException ex) {
                        throw new GlycanRepositoryException((linker ? "Linkers": "Features") + " cannot be added for user " + p.getName(), e);
                    } 
                });
                confirmation.get(1000, TimeUnit.MILLISECONDS);
            } catch (IllegalArgumentException e) {
                result.setStatus(FutureTaskStatus.ERROR);
                if (e.getCause() != null && e.getCause() instanceof ErrorMessage)
                    result.setError((ErrorMessage) e.getCause());
                repository.updateStatus (uri, result, user);
                throw e;
            } catch (TimeoutException e) {
                synchronized (this) {
                    if (result.getError() == null || (result.getError() != null && (result.getError().getErrors() == null || result.getError().getErrors().isEmpty())))
                        result.setStatus(FutureTaskStatus.PROCESSING);
                    else 
                        result.setStatus(FutureTaskStatus.ERROR);
                    repository.updateStatus (uri, result, user);
                    return result;
                }
            }
            return result;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException((linker ? "Linkers": "Features") + " cannot be added for user " + p.getName(), e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            if (e.getCause() instanceof IllegalArgumentException){
                throw (IllegalArgumentException)e.getCause();
            } else {
                logger.error("Error uploading glycans from the given file", e);
                ErrorMessage errorMessage = new ErrorMessage("Error uploading " + (linker ? "Linkers": "Features") +  " from the given file");
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                    for (ObjectError err: ((ErrorMessage) e.getCause()).getErrors()) {
                        errorMessage.addError(err);
                    }
                } else {
                    String[] codes = new String[] {e.getMessage()};
                    errorMessage.addError(new ObjectError ("file", codes, null, " NotValid"));
                }
                throw new IllegalArgumentException("Error uploading " + (linker ? "Linkers": "Features") +  " from the given file", errorMessage);
            }
        }
    }
    
    @Operation(summary = "Register all linkers listed in a file", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/addBatchLinker", method=RequestMethod.POST, 
            consumes = {"application/json", "application/xml"}, produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Linkers processed successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request if file is not a valid file"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to register linkers"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public AsyncBatchUploadResult addLinkerFromFile (
            @Parameter(required=true, name="file", description="details of the uploded file") 
            @RequestBody
            FileWrapper fileWrapper, Principal p, 
            @Parameter(required=true, name="filetype", description="type of the file", schema = @Schema(type = "string", allowableValues= {"Repository Export (.json)"}))
            @RequestParam(required=true, value="filetype") String fileType,
            @Parameter(required=true, description="type of the molecule", schema = @Schema(type = "string", allowableValues= {"SMALLMOLECULE", "LIPID", "PEPTIDE", "PROTEIN", "OTHER"})) 
            @RequestParam("type") String moleculeType) {
        
        LinkerType linkerType = null;
        try {
            linkerType = LinkerType.valueOf(moleculeType);
            if (linkerType == null) {
                ErrorMessage errorMessage = new ErrorMessage("Incorrect molecule type");
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                String[] codes = new String[] {"Molecule Type:", moleculeType};
                errorMessage.addError(new ObjectError("moleculeType", codes, null, "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Incorrect molecule type", errorMessage);
            }
        } catch (Exception e) {
            ErrorMessage errorMessage = new ErrorMessage("Incorrect molecule type");
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            String[] codes = new String[] {"Molecule Type:", moleculeType};
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
                    return handleAsyncUpload(p, fileContent, linkerType);
                } else {
                    ErrorMessage errorMessage = new ErrorMessage("filetype is not accepted");
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    errorMessage.addError(new ObjectError("filetype", "NotValid"));
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                    throw new IllegalArgumentException("File is not acceptable", errorMessage);
                }
            } catch (IllegalArgumentException e) {
              if (e.getCause() instanceof ErrorMessage) 
                  throw e;
              else {
                  ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
                  errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                  errorMessage.addError(new ObjectError("file", "NotValid"));
                  throw new IllegalArgumentException("File cannot be read", errorMessage);
              }
            } catch (Exception e) {
                ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("file", "NotValid"));
                throw new IllegalArgumentException("File cannot be read", errorMessage);
            }
        }
    }
    
    @Operation(summary = "Register all features listed in a file", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/addBatchFeature", method=RequestMethod.POST, 
            consumes = {"application/json", "application/xml"}, produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Features processed successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request if file is not a valid file"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to register features"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public AsyncBatchUploadResult addFeatureFromFile (
            @Parameter(required=true, name="file", description="details of the uploded file") 
            @RequestBody
            FileWrapper fileWrapper, Principal p, 
            @Parameter(required=true, name="filetype", description="type of the file", schema = @Schema(type = "string", allowableValues= {"Repository Export (.json)"})) 
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
                    return handleAsyncUpload(p, fileContent, null); 
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

	@Operation(summary = "Add given linker for the user", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/addlinker", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added linker"), 
			@ApiResponse(responseCode="400", description="Invalid request, validation error"),
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to register linkers"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public String addLinker (
			@Parameter(required=true, description="Linker to be added, type needs to be set correctly, pubChemId is required for small molecule and lipid, "
			        + "sequence is required for protein and peptide, other fields are optional") 
			@RequestBody Linker linker, 
			@RequestParam(value="unknown", required=false)
			@Parameter(required=false, description="true, if the linker is of unknown type. The default is false")
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

	@Operation(summary = "Add given slide layout for the user", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/addslidelayout", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Slide layout added successfully"), 
			@ApiResponse(responseCode="400", description="Invalid request, validation error"),
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to register slide layouts"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public String addSlideLayout (
			@Parameter(required=true, description="Slide Layout to be added, name, width, height and blocks are required")
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
	
	
	
	@Operation(summary = "Checks whether the given slidelayout name is available to be used (returns true if available, false if alredy in use", 
	        security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value = "/checkSlidelayoutName", method = RequestMethod.GET)
	@ApiResponses(value = { @ApiResponse(responseCode="200", description="Check performed successfully", content = {
            @Content( schema = @Schema(implementation = Boolean.class))}),
			@ApiResponse(responseCode="415", description= "Media type is not supported"),
			@ApiResponse(responseCode="500", description= "Internal Server Error") })
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
	
	@Operation(summary = "Retrieve active batch upload processes by type", 
            security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/checkbatchupload", method = RequestMethod.GET,
            produces={"application/json", "application/xml"})
    @ApiResponses(value = { @ApiResponse(responseCode="200", description= "Check performed successfully", content = {
            @Content( schema = @Schema(implementation = AsyncBatchUploadResult.class))}),
            @ApiResponse(responseCode="415", description= "Media type is not supported"),
            @ApiResponse(responseCode="500", description= "Internal Server Error") })
    public AsyncBatchUploadResult checkActiveBatchUpload(
            @Parameter(required=true, description="type of the batch upload to check", 
                    schema = @Schema(type = "string", allowableValues= {"batch_glycan_job", "batch_linker_job", "batch_feature_job"}))
            @RequestParam("uploadtype") final String type, 
            @Parameter(required=false, description="type of the molecule", 
                    schema = @Schema(type = "string", allowableValues= {"SMALLMOLECULE", "LIPID", "PEPTIDE", "PROTEIN", "OTHER"}))
            @RequestParam(required=false, value="moleculetype") String moleculeType,
            Principal principal) throws SparqlException, SQLException {

        UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
        if (type == null || 
                (!(GlygenArrayRepository.ontPrefix + type).equalsIgnoreCase(GlygenArrayRepositoryImpl.batchGlycanTypePredicate) && 
                        !(GlygenArrayRepository.ontPrefix + type).contains(GlygenArrayRepositoryImpl.batchLinkerTypePredicate) &&
                        !(GlygenArrayRepository.ontPrefix + type).equalsIgnoreCase(GlygenArrayRepositoryImpl.batchFeatureTypePredicate))) {
            ErrorMessage errorMessage = new ErrorMessage("upload type is not in the list of accepted values");
            String[] codes = {type};
            errorMessage.addError(new ObjectError("uploadType", codes, null, "NotValid"));
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            throw new IllegalArgumentException("Type is invalid", errorMessage);
        }
        
        if (moleculeType != null) {
            if (!moleculeType.equalsIgnoreCase("SMALLMOLECULE") &&
                    !moleculeType.equalsIgnoreCase("LIPID") &&
                    !moleculeType.equalsIgnoreCase("PEPTIDE") &&
                    !moleculeType.equalsIgnoreCase("PROTEIN") &&
                    !moleculeType.equalsIgnoreCase("OTHER")) {
                ErrorMessage errorMessage = new ErrorMessage("molecule type is not in the list of accepted values");
                String[] codes = {moleculeType};
                errorMessage.addError(new ObjectError("moleculeType", codes, null, "NotValid"));
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                throw new IllegalArgumentException("Molecule Type is invalid", errorMessage);
            }
        }
        
        List<AsyncBatchUploadResult> results = repository.getActiveBatchUploadByType((GlygenArrayRepository.ontPrefix + type + (moleculeType == null ? "" : moleculeType)), user);

        if (!results.isEmpty()) {
            //repository.updateBatchUpload(results.get(0), user);
            return results.get(0);
        }

        throw new EntityNotFoundException("There is no active upload");
    }
	
	@Operation(summary = "Update (hide) active batch upload processes by type", 
            security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/updatebatchupload", method = RequestMethod.POST,
            produces={"application/json", "application/xml"})
    @ApiResponses(value = { @ApiResponse(responseCode="200", description= "Update performed successfully", content = {
            @Content( schema = @Schema(implementation = AsyncBatchUploadResult.class))}),
            @ApiResponse(responseCode="415", description= "Media type is not supported"),
            @ApiResponse(responseCode="500", description= "Internal Server Error") })
    public Confirmation updateActiveBatchUpload(
            @Parameter(required=true, description="type of the batch upload to check", 
                    schema = @Schema(type = "string", allowableValues= {"batch_glycan_job", "batch_linker_job", "batch_feature_job"}))
            @RequestParam("uploadtype") final String type, 
            @Parameter(required=false, description="type of the molecule", 
                    schema = @Schema(type = "string", allowableValues= {"SMALLMOLECULE", "LIPID", "PEPTIDE", "PROTEIN", "OTHER"}))
            @RequestParam(required=false, value="moleculetype") final String moleculeType,
            Principal principal) throws SparqlException, SQLException {

        UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
        if (type == null || 
                (!(GlygenArrayRepository.ontPrefix + type).equalsIgnoreCase(GlygenArrayRepositoryImpl.batchGlycanTypePredicate) && 
                        !(GlygenArrayRepository.ontPrefix + type).contains(GlygenArrayRepositoryImpl.batchLinkerTypePredicate) &&
                        !(GlygenArrayRepository.ontPrefix + type).equalsIgnoreCase(GlygenArrayRepositoryImpl.batchFeatureTypePredicate))) {
            ErrorMessage errorMessage = new ErrorMessage("upload type is not in the list of accepted values");
            errorMessage.addError(new ObjectError("uploadType", "NotValid"));
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            throw new IllegalArgumentException("Type is invalid", errorMessage);
        }
        
        List<AsyncBatchUploadResult> results = repository.getActiveBatchUploadByType((GlygenArrayRepository.ontPrefix + type + (moleculeType == null ? "" : moleculeType)), user);

        if (!results.isEmpty()) {
            repository.updateBatchUploadAccess(results.get(0), user);
        }

        return new Confirmation ("Last active batch upload is updated, will not be shown again", HttpStatus.OK.value());
    }
	
	@Operation(summary = "Delete given block layout", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/deleteblocklayout/{layoutId}", method = RequestMethod.DELETE, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Block Layout deleted successfully"), 
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to delete block layouts"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public Confirmation deleteBlockLayout (
			@Parameter(required=true, description="id of the block layout to delete") 
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
	
	@Operation(summary = "Delete given feature from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/deletefeature/{featureId}", method = RequestMethod.DELETE, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Feature deleted successfully"), 
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to delete linkers"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public Confirmation deleteFeature (
			@Parameter(required=true, description="id of the feature to delete") 
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
	
	@Operation(summary = "Delete given glycan from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/delete/{glycanId}", method = RequestMethod.DELETE, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Glycan deleted successfully"), 
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to delete glycans"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public Confirmation deleteGlycan (
			@Parameter(required=true, description="id of the glycan to delete") 
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
	
	@Operation(summary = "Delete given linker from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/deletelinker/{linkerId}", method = RequestMethod.DELETE, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Linker deleted successfully"), 
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to delete linkers"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public Confirmation deleteLinker (
			@Parameter(required=true, description="id of the linker to delete") 
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
	
	@Operation(summary = "Delete given slide layout", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/deleteslidelayout/{layoutId}", method = RequestMethod.DELETE, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Slide Layout deleted successfully"), 
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to delete slide layouts"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public Confirmation deleteSlideLayout (
			@Parameter(required=true, description="id of the block layout to delete") 
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
	
	@Operation(summary = "Retrieve block layout with the given id", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/getblocklayout/{layoutId}", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Block Layout retrieved successfully"), 
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to list block layouts"),
			@ApiResponse(responseCode="404", description="Block layout with given id does not exist"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public BlockLayout getBlockLayout (
			@Parameter(required=true, description="id of the block layout to retrieve") 
			@PathVariable("layoutId") String layoutId, 
			@Parameter (required=false, schema = @Schema(type = "boolean", defaultValue="true"), description="if false, do not load block details. Default is true (to load all)")
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
    public static SlideLayout getFullLayoutFromLibrary (File libraryFile, SlideLayout layout, MetadataTemplateRepository templateRepository, boolean layoutOnly) {
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
	        		// keep the original values if already set
	        		mySlideLayout.setDateAddedToLibrary(layout.getDateAddedToLibrary());
	        		mySlideLayout.setDateCreated(layout.getDateCreated());
	        		mySlideLayout.setDateModified(layout.getDateModified());
	        		mySlideLayout.setUri(layout.getUri());
	        		mySlideLayout.setStatus(layout.getStatus());
	        		mySlideLayout.setStartDate(layout.getStartDate());
	        		mySlideLayout.setError(layout.getError());
	        		
	        		if (!layoutOnly) {
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
    	        	        String uri = templateRepository.getTemplateByName("Default Feature", MetadataTemplateType.FEATURE);
    	        	        if (uri != null) {
    	        	        	MetadataTemplate template = templateRepository.getTemplateFromURI(uri);
    	        	        	DescriptionTemplate descT = ExtendedGalFileParser.getKeyFromTemplate("Commercial source", template);
    	        	        	DescriptorGroup group = new DescriptorGroup();
    	        	            group.setKey(descT);
    	        	            group.setNotRecorded(true);
    	        	            DescriptionTemplate descT2 = ExtendedGalFileParser.getKeyFromTemplate("Non-commercial", template);
                                DescriptorGroup group2 = new DescriptorGroup();
                                group2.setKey(descT2);
                                group2.setNotRecorded(true);
    	        	            featureMetadata.setDescriptorGroups(new ArrayList<>());
    	        	            featureMetadata.getDescriptorGroups().add(group);
    	        	            featureMetadata.getDescriptorGroups().add(group2);
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
    	                            errorMessage.addError(new ObjectError("file", e.getMessage()));
    	                        }
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
	
	@Operation(summary = "Import slide layout from uploaded GAL file", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/addSlideLayoutFromGalFile", method=RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added slide layout"), 
            @ApiResponse(responseCode="400", description="Invalid request, file cannot be found"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to register slide layouts"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
	public String addSlideLayoutFromGalFile (
	        @Parameter(required=true, description="uploaded GAL file information")
	        @RequestBody
            FileWrapper fileWrapper,
	        @Parameter(required=true, description="name of the slide layout to be created") 
	        @RequestParam("name")
	        String slideLayoutName, 
	        @Parameter(required=false, description="width of the slide layout", example="1") 
            @RequestParam(required=false, value="width")
	        Integer width,
	        @Parameter(required=false, description="height of the slide layout", example="1") 
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
                    GlygenArrayController.calculateChecksum (fileWrapper);
                    fileWrapper.setIdentifier(uploadedFileName);
                    fileWrapper.setFileFormat("GAL");    //TODO do we need to standardize this?
                    fileWrapper.setCreatedDate(new Date());
                    
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
	
	@Operation(summary = "Check if there is an active slide upload process", 
            security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/checkslideupload", method = RequestMethod.GET,
            produces={"application/json", "application/xml"})
    @ApiResponses(value = { @ApiResponse(responseCode="200", description= "There is an ongoing upload process", content = {
            @Content( schema = @Schema(implementation = AsyncBatchUploadResult.class))}),
            @ApiResponse(responseCode="404", description= "No active upload found"),
            @ApiResponse(responseCode="500", description= "Internal Server Error") })
    public Confirmation checkActiveSlideLayoutUpload(Principal p) throws SparqlException, SQLException {
	    UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        
        try {
            List<SlideLayout> layouts = layoutRepository.getSlideLayoutByUser(user, 0, -1, null, false, 0, null);
            for (SlideLayout layout: layouts) {
                if (layout.getStatus() == FutureTaskStatus.PROCESSING) {
                    return new Confirmation("There is another active slide layout upload", HttpStatus.OK.value());
                    
                }
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("cannot retrieve slide layouts " + p.getName(), e);
        }
        
        throw new EntityNotFoundException("There is no active slide layout upload");
	}

    @Operation(summary = "Import selected slide layouts from uploaded GRITS array library file", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value = "/addSlideLayoutFromLibrary", method=RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Slide layouts imported into repository successfully"), 
			@ApiResponse(responseCode="400", description="Invalid request, file cannot be found"),
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to register slide layouts"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public String addSlideLayoutFromLibrary (
			@Parameter(required=true, description="uploaded file and the list of slide layouts to be imported, only name is sufficient for a slide layout")
			@RequestBody LibraryImportInput input, 
			Principal p) {
		
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        
        //do not allow if there is another ongoing process
        try {
            List<SlideLayout> layouts = layoutRepository.getSlideLayoutByUser(user, 0, -1, null, false, 0, null);
            for (SlideLayout layout: layouts) {
                if (layout.getStatus() == FutureTaskStatus.PROCESSING) {
                    ErrorMessage errorMessage = new ErrorMessage("There is another active slide layout upload. Please try again later");
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    errorMessage.addError(new ObjectError("slideLayout", "NotDone"));
                    errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
                    throw new IllegalArgumentException("There is another active slide layout upload. Please try again later", errorMessage);
                }
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("cannot retrieve slide layouts " + p.getName(), e);
        }
        
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
					errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
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
                            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                            
						}
					} catch (Exception e) {
						errorMessage.addError(new ObjectError("slideLayout", e.getMessage()));
						errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
						throw new IllegalArgumentException("Slide layout search failed", errorMessage);
					}
				}
				if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
				    throw new IllegalArgumentException("Slide layout with the given name already exists", errorMessage);
				}
				
				try {
				    SlideLayout slideLayout = getFullLayoutFromLibrary (libraryFile, input.getSlideLayout(), templateRepository, true);
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
                            slideId = parserAsyncService.importSlideLayout(slideLayout, libraryFile, errorMessage, user);
                            if (slideId.isCompletedExceptionally()) {
                                logger.error("slide upload completed with exception!!");
                            }
                            // add the slide layout 
                            slideId.whenComplete((uriString, e) -> {
                                try {
                                    if (e != null) {
                                        logger.error(e.getMessage(), e);
                                        slideLayout.setStatus(FutureTaskStatus.ERROR);
                                        if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException && e.getCause().getCause() instanceof ErrorMessage) 
                                            slideLayout.setError((ErrorMessage) e.getCause().getCause());
                                        else {
                                            errorMessage.addError(new ObjectError("exception", e.getMessage()));
                                            slideLayout.setError(errorMessage);
                                        }
                                    } else {
                                        slideLayout.setStatus(FutureTaskStatus.DONE);    
                                    }
                                    repository.updateStatus (slideLayout.getUri(), slideLayout, user);
                                } catch (SparqlException | SQLException ex) {
                                    throw new GlycanRepositoryException("SlideLayout cannot be added for user " + p.getName(), e);
                                } 
                            });
                            slideId.get(10, TimeUnit.MILLISECONDS);
                        } catch (IllegalArgumentException e) {
                            slideLayout.setStatus(FutureTaskStatus.ERROR);
                            if (e.getCause() != null && e.getCause() instanceof ErrorMessage)
                                slideLayout.setError((ErrorMessage) e.getCause());
                            else {
                                errorMessage.addError(new ObjectError ("exception", e.getMessage()));
                                errorMessage.setErrorCode(ErrorCodes.INTERNAL_ERROR);
                                slideLayout.setError(errorMessage);
                            }
                            repository.updateStatus (uri, slideLayout, user);
                            return id;
                            //throw e;
                        } catch (TimeoutException e) {
                            synchronized (this) {
                                if (slideLayout.getError() == null)
                                    slideLayout.setStatus(FutureTaskStatus.PROCESSING);
                                else 
                                    slideLayout.setStatus(FutureTaskStatus.ERROR);
                                repository.updateStatus (uri, slideLayout, user);
                                return id;
                            }
                        } catch (CompletionException e) {
                            slideLayout.setStatus(FutureTaskStatus.ERROR);
                            if (e.getCause() != null && e.getCause() instanceof ErrorMessage)
                                slideLayout.setError((ErrorMessage) e.getCause());
                            else {
                                errorMessage.addError(new ObjectError ("exception", e.getMessage()));
                                slideLayout.setError(errorMessage);
                            }
                            repository.updateStatus (uri, slideLayout, user);
                        }
                        return id;
                    } else {
                        errorMessage.addError(new ObjectError("slideLayout", "NotValid"));
                        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                        throw new IllegalArgumentException("Given slide layout cannot be found in the file", errorMessage);
                    }
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Slide Layout cannot be added for user " + p.getName(), e);
                } catch (IllegalArgumentException e) {
                    throw e;
                } catch (Exception e) {
                    logger.error("Error getting slide layout from the library file", e);
                    if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                        for (ObjectError err: ((ErrorMessage) e.getCause()).getErrors()) {
                            errorMessage.addError(err);
                        }
                    } else {
                        errorMessage.addError(new ObjectError("file", e.getMessage()));
                        errorMessage.setErrorCode(ErrorCodes.INTERNAL_ERROR);
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
	
	@Operation(summary = "Retrieve glycan with the given id", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/getglycan/{glycanId}", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Glycan retrieved successfully"), 
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to list glycans"),
			@ApiResponse(responseCode="404", description="Gycan with given id does not exist"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public Glycan getGlycan (
			@Parameter(required=true, description="id of the glycan to retrieve") 
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
	
	@Operation(summary = "Retrieve id for a glycan given the sequence", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/getGlycanBySequence", method = RequestMethod.GET)
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Glycan id retrieved successfully", content = {
            @Content( schema = @Schema(implementation = String.class))}), 
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to list glycans"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public String getGlycanBySequence (
			@Parameter(required=true, description="Sequence of the glycan to retrieve (in GlycoCT)") 
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
	
	@Operation(summary = "Retrieve sequence (in GlycoCT) from GlyToucan for the glycan with the given glytoucan id (accession number)")
	@RequestMapping(value="/getGlycanFromGlytoucan", method = RequestMethod.GET)
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Glycan retrieved successfully", content = {
            @Content( schema = @Schema(implementation = String.class))}), 
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public String getGlycanFromGlytoucan (
			@Parameter(required=true, description="Accession number of the glycan to retrieve (from GlyToucan)") 
			@RequestParam String glytoucanId) {
		if (glytoucanId == null || glytoucanId.trim().isEmpty())
			return null;
		return addService.getSequenceFromGlytoucan(glytoucanId);
	}
	
	@Operation(summary = "Retrieve image for given glycan")
	@RequestMapping(value="/getimage/{glycanId}", method = RequestMethod.GET, 
		produces = MediaType.IMAGE_PNG_VALUE )
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Image retrieved successfully"), 
			@ApiResponse(responseCode="404", description="Image for the given glycan is not available"),
			@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public @ResponseBody byte[] getImageForGlycan (
			@Parameter(required=true, description="Id of the glycan to retrieve the image for") 
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
	
	@Operation(summary = "Retrieve linker with the given id", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/getlinker/{linkerId}", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Linker retrieved successfully"), 
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to list glycans"),
			@ApiResponse(responseCode="404", description="Linker with given id does not exist"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public Linker getLinker (
			@Parameter(required=true, description="id of the linker to retrieve") 
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
	
	@Operation(summary = "Retrieve slide layout with the given id", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/getslidelayout/{layoutId}", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Slide Layout retrieved successfully"), 
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to list slide layouts"),
			@ApiResponse(responseCode="404", description="Slide layout with given id does not exist"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public SlideLayout getSlideLayout (
			@Parameter(required=true, description="id of the slide layout to retrieve") 
			@PathVariable("layoutId") String layoutId, 
			@Parameter (required=false, schema = @Schema(type = "boolean", defaultValue="true"), description="if false, do not load slide details. Default is true (to load all)")
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
	
	@Operation(summary = "Retrieve feature with the given id", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/getfeature/{featureId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Feature retrieved successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve the feature"),
            @ApiResponse(responseCode="404", description="Feature with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public org.glygen.array.persistence.rdf.Feature getFeature (
            @Parameter(required=true, description="id of the feature to retrieve") 
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
    @Operation(summary = "Retrieve slide layouts from uploaded GRITS array library file", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value = "/getSlideLayoutFromLibrary", method=RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Slide layout retrieved from file successfully"), 
			@ApiResponse(responseCode="400", description="Invalid request, file is not valid or cannot be found"),
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to retrieve slide layouts"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public @ResponseBody List<SlideLayout> getSlideLayoutsFromLibrary(
			@Parameter(required=true, description="uploaded file with slide layouts")
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
	
	
	public static List<org.glygen.array.persistence.rdf.Spot> getSpotsFromBlockLayout (ArrayDesignLibrary library, org.grits.toolbox.glycanarray.library.om.layout.BlockLayout blockLayout, SpotMetadata spotMetadata, FeatureMetadata featureMetadata) {
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
        			    if (probe.getRatio() != null && !probe.getRatio().isEmpty()) {
            				for (Ratio r1 : probe.getRatio()) {
            					LinkedGlycan myFeature = new LinkedGlycan();
            					myFeature.setGlycans(new ArrayList<GlycanInFeature>());
            					myFeature.setMetadata(featureMetadata);
            					org.grits.toolbox.glycanarray.library.om.feature.Glycan glycan = LibraryInterface.getGlycan(library, r1.getItemId());
            					if (glycan != null) {
            					    if (!glycan.getName().equalsIgnoreCase("empty")) {
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
        		        					// special handling of 183 Sp8 (CFG data)
        		        					if (glycan.getName().equals("183 Sp8")) {
        		        					    redEnd.setType(ReducingEndType.OPENSRING);
        		        					} else {
        		        					    redEnd.setType(getReducingEnd (glycan.getSequence().trim()));
        		        					}
        	                                glycanFeature.setReducingEndConfiguration(redEnd);
        		        				} else {
        		        					myGlycan = new UnknownGlycan();
        		        				}
        		        				glycanFeature.setGlycan(myGlycan);
        		        				myGlycan.setName(glycan.getName());
        		        				myGlycan.setDescription(glycan.getComment());
        		        				myGlycan.setInternalId(glycan.getId() == null ? "" : glycan.getId().toString());
        		        				myFeature.getGlycans().add(glycanFeature);
            					    }
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
    		        			    if (glycan != null && glycan.getName().equalsIgnoreCase("empty")) {
    		        			        // skip this feature
    		        			        continue;
    		        			    }
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
        			    }
        			    else {
        			        // no glycan ratio --> Control
        			        org.grits.toolbox.glycanarray.library.om.feature.Linker linker = LibraryInterface.getLinker(library, probe.getLinker());
                            if (linker != null) {
                                org.glygen.array.persistence.rdf.Feature myFeature = null;
                                if (linker.getName().toLowerCase().contains("grid marker")) {
                                    myFeature = new LandingLight();
                                } else {
                                    myFeature = new ControlFeature();
                                }
                                myFeature.setMetadata(featureMetadata);
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
                                myFeature.setName (feature.getName());
    		        			myFeature.setInternalId(feature.getName());
                                concentrationMap.put(myFeature, spot.getConcentration());
                                features.add(myFeature);
                            }
                            else {
                                // should have been there
                                errorMessage.addError(new ObjectError("linker:" + probe.getLinker(), "NotFound"));
                            }
        			    }
        			} else {
        			    // should have been there
        			    errorMessage.addError(new ObjectError("probe:" + r.getItemId(), "NotFound"));
        			}
        		}
    		} else if (spot.getFeatureId() != null){
    		    // should have been there
    		    errorMessage.addError(new ObjectError("feature:" + spot.getFeatureId(), "NotFound"));
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
	
	@Operation(summary = "List all block layouts for the user", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/listBlocklayouts", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Block layouts retrieved successfully"), 
			@ApiResponse(responseCode="400", description="Invalid request, validation error for arguments", content = {
		            @Content( schema = @Schema(implementation = ErrorMessage.class))}),
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to list block layouts"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error", content = {
    	            @Content( schema = @Schema(implementation = ErrorMessage.class))})})
	public BlockLayoutResultView listBlockLayouts (
			@Parameter(required=true, description="offset for pagination, start from 0", example="0") 
			@RequestParam("offset") Integer offset,
			@Parameter(required=false, description="limit of the number of layouts to be retrieved", example="10") 
			@RequestParam(value="limit", required=false) Integer limit, 
			@Parameter(required=false, description="name of the sort field, defaults to id") 
			@RequestParam(value="sortBy", required=false) String field, 
			@Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
			@RequestParam(value="order", required=false) Integer order, 
			@Parameter (required=false, schema = @Schema(type = "boolean", defaultValue="true"), description="if false, do not load spot details. Default is true (to load all)")
			@RequestParam(required=false, defaultValue = "true", value="loadAll") Boolean loadAll, 
			@Parameter(required=false, description="a filter value to match") 
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

	@Operation(summary = "List all features for the user", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/listFeatures", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Features retrieved successfully"), 
			@ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to list features"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public FeatureListResultView listFeature (
			@Parameter(required=true, description="offset for pagination, start from 0", example="0") 
			@RequestParam("offset") Integer offset,
			@Parameter(required=false, description="limit of the number of features to be retrieved", example="10") 
			@RequestParam(value="limit", required=false) Integer limit, 
			@Parameter(required=false, description="name of the sort field, defaults to id") 
			@RequestParam(value="sortBy", required=false) String field, 
			@Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
			@RequestParam(value="order", required=false) Integer order, 
			@Parameter(required=false, description="a filter value to match") 
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
	
	@Operation(summary = "List all features of given type for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listFeaturesByType", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Features retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to list features"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public FeatureListResultView listFeaturesByType (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of features to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue,
            @Parameter(required=true, description="type of the molecule", 
                    schema = @Schema(type = "string", allowableValues= {"LINKEDGLYCAN", "GLYCOLIPID", "GLYCOPEPTIDE", 
                    "GLYCOPROTEIN", "GPLINKEDGLYCOPEPTIDE", "CONTROL", "NEGATIVE_CONTROL", "COMPOUND", "LANDING_LIGHT"})) 
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

	@Operation(summary = "List all glycans for the user", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/listGlycans", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Glycans retrieved successfully", content = {
            @Content( schema = @Schema(implementation = GlycanListResultView.class))}), 
			@ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to list glycans"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error", content = {
    	            @Content( schema = @Schema(implementation = ErrorMessage.class))})})
	public GlycanListResultView listGlycans (
			@Parameter(required=true, description="offset for pagination, start from 0", example="0") 
			@RequestParam("offset") Integer offset,
			@Parameter(required=false, description="limit of the number of glycans to be retrieved", example="10") 
			@RequestParam(value="limit", required=false) Integer limit, 
			@Parameter(required=false, description="name of the sort field, defaults to id") 
			@RequestParam(value="sortBy", required=false) String field, 
			@Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
			@RequestParam(value="order", required=false) Integer order, 
			@Parameter(required=false, description="a filter value to match") 
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
	
	

    @Operation(summary = "List all glycans for the user and the public ones", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listAllGlycans", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Glycans retrieved successfully", content = {
            @Content( schema = @Schema(implementation = GlycanListResultView.class))}), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to list glycans"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public GlycanListResultView listAllGlycans (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of glycans to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="a filter value to match") 
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
	
	@Operation(summary = "List all linkers for the user and the public ones", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/listAllLinkers", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Linkers retrieved successfully"), 
			@ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to list linkers"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error", content = {
    	            @Content( schema = @Schema(implementation = ErrorMessage.class))})})
	public LinkerListResultView listAllLinkers (
			@Parameter(required=true, description="offset for pagination, start from 0", example="0") 
			@RequestParam("offset") Integer offset,
			@Parameter(required=false, description="limit of the number of linkers to be retrieved", example="10") 
			@RequestParam(value="limit", required=false) Integer limit, 
			@Parameter(required=false, description="name of the sort field, defaults to id") 
			@RequestParam(value="sortBy", required=false) String field, 
			@Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
			@RequestParam(value="order", required=false) Integer order, 
			@Parameter(required=false, description="a filter value to match") 
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
	
	@Operation(summary = "List all linkers for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listLinkers", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Linkers retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to list linkers"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public LinkerListResultView listLinkers (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of linkers to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="a filter value to match") 
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
	
	@Operation(summary = "List all linkers of the given type for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listMoleculesByType", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Linkers retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to list linkers"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public LinkerListResultView listLinkersByType (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of linkers to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @Parameter(required=true, description="type of the molecule", schema = @Schema(type = "string", allowableValues= {"SMALLMOLECULE", "LIPID", "PEPTIDE", "PROTEIN", "OTHER"})) 
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
	
	@Operation(summary = "List all linkers of the given type for the user and the public ones", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listAllMoleculesByType", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Linkers retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to list linkers"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public LinkerListResultView listAllLinkersByType (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of linkers to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @Parameter(required=true, description="type of the molecule", schema = @Schema(type = "string", allowableValues= {"SMALLMOLECULE", "LIPID", "PEPTIDE", "PROTEIN", "OTHER"})) 
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
	
	@Operation(summary = "List all slide layouts for the user", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value="/listSlidelayouts", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Slide layouts retrieved successfully"), 
			@ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to list slide layouts"),
			@ApiResponse(responseCode="415", description="Media type is not supported"),
			@ApiResponse(responseCode="500", description="Internal Server Error", content = {
		            @Content( schema = @Schema(implementation = ErrorMessage.class))})})
	public SlideLayoutResultView listSlideLayouts (
			@Parameter(required=true, description="offset for pagination, start from 0", example="0") 
			@RequestParam("offset") Integer offset,
			@Parameter(required=false, description="limit of the number of layouts to be retrieved", example="10") 
			@RequestParam(value="limit", required=false) Integer limit, 
			@Parameter(required=false, description="name of the sort field, defaults to id") 
			@RequestParam(value="sortBy", required=false) String field, 
			@Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
			@RequestParam(value="order", required=false) Integer order, 
			@Parameter (required=false, schema = @Schema(type = "boolean", defaultValue="true"), description="if false, do not load block details. Default is true (to load all)")
			@RequestParam(required=false, defaultValue = "true", value="loadAll") Boolean loadAll, 
			@Parameter(required=false, description="a filter value to match") 
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
	
	@Operation(summary = "Make given glycan public", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/makeglycanpublic/{glycanId}", method = RequestMethod.POST)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="id of the public glycan"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to modify glycans"),
            @ApiResponse(responseCode="409", description="A glycan with the given name already exists in public repository!"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String makeGlycanPublic (
            @Parameter(required=true, description="id of the glycan to retrieve") 
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
	
	@Operation(summary = "Make given linker public", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/makelinkerpublic/{linkerId}", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="id of the public linker"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to modify linker"),
            @ApiResponse(responseCode="409", description="A linker with the given name already exists in public repository!"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String makeLinkerPublic (
            @Parameter(required=true, description="id of the linker to retrieve") 
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
	
	@Operation(summary = "Make given slide layout public", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/makeslidelayoutpublic/{layoutId}", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="id of the public slide layout"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to modify slide layout"),
            @ApiResponse(responseCode="409", description="A slide layout with the given name already exists in public repository!"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String makeSlideLayoutPublic (
            @Parameter(required=true, description="id of the slide layout to retrieve") 
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
	
	@Operation(summary = "Export glycans into a file", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/exportglycans", method=RequestMethod.GET)
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="confirmation message"), 
            @ApiResponse(responseCode="400", description="Invalid request, file not found, not writable etc."),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to export glycans"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
	public @ResponseBody String exportGlycans (
	        @Parameter(required=false, description="offset for pagination, start from 0", example="0") 
            @RequestParam(value="offset", required=false) Integer offset,
            @Parameter(required=false, description="limit of the number of glycans to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue,
            Principal p) {
	    
	    if (offset == null)
	        offset = 0;
	    if (limit == null) 
	        limit = -1;
	    
	    UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
	    try {
            List<Glycan> myGlycans = glycanRepository.getGlycanByUser(user, offset, limit, null, 0, searchValue);
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
	
	@Operation(summary = "Export linkers into a file", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/exportlinkers", method=RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="confirmation message"), 
            @ApiResponse(responseCode="400", description="Invalid request, file not found, not writable etc."),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to export linkers"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public @ResponseBody String exportLinkers (
            @Parameter(required=true, description="type of the molecule", schema = @Schema(type = "string", allowableValues= {"SMALLMOLECULE", "LIPID", "PEPTIDE", "PROTEIN", "OTHER"})) 
            @RequestParam("type") String moleculeType,
            @Parameter(required=false, description="offset for pagination, start from 0", example="0") 
            @RequestParam(value="offset", required=false) Integer offset,
            @Parameter(required=false, description="limit of the number of glycans to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue,
            Principal p) {
	    if (offset == null)
            offset = 0;
        if (limit == null) 
            limit = -1;
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
            
            List<Linker> myLinkers = linkerRepository.getLinkerByUser(user, offset, limit, null, 0, searchValue, linkerType);
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
	
	@Operation(summary = "Export features into a file", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/exportfeatures", method=RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="confirmation message"), 
            @ApiResponse(responseCode="400", description="Invalid request, file not found, not writable etc."),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to export features"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public @ResponseBody String exportFeatures (
            @Parameter(required=false, description="type of the feature", 
                    schema = @Schema(type = "string", allowableValues= {"LINKEDGLYCAN", "GLYCOLIPID", "GLYCOPEPTIDE", 
                            "GLYCOPROTEIN", "GPLINKEDGLYCOPEPTIDE", "CONTROL", "NEGATIVE_CONTROL", "COMPOUND", "LANDING_LIGHT"})) 
            @RequestParam(value="type", required=false) String type,
            @Parameter(required=false, description="offset for pagination, start from 0", example="0") 
            @RequestParam(value="offset", required=false) Integer offset,
            @Parameter(required=false, description="limit of the number of glycans to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue,
            Principal p) {
        
	    if (offset == null)
            offset = 0;
        if (limit == null) 
            limit = -1;
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        try {
            FeatureType featureType = null;
            if (type != null) {
                try {
                    featureType = FeatureType.valueOf(type);
                    if (featureType == null) {
                        ErrorMessage errorMessage = new ErrorMessage("Incorrect feature type");
                        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                        errorMessage.addError(new ObjectError("type", "NotValid"));
                        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                        throw new IllegalArgumentException("Incorrect feature type", errorMessage);
                    }
                } catch (Exception e) {
                    ErrorMessage errorMessage = new ErrorMessage("Incorrect feature type");
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    errorMessage.addError(new ObjectError("type", "NotValid"));
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                    throw new IllegalArgumentException("Incorrect feature type", errorMessage);
                }
            }
            List<org.glygen.array.persistence.rdf.Feature> myFeatures = null;
            if (featureType != null) {
                myFeatures = featureRepository.getFeatureByUser(user, offset, limit, null, 0, searchValue, featureType, false);
            } else {
                myFeatures = featureRepository.getFeatureByUser(user, offset, limit, null, 0, searchValue);
            }
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
	
	@Operation(summary = "Export slide layout in extended GAL format", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/downloadSlideLayout", method=RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="File generated successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, file cannot be found"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve slide layout"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<Resource> exportSlideLayout (
            @Parameter(required=true, description="id of the slide layout") 
            @RequestParam("slidelayoutid")
            String slidelayoutid,
            @Parameter(required=false, description="the name for downloaded file") 
            @RequestParam(value="filename", required=false)
            String fileName,        
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        
        if (fileName == null || fileName.isEmpty()) {
            fileName = slidelayoutid + ".gal";
        }
        File newFile = new File (uploadDir, "tmp" + fileName);
        
        try {
            SlideLayout layout = layoutRepository.getSlideLayoutById(slidelayoutid, user, true);
            if (layout == null) {
                // check if it is public
                layout = layoutRepository.getSlideLayoutById(slidelayoutid, null, true);
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
	
	@Operation(summary = "Check status for upload file", security = { @SecurityRequirement(name = "bearer-key") })
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
	
	@Operation(summary = "Update given block layout for the user", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value = "/updateBlockLayout", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Block layout updated successfully"), 
			@ApiResponse(responseCode="400", description="Invalid request, validation error"),
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to update block layout"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public Confirmation updateBlockLayout(
			@Parameter(required=true, description="Block layout to be updated, id is required, name and comment can be updated only") 
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
			// check if it exists in the repository
            BlockLayout existing = layoutRepository.getBlockLayoutById(layout.getId(), user, false);
            if (existing == null) {
                errorMessage.addError(new ObjectError("layout", "NotFound"));
            }
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
	
	@Operation(summary = "Update given glycan for the user", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value = "/updateGlycan", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Glycan updated successfully"), 
			@ApiResponse(responseCode="400", description="Invalid request, validation error"),
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to update glycans"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public Confirmation updateGlycan(
			@Parameter(required=true, description="Glycan with updated fields") 
			@RequestBody Glycan glycanView, 
			@RequestParam(value="changeSummary", required=false)
            String changeSummary,
            @Parameter(required=false, description="field that has changed, can provide multiple") 
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
			Glycan existing = glycanRepository.getGlycanById(glycanView.getId(), user);
			if (existing == null) {
                errorMessage.addError(new ObjectError("glycan", "NotFound"));
            }

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
	
	@Operation(summary = "Update given linker for the user", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value = "/updateLinker", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Linker updated successfully"), 
			@ApiResponse(responseCode="400", description="Invalid request, validation error"),
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to update linkers"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public Confirmation updateLinker(
			@Parameter(required=true, description="Linker to be updated, id is required, only name and comment can be updated") 
			@RequestBody Linker linkerView, 
			@Parameter(required=false, description="summary of the changes") 
            @RequestParam(value="changeSummary", required=false)
            String changeSummary,
            @Parameter(required=false, description="field that has changed, can provide multiple") 
            @RequestParam(value="changedField", required=false)
            List<String> changedFields,
            @RequestParam(value="unknown", required=false)
            @Parameter(required=false, description="true, if the linker is of unknown type. The default is false")
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
			Linker existing = linkerRepository.getLinkerById(linkerView.getId(), user);
			if (existing == null) {
                errorMessage.addError(new ObjectError("linker", "NotFound"));
            }

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
	
	
	@Operation(summary = "Update given feature for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/updateFeature", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Feature updated successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to update feature"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation updateFeature(
            @Parameter(required=true, description="Feature to be updated, id and type are required, only name and internalId can be updated") 
            @RequestBody org.glygen.array.persistence.rdf.Feature feature, 
            @Parameter(required=false, description="summary of the changes") 
            @RequestParam(value="changeSummary", required=false)
            String changeSummary,
            @Parameter(required=false, description="field that has changed, can provide multiple") 
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
            org.glygen.array.persistence.rdf.Feature f = featureRepository.getFeatureById(feature.getId(), user);
            if (f == null) {
                errorMessage.addError(new ObjectError("feature", "NotFound"));
            }
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
	
	@Operation(summary = "Update given slide layout for the user", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value = "/updateSlideLayout", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="Slide layout updated successfully"), 
			@ApiResponse(responseCode="400", description="Invalid request, validation error"),
			@ApiResponse(responseCode="401", description="Unauthorized"),
			@ApiResponse(responseCode="403", description="Not enough privileges to update slide layout"),
    		@ApiResponse(responseCode="415", description="Media type is not supported"),
    		@ApiResponse(responseCode="500", description="Internal Server Error")})
	public Confirmation updateSlideLayout(
			@Parameter(required=true, description="Slide layout to be updated, id is required, name and comment can be updated only") 
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
			// check if it exists in the repository
            SlideLayout existing = layoutRepository.getSlideLayoutById(layout.getId(), user, false);
            if (existing == null) {
                errorMessage.addError(new ObjectError("layout", "NotFound"));
            }
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
	
	@Operation(summary = "Upload file", security = { @SecurityRequirement(name = "bearer-key") })
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
                t_image = glycanWorkspace.getGlycanRenderer().getImage(glycanObject, true, false, true, 1.0D);
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
                            t_image = glycanWorkspace.getGlycanRenderer().getImage(glycanObject, true, false, true, 1.0D);
                        }
                    } catch (Exception e1) {
                        logger.error ("Glycan image cannot be generated from WURCS", e);
                    }
                }
            }
            
        }
        return t_image;
    }
	
	public static void calculateChecksum (FileWrapper file) {
	    // calculate checksum
        try (InputStream istream = Files.newInputStream(Paths.get(file.getFileFolder() + File.separator + file.getIdentifier()))) {
            String md5 = DigestUtils.sha256Hex(istream);
            Checksum checksum = new Checksum();
            checksum.setChecksum(md5);
            checksum.setType("sha-256");
            file.setChecksum(checksum);
        } catch (IOException e) {
            logger.error("Failed to calculate checksum for file " + file.getIdentifier());
        }
	}
	
	private static  ReducingEndType getReducingEnd (String glycoCT) {
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
