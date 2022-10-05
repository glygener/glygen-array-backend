package org.glygen.array.util.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.LinkerType;
import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.data.Intensity;
import org.glygen.array.persistence.rdf.data.ProcessedData;
import org.glygen.array.service.FeatureRepository;
import org.glygen.array.service.GlycanRepository;
import org.glygen.array.service.LayoutRepository;
import org.glygen.array.service.LinkerRepository;
import org.glygen.array.util.SequenceUtils;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.grits.toolbox.glycanarray.om.parser.cfg.CFGMasterListParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.ObjectError;

public class ProcessedDataParser {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    LayoutRepository layoutRepository;
    GlycanRepository glycanRepository;
    LinkerRepository linkerRepository;
    FeatureRepository featureRepository;
    
    Map<String, String> sequenceErrorMap = new HashMap<String, String>();
    
    Map<String, String> glycanURICache = new HashMap<String, String>();
    Map<String, Linker> linkerCache = new HashMap<String, Linker>();
    Map<String, Feature> featureCache = new HashMap<String, Feature>();
    
    public ProcessedDataParser(FeatureRepository f, LayoutRepository r, GlycanRepository g, LinkerRepository l) {
        this.featureRepository = f;
        this.layoutRepository = r;
        this.glycanRepository = g;
        this.linkerRepository = l;
    }
    
