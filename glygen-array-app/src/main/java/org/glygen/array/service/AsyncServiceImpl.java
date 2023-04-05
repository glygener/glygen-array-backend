package org.glygen.array.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

import javax.validation.Validator;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.glygen.array.controller.GlygenArrayController;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.FeatureType;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.GlycanInFeature;
import org.glygen.array.persistence.rdf.GlycanSequenceFormat;
import org.glygen.array.persistence.rdf.LinkedGlycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.LinkerType;
import org.glygen.array.persistence.rdf.MassOnlyGlycan;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.UnknownGlycan;
import org.glygen.array.persistence.rdf.data.FileWrapper;
import org.glygen.array.persistence.rdf.data.Intensity;
import org.glygen.array.persistence.rdf.data.Slide;
import org.glygen.array.util.ParserConfiguration;
import org.glygen.array.util.SequenceUtils;
import org.glygen.array.util.parser.ProcessedDataParser;
import org.glygen.array.util.parser.ProcessedResultConfiguration;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.grits.toolbox.glycanarray.library.om.ArrayDesignLibrary;
import org.grits.toolbox.util.structure.glycan.util.FilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.ObjectError;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    MetadataTemplateRepository templateRepository;
    
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
                    
                    ProcessedResultConfiguration config = createConfigForVersion(file.getFileFormat());
                    if (config == null) {
                        errorMessage.addError(new ObjectError ("format", "NotValid"));
                        throw new IllegalArgumentException("The configuration for the given format cannot be found", errorMessage);
                    }
                    config.setSlideLayoutUri(slide.getPrintedSlide().getLayout().getUri());
                    intensities = parser.parse(newFile.getAbsolutePath(), resource.getFile().getAbsolutePath(), 
                            config, user);
                    
                    return CompletableFuture.completedFuture(intensities);
                } else {
                    errorMessage.addError(new ObjectError("file", "NotFound"));
                    errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
                    return CompletableFuture.failedFuture(new IllegalArgumentException("File cannot be found", errorMessage));
                }
            } else {
                errorMessage.addError(new ObjectError("file", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                return CompletableFuture.failedFuture( new IllegalArgumentException("File cannot be found", errorMessage));
            }
        } catch (InvalidFormatException | IOException e) {
            errorMessage.addError(new ObjectError("file", e.getMessage()));
            errorMessage.setErrorCode(ErrorCodes.PARSE_ERROR);
            return CompletableFuture.failedFuture( new IllegalArgumentException("File cannot be parsed", errorMessage));
        } catch (IllegalArgumentException e) {
            if (e.getCause() instanceof ErrorMessage) {
                for (ObjectError err: ((ErrorMessage) e.getCause()).getErrors()) {
                    errorMessage.addError(err);
                }
            } else {
                errorMessage.addError(new ObjectError("file", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            }
            return CompletableFuture.failedFuture( new IllegalArgumentException("File is not a valid excel file", errorMessage));
        } catch (Exception e) {
            errorMessage.addError(new ObjectError("file", "NotValid"));
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            logger.error("Error parsing the processed data file", e);
            return CompletableFuture.failedFuture( new IllegalArgumentException("File is not a valid excel file", errorMessage));
        }
    }
    
    ProcessedResultConfiguration createConfigForVersion (String fileFormat) {
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
        } else if (fileFormat.equalsIgnoreCase("CFG_V5.0") || fileFormat.equalsIgnoreCase("CFG_V5.1")) {
            config.setCvColumnId(10);
            config.setFeatureColumnId(7);
            config.setResultFileType("cfg");
            config.setRfuColumnId(8);
            config.setSheetNumber(0);
            config.setStDevColumnId(9);
            config.setStartRow(1);
        } else if (fileFormat.equalsIgnoreCase("CFG_V4.2")) {
            config.setCvColumnId(19);
            config.setFeatureColumnId(16);
            config.setResultFileType("cfg");
            config.setRfuColumnId(17);
            config.setSheetNumber(0);
            config.setStDevColumnId(18);
            config.setStartRow(3);
        } else if (fileFormat.equalsIgnoreCase("CFG_V4.1") || fileFormat.equalsIgnoreCase("CFG_V4.0") || fileFormat.equalsIgnoreCase("CFG_V3.1")) {
            config.setCvColumnId(30);
            config.setFeatureColumnId(27);
            config.setResultFileType("cfg");
            config.setRfuColumnId(28);
            config.setSheetNumber(0);
            config.setStDevColumnId(29);
            config.setStartRow(3);
        } else if (fileFormat.equalsIgnoreCase("CFG_V3.2")) {
            config.setCvColumnId(30);
            config.setFeatureColumnId(27);
            config.setResultFileType("cfg");
            config.setRfuColumnId(28);
            config.setSheetNumber(1);
            config.setStDevColumnId(29);
            config.setStartRow(3);
        } else if (fileFormat.equalsIgnoreCase("CFG_V3.0")) {
            config.setCvColumnId(30);
            config.setFeatureColumnId(26);
            config.setResultFileType("cfg");
            config.setRfuColumnId(27);
            config.setSheetNumber(1);
            config.setStDevColumnId(28);
            config.setStartRow(3);
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
    public CompletableFuture<String> importSlideLayout(SlideLayout slideLayout, File libraryFile, ErrorMessage errorMessage,
            UserEntity user) {
        try {
            slideLayout = GlygenArrayController.getFullLayoutFromLibrary(libraryFile, slideLayout, templateRepository, false);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
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
                                            String glycanId = addService.addGlycan(g, user, true, false);
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
                                                if (feature.getType() == FeatureType.CONTROL || feature.getType() == FeatureType.LANDING_LIGHT) {
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
                            
                            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                                return CompletableFuture.failedFuture(new IllegalArgumentException("error adding molecules! Reason: " + errorMessage.toString(), errorMessage));
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
                    
                    if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                        return CompletableFuture.failedFuture(new IllegalArgumentException("error adding features! Reason: " + errorMessage.toString(), errorMessage));
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
            return CompletableFuture.failedFuture(new IllegalArgumentException("error adding block layouts! Reason: " + errorMessage.toString(), errorMessage));
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
                return CompletableFuture.failedFuture(new IllegalArgumentException("error adding slide layout", errorMessage));
            }
        }
    }
    
    @Override
    @Async("GlygenArrayAsyncExecutor")
    public CompletableFuture<Confirmation> addGlycansFromExportFile(byte[] contents, Boolean noGlytoucanRegistration,
            UserEntity user, ErrorMessage errorMessage) {
        //BatchGlycanUploadResult result = new BatchGlycanUploadResult();
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
                    String id = addService.addGlycan(glycan, user, noGlytoucanRegistration, true);
                    if (id != null) {
                        //glycan.setId(id);
                       // glycan.setUri(GlygenArrayRepositoryImpl.uriPrefix + id);
                        //if (glycan instanceof SequenceDefinedGlycan) {
                        //    byte[] image = GlygenArrayController.getCartoonForGlycan(glycan.getId(), imageLocation);
                       //     glycan.setCartoon(image);
                       // }
                        //result.getAddedGlycans().add(glycan);
                        countSuccess ++;
                    } else {
                        // error
                        String sequence = null;
                        if (glycan instanceof SequenceDefinedGlycan) {
                            sequence = ((SequenceDefinedGlycan) glycan).getSequence();
                        }
                        
                        String[] codes = new String[] {i+"", sequence};
                        errorMessage.addError(new ObjectError("sequence", codes, null, glycan.getName() + " not added"));
                        //result.addWrongSequence(glycan.getName(), i, sequence, "not added, id is null");
                    }
                } catch (Exception e) {
                    logger.error ("Exception adding the glycan: " + glycan.getName(), e);
                    if (e.getCause() instanceof ErrorMessage) {
                        if (((ErrorMessage)e.getCause()).toString().contains("Duplicate")) {
                            ErrorMessage error = (ErrorMessage)e.getCause();
                            if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                                ObjectError err = error.getErrors().get(0);
                                if (err.getCodes() != null && err.getCodes().length != 0) {
                                    //Glycan duplicateGlycan = new Glycan();
                                   // try {
                                    //    duplicateGlycan = glycanRepository.getGlycanById(err.getCodes()[0], user);
                                    //    if (duplicateGlycan instanceof SequenceDefinedGlycan) {
                                    //        byte[] image = GlygenArrayController.getCartoonForGlycan(duplicateGlycan.getId(), imageLocation);
                                    //        duplicateGlycan.setCartoon(image);
                                    //    }
                                        String[] codes = new String[] {i+"", err.getCodes()[0]};
                                        errorMessage.addError(new ObjectError ("duplicate", codes, null, glycan.getName() + " is a duplicate"));
                                        //result.addDuplicateSequence(duplicateGlycan);
                                   // } catch (SparqlException | SQLException e1) {
                                  //      logger.error("Error retrieving duplicate glycan", e1);
                                   // }
                                } else {
                                    String[] codes = new String[] {i+""};
                                    errorMessage.addError(new ObjectError ("duplicate", codes, null, glycan.getName() + " is a duplicate"));
                                    //result.addDuplicateSequence(glycan);
                                }
                            } 
                        } else {
                            String[] codes = new String[] {i+""};
                            errorMessage.addError(new ObjectError("sequence", codes, null, ((ErrorMessage)e.getCause()).toString()));
                            //result.addWrongSequence(glycan.getName(), i, null, ((ErrorMessage)e.getCause()).toString());
                        }
                    } else { 
                        String[] codes = new String[] {i+""};
                        errorMessage.addError(new ObjectError("sequence", codes, null, e.getMessage()));
                        //result.addWrongSequence(glycan.getName(), i, null, e.getMessage());
                    }
                }
            }
            
            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Errors in the upload process", errorMessage));
            }
         
            //result.setSuccessMessage(countSuccess + " out of " + inputArray.length() + " glycans are added");
            Confirmation confirmation = new Confirmation (countSuccess + " out of " + inputArray.length() + " glycans are added", HttpStatus.OK.value());
            return CompletableFuture.completedFuture(confirmation);
        } catch (IOException | JSONException e) {
            errorMessage.addError(new ObjectError("file", "NotValid"));
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            return CompletableFuture.failedFuture(new IllegalArgumentException("File is not acceptable", errorMessage));
        }
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    @Async("GlygenArrayAsyncExecutor")
    public CompletableFuture<Confirmation> addGlycanFromLibraryFile (byte[] contents, Boolean noGlytoucanRegistration, UserEntity user, ErrorMessage errorMessage) {
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
                        String[] codes = new String[] {"Row: " + (count+1)};
                        errorMessage.addError(new ObjectError("sequence", codes, null, (glycan.getName() == null ? glycan.getId()+"" : glycan.getName()) + " Not a glycan"));
                        
                        //result.addWrongSequence ((glycan.getName() == null ? glycan.getId()+"" : glycan.getName()), count, null, "Not a glycan");
                  
                        continue;
                    } else if (glycan.getOrigSequence() == null || (glycan.getOrigSequence() != null && glycan.getOriginalSequenceType().equalsIgnoreCase("other"))) {  
                        // unknown glycan
                        view = new UnknownGlycan();
                    } 
                    if (glycan.getFilterSetting() != null) {
                        // there is a glycan with composition, TODO we don't handle that right now
                        String[] codes = new String[] {"Row: " + (count+1), glycan.getSequence()};
                        errorMessage.addError(new ObjectError("sequence", codes, null, (glycan.getName() == null ? glycan.getId()+"" : glycan.getName()) + " Glycan Type not supported"));
                        
                        //result.addWrongSequence((glycan.getName() == null ? glycan.getId()+"" : glycan.getName()), count, glycan.getSequence(), "Glycan Type not supported");
                 
                        continue;
                    }
                } else {
                    view = new SequenceDefinedGlycan();
                    ((SequenceDefinedGlycan) view).setGlytoucanId(glycan.getGlyTouCanId());
                    ((SequenceDefinedGlycan) view).setSequence(glycan.getSequence().trim());
                    ((SequenceDefinedGlycan) view).setSequenceType(GlycanSequenceFormat.GLYCOCT);
                }
                if (view == null) {
                    String[] codes = new String[] {"Row: " + (count+1), glycan.getSequence()};
                    errorMessage.addError(new ObjectError("sequence", codes, null, (glycan.getName() == null ? glycan.getId()+"" : glycan.getName()) + " not added"));
                    //result.addWrongSequence((glycan.getName() == null ? glycan.getId()+"" : glycan.getName()), count , glycan.getSequence(), "Not a glycan");
                    continue;
                }
                try {
                    view.setInternalId(glycan.getId()+ "");
                    view.setName(glycan.getName());
                    view.setDescription(glycan.getComment());   
                    String id = addService.addGlycan(view, user, noGlytoucanRegistration, false);
                    //Glycan addedGlycan = glycanRepository.getGlycanById(id, user);
                    //if (addedGlycan instanceof SequenceDefinedGlycan) {
                    //    byte[] image = GlygenArrayController.getCartoonForGlycan(addedGlycan.getId(), imageLocation);
                    //    addedGlycan.setCartoon(image);
                    //}
                    //result.getAddedGlycans().add(addedGlycan);
                    countSuccess ++;
                } catch (Exception e) {
                    if (e.getCause() instanceof ErrorMessage) {
                        if (((ErrorMessage)e.getCause()).toString().contains("Duplicate")) {
                            ErrorMessage error = (ErrorMessage)e.getCause();
                            if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                                ObjectError err = error.getErrors().get(0);
                                if (err.getCodes() != null && err.getCodes().length != 0) {
                                    //Glycan duplicateGlycan = new Glycan();
                                   // try {
                                   //     duplicateGlycan = glycanRepository.getGlycanById(err.getCodes()[0], user);
                                    //    if (duplicateGlycan instanceof SequenceDefinedGlycan) {
                                     //       byte[] image = GlygenArrayController.getCartoonForGlycan(duplicateGlycan.getId(), imageLocation);
                                    //        duplicateGlycan.setCartoon(image);
                                   //     }
                                        String[] codes = new String[] {"Row: " + (count+1), err.getCodes()[0]};
                                        errorMessage.addError(new ObjectError ("duplicate", codes, null, (glycan.getName() == null ? glycan.getId()+"" : glycan.getName()) + " is a duplicate"));
                                        //result.addDuplicateSequence(duplicateGlycan);
                                   // } catch (SparqlException | SQLException e1) {
                                  //      logger.error("Error retrieving duplicate glycan", e1);
                                  //  }
                                }
                            } 
                        } else {
                            String[] codes = new String[] {"Row: " + (count+1), glycan.getSequence()};
                            errorMessage.addError(new ObjectError("sequence", codes, null, (glycan.getName() == null ? glycan.getId()+"" : glycan.getName()) + "-" + ((ErrorMessage)e.getCause()).toString()));
                            //result.addWrongSequence((glycan.getName() == null ? glycan.getId()+"" : glycan.getName()), count , glycan.getSequence(), ((ErrorMessage)e.getCause()).toString());
                        }
                    } else { 
                        logger.error ("Exception adding the glycan: " + glycan.getName(), e);
                        String[] codes = new String[] {"Row: " + (count+1), glycan.getSequence()};
                        errorMessage.addError(new ObjectError("sequence", codes, null, (glycan.getName() == null ? glycan.getId()+"" : glycan.getName()) + "-" + ((ErrorMessage)e.getCause()).toString()));
                        
                        //result.addWrongSequence((glycan.getName() == null ? glycan.getId()+"" : glycan.getName()), count , glycan.getSequence(), ((ErrorMessage)e.getCause()).toString());
                    }
                } 
            }
            stream.close();
            
            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Errors in the upload process", errorMessage));
            }
            //result.setSuccessMessage(countSuccess + " out of " + count + " glycans are added");
            logger.info("Processed the file. " + countSuccess + " out of " + count + " glycans are added" );
            Confirmation confirmation = new Confirmation (countSuccess + " out of " + count + " glycans are added", HttpStatus.OK.value());
            return CompletableFuture.completedFuture(confirmation);
        } catch (Exception e) {
            errorMessage.addError(new ObjectError("file", "NotValid"));
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            return CompletableFuture.failedFuture(new IllegalArgumentException("File is not valid.", errorMessage));
        } 
    }
    
    @Override
    @Async("GlygenArrayAsyncExecutor")
    public CompletableFuture<Confirmation> addGlycanFromCSVFile (byte[] contents, Boolean noGlytoucanRegistration, UserEntity user, ErrorMessage errorMessage, ParserConfiguration config) {
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
                            String[] codes = new String[] {"Row: " + (count+1)};
                            errorMessage.addError(new ObjectError("sequence", codes, null, glycanName + "-" + e.getMessage()));
                            //result.addWrongSequence(glycanName, count, null, e.getMessage());
                        }
                    } else {
                        // ERROR
                        // add to error list
                        String[] codes = new String[] {"Row: " + (count+1), sequence};
                        errorMessage.addError(new ObjectError("sequence", codes, null, glycanName + " No sequence and mass information found"));
                        //result.addWrongSequence(glycanName, count, sequence, "No sequence and mass information found");
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
                    String id = addService.addGlycan(glycan, user, noGlytoucanRegistration, false);
                    //Glycan addedGlycan = glycanRepository.getGlycanById(id, user);
                    //if (addedGlycan instanceof SequenceDefinedGlycan) {
                    //    byte[] image = GlygenArrayController.getCartoonForGlycan(addedGlycan.getId(), imageLocation);
                    //    addedGlycan.setCartoon(image);
                   // }
                    //result.getAddedGlycans().add(addedGlycan);
                    countSuccess ++;
                } catch (Exception e) {
                    logger.error ("Exception adding the glycan: " + glycan.getName(), e);
                    if (e.getCause() instanceof ErrorMessage) {
                        if (((ErrorMessage)e.getCause()).toString().contains("Duplicate")) {
                            ErrorMessage error = (ErrorMessage)e.getCause();
                            if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                                ObjectError err = error.getErrors().get(0);
                                if (err.getCodes().length != 0) {
                                   // Glycan duplicateGlycan = new Glycan();
                                   // try {
                                    //    duplicateGlycan = glycanRepository.getGlycanById(err.getCodes()[0], user);
                                   //     if (duplicateGlycan instanceof SequenceDefinedGlycan) {
                                    //        byte[] image = GlygenArrayController.getCartoonForGlycan(duplicateGlycan.getId(), imageLocation);
                                    //        duplicateGlycan.setCartoon(image);
                                   //     }
                                        String[] codes = new String[] {"Row: " + (count+1), err.getCodes()[0]};
                                        errorMessage.addError(new ObjectError ("duplicate", codes, null, (glycan.getName() == null ? glycan.getId()+"" : glycan.getName()) + " is a duplicate"));
                                     
                                   //     result.addDuplicateSequence(duplicateGlycan);
                                  //  } catch (SparqlException | SQLException e1) {
                                  //      logger.error("Error retrieving duplicate glycan", e1);
                                  //  }
                                }
                            } 
                        } else {
                            String[] codes = new String[] {"Row: " + (count+1), sequence};
                            errorMessage.addError(new ObjectError("sequence", codes, null, glycanName + " " + ((ErrorMessage)e.getCause()).toString()));
                            //result.addWrongSequence(glycan.getName(), count, sequence, ((ErrorMessage)e.getCause()).toString());
                        }
                    } else { 
                        String[] codes = new String[] {"Row: " + (count+1), sequence};
                        errorMessage.addError(new ObjectError("sequence", codes, null, glycanName + " " + e.getMessage()));
                        //result.addWrongSequence(glycan.getName(), count, sequence, e.getMessage());
                    }
                } 
            }
            scan.close();
            stream.close();
            
            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Errors in the upload process", errorMessage));
            }
            Confirmation confirmation = new Confirmation (countSuccess + " out of " + count + " glycans are added", HttpStatus.OK.value());
            return CompletableFuture.completedFuture(confirmation);
        } catch (IOException e) {
            errorMessage.addError(new ObjectError("file", "NotValid"));
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            return CompletableFuture.failedFuture(new IllegalArgumentException("File is not valid.", errorMessage));
        }
    }
    
    @Override
    @Async("GlygenArrayAsyncExecutor")
    public CompletableFuture<Confirmation> addGlycanFromTextFile (byte[] contents, Boolean noGlytoucanRegistration, UserEntity user, ErrorMessage errorMessage, String format, String delimeter) {
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
                            ArrayList<String> errorCodes = new ArrayList<>();
                            for (ObjectError err: e.getErrors()) {
                                if (err.getCodes() != null)
                                    errorCodes.addAll(Arrays.asList(err.getCodes()));
                            }
                            String[] codes = errorCodes.toArray(new String[errorCodes.size()+1]);
                            codes[codes.length-1] = "Row: " + (count+1);
                            errorMessage.addError(new ObjectError("sequence", codes, null, e.getErrors().get(0).getDefaultMessage()));
                            //result.addWrongSequence(null, count, sequence, e.getErrors().get(0).getDefaultMessage());
                        } else {
                            String[] codes = new String[] {"Row: " + (count+1), sequence};
                            errorMessage.addError(new ObjectError("sequence", codes, null, "Cannot parse the sequence"));
                            //result.addWrongSequence(null, count, sequence, "Cannot parse the sequence");
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
                            //Glycan glycan = glycanRepository.getGlycanById(id, user);
                            //if (glycan instanceof SequenceDefinedGlycan) {
                            //    byte[] image = GlygenArrayController.getCartoonForGlycan(glycan.getId(), imageLocation);
                            //    glycan.setCartoon(image);
                           // }
                           // if (glycan != null) {
                                String[] codes = new String[] {"Row: " + (count+1), id};
                                errorMessage.addError(new ObjectError ("duplicate", codes, null, "Glycan is a duplicate"));
                                //result.addDuplicateSequence(glycan);
                            //} else {
                           //     logger.warn ("the duplicate glycan cannot be retrieved back: " + id);
                          //  }
                        } else {
                            String id = addService.addGlycan(g, user, noGlytoucanRegistration, false);
                            if (id == null) {
                                // cannot be added
                                String[] codes = new String[] {"Row: " + (count+1), sequence};
                                errorMessage.addError(new ObjectError("sequence", codes, null, "Cannot parse the sequence"));
                                //result.addWrongSequence(null, count, sequence, "Cannot parse the sequence");
                            } else {
                                countSuccess ++; 
                            }
                        }
                    }
                }
                catch (SparqlException e) {
                    // cannot add glycan
                    stream.close();
                    return CompletableFuture.failedFuture(new GlycanRepositoryException("Glycans cannot be added. Reason: " + e.getMessage()));
                } catch (Exception e) {
                    logger.error ("Exception adding the sequence: " + sequence, e);
                    // sequence is not valid
                    String[] codes = new String[] {"Row: " + (count+1), sequence};
                    errorMessage.addError(new ObjectError("sequence", codes, null, e.getMessage()));
                    //result.addWrongSequence(null, count, sequence, e.getMessage());
                }
            }
            stream.close();
            
            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Errors in the upload process", errorMessage));
            }
            Confirmation confirmation = new Confirmation (countSuccess + " out of " + count + " glycans are added", HttpStatus.OK.value());
            return CompletableFuture.completedFuture(confirmation);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("File is not valid. Reason: " + e.getMessage()));
        }
    }
    
    @Override
    @Async("GlygenArrayAsyncExecutor")
    public CompletableFuture<Confirmation> addFeaturesFromExportFile(byte[] contents, UserEntity user, ErrorMessage errorMessage) {
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
                    // clean up existing URIs for glycans/linkers/metadata
                    Map<Object, String> newPositions = AddToRepositoryServiceImpl.cleanFeature (feature);
                    String id = addService.importFeature(feature, newPositions, user);
                    feature.setId(id);
                    //result.getAddedFeatures().add(feature);
                    countSuccess ++;
                } catch (Exception e) {
                    logger.error ("Exception adding the feature: " + feature.getName(), e);
                    if (e.getCause() instanceof ErrorMessage) {
                        if (((ErrorMessage)e.getCause()).toString().contains("Duplicate")) {
                            ErrorMessage error = (ErrorMessage)e.getCause();
                            if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                                ObjectError err = error.getErrors().get(0);
                                if (err.getCodes() != null && err.getCodes().length != 0) {
                                    //try {
                                        //org.glygen.array.persistence.rdf.Feature duplicate = featureRepository.getFeatureById(err.getCodes()[0], user);
                                        //result.getDuplicateFeatures().add(duplicate);
                                        String[] codes = new String[] {"Row: " + (i+1), err.getCodes()[0]};
                                        errorMessage.addError(new ObjectError ("duplicate", codes, null, feature.getInternalId() + " is a duplicate"));
                                     
                                    //} catch (SparqlException | SQLException e1) {
                                    //    logger.error("Error retrieving duplicate feature", e1);
                                    //}
                                } else {
                                    String[] codes = new String[] {"Row: " + (i+1)};
                                    errorMessage.addError(new ObjectError ("duplicate", codes, null, feature.getInternalId() + " is a duplicate"));
                                    //result.getDuplicateFeatures().add(feature);
                                }
                            } 
                        } else {
                            ErrorMessage error = (ErrorMessage)e.getCause();
                            if (error.getErrors() != null) {
                                for (ObjectError err: error.getErrors()) {
                                    errorMessage.addError(err);
                                }
                            }
                            //result.getErrors().add((ErrorMessage)e.getCause());
                        }
                    } else { 
                        String[] codes = new String[] {"Row: " + (i+1)};
                        errorMessage.addError(new ObjectError ("duplicate", codes, null, e.getMessage()));
                        //ErrorMessage error = new ErrorMessage(e.getMessage());
                        //result.getErrors().add(error);
                    }
                }
            }
            
            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Errors in the upload process", errorMessage));
            }
         
            return CompletableFuture.completedFuture(new Confirmation(countSuccess + " out of " + inputArray.length() + " features are added", HttpStatus.OK.value()));
        } catch (IOException | JSONException e) {
            return CompletableFuture.failedFuture( new IllegalArgumentException("File is not valid. Reason: " + e.getMessage()));
        }
    }

    @Override
    @Async("GlygenArrayAsyncExecutor")
    public CompletableFuture<Confirmation> addLinkersFromExportFile(byte[] contents, LinkerType type, 
            UserEntity user, ErrorMessage errorMessage) {
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
                    String[] codes = new String[] {"selected type=" + type.name(), "type in file=" + linker.getType().name(), "linker=" + linker.getName()};
                    errorMessage.addError(new ObjectError("type", codes, null, "NotValid"));
                } else {
                    try {  
                        String id = addService.addLinker(linker, linker.getType().name().contains("UNKNOWN"), user);
                        linker.setId(id);
                        //result.getAddedLinkers().add(linker);
                        countSuccess ++;
                    } catch (Exception e) {
                        if (e.getCause() instanceof ErrorMessage) {
                            if (((ErrorMessage)e.getCause()).toString().contains("Duplicate")) {
                                ErrorMessage error = (ErrorMessage)e.getCause();
                                if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                                    ObjectError err = error.getErrors().get(0);
                                    if (err.getCodes() != null && err.getCodes().length != 0) {
                                        //try {
                                        //    Linker duplicate = linkerRepository.getLinkerById(err.getCodes()[0], user);
                                        //    result.getDuplicateLinkers().add(duplicate);
                                        //} catch (SparqlException | SQLException e1) {
                                       //     logger.error("Error retrieving duplicate linker", e1);
                                        //}
                                        String[] codes = new String[] {"Row: " + (i+1), err.getCodes()[0]};
                                        errorMessage.addError(new ObjectError ("duplicate", codes, null, linker.getName() + " is a duplicate"));
                                    } else {
                                        String[] codes = new String[] {"Row: " + (i+1)};
                                        errorMessage.addError(new ObjectError ("duplicate", codes, null, linker.getName() + " is a duplicate"));
                                        //result.getDuplicateLinkers().add(linker);
                                    }
                                } 
                            } else {
                                logger.error ("Exception adding the linker: " + linker.getName(), e);
                                ErrorMessage error = (ErrorMessage)e.getCause();
                                if (error.getErrors() != null) {
                                    for (ObjectError err: error.getErrors()) {
                                        errorMessage.addError(err);
                                    }
                                }
                                //result.getErrors().add((ErrorMessage)e.getCause());
                            }
                        } else { 
                            logger.error ("Exception adding the linker: " + linker.getName(), e);
                            //ErrorMessage error = new ErrorMessage(e.getMessage());
                            String[] codes = new String[] {"Row: " + (i+1)};
                            errorMessage.addError(new ObjectError ("duplicate", codes, null, e.getMessage()));
                            //result.getErrors().add(error);
                        }
                    }
                }
            }
         
            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Errors in the upload process", errorMessage));
            }
         
            return CompletableFuture.completedFuture(new Confirmation(countSuccess + " out of " + inputArray.length() + " linkers are added", HttpStatus.OK.value()));
        } catch (IOException | JSONException e) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("File is not acceptable", e));
        }
    }
}
