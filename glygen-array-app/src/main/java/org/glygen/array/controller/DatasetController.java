package org.glygen.array.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.apache.commons.io.IOUtils;
import org.glygen.array.config.SesameTransactionConfig;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.GraphPermissionEntity;
import org.glygen.array.persistence.SettingEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.GraphPermissionRepository;
import org.glygen.array.persistence.dao.SettingsRepository;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.Block;
import org.glygen.array.persistence.rdf.Creator;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.LinkerType;
import org.glygen.array.persistence.rdf.Publication;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.data.ArrayDataset;
import org.glygen.array.persistence.rdf.data.ChangeLog;
import org.glygen.array.persistence.rdf.data.ChangeType;
import org.glygen.array.persistence.rdf.data.FileWrapper;
import org.glygen.array.persistence.rdf.data.FutureTask;
import org.glygen.array.persistence.rdf.data.FutureTaskStatus;
import org.glygen.array.persistence.rdf.data.Grant;
import org.glygen.array.persistence.rdf.data.Image;
import org.glygen.array.persistence.rdf.data.Intensity;
import org.glygen.array.persistence.rdf.data.Measurement;
import org.glygen.array.persistence.rdf.data.PrintedSlide;
import org.glygen.array.persistence.rdf.data.ProcessedData;
import org.glygen.array.persistence.rdf.data.RawData;
import org.glygen.array.persistence.rdf.data.Slide;
import org.glygen.array.persistence.rdf.data.StatisticalMethod;
import org.glygen.array.persistence.rdf.metadata.AssayMetadata;
import org.glygen.array.persistence.rdf.metadata.DataProcessingSoftware;
import org.glygen.array.persistence.rdf.metadata.Description;
import org.glygen.array.persistence.rdf.metadata.Descriptor;
import org.glygen.array.persistence.rdf.metadata.DescriptorGroup;
import org.glygen.array.persistence.rdf.metadata.FeatureMetadata;
import org.glygen.array.persistence.rdf.metadata.ImageAnalysisSoftware;
import org.glygen.array.persistence.rdf.metadata.MetadataCategory;
import org.glygen.array.persistence.rdf.metadata.PrintRun;
import org.glygen.array.persistence.rdf.metadata.Printer;
import org.glygen.array.persistence.rdf.metadata.Sample;
import org.glygen.array.persistence.rdf.metadata.ScannerMetadata;
import org.glygen.array.persistence.rdf.metadata.SlideMetadata;
import org.glygen.array.persistence.rdf.metadata.SpotMetadata;
import org.glygen.array.persistence.rdf.template.DescriptionTemplate;
import org.glygen.array.persistence.rdf.template.DescriptorGroupTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplateType;
import org.glygen.array.service.ArrayDatasetRepository;
import org.glygen.array.service.ArrayDatasetRepositoryImpl;
import org.glygen.array.service.AsyncService;
import org.glygen.array.service.FeatureRepository;
import org.glygen.array.service.GlycanRepository;
import org.glygen.array.service.GlycanRepositoryImpl;
import org.glygen.array.service.GlygenArrayRepository;
import org.glygen.array.service.GlygenArrayRepositoryImpl;
import org.glygen.array.service.LayoutRepository;
import org.glygen.array.service.LinkerRepository;
import org.glygen.array.service.MetadataRepository;
import org.glygen.array.service.MetadataTemplateRepository;
import org.glygen.array.util.ExclusionInfoParser;
import org.glygen.array.util.MetadataImportExportUtil;
import org.glygen.array.util.parser.ProcessedDataParser;
import org.glygen.array.util.parser.RawdataParser;
import org.glygen.array.util.pubmed.DTOPublication;
import org.glygen.array.util.pubmed.PubmedUtil;
import org.glygen.array.view.AllMetadataView;
import org.glygen.array.view.ArrayDatasetListView;
import org.glygen.array.view.AsyncBatchUploadResult;
import org.glygen.array.view.BatchGlycanFileType;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.glygen.array.view.ImportMetadataResultView;
import org.glygen.array.view.MetadataError;
import org.glygen.array.view.MetadataImportInput;
import org.glygen.array.view.MetadataListResultView;
import org.glygen.array.view.PrintedSlideListView;
import org.glygen.array.view.SlideLayoutError;
import org.glygen.array.view.StatisticsView;
import org.glygen.array.view.User;
import org.glygen.array.view.UserStatisticsView;
import org.glygen.array.view.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.media.MediaHttpUploader.UploadState;

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
public class DatasetController {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    @Autowired
    ArrayDatasetRepository datasetRepository;
    
    @Autowired
    SettingsRepository settingsRepository;
    
    @Autowired
    GlycanRepository glycanRepository;
    
    @Autowired
    LinkerRepository linkerRepository;
    
    @Autowired
    LayoutRepository layoutRepository;
    
    @Autowired
    FeatureRepository featureRepository;
    
    @Autowired
    MetadataTemplateRepository templateRepository;
    
    @Autowired
    MetadataRepository metadataRepository;
    
    @Autowired
    GraphPermissionRepository permissionRepository;
    
    @Autowired
    @Qualifier("glygenArrayRepositoryImpl")
    GlygenArrayRepository repository;
    
    @Autowired
    UserRepository userRepository;
    
    @Value("${spring.file.uploaddirectory}")
    String uploadDir;
    
    @Value("${spring.file.imagedirectory}")
    String imageLocation;
    
    @Value("${glygen.frontend.scheme}")
    String scheme;
    
    @Value("${glygen.frontend.host}")
    String host;
    
    @Value("${glygen.frontend.basePath}")
    String basePath;
    
    @Autowired
    ResourceLoader resourceLoader;
    
    @Autowired
    Validator validator;
    
    @Autowired
    AsyncService parserAsyncService;
    
