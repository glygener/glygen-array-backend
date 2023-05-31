package org.glygen.array.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.glygen.array.persistence.rdf.Publication;
import org.glygen.array.persistence.rdf.data.ArrayDataset;
import org.glygen.array.persistence.rdf.data.Image;
import org.glygen.array.persistence.rdf.data.ProcessedData;
import org.glygen.array.persistence.rdf.data.RawData;
import org.glygen.array.persistence.rdf.data.Slide;
import org.glygen.array.persistence.rdf.metadata.Description;
import org.glygen.array.persistence.rdf.metadata.Descriptor;
import org.glygen.array.persistence.rdf.metadata.DescriptorGroup;
import org.glygen.array.persistence.rdf.metadata.MetadataCategory;

public class MetadataImportExportUtil {
    
    String baseURL = "http://localhost:3000"; // base URL of the repository application
    String privateURL = "experiments/editExperiment/";
    String publicURL = "data/dataset/";
    
    CellStyle boldStyle;
    CellStyle wrapTextStyle;
    private XSSFCellStyle hlinkstyle;
    
    public MetadataImportExportUtil() {
    }
    
    public MetadataImportExportUtil(String baseURL) {
        this.baseURL = baseURL;
    }
    
    public void exportIntoExcel (ArrayDataset dataset, String outputFile) throws IOException {
        exportIntoExcel(dataset, outputFile, false, false);
    }
    
