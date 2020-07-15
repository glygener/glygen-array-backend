package org.glygen.array.controller;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.persistence.EntityExistsException;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.glygen.array.config.SesameTransactionConfig;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.data.ArrayDataset;
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
import org.glygen.array.service.LayoutRepository;
import org.glygen.array.service.LinkerRepository;
import org.glygen.array.service.MetadataTemplateRepository;
import org.glygen.array.util.parser.ProcessedDataParser;
import org.glygen.array.util.parser.ProcessedResultConfiguration;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.glygen.array.view.MetadataListResultView;
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
    
    @ApiOperation(value = "Add given array dataset for the user")
    @RequestMapping(value="/addDataset", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added dataset"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to register datasets"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addArrayDataset (
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
        //TODO do validation on other fields such as Sample, rawdata, processeddata
        if (dataset.getName() == null || dataset.getName().isEmpty()) {
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid feature information", errorMessage);
        
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
            @RequestBody RawData rawData, Principal p) {
        
        //TODO
        return null;
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
            throw new GlycanRepositoryException("Error retrieving image analysis metadata template " + p.getName(), e1);
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid image analysis metadata information", errorMessage);
        
        try {
            return datasetRepository.addDataProcessingSoftware(metadata, user);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Image Analysis metadata cannot be added for user " + p.getName(), e);
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
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added array dataset"), 
            @ApiResponse(code=400, message="Invalid request, file cannot be found"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to add array datasets"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addProcessedDataFromExcel (
            @ApiParam(required=true, value="uploaded Excel with the experiment results") 
            @RequestParam("file") String uploadedFileName, 
            @ApiParam(required=true, value="name of the array dataset (must already be in the repository) to add the processed data") 
            @RequestParam("name")
            String datasetName, 
            @ApiParam(required=true, value="(internal) name of the sample used in the experiment, must already be in the repository") 
            @RequestParam("sampleName")
            String sample,
            @ApiParam(required=true, value="configuration information related to excel file") 
            @RequestBody
            ProcessedResultConfiguration config,
            Principal p) {
        if (uploadedFileName != null) {
            File excelFile = new File(uploadDir, uploadedFileName);
            if (excelFile.exists()) {
                UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
                ProcessedDataParser parser = new ProcessedDataParser(featureRepository, glycanRepository, linkerRepository);
                try {
                    Resource resource = resourceLoader.getResource("classpath:sequenceMap.txt");
                    if (!resource.exists()) {
                        ErrorMessage errorMessage = new ErrorMessage();
                        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                        errorMessage.addError(new ObjectError("mapFile", "NotValid"));
                        throw new IllegalArgumentException("Mapping file cannot be found in resources", errorMessage);
                    }
                    ProcessedData processedData = parser.parse(excelFile.getAbsolutePath(), resource.getFile().getAbsolutePath(), config, user);
                    ArrayDataset dataset = new ArrayDataset();
                    dataset.setName(datasetName);
                    // TODO retrieve dataset from repository and update (add processed data)
                    // TODO get Sample from repository
                    //dataset.setSample(sample);
                    dataset.setProcessedData(processedData);
                    return datasetRepository.addArrayDataset(dataset, user);   
                } catch (InvalidFormatException | IOException | SparqlException | SQLException e)  {
                    ErrorMessage errorMessage = new ErrorMessage();
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    errorMessage.addError(new ObjectError("file", "NotValid"));
                    throw new IllegalArgumentException("File cannot be parsed", errorMessage);
                } catch (IllegalArgumentException e) {
                    throw e;
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
                ErrorMessage err = validateMetadata (sample, template);
                if (err != null) {
                    for (ObjectError error: err.getErrors())
                        errorMessage.addError(error);
                }    
            }
        } catch (SparqlException | SQLException e1) {
            logger.error("Error retrieving template", e1);
            throw new GlycanRepositoryException("Error retrieving sample template " + p.getName(), e1);
        }
        
        // check if the name is unique
        if (sample.getName() != null && !sample.getName().trim().isEmpty()) {
            try {
                Sample existing = datasetRepository.getSampleByLabel(sample.getName(), user);
                if (existing != null) {
                    errorMessage.addError(new ObjectError("name", "Duplicate"));
                }
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Could not query existing samples", e);
            }
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
            @RequestParam("metadata type")
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
            metadata = datasetRepository.getMetadataByLabel(name, typePredicate, user);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve metadata by name", e);
        }
        
        return metadata == null;
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
            if (!descTemplate.isGroup()) {
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
    
}