    @Operation(summary = "Add given array dataset  for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addDataset", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added array dataset"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to register array datasets"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String addDataset (
            @Parameter(required=true, description="Array dataset to be added") 
            @RequestBody ArrayDataset dataset, Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        // validate first
        if (validator != null) {
            if  (dataset.getName() != null) {
                Set<ConstraintViolation<ArrayDataset>> violations = validator.validateValue(ArrayDataset.class, "name", dataset.getName());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("name", "LengthExceeded"));
                }       
            }
            
            if  (dataset.getDescription() != null) {
                Set<ConstraintViolation<ArrayDataset>> violations = validator.validateValue(ArrayDataset.class, "description", dataset.getDescription());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("description", "LengthExceeded"));
                }       
            }
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        
        if (dataset.getName() == null || dataset.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        if (dataset.getSample() == null || (dataset.getSample().getId() == null && dataset.getSample().getUri() == null && dataset.getSample().getName() == null)) {
            errorMessage.addError(new ObjectError("sample", "NoEmpty"));
        }
        
        // check if sample exists!
        if (dataset.getSample() != null) {
            try {
                String uri = dataset.getSample().getUri();
                if (uri == null) {
                    if (dataset.getSample().getId() != null) {
                        uri = ArrayDatasetRepositoryImpl.uriPrefix + dataset.getSample().getId();
                    }
                }
                if (uri != null) {
                    Sample existing = metadataRepository.getSampleFromURI(uri, user);
                    if (existing == null) {
                        errorMessage.addError(new ObjectError("sample", "NotFound"));
                    } else {
                        dataset.setSample(existing);
                    }
                } else if (dataset.getSample().getName() != null) {
                    Sample existing = metadataRepository.getSampleByLabel(dataset.getSample().getName(), user);
                    if (existing == null) {
                        errorMessage.addError(new ObjectError("sample", "NotFound"));
                    } else {
                        dataset.setSample(existing);
                    }
                }
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the sample", e);
            }
        }
        
        String fileFolder = uploadDir;
        if (dataset.getFiles() != null && !dataset.getFiles().isEmpty()) {
            // check the existence of the files and move them to the dataset folder
            for (FileWrapper file: dataset.getFiles()) {
                if (file.getFileFolder() != null)
                    fileFolder = file.getFileFolder();
                File newFile = new File (fileFolder, file.getIdentifier());
                if (!newFile.exists()) {
                    errorMessage.addError(new ObjectError("file" + file.getIdentifier(), "NotFound"));
                }
            }
        }
        
        // check for duplicate name
        try {
            if (dataset.getName() != null) {
                ArrayDataset existing = datasetRepository.getArrayDatasetByLabel(dataset.getName().trim(), false, user);
                if (existing != null) {
                    errorMessage.addError(new ObjectError("name", "Duplicate"));
                }
            }
        } catch (SparqlException | SQLException e2) {
            throw new GlycanRepositoryException("Error checking for duplicate array dataset", e2);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid array dataset information", errorMessage);
        
        try {
            Set<String> duplicateCheck = new HashSet<String>();
            if (dataset.getPublications() != null && !dataset.getPublications().isEmpty()) {
                List<Publication> publications = new ArrayList<Publication>();
                for (Publication pub: dataset.getPublications()) {
                    // check and remove duplicates
                    if (!duplicateCheck.contains(pub.getPubmedId()+"")) {
                        publications.add(pub);
                        duplicateCheck.add(pub.getPubmedId()+"");
                    }
                }
                dataset.setPublications(publications);
            }
            duplicateCheck.clear();
            if (dataset.getGrants() != null && !dataset.getGrants().isEmpty()) {
                List<Grant> grants = new ArrayList<Grant>();
                for (Grant grant: dataset.getGrants()) {
                    // check and remove duplicates
                    if (!duplicateCheck.contains(grant.getIdentifier())) {
                        grants.add(grant);
                        duplicateCheck.add(grant.getIdentifier());
                    }
                }
                dataset.setGrants(grants);
            }
            
            if (dataset.getKeywords() != null && !dataset.getKeywords().isEmpty()) {
                List<String> keywords = new ArrayList<String>();
                for (String keyword: dataset.getKeywords()) {
                    // check and remove duplicates
                    if (!keywords.contains(keyword)) {
                        keywords.add(keyword);
                    }
                }
                dataset.setKeywords(keywords);
            }
            duplicateCheck.clear();
            if (dataset.getCollaborators() != null && !dataset.getCollaborators().isEmpty()) {
                List<Creator> collaborators = new ArrayList<Creator>();
                for (Creator collab: dataset.getCollaborators()) {
                    // check and remove duplicates
                    if (!duplicateCheck.contains(collab.getName())) {
                        collaborators.add(collab);
                        duplicateCheck.add(collab.getName());
                    }
                }
                dataset.setCollaborators(collaborators);
            }
             
            String uri = datasetRepository.addArrayDataset(dataset, user);    
            String id = uri.substring(uri.lastIndexOf("/")+1);
            
            // save the files with the dataset
            // create a folder for the experiment, if it does not exists, and move the file into that folder
            File experimentFolder = new File (uploadDir + File.separator + id);
            if (!experimentFolder.exists()) {
                experimentFolder.mkdirs();
            }
            for (FileWrapper file: dataset.getFiles()) {
                    File f = new File (fileFolder, file.getIdentifier());
                    File newFile = new File(experimentFolder + File.separator + file.getIdentifier());
                    if (!f.renameTo (newFile)) { 
                        throw new GlycanRepositoryException("File cannot be moved to the dataset folder");
                    } 
                    file.setFileFolder(experimentFolder.getAbsolutePath());
                    file.setFileSize(newFile.length());
                    file.setCreatedDate(new Date());
                    file.setDrsId(file.getIdentifier().substring(0, file.getIdentifier().lastIndexOf(".")));
                    file.setExtension(file.getIdentifier().substring(file.getIdentifier().lastIndexOf(".")+1));
                    GlygenArrayController.calculateChecksum (file);
                
            }
            datasetRepository.updateArrayDataset(dataset, user);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Array dataset cannot be added for user " + p.getName(), e);
        }
    } 
    
    @Operation(summary = "Add given publication to the dataset for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addPublication", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added publication"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to add publications"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String addPublication (
            @Parameter(required=true, description="Publication to be added.")
            @RequestBody Publication publication, 
            @Parameter(required=true, description="id of the array dataset (must already be in the repository) to add the publication") 
            @RequestParam("arraydatasetId")
            String datasetId,  
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserEntity owner = user;
        ArrayDataset dataset = null;
        // check if the dataset with the given id exists
        try {
            dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, user);
            if (dataset == null) {
                // check if the user can access this dataset as a co-owner
                String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                if (coOwnedGraph != null) {
                    UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                    if (originalUser != null) {
                        dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, originalUser);
                        owner = originalUser;
                    }
                }
            }
            if (dataset == null)
                errorMessage.addError(new ObjectError("dataset", "NotFound")); 
            
            // check for duplicates
            if (dataset.getPublications() != null) {
                for (Publication pub: dataset.getPublications()) {
                    if (pub.getPubmedId() != null && pub.getPubmedId().equals(publication.getPubmedId())) {
                        // duplicate
                        errorMessage.addError(new ObjectError("pubmedid", "Duplicate"));
                    }
                }
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
        }
        
        
        // check if the details of the publication needs to be retrieved from pubmed
        if (publication.getAuthors() == null && publication.getTitle() == null) {
            if (publication.getPubmedId() != null) {
                Integer pubmedId = publication.getPubmedId();
                PubmedUtil util = new PubmedUtil();
                try {
                    DTOPublication pub = util.createFromPubmedId(pubmedId);
                    publication =  UtilityController.getPublicationFrom(pub);
                } catch (Exception e) {
                    errorMessage.addError(new ObjectError("pubmedid", "NotValid"));
                    
                }
            } else {
                errorMessage.addError(new ObjectError("title", "NoEmpty"));
                errorMessage.addError(new ObjectError("authors", "NoEmpty"));
            }
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid publication/dataset information", errorMessage);
        
        try {
            dataset.getPublications().add(publication);
            datasetRepository.updateArrayDataset(dataset, owner);
            if (publication.getId() != null) {
                return publication.getId();
            } 
            return null;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("The publication cannot be added for user " + p.getName(), e);
        }
    }
    
    @Operation(summary = "Add given file to the dataset for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addFile", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"}, produces= {"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return confirmation"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to add files"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation addFile (
            @Parameter(required=true, description="File to be added.")
            @RequestBody FileWrapper file, 
            @Parameter(required=true, description="id of the array dataset (must already be in the repository) to add the file") 
            @RequestParam("arraydatasetId")
            String datasetId,  
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserEntity owner = user;
        ArrayDataset dataset = null;
        String fileFolder = uploadDir;
        // check if the dataset with the given id exists
        try {
            dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, user);
            if (dataset == null) {
                // check if the user can access this dataset as a co-owner
                String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                if (coOwnedGraph != null) {
                    UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                    if (originalUser != null) {
                        dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, originalUser);
                        owner = originalUser;
                    }
                }
            }
            if (dataset == null)
                errorMessage.addError(new ObjectError("dataset", "NotFound")); 
            
            // check for duplicates
            if (dataset.getFiles() != null) {
                for (FileWrapper f: dataset.getFiles()) {
                    if (file.getIdentifier().equalsIgnoreCase(f.getIdentifier())) {
                        // duplicate
                        errorMessage.addError(new ObjectError("file", "Duplicate"));
                    }
                }
            }
            
            if (file.getFileFolder() != null)
                fileFolder = file.getFileFolder();
            File newFile = new File (fileFolder, file.getIdentifier());
            if (!newFile.exists()) {
                errorMessage.addError(new ObjectError("file" + file.getIdentifier(), "NotFound"));
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid array dataset information", errorMessage);
        
        try {
            // save the files with the dataset
            // create a folder for the experiment, if it does not exists, and move the file into that folder
            File experimentFolder = new File (uploadDir + File.separator + datasetId);
            if (!experimentFolder.exists()) {
                experimentFolder.mkdirs();
            }
            
            File f = new File (fileFolder, file.getIdentifier());
            File newFile = new File(experimentFolder + File.separator + file.getIdentifier());
            if (!f.renameTo (newFile)) { 
                throw new GlycanRepositoryException("File cannot be moved to the dataset folder");
            } 
            file.setFileFolder(experimentFolder.getAbsolutePath());
            file.setFileSize(newFile.length());
            file.setCreatedDate(new Date());
            file.setDrsId(file.getIdentifier().substring(0, file.getIdentifier().lastIndexOf(".")));
            file.setExtension(file.getIdentifier().substring(file.getIdentifier().lastIndexOf(".")+1));
            GlygenArrayController.calculateChecksum (file);
            dataset.getFiles().add(file);
                
            datasetRepository.updateArrayDataset(dataset, owner);
            return new Confirmation("File added successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Array dataset cannot be added for user " + p.getName(), e);
        }
    }
    
    @Operation(summary = "Add given grant to the dataset for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addGrant", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added grant"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to add grants"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String addGrant (
            @Parameter(required=true, description="Grant to be added.")
            @RequestBody Grant grant, 
            @Parameter(required=true, description="id of the array dataset (must already be in the repository) to add the grant") 
            @RequestParam("arraydatasetId")
            String datasetId,  
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        // check if the dataset with the given id exists
        UserEntity owner = user;
        ArrayDataset dataset = null;
        try {
            dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, user);
            if (dataset == null) {
                // check if the user can access this dataset as a co-owner
                String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                if (coOwnedGraph != null) {
                    UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                    if (originalUser != null) {
                        dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, originalUser);
                        owner = originalUser;
                    }
                }
            }
            if (dataset == null)
                errorMessage.addError(new ObjectError("dataset", "NotFound"));
            // check for duplicates
            if (dataset.getGrants() != null) {
                for (Grant gr: dataset.getGrants()) {
                    if (gr.getIdentifier() != null && gr.getIdentifier().equalsIgnoreCase(grant.getIdentifier())) {
                     // duplicate
                        errorMessage.addError(new ObjectError("grant", "Duplicate"));
                    }
                }
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid publication/dataset information", errorMessage);
        
        try {
            dataset.getGrants().add(grant);
            datasetRepository.updateArrayDataset(dataset, owner);
            if (grant.getUri() != null) {
                String id = grant.getUri().substring(grant.getUri().lastIndexOf("/")+1);
                return id;
            } 
            return null;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("The grant cannot be added for user " + p.getName(), e);
        }
    }
    
    @Operation(summary="Retrieving all keywords from the repository", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/getallkeywords", method=RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses(value= {@ApiResponse(responseCode="200", description="list of existing keywords in the repository"),
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve keywords"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Set<String> getKeywords(){
        try {
            Set<String> keywords = new HashSet<String>();
            keywords.add("Service work");
            keywords.add("Published");
            keywords.add("Unpublished");
            keywords.add("Historical data");
            keywords.addAll(datasetRepository.getAllKeywords());
            return keywords;
        } catch (SparqlException | SQLException e) {
            ErrorMessage errorMessage = new ErrorMessage("Error retrieving keywords from the repository");
            errorMessage.addError(new ObjectError("keyword", e.getMessage()));
            errorMessage.setErrorCode(ErrorCodes.INTERNAL_ERROR);
            errorMessage.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            throw errorMessage;
        }
    }
    
    @Operation(summary="Retrieving all funding organizations from the repository", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/getallfundingorganizations", method=RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses(value= {@ApiResponse(responseCode="200", description="list of existing funding organizations in the repository"),
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve funding organizations"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Set<String> getFundingOrganizations(){
        try {
            Set<String> orgs = new HashSet<String>();
            orgs.add("NIH");
            orgs.add("FDA");
            orgs.add("DOI");
            orgs.addAll(datasetRepository.getAllFundingOrganizations());
            if (!orgs.contains("Other")) {
                orgs.add("Other");
            }
            return orgs;
        } catch (SparqlException e) {
            ErrorMessage errorMessage = new ErrorMessage("Error retrieving funding organizations from the repository");
            errorMessage.addError(new ObjectError("fundingorganization", e.getMessage()));
            errorMessage.setErrorCode(ErrorCodes.INTERNAL_ERROR);
            errorMessage.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            throw errorMessage;
        }
    }
    
    @Operation(summary = "Add given keyword to the dataset for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addKeyword", method = RequestMethod.POST, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return confirmation"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to add keywords"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation addKeyword (
            @Parameter(required=true, description="Keyword to be added.")
            @RequestParam("keyword") String keyword, 
            @Parameter(required=true, description="id of the array dataset (must already be in the repository) to add the keyword") 
            @RequestParam("arraydatasetId")
            String datasetId,  
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        // check if the dataset with the given id exists
        UserEntity owner = user;
        ArrayDataset dataset = null;
        try {
            dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, user);
            if (dataset == null) {
                // check if the user can access this dataset as a co-owner
                String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                if (coOwnedGraph != null) {
                    UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                    if (originalUser != null) {
                        dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, originalUser);
                        owner = originalUser;
                    }
                }
            }
            if (dataset == null)
                errorMessage.addError(new ObjectError("dataset", "NotFound"));
            // check for duplicates
            if (dataset.getKeywords() != null) {
                for (String k: dataset.getKeywords()) {
                    if (k.equalsIgnoreCase(keyword)) {
                        // duplicate
                        errorMessage.addError(new ObjectError("keyword", "Duplicate"));
                    }
                }
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid publication/dataset information", errorMessage);
        
        try {
            dataset.getKeywords().add(keyword);
            datasetRepository.updateArrayDataset(dataset, owner);
            return new Confirmation("Keyword added successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("The grant cannot be added for user " + p.getName(), e);
        }
    }
    
    @Operation(summary = "Add given collaborator to the dataset for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addCollaborator", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return confirmation"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to add collaborators"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation addCollaborator (
            @Parameter(required=true, description="Collaborator user to be added.")
            @RequestBody User collab, 
            @Parameter(required=true, description="id of the array dataset (must already be in the repository) to add the collaborator") 
            @RequestParam("arraydatasetId")
            String datasetId,  
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserEntity owner = user;
        // check if the dataset with the given id exists
        ArrayDataset dataset = null;
        try {
            dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, user);
            if (dataset == null) {
                // check if the user can access this dataset as a co-owner
                String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                if (coOwnedGraph != null) {
                    UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                    if (originalUser != null) {
                        dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, originalUser);
                        owner = originalUser;
                    }
                }
            }
            if (dataset == null)
                errorMessage.addError(new ObjectError("dataset", "NotFound"));
            // check for duplicates
            if (dataset.getCollaborators() != null) {
                for (Creator c: dataset.getCollaborators()) {
                    if (c.getName() != null && c.getName().equalsIgnoreCase(collab.getUserName())) {
                        // duplicate
                        errorMessage.addError(new ObjectError("collaborator", "Duplicate"));
                    }
                }
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
        }
        
        
        // check if collaborator exists
        UserEntity collaboratorEntity = userRepository.findByUsernameIgnoreCase(collab.getUserName());
        if (collaboratorEntity == null) {
            errorMessage.addError(new ObjectError("collab", "NotFound"));
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid collaborator information", errorMessage);
        
        try {
            Creator collaborator = new Creator();
            collaborator.setName(collaboratorEntity.getUsername());
            collaborator.setFirstName(collaboratorEntity.getFirstName());
            collaborator.setLastName(collaboratorEntity.getLastName());
            collaborator.setAffiliation(collaboratorEntity.getAffiliation());
            collaborator.setUserId(collaboratorEntity.getUserId());
            collaborator.setGroupName(collaboratorEntity.getGroupName());
            collaborator.setDepartment(collaboratorEntity.getDepartment());
            dataset.getCollaborators().add(collaborator);
            datasetRepository.updateArrayDataset(dataset, owner);
            return new Confirmation("Collaborator added successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("The collaborator cannot be added for user " + p.getName(), e);
        }
    }
    
    @Operation(summary = "Add the given user as a co-owner to the given dataset", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addCoowner", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return confirmation message"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to add co-owners"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation addCoownerToDataset (
            @Parameter(required=true, description="User to be added.")
            @RequestBody User coowner, 
            @Parameter(required=true, description="id of the array dataset (must already be in the repository) to add the co-owner") 
            @RequestParam("arraydatasetId")
            String datasetId,  
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        // check if the dataset with the given id exists
        try {
            ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, user);
            if (dataset == null) {
                errorMessage.addError(new ObjectError("dataset", "NotFound"));
            }
            
            UserEntity coOwner = userRepository.findByUsernameIgnoreCase(coowner.getUserName());
            if (coOwner == null) {
                errorMessage.addError(new ObjectError("coowner", "NotFound"));
            }
            // check for duplicates
            List<GraphPermissionEntity> entities = permissionRepository.findByResourceIRI(GlygenArrayRepositoryImpl.uriPrefix + datasetId.trim());
            for (GraphPermissionEntity e: entities) {
                if (e.getUser() != null && e.getUser().getUsername().equals (coowner.getUserName())) {
                    errorMessage.addError(new ObjectError("coowner", "Duplicate"));
                }
            }
            
            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                throw new IllegalArgumentException("Invalid Input", errorMessage);
            }
            //TODO add change log and save it
            datasetRepository.addCowner(coOwner, dataset.getUri(), user);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
        }
        
        return new Confirmation("Co-owner added successfully", HttpStatus.OK.value());
    }
    
    @Operation(summary = "Delete the given user as a co-owner from the given dataset", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deleteCoowner/{username}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return confirmation if co-owner deleted successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete co-owners"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation removeCoownerFromDataset (
            @Parameter(required=true, description="User to be removed.")
//            @RequestBody User coowner,
            @PathVariable("username") String coowner,
            @Parameter(required=true, description="id of the array dataset (must already be in the repository) to remove the co-owner") 
            @RequestParam("datasetId")
            String datasetId,  
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        // check if the dataset with the given id exists
        try {
            ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, user);
            if (dataset == null) {
                errorMessage.addError(new ObjectError("dataset", "NotFound"));
            }
            
//            UserEntity coOwner = userRepository.findByUsernameIgnoreCase(coowner.getUserName());
            UserEntity coOwner = userRepository.findByUsernameIgnoreCase(coowner);
            if (coOwner == null) {
                errorMessage.addError(new ObjectError("coowner", "NotFound"));
            }
            
            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                throw new IllegalArgumentException("Invalid Input", errorMessage);
            }
            
            //TODO add change log and save it
            datasetRepository.deleteCoowner(coOwner, dataset.getUri(), user);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
        }
        
        return new Confirmation("Co-owner deleted successfully", HttpStatus.OK.value());
    }
    
    @Operation(summary = "Add given slide to the dataset for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addSlide", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added slide"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to register datasets"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String addSlide (
            @Parameter(required=true, description="Slide to be added. Slide"
                    + " should have an existing printedSlide (specified by name or uri or id), "
                    + "and it should have an existing AssayMetadata (specified by name, id or uri, "
                    + " images are ignored. You should use addImage to add the images and other data")
            @RequestBody Slide slide, 
            @Parameter(required=true, description="id of the array dataset (must already be in the repository) to add the slide") 
            @RequestParam("arraydatasetId")
            String datasetId,  
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        boolean allowPartialData = false;
        if (user.hasRole("ROLE_DATA"))
            allowPartialData = true;
        
        UserEntity owner = user;
        // check if the dataset with the given id exists
        try {
            ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, user);
            if (dataset == null) {
                // check if the user can access this dataset as a co-owner
                String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                if (coOwnedGraph != null) {
                    UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                    if (originalUser != null) {
                        dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, originalUser);
                        owner = originalUser;
                    }
                }
            }
            if (dataset == null)
                errorMessage.addError(new ObjectError("dataset", "NotFound"));
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
        }
        
        //check if the dataset is public
        try {
            String publicID = datasetRepository.getDatasetPublicId(datasetId.trim());
            if (publicID != null) {
                // this is major change, do not allow
                errorMessage.addError(new ObjectError("dataset", "Public"));
            }
        } catch (SparqlException e) {
            throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
        }
        
        if (slide.getMetadata() == null) {
            if (!allowPartialData)
                errorMessage.addError(new ObjectError("assayMetadata", "NoEmpty"));
        } else  {
            try {
                if (slide.getMetadata().getName() != null) {
                    AssayMetadata metadata = metadataRepository.getAssayMetadataByLabel(slide.getMetadata().getName(), owner);
                    if (metadata == null) {
                        errorMessage.addError(new ObjectError("assayMetadata", "NotFound"));
                    } else {
                        slide.setMetadata(metadata);
                    }
                } else if (slide.getMetadata().getUri() != null) {
                    AssayMetadata metadata = metadataRepository.getAssayMetadataFromURI(slide.getMetadata().getUri(), owner);
                    if (metadata == null) {
                        errorMessage.addError(new ObjectError("assayMetadata", "NotFound"));
                    } else {
                        slide.setMetadata(metadata);
                    }
                } else if (slide.getMetadata().getId() != null) {
                    AssayMetadata metadata = 
                            metadataRepository.getAssayMetadataFromURI(ArrayDatasetRepositoryImpl.uriPrefix + slide.getMetadata().getId(), owner);
                    if (metadata == null) {
                        errorMessage.addError(new ObjectError("assayMetadata", "NotFound"));
                    } else {
                        slide.setMetadata(metadata);
                    }
                }
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the assay metadata", e);
            }
        }
        
        if (slide.getPrintedSlide() == null) {
            errorMessage.addError(new ObjectError("printedSlide", "NoEmpty"));
        } else {
            // check if the printed slide exists
            try {
                String printedSlideUri = slide.getPrintedSlide().getUri();
                if (printedSlideUri != null) {
                    PrintedSlide existing = datasetRepository.getPrintedSlideFromURI(printedSlideUri, owner);
                    if (existing == null) {
                        errorMessage.addError(new ObjectError("printedSlide", "NotFound"));
                    } else {
                        slide.setPrintedSlide(existing);
                    }
                } else if (slide.getPrintedSlide().getId() != null) {
                    String printedSlideId = slide.getPrintedSlide().getId();
                    // check locally first
                    PrintedSlide existing = datasetRepository.getPrintedSlideFromURI(GlygenArrayRepositoryImpl.uriPrefix + printedSlideId, owner);
                    if (existing == null) { // check public repo
                        existing = datasetRepository.getPrintedSlideFromURI(GlygenArrayRepositoryImpl.uriPrefixPublic + printedSlideId, owner);
                    }
                    if (existing == null) {
                        errorMessage.addError(new ObjectError("printedSlide", "NotFound"));
                    } else {
                        slide.setPrintedSlide(existing);
                    }
                } else if (slide.getPrintedSlide().getName() != null) {
                    PrintedSlide existing = datasetRepository.getPrintedSlideByLabel(slide.getPrintedSlide().getName(), false, owner);
                    if (existing == null) {
                        // try to see if it exists in public graph
                        existing = datasetRepository.getPrintedSlideByLabel(slide.getPrintedSlide().getName(), false, null);
                        if (existing == null) {
                            errorMessage.addError(new ObjectError("printedSlide", "NotFound"));
                        } else {
                            slide.setPrintedSlide(existing);
                        }
                    } else {
                        slide.setPrintedSlide(existing);
                    }
                }
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the printed slide", e);
            }
        }
        
        /*if (slide.getImages() != null && !slide.getImages().isEmpty()) {
            // create a folder for the experiment, if it does not exists, and move the file into that folder
            File experimentFolder = new File (uploadDir + File.separator + datasetId);
            if (!experimentFolder.exists()) {
                experimentFolder.mkdirs();
            }
            for (Image image: slide.getImages()) {
                // check to make sure the image is specified and image file is in uploads folder
                if (image.getFile() != null && image.getFile().getIdentifier() != null) {
                    // move it to the dataset folder
                    File imageFile = new File (uploadDir, image.getFile().getIdentifier());
                    if (!imageFile.exists()) {
                        errorMessage.addError(new ObjectError("imageFile", "NotFound"));
                    }
                    else {
                        File newFile = new File(experimentFolder + File.separator + image.getFile().getIdentifier());
                        if(!imageFile.renameTo (newFile)) { 
                            throw new GlycanRepositoryException("Image file cannot be moved to the dataset folder");
                        } 
                        image.getFile().setFileFolder(uploadDir + File.separator + datasetId);
                        image.getFile().setFileSize(newFile.length());
                    }
                    
                } else {
                    if (!allowPartialData)
                        errorMessage.addError(new ObjectError("imageFile", "NoEmpty"));
                }
                // check the metadata
                if (image.getScanner() == null) {
                    if (!allowPartialData) errorMessage.addError(new ObjectError("scannerMetadata", "NoEmpty"));
                } else {
                    try {
                        if (image.getScanner().getName() != null) {
                            ScannerMetadata metadata = metadataRepository.getScannerMetadataByLabel(image.getScanner().getName(), owner);
                            if (metadata == null) {
                                errorMessage.addError(new ObjectError("scannerMetadata", "NotFound"));
                            } else {
                                image.setScanner(metadata);
                            }
                        } else if (image.getScanner().getUri() != null) {
                            ScannerMetadata metadata = metadataRepository.getScannerMetadataFromURI(image.getScanner().getUri(), owner);
                            if (metadata == null) {
                                errorMessage.addError(new ObjectError("scannerMetadata", "NotFound"));
                            } else {
                                image.setScanner(metadata);
                            }
                        } else if (image.getScanner().getId() != null) {
                            ScannerMetadata metadata = 
                                    metadataRepository.getScannerMetadataFromURI(ArrayDatasetRepositoryImpl.uriPrefix + image.getScanner().getId(), owner);
                            if (metadata == null) {
                                errorMessage.addError(new ObjectError("scannerMetadata", "NotFound"));
                            } else {
                                image.setScanner(metadata);
                            }
                        }
                    } catch (SQLException | SparqlException e) {
                        throw new GlycanRepositoryException("Error checking for the existince of the image analysis metadata", e);
                    }
                }
                // check the rawData
                if (image.getRawDataList() == null || image.getRawDataList().isEmpty()) {
                    errorMessage.addError(new ObjectError("rawData", "NoEmpty"));
                } else {
                    for (RawData rawData: image.getRawDataList()) {
                        // set the slide for rawData
                            rawData.setSlide(slide);
                    
                        // check the file for rawData
                        if (rawData.getFile() == null || rawData.getFile().getIdentifier() == null) {
                            if (!allowPartialData) errorMessage.addError(new ObjectError("rawData filename", "NotFound"));
                        } 
                        
                        if (rawData.getMetadata() == null) {
                            if (!allowPartialData) errorMessage.addError(new ObjectError("imageAnalysisMetadata", "NoEmpty"));
                        } else {
                            try {
                                if (rawData.getMetadata().getName() != null) {
                                    ImageAnalysisSoftware metadata = metadataRepository.getImageAnalysisSoftwarByLabel(rawData.getMetadata().getName(), owner);
                                    if (metadata == null) {
                                        errorMessage.addError(new ObjectError("imageAnalysisMetadata", "NotFound"));
                                    } else {
                                        rawData.setMetadata(metadata);
                                    }
                                } else if (rawData.getMetadata().getUri() != null) {
                                    ImageAnalysisSoftware metadata = metadataRepository.getImageAnalysisSoftwareFromURI(rawData.getMetadata().getUri(), owner);
                                    if (metadata == null) {
                                        errorMessage.addError(new ObjectError("imageAnalysisMetadata", "NotFound"));
                                    } else {
                                        rawData.setMetadata(metadata);
                                    }
                                } else if (rawData.getMetadata().getId() != null) {
                                    ImageAnalysisSoftware metadata = 
                                            metadataRepository.getImageAnalysisSoftwareFromURI(ArrayDatasetRepositoryImpl.uriPrefix + rawData.getMetadata().getId(), owner);
                                    if (metadata == null) {
                                        errorMessage.addError(new ObjectError("imageAnalysisMetadata", "NotFound"));
                                    } else {
                                        rawData.setMetadata(metadata);
                                    }
                                }
                            } catch (SQLException | SparqlException e) {
                                throw new GlycanRepositoryException("Error checking for the existince of the image analysis metadata", e);
                            }
                        }
                        
                        if (rawData.getProcessedDataList() == null || rawData.getProcessedDataList().isEmpty()) {
                            errorMessage.addError(new ObjectError("processedData", "NoEmpty"));
                        } else {
                            for (ProcessedData processedData: rawData.getProcessedDataList()) {
                                if (processedData.getMetadata() == null) {
                                    if (!allowPartialData) errorMessage.addError(new ObjectError("dataProcessingSoftware", "NoEmpty"));
                                } else {
                                    try {
                                        if (processedData.getMetadata().getName() != null) {
                                            DataProcessingSoftware metadata = metadataRepository.getDataProcessingSoftwareByLabel(processedData.getMetadata().getName(), owner);
                                            if (metadata == null) {
                                                errorMessage.addError(new ObjectError("dataProcessingSoftware", "NotFound"));
                                            } else {
                                                processedData.setMetadata(metadata);
                                            }
                                        } else if (processedData.getMetadata().getUri() != null) {
                                            DataProcessingSoftware metadata = metadataRepository.getDataProcessingSoftwareFromURI(processedData.getMetadata().getUri(), owner);
                                            if (metadata == null) {
                                                errorMessage.addError(new ObjectError("dataProcessingSoftware", "NotFound"));
                                            } else {
                                                processedData.setMetadata(metadata);
                                            }
                                        } else if (processedData.getMetadata().getId() != null) {
                                            DataProcessingSoftware metadata = 
                                                    metadataRepository.getDataProcessingSoftwareFromURI(ArrayDatasetRepositoryImpl.uriPrefix + processedData.getMetadata().getId(), owner);
                                            if (metadata == null) {
                                                errorMessage.addError(new ObjectError("dataProcessingSoftware", "NotFound"));
                                            } else {
                                                processedData.setMetadata(metadata);
                                            }
                                        }
                                    } catch (SQLException | SparqlException e) {
                                        throw new GlycanRepositoryException("Error checking for the existince of the image analysis metadata", e);
                                    }
                                }
                                if (processedData.getFile() == null || processedData.getFile().getIdentifier() == null) {
                                    errorMessage.addError(new ObjectError("processedData file", "NoEmpty"));
                                }  else {
                                    // check for the existence of the file
                                    File excelFile = new File(uploadDir, processedData.getFile().getIdentifier());
                                    if (!excelFile.exists()) {
                                        errorMessage.addError(new ObjectError("processedData file", "NotFound"));
                                    } else {
                                        // check for the fileFormat
                                        if (processedData.getFile().getFileFormat() == null) {
                                            errorMessage.addError(new ObjectError("processedData fileformat", "NoEmpty"));
                                        }
                                    }
                                }
                                
                                if (processedData.getMethod() == null) {
                                    errorMessage.addError(new ObjectError("method", "NoEmpty"));
                                }
                            }
                        }
                    }
                }
            }
        }*/
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid slide information", errorMessage);
        
        // save the slide 
        try {
            String slideURI = datasetRepository.addSlide(slide, datasetId, owner);
            if (slideURI != null) {
                return slideURI.substring(slideURI.lastIndexOf("/") + 1);
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException ("Cannot save the slide to the repository", e);
        }
        
        return null;
    }
    
    @Operation(summary = "Add given Image to the slide of the dataset for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addImage", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added image"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to add images to a dataset"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String addImage (
            @Parameter(required=true, description="Image to be added. Image"
                    + " should have a filename (already uploaded), "
                    + "and it should have an existing ScannerMetadata (specified by name, id or uri), "
                    + " Raw data are ignored. You should be using addRawData to add those")
            @RequestBody Image image, 
            @Parameter(required=true, description="id of the array dataset (must already be in the repository) to add the image") 
            @RequestParam("arraydatasetId")
            String datasetId,  
            @Parameter(required=true, description="id of the slide (must already be in the repository) to add the image") 
            @RequestParam("slideId")
            String slideId,  
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        boolean allowPartialData = false;
        if (user.hasRole("ROLE_DATA"))
            allowPartialData = true;
        
        UserEntity owner = user;
        // check if the dataset with the given id exists
        try {
            ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, user);
            if (dataset == null) {
                // check if the user can access this dataset as a co-owner
                String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                if (coOwnedGraph != null) {
                    UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                    if (originalUser != null) {
                        dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, originalUser);
                        owner = originalUser;
                    }
                }
            }
            if (dataset == null)
                errorMessage.addError(new ObjectError("dataset", "NotFound"));
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
        }
        
        Slide slide = null;
        // check if the slide with the given id exists
        try {
            slide = datasetRepository.getSlideFromURI(GlygenArrayRepositoryImpl.uriPrefix + slideId, false, user);
            if (slide == null) {
                errorMessage.addError(new ObjectError("slide", "NotFound"));
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Slide " + slideId + " cannot be retrieved for user " + p.getName(), e);
        }
        
        //check if the dataset is public
        try {
            String publicID = datasetRepository.getDatasetPublicId(datasetId.trim());
            if (publicID != null) {
                // this is major change, do not allow
                errorMessage.addError(new ObjectError("dataset", "Public"));
            }
        } catch (SparqlException e) {
            throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
        }
        
        // create a folder for the experiment, if it does not exists, and move the file into that folder
        File experimentFolder = new File (uploadDir + File.separator + datasetId);
        if (!experimentFolder.exists()) {
            experimentFolder.mkdirs();
        }
        // check to make sure the image is specified and image file is in uploads folder
        if (image.getFile() != null && image.getFile().getIdentifier() != null) {
            // move it to the dataset folder
            File imageFile = new File (uploadDir, image.getFile().getIdentifier());
            if (!imageFile.exists()) {
                errorMessage.addError(new ObjectError("imageFile", "NotFound"));
            }
            else {
                File newFile = new File(experimentFolder + File.separator + image.getFile().getIdentifier());
                if(!imageFile.renameTo (newFile)) { 
                    throw new GlycanRepositoryException("Image file cannot be moved to the dataset folder");
                } 
                image.getFile().setFileFolder(uploadDir + File.separator + datasetId);
                image.getFile().setFileSize(newFile.length());
                image.getFile().setCreatedDate(new Date());
                image.getFile().setDrsId(image.getFile().getIdentifier().substring(0, image.getFile().getIdentifier().lastIndexOf(".")));
                image.getFile().setExtension(image.getFile().getIdentifier().substring(image.getFile().getIdentifier().lastIndexOf(".")+1));
                GlygenArrayController.calculateChecksum (image.getFile());
            }
            
        } else {
            if (!allowPartialData)
                errorMessage.addError(new ObjectError("imageFile", "NoEmpty"));
        }
        // check the metadata
        if (image.getScanner() == null) {
            if (!allowPartialData) errorMessage.addError(new ObjectError("scannerMetadata", "NoEmpty"));
        } else {
            try {
                if (image.getScanner().getName() != null) {
                    ScannerMetadata metadata = metadataRepository.getScannerMetadataByLabel(image.getScanner().getName(), owner);
                    if (metadata == null) {
                        errorMessage.addError(new ObjectError("scannerMetadata", "NotFound"));
                    } else {
                        image.setScanner(metadata);
                    }
                } else if (image.getScanner().getUri() != null) {
                    ScannerMetadata metadata = metadataRepository.getScannerMetadataFromURI(image.getScanner().getUri(), owner);
                    if (metadata == null) {
                        errorMessage.addError(new ObjectError("scannerMetadata", "NotFound"));
                    } else {
                        image.setScanner(metadata);
                    }
                } else if (image.getScanner().getId() != null) {
                    ScannerMetadata metadata = 
                            metadataRepository.getScannerMetadataFromURI(ArrayDatasetRepositoryImpl.uriPrefix + image.getScanner().getId(), owner);
                    if (metadata == null) {
                        errorMessage.addError(new ObjectError("scannerMetadata", "NotFound"));
                    } else {
                        image.setScanner(metadata);
                    }
                }
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the image analysis metadata", e);
            }
        }
        // check the rawData
      /*  if (image.getRawDataList() != null && image.getRawDataList().isEmpty()) {
            for (RawData rawData: image.getRawDataList()) {
                // set the slide for rawData
                rawData.setSlide(slide);
            
                // check the file for rawData
                if (rawData.getFile() == null || rawData.getFile().getIdentifier() == null) {
                    if (!allowPartialData) errorMessage.addError(new ObjectError("rawData filename", "NotFound"));
                } 
                
                if (rawData.getMetadata() == null) {
                    if (!allowPartialData) errorMessage.addError(new ObjectError("imageAnalysisMetadata", "NoEmpty"));
                } else {
                    try {
                        if (rawData.getMetadata().getName() != null) {
                            ImageAnalysisSoftware metadata = metadataRepository.getImageAnalysisSoftwarByLabel(rawData.getMetadata().getName(), owner);
                            if (metadata == null) {
                                errorMessage.addError(new ObjectError("imageAnalysisMetadata", "NotFound"));
                            } else {
                                rawData.setMetadata(metadata);
                            }
                        } else if (rawData.getMetadata().getUri() != null) {
                            ImageAnalysisSoftware metadata = metadataRepository.getImageAnalysisSoftwareFromURI(rawData.getMetadata().getUri(), owner);
                            if (metadata == null) {
                                errorMessage.addError(new ObjectError("imageAnalysisMetadata", "NotFound"));
                            } else {
                                rawData.setMetadata(metadata);
                            }
                        } else if (rawData.getMetadata().getId() != null) {
                            ImageAnalysisSoftware metadata = 
                                    metadataRepository.getImageAnalysisSoftwareFromURI(ArrayDatasetRepositoryImpl.uriPrefix + rawData.getMetadata().getId(), owner);
                            if (metadata == null) {
                                errorMessage.addError(new ObjectError("imageAnalysisMetadata", "NotFound"));
                            } else {
                                rawData.setMetadata(metadata);
                            }
                        }
                    } catch (SQLException | SparqlException e) {
                        throw new GlycanRepositoryException("Error checking for the existince of the image analysis metadata", e);
                    }
                }
                
                if (rawData.getProcessedDataList() == null || rawData.getProcessedDataList().isEmpty()) {
                    errorMessage.addError(new ObjectError("processedData", "NoEmpty"));
                } else {
                    for (ProcessedData processedData: rawData.getProcessedDataList()) {
                        if (processedData.getMetadata() == null) {
                            if (!allowPartialData) errorMessage.addError(new ObjectError("dataProcessingSoftware", "NoEmpty"));
                        } else {
                            try {
                                if (processedData.getMetadata().getName() != null) {
                                    DataProcessingSoftware metadata = metadataRepository.getDataProcessingSoftwareByLabel(processedData.getMetadata().getName(), owner);
                                    if (metadata == null) {
                                        errorMessage.addError(new ObjectError("dataProcessingSoftware", "NotFound"));
                                    } else {
                                        processedData.setMetadata(metadata);
                                    }
                                } else if (processedData.getMetadata().getUri() != null) {
                                    DataProcessingSoftware metadata = metadataRepository.getDataProcessingSoftwareFromURI(processedData.getMetadata().getUri(), owner);
                                    if (metadata == null) {
                                        errorMessage.addError(new ObjectError("dataProcessingSoftware", "NotFound"));
                                    } else {
                                        processedData.setMetadata(metadata);
                                    }
                                } else if (processedData.getMetadata().getId() != null) {
                                    DataProcessingSoftware metadata = 
                                            metadataRepository.getDataProcessingSoftwareFromURI(ArrayDatasetRepositoryImpl.uriPrefix + processedData.getMetadata().getId(), owner);
                                    if (metadata == null) {
                                        errorMessage.addError(new ObjectError("dataProcessingSoftware", "NotFound"));
                                    } else {
                                        processedData.setMetadata(metadata);
                                    }
                                }
                            } catch (SQLException | SparqlException e) {
                                throw new GlycanRepositoryException("Error checking for the existince of the image analysis metadata", e);
                            }
                        }
                        if (processedData.getFile() == null || processedData.getFile().getIdentifier() == null) {
                            errorMessage.addError(new ObjectError("processedData file", "NoEmpty"));
                        }  else {
                            // check for the existence of the file
                            File excelFile = new File(uploadDir, processedData.getFile().getIdentifier());
                            if (!excelFile.exists()) {
                                errorMessage.addError(new ObjectError("processedData file", "NotFound"));
                            } else {
                                // check for the fileFormat
                                if (processedData.getFile().getFileFormat() == null) {
                                    errorMessage.addError(new ObjectError("processedData fileformat", "NoEmpty"));
                                }
                            }
                        }
                        
                        if (processedData.getMethod() == null) {
                            errorMessage.addError(new ObjectError("method", "NoEmpty"));
                        }
                    }
                }
            }
        }*/
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid image information", errorMessage);
        
        
        // save the image 
        try {
            String imageURI = datasetRepository.addImage(image, slide.getId(), owner);
            if (imageURI != null) {
                return imageURI.substring(imageURI.lastIndexOf("/") + 1);
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException ("Cannot save the slide to the repository", e);
        }
        
        return null;
    }
    
    
    @Operation(summary = "Add given Rawdata to the image of the dataset for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addRawdata", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added rawdata"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to add rawdata to a dataset"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String addRawData (
            @Parameter(required=true, description="Raw Data to be added. Raw data"
                    + " should have a filename (already uploaded), slide information with printed slide id/uri, "
                    + "and it should have an existing ImageAnalysisMetadata (specified by name, id or uri)"
                    + " Processed data are ignored. You should use addProcessedDataFromExcel to add the processed data")
            @RequestBody RawData rawData, 
            @Parameter(required=true, description="id of the array dataset (must already be in the repository) to add the raw data") 
            @RequestParam("arraydatasetId")
            String datasetId,  
            @Parameter(required=true, description="id of the image (must already be in the repository) to add the raw data") 
            @RequestParam("imageId")
            String imageId,  
            Principal p) {
    
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        boolean allowPartialData = false;
        if (user.hasRole("ROLE_DATA"))
            allowPartialData = true;
        UserEntity owner = user;
        // check if the dataset with the given id exists
        ArrayDataset dataset = null;
        try {
            dataset = datasetRepository.getArrayDataset(datasetId, false, user);
            if (dataset == null) {
                // check if the user can access this dataset as a co-owner
                String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                if (coOwnedGraph != null) {
                    UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                    if (originalUser != null) {
                        dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                        owner = originalUser;
                    }
                }
            }
            if (dataset == null) {
                errorMessage.addError(new ObjectError("dataset", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
        }
        
        Image image = null;
        // check if the image with the given id exists
        try {
            image = datasetRepository.getImageFromURI(GlygenArrayRepositoryImpl.uriPrefix + imageId, false, owner);
            if (image == null) {
                errorMessage.addError(new ObjectError("image", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Image " + imageId + " cannot be retrieved for user " + p.getName(), e);
        }
        
        // need to locate the slide from the dataset and image
        if (dataset != null && dataset.getSlides() != null) {
            for (Slide slide: dataset.getSlides()) {
                if (slide.getImages() != null) {
                    for (Image i: slide.getImages()) {
                        if (i.getId().equals(imageId)) {
                            rawData.setSlide(slide);
                            break;
                        }
                    }
                }
            }
        }
         
        if (rawData.getSlide() == null || rawData.getSlide().getPrintedSlide() == null) {
            errorMessage.addError(new ObjectError("slide", "NotFound"));
            errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
        } else {
            try {
                String printedSlideUri = rawData.getSlide().getPrintedSlide().getUri();
                if (printedSlideUri != null) {
                    PrintedSlide existing = datasetRepository.getPrintedSlideFromURI(printedSlideUri, owner);
                    if (existing == null) {
                        errorMessage.addError(new ObjectError("printedSlide", "NotFound"));
                        errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
                    } else {
                        rawData.getSlide().setPrintedSlide(existing);
                    }
                } else if (rawData.getSlide().getPrintedSlide().getId() != null) {
                    String printedSlideId = rawData.getSlide().getPrintedSlide().getId();
                    // check locally first
                    PrintedSlide existing = datasetRepository.getPrintedSlideFromURI(GlygenArrayRepositoryImpl.uriPrefix + printedSlideId, owner);
                    if (existing == null) { // check public repo
                        existing = datasetRepository.getPrintedSlideFromURI(GlygenArrayRepositoryImpl.uriPrefixPublic + printedSlideId, owner);
                    }
                    if (existing == null) {
                        errorMessage.addError(new ObjectError("printedSlide", "NotFound"));
                        errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
                    } else {
                        rawData.getSlide().setPrintedSlide(existing);
                    }
                } else if (rawData.getSlide().getPrintedSlide().getName() != null) {
                    PrintedSlide existing = datasetRepository.getPrintedSlideByLabel(rawData.getSlide().getPrintedSlide().getName(), false, owner);
                    if (existing == null) {
                        // try to see if it exists in public graph
                        existing = datasetRepository.getPrintedSlideByLabel(rawData.getSlide().getPrintedSlide().getName(), false, null);
                        if (existing == null) {
                            errorMessage.addError(new ObjectError("printedSlide", "NotFound"));
                            errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
                        } else {
                            rawData.getSlide().setPrintedSlide(existing);
                        }
                    } else {
                        rawData.getSlide().setPrintedSlide(existing);
                    }
                }
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the printed slide", e);
            }
        }
        
        File file = null;
        // check to make sure, the file is specified and exists in the uploads folder
        if (rawData.getFile() == null || rawData.getFile().getIdentifier() == null) {
            if (!allowPartialData) {
                errorMessage.addError(new ObjectError("filename", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
            }
        } else {
            String fileFolder = uploadDir;
            if (rawData.getFile().getFileFolder() != null)
                fileFolder = rawData.getFile().getFileFolder();
            file = new File (fileFolder, rawData.getFile().getIdentifier());
            if (!file.exists()) {
                errorMessage.addError(new ObjectError("file", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
            }
            else {
                // create a folder for the experiment, if it does not exists, and move the file into that folder
                File experimentFolder = new File (uploadDir + File.separator + datasetId);
                if (!experimentFolder.exists()) {
                    experimentFolder.mkdirs();
                }
                File newFile = new File(experimentFolder + File.separator + rawData.getFile().getIdentifier());
                if (!file.renameTo (newFile)) { 
                    throw new GlycanRepositoryException("File cannot be moved to the dataset folder");
                } 
                rawData.getFile().setFileFolder(uploadDir + File.separator + datasetId);
                rawData.getFile().setFileSize(newFile.length());
                rawData.getFile().setCreatedDate(new Date());
                rawData.getFile().setDrsId(rawData.getFile().getIdentifier().substring(0, rawData.getFile().getIdentifier().lastIndexOf(".")));
                rawData.getFile().setExtension(rawData.getFile().getIdentifier().substring(rawData.getFile().getIdentifier().lastIndexOf(".")+1));
                GlygenArrayController.calculateChecksum (rawData.getFile());
            }
        }
        
        if (rawData.getMetadata() == null) {
            if (!allowPartialData) errorMessage.addError(new ObjectError("imageAnalysisMetadata", "NoEmpty"));
        } else {
            try {
                if (rawData.getMetadata().getName() != null) {
                    ImageAnalysisSoftware metadata = metadataRepository.getImageAnalysisSoftwarByLabel(rawData.getMetadata().getName(), owner);
                    if (metadata == null) {
                        errorMessage.addError(new ObjectError("imageAnalysisMetadata", "NotFound"));
                        errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
                    } else {
                        rawData.setMetadata(metadata);
                    }
                } else if (rawData.getMetadata().getUri() != null) {
                    ImageAnalysisSoftware metadata = metadataRepository.getImageAnalysisSoftwareFromURI(rawData.getMetadata().getUri(), owner);
                    if (metadata == null) {
                        errorMessage.addError(new ObjectError("imageAnalysisMetadata", "NotFound"));
                        errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
                    } else {
                        rawData.setMetadata(metadata);
                    }
                } else if (rawData.getMetadata().getId() != null) {
                    ImageAnalysisSoftware metadata = 
                            metadataRepository.getImageAnalysisSoftwareFromURI(ArrayDatasetRepositoryImpl.uriPrefix + rawData.getMetadata().getId(), owner);
                    if (metadata == null) {
                        errorMessage.addError(new ObjectError("imageAnalysisMetadata", "NotFound"));
                        errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
                    } else {
                        rawData.setMetadata(metadata);
                    }
                }
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the image analysis metadata", e);
            }
        }
        
     /*   if (rawData.getProcessedDataList() != null && rawData.getProcessedDataList().isEmpty()) {
            try {
                List<ProcessedData> retrievedList = new ArrayList<ProcessedData>();
                // check if the processed data already exists
                for (ProcessedData processedData: rawData.getProcessedDataList()) {
                    if (processedData.getUri() != null) {
                        ProcessedData existing = datasetRepository.getProcessedDataFromURI(processedData.getUri(), false, owner);
                        if (existing == null) {
                            String[] codes = {processedData.getUri()};
                            errorMessage.addError(new ObjectError("processedData", codes, null, "NotFound"));
                        } else {
                            retrievedList.add(existing);
                        }
                    } else if (processedData.getId() != null) {
                        ProcessedData existing = datasetRepository.getProcessedDataFromURI(ArrayDatasetRepositoryImpl.uriPrefix + processedData.getId(), false, owner);
                        if (existing == null) {
                            String[] codes = {processedData.getId()};
                            errorMessage.addError(new ObjectError("processedData", codes, null, "NotFound"));
                        } else {
                            retrievedList.add(existing);
                        }
                    } else {
                        errorMessage.addError(new ObjectError("processedData", "NotFound"));
                    }
                }
                rawData.setProcessedDataList(retrievedList);
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the processed data", e);
            }
        }*/
         
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid raw data information", errorMessage);
        
        
        try {
            // save whatever we have for now for raw data and update its status to "processing"
            String uri = datasetRepository.addRawData(rawData, imageId, owner);  
            rawData.setUri(uri);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            if (rawData.getError() == null)
                rawData.setStatus(FutureTaskStatus.PROCESSING);
            else 
                rawData.setStatus(FutureTaskStatus.ERROR);
            datasetRepository.updateStatus (uri, rawData, owner);
            
            // check if there is a file, if not no need to go through parsing
            if (rawData.getFile() == null) {
                // set the status to DONE
                rawData.setStatus(FutureTaskStatus.DONE);
                datasetRepository.updateStatus (uri, rawData, owner);
                return id;
            }
            
            CompletableFuture<String> rawDataURI = null;
            try {
                // need to load the full layout before parsing
                SlideLayout fullLayout = null;
                String uriPre = ArrayDatasetRepositoryImpl.uriPrefix;
                // check if it is already loaded fully
                if (rawData.getSlide().getPrintedSlide().getLayout().getBlocks() !=null && !rawData.getSlide().getPrintedSlide().getLayout().getBlocks().isEmpty() &&
                        rawData.getSlide().getPrintedSlide().getLayout().getBlocks().get(0).getBlockLayout() != null && 
                        rawData.getSlide().getPrintedSlide().getLayout().getBlocks().get(0).getBlockLayout().getSpots() != null &&
                        !rawData.getSlide().getPrintedSlide().getLayout().getBlocks().get(0).getBlockLayout().getSpots().isEmpty()) {
                    fullLayout = rawData.getSlide().getPrintedSlide().getLayout();
                    if (rawData.getSlide().getPrintedSlide().getLayout().getIsPublic()) {
                        uriPre = ArrayDatasetRepositoryImpl.uriPrefixPublic;
                    }
                } else {
                    if (rawData.getSlide().getPrintedSlide().getLayout().getIsPublic() || rawData.getSlide().getPrintedSlide().getLayout().getUri().contains("public") ) {
                        fullLayout = layoutRepository.getSlideLayoutById(rawData.getSlide().getPrintedSlide().getLayout().getId(), null);
                        uriPre = ArrayDatasetRepositoryImpl.uriPrefixPublic;
                    } else {
                        fullLayout = layoutRepository.getSlideLayoutById(rawData.getSlide().getPrintedSlide().getLayout().getId(), owner);
                    }
                }
                if (fullLayout == null) {
                    errorMessage.addError(new ObjectError("exception", "slide layout cannot be located: " + rawData.getSlide().getPrintedSlide().getLayout().getId()));
                    errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
                    rawData.setError(errorMessage);
                    rawData.setStatus(FutureTaskStatus.ERROR);
                    datasetRepository.updateStatus (uri, rawData, owner);
                    return id; // already saved the error, no need to throw it
                    //throw new IllegalArgumentException("Cannot locate the slide layout!", errorMessage);
                }
                try {
                    Map<Measurement, Spot> dataMap = RawdataParser.parse(rawData.getFile(), fullLayout, rawData.getPowerLevel());
                    // check blocks used and extract only those measurements
                    if (rawData.getSlide().getBlocksUsed() != null && !rawData.getSlide().getBlocksUsed().isEmpty()) {
                        Map<Measurement, Spot> filteredMap = new HashMap<Measurement, Spot>();
                        List<String> foundBlocks = new ArrayList<String>();
                        for (String blockId: rawData.getSlide().getBlocksUsed()) { 
                            boolean found = false;
                            for (Map.Entry<Measurement, Spot> entry: dataMap.entrySet()) {
                                if (entry.getValue().getBlockURI().equals(uriPre + blockId)) {
                                    filteredMap.put(entry.getKey(), entry.getValue());
                                    found = true;
                                    if (!foundBlocks.contains(blockId)) {
                                        foundBlocks.add(blockId);
                                    }
                                }
                            }
                            if (!found) {
                                String[] codes = new String[] {blockId};
                                logger.warn("cannot find data for the selected block " + blockId);
                               // errorMessage.addError(new ObjectError("blockId", codes, null, "cannot find data for the selected block"));
                            }
                        }
                        //TODO commented out temporarily
                       /* if (foundBlocks.size() != rawData.getSlide().getBlocksUsed().size()) {
                            // we could not find the data for the selected blocks from the raw data file
                            errorMessage.addError(new ObjectError("blocksUsed", "NotValid"));
                            rawData.setError(errorMessage);
                            rawData.setStatus(FutureTaskStatus.ERROR);
                            datasetRepository.updateStatus (uri, rawData, owner);
                            return id;  // already saved the error, no need to throw it
                            //throw new IllegalArgumentException("Cannot parse the file", errorMessage);
                        }*/
                       
                        rawData.setDataMap(filteredMap); 
                    } else {
                        rawData.setDataMap(dataMap);
                    }
                } catch (IOException e) {
                    errorMessage.addError(new ObjectError("file", e.getMessage()));
                    errorMessage.setErrorCode(ErrorCodes.PARSE_ERROR);
                    rawData.setError(errorMessage);
                    rawData.setStatus(FutureTaskStatus.ERROR);
                    datasetRepository.updateStatus (uri, rawData, owner);
                    return id;// already saved the error, no need to throw it
                    //throw new IllegalArgumentException("Cannot parse the file", errorMessage);
                }
                UserEntity originalUser = owner;
                rawDataURI = datasetRepository.addMeasurementsToRawData(rawData, owner);
                if (rawDataURI.isCompletedExceptionally()) {
                    logger.error("completed exceptionally!!!");
                }
                rawDataURI.whenComplete((uriString, e) -> {
                    try {
                        if (e != null) {
                            logger.error(e.getMessage(), e);
                            rawData.setStatus(FutureTaskStatus.ERROR);
                            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException && e.getCause().getCause() instanceof ErrorMessage) 
                                rawData.setError((ErrorMessage) e.getCause().getCause());
                            else {
                                errorMessage.addError(new ObjectError("exception", e.getMessage()));
                                errorMessage.setErrorCode(ErrorCodes.INTERNAL_ERROR);
                                rawData.setError(errorMessage);
                            }
                        } else {
                            rawData.setStatus(FutureTaskStatus.DONE);    
                        }
                        datasetRepository.updateStatus (uriString, rawData, originalUser);
                    } catch (SparqlException | SQLException ex) {
                        throw new GlycanRepositoryException("Rawdata cannot be added for user " + p.getName(), e);
                    } 
                });
                rawDataURI.get(1000, TimeUnit.MILLISECONDS);
            } catch (IllegalArgumentException e) {
                rawData.setStatus(FutureTaskStatus.ERROR);
                if (e.getCause() != null && e.getCause() instanceof ErrorMessage)
                    rawData.setError((ErrorMessage) e.getCause());
                datasetRepository.updateStatus (uri, rawData, owner);
                return id;
                //throw e;
            } catch (TimeoutException e) {
                synchronized (this) {
                    if (rawData.getError() == null)
                        rawData.setStatus(FutureTaskStatus.PROCESSING);
                    else 
                        rawData.setStatus(FutureTaskStatus.ERROR);
                    datasetRepository.updateStatus (uri, rawData, owner);
                    // delete files (they should already be moved to the experiment folder)
                    if (file != null) file.delete();
                    return id;
                }
            } catch (Exception e) {
                errorMessage.addError(new ObjectError ("exception", e.getMessage()));
                errorMessage.setErrorCode(ErrorCodes.INTERNAL_ERROR);
                rawData.setError(errorMessage);
                rawData.setStatus(FutureTaskStatus.ERROR);
                datasetRepository.updateStatus (rawData.getUri(), rawData, owner);
                logger.error("Cannot add the raw data measurements to the repository", e);
                //throw new IllegalArgumentException("Cannot add the raw data measurements to the repository", e);
            }
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Rawdata cannot be added for user " + p.getName(), e);
        } catch (IllegalArgumentException e) {
            throw e;
        } 
    }
    
    @Operation(summary = "Add given printed slide set for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addPrintedSlide", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added printed slide"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to register slides"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String addPrintedSlide (
            @Parameter(required=true, description="Printed slide to be added") 
            @RequestBody PrintedSlide slide, Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        // validate first
        if (validator != null) {
            if  (slide.getName() != null) {
                Set<ConstraintViolation<PrintedSlide>> violations = validator.validateValue(PrintedSlide.class, "name", slide.getName());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("name", "LengthExceeded"));
                }       
            }
            
            if  (slide.getDescription() != null) {
                Set<ConstraintViolation<PrintedSlide>> violations = validator.validateValue(PrintedSlide.class, "description", slide.getDescription());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("description", "LengthExceeded"));
                }       
            }
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
        if (slide.getName() == null || slide.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        // check to make sure, the slide layout is specified
        if (slide.getLayout() == null || (slide.getLayout().getId() == null && slide.getLayout().getUri() == null && slide.getLayout().getName() == null)) {
            errorMessage.addError(new ObjectError("slidelayout", "NoEmpty"));
        } 

        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        
        // check for duplicate name
        try {
            PrintedSlide existing = datasetRepository.getPrintedSlideByLabel(slide.getName().trim(), false, user);
            if (existing != null) {
                errorMessage.addError(new ObjectError("name", "Duplicate"));
            }
        } catch (SparqlException | SQLException e2) {
            throw new GlycanRepositoryException("Error checking for duplicate printedSlide", e2);
        }
        
        // check if the slide layout exists
        if (slide.getLayout() != null) {
            try {
                String slideLayoutId = slide.getLayout().getId();
                if (slideLayoutId == null) {
                    if (slide.getLayout().getUri() != null) {
                        slideLayoutId = slide.getLayout().getUri().substring(slide.getLayout().getUri().lastIndexOf("/") + 1);
                    }
                }
                if (slideLayoutId != null) {
                    SlideLayout existing = layoutRepository.getSlideLayoutById(slideLayoutId, user, false);
                    if (existing == null) {
                        errorMessage.addError(new ObjectError("slidelayout", "NotFound"));
                    } else {
                        slide.setLayout(existing);
                        // check if the slide layout is done
                        if (existing.getStatus() != FutureTaskStatus.DONE) {
                            errorMessage.addError(new ObjectError ("slideLayout", "NotDone"));
                        }  
                    }
                } else if (slide.getLayout().getName() != null) {
                    SlideLayout existing = layoutRepository.getSlideLayoutByName(slide.getLayout().getName(), user);
                    if (existing == null) {
                        errorMessage.addError(new ObjectError("slidelayout", "NotFound"));
                    } else {
                        slide.setLayout(existing);
                        // check if the slide layout is done
                        if (existing.getStatus() != FutureTaskStatus.DONE) {
                            errorMessage.addError(new ObjectError ("slideLayout", "NotDone"));
                        }
                    }
                }
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the slide layout", e);
            }
        }
        
        //TODO do we check to make sure there is metadata??
        if (slide.getMetadata() != null) {
            try {
                if (slide.getMetadata().getName() != null) {
                    SlideMetadata slideMetadata = metadataRepository.getSlideMetadataByLabel(slide.getMetadata().getName(), user);
                    if (slideMetadata == null) {
                        errorMessage.addError(new ObjectError("slideMetadata", "NotFound"));
                    } else {
                        slide.setMetadata(slideMetadata);
                    }
                } else if (slide.getMetadata().getUri() != null) {
                    SlideMetadata slideMetadata = metadataRepository.getSlideMetadataFromURI(slide.getMetadata().getUri(), user);
                    if (slideMetadata == null) {
                        errorMessage.addError(new ObjectError("slideMetadata", "NotFound"));
                    } else {
                        slide.setMetadata(slideMetadata);
                    }
                } else if (slide.getMetadata().getId() != null) {
                    SlideMetadata slideMetadata = metadataRepository.getSlideMetadataFromURI(ArrayDatasetRepositoryImpl.uriPrefix + slide.getMetadata().getId(), user);
                    if (slideMetadata == null) {
                        errorMessage.addError(new ObjectError("slideMetadata", "NotFound"));
                    } else {
                        slide.setMetadata(slideMetadata);
                    }
                }
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the slide metadata", e);
            }
        }
        
        if (slide.getPrinter() != null) {
            try {
                if (slide.getPrinter().getName() != null) {
                    Printer printer = metadataRepository.getPrinterByLabel(slide.getPrinter().getName(), user);
                    if (printer == null) {
                        errorMessage.addError(new ObjectError("printer", "NotFound"));
                    } else {
                        slide.setPrinter(printer);
                    }
                } else if (slide.getPrinter().getUri() != null) {
                    Printer printer = metadataRepository.getPrinterFromURI(slide.getPrinter().getUri(), user);
                    if (printer == null) {
                        errorMessage.addError(new ObjectError("printer", "NotFound"));
                    } else {
                        slide.setPrinter(printer);
                    }
                } else if (slide.getPrinter().getId() != null) {
                    Printer printer = metadataRepository.getPrinterFromURI(ArrayDatasetRepositoryImpl.uriPrefix + slide.getPrinter().getId(), user);
                    if (printer == null) {
                        errorMessage.addError(new ObjectError("printer", "NotFound"));
                    } else {
                        slide.setPrinter(printer);
                    }
                }
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the printer", e);
            }
        }
        
        if (slide.getPrintRun() != null) {
            try {
                if (slide.getPrintRun().getName() != null) {
                    PrintRun printer = metadataRepository.getPrintRunByLabel(slide.getPrintRun().getName(), user);
                    if (printer == null) {
                        errorMessage.addError(new ObjectError("printrun", "NotFound"));
                    } else {
                        slide.setPrintRun(printer);
                    }
                } else if (slide.getPrintRun().getUri() != null) {
                    PrintRun printer = metadataRepository.getPrintRunFromURI(slide.getPrintRun().getUri(), user);
                    if (printer == null) {
                        errorMessage.addError(new ObjectError("printrun", "NotFound"));
                    } else {
                        slide.setPrintRun(printer);
                    }
                } else if (slide.getPrintRun().getId() != null) {
                    PrintRun printer = metadataRepository.getPrintRunFromURI(ArrayDatasetRepositoryImpl.uriPrefix + slide.getPrintRun().getId(), user);
                    if (printer == null) {
                        errorMessage.addError(new ObjectError("printrun", "NotFound"));
                    } else {
                        slide.setPrintRun(printer);
                    }
                }
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the printrun", e);
            }
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid printed slide information", errorMessage);
        
        try {
            String uri = datasetRepository.addPrintedSlide(slide, user);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Printed slide cannot be added for user " + p.getName(), e);
        }
    }
    
    @Operation(summary = "Add given data processing software for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addDataProcessingSoftware", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added data processing metadata"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to register data processing metadata"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String addDataProcessingSoftware (
            @Parameter(required=true, description="Data processing software metadata to be added") 
            @RequestBody DataProcessingSoftware metadata, 
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        // validate first
        if (validator != null) {
            if  (metadata.getName() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "name", metadata.getName());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("name", "LengthExceeded"));
                }       
            }
            
            if  (metadata.getDescription() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "description", metadata.getDescription());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("description", "LengthExceeded"));
                }       
            }
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        Boolean validate = true;
        if (user.hasRole("ROLE_DATA")) 
            validate = false;
        
        if (metadata.getName() == null || metadata.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        if (metadata.getTemplate() == null || metadata.getTemplate().isEmpty()) {
            errorMessage.addError(new ObjectError("type", "NoEmpty"));
        }
        
        // check for duplicate name
        try {
            MetadataCategory existing = metadataRepository.getMetadataByLabel(metadata.getName().trim(), ArrayDatasetRepositoryImpl.dataProcessingTypePredicate, user);
            if (existing != null) {
                errorMessage.addError(new ObjectError("name", "Duplicate"));
            }
        } catch (SparqlException | SQLException e2) {
            throw new GlycanRepositoryException("Error checking for duplicate metadata", e2);
        }
        
        // check if the template exists
        try {
            String templateURI = templateRepository.getTemplateByName(metadata.getTemplate(), MetadataTemplateType.DATAPROCESSINGSOFTWARE);
            if (templateURI == null) {
                errorMessage.addError(new ObjectError("type", "NotValid"));
            }
            else {
                // validate mandatory/multiple etc.
                if (validate != null && validate) {
                    MetadataTemplate template = templateRepository.getTemplateFromURI(templateURI);
                    ErrorMessage err = validateMetadata (metadata, template);
                    if (err != null) {
                        for (ObjectError error: err.getErrors())
                            errorMessage.addError(error);
                    }    
                }
            }
        } catch (SparqlException | SQLException e1) {
            logger.error("Error retrieving template", e1);
            throw new GlycanRepositoryException("Error retrieving Data processing software metadata template " + p.getName(), e1);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid Data processing software metadata information", errorMessage);
        
        try {
            String uri = metadataRepository.addDataProcessingSoftware(metadata, user);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Data processing software metadata cannot be added for user " + p.getName(), e);
        }
        
    }
    
    @Operation(summary = "Add given image analysis software for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addImageAnalysis", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added image analysis metadata"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to register image analysis metadata"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String addImageAnalysisSoftware (
            @Parameter(required=true, description="Image Analysis metadata to be added") 
            @RequestBody ImageAnalysisSoftware metadata, 
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        // validate first
        if (validator != null) {
            if  (metadata.getName() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "name", metadata.getName());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("name", "LengthExceeded"));
                }       
            }
            
            if  (metadata.getDescription() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "description", metadata.getDescription());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("description", "LengthExceeded"));
                }       
            }
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        Boolean validate = true;
        if (user.hasRole("ROLE_DATA")) 
            validate = false;
        
        if (metadata.getName() == null || metadata.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        if (metadata.getTemplate() == null || metadata.getTemplate().isEmpty()) {
            errorMessage.addError(new ObjectError("type", "NoEmpty"));
        }
        
        // check for duplicate name
        try {
            MetadataCategory existing = metadataRepository.getMetadataByLabel(metadata.getName().trim(), ArrayDatasetRepositoryImpl.imageAnalysisTypePredicate, user);
            if (existing != null) {
                errorMessage.addError(new ObjectError("name", "Duplicate"));
            }
        } catch (SparqlException | SQLException e2) {
            throw new GlycanRepositoryException("Error checking for duplicate metadata", e2);
        }
        
        // check if the template exists
        try {
            String templateURI = templateRepository.getTemplateByName(metadata.getTemplate(), MetadataTemplateType.IMAGEANALYSISSOFTWARE);
            if (templateURI == null) {
                errorMessage.addError(new ObjectError("type", "NotValid"));
            }
            else {
                // validate mandatory/multiple etc.
                if (validate != null && validate) {
                    MetadataTemplate template = templateRepository.getTemplateFromURI(templateURI);
                    ErrorMessage err = validateMetadata (metadata, template);
                    if (err != null) {
                        for (ObjectError error: err.getErrors())
                            errorMessage.addError(error);
                    }   
                }
            }
        } catch (SparqlException | SQLException e1) {
            logger.error("Error retrieving template", e1);
            throw new GlycanRepositoryException("Error retrieving image analysis metadata template " + p.getName(), e1);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid image analysis metadata information", errorMessage);
        
        try {
            String uri = metadataRepository.addImageAnalysisSoftware(metadata, user);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Image Analysis metadata cannot be added for user " + p.getName(), e);
        }
        
    }
    
    @Operation(summary = "Add given printer metadata for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addPrinter", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added printer"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to register printers"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String addPrinter (
            @Parameter(required=true, description="Printer metadata to be added") 
            @RequestBody Printer printer, 
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        // validate first
        if (validator != null) {
            if  (printer.getName() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "name", printer.getName());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("name", "LengthExceeded"));
                }       
            }
            
            if  (printer.getDescription() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "description", printer.getDescription());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("description", "LengthExceeded"));
                }       
            }
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        Boolean validate = true;
        if (user.hasRole("ROLE_DATA")) 
            validate = false;
        
        if (printer.getName() == null || printer.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        if (printer.getTemplate() == null || printer.getTemplate().isEmpty()) {
            errorMessage.addError(new ObjectError("type", "NoEmpty"));
        }
        
        // check for duplicate name
        try {
            MetadataCategory metadata = metadataRepository.getMetadataByLabel(printer.getName().trim(), ArrayDatasetRepositoryImpl.printerTypePredicate, user);
            if (metadata != null) {
                errorMessage.addError(new ObjectError("name", "Duplicate"));
            }
        } catch (SparqlException | SQLException e2) {
            throw new GlycanRepositoryException("Error checking for duplicate metadata", e2);
        }
        
        // check if the template exists
        try {
            String templateURI = templateRepository.getTemplateByName(printer.getTemplate(), MetadataTemplateType.PRINTER);
            if (templateURI == null) {
                errorMessage.addError(new ObjectError("type", "NotValid"));
            }
            else {
                // validate mandatory/multiple etc.
                if (validate != null && validate) {
                    MetadataTemplate template = templateRepository.getTemplateFromURI(templateURI);
                    ErrorMessage err = validateMetadata (printer, template);
                    if (err != null) {
                        for (ObjectError error: err.getErrors())
                            errorMessage.addError(error);
                    }    
                }
            }
        } catch (SparqlException | SQLException e1) {
            logger.error("Error retrieving template", e1);
            throw new GlycanRepositoryException("Error retrieving printer template " + p.getName(), e1);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid printer information", errorMessage);
        
        try {
            String uri = metadataRepository.addPrinter(printer, user);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Printer cannot be added for user " + p.getName(), e);
        }
        
    }
    
    @Operation(summary = "Add given print run metadata for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addPrintrun", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added print run"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to register print runs"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String addPrintrun (
            @Parameter(required=true, description="Print run metadata to be added") 
            @RequestBody PrintRun printer, 
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        // validate first
        if (validator != null) {
            if  (printer.getName() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "name", printer.getName());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("name", "LengthExceeded"));
                }       
            }
            
            if  (printer.getDescription() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "description", printer.getDescription());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("description", "LengthExceeded"));
                }       
            }
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        Boolean validate = true;
        if (user.hasRole("ROLE_DATA")) 
            validate = false;
        
        if (printer.getName() == null || printer.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        if (printer.getTemplate() == null || printer.getTemplate().isEmpty()) {
            errorMessage.addError(new ObjectError("type", "NoEmpty"));
        }
        
        // check for duplicate name
        try {
            MetadataCategory metadata = metadataRepository.getMetadataByLabel(printer.getName().trim(), ArrayDatasetRepositoryImpl.printRunTypePredicate, user);
            if (metadata != null) {
                errorMessage.addError(new ObjectError("name", "Duplicate"));
            }
        } catch (SparqlException | SQLException e2) {
            throw new GlycanRepositoryException("Error checking for duplicate metadata", e2);
        }
        
        // check if the template exists
        try {
            String templateURI = templateRepository.getTemplateByName(printer.getTemplate(), MetadataTemplateType.PRINTRUN);
            if (templateURI == null) {
                errorMessage.addError(new ObjectError("type", "NotValid"));
            }
            else {
                // validate mandatory/multiple etc.
                if (validate != null && validate) {
                    MetadataTemplate template = templateRepository.getTemplateFromURI(templateURI);
                    ErrorMessage err = validateMetadata (printer, template);
                    if (err != null) {
                        for (ObjectError error: err.getErrors())
                            errorMessage.addError(error);
                    }    
                }
            }
        } catch (SparqlException | SQLException e1) {
            logger.error("Error retrieving template", e1);
            throw new GlycanRepositoryException("Error retrieving printer template " + p.getName(), e1);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid printer information", errorMessage);
        
        try {
            String uri = metadataRepository.addPrintRun(printer, user);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Printer cannot be added for user " + p.getName(), e);
        }
        
    }
    
    @Operation(summary = "Add given assay metadata for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addAssayMetadata", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added assay metadata"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to register assay metadata"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String addAssayMetadata (
            @Parameter(required=true, description="Assay metadata to be added") 
            @RequestBody AssayMetadata metadata, 
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        // validate first
        if (validator != null) {
            if (metadata.getName() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "name", metadata.getName());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("name", "LengthExceeded"));
                }       
            }
            
            if (metadata.getDescription() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "description", metadata.getDescription());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("description", "LengthExceeded"));
                }       
            }
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        Boolean validate = true;
        if (user.hasRole("ROLE_DATA")) 
            validate = false;
        
        if (metadata.getName() == null || metadata.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        if (metadata.getTemplate() == null || metadata.getTemplate().isEmpty()) {
            errorMessage.addError(new ObjectError("type", "NoEmpty"));
        }
        
        // check for duplicate name
        try {
            MetadataCategory existing = metadataRepository.getMetadataByLabel(metadata.getName().trim(), ArrayDatasetRepositoryImpl.assayTypePredicate, user);
            if (existing != null) {
                errorMessage.addError(new ObjectError("name", "Duplicate"));
            }
        } catch (SparqlException | SQLException e2) {
            throw new GlycanRepositoryException("Error checking for duplicate metadata", e2);
        }
        
        // check if the template exists
        try {
            String templateURI = templateRepository.getTemplateByName(metadata.getTemplate(), MetadataTemplateType.ASSAY);
            if (templateURI == null) {
                errorMessage.addError(new ObjectError("type", "NotValid"));
            }
            else {
                // validate mandatory/multiple etc.
                if (validate != null && validate) {
                    MetadataTemplate template = templateRepository.getTemplateFromURI(templateURI);
                    ErrorMessage err = validateMetadata (metadata, template);
                    if (err != null) {
                        for (ObjectError error: err.getErrors())
                            errorMessage.addError(error);
                    }    
                }
            }
        } catch (SparqlException | SQLException e1) {
            logger.error("Error retrieving template", e1);
            throw new GlycanRepositoryException("Error retrieving assay template " + p.getName(), e1);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid assay information", errorMessage);
        
        try {
            String uri = metadataRepository.addAssayMetadata(metadata, user);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Assay metadata cannot be added for user " + p.getName(), e);
        }
        
    }
    
    @Operation(summary = "Add given spot metadata for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addSpotMetadata", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added spot metadata"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to register spot metadata"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String addSpotMetadata (
            @Parameter(required=true, description="Spot metadata to be added") 
            @RequestBody SpotMetadata metadata, 
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        // validate first
        if (validator != null) {
            if (metadata.getName() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "name", metadata.getName());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("name", "LengthExceeded"));
                }       
            }
            
            if (metadata.getDescription() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "description", metadata.getDescription());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("description", "LengthExceeded"));
                }       
            }
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        Boolean validate = true;
        if (user.hasRole("ROLE_DATA")) 
            validate = false;
        
        if (metadata.getName() == null || metadata.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        if (metadata.getTemplate() == null || metadata.getTemplate().isEmpty()) {
            errorMessage.addError(new ObjectError("type", "NoEmpty"));
        }
        
        // check for duplicate name
        try {
            MetadataCategory existing = metadataRepository.getMetadataByLabel(metadata.getName().trim(), ArrayDatasetRepositoryImpl.spotMetadataTypePredicate, user);
            if (existing != null) {
                errorMessage.addError(new ObjectError("name", "Duplicate"));
            }
        } catch (SparqlException | SQLException e2) {
            throw new GlycanRepositoryException("Error checking for duplicate metadata", e2);
        }
        
        // check if the template exists
        try {
            String templateURI = templateRepository.getTemplateByName(metadata.getTemplate(), MetadataTemplateType.SPOT);
            if (templateURI == null) {
                errorMessage.addError(new ObjectError("type", "NotValid"));
            }
            else {
                // validate mandatory/multiple etc.
                if (validate != null && validate) {
                    MetadataTemplate template = templateRepository.getTemplateFromURI(templateURI);
                    ErrorMessage err = validateMetadata (metadata, template);
                    if (err != null) {
                        for (ObjectError error: err.getErrors())
                            errorMessage.addError(error);
                    }    
                }
            }
        } catch (SparqlException | SQLException e1) {
            logger.error("Error retrieving template", e1);
            throw new GlycanRepositoryException("Error retrieving assay template " + p.getName(), e1);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid assay information", errorMessage);
        
        try {
            String uri = metadataRepository.addSpotMetadata(metadata, user);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Spot metadata cannot be added for user " + p.getName(), e);
        }
    }
    
    @Operation(summary = "Import processed data results from uploaded excel file", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/addProcessedDataFromExcel", method=RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added processed data for the given raw data of the given array dataset"), 
            @ApiResponse(responseCode="400", description="Invalid request, file cannot be found"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to modify array datasets"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String addProcessedDataFromExcel (
            @Parameter(required=true, description="processed data file details such as name, original name, folder, format") 
            @RequestBody
            FileWrapper file,
            @Parameter(required=true, description="id of the array dataset (must already be in the repository) to add the processed data") 
            @RequestParam("arraydatasetId")
            String datasetId,  
            @Parameter(required=true, description="id of the raw data (must already be in the repository) to add the processed data") 
            @RequestParam("rawdataId")
            String rawDataId,  
            @Parameter(required=false, description="Data processing software metadata id (must already be in the repository)") 
            @RequestParam(value="metadataId", required=false)
            String metadataId,
            @Parameter(required=true, description="the statistical method used (eg. eliminate, average etc.") 
            @RequestParam("methodName")
            String methodName,
            @Parameter(required=false, description="uploaded file with the exclusion information")
            @RequestParam(value="exclusionFile", required=false) String exclusionFile,
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        boolean allowPartialData = false;
        if (user.hasRole("ROLE_DATA"))
            allowPartialData = true;
                  
        // check if metadata exists!
        DataProcessingSoftware metadata = null;
        ArrayDataset dataset;
        UserEntity owner = user;
        // check if the dataset with the given id exists
        try {
            dataset = datasetRepository.getArrayDataset(datasetId, false, user);
            if (dataset == null) {
                // check if the user can access this dataset as a co-owner
                String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                if (coOwnedGraph != null) {
                    UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                    if (originalUser != null) {
                        dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                        owner = originalUser;
                    }
                }
            }
            if (dataset == null) {
                errorMessage.addError(new ObjectError("dataset", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve dataset from the repository", e);
        }
        
        RawData rawData = null;
        // check if the rawData with the given id exists
        try {
            rawData = datasetRepository.getRawDataFromURI(GlygenArrayRepositoryImpl.uriPrefix + rawDataId, false, user);
            if (rawData == null) {
                errorMessage.addError(new ObjectError("rawdata", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
            } else {
                // check if it is done
                if (rawData.getStatus() != FutureTaskStatus.DONE) {
                    errorMessage.addError(new ObjectError("rawdata", "NotDone"));
                    errorMessage.setErrorCode(ErrorCodes.DISABLED);
                }
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("RawData " + rawDataId + " cannot be retrieved for user " + p.getName(), e);
        }
        
        Slide mySlide = null;
        // need to locate the slide from the dataset and image
        if (dataset != null && dataset.getSlides() != null) {
            for (Slide slide: dataset.getSlides()) {
                if (slide.getImages() != null) {
                    for (Image i: slide.getImages()) {
                        if (i.getRawDataList() != null) {
                            for (RawData r: i.getRawDataList()) {
                                if (r.getId().equals(rawDataId)) {
                                    mySlide = slide;
                                    break;
                                }
                            }
                        }
                        
                    }
                }
            }
        }
        
        if (mySlide == null) {
            errorMessage.addError(new ObjectError("slide", "NotFound"));
            errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
        }
            
        if (metadataId != null) {    
            try {
                metadata = metadataRepository.getDataProcessingSoftwareFromURI(GlygenArrayRepositoryImpl.uriPrefix + metadataId, owner);
                if (metadata == null) {
                    errorMessage.addError(new ObjectError("metadata", "NotFound"));
                    errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
                }
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Cannot retrieve data processing software metadata", e);
            }
        } else {
            if (!allowPartialData) {
                errorMessage.addError(new ObjectError("metadata", "NoEmpty"));
                errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
            }
        }
        ProcessedData processedData = new ProcessedData();     
        processedData.setMetadata(metadata);
        if (file == null) {
            // error
            errorMessage.addError(new ObjectError("file", "NotValid"));
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        } else {
            String folder = uploadDir;
            if (file.getFileFolder() != null) {
                folder = file.getFileFolder();
            }
            File excelFile = new File(folder, file.getIdentifier());
            if (excelFile.exists()) {
                // move the file to the experiment folder
                // create a folder for the experiment, if it does not exists, and move the file into that folder
                File experimentFolder = new File (uploadDir + File.separator + datasetId);
                if (!experimentFolder.exists()) {
                    experimentFolder.mkdirs();
                }
                File newFile = new File(experimentFolder + File.separator + file.getIdentifier());
                if(excelFile.renameTo (newFile)) { 
                         // if file copied successfully then delete the original file 
                    excelFile.delete(); 
                } else { 
                    throw new GlycanRepositoryException("File cannot be moved to the dataset folder");
                }
                file.setFileFolder(uploadDir + File.separator + datasetId);
                file.setFileSize(newFile.length());
                file.setCreatedDate(new Date());
                file.setDrsId(file.getIdentifier().substring(0, file.getIdentifier().lastIndexOf(".")));
                file.setExtension(file.getIdentifier().substring(file.getIdentifier().lastIndexOf(".")+1));
                GlygenArrayController.calculateChecksum (file);
                processedData.setFile(file);
            } else {
                // error
                errorMessage.addError(new ObjectError("file", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
            }
        }
        
        try {
            List<StatisticalMethod> methods = templateRepository.getAllStatisticalMethods();
            StatisticalMethod found = null;
            for (StatisticalMethod method: methods) {
                if (method.getName().equalsIgnoreCase(methodName)) {
                    found = method;
                }
            }
            if (found == null) {
                errorMessage.addError(new ObjectError("method", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            } else {
                processedData.setMethod(found);
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve statistical methods from the repository", e);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
            throw new IllegalArgumentException("Invalid Input: Not a valid processed data information", errorMessage);
        }
        try {
            UserEntity originalUser = owner;
            
            // save whatever we have for now for processed data and update its status to "processing"
            String uri = datasetRepository.addProcessedData(processedData, rawDataId, owner);  
            processedData.setUri(uri);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            if (processedData.getError() == null)
                processedData.setStatus(FutureTaskStatus.PROCESSING);
            else 
                processedData.setStatus(FutureTaskStatus.ERROR);
            datasetRepository.updateStatus (uri, processedData, owner);
            if (exclusionFile != null) {
                try {
                    File f = new File (uploadDir, exclusionFile);
                    if (f.exists()) {
                        ExclusionInfoParser parser = new ExclusionInfoParser(featureRepository);
                        ProcessedData emptyData = parser.parse(f.getAbsolutePath(), owner);
                        processedData.setTechnicalExclusions(emptyData.getTechnicalExclusions());
                        processedData.setFilteredDataList(emptyData.getFilteredDataList());
                        //TODO do we need to check if the listed features belong to the slide of this processed data?
                        datasetRepository.addExclusionInfoToProcessedData(processedData, owner);
                    } else {
                        errorMessage.addError(new ObjectError("exclusionFile", "NotValid"));
                        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                        processedData.setStatus(FutureTaskStatus.ERROR);
                        processedData.setError(errorMessage);
                        datasetRepository.updateStatus (uri, processedData, originalUser);
                        //throw new IllegalArgumentException("File cannot be found", errorMessage);
                    }
                } catch (Exception e) {
                    if (e.getCause() instanceof ErrorMessage) {
                        processedData.setError((ErrorMessage) e.getCause());
                    } else {
                        errorMessage.addError(new ObjectError("exclusionFile", e.getMessage()));
                        errorMessage.setErrorCode(ErrorCodes.PARSE_ERROR);
                        processedData.setError(errorMessage);
                    }
                    processedData.setStatus(FutureTaskStatus.ERROR);
                    datasetRepository.updateStatus (uri, processedData, originalUser);
                    return id;
                }
            }
            
            CompletableFuture<List<Intensity>> intensities = null;
            try {
                intensities = parserAsyncService.parseProcessDataFile(datasetId, file, mySlide, owner);
                if (intensities.isCompletedExceptionally()) {
                    logger.error("processed data completed exceptionally!!!");
                }
                intensities.whenComplete((intensity, e) -> {
                    try {
                        //String uri = processedData.getUri();
                        if (e != null) {
                            logger.error(e.getMessage(), e);
                            processedData.setStatus(FutureTaskStatus.ERROR);
                            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException && e.getCause().getCause() instanceof ErrorMessage) 
                                processedData.setError((ErrorMessage) e.getCause().getCause());
                            
                        } else {
                            processedData.setIntensity(intensity);
                            file.setFileFolder(uploadDir + File.separator + datasetId);
                            processedData.setFile(file);
                            datasetRepository.addIntensitiesToProcessedData(processedData, originalUser);
                            processedData.setStatus(FutureTaskStatus.DONE);                            
                        }
                    
                        datasetRepository.updateStatus (uri, processedData, originalUser);
                    } catch (SparqlException | SQLException e1) {
                        logger.error("Could not save the processedData", e1);
                        errorMessage.addError(new ObjectError("processedData", "Cannot complete processing. Reason:" + e1.getMessage()));
                        throw new GlycanRepositoryException ("Could not save the processedData", errorMessage);
                    } 
                });
                processedData.setIntensity(intensities.get(5000, TimeUnit.MILLISECONDS));
            } catch (IllegalArgumentException e) {
                if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                    processedData.setError((ErrorMessage) e.getCause());
                } else {
                    errorMessage.addError(new ObjectError("processedData", "Cannot complete processing. Reason:" + e.getMessage()));
                    errorMessage.setErrorCode(ErrorCodes.PARSE_ERROR);
                    processedData.setError(errorMessage);
                }
                processedData.setStatus(FutureTaskStatus.ERROR);
                datasetRepository.updateStatus (processedData.getUri(), processedData, originalUser);
            } catch (TimeoutException e) {
                synchronized (this) {
                    if (processedData.getError() == null)
                        processedData.setStatus(FutureTaskStatus.PROCESSING);
                    else 
                        processedData.setStatus(FutureTaskStatus.ERROR);
                    datasetRepository.updateStatus (uri, processedData, originalUser);
                    return id;
                }
            } catch (Exception e) {
                if (e instanceof IllegalArgumentException && e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                    processedData.setError((ErrorMessage) e.getCause());
                } else if (e.getCause() instanceof IllegalArgumentException && e.getCause().getCause() != null &&
                        e.getCause().getCause() instanceof ErrorMessage) {
                    processedData.setError((ErrorMessage) e.getCause().getCause());
                } else {
                    errorMessage.addError(new ObjectError ("exception", e.getMessage()));
                    processedData.setError(errorMessage);
                }
                processedData.setStatus(FutureTaskStatus.ERROR);
                datasetRepository.updateStatus (processedData.getUri(), processedData, originalUser);
                logger.error("Cannot add the intensities to the repository", e);
                return processedData.getUri().substring(processedData.getUri().lastIndexOf("/")+1);
                //throw new IllegalArgumentException("Cannot add the intensitites to the repository", e);
            }
            
            return id;
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException)
                throw (IllegalArgumentException)e;
            else if (e.getCause() instanceof IllegalArgumentException) {
                throw (IllegalArgumentException)e.getCause();
            }
            else throw new GlycanRepositoryException("Cannot add the processed data to the repository", e);
        }
    }
    
    @Operation(summary = "Download exclusion lists for the processed data to a file", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/downloadProcessedDataExclusionInfo", method=RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="File downloaded successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, file cannot be found"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to add array datasets"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<Resource> downloadProcessedDataExclusionInfo (
            @Parameter(required=true, description="id of the array dataset (must already be in the repository) to retrieve the exclusion info") 
            @RequestParam("arraydatasetId")
            String datasetId,
            @Parameter(required=true, description="the name for downloaded file") 
            @RequestParam("filename")
            String fileName,        
            @Parameter(required=true, description="an existing processed data id") 
            @RequestParam("processedDataId")
            String processedDataId,
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        ArrayDataset dataset;
        UserEntity owner = user;
        // check if the dataset with the given id exists
        try {
            dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, user);
            if (dataset == null) {
                // check if the user can access this dataset as a co-owner
                String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId.trim());
                if (coOwnedGraph != null) {
                    UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                    if (originalUser != null) {
                        dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, originalUser);
                        owner = originalUser;
                    }
                }
            }
            if (dataset == null)
                errorMessage.addError(new ObjectError("dataset", "NotFound"));
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve dataset from the repository", e);
        }
        
        String uri = GlygenArrayRepositoryImpl.uriPrefix + processedDataId;
        File newFile = new File (uploadDir, "tmp" + fileName);
        
        try {
            ProcessedData existing = datasetRepository.getProcessedDataFromURI(uri, false, owner);
            if (existing == null) {
                errorMessage.addError(new ObjectError("id", "NotFound"));
                throw new IllegalArgumentException("Processed data cannot be found in the repository", errorMessage);
            }
            
            try {
                ExclusionInfoParser.exportToFile(existing, newFile.getAbsolutePath());
            } catch (IOException e) {
                errorMessage.addError(new ObjectError("file", "NotFound"));
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new GlycanRepositoryException("Cannot retrieve processed data from the repository", e);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return download(newFile, fileName);
    }
    
    
    @Operation(summary = "Add the exclusion info given in the file to the given processed data", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/addExclusionInfoFromFile", method=RequestMethod.POST)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return an (otherwise) empty processed data object containing only the exclusion lists"), 
            @ApiResponse(responseCode="400", description="Invalid request, file cannot be found"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String addExclusionInfoFromFile (
            @Parameter(required=true, description="id of the array dataset (must already be in the repository) to add the processed data") 
            @RequestParam("arraydatasetId")
            String datasetId, 
            @Parameter(required=true, description="uploaded file with the exclusion information")
            @RequestParam("file") String uploadedFileName,
            @Parameter(required=true, description="processed data to add the exclusion information")
            @RequestParam("processeddataid") String processedDataId,
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        
        ArrayDataset dataset;
        UserEntity owner = user;
        // check if the dataset with the given id exists
        try {
            dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, user);
            if (dataset == null) {
                // check if the user can access this dataset as a co-owner
                String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId.trim());
                if (coOwnedGraph != null) {
                    UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                    if (originalUser != null) {
                        dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, originalUser);
                        owner = originalUser;
                    }
                }
            }
            if (dataset == null) {
                errorMessage.addError(new ObjectError("dataset", "NotFound"));
                throw new IllegalArgumentException("dataset cannot be found", errorMessage);
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve dataset from the repository", e);
        }
        
        try {
            ProcessedData existing = datasetRepository.getProcessedDataFromURI(GlygenArrayRepositoryImpl.uriPrefix + processedDataId, false, owner);
            if (existing == null) {
                errorMessage.addError(new ObjectError("id", "NotFound"));
            } else {
                File file = new File (uploadDir, uploadedFileName);
                if (file.exists()) {
                    ExclusionInfoParser parser = new ExclusionInfoParser(featureRepository);
                    ProcessedData emptyData = parser.parse(file.getAbsolutePath(), user);
                    existing.setTechnicalExclusions(emptyData.getTechnicalExclusions());
                    existing.setFilteredDataList(emptyData.getFilteredDataList());
                    //TODO do we need to check if the listed features belong to the slide of this processed data?
                    datasetRepository.addExclusionInfoToProcessedData(existing, user);
                    return existing.getId();
                } else {
                    errorMessage.addError(new ObjectError("file", "NotValid"));
                    throw new IllegalArgumentException("File cannot be found", errorMessage);
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new GlycanRepositoryException("Cannot retrieve processed data from the repository", e);
        }
        
        return null;
    }
    
    @Operation(summary = "Update processed data with results from uploaded excel file", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/updateProcessedDataFromExcel", method=RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added processed data for the given array dataset"), 
            @ApiResponse(responseCode="400", description="Invalid request, file cannot be found"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to add array datasets"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String updateProcessedDataFromExcel (
            @Parameter(required=true, description="id of the array dataset (must already be in the repository) to add the processed data") 
            @RequestParam("arraydatasetId")
            String datasetId,      
            @Parameter(required=true, description="id of the raw data (must already be in the repository) to add the processed data") 
            @RequestParam("rawdataId")
            String rawDataId, 
            @Parameter(required=true, description="processed data with an existing id/uri. If file is provided, the new file information is used. "
                    + "If not, existing file is used for processing") 
            @RequestBody
            ProcessedData processedData,
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        
        ArrayDataset dataset;
        UserEntity owner = user;
        // check if the dataset with the given id exists
        try {
            dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, user);
            if (dataset == null) {
                // check if the user can access this dataset as a co-owner
                String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId.trim());
                if (coOwnedGraph != null) {
                    UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                    if (originalUser != null) {
                        dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, originalUser);
                        owner = originalUser;
                    }
                }
            }
            if (dataset == null)
                errorMessage.addError(new ObjectError("dataset", "NotFound"));
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve dataset from the repository", e);
        }
        
        String uri = processedData.getUri();
        if (uri == null && processedData.getId() != null) {
            uri = GlygenArrayRepositoryImpl.uriPrefix + processedData.getId();
            processedData.setUri(uri);
        }
        
        if (uri == null) {
            errorMessage.addError(new ObjectError("id", "NoEmpty"));
            throw new IllegalArgumentException("Processed data should have an existing id", errorMessage);
        }
        
        String id = processedData.getId();
        if (id == null) {
            id = uri.substring(uri.lastIndexOf("/")+1);
        }
        
        FileWrapper file = processedData.getFile();
        
        Slide mySlide = null;
        // need to locate the slide from the dataset and image
        if (dataset != null && dataset.getSlides() != null) {
            for (Slide slide: dataset.getSlides()) {
                if (slide.getImages() != null) {
                    for (Image i: slide.getImages()) {
                        if (i.getRawDataList() != null) {
                            for (RawData r: i.getRawDataList()) {
                                if (r.getId().equals(rawDataId)) {
                                    mySlide = slide;
                                    break;
                                }
                            }
                        }
                        
                    }
                }
            }
        }
        
        if (mySlide == null) {
            errorMessage.addError(new ObjectError("slide", "NotFound"));
        }
        
        try {
            ProcessedData existing = datasetRepository.getProcessedDataFromURI(uri, true, owner);
            if (existing == null) {
                errorMessage.addError(new ObjectError("id", "NotFound"));
                throw new IllegalArgumentException("Processed data cannot be found in the repository", errorMessage);
            }
            
            if (existing.getStatus() == FutureTaskStatus.DONE) {
                errorMessage.addError(new ObjectError("status", "NotAllowed"));
                throw new IllegalArgumentException("The processing has finished already. No updates are allowed", errorMessage);
            }
            
            // check the timestamp and see if enough time has passed
            Long timeDelay = 3600L;
            SettingEntity entity = settingsRepository.findByName("timeDelay");
            if (entity != null) {
                timeDelay = Long.parseLong(entity.getValue());
            }
           
            Date current = new Date();
            Date startDate = existing.getStartDate();
            if (startDate != null) {
                long diffInMillies = Math.abs(current.getTime() - startDate.getTime());
                if (timeDelay > diffInMillies / 1000) {
                    // not enough time has passed, cannot restart!
                    errorMessage.addError(new ObjectError("time", "NotValid"));
                    throw new IllegalArgumentException("Not enough time has passed. Please wait before restarting", errorMessage);
                }
            }
            
            if (file == null) { // if file is not included in update, we use the existing file
                processedData.setFile(existing.getFile());
            }
            processedData.setMethod(existing.getMethod());
            processedData.setMetadata(existing.getMetadata());
                
            UserEntity originalUser = owner;
            CompletableFuture<List<Intensity>> intensities = null;
            try {
                intensities = parserAsyncService.parseProcessDataFile(datasetId.trim(), file, mySlide, owner);
                intensities.whenComplete((intensity, e) -> {
                    try {
                        String processedURI = processedData.getUri();
                        if (e != null) {
                            logger.error(e.getMessage(), e);
                            processedData.setStatus(FutureTaskStatus.ERROR);
                            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException && e.getCause().getCause() instanceof ErrorMessage) 
                                processedData.setError((ErrorMessage) e.getCause().getCause());
                            
                        } else {
                            processedData.setIntensity(intensity);
                            file.setFileFolder(uploadDir + File.separator + datasetId.trim());
                            processedData.setFile(file);
                            datasetRepository.addIntensitiesToProcessedData(processedData, originalUser);
                            processedData.setStatus(FutureTaskStatus.DONE);
                        }
                    
                        datasetRepository.updateStatus (processedURI, processedData, originalUser);
                    } catch (SparqlException | SQLException e1) {
                        logger.error("Could not save the processedData", e1);
                    } 
                });
                processedData.setIntensity(intensities.get(20000, TimeUnit.MILLISECONDS));
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (TimeoutException e) {
                synchronized (this) {
                    if (processedData.getError() == null)
                        processedData.setStatus(FutureTaskStatus.PROCESSING);
                    else 
                        processedData.setStatus(FutureTaskStatus.ERROR);
                    datasetRepository.updateStatus (uri, processedData, owner);
                    return id;
                }
            }
                
            return processedData.getId();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new GlycanRepositoryException("Cannot retrieve processed data from the repository", e);
        }
        
    }
    
    
    @Operation(summary = "Add given sample metadata for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addSample", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added sample"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to register samples"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String addSample (
            @Parameter(required=true, description="Sample metadata to be added") 
            @RequestBody Sample sample, 
            @Parameter(required=false, schema = @Schema(type = "boolean", defaultValue="true"), description="bypass mandatory/multiplicty validation checks if set to false (not recommended)") 
            Principal p) {   
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        // validate first
        if (validator != null) {
            if  (sample.getName() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "name", sample.getName());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("name", "LengthExceeded"));
                }       
            }
            
            if  (sample.getDescription() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "description", sample.getDescription());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("description", "LengthExceeded"));
                }       
            }
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        Boolean validate = true;
        if (user.hasRole("ROLE_DATA")) 
            validate = false;
        
        if (sample.getName() == null || sample.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        if (sample.getTemplate() == null || sample.getTemplate().isEmpty()) {
            errorMessage.addError(new ObjectError("type", "NoEmpty"));
        }
        
        // check for duplicate name
        try {
            MetadataCategory existing = metadataRepository.getMetadataByLabel(sample.getName().trim(), ArrayDatasetRepositoryImpl.sampleTypePredicate, user);
            if (existing != null) {
                errorMessage.addError(new ObjectError("name", "Duplicate"));
            }
        } catch (SparqlException | SQLException e2) {
            throw new GlycanRepositoryException("Error checking for duplicate metadata", e2);
        }
        
        // check if the template exists
        String templateURI = null;
        try {
            
            if (sample.getTemplate() != null && !sample.getTemplate().isEmpty())
                templateURI = templateRepository.getTemplateByName(sample.getTemplate(), MetadataTemplateType.SAMPLE);
            if (templateURI == null) {
                errorMessage.addError(new ObjectError("type", "NotValid"));
            }
            else {
                // validate mandatory/multiple etc.
                if (validate != null && validate) {
                    MetadataTemplate template = templateRepository.getTemplateFromURI(templateURI);
                    ErrorMessage err = validateMetadata (sample, template);
                    if (err != null) {
                        for (ObjectError error: err.getErrors())
                            errorMessage.addError(error);
                    }    
                }
            }
        } catch (SparqlException | SQLException e1) {
            logger.error("Error retrieving template", e1);
            throw new GlycanRepositoryException("Error retrieving sample template " + p.getName(), e1);
        }
        

        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid sample information", errorMessage);
        
        try {
            String uri = metadataRepository.addSample(sample, user);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Sample cannot be added for user " + p.getName(), e);
        }
        
    }
    
    @Operation(summary = "Add given scanner metadata for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addScanner", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added scanner"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to register scanners"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String addScanner (
            @Parameter(required=true, description="Scanner metadata to be added") 
            @RequestBody ScannerMetadata metadata, 
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        // validate first
        if (validator != null) {
            if  (metadata.getName() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "name", metadata.getName());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("name", "LengthExceeded"));
                }       
            }
            
            if  (metadata.getDescription() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "description", metadata.getDescription());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("description", "LengthExceeded"));
                }       
            }
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        Boolean validate = true;
        if (user.hasRole("ROLE_DATA")) 
            validate = false;
        
        if (metadata.getName() == null || metadata.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        if (metadata.getTemplate() == null || metadata.getTemplate().isEmpty()) {
            errorMessage.addError(new ObjectError("type", "NoEmpty"));
        }
        
        // check for duplicate name
        try {
            MetadataCategory existing = metadataRepository.getMetadataByLabel(metadata.getName().trim(), ArrayDatasetRepositoryImpl.scannerTypePredicate, user);
            if (existing != null) {
                errorMessage.addError(new ObjectError("name", "Duplicate"));
            }
        } catch (SparqlException | SQLException e2) {
            throw new GlycanRepositoryException("Error checking for duplicate metadata", e2);
        }
        
        // check if the template exists
        try {
            String templateURI = templateRepository.getTemplateByName(metadata.getTemplate(), MetadataTemplateType.SCANNER);
            if (templateURI == null) {
                errorMessage.addError(new ObjectError("type", "NotValid"));
            }
            else {
                // validate mandatory/multiple etc.
                if (validate != null && validate) {
                    MetadataTemplate template = templateRepository.getTemplateFromURI(templateURI);
                    ErrorMessage err = validateMetadata (metadata, template);
                    if (err != null) {
                        for (ObjectError error: err.getErrors())
                            errorMessage.addError(error);
                    }    
                }
            }
        } catch (SparqlException | SQLException e1) {
            logger.error("Error retrieving template", e1);
            throw new GlycanRepositoryException("Error retrieving scanner template " + p.getName(), e1);
        }
       
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid scanner information", errorMessage);
        
        try {
            String uri = metadataRepository.addScannerMetadata(metadata, user);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Scanner cannot be added for user " + p.getName(), e);
        }
        
    }
    
    
    @Operation(summary = "Add given slide metadata for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/addSlideMetadata", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="return id for the newly added slide metadata"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to register slides"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String addSlideMetadata (
            @Parameter(required=true, description="Slide metadata to be added") 
            @RequestBody SlideMetadata metadata, 
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        // validate first
        if (validator != null) {
            if  (metadata.getName() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "name", metadata.getName());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("name", "LengthExceeded"));
                }       
            }
            
            if  (metadata.getDescription() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "description", metadata.getDescription());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("description", "LengthExceeded"));
                }       
            }
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        Boolean validate = true;
        if (user.hasRole("ROLE_DATA")) 
            validate = false;
        
        if (metadata.getName() == null || metadata.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        if (metadata.getTemplate() == null || metadata.getTemplate().isEmpty()) {
            errorMessage.addError(new ObjectError("type", "NoEmpty"));
        }
        
        // check for duplicate name
        try {
            MetadataCategory existing = metadataRepository.getMetadataByLabel(metadata.getName().trim(), ArrayDatasetRepositoryImpl.slideTemplateTypePredicate, user);
            if (existing != null) {
                errorMessage.addError(new ObjectError("name", "Duplicate"));
            }
        } catch (SparqlException | SQLException e2) {
            throw new GlycanRepositoryException("Error checking for duplicate metadata", e2);
        }
        
        // check if the template exists
        try {
            String templateURI = templateRepository.getTemplateByName(metadata.getTemplate(), MetadataTemplateType.SLIDE);
            if (templateURI == null) {
                errorMessage.addError(new ObjectError("type", "NotValid"));
            }
            else {
                // validate mandatory/multiple etc.
                if (validate != null && validate) {
                    MetadataTemplate template = templateRepository.getTemplateFromURI(templateURI);
                    ErrorMessage err = validateMetadata (metadata, template);
                    if (err != null) {
                        for (ObjectError error: err.getErrors())
                            errorMessage.addError(error);
                    }  
                }
            }
        } catch (SparqlException | SQLException e1) {
            logger.error("Error retrieving template", e1);
            throw new GlycanRepositoryException("Error retrieving slide metadata template " + p.getName(), e1);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid slide metadata information", errorMessage);
        
        try {
            String uri = metadataRepository.addSlideMetadata(metadata, user);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Slide metadata cannot be added for user " + p.getName(), e);
        }
        
    }
    
    @GetMapping("/availableMetadataname")
    @Operation(summary="Checks whether the given name is available to be used (returns true if available, false if already in use", security = { @SecurityRequirement(name = "bearer-key") })
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Check performed successfully", content = {
            @Content( schema = @Schema(implementation = Boolean.class))}), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Boolean checkMetadataName(@RequestParam("name") final String name, 
            @RequestParam("metadatatype")
            MetadataTemplateType type, Principal p) {
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        String typePredicate = null;
        switch (type) {
        case SAMPLE:
            typePredicate = ArrayDatasetRepositoryImpl.sampleTypePredicate;
            break;
        case PRINTER: 
            typePredicate = ArrayDatasetRepositoryImpl.printerTypePredicate;
            break;
        case SCANNER:
            typePredicate = ArrayDatasetRepositoryImpl.scannerTypePredicate;
            break;
        case SLIDE:
            typePredicate = ArrayDatasetRepositoryImpl.slideTemplateTypePredicate;
            break;
        case DATAPROCESSINGSOFTWARE: 
            typePredicate = ArrayDatasetRepositoryImpl.dataProcessingTypePredicate;
            break;
        case IMAGEANALYSISSOFTWARE:
            typePredicate = ArrayDatasetRepositoryImpl.imageAnalysisTypePredicate;
            break;
        case ASSAY:
            typePredicate = ArrayDatasetRepositoryImpl.assayTypePredicate;
            break;
        case SPOT:
            typePredicate = ArrayDatasetRepositoryImpl.spotMetadataTypePredicate;
            break;
        case FEATURE:
            typePredicate = ArrayDatasetRepositoryImpl.featureMetadataTypePredicate;
            break;
        case PRINTRUN:
            typePredicate = ArrayDatasetRepositoryImpl.printRunTypePredicate;
            break;
        }
        MetadataCategory metadata = null;
        try {
            metadata = metadataRepository.getMetadataByLabel(name.trim(), typePredicate, user);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve metadata by name", e);
        }
        
        return metadata == null;
    }
    
    @GetMapping("/isMirageCompliant/{id}")
    @Operation(summary="Checks whether the given metadata contains all MIRAGE recommended descriptors", security = { @SecurityRequirement(name = "bearer-key") })
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Check performed successfully", content = {
            @Content( schema = @Schema(implementation = Boolean.class))}), 
            @ApiResponse(responseCode="400", description="Not mirage compliant, error message contains the missing (required) fields"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Boolean checkMirageCompliance(
            @PathVariable("id")
            String metadataId, 
            @RequestParam("type")
            MetadataTemplateType type,
            String datasetId,
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        MetadataCategory metadata = null;
        metadataId = metadataId.trim();
        
        if (datasetId != null)
            datasetId = datasetId.trim();
        try {
            switch (type) {
            case SAMPLE:
                metadata = getSample(metadataId, datasetId, p);
                break;
            case DATAPROCESSINGSOFTWARE:
                metadata = getDataProcessingSoftware(metadataId, datasetId, p);
                break;
            case IMAGEANALYSISSOFTWARE:
                metadata = getImageAnaylsisSoftware(metadataId, datasetId, p);
                break;
            case PRINTER:
                metadata = getPrinter(metadataId, datasetId, p);
                break;
            case SCANNER:
                metadata = getScanner(metadataId, datasetId, p);
                break;
            case SLIDE:
                metadata = getSlideMetadata(metadataId, datasetId, p);
                break;
            case ASSAY:
                metadata = getAssayMetadata(metadataId, datasetId, p);
            case SPOT:
                metadata = getSpotMetadata(metadataId, datasetId, p);
                break;
            case PRINTRUN:
                metadata = getPrintRun(metadataId, datasetId, p);
                break;
            case FEATURE:
                throw new IllegalArgumentException("Feature metadata compliance check is not supported");
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) 
                throw e;
            errorMessage.addError(new ObjectError("type", "NotValid"));
        }
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        // check if the template exists
        String templateURI = null;
        try {
            if (metadata != null && metadata.getTemplateType() != null && !metadata.getTemplateType().isEmpty()) {
                templateURI = MetadataTemplateRepository.templatePrefix + metadata.getTemplateType();
            }
            if (templateURI == null) {
                errorMessage.addError(new ObjectError("type", "NotValid"));
            }
            else {
                // validate mandatory/multiple etc.
                MetadataTemplate template = templateRepository.getTemplateFromURI(templateURI);
                if (template == null) {
                    errorMessage.addError(new ObjectError("type", "NotValid"));
                } else {
                    ErrorMessage err = checkMirage (metadata, template);
                    if (err != null) {
                        for (ObjectError error: err.getErrors())
                            errorMessage.addError(error);
                        metadata.setIsMirage(false);
                        // save it back to the repository
                        metadataRepository.updateMetadataMirage(metadata, user);
                    }   
                }
            }
        } catch (SparqlException | SQLException e1) {
            logger.error("Error retrieving template", e1);
            throw new GlycanRepositoryException("Error retrieving or saving metadata ", e1);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
            throw new IllegalArgumentException("Not mirage compliant", errorMessage);
        }
        
        try {
            metadata.setIsMirage(true);
            // save it back to the repository
            metadataRepository.updateMetadataMirage(metadata, user);
        } catch (SparqlException | SQLException e1) {
            throw new GlycanRepositoryException("Error updating mirage for metadata ", e1);
        }
        return true;
    }
    
    private ErrorMessage checkMirage(MetadataCategory metadata, MetadataTemplate template) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        
        //TODO need to consider mandateGroups, even if both is marked as "mirage", one should satisfy the requirement
        Map<Integer, List<Description>> mandateGroups = new HashMap<>();
        
        for (DescriptionTemplate descTemplate: template.getDescriptors()) {
            boolean exists = false;
            if (descTemplate.isGroup() && descTemplate.isMirage()) {
                if (descTemplate.getMandateGroup() != null) {
                    if (mandateGroups.get(descTemplate.getMandateGroup().getId()) == null) {
                        mandateGroups.put(descTemplate.getMandateGroup().getId(), new ArrayList<Description>());
                    }
                }
                // check if it is provided in metadata
                for (DescriptorGroup g: metadata.getDescriptorGroups()) {
                    if (g.getKey().equals(descTemplate)) {
                        if (descTemplate.getMandateGroup() != null) {
                            mandateGroups.get (descTemplate.getMandateGroup().getId()).add(g);
                        }
                        exists = true;
                        ErrorMessage error = checkMirageDescriptorGroup((DescriptorGroup)g, descTemplate);
                        if (error != null) {
                            for (ObjectError err: error.getErrors())
                                errorMessage.addError(err);
                        }  
                    }
                }
                
            } else if (descTemplate.isMirage()) {
                if (descTemplate.getMandateGroup() != null) {
                    if (mandateGroups.get(descTemplate.getMandateGroup().getId()) == null) {
                        mandateGroups.put(descTemplate.getMandateGroup().getId(), new ArrayList<Description>());
                    }
                }
                for (Descriptor d: metadata.getDescriptors()) {
                    if (d.getKey().equals(descTemplate)) {
                        if (descTemplate.getMandateGroup() != null) {
                            mandateGroups.get (descTemplate.getMandateGroup().getId()).add(d);
                        }
                        exists = true;
                    }
                }
            }
            if (descTemplate.isMirage() && !exists) {
                // but if there is another descriptorgroup in the mandategroup, it is still fine
                if (descTemplate.getMandateGroup() != null) {
                    if (mandateGroups.get(descTemplate.getMandateGroup().getId()) == null ||
                            mandateGroups.get(descTemplate.getMandateGroup().getId()).isEmpty()  ) {
                        // violation
                        errorMessage.addError(new ObjectError (descTemplate.getName(), "NotFound"));
                    }
                } else {
                    // violation
                    errorMessage.addError(new ObjectError (descTemplate.getName(), "NotFound"));
                }
            }
        }
        
        if (errorMessage.getErrors() == null || errorMessage.getErrors().isEmpty())
            return null;
        return errorMessage;
    }

    private ErrorMessage checkMirageDescriptorGroup(DescriptorGroup descGroup, DescriptionTemplate descGroupTemplate) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        
        Map<Integer, List<Description>> mandateGroups = new HashMap<>();
        
        for (DescriptionTemplate descTemplate: ((DescriptorGroupTemplate)descGroupTemplate).getDescriptors()) {
            boolean exists = false;
            if (descTemplate.getMandateGroup() != null) {
                if (mandateGroups.get(descTemplate.getMandateGroup().getId()) == null) {
                    mandateGroups.put(descTemplate.getMandateGroup().getId(), new ArrayList<Description>());
                }
            }
            for (Description d: descGroup.getDescriptors()) {
                if (d.getKey().equals(descTemplate)) {
                    exists = true;
                    if (descTemplate.getMandateGroup() != null) {
                        mandateGroups.get (descTemplate.getMandateGroup().getId()).add(d);
                    }
                    if (d.isGroup()) {
                        ErrorMessage error = checkMirageDescriptorGroup((DescriptorGroup)d, descTemplate);
                        if (error != null) {
                            for (ObjectError err: error.getErrors()) {
                                ObjectError o = new ObjectError (descGroupTemplate.getName() + ":" + err.getObjectName(), err.getDefaultMessage());
                                errorMessage.addError(o);
                            }
                        } 
                    }
                }
            }

            if (descTemplate.isMirage() && !exists) {
             // but if there is another descriptorgroup in the mandategroup, it is still fine
                if (descTemplate.getMandateGroup() != null) {
                    if (mandateGroups.get(descTemplate.getMandateGroup().getId()) == null ||
                            mandateGroups.get(descTemplate.getMandateGroup().getId()).isEmpty()  ) {
                        // violation
                        errorMessage.addError(new ObjectError (descGroupTemplate.getName() + ":" + descTemplate.getName(), "NotFound"));
                    }
                } else {
                    // violation
                    errorMessage.addError(new ObjectError (descGroupTemplate.getName() + ":" + descTemplate.getName(), "NotFound"));
                }
            }
        }
        
        if (errorMessage.getErrors() == null || errorMessage.getErrors().isEmpty())
            return null;
        return errorMessage;
    }

    @Operation(summary = "List all datasets for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listArrayDataset", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Array datasets retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public ArrayDatasetListView listArrayDataset (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="load rawdata and processed data details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, Principal p) {
        ArrayDatasetListView result = new ArrayDatasetListView();
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
            int total = datasetRepository.getArrayDatasetCountByUser(user, searchValue);
            
            List<ArrayDataset> resultList = datasetRepository.getArrayDatasetByUser(user, offset, limit, field, order, searchValue, loadAll);
            // check uploadStatus and set the field accordingly
            for (ArrayDataset d: resultList) {
                d.setUploadStatus(getDatasetStatus(d));
                if (!loadAll) {
                    // clear block layout info if exists
                    for (Slide slide: d.getSlides()) {
                        if (slide.getPrintedSlide() != null && slide.getPrintedSlide().getLayout() != null && slide.getPrintedSlide().getLayout().getBlocks() != null) {
                            for (Block b: slide.getPrintedSlide().getLayout().getBlocks()) {
                               if (b.getBlockLayout() != null) {
                                   b.getBlockLayout().setSpots(new ArrayList<Spot>());
                               }
                            }
                        }
                    }
                }
            }
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(resultList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve array datasets for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all datasets for the user (as a coowner)", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listArrayDatasetCoowner", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Array datasets retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public ArrayDatasetListView listArrayDatasetByCoowner (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="load rawdata and processed data details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, Principal p) {
        ArrayDatasetListView result = new ArrayDatasetListView();
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
            
            int total = datasetRepository.getArrayDatasetCountByCoOwner(user);
            
            List<ArrayDataset> resultList = datasetRepository.getArrayDatasetByCoOwner(user, offset, limit, field, order, searchValue, loadAll);
            // check uploadStatus and set the field accordingly
            for (ArrayDataset d: resultList) {
                d.setUploadStatus(getDatasetStatus(d));
                if (!loadAll) {
                    // clear block layout info if exists
                    for (Slide slide: d.getSlides()) {
                        if (slide.getPrintedSlide() != null && slide.getPrintedSlide().getLayout() != null && slide.getPrintedSlide().getLayout().getBlocks() != null) {
                            for (Block b: slide.getPrintedSlide().getLayout().getBlocks()) {
                               if (b.getBlockLayout() != null) {
                                   b.getBlockLayout().setSpots(new ArrayList<Spot>());
                               }
                            }
                        }
                    }
                }
            }
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(resultList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve array datasets for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List co-owners for the dataset", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listcoowners", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Co-owners retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public List<User> listCoownersForDataset(
            @Parameter(required=true, description="id of the array dataset for which to retrive the applicable coowners") 
            @RequestParam(value="arraydatasetId", required=true)
            String datasetId,
            Principal p) {
        
        List<User> users = new ArrayList<User>();
        List<GraphPermissionEntity> entities = permissionRepository.findByResourceIRI(GlygenArrayRepositoryImpl.uriPrefix + datasetId);
        for (GraphPermissionEntity e: entities) {
            UserEntity u = e.getUser();
            User user = new User();
            user.setUserName(u.getUsername());
            user.setFirstName(u.getFirstName());
            user.setLastName(u.getLastName());
            user.setAffiliation(u.getAffiliation());
            user.setGroupName(u.getGroupName());
            user.setDepartment(u.getDepartment());
            users.add(user);
        }
        return users;  
    }
    
    @Operation(summary = "List all printed slides for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listPrintedSlide", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Printed slides retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public PrintedSlideListView listPrintedSlide (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable slides") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        PrintedSlideListView result = new PrintedSlideListView();
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserEntity owner = user;
        
        if (datasetId != null) {
            // check if the dataset with the given id exists for this user, or if the user is the co-owner
            try {
                ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, user);
                if (dataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId.trim());
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, originalUser);
                            owner = originalUser;
                        }
                    }
                }
                
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
            }
        }
        
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
            
            int total = datasetRepository.getPrintedSlideCountByUser(owner, searchValue);
            
            List<PrintedSlide> resultList = datasetRepository.getPrintedSlideByUser(owner, offset, limit, field, order, searchValue, false);
            // clear unnecessary fields before sending the results back
            for (PrintedSlide slide: resultList) {
                if (slide.getLayout() != null)
                    slide.getLayout().setBlocks(null);
                if (slide.getPrinter() != null) {
                    slide.getPrinter().setDescriptorGroups(null);
                    slide.getPrinter().setDescriptors(null);
                }
                if (slide.getMetadata() != null) {
                    slide.getMetadata().setDescriptorGroups(null);
                    slide.getMetadata().setDescriptors(null);
                }
            }
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(resultList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve printed slides for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all printed slides for the user and the public ones", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listAllPrintedSlide", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Printed slides retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public PrintedSlideListView listAllPrintedSlides (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="load slide layout details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable slides") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        PrintedSlideListView result = new PrintedSlideListView();
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserEntity owner = user;
        
        if (datasetId != null) {
            // check if the dataset with the given id exists for this user, or if the user is the co-owner
            try {
                datasetId = datasetId.trim();
                ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                if (dataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                            owner = originalUser;
                        }
                    }
                }
                
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
            }
        }
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
            
            int total = datasetRepository.getPrintedSlideCountByUser(owner, searchValue);
            List<PrintedSlide> resultList = datasetRepository.getPrintedSlideByUser(owner, offset, limit, field, order, searchValue, loadAll);
            List<PrintedSlide> totalResultList = new ArrayList<PrintedSlide>();
            totalResultList.addAll(resultList);
            
            int totalPublic = datasetRepository.getPrintedSlideCountByUser(null, searchValue);
            
            List<PrintedSlide> publicResultList = datasetRepository.getPrintedSlideByUser(null, offset, limit, field, order, searchValue, loadAll);
            for (PrintedSlide slide: publicResultList) {
                boolean duplicate = false;
                for (PrintedSlide slide2: resultList) {
                    if (slide.getName().equals(slide2.getName())) {
                        duplicate = true;
                    }
                }
                if (!duplicate) {
                    totalResultList.add(slide);
                } 
            }
            
            // clear unnecessary fields before sending the results back
            for (PrintedSlide slide: totalResultList) {
                if (slide.getLayout() != null)
                    slide.getLayout().setBlocks(null);
                if (slide.getPrinter() != null) {
                    slide.getPrinter().setDescriptorGroups(null);
                    slide.getPrinter().setDescriptors(null);
                }
                if (slide.getMetadata() != null) {
                    slide.getMetadata().setDescriptorGroups(null);
                    slide.getMetadata().setDescriptors(null);
                }
            }
            result.setRows(totalResultList);
            result.setTotal(total+totalPublic);
            result.setFilteredTotal(totalResultList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve printed slides. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all data processing software metadata for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listDataProcessingSoftware", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Data processing software metadata list retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public MetadataListResultView listDataProcessingSoftware (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="load descriptor details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable slides") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        MetadataListResultView result = new MetadataListResultView();
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserEntity owner = user;
        
        if (datasetId != null) {
            // check if the dataset with the given id exists for this user, or if the user is the co-owner
            try {
                datasetId = datasetId.trim();
                ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                if (dataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                            owner = originalUser;
                        }
                    }
                }
                
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
            }
        }
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
            
            int total = metadataRepository.getDataProcessingSoftwareCountByUser(owner, searchValue);
            
            List<DataProcessingSoftware> metadataList = metadataRepository.getDataProcessingSoftwareByUser(owner, offset, limit, field, order, searchValue, loadAll);
            List<MetadataCategory> resultList = new ArrayList<MetadataCategory>();
            resultList.addAll(metadataList);
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(metadataList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve data processing software for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all image analysis software metadata for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listImageAnalysisSoftware", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Image analysis software metadata list retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public MetadataListResultView listImageAnalysisSoftware (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="load descriptor details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable slides") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        MetadataListResultView result = new MetadataListResultView();
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserEntity owner = user;
        
        if (datasetId != null) {
            // check if the dataset with the given id exists for this user, or if the user is the co-owner
            try {
                datasetId = datasetId.trim();
                ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                if (dataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                            owner = originalUser;
                        }
                    }
                }
                
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
            }
        }
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
            
            int total = metadataRepository.getImageAnalysisSoftwareCountByUser(owner, searchValue);
            
            List<ImageAnalysisSoftware> metadataList = metadataRepository.getImageAnalysisSoftwareByUser(owner, offset, limit, field, order, searchValue, loadAll);
            List<MetadataCategory> resultList = new ArrayList<MetadataCategory>();
            resultList.addAll(metadataList);
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(metadataList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve image analysis software for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all printer metadata for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listPrinters", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Printer list retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public MetadataListResultView listPrinters (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="load descriptor details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable slides") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        MetadataListResultView result = new MetadataListResultView();
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserEntity owner = user;
        
        if (datasetId != null) {
            // check if the dataset with the given id exists for this user, or if the user is the co-owner
            try {
                datasetId = datasetId.trim();
                ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                if (dataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                            owner = originalUser;
                        }
                    }
                }
                
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
            }
        }
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
            
            int total = metadataRepository.getPrinterCountByUser(owner, searchValue);
            
            List<Printer> metadataList = metadataRepository.getPrinterByUser(owner, offset, limit, field, order, searchValue, loadAll);
            List<MetadataCategory> resultList = new ArrayList<MetadataCategory>();
            resultList.addAll(metadataList);
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(metadataList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve printers for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all printer metadata for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listPrintruns", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Printer list retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public MetadataListResultView listPrintRuns (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="load descriptor details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable slides") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        MetadataListResultView result = new MetadataListResultView();
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserEntity owner = user;
        
        if (datasetId != null) {
            // check if the dataset with the given id exists for this user, or if the user is the co-owner
            try {
                datasetId = datasetId.trim();
                ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                if (dataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                            owner = originalUser;
                        }
                    }
                }
                
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
            }
        }
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
            
            int total = metadataRepository.getPrintRunCountByUser(owner, searchValue);
            
            List<PrintRun> metadataList = metadataRepository.getPrintRunByUser(owner, offset, limit, field, order, searchValue, loadAll);
            List<MetadataCategory> resultList = new ArrayList<MetadataCategory>();
            resultList.addAll(metadataList);
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(metadataList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve printers for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all samples for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listSamples", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Samples retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public MetadataListResultView listSamples (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="load descriptor details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable samples") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        MetadataListResultView result = new MetadataListResultView();
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserEntity owner = user;
        
        if (datasetId != null) {
            // check if the dataset with the given id exists for this user, or if the user is the co-owner
            try {
                datasetId = datasetId.trim();
                ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                if (dataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                            owner = originalUser;
                        }
                    }
                }
                
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
            }
        }
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
            
            int total = metadataRepository.getSampleCountByUser (owner, searchValue);
            
            List<Sample> metadataList = metadataRepository.getSampleByUser(owner, offset, limit, field, order, searchValue, loadAll);
            List<MetadataCategory> resultList = new ArrayList<MetadataCategory>();
            resultList.addAll(metadataList);
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(metadataList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve samples for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all scanner metadata for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listScanners", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Scanner list retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public MetadataListResultView listScanners (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="load descriptor details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable slides") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        MetadataListResultView result = new MetadataListResultView();
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserEntity owner = user;
        
        if (datasetId != null) {
            // check if the dataset with the given id exists for this user, or if the user is the co-owner
            try {
                datasetId = datasetId.trim();
                ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                if (dataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                            owner = originalUser;
                        }
                    }
                }
                
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
            }
        }
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
            
            int total = metadataRepository.getScannerMetadataCountByUser(owner, searchValue);
            
            List<ScannerMetadata> metadataList = metadataRepository.getScannerMetadataByUser(owner, offset, limit, field, order, searchValue, loadAll);
            List<MetadataCategory> resultList = new ArrayList<MetadataCategory>();
            resultList.addAll(metadataList);
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(metadataList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve scanners for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all slide metadata for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listSlideMetadata", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Slide metadata list retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public MetadataListResultView listSlideMetadata (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="load descriptor details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable slides") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        MetadataListResultView result = new MetadataListResultView();
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserEntity owner = user;
        
        if (datasetId != null) {
            // check if the dataset with the given id exists for this user, or if the user is the co-owner
            try {
                datasetId = datasetId.trim();
                ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                if (dataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                            owner = originalUser;
                        }
                    }
                }
                
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
            }
        }
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
            
            int total = metadataRepository.getSlideMetadataCountByUser (owner, searchValue);
            
            List<SlideMetadata> metadataList = metadataRepository.getSlideMetadataByUser(owner, offset, limit, field, order, searchValue, loadAll);
            List<MetadataCategory> resultList = new ArrayList<MetadataCategory>();
            resultList.addAll(metadataList);
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(metadataList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve slide metadata for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all assay metadata for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listAssayMetadata", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Assay metadata list retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public MetadataListResultView listAssayMetadata (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="load descriptor details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable slides") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        MetadataListResultView result = new MetadataListResultView();
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserEntity owner = user;
        
        if (datasetId != null) {
            // check if the dataset with the given id exists for this user, or if the user is the co-owner
            try {
                datasetId = datasetId.trim();
                ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                if (dataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                            owner = originalUser;
                        }
                    }
                }
                
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
            }
        }
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
            
            int total = metadataRepository.getAssayMetadataCountByUser(owner, searchValue);
            
            List<AssayMetadata> metadataList = metadataRepository.getAssayMetadataByUser(owner, offset, limit, field, order, searchValue, loadAll);
            List<MetadataCategory> resultList = new ArrayList<MetadataCategory>();
            resultList.addAll(metadataList);
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(metadataList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve assay metadata for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all spot metadata for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listSpotMetadata", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Spot metadata list retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public MetadataListResultView listSpotMetadata (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="load descriptor details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable slides") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        MetadataListResultView result = new MetadataListResultView();
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserEntity owner = user;
        
        if (datasetId != null) {
            // check if the dataset with the given id exists for this user, or if the user is the co-owner
            try {
                datasetId = datasetId.trim();
                ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                if (dataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                            owner = originalUser;
                        }
                    }
                }
                
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
            }
        }
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
            
            int total = metadataRepository.getSpotMetadataCountByUser(owner, searchValue);
            
            List<SpotMetadata> metadataList = metadataRepository.getSpotMetadataByUser(owner, offset, limit, field, order, searchValue, loadAll);
            List<MetadataCategory> resultList = new ArrayList<MetadataCategory>();
            resultList.addAll(metadataList);
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(metadataList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve spot metadata for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    private ErrorMessage validateDescriptorGroup(DescriptorGroup descGroup, DescriptionTemplate descGroupTemplate) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        
        for (DescriptionTemplate descTemplate: ((DescriptorGroupTemplate)descGroupTemplate).getDescriptors()) {
            boolean exists = false;
            int count = 0;
            for (Description d: descGroup.getDescriptors()) {
                DescriptionTemplate t = d.getKey();
                if (t == null) {
                    errorMessage.addError(new ObjectError ("template for " + d.getName(), "NotFound"));
                    continue;     // cannot check the validity if the temmplate is not available
                }
                if (t.getId() != null) {
                    if (t.getId().equals(descTemplate.getId())) {
                        exists = true;
                        count ++;
                        if (d.isGroup()) {
                            ErrorMessage error = validateDescriptorGroup((DescriptorGroup)d, descTemplate);
                            if (error != null) {
                                for (ObjectError err: error.getErrors())
                                    errorMessage.addError(err);
                            } 
                        } else {
                            // need to check the value 
                            if (descTemplate.isMandatory()) {
                                if (d.getNotRecorded() || d.getNotApplicable()) {
                                    // fine
                                } else if (((Descriptor) d).getValue() == null || ((Descriptor) d).getValue().isEmpty()) {
                                    errorMessage.addError(new ObjectError (t.getName() + "-value", "NotFound"));
                                }
                            }
                        }
                    }
                } else if (t.getUri() != null) {
                    if (t.getUri().equals(descTemplate.getUri())) {
                        exists = true;
                        count++;
                        if (d.isGroup()) {
                            ErrorMessage error = validateDescriptorGroup((DescriptorGroup)d, descTemplate);
                            if (error != null) {
                                for (ObjectError err: error.getErrors())
                                    errorMessage.addError(err);
                            } 
                        } else {
                            // need to check the value 
                            if (descTemplate.isMandatory()) {
                                if (d.getNotRecorded() || d.getNotApplicable()) {
                                    // fine
                                } else if (((Descriptor) d).getValue() == null || ((Descriptor) d).getValue().isEmpty()) {
                                    errorMessage.addError(new ObjectError (t.getName() + "-value", "NotFound"));
                                }
                            }
                        }
                    }
                }
            }

            if (descTemplate.isMandatory() && !exists) {
                if (descTemplate.getMandateGroup() == null) {   // if part of the mandate group, this descriptor group may not exist
                    // violation
                    errorMessage.addError(new ObjectError (descTemplate.getName() + "-mandatory", "NotFound"));
                }
            }
            if (descTemplate.getMaxOccurrence() == 1 && count > 1) {
             // violation
                errorMessage.addError(new ObjectError (descTemplate.getName() + "-maxOccurrence", "NumberExceeded"));
            }
        }
        
        if (errorMessage.getErrors() == null || errorMessage.getErrors().isEmpty())
            return null;
        return errorMessage;
    }

    ErrorMessage validateMetadata (MetadataCategory metadata, MetadataTemplate template) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        
        //TODO validate mandate groups to make sure xor/or criteria is satisfied
        
        for (DescriptionTemplate descTemplate: template.getDescriptors()) {
            // validate mandatory and multiplicity
            boolean exists = false;
            boolean valueExists = false;
            int count = 0;
            if (!descTemplate.isGroup()) {
                for (Description d: metadata.getDescriptors()) {
                    DescriptionTemplate t = d.getKey();
                    if (t == null) {
                        errorMessage.addError(new ObjectError ("template for " + d.getName(), "NotFound"));
                        continue;     // cannot check the validity if the temmplate is not available
                    }
                    if (t.getId() != null) {
                        if (t.getId().equals(descTemplate.getId())) {
                            exists = true;
                            count ++;
                            if (((Descriptor)d).getValue() != null && !((Descriptor)d).getValue().isEmpty()) {
                                valueExists = true;
                            }
                            if (((Descriptor)d).getNotRecorded() || ((Descriptor)d).getNotApplicable()) { // treat it as exists
                                valueExists = true;
                            }
                        }
                    } else if (t.getUri() != null) {
                        if (t.getUri().equals(descTemplate.getUri())) {
                            exists = true;
                            count++;
                            if (((Descriptor)d).getValue() != null && !((Descriptor)d).getValue().isEmpty()) {
                                valueExists = true;
                            }
                            if (((Descriptor)d).getNotRecorded() || ((Descriptor)d).getNotApplicable()) { // treat it as exists
                                valueExists = true;
                            }
                        }
                    }
                }
            } else {
                for (Description d: metadata.getDescriptorGroups()) {
                    DescriptionTemplate t = d.getKey();
                    if (t == null) {
                        errorMessage.addError(new ObjectError ("template for " + d.getName(), "NotFound"));
                        continue;     // cannot check the validity if the temmplate is not available
                    }
                    if (t.getId() != null) {
                        if (t.getId().equals(descTemplate.getId())) {
                            exists = true;
                            count ++;
                            if (((DescriptorGroup)d).getNotRecorded() || ((DescriptorGroup)d).getNotApplicable()) {
                                continue;
                            }
                            ErrorMessage error = validateDescriptorGroup((DescriptorGroup)d, descTemplate);
                            if (error != null) {
                                for (ObjectError err: error.getErrors())
                                    errorMessage.addError(err);
                            }  
                            
                        }
                    } else if (t.getUri() != null) {
                        if (t.getUri().equals(descTemplate.getUri())) {
                            exists = true;
                            count++;
                            if (((DescriptorGroup)d).getNotRecorded() || ((DescriptorGroup)d).getNotApplicable()) {
                                continue;
                            }
                            ErrorMessage error = validateDescriptorGroup((DescriptorGroup)d, descTemplate);
                            if (error != null) {
                                for (ObjectError err: error.getErrors())
                                    errorMessage.addError(err);
                            } 
                        }
                    }
                    
                }
            }
            if (descTemplate.isMandatory() && !exists) {
                if (descTemplate.getMandateGroup() == null) {   // if part of the mandate group, this descriptor group may not exist
                    // violation
                    errorMessage.addError(new ObjectError (descTemplate.getName() + "-mandatory", "NotFound"));
                }
            }
            if (descTemplate.getMaxOccurrence() == 1 && count > 1) {
             // violation
                errorMessage.addError(new ObjectError (descTemplate.getName() + "-maxOccurrence", "NumberExceeded"));
            }
            if (!descTemplate.isGroup() && descTemplate.isMandatory() && !valueExists) {
                // violation
                errorMessage.addError(new ObjectError (descTemplate.getName() + "-value", "NotFound"));
            }
        }
        
        if (errorMessage.getErrors() == null || errorMessage.getErrors().isEmpty())
            return null;
        return errorMessage;
        
    }
    
    @Operation(summary = "Delete given printed slide from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deleteprintedslide/{slideId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Slide deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete slides"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deletePrintedSlide (
            @Parameter(required=true, description="id of the printed slide to delete") 
            @PathVariable("slideId") String id, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable slides") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
                }
            }
            datasetRepository.deletePrintedSlide(id, owner);
            return new Confirmation("Printed slide deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete printed slide " + id, e);
        } catch (IllegalArgumentException e) {
            // in use, we cannot delete
            ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
            errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("printedSlide", "InUse"));
            throw new IllegalArgumentException(e.getMessage(), errorMessage);
        }
    }
    
    @Operation(summary = "Delete the given array dataset from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deletedataset/{datasetId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Dataset deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete datasets"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deleteArrayDataset (
            @Parameter(required=true, description="id of the array dataset to delete") 
            @PathVariable("datasetId") String id, Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            
            // delete the files associated with the array dataset
            ArrayDataset dataset = getArrayDataset(id.trim(), false, principal);
            // check the status of dataset, if PROCESSING cannot delete
            if (dataset != null && dataset.getStatus() == FutureTaskStatus.PROCESSING) {
                // check the timestamp and see if enough time has passed
                Long timeDelay = 3600L;
                SettingEntity entity = settingsRepository.findByName("timeDelay");
                if (entity != null) {
                    timeDelay = Long.parseLong(entity.getValue());
                }
                Date current = new Date();
                Date startDate = dataset.getStartDate();
                if (startDate != null) {
                    long diffInMillies = Math.abs(current.getTime() - startDate.getTime());
                    if (timeDelay > diffInMillies / 1000) {
                        // not enough time has passed, cannot delete!
                        ErrorMessage errorMessage = new ErrorMessage();
                        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                        errorMessage.addError(new ObjectError("dataset", "NotDone"));
                        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                        throw new IllegalArgumentException("Cannot delete the dataset when it is still processing", errorMessage);
                    }
                }
            }
            
            //check if the dataset is public
            try {
                String publicID = datasetRepository.getDatasetPublicId(id.trim());
                if (publicID != null) {
                    // this is major change, do not allow
                    ErrorMessage errorMessage = new ErrorMessage();
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    errorMessage.addError(new ObjectError("dataset", "Public"));
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                    throw new IllegalArgumentException("Cannot delete the dataset when it is public", errorMessage);
                }
            } catch (SparqlException e) {
                throw new GlycanRepositoryException("Dataset " + id + " cannot be retrieved for user " + principal.getName(), e);
            }
            
            if (dataset != null && dataset.getSlides() != null) {
                for (Slide slide: dataset.getSlides()) {
                    deleteSlide(slide.getId(), id, principal);
                }
                datasetRepository.deleteArrayDataset(id, user);
                if (dataset.getFiles() != null) {
                    for (FileWrapper f: dataset.getFiles()) {
                        File dataFile = new File (f.getFileFolder(), f.getIdentifier());
                        if (dataFile.exists()) {
                            dataFile.delete();
                        }
                    }
                }
            }
            
            if (dataset == null) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("id", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Cannot find array dataset with the given id", errorMessage);
            }
            return new Confirmation("array dataset deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete array dataset " + id, e);
        } 
    }
    
    @Operation(summary = "Delete the given raw data from the given array dataset", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deleterawdata/{rawdataId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="RawData deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete rawdata"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deleteRawData (
            @Parameter(required=true, description="id of the rawdata to delete") 
            @PathVariable("rawdataId") String id, 
            @Parameter(required=true, description="id of the array dataset this rawdata belongs to") 
            @RequestParam(name="datasetId", required=true)
            String datasetId,
            Principal principal) {
        try {
            
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + principal.getName(), e);
                }
            }
            
            //check if the dataset is public
            try {
                String publicID = datasetRepository.getDatasetPublicId(datasetId);
                if (publicID != null) {
                    // this is major change, do not allow
                    ErrorMessage errorMessage = new ErrorMessage();
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    errorMessage.addError(new ObjectError("dataset", "Public"));
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                    throw new IllegalArgumentException("Cannot delete the slide when it is public", errorMessage);
                }
            } catch (SparqlException e) {
                throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + principal.getName(), e);
            }
            
            // check the timestamp and see if enough time has passed
            Long timeDelay = 3600L;
            SettingEntity entity = settingsRepository.findByName("timeDelay");
            if (entity != null) {
                timeDelay = Long.parseLong(entity.getValue());
            }
           
            Date current = new Date();
            
            RawData rawData = datasetRepository.getRawDataFromURI(GlygenArrayRepositoryImpl.uriPrefix + id, false, owner);
            // check the status first
            if (rawData != null) {
                if (rawData.getStatus() == FutureTaskStatus.PROCESSING) {
                    Date startDate = rawData.getStartDate();
                    if (startDate != null) {
                        long diffInMillies = Math.abs(current.getTime() - startDate.getTime());
                        if (timeDelay > diffInMillies / 1000) {
                            // not enough time has passed, cannot delete!
                            ErrorMessage errorMessage = new ErrorMessage();
                            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                            errorMessage.addError(new ObjectError("rawData", "NotDone"));
                            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                            throw new IllegalArgumentException("Cannot delete the raw data when it is still processing", errorMessage);
                        }
                    }
                }
                if (rawData.getProcessedDataList() != null) {
                    for (ProcessedData processedData: rawData.getProcessedDataList()) {
                        if (processedData.getStatus() == FutureTaskStatus.PROCESSING) {
                            Date startDate = processedData.getStartDate();
                            if (startDate != null) {
                                long diffInMillies = Math.abs(current.getTime() - startDate.getTime());
                                if (timeDelay > diffInMillies / 1000) {
                                    // not enough time has passed, cannot delete!
                                    ErrorMessage errorMessage = new ErrorMessage();
                                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                                    errorMessage.addError(new ObjectError("processedData", "NotDone"));
                                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                                    throw new IllegalArgumentException("Cannot delete the processed data when it is still processing", errorMessage);
                                }
                            }
                        }
                    }
                }
                datasetRepository.deleteRawData(id, datasetId, owner);
                //delete the files associated with the rawdata
                if (rawData.getFile() != null) {
                    File rawDataFile = new File (rawData.getFile().getFileFolder(), rawData.getFile().getIdentifier());
                    if (rawDataFile.exists()) {
                        rawDataFile.delete();
                    }
                    if (rawData.getProcessedDataList() != null) {
                        for (ProcessedData processedData: rawData.getProcessedDataList()) {
                            if (processedData.getFile() != null) {
                                File dataFile = new File (processedData.getFile().getFileFolder(), processedData.getFile().getIdentifier());
                                if (dataFile.exists()) {
                                    dataFile.delete();
                                }
                            }
                            //deleteProcessedData(processedData.getId(), datasetId, principal);
                        }
                    }
                }
                return new Confirmation("Rawdata deleted successfully", HttpStatus.OK.value());
            } else {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("id", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Cannot find rawData with the given id", errorMessage);
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete rawdata " + id, e);
        } 
    }
    
    @Operation(summary = "Delete the given processed data from the given array dataset", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deleteprocesseddata/{processeddataId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="ProcessedData deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete ProcessedData"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deleteProcessedData (
            @Parameter(required=true, description="id of the ProcessedData to delete") 
            @PathVariable("processeddataId") String id, 
            @Parameter(required=true, description="id of the array dataset this ProcessedData belongs to") 
            @RequestParam(name="datasetId", required=true)
            String datasetId,
            Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + principal.getName(), e);
                }
                
                //check if the dataset is public
                try {
                    String publicID = datasetRepository.getDatasetPublicId(datasetId);
                    if (publicID != null) {
                        // this is major change, do not allow
                        ErrorMessage errorMessage = new ErrorMessage();
                        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                        errorMessage.addError(new ObjectError("dataset", "Public"));
                        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                        throw new IllegalArgumentException("Cannot delete the slide when it is public", errorMessage);
                    }
                } catch (SparqlException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + principal.getName(), e);
                }
            }
            
            // check the timestamp and see if enough time has passed
            Long timeDelay = 3600L;
            SettingEntity entity = settingsRepository.findByName("timeDelay");
            if (entity != null) {
                timeDelay = Long.parseLong(entity.getValue());
            }
           
            Date current = new Date();
            
            ProcessedData processedData = datasetRepository.getProcessedDataFromURI(GlygenArrayRepositoryImpl.uriPrefix + id, false, owner);
            // check the status first
            if (processedData != null) {
                if (processedData.getStatus() == FutureTaskStatus.PROCESSING) {
                    Date startDate = processedData.getStartDate();
                    if (startDate != null) {
                        long diffInMillies = Math.abs(current.getTime() - startDate.getTime());
                        if (timeDelay > diffInMillies / 1000) {
                            // not enough time has passed, cannot delete!
                            ErrorMessage errorMessage = new ErrorMessage();
                            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                            errorMessage.addError(new ObjectError("processedData", "NotDone"));
                            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                            throw new IllegalArgumentException("Cannot delete the processed data when it is still processing", errorMessage);
                        }
                    }
                }
            } else {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("id", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Cannot find ProcessedData with the given id", errorMessage);
            }
            
            datasetRepository.deleteProcessedData(id, datasetId, user);
            if (processedData.getFile() != null) {
                File dataFile = new File (processedData.getFile().getFileFolder(), processedData.getFile().getIdentifier());
                if (dataFile.exists()) {
                    dataFile.delete();
                }
            }
            
            return new Confirmation("ProcessedData deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete ProcessedData " + id, e);
        } 
    }
    
    
    @Operation(summary = "Delete the given slide from the given array dataset", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deleteslide/{slideId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Slide deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete slide"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deleteSlide (
            @Parameter(required=true, description="id of the slide to delete") 
            @PathVariable("slideId") String id, 
            @Parameter(required=true, description="id of the array dataset this slide belongs to") 
            @RequestParam(name="datasetId", required=true)
            String datasetId,
            Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            UserEntity owner = user;
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + principal.getName(), e);
                }
                
                //check if the dataset is public
                try {
                    String publicID = datasetRepository.getDatasetPublicId(datasetId);
                    if (publicID != null) {
                        // this is major change, do not allow
                        errorMessage.addError(new ObjectError("dataset", "Public"));
                        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                        throw new IllegalArgumentException("Cannot delete the slide when it is public", errorMessage);
                    }
                } catch (SparqlException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + principal.getName(), e);
                }
            }
            
            // check the timestamp and see if enough time has passed
            Long timeDelay = 3600L;
            SettingEntity entity = settingsRepository.findByName("timeDelay");
            if (entity != null) {
                timeDelay = Long.parseLong(entity.getValue());
            }
           
            Date current = new Date();
            
            Slide slide = datasetRepository.getSlideFromURI(GlygenArrayRepositoryImpl.uriPrefix + id, false, owner);
            //delete the files associated with the slide (image, raw data and processed data files)
            if (slide != null) {
                for (Image image: slide.getImages()) {
                    if (image.getRawDataList() != null) {
                        for (RawData rawData: image.getRawDataList()) {
                            if (rawData != null) {
                                if (rawData.getStatus() == FutureTaskStatus.PROCESSING) {
                                    Date startDate = rawData.getStartDate();
                                    if (startDate != null) {
                                        long diffInMillies = Math.abs(current.getTime() - startDate.getTime());
                                        if (timeDelay > diffInMillies / 1000) {
                                            // not enough time has passed, cannot restart!
                                            errorMessage.addError(new ObjectError("rawData", "NotDone"));
                                            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                                            throw new IllegalArgumentException("Cannot delete the raw data when it is still processing", errorMessage);
                                        }
                                    }
                                }
                                if (rawData.getProcessedDataList() != null) {
                                    for (ProcessedData processedData: rawData.getProcessedDataList()) {
                                        if (processedData.getStatus() == FutureTaskStatus.PROCESSING) {
                                            Date startDate = processedData.getStartDate();
                                            if (startDate != null) {
                                                long diffInMillies = Math.abs(current.getTime() - startDate.getTime());
                                                if (timeDelay > diffInMillies / 1000) {
                                                    // not enough time has passed, cannot restart!
                                                    errorMessage.addError(new ObjectError("processedData", "NotDone"));
                                                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                                                    throw new IllegalArgumentException("Cannot delete the processed data when it is still processing", errorMessage);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                datasetRepository.deleteSlide(id, datasetId, owner);   
                for (Image image: slide.getImages()) {
                    if (image.getFile() != null) {
                        File file = new File (image.getFile().getFileFolder(), image.getFile().getIdentifier());
                        if (file.exists()) {
                            file.delete();
                        }
                    }
                    if (image.getRawDataList() != null) {
                        for (RawData rawData: image.getRawDataList()) {
                            if (rawData != null && rawData.getFile() != null) {
                                File rawDataFile = new File (rawData.getFile().getFileFolder(), rawData.getFile().getIdentifier());
                                if (rawDataFile.exists()) {
                                    rawDataFile.delete();
                                }
                                if (rawData.getProcessedDataList() != null) {
                                    for (ProcessedData processedData: rawData.getProcessedDataList()) {
                                        if (processedData.getFile() != null) {
                                            File dataFile = new File (processedData.getFile().getFileFolder(), processedData.getFile().getIdentifier());
                                            if (dataFile.exists()) {
                                                dataFile.delete();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return new Confirmation("Slide deleted successfully", HttpStatus.OK.value());
            } else {
                errorMessage.addError(new ObjectError("slideId", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Cannot find slide with the given id", errorMessage);
            }
            
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete slide " + id, e);
        } 
    }
    
    @Operation(summary = "Delete the given image from the given array dataset", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deleteimage/{imageId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Image deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete image"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deleteImage (
            @Parameter(required=true, description="id of the image to delete") 
            @PathVariable("imageId") String id, 
            @Parameter(required=true, description="id of the array dataset this image belongs to") 
            @RequestParam(name="datasetId", required=true)
            String datasetId,
            Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            UserEntity owner = user;
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + principal.getName(), e);
                }
                
                //check if the dataset is public
                try {
                    String publicID = datasetRepository.getDatasetPublicId(datasetId);
                    if (publicID != null) {
                        // this is major change, do not allow
                        errorMessage.addError(new ObjectError("dataset", "Public"));
                        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                        throw new IllegalArgumentException("Cannot delete the slide when it is public", errorMessage);
                    }
                } catch (SparqlException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + principal.getName(), e);
                }
            }
            
            // check the timestamp and see if enough time has passed
            Long timeDelay = 3600L;
            SettingEntity entity = settingsRepository.findByName("timeDelay");
            if (entity != null) {
                timeDelay = Long.parseLong(entity.getValue());
            }
           
            Date current = new Date();
            
            Image image = datasetRepository.getImageFromURI(GlygenArrayRepositoryImpl.uriPrefix + id, false, owner);
            if (image != null) {
                if (image.getRawDataList() != null) {
                    for (RawData rawData: image.getRawDataList()) {
                        if (rawData != null) {
                            if (rawData.getStatus() == FutureTaskStatus.PROCESSING) {
                                Date startDate = rawData.getStartDate();
                                if (startDate != null) {
                                    long diffInMillies = Math.abs(current.getTime() - startDate.getTime());
                                    if (timeDelay > diffInMillies / 1000) {
                                        // not enough time has passed, cannot restart!
                                        errorMessage.addError(new ObjectError("rawData", "NotDone"));
                                        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                                        throw new IllegalArgumentException("Cannot delete the raw data when it is still processing", errorMessage);
                                    }
                                }
                            }
                            if (rawData.getProcessedDataList() != null) {
                                for (ProcessedData processedData: rawData.getProcessedDataList()) {
                                    if (processedData.getStatus() == FutureTaskStatus.PROCESSING) {
                                        Date startDate = processedData.getStartDate();
                                        if (startDate != null) {
                                            long diffInMillies = Math.abs(current.getTime() - startDate.getTime());
                                            if (timeDelay > diffInMillies / 1000) {
                                                // not enough time has passed, cannot restart!
                                                errorMessage.addError(new ObjectError("processedData", "NotDone"));
                                                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                                                throw new IllegalArgumentException("Cannot delete the processed data when it is still processing", errorMessage);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                datasetRepository.deleteImage(id, datasetId, owner);   
                
                if (image.getFile() != null) {
                    File file = new File (image.getFile().getFileFolder(), image.getFile().getIdentifier());
                    if (file.exists()) {
                        file.delete();
                    }
                }
                if (image.getRawDataList() != null) {
                    for (RawData rawData: image.getRawDataList()) {
                        if (rawData != null && rawData.getFile() != null) {
                            File rawDataFile = new File (rawData.getFile().getFileFolder(), rawData.getFile().getIdentifier());
                            if (rawDataFile.exists()) {
                                rawDataFile.delete();
                            }
                            if (rawData.getProcessedDataList() != null) {
                                for (ProcessedData processedData: rawData.getProcessedDataList()) {
                                    if (processedData.getFile() != null) {
                                        File dataFile = new File (processedData.getFile().getFileFolder(), processedData.getFile().getIdentifier());
                                        if (dataFile.exists()) {
                                            dataFile.delete();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return new Confirmation("Image deleted successfully", HttpStatus.OK.value());
            } else {
                errorMessage.addError(new ObjectError("imageId", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Cannot find image with the given id", errorMessage);
            }
            
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete slide " + id, e);
        } 
    }
    
    @Operation(summary = "Delete the given publication from the given array dataset", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deletepublication/{publicationid}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Publication deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete publication"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deletePublication (
            @Parameter(required=true, description="id of the publication to delete") 
            @PathVariable("publicationid") String id, 
            @Parameter(required=true, description="id of the array dataset this publication belongs to") 
            @RequestParam(name="datasetId", required=true)
            String datasetId,
            Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            
            UserEntity owner = user;
            // check if the dataset with the given id exists
            ArrayDataset dataset;
            try {
                datasetId = datasetId.trim();
                dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                if (dataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                            owner = originalUser;
                        }
                    }
                }
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Cannot retrieve dataset from the repository", e);
            }
            
            if (dataset != null && dataset.getPublications() != null) {
                boolean found = false;
                boolean isPublic = false;
                for (Publication pub: dataset.getPublications()) {
                    if (pub.getUri().equals(GlygenArrayRepositoryImpl.uriPrefix+id)) {
                        found = true;
                    }
                    if (pub.getUri().equals(GlygenArrayRepositoryImpl.uriPrefixPublic+id)) {
                        found = true;
                        isPublic = true;
                    }
                }
                if (!found) {
                    errorMessage.addError(new ObjectError("publicationId", "NotFound"));
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                    throw new IllegalArgumentException("Given array dataset does not have a publication with the given id", errorMessage);
                }
                else {
                    //TODO create a change log and save it
                    String publicId = datasetRepository.getDatasetPublicId(datasetId);
                    datasetRepository.deletePublication(id, isPublic ? publicId : datasetId, isPublic? null: owner);
                    return new Confirmation("Publication deleted successfully", HttpStatus.OK.value());
                }
            }
            else {
                errorMessage.addError(new ObjectError("datasetId", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Cannot find array dataset with the given id", errorMessage);
            }
            
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete publication " + id, e);
        } 
    }
    
    @Operation(summary = "Delete the given grant from the given array dataset", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deletegrant/{grantid}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="grant deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete grant"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deleteGrant (
            @Parameter(required=true, description="id of the grant to delete") 
            @PathVariable("grantid") String id, 
            @Parameter(required=true, description="id of the array dataset this grant belongs to") 
            @RequestParam(name="datasetId", required=true)
            String datasetId,
            Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            
            UserEntity owner = user;
            // check if the dataset with the given id exists
            ArrayDataset dataset;
            try {
                datasetId = datasetId.trim();
                dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                if (dataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                            owner = originalUser;
                        }
                    }
                }
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Cannot retrieve dataset from the repository", e);
            }
            
            if (dataset != null && dataset.getGrants() != null) {
                boolean found = false;
                boolean isPublic = false;
                for (Grant g: dataset.getGrants()) {
                    if (g.getUri().equals(GlygenArrayRepositoryImpl.uriPrefix+id)) {
                        found = true;
                    }
                    if (g.getUri().equals(GlygenArrayRepositoryImpl.uriPrefixPublic+id)) {
                        found = true;
                        isPublic = true;
                    }
                }
                if (!found) {
                    errorMessage.addError(new ObjectError("grantid", "NotFound"));
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                    throw new IllegalArgumentException("Given array dataset does not have a grant with the given id", errorMessage);
                }
                else {
                    //TODO create a change log and save it
                    String publicId = datasetRepository.getDatasetPublicId(datasetId);
                    datasetRepository.deleteGrant(id, isPublic ? publicId : datasetId, isPublic? null: owner);
                    return new Confirmation("Grant deleted successfully", HttpStatus.OK.value());
                }
            }
            else {
                errorMessage.addError(new ObjectError("datasetId", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Cannot find array dataset with the given id", errorMessage);
            }
            
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete grant " + id, e);
        } 
    }
    
    @Operation(summary = "Delete the given keyword from the given array dataset", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deletekeyword/{keyword}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="keyword deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete keywords"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deleteKeyword (
            @Parameter(required=true, description="id of the file to delete") 
            @PathVariable("keyword") String keyword, 
            @Parameter(required=true, description="id of the array dataset this keyword belongs to") 
            @RequestParam(name="datasetId", required=true)
            String datasetId,
            Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            
            UserEntity owner = user;
            // check if the dataset with the given id exists
            ArrayDataset dataset;
            try {
                datasetId = datasetId.trim();
                dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                if (dataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                            owner = originalUser;
                        }
                    }
                }
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Cannot retrieve dataset from the repository", e);
            }
            
            if (dataset != null && dataset.getKeywords() != null) {
                List<String> newKeywords = new ArrayList<String>();
                boolean found = false;
                for (String k: dataset.getKeywords()) {
                    if (k.equals(keyword)) {
                        found = true;
                    } else {
                        newKeywords.add(k);
                    }
                }
                if (!found) {
                    errorMessage.addError(new ObjectError("keyword", "NotFound"));
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                    throw new IllegalArgumentException("Given array dataset does not have the given keyword", errorMessage);
                }
                else {
                    dataset.setKeywords(newKeywords);
                    datasetRepository.updateArrayDataset(dataset, owner);
                    return new Confirmation("keyword deleted successfully", HttpStatus.OK.value());
                }
            }
            else {
                errorMessage.addError(new ObjectError("datasetId", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Cannot find array dataset with the given id", errorMessage);
            }
            
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete keyword " + keyword, e);
        } 
    }
    
    @Operation(summary = "Delete the given file from the given array dataset", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deletefile/{fileidentifier}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="file deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete file"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deleteFile (
            @Parameter(required=true, description="id of the file to delete") 
            @PathVariable("fileidentifier") String id, 
            @Parameter(required=true, description="id of the array dataset this file belongs to") 
            @RequestParam(name="datasetId", required=true)
            String datasetId,
            Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            
            UserEntity owner = user;
            // check if the dataset with the given id exists
            ArrayDataset dataset;
            try {
                datasetId = datasetId.trim();
                dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                if (dataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                            owner = originalUser;
                        }
                    }
                }
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Cannot retrieve dataset from the repository", e);
            }
            
            if (dataset != null && dataset.getFiles() != null) {
                List<FileWrapper> newFiles = new ArrayList<FileWrapper>();
                boolean found = false;
                FileWrapper toDelete = null;
                for (FileWrapper f: dataset.getFiles()) {
                    if (f.getIdentifier().equals(id)) {
                        found = true;
                        toDelete = f;
                    } else {
                        newFiles.add(f);
                    }
                }
                if (!found) {
                    errorMessage.addError(new ObjectError("fileidentifer", "NotFound"));
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                    throw new IllegalArgumentException("Given array dataset does not have a file with the given id", errorMessage);
                }
                else {
                    dataset.setFiles(newFiles);
                    datasetRepository.updateArrayDataset(dataset, owner);
                    // delete the file from the experiment folder
                    if (toDelete != null) {
                        File file = new File (toDelete.getFileFolder(), toDelete.getIdentifier());
                        if (file.exists()) {
                            file.delete();
                        }
                    }
                    return new Confirmation("File deleted successfully", HttpStatus.OK.value());
                }
            }
            else {
                errorMessage.addError(new ObjectError("datasetId", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Cannot find array dataset with the given id", errorMessage);
            }
            
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete file " + id, e);
        } 
    }
    
    @Operation(summary = "Delete the given collaborator from the given array dataset", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deletecollaborator/{username}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Collaborator deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete collaborator"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deleteCollaborator (
            @Parameter(required=true, description="username of the collaborator to delete") 
            @PathVariable("username") String username, 
            @Parameter(required=true, description="id of the array dataset this collaborator belongs to") 
            @RequestParam(name="datasetId", required=true)
            String datasetId,
            Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            
            UserEntity owner = user;
            // check if the dataset with the given id exists
            ArrayDataset dataset;
            try {
                datasetId = datasetId.trim();
                dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                if (dataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                            owner = originalUser;
                        }
                    }
                }
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Cannot retrieve dataset from the repository", e);
            }
            if (dataset != null && dataset.getGrants() != null) {
                boolean found = false;
                for (Creator c: dataset.getCollaborators()) {
                    if (c.getName().equalsIgnoreCase(username)) {
                        found = true;
                    }
                }
                if (!found) {
                    errorMessage.addError(new ObjectError("username", "NotFound"));
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                    throw new IllegalArgumentException("Given array dataset does not have a collaborator with the given username", errorMessage);
                }
                else {
                    String publicId = datasetRepository.getDatasetPublicId(datasetId);
                    //TODO create a change log and save it
                    if (publicId != null) {
                        datasetRepository.deleteCollaborator(username, publicId, null);
                    } else {
                        datasetRepository.deleteCollaborator(username, datasetId, owner);
                    }
                    return new Confirmation("Collaborator deleted successfully", HttpStatus.OK.value());
                }
            }
            else {
                errorMessage.addError(new ObjectError("datasetId", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Cannot find array dataset with the given id", errorMessage);
            }
            
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete collaborator " + username, e);
        } 
    }
    
    @Operation(summary = "Delete given sample from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deletesample/{sampleId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Sample deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete samples"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deleteSample (
            @Parameter(required=true, description="id of the sample to delete") 
            @PathVariable("sampleId") String id, 
            @Parameter(required=false, description="id of the array dataset (to check for permissions)") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + principal.getName(), e);
                }
            }
            metadataRepository.deleteMetadata(id, owner);
            return new Confirmation("Sample deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete sample " + id, e);
        } catch (IllegalArgumentException e) {
            // in use, we cannot delete
            ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
            errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("metadata", "InUse"));
            throw new IllegalArgumentException(e.getMessage(), errorMessage);
        }
    }
    
    @Operation(summary = "Delete given image analysis software from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deleteimagemetadata/{imageAnaysisMetadataId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Image analysis software deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete image analysis software"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deleteImageAnalysisSoftware (
            @Parameter(required=true, description="id of the image analysis software to delete") 
            @PathVariable("imageAnaysisMetadataId") String id, 
            @Parameter(required=false, description="id of the array dataset (to check for permissions)") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + principal.getName(), e);
                }
            }
            metadataRepository.deleteMetadata(id, owner);
            return new Confirmation("Image analysis software deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete image analysis software " + id, e);
        } catch (IllegalArgumentException e) {
            // in use, we cannot delete
            ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
            errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("metadata", "InUse"));
            throw new IllegalArgumentException(e.getMessage(), errorMessage);
        }
    }
    
    @Operation(summary = "Delete given slide metadata from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deleteslidemetadata/{slideMetadataId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Slide metadata deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete slide metadata"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deleteSlideMetadata (
            @Parameter(required=true, description="id of the slide metadata to delete") 
            @PathVariable("slideMetadataId") String id, 
            @Parameter(required=false, description="id of the array dataset (to check for permissions)") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + principal.getName(), e);
                }
            }
            metadataRepository.deleteMetadata(id, owner);
            return new Confirmation("Slide metadata deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete slide metadata " + id, e);
        } catch (IllegalArgumentException e) {
            // in use, we cannot delete
            ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
            errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("metadata", "InUse"));
            throw new IllegalArgumentException(e.getMessage(), errorMessage);
        }
    }
    
    @Operation(summary = "Delete given data processing software from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deletedataprocessingmetadata/{dataProcessingMetadataId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Data processing software deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete data processing software"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deleteDataProcessingSoftware (
            @Parameter(required=true, description="id of the data processing software to delete") 
            @PathVariable("dataProcessingMetadataId") String id, 
            @Parameter(required=false, description="id of the array dataset (to check for permissions)") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + principal.getName(), e);
                }
            }
            metadataRepository.deleteMetadata(id, owner);
            return new Confirmation("Data processing software deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete data processing software " + id, e);
        } catch (IllegalArgumentException e) {
            // in use, we cannot delete
            ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
            errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("metadata", "InUse"));
            throw new IllegalArgumentException(e.getMessage(), errorMessage);
        }
    }
    
    @Operation(summary = "Delete given scanner from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deletescannermetadata/{scannerId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Scanner deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete scanner"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deleteScanner (
            @Parameter(required=true, description="id of the scanner to delete") 
            @PathVariable("scannerId") String id, 
            @Parameter(required=false, description="id of the array dataset (to check for permissions)") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + principal.getName(), e);
                }
            }
            metadataRepository.deleteMetadata(id, owner);
            return new Confirmation("Scanner deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete scanner " + id, e);
        } catch (IllegalArgumentException e) {
            // in use, we cannot delete
            ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
            errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("metadata", "InUse"));
            throw new IllegalArgumentException(e.getMessage(), errorMessage);
        }
    }
    
    @Operation(summary = "Delete given printer from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deleteprintermetadata/{printerId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Printer deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete printer"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deletePrinter (
            @Parameter(required=true, description="id of the printer metadata to delete") 
            @PathVariable("printerId") String id, 
            @Parameter(required=false, description="id of the array dataset (to check for permissions)") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + principal.getName(), e);
                }
            }
            metadataRepository.deleteMetadata(id, owner);
            return new Confirmation("Printer deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete printer " + id, e);
        } catch (IllegalArgumentException e) {
            // in use, we cannot delete
            ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
            errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("metadata", "InUse"));
            throw new IllegalArgumentException(e.getMessage(), errorMessage);
        }
    }
    
    @Operation(summary = "Delete given printrun from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deleteprintrunmetadata/{printrunId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Printer deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete printer"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deletePrintRun (
            @Parameter(required=true, description="id of the printrun metadata to delete") 
            @PathVariable("printrunId") String id, 
            @Parameter(required=false, description="id of the array dataset (to check for permissions)") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + principal.getName(), e);
                }
            }
            metadataRepository.deleteMetadata(id, owner);
            return new Confirmation("Print run deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete printer " + id, e);
        } catch (IllegalArgumentException e) {
            // in use, we cannot delete
            ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
            errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("metadata", "InUse"));
            throw new IllegalArgumentException(e.getMessage(), errorMessage);
        }
    }
    
    @Operation(summary = "Delete given assay metadata from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deleteassaymetadata/{assayId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Assay metadata deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete assay metadata"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deleteAssayMetadata (
            @Parameter(required=true, description="id of the assay metadata to delete") 
            @PathVariable("assayId") String id, 
            @Parameter(required=false, description="id of the array dataset (to check for permissions)") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + principal.getName(), e);
                }
            }
            metadataRepository.deleteMetadata(id, owner);
            return new Confirmation("Assay deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete assay metadata " + id, e);
        } catch (IllegalArgumentException e) {
            // in use, we cannot delete
            ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
            errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("metadata", "InUse"));
            throw new IllegalArgumentException(e.getMessage(), errorMessage);
        }
    }
    
    @Operation(summary = "Delete given spot metadata from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deletespotmetadata/{spotMetadataId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Assay metadata deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete spot metadata"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation deleteSpotMetadata (
            @Parameter(required=true, description="id of the spot metadata to delete") 
            @PathVariable("spotMetadataId") String id, 
            @Parameter(required=false, description="id of the array dataset (to check for permissions)") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + principal.getName(), e);
                }
            }
            metadataRepository.deleteMetadata(id, owner);
            return new Confirmation("Assay deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete spot metadata " + id, e);
        } catch (IllegalArgumentException e) {
            // in use, we cannot delete
            ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
            errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("metadata", "InUse"));
            throw new IllegalArgumentException(e.getMessage(), errorMessage);
        }
    }
    
    @Operation(summary = "Export metadata into a file", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/exportmetadata", method=RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="confirmation message"), 
            @ApiResponse(responseCode="400", description="Invalid request, file not found, not writable etc."),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to export metadata"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public @ResponseBody String exportMetadata (
            @Parameter(required=false, description="id of metadata to export") 
            @RequestParam(value="metadataId", required=false) String metadataId,
            @Parameter(required=true, name="template", description="type of the metadata, if not provided it will retrieve all metadata") 
            @RequestParam(required=true, value="template") MetadataTemplateType templateType,
            Principal p) {
                
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        
        boolean exportAll = (metadataId == null);
        try {
            String typePredicate = null;
            switch (templateType) {
            case SAMPLE: 
                typePredicate = GlygenArrayRepositoryImpl.sampleTypePredicate;
                break;
            case ASSAY:
                typePredicate = GlygenArrayRepositoryImpl.assayTypePredicate;
                break;
            case DATAPROCESSINGSOFTWARE:
                typePredicate = GlygenArrayRepositoryImpl.dataProcessingTypePredicate;
                break;
            case FEATURE:
                typePredicate = GlygenArrayRepositoryImpl.featureMetadataTypePredicate;
                break;
            case IMAGEANALYSISSOFTWARE:
                typePredicate = GlygenArrayRepositoryImpl.imageAnalysisTypePredicate;
                break;
            case PRINTER:
                typePredicate = GlygenArrayRepositoryImpl.printerTypePredicate;
                break;
            case PRINTRUN:
                typePredicate = GlygenArrayRepositoryImpl.printRunTypePredicate;
                break;
            case SCANNER:
                typePredicate = GlygenArrayRepositoryImpl.scannerTypePredicate;
                break;
            case SLIDE:
                typePredicate = GlygenArrayRepositoryImpl.slideTemplateTypePredicate;
                break;
            case SPOT:
                typePredicate = GlygenArrayRepositoryImpl.spotMetadataTypePredicate;
                break;
            }
            List<MetadataCategory> metadataToExport = new ArrayList<MetadataCategory>();
            if (exportAll) {
                metadataToExport = metadataRepository.getMetadataCategoryByUser(user, 0, -1, null, 0, null, typePredicate, true);   
            } else {
                MetadataCategory myMetadata = metadataRepository.getMetadataCategoryFromURI(GlygenArrayRepositoryImpl.uriPrefix + metadataId, typePredicate, true, user);
                if (myMetadata == null) {
                    // try public graph
                    metadataRepository.getMetadataCategoryFromURI(GlygenArrayRepositoryImpl.uriPrefixPublic + metadataId, typePredicate, true, user);
                }
                if (myMetadata == null) {
                    throw new EntityNotFoundException("Given metadata " + metadataId + " cannot be found for the user " + p.getName());
                }
                metadataToExport.add(myMetadata);
            }
            AllMetadataView view = new AllMetadataView();
            try {
                SettingEntity entity = settingsRepository.findByName("apiVersion");
                if (entity != null) {
                    view.setVersion(entity.getValue()); 
                }
            } catch (Exception e) {
                view.setVersion("1.0.0");
            }
            view.setMetadataList(metadataToExport);
            ObjectMapper mapper = new ObjectMapper();         
            String json = mapper.writeValueAsString(view);
            return json;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Metadata cannot be retrieved for user " + p.getName(), e);
        } catch (JsonProcessingException e) {
            ErrorMessage errorMessage = new ErrorMessage("Cannot generate the metadata list");
            errorMessage.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            errorMessage.addError(new ObjectError("reason", e.getMessage()));
            errorMessage.setErrorCode(ErrorCodes.INTERNAL_ERROR);
            throw new IllegalArgumentException("Cannot generate the metadata json", errorMessage);
        }
    }
    
    @Operation(summary = "Retrieve metadata listed in a repository export (json) file", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/getmetadatafromfile", method=RequestMethod.GET, 
            consumes = {"application/json", "application/xml"}, produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Metadata retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request if file is not a valid file"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to add metadata"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public List<MetadataCategory> getMetadataFromFile (
            @Parameter(required=true, name="file", description="details of the uploded file") 
            @RequestParam(required=true, value="file")
            String filename, Principal p, 
            @Parameter(required=true, name="template", description="type of the metadata, if not provided it will retrieve all metadata") 
            @RequestParam(required=true, value="template") MetadataTemplateType templateType,
            @Parameter(required=false, name="filetype", description="type of the file, the default is Repository Export (.json)", schema = @Schema(type = "string", allowableValues= {"Repository Export (.json)" })) 
            @RequestParam(required=false, value="filetype") String fileType) {
        
            List<MetadataCategory> metadataFromFile = new ArrayList<MetadataCategory>();
            if (fileType != null && !fileType.contains ("Repository Export")) {
                // not supported at this time
                ErrorMessage errorMessage = new ErrorMessage("File type is not supported");
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("file", "NotValid"));
                throw new IllegalArgumentException("File is not acceptable", errorMessage);
            }
            
            filename = moveToTempFile (filename.trim());
            File file = new File(uploadDir, filename);
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
                    ErrorMessage errorMessage = new ErrorMessage();
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    try {
                        ByteArrayInputStream stream = new   ByteArrayInputStream(fileContent);
                        String fileAsString = IOUtils.toString(stream, StandardCharsets.UTF_8);
                        ObjectMapper objectMapper = new ObjectMapper();
                        AllMetadataView view = objectMapper.readValue(fileAsString, AllMetadataView.class);
                       
                        if (view.getMetadataList() == null || view.getMetadataList().isEmpty()) {
                            errorMessage.addError(new ObjectError ("file", null, null, "EMPTY"));
                        } else {
                            for (int i=0; i < view.getMetadataList().size(); i++) {
                                MetadataCategory metadata = view.getMetadataList().get(i);
                                if (checkTemplateType(metadata, templateType)) {
                                    metadataFromFile.add(metadata);
                                }
                            }
                        }
                        
                        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                            throw new IllegalArgumentException("Errors in the upload process", errorMessage);
                        }
                        return metadataFromFile;
                    } catch (IOException e) {
                        errorMessage.addError(new ObjectError("file", "NotValid"));
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
    
    private boolean checkTemplateType(MetadataCategory metadata, MetadataTemplateType templateType) {
        switch (templateType) {
        case ASSAY:
            if (metadata instanceof AssayMetadata)
                return true;
            break;
        case DATAPROCESSINGSOFTWARE:
            if (metadata instanceof DataProcessingSoftware)
                return true;
            break;
        case FEATURE:
            if (metadata instanceof FeatureMetadata)
                return true;
            break;
        case IMAGEANALYSISSOFTWARE:
            if (metadata instanceof ImageAnalysisSoftware)
                return true;
            break;
        case PRINTER:
            if (metadata instanceof Printer)
                return true;
            break;
        case PRINTRUN:
            if (metadata instanceof PrintRun)
                return true;
            break;
        case SAMPLE:
            if (metadata instanceof Sample)
                return true;
            break;
        case SCANNER:
            if (metadata instanceof ScannerMetadata)
                return true;
            break;
        case SLIDE:
            if (metadata instanceof SlideMetadata)
                return true;
            break;
        case SPOT:
            if (metadata instanceof SpotMetadata)
                return true;
            break;
        }
        return false;
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
    
    @Operation(summary = "Import listed metadata to the repository", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/importmetadata", method=RequestMethod.POST, 
            consumes = {"application/json", "application/xml"}, produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Metadata processed successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request if file is not a valid file"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to add metadata"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ImportMetadataResultView importMetadata (
            @Parameter(required=true, description="list of metadata to be uploaded") 
            @RequestBody
            MetadataImportInput input, 
            Principal p) {
        
        ImportMetadataResultView result = new ImportMetadataResultView();
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        FileWrapper fileWrapper = input.getFile();
        if (fileWrapper != null && fileWrapper.getIdentifier() != null) {
            String uploadedFileName = fileWrapper.getIdentifier();
            String finalFileName = moveToTempFile (uploadedFileName.trim());
            File jsonFile = new File(uploadDir, finalFileName);
            if (jsonFile.exists()) {
                AllMetadataView view = null;
                byte[] fileContent;
                try {
                    fileContent = Files.readAllBytes(jsonFile.toPath());
                    ByteArrayInputStream stream = new   ByteArrayInputStream(fileContent);
                    String fileAsString = IOUtils.toString(stream, StandardCharsets.UTF_8);
                    ObjectMapper objectMapper = new ObjectMapper();
                    view = objectMapper.readValue(fileAsString, AllMetadataView.class);
                } catch (IOException e) {
                    // cannot read the file
                    ErrorMessage errorMessage = new ErrorMessage("File cannot be found");
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    errorMessage.addError(new ObjectError("file", "NotFound"));
                    throw new IllegalArgumentException("File cannot be found", errorMessage);
                }
                if (input.getSelectedMetadata() == null || input.getSelectedMetadata().isEmpty()) {
                    ErrorMessage errorMessage = new ErrorMessage("No metadata selected");
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    errorMessage.addError(new ObjectError("selectedMetadata", "NoEmpty"));
                    errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
                    throw new IllegalArgumentException("No metadata selected", errorMessage);
                }
                
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                int countSuccess = 0;
                for (int i=0; i < input.getSelectedMetadata().size(); i++) {
                    MetadataCategory metadata = input.getSelectedMetadata().get(i);
                    String searchName = null;
                    if (metadata.getName() != null) {
                        searchName = metadata.getName();
                    }
                    boolean duplicate = false;
                    String typePredicate = null;
                    try {
                        if (metadata instanceof Sample) {
                            typePredicate = GlygenArrayRepositoryImpl.sampleTypePredicate;
                        } else if (metadata instanceof AssayMetadata) {
                            typePredicate = GlygenArrayRepositoryImpl.assayTypePredicate;
                        } else if (metadata instanceof DataProcessingSoftware) {
                            typePredicate = GlygenArrayRepositoryImpl.dataProcessingTypePredicate;
                        } else if (metadata instanceof FeatureMetadata) {
                            typePredicate = GlygenArrayRepositoryImpl.featureMetadataTypePredicate;
                        } else if (metadata instanceof ImageAnalysisSoftware) {
                            typePredicate = GlygenArrayRepositoryImpl.imageAnalysisTypePredicate;
                        } else if (metadata instanceof Printer) {
                            typePredicate = GlygenArrayRepositoryImpl.printerTypePredicate;
                        } else if (metadata instanceof PrintRun) {
                            typePredicate = GlygenArrayRepositoryImpl.printRunTypePredicate;
                        } else if (metadata instanceof ScannerMetadata) {
                            typePredicate = GlygenArrayRepositoryImpl.scannerTypePredicate;
                        } else if (metadata instanceof SlideMetadata) {
                            typePredicate = GlygenArrayRepositoryImpl.slideTemplateTypePredicate;
                        } else if (metadata instanceof SpotMetadata) {
                            typePredicate = GlygenArrayRepositoryImpl.spotMetadataTypePredicate;
                        } else {
                            // error
                            errorMessage.addError(new ObjectError("templateType", "NotValid"));
                            throw new IllegalArgumentException("template type is not valid", errorMessage);
                        }
                        if (typePredicate != null && searchName != null) {
                            MetadataCategory existing = metadataRepository.getMetadataByLabel(searchName, typePredicate, user);
                            if (existing != null) {
                                //MetadataError error = new MetadataError();
                                //error.setMetadata(metadata);
                                //String[] codes = new String[] {metadata.getName()};
                                //errorMessage.addError(new ObjectError("name", codes, null, "Duplicate"));
                                //errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                                //error.setError(errorMessage);
                                result.getDuplicates().add(existing);
                                duplicate = true;
                            }
                        }
                    } catch (Exception e) {
                        errorMessage.addError(new ObjectError("slideLayout", e.getMessage()));
                        errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
                        throw new IllegalArgumentException("metadata search failed", errorMessage);
                    }
                    
                    try {
                        if (duplicate)
                            continue;
                        
                        // get full metadata from file
                        for (MetadataCategory m: view.getMetadataList()) {
                            if (m.getId().equalsIgnoreCase(metadata.getId())) {
                                metadata = m;
                                if (searchName != null) {
                                    metadata.setName(searchName);
                                }
                            }
                        }
                        
                        String id=null;
                        if (metadata instanceof Sample) {
                            id = addSample((Sample)metadata, p);
                        } else if (metadata instanceof AssayMetadata) {
                            id = addAssayMetadata((AssayMetadata)metadata, p);
                        } else if (metadata instanceof Printer) {
                            id = addPrinter((Printer)metadata, p);
                        } else if (metadata instanceof PrintRun) {
                            id = addPrintrun((PrintRun)metadata, p);
                        } else if (metadata instanceof ImageAnalysisSoftware) {
                            id = addImageAnalysisSoftware((ImageAnalysisSoftware)metadata, p);
                        } else if (metadata instanceof DataProcessingSoftware) {
                            id = addDataProcessingSoftware((DataProcessingSoftware)metadata, p);
                        } else if (metadata instanceof ScannerMetadata) {
                            id = addScanner((ScannerMetadata)metadata, p);
                        } else if (metadata instanceof SpotMetadata) {
                            id = addSpotMetadata((SpotMetadata)metadata, p);
                        } else if (metadata instanceof SlideMetadata) {
                            id = addSlideMetadata((SlideMetadata)metadata, p);
                        }
                      
                        if (id != null) {
                            countSuccess ++;
                            result.getAddedMetadata().add(metadata);
                        } else {
                            // error
                            String[] codes = new String[] {i+""};
                            errorMessage.addError(new ObjectError("metadata", codes, null, metadata.getName() + " not added"));
                            MetadataError error = new MetadataError();
                            error.setMetadata(metadata);
                            error.setError(errorMessage);
                            result.getErrors().add(error);
                        }
                    } catch (Exception e) {
                        logger.error ("Exception adding the metadata: " + metadata.getName(), e);
                        if (e.getCause() instanceof ErrorMessage) {
                            if (((ErrorMessage)e.getCause()).toString().contains("Duplicate")) {
                                try {
                                    MetadataCategory existing = metadataRepository.getMetadataByLabel(metadata.getName(), typePredicate, user);
                                    result.getDuplicates().add(existing);
                                } catch (Exception e1) {
                                    logger.error("could not check for existence of metadata by name!", e1);
                                }
                            } else {
                                String[] codes = new String[] {i+""};
                                errorMessage.addError(new ObjectError("metadata", codes, null, ((ErrorMessage)e.getCause()).toString()));
                                MetadataError error = new MetadataError();
                                error.setMetadata(metadata);
                                error.setError(errorMessage);
                                result.getErrors().add(error);
                            }
                        } else { 
                            String[] codes = new String[] {i+""};
                            errorMessage.addError(new ObjectError("metadata", codes, null, e.getMessage()));
                            MetadataError error = new MetadataError();
                            error.setMetadata(metadata);
                            error.setError(errorMessage);
                            result.getErrors().add(error);
                        }
                    }
                }
                
                result.setSuccessMessage(countSuccess + " out of " + input.getSelectedMetadata().size() + " metadata are added");
                return result;
            } else {
                ErrorMessage errorMessage = new ErrorMessage("File cannot be found");
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("file", "NotFound"));
                throw new IllegalArgumentException("File cannot be found", errorMessage);
            }
        } else {
            ErrorMessage errorMessage = new ErrorMessage("File cannot be found");
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("file", "NotFound"));
            throw new IllegalArgumentException("File cannot be found", errorMessage);
        }
        
    }
    
    @Operation(summary = "Retrieve slide with the given id", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/getprintedslide/{slideId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Printed Slide retrieved successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve"),
            @ApiResponse(responseCode="404", description="Printed slide with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public PrintedSlide getPrintedSlide (
            @Parameter(required=true, description="id of the printed slide to retrieve") 
            @PathVariable("slideId") String id, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable slides") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
                }
            }
            
            
            PrintedSlide slide = datasetRepository.getPrintedSlideFromURI(GlygenArrayRepository.uriPrefix + id, owner);
            if (slide == null) {
                throw new EntityNotFoundException("Printed slide with id : " + id + " does not exist in the repository");
            }
            // check if it is in use
            boolean notInUse = datasetRepository.canDeletePrintedSlide (slide.getUri(), owner);
            slide.setInUse(!notInUse);
            
            // clear the inner objects
            if (slide.getLayout() != null)
                slide.getLayout().setBlocks(null);
            if (slide.getPrinter() != null) {
                slide.getPrinter().setDescriptorGroups(null);
                slide.getPrinter().setDescriptors(null);
            }
            if (slide.getMetadata() != null) {
                slide.getMetadata().setDescriptorGroups(null);
                slide.getMetadata().setDescriptors(null);
            }
            
            return slide;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Printed slide cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @Operation(summary = "Retrieve dataset with the given id", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/getarraydataset/{datasetid}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Dataset retrieved successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve the dataset"),
            @ApiResponse(responseCode="404", description="Dataset with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ArrayDataset getArrayDataset (
            @Parameter(required=true, description="id of the array dataset to retrieve") 
            @PathVariable("datasetid") String id, 
            @Parameter(required=false, description="load rawdata and processed data measurements or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            ArrayDataset dataset = datasetRepository.getArrayDataset(id.trim(), loadAll, user);
            if (dataset == null) {
                // check if the user can access this dataset as a co-owner
                String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + id);
                if (coOwnedGraph != null) {
                    UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                    if (originalUser != null) {
                        dataset = datasetRepository.getArrayDataset(id.trim(), loadAll, originalUser);
                    }
                }
            }
            if (dataset == null) {
                throw new EntityNotFoundException("Array dataset with id : " + id + " does not exist in the repository");
            }
            
            dataset.setUploadStatus(getDatasetStatus(dataset));
            return dataset;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Array dataset with id " + id + " cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @Operation(summary = "Retrieve processed data with the given id", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/getprocesseddata/{id}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Processed data retrieved successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve the dataset"),
            @ApiResponse(responseCode="404", description="Processed data with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ProcessedData getProcessedData (
            @Parameter(required=true, description="id of the processed data to retrieve") 
            @PathVariable("id") String id, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the processed data") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        try {
            if (id == null || id.isEmpty()) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("id", "NoEmpty"));
                throw new IllegalArgumentException("id must be provided", errorMessage);
            }
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
                }
            }
            ProcessedData data = datasetRepository.getProcessedDataFromURI(GlygenArrayRepository.uriPrefix + id, false, owner);
            if (data == null) {
                throw new EntityNotFoundException("Processed data with id : " + id + " does not exist in the repository");
            }
            
            return data;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Processed data with id " + id + " cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @Operation(summary = "Retrieve sample with the given id", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/getsample/{sampleId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Sample retrieved successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve"),
            @ApiResponse(responseCode="404", description="Sample with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Sample getSample (
            @Parameter(required=true, description="id of the sample to retrieve") 
            @PathVariable("sampleId") String id, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
                }
            }
            Sample sample = metadataRepository.getSampleFromURI(GlygenArrayRepository.uriPrefix + id, owner);
            if (sample == null) {
                // check the public ones
                sample = metadataRepository.getSampleFromURI(GlygenArrayRepository.uriPrefixPublic + id, null);
                if (sample == null)
                    throw new EntityNotFoundException("Sample with id : " + id + " does not exist in the repository");
            }
            return sample;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Sample cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @Operation(summary = "Retrieve printer with the given id", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/getPrinter/{printerId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Printer retrieved successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve"),
            @ApiResponse(responseCode="404", description="Printer with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Printer getPrinter (
            @Parameter(required=true, description="id of the printer to retrieve") 
            @PathVariable("printerId") String id, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
                }
            }
            Printer metadata = metadataRepository.getPrinterFromURI(GlygenArrayRepository.uriPrefix + id, owner);
            if (metadata == null) {
                // check the public ones
                metadata = metadataRepository.getPrinterFromURI(GlygenArrayRepository.uriPrefixPublic + id, null);
                if (metadata == null) {
                    throw new EntityNotFoundException("Printer with id : " + id + " does not exist in the repository");
                }
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Printer cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    
    @Operation(summary = "Retrieve print run with the given id", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/getPrintRun/{printRunId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Printrun retrieved successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve"),
            @ApiResponse(responseCode="404", description="Printrun with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public PrintRun getPrintRun (
            @Parameter(required=true, description="id of the print run to retrieve") 
            @PathVariable("printRunId") String id, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
                }
            }
            PrintRun metadata = metadataRepository.getPrintRunFromURI(GlygenArrayRepository.uriPrefix + id, owner);
            if (metadata == null) {
                // check the public ones
                metadata = metadataRepository.getPrintRunFromURI(GlygenArrayRepository.uriPrefixPublic + id, null);
                if (metadata == null) {
                    throw new EntityNotFoundException("Printrun with id : " + id + " does not exist in the repository");
                }
                
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Printer cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @Operation(summary = "Retrieve scanner with the given id", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/getScanner/{scannerId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Scanner retrieved successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve"),
            @ApiResponse(responseCode="404", description="ScannerMetadata with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ScannerMetadata getScanner (
            @Parameter(required=true, description="id of the ScannerMetadata to retrieve") 
            @PathVariable("scannerId") String id, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
                }
            }
            ScannerMetadata metadata = metadataRepository.getScannerMetadataFromURI(GlygenArrayRepository.uriPrefix + id, owner);
            if (metadata == null) {
                // check the public ones
                metadata = metadataRepository.getScannerMetadataFromURI(GlygenArrayRepository.uriPrefixPublic + id, null);
                if (metadata == null) {
                    throw new EntityNotFoundException("ScannerMetadata with id : " + id + " does not exist in the repository");
                }
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("ScannerMetadata cannot be retrieved for user " + p.getName(), e);
        }   
    }
    @Operation(summary = "Retrieve SlideMetadata with the given id", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/getSlideMetadata/{slideId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="SlideMetadata retrieved successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve"),
            @ApiResponse(responseCode="404", description="Printer with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public SlideMetadata getSlideMetadata (
            @Parameter(required=true, description="id of the SlideMetadata to retrieve") 
            @PathVariable("slideId") String id, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
                }
            }
            SlideMetadata metadata = metadataRepository.getSlideMetadataFromURI(GlygenArrayRepository.uriPrefix + id, owner);
            if (metadata == null) {
                // check the public ones
                metadata = metadataRepository.getSlideMetadataFromURI(GlygenArrayRepository.uriPrefixPublic + id, null);
                if (metadata == null) {
                    throw new EntityNotFoundException("Slide metadata with id : " + id + " does not exist in the repository");
                }
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("SlideMetadata cannot be retrieved for user " + p.getName(), e);
        }   
    }
    @Operation(summary = "Retrieve ImageAnalysisSoftware with the given id", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/getImageAnalysisSoftware/{imagesoftwareId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="ImageAnalysisSoftware retrieved successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve"),
            @ApiResponse(responseCode="404", description="ImageAnalysisSoftware with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ImageAnalysisSoftware getImageAnaylsisSoftware (
            @Parameter(required=true, description="id of the ImageAnalysisSoftware to retrieve") 
            @PathVariable("imagesoftwareId") String id, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
                }
            }
            ImageAnalysisSoftware metadata = metadataRepository.getImageAnalysisSoftwareFromURI(GlygenArrayRepository.uriPrefix + id, owner);
            if (metadata == null) {
                // check the public ones
                metadata = metadataRepository.getImageAnalysisSoftwareFromURI(GlygenArrayRepository.uriPrefixPublic + id, null);
                if (metadata == null) {
                    throw new EntityNotFoundException("ImageAnalysisSoftware with id : " + id + " does not exist in the repository");
                }
                
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("ImageAnalysisSoftware cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @Operation(summary = "Retrieve DataProcessingSoftware with the given id", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/getDataProcessingSoftware/{dataprocessingId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="DataProcessingSoftware retrieved successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve"),
            @ApiResponse(responseCode="404", description="DataProcessingSoftware with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public DataProcessingSoftware getDataProcessingSoftware (
            @Parameter(required=true, description="id of the DataProcessingSoftware to retrieve") 
            @PathVariable("dataprocessingId") String id, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
                }
            }
            DataProcessingSoftware metadata = metadataRepository.getDataProcessingSoftwareFromURI(GlygenArrayRepository.uriPrefix + id, owner);
            if (metadata == null) {
                // check the public ones
                metadata = metadataRepository.getDataProcessingSoftwareFromURI(GlygenArrayRepository.uriPrefixPublic + id, null);
                if (metadata == null) {
                    throw new EntityNotFoundException("DataProcessingSoftware with id : " + id + " does not exist in the repository");
                }
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("DataProcessingSoftware cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @Operation(summary = "Retrieve assay metadata with the given id", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/getAssayMetadata/{assayId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Assay metadata retrieved successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve"),
            @ApiResponse(responseCode="404", description="Assay metadata with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public AssayMetadata getAssayMetadata (
            @Parameter(required=true, description="id of the Assay metadata to retrieve") 
            @PathVariable("assayId") String id, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
                }
            }
            AssayMetadata metadata = metadataRepository.getAssayMetadataFromURI(GlygenArrayRepository.uriPrefix + id, owner);
            if (metadata == null) {
                // check the public ones
                metadata = metadataRepository.getAssayMetadataFromURI(GlygenArrayRepository.uriPrefixPublic + id, null);
                if (metadata == null) {
                    throw new EntityNotFoundException("Assay metadata with id : " + id + " does not exist in the repository");
                }
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Assay metadata cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @Operation(summary = "Retrieve spot metadata with the given id", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/getSpotMetadata/{spotMetadataId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Assay metadata retrieved successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve"),
            @ApiResponse(responseCode="404", description="Spot metadata with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public SpotMetadata getSpotMetadata (
            @Parameter(required=true, description="id of the Spot metadata to retrieve") 
            @PathVariable("spotMetadataId") String id, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            UserEntity owner = user;
            
            if (datasetId != null) {
                // check if the dataset with the given id exists for this user, or if the user is the co-owner
                try {
                    datasetId = datasetId.trim();
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                    if (dataset == null) {
                        // check if the user can access this dataset as a co-owner
                        String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                        if (coOwnedGraph != null) {
                            UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                            if (originalUser != null) {
                                dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                                owner = originalUser;
                            }
                        }
                    }
                    
                } catch (SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
                }
            }
            SpotMetadata metadata = metadataRepository.getSpotMetadataFromURI(GlygenArrayRepository.uriPrefix + id, owner);
            if (metadata == null) {
                // check the public ones
                metadata = metadataRepository.getSpotMetadataFromURI(GlygenArrayRepository.uriPrefixPublic + id, null);
                if (metadata == null) {
                    throw new EntityNotFoundException("Spot metadata with id : " + id + " does not exist in the repository");
                }
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Spot metadata cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @Operation(summary = "Update given printed slide for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/updatePrintedSlide", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Printed slide updated successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to update slides"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation updatePrintedSlide(
            @Parameter(required=true, description="Printed slide with updated fields, only name and description can be changed") 
            @RequestBody PrintedSlide printedSlide, 
            @Parameter(required=false, description="id of the array dataset that uses this printed slide") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) throws SQLException {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        // validate first
        if (validator != null) {
            if  (printedSlide.getName() != null) {
                Set<ConstraintViolation<PrintedSlide>> violations = validator.validateValue(PrintedSlide.class, "name", printedSlide.getName());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("name", "LengthExceeded"));
                }       
            }
            
            if  (printedSlide.getDescription() != null) {
                Set<ConstraintViolation<PrintedSlide>> violations = validator.validateValue(PrintedSlide.class, "description", printedSlide.getDescription());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("description", "LengthExceeded"));
                }       
            }
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
       
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserEntity owner = user;
        
        if (datasetId != null) {
            // check if the dataset with the given id exists for this user, or if the user is the co-owner
            try {
                datasetId = datasetId.trim();
                ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                if (dataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                            owner = originalUser;
                        }
                    }
                }
                
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
            }
        }
        
        if (printedSlide.getName() == null || printedSlide.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        
        
        // do not allow layout and metadata changes!!!
        // check to make sure, the slide layout is specified
      /*  if (printedSlide.getLayout() == null || (printedSlide.getLayout().getId() == null && printedSlide.getLayout().getUri() == null && printedSlide.getLayout().getName() == null)) {
            errorMessage.addError(new ObjectError("slidelayout", "NoEmpty"));
        } */

        // check for duplicate name
        try {
            PrintedSlide existing = datasetRepository.getPrintedSlideByLabel(printedSlide.getName().trim(), false, owner);
            if (existing != null && !existing.getUri().equals(printedSlide.getUri()) && !existing.getId().equals(printedSlide.getId())) {
                errorMessage.addError(new ObjectError("name", "Duplicate"));
            }
        } catch (SparqlException | SQLException e2) {
            throw new GlycanRepositoryException("Error checking for duplicate printedSlide", e2);
        }
        
        // check if the slide layout exists
      /*  if (printedSlide.getLayout() != null) {
            try {
                String slideLayoutId = printedSlide.getLayout().getId();
                if (slideLayoutId == null) {
                    if (printedSlide.getLayout().getUri() != null) {
                        slideLayoutId = printedSlide.getLayout().getUri().substring(printedSlide.getLayout().getUri().lastIndexOf("/") + 1);
                    }
                }
                if (slideLayoutId != null) {
                    SlideLayout existing = layoutRepository.getSlideLayoutById(slideLayoutId, owner, false);
                    if (existing == null) {
                        errorMessage.addError(new ObjectError("slidelayout", "NotFound"));
                    } else {
                        printedSlide.setLayout(existing);
                    }
                } else if (printedSlide.getLayout().getName() != null) {
                    SlideLayout existing = layoutRepository.getSlideLayoutByName(printedSlide.getLayout().getName(), owner);
                    if (existing == null) {
                        errorMessage.addError(new ObjectError("slidelayout", "NotFound"));
                    } else {
                        printedSlide.setLayout(existing);
                    }
                }
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the slide layout", e);
            }
        }
        
        if (printedSlide.getMetadata() != null) {
            try {
                if (printedSlide.getMetadata().getName() != null) {
                    SlideMetadata slideMetadata = metadataRepository.getSlideMetadataByLabel(printedSlide.getMetadata().getName(), owner);
                    if (slideMetadata == null) {
                        errorMessage.addError(new ObjectError("slideMetadata", "NotFound"));
                    } else {
                        printedSlide.setMetadata(slideMetadata);
                    }
                } else if (printedSlide.getMetadata().getUri() != null) {
                    SlideMetadata slideMetadata = metadataRepository.getSlideMetadataFromURI(printedSlide.getMetadata().getUri(), owner);
                    if (slideMetadata == null) {
                        errorMessage.addError(new ObjectError("slideMetadata", "NotFound"));
                    } else {
                        printedSlide.setMetadata(slideMetadata);
                    }
                } else if (printedSlide.getMetadata().getId() != null) {
                    SlideMetadata slideMetadata = metadataRepository.getSlideMetadataFromURI(ArrayDatasetRepositoryImpl.uriPrefix + printedSlide.getMetadata().getId(), owner);
                    if (slideMetadata == null) {
                        errorMessage.addError(new ObjectError("slideMetadata", "NotFound"));
                    } else {
                        printedSlide.setMetadata(slideMetadata);
                    }
                }
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the slide metadata", e);
            }
        }
        
        if (printedSlide.getPrinter() != null) {
            try {
                if (printedSlide.getPrinter().getName() != null) {
                    Printer printer = metadataRepository.getPrinterByLabel(printedSlide.getPrinter().getName(), owner);
                    if (printer == null) {
                        errorMessage.addError(new ObjectError("printer", "NotFound"));
                    } else {
                        printedSlide.setPrinter(printer);
                    }
                } else if (printedSlide.getPrinter().getUri() != null) {
                    Printer printer = metadataRepository.getPrinterFromURI(printedSlide.getPrinter().getUri(), owner);
                    if (printer == null) {
                        errorMessage.addError(new ObjectError("printer", "NotFound"));
                    } else {
                        printedSlide.setPrinter(printer);
                    }
                } else if (printedSlide.getPrinter().getId() != null) {
                    Printer printer = metadataRepository.getPrinterFromURI(ArrayDatasetRepositoryImpl.uriPrefix + printedSlide.getPrinter().getId(), owner);
                    if (printer == null) {
                        errorMessage.addError(new ObjectError("printer", "NotFound"));
                    } else {
                        printedSlide.setPrinter(printer);
                    }
                }
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the printer", e);
            }
        }
        
        if (printedSlide.getPrintRun() != null) {
            try {
                if (printedSlide.getPrintRun().getName() != null) {
                    PrintRun printer = metadataRepository.getPrintRunByLabel(printedSlide.getPrintRun().getName(), owner);
                    if (printer == null) {
                        errorMessage.addError(new ObjectError("printRun", "NotFound"));
                    } else {
                        printedSlide.setPrintRun(printer);
                    }
                } else if (printedSlide.getPrintRun().getUri() != null) {
                    PrintRun printer = metadataRepository.getPrintRunFromURI(printedSlide.getPrintRun().getUri(), owner);
                    if (printer == null) {
                        errorMessage.addError(new ObjectError("printRun", "NotFound"));
                    } else {
                        printedSlide.setPrintRun(printer);
                    }
                } else if (printedSlide.getPrintRun().getId() != null) {
                    PrintRun printer = metadataRepository.getPrintRunFromURI(ArrayDatasetRepositoryImpl.uriPrefix + printedSlide.getPrintRun().getId(), owner);
                    if (printer == null) {
                        errorMessage.addError(new ObjectError("printRun", "NotFound"));
                    } else {
                        printedSlide.setPrintRun(printer);
                    }
                }
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the printer", e);
            }
        }*/
        
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid printed slide information", errorMessage);
        
        try {
            datasetRepository.updatePrintedSlide(printedSlide, owner);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Printed slide cannot be updated for user " + p.getName(), e);
        }       
        return new Confirmation("Printed slide updated successfully", HttpStatus.OK.value());
    }
    
    @Operation(summary = "Update given array dataset for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/updatearraydataset", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Array dataset updated successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to update datasets"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation updateDataset(
            @Parameter(required=true, description="Array dataset with updated fields. You can change name, description, keywords, publications, grants, coowners, collaborators and files"
                    + ". Whatever is included in these fields would be reflected in the repository. Therefore, if you don't want the existing keywords to be deleted, for example,"
                    + " you need to have the existing ones listed as part of the object") 
            @RequestBody ArrayDataset dataset, 
            @Parameter(required=false, description="summary of the changes") 
            @RequestParam(value="changeSummary", required=false)
            String changeSummary,
            @Parameter(required=false, description="field that has changed, can provide multiple") 
            @RequestParam(value="changedField", required=false)
            List<String> changedFields,
            Principal p) throws SQLException {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        // validate first
        if (validator != null) {
            if  (dataset.getName() != null) {
                Set<ConstraintViolation<ArrayDataset>> violations = validator.validateValue(ArrayDataset.class, "name", dataset.getName());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("name", "LengthExceeded"));
                }       
            }
            
            if  (dataset.getDescription() != null) {
                Set<ConstraintViolation<ArrayDataset>> violations = validator.validateValue(ArrayDataset.class, "description", dataset.getDescription());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("description", "LengthExceeded"));
                }       
            }
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserEntity owner = user;
        
        if (dataset.getId() != null) {
            // check if the dataset with the given id exists for this user, or if the user is the co-owner
            try {
                ArrayDataset existingdataset = datasetRepository.getArrayDataset(dataset.getId(), false, user);
                if (existingdataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + dataset.getId());
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            owner = originalUser;
                        }
                    }
                }
                
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Dataset " + dataset.getId() + " cannot be retrieved for user " + p.getName(), e);
            }
        }
        
        if (dataset.getName() == null || dataset.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        
        // check to make sure, the sample is specified
     /*   if (dataset.getSample() == null || 
                (dataset.getSample().getId() == null && dataset.getSample().getUri() == null && dataset.getSample().getName() == null)) {
            errorMessage.addError(new ObjectError("sample", "NoEmpty"));
        } */

        // check for duplicate name
        try {
            ArrayDataset existing = datasetRepository.getArrayDatasetByLabel(dataset.getName(), false, owner);
            if (existing != null && !existing.getUri().equals(dataset.getUri()) && !existing.getId().equals(dataset.getId())) {
                errorMessage.addError(new ObjectError("name", "Duplicate"));
            }
        } catch (SparqlException | SQLException e2) {
            throw new GlycanRepositoryException("Error checking for duplicate array dataset", e2);
        }
        
        // check if the sample exists
    /*    if (dataset.getSample() != null) {
            try {
                String id = dataset.getSample().getId();
                if (id == null) {
                    if (dataset.getSample().getUri() != null) {
                        id = dataset.getSample().getUri().substring(dataset.getSample().getUri().lastIndexOf("/") + 1);
                    }
                }
                if (id != null) {
                    Sample existing = metadataRepository.getSampleFromURI(GlygenArrayRepositoryImpl.uriPrefix + id, owner);
                    if (existing == null) {
                        // check the public data
                        existing = metadataRepository.getSampleFromURI(GlygenArrayRepositoryImpl.uriPrefixPublic + id, null);
                        if (existing == null) {
                            errorMessage.addError(new ObjectError("sample", "NotFound"));
                        }
                    } else {
                        dataset.setSample(existing);
                    }
                } else if (dataset.getSample().getName() != null) {
                    Sample existing = metadataRepository.getSampleByLabel(dataset.getSample().getName(), owner);
                    if (existing == null) {
                        errorMessage.addError(new ObjectError("sample", "NotFound"));
                    } else {
                        dataset.setSample(existing);
                    }
                }
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the sample", e);
            }
        }*/
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
            throw new IllegalArgumentException("Invalid Input: Not a valid printed slide information", errorMessage);
        }
        
        try {
            ChangeLog changeLog = new ChangeLog();
            changeLog.setUser(p.getName());
            changeLog.setChangeType(ChangeType.MINOR);
            changeLog.setDate(new Date());
            changeLog.setSummary(changeSummary);
            changeLog.setChangedFields(changedFields);
            dataset.addChange(changeLog);
            datasetRepository.updateArrayDataset(dataset, owner);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Array dataset cannot be updated for user " + p.getName(), e);
        }       
        return new Confirmation("Array dataset updated successfully", HttpStatus.OK.value());
    }
    
    @Operation(summary = "Update given array dataset for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/updatedatasetnamedescription", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Array dataset updated successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to update datasets"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation updateDatasetGeneralDetails(
            @Parameter(required=true, description="Array dataset with updated fields. You can change name and description. If there is no description provided, it would be removed in the repository as well.") 
            @RequestBody ArrayDataset dataset, 
            @Parameter(required=false, description="summary of the changes") 
            @RequestParam(value="changeSummary", required=false)
            String changeSummary,
            @Parameter(required=false, description="field that has changed, can provide multiple") 
            @RequestParam(value="changedField", required=false)
            List<String> changedFields,
            Principal p) throws SQLException {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        // validate first
        if (validator != null) {
            if  (dataset.getName() != null) {
                Set<ConstraintViolation<ArrayDataset>> violations = validator.validateValue(ArrayDataset.class, "name", dataset.getName());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("name", "LengthExceeded"));
                }       
            }
            
            if  (dataset.getDescription() != null) {
                Set<ConstraintViolation<ArrayDataset>> violations = validator.validateValue(ArrayDataset.class, "description", dataset.getDescription());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("description", "LengthExceeded"));
                }       
            }
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserEntity owner = user;
        
        if (dataset.getId() != null) {
            // check if the dataset with the given id exists for this user, or if the user is the co-owner
            try {
                ArrayDataset existingdataset = datasetRepository.getArrayDataset(dataset.getId(), false, user);
                if (existingdataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + dataset.getId());
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            owner = originalUser;
                        }
                    }
                }
                
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Dataset " + dataset.getId() + " cannot be retrieved for user " + p.getName(), e);
            }
        }
        
        if (dataset.getName() == null || dataset.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        
        ArrayDataset existing = null;
        // check for duplicate name
        try {
            existing = datasetRepository.getArrayDatasetByLabel(dataset.getName(), false, owner);
            if (existing != null && !existing.getUri().equals(dataset.getUri()) && !existing.getId().equals(dataset.getId())) {
                errorMessage.addError(new ObjectError("name", "Duplicate"));
            }
            if (existing == null) {
                errorMessage.addError(new ObjectError("dataset", "NotFound"));
            }
        } catch (SparqlException | SQLException e2) {
            throw new GlycanRepositoryException("Error checking for duplicate array dataset", e2);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
            throw new IllegalArgumentException("Invalid Input: Not a valid dataset information", errorMessage);
        }
        
        try {
            if (existing != null) {  // should not be ever null at this point
                ChangeLog changeLog = new ChangeLog();
                changeLog.setUser(p.getName());
                changeLog.setChangeType(ChangeType.MINOR);
                changeLog.setDate(new Date());
                changeLog.setSummary(changeSummary);
                changeLog.setChangedFields(changedFields);
                existing.addChange(changeLog);
                existing.setName(dataset.getName());
                existing.setDescription(dataset.getDescription());
                datasetRepository.updateArrayDataset(existing, owner);
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Array dataset cannot be updated for user " + p.getName(), e);
        }       
        return new Confirmation("Array dataset updated successfully", HttpStatus.OK.value());
    }
    
    @Operation(summary = "Update given sample for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/updateSample", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Sample updated successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to update samples"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation updateSample(
            @Parameter(required=true, description="Sample with updated fields") 
            @RequestBody Sample metadata, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) throws SQLException {
        return updateMetadata(metadata, datasetId, MetadataTemplateType.SAMPLE, p);
        
    }
    
    @Operation(summary = "Update given printer for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/updatePrinter", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Printer updated successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to update printers"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation updatePrinter(
            @Parameter(required=true, description="Printer with updated fields") 
            @RequestBody Printer metadata, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) throws SQLException {
        return updateMetadata(metadata, datasetId, MetadataTemplateType.PRINTER, p);
        
    }
    
    @Operation(summary = "Update given printrun for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/updatePrintrun", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Printrun updated successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to update printruns"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation updatePrintrun(
            @Parameter(required=true, description="Printer with updated fields") 
            @RequestBody Printer metadata, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) throws SQLException {
        return updateMetadata(metadata, datasetId, MetadataTemplateType.PRINTRUN, p);
        
    }
    
    @Operation(summary = "Update given scanner metadata for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/updateScanner", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Scanner metadata updated successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to update scanners"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation updateScanner(
            @Parameter(required=true, description="Scanner with updated fields") 
            @RequestBody ScannerMetadata metadata, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) throws SQLException {
        return updateMetadata(metadata, datasetId, MetadataTemplateType.SCANNER, p);
        
    }
    
    @Operation(summary = "Update given slide metadata for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/updateSlideMetadata", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Slide Metadata updated successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to update slide metadata"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation updateSlideMetadata(
            @Parameter(required=true, description="Slide metadata with updated fields") 
            @RequestBody SlideMetadata metadata, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) throws SQLException {
        return updateMetadata(metadata, datasetId, MetadataTemplateType.SLIDE, p);
        
    }
    
    @Operation(summary = "Update given image analysis software for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/updateImageAnalysisSoftware", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Image analysis software updated successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to update image analysis software"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation updateImageAnalysisSoftware(
            @Parameter(required=true, description="Image analysis software with updated fields") 
            @RequestBody ImageAnalysisSoftware metadata, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) throws SQLException {
        return updateMetadata(metadata, datasetId, MetadataTemplateType.IMAGEANALYSISSOFTWARE, p);
        
    }
    
    @Operation(summary = "Update given data processing software for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/updateDataProcessingSoftware", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="DataProcessingSoftware updated successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to update data processing software"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation updateDataProcessingSoftware(
            @Parameter(required=true, description="Data processing software with updated fields") 
            @RequestBody DataProcessingSoftware metadata, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) throws SQLException {
        return updateMetadata(metadata, datasetId, MetadataTemplateType.DATAPROCESSINGSOFTWARE, p);
        
    }
    
    @Operation(summary = "Update given assay metadata for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/updateAssayMetadata", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="assay metadata updated successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to update assay metadata"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation updateAssayMetadata(
            @Parameter(required=true, description="Assay metadata with updated fields") 
            @RequestBody AssayMetadata metadata, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) throws SQLException {
        return updateMetadata(metadata, datasetId, MetadataTemplateType.ASSAY, p);
        
    }
    
    @Operation(summary = "Update given spot metadata for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/updateSpotMetadata", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="assay metadata updated successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to update spot metadata"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Confirmation updateSpotMetadata(
            @Parameter(required=true, description="Spot metadata with updated fields") 
            @RequestBody SpotMetadata metadata, 
            @Parameter(required=false, description="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) throws SQLException {
        return updateMetadata(metadata, datasetId, MetadataTemplateType.SPOT, p);
        
    }
     
    private Confirmation updateMetadata (MetadataCategory metadata, String datasetId, MetadataTemplateType type, Principal p) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        // validate first
        if (validator != null) {
            if  (metadata.getName() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "name", metadata.getName());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("name", "LengthExceeded"));
                }       
            }
            
            if  (metadata.getDescription() != null) {
                Set<ConstraintViolation<MetadataCategory>> violations = validator.validateValue(MetadataCategory.class, "description", metadata.getDescription());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("description", "LengthExceeded"));
                }       
            }
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
       
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserEntity owner = user;
        
        if (datasetId != null) {
            // check if the dataset with the given id exists for this user, or if the user is the co-owner
            try {
                datasetId = datasetId.trim();
                ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
                if (dataset == null) {
                    // check if the user can access this dataset as a co-owner
                    String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                    if (coOwnedGraph != null) {
                        UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                        if (originalUser != null) {
                            dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                            owner = originalUser;
                        }
                    }
                }
                
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
            }
        }
        
        if (metadata.getName() == null || metadata.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        if (metadata.getTemplate() == null || metadata.getTemplate().isEmpty()) {
            errorMessage.addError(new ObjectError("type", "NoEmpty"));
        }
        
        // check if the template exists
        String templateURI = null;
        try {
            
            if (metadata.getTemplate() != null && !metadata.getTemplate().isEmpty())
                templateURI = templateRepository.getTemplateByName(metadata.getTemplate(), type);
            if (templateURI == null) {
                errorMessage.addError(new ObjectError("type", "NotValid"));
            }
            else {
                // validate mandatory/multiple etc.
                MetadataTemplate template = templateRepository.getTemplateFromURI(templateURI);
                ErrorMessage err = validateMetadata (metadata, template);
                if (err != null) {
                    for (ObjectError error: err.getErrors())
                        errorMessage.addError(error);
                }    
            }
        } catch (SparqlException | SQLException e1) {
            logger.error("Error retrieving template", e1);
            throw new GlycanRepositoryException("Error retrieving metadata template " + p.getName(), e1);
        }
        
        // check if the name is unique
        if (metadata.getName() != null && !metadata.getName().trim().isEmpty()) {
            try {
                
                MetadataCategory existing = null;
                switch (type) {
                case SAMPLE:
                    existing = metadataRepository.getSampleByLabel(metadata.getName(), owner);
                    break;
                case PRINTER:
                    existing = metadataRepository.getPrinterByLabel(metadata.getName(), owner);
                    break;
                case SCANNER:
                    existing = metadataRepository.getScannerMetadataByLabel(metadata.getName(), owner);
                    break;
                case SLIDE:
                    existing = metadataRepository.getSlideMetadataByLabel(metadata.getName(), owner);
                    break;
                case IMAGEANALYSISSOFTWARE:
                    existing = metadataRepository.getImageAnalysisSoftwarByLabel(metadata.getName(), owner);
                    break;
                case DATAPROCESSINGSOFTWARE:
                    existing = metadataRepository.getDataProcessingSoftwareByLabel(metadata.getName(), owner);
                    break;
                case ASSAY:
                    existing = metadataRepository.getAssayMetadataByLabel(metadata.getName(), owner);
                    break;
                case SPOT:
                    existing = metadataRepository.getSpotMetadataByLabel(metadata.getName(), owner);
                    break;
                case PRINTRUN:
                    existing = metadataRepository.getPrintRunByLabel(metadata.getName(), owner);
                    break;
                case FEATURE:
                    throw new IllegalArgumentException("Feature metadata update is not supported");
                }
                    
                if (existing != null) {
                    if (!existing.getId().equalsIgnoreCase(metadata.getId())) {
                        errorMessage.addError(new ObjectError("name", "Duplicate"));
                    }
                }
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Could not query existing metadata", e);
            }
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid metadata information", errorMessage);
        
        try {
            metadataRepository.updateMetadata(metadata, owner);
            try {
                // check mirage and update mirage info
                boolean isMirage = checkMirageCompliance(metadata.getId(), type, datasetId, p);
                metadata.setIsMirage(isMirage);
            } catch (IllegalArgumentException e) {
                metadata.setIsMirage(false);
                logger.error("Checking for mirage compliance", e);
            }
            metadataRepository.updateMetadataMirage(metadata, owner);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Error updating metadata with id: " + metadata.getId());
        }
        return new Confirmation("Metadata updated successfully", HttpStatus.OK.value());
        
    }
    
    @Operation(summary = "Make the given array dataset public", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/makearraydatasetpublic/{datasetid}", method = RequestMethod.POST)
    @ApiResponses (value = {
            @ApiResponse(responseCode="200", description="id of the public array dataset"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to modify the dataset"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public String makeArrayDatasetPublic (
            @Parameter(required=true, description="id of the dataset to make public") 
            @PathVariable("datasetid") String datasetId, Principal p) {
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserEntity owner = user;
        
        try {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, user);
            
            if (dataset == null) {
                // check if the user can access this dataset as a co-owner
                String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId.trim());
                if (coOwnedGraph != null) {
                    UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                    if (originalUser != null) {
                        dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, originalUser);
                        owner = originalUser;
                    }
                }
                if (dataset == null) {
                    errorMessage.addError(new ObjectError("datasetId", "NotValid"));
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                    throw new IllegalArgumentException("There is no dataset with the given id in user's repository", errorMessage); 
                }
            }
              
            // check if the dataset is already public
            if (dataset.getIsPublic()) {
                // already been made public
                errorMessage.addError(new ObjectError("datasetId", "This dataset is already public"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("This dataset is already public!", errorMessage); 
            }
            // check if the array dataset is ready to be made public
            if (dataset.getStatus() == FutureTaskStatus.PROCESSING) {
                // check if enough time has passed to restart processing
                // check the timestamp and see if enough time has passed
                Long timeDelay = 3600L;
                SettingEntity entity = settingsRepository.findByName("timeDelay");
                if (entity != null) {
                    timeDelay = Long.parseLong(entity.getValue());
                }
               
                Date current = new Date();
                Date startDate = dataset.getStartDate();
                if (startDate != null) {
                    long diffInMillies = Math.abs(current.getTime() - startDate.getTime());
                    if (timeDelay > diffInMillies / 1000) {
                        // not enough time has passed, cannot restart!
                        // it is already being made public, do not allow it again
                        errorMessage.addError(new ObjectError("status", "NotDone"));
                        errorMessage.addError(new ObjectError("time", "NotValid"));
                        throw new IllegalArgumentException("Not enough time has passed. Please wait before restarting", errorMessage);
                    }
                }
                
            } 
            if (dataset.getSlides() == null || dataset.getSlides().isEmpty()) {
                errorMessage.addError(new ObjectError("slide", "NotFound"));
            }
            
            FutureTaskStatus status = getDatasetStatus(dataset);
            if (status == FutureTaskStatus.PROCESSING) {
                errorMessage.addError(new ObjectError("dataset", "NotDone"));
            } else if (status == FutureTaskStatus.ERROR) {
                errorMessage.addError(new ObjectError("dataset", "HasError"));
            } else if (status == FutureTaskStatus.NOTSTARTED) {
                errorMessage.addError(new ObjectError("dataset", "Has no processed data"));
            }
            
            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                throw new IllegalArgumentException("Cannot make public now!", errorMessage); 
            }
            
            CompletableFuture<String> datasetURI = null;
            FutureTask task = new FutureTask();
            task.setStatus(FutureTaskStatus.PROCESSING);
            try {
                //ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, true, user);
                // set the status to processing first
                dataset.setStatus(FutureTaskStatus.PROCESSING);
                datasetRepository.updateStatus (dataset.getUri(), dataset, owner);
                datasetURI = datasetRepository.makePublicArrayDataset(dataset, owner); 
                final ArrayDataset data = dataset;
                final UserEntity o = owner;
                if (datasetURI.isCompletedExceptionally()) {
                    logger.error("make public completed with exception!!");
                }
                datasetURI.whenComplete((uri, e) -> {
                    try {
                        String existingURI = data.getUri();
                        if (e != null) {
                            task.setStatus(FutureTaskStatus.ERROR);
                            logger.error(e.getMessage(), e);
                            data.setStatus(FutureTaskStatus.ERROR);
                            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException && e.getCause().getCause() instanceof ErrorMessage) 
                                data.setError((ErrorMessage) e.getCause().getCause());
                            else {
                                errorMessage.addError(new ObjectError("exception", e.getMessage()));
                                data.setError(errorMessage);
                            }
                        } else {
                            task.setStatus(FutureTaskStatus.DONE);
                            data.setStatus(FutureTaskStatus.DONE);
                            data.setUri(uri);
                        }
                        datasetRepository.updateStatus (existingURI, data, o);
                        
                        if (task.getStatus() == FutureTaskStatus.DONE) {
                            // generate public glycan images if necessary
                            for (Slide s: data.getSlides()) {
                                if (s.getPrintedSlide() != null && s.getPrintedSlide().getLayout() != null) {
                                    // reload slide layout with loadAll=true to make sure we have the features
                                    SlideLayout l = layoutRepository.getSlideLayoutById(s.getPrintedSlide().getLayout().getId(), null, true);
                                    if (l == null) {
                                        // if the id is not the public one, find the public layout id
                                        if (s.getPrintedSlide() != null && s.getPrintedSlide().getLayout() != null && s.getPrintedSlide().getLayout().getUri() != null
                                                && !s.getPrintedSlide().getLayout().getUri().contains("public")) {
                                            String publicURI = repository.getPublicUri(s.getPrintedSlide().getLayout().getUri(), user);
                                            if (publicURI != null) {
                                                String publicId = publicURI.substring(publicURI.lastIndexOf("/")+1);
                                                l = layoutRepository.getSlideLayoutById(publicId, null, true);
                                            }
                                            
                                        }
                                    }
                                    if (l != null) {
                                        for (Block b: l.getBlocks()) {
                                            if (b.getBlockLayout() != null) {
                                                for (Spot spot: b.getBlockLayout().getSpots()) {
                                                    if (spot.getFeatures() != null) {
                                                        for (Feature f: spot.getFeatures()) {
                                                            GlygenArrayController.populateFeatureGlycanImages(f, imageLocation);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        logger.error("Generating public glycan cartoons: "
                                                + "cannot locate the public slide layout correctly!" + s.getPrintedSlide().getLayout().getUri());
                                    }
                                }
                            }
                        }
                        
                    } catch (SparqlException | SQLException e1) {
                        logger.error("Could not make the dataset public", e1);
                        throw new GlycanRepositoryException("Could not make the dataset public", e1);
                    } 
                });
                datasetURI.get(200, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                return null; // not ready yet
            } catch (Exception e) {
                logger.error("exception while making dataset public", e);
                if (e instanceof IllegalArgumentException && e.getCause() instanceof ErrorMessage) {
                    dataset.setError((ErrorMessage) e.getCause());
                } else {
                    errorMessage.addError(new ObjectError ("Exception", e.getMessage()));
                    dataset.setError(errorMessage);
                }
                dataset.setStatus(FutureTaskStatus.ERROR);
                datasetRepository.updateStatus (dataset.getUri(), dataset, owner);
                return null;
            }
            if (task.getStatus() == FutureTaskStatus.DONE) {
                String uri = datasetURI.get();
                return uri.substring(uri.lastIndexOf("/")+1);
            } else {
                return null; // not ready yet
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Array dataset cannot be made public for user " + p.getName(), e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new GlycanRepositoryException("Array dataset cannot be made public for user " + p.getName(), e);
        }
    }
    
    private FutureTaskStatus getDatasetStatus(ArrayDataset dataset) {
        FutureTaskStatus status = FutureTaskStatus.NOTSTARTED;
        if (dataset != null && dataset.getSlides() != null) {
            for (Slide slide: dataset.getSlides()) {
                if (slide.getImages() != null) {
                    for (Image image: slide.getImages()) {
                        if (image.getRawDataList() != null) {
                            for (RawData rawData: image.getRawDataList()) {
                                if (rawData.getStatus() == FutureTaskStatus.PROCESSING)
                                    return FutureTaskStatus.PROCESSING;
                                else if (rawData.getStatus() == FutureTaskStatus.ERROR)
                                    return FutureTaskStatus.ERROR;
                                else {
                                    if (rawData.getProcessedDataList() != null) {
                                        for (ProcessedData p: rawData.getProcessedDataList()) {
                                            if (p.getStatus() == FutureTaskStatus.PROCESSING)
                                                return FutureTaskStatus.PROCESSING;
                                            else if (p.getStatus() == FutureTaskStatus.ERROR)
                                                return FutureTaskStatus.ERROR;
                                            else 
                                                status = FutureTaskStatus.DONE;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return status;
    }

    @Operation(summary = "Download the given file", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/download", method = RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="File downloaded successfully"), 
            @ApiResponse(responseCode="400", description="File not found, or not accessible publicly", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))}),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to download files of the dataset"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<Resource> downloadFile(
            @Parameter(required=true, description="the folder of the file") 
            @RequestParam String fileFolder, 
            @Parameter(required=true, description="the identifier of the file to be downloaded") 
            @RequestParam String fileIdentifier,
            @Parameter(required=false, description="filename to save the downloaded file as. If not provided, the original file name is used if available") 
            @RequestParam(value="filename", required=false)
            String originalName, Principal p) {
        
        if (originalName != null) originalName = originalName.trim();
        fileFolder = fileFolder.trim();
        fileIdentifier = fileIdentifier.trim();
        
        // check to see if the user can access this file
        String datasetId = fileFolder.substring(fileFolder.lastIndexOf("/")+1);
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        ErrorMessage errorMessage = new ErrorMessage("Invalid request");
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        try {
            ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
            if (dataset == null) {
                // check if the user can access this dataset as a co-owner
                String coOwnedGraph = datasetRepository.getCoownerGraphForUser(user, GlycanRepositoryImpl.uriPrefix + datasetId);
                if (coOwnedGraph != null) {
                    UserEntity originalUser = userRepository.findByUsernameIgnoreCase(coOwnedGraph.substring(coOwnedGraph.lastIndexOf("/")+1));
                    if (originalUser != null) {
                        dataset = datasetRepository.getArrayDataset(datasetId, false, originalUser);
                    }
                }
                if (dataset == null) {
                    errorMessage.addError(new ObjectError("fileIdentifier", "This file does not belong to this user. Cannot be downloaded!"));
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                }
            } 
        } catch (Exception e) {
            throw new GlycanRepositoryException("Array dataset cannot be loaded for user " + p.getName(), e);
        }
        File file = new File(fileFolder, fileIdentifier);
        if (!file.exists()) {
            errorMessage.addError(new ObjectError("fileIdentifier", "NotFound"));
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
            return ResponseEntity.notFound().build();
            //throw new IllegalArgumentException ("File is not accessible", errorMessage);
        }
        
        if (originalName == null) {
            try {
                FileWrapper fw = repository.getFileByIdentifier(fileIdentifier, user);
                if (fw != null) {
                    originalName = fw.getOriginalName();
                }
            } catch (Exception e) {
                logger.warn ("error getting file details from the repository", e);
                
            }
            if (originalName == null)
                originalName = fileIdentifier;
        }
 
        return download (file, originalName);
    }
    
    public static ResponseEntity<Resource> download (File file, String originalName) {
        FileSystemResource r = new FileSystemResource(file);
        MediaType mediaType = MediaTypeFactory
                .getMediaType(r)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
       
        
        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(mediaType);
        respHeaders.setContentLength(file.length());
       //respHeaders.add("Content-Transfer-Encoding", "binary");

        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(originalName)
                .build();

        respHeaders.set(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        respHeaders.set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,"Content-Disposition");
        
        return new ResponseEntity<Resource>(
                r, respHeaders, HttpStatus.OK
        );
    }
    
    @Operation(summary = "Export processed data in glygen array data file format", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/downloadProcessedData", method=RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="File generated successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, file cannot be found"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve processed data"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<Resource> exportProcessedData (
            @Parameter(required=true, description="id of the processed data") 
            @RequestParam("processeddataid")
            String processedDataId,
            @Parameter(required=false, description="the name for downloaded file") 
            @RequestParam(value="filename", required=false)
            String fileName,        
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        
        String uri = GlygenArrayRepositoryImpl.uriPrefix + processedDataId;
        if (fileName == null || fileName.isEmpty()) {
            fileName = processedDataId + ".xlsx";
        }
        File newFile = new File (uploadDir, "tmp" + fileName);
        
        try {
            ProcessedData data = datasetRepository.getProcessedDataFromURI(uri, true, user);
            if (data == null) {
                // check if it is public
                uri = GlygenArrayRepositoryImpl.uriPrefixPublic + processedDataId;
                data = datasetRepository.getProcessedDataFromURI(uri, true, null);
                if (data == null) {
                    errorMessage.addError(new ObjectError("processeddataid", "NotFound"));
                }
            }
          
            if (data != null) {
                try {
                    ProcessedDataParser.exportToFile(data, newFile.getAbsolutePath());  
                } catch (IOException e) {
                    errorMessage.addError(new ObjectError("file", "NotFound"));
                }
            }
            
            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return download (newFile, fileName);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve dataset from the repository", e);
        }
    }
    
    @RequestMapping(value="/getstatistics", method=RequestMethod.GET, produces={"application/xml", "application/json"})
    @Operation(summary="Retrieve the stats of the repository for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Stats retrieved successfully", content = {
            @Content( schema = @Schema(implementation = UserStatisticsView.class))}), 
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public @ResponseBody UserStatisticsView getStatistics (Principal p) {
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        UserStatisticsView stats = new UserStatisticsView();
        try {
            stats.setDatasetCount((long) datasetRepository.getArrayDatasetCountByUser(user, null));
            stats.setSlideCount((long) layoutRepository.getSlideLayoutCountByUser(user, null));
            stats.setSampleCount((long) metadataRepository.getSampleCountByUser(user, null));
            stats.setGlycanCount((long)glycanRepository.getGlycanCountByUser(user, null));
            int linker = linkerRepository.getLinkerCountByUserByType(user, LinkerType.SMALLMOLECULE, null);
            //linker += linkerRepository.getLinkerCountByUserByType(user, LinkerType.UNKNOWN_SMALLMOLECULE, null);
            stats.setLinkerCount((long)linker);
            
            int protein = linkerRepository.getLinkerCountByUserByType(user, LinkerType.PROTEIN, null);
            //protein += linkerRepository.getLinkerCountByUserByType(user, LinkerType.UNKNOWN_PROTEIN, null);
            stats.setProteinCount((long)protein);
            
            int lipid = linkerRepository.getLinkerCountByUserByType(user, LinkerType.LIPID, null);
            //lipid += linkerRepository.getLinkerCountByUserByType(user, LinkerType.UNKNOWN_LIPID, null);
            stats.setLipidCount((long)lipid);
            
            int peptide = linkerRepository.getLinkerCountByUserByType(user, LinkerType.PEPTIDE, null);
            //peptide += linkerRepository.getLinkerCountByUserByType(user, LinkerType.UNKNOWN_PEPTIDE, null);
            stats.setPeptideCount((long)peptide);
            
            // count the public ones
            int publicDataset = datasetRepository.getPublicArrayDatasetCountByUser(user); 
            stats.setPublicDatasetCount((long)publicDataset);
            int publicSlide = datasetRepository.getPublicSlideCountByUser(user); 
            stats.setPublicSlideCount((long)publicSlide);
        } catch (SQLException | SparqlException e) {
            throw new GlycanRepositoryException("Cannot retrieve the counts from the repository",e);
        }
        return stats;
    }
    
    @Operation(summary = "Export metadata into Excel", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/downloadMetadata", method=RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="File generated successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, file cannot be found"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve array dataset"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<Resource> exportAllMetadata (
            @Parameter(required=true, description="id of the array dataset") 
            @RequestParam("datasetId")
            String datasetId,
            @Parameter(required=false, description="the name for downloaded file") 
            @RequestParam(value="filename", required=false)
            String fileName,   
            @Parameter(required=false, description="mirage metadata only") 
            @RequestParam(value="mirageOnly", required=false)
            Boolean mirageOnly,
            @Parameter(required=false, description="single sheet") 
            @RequestParam(value="singleSheet", required=false)
            Boolean singleSheet,
            @Parameter(required=false, name="filetype", description="type of the file, the default is Excel", schema = @Schema(type = "string", allowableValues= {"Excel", "json" })) 
            @RequestParam(required=false, value="filetype") String fileType,
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        
        if (fileName == null || fileName.isEmpty()) {
            if (fileType == null || fileType.equalsIgnoreCase("excel"))
                fileName = datasetId + ".xlsx";
            else 
                fileName = datasetId + ".json";
        }
        File newFile = new File (uploadDir, "tmp" + fileName);
        
        try {
            ArrayDataset data = datasetRepository.getArrayDataset(datasetId, true, false, user);
            if (data == null) {
                // check if it is public
                data = datasetRepository.getArrayDataset(datasetId, true, false, null);
                if (data == null) {
                    errorMessage.addError(new ObjectError("datasetId", "NotFound"));
                }
            }
          
            if (data != null) {
                try {
                    // update user info
                    if (user != null) {
                        data.getUser().setFirstName(user.getFirstName());
                        data.getUser().setLastName(user.getLastName());
                    }      
                    if (fileType == null || fileType.equalsIgnoreCase("excel")) {
                        new MetadataImportExportUtil(scheme+host+basePath).exportIntoExcel(data, newFile.getAbsolutePath(), mirageOnly, singleSheet);
                    } else {
                        // json 
                        AllMetadataView view = new AllMetadataView();
                        Set<MetadataCategory> metadataList = new HashSet<MetadataCategory>();
                        metadataList.add(data.getSample());
                        if (data.getSlides() != null) {
                            for (Slide slide: data.getSlides()) {
                                if (slide.getMetadata() != null) {
                                    metadataList.add(slide.getMetadata());
                                }
                                if (slide.getPrintedSlide().getMetadata() != null) {
                                    metadataList.add(slide.getPrintedSlide().getMetadata());
                                }
                                if (slide.getPrintedSlide().getPrinter() != null) {
                                    metadataList.add(slide.getPrintedSlide().getPrinter());
                                }
                                if (slide.getPrintedSlide().getPrintRun() != null) {
                                    metadataList.add(slide.getPrintedSlide().getPrintRun());
                                }
                                if (slide.getImages() != null) {
                                    for (Image image: slide.getImages()) {
                                        if (image.getScanner() != null) {
                                            metadataList.add(image.getScanner());
                                        }
                                        for (RawData rawData: image.getRawDataList()) {
                                            if (rawData.getMetadata() != null) {
                                                metadataList.add(rawData.getMetadata());
                                            }
                                            for (ProcessedData processed: rawData.getProcessedDataList()) {
                                                if (processed.getMetadata() != null) {
                                                    metadataList.add(processed.getMetadata());
                                                }
                                            }
                                        }
                                    }
                                    
                                }
                            }
                        }
                        view.setMetadataList(new ArrayList<>(metadataList));
                        try {
                            SettingEntity entity = settingsRepository.findByName("apiVersion");
                            if (entity != null) {
                                view.setVersion(entity.getValue()); 
                            }
                        } catch (Exception e) {
                            view.setVersion("1.0.0");
                        }
                        
                        ObjectMapper mapper = new ObjectMapper();         
                        String json = mapper.writeValueAsString(view);
                        PrintWriter writer = new PrintWriter(newFile);
                        writer.write (json);
                        writer.close();
                    }
                } catch (IOException e) {
                    errorMessage.addError(new ObjectError("file", "NotFound"));
                }
            }
            
            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return download (newFile, fileName);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve dataset from the repository", e);
        }
    }
}
