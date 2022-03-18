package org.glygen.array.service;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.validation.Validator;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.FeatureType;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.GlycanInFeature;
import org.glygen.array.persistence.rdf.LinkedGlycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.data.FileWrapper;
import org.glygen.array.persistence.rdf.data.Intensity;
import org.glygen.array.persistence.rdf.data.Slide;
import org.glygen.array.util.parser.ProcessedDataParser;
import org.glygen.array.util.parser.ProcessedResultConfiguration;
import org.glygen.array.view.ErrorCodes;
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
    
    @Autowired
    UserRepository userRepository;
    
    @Autowired
    Validator validator;
    
    @Value("${spring.file.imagedirectory}")
    String imageLocation;
    
    @Autowired
    AddToRepositoryService addService;
    
    @Override
    @Async("GlygenArrayAsyncExecutor")
    public CompletableFuture<List<Intensity>> parseProcessDataFile (
            String datasetId,
            FileWrapper file,
            Slide slide,
            UserEntity user) {
        
        ErrorMessage errorMessage = new ErrorMessage("Error parsing the processed data file");
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        errorMessage.setErrorCode(ErrorCodes.PARSE_ERROR);
        
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
                    file.setFileSize(newFile.length());
                    
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
                        config.setSlideLayoutUri(slide.getPrintedSlide().getLayout().getUri());
                        intensities = parser.parse(newFile.getAbsolutePath(), resource.getFile().getAbsolutePath(), 
                                config, user);
                    } catch (SparqlException | SQLException e) {
                        errorMessage.addError(new ObjectError ("format", "NotValid"));
                        throw new IllegalArgumentException("Config for the given format cannot be found", errorMessage);
                    }
                    return CompletableFuture.completedFuture(intensities);
                } else {
                    errorMessage.addError(new ObjectError("file", "NotFound"));
                    throw new IllegalArgumentException("File cannot be found", errorMessage);
                }
            } else {
                errorMessage.addError(new ObjectError("file", "NotValid"));
                throw new IllegalArgumentException("File cannot be found", errorMessage);
            }
        } catch (InvalidFormatException | IOException e) {
            errorMessage.addError(new ObjectError("file", e.getMessage()));
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
        } else if (fileFormat.startsWith("Glygen")){
            config.setCvColumnId(-1);
            config.setFeatureColumnId(-1);
            config.setFeatureNameColumnId(1);
            config.setRfuColumnId(3);
            config.setStDevColumnId(4);
            config.setGroupColumnId(2);
            config.setResultFileType("Glygen array data file");
            config.setSheetName("Repo Data");
            config.setStartRow(1);
        }
        return config;
        
    }

    @Override
    @Async("GlygenArrayAsyncExecutor")
    public CompletableFuture<String> importSlideLayout(SlideLayout slideLayout, ErrorMessage errorMessage,
            UserEntity user) {
        // find all block layouts, glycans, linkers and add them first
        for (org.glygen.array.persistence.rdf.Block block: slideLayout.getBlocks()) {
            if (block.getBlockLayout() != null) { 
                try {
                    BlockLayout existing = layoutRepository.getBlockLayoutByName(block.getBlockLayout().getName(), user, false);
                    if (existing != null) { // already added no need to go through glycans/linkers
                        continue;
                    }
                    for (org.glygen.array.persistence.rdf.Spot spot: block.getBlockLayout().getSpots()) {
                        for (org.glygen.array.persistence.rdf.Feature feature: spot.getFeatures()) {
                            // check if the feature already exists before trying to add glycans and linkers
                            if (feature.getInternalId() != null) {
                                org.glygen.array.persistence.rdf.Feature f = featureRepository.getFeatureByLabel(feature.getInternalId(), 
                                        "gadr:has_internal_id", user);
                                if (f != null) { // already added no need to go through glycans/linkers
                                    feature.setId(f.getId());
                                    feature.setUri(f.getUri());
                                    continue;
                                }
                            }
                            if (feature.getType() == FeatureType.LINKEDGLYCAN) {
                                if (((LinkedGlycan) feature).getGlycans() != null) {
                                    for (GlycanInFeature gf: ((LinkedGlycan) feature).getGlycans()) {
                                        Glycan g = gf.getGlycan();
                                        try {   
                                            String glycanId = addService.addGlycan(g, user, true);
                                            if (glycanId != null) {
                                                g.setId(glycanId);
                                                g.setUri(GlygenArrayRepositoryImpl.uriPrefix + glycanId);
                                            }
                                        } catch (Exception e) {
                                            if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                                                ErrorMessage error = (ErrorMessage) e.getCause();
                                                for (ObjectError err: error.getErrors()) {
                                                    if (err.getDefaultMessage().equalsIgnoreCase("duplicate")) {
                                                        if (err.getObjectName().contains("sequence")) {
                                                            if (g instanceof SequenceDefinedGlycan) {
                                                                if (g.getName() != null) {
                                                                    // add name as an alias
                                                                    glycanRepository.addAliasForGlycan(g.getId(), g.getName(), user);
                                                                    //TODO need to update the glycan object of the feature with the duplicate glycan
                                                                }
                                                            }
                                                        }
                                                        
                                                        break;
                                                    } else {
                                                        errorMessage.addError(err);
                                                    }
                                                }
                                            } else {
                                                logger.info("Could not add glycan: ", e);
                                                errorMessage.addError(new ObjectError("glycan", e.getMessage()));
                                            }
                                        }
                                    }
                                    
                                }
                                if (feature.getLinker() != null) {                                 
                                    try {
                                        String linkerID = addService.addLinker(feature.getLinker(),
                                                feature.getLinker().getType().name().startsWith("UNKNOWN"), user);
                                        feature.getLinker().setId(linkerID);
                                        feature.getLinker().setUri(GlygenArrayRepositoryImpl.uriPrefix + linkerID);
                                    } catch (Exception e) {
                                        if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                                            ErrorMessage error = (ErrorMessage) e.getCause();
                                            boolean needAlias = false;
                                            for (ObjectError err: error.getErrors()) {
                                                if (err.getDefaultMessage().contains("Duplicate")) {
                                                    needAlias = true;
                                                    if (err.getObjectName().contains("pubchemid")) {
                                                        needAlias = false;
                                                        break;
                                                    }
                                                } else {
                                                    errorMessage.addError(err);
                                                }
                                            }
                                            if (needAlias) {        
                                                Linker linker = feature.getLinker();
                                                linker.setName(linker.getName()+"B");
                                                try {
                                                    String linkerID = addService.addLinker(linker, linker.getType().name().startsWith("UNKNOWN"), user);
                                                    feature.getLinker().setId(linkerID);
                                                    feature.getLinker().setUri(GlygenArrayRepositoryImpl.uriPrefix + linkerID);
                                                } catch (IllegalArgumentException e1) {
                                                    // ignore, probably already added
                                                    logger.debug ("duplicate linker cannot be added", e1);
                                                }
                                            }
                                        }
                                        else {
                                            logger.info("Could not add linker: ", e);
                                            errorMessage.addError(new ObjectError("linker", e.getMessage()));
                                        }
                                    }
                                }
                                 
                            }
                            try {
                                String id = addService.addFeature(feature, user);
                                feature.setId(id);
                                feature.setUri(GlygenArrayRepositoryImpl.uriPrefix + id);
                            } catch (Exception e) {
                                boolean ignore = false;
                                if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                                    ErrorMessage error = (ErrorMessage) e.getCause();
                                    for (ObjectError err: error.getErrors()) {
                                        if (err.getDefaultMessage().contains("Duplicate")) {
                                            // ignore the error, it is already created
                                            ignore = true;
                                        } else {
                                            errorMessage.addError(err);
                                        }
                                    }
                                }
                                if (!ignore) {
                                    logger.info("Could not add feature: ", e);
                                    errorMessage.addError(new ObjectError("feature", e.getMessage()));
                                }
                            }
                        }
                    }
                    String id = addService.addBlockLayout(block.getBlockLayout(), false, user);
                    block.getBlockLayout().setId(id);
                    block.getBlockLayout().setUri(GlygenArrayRepositoryImpl.uriPrefix + id);
                } catch (Exception e) {
                    logger.info("Could not add block layout", e);
                    if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                        ErrorMessage error = (ErrorMessage) e.getCause();
                        for (ObjectError err: error.getErrors()) {
                            errorMessage.addError(err);
                        }
                    } else {
                        errorMessage.addError(new ObjectError("blockLayout", e.getMessage()));
                    }
                }
            }
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
            throw new IllegalArgumentException("error adding block layouts!", errorMessage);
        }
        
        try {
            String id = layoutRepository.addBlocksToSlideLayout(slideLayout, user);
            slideLayout.setId(id);
            return CompletableFuture.completedFuture(id);
        } catch (Exception e) {
            if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                ErrorMessage error = (ErrorMessage) e.getCause();
                throw new IllegalArgumentException("error adding slide layout", error);
            } else {
                logger.debug("Could not add slide layout", e);
                errorMessage.addError(new ObjectError("slideLayout", e.getMessage()));
                throw new IllegalArgumentException("error adding slide layout", errorMessage);
            }
        }
    }
}
