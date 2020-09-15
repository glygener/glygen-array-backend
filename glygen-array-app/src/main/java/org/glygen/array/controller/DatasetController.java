package org.glygen.array.controller;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.glygen.array.config.SesameTransactionConfig;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.data.ArrayDataset;
import org.glygen.array.persistence.rdf.data.PrintedSlide;
import org.glygen.array.persistence.rdf.data.ProcessedData;
import org.glygen.array.persistence.rdf.data.RawData;
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
import org.glygen.array.service.FeatureRepository;
import org.glygen.array.service.GlycanRepository;
import org.glygen.array.service.GlygenArrayRepository;
import org.glygen.array.service.LayoutRepository;
import org.glygen.array.service.LinkerRepository;
import org.glygen.array.service.MetadataTemplateRepository;
import org.glygen.array.util.parser.ProcessedDataParser;
import org.glygen.array.util.parser.ProcessedResultConfiguration;
import org.glygen.array.view.ArrayDatasetListView;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.glygen.array.view.MetadataListResultView;
import org.glygen.array.view.PrintedSlideListView;
import org.grits.toolbox.glycanarray.om.model.StatisticalMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
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

@Import(SesameTransactionConfig.class)
@RestController
@RequestMapping("/array")
public class DatasetController {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    @Autowired
    ArrayDatasetRepository datasetRepository;
    
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
            ArrayDataset existing = datasetRepository.getArrayDatasetByLabel(dataset.getName().trim(), user);
            if (existing != null) {
                errorMessage.addError(new ObjectError("name", "Duplicate"));
            }
        } catch (SparqlException | SQLException e2) {
            throw new GlycanRepositoryException("Error checking for duplicate array dataset", e2);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid array dataset information", errorMessage);
        
        try {
            return datasetRepository.addArrayDataset(dataset, user);
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
            @ApiParam(required=true, value="Raw data set to be added") 
            @RequestBody RawData rawData, 
            @ApiParam(required=true, value="name of the array dataset (must already be in the repository) to add the raw data") 
            @RequestParam("arraydatasetId")
            String datasetId,  
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        // check if the dataset with the given id exists
        try {
            ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, user);
            if (dataset == null) {
                errorMessage.addError(new ObjectError("dataset", "NotFound"));
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Dataset " + datasetId + " cannot be retrieved for user " + p.getName(), e);
        }
        
        // check to make sure, the file is specified and exists in the uploads folder
        if (rawData.getFilename() == null) {
            errorMessage.addError(new ObjectError("filename", "NotFound"));
        } else {
            File file = new File (uploadDir + rawData.getFilename());
            if (!file.exists()) {
                errorMessage.addError(new ObjectError("file", "NotFound"));
            }
        }
        // TODO check to make sure the image is specified and image file is in uploads folder
        // TODO check to make sure the slide is specified and the slide already exists
        // TODO check if the metadata exists
        
        // TODO parse the file and extract measurements
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid raw data information", errorMessage);
        
        try {
            return datasetRepository.addRawData(rawData, datasetId, user);
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
            return datasetRepository.addPrintedSlide(slide, user);
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
            @RequestBody DataProcessingSoftware metadata, Principal p) {
        
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
                ErrorMessage err = validateMetadata (metadata, template);
                if (err != null) {
                    for (ObjectError error: err.getErrors())
                        errorMessage.addError(error);
                }    
            }
        } catch (SparqlException | SQLException e1) {
            logger.error("Error retrieving template", e1);
            throw new GlycanRepositoryException("Error retrieving Data processing software metadata template " + p.getName(), e1);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid Data processing software metadata information", errorMessage);
        
        try {
            return datasetRepository.addDataProcessingSoftware(metadata, user);
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
            @RequestBody ImageAnalysisSoftware metadata, Principal p) {
        
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
                ErrorMessage err = validateMetadata (metadata, template);
                if (err != null) {
                    for (ObjectError error: err.getErrors())
                        errorMessage.addError(error);
                }    
            }
        } catch (SparqlException | SQLException e1) {
            logger.error("Error retrieving template", e1);
            throw new GlycanRepositoryException("Error retrieving image analysis metadata template " + p.getName(), e1);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid image analysis metadata information", errorMessage);
        
        try {
            return datasetRepository.addImageAnalysisSoftware(metadata, user);
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
            @RequestBody Printer printer, Principal p) {
        
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
                ErrorMessage err = validateMetadata (printer, template);
                if (err != null) {
                    for (ObjectError error: err.getErrors())
                        errorMessage.addError(error);
                }    
            }
        } catch (SparqlException | SQLException e1) {
            logger.error("Error retrieving template", e1);
            throw new GlycanRepositoryException("Error retrieving printer template " + p.getName(), e1);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid printer information", errorMessage);
        
        try {
            return datasetRepository.addPrinter(printer, user);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Printer cannot be added for user " + p.getName(), e);
        }
        
    }
    
    @ApiOperation(value = "Import experiment results from uploaded excel file")
    @RequestMapping(value = "/addDatasetFromExcel", method=RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added processed data for the given array dataset"), 
            @ApiResponse(code=400, message="Invalid request, file cannot be found"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to add array datasets"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addProcessedDataFromExcel (
            @ApiParam(required=true, value="uploaded Excel with the experiment results") 
            @RequestParam("file") String uploadedFileName, 
            @ApiParam(required=true, value="name of the array dataset (must already be in the repository) to add the processed data") 
            @RequestParam("arraydatasetId")
            String datasetId,        
            @ApiParam(required=true, value="configuration information related to the excel file") 
            @RequestBody
            ProcessedResultConfiguration config,
            Principal p) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        if (uploadedFileName != null) {
            File excelFile = new File(uploadDir, uploadedFileName);
            if (excelFile.exists()) {
                UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
                try {
                    // check if the dataset with the given id exists
                    ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId, user);
                    if (dataset == null) {
                        errorMessage.addError(new ObjectError("dataset", "NotFound"));
                    }
                    
                    ProcessedDataParser parser = new ProcessedDataParser(featureRepository, layoutRepository, glycanRepository, linkerRepository);
                
                    Resource resource = resourceLoader.getResource("classpath:sequenceMap.txt");
                    if (!resource.exists()) {
                        errorMessage.addError(new ObjectError("mapFile", "NotValid"));
                        throw new IllegalArgumentException("Mapping file cannot be found in resources", errorMessage);
                    }
                    ProcessedData processedData = parser.parse(excelFile.getAbsolutePath(), resource.getFile().getAbsolutePath(), config, user);
                    
                    if (config.getResultFileType().equalsIgnoreCase("cfg")) {
                        processedData.setMethod(StatisticalMethod.ELIMINATE);
                    } else {
                        processedData.setMethod(StatisticalMethod.AVERAGE);
                    }
                    
                    //TODO move the file to the temp folder so that it is deleted with the next cleanup
                    
                    return datasetRepository.addProcessedData(processedData, datasetId, user);   
                } catch (InvalidFormatException | IOException | SparqlException | SQLException e)  {
                    errorMessage.addError(new ObjectError("file", "NotValid"));
                    throw new IllegalArgumentException("File cannot be parsed", errorMessage);
                } catch (IllegalArgumentException e) {
                    throw e;
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
            @RequestBody Sample sample, Principal p) {
        return addSample(sample, true, p);
    }
        
    @ApiOperation(value = "Add given sample metadata for the user")
    @RequestMapping(value="/addSampleNoValidation", method = RequestMethod.POST, 
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
            return datasetRepository.addSample(sample, user);
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
            @RequestBody ScannerMetadata metadata, Principal p) {
        
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
                ErrorMessage err = validateMetadata (metadata, template);
                if (err != null) {
                    for (ObjectError error: err.getErrors())
                        errorMessage.addError(error);
                }    
            }
        } catch (SparqlException | SQLException e1) {
            logger.error("Error retrieving template", e1);
            throw new GlycanRepositoryException("Error retrieving scanner template " + p.getName(), e1);
        }
       
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid scanner information", errorMessage);
        
        try {
            return datasetRepository.addScannerMetadata(metadata, user);
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
            @RequestBody SlideMetadata metadata, Principal p) {
        
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
                ErrorMessage err = validateMetadata (metadata, template);
                if (err != null) {
                    for (ObjectError error: err.getErrors())
                        errorMessage.addError(error);
                }    
            }
        } catch (SparqlException | SQLException e1) {
            logger.error("Error retrieving template", e1);
            throw new GlycanRepositoryException("Error retrieving slide metadata template " + p.getName(), e1);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid slide metadata information", errorMessage);
        
        try {
            return datasetRepository.addSlideMetadata(metadata, user);
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
            
            List<ArrayDataset> resultList = datasetRepository.getArrayDatasetByUser(user, offset, limit, field, order, searchValue);
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
    public MetadataListResultView listImageanAlysisSoftware (
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
            throw new GlycanRepositoryException("Cannot retrieve data processing software for user. Reason: " + e.getMessage());
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
            return slide;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Printed slide cannot be retrieved for user " + p.getName(), e);
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
            if (existing != null) {
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
}
