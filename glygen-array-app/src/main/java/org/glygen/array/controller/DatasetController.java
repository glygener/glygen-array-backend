package org.glygen.array.controller;

import java.io.File;
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
import org.glygen.array.exception.GlycanExistsException;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.SettingEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.SettingsRepository;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.data.ArrayDataset;
import org.glygen.array.persistence.rdf.data.FileWrapper;
import org.glygen.array.persistence.rdf.data.FutureTaskStatus;
import org.glygen.array.persistence.rdf.data.Intensity;
import org.glygen.array.persistence.rdf.data.Measurement;
import org.glygen.array.persistence.rdf.data.PrintedSlide;
import org.glygen.array.persistence.rdf.data.ProcessedData;
import org.glygen.array.persistence.rdf.data.RawData;
import org.glygen.array.persistence.rdf.data.StatisticalMethod;
import org.glygen.array.persistence.rdf.metadata.AssayMetadata;
import org.glygen.array.persistence.rdf.metadata.DataProcessingSoftware;
import org.glygen.array.persistence.rdf.metadata.Description;
import org.glygen.array.persistence.rdf.metadata.Descriptor;
import org.glygen.array.persistence.rdf.metadata.DescriptorGroup;
import org.glygen.array.persistence.rdf.metadata.ImageAnalysisSoftware;
import org.glygen.array.persistence.rdf.metadata.MetadataCategory;
import org.glygen.array.persistence.rdf.metadata.Printer;
import org.glygen.array.persistence.rdf.metadata.Sample;
import org.glygen.array.persistence.rdf.metadata.ScannerMetadata;
import org.glygen.array.persistence.rdf.metadata.SlideMetadata;
import org.glygen.array.persistence.rdf.template.DescriptionTemplate;
import org.glygen.array.persistence.rdf.template.DescriptorGroupTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplateType;
import org.glygen.array.service.ArrayDatasetRepository;
import org.glygen.array.service.ArrayDatasetRepositoryImpl;
import org.glygen.array.service.AsyncService;
import org.glygen.array.service.FeatureRepository;
import org.glygen.array.service.GlycanRepository;
import org.glygen.array.service.GlygenArrayRepository;
import org.glygen.array.service.GlygenArrayRepositoryImpl;
import org.glygen.array.service.LayoutRepository;
import org.glygen.array.service.LinkerRepository;
import org.glygen.array.service.MetadataTemplateRepository;
import org.glygen.array.util.parser.RawdataParser;
import org.glygen.array.view.ArrayDatasetListView;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.glygen.array.view.MetadataListResultView;
import org.glygen.array.view.PrintedSlideListView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

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
    UserRepository userRepository;
    
    @Value("${spring.file.uploaddirectory}")
    String uploadDir;
    
    @Autowired
    ResourceLoader resourceLoader;
    
    @Autowired
    Validator validator;
    
    @Autowired
    AsyncService parserAsyncService;
    
    @ApiOperation(value = "Add given array dataset  for the user")
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
                    Sample existing = datasetRepository.getSampleFromURI(uri, user);
                    if (existing == null) {
                        errorMessage.addError(new ObjectError("sample", "NotFound"));
                    } else {
                        dataset.setSample(existing);
                    }
                } else if (dataset.getSample().getName() != null) {
                    Sample existing = datasetRepository.getSampleByLabel(dataset.getSample().getName(), user);
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
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Array dataset cannot be added for user " + p.getName(), e);
        }
        
    } 
    
    @ApiOperation(value = "Add given  rawdata set for the user")
    @RequestMapping(value="/addRawData", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added rawdata set"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to register datasets"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addRawData (
            @ApiParam(required=true, value="Raw data set to be added. RawData should have a slide, "
                    + " and slide should have an existing printedSlide (specified by name or uri or id), "
                    + "it should have an Image (specified with filename (already uploaded) and an existing Scanner metadata (by name, uri or id)), "
                    + "it should have a filename (already uploaded), file format and a power level, "
                    + "and it should have an existing ImageAnalysisSoftwareMetadata (specified by name, id or uri")
            @RequestBody RawData rawData, 
            @ApiParam(required=true, value="id of the array dataset (must already be in the repository) to add the raw data") 
            @RequestParam("arraydatasetId")
            String datasetId,  
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        // check if the dataset with the given id exists
        try {
            ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, false, user);
            if (dataset == null) {
                errorMessage.addError(new ObjectError("dataset", "NotFound"));
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
        }
         
        if (rawData.getSlide() == null || rawData.getSlide().getPrintedSlide() == null) {
            errorMessage.addError(new ObjectError("slide", "NoEmpty"));
        } else {
            if (rawData.getImage() != null) {
                rawData.getSlide().setImage(rawData.getImage());
            }
            try {
                String printedSlideId = rawData.getSlide().getPrintedSlide().getId();
                if (printedSlideId == null) {
                    if (rawData.getSlide().getPrintedSlide().getUri() != null) {
                        printedSlideId = rawData.getSlide().getPrintedSlide().getUri().substring(
                                rawData.getSlide().getPrintedSlide().getUri().lastIndexOf("/") + 1);
                    }
                }
                if (printedSlideId != null) {
                    PrintedSlide existing = datasetRepository.getPrintedSlideFromURI(GlygenArrayRepositoryImpl.uriPrefix + printedSlideId, user);
                    if (existing == null) {
                        errorMessage.addError(new ObjectError("printedSlide", "NotFound"));
                    } else {
                        rawData.getSlide().setPrintedSlide(existing);
                    }
                } else if (rawData.getSlide().getPrintedSlide().getName() != null) {
                    PrintedSlide existing = datasetRepository.getPrintedSlideByLabel(rawData.getSlide().getPrintedSlide().getName(), user);
                    if (existing == null) {
                        errorMessage.addError(new ObjectError("printedSlide", "NotFound"));
                    } else {
                        rawData.getSlide().setPrintedSlide(existing);
                    }
                }
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the printed slide", e);
            }
        }
        
        // check to make sure, the file is specified and exists in the uploads folder
        if (rawData.getFile() == null || rawData.getFile().getIdentifier() == null) {
            errorMessage.addError(new ObjectError("filename", "NotFound"));
        } else {
            String fileFolder = uploadDir;
            if (rawData.getFile().getFileFolder() != null)
                fileFolder = rawData.getFile().getFileFolder();
            File file = new File (fileFolder, rawData.getFile().getIdentifier());
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
                if(file.renameTo (newFile)) { 
                         // if file copied successfully then delete the original file 
                    file.delete(); 
                    
                } else { 
                    throw new GlycanRepositoryException("File cannot be moved to the dataset folder");
                } 
                rawData.getFile().setFileFolder(uploadDir + File.separator + datasetId);
                
                // check to make sure the image is specified and image file is in uploads folder
                if (rawData.getImage() != null && rawData.getImage().getFile() != null && rawData.getImage().getFile().getIdentifier() != null) {
                    // move it to the dataset folder
                    File imageFile = new File (uploadDir, rawData.getImage().getFile().getIdentifier());
                    if (!imageFile.exists()) {
                        errorMessage.addError(new ObjectError("imageFile", "NotFound"));
                    }
                    else {
                        if(imageFile.renameTo 
                                (new File(experimentFolder + File.separator + rawData.getImage().getFile().getIdentifier()))) { 
                                 // if file copied successfully then delete the original file 
                            imageFile.delete(); 
                            
                        } else { 
                            throw new GlycanRepositoryException("Image file cannot be moved to the dataset folder");
                        } 
                    }
                    rawData.getImage().getFile().setFileFolder(uploadDir + File.separator + datasetId);
                }
                //else {   // image is not mandatory!!!
                //    errorMessage.addError(new ObjectError("image", "NoEmpty"));
                //}
            }
        }
        
        if (rawData.getMetadata() == null) {
            errorMessage.addError(new ObjectError("imageAnalysisMetadata", "NoEmpty"));
        } else {
            try {
                if (rawData.getMetadata().getName() != null) {
                    ImageAnalysisSoftware metadata = datasetRepository.getImageAnalysisSoftwarByLabel(rawData.getMetadata().getName(), user);
                    if (metadata == null) {
                        errorMessage.addError(new ObjectError("imageAnalysisMetadata", "NotFound"));
                    } else {
                        rawData.setMetadata(metadata);
                    }
                } else if (rawData.getMetadata().getUri() != null) {
                    ImageAnalysisSoftware metadata = datasetRepository.getImageAnalysisSoftwareFromURI(rawData.getMetadata().getUri(), user);
                    if (metadata == null) {
                        errorMessage.addError(new ObjectError("imageAnalysisMetadata", "NotFound"));
                    } else {
                        rawData.setMetadata(metadata);
                    }
                } else if (rawData.getMetadata().getId() != null) {
                    ImageAnalysisSoftware metadata = 
                            datasetRepository.getImageAnalysisSoftwareFromURI(ArrayDatasetRepositoryImpl.uriPrefix + rawData.getMetadata().getId(), user);
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
        
        if (rawData.getImage() != null && rawData.getImage().getScanner() == null) {
            errorMessage.addError(new ObjectError("scannerMetadata", "NoEmpty"));
        } else if (rawData.getImage() != null) {
            try {
                if (rawData.getImage().getScanner().getName() != null) {
                    ScannerMetadata metadata = datasetRepository.getScannerMetadataByLabel(rawData.getImage().getScanner().getName(), user);
                    if (metadata == null) {
                        errorMessage.addError(new ObjectError("scannerMetadata", "NotFound"));
                    } else {
                        rawData.getImage().setScanner(metadata);
                    }
                } else if (rawData.getImage().getScanner().getUri() != null) {
                    ScannerMetadata metadata = datasetRepository.getScannerMetadataFromURI(rawData.getImage().getScanner().getUri(), user);
                    if (metadata == null) {
                        errorMessage.addError(new ObjectError("scannerMetadata", "NotFound"));
                    } else {
                        rawData.getImage().setScanner(metadata);
                    }
                } else if (rawData.getImage().getScanner().getId() != null) {
                    ScannerMetadata metadata = 
                            datasetRepository.getScannerMetadataFromURI(ArrayDatasetRepositoryImpl.uriPrefix + rawData.getImage().getScanner().getId(), user);
                    if (metadata == null) {
                        errorMessage.addError(new ObjectError("scannerMetadata", "NotFound"));
                    } else {
                        rawData.getImage().setScanner(metadata);
                    }
                }
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the image analysis metadata", e);
            }
        }
        
        if (rawData.getSlide() != null && rawData.getSlide().getMetadata() == null) {
            errorMessage.addError(new ObjectError("assayMetadata", "NoEmpty"));
        } else if (rawData.getSlide() != null) {
            try {
                if (rawData.getSlide().getMetadata().getName() != null) {
                    AssayMetadata metadata = datasetRepository.getAssayMetadataByLabel(rawData.getSlide().getMetadata().getName(), user);
                    if (metadata == null) {
                        errorMessage.addError(new ObjectError("assayMetadata", "NotFound"));
                    } else {
                        rawData.getSlide().setMetadata(metadata);
                    }
                } else if (rawData.getImage().getScanner().getUri() != null) {
                    AssayMetadata metadata = datasetRepository.getAssayMetadataFromURI(rawData.getSlide().getMetadata().getUri(), user);
                    if (metadata == null) {
                        errorMessage.addError(new ObjectError("assayMetadata", "NotFound"));
                    } else {
                        rawData.getSlide().setMetadata(metadata);
                    }
                } else if (rawData.getImage().getScanner().getId() != null) {
                    AssayMetadata metadata = 
                            datasetRepository.getAssayMetadataFromURI(ArrayDatasetRepositoryImpl.uriPrefix + rawData.getSlide().getMetadata().getId(), user);
                    if (metadata == null) {
                        errorMessage.addError(new ObjectError("assayMetadata", "NotFound"));
                    } else {
                        rawData.getSlide().setMetadata(metadata);
                    }
                }
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the assay metadata", e);
            }
        }
         
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid raw data information", errorMessage);
        
        try {
            // parse the file and extract measurements
            try {
                if (rawData.getSlide() != null && rawData.getSlide().getPrintedSlide() != null 
                        && rawData.getSlide().getPrintedSlide().getLayout() != null) {
                    // need to load the full layout before parsing
                    SlideLayout fullLayout = layoutRepository.getSlideLayoutById(rawData.getSlide().getPrintedSlide().getLayout().getId(), user);
                    // parse the file
                    Map<Measurement, Spot> dataMap = RawdataParser.parse(rawData.getFile(), fullLayout, rawData.getPowerLevel());
                    // check blocks used and extract only those measurements
                    if (rawData.getSlide().getBlocksUsed() != null && !rawData.getSlide().getBlocksUsed().isEmpty()) {
                        Map<Measurement, Spot> filteredMap = new HashMap<Measurement, Spot>();
                        for (Map.Entry<Measurement, Spot> entry: dataMap.entrySet()) {
                            for (String blockId: rawData.getSlide().getBlocksUsed()) { 
                                if (entry.getValue().getBlockId().equals(blockId)) {
                                    filteredMap.put(entry.getKey(), entry.getValue());
                                    break;
                                }
                            }
                        }
                        rawData.setDataMap(filteredMap); 
                    } else {
                        rawData.setDataMap(dataMap);
                    }                      
                } else {
                    errorMessage.addError(new ObjectError("slideLayout", "NoEmpty"));
                    throw new IllegalArgumentException("Invalid Input: slide layout should be there", errorMessage);
                }
            } catch (Exception e) {
                errorMessage.addError(new ObjectError("file", "NotValid"));
                errorMessage.addError(new ObjectError("parseError", e.getMessage()));
                throw new IllegalArgumentException("Invalid Input: Not a valid raw data file", errorMessage);
            }
            String uri = datasetRepository.addRawData(rawData, datasetId, user);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Rawdata cannot be added for user " + p.getName(), e);
        }
    }
    
    @ApiOperation(value = "Add given printed slide set for the user")
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
            PrintedSlide existing = datasetRepository.getPrintedSlideByLabel(slide.getName().trim(), user);
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
                    }
                } else if (slide.getLayout().getName() != null) {
                    SlideLayout existing = layoutRepository.getSlideLayoutByName(slide.getLayout().getName(), user);
                    if (existing == null) {
                        errorMessage.addError(new ObjectError("slidelayout", "NotFound"));
                    } else {
                        slide.setLayout(existing);
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
                    SlideMetadata slideMetadata = datasetRepository.getSlideMetadataByLabel(slide.getMetadata().getName(), user);
                    if (slideMetadata == null) {
                        errorMessage.addError(new ObjectError("slideMetadata", "NotFound"));
                    } else {
                        slide.setMetadata(slideMetadata);
                    }
                } else if (slide.getMetadata().getUri() != null) {
                    SlideMetadata slideMetadata = datasetRepository.getSlideMetadataFromURI(slide.getMetadata().getUri(), user);
                    if (slideMetadata == null) {
                        errorMessage.addError(new ObjectError("slideMetadata", "NotFound"));
                    } else {
                        slide.setMetadata(slideMetadata);
                    }
                } else if (slide.getMetadata().getId() != null) {
                    SlideMetadata slideMetadata = datasetRepository.getSlideMetadataFromURI(ArrayDatasetRepositoryImpl.uriPrefix + slide.getMetadata().getId(), user);
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
                    Printer printer = datasetRepository.getPrinterByLabel(slide.getPrinter().getName(), user);
                    if (printer == null) {
                        errorMessage.addError(new ObjectError("printer", "NotFound"));
                    } else {
                        slide.setPrinter(printer);
                    }
                } else if (slide.getPrinter().getUri() != null) {
                    Printer printer = datasetRepository.getPrinterFromURI(slide.getPrinter().getUri(), user);
                    if (printer == null) {
                        errorMessage.addError(new ObjectError("printer", "NotFound"));
                    } else {
                        slide.setPrinter(printer);
                    }
                } else if (slide.getMetadata().getId() != null) {
                    Printer printer = datasetRepository.getPrinterFromURI(ArrayDatasetRepositoryImpl.uriPrefix + slide.getPrinter().getId(), user);
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
    
    @ApiOperation(value = "Add given data processing software for the user")
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
            @RequestParam(name="validate", required=false, defaultValue="true")
            Boolean validate,
            Principal p) {
        
        if (validate == null)
            validate = true;
        
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
        
        if (metadata.getName() == null || metadata.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        if (metadata.getTemplate() == null || metadata.getTemplate().isEmpty()) {
            errorMessage.addError(new ObjectError("type", "NoEmpty"));
        }
        
        // check for duplicate name
        try {
            MetadataCategory existing = datasetRepository.getMetadataByLabel(metadata.getName().trim(), ArrayDatasetRepositoryImpl.dataProcessingTypePredicate, user);
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
                MetadataTemplate template = templateRepository.getTemplateFromURI(templateURI);
                if (validate != null && validate) {
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
            String uri = datasetRepository.addDataProcessingSoftware(metadata, user);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Data processing software metadata cannot be added for user " + p.getName(), e);
        }
        
    }
    
    @ApiOperation(value = "Add given image analysis software for the user")
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
            @RequestParam(name="validate", required=false, defaultValue="true")
            Boolean validate,
            Principal p) {
        
        if (validate == null)
            validate = true;
        
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
        
        if (metadata.getName() == null || metadata.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        if (metadata.getTemplate() == null || metadata.getTemplate().isEmpty()) {
            errorMessage.addError(new ObjectError("type", "NoEmpty"));
        }
        
        // check for duplicate name
        try {
            MetadataCategory existing = datasetRepository.getMetadataByLabel(metadata.getName().trim(), ArrayDatasetRepositoryImpl.imageAnalysisTypePredicate, user);
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
                MetadataTemplate template = templateRepository.getTemplateFromURI(templateURI);
                if (validate != null && validate) {
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
            String uri = datasetRepository.addImageAnalysisSoftware(metadata, user);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Image Analysis metadata cannot be added for user " + p.getName(), e);
        }
        
    }
    
    @ApiOperation(value = "Add given printer metadata for the user")
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
            @RequestParam(name="validate", required=false, defaultValue="true")
            Boolean validate,
            Principal p) {
        
        if (validate == null)
            validate = true;
        
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
        
        if (printer.getName() == null || printer.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        if (printer.getTemplate() == null || printer.getTemplate().isEmpty()) {
            errorMessage.addError(new ObjectError("type", "NoEmpty"));
        }
        
        // check for duplicate name
        try {
            MetadataCategory metadata = datasetRepository.getMetadataByLabel(printer.getName().trim(), ArrayDatasetRepositoryImpl.printerTypePredicate, user);
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
                MetadataTemplate template = templateRepository.getTemplateFromURI(templateURI);
                if (validate != null && validate) {
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
            String uri = datasetRepository.addPrinter(printer, user);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Printer cannot be added for user " + p.getName(), e);
        }
        
    }
    
    @ApiOperation(value = "Add given assay metadata for the user")
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
            @RequestParam(name="validate", required=false, defaultValue="true")
            Boolean validate,
            Principal p) {
        
        if (validate == null)
            validate = true;
        
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
        
        if (metadata.getName() == null || metadata.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        if (metadata.getTemplate() == null || metadata.getTemplate().isEmpty()) {
            errorMessage.addError(new ObjectError("type", "NoEmpty"));
        }
        
        // check for duplicate name
        try {
            MetadataCategory existing = datasetRepository.getMetadataByLabel(metadata.getName().trim(), ArrayDatasetRepositoryImpl.assayTypePredicate, user);
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
                MetadataTemplate template = templateRepository.getTemplateFromURI(templateURI);
                if (validate != null && validate) {
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
            String uri = datasetRepository.addAssayMetadata(metadata, user);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Assay metadata cannot be added for user " + p.getName(), e);
        }
        
    }
    
    
    @ApiOperation(value = "Import processed data results from uploaded excel file")
    @RequestMapping(value = "/addProcessedDataFromExcel", method=RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added processed data for the given array dataset"), 
            @ApiResponse(code=400, message="Invalid request, file cannot be found"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to add array datasets"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addProcessedDataFromExcel (
            @ApiParam(required=true, value="id of the array dataset (must already be in the repository) to add the processed data") 
            @RequestParam("arraydatasetId")
            String datasetId,        
            @ApiParam(required=true, value="processed data file details such as name, original name, folder, format") 
            @RequestBody
            FileWrapper file,
            @ApiParam(required=true, value="Data processing software metadata id (must already be in the repository)") 
            @RequestParam("metadataId")
            String metadataId,
            @ApiParam(required=true, value="the statistical method used (eg. eliminate, average etc.") 
            @RequestParam("methodName")
            String methodName,
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
                  
        // check if metadata exists!
        DataProcessingSoftware metadata = null;
        ArrayDataset dataset;
        try {
            // check if the dataset with the given id exists
            dataset = datasetRepository.getArrayDataset(datasetId, false, user);
            if (dataset == null) {
                errorMessage.addError(new ObjectError("dataset", "NotFound"));
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve dataset from the repository", e);
        }
            
        if (metadataId != null) {    
            try {
                metadata = datasetRepository.getDataProcessingSoftwareFromURI(GlygenArrayRepositoryImpl.uriPrefix + metadataId, user);
                if (metadata == null) {
                    errorMessage.addError(new ObjectError("metadata", "NotFound"));
                }
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Cannot retrieve data processing software metadata", e);
            }
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
            CompletableFuture<List<Intensity>> intensities = null;
            try {
                intensities = parserAsyncService.parseProcessDataFile(datasetId, file, user);
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
                            datasetRepository.addIntensitiesToProcessedData(processedData, user);
                            processedData.setStatus(FutureTaskStatus.DONE);
                        }
                    
                        datasetRepository.updateStatus (uri, processedData, user);
                    } catch (SparqlException | SQLException e1) {
                        logger.error("Could not save the processedData", e1);
                    } 
                });
                processedData.setIntensity(intensities.get(20000, TimeUnit.MILLISECONDS));
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (TimeoutException e) {
                synchronized (this) {
                    // save whatever we have for now for processed data and update its status to "processing"
                    String uri = datasetRepository.addProcessedData(processedData, datasetId, user);  
                    processedData.setUri(uri);
                    String id = uri.substring(uri.lastIndexOf("/")+1);
                    if (processedData.getError() == null)
                        processedData.setStatus(FutureTaskStatus.PROCESSING);
                    else 
                        processedData.setStatus(FutureTaskStatus.ERROR);
                    datasetRepository.updateStatus (uri, processedData, user);
                    return id;
                }
            }
            
            //TODO do we ever come to this ??
            if (intensities != null && intensities.isDone()) {
                file.setFileFolder(uploadDir + File.separator + datasetId);
                processedData.setFile(file);
                processedData.setIntensity(intensities.get());
                String uri = datasetRepository.addProcessedData(processedData, datasetId, user);   
                String id = uri.substring(uri.lastIndexOf("/")+1);
                if (processedData.getError() == null)
                    processedData.setStatus(FutureTaskStatus.DONE);
                else 
                    processedData.setStatus(FutureTaskStatus.ERROR);
                datasetRepository.updateStatus (uri, processedData, user);
                return id;
            } else {
                String uri = datasetRepository.addProcessedData(processedData, datasetId, user);  
                processedData.setUri(uri);
                String id = uri.substring(uri.lastIndexOf("/")+1);
                processedData.setStatus(FutureTaskStatus.PROCESSING);
                datasetRepository.updateStatus (uri, processedData, user);
                return id;
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new GlycanRepositoryException("Cannot add the intensities to the repository", e);
        }
    }
    
    @ApiOperation(value = "Update processed data with results from uploaded excel file")
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
            @ApiParam(required=true, value="processed data with an existing id/uri. If file is provided, the new file information is used. "
                    + "If not, existing file is used for processing") 
            @RequestBody
            ProcessedData processedData,
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        
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
        
        try {
            ProcessedData existing = datasetRepository.getProcessedDataFromURI(uri, true, user);
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
                
            CompletableFuture<List<Intensity>> intensities = null;
            try {
                intensities = parserAsyncService.parseProcessDataFile(datasetId, file, user);
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
                            file.setFileFolder(uploadDir + File.separator + datasetId);
                            processedData.setFile(file);
                            datasetRepository.addIntensitiesToProcessedData(processedData, user);
                            processedData.setStatus(FutureTaskStatus.DONE);
                        }
                    
                        datasetRepository.updateStatus (processedURI, processedData, user);
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
                    datasetRepository.updateStatus (uri, processedData, user);
                    return id;
                }
            }
                
            return processedData.getId();
        } catch (Exception e) {
            throw new GlycanRepositoryException("Cannot retrieve processed data from the repository", e);
        }
        
    }
    
    
    @ApiOperation(value = "Add given sample metadata for the user")
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
            @RequestParam(name="validate", required=false, defaultValue="true")
            Boolean validate, Principal p) {   
        
        if (validate == null)
            validate = true;
        
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
        
        if (sample.getName() == null || sample.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        if (sample.getTemplate() == null || sample.getTemplate().isEmpty()) {
            errorMessage.addError(new ObjectError("type", "NoEmpty"));
        }
        
        // check for duplicate name
        try {
            MetadataCategory existing = datasetRepository.getMetadataByLabel(sample.getName().trim(), ArrayDatasetRepositoryImpl.sampleTypePredicate, user);
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
                MetadataTemplate template = templateRepository.getTemplateFromURI(templateURI);
                if (validate != null && validate) {
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
            String uri = datasetRepository.addSample(sample, user);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Sample cannot be added for user " + p.getName(), e);
        }
        
    }
    
    @ApiOperation(value = "Add given scanner metadata for the user")
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
            @RequestParam(name="validate", required=false, defaultValue="true")
            Boolean validate,
            Principal p) {
        
        if (validate == null)
            validate = true;
        
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
        
        if (metadata.getName() == null || metadata.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        if (metadata.getTemplate() == null || metadata.getTemplate().isEmpty()) {
            errorMessage.addError(new ObjectError("type", "NoEmpty"));
        }
        
        // check for duplicate name
        try {
            MetadataCategory existing = datasetRepository.getMetadataByLabel(metadata.getName().trim(), ArrayDatasetRepositoryImpl.scannerTypePredicate, user);
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
                MetadataTemplate template = templateRepository.getTemplateFromURI(templateURI);
                if (validate != null && validate) {
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
            String uri = datasetRepository.addScannerMetadata(metadata, user);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Scanner cannot be added for user " + p.getName(), e);
        }
        
    }
    
    
    @ApiOperation(value = "Add given slide metadata for the user")
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
            @RequestParam(name="validate", required=false, defaultValue="true")
            Boolean validate,
            Principal p) {
        
        if (validate == null)
            validate = true;
        
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
        
        if (metadata.getName() == null || metadata.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        if (metadata.getTemplate() == null || metadata.getTemplate().isEmpty()) {
            errorMessage.addError(new ObjectError("type", "NoEmpty"));
        }
        
        // check for duplicate name
        try {
            MetadataCategory existing = datasetRepository.getMetadataByLabel(metadata.getName().trim(), ArrayDatasetRepositoryImpl.slideTemplateTypePredicate, user);
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
                MetadataTemplate template = templateRepository.getTemplateFromURI(templateURI);
                if (validate != null && validate) {
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
            String uri = datasetRepository.addSlideMetadata(metadata, user);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Slide metadata cannot be added for user " + p.getName(), e);
        }
        
    }
    
    @GetMapping("/availableMetadataname")
    @ApiOperation(value="Checks whether the given name is available to be used (returns true if available, false if already in use", response=Boolean.class)
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
        }
        MetadataCategory metadata = null;
        try {
            metadata = datasetRepository.getMetadataByLabel(name.trim(), typePredicate, user);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve metadata by name", e);
        }
        
        return metadata == null;
    }
    
    @GetMapping("/isMirageCompliant/{id}")
    @ApiOperation(value="Checks whether the given metadata contains all MIRAGE recommended descriptors", response=Boolean.class)
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
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        MetadataCategory metadata = null;
        switch (type) {
        case SAMPLE:
            metadata = getSample(metadataId, p);
            break;
        case DATAPROCESSINGSOFTWARE:
            metadata = getDataProcessingSoftware(metadataId, p);
            break;
        case IMAGEANALYSISSOFTWARE:
            metadata = getImageAnaylsisSoftware(metadataId, p);
            break;
        case PRINTER:
            metadata = getPrinter(metadataId, p);
            break;
        case SCANNER:
            metadata = getScanner(metadataId, p);
            break;
        case SLIDE:
            metadata = getSlideMetadata(metadataId, p);
            break;
        case ASSAY:
            metadata = getAssayMetadata(metadataId, p);
        default:
            break;
        }
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        // check if the template exists
        String templateURI = null;
        try {
            if (metadata != null && metadata.getTemplate() != null && !metadata.getTemplate().isEmpty()) {
                templateURI = MetadataTemplateRepository.templatePrefix + metadata.getTemplate();
            }
            if (templateURI == null) {
                errorMessage.addError(new ObjectError("type", "NotValid"));
            }
            else {
                // validate mandatory/multiple etc.
                MetadataTemplate template = templateRepository.getTemplateFromURI(templateURI);
                if (template == null) {
                    errorMessage.addError(new ObjectError("type", "NotValid"));
                }
                ErrorMessage err = checkMirage (metadata, template);
                if (err != null) {
                    for (ObjectError error: err.getErrors())
                        errorMessage.addError(error);
                    metadata.setIsMirage(false);
                    // save it back to the repository
                    datasetRepository.updateMetadataMirage(metadata, user);
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
            datasetRepository.updateMetadataMirage(metadata, user);
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

    @ApiOperation(value = "List all datasets for the user")
    @RequestMapping(value="/listArrayDataset", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Array datasets retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public ArrayDatasetListView listArrayDataset (
            @ApiParam(required=true, value="offset for pagination, start from 0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
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
            
            int total = datasetRepository.getArrayDatasetCountByUser(user);
            
            List<ArrayDataset> resultList = datasetRepository.getArrayDatasetByUser(user, offset, limit, field, order, searchValue, loadAll);
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(resultList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve array datasets for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @ApiOperation(value = "List all printed slides for the user")
    @RequestMapping(value="/listPrintedSlide", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Printed slides retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public PrintedSlideListView listPrintedSlide (
            @ApiParam(required=true, value="offset for pagination, start from 0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, Principal p) {
        PrintedSlideListView result = new PrintedSlideListView();
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
            
            int total = datasetRepository.getPrintedSlideCountByUser(user);
            
            List<PrintedSlide> resultList = datasetRepository.getPrintedSlideByUser(user, offset, limit, field, order, searchValue);
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
            throw new GlycanRepositoryException("Cannot retrieve array datasets for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @ApiOperation(value = "List all data processing software metadata for the user")
    @RequestMapping(value="/listDataProcessingSoftware", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Data processing software metadata list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listDataProcessingSoftware (
            @ApiParam(required=true, value="offset for pagination, start from 0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, Principal p) {
        MetadataListResultView result = new MetadataListResultView();
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
            
            int total = datasetRepository.getDataProcessingSoftwareCountByUser(user);
            
            List<DataProcessingSoftware> metadataList = datasetRepository.getDataProcessingSoftwareByUser(user, offset, limit, field, order, searchValue);
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
    
    @ApiOperation(value = "List all image analysis software metadata for the user")
    @RequestMapping(value="/listImageAnalysisSoftware", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Image analysis software metadata list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listImageAnalysisSoftware (
            @ApiParam(required=true, value="offset for pagination, start from 0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, Principal p) {
        MetadataListResultView result = new MetadataListResultView();
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
            
            int total = datasetRepository.getImageAnalysisSoftwareCountByUser(user);
            
            List<ImageAnalysisSoftware> metadataList = datasetRepository.getImageAnalysisSoftwareByUser(user, offset, limit, field, order, searchValue);
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
    
    @ApiOperation(value = "List all printer metadata for the user")
    @RequestMapping(value="/listPrinters", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Printer list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listPrinters (
            @ApiParam(required=true, value="offset for pagination, start from 0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, Principal p) {
        MetadataListResultView result = new MetadataListResultView();
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
            
            int total = datasetRepository.getPrinterCountByUser(user);
            
            List<Printer> metadataList = datasetRepository.getPrinterByUser(user, offset, limit, field, order, searchValue);
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
    
    @ApiOperation(value = "List all samples for the user")
    @RequestMapping(value="/listSamples", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Samples retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listSamples (
            @ApiParam(required=true, value="offset for pagination, start from 0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, Principal p) {
        MetadataListResultView result = new MetadataListResultView();
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
            
            int total = datasetRepository.getSampleCountByUser (user);
            
            List<Sample> metadataList = datasetRepository.getSampleByUser(user, offset, limit, field, order, searchValue);
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
    
    @ApiOperation(value = "List all scanner metadata for the user")
    @RequestMapping(value="/listScanners", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Scanner list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listScanners (
            @ApiParam(required=true, value="offset for pagination, start from 0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, Principal p) {
        MetadataListResultView result = new MetadataListResultView();
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
            
            int total = datasetRepository.getScannerMetadataCountByUser(user);
            
            List<ScannerMetadata> metadataList = datasetRepository.getScannerMetadataByUser(user, offset, limit, field, order, searchValue);
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
    
    @ApiOperation(value = "List all slide metadata for the user")
    @RequestMapping(value="/listSlideMetadata", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Slide metadata list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listSlideMetadata (
            @ApiParam(required=true, value="offset for pagination, start from 0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, Principal p) {
        MetadataListResultView result = new MetadataListResultView();
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
            
            int total = datasetRepository.getSlideMetadataCountByUser (user);
            
            List<SlideMetadata> metadataList = datasetRepository.getSlideMetadataByUser(user, offset, limit, field, order, searchValue);
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
    
    @ApiOperation(value = "List all assay metadata for the user")
    @RequestMapping(value="/listAssayMetadata", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Assay metadata list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listAssayMetadata (
            @ApiParam(required=true, value="offset for pagination, start from 0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, Principal p) {
        MetadataListResultView result = new MetadataListResultView();
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
            
            int total = datasetRepository.getAssayMetadataCountByUser(user);
            
            List<AssayMetadata> metadataList = datasetRepository.getAssayMetadataByUser(user, offset, limit, field, order, searchValue);
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
    
    private ErrorMessage validateDescriptorGroup(DescriptorGroup descGroup, DescriptionTemplate descGroupTemplate) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        
        for (DescriptionTemplate descTemplate: ((DescriptorGroupTemplate)descGroupTemplate).getDescriptors()) {
            boolean exists = false;
            int count = 0;
            for (Description d: descGroup.getDescriptors()) {
                DescriptionTemplate t = d.getKey();
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
                        }
                    }
                }
            }

            if (descTemplate.isMandatory() && !exists) {
                // violation
                errorMessage.addError(new ObjectError (descTemplate.getName() + "-mandatory", "NotFound"));
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
        
        for (DescriptionTemplate descTemplate: template.getDescriptors()) {
            // validate mandatory and multiplicity
            boolean exists = false;
            boolean valueExists = false;
            int count = 0;
            if (!descTemplate.isGroup()) {
                for (Description d: metadata.getDescriptors()) {
                    DescriptionTemplate t = d.getKey();
                    if (t.getId() != null) {
                        if (t.getId().equals(descTemplate.getId())) {
                            exists = true;
                            count ++;
                            if (((Descriptor)d).getValue() != null && !((Descriptor)d).getValue().isEmpty()) {
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
                        }
                    }
                }
            } else {
                for (Description d: metadata.getDescriptorGroups()) {
                    DescriptionTemplate t = d.getKey();
                    if (t.getId() != null) {
                        if (t.getId().equals(descTemplate.getId())) {
                            exists = true;
                            count ++;
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
                // violation
                errorMessage.addError(new ObjectError (descTemplate.getName() + "-mandatory", "NotFound"));
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
    
    @ApiOperation(value = "Delete given printed slide from the user's list")
    @RequestMapping(value="/deleteprintedslide/{slideId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Slide deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete slides"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deletePrintedSlide (
            @ApiParam(required=true, value="id of the printed slide to delete") 
            @PathVariable("slideId") String id, Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            datasetRepository.deletePrintedSlide(id, user);
            return new Confirmation("Printed slide deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete printed slide " + id, e);
        } 
    }
    
    @ApiOperation(value = "Delete the given array dataset from the user's list")
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
            ArrayDataset dataset = getArrayDataset(id, false, principal);
            if (dataset != null && dataset.getRawDataList() != null) {
                for (RawData rawData: dataset.getRawDataList()) {
                    if (rawData.getFile() != null) {
                        File rawDataFile = new File (rawData.getFile().getFileFolder(), rawData.getFile().getIdentifier());
                        if (rawDataFile.exists()) {
                            rawDataFile.delete();
                        }
                    }
                    if (rawData.getImage() != null && rawData.getImage().getFile() != null) {
                        File imageFile = new File (rawData.getImage().getFile().getFileFolder(), rawData.getImage().getFile().getIdentifier());
                        if (imageFile.exists()) {
                            imageFile.delete();
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
            
            datasetRepository.deleteArrayDataset(id, user);
            return new Confirmation("array dataset deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete array dataset " + id, e);
        } 
    }
    
    @ApiOperation(value = "Delete the given raw data from the given array dataset")
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
            
            RawData rawData = datasetRepository.getRawDataFromURI(GlygenArrayRepositoryImpl.uriPrefix + id, false, user);
            //delete the files associated with the rawdata
            if (rawData != null) {
                if (rawData.getFile() != null) {
                    File rawDataFile = new File (rawData.getFile().getFileFolder(), id + rawData.getFile().getIdentifier());
                    if (rawDataFile.exists()) {
                        rawDataFile.delete();
                    }
                }
                if (rawData.getImage() != null && rawData.getImage().getFile() != null) {
                    File imageFile = new File (rawData.getImage().getFile().getFileFolder(), rawData.getImage().getFile().getIdentifier());
                    if (imageFile.exists()) {
                        imageFile.delete();
                    }
                }
            } else {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("id", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Cannot find rawData with the given id", errorMessage);
            }
            
            datasetRepository.deleteRawData(id, datasetId, user);
            return new Confirmation("Rawdata deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete rawdata " + id, e);
        } 
    }
    
    @ApiOperation(value = "Delete the given processed data from the given array dataset")
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
            datasetRepository.deleteProcessedData(id, datasetId, user);
            return new Confirmation("ProcessedData deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete ProcessedData " + id, e);
        } 
    }
    
    @ApiOperation(value = "Delete given sample from the user's list")
    @RequestMapping(value="/deletesample/{sampleId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Sample deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete samples"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteSample (
            @ApiParam(required=true, value="id of the sample to delete") 
            @PathVariable("sampleId") String id, Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            datasetRepository.deleteMetadata(id, user);
            return new Confirmation("Sample deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete sample " + id, e);
        } 
    }
    
    @ApiOperation(value = "Delete given image analysis software from the user's list")
    @RequestMapping(value="/deleteimagemetadata/{imageAnaysisMetadataId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Image analysis software deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete image analysis software"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteImageAnalysisSoftware (
            @ApiParam(required=true, value="id of the image analysis software to delete") 
            @PathVariable("imageAnaysisMetadataId") String id, Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            datasetRepository.deleteMetadata(id, user);
            return new Confirmation("Image analysis software deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete image analysis software " + id, e);
        } 
    }
    
    @ApiOperation(value = "Delete given slide metadata from the user's list")
    @RequestMapping(value="/deleteslidemetadata/{slideMetadataId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Slide metadata deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete slide metadata"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteSlideMetadata (
            @ApiParam(required=true, value="id of the slide metadata to delete") 
            @PathVariable("sampleId") String id, Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            datasetRepository.deleteMetadata(id, user);
            return new Confirmation("Slide metadata deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete slide metadata " + id, e);
        } 
    }
    
    @ApiOperation(value = "Delete given data processing software from the user's list")
    @RequestMapping(value="/deletedataprocessingmetadata/{dataProcessingMetadataId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Data processing software deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete data processing software"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteDataProcessingSoftware (
            @ApiParam(required=true, value="id of the data processing software to delete") 
            @PathVariable("dataProcessingMetadataId") String id, Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            datasetRepository.deleteMetadata(id, user);
            return new Confirmation("Data processing software deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete data processing software " + id, e);
        } 
    }
    
    @ApiOperation(value = "Delete given scanner from the user's list")
    @RequestMapping(value="/deletescannermetadata/{scannerId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Scanner deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete scanner"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteScanner (
            @ApiParam(required=true, value="id of the scanner to delete") 
            @PathVariable("scannerId") String id, Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            datasetRepository.deleteMetadata(id, user);
            return new Confirmation("Scanner deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete scanner " + id, e);
        } 
    }
    
    @ApiOperation(value = "Delete given printer from the user's list")
    @RequestMapping(value="/deleteprintermetadata/{printerId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Printer deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete printer"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deletePrinter (
            @ApiParam(required=true, value="id of the printer metadata to delete") 
            @PathVariable("printerId") String id, Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            datasetRepository.deleteMetadata(id, user);
            return new Confirmation("Printer deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete printer " + id, e);
        } 
    }
    
    @ApiOperation(value = "Delete given assay metadata from the user's list")
    @RequestMapping(value="/deleteassaymetadata/{assayId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Assay metadata deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete assay metadata"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteAssayMetadata (
            @ApiParam(required=true, value="id of the assay metadata to delete") 
            @PathVariable("assayId") String id, Principal principal) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
            datasetRepository.deleteMetadata(id, user);
            return new Confirmation("Assay deleted successfully", HttpStatus.OK.value());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete assay metadata " + id, e);
        } 
    }
    
    @ApiOperation(value = "Retrieve slide with the given id")
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
            @PathVariable("slideId") String id, Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            PrintedSlide slide = datasetRepository.getPrintedSlideFromURI(GlygenArrayRepository.uriPrefix + id, user);
            if (slide == null) {
                throw new EntityNotFoundException("Printed slide with id : " + id + " does not exist in the repository");
            }
            // check if it is in use
            boolean notInUse = datasetRepository.canDeletePrintedSlide (slide.getUri(), user);
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
    
    @ApiOperation(value = "Retrieve dataset with the given id")
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
            ArrayDataset dataset = datasetRepository.getArrayDataset(id, loadAll, user);
            if (dataset == null) {
                throw new EntityNotFoundException("Array dataset with id : " + id + " does not exist in the repository");
            }
            
            return dataset;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Array dataset with id " + id + " cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @ApiOperation(value = "Retrieve processed data with the given id")
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
            @PathVariable("id") String id, Principal p) {
        try {
            if (id == null || id.isEmpty()) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("id", "NoEmpty"));
                throw new IllegalArgumentException("id must be provided", errorMessage);
            }
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            ProcessedData data = datasetRepository.getProcessedDataFromURI(GlygenArrayRepository.uriPrefix + id, false, user);
            if (data == null) {
                throw new EntityNotFoundException("Processed data with id : " + id + " does not exist in the repository");
            }
            
            return data;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Processed data with id " + id + " cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @ApiOperation(value = "Retrieve sample with the given id")
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
            @PathVariable("sampleId") String id, Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            Sample sample = datasetRepository.getSampleFromURI(GlygenArrayRepository.uriPrefix + id, user);
            if (sample == null) {
                throw new EntityNotFoundException("Sample with id : " + id + " does not exist in the repository");
            }
            return sample;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Sample cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @ApiOperation(value = "Retrieve printer with the given id")
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
            @PathVariable("printerId") String id, Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            Printer metadata = datasetRepository.getPrinterFromURI(GlygenArrayRepository.uriPrefix + id, user);
            if (metadata == null) {
                throw new EntityNotFoundException("Printer with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Printer cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @ApiOperation(value = "Retrieve scanner with the given id")
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
            @PathVariable("scannerId") String id, Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            ScannerMetadata metadata = datasetRepository.getScannerMetadataFromURI(GlygenArrayRepository.uriPrefix + id, user);
            if (metadata == null) {
                throw new EntityNotFoundException("ScannerMetadata with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("ScannerMetadata cannot be retrieved for user " + p.getName(), e);
        }   
    }
    @ApiOperation(value = "Retrieve SlideMetadata with the given id")
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
            @PathVariable("slideId") String id, Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            SlideMetadata metadata = datasetRepository.getSlideMetadataFromURI(GlygenArrayRepository.uriPrefix + id, user);
            if (metadata == null) {
                throw new EntityNotFoundException("SlideMetadata with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("SlideMetadata cannot be retrieved for user " + p.getName(), e);
        }   
    }
    @ApiOperation(value = "Retrieve ImageAnalysisSoftware with the given id")
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
            @PathVariable("imagesoftwareId") String id, Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            ImageAnalysisSoftware metadata = datasetRepository.getImageAnalysisSoftwareFromURI(GlygenArrayRepository.uriPrefix + id, user);
            if (metadata == null) {
                throw new EntityNotFoundException("ImageAnalysisSoftware with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("ImageAnalysisSoftware cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @ApiOperation(value = "Retrieve DataProcessingSoftware with the given id")
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
            @PathVariable("dataprocessingId") String id, Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            DataProcessingSoftware metadata = datasetRepository.getDataProcessingSoftwareFromURI(GlygenArrayRepository.uriPrefix + id, user);
            if (metadata == null) {
                throw new EntityNotFoundException("DataProcessingSoftware with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("DataProcessingSoftware cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @ApiOperation(value = "Retrieve assay metadata with the given id")
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
            @PathVariable("assayId") String id, Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            AssayMetadata metadata = datasetRepository.getAssayMetadataFromURI(GlygenArrayRepository.uriPrefix + id, user);
            if (metadata == null) {
                throw new EntityNotFoundException("Assay metadata with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Assay metadata cannot be retrieved for user " + p.getName(), e);
        }   
    }
    
    @ApiOperation(value = "Update given printed slide for the user")
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
            @ApiParam(required=true, value="Printed slide with updated fields") 
            @RequestBody PrintedSlide printedSlide, Principal p) throws SQLException {
        
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
        
        if (printedSlide.getName() == null || printedSlide.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        
        // check to make sure, the slide layout is specified
        if (printedSlide.getLayout() == null || (printedSlide.getLayout().getId() == null && printedSlide.getLayout().getUri() == null && printedSlide.getLayout().getName() == null)) {
            errorMessage.addError(new ObjectError("slidelayout", "NoEmpty"));
        } 

        // check for duplicate name
        try {
            PrintedSlide existing = datasetRepository.getPrintedSlideByLabel(printedSlide.getName().trim(), user);
            if (existing != null && !existing.getUri().equals(printedSlide.getUri())) {
                errorMessage.addError(new ObjectError("name", "Duplicate"));
            }
        } catch (SparqlException | SQLException e2) {
            throw new GlycanRepositoryException("Error checking for duplicate printedSlide", e2);
        }
        
        // check if the slide layout exists
        if (printedSlide.getLayout() != null) {
            try {
                String slideLayoutId = printedSlide.getLayout().getId();
                if (slideLayoutId == null) {
                    if (printedSlide.getLayout().getUri() != null) {
                        slideLayoutId = printedSlide.getLayout().getUri().substring(printedSlide.getLayout().getUri().lastIndexOf("/") + 1);
                    }
                }
                if (slideLayoutId != null) {
                    SlideLayout existing = layoutRepository.getSlideLayoutById(slideLayoutId, user, false);
                    if (existing == null) {
                        errorMessage.addError(new ObjectError("slidelayout", "NotFound"));
                    } else {
                        printedSlide.setLayout(existing);
                    }
                } else if (printedSlide.getLayout().getName() != null) {
                    SlideLayout existing = layoutRepository.getSlideLayoutByName(printedSlide.getLayout().getName(), user);
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
                    SlideMetadata slideMetadata = datasetRepository.getSlideMetadataByLabel(printedSlide.getMetadata().getName(), user);
                    if (slideMetadata == null) {
                        errorMessage.addError(new ObjectError("slideMetadata", "NotFound"));
                    } else {
                        printedSlide.setMetadata(slideMetadata);
                    }
                } else if (printedSlide.getMetadata().getUri() != null) {
                    SlideMetadata slideMetadata = datasetRepository.getSlideMetadataFromURI(printedSlide.getMetadata().getUri(), user);
                    if (slideMetadata == null) {
                        errorMessage.addError(new ObjectError("slideMetadata", "NotFound"));
                    } else {
                        printedSlide.setMetadata(slideMetadata);
                    }
                } else if (printedSlide.getMetadata().getId() != null) {
                    SlideMetadata slideMetadata = datasetRepository.getSlideMetadataFromURI(ArrayDatasetRepositoryImpl.uriPrefix + printedSlide.getMetadata().getId(), user);
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
                    Printer printer = datasetRepository.getPrinterByLabel(printedSlide.getPrinter().getName(), user);
                    if (printer == null) {
                        errorMessage.addError(new ObjectError("printer", "NotFound"));
                    } else {
                        printedSlide.setPrinter(printer);
                    }
                } else if (printedSlide.getPrinter().getUri() != null) {
                    Printer printer = datasetRepository.getPrinterFromURI(printedSlide.getPrinter().getUri(), user);
                    if (printer == null) {
                        errorMessage.addError(new ObjectError("printer", "NotFound"));
                    } else {
                        printedSlide.setPrinter(printer);
                    }
                } else if (printedSlide.getMetadata().getId() != null) {
                    Printer printer = datasetRepository.getPrinterFromURI(ArrayDatasetRepositoryImpl.uriPrefix + printedSlide.getPrinter().getId(), user);
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
        
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid printed slide information", errorMessage);
        
        try {
            datasetRepository.updatePrintedSlide(printedSlide, user);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Printed slide cannot be updated for user " + p.getName(), e);
        }       
        return new Confirmation("Printed slide updated successfully", HttpStatus.OK.value());
    }
    
    @ApiOperation(value = "Update given array dataset for the user")
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
            @ApiParam(required=true, value="Array dataset with updated fields. You can change name, description and sample") 
            @RequestBody ArrayDataset dataset, Principal p) throws SQLException {
        
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
        
        // check to make sure, the sample is specified
        if (dataset.getSample() == null || 
                (dataset.getSample().getId() == null && dataset.getSample().getUri() == null && dataset.getSample().getName() == null)) {
            errorMessage.addError(new ObjectError("sample", "NoEmpty"));
        } 

        // check for duplicate name
        try {
            ArrayDataset existing = datasetRepository.getArrayDatasetByLabel(dataset.getName(), false, user);
            if (existing != null && !existing.getUri().equals(dataset.getUri()) && !existing.getId().equals(dataset.getId())) {
                errorMessage.addError(new ObjectError("name", "Duplicate"));
            }
        } catch (SparqlException | SQLException e2) {
            throw new GlycanRepositoryException("Error checking for duplicate array dataset", e2);
        }
        
        // check if the sample exists
        if (dataset.getSample() != null) {
            try {
                String id = dataset.getSample().getId();
                if (id == null) {
                    if (dataset.getSample().getUri() != null) {
                        id = dataset.getSample().getUri().substring(dataset.getSample().getUri().lastIndexOf("/") + 1);
                    }
                }
                if (id != null) {
                    Sample existing = datasetRepository.getSampleFromURI(GlygenArrayRepositoryImpl.uriPrefix + id, user);
                    if (existing == null) {
                        errorMessage.addError(new ObjectError("sample", "NotFound"));
                    } else {
                        dataset.setSample(existing);
                    }
                } else if (dataset.getSample().getName() != null) {
                    Sample existing = datasetRepository.getSampleByLabel(dataset.getSample().getName(), user);
                    if (existing == null) {
                        errorMessage.addError(new ObjectError("slidelayout", "NotFound"));
                    } else {
                        dataset.setSample(existing);
                    }
                }
            } catch (SQLException | SparqlException e) {
                throw new GlycanRepositoryException("Error checking for the existince of the sample", e);
            }
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
            throw new IllegalArgumentException("Invalid Input: Not a valid printed slide information", errorMessage);
        }
        
        try {
            datasetRepository.updateArrayDataset(dataset, user);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Array dataset cannot be updated for user " + p.getName(), e);
        }       
        return new Confirmation("Array dataset updated successfully", HttpStatus.OK.value());
    }
    
    @ApiOperation(value = "Update given sample for the user")
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
            @RequestBody Sample metadata, Principal p) throws SQLException {
        return updateMetadata(metadata, MetadataTemplateType.SAMPLE, p);
        
    }
    
    @ApiOperation(value = "Update given printer for the user")
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
            @RequestBody Printer metadata, Principal p) throws SQLException {
        return updateMetadata(metadata, MetadataTemplateType.PRINTER, p);
        
    }
    
    @ApiOperation(value = "Update given scanner metadata for the user")
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
            @RequestBody ScannerMetadata metadata, Principal p) throws SQLException {
        return updateMetadata(metadata, MetadataTemplateType.SCANNER, p);
        
    }
    
    @ApiOperation(value = "Update given slide metadata for the user")
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
            @RequestBody SlideMetadata metadata, Principal p) throws SQLException {
        return updateMetadata(metadata, MetadataTemplateType.SLIDE, p);
        
    }
    
    @ApiOperation(value = "Update given image analysis software for the user")
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
            @RequestBody ImageAnalysisSoftware metadata, Principal p) throws SQLException {
        return updateMetadata(metadata, MetadataTemplateType.IMAGEANALYSISSOFTWARE, p);
        
    }
    
    @ApiOperation(value = "Update given data processing software for the user")
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
            @RequestBody DataProcessingSoftware metadata, Principal p) throws SQLException {
        return updateMetadata(metadata, MetadataTemplateType.DATAPROCESSINGSOFTWARE, p);
        
    }
    
    @ApiOperation(value = "Update given assay metadata for the user")
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
            @RequestBody DataProcessingSoftware metadata, Principal p) throws SQLException {
        return updateMetadata(metadata, MetadataTemplateType.ASSAY, p);
        
    }
     
    private Confirmation updateMetadata (MetadataCategory metadata, MetadataTemplateType type, Principal p) {
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
                    existing = datasetRepository.getSampleByLabel(metadata.getName(), user);
                    break;
                case PRINTER:
                    existing = datasetRepository.getPrinterByLabel(metadata.getName(), user);
                    break;
                case SCANNER:
                    existing = datasetRepository.getScannerMetadataByLabel(metadata.getName(), user);
                    break;
                case SLIDE:
                    existing = datasetRepository.getSlideMetadataByLabel(metadata.getName(), user);
                    break;
                case IMAGEANALYSISSOFTWARE:
                    existing = datasetRepository.getImageAnalysisSoftwarByLabel(metadata.getName(), user);
                    break;
                case DATAPROCESSINGSOFTWARE:
                    existing = datasetRepository.getDataProcessingSoftwareByLabel(metadata.getName(), user);
                    break;
                case ASSAY:
                    existing = datasetRepository.getAssayMetadataByLabel(metadata.getName(), user);
                    break;
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
            datasetRepository.updateMetadata(metadata, user);
            try {
                // check mirage and update mirage info
                boolean isMirage = checkMirageCompliance(metadata.getId(), type, p);
                metadata.setIsMirage(isMirage);
            } catch (IllegalArgumentException e) {
                metadata.setIsMirage(false);
                logger.error("Error checking for mirage compliance", e);
            }
            datasetRepository.updateMetadataMirage(metadata, user);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Error updating metadata with id: " + metadata.getId());
        }
        return new Confirmation("Metadata updated successfully", HttpStatus.OK.value());
        
    }
    
    @ApiOperation(value = "Make the given array dataset public")
    @RequestMapping(value="/makearraydatasetpublic/{datasetid}", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="id of the public array dataset"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to modify the dataset"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String makeArrayDatasetPublic (
            @ApiParam(required=true, value="id of the dataset to make pblic") 
            @PathVariable("datasetId") String datasetId, Principal p) {
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        try {
            ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, true, user);
            if (dataset == null) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("datasetId", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("There is no dataset with the given id in user's repository", errorMessage); 
            }
            String datasetURI = datasetRepository.makePublicArrayDataset(dataset, user); 
            return datasetURI.substring(datasetURI.lastIndexOf("/")+1);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Array dataset cannot be made public for user " + p.getName(), e);
        } 
    }
}
