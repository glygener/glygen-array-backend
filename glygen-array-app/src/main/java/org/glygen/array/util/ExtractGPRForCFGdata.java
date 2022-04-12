package org.glygen.array.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtractGPRForCFGdata {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    public static void main(String[] args) {
        if (args.length < 1) {
            logger.error("need to pass processed file folder as argument");
            return;
        }
        
        // read all folders in the given folder
        File dataFolder = new File (args[0]);
        if (!dataFolder.isDirectory()) {
            logger.error(args[2] + " is not a folder");
            return;
        }
        
        String[] datasetFolders = dataFolder.list();
        for (String experimentName: datasetFolders) {
            File experimentFolder = new File (dataFolder + File.separator + experimentName);
            if (experimentFolder.isDirectory()) {
                String processedDataFile = null;
                String rawDataFile = null;
                for (String filename: experimentFolder.list()) {
                    if (filename.endsWith(".xls")) {
                        processedDataFile = filename;
                    } else if (filename.endsWith(".txt") || filename.endsWith(".gpr"))
                        rawDataFile = filename;
                }
                if (processedDataFile != null && rawDataFile == null) {
                    // try to see if the processedDataFile contains the GPR sheet
                    try {
                        extractGPR (experimentFolder.getAbsolutePath() + File.separator + processedDataFile);
                    } catch (InvalidFormatException | IOException e) {
                        logger.error("could not extract GPR for " + experimentFolder.getAbsolutePath(), e);
                    } catch (Exception e) {
                        logger.error("could not extract GPR for " + experimentFolder.getAbsolutePath(), e);
                    }
                }
            }
        }
    }

    private static void extractGPR(String processedDataFile) throws InvalidFormatException, IOException {
        File file = new File(processedDataFile);
        if (!file.exists()) 
            throw new FileNotFoundException(processedDataFile + " does not exist!");
        
        //Create Workbook instance holding reference to .xls file
        Workbook workbook = WorkbookFactory.create(file);
        Sheet sheet = workbook.getSheetAt(workbook.getNumberOfSheets()-1);
        // to check if sheet contains GPR file
        boolean gpr = false;
        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Cell cell = row.getCell(0);
            if (cell != null && cell.getCellType() == Cell.CELL_TYPE_STRING) {
                String firstCell = cell.getStringCellValue();
                if (firstCell != null && firstCell.toLowerCase().equals("begin header")) {
                    // GPR file
                    gpr = true;
                }
            }
        }
        
        if (gpr) {
            String outputFile = processedDataFile.substring(0, processedDataFile.lastIndexOf(".")) + ".txt";
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
            // save the sheet as a tab delimited text file
            for(int rn=0; rn<sheet.getLastRowNum(); rn++) {
                StringBuffer rowString = new StringBuffer();
                Row row = sheet.getRow(rn);
                if (row == null) {
                    out.println();
                } else {
                    for(int cn=0; cn<row.getLastCellNum(); cn++) {
                        // If the cell is missing from the file, generate a blank one
                        // (Works by specifying a MissingCellPolicy)
                        Cell cell = row.getCell(cn, Row.CREATE_NULL_AS_BLANK);
                        if (cell.getCellType() == Cell.CELL_TYPE_BLANK) {
                            rowString.append("\t");
                        } else if (cell.getCellType() == Cell.CELL_TYPE_BOOLEAN){
                            rowString.append(cell.getBooleanCellValue() + "\t");
                        } else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC){
                            rowString.append(cell.getNumericCellValue() + "\t");
                        } else if (cell.getCellType() == Cell.CELL_TYPE_STRING){
                            rowString.append(cell.getStringCellValue() + "\t");
                        }
                    }
                    out.println(rowString);
                }
            }
            out.close();
            logger.info("extracted GPR " + outputFile);
        } 
    }
}