    public List<Intensity> parse (String filePath, String errorMapFilePath, ProcessedResultConfiguration config, UserEntity user) throws InvalidFormatException, IOException {
        if (config == null)
            throw new InvalidFormatException("Configuration is not given. This version is not being supported!");
        
        List<Intensity> intensities = new ArrayList<>();
        
        if ((config.getFeatureColumnId() == null && config.getFeatureNameColumnId() == null) || 
                (config.getFeatureColumnId() == -1 && config.getFeatureNameColumnId() == -1))
            throw new InvalidFormatException("Feature column must be specified");
        
        List<ErrorMessage> errorList = new ArrayList<ErrorMessage>();
        
        File file = new File(filePath);
        if (!file.exists()) 
            throw new FileNotFoundException(filePath + " does not exist!");
        
        File errorMapFile = new File (errorMapFilePath);
        if (errorMapFile.exists()) {
            readErrorMapFile (errorMapFile);
        }
        
        //Create Workbook instance holding reference to .xls file
        Workbook workbook = WorkbookFactory.create(file);
        Sheet sheet = null;
        // get the sheet with the masterlist numbers and structures
        if (config.getSheetNumber() != null && config.getSheetNumber() != -1) {
            sheet = workbook.getSheetAt(config.getSheetNumber());
        } else if (config.getSheetName() != null) {
            sheet = workbook.getSheet(config.getSheetName());
        }
        
        if (sheet == null) {
            throw new InvalidFormatException("The required sheet is not found");
        }
        //Iterate through each row one by one
        Iterator<Row> rowIterator = sheet.iterator();
        for (int i=0; i < config.getStartRow(); i++)
            rowIterator.next();      // skip to the data row
        
        boolean start = false;
        
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Cell featureCell = null;
            Cell groupCell = null;
            if (config.getFeatureColumnId() != null && config.getFeatureColumnId() != -1) 
                featureCell = row.getCell(config.getFeatureColumnId());
            else if (config.getFeatureNameColumnId() != null && config.getFeatureNameColumnId() != -1) 
                featureCell = row.getCell(config.getFeatureNameColumnId());
            
            if (config.getGroupColumnId() != null && config.getGroupColumnId() != -1) 
                groupCell = row.getCell(config.getGroupColumnId());
            
            
            Cell rfuCell = row.getCell(config.getRfuColumnId());
            Cell stDevCell = row.getCell(config.getStDevColumnId());
            
            if (!start && (rfuCell == null || stDevCell == null || featureCell == null)) {
                ErrorMessage error = new ErrorMessage();
                error.setErrorCode(ErrorCodes.PARSE_ERROR);
                error.setStatus(HttpStatus.BAD_REQUEST.value());
                error.addError(new ObjectError("config", "Error parsing the file, the configuration does not seem to work")); 
                throw new IllegalArgumentException(error);
            } else if (start && (rfuCell == null || stDevCell == null || featureCell == null))  {// data ends
                break;
            }
            
            start = true;
            Cell cvCell = null;
            if (config.getCvColumnId() != null && config.getCvColumnId() != -1) 
                cvCell = row.getCell(config.getCvColumnId());
            Intensity intensity = new Intensity();
            intensity.setRfu(rfuCell.getNumericCellValue());
            intensity.setStDev(stDevCell.getNumericCellValue());
            if (cvCell != null && cvCell.getCellType() == Cell.CELL_TYPE_NUMERIC) 
                intensity.setPercentCV(cvCell.getNumericCellValue());
            
            if (config.getResultFileType().equalsIgnoreCase("CFG")) {
                // feature column contains the CFG name for the glycans, no glycopeptides or mixtures
                String featureString = featureCell.getStringCellValue().trim(); 
                // parse sequence and find the glycan with the given sequence
                // parse linker and find the linker with the given name
                // find the feature with given glycan and linker
                List<ErrorMessage> parserErrors = new ArrayList<ErrorMessage>();
                String glycoCT = parseSequence (featureString, parserErrors);
                if (glycoCT == null) {
                    // check errorMapFile
                    String modified = sequenceErrorMap.get(featureString.trim());
                    if ( modified != null && !modified.isEmpty()) {
                        if (modified.startsWith("RES")) {
                            // already glycoCT
                            glycoCT = modified;
                        }
                        else {
                            glycoCT = parseSequence (modified, parserErrors);
                        }
                        if (glycoCT == null) {
                            // add to error list
                            appendErrorMapFile(errorMapFilePath, featureString.trim());
                            ErrorMessage error = new ErrorMessage("Error retrieving glycan for row" + row.getRowNum());
                            error.setErrorCode(ErrorCodes.PARSE_ERROR);
                            error.setStatus(HttpStatus.BAD_REQUEST.value());
                            String[] codes = {row.getRowNum()+""};
                            error.addError(new ObjectError("sequence", codes, null, "Row " + row.getRowNum() + ": glycan with the sequence " + featureString + " cannot be translated to an existing glycan in the repository"));
                            for (ErrorMessage err: parserErrors) {
                                for (ObjectError o: err.getErrors()) {
                                    error.addError(o);
                                }
                            }
                            errorList.add(error);
                        }
                    } else if (modified == null){
                        // add to error list
                        appendErrorMapFile(errorMapFilePath, featureString.trim());
                        ErrorMessage error = new ErrorMessage("Error retrieving glycan for row" + row.getRowNum());
                        error.setErrorCode(ErrorCodes.PARSE_ERROR);
                        error.setStatus(HttpStatus.BAD_REQUEST.value());
                        String[] codes = {row.getRowNum()+""};
                        error.addError(new ObjectError("sequence", codes, null, "Row " + row.getRowNum() + ": glycan with the sequence " + featureString + " cannot be translated to an existing glycan in the repository"));
                        for (ErrorMessage err: parserErrors) {
                            for (ObjectError o: err.getErrors()) {
                                error.addError(o);
                            }
                        }
                        errorList.add(error);
                    }
                }
                
                if (glycoCT == null)
                    continue;
                
                String linkerName = SequenceUtils.getLinker(featureString);
                String glycanURI = glycanURICache.get(glycoCT.trim());
                try {
                    if (glycanURI == null) {
                        glycanURI = glycanRepository.getGlycanBySequence(glycoCT.trim(), user);
                        if (glycanURI != null) {
                            glycanURICache.put(glycoCT.trim(), glycanURI);
                        } else {
                            // find an alternative sequence from the error map
                            // check errorMapFile
                            String modified = sequenceErrorMap.get(featureString.trim());
                            if ( modified != null && !modified.isEmpty()) {
                                if (modified.startsWith("RES")) {
                                    // already glycoCT
                                    glycoCT = modified;
                                }
                                else {
                                    glycoCT = parseSequence (modified, parserErrors);
                                }
                            } 
                            if (glycoCT != null) {
                                glycanURI = glycanRepository.getGlycanBySequence(glycoCT.trim(), user);
                                if (glycanURI != null) {
                                    glycanURICache.put(glycoCT.trim(), glycanURI);
                                }
                            }
                        }
                    } 
                    if (glycanURI == null) {
                        ErrorMessage error = new ErrorMessage("Error retrieving glycan for row" + row.getRowNum());
                        error.setErrorCode(ErrorCodes.PARSE_ERROR);
                        error.setStatus(HttpStatus.BAD_REQUEST.value());
                        String[] codes = {row.getRowNum()+""};
                        error.addError(new ObjectError("sequence", codes, null, "Row " + row.getRowNum() + ": glycan with the sequence " + featureString + " cannot be found in the repository"));
                        errorList.add(error);
                    } else {
                        //Glycan glycan = glycanRepository.getGlycanFromURI(glycanURI, user);
                        Glycan glycan = new Glycan();
                        glycan.setUri(glycanURI);
                        Linker linker = linkerCache.get(linkerName.trim());
                        if (linker == null) {
                            linker = linkerRepository.getLinkerByLabel(linkerName.trim(), LinkerType.SMALLMOLECULE, user);
                            if (linker != null)
                                linkerCache.put(linkerName.trim(), linker);
                            else {
                                linker = linkerRepository.getLinkerByLabel(linkerName+"B", LinkerType.SMALLMOLECULE, user);
                                if (linker != null)
                                    linkerCache.put(linkerName+"B", linker);
                            }
                        }
                        if (linker == null) {
                            // error
                            ErrorMessage error = new ErrorMessage("Linker cannot be found in the repository for row: " + row.getRowNum());
                            error.setErrorCode(ErrorCodes.PARSE_ERROR);
                            error.setStatus(HttpStatus.BAD_REQUEST.value());
                            String[] codes = {row.getRowNum()+""};
                            error.addError(new ObjectError("linker", codes, null, "Row " + row.getRowNum() + ": linker " + linkerName + " cannot be found in the repository"));
                            errorList.add(error); 
                            continue;
                        }
                        String key = glycan.getUri() + linker.getUri();
                        Feature feature = featureCache.get(key);
                        if (feature == null) {
                            feature = featureRepository.getFeatureByGlycanLinker(glycan, linker, config.getSlideLayoutUri(), null, user);
                            if (feature != null)
                                featureCache.put(key, feature);
                        }
                        if (feature == null) {
                            // try finding as SpxxB
                            linker = linkerCache.get(linkerName+"B");
                            if (linker == null) {
                                linker = linkerRepository.getLinkerByLabel(linkerName+"B", LinkerType.SMALLMOLECULE, user);
                                if (linker != null)
                                    linkerCache.put(linkerName+"B", linker);
                            }
                            if (linker != null) {
                                key = glycan.getUri() + linker.getUri();
                                feature = featureCache.get(key);
                                if (feature == null) {
                                    feature = featureRepository.getFeatureByGlycanLinker(glycan, linker, config.getSlideLayoutUri(), null, user);
                                    if (feature != null)
                                        featureCache.put(key, feature);
                                }
                            }
                            if (feature == null) {
                                ErrorMessage error = new ErrorMessage("Row " + row.getRowNum() + ": feature with the sequence " + featureString + " cannot be found in the repository");
                                error.setErrorCode(ErrorCodes.PARSE_ERROR);
                                error.setStatus(HttpStatus.BAD_REQUEST.value());
                                String[] codes = {row.getRowNum()+""};
                                error.addError(new ObjectError("feature", codes, null, "Row " + row.getRowNum() + ": feature with the sequence " + featureString + " cannot be found in the repository"));
                                errorList.add(error); 
                            } else {
                                List<Feature> features = new ArrayList<Feature>();
                                features.add(feature);
                                List<Spot> spots = layoutRepository.getSpotByFeatures(features, config.getSlideLayoutUri(), null, user);
                                /*if (spots != null) {
                                    for (Spot spot: spots) {
                                        spot.setBlockLayoutUri(config.getBlockLayoutUri());
                                    }
                                }*/
                                intensity.setSpots(spots);
                            }
                        } else {
                            List<Feature> features = new ArrayList<Feature>();
                            features.add(feature);
                            List<Spot> spots = layoutRepository.getSpotByFeatures(features, config.getSlideLayoutUri(), null, user);
                            /*if (spots != null) {
                                for (Spot spot: spots) {
                                    spot.setBlockLayoutUri(config.getBlockLayoutUri());
                                }
                            }*/
                            intensity.setSpots(spots);
                        }
                    }
                } catch (SparqlException | SQLException e) {
                    ErrorMessage error = new ErrorMessage("Repository exception:" + e.getMessage());
                    error.setErrorCode(ErrorCodes.PARSE_ERROR);
                    error.setStatus(HttpStatus.BAD_REQUEST.value());
                    error.addError(new ObjectError("feature", e.getMessage()));
                    errorList.add(error); 
                }
            } else { // repository format: repoID column contains the internal id of the feature in the repository
                String featureString = featureCell.getStringCellValue().trim(); 
                List<Feature> features = new ArrayList<Feature>();
                if (featureString.contains("||")) {
                    // mixture
                    String[] featureIds = featureString.split("\\|\\|");
                    for (String fId: featureIds) {
                        try {
                            Feature feature = featureRepository.getFeatureByLabel(fId, "gadr:has_internal_id", user);
                            if (feature == null) {
                                // error
                                ErrorMessage error = new ErrorMessage("Row " + row.getRowNum() + ": feature with the repo id " + fId + " cannot be found in the repository");
                                error.setErrorCode(ErrorCodes.PARSE_ERROR);
                                error.setStatus(HttpStatus.BAD_REQUEST.value());
                                String[] codes = {row.getRowNum()+""};
                                error.addError(new ObjectError("feature", codes, null, "Row " + row.getRowNum() + ": feature with the repo id " + fId + " cannot be found in the repository"));
                                errorList.add(error); 
                            } else {
                                features.add(feature);
                            }
                        } catch (SparqlException | SQLException e) {
                            ErrorMessage error = new ErrorMessage("Repository exception:" + e.getMessage());
                            error.setErrorCode(ErrorCodes.PARSE_ERROR);
                            error.setStatus(HttpStatus.BAD_REQUEST.value());
                            error.addError(new ObjectError("feature", e.getMessage()));
                            errorList.add(error); 
                        }
                    }
                    
                } else {
                    try {
                        Feature feature = featureRepository.getFeatureByLabel(featureString, "gadr:has_internal_id", user);
                        if (feature == null) {
                            // error
                            ErrorMessage error = new ErrorMessage("Row " + row.getRowNum() + ": feature with the repo id " + featureString + " cannot be found in the repository");
                            error.setErrorCode(ErrorCodes.PARSE_ERROR);
                            error.setStatus(HttpStatus.BAD_REQUEST.value());
                            String[] codes = {row.getRowNum()+""};
                            error.addError(new ObjectError("feature", codes, null, "Row " + row.getRowNum() + ": feature with the repo id " + featureString + " cannot be found in the repository"));
                            errorList.add(error); 
                        } else {
                            features.add(feature);
                        }
                    } catch (SparqlException | SQLException e) {
                        ErrorMessage error = new ErrorMessage("Repository exception:" + e.getMessage());
                        error.setErrorCode(ErrorCodes.PARSE_ERROR);
                        error.setStatus(HttpStatus.BAD_REQUEST.value());
                        error.addError(new ObjectError("feature", e.getMessage()));
                        errorList.add(error); 
                    }
                }
                try {
                    //use group information, if provided, to find the correct spots
                    String groupId = null;
                    if (groupCell != null) {
                        if (groupCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                            groupId = groupCell.getNumericCellValue() + "";
                        } else if (groupCell.getCellType() == Cell.CELL_TYPE_STRING){
                            groupId = groupCell.getStringCellValue().trim();
                        }
                    }
                    List<Spot> spots = layoutRepository.getSpotByFeatures(features, config.getSlideLayoutUri(), groupId, user);
                    /*if (spots != null) {
                        for (Spot spot: spots) {
                            spot.setBlockLayoutUri(config.getBlockLayoutUri());
                        }
                    }*/
                    intensity.setSpots(spots);
                } catch (SparqlException | SQLException e) {
                    ErrorMessage error = new ErrorMessage("Repository exception:" + e.getMessage());
                    error.setErrorCode(ErrorCodes.PARSE_ERROR);
                    error.setStatus(HttpStatus.BAD_REQUEST.value());
                    error.addError(new ObjectError("spot", e.getMessage()));
                    errorList.add(error); 
                }
            }
            intensities.add(intensity);
        }
        
