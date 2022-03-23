package org.glygen.array.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.data.FilterExclusionInfo;
import org.glygen.array.persistence.rdf.data.FilterExclusionReasonType;
import org.glygen.array.persistence.rdf.data.ProcessedData;
import org.glygen.array.persistence.rdf.data.TechnicalExclusionInfo;
import org.glygen.array.persistence.rdf.data.TechnicalExclusionReasonType;
import org.glygen.array.service.FeatureRepository;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.ObjectError;

public class ExclusionInfoParser {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    FeatureRepository featureRepository;
    
    public ExclusionInfoParser(FeatureRepository repo) {
        this.featureRepository = repo;
    }
    
    public ProcessedData parse (String filePath, UserEntity user) throws IOException, InvalidFormatException, SparqlException, SQLException {
        ProcessedData dummy = new ProcessedData();
        File file = new File(filePath);
        if (!file.exists())
            throw new FileNotFoundException(filePath + " does not exist!");
        
        List<ErrorMessage> errorList = new ArrayList<ErrorMessage>();
        
        Workbook workbook = WorkbookFactory.create(file);
        
        Sheet sheet1 = workbook.getSheet("Technical");
        if (sheet1 == null) {
            ErrorMessage error = new ErrorMessage();
            error.setErrorCode(ErrorCodes.INVALID_INPUT);
            error.setStatus(HttpStatus.BAD_REQUEST.value());
            error.addError(new ObjectError("Technical", "Sheet for technical exclusions not found"));
            errorList.add(error);
        }
        
        Sheet sheet2 = workbook.getSheet("Filtered data");
        if (sheet2 == null) {
            ErrorMessage error = new ErrorMessage();
            error.setErrorCode(ErrorCodes.INVALID_INPUT);
            error.setStatus(HttpStatus.BAD_REQUEST.value());
            error.addError(new ObjectError("Filtered data", "Sheet for filter exclusions not found")); 
            errorList.add(error);
        }
        
        Map<String, List<String>> technicalMap = parseRows (sheet1);
        List<TechnicalExclusionInfo> technicalExclusionList = new ArrayList<TechnicalExclusionInfo>();
        
        for (String reason: technicalMap.keySet()) {
            List<String> featureList = technicalMap.get(reason);
            if (featureList == null || featureList.isEmpty()) 
                continue;
            List<Feature> features = new ArrayList<Feature>();
            TechnicalExclusionInfo info = new TechnicalExclusionInfo();
            info.setFeatures(features);
            TechnicalExclusionReasonType reasonType = null;
            try {
                reasonType = TechnicalExclusionReasonType.valueOf(reason);
            } catch (Exception e) {
                // ignore, that means it is not one of the enumerated reasons
            }
            if (reasonType == null) {
                reason = reason.replaceAll("_", " "); // put the spaces back
                info.setOtherReason(reason);
            } else {
                info.setReason(reasonType);
            }
            for (String featureId: featureList) {
                Feature feature = featureRepository.getFeatureByLabel(featureId, "gadr:has_internal_id", user);
                if (feature == null) {
                    ErrorMessage error = new ErrorMessage();
                    error.setErrorCode(ErrorCodes.INVALID_INPUT);
                    error.setStatus(HttpStatus.BAD_REQUEST.value());
                    String[] codes = new String[] {featureId};
                    error.addError(new ObjectError("feature", codes, null, "NotFound")); 
                    errorList.add(error);
                } else {
                    features.add(feature);
                }
            }
            technicalExclusionList.add(info);
        }
        
        Map<String, List<String>> filterMap = parseRows (sheet2);
        List<FilterExclusionInfo> filterExclusionList = new ArrayList<FilterExclusionInfo>();
        
        for (String reason: filterMap.keySet()) {
            List<String> featureList = filterMap.get(reason);
            if (featureList == null || featureList.isEmpty()) 
                continue;
            List<Feature> features = new ArrayList<Feature>();
            FilterExclusionInfo info = new FilterExclusionInfo();
            info.setFeatures(features);
            FilterExclusionReasonType reasonType = null;
            try {
                reasonType = FilterExclusionReasonType.valueOf(reason);
            } catch (Exception e) {
                // ignore, that means it is not one of the enumerated reasons
            }
            if (reasonType == null) {
                reason = reason.replaceAll("_", " "); // put the spaces back
                info.setOtherReason(reason);
            } else {
                info.setReason(reasonType);
            }
            for (String featureId: featureList) {
                Feature feature = featureRepository.getFeatureByLabel(featureId, "gadr:has_internal_id", user);
                if (feature == null) {
                    ErrorMessage error = new ErrorMessage();
                    error.setErrorCode(ErrorCodes.INVALID_INPUT);
                    error.setStatus(HttpStatus.BAD_REQUEST.value());
                    String[] codes = new String[] {featureId};
                    error.addError(new ObjectError("feature", codes, null, "NotFound")); 
                    errorList.add(error);
                } else {
                    features.add(feature);
                }
            }
            filterExclusionList.add(info);
        }
        
        if (errorList.isEmpty()) {
            dummy.setTechnicalExclusions(technicalExclusionList);
            dummy.setFilteredDataList(filterExclusionList);
        } else {
            ErrorMessage error = new ErrorMessage("Errors parsing exclusion information file");
            error.setErrorCode(ErrorCodes.PARSE_ERROR);
            error.setStatus(HttpStatus.BAD_REQUEST.value());
            
            for (ErrorMessage e: errorList) {
                for (ObjectError o: e.getErrors()) {
                    error.addError(o);
                }
            }
            throw new IllegalArgumentException(error.getMessage(), error);
        }
        return dummy;
    }
    
    
    Map<String, List<String>> parseRows (Sheet sheet) {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        Map <Integer, String> reasonCols = new HashMap<>();
        if (sheet != null) {
            // read the first row for the reason list
            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) {
                Row headerRow = rowIterator.next();
                Iterator<Cell> cellIterator = headerRow.cellIterator();
                int col = 0;
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                        String reason = cell.getStringCellValue().trim();
                        reason = reason.replaceAll(" ", "_");
                        map.put (reason, new ArrayList<String>());
                        reasonCols.put(col, reason);
                        col++;
                    }
                }
            }
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Iterator<Cell> cellIterator = row.cellIterator();
                int col = 0;
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                        String repoId = cell.getStringCellValue().trim();
                        if (!repoId.isEmpty()) {
                            if (reasonCols.get(col) != null && map.get(reasonCols.get(col)) != null)
                                map.get(reasonCols.get(col)).add(repoId);
                        }
                    }
                    col++;
                }
            }
        }
        
        return map;
    }

}
