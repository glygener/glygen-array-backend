package org.glygen.array.util.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
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
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.data.Intensity;
import org.glygen.array.persistence.rdf.data.ProcessedData;
import org.glygen.array.service.FeatureRepository;
import org.glygen.array.service.GlycanRepository;
import org.glygen.array.service.LinkerRepository;
import org.glygen.array.util.ExtendedGalFileParser;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.grits.toolbox.glycanarray.om.parser.cfg.CFGMasterListParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.ObjectError;

public class ProcessedDataParser {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    FeatureRepository featureRepository;
    GlycanRepository glycanRepository;
    LinkerRepository linkerRepository;
    
    Map<String, String> sequenceErrorMap = new HashMap<String, String>();
    
    public ProcessedDataParser(FeatureRepository f, GlycanRepository g, LinkerRepository l) {
        this.featureRepository = f;
        this.glycanRepository = g;
        this.linkerRepository = l;
    }
    
    public ProcessedData parse (String filePath, String errorMapFilePath, ProcessedResultConfiguration config, UserEntity user) throws InvalidFormatException, IOException {
        ProcessedData data = new ProcessedData();
        List<Intensity> intensities = new ArrayList<>();
        data.setIntensity(intensities);
        
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
        
        // get the sheet with the masterlist numbers and structures
        Sheet sheet = workbook.getSheetAt(config.getSheetNumber());
        //Iterate through each row one by one
        Iterator<Row> rowIterator = sheet.iterator();
        for (int i=0; i < config.getStartRow(); i++)
            rowIterator.next();      // skip to the data row
        
        boolean start = false;
        
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Cell featureCell = null;
            if (config.getFeatureColumnId() != null && config.getFeatureColumnId() != -1) 
                featureCell = row.getCell(config.getFeatureColumnId());
            else if (config.getFeatureNameColumnId() != null && config.getFeatureNameColumnId() != -1) 
                featureCell = row.getCell(config.getFeatureNameColumnId());
            
            Cell rfuCell = row.getCell(config.getRfuColumnId());
            Cell stDevCell = row.getCell(config.getStDevColumnId());
            
            if (!start && (rfuCell == null || stDevCell == null || featureCell == null)) {
                ErrorMessage error = new ErrorMessage();
                error.setErrorCode(ErrorCodes.INVALID_INPUT);
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
                String glycoCT = parseSequence (featureString, errorList);
                if (glycoCT == null) {
                    // check errorMapFile
                    String modified = sequenceErrorMap.get(featureString.trim());
                    if ( modified != null && !modified.isEmpty()) {
                        if (modified.startsWith("RES")) {
                            // already glycoCT
                            glycoCT = modified;
                        }
                        else {
                            glycoCT = parseSequence (modified, errorList);
                        }
                        if (glycoCT == null) {
                            // add to error list
                            appendErrorMapFile(errorMapFilePath, featureString.trim());
                        }
                    } else if (modified == null){
                        // add to error list
                        appendErrorMapFile(errorMapFilePath, featureString.trim());
                    }
                    continue;
                }
                String linkerName = ExtendedGalFileParser.getLinker(featureString);
                String glycanURI;
                try {
                    glycanURI = glycanRepository.getGlycanBySequence(glycoCT.trim(), user);
                    if (glycanURI == null) {
                        ErrorMessage error = new ErrorMessage("Error retrieving glycan for row" + row.getRowNum());
                        error.setErrorCode(ErrorCodes.INVALID_INPUT);
                        error.setStatus(HttpStatus.BAD_REQUEST.value());
                        error.addError(new ObjectError("sequence", "Row " + row.getRowNum() + ": glycan with the sequence " + featureString + "-glycoCT: " + glycoCT + " cannot be found in the repository"));
                        errorList.add(error);
                    } else {
                        //Glycan glycan = glycanRepository.getGlycanFromURI(glycanURI, user);
                        Glycan glycan = new Glycan();
                        glycan.setUri(glycanURI);
                        Linker linker = linkerRepository.getLinkerByLabel(linkerName.trim(), user);
                        if (linker == null) {
                            // error
                            ErrorMessage error = new ErrorMessage("Linker cannot be found in the repository for row: " + row.getRowNum());
                            error.setErrorCode(ErrorCodes.INVALID_INPUT);
                            error.setStatus(HttpStatus.BAD_REQUEST.value());
                            
                            error.addError(new ObjectError("linker", "Row " + row.getRowNum() + ": linker " + linkerName + " cannot be found in the repository"));
                            errorList.add(error); 
                            continue;
                        }
                        Feature feature = featureRepository.getFeatureByGlycanLinker(glycan, linker, user);
                        if (feature == null) {
                            // try finding as SpxxB
                            linker = linkerRepository.getLinkerByLabel(linkerName+"B", user);
                            feature = featureRepository.getFeatureByGlycanLinker(glycan, linker, user);
                            if (feature == null) {
                                ErrorMessage error = new ErrorMessage("Row " + row.getRowNum() + ": feature with the sequence " + featureString + " cannot be found in the repository");
                                error.setErrorCode(ErrorCodes.INVALID_INPUT);
                                error.setStatus(HttpStatus.BAD_REQUEST.value());
                                error.addError(new ObjectError("feature", "Row " + row.getRowNum() + ": feature with the sequence " + featureString + " cannot be found in the repository"));
                                errorList.add(error); 
                            } else {
                                intensity.setFeature(feature);
                            }
                        } else {
                            intensity.setFeature(feature);
                        }
                    }
                } catch (SparqlException | SQLException e) {
                    ErrorMessage error = new ErrorMessage("Repository exception:" + e.getMessage());
                    error.setErrorCode(ErrorCodes.INVALID_INPUT);
                    error.setStatus(HttpStatus.BAD_REQUEST.value());
                    error.addError(new ObjectError("feature", e.getMessage()));
                    errorList.add(error); 
                }
            } else { // feature column contains the name of the feature
                // GLAD? // Imperial?
                // TODO
                
                // Imperial extract concentration level
                
            }
            intensities.add(intensity);
        }
        
        if (errorList.isEmpty())
            return data;
        else {
            ErrorMessage error = new ErrorMessage("Errors parsing processed data excel file");
            error.setErrorCode(ErrorCodes.INVALID_INPUT);
            error.setStatus(HttpStatus.BAD_REQUEST.value());
            System.out.println("Errors:");
            for (ErrorMessage e: errorList) {
                for (ObjectError o: e.getErrors()) {
                    System.out.println(o.getDefaultMessage());
                    error.addError(o);
                }
            }
            throw new IllegalArgumentException(error);
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
            String glycanSequence = ExtendedGalFileParser.getSequence(sequence);
            String glycoCT = parser.translateSequence(glycanSequence);
            return glycoCT;
        } catch (Exception e) {
            logger.error("Sequence parse error", e);
        }
        return null;
    }

}