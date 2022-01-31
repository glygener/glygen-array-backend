package org.glygen.array.util.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.glygen.array.persistence.rdf.template.DescriptionTemplate;
import org.glygen.array.persistence.rdf.template.DescriptorGroupTemplate;
import org.glygen.array.persistence.rdf.template.DescriptorTemplate;
import org.glygen.array.persistence.rdf.template.MandateGroup;
import org.glygen.array.persistence.rdf.template.MetadataTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplateType;
import org.glygen.array.persistence.rdf.template.Namespace;
import org.glygen.array.service.MetadataTemplateRepository;


class Config {
    int description = 3;
    int mandatory = 4;
    int multiplicity = 5;
    int allowBypass = 6;
    int type = 7;
    int dictionary = 8;
    int unit=9;
    int example=10;
    int wiki = 11;
    int group = 13;
    int mirage = 12;
    int groupName = 14;
    int review = 17;
    int totalCols = 18;
}

public class MetadataOntologyParser {
     
    static String[] allowedTypesList = new String[] {"longtext", "text", "selection", "dictionary", "number", "date", "boolean", "Longtext", "LongText", "Text", 
            "Selection", "Dictionary", "Number", "Date", "Boolean"};
   
    public static PrintStream warningOut;
    public static PrintStream errorOut;
    public static final String SEPERATOR = "/";
    public static final String SEPERATOR2 = ",";
    
    static String prefix = MetadataTemplateRepository.templatePrefix;
    static String dataprefix = "http://purl.org/gadr/data/";
    
