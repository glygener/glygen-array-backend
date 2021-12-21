package org.glygen.array.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.glygen.array.persistence.rdf.Block;
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.metadata.Description;
import org.glygen.array.persistence.rdf.metadata.Descriptor;
import org.glygen.array.persistence.rdf.metadata.DescriptorGroup;
import org.glygen.array.persistence.rdf.metadata.SpotMetadata;
import org.glygen.array.view.ErrorMessage;
import org.grits.toolbox.glycanarray.library.om.layout.LevelUnit;
import org.grits.toolbox.glycanarray.om.model.UnitOfLevels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.ObjectError;

public class ExtendedGalFileParser { 

    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    @Autowired
    SpotMetadataConfig metadataConfig;
    
    @Autowired
    ParserConfiguration config;
    
    /**
     * parses the given GAL file to create a Slide Layout with all its blocks. Features should exist already. Linkers should be created
     * before hand and passed in as a list
     * 
     * @param filePath file path of the GAL file to be parsed
     * @param name name of the slide layout to be created
     * @param linkerList should contain all the existing linkers 
     * @return import result with slide layout and all the block layouts to be created
     * @throws IOException if the file cannot be found or invalid
     */
    public GalFileImportResult parse (String filePath, String name, List<Linker> linkerList) throws IOException {
        File file = new File(filePath);
        if (!file.exists())
            throw new FileNotFoundException(filePath + " does not exist!");
        
        Scanner scan = new Scanner(file);
        boolean dataStarts = false;
        String type = "GenePix ArrayList V1.0";
        Integer prevBlockLocation = -1;
        Boolean first = false;
        Block block = null;
        int maxRow = 0;
        int maxColumn = 0;
        int groupId = 1;
        int maxGroup = 0;
        Map <String, Feature> featureMap = new HashMap<>();
        Map <String, Integer> featureGroupMap = new HashMap<>();
        
        // these are the new structures to be imported into the repository
        List<Feature> featureList = new ArrayList<>();
        List<BlockLayout> layoutList = new ArrayList<>();
        
        List<ErrorMessage> errorList = new ArrayList<ErrorMessage>();
        
        SlideLayout slideLayout = new SlideLayout();
        slideLayout.setName(name);
        slideLayout.setWidth(1);   // 1 dimensional by default
        slideLayout.setDescription(type);
        slideLayout.setBlocks(new ArrayList<Block>());
        
        List<LevelUnit> levels = new ArrayList<>();
        
        BlockLayout blockLayout=null;
        
        while(scan.hasNext()){
            String curLine = scan.nextLine();
            String[] splitted = curLine.split("\t");
            if (splitted.length == 0)
                continue;
            String firstColumn = splitted[0].trim();
            
            if (firstColumn.contains("Type=")) {
                type = firstColumn.substring(firstColumn.indexOf("Type=")+5);
            }
            if (firstColumn.contains("BlockCount=")) {
                try {
                    if (firstColumn.contains("\"BlockCount=")) {
                        String blockCount = firstColumn.substring(firstColumn.indexOf("BlockCount=")+11);
                        Integer height = Integer.parseInt(blockCount.substring(0, blockCount.length()-1));
                        slideLayout.setHeight(height);
                    } else { // no quotes at the beginning and end
                        String blockCount = firstColumn.substring(firstColumn.indexOf("BlockCount=")+11);
                        Integer height = Integer.parseInt(blockCount);
                        slideLayout.setHeight(height);
                    }
                } catch (NumberFormatException e) {
                    slideLayout.setHeight(32);   // default
                }
            }
            String blockColumn = null;
            if (config.getBlockColumn() != -1) {
                blockColumn = splitted[config.getBlockColumn()].trim();
                if (blockColumn.indexOf("Block") != -1 && splitted.length > 1) {
                    dataStarts = true;
                    prevBlockLocation=1;
                    first = true;
                    continue; // skip to next line 
                }
            }
            if (dataStarts) {
                try {
                    if (blockColumn != null) {
                        // firstColumn is the location of the block
                        int blockLocation = Integer.parseInt(blockColumn.trim());
                        if (prevBlockLocation != -1 && blockLocation != prevBlockLocation) { // go to next block
                            // set the row/column/levels/groupnum for the previous block layout
                            blockLayout.setWidth(maxColumn);
                            blockLayout.setHeight(maxRow);
                            //blockLayout.setLevelUnit(levels);
                            //blockLayout.setGroupNum(maxGroup);
                            layoutList.add(blockLayout);
                            
                            // start a new one
                            blockLayout = new BlockLayout();
                            blockLayout.setName(name + "-" + blockLocation);
                            //blockLayout.setReplicNum(numOfReplicates);
                            blockLayout.setSpots(new ArrayList<Spot>());
                            prevBlockLocation = blockLocation;
                            block = new Block();
                            //block.setLayoutId(blockLayout.getId());
                            block.setBlockLayout(blockLayout);
                            block.setColumn(1);   // we assume one dimension for the slide
                            block.setRow(blockLocation);
                            slideLayout.getBlocks().add(block);
                            
                            //re-init counters and maps
                            featureMap = new HashMap<>();
                            featureGroupMap = new HashMap<>();
                            maxRow = 0;
                            maxColumn = 0;
                            maxGroup = 0;
                            groupId = 1;
                        }
                        else if (first) {
                            // first one
                            blockLayout = new BlockLayout();
                            //blockLayout.setId(blockLayoutIdCounter++);
                            blockLayout.setName(name + "-" + blockLocation);
                            //blockLayout.setReplicNum(numOfReplicates);
                            blockLayout.setSpots(new ArrayList<Spot>());
                            block = new Block();
                            //block.setLayoutId(blockLayout.getId());
                            block.setBlockLayout(blockLayout);
                            block.setColumn(1);   // we assume one dimension for the slide
                            block.setRow(blockLocation);
                            slideLayout.getBlocks().add(block);
                            first = false;
                        }
                    }
                    
                    // second and third column are spot indices
                    int x = Integer.parseInt(splitted[config.getCoordinateColumnX()].trim()); // column
                    int y = Integer.parseInt(splitted[config.getCoordinateColumnY()].trim()); // row
                    String glycanName = splitted[config.getNameColumn()].trim(); // name of the probe
                    String featureName = glycanName;
                    String concentration = splitted[config.getConcentrationColumn()].trim();
                    String featureId = splitted[config.getIdColumn()].trim();
                    String ratio = splitted[config.getRatioColumn()].trim();
                    LevelUnit levelUnit = null;
                    boolean mixture = featureName != null && featureName.contains("\\|\\|");
                    String printingFlags = splitted[config.flagColumn].trim();
                    String group = splitted[config.groupColumn].trim();
                    
                    
                    if (y > maxRow)
                        maxRow = y;
                    if (x > maxColumn)
                        maxColumn = x;
                    
                    Spot spot = new Spot();
                    spot.setColumn(x);
                    spot.setRow(y);
                    spot.setMetadata(addSpotMetadata(name + "metadata-" + x + ":" + y, splitted));
                    spot.setFlag(printingFlags);
                    //if (!group.isEmpty()) {
                    //    spot.setGroup(Integer.parseInt(group));
                    //}
                    
                    if (!mixture) {
                        levelUnit = addLevel(concentration, levels);   
                    }
                    
                    List<Feature> spotFeatures = new ArrayList<Feature>();
                
                    blockLayout.getSpots().add(spot);
                    spot.setFeatures(spotFeatures);
                
                    if (glycanName.equals("0") || glycanName.equals("\"0\"") 
                            || glycanName.equalsIgnoreCase("empty") || glycanName.equalsIgnoreCase("\"empty\"")) {
                            //|| glycanName.equalsIgnoreCase("Grid Marker") || glycanName.equalsIgnoreCase("\"Grid Marker\"")) {  //empty spots and grid markers
                        spot.setFeatures(null);    //what to set for empty spots ==> no feature assigned for the spot
                    }
                    else {
                        if (glycanName.startsWith("\"")) {
                            // remove the quotes
                            glycanName = glycanName.substring(1, glycanName.length()-1);
                            featureName = glycanName;
                        } 
                        glycanName = glycanName.trim();
                        featureName = featureName.trim();
                        if (mixture) {
                            String[] concentrations = concentration.split("\\|\\|");
                            String[] featureNames = featureName.split("\\|\\|");
                            String[] featureIds = featureId.split("\\|\\|");
                            LevelUnit[] levelUnits = new LevelUnit[concentrations.length];
                            int i=0;
                            for (String c: concentrations) {
                                levelUnits[i++] = addLevel(c, levels); 
                            }
                            i = 0;
                            for (String fName: featureNames) {
                                Feature feature = new Feature();
                                feature.setName(fName.trim());
                                feature.setId(featureIds[i]);
                                spotFeatures.add(feature);
                                spot.setConcentration(featureIds[i], levelUnits[i]);
                                featureList.add(feature);
                                i++;
                            }
                            
                            if (ratio != null && !ratio.isEmpty()) {
                                String[] ratios = ratio.split(":");
                                if (ratios.length == 2) {
                                    try {
                                        if (!ratios[0].equals("1") || !ratios[0].equals("1.0")) {
                                            logger.warn("Ratio is incorrect:" + ratio);
                                        } else {
                                            Double sum = 0.0;
                                            for (int j = 1; j < ratios.length; j++) {
                                                sum += Double.parseDouble(ratios[j]);
                                            }
                                            Double percentage1 = (1.0 - sum) * 100;
                                            spot.setRatio(featureIds[0], percentage1);
                                            for (int j = 1; j < ratios.length; j++) {
                                                Double percentage = Double.parseDouble(ratios[j]) * 100;
                                                spot.setRatio(featureIds[j], percentage);
                                            }
                                        }
                                    } catch (NumberFormatException e) {
                                        logger.warn("Ratio is incorrect:" + ratio, e);
                                        ErrorMessage error = new ErrorMessage("Ratio is incorrect: " + x +"-" + y);
                                        String[] codes = new String[] {"Row " + x, "Row " + y};
                                        error.addError(new ObjectError ("ratio", codes, null, "NotValid"));
                                        errorList.add(error);
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        logger.error("Ratio is not given correctly for all features on the spot:" + ratio);
                                        ErrorMessage error = new ErrorMessage("Ratio is not given correctly for all features on the spot: " + x +"-" + y);
                                        String[] codes = new String[] {"Row " + x, "Row " + y};
                                        error.addError(new ObjectError ("ratio", codes, null, "NotValid"));
                                        errorList.add(error);
                                    }
                                }
                            }
                            
                        } else {
                            if (featureMap.get(featureId) != null) {
                                // already created the feature
                                spotFeatures.add(featureMap.get(featureId));
                                spot.setFeatures(spotFeatures);
                                spot.setGroup(featureGroupMap.get(featureId));
                                if (levelUnit != null)
                                    spot.setConcentration(featureId, levelUnit);    
                            } else {
                                Feature feature = new Feature();
                                feature.setName(featureName.trim());
                                feature.setId(featureId);
                                spotFeatures.add(feature);
                                if (groupId > maxGroup)
                                    maxGroup = groupId;
                                
                                spot.setGroup(groupId++);
                                if (levelUnit != null)
                                    spot.setConcentration(featureId, levelUnit);  
                                featureList.add(feature);
                                
                                featureGroupMap.put(featureId, spot.getGroup());
                                featureMap.put(featureId, feature);
                            }
                        }
                    }
                    
                } catch (NumberFormatException e) {
                    // should not occur
                    logger.error("Value should have been a number", e);
                    ErrorMessage error = new ErrorMessage("Value should have been a number");
                    String[] codes = new String[] {e.getMessage()};
                    error.addError(new ObjectError ("value", codes, null, "NotValid"));
                    errorList.add(error);
                }
            }         
        }
        
        scan.close();
        
        // add the last blockLayout
        blockLayout.setWidth(maxColumn);
        blockLayout.setHeight(maxRow);
        
        layoutList.add(blockLayout);
        
        GalFileImportResult result = new GalFileImportResult();
        result.setFeatureList(featureList);
        result.setLayout(slideLayout);
        result.setLayoutList(layoutList);
        result.setErrors(errorList);
        return result;
    }
    
    
    SpotMetadata addSpotMetadata (String metadataName, String[] splittedRow) {
        SpotMetadata spotMetadata = new SpotMetadata();
        spotMetadata.setName(metadataName);
        List<DescriptorGroup> descriptorGroups = new ArrayList<DescriptorGroup>();
        List<Descriptor> descriptors = new ArrayList<Descriptor>();
        spotMetadata.setDescriptorGroups(descriptorGroups);
        spotMetadata.setDescriptors(descriptors);
        
        String metadata1 = metadataConfig.getFormulationSolutionDescription();
        String[] splitted = metadata1.split("::");
        String descriptorGroupName = splitted[0];
        
        DescriptorGroup group = new DescriptorGroup();
        group.setName(descriptorGroupName);
        group.setDescriptors(new ArrayList<Description>());
        String buffer = splitted[1];
        Descriptor desc1 = new Descriptor();
        desc1.setName(buffer);
        desc1.setValue(splittedRow[config.bufferColumn]);
        group.getDescriptors().add(desc1);
        String carrier = metadataConfig.getFormulationCarrierDescription().split("::")[1];
        Descriptor desc2 = new Descriptor();
        desc2.setName(carrier);
        desc2.setValue(splittedRow[config.carrierColumn]);
        group.getDescriptors().add(desc2);
        String method = metadataConfig.getFormulationMethodDescription().split("::")[1];
        Descriptor desc3 = new Descriptor();
        desc3.setName(method);
        desc3.setValue(splittedRow[config.methodColumn]);
        group.getDescriptors().add(desc3);
        String reference = metadataConfig.getFormulationReferenceDescription().split("::")[1];
        DescriptorGroup desc4 = new DescriptorGroup();
        desc4.setName(reference);
        desc4.setDescriptors(new ArrayList<Description>());
        Descriptor subDesc4 = new Descriptor();
        subDesc4.setName("Value");
        subDesc4.setValue(splittedRow[config.referenceColumn]);
        Descriptor sub1Desc4 = new Descriptor();
        sub1Desc4.setName("Type");
        sub1Desc4.setValue("URL");    //TODO which one are we expecting
        desc4.getDescriptors().add(subDesc4);
        desc4.getDescriptors().add(sub1Desc4);
        group.getDescriptors().add(desc4);
        String volume = metadataConfig.getVolumeDescription().split("::")[1];
        Descriptor desc5 = new Descriptor();
        desc5.setName(volume);
        desc5.setValue(splittedRow[config.volumeColumn]);
        group.getDescriptors().add(desc5);
        String dispenses = metadataConfig.getNumberDispensesDescription().split("::")[1];
        Descriptor desc6 = new Descriptor();
        desc6.setName(dispenses);
        desc6.setValue(splittedRow[config.dispensesColumn]);
        group.getDescriptors().add(desc6);
        descriptorGroups.add(group);
        
        Descriptor comment = new Descriptor();
        comment.setName(metadataConfig.getCommentDescription());
        comment.setValue(splittedRow[config.commentColumn]);
        descriptors.add(comment);
        
        return spotMetadata;
        
    }