        if (errorList.isEmpty())
            return intensities;
        else {
            ErrorMessage error = new ErrorMessage("Errors parsing processed data excel file");
            error.setErrorCode(ErrorCodes.PARSE_ERROR);
            error.setStatus(HttpStatus.BAD_REQUEST.value());
            
            for (ErrorMessage e: errorList) {
                for (ObjectError o: e.getErrors()) {
                    error.addError(o);
                }
            }
            //logger.error("Error parsing the excel file" + error.toString());
            throw new IllegalArgumentException(error.getMessage(), error);
        }
    }
    
    private void readErrorMapFile(File errorMapFile) throws FileNotFoundException {
        Scanner scanner = new Scanner(errorMapFile);
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            String[] sequences = line.split("\\t");
            if (sequences.length >= 2 && sequences[1].trim().length() > 0) {
                sequenceErrorMap.put(sequences[0].trim(), sequences[1].trim());
            } else if (sequences.length == 1) {
                sequenceErrorMap.put(sequences[0].trim(), "");
            }
        }
        scanner.close();
    }
    
    void appendErrorMapFile (String errorMapFilePath, String sequence) throws IOException {
     // Open given file in append mode. 
        BufferedWriter out = new BufferedWriter( 
               new FileWriter(errorMapFilePath, true)); 
        out.write(sequence); 
        out.write("\t");
        out.write("\n");
        out.close(); 
    }

    String parseSequence (String sequence, List<ErrorMessage> errors) {
        // parse the sequence
        try {
            CFGMasterListParser parser = new CFGMasterListParser();
            String glycanSequence = SequenceUtils.getSequence(sequence);
            String glycoCT = parser.translateSequence(glycanSequence);
            return glycoCT;
        } catch (Exception e) {
            ErrorMessage parseError = new ErrorMessage(e.getMessage());
            parseError.addError(new ObjectError("sequence", "Error parsing " + sequence + ". Reason: " + e.getMessage()));
            errors.add(parseError);
        }
        return null;
    }
    
    
    public static void exportToFile (ProcessedData processedData, String outputFile) throws IOException {
        // write to Excel
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Repo Data");
        int idx = 0;
        Row header = sheet.createRow(idx++);
        Cell cell1 = header.createCell(0, Cell.CELL_TYPE_STRING);
        cell1.setCellValue("ID");
        Cell cell2 = header.createCell(1, Cell.CELL_TYPE_STRING);
        cell2.setCellValue("RepoID");
        Cell cell3 = header.createCell(2, Cell.CELL_TYPE_STRING);
        cell3.setCellValue("GroupID");
        Cell cell4 = header.createCell(3, Cell.CELL_TYPE_STRING);
        cell4.setCellValue("RFU");
        Cell cell5 = header.createCell(4, Cell.CELL_TYPE_STRING);
        cell5.setCellValue("SD");
        
        for (Intensity intensity: processedData.getIntensity()) {
            for (Spot spot: intensity.getSpots()) {
                Row row = sheet.createRow(idx);
                cell1 = row.createCell(0, Cell.CELL_TYPE_NUMERIC);
                cell1.setCellValue(idx++);
                cell2 = row.createCell(1, Cell.CELL_TYPE_STRING);
                String featureString = "";
                int i=0;
                for (Feature f: spot.getFeatures()) {
                    featureString += f.getInternalId();
                    if (i < spot.getFeatures().size()-1) {
                        featureString += "||";
                    }
                    i++;
                }
                cell2.setCellValue(featureString);
                cell3 = row.createCell(2, Cell.CELL_TYPE_STRING);
                cell3.setCellValue(spot.getGroup());
                cell4 = row.createCell(3, Cell.CELL_TYPE_STRING);
                cell4.setCellValue(intensity.getRfu());
                cell5 = row.createCell(4, Cell.CELL_TYPE_STRING);
                cell5.setCellValue(intensity.getStDev());
                break;
            }
        }
        
        /*ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            workbook.write(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();*/
        
        FileOutputStream os = new FileOutputStream(outputFile);
        workbook.write(os);
        os.close();
        
       /* PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
        //write header
        out.append("ID\tRepoID\tGroupID\tRFU\tSD\n");
        int idx = 1;
        for (Intensity intensity: processedData.getIntensity()) {
            for (Spot spot: intensity.getSpots()) {
                StringBuffer row = new StringBuffer();
                row.append(idx + "\t");
                String featureString = "";
                int i=0;
                for (Feature f: spot.getFeatures()) {
                    featureString += f.getInternalId();
                    if (i < spot.getFeatures().size()-1) {
                        featureString += "||";
                    }
                    i++;
                }
                row.append(featureString + "\t");
                row.append(spot.getGroup() + "\t");
                row.append(intensity.getRfu() + "\t");
                row.append(intensity.getStDev() + "\t");
                out.println(row);
                idx++;
                break;   // do not list all the spots. one intensity for one feature
            }
        }
        out.close();*/
    }

}
