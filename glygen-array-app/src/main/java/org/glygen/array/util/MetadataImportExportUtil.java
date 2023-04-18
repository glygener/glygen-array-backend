package org.glygen.array.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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
    
    public MetadataImportExportUtil() {
    }
    
    public MetadataImportExportUtil(String baseURL) {
        this.baseURL = baseURL;
    }
    
    public void exportIntoExcel (ArrayDataset dataset, String outputFile) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        createDatasetSheet (dataset, "DatasetInfo", workbook);
        createMetadataSheet (dataset.getSample(), "Sample", workbook);
        int slideCount = 1;
      //  Set<String> spotMetadataList = new HashSet<>();
        
        for (Slide slide: dataset.getSlides()) {
            if (slide.getMetadata() != null) {
                createMetadataSheet(slide.getMetadata(), "Slide-" + slideCount + "-AssayMetadata", workbook);
            }
            if (slide.getPrintedSlide().getMetadata() != null) {
                createMetadataSheet(slide.getPrintedSlide().getMetadata(), "Slide-" + slideCount + "-SlideMetadata", workbook);
            }
            if (slide.getPrintedSlide().getPrinter() != null) {
                createMetadataSheet(slide.getPrintedSlide().getPrinter(), "Slide-" + slideCount + "-PrinterMetadata", workbook);
            }
            if (slide.getPrintedSlide().getPrintRun() != null) {
                createMetadataSheet(slide.getPrintedSlide().getPrintRun(), "Slide-" + slideCount + "-PrintrunMetadata", workbook);
            }
            
        /*    SlideLayout layout = slide.getPrintedSlide().getLayout();
            int spotMetadataCount = 1;
            for (Block block: layout.getBlocks()) {
            	for (Spot spot: block.getBlockLayout().getSpots()) {
            		if (spot != null) {
	            		if (spot.getMetadata() != null) {
	            			if (!spotMetadataList.contains(spot.getMetadata().getName())) {
	            				spotMetadataList.add(spot.getMetadata().getName());
	            				createMetadataSheet(spot.getMetadata(), "Slide-" + slideCount + "-SpotMetadata-" + spotMetadataCount, workbook);
	            				spotMetadataCount++;
	            			}
	            		}
            		}
            	}
            }*/
            int imageCount = 1;
            for (Image image: slide.getImages()) {
                if (image.getScanner() != null) {
                    createMetadataSheet(image.getScanner(), "Slide-" + slideCount + "-ScannerMetadata-" + imageCount, workbook);
                }
                int rawDataCount = 1;
                for (RawData rawData: image.getRawDataList()) {
                    if (rawData.getMetadata() != null) {
                        createMetadataSheet(rawData.getMetadata(), "Slide-" + slideCount + "-ImageAnalysisMetadata-" + rawDataCount, workbook);
                    }
                    
                    int processedCount=1;
                    for (ProcessedData processed: rawData.getProcessedDataList()) {
                        if (processed.getMetadata() != null) {
                            createMetadataSheet(processed.getMetadata(), "Slide-" + slideCount + "-DataProcessingSoftwareMetadata-" + processedCount, workbook);
                        }
                        processedCount++;
                    }
                    rawDataCount++;
                }
                imageCount++;
            }
            slideCount++;
        }
        
        FileOutputStream os = new FileOutputStream(outputFile);
        workbook.write(os);
        os.close();
        
    }
    
    private void createDatasetSheet(ArrayDataset dataset, String sheetName, Workbook workbook) {
        Sheet sheet = workbook.createSheet(sheetName);
        CreationHelper createHelper = workbook.getCreationHelper();
        CellStyle rowStyle = workbook.createCellStyle();
        rowStyle.setWrapText(true);
        
        XSSFCellStyle hlinkstyle = ((XSSFWorkbook)workbook).createCellStyle();
        XSSFFont hlinkfont = ((XSSFWorkbook)workbook).createFont();
        hlinkfont.setUnderline(XSSFFont.U_SINGLE);
        hlinkfont.setColor(IndexedColors.BLUE.index);
        hlinkstyle.setFont(hlinkfont);
        
        Row header = sheet.createRow(0);
        Cell headerCell = header.createCell(0);
        headerCell.setCellValue("Dataset URL");
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
            
        Row name = sheet.createRow(1);
        Cell cell = name.createCell(0);
        cell.setCellValue("Name");
        cell = name.createCell(1);
        cell.setCellValue(dataset.getName());
        
        Row description = sheet.createRow(2);
        description.setRowStyle(rowStyle);
        cell = description.createCell(0);
        cell.setCellValue("Description");
        cell = description.createCell(1);
        cell.setCellValue(dataset.getDescription());
        
        Row createdDate = sheet.createRow(3);
        cell = createdDate.createCell(0);
        cell.setCellValue("Submission Date");
        cell = createdDate.createCell(1);
        cell.setCellValue(dataset.getDateAddedToLibrary().toString());
        
        Row publicDate = sheet.createRow(4);
        cell = publicDate.createCell(0);
        cell.setCellValue("Release Date");
        cell = publicDate.createCell(1);
        if (dataset.getIsPublic()) {
            cell.setCellValue(dataset.getDateCreated().toString());
        }
        
        Row submitter = sheet.createRow(4);
        cell = submitter.createCell(0);
        cell.setCellValue("Submitted By");
        cell = submitter.createCell(1);
        if (dataset.getUser().getFirstName() != null && dataset.getUser().getLastName() != null)
            cell.setCellValue(dataset.getUser().getName() + ": " + dataset.getUser().getFirstName() + " " + dataset.getUser().getLastName());
        else 
            cell.setCellValue(dataset.getUser().getName());
        
        int rownum = 5;
        for (String keyword: dataset.getKeywords()) {
           Row row = sheet.createRow(rownum++);
           cell = row.createCell(0);
           cell.setCellValue("Keyword");
           cell = row.createCell(1);
           cell.setCellValue(keyword);
        }
        for (Publication pub: dataset.getPublications()) {
            Row row = sheet.createRow(rownum++);
            row.setRowStyle(rowStyle);
            cell = row.createCell(0);
            cell.setCellValue("Publication");
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
        
        // print out the hierarchy
        int slideCount = 1;
        for (Slide slide: dataset.getSlides()) {
            Row slideRow = sheet.createRow(rownum++);
            cell = slideRow.createCell(0);
            cell.setCellValue("Slide-" + slideCount);
            Row row = sheet.createRow(rownum++);
            cell = row.createCell(0);
            cell = row.createCell(1);
            if (slide.getMetadata() != null) {
                cell.setCellValue("Slide-" + slideCount + "-AssayMetadata");
                /*link = (XSSFHyperlink)createHelper.createHyperlink(XSSFHyperlink.LINK_DOCUMENT);
                link.setAddress("'Slide-" + slideCount + "-AssayMetadata" + "'!A1");
                cell.setHyperlink((XSSFHyperlink) link);
                cell.setCellStyle(hlinkstyle);*/
            } else {
                cell.setCellValue("No Assay Metadata provided");
            }
            row = sheet.createRow(rownum++);
            cell = row.createCell(0);
            cell = row.createCell(1);
            if (slide.getPrintedSlide().getMetadata() != null) {
                cell.setCellValue("Slide-" + slideCount + "-SlideMetadata");
               /* link = (XSSFHyperlink)createHelper.createHyperlink(XSSFHyperlink.LINK_DOCUMENT);
                link.setAddress("'Slide-" + slideCount + "-SlideMetadata" + "'!A1");
                cell.setHyperlink((XSSFHyperlink) link);
                cell.setCellStyle(hlinkstyle);*/
            } else {
                cell.setCellValue("No Slide Metadata provided");
            }
            row = sheet.createRow(rownum++);
            cell = row.createCell(0);
            cell = row.createCell(1);
            if (slide.getPrintedSlide().getPrinter() != null) {
                cell.setCellValue("Slide-" + slideCount + "-PrinterMetadata");
              /*  link = (XSSFHyperlink)createHelper.createHyperlink(XSSFHyperlink.LINK_DOCUMENT);
                link.setAddress("'Slide-" + slideCount + "-PrinterMetadata"+ "'!A1");
                cell.setHyperlink((XSSFHyperlink) link);
                cell.setCellStyle(hlinkstyle);*/
            } else {
                cell.setCellValue("No Printer Metadata provided");
            }
            row = sheet.createRow(rownum++);
            cell = row.createCell(0);
            cell = row.createCell(1);
            if (slide.getPrintedSlide().getPrintRun() != null) {
                cell.setCellValue("Slide-" + slideCount + "-PrintrunMetadata");
             /*   link = (XSSFHyperlink)createHelper.createHyperlink(XSSFHyperlink.LINK_DOCUMENT);
                link.setAddress("'Slide-" + slideCount + "-PrintrunMetadata" + "'!A1");
                cell.setHyperlink((XSSFHyperlink) link);
                cell.setCellStyle(hlinkstyle);*/
            } else {
                cell.setCellValue("No Printrun Metadata provided");
            }
            
            int imageCount = 1;
            for (Image image: slide.getImages()) {
                Row imageRow = sheet.createRow(rownum++);
                cell = imageRow.createCell(0);
                cell = imageRow.createCell(1);
                cell.setCellValue("Image-" + imageCount);
                row = sheet.createRow(rownum++);
                cell = row.createCell(0);
                cell = row.createCell(1);
                cell = row.createCell(2);
                if (image.getScanner() != null) {
                    cell.setCellValue("Slide-" + slideCount + "-ScannerMetadata-" + imageCount);
                    /*link = (XSSFHyperlink)createHelper.createHyperlink(XSSFHyperlink.LINK_DOCUMENT);
                    link.setAddress("'Slide-" + slideCount + "-ScannerMetadata-" + imageCount + "'!A1");
                    cell.setHyperlink((XSSFHyperlink) link);
                    cell.setCellStyle(hlinkstyle);*/
                } else {
                    cell.setCellValue("No Scanner Metadata provided");
                }
                int rawDataCount = 1;
                for (RawData rawData: image.getRawDataList()) {
                    Row rawdataRow = sheet.createRow(rownum++);
                    cell = rawdataRow.createCell(0);
                    cell = rawdataRow.createCell(1);
                    cell = rawdataRow.createCell(2);
                    cell.setCellValue("RawData-" + rawDataCount);
                    row = sheet.createRow(rownum++);
                    cell = row.createCell(0);
                    cell = row.createCell(1);
                    cell = row.createCell(2);
                    cell = row.createCell(3);
                    if (rawData.getMetadata() != null) {
                        cell.setCellValue("Slide-" + slideCount + "-ImageAnalysisMetadata-" + rawDataCount);
                       /* link = (XSSFHyperlink)createHelper.createHyperlink(XSSFHyperlink.LINK_DOCUMENT);
                        link.setAddress("'Slide-" + slideCount + "-ImageAnalysisMetadata-" + rawDataCount+ "'!A1");
                        cell.setHyperlink((XSSFHyperlink) link);
                        cell.setCellStyle(hlinkstyle);*/
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
                        cell.setCellValue("ProcessedData-" + processedCount);
                        row = sheet.createRow(rownum++);
                        cell = row.createCell(0);
                        cell = row.createCell(1);
                        cell = row.createCell(2);
                        cell = row.createCell(3);
                        cell = row.createCell(4);
                        if (processed.getMetadata() != null) {
                            cell.setCellValue("Slide-" + slideCount + "-DataProcessingSoftwareMetadata-" + processedCount);
                           /* link = (XSSFHyperlink)createHelper.createHyperlink(XSSFHyperlink.LINK_DOCUMENT);
                            link.setAddress("'Slide-" + slideCount + "-DataProcessingSoftwareMetadata-" + processedCount + "'!A1");
                            cell.setHyperlink((XSSFHyperlink) link);
                            cell.setCellStyle(hlinkstyle);*/
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
    }

    public void createMetadataSheet (MetadataCategory metadata, String sheetName, Workbook workbook) {
        try {
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
            
            if (metadata.getDescriptors() != null) {
                // sort them according to their order
                Collections.sort(metadata.getDescriptors());
                for (Description description: metadata.getDescriptors()) {
                    idx = addMetadataRow(description, sheet, idx, 0);
                }
            }
            
            if (metadata.getDescriptorGroups() != null) {
                Collections.sort(metadata.getDescriptorGroups());
                for (Description description: metadata.getDescriptorGroups()) {
                    idx = addMetadataRow(description, sheet, idx, 0);
                }
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException (e.getMessage() + " sheet:" + sheetName);
        }
    }
    
    int addMetadataRow (Description description, Sheet sheet, int rowIdx, int level) {
        Row row = sheet.createRow(rowIdx++);
        Cell level1 = row.createCell(0, Cell.CELL_TYPE_STRING);
        Cell level2 = row.createCell(1, Cell.CELL_TYPE_STRING);
        Cell level3 = row.createCell(2, Cell.CELL_TYPE_STRING);
        Cell value = row.createCell(3, Cell.CELL_TYPE_STRING);
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
                    rowIdx = addMetadataRow(sub, sheet, rowIdx, level+1);
                }
            }
        }
        
        return rowIdx;
    }

}
