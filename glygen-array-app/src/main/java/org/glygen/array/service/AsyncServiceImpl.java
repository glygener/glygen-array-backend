package org.glygen.array.service;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.data.FileWrapper;
import org.glygen.array.persistence.rdf.data.Intensity;
import org.glygen.array.persistence.rdf.data.Measurement;
import org.glygen.array.util.parser.ProcessedDataParser;
import org.glygen.array.util.parser.ProcessedResultConfiguration;
import org.glygen.array.util.parser.RawdataParser;
import org.glygen.array.view.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.ObjectError;

@Service
public class AsyncServiceImpl implements AsyncService {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    @Value("${spring.file.uploaddirectory}")
    String uploadDir;
    
    @Autowired
    ResourceLoader resourceLoader;
    
    @Autowired
    GlycanRepository glycanRepository;
    
    @Autowired
    LinkerRepository linkerRepository;
    
    @Autowired
    LayoutRepository layoutRepository;
    
    @Autowired
    FeatureRepository featureRepository;
    
    @Override
    @Async("GlygenArrayAsyncExecutor")
    public CompletableFuture<List<Intensity>> parseProcessDataFile (
            String datasetId,
            FileWrapper file, 
            UserEntity user) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        List<Intensity> intensities = null;
        
        try {
            if (file != null) {
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
                    
                    ProcessedDataParser parser = new ProcessedDataParser(featureRepository, layoutRepository, glycanRepository, linkerRepository);
                    
                    Resource resource = resourceLoader.getResource("classpath:sequenceMap.txt");
                    if (!resource.exists()) {
                        errorMessage.addError(new ObjectError("mapFile", "NotFound"));
                        throw new IllegalArgumentException("Mapping file cannot be found in resources", errorMessage);
                    }
                    try {
                        ProcessedResultConfiguration config = createConfigForVersion(file.getFileFormat(), user);
                        if (config == null) {
                            errorMessage.addError(new ObjectError ("format", "NotValid"));
                            throw new IllegalArgumentException("The configuration for the given format cannot be found", errorMessage);
                        }
                        intensities = parser.parse(newFile.getAbsolutePath(), resource.getFile().getAbsolutePath(), 
                                config, user);
                    } catch (SparqlException | SQLException e) {
                        errorMessage.addError(new ObjectError ("format", "NotValid"));
                        throw new IllegalArgumentException("Config for the given format cannot be found", errorMessage);
                    }
                    return CompletableFuture.completedFuture(intensities);
                } else {
                    errorMessage.addError(new ObjectError("file", "NotValid"));
                    throw new IllegalArgumentException("File cannot be found", errorMessage);
                }
            } else {
                errorMessage.addError(new ObjectError("file", "NotValid"));
                throw new IllegalArgumentException("File cannot be found", errorMessage);
            }
        } catch (InvalidFormatException | IOException e) {
            errorMessage.addError(new ObjectError("file", "NotValid"));
            throw new IllegalArgumentException("File cannot be parsed", errorMessage);
        } catch (IllegalArgumentException e) {
            if (e.getCause() instanceof ErrorMessage) {
                for (ObjectError err: ((ErrorMessage) e.getCause()).getErrors()) {
                    errorMessage.addError(err);
                }
            } else {
                errorMessage.addError(new ObjectError("file", "NotValid"));
            }
            throw new IllegalArgumentException("File is not a valid excel file", errorMessage);
        } catch (Exception e) {
            errorMessage.addError(new ObjectError("file", "NotValid"));
            logger.error("Error parsing the processed data file", e);
            throw new IllegalArgumentException("File is not a valid excel file", errorMessage);
        }
    }
    
    ProcessedResultConfiguration createConfigForVersion (String fileFormat, UserEntity user) throws SparqlException, SQLException {
        // decide on the configuration based on fileFormat
        ProcessedResultConfiguration config = new ProcessedResultConfiguration();
        if (fileFormat.equalsIgnoreCase("CFG_V5.2")) {
            config.setCvColumnId(32);
            config.setFeatureColumnId(29);
            config.setResultFileType("cfg");
            config.setRfuColumnId(30);
            config.setSheetNumber(0);
            config.setStDevColumnId(31);
            config.setStartRow(1);
            // find the slidelayoutid (format name --> slide layout name)
            SlideLayout layout = layoutRepository.getSlideLayoutByName(fileFormat, user);
            if (layout != null) {
                config.setSlideLayoutId(layout.getId());
            } else {
                return null;
            }
        } else {
            return null;
        }
        return config;
        
    }

    @Override
    @Async("GlygenArrayAsyncExecutor")
    public CompletableFuture<Map<Measurement, Spot>> parseRawDataFile(FileWrapper file, SlideLayout layout,
            Double powerLevel) {
        Map<Measurement, Spot> dataMap;
        try {
            dataMap = RawdataParser.parse(file, layout, powerLevel);
            return CompletableFuture.completedFuture(dataMap);
        } catch (Exception e) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            if (e.getCause() instanceof ErrorMessage) {
                for (ObjectError err: ((ErrorMessage) e.getCause()).getErrors()) {
                    errorMessage.addError(err);
                }
            } else {
                errorMessage.addError(new ObjectError("file", "NotValid"));
            }
            logger.error("Error parsing the raw data file", e);
            throw new IllegalArgumentException("File is not a valid file", errorMessage);
        }
        
    }

}
