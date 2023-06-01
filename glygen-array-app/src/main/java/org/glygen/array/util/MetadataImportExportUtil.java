package org.glygen.array.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
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
    CellStyle backgroundcolor1;
    CellStyle backgroundcolor2;
    
 // Declaring the static map
    private static Map<String, String> mirageMap;
  
    // Instantiating the static map
    static
    {
        mirageMap = new HashMap<>();
        mirageMap.put("Sample", "A wide variety of samples can be applied to glycan microarrays. Here we use the term ‘Glycan Binding Sample’ [‘Sample’] for a protein, a microorganism or other molecule being analysed for carbohydrate recognition. Samples may include but are not restricted to glycan binding proteins (such as lectins, antibodies, adhesion molecules, and carbohydrate-binding modules), glycan binding organisms (such as viruses, fungi, bacteria, and animal cells), and other binding systems that may have affinity for glycans including aptamers, or other synthetic glycan binding molecules.");
        mirageMap.put("Slide", "A wide variety of solid phases can be used to print glycan microarrays. The utility of the data generated from a glycan array analysis will be related to the quality of the printed array.");
        mirageMap.put("Printer", "Glycan microarrays are printed using a robotic liquid delivery system to deposit glycans on a printing surface. The utility of the data generated will be related to the quality of the printed array.");
        mirageMap.put("Assay", "The protocol used for microarray binding analysis of Sample should include Sample concentration. Describe the composition of solutions, time and temperature used for blocking (preventing non-specific binding) binding, and washing, as well as application of secondary reagent(s) required for the analyses. If the Sample is precomplexed with secondary detection reagents prior to adding to the array (e.g. anti-His antibody with his-tagged Sample) give the ratio of reactants and pre-complexing time and temperature.");
        mirageMap.put("Print-run", "Indicate the composition of the printing solution and the concentration of the glycan in the printing solution (single or more than one concentration). The physical conditions reported for array production should include temperature, humidity, reaction time for covalent coupling or adsorption, and post reaction treatment if applicable.");
        mirageMap.put("ImageAnalysis", "Describe the software used to analyse (quantify) the output scanner image, indicating the name, version and manufacturer used and any special features active in the software (i.e. data smoothing, normalization, etc.).");
        mirageMap.put("Scanner", "The most common detection method currently used for glycan microarray analysis is microscope slide fluorescence scanning, and there are several commercial instruments available for this type of analysis. Glycan microarrays can be printed in a variety of other formats including microtiter plate, and possibly in other geometries in custom instruments. In addition, a variety of detection methods are possible including surface plasmon resonance (SPR) and MS readouts and more precise or more sensitive detection may be developed in the future.");
        mirageMap.put("DataProcessing", "Provide details of how data in the table of microarray binding results are generated and calculated, i.e., specific software, normalization method, data selection procedures, and parameters, statistical analysis (including how the data from glycan replicates on the array were handled in the statistical method), transformation algorithm and scaling parameters.");
    }
    
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
        
        this.backgroundcolor1 = workbook.createCellStyle();
        XSSFColor myColor = new XSSFColor(new java.awt.Color(255, 255, 204));
        ((XSSFCellStyle)backgroundcolor1).setFillForegroundColor(myColor);
        ((XSSFCellStyle)backgroundcolor1).setFillPattern(FillPatternType.SOLID_FOREGROUND);
        backgroundcolor1.setFont(font);
        
        XSSFColor myColor2 = new XSSFColor(new java.awt.Color(255, 255, 230));
        this.backgroundcolor2 = workbook.createCellStyle();
        ((XSSFCellStyle)backgroundcolor2).setFillForegroundColor(myColor2);
        ((XSSFCellStyle)backgroundcolor2).setFillPattern(FillPatternType.SOLID_FOREGROUND);
        backgroundcolor2.setWrapText(true);
        //backgroundcolor2.setFont(font);
        
        Map<String, List<Integer>> rowNumbers = createDatasetSheet (dataset, "DatasetInfo", workbook, mirageOnly, singleSheet);
        
        if (singleSheet) {
            Map<String, Integer> linkRows = createSingleSheet (dataset, workbook, mirageOnly);
            addHyperLinks(workbook, workbook.getSheet("DatasetInfo"), singleSheet, rowNumbers, linkRows);
        } else {
            addHyperLinks(workbook, workbook.getSheet("DatasetInfo"), singleSheet, rowNumbers, null);
            Sheet sheet = workbook.createSheet("Sample - " + dataset.getSample().getName());
            createMetadataSheet (dataset.getSample(), sheet, "Sample - " + dataset.getSample().getName(), mirageMap.get("Sample"), workbook, mirageOnly, 0);
            
            Set<String> alreadyCreatedSheets = new HashSet<>();
            for (Slide slide: dataset.getSlides()) {
                if (slide.getMetadata() != null) {
                    String sheetName = "Assay - " + slide.getMetadata().getName();
                    if (!alreadyCreatedSheets.contains(sheetName)) {
                        alreadyCreatedSheets.add(sheetName);
                        sheet = workbook.createSheet(sheetName);
                        createMetadataSheet(slide.getMetadata(), sheet, sheetName, mirageMap.get("Assay"), workbook, mirageOnly, 0);
                    }
                }
                if (slide.getPrintedSlide().getMetadata() != null) {
                    String sheetName = "Slide - " + slide.getPrintedSlide().getMetadata().getName();
                    if (!alreadyCreatedSheets.contains(sheetName)) {
                        alreadyCreatedSheets.add(sheetName);
                        sheet = workbook.createSheet(sheetName);
                        createMetadataSheet(slide.getPrintedSlide().getMetadata(), sheet, sheetName, mirageMap.get("Slide"), workbook, mirageOnly, 0);
                    }
                }
                if (slide.getPrintedSlide().getPrinter() != null) {
                    String sheetName = "Printer - " + slide.getPrintedSlide().getPrinter().getName();
                    if (!alreadyCreatedSheets.contains(sheetName)) {
                        alreadyCreatedSheets.add(sheetName);
                        sheet = workbook.createSheet(sheetName);
                        createMetadataSheet(slide.getPrintedSlide().getPrinter(), sheet, sheetName, mirageMap.get("Printer"), workbook, mirageOnly, 0);
                    }
                }
                if (slide.getPrintedSlide().getPrintRun() != null) {
                    String sheetName = "Print-run - " + slide.getPrintedSlide().getPrintRun().getName();
                    if (!alreadyCreatedSheets.contains(sheetName)) {
                        alreadyCreatedSheets.add(sheetName);
                        sheet = workbook.createSheet(sheetName);
                        createMetadataSheet(slide.getPrintedSlide().getPrintRun(), sheet, sheetName, mirageMap.get("Print-run"), workbook, mirageOnly, 0);
                    }
                }
                
                for (Image image: slide.getImages()) {
                    if (image.getScanner() != null) {
                        String sheetName = "Scanner - " + image.getScanner().getName();
                        if (!alreadyCreatedSheets.contains(sheetName)) {
                            alreadyCreatedSheets.add(sheetName);
                            sheet = workbook.createSheet(sheetName);
                            createMetadataSheet(image.getScanner(), sheet, sheetName, mirageMap.get("Scanner"), workbook, mirageOnly, 0);
                        }
                    }
                    
                    for (RawData rawData: image.getRawDataList()) {
                        if (rawData.getMetadata() != null) {
                            String sheetName = "ImageAnalysis - " + rawData.getMetadata().getName();
                            if (!alreadyCreatedSheets.contains(sheetName)) {
                                alreadyCreatedSheets.add(sheetName);
                                sheet = workbook.createSheet(sheetName);
                                createMetadataSheet(rawData.getMetadata(), sheet, sheetName, mirageMap.get("ImageAnalysis"), workbook, mirageOnly, 0);
                            }
                        }
                        
                        for (ProcessedData processed: rawData.getProcessedDataList()) {
                            if (processed.getMetadata() != null) {
                                String sheetName = "DataProcessing - " + processed.getMetadata().getName();
                                if (!alreadyCreatedSheets.contains(sheetName)) {
                                    alreadyCreatedSheets.add(sheetName);
                                    sheet = workbook.createSheet(sheetName);
                                    createMetadataSheet(processed.getMetadata(), sheet, sheetName, mirageMap.get("DataProcessing"), workbook, mirageOnly, 0);
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
        
        int rownum = createMetadataSheet(dataset.getSample(), sheet, "Sample - " + dataset.getSample().getName(), mirageMap.get("Sample"), workbook, mirageOnly, 0);
        linkRows.put("Sample", 1);
        
        Set<String> alreadyCreatedSheets = new HashSet<>();
        // get all "Assay metadata"
        for (Slide slide: dataset.getSlides()) {
            if (slide.getMetadata() != null) {
                String sheetName = "Assay - " + slide.getMetadata().getName();
                if (!alreadyCreatedSheets.contains(sheetName)) {
                    alreadyCreatedSheets.add(sheetName);
                    linkRows.put(sheetName, rownum+1);
                    rownum = createMetadataSheet(slide.getMetadata(), sheet, sheetName, mirageMap.get("Assay"), workbook, mirageOnly, rownum);
                }
            }
        }
      
        // get all printrun
        for (Slide slide: dataset.getSlides()) {
            if (slide.getPrintedSlide() != null && slide.getPrintedSlide().getPrintRun() != null) {
                String sheetName = "Print-run - " + slide.getPrintedSlide().getPrintRun().getName();
                if (!alreadyCreatedSheets.contains(sheetName)) {
                    alreadyCreatedSheets.add(sheetName);
                    linkRows.put(sheetName, rownum+1);
                    rownum = createMetadataSheet(slide.getPrintedSlide().getPrintRun(), sheet, sheetName, mirageMap.get("Print-run"), workbook, mirageOnly, rownum);
                }
            }
        }
        
        // get all printer metadata
        for (Slide slide: dataset.getSlides()) {
            if (slide.getPrintedSlide().getPrinter() != null) {
                String sheetName = "Printer - " + slide.getPrintedSlide().getPrinter().getName();
                if (!alreadyCreatedSheets.contains(sheetName)) {
                    alreadyCreatedSheets.add(sheetName);
                    linkRows.put(sheetName, rownum+1);
                    rownum = createMetadataSheet(slide.getPrintedSlide().getPrinter(), sheet, sheetName, mirageMap.get("Printer"), workbook, mirageOnly, rownum);
                }
            }
            
        }
        
        // get all slide metadata
        for (Slide slide: dataset.getSlides()) {
            if (slide.getPrintedSlide().getMetadata() != null) {
                String sheetName = "Slide - " + slide.getPrintedSlide().getMetadata().getName();
                if (!alreadyCreatedSheets.contains(sheetName)) {
                    alreadyCreatedSheets.add(sheetName);
                    linkRows.put(sheetName, rownum+1);
                    rownum = createMetadataSheet(slide.getPrintedSlide().getMetadata(), sheet, sheetName, mirageMap.get("Slide"), workbook, mirageOnly, rownum);
                }
            }
        }
        
        // get all scanner and image analysis metadata
        for (Slide slide: dataset.getSlides()) {
            for (Image image: slide.getImages()) {
                if (image.getScanner() != null) {
                    String sheetName = "Scanner - " + image.getScanner().getName();
                    if (!alreadyCreatedSheets.contains(sheetName)) {
                        alreadyCreatedSheets.add(sheetName);
                        linkRows.put(sheetName, rownum+1);
                        rownum = createMetadataSheet(image.getScanner(), sheet, sheetName, mirageMap.get("Scanner"), workbook, mirageOnly, rownum);
                    }
                }
                
                for (RawData rawData: image.getRawDataList()) {
                    if (rawData.getMetadata() != null) {
                        String sheetName = "ImageAnalysis - " + rawData.getMetadata().getName();
                        if (!alreadyCreatedSheets.contains(sheetName)) {
                            alreadyCreatedSheets.add(sheetName);
                            linkRows.put(sheetName, rownum+1);
                            rownum = createMetadataSheet(rawData.getMetadata(), sheet, sheetName, mirageMap.get("ImageAnalysis"), workbook, mirageOnly, rownum);
                        }
                    }
                }
            }
        }
        
        // get all data processing metadata
        for (Slide slide: dataset.getSlides()) {
            for (Image image: slide.getImages()) {
                for (RawData rawData: image.getRawDataList()) {
                    for (ProcessedData processed: rawData.getProcessedDataList()) {
                        if (processed.getMetadata() != null) {
                            String sheetName = "DataProcessing - " + processed.getMetadata().getName();
                            if (!alreadyCreatedSheets.contains(sheetName)) {
                                alreadyCreatedSheets.add(sheetName);
                                linkRows.put(sheetName, rownum+1);
                                rownum = createMetadataSheet(processed.getMetadata(), sheet, sheetName, mirageMap.get("DataProcessing"), workbook, mirageOnly, rownum);
                            }
                        }
                    }
                }
            }
        }
        
        return linkRows;   
    }
    
    void addHyperLinks (Workbook workbook, Sheet sheet, Boolean singleSheet, Map<String, List<Integer>> rowNumbers, Map<String, Integer> linkRows) {
        CreationHelper createHelper = workbook.getCreationHelper();
        if (!singleSheet) {
            for (String sheetName: rowNumbers.keySet()) {
                for (Integer rownum: rowNumbers.get(sheetName)) {
                    Row row = sheet.getRow(rownum);
                    Cell cell;
                    if (sheetName.startsWith("Sample"))
                        cell = row.getCell(1);
                    else cell = row.getCell(4);
                    XSSFHyperlink link = (XSSFHyperlink)createHelper.createHyperlink(XSSFHyperlink.LINK_DOCUMENT);
                    link.setAddress("'" + sheetName + "'!A1");
                    cell.setHyperlink((XSSFHyperlink) link);
                    cell.setCellStyle(hlinkstyle);
                }
            }
        } else {
            Row sampleRow = sheet.getRow(7);
            Cell cell = sampleRow.getCell(1);
            XSSFHyperlink link = (XSSFHyperlink)createHelper.createHyperlink(XSSFHyperlink.LINK_DOCUMENT);
            link.setAddress("'Metadata'!A" + linkRows.get("Sample"));
            cell.setHyperlink((XSSFHyperlink) link);
            cell.setCellStyle(hlinkstyle);
            
            for (String sheetName: rowNumbers.keySet()) {
                if (sheetName.startsWith("Sample"))
                    continue;
                for (Integer rownum: rowNumbers.get(sheetName)) {
                    Row row = sheet.getRow(rownum);
                    cell = row.getCell(4);
                    link = (XSSFHyperlink)createHelper.createHyperlink(XSSFHyperlink.LINK_DOCUMENT);
                    link.setAddress("'Metadata'!A" + linkRows.get(sheetName));
                    cell.setHyperlink((XSSFHyperlink) link);
                    cell.setCellStyle(hlinkstyle);
                }
            }
        }
    }

    private Map<String, List<Integer>> createDatasetSheet(ArrayDataset dataset, String sheetName, Workbook workbook, Boolean mirageOnly, Boolean singleSheet) {
        Map<String, List<Integer>> rowNumbers = new HashMap<>();
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
        cell = exportOptions.createCell(0);
        cell.setCellValue("Export Options");
        cell.setCellStyle(boldStyle);
        cell = exportOptions.createCell(1);
        cell.setCellValue("Mirage Only? " + (mirageOnly != null && mirageOnly ? "Yes" : "No") + 
                " Single Sheet? " + (singleSheet != null && singleSheet ? "Yes" : "No"));
        
        Row description = sheet.createRow(3);
        cell = description.createCell(0);
        cell.setCellValue("Description");
        cell.setCellStyle(boldStyle);
        cell = description.createCell(1);
        cell.setCellValue(dataset.getDescription());
        cell.setCellStyle(wrapTextStyle);
        cell = description.createCell(2);
        cell.setCellStyle(wrapTextStyle);
        cell = description.createCell(3);
        cell.setCellStyle(wrapTextStyle);
        cell = description.createCell(4);
        cell.setCellStyle(wrapTextStyle);
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
        rowNumbers.put("Sample - " + dataset.getSample().getName(), new ArrayList<Integer>());
        rowNumbers.get("Sample - " + dataset.getSample().getName()).add(7);
        
        int rownum = 8;
        for (String keyword: dataset.getKeywords()) {
           Row row = sheet.createRow(rownum++);
           cell = row.createCell(0);
           cell.setCellValue("Keyword");
           cell.setCellStyle(boldStyle);
           cell = row.createCell(1);
           cell.setCellValue(keyword);
        }
        
        Row row = sheet.createRow(rownum++);
        cell = row.createCell(0);
        cell.setCellValue("Publication");
        cell.setCellStyle(boldStyle);
        boolean first = true;
        for (Publication pub: dataset.getPublications()) {
            if (!first) {
                row = sheet.createRow(rownum++);
                row.createCell(0);
            } else 
                first = false;
            cell = row.createCell(1);
            cell.setCellStyle(wrapTextStyle);
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
        
        row = sheet.createRow(rownum++);
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
            cell.setCellValue("Slide - " + slideCount + ": " + slide.getPrintedSlide().getName());
            cell = row.createCell(1);
            cell = row.createCell(2);
            cell = row.createCell(3);
            cell = row.createCell(4);
            if (slide.getMetadata() != null) {
                sheetName = "Assay - " + slide.getMetadata().getName();
                cell.setCellValue(sheetName);
                if (rowNumbers.get(sheetName) == null)
                    rowNumbers.put(sheetName, new ArrayList<Integer>());
                rowNumbers.get(sheetName).add(rownum-1);
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
                sheetName = "Slide - " + slide.getPrintedSlide().getMetadata().getName();
                cell.setCellValue(sheetName);
                if (rowNumbers.get(sheetName) == null)
                    rowNumbers.put(sheetName, new ArrayList<Integer>());
                rowNumbers.get(sheetName).add(rownum-1);
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
                sheetName = "Printer - " + slide.getPrintedSlide().getPrinter().getName();
                cell.setCellValue(sheetName);
                if (rowNumbers.get(sheetName) == null)
                    rowNumbers.put(sheetName, new ArrayList<Integer>());
                rowNumbers.get(sheetName).add(rownum-1);
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
                sheetName = "PrintRun - " + slide.getPrintedSlide().getPrintRun().getName();
                cell.setCellValue(sheetName);
                if (rowNumbers.get(sheetName) == null)
                    rowNumbers.put(sheetName, new ArrayList<Integer>());
                rowNumbers.get(sheetName).add(rownum-1);
            } else {
                cell.setCellValue("No Printrun Metadata provided");
            }
            
            int imageCount = 1;
            for (Image image: slide.getImages()) {
                Row imageRow = sheet.createRow(rownum++);
                cell = imageRow.createCell(0);
                cell = imageRow.createCell(1);
                cell.setCellValue("Image - " + imageCount + ": " + (image.getFile() == null ? "No image provided" : image.getFile().getOriginalName()));
                cell = imageRow.createCell(2);
                cell = imageRow.createCell(3);
                cell = imageRow.createCell(4);
                if (image.getScanner() != null) {
                    sheetName = "Scanner - " + image.getScanner().getName();
                    cell.setCellValue(sheetName);
                    if (rowNumbers.get(sheetName) == null)
                        rowNumbers.put(sheetName, new ArrayList<Integer>());
                    rowNumbers.get(sheetName).add(rownum-1);
                } else {
                    cell.setCellValue("No Scanner Metadata provided");
                }
                int rawDataCount = 1;
                for (RawData rawData: image.getRawDataList()) {
                    Row rawdataRow = sheet.createRow(rownum++);
                    cell = rawdataRow.createCell(0);
                    cell = rawdataRow.createCell(1);
                    cell = rawdataRow.createCell(2);
                    cell.setCellValue("RawData - " + rawDataCount + ": " + (rawData.getFile() == null ? "No rawdata provided" : rawData.getFile().getOriginalName()));
                    cell = rawdataRow.createCell(3);
                    cell = rawdataRow.createCell(4);
                    if (rawData.getMetadata() != null) {
                        sheetName = "ImageAnalysis - " + rawData.getMetadata().getName();
                        cell.setCellValue(sheetName);
                        if (rowNumbers.get(sheetName) == null)
                            rowNumbers.put(sheetName, new ArrayList<Integer>());
                        rowNumbers.get(sheetName).add(rownum-1);
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
                        cell.setCellValue("ProcessedData - " + processedCount + ": " + (processed.getFile() == null ? "No processed data provided" : processed.getFile().getOriginalName()));
                        cell = processedRow.createCell(4);
                        if (processed.getMetadata() != null) {
                            sheetName = "DataProcessing - " + processed.getMetadata().getName();
                            cell.setCellValue(sheetName);
                            if (rowNumbers.get(sheetName) == null)
                                rowNumbers.put(sheetName, new ArrayList<Integer>());
                            rowNumbers.get(sheetName).add(rownum-1);
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
        
        sheet.setColumnWidth(0, getColumnWidth(20f));
        sheet.setColumnWidth(1, getColumnWidth(30f));
        sheet.setColumnWidth(2, getColumnWidth(30f));
        sheet.setColumnWidth(3, getColumnWidth(30f));
        sheet.setColumnWidth(4, getColumnWidth(30f));
        
        return rowNumbers;
    }

    public int createMetadataSheet (MetadataCategory metadata, Sheet sheet, String sheetName, String description, Workbook workbook, Boolean mirageOnly, int rownum) {
        if (mirageOnly == null) mirageOnly = false;
        try {
            Row headline = sheet.createRow(rownum);
            Cell cell = headline.createCell(0);
            cell.setCellValue(sheetName);
            cell.setCellStyle(backgroundcolor1);
            headline.createCell(1);
            headline.createCell(2);
            headline.createCell(3);
            headline.createCell(4);
            headline.createCell(5);
            sheet.addMergedRegion(new CellRangeAddress(rownum, rownum, 0, 5));
            rownum++;
            
            Row headline2 = sheet.createRow(rownum);
            Cell cell1 = headline2.createCell(0);
            cell1.setCellValue(description);
            cell1.setCellStyle(backgroundcolor2);
            headline2.createCell(1);
            headline2.createCell(2);
            headline2.createCell(3);
            headline2.createCell(4);
            headline2.createCell(5);
            sheet.addMergedRegion(new CellRangeAddress(rownum, rownum, 0, 5));
            
            sheet.setColumnWidth(0, getColumnWidth(20f));
            sheet.setColumnWidth(1, getColumnWidth(20f));
            sheet.setColumnWidth(2, getColumnWidth(20f));
            sheet.setColumnWidth(3, getColumnWidth(60f));
            sheet.setColumnWidth(4, getColumnWidth(10f));
            sheet.setColumnWidth(5, getColumnWidth(10f));
         
            return addMetadataSection (sheet, rownum+1, metadata, mirageOnly, workbook);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException (e.getMessage() + " sheet:" + sheetName);
        }
    }
    
    int addMetadataSection (Sheet sheet, int idx, MetadataCategory metadata, Boolean mirageOnly, Workbook workbook) {
        Row header = sheet.createRow(idx++);
        Cell cell1 = header.createCell(0, Cell.CELL_TYPE_STRING);
        cell1.setCellValue("Information");
        cell1.setCellStyle(boldStyle);
        header.createCell(1, Cell.CELL_TYPE_STRING);
        header.createCell(2, Cell.CELL_TYPE_STRING);
        sheet.addMergedRegion(new CellRangeAddress(idx-1, idx-1, 0, 2));
        CellUtil.setAlignment(cell1, workbook, CellStyle.ALIGN_CENTER);
        Cell cell4 = header.createCell(3, Cell.CELL_TYPE_STRING);
        cell4.setCellValue("Value");
        cell4.setCellStyle(boldStyle);
        Cell cell5 = header.createCell(4, Cell.CELL_TYPE_STRING);
        cell5.setCellValue("Unit");
        cell5.setCellStyle(boldStyle);
        Cell cell6 = header.createCell(5, Cell.CELL_TYPE_STRING);
        cell6.setCellValue("MIRAGE");
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
        Cell level1 = row.createCell(0, Cell.CELL_TYPE_STRING);
        Cell level2 = row.createCell(1, Cell.CELL_TYPE_STRING);
        Cell level3 = row.createCell(2, Cell.CELL_TYPE_STRING);
        Cell value = row.createCell(3, Cell.CELL_TYPE_STRING);
        value.setCellStyle(wrapTextStyle);
        Cell unit = row.createCell(4, Cell.CELL_TYPE_STRING);
        Cell mirage = row.createCell(5, Cell.CELL_TYPE_STRING);
        
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
