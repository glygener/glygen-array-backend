package org.glygen.array.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.glygen.array.persistence.rdf.data.ArrayDataset;
import org.glygen.array.persistence.rdf.metadata.Description;
import org.glygen.array.persistence.rdf.metadata.Descriptor;
import org.glygen.array.persistence.rdf.metadata.DescriptorGroup;
import org.glygen.array.persistence.rdf.metadata.MetadataCategory;

public class MetadataImportExportUtil {
    
    public void exportIntoExcel (ArrayDataset dataset, String outputFile) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        createMetadataSheet (dataset.getSample(), "Sample", workbook);
        
        FileOutputStream os = new FileOutputStream(outputFile);
        workbook.write(os);
        os.close();
        
    }
    
    public void createMetadataSheet (MetadataCategory metadata, String sheetName, Workbook workbook) {
        Sheet sheet = workbook.createSheet(sheetName);
        int idx = 0;
        Row header = sheet.createRow(idx++);
        header.createCell(0, Cell.CELL_TYPE_STRING);
        header.createCell(1, Cell.CELL_TYPE_STRING);
        header.createCell(2, Cell.CELL_TYPE_STRING);
        Cell cell4 = header.createCell(3, Cell.CELL_TYPE_STRING);
        cell4.setCellValue("Value");
        Cell cell5 = header.createCell(4, Cell.CELL_TYPE_STRING);
        cell5.setCellValue("Unit");
        Cell cell6 = header.createCell(5, Cell.CELL_TYPE_STRING);
        cell6.setCellValue("Mirage?");
        
        for (Description description: metadata.getDescriptors()) {
            idx = addMetadataRow(description, sheet, idx, 0);
        }
        
        for (Description description: metadata.getDescriptorGroups()) {
            idx = addMetadataRow(description, sheet, idx, 0);
        }
    }
    
    int addMetadataRow (Description description, Sheet sheet, int rowIdx, int level) {
        Row row = sheet.createRow(rowIdx++);
        Cell level1 = row.createCell(0, Cell.CELL_TYPE_STRING);
        Cell level2 = row.createCell(1, Cell.CELL_TYPE_STRING);
        Cell level3 = row.createCell(2, Cell.CELL_TYPE_STRING);
        Cell value = row.createCell(3, Cell.CELL_TYPE_STRING);
        Cell unit = row.createCell(4, Cell.CELL_TYPE_STRING);
        Cell mirage = row.createCell(5, Cell.CELL_TYPE_BOOLEAN);
        
        if (level == 0) {
            level1.setCellValue(description.getName());
        } else if (level == 1) {
            level2.setCellValue(description.getName());
        } else if (level == 2) {
            level3.setCellValue(description.getName());
        }
        if (description instanceof Descriptor) {
            if (description.getNotApplicable()) {
                value.setCellValue("Not Applicable");
            }
            if (description.getNotRecorded()) {
                value.setCellValue("Not Recorded");
            }
            if (((Descriptor) description).getValue() != null && !((Descriptor) description).getValue().isEmpty()) 
                value.setCellValue(((Descriptor) description).getValue());
            if (((Descriptor) description).getUnit() != null) {
                unit.setCellValue(((Descriptor) description).getUnit());
            }
        }
        if (description.getKey() != null) {
            mirage.setCellValue(description.getKey().isMirage());
        }
        
        if (description instanceof DescriptorGroup) {
            for (Description sub: ((DescriptorGroup) description).getDescriptors()) {
                rowIdx = addMetadataRow(sub, sheet, rowIdx, level+1);
            }
        }
        
        return rowIdx;
    }

}
