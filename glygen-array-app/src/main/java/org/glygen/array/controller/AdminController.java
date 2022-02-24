package org.glygen.array.controller;

import java.io.File;
import java.security.Principal;
import java.sql.SQLException;

import org.glygen.array.config.SesameTransactionConfig;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.rdf.data.ArrayDataset;
import org.glygen.array.persistence.rdf.data.FutureTaskStatus;
import org.glygen.array.persistence.rdf.data.Image;
import org.glygen.array.persistence.rdf.data.ProcessedData;
import org.glygen.array.persistence.rdf.data.RawData;
import org.glygen.array.persistence.rdf.data.Slide;
import org.glygen.array.service.ArrayDatasetRepository;
import org.glygen.array.service.GlygenArrayRepository;
import org.glygen.array.service.GlygenArrayRepositoryImpl;
import org.glygen.array.service.MetadataTemplateRepository;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
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

@Import(SesameTransactionConfig.class)
@RestController
@RequestMapping("/admin")
public class AdminController {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    @Autowired
    @Qualifier("glygenArrayRepositoryImpl")
    GlygenArrayRepository repository;
    
    @Autowired
    MetadataTemplateRepository templateRepository;
    
    @Autowired
    ArrayDatasetRepository datasetRepository;
     

    @ApiOperation(value = "Deletes everything in the repository. It also removes the required initial data/settings in the triple store. "
            + "It is therefore necessary to restart the triple store after executing this web service.")
    @RequestMapping(value="/reset", method=RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses(value= {@ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation resetRepository (Principal p) {
        try {
            repository.resetRepository();
            return new Confirmation("emptied the repository", HttpStatus.OK.value());
        } catch (SQLException e) {
            throw new GlycanRepositoryException(e);
        }
    }
    
    @ApiOperation(value = "Deletes templates for metadata")
    @RequestMapping(value="/deleteTemplates", method = RequestMethod.POST)
    public void deleteTemplates (Principal p) { 
        try {
            // cleanup
            templateRepository.deleteTemplates();
            
        } catch (SparqlException e) {
            logger.error("Error deleting templates", e);
            throw new GlycanRepositoryException("Error deleting templates", e);
        }
    }
    
    @ApiOperation(value = "Delete and recreate templates using the current version of the template ontology.")
    @RequestMapping(value="/populateTemplates", method = RequestMethod.POST)
    public void populateTemplates (Principal p) { 
        try {
            // cleanup first
            templateRepository.deleteTemplates();
            templateRepository.populateTemplateOntology();
        } catch (SparqlException e) {
            logger.error("Error populating templates", e);
            throw new GlycanRepositoryException("Error populating templates", e);
        }
    }
    
    @ApiOperation(value = "Delete the given array dataset from public repository")
    @RequestMapping(value="/deletepublicdataset/{datasetId}", method = RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Dataset deleted successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to delete datasets"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation deleteArrayDataset (
            @ApiParam(required=true, value="id of the array dataset to delete") 
            @PathVariable("datasetId") String id) {
        try {
            
            // delete the files associated with the array dataset
            ArrayDataset dataset = datasetRepository.getArrayDataset(id, false, null);
            // check the status of dataset, if PROCESSING cannot delete
            if (dataset != null && dataset.getStatus() == FutureTaskStatus.PROCESSING) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("dataset", "NotDone"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Cannot delete the dataset when it is still processing", errorMessage);
            }
            if (dataset != null && dataset.getSlides() != null) {
                for (Slide slide: dataset.getSlides()) {
                    deleteSlide(slide.getId(), id);
                }
                datasetRepository.deleteArrayDataset(id, null);
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
    
    public Confirmation deleteSlide (
            @ApiParam(required=true, value="id of the slide to delete") 
            @PathVariable("slideId") String id, 
            @ApiParam(required=true, value="id of the array dataset this slide belongs to") 
            @RequestParam(name="datasetId", required=true)
            String datasetId) {
        try {
            Slide slide = datasetRepository.getSlideFromURI(GlygenArrayRepositoryImpl.uriPrefixPublic + id, false, null);
            //delete the files associated with the slide (image, raw data and processed data files)
            if (slide != null) {
                for (Image image: slide.getImages()) {
                    if (image.getRawDataList() != null) {
                        for (RawData rawData: image.getRawDataList()) {
                            if (rawData != null && rawData.getFile() != null) {
                                if (rawData.getStatus() == FutureTaskStatus.PROCESSING) {
                                    ErrorMessage errorMessage = new ErrorMessage();
                                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                                    errorMessage.addError(new ObjectError("rawData", "NotDone"));
                                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                                    throw new IllegalArgumentException("Cannot delete the slide when it is still processing", errorMessage);
                                }
                                if (rawData.getProcessedDataList() != null) {
                                    for (ProcessedData processedData: rawData.getProcessedDataList()) {
                                        if (processedData.getStatus() == FutureTaskStatus.PROCESSING) {
                                            ErrorMessage errorMessage = new ErrorMessage();
                                            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                                            errorMessage.addError(new ObjectError("processedData", "NotDone"));
                                            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                                            throw new IllegalArgumentException("Cannot delete the slide when it is still processing", errorMessage);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                datasetRepository.deleteSlide(id, datasetId, null);   
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
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("slideId", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Cannot find slide with the given id", errorMessage);
            }
            
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot delete slide " + id, e);
        } 
    }

}
