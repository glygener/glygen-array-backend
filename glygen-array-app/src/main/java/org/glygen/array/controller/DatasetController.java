package org.glygen.array.controller;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.glygen.array.config.SesameTransactionConfig;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.GraphPermissionEntity;
import org.glygen.array.persistence.SettingEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.GraphPermissionRepository;
import org.glygen.array.persistence.dao.SettingsRepository;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.Creator;
import org.glygen.array.persistence.rdf.Publication;
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
import org.glygen.array.util.parser.ProcessedDataParser;
import org.glygen.array.util.parser.RawdataParser;
import org.glygen.array.util.pubmed.DTOPublication;
import org.glygen.array.util.pubmed.PubmedUtil;
import org.glygen.array.view.ArrayDatasetListView;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.glygen.array.view.MetadataListResultView;
import org.glygen.array.view.PrintedSlideListView;
import org.glygen.array.view.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

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
    UserRepository userRepository;
    
    @Value("${spring.file.uploaddirectory}")
    String uploadDir;
    
    @Autowired
    ResourceLoader resourceLoader;
    
    @Autowired
    Validator validator;
    
    @Autowired
    AsyncService parserAsyncService;
    
    @ApiOperation(value = "Add given array dataset  for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/addDataset", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added array dataset"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to register array datasets"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addDataset (
            @ApiParam(required=true, value="Array dataset to be added") 
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
                
            }
            datasetRepository.updateArrayDataset(dataset, user);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Array dataset cannot be added for user " + p.getName(), e);
        }
    } 
    
    @ApiOperation(value = "Add given publication to the dataset for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/addPublication", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added publication"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to add publications"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addPublication (
            @ApiParam(required=true, value="Publication to be added.")
            @RequestBody Publication publication, 
            @ApiParam(required=true, value="id of the array dataset (must already be in the repository) to add the publication") 
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
    
    @ApiOperation(value = "Add given file to the dataset for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/addFile", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"}, produces= {"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return confirmation"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to add files"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation addFile (
            @ApiParam(required=true, value="File to be added.")
            @RequestBody FileWrapper file, 
            @ApiParam(required=true, value="id of the array dataset (must already be in the repository) to add the file") 
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
            dataset.getFiles().add(file);
                
            datasetRepository.updateArrayDataset(dataset, owner);
            return new Confirmation("File added successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Array dataset cannot be added for user " + p.getName(), e);
        }
    }
    
    @ApiOperation(value = "Add given grant to the dataset for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/addGrant", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added grant"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to add grants"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addGrant (
            @ApiParam(required=true, value="Grant to be added.")
            @RequestBody Grant grant, 
            @ApiParam(required=true, value="id of the array dataset (must already be in the repository) to add the grant") 
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
    
    @ApiOperation(value="Retrieving all keywords from the repository", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/getallkeywords", method=RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses(value= {@ApiResponse(code=200, message="list of existing keywords in the repository"),
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to retrieve keywords"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Set<String> getKeywords(){
        try {
            return datasetRepository.getAllKeywords();
        } catch (SparqlException | SQLException e) {
            ErrorMessage errorMessage = new ErrorMessage("Error retrieving keywords from the repository");
            errorMessage.addError(new ObjectError("keyword", e.getMessage()));
            errorMessage.setErrorCode(ErrorCodes.INTERNAL_ERROR);
            errorMessage.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            throw errorMessage;
        }
    }
    
    @ApiOperation(value = "Add given keyword to the dataset for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/addKeyword", method = RequestMethod.POST, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return confirmation"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to add keywords"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation addKeyword (
            @ApiParam(required=true, value="Keyword to be added.")
            @RequestParam("keyword") String keyword, 
            @ApiParam(required=true, value="id of the array dataset (must already be in the repository) to add the keyword") 
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
    
    @ApiOperation(value = "Add given collaborator to the dataset for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/addCollaborator", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return confirmation"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to add collaborators"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation addCollaborator (
            @ApiParam(required=true, value="Collaborator user to be added.")
            @RequestBody User collab, 
            @ApiParam(required=true, value="id of the array dataset (must already be in the repository) to add the collaborator") 
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
    
    @ApiOperation(value = "Add the given user as a co-owner to the given dataset", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/addCoowner", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return confirmation message"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to add co-owners"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation addCoownerToDataset (
            @ApiParam(required=true, value="User to be added.")
            @RequestBody User coowner, 
            @ApiParam(required=true, value="id of the array dataset (must already be in the repository) to add the co-owner") 
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
    
    @ApiOperation(value = "Delete the given user as a co-owner from the given dataset", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deleteCoowner/{username}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return confirmation if co-owner deleted successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete co-owners"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation removeCoownerFromDataset (
            @ApiParam(required=true, value="User to be removed.")
//            @RequestBody User coowner,
            @PathVariable("username") String coowner,
            @ApiParam(required=true, value="id of the array dataset (must already be in the repository) to remove the co-owner") 
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
    
    @ApiOperation(value = "Add given slide to the dataset for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/addSlide", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added slide"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to register datasets"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addSlide (
            @ApiParam(required=true, value="Slide to be added. Slide"
                    + " should have an existing printedSlide (specified by name or uri or id), "
                    + "and it should have an existing AssayMetadata (specified by name, id or uri, "
                    + " images are ignored. You should use addImage to add the images and other data")
            @RequestBody Slide slide, 
            @ApiParam(required=true, value="id of the array dataset (must already be in the repository) to add the slide") 
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
                errorMessage.addError(new ObjectError("assatMetadata", "NoEmpty"));
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
    
    @ApiOperation(value = "Add given Image to the slide of the dataset for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/addImage", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added image"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to add images to a dataset"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addImage (
            @ApiParam(required=true, value="Image to be added. Image"
                    + " should have a filename (already uploaded), "
                    + "and it should have an existing ScannerMetadata (specified by name, id or uri), "
                    + " Raw data are ignored. You should be using addRawData to add those")
            @RequestBody Image image, 
            @ApiParam(required=true, value="id of the array dataset (must already be in the repository) to add the image") 
            @RequestParam("arraydatasetId")
            String datasetId,  
            @ApiParam(required=true, value="id of the slide (must already be in the repository) to add the image") 
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
    
    
    @ApiOperation(value = "Add given Rawdata to the image of the dataset for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/addRawdata", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added rawdata"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to add rawdata to a dataset"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addRawData (
            @ApiParam(required=true, value="Raw Data to be added. Raw data"
                    + " should have a filename (already uploaded), slide information with printed slide id/uri, "
                    + "and it should have an existing ImageAnalysisMetadata (specified by name, id or uri)"
                    + " Processed data are ignored. You should use addProcessedDataFromExcel to add the processed data")
            @RequestBody RawData rawData, 
            @ApiParam(required=true, value="id of the array dataset (must already be in the repository) to add the raw data") 
            @RequestParam("arraydatasetId")
            String datasetId,  
            @ApiParam(required=true, value="id of the image (must already be in the repository) to add the raw data") 
            @RequestParam("imageId")
            String imageId,  
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
            if (dataset == null)
                errorMessage.addError(new ObjectError("dataset", "NotFound"));
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
        }
        
        Image image = null;
        // check if the image with the given id exists
        try {
            image = datasetRepository.getImageFromURI(GlygenArrayRepositoryImpl.uriPrefix + imageId, false, owner);
            if (image == null) {
                errorMessage.addError(new ObjectError("image", "NotFound"));
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
        } else {
            try {
                String printedSlideUri = rawData.getSlide().getPrintedSlide().getUri();
                if (printedSlideUri != null) {
                    PrintedSlide existing = datasetRepository.getPrintedSlideFromURI(printedSlideUri, owner);
                    if (existing == null) {
                        errorMessage.addError(new ObjectError("printedSlide", "NotFound"));
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
            if (!allowPartialData) errorMessage.addError(new ObjectError("filename", "NotFound"));
        } else {
            String fileFolder = uploadDir;
            if (rawData.getFile().getFileFolder() != null)
                fileFolder = rawData.getFile().getFileFolder();
            file = new File (fileFolder, rawData.getFile().getIdentifier());
            if (!file.exists()) {
                errorMessage.addError(new ObjectError("file", "NotFound"));
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
                if (rawData.getSlide().getPrintedSlide().getLayout().getIsPublic()) {
                    fullLayout = layoutRepository.getSlideLayoutById(rawData.getSlide().getPrintedSlide().getLayout().getId(), null);
                    uriPre = ArrayDatasetRepositoryImpl.uriPrefixPublic;
                } else {
                    fullLayout = layoutRepository.getSlideLayoutById(rawData.getSlide().getPrintedSlide().getLayout().getId(), owner);
                }
                try {
                    Map<Measurement, Spot> dataMap = RawdataParser.parse(rawData.getFile(), fullLayout, rawData.getPowerLevel());
                    // check blocks used and extract only those measurements
                    if (rawData.getSlide().getBlocksUsed() != null && !rawData.getSlide().getBlocksUsed().isEmpty()) {
                        Map<Measurement, Spot> filteredMap = new HashMap<Measurement, Spot>();
                        List<String> foundBlocks = new ArrayList<String>();
                        for (Map.Entry<Measurement, Spot> entry: dataMap.entrySet()) {
                            for (String blockId: rawData.getSlide().getBlocksUsed()) { 
                                if (entry.getValue().getBlockLayoutUri().equals(uriPre + blockId)) {
                                    filteredMap.put(entry.getKey(), entry.getValue());
                                    if (!foundBlocks.contains(blockId)) {
                                        foundBlocks.add(blockId);
                                    }
                                    break;
                                }
                            }
                        }
                        if (foundBlocks.size() != rawData.getSlide().getBlocksUsed().size()) {
                            // we could not find the data for the selected blocks from the raw data file
                            errorMessage.addError(new ObjectError("blocksUsed", "NotValid"));
                            throw new IllegalArgumentException("Cannot parse the file", errorMessage);
                        }
                        rawData.setDataMap(filteredMap); 
                    } else {
                        rawData.setDataMap(dataMap);
                    }
                } catch (IOException e) {
                    errorMessage.addError(new ObjectError("file", e.getMessage()));
                    throw new IllegalArgumentException("Cannot parse the file", errorMessage);
                }
                UserEntity originalUser = owner;
                rawDataURI = datasetRepository.addMeasurementsToRawData(rawData, owner);
                rawDataURI.whenComplete((uriString, e) -> {
                    try {
                        if (e != null) {
                            logger.error(e.getMessage(), e);
                            rawData.setStatus(FutureTaskStatus.ERROR);
                            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException && e.getCause().getCause() instanceof ErrorMessage) 
                                rawData.setError((ErrorMessage) e.getCause().getCause());
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
                throw e;
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
            }
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Rawdata cannot be added for user " + p.getName(), e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new GlycanRepositoryException("Cannot add the raw data measurements to the repository", e);
        }
    }
    
    @ApiOperation(value = "Add given printed slide set for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/addPrintedSlide", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added printed slide"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to register slides"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addPrintedSlide (
            @ApiParam(required=true, value="Printed slide to be added") 
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
    
    @ApiOperation(value = "Add given data processing software for the user, authorizations = { @Authorization(value=\"Authorization\") }")
    @RequestMapping(value="/addDataProcessingSoftware", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added data processing metadata"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to register data processing metadata"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addDataProcessingSoftware (
            @ApiParam(required=true, value="Data processing software metadata to be added") 
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
    
    @ApiOperation(value = "Add given image analysis software for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/addImageAnalysis", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added image analysis metadata"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to register image analysis metadata"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addImageAnalysisSoftware (
            @ApiParam(required=true, value="Image Analysis metadata to be added") 
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
    
    @ApiOperation(value = "Add given printer metadata for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/addPrinter", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added printer"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to register printers"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addPrinter (
            @ApiParam(required=true, value="Printer metadata to be added") 
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
    
    @ApiOperation(value = "Add given print run metadata for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/addPrintrun", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added print run"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to register print runs"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addPrintrun (
            @ApiParam(required=true, value="Print run metadata to be added") 
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
    
    @ApiOperation(value = "Add given assay metadata for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/addAssayMetadata", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added assay metadata"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to register assay metadata"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addAssayMetadata (
            @ApiParam(required=true, value="Assay metadata to be added") 
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
    
    @ApiOperation(value = "Add given spot metadata for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/addSpotMetadata", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added spot metadata"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to register spot metadata"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addSpotMetadata (
            @ApiParam(required=true, value="Spot metadata to be added") 
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
    
    @ApiOperation(value = "Import processed data results from uploaded excel file", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/addProcessedDataFromExcel", method=RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added processed data for the given raw data of the given array dataset"), 
            @ApiResponse(code=400, message="Invalid request, file cannot be found"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to modify array datasets"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addProcessedDataFromExcel (
            @ApiParam(required=true, value="processed data file details such as name, original name, folder, format") 
            @RequestBody
            FileWrapper file,
            @ApiParam(required=true, value="id of the array dataset (must already be in the repository) to add the processed data") 
            @RequestParam("arraydatasetId")
            String datasetId,  
            @ApiParam(required=true, value="id of the raw data (must already be in the repository) to add the processed data") 
            @RequestParam("rawdataId")
            String rawDataId,  
            @ApiParam(required=false, value="Data processing software metadata id (must already be in the repository)") 
            @RequestParam(value="metadataId", required=false)
            String metadataId,
            @ApiParam(required=true, value="the statistical method used (eg. eliminate, average etc.") 
            @RequestParam("methodName")
            String methodName,
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
            if (dataset == null)
                errorMessage.addError(new ObjectError("dataset", "NotFound"));
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve dataset from the repository", e);
        }
        
        RawData rawData = null;
        // check if the rawData with the given id exists
        try {
            rawData = datasetRepository.getRawDataFromURI(GlygenArrayRepositoryImpl.uriPrefix + rawDataId, false, user);
            if (rawData == null) {
                errorMessage.addError(new ObjectError("rawdata", "NotFound"));
            } else {
                // check if it is done
                if (rawData.getStatus() != FutureTaskStatus.DONE) {
                    errorMessage.addError(new ObjectError("rawdata", "NotDone"));
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
        }
            
        if (metadataId != null) {    
            try {
                metadata = metadataRepository.getDataProcessingSoftwareFromURI(GlygenArrayRepositoryImpl.uriPrefix + metadataId, owner);
                if (metadata == null) {
                    errorMessage.addError(new ObjectError("metadata", "NotFound"));
                }
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Cannot retrieve data processing software metadata", e);
            }
        } else {
            if (!allowPartialData) errorMessage.addError(new ObjectError("metadata", "NoEmpty"));
        }
        ProcessedData processedData = new ProcessedData();     
        processedData.setMetadata(metadata);
        processedData.setFile(file);
        try {
            List<StatisticalMethod> methods = templateRepository.getAllStatisticalMethods();
            StatisticalMethod found = null;
            for (StatisticalMethod method: methods) {
                if (method.getName().equalsIgnoreCase(methodName)) {
                    found = method;
                }
            }
            if (found == null)
                errorMessage.addError(new ObjectError("method", "NotValid"));
            else {
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
            CompletableFuture<List<Intensity>> intensities = null;
            try {
                intensities = parserAsyncService.parseProcessDataFile(datasetId, file, mySlide, owner);
                intensities.whenComplete((intensity, e) -> {
                    try {
                        String uri = processedData.getUri();
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
                    } 
                })/*.exceptionally(ex -> { 
                    logger.error("Exception in processed data parsing", ex);
                    if (ex.getCause() != null && ex.getCause() instanceof IllegalArgumentException) {
                        if (ex.getCause().getCause() != null && ex.getCause().getCause() instanceof ErrorMessage) {
                            processedData.setError((ErrorMessage) ex.getCause().getCause());
                        } else {
                            errorMessage.addError(new ObjectError("processedData", "Cannot complete processing. Reason:" + ex.getMessage()));
                            processedData.setError(errorMessage);
                        }
                    } else {
                        errorMessage.addError(new ObjectError("processedData", "Cannot complete processing. Reason:" + ex.getMessage()));
                        processedData.setError(errorMessage);
                    }
                    processedData.setStatus(FutureTaskStatus.ERROR);
                    return null;
                })*/;
                processedData.setIntensity(intensities.get(5000, TimeUnit.MILLISECONDS));
            } catch (IllegalArgumentException e) {
                if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                    processedData.setError((ErrorMessage) e.getCause());
                } else {
                    errorMessage.addError(new ObjectError("processedData", "Cannot complete processing. Reason:" + e.getMessage()));
                    processedData.setError(errorMessage);
                }
                processedData.setStatus(FutureTaskStatus.ERROR);
            } catch (TimeoutException e) {
                synchronized (this) {
                    // save whatever we have for now for processed data and update its status to "processing"
                    String uri = datasetRepository.addProcessedData(processedData, rawDataId, owner);  
                    processedData.setUri(uri);
                    String id = uri.substring(uri.lastIndexOf("/")+1);
                    if (processedData.getError() == null)
                        processedData.setStatus(FutureTaskStatus.PROCESSING);
                    else 
                        processedData.setStatus(FutureTaskStatus.ERROR);
                    datasetRepository.updateStatus (uri, processedData, owner);
                    return id;
                }
            } 
            
            //TODO do we ever come to this ??
            if (intensities != null && intensities.isDone()) {
                file.setFileFolder(uploadDir + File.separator + datasetId);
                processedData.setFile(file);
                processedData.setIntensity(intensities.get());
                String uri = datasetRepository.addProcessedData(processedData, rawDataId, owner);   
                String id = uri.substring(uri.lastIndexOf("/")+1);
                if (processedData.getError() == null)
                    processedData.setStatus(FutureTaskStatus.DONE);
                else 
                    processedData.setStatus(FutureTaskStatus.ERROR);
                datasetRepository.updateStatus (uri, processedData, owner);
                return id;
            } else {
                String uri = datasetRepository.addProcessedData(processedData, rawDataId, owner);  
                processedData.setUri(uri);
                String id = uri.substring(uri.lastIndexOf("/")+1);
                datasetRepository.updateStatus (uri, processedData, owner);
                return id;
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new GlycanRepositoryException("Cannot add the intensities to the repository", e);
        }
    }
    
    
    @ApiOperation(value = "Download exclusion lists for the processed data to a file", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/downloadProcessedDataExclusionInfo", method=RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(code=200, message="File downloaded successfully"), 
            @ApiResponse(code=400, message="Invalid request, file cannot be found"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to add array datasets"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public ResponseEntity<Resource> downloadProcessedDataExclusionInfo (
            @ApiParam(required=true, value="id of the array dataset (must already be in the repository) to retrieve the exclusion info") 
            @RequestParam("arraydatasetId")
            String datasetId,
            @ApiParam(required=true, value="the name for downloaded file") 
            @RequestParam("filename")
            String fileName,        
            @ApiParam(required=true, value="an existing processed data id") 
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
    
    
    @ApiOperation(value = "Add the exclusion info given in the file to the given processed data", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/addExclusionInfoFromFile", method=RequestMethod.POST)
    @ApiResponses (value ={@ApiResponse(code=200, message="return an (otherwise) empty processed data object containing only the exclusion lists"), 
            @ApiResponse(code=400, message="Invalid request, file cannot be found"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addExclusionInfoFromFile (
            @ApiParam(required=true, value="id of the array dataset (must already be in the repository) to add the processed data") 
            @RequestParam("arraydatasetId")
            String datasetId, 
            @ApiParam(required=true, value="uploaded file with the exclusion information")
            @RequestParam("file") String uploadedFileName,
            @ApiParam(required=true, value="processed data to add the exclusion information")
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
    
    @ApiOperation(value = "Update processed data with results from uploaded excel file", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/updateProcessedDataFromExcel", method=RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added processed data for the given array dataset"), 
            @ApiResponse(code=400, message="Invalid request, file cannot be found"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to add array datasets"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String updateProcessedDataFromExcel (
            @ApiParam(required=true, value="id of the array dataset (must already be in the repository) to add the processed data") 
            @RequestParam("arraydatasetId")
            String datasetId,      
            @ApiParam(required=true, value="id of the raw data (must already be in the repository) to add the processed data") 
            @RequestParam("rawdataId")
            String rawDataId, 
            @ApiParam(required=true, value="processed data with an existing id/uri. If file is provided, the new file information is used. "
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
    
    
    @ApiOperation(value = "Add given sample metadata for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/addSample", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added sample"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to register samples"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addSample (
            @ApiParam(required=true, value="Sample metadata to be added") 
            @RequestBody Sample sample, 
            @ApiParam(required=false, defaultValue = "true", value="bypass mandatory/multiplicty validation checks if set to false (not recommended)") 
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
    
    @ApiOperation(value = "Add given scanner metadata for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/addScanner", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added scanner"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to register scanners"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addScanner (
            @ApiParam(required=true, value="Scanner metadata to be added") 
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
    
    
    @ApiOperation(value = "Add given slide metadata for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/addSlideMetadata", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added slide metadata"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to register slides"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addSlideMetadata (
            @ApiParam(required=true, value="Slide metadata to be added") 
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
    @ApiOperation(value="Checks whether the given name is available to be used (returns true if available, false if already in use", response=Boolean.class, authorizations = { @Authorization(value="Authorization") })
    @ApiResponses (value ={@ApiResponse(code=200, message="Check performed successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
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
    @ApiOperation(value="Checks whether the given metadata contains all MIRAGE recommended descriptors", response=Boolean.class, authorizations = { @Authorization(value="Authorization") })
    @ApiResponses (value ={@ApiResponse(code=200, message="Check performed successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
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
        
        for (DescriptionTemplate descTemplate: template.getDescriptors()) {
            boolean exists = false;
            if (descTemplate.isGroup() && descTemplate.isMirage()) {
                // check if it is provided in metadata
                for (DescriptorGroup g: metadata.getDescriptorGroups()) {
                    if (g.getKey().equals(descTemplate)) {
                        exists = true;
                        ErrorMessage error = checkMirageDescriptorGroup((DescriptorGroup)g, descTemplate);
                        if (error != null) {
                            for (ObjectError err: error.getErrors())
                                errorMessage.addError(err);
                        }  
                    }
                }
                
            } else if (descTemplate.isMirage()) {
                for (Descriptor d: metadata.getDescriptors()) {
                    if (d.getKey().equals(descTemplate))
                        exists = true;
                }
            }
            if (descTemplate.isMirage() && !exists) {
                // violation
                errorMessage.addError(new ObjectError (descTemplate.getName(), "NotFound"));
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
        
        for (DescriptionTemplate descTemplate: ((DescriptorGroupTemplate)descGroupTemplate).getDescriptors()) {
            boolean exists = false;
            for (Description d: descGroup.getDescriptors()) {
                if (d.getKey().equals(descTemplate)) {
                    exists = true;
                 
                    if (d.isGroup()) {
                        ErrorMessage error = checkMirageDescriptorGroup((DescriptorGroup)d, descTemplate);
                        if (error != null) {
                            for (ObjectError err: error.getErrors())
                                errorMessage.addError(err);
                        } 
                    }
                }
            }

            if (descTemplate.isMirage() && !exists) {
                // violation
                errorMessage.addError(new ObjectError (descTemplate.getName(), "NotFound"));
            }
        }
        
        if (errorMessage.getErrors() == null || errorMessage.getErrors().isEmpty())
            return null;
        return errorMessage;
    }

    @ApiOperation(value = "List all datasets for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/listArrayDataset", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Array datasets retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public ArrayDatasetListView listArrayDataset (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="load rawdata and processed data details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @ApiParam(required=false, value="a filter value to match") 
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
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(resultList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve array datasets for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @ApiOperation(value = "List all datasets for the user (as a coowner)", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/listArrayDatasetCoowner", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Array datasets retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public ArrayDatasetListView listArrayDatasetByCoowner (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="load rawdata and processed data details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @ApiParam(required=false, value="a filter value to match") 
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
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(resultList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve array datasets for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @ApiOperation(value = "List co-owners for the dataset", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/listcoowners", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Co-owners retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public List<User> listCoownersForDataset(
            @ApiParam(required=true, value="id of the array dataset for which to retrive the applicable coowners") 
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
    
    @ApiOperation(value = "List all printed slides for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/listPrintedSlide", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Printed slides retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public PrintedSlideListView listPrintedSlide (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable slides") 
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
    
    @ApiOperation(value = "List all printed slides for the user and the public ones", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/listAllPrintedSlide", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Printed slides retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public PrintedSlideListView listAllPrintedSlides (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="load slide layout details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable slides") 
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
    
    @ApiOperation(value = "List all data processing software metadata for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/listDataProcessingSoftware", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Data processing software metadata list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listDataProcessingSoftware (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="load descriptor details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable slides") 
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
    
    @ApiOperation(value = "List all image analysis software metadata for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/listImageAnalysisSoftware", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Image analysis software metadata list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listImageAnalysisSoftware (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="load descriptor details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable slides") 
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
    
    @ApiOperation(value = "List all printer metadata for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/listPrinters", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Printer list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listPrinters (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="load descriptor details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable slides") 
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
    
    @ApiOperation(value = "List all printer metadata for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/listPrintruns", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Printer list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listPrintRuns (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="load descriptor details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable slides") 
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
    
    @ApiOperation(value = "List all samples for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/listSamples", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Samples retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listSamples (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="load descriptor details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable samples") 
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
    
    @ApiOperation(value = "List all scanner metadata for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/listScanners", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Scanner list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listScanners (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="load descriptor details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable slides") 
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
    
    @ApiOperation(value = "List all slide metadata for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/listSlideMetadata", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Slide metadata list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listSlideMetadata (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="load descriptor details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable slides") 
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
    
    @ApiOperation(value = "List all assay metadata for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/listAssayMetadata", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Assay metadata list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listAssayMetadata (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="load descriptor details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable slides") 
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
    
    @ApiOperation(value = "List all spot metadata for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/listSpotMetadata", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Spot metadata list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listSpotMetadata (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="load descriptor details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable slides") 
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
    
    @ApiOperation(value = "Delete given printed slide from the user's list", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deleteprintedslide/{slideId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Slide deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete slides"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deletePrintedSlide (
            @ApiParam(required=true, value="id of the printed slide to delete") 
            @PathVariable("slideId") String id, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable slides") 
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
    
    @ApiOperation(value = "Delete the given array dataset from the user's list", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deletedataset/{datasetId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Dataset deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete datasets"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteArrayDataset (
            @ApiParam(required=true, value="id of the array dataset to delete") 
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
                    throw new IllegalArgumentException("Cannot delete the slide when it is public", errorMessage);
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
    
    @ApiOperation(value = "Delete the given raw data from the given array dataset", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deleterawdata/{rawdataId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="RawData deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete rawdata"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteRawData (
            @ApiParam(required=true, value="id of the rawdata to delete") 
            @PathVariable("rawdataId") String id, 
            @ApiParam(required=true, value="id of the array dataset this rawdata belongs to") 
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
    
    @ApiOperation(value = "Delete the given processed data from the given array dataset", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deleteprocesseddata/{processeddataId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="ProcessedData deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete ProcessedData"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteProcessedData (
            @ApiParam(required=true, value="id of the ProcessedData to delete") 
            @PathVariable("processeddataId") String id, 
            @ApiParam(required=true, value="id of the array dataset this ProcessedData belongs to") 
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
    
    
    @ApiOperation(value = "Delete the given slide from the given array dataset", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deleteslide/{slideId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Slide deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete slide"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteSlide (
            @ApiParam(required=true, value="id of the slide to delete") 
            @PathVariable("slideId") String id, 
            @ApiParam(required=true, value="id of the array dataset this slide belongs to") 
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
    
    @ApiOperation(value = "Delete the given image from the given array dataset", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deleteimage/{imageId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Image deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete image"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteImage (
            @ApiParam(required=true, value="id of the image to delete") 
            @PathVariable("imageId") String id, 
            @ApiParam(required=true, value="id of the array dataset this image belongs to") 
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
                throw new IllegalArgumentException("Cannot find slide with the given id", errorMessage);
            }
            
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete slide " + id, e);
        } 
    }
    
    @ApiOperation(value = "Delete the given publication from the given array dataset", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deletepublication/{publicationid}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Publication deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete publication"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deletePublication (
            @ApiParam(required=true, value="id of the publication to delete") 
            @PathVariable("publicationid") String id, 
            @ApiParam(required=true, value="id of the array dataset this publication belongs to") 
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
    
    @ApiOperation(value = "Delete the given grant from the given array dataset", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deletegrant/{grantid}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="grant deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete grant"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteGrant (
            @ApiParam(required=true, value="id of the grant to delete") 
            @PathVariable("grantid") String id, 
            @ApiParam(required=true, value="id of the array dataset this grant belongs to") 
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
    
    @ApiOperation(value = "Delete the given keyword from the given array dataset", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deletekeyword/{keyword}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="keyword deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete keywords"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteKeyword (
            @ApiParam(required=true, value="id of the file to delete") 
            @PathVariable("keyword") String keyword, 
            @ApiParam(required=true, value="id of the array dataset this keyword belongs to") 
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
    
    @ApiOperation(value = "Delete the given file from the given array dataset", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deletefile/{fileidentifier}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="file deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete file"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteFile (
            @ApiParam(required=true, value="id of the file to delete") 
            @PathVariable("fileidentifier") String id, 
            @ApiParam(required=true, value="id of the array dataset this file belongs to") 
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
                for (FileWrapper f: dataset.getFiles()) {
                    if (f.getIdentifier().equals(id)) {
                        found = true;
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
    
    @ApiOperation(value = "Delete the given collaborator from the given array dataset", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deletecollaborator/{username}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Collaborator deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete collaborator"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteCollaborator (
            @ApiParam(required=true, value="username of the collaborator to delete") 
            @PathVariable("username") String username, 
            @ApiParam(required=true, value="id of the array dataset this collaborator belongs to") 
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
    
    @ApiOperation(value = "Delete given sample from the user's list", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deletesample/{sampleId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Sample deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete samples"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteSample (
            @ApiParam(required=true, value="id of the sample to delete") 
            @PathVariable("sampleId") String id, 
            @ApiParam(required=false, value="id of the array dataset (to check for permissions)") 
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
    
    @ApiOperation(value = "Delete given image analysis software from the user's list", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deleteimagemetadata/{imageAnaysisMetadataId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Image analysis software deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete image analysis software"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteImageAnalysisSoftware (
            @ApiParam(required=true, value="id of the image analysis software to delete") 
            @PathVariable("imageAnaysisMetadataId") String id, 
            @ApiParam(required=false, value="id of the array dataset (to check for permissions)") 
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
    
    @ApiOperation(value = "Delete given slide metadata from the user's list", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deleteslidemetadata/{slideMetadataId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Slide metadata deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete slide metadata"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteSlideMetadata (
            @ApiParam(required=true, value="id of the slide metadata to delete") 
            @PathVariable("slideMetadataId") String id, 
            @ApiParam(required=false, value="id of the array dataset (to check for permissions)") 
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
    
    @ApiOperation(value = "Delete given data processing software from the user's list", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deletedataprocessingmetadata/{dataProcessingMetadataId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Data processing software deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete data processing software"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteDataProcessingSoftware (
            @ApiParam(required=true, value="id of the data processing software to delete") 
            @PathVariable("dataProcessingMetadataId") String id, 
            @ApiParam(required=false, value="id of the array dataset (to check for permissions)") 
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
    
    @ApiOperation(value = "Delete given scanner from the user's list", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deletescannermetadata/{scannerId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Scanner deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete scanner"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteScanner (
            @ApiParam(required=true, value="id of the scanner to delete") 
            @PathVariable("scannerId") String id, 
            @ApiParam(required=false, value="id of the array dataset (to check for permissions)") 
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
    
    @ApiOperation(value = "Delete given printer from the user's list", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deleteprintermetadata/{printerId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Printer deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete printer"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deletePrinter (
            @ApiParam(required=true, value="id of the printer metadata to delete") 
            @PathVariable("printerId") String id, 
            @ApiParam(required=false, value="id of the array dataset (to check for permissions)") 
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
    
    @ApiOperation(value = "Delete given printrun from the user's list", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deleteprintrunmetadata/{printrunId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Printer deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete printer"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deletePrintRun (
            @ApiParam(required=true, value="id of the printrun metadata to delete") 
            @PathVariable("printrunId") String id, 
            @ApiParam(required=false, value="id of the array dataset (to check for permissions)") 
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
    
    @ApiOperation(value = "Delete given assay metadata from the user's list", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deleteassaymetadata/{assayId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Assay metadata deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete assay metadata"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteAssayMetadata (
            @ApiParam(required=true, value="id of the assay metadata to delete") 
            @PathVariable("assayId") String id, 
            @ApiParam(required=false, value="id of the array dataset (to check for permissions)") 
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
    
    @ApiOperation(value = "Delete given spot metadata from the user's list", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/deletespotmetadata/{spotMetadataId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Assay metadata deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete spot metadata"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteSpotMetadata (
            @ApiParam(required=true, value="id of the spot metadata to delete") 
            @PathVariable("spotMetadataId") String id, 
            @ApiParam(required=false, value="id of the array dataset (to check for permissions)") 
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
    
    @ApiOperation(value = "Retrieve slide with the given id", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/getprintedslide/{slideId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Printed Slide retrieved successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to retrieve"),
            @ApiResponse(code=404, message="Printed slide with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public PrintedSlide getPrintedSlide (
            @ApiParam(required=true, value="id of the printed slide to retrieve") 
            @PathVariable("slideId") String id, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable slides") 
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
    
    @ApiOperation(value = "Retrieve dataset with the given id", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/getarraydataset/{datasetid}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Dataset retrieved successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to retrieve the dataset"),
            @ApiResponse(code=404, message="Dataset with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public ArrayDataset getArrayDataset (
            @ApiParam(required=true, value="id of the array dataset to retrieve") 
            @PathVariable("datasetid") String id, 
            @ApiParam(required=false, value="load rawdata and processed data measurements or not, default= true to load all the details") 
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
            
            return dataset;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Array dataset with id " + id + " cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @ApiOperation(value = "Retrieve processed data with the given id", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/getprocesseddata/{id}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Processed data retrieved successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to retrieve the dataset"),
            @ApiResponse(code=404, message="Processed data with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public ProcessedData getProcessedData (
            @ApiParam(required=true, value="id of the processed data to retrieve") 
            @PathVariable("id") String id, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the processed data") 
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
    
    @ApiOperation(value = "Retrieve sample with the given id", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/getsample/{sampleId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Sample retrieved successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to retrieve"),
            @ApiResponse(code=404, message="Sample with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Sample getSample (
            @ApiParam(required=true, value="id of the sample to retrieve") 
            @PathVariable("sampleId") String id, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable metadata") 
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
                throw new EntityNotFoundException("Sample with id : " + id + " does not exist in the repository");
            }
            return sample;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Sample cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @ApiOperation(value = "Retrieve printer with the given id", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/getPrinter/{printerId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Printer retrieved successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to retrieve"),
            @ApiResponse(code=404, message="Printer with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Printer getPrinter (
            @ApiParam(required=true, value="id of the printer to retrieve") 
            @PathVariable("printerId") String id, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable metadata") 
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
                throw new EntityNotFoundException("Printer with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Printer cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    
    @ApiOperation(value = "Retrieve print run with the given id", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/getPrintRun/{printRunId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Printrun retrieved successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to retrieve"),
            @ApiResponse(code=404, message="Printrun with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public PrintRun getPrintRun (
            @ApiParam(required=true, value="id of the print run to retrieve") 
            @PathVariable("printRunId") String id, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable metadata") 
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
                throw new EntityNotFoundException("Printrun with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Printer cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @ApiOperation(value = "Retrieve scanner with the given id", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/getScanner/{scannerId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Scanner retrieved successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to retrieve"),
            @ApiResponse(code=404, message="ScannerMetadata with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public ScannerMetadata getScanner (
            @ApiParam(required=true, value="id of the ScannerMetadata to retrieve") 
            @PathVariable("scannerId") String id, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable metadata") 
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
                throw new EntityNotFoundException("ScannerMetadata with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("ScannerMetadata cannot be retrieved for user " + p.getName(), e);
        }   
    }
    @ApiOperation(value = "Retrieve SlideMetadata with the given id", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/getSlideMetadata/{slideId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="SlideMetadata retrieved successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to retrieve"),
            @ApiResponse(code=404, message="Printer with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public SlideMetadata getSlideMetadata (
            @ApiParam(required=true, value="id of the SlideMetadata to retrieve") 
            @PathVariable("slideId") String id, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable metadata") 
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
                throw new EntityNotFoundException("SlideMetadata with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("SlideMetadata cannot be retrieved for user " + p.getName(), e);
        }   
    }
    @ApiOperation(value = "Retrieve ImageAnalysisSoftware with the given id", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/getImageAnalysisSoftware/{imagesoftwareId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="ImageAnalysisSoftware retrieved successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to retrieve"),
            @ApiResponse(code=404, message="ImageAnalysisSoftware with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public ImageAnalysisSoftware getImageAnaylsisSoftware (
            @ApiParam(required=true, value="id of the ImageAnalysisSoftware to retrieve") 
            @PathVariable("imagesoftwareId") String id, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable metadata") 
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
                throw new EntityNotFoundException("ImageAnalysisSoftware with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("ImageAnalysisSoftware cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @ApiOperation(value = "Retrieve DataProcessingSoftware with the given id", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/getDataProcessingSoftware/{dataprocessingId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="DataProcessingSoftware retrieved successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to retrieve"),
            @ApiResponse(code=404, message="DataProcessingSoftware with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public DataProcessingSoftware getDataProcessingSoftware (
            @ApiParam(required=true, value="id of the DataProcessingSoftware to retrieve") 
            @PathVariable("dataprocessingId") String id, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable metadata") 
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
                throw new EntityNotFoundException("DataProcessingSoftware with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("DataProcessingSoftware cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @ApiOperation(value = "Retrieve assay metadata with the given id", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/getAssayMetadata/{assayId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Assay metadata retrieved successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to retrieve"),
            @ApiResponse(code=404, message="Assay metadata with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public AssayMetadata getAssayMetadata (
            @ApiParam(required=true, value="id of the Assay metadata to retrieve") 
            @PathVariable("assayId") String id, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable metadata") 
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
                throw new EntityNotFoundException("Assay metadata with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Assay metadata cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @ApiOperation(value = "Retrieve spot metadata with the given id", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/getSpotMetadata/{spotMetadataId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Assay metadata retrieved successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to retrieve"),
            @ApiResponse(code=404, message="Spot metadata with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public SpotMetadata getSpotMetadata (
            @ApiParam(required=true, value="id of the Spot metadata to retrieve") 
            @PathVariable("spotMetadataId") String id, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable metadata") 
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
                throw new EntityNotFoundException("Spot metadata with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Spot metadata cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @ApiOperation(value = "Update given printed slide for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/updatePrintedSlide", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Printed slide updated successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to update slides"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation updatePrintedSlide(
            @ApiParam(required=true, value="Printed slide with updated fields, only name and description can be changed") 
            @RequestBody PrintedSlide printedSlide, 
            @ApiParam(required=false, value="id of the array dataset that uses this printed slide") 
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
    
    @ApiOperation(value = "Update given array dataset for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/updatearraydataset", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Array dataset updated successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to update datasets"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation updateDataset(
            @ApiParam(required=true, value="Array dataset with updated fields. You can change name, description, keywords, publications, grants, coowners, collaborators and files"
                    + ". Whatever is included in these fields would be reflected in the repository. Therefore, if you don't want the existing keywords to be deleted, for example,"
                    + " you need to have the existing ones listed as part of the object") 
            @RequestBody ArrayDataset dataset, 
            @ApiParam(required=false, value="summary of the changes") 
            @RequestParam(value="changeSummary", required=false)
            String changeSummary,
            @ApiParam(required=false, value="field that has changed, can provide multiple") 
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
    
    @ApiOperation(value = "Update given array dataset for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/updatedatasetnamedescription", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Array dataset updated successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to update datasets"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation updateDatasetGeneralDetails(
            @ApiParam(required=true, value="Array dataset with updated fields. You can change name and description. If there is no description provided, it would be removed in the repository as well.") 
            @RequestBody ArrayDataset dataset, 
            @ApiParam(required=false, value="summary of the changes") 
            @RequestParam(value="changeSummary", required=false)
            String changeSummary,
            @ApiParam(required=false, value="field that has changed, can provide multiple") 
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
    
    @ApiOperation(value = "Update given sample for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/updateSample", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Sample updated successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to update samples"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation updateSample(
            @ApiParam(required=true, value="Sample with updated fields") 
            @RequestBody Sample metadata, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) throws SQLException {
        return updateMetadata(metadata, datasetId, MetadataTemplateType.SAMPLE, p);
        
    }
    
    @ApiOperation(value = "Update given printer for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/updatePrinter", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Printer updated successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to update printers"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation updatePrinter(
            @ApiParam(required=true, value="Printer with updated fields") 
            @RequestBody Printer metadata, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam("arraydatasetId")
            String datasetId,
            Principal p) throws SQLException {
        return updateMetadata(metadata, datasetId, MetadataTemplateType.PRINTER, p);
        
    }
    
    @ApiOperation(value = "Update given printrun for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/updatePrintrun", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Printrun updated successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to update printruns"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation updatePrintrun(
            @ApiParam(required=true, value="Printer with updated fields") 
            @RequestBody Printer metadata, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam("arraydatasetId")
            String datasetId,
            Principal p) throws SQLException {
        return updateMetadata(metadata, datasetId, MetadataTemplateType.PRINTRUN, p);
        
    }
    
    @ApiOperation(value = "Update given scanner metadata for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/updateScanner", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Scanner metadata updated successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to update scanners"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation updateScanner(
            @ApiParam(required=true, value="Scanner with updated fields") 
            @RequestBody ScannerMetadata metadata, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) throws SQLException {
        return updateMetadata(metadata, datasetId, MetadataTemplateType.SCANNER, p);
        
    }
    
    @ApiOperation(value = "Update given slide metadata for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/updateSlideMetadata", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Slide Metadata updated successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to update slide metadata"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation updateSlideMetadata(
            @ApiParam(required=true, value="Slide metadata with updated fields") 
            @RequestBody SlideMetadata metadata, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) throws SQLException {
        return updateMetadata(metadata, datasetId, MetadataTemplateType.SLIDE, p);
        
    }
    
    @ApiOperation(value = "Update given image analysis software for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/updateImageAnalysisSoftware", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Image analysis software updated successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to update image analysis software"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation updateImageAnalysisSoftware(
            @ApiParam(required=true, value="Image analysis software with updated fields") 
            @RequestBody ImageAnalysisSoftware metadata, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) throws SQLException {
        return updateMetadata(metadata, datasetId, MetadataTemplateType.IMAGEANALYSISSOFTWARE, p);
        
    }
    
    @ApiOperation(value = "Update given data processing software for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/updateDataProcessingSoftware", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="DataProcessingSoftware updated successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to update data processing software"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation updateDataProcessingSoftware(
            @ApiParam(required=true, value="Data processing software with updated fields") 
            @RequestBody DataProcessingSoftware metadata, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) throws SQLException {
        return updateMetadata(metadata, datasetId, MetadataTemplateType.DATAPROCESSINGSOFTWARE, p);
        
    }
    
    @ApiOperation(value = "Update given assay metadata for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/updateAssayMetadata", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="assay metadata updated successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to update assay metadata"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation updateAssayMetadata(
            @ApiParam(required=true, value="Assay metadata with updated fields") 
            @RequestBody AssayMetadata metadata, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable metadata") 
            @RequestParam(value="arraydatasetId", required=false)
            String datasetId,
            Principal p) throws SQLException {
        return updateMetadata(metadata, datasetId, MetadataTemplateType.ASSAY, p);
        
    }
    
    @ApiOperation(value = "Update given spot metadata for the user", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/updateSpotMetadata", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="assay metadata updated successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to update spot metadata"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation updateSpotMetadata(
            @ApiParam(required=true, value="Spot metadata with updated fields") 
            @RequestBody SpotMetadata metadata, 
            @ApiParam(required=false, value="id of the array dataset for which to retrive the applicable metadata") 
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
                logger.error("Error checking for mirage compliance", e);
            }
            metadataRepository.updateMetadataMirage(metadata, owner);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Error updating metadata with id: " + metadata.getId());
        }
        return new Confirmation("Metadata updated successfully", HttpStatus.OK.value());
        
    }
    
    @ApiOperation(value = "Make the given array dataset public", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/makearraydatasetpublic/{datasetid}", method = RequestMethod.POST)
    @ApiResponses (value = {
            @ApiResponse(code=200, message="id of the public array dataset"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to modify the dataset"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String makeArrayDatasetPublic (
            @ApiParam(required=true, value="id of the dataset to make public") 
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
                datasetURI.whenComplete((uri, e) -> {
                    try {
                        String existingURI = data.getUri();
                        if (e != null) {
                            task.setStatus(FutureTaskStatus.ERROR);
                            logger.error(e.getMessage(), e);
                            data.setStatus(FutureTaskStatus.ERROR);
                            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException && e.getCause().getCause() instanceof ErrorMessage) 
                                data.setError((ErrorMessage) e.getCause().getCause());
                            
                        } else {
                            task.setStatus(FutureTaskStatus.DONE);
                            data.setStatus(FutureTaskStatus.DONE);
                            data.setUri(uri);
                        }
                        datasetRepository.updateStatus (existingURI, data, o);
                        
                    } catch (SparqlException | SQLException e1) {
                        logger.error("Could not save the processedData", e1);
                    } 
                });
                datasetURI.get(2000, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                return null; // not ready yet
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
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return FutureTaskStatus.DONE;
    }

    @ApiOperation(value = "Download the given file", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/download", method = RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(code=200, message="File downloaded successfully"), 
            @ApiResponse(code=400, message="File not found, or not accessible publicly", response = ErrorMessage.class),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to download files of the dataset"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public ResponseEntity<Resource> downloadFile(
            @ApiParam(required=true, value="the folder of the file") 
            @RequestParam String fileFolder, 
            @ApiParam(required=true, value="the identifier of the file to be downloaded") 
            @RequestParam String fileIdentifier,
            @ApiParam(required=true, value="the original file name") 
            @RequestParam String originalName, Principal p) {
        
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
                    errorMessage.addError(new ObjectError("fileWrapper", "This file does not belong to this user. Cannot be downloaded!"));
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                }
            } 
        } catch (Exception e) {
            throw new GlycanRepositoryException("Array dataset cannot be loaded for user " + p.getName(), e);
        }
        File file = new File(fileFolder, fileIdentifier);
        if (!file.exists()) {
            errorMessage.addError(new ObjectError("fileWrapper", "NotFound"));
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
            return ResponseEntity.notFound().build();
            //throw new IllegalArgumentException ("File is not accessible", errorMessage);
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
    
    @ApiOperation(value = "Export processed data in glygen array data file format", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value = "/downloadProcessedData", method=RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(code=200, message="File generated successfully"), 
            @ApiResponse(code=400, message="Invalid request, file cannot be found"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to retrieve processed data"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public ResponseEntity<Resource> exportProcessedData (
            @ApiParam(required=true, value="id of the processed data") 
            @RequestParam("processeddataid")
            String processedDataId,
            @ApiParam(required=false, value="the name for downloaded file") 
            @RequestParam("filename")
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
}