    public void exportIntoExcel (ArrayDataset dataset, String outputFile, Boolean mirageOnly, Boolean singleSheet) throws IOException {
        if (singleSheet == null)
            singleSheet = false;
        if (mirageOnly == null)
            mirageOnly = false;
        
        Workbook workbook = new XSSFWorkbook();
        
        XSSFFont font= (XSSFFont) workbook.createFont();
        font.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
        font.setBold(true);
        font.setItalic(false);
        boldStyle = workbook.createCellStyle();
        boldStyle.setFont(font);
        
        Map<String, Integer> rowNumbers = createDatasetSheet (dataset, "DatasetInfo", workbook, mirageOnly, singleSheet);
        
        if (singleSheet) {
            Map<String, Integer> linkRows = createSingleSheet (dataset, workbook, mirageOnly);
            addHyperLinks(workbook, workbook.getSheet("DatasetInfo"), singleSheet, rowNumbers, linkRows);
        } else {
            addHyperLinks(workbook, workbook.getSheet("DatasetInfo"), singleSheet, rowNumbers, null);
            Sheet sheet = workbook.createSheet("Sample");
            createMetadataSheet (dataset.getSample(), sheet, "Sample", workbook, mirageOnly, 0);
            
            Set<String> alreadyCreatedSheets = new HashSet<>();
            for (Slide slide: dataset.getSlides()) {
                if (slide.getMetadata() != null) {
                    String sheetName = "Assay-" + slide.getMetadata().getName();
                    if (!alreadyCreatedSheets.contains(sheetName)) {
                        alreadyCreatedSheets.add(sheetName);
                        sheet = workbook.createSheet(sheetName);
                        createMetadataSheet(slide.getMetadata(), sheet, sheetName, workbook, mirageOnly, 0);
                    }
                }
                if (slide.getPrintedSlide().getMetadata() != null) {
                    String sheetName = "Slide-" + slide.getPrintedSlide().getMetadata().getName();
                    if (!alreadyCreatedSheets.contains(sheetName)) {
                        alreadyCreatedSheets.add(sheetName);
                        sheet = workbook.createSheet(sheetName);
                        createMetadataSheet(slide.getPrintedSlide().getMetadata(), sheet, sheetName, workbook, mirageOnly, 0);
                    }
                }
                if (slide.getPrintedSlide().getPrinter() != null) {
                    String sheetName = "Printer-" + slide.getPrintedSlide().getPrinter().getName();
                    if (!alreadyCreatedSheets.contains(sheetName)) {
                        alreadyCreatedSheets.add(sheetName);
                        sheet = workbook.createSheet(sheetName);
                        createMetadataSheet(slide.getPrintedSlide().getPrinter(), sheet, sheetName, workbook, mirageOnly, 0);
                    }
                }
                if (slide.getPrintedSlide().getPrintRun() != null) {
                    String sheetName = "PrintRun-" + slide.getPrintedSlide().getPrintRun().getName();
                    if (!alreadyCreatedSheets.contains(sheetName)) {
                        alreadyCreatedSheets.add(sheetName);
                        sheet = workbook.createSheet(sheetName);
                        createMetadataSheet(slide.getPrintedSlide().getPrintRun(), sheet, sheetName, workbook, mirageOnly, 0);
                    }
                }
                
                for (Image image: slide.getImages()) {
                    if (image.getScanner() != null) {
                        String sheetName = "Scanner-" + image.getScanner().getName();
                        if (!alreadyCreatedSheets.contains(sheetName)) {
                            alreadyCreatedSheets.add(sheetName);
                            sheet = workbook.createSheet(sheetName);
                            createMetadataSheet(image.getScanner(), sheet, sheetName, workbook, mirageOnly, 0);
                        }
                    }
                    
                    for (RawData rawData: image.getRawDataList()) {
                        if (rawData.getMetadata() != null) {
                            String sheetName = "ImageAnalysis-" + rawData.getMetadata().getName();
                            if (!alreadyCreatedSheets.contains(sheetName)) {
                                alreadyCreatedSheets.add(sheetName);
                                sheet = workbook.createSheet(sheetName);
                                createMetadataSheet(rawData.getMetadata(), sheet, sheetName, workbook, mirageOnly, 0);
                            }
                        }
                        
                        for (ProcessedData processed: rawData.getProcessedDataList()) {
                            if (processed.getMetadata() != null) {
                                String sheetName = "DataProcessing-" + processed.getMetadata().getName();
                                if (!alreadyCreatedSheets.contains(sheetName)) {
                                    alreadyCreatedSheets.add(sheetName);
                                    sheet = workbook.createSheet(sheetName);
                                    createMetadataSheet(processed.getMetadata(), sheet, sheetName, workbook, mirageOnly, 0);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        FileOutputStream os = new FileOutputStream(outputFile);
        workbook.write(os);
        os.close();
        
    }
    
    private Map<String, Integer> createSingleSheet(ArrayDataset dataset, Workbook workbook, Boolean mirageOnly) {
        Map<String, Integer> linkRows = new HashMap<>();
        Sheet sheet = workbook.createSheet("Metadata");
        // first put Sample
        Row headline1 = sheet.createRow(0);
        Cell cell1 = headline1.createCell(0);
        cell1.setCellValue("1.  Sample: Glycan Binding Sample");
        cell1.setCellStyle(boldStyle);
        headline1.createCell(1);
        headline1.createCell(2);
        headline1.createCell(3);
        headline1.createCell(4);
        headline1.createCell(5);
        headline1.createCell(6);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
        Row headline2 = sheet.createRow(1);
        cell1 = headline2.createCell(0);
        cell1.setCellValue("Sample-" + dataset.getSample().getName());
        cell1.setCellStyle(boldStyle);
        headline2.createCell(1);
        headline2.createCell(2);
        headline2.createCell(3);
        headline2.createCell(4);
        headline2.createCell(5);
        headline2.createCell(6);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));
        
        int rownum = createMetadataSheet(dataset.getSample(), sheet, "Sample", workbook, mirageOnly, 2);
        linkRows.put("Sample", 2);
        
        Set<String> alreadyCreatedSheets = new HashSet<>();
        // get all "Assay metadata"
        for (Slide slide: dataset.getSlides()) {
            if (slide.getMetadata() != null) {
                String sheetName = "Assay-" + slide.getMetadata().getName();
                if (!alreadyCreatedSheets.contains(sheetName)) {
                    alreadyCreatedSheets.add(sheetName);
                    linkRows.put(sheetName, rownum+1);
                    rownum = createMetadataSheet(slide.getMetadata(), sheet, sheetName, workbook, mirageOnly, rownum);
                }
            }
        }
        
        headline1 = sheet.createRow(rownum);
        cell1 = headline1.createCell(0);
        cell1.setCellValue("3. Printing Surface; e.g., Microarray Slide");
        cell1.setCellStyle(boldStyle);
        headline1.createCell(1);
        headline1.createCell(2);
        headline1.createCell(3);
        headline1.createCell(4);
        headline1.createCell(5);
        headline1.createCell(6);
        sheet.addMergedRegion(new CellRangeAddress(rownum, rownum, 0, 6));
        rownum++;
        // get all printrun
        for (Slide slide: dataset.getSlides()) {
            if (slide.getPrintedSlide() != null && slide.getPrintedSlide().getPrintRun() != null) {
                String sheetName = "PrintRun-" + slide.getPrintedSlide().getPrintRun().getName();
                if (!alreadyCreatedSheets.contains(sheetName)) {
                    alreadyCreatedSheets.add(sheetName);
                    linkRows.put(sheetName, rownum+1);
                    rownum = createMetadataSheet(slide.getPrintedSlide().getPrintRun(), sheet, sheetName, workbook, mirageOnly, rownum);
                }
            }
        }
        
        headline1 = sheet.createRow(rownum);
        cell1 = headline1.createCell(0);
        cell1.setCellValue("4. Arrayer (Printer)");
        cell1.setCellStyle(boldStyle);
        headline1.createCell(1);
        headline1.createCell(2);
        headline1.createCell(3);
        headline1.createCell(4);
        headline1.createCell(5);
        headline1.createCell(6);
        sheet.addMergedRegion(new CellRangeAddress(rownum, rownum, 0, 6));
        rownum++;
        
        // get all printer metadata
        for (Slide slide: dataset.getSlides()) {
            if (slide.getPrintedSlide().getPrinter() != null) {
                String sheetName = "Printer-" + slide.getPrintedSlide().getPrinter().getName();
                if (!alreadyCreatedSheets.contains(sheetName)) {
                    alreadyCreatedSheets.add(sheetName);
                    linkRows.put(sheetName, rownum+1);
                    rownum = createMetadataSheet(slide.getPrintedSlide().getPrinter(), sheet, sheetName, workbook, mirageOnly, rownum);
                }
            }
            
        }
        
        headline1 = sheet.createRow(rownum);
        cell1 = headline1.createCell(0);
        cell1.setCellValue("5. Glycan Microarray with “Map”");
        cell1.setCellStyle(boldStyle);
        headline1.createCell(1);
        headline1.createCell(2);
        headline1.createCell(3);
        headline1.createCell(4);
        headline1.createCell(5);
        headline1.createCell(6);
        sheet.addMergedRegion(new CellRangeAddress(rownum, rownum, 0, 6));
        rownum++;
        
        // get all slide metadata
        for (Slide slide: dataset.getSlides()) {
            if (slide.getPrintedSlide().getMetadata() != null) {
                String sheetName = "Slide-" + slide.getPrintedSlide().getMetadata().getName();
                if (!alreadyCreatedSheets.contains(sheetName)) {
                    alreadyCreatedSheets.add(sheetName);
                    linkRows.put(sheetName, rownum+1);
                    rownum = createMetadataSheet(slide.getPrintedSlide().getMetadata(), sheet, sheetName, workbook, mirageOnly, rownum);
                }
            }
        }
        
        headline1 = sheet.createRow(rownum);
        cell1 = headline1.createCell(0);
        cell1.setCellValue("6. Detector and Data Processing");
        cell1.setCellStyle(boldStyle);
        headline1.createCell(1);
        headline1.createCell(2);
        headline1.createCell(3);
        headline1.createCell(4);
        headline1.createCell(5);
        headline1.createCell(6);
        sheet.addMergedRegion(new CellRangeAddress(rownum, rownum, 0, 6));
        rownum++;
        
        // get all scanner and image analysis metadata
        for (Slide slide: dataset.getSlides()) {
            for (Image image: slide.getImages()) {
                if (image.getScanner() != null) {
                    String sheetName = "Scanner-" + image.getScanner().getName();
                    if (!alreadyCreatedSheets.contains(sheetName)) {
                        alreadyCreatedSheets.add(sheetName);
                        linkRows.put(sheetName, rownum+1);
                        rownum = createMetadataSheet(image.getScanner(), sheet, sheetName, workbook, mirageOnly, rownum);
                    }
                }
                
                for (RawData rawData: image.getRawDataList()) {
                    if (rawData.getMetadata() != null) {
                        String sheetName = "ImageAnalysis-" + rawData.getMetadata().getName();
                        if (!alreadyCreatedSheets.contains(sheetName)) {
                            alreadyCreatedSheets.add(sheetName);
                            linkRows.put(sheetName, rownum+1);
                            rownum = createMetadataSheet(rawData.getMetadata(), sheet, sheetName, workbook, mirageOnly, rownum);
                        }
                    }
                }
            }
        }
        
        headline1 = sheet.createRow(rownum);
        cell1 = headline1.createCell(0);
        cell1.setCellValue("7. Glycan Microarray Data Presentation");
        cell1.setCellStyle(boldStyle);
        headline1.createCell(1);
        headline1.createCell(2);
        headline1.createCell(3);
        headline1.createCell(4);
        headline1.createCell(5);
        headline1.createCell(6);
        sheet.addMergedRegion(new CellRangeAddress(rownum, rownum, 0, 6));
        rownum++;
        
        // get all data processing metadata
        for (Slide slide: dataset.getSlides()) {
            for (Image image: slide.getImages()) {
                for (RawData rawData: image.getRawDataList()) {
                    for (ProcessedData processed: rawData.getProcessedDataList()) {
                        if (processed.getMetadata() != null) {
                            String sheetName = "DataProcessing-" + processed.getMetadata().getName();
                            if (!alreadyCreatedSheets.contains(sheetName)) {
                                alreadyCreatedSheets.add(sheetName);
                                linkRows.put(sheetName, rownum+1);
                                rownum = createMetadataSheet(processed.getMetadata(), sheet, sheetName, workbook, mirageOnly, rownum);
                            }
                        }
                    }
                }
            }
        }
        
        
        return linkRows;   
    }
    
    void addHyperLinks (Workbook workbook, Sheet sheet, Boolean singleSheet, Map<String, Integer> rowNumbers, Map<String, Integer> linkRows) {
        CreationHelper createHelper = workbook.getCreationHelper();
        if (!singleSheet) {
            Row sampleRow = sheet.getRow(7);
            Cell cell = sampleRow.getCell(1);
            XSSFHyperlink link = (XSSFHyperlink)createHelper.createHyperlink(XSSFHyperlink.LINK_DOCUMENT);
            link.setAddress("'Sample'!A1");
            cell.setHyperlink((XSSFHyperlink) link);
            cell.setCellStyle(hlinkstyle);
            
            for (String sheetName: rowNumbers.keySet()) {
                Row row = sheet.getRow(rowNumbers.get(sheetName));
                cell = row.getCell(4);
                link = (XSSFHyperlink)createHelper.createHyperlink(XSSFHyperlink.LINK_DOCUMENT);
                link.setAddress("'" + sheetName + "'!A1");
                cell.setHyperlink((XSSFHyperlink) link);
                cell.setCellStyle(hlinkstyle);
            }
        } else {
            Row sampleRow = sheet.getRow(7);
            Cell cell = sampleRow.getCell(1);
            XSSFHyperlink link = (XSSFHyperlink)createHelper.createHyperlink(XSSFHyperlink.LINK_DOCUMENT);
            link.setAddress("'Metadata'!A" + linkRows.get("Sample"));
            cell.setHyperlink((XSSFHyperlink) link);
            cell.setCellStyle(hlinkstyle);
            
            for (String sheetName: rowNumbers.keySet()) {
                Row row = sheet.getRow(rowNumbers.get(sheetName));
                cell = row.getCell(4);
                link = (XSSFHyperlink)createHelper.createHyperlink(XSSFHyperlink.LINK_DOCUMENT);
                link.setAddress("'Metadata'!A" + linkRows.get(sheetName));
                cell.setHyperlink((XSSFHyperlink) link);
                cell.setCellStyle(hlinkstyle);
            }
        }
    }

    private Map<String, Integer> createDatasetSheet(ArrayDataset dataset, String sheetName, Workbook workbook, Boolean mirageOnly, Boolean singleSheet) {
        Map<String, Integer> rowNumbers = new HashMap<String, Integer>();
        Sheet sheet = workbook.createSheet(sheetName);
        CreationHelper createHelper = workbook.getCreationHelper();
        wrapTextStyle = workbook.createCellStyle();
        wrapTextStyle.setWrapText(true);
        
        hlinkstyle = ((XSSFWorkbook)workbook).createCellStyle();
        XSSFFont hlinkfont = ((XSSFWorkbook)workbook).createFont();
        hlinkfont.setUnderline(XSSFFont.U_SINGLE);
        hlinkfont.setColor(IndexedColors.BLUE.index);
        hlinkstyle.setFont(hlinkfont);
        
        Row name = sheet.createRow(0);
        Cell cell = name.createCell(0);
        cell.setCellValue("Name");
        cell.setCellStyle(boldStyle);
        cell = name.createCell(1);
        cell.setCellValue(dataset.getName());
        
        Row header = sheet.createRow(1);
        Cell headerCell = header.createCell(0);
        headerCell.setCellValue("Dataset URL");
        headerCell.setCellStyle(boldStyle);
        headerCell = header.createCell(1);
        XSSFHyperlink link = (XSSFHyperlink)createHelper.createHyperlink(XSSFHyperlink.LINK_URL);
        if (dataset.getIsPublic()) {
            if (dataset.getUri().contains("public")) {
                headerCell.setCellValue(baseURL + publicURL + dataset.getId());
                link.setAddress(baseURL + publicURL + dataset.getId());
            } else {
                headerCell.setCellValue(baseURL + publicURL + dataset.getPublicId());
                link.setAddress(baseURL + publicURL + dataset.getPublicId());
            }
        } else {
            headerCell.setCellValue(baseURL + privateURL + dataset.getId());
            link.setAddress(baseURL + privateURL + dataset.getId());
        }
       
        headerCell.setHyperlink((XSSFHyperlink) link);
        headerCell.setCellStyle(hlinkstyle);
        
        Row exportOptions = sheet.createRow(2);
        exportOptions.setRowStyle(wrapTextStyle);
        cell = exportOptions.createCell(0);
        cell.setCellValue("Export Options");
        cell.setCellStyle(boldStyle);
        cell = exportOptions.createCell(1);
        cell.setCellValue("Mirage Only? " + (mirageOnly != null && mirageOnly ? "Yes" : "No") + 
                " Single Sheet? " + (singleSheet != null && singleSheet ? "Yes" : "No"));
        
        Row description = sheet.createRow(3);
        description.setRowStyle(wrapTextStyle);
        cell = description.createCell(0);
        cell.setCellValue("Description");
        cell.setCellStyle(boldStyle);
        cell = description.createCell(1);
        cell.setCellValue(dataset.getDescription());
        cell = description.createCell(2);
        cell = description.createCell(3);
        cell = description.createCell(4);
        sheet.addMergedRegion(new CellRangeAddress(description.getRowNum(), description.getRowNum(), 1, 4));
        
        Row createdDate = sheet.createRow(4);
        cell = createdDate.createCell(0);
        cell.setCellValue("Submission Date");
        cell.setCellStyle(boldStyle);
        cell = createdDate.createCell(1);
        cell.setCellValue(dataset.getDateAddedToLibrary().toString());
        
        Row publicDate = sheet.createRow(5);
        cell = publicDate.createCell(0);
        cell.setCellValue("Release Date");
        cell.setCellStyle(boldStyle);
        cell = publicDate.createCell(1);
        if (dataset.getIsPublic()) {
            cell.setCellValue(dataset.getDateCreated().toString());
        }
        
        Row submitter = sheet.createRow(6);
        cell = submitter.createCell(0);
        cell.setCellValue("Submitted By");
        cell.setCellStyle(boldStyle);
        cell = submitter.createCell(1);
        if (dataset.getUser().getFirstName() != null && dataset.getUser().getLastName() != null)
            cell.setCellValue(dataset.getUser().getName() + ": " + dataset.getUser().getFirstName() + " " + dataset.getUser().getLastName());
        else 
            cell.setCellValue(dataset.getUser().getName());
        
        Row sample = sheet.createRow(7);
        cell = sample.createCell(0);
        cell.setCellValue("Sample");
        cell.setCellStyle(boldStyle);
        cell = sample.createCell(1);
        cell.setCellValue(dataset.getSample().getName());
        
        
        int rownum = 8;
        for (String keyword: dataset.getKeywords()) {
           Row row = sheet.createRow(rownum++);
           cell = row.createCell(0);
           cell.setCellValue("Keyword");
           cell.setCellStyle(boldStyle);
           cell = row.createCell(1);
           cell.setCellValue(keyword);
        }
        for (Publication pub: dataset.getPublications()) {
            Row row = sheet.createRow(rownum++);
            row.setRowStyle(wrapTextStyle);
            cell = row.createCell(0);
            cell.setCellValue("Publication");
            cell.setCellStyle(boldStyle);
            cell = row.createCell(1);
            StringBuffer pubString = new StringBuffer();
            pubString.append(pub.getTitle() + "\n");
            pubString.append(pub.getAuthors() + "\n");
            pubString.append(pub.getJournal());
            if (pub.getYear() != null) {
                pubString.append(" (" + pub.getYear() + ")");
            } 
            if (pub.getPubmedId() != null) 
                pubString.append("\nPMID: " + pub.getPubmedId());
            else if (pub.getDoiId() != null)
                pubString.append("\nDOI: " + pub.getDoiId());
            cell.setCellValue(pubString.toString());
        }
        
        Row row = sheet.createRow(rownum++);
        row = sheet.createRow(rownum++);
        cell = row.createCell(0);
        cell.setCellValue("Slide");
        cell.setCellStyle(boldStyle);
        cell = row.createCell(1);
        cell.setCellValue("Image");
        cell.setCellStyle(boldStyle);
        cell = row.createCell(2);
        cell.setCellValue("Raw data");
        cell.setCellStyle(boldStyle);
        cell = row.createCell(3);
        cell.setCellValue("Processed data");
        cell.setCellStyle(boldStyle);
        cell = row.createCell(4);
        cell.setCellValue("Metadata");
        cell.setCellStyle(boldStyle);
        // print out the hierarchy
        int slideCount = 1;
        for (Slide slide: dataset.getSlides()) {
            row = sheet.createRow(rownum++);
            cell = row.createCell(0);
            cell.setCellValue("Slide-" + slideCount + ": " + slide.getPrintedSlide().getName());
            cell = row.createCell(1);
            cell = row.createCell(2);
            cell = row.createCell(3);
            cell = row.createCell(4);
            if (slide.getMetadata() != null) {
                sheetName = "Assay-" + slide.getMetadata().getName();
                cell.setCellValue(sheetName);
                rowNumbers.put(sheetName, (rownum-1));
            } else {
                cell.setCellValue("No Assay Metadata provided");
            }
            row = sheet.createRow(rownum++);
            cell = row.createCell(0);
            cell = row.createCell(1);
            cell = row.createCell(2);
            cell = row.createCell(3);
            cell = row.createCell(4);
            if (slide.getPrintedSlide().getMetadata() != null) {
                sheetName = "Slide-" + slide.getPrintedSlide().getMetadata().getName();
                cell.setCellValue(sheetName);
                rowNumbers.put(sheetName, (rownum-1));
            } else {
                cell.setCellValue("No Slide Metadata provided");
            }
            row = sheet.createRow(rownum++);
            cell = row.createCell(0);
            cell = row.createCell(1);
            cell = row.createCell(2);
            cell = row.createCell(3);
            cell = row.createCell(4);
            if (slide.getPrintedSlide().getPrinter() != null) {
                sheetName = "Printer-" + slide.getPrintedSlide().getPrinter().getName();
                cell.setCellValue(sheetName);
                rowNumbers.put(sheetName, (rownum-1));
            } else {
                cell.setCellValue("No Printer Metadata provided");
            }
            row = sheet.createRow(rownum++);
            cell = row.createCell(0);
            cell = row.createCell(1);
            cell = row.createCell(2);
            cell = row.createCell(3);
            cell = row.createCell(4);
            if (slide.getPrintedSlide().getPrintRun() != null) {
                sheetName = "PrintRun-" + slide.getPrintedSlide().getPrintRun().getName();
                cell.setCellValue(sheetName);
                rowNumbers.put(sheetName, (rownum-1));
            } else {
                cell.setCellValue("No Printrun Metadata provided");
            }
            
            int imageCount = 1;
            for (Image image: slide.getImages()) {
                Row imageRow = sheet.createRow(rownum++);
                cell = imageRow.createCell(0);
                cell = imageRow.createCell(1);
                cell.setCellValue("Image-" + imageCount + ": " + (image.getFile() == null ? "No image provided" : image.getFile().getOriginalName()));
                cell = imageRow.createCell(2);
                cell = imageRow.createCell(3);
                cell = imageRow.createCell(4);
                if (image.getScanner() != null) {
                    sheetName = "Scanner-" + image.getScanner().getName();
                    cell.setCellValue(sheetName);
                    rowNumbers.put(sheetName, (rownum-1));
                } else {
                    cell.setCellValue("No Scanner Metadata provided");
                }
                int rawDataCount = 1;
                for (RawData rawData: image.getRawDataList()) {
                    Row rawdataRow = sheet.createRow(rownum++);
                    cell = rawdataRow.createCell(0);
                    cell = rawdataRow.createCell(1);
                    cell = rawdataRow.createCell(2);
                    cell.setCellValue("RawData-" + rawDataCount + ": " + (rawData.getFile() == null ? "No rawdata provided" : rawData.getFile().getOriginalName()));
                    cell = rawdataRow.createCell(3);
                    cell = rawdataRow.createCell(4);
                    if (rawData.getMetadata() != null) {
                        sheetName = "ImageAnalysis-" + rawData.getMetadata().getName();
                        cell.setCellValue(sheetName);
                        rowNumbers.put(sheetName, (rownum-1));
                    } else {
                        cell.setCellValue("No Image Analysis Metadata provided");
                    }
                    int processedCount=1;
                    for (ProcessedData processed: rawData.getProcessedDataList()) {
                        Row processedRow = sheet.createRow(rownum++);
                        cell = processedRow.createCell(0);
                        cell = processedRow.createCell(1);
                        cell = processedRow.createCell(2);
                        cell = processedRow.createCell(3);
                        cell.setCellValue("ProcessedData-" + processedCount + ": " + (processed.getFile() == null ? "No processed data provided" : processed.getFile().getOriginalName()));
                        cell = processedRow.createCell(4);
                        if (processed.getMetadata() != null) {
                            sheetName = "DataProcessing-" + processed.getMetadata().getName();
                            cell.setCellValue(sheetName);
                            rowNumbers.put(sheetName, (rownum-1));
                        } else {
                            cell.setCellValue("No Data Processing Software Metadata provided");
                        }
                        processedCount++;
                    }
                    rawDataCount++;
                }
                imageCount++;
            }
            slideCount++;
        }
        
        sheet.setColumnWidth(0, getColumnWidth(15f));
        sheet.setColumnWidth(1, getColumnWidth(28f));
        sheet.setColumnWidth(2, getColumnWidth(28f));
        sheet.setColumnWidth(3, getColumnWidth(28f));
        sheet.setColumnWidth(4, getColumnWidth(28f));
        
        return rowNumbers;
    }

    public int createMetadataSheet (MetadataCategory metadata, Sheet sheet, String sheetName, Workbook workbook, Boolean mirageOnly, int rownum) {
        if (mirageOnly == null) mirageOnly = false;
        try {
            Row headline = sheet.createRow(rownum);
            Cell cell = headline.createCell(0);
            cell.setCellValue(sheetName);
            cell.setCellStyle(boldStyle);
            headline.createCell(1);
            headline.createCell(2);
            headline.createCell(3);
            headline.createCell(4);
            headline.createCell(5);
            headline.createCell(6);
            sheet.addMergedRegion(new CellRangeAddress(rownum, rownum, 0, 6));
         
            return addMetadataSection (sheet, rownum+1, metadata, mirageOnly);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException (e.getMessage() + " sheet:" + sheetName);
        }
    }
    
    int addMetadataSection (Sheet sheet, int idx, MetadataCategory metadata, Boolean mirageOnly) {
        Row header = sheet.createRow(idx++);
        Cell cell1 = header.createCell(1, Cell.CELL_TYPE_STRING);
        cell1.setCellValue("Parameter 1");
        cell1.setCellStyle(boldStyle);
        Cell cell2 = header.createCell(2, Cell.CELL_TYPE_STRING);
        cell2.setCellValue("Parameter 2");
        cell2.setCellStyle(boldStyle);
        Cell cell3 = header.createCell(3, Cell.CELL_TYPE_STRING);
        cell3.setCellValue("Parameter 3");
        cell3.setCellStyle(boldStyle);
        Cell cell4 = header.createCell(4, Cell.CELL_TYPE_STRING);
        cell4.setCellValue("Value");
        cell4.setCellStyle(boldStyle);
        Cell cell5 = header.createCell(5, Cell.CELL_TYPE_STRING);
        cell5.setCellValue("Unit");
        cell5.setCellStyle(boldStyle);
        Cell cell6 = header.createCell(6, Cell.CELL_TYPE_STRING);
        cell6.setCellValue("Mirage?");
        cell6.setCellStyle(boldStyle);
        
        if (metadata.getDescriptors() != null) {
            // sort them according to their order
            Collections.sort(metadata.getDescriptors());
            for (Description description: metadata.getDescriptors()) {
                if (!mirageOnly || (mirageOnly && isMirage (description))) 
                    idx = addMetadataRow(description, sheet, idx, 0, mirageOnly);
            }
        }
        
        if (metadata.getDescriptorGroups() != null) {
            Collections.sort(metadata.getDescriptorGroups());
            for (Description description: metadata.getDescriptorGroups()) {
                if (!mirageOnly || (mirageOnly && isMirage (description))) 
                    idx = addMetadataRow(description, sheet, idx, 0, mirageOnly);
            }
        }
        return idx;
    }
    
    int addMetadataRow (Description description, Sheet sheet, int rowIdx, int level, boolean mirageOnly) {
        Row row = sheet.createRow(rowIdx++);
        Cell level1 = row.createCell(1, Cell.CELL_TYPE_STRING);
        Cell level2 = row.createCell(2, Cell.CELL_TYPE_STRING);
        Cell level3 = row.createCell(3, Cell.CELL_TYPE_STRING);
        Cell value = row.createCell(4, Cell.CELL_TYPE_STRING);
        Cell unit = row.createCell(5, Cell.CELL_TYPE_STRING);
        Cell mirage = row.createCell(6, Cell.CELL_TYPE_STRING);
        
        if (level == 0) {
            level1.setCellValue(description.getName());
        } else if (level == 1) {
            level2.setCellValue(description.getName());
        } else if (level == 2) {
            level3.setCellValue(description.getName());
        }
        
        if (description.getNotApplicable()) {
            value.setCellValue("Not Applicable");
        }
        if (description.getNotRecorded()) {
            value.setCellValue("Not Recorded");
        }
        
        if (description instanceof Descriptor) {
            if (((Descriptor) description).getValue() != null && !((Descriptor) description).getValue().isEmpty()) 
                value.setCellValue(((Descriptor) description).getValue());
            if (((Descriptor) description).getUnit() != null) {
                unit.setCellValue(((Descriptor) description).getUnit());
            }
        }
        if (description.getKey() != null) {
            mirage.setCellValue(description.getKey().isMirage() ? "Yes" : "No");
        }
        
        if (description instanceof DescriptorGroup) {
            if (((DescriptorGroup) description).getDescriptors() != null) {
                for (Description sub: ((DescriptorGroup) description).getDescriptors()) {
                    if (!mirageOnly || (mirageOnly && isMirage (description))) 
                        rowIdx = addMetadataRow(sub, sheet, rowIdx, level+1, mirageOnly);
                }
            }
        }
        
        return rowIdx;
    }
    
    int getColumnWidth (float widthExcel) {
        return (int)Math.floor((widthExcel * 7.0017f + 5) / 7.0017f * 256);
    }
    
    boolean isMirage (Description d) {
        if (d instanceof DescriptorGroup) {
            if (d.getKey().isMirage())
                return true;
            else {
                boolean isMirage = false;
                for (Description sub: ((DescriptorGroup) d).getDescriptors() ) {
                    isMirage = isMirage (sub);
                    if (isMirage)  // only one mirage descriptor is sufficient for the group to be included
                        break;
                }
                return isMirage;
            }
        } else {
            return d.getKey().isMirage();
        }
    }

}