    int descriptorId=1;
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println ("Please provide the metadata file folder location as an argument");
            System.exit(1);
        }
        
        try {
            warningOut = new PrintStream(new FileOutputStream("warningoutput.txt" ));
            errorOut = new PrintStream(new FileOutputStream("erroroutput.txt" ));
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        
        String metadataFileFolder = args[0];
        File templateOntologyFile = new File("ontology/gadr-template.owl");
        File templateOntologyOriginalFile = new File ("ontology/gadr-template-original.owl");
        try {
            FileUtils.copyFile(templateOntologyFile, templateOntologyOriginalFile);
        
            File templateOntologyInstanceFile = new File("ontology/gadr-template-individuals.owl");
            Integer descId = 1;
            MetadataOntologyParser parser = new MetadataOntologyParser();
            for (MetadataTemplateType type: MetadataTemplateType.values()) {
                String metadataSheet = metadataFileFolder + File.separator + type.getLabel() + ".xlsx";   
                    
                if (descId != null)  parser.setDescriptorId(descId);
                HashMap<String, List<Descriptor>> mp = parser.read(metadataSheet, new Config());
                parser.createOntology(mp, type.getLabel());
                
                // delete ontology file
                templateOntologyFile.delete();
                // rename individuals file to ontology
                templateOntologyInstanceFile.renameTo(templateOntologyFile);
                
                descId += 400;
            }
            
            templateOntologyFile.renameTo (templateOntologyInstanceFile);
            templateOntologyOriginalFile.renameTo(templateOntologyFile);
            
            System.out.println("Finished importing metaData to the Ontology");
        
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            warningOut.close();
            errorOut.close();
        }
                
       /* String filename = args[0];
        String type = args[1];
        Integer descId = null;
        if (args.length > 2) {
            descId = Integer.parseInt(args[2]);
        }
        try {       
            MetadataOntologyParser parser = new MetadataOntologyParser();
            if (descId != null)  parser.setDescriptorId(descId);
            HashMap<String, List<Descriptor>> mp = parser.read(filename, new Config());
            parser.createOntology(mp, type);
            System.out.println("Finished importing metaData to the Ontology");
        } finally {
            warningOut.close();
            errorOut.close();
        }*/
    }

    /**
     * Reads the excel file and extracts the data  
     * @param fileName name of the file to be read
     * @param config configuration with the column numbers
     * @return HashMap with the data from file
     */
    public HashMap<String, List<Descriptor>> read(String fileName, Config config) {
        try {
            FileInputStream fileReader = new FileInputStream(fileName);
            Workbook exampleWb = new XSSFWorkbook(fileReader);
            HashMap<String, List<Descriptor>> sheetMap = new HashMap<>();
            for (int i = 0; i < exampleWb.getNumberOfSheets(); i++) {
                Sheet sheet = exampleWb.getSheetAt(i);
                List<Descriptor> descriptorList = new ArrayList<>();
                Descriptor descriptor = null;
                Descriptor childDescriptor = null;
                Descriptor subDescriptor = null;
                
                int headerRow = 0;
                int level = 0;
                if (sheet.getSheetName().startsWith("Common")) {
                    // iterate to the header row
                   headerRow = 2;
                }
                
                for (Row row : sheet) {
                    if (isRowEmpty(row) || row.getRowNum() <= headerRow){
                        continue;
                    } else {
                        if (nameColumnsEmpty(row, sheet)) {
                            continue;
                        }
                        checkSelectionEmpty(row, config, sheet); 
                        checkDictionaryEmpty(row, config, sheet);
                     
                        for (int columnNumber = 0; columnNumber < config.totalCols; columnNumber++) {
                            Cell cell = row.getCell(columnNumber);
                            if (cell == null || cell.getCellType() == Cell.CELL_TYPE_BLANK) { 
                                cellEmptyError(sheet, row, config, columnNumber);
                                continue;
                            } else if (cell.getCellType() == Cell.CELL_TYPE_STRING && cell.getRichStringCellValue().getString().isEmpty()) {
                                cellEmptyError(sheet, row, config, columnNumber);
                                continue;
                            } else {
                                if (cell.getColumnIndex() == 0) {
                                    if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                                        String name = cell.getRichStringCellValue().getString();
                                        if (!name.equals("")) {
                                            descriptor = new Descriptor();
                                            descriptor.setName(name);
                                            descriptor.setPosition(row.getRowNum());
                                            findDuplicates(descriptorList, descriptor);
                                            descriptorList.add(descriptor);
                                            level = 0;
                                        }
                                    } else {
                                        errorOut.println("ERROR: Invalid value for name on sheet: "
                                                + sheet.getSheetName() + " at row: " + (cell.getRowIndex() + 1)
                                                + " column: " + (cell.getColumnIndex() + 1));
                                        break;
                                    }
                                } else if (cell.getColumnIndex() == 1) {
                                    if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                                        String childName = cell.getRichStringCellValue().getString();
                                        if (!childName.equals("")) {
                                            childDescriptor = new Descriptor();
                                            childDescriptor.setName(childName);
                                            if (descriptor != null) {
                                                descriptor.addChild(childDescriptor);
                                                childDescriptor.setPosition(row.getRowNum() - descriptor.getPosition());
                                            }
                                            level = 1;
                                        }
                                    } else {
                                        errorOut.println("ERROR: Invalid value for name on sheet: "
                                                + sheet.getSheetName() + " at row: " + (cell.getRowIndex() + 1)
                                                + " column: " + (cell.getColumnIndex() + 1));
                                        break;
                                    }
                                } else if (cell.getColumnIndex() == 2) {
                                    // third level descriptor
                                    if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                                        String childName = cell.getRichStringCellValue().getString();
                                        if (!childName.equals("")) {
                                            subDescriptor = new Descriptor();
                                            subDescriptor.setName(childName);
                                            if (childDescriptor != null) {
                                                childDescriptor.addChild(subDescriptor);
                                                subDescriptor.setPosition(row.getRowNum() - childDescriptor.getPosition());
                                            }
                                            level = 2;
                                        }
                                    } else {
                                        errorOut.println("ERROR: Invalid value for name on sheet: "
                                                + sheet.getSheetName() + " at row: " + (cell.getRowIndex() + 1)
                                                + " column: " + (cell.getColumnIndex() + 1));
                                        break;
                                    }
                                } else if (cell.getColumnIndex() == config.multiplicity) {
                                    readMultiplicty(cell, descriptor, childDescriptor, subDescriptor, sheet, level);
                                } else if (cell.getColumnIndex() == config.description) {
                                    readDescription(cell, descriptor, childDescriptor, subDescriptor, sheet, level);
                                } else if (cell.getColumnIndex() == config.type) {
                                    readType(cell, descriptor, childDescriptor, subDescriptor, sheet, level);
                                } else if (cell.getColumnIndex() == config.mandatory) {
                                    readMandatory(cell, descriptor, childDescriptor, subDescriptor, sheet, level);
                                } else if (cell.getColumnIndex() == config.dictionary) {
                                    //readDictionary(cell, descriptor, childDescriptor, subDescriptor, sheet, level);
                                    readSelectionOrDictionary(cell, descriptor, childDescriptor, subDescriptor, sheet, level);
                                } else if (cell.getColumnIndex() == config.unit) {
                                    readMeasurement(cell, descriptor, childDescriptor, subDescriptor, sheet, level);
                                } else if (cell.getColumnIndex() == config.example) {
                                    readExample(cell, descriptor, childDescriptor, subDescriptor, sheet, level);
                                } else if (cell.getColumnIndex() == config.wiki) {
                                    readWiki(cell, descriptor, childDescriptor, subDescriptor, sheet, level);
                                } else if (cell.getColumnIndex() == config.group) {
                                    readGroup(cell, descriptor, childDescriptor, subDescriptor, sheet, level);
                                } else if (cell.getColumnIndex() == config.groupName) {
                                    readGroupName(cell, descriptor, childDescriptor, subDescriptor, sheet, level);
                                } else if (cell.getColumnIndex() == config.mirage) {
                                    readMirage(cell, descriptor, childDescriptor, subDescriptor, sheet, level);
                                } else if (cell.getColumnIndex() == config.allowBypass) {
                                    readNotRecorded(cell, descriptor, childDescriptor, subDescriptor, sheet, level);
                                } else if (cell.getColumnIndex() == config.review) {
                                    readReview(cell, descriptor, childDescriptor, subDescriptor, sheet, level);
                                } 
                                else {
                                    continue;
                                }
                            }
                        }
                    }
                }
                checkTypes(descriptorList, sheet);
                sheetMap.put(sheet.getSheetName(), descriptorList);
            }
            fileReader.close();
            return sheetMap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void readReview(Cell cell, Descriptor descriptor, Descriptor childDescriptor, Descriptor subDescriptor,
            Sheet sheet, int level) {
        if (cell != null && !cell.getStringCellValue().isEmpty()) {
            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                String review = cell.getRichStringCellValue().getString();
                if (review != null && !review.trim().isEmpty()) {
                    if (level == 2) {
                        subDescriptor.setReview(true);
                    } else if (level == 1) {
                        childDescriptor.setReview(true);
                    } else if (level == 0){
                        descriptor.setReview(true);
                    }
                }
            }
        }   
    }

    private void readNotRecorded(Cell cell, Descriptor descriptor, Descriptor childDescriptor, Descriptor subDescriptor,
            Sheet sheet, int level) {
        if (cell != null && !cell.getStringCellValue().isEmpty()) {
            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                String notRecorded = cell.getRichStringCellValue().getString();
                if (notRecorded != null && !notRecorded.trim().isEmpty()) {
                    if (level == 2) {
                        //if (notRecorded.contains("recorded"))
                            subDescriptor.setAllowNotRecorded(true);
                            subDescriptor.setAllowNotApplicable(true);
                        //if (notRecorded.contains("available"))
                        //    subDescriptor.setAllowNotApplicable(true);
                    } else if (level == 1) {
                        //if (notRecorded.contains("recorded"))
                            childDescriptor.setAllowNotRecorded(true);
                            childDescriptor.setAllowNotApplicable(true);
                        //if (notRecorded.contains("available"))
                        //    childDescriptor.setAllowNotApplicable(true);
                    } else if (level == 0){
                        //if (notRecorded.contains("recorded"))
                            descriptor.setAllowNotRecorded(true);
                            descriptor.setAllowNotApplicable(true);
                        //if (notRecorded.contains("available"))
                        //    descriptor.setAllowNotApplicable(true);
                    }
                }
            }
        }
        
    }

    /**
     * reads the value for mandate group name
     * 
     * @param cell cell to be read
     * @param descriptor descriptor to be modified
     * @param childDescriptor child descriptor to be modified
     * @param subDescriptor child of child descriptor to be modified
     * @param sheet which sheet the script is reading
     * @param level level of the descriptor
     */
    private void readGroupName(Cell cell, Descriptor descriptor, Descriptor childDescriptor, Descriptor subDescriptor,
            Sheet sheet, int level) {
        if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
            String name = cell.getRichStringCellValue().getString();
            if (level == 1) {
                childDescriptor.setGroupName(name);
            } else if (level == 0){
                descriptor.setGroupName(name);
            } else if (level == 2){
                subDescriptor.setGroupName(name);
            }
        } else {
            errorOut.println("ERROR: Invalid value for mandate group name on sheet: "
                    + sheet.getSheetName() + " at row " + (cell.getRowIndex() + 1)
                    + " column: " + (cell.getColumnIndex() + 1));
        } 
        
    }

    /***
     * Reads the values for multiplicity and adds to the descriptor
     * @param cell cell to be read
     * @param descriptor descriptor to be modified
     * @param childDescriptor child descriptor to be modified
     * @param subDescriptor child of child descriptor to be modified
     * @param sheet which sheet the script is reading
     * @param level level of the descriptor
     */
    public static void readMultiplicty(Cell cell, Descriptor descriptor, Descriptor childDescriptor, Descriptor subDescriptor, Sheet sheet, int level) {
        String errorMessage = "ERROR: Invalid value for multiplicity on sheet: "
                + sheet.getSheetName() + " at row: " + (cell.getRowIndex() + 1)
                + " column: " + (cell.getColumnIndex() + 1);
        if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            Double cellValue = cell.getNumericCellValue();
            int cellInt = cellValue.intValue();
            String multiplicity = Integer.toString(cellInt);
            if (level == 2) {
                if (multiplicity.equals("1")) {
                    subDescriptor.setMultiplicity(multiplicity);
                } else {
                    errorOut.println(errorMessage);
                }
            }
            else if (level == 1) {
                if (multiplicity.equals("1")) {
                    childDescriptor.setMultiplicity(multiplicity);
                } else {
                    errorOut.println(errorMessage);
                }
            } else if (level == 0) {
                if (multiplicity.equals("1")) {
                    descriptor.setMultiplicity(multiplicity);
                } else {
                    errorOut.println(errorMessage);
                }
            }
        } else if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
            String multiplicity = cell.getRichStringCellValue().getString();
            if (level == 0) {
                if (multiplicity.equals("n") || multiplicity.equals("1")) {
                    descriptor.setMultiplicity(multiplicity);
                } else if (multiplicity.equals("0-n")) {
                    descriptor.setMultiplicity("n");
                } else {
                    errorOut.println(errorMessage);
                    
                }
            } else if (level == 1) {
                if (multiplicity.equals("n") || multiplicity.equals("0-1")
                        || multiplicity.equals("1-n") || multiplicity.equals("1")) {
                    childDescriptor.setMultiplicity(multiplicity);
                } else if (multiplicity.equals("0-n")) {
                    childDescriptor.setMultiplicity("n");
                } else {
                    errorOut.println(errorMessage);
                }
            } else if (level == 2) {
                if (multiplicity.equals("n") || multiplicity.equals("0-1")
                        || multiplicity.equals("1-n") || multiplicity.equals("1")) {
                    subDescriptor.setMultiplicity(multiplicity);
                } else if (multiplicity.equals("0-n")) {
                    subDescriptor.setMultiplicity("n");
                } else {
                    errorOut.println(errorMessage);
                }
            }
        }
    }
    
    /***
     * Reads the values for description and adds to the descriptor 
     * @param cell cell to be read
     * @param descriptor descriptor to be modified
     * @param childDescriptor child descriptor to be modified
     * @param sheet sheet that is currently being read
     * @param level level of the descriptor: 0, 1, 2
     */
    public static void readDescription(Cell cell, Descriptor descriptor, Descriptor childDescriptor, Descriptor subDescriptor, Sheet sheet, int level) {
        if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
            String description = cell.getRichStringCellValue().getString();
            if (level == 1) {
                childDescriptor.setDescription(description);
            } else if (level == 0){
                descriptor.setDescription(description);
            } else if (level == 2){
                subDescriptor.setDescription(description);
            }
        } else {
            errorOut.println("ERROR: Invalid value for description on sheet: "
                    + sheet.getSheetName() + " at row " + (cell.getRowIndex() + 1)
                    + " column: " + (cell.getColumnIndex() + 1));
        } 
    }
    
    /***
     * Reads the values for type and adds to the descriptor 
     * @param cell cell to be read
     * @param descriptor descriptor to be modified
     * @param childDescriptor child descriptor to be modified
     * @param subDescriptor child of child descriptor to be modified
     * @param sheet sheet that is currently being read
     * @param level level of the descriptor
     */
    public static void readType(Cell cell, Descriptor descriptor, Descriptor childDescriptor, Descriptor subDescriptor, Sheet sheet, int level) {
        if (cell != null && !cell.getStringCellValue().isEmpty()) {
            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                String type = cell.getRichStringCellValue().getString();
                if (Arrays.asList(allowedTypesList).contains(type)) {
                    if (level == 2) {
                        subDescriptor.setType(type);
                    } else if (level == 1) {
                        childDescriptor.setType(type);
                    } else if (level == 0){
                        descriptor.setType(type);
                    }
                }
            }
        }
    }
    
    /***
     * Reads whether the descriptor is mandatory or not
     * @param cell cell to be read
     * @param descriptor descriptor to be modified
     * @param childDescriptor child descriptor to be modified
     * @param subDescriptor child of child descriptor to be modified
     * @param sheet sheet that is currently being read
     * @param level level of the descriptor: 0, 1, 2
     */
    public static void readMandatory(Cell cell, Descriptor descriptor, Descriptor childDescriptor, Descriptor subDescriptor, Sheet sheet, int level) {
        if (cell != null && !cell.getStringCellValue().isEmpty()) {
            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                String mandatoryCell = cell.getRichStringCellValue().getString();
                Boolean mandatory = mandatoryCell.equalsIgnoreCase("y");
                if (level == 2) {
                    subDescriptor.setMandatory(mandatory);
                } else if (level == 1) {
                    childDescriptor.setMandatory(mandatory);
                } else if (level == 0){
                    descriptor.setMandatory(mandatory);
                }
            }
        }
    }
    
    /***
     * Checks to see if the types are valid
     * @param descriptorList List of descriptors
     * @param sheet The current sheet
     */
    public static void checkTypes(List<Descriptor> descriptorList, Sheet sheet) {
        for (Descriptor descriptor : descriptorList) {
            if (descriptor.getType().equals("")) {
                if (descriptor.getChildren() == null) {
                    errorOut.println("ERROR: Invalid or missing value for type on sheet: "
                            + sheet.getSheetName() + " for descriptor: " + descriptor.getName());
                } else {
                    checkTypes (descriptor.getChildren(), sheet);
                }
            }
        }
    }
    
    /***
     * Reads the values for selection and adds to the descriptor 
     * @param cell cell to be read
     * @param descriptor descriptor to be modified
     * @param childDescriptor child descriptor to be modified
     * @param sheet sheet that is currently being read
     * @param level level of the descriptor: 0, 1, 2
     */
    public static void readSelectionOrDictionary(Cell cell, Descriptor descriptor, Descriptor childDescriptor, Descriptor subDescriptor, Sheet sheet, int level) {
        if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
            String selection = cell.getRichStringCellValue().getString().trim();
            if (!selection.equals("")) {
                if (selection.startsWith("List:")) 
                    selection = selection.substring(selection.indexOf("List:")+5).trim();
                String[] selectionItems = selection.split(SEPERATOR);
                String[] trimmed = new String[selectionItems.length];
                int i=0;
                for (String item: selectionItems) {
                    trimmed[i++] = item.trim();
                }
                if (level == 0) {
                    if (descriptor.getType().equals("selection")) {                                     
                        descriptor.setSelection(trimmed);        
                    }
                    else if (descriptor.getType().equals("dictionary")) {
                        descriptor.setDictionary(selection);
                    }
                } else if (level == 1) {
                    if (childDescriptor.getType().equals("selection")) {
                        childDescriptor.setSelection(trimmed);
                    } else if (descriptor.getType().equals("dictionary")) {
                        childDescriptor.setDictionary(selection);
                    }
                }
                else if (level == 2) {
                    if (subDescriptor.getType().equals("selection")) {
                        subDescriptor.setSelection(trimmed);
                    } else if (descriptor.getType().equals("dictionary")) {
                        subDescriptor.setDictionary(selection);
                    }
                }
            } else {
                errorOut.println("ERROR: No value for dictionary/list provided on sheet: "
                        + sheet.getSheetName() + " at row: " + (cell.getRowIndex() + 1)
                        + " column: " + (cell.getColumnIndex() + 1));
            }
        } else {
            errorOut.println("ERROR: value for dictionary/list on sheet: " + sheet.getSheetName()
                    + " at row: " + (cell.getRowIndex() + 1) + " column: "
                    + (cell.getColumnIndex() + 1));
        }
    }
    
    
    /***
     * Reads the values for example and adds to the descriptor 
     * @param cell cell to be read
     * @param descriptor descriptor to be modified
     * @param childDescriptor child descriptor to be modified
     * @param sheet sheet that is currently being read
     * @param level level of the descriptor: 0, 1, 2
     */
    public static void readExample(Cell cell, Descriptor descriptor, Descriptor childDescriptor, Descriptor subDescriptor, Sheet sheet, int level) {
        if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
            String example = cell.getRichStringCellValue().getString();
            if (!example.equals("")) {
                if (level == 1) {
                    childDescriptor.setExample(example);
                } else if (level == 0){
                    descriptor.setExample(example);
                } else if (level == 2){
                    subDescriptor.setExample(example);
                }
            } else {
                warningOut.println("WARNING: No value for Example provided on sheet: "
                        + sheet.getSheetName() + " at row: " + (cell.getRowIndex() + 1)
                        + " column: " + (cell.getColumnIndex() + 1));
            }
        } else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            String example = cell.getNumericCellValue() + "";
            if (!example.equals("")) {
                if (level == 1) {
                    childDescriptor.setExample(example);
                } else if (level == 0){
                    descriptor.setExample(example);
                } else if (level == 2){
                    subDescriptor.setExample(example);
                }
            } else {
                warningOut.println("WARNING: No value for Example provided on sheet: "
                        + sheet.getSheetName() + " at row: " + (cell.getRowIndex() + 1)
                        + " column: " + (cell.getColumnIndex() + 1));
            }
            
        } else {
            errorOut.println("ERROR: Invalid value for example on sheet: "
                    + sheet.getSheetName() + " at row: " + (cell.getRowIndex() + 1)
                    + " column: " + (cell.getColumnIndex() + 1));
        }
    }
    
    /***
     * Reads the values for wiki url and adds to the descriptor 
     * @param cell cell to be read
     * @param descriptor descriptor to be modified
     * @param childDescriptor child descriptor to be modified
     * @param sheet sheet that is currently being read
     * @param level level of the descriptor: 0, 1, 2
     */
    public static void readWiki(Cell cell, Descriptor descriptor, Descriptor childDescriptor, Descriptor subDescriptor, Sheet sheet, int level) {
        if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
            String wiki = cell.getRichStringCellValue().getString();
            if (!wiki.trim().isEmpty()) {
                String wikiLink = descriptor.getName().replace(" ", "_");
                if (level == 1) {
                    childDescriptor.setWikiLink(wiki.trim() + "#" + wikiLink);
                } else if (level == 0) {
                    descriptor.setWikiLink(wiki.trim() + "#" + wikiLink);
                } else if (level == 2){
                    subDescriptor.setWikiLink(wiki.trim() + "#" + wikiLink);
                }
            } else {
                warningOut.println("WARNING: No value for wiki link provided on sheet: "
                        + sheet.getSheetName() + " at row: " + (cell.getRowIndex() + 1)
                        + " column: " + (cell.getColumnIndex() + 1));
            }
        } else {
            errorOut.println("ERROR: Invalid value for wiki link on sheet: "
                    + sheet.getSheetName() + " at row: " + (cell.getRowIndex() + 1)
                    + " column: " + (cell.getColumnIndex() + 1));
        }
    }
    
    /***
     * Reads the values for  group and adds to the descriptor 
     * @param cell cell to be read
     * @param descriptor descriptor to be modified
     * @param childDescriptor child descriptor to be modified
     * @param sheet sheet that is currently being read
     * @param level level of the descriptor: 0, 1, 2
     */
    public static void readGroup(Cell cell, Descriptor descriptor, Descriptor childDescriptor, Descriptor subDescriptor, Sheet sheet, int level) {
        if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {  // assume xor group
            try {
                Double group = cell.getNumericCellValue();
                if (group != null) {
                    if (level == 1) {
                        childDescriptor.setGroup(group.intValue());
                    } else if (level == 0){
                        descriptor.setGroup(group.intValue());
                    } else if (level == 2){
                        subDescriptor.setGroup(group.intValue());
                    }
                }
            } catch (NumberFormatException e) {
                warningOut.println("WARNING: Invalid value for group on sheet: "
                        + sheet.getSheetName() + " at row: " + (cell.getRowIndex() + 1)
                        + " column: " + (cell.getColumnIndex() + 1));
            }
        } else if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
            // first letter "O" or "X"
            String groupInfo = cell.getStringCellValue().trim();
            if (!(groupInfo.startsWith("O") || groupInfo.startsWith("X") || groupInfo.startsWith("o") || groupInfo.startsWith("x"))) {
                errorOut.println("ERROR: Invalid value for group on sheet: "
                        + sheet.getSheetName() + " at row: " + (cell.getRowIndex() + 1)
                        + " column: " + (cell.getColumnIndex() + 1));
            } else {
                if (groupInfo.startsWith("O") || groupInfo.startsWith("o")) {
                    try {
                        String numberString = groupInfo.substring(1);
                        Integer group = Integer.parseInt(numberString);
                        if (level == 1) {
                            childDescriptor.setXor(false);
                            childDescriptor.setGroup(group.intValue());
                        } else if (level == 0){
                            descriptor.setGroup(group.intValue());
                            descriptor.setXor(false);
                        } else if (level == 2){
                            subDescriptor.setGroup(group.intValue());
                            subDescriptor.setXor(false);
                        }
                    } catch (NumberFormatException e) {
                        warningOut.println("WARNING: Invalid value for group on sheet: "
                                + sheet.getSheetName() + " at row: " + (cell.getRowIndex() + 1)
                                + " column: " + (cell.getColumnIndex() + 1));
                    }
                } else { // else it is xor and that is the default 
                    try {
                        String numberString = groupInfo.substring(1);
                        Integer group = Integer.parseInt(numberString);
                        if (level == 1) {
                            childDescriptor.setXor(true);
                            childDescriptor.setGroup(group.intValue());
                        } else if (level == 0){
                            descriptor.setGroup(group.intValue());
                            descriptor.setXor(true);
                        } else if (level == 2){
                            subDescriptor.setGroup(group.intValue());
                            subDescriptor.setXor(true);
                        }
                    } catch (NumberFormatException e) {
                        warningOut.println("WARNING: Invalid value for group on sheet: "
                                + sheet.getSheetName() + " at row: " + (cell.getRowIndex() + 1)
                                + " column: " + (cell.getColumnIndex() + 1));
                    }
                }
            }
        }
    }
    
    /**
     * Reads the value of whether the descriptor is part of MIRAGE guidelines or not
     * @param cell cell to be read
     * @param descriptor descriptor to be modified
     * @param childDescriptor child descriptor to be modified
     * @param subDescripor child of a child descriptor to be modified
     * @param sheet sheet that is currently being read
     * @param level level of the descriptor: 0, 1, 2
     */
    private void readMirage(Cell cell, Descriptor descriptor, Descriptor childDescriptor, Descriptor subDescriptor,
            Sheet sheet, int level) {
        if (cell != null && !cell.getStringCellValue().isEmpty()) {
            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                String mirageCell = cell.getRichStringCellValue().getString();
                Boolean mirage = mirageCell.equalsIgnoreCase("y");
                if (level == 2) {
                    subDescriptor.setMirage(mirage);
                } else if (level == 1) {
                    childDescriptor.setMirage(mirage);
                } else if (level == 0){
                    descriptor.setMirage(mirage);
                }
            }
        }
        
    }
    
    /***
     * Reads the values for measurement and adds to the descriptor 
     * @param cell cell to be read
     * @param descriptor descriptor to be modified
     * @param childDescriptor child descriptor to be modified
     * @param subDescriptor child or child descriptor to be modified
     * @param sheet sheet that is currently being read
     * @param level level of the descriptor
     */
    public static void readMeasurement(Cell cell, Descriptor descriptor, Descriptor childDescriptor, Descriptor subDescriptor, Sheet sheet, int level) {
        if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
            String units = cell.getRichStringCellValue().getString();
            String[] measurements = units.split(SEPERATOR2);
            if (level == 1) {
                childDescriptor.setMeasurement(measurements);
            } else if (level == 0){
                descriptor.setMeasurement(measurements);
            } else if (level == 2){
                subDescriptor.setMeasurement(measurements);
            }
        } else {
           errorOut.println("ERROR: Invalid value for unit of measurement on sheet: "
                    + sheet.getSheetName() + " at row: " + (cell.getRowIndex() + 1)
                    + " column: " + (cell.getColumnIndex() + 1));
        }   
    }
    
    /***
     * Iterates through columns and prints out error message for each
     * @param sheet sheet the error is on
     * @param row row the error is in
     * @param columnNumber index of column
     */
    public static void cellEmptyError(Sheet sheet, Row row, Config config, int columnNumber) { 
        if (columnNumber == config.multiplicity) {
            errorOut.println("ERROR: No value for multiplicity on sheet: " + sheet.getSheetName()
                + " at row: " + (row.getRowNum() + 1) + " column: " + (columnNumber + 1));
        } else if (columnNumber == config.description) {
            errorOut.println("ERROR: No value for description on sheet: " + sheet.getSheetName()
                + " at row: " + (row.getRowNum() + 1) + " column: " + (columnNumber + 1));
        } else if (columnNumber == config.unit) {
            warningOut.println("WARNING: No value for unit of measurement on sheet: "
                + sheet.getSheetName() + " at row: " + (row.getRowNum() + 1) + " column: "
                + (columnNumber + 1));
        }
    }

    /**
     * Checks if all name columns are empty
     * @param row row to be checked
     * @param sheet the sheet that is being checked
     * @return true if empty
     */
    private boolean nameColumnsEmpty(Row row, Sheet sheet) {
        Cell nameCell = row.getCell(0);
        Cell childNameCell = row.getCell(1);
        Cell subNameCell = row.getCell(2);
        if ((nameCell == null || nameCell.getCellType() == Cell.CELL_TYPE_BLANK) 
            && (childNameCell == null || childNameCell.getCellType() == Cell.CELL_TYPE_BLANK)
            && (subNameCell == null || subNameCell.getCellType() == Cell.CELL_TYPE_BLANK)) { 
            errorOut.println("ERROR: No value for name on sheet: " + sheet.getSheetName()
                + " at row: " + (row.getRowNum() + 1));
            return true;
        }
        return false;
    }
    
    /**
     * Checks whether or not the dictionary is column when type is specified as dictionary
     * @param row row where type was said to be dictionary
     * @param sheet sheet where type was said to be dictionary
     */
    private void checkDictionaryEmpty(Row row, Config config, Sheet sheet) {
        Cell typeCell = row.getCell(config.type);
        Cell dictionaryCell = row.getCell(config.dictionary);
        if (!(typeCell == null || typeCell.getCellType() == Cell.CELL_TYPE_BLANK)) {
            String type = typeCell.getRichStringCellValue().getString();
            if (type.equals("dictionary") && (dictionaryCell == null || dictionaryCell.getCellType() == Cell.CELL_TYPE_BLANK)) {
                errorOut.println("ERROR: No value for dictionary on sheet: " + sheet.getSheetName()
                        + " at row: " + (row.getRowNum() + 1) + " column: 7");
            } 
        }
    }
    
    /**
     * Checks if selection column is empty when type is said to be selection
     * @param row row where type is said to be selection
     * @param sheet sheet where type is said to be selection
     */
    private void checkSelectionEmpty(Row row, Config config, Sheet sheet) {
        Cell typeCell = row.getCell(config.type);
        Cell selectionCell = row.getCell(config.dictionary);
        if (!(typeCell == null || typeCell.getCellType() == Cell.CELL_TYPE_BLANK)) {
            String selection = typeCell.getRichStringCellValue().getString();
            if (selection.equals("selection") && (selectionCell == null || selectionCell.getCellType() == Cell.CELL_TYPE_BLANK)) {
                errorOut.println("ERROR: No items for selection on sheet: " + sheet.getSheetName()
                        + " at row: " + (row.getRowNum() + 1) + " column: 6");
            }
        }
    }
    
    /**
     * Checks to see if the row is empty or not
     * @param row row to be checked
     * @return boolean that represents if the row is empty
     */
    public static boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK)
                return false;
        }
        return true;
    }
    
    /***
     * Finds duplicates within the same excel file
     * @param descriptorList List of descriptors currently created
     * @param inDescriptor the descriptor to be added to the list
     * @return boolean representing if the descriptor is duplicate
     */
    public static boolean findDuplicates(List<Descriptor> descriptorList, Descriptor inDescriptor) {
        boolean duplicateFound = false;
        for (Descriptor descriptor : descriptorList) {
            if (descriptor.getChildren() == null) {
                if (descriptor.getName().trim().equalsIgnoreCase(inDescriptor.getName().trim())) {
                    duplicateFound = true;
                    warningOut.println("WARNING: This descriptor: " + inDescriptor.getName() + " is a duplicate within the excel file!");
                }
            }
        }
        return duplicateFound;
    }
    
    private void createOntology(HashMap<String, List<Descriptor>> mp, String type) {
        List<MetadataTemplate> templates = new ArrayList<MetadataTemplate>();
        for (String sheetName: mp.keySet()) {
            MetadataTemplate metadataTemplate = new MetadataTemplate();
            MetadataTemplateType mType = MetadataTemplateType.forValue(type);
            String firstLetter = mType.name().substring(0, 1);
            String rest = mType.name().substring(1);
            metadataTemplate.setName(sheetName + " " + firstLetter + rest.toLowerCase());
            metadataTemplate.setType (mType);
            List<DescriptionTemplate> descriptors = new ArrayList<DescriptionTemplate>();
            int i=1;
            for (Descriptor d: mp.get(sheetName)) {
                DescriptionTemplate description = createDescription(d);
                description.setOrder(i++);
                descriptors.add(description);
            }
            metadataTemplate.setDescriptors(descriptors);
            templates.add(metadataTemplate);
        }
        
        // copy common descriptors to all other templates
        MetadataTemplate commonTemplate = null;
        for (MetadataTemplate template: templates) {
            if (template.getName().startsWith("Common")) {
                commonTemplate = template;
            }
        }
        if (commonTemplate != null) {
            for (MetadataTemplate template: templates) {
                if (!template.getName().startsWith("Common")) {
                    int commonSize = commonTemplate.getDescriptors().size();
                    //advance the order to be able to put the common descriptors to the beginning
                    for (DescriptionTemplate d: template.getDescriptors()) {
                        d.setOrder(commonSize + d.getOrder());
                    }
                    template.getDescriptors().addAll(commonTemplate.getDescriptors());
                }
            }
        }
        templates.remove(commonTemplate);
        
        // read the ontology (template ontology) add templates and write back into the OWL file
        try {
            InputStream inputStream = new FileInputStream(new File("ontology/gadr-template.owl"));
            Model model = Rio.parse(inputStream, "http://purl.org/gadr/template", RDFFormat.RDFXML);
            ValueFactory f = SimpleValueFactory.getInstance();
            
            int lastIdUsed = 1;
            Iterator<Statement> itr = model.iterator();
            while (itr.hasNext()) {
                Statement st = itr.next();
                String uri = st.getSubject().stringValue();
                if (uri.contains ("Metadata")) {
                    String idPart = uri.substring(uri.indexOf("Metadata")+8);
                    try {
                        int id = Integer.parseInt(idPart);
                        if (id > lastIdUsed)
                            lastIdUsed = id;
                    } catch (NumberFormatException e) {
                        System.err.println("Could not extract id");
                        e.printStackTrace();
                    }
                }
            }
            
            // add templates/descriptors etc. as individuals
            int id = lastIdUsed+1;
            
            for (MetadataTemplate template: templates) {
                IRI metadataIRI = f.createIRI( prefix + "Metadata" + id);
                IRI typeIRI = f.createIRI(prefix + template.getType().getLabel());
                model.add(f.createStatement(metadataIRI, RDF.TYPE, typeIRI));
                model.add(f.createStatement(metadataIRI, RDFS.LABEL, f.createLiteral(template.getName())));
                if (template.getDescription() != null)
                    model.add(f.createStatement(metadataIRI, RDFS.COMMENT, f.createLiteral(template.getDescription())));
                
                for (DescriptionTemplate description: template.getDescriptors()) {
                    String descriptionURI = addDescriptionToOntology (model, f, description, id);
                    descriptorId++;
                    model.add(f.createStatement(metadataIRI, f.createIRI(prefix + "has_description_context"), f.createIRI(descriptionURI)));
                }
                id++;
            }
            
            Rio.write(model, new FileOutputStream("ontology/gadr-template-individuals.owl"), RDFFormat.RDFXML);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
    }
    
    private String addDescriptionToOntology(Model model, ValueFactory f, DescriptionTemplate description, int metadataId) {
        String uri = prefix + "DescriptionContext" + descriptorId;
        IRI descriptionContext = f.createIRI(uri);
        if (description instanceof DescriptorTemplate) {
            model.add(f.createStatement(descriptionContext, RDF.TYPE, f.createIRI(prefix + "simple_description_context")));
            String descriptorURI = prefix + "Descriptor" + descriptorId;
            IRI descriptor = f.createIRI(descriptorURI);
            IRI hasDescriptor = f.createIRI(prefix + "has_descriptor");
            model.add(f.createStatement(descriptor, RDF.TYPE, f.createIRI(dataprefix + "descriptor")));
            model.add(f.createStatement(descriptor, RDFS.LABEL, f.createLiteral(description.getName())));
            if (description.getDescription() != null)
                model.add(f.createStatement(descriptor, RDFS.COMMENT, f.createLiteral(description.getDescription())));
            model.add(f.createStatement(descriptionContext, hasDescriptor, descriptor));
            if (((DescriptorTemplate) description).getUnits() != null && !((DescriptorTemplate) description).getUnits().isEmpty()) {
                IRI hasUnit = f.createIRI(dataprefix + "has_unit_of_measurement");
                for (String unit: ((DescriptorTemplate) description).getUnits()) {
                    model.add(f.createStatement(descriptionContext, hasUnit, f.createLiteral(unit)));
                }
            }
            // namespace
            if (((DescriptorTemplate) description).getNamespace() != null) {
                Namespace namespace = ((DescriptorTemplate) description).getNamespace();
                IRI hasNamespace = f.createIRI(prefix + "has_namespace");
                IRI hasFile = f.createIRI(prefix + "has_file");
                IRI hasSelection = f.createIRI(prefix + "has_selection");
                if (namespace.getName().equalsIgnoreCase("selection") || namespace.getName().equalsIgnoreCase("dictionary")) {
                    // create namespace object in the ontology
                    //TODO how to handle dictionary?
                    //through files?
                    IRI namespaceIRI = f.createIRI(prefix + "Namespace-" + namespace.getName() + descriptorId);
                    model.add(f.createStatement(namespaceIRI, RDF.TYPE, f.createIRI(prefix + "namespace")));
                    model.add(f.createStatement(descriptor, hasNamespace, namespaceIRI));
                    model.add(f.createStatement(namespaceIRI, RDFS.LABEL, f.createLiteral(namespace.getName())));
                    if (namespace.getFilename() != null)
                        model.add(f.createStatement(namespaceIRI, hasFile, f.createLiteral(namespace.getFilename())));
                    if (((DescriptorTemplate) description).getSelectionList() != null 
                            && !((DescriptorTemplate) description).getSelectionList().isEmpty()) {
                        for (String selection: ((DescriptorTemplate) description).getSelectionList()) {
                            model.add(f.createStatement(namespaceIRI, hasSelection, f.createLiteral(selection)));
                        }
                    }
                } else {
                    // just add the object property
                    if (namespace.getUri() == null) {
                        errorOut.append("no uri for namespace " + namespace.getName() + " for descriptor " + description.getName());
                    } else {
                        model.add(f.createStatement(descriptor, hasNamespace, f.createIRI(namespace.getUri())));
                    }
                }
            }
            
        } else if (description instanceof DescriptorGroupTemplate) {
            model.add(f.createStatement(descriptionContext, RDF.TYPE, f.createIRI(prefix + "complex_description_context")));
            model.add(f.createStatement(descriptionContext, RDFS.LABEL, f.createLiteral(description.getName())));
            if (description.getDescription() != null)
                model.add(f.createStatement(descriptionContext, RDFS.COMMENT, f.createLiteral(description.getDescription())));
            
            for (DescriptionTemplate d: ((DescriptorGroupTemplate) description).getDescriptors()) {
                descriptorId++;
                String descURI = addDescriptionToOntology(model, f, d, metadataId);
                model.add(f.createStatement(descriptionContext, f.createIRI(prefix + "has_description_context"), f.createIRI(descURI)));
            }
        }
        
        IRI cardinality = f.createIRI(prefix + "cardinality");
        IRI isRequired = f.createIRI(prefix + "is_required");
        IRI hasExample = f.createIRI(prefix + "has_example");
        IRI hasUrl = f.createIRI(prefix + "has_url");
        IRI hasGroup = f.createIRI(prefix + "has_mandate_group");
        IRI isXor = f.createIRI(prefix + "is_xor");
        IRI isMirage = f.createIRI(prefix + "is_mirage");
        IRI hasOrder = f.createIRI(prefix + "has_order");
        IRI hasGroupId = f.createIRI(prefix + "has_id");
        IRI allowNotRecorded = f.createIRI(prefix + "allows_not_recorded");
        IRI allowNotApplicable = f.createIRI(prefix + "allows_not_applicable");
        IRI allowReview = f.createIRI(prefix + "allows_review");
        Literal card = description.getMaxOccurrence() == 1 ? f.createLiteral("1"): f.createLiteral("n");
        Literal required = f.createLiteral(description.isMandatory());
        model.add(f.createStatement(descriptionContext, cardinality, card));
        model.add(f.createStatement(descriptionContext, isRequired, required));
        if (description.getExample() != null) {
            model.add(f.createStatement(descriptionContext, hasExample, f.createLiteral(description.getExample())));
        }
        if (description.getAllowNotRecorded() != null && description.getAllowNotRecorded()) {
            model.add(f.createStatement(descriptionContext, allowNotRecorded, f.createLiteral(true)));
        } else {
            model.add(f.createStatement(descriptionContext, allowNotRecorded, f.createLiteral(false)));
        }
        if (description.getAllowNotApplicable() != null && description.getAllowNotApplicable()) {
            model.add(f.createStatement(descriptionContext, allowNotApplicable, f.createLiteral(true)));
        } else {
            model.add(f.createStatement(descriptionContext, allowNotApplicable, f.createLiteral(false)));
        }
        if (description.getWikiLink() != null) {
            model.add(f.createStatement(descriptionContext, hasUrl, f.createLiteral(description.getWikiLink())));
        }
        if (description.getMandateGroup() != null) {
            boolean xOR = description.getMandateGroup().getxOrMandate() != null && description.getMandateGroup().getxOrMandate();
            IRI mandateIRI = f.createIRI (prefix + "mandateGroup" +  metadataId + "-" + (xOR ? "XOR" : "OR") + description.getMandateGroup().getId());
            model.add(f.createStatement(mandateIRI, RDF.TYPE, f.createIRI(prefix + "mandate_group")));
            model.add(f.createStatement(descriptionContext, hasGroup, mandateIRI));
            if (description.getMandateGroup().getxOrMandate() != null) {
                model.add(f.createStatement(mandateIRI, isXor, f.createLiteral(description.getMandateGroup().getxOrMandate())));
            }
            if (description.getMandateGroup().getName() != null)
                model.add(f.createStatement(mandateIRI, RDFS.LABEL, f.createLiteral(description.getMandateGroup().getName())));
            model.add(f.createStatement(mandateIRI, hasGroupId, f.createLiteral(description.getMandateGroup().getId())));
            
        }
        if (description.isMirage() != null) {
            model.add(f.createStatement(descriptionContext, isMirage, f.createLiteral(description.isMirage())));
        }
        
        if (description.getOrder() != null) {
            model.add(f.createStatement(descriptionContext, hasOrder, f.createLiteral(description.getOrder())));
        }
        
        if (description.getReview() != null && description.getReview()) {
            model.add(f.createStatement(descriptionContext, allowReview, f.createLiteral(true)));
        } else {
            model.add(f.createStatement(descriptionContext, allowReview, f.createLiteral(false)));
        }
        
        return uri;
    }

    DescriptionTemplate createDescription (Descriptor d) {
        DescriptionTemplate description = null;
        if (d.getChildren() != null && !d.getChildren().isEmpty()) {
            // top level descriptor group
            description = new DescriptorGroupTemplate();
            List<DescriptionTemplate> descriptors = new ArrayList<DescriptionTemplate>();
            int i=1;
            for (Descriptor child: d.getChildren()) {
                DescriptionTemplate childTemplate = createDescription(child);
                childTemplate.setOrder(i++);
                descriptors.add(childTemplate);
            }
            ((DescriptorGroupTemplate)description).setDescriptors(descriptors);
        } else {
            description = new DescriptorTemplate();
            Namespace namespace = new Namespace();
            namespace.setName(d.getType());
            if (d.getType().equalsIgnoreCase("text")) {
                namespace.setUri("http://www.w3.org/2001/XMLSchema#token");
            } else if (d.getType().equalsIgnoreCase("longtext")) {
                namespace.setUri("http://www.w3.org/2001/XMLSchema#string");
            } else if (d.getType().equalsIgnoreCase("Number")) {
                namespace.setUri("http://www.w3.org/2001/XMLSchema#double");
            } else if (d.getType().equalsIgnoreCase("Date")) {
                namespace.setUri("http://www.w3.org/2001/XMLSchema#date");
            } else if (d.getType().equalsIgnoreCase("Boolean")) { 
                namespace.setUri("http://www.w3.org/2001/XMLSchema#boolean");
            } else if (d.getType().equalsIgnoreCase("selection")) {
                if (d.getSelection() != null)
                    ((DescriptorTemplate)description).setSelectionList(Arrays.asList(d.getSelection()));
            } else if (d.getType().equalsIgnoreCase("dictionary")) {
                namespace.setUri(d.getDictionary());
            }
                
            ((DescriptorTemplate)description).setNamespace(namespace);
            if (d.getMeasurement() != null) {
                ((DescriptorTemplate)description).setUnits(Arrays.asList(d.getMeasurement()));
            }
        }
        
        description.setName(d.getName());
        description.setDescription(d.getDescription());
        description.setMandatory(d.getMandatory());
        description.setMaxOccurrence(d.getMultiplicity().equals("n") ? Integer.MAX_VALUE : 1);
        description.setExample(d.getExample());
        description.setWikiLink(d.getWikiLink());
        if (d.getGroup() != null) {
            MandateGroup group = new MandateGroup();
            group.setId(d.getGroup());
            group.setxOrMandate(d.getXor());
            group.setName(d.getGroupName());
            description.setMandateGroup(group);
        }
        description.setMirage(d.getMirage());
        description.setAllowNotRecorded(d.allowNotRecorded);
        description.setAllowNotApplicable(d.getAllowNotApplicable());
        description.setReview(d.review);
        return description;
        
    }
    
    class Descriptor {
        String name;
        String multiplicity = "";
        String description = "";
        String type = "";
        String[] selection;
        String dictionary = null; 
        String example = "";
        String[] measurements;
        List<Descriptor> children;
        Boolean mandatory = false;
        Integer position = null;
        Integer group = null;
        Boolean xor = true;  // default
        String groupName;
        Boolean mirage = false;
        String wikiLink = "https://wiki.glygen.org/index.php/Main_Page";
        Boolean allowNotRecorded = false;
        Boolean allowNotApplicable = false;
        Boolean review = false;

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @param name the name to set
         */
        public void setName(String name) {
            this.name = name;
        }
        
        /**
         * @return the multiplicity
         */
        public String getMultiplicity() {
            return multiplicity;
        }

        /**
         * @param multiplicty the multiplicity to set
         */
        public void setMultiplicity(String multiplicity) {
            this.multiplicity = multiplicity;
        }
        
        /**
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /**
         * @param description the description to set
         */
        public void setDescription(String description) {
            this.description = description;
        }
        
        /**
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * @param type the type to set
         */
        public void setType(String type) {
            this.type = type;
        }
        
        /**
         * @return the selection
         */
        public String[] getSelection() {
            return selection;
        }

        /**
         * @param selection the selection to set
         */
        public void setSelection(String[] selection) {
            this.selection = selection;
        }
        
        /**
         * @return the dictionary
         */
        public String getDictionary() {
            return dictionary;
        }

        /**
         * @param dictionary the dictionary to set
         */
        public void setDictionary(String dictionary) {
            this.dictionary = dictionary;
        }

        /**
         * @return the example
         */
        public String getExample() {
            return example;
        }

        /**
         * @param example the example to set
         */
        public void setExample(String example) {
            this.example = example;
        }

        /**
         * @return the measurement
         */
        public String[] getMeasurement() {
            return measurements;
        }

        /**
         * @param measurement the measurement to set
         */
        public void setMeasurement(String[] measurements) {
            this.measurements = measurements;
        }

        /**
         * @return the children
         */
        public List<Descriptor> getChildren() {
            return children;
        }
        
        /**
         * 
         * @return the position
         */
        public Integer getPosition() {
            return position;
        }
        
        /**
         * 
         * @param position the position to set
         */
        public void setPosition(Integer position) {
            this.position = position;
        }

        /**
         * @param children the children to set
         */
        public void setChildren(List<Descriptor> children) {
            this.children = children;
        }

        /***
         * Adds a child to the list of child descriptors
         * @param childDescriptor the child to be added
         */
        public void addChild(Descriptor childDescriptor) {
            if (this.children == null) {
                this.children = new ArrayList<>(); 
            } 
            this.children.add(childDescriptor);
        }

        /**
         * @return the mandatory
         */
        public Boolean getMandatory() {
            return mandatory;
        }

        /**
         * @param mandatory the mandatory to set
         */
        public void setMandatory(Boolean mandatory) {
            this.mandatory = mandatory;
        }

        /**
         * @return the wikiLink
         */
        public String getWikiLink() {
            return wikiLink;
        }

        /**
         * @param wikiLink the wikiLink to set
         */
        public void setWikiLink(String wikiLink) {
            this.wikiLink = wikiLink;
        }

        /**
         * @return the group
         */
        public Integer getGroup() {
            return group;
        }

        /**
         * @param group the group to set
         */
        public void setGroup(Integer group) {
            this.group = group;
        }

        /**
         * @return the mirage
         */
        public Boolean getMirage() {
            return mirage;
        }

        /**
         * @param mirage the mirage to set
         */
        public void setMirage(Boolean mirage) {
            this.mirage = mirage;
        }

        /**
         * @return the xor
         */
        public Boolean getXor() {
            return xor;
        }

        /**
         * @param xor the xor to set
         */
        public void setXor(Boolean xor) {
            this.xor = xor;
        }

        /**
         * @return the groupName
         */
        public String getGroupName() {
            return groupName;
        }

        /**
         * @param groupName the groupName to set
         */
        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        /**
         * @return the allowNotRecorded
         */
        public Boolean getAllowNotRecorded() {
            return allowNotRecorded;
        }

        /**
         * @param allowNotRecorded the allowNotRecorded to set
         */
        public void setAllowNotRecorded(Boolean allowNotRecorded) {
            this.allowNotRecorded = allowNotRecorded;
        }

        /**
         * @return the allowNotApplicable
         */
        public Boolean getAllowNotApplicable() {
            return allowNotApplicable;
        }

        /**
         * @param allowNotApplicable the allowNotApplicable to set
         */
        public void setAllowNotApplicable(Boolean allowNotApplicable) {
            this.allowNotApplicable = allowNotApplicable;
        }

        /**
         * @return the review
         */
        public Boolean getReview() {
            return review;
        }

        /**
         * @param review the review to set
         */
        public void setReview(Boolean review) {
            this.review = review;
        }

    }

    /**
     * @param descriptorId the descriptorId to set
     */
    public void setDescriptorId(int descriptorId) {
        this.descriptorId = descriptorId;
    }
}