    /**
     * look for given level in the levels, if not exists, add and return, if exists return
     * @param levelString 100uM, 10uM etc.
     * @param levels existing levels
     * @return added or found levelunit corresponding to the levelstring
     */
    private LevelUnit addLevel(String levelString, List<LevelUnit> levels) {
        // parse level String to get the integer part and the unit part
        String numbers = "0123456789";
        String concentration ="";
        String unit = "";
        int i=0;
        while (i < levelString.length()) {
            if (numbers.contains(levelString.subSequence(i, i+1)))
                concentration += levelString.subSequence(i, i+1);
            else
                unit += levelString.subSequence(i, i+1);
            i++;
        }
        try {
            Double con = Double.parseDouble(concentration);
            unit = unit.trim();
            UnitOfLevels unitLevel = UnitOfLevels.lookUp(unit);
            if (unit.equals("uM"))
                unitLevel = UnitOfLevels.MICROMOL;
            if (unit.equals("ug/ml"))
                unitLevel = UnitOfLevels.MICROML;
            
            
            for (LevelUnit u: levels) {
                if (u.getConcentration().equals(con) && unitLevel != null && unitLevel.equals(u.getLevelUnit()))
                    return u;
            }
            // does not exists, add
            if (unitLevel != null) {
                LevelUnit newLevelUnit = new LevelUnit();
                newLevelUnit.setConcentration(con);
                newLevelUnit.setLevelUnit(unitLevel);
                levels.add(newLevelUnit);
                return newLevelUnit;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        
        return null;
    }
    
    public static String getSequence(String a_sequence) {
        int t_index = a_sequence.lastIndexOf("-");
        String sequence = a_sequence.substring(0, t_index).trim();
        return cleanupSequence(sequence);
    }
    
    public static String cleanupSequence (String a_sequence) {
        String sequence = a_sequence.trim();
        sequence = sequence.replaceAll(" ", "");
        sequence = sequence.replaceAll("\u00A0", "");
        if (sequence.endsWith("1") || sequence.endsWith("2")) {
            sequence = sequence.substring(0, sequence.length()-1);
        }
        return sequence;
    }
    
    public static String getLinker(String a_sequence) {
        int t_index = a_sequence.lastIndexOf("-");
        String linker = a_sequence.substring(t_index+1).trim();
                
        linker = linker.replaceAll("\u00A0", "");
        return linker;
    }
}
