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
import org.glygen.array.persistence.rdf.FeatureType;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.GlycanSequenceFormat;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.PeptideLinker;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.SmallMoleculeLinker;
import org.glygen.array.persistence.rdf.Spot;
import org.grits.toolbox.glycanarray.library.om.layout.LevelUnit;
import org.grits.toolbox.glycanarray.om.model.UnitOfLevels;
import org.grits.toolbox.glycanarray.om.parser.cfg.CFGMasterListParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtendedGalFileParser {
    
    ParserConfiguration config;

    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    public void setConfig(ParserConfiguration config) {
        this.config = config;
    }
    
    /**
     * parses the given GAL file to create a Slide Layout with all its blocks, features and glycans. Linkers should be created
     * before hand and passed in as a list
     * 
     * @param filePath file path of the GAL file to be parsed
     * @param name name of the slide layout to be created
     * @param linkerList should contain all the existing linkers 
     * @return import result with slide layout and all the block layouts, feature and glycans to be created
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
        Map <String, Feature> glycanMap = new HashMap<>();
        Map <String, Integer> glycanGroupMap = new HashMap<>();
        
        // these are the new structures to be imported into the repository
        List<Glycan> glycanList = new ArrayList<>();
        List<Feature> featureList = new ArrayList<>();
        List<BlockLayout> layoutList = new ArrayList<>();
        
        SlideLayout slideLayout = new SlideLayout();
        if (filePath.lastIndexOf(File.separator) != -1)
            slideLayout.setName(filePath.substring(filePath.lastIndexOf(File.separator)+1));
        else 
            slideLayout.setName(filePath);
        
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
                            glycanMap = new HashMap<>();
                            glycanGroupMap = new HashMap<>();
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
                    String sequence = splitted[config.getSequenceColumn()].trim();
                    String sequenceType = splitted[config.getSequenceTypeColumn()].trim();
                    Boolean mixture = Boolean.parseBoolean(splitted[config.getMixtureColumn()].trim()); // mixture or not 
                    String concentration = splitted[config.getConcentrationColumn()].trim();
                    String featureType = splitted[config.getTypeColumn()].trim();
                    String internalId = splitted[config.getIdColumn()].trim();
                    LevelUnit levelUnit = null;
                    if (!mixture) {
                        levelUnit = addLevel(concentration, levels);
                    }
                    
                    if (y > maxRow)
                        maxRow = y;
                    if (x > maxColumn)
                        maxColumn = x;
                    
                    Spot spot = new Spot();
                    spot.setColumn(x);
                    spot.setRow(y);
                    
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
                            String[] types = sequenceType.split("|");
                            String[] seqs = sequence.split("|");
                            String[] concentrations = concentration.split("|");
                            String[] featureTypes = featureType.split("|");
                            LevelUnit[] levelUnits = new LevelUnit[concentrations.length];
                            int i=0;
                            for (String c: concentrations) {
                                levelUnits[i++] = addLevel(c, levels); 
                            }
                            i = 0;
                            for (String seq: seqs) {
                                // create a feature for each seq
                                // and add to the spot
                                Feature feature = parseSequenceForFeature (featureTypes[i], featureName, internalId, seq, types[i], levelUnits[i], glycanList, linkerList);
                                i++;
                                spotFeatures.add(feature);
                                featureList.add(feature);
                            }
                        } else {
                            if (glycanMap.get(internalId) != null) {
                                // already created the feature
                                spotFeatures.add(glycanMap.get(internalId));
                                spot.setFeatures(spotFeatures);
                                spot.setGroup(glycanGroupMap.get(internalId));
                                if (levelUnit != null)
                                    spot.setConcentration(levelUnit);    
                            } else {
                                Feature feature = parseSequenceForFeature (featureType, featureName, internalId, sequence, sequenceType, levelUnit, glycanList, linkerList);
                                spotFeatures.add(feature);
                                if (groupId > maxGroup)
                                    maxGroup = groupId;
                                
                                spot.setGroup(groupId++);
                                featureList.add(feature);
                                
                                glycanGroupMap.put(internalId, spot.getGroup());
                                glycanMap.put(internalId, feature);
                            }
                        }
                    }
                    
                } catch (NumberFormatException e) {
                    // should not occur
                    logger.error("Value should have been a number", e);
                    scan.close();
                    throw new IOException("Value should have been a number: " + e.getMessage());
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
        result.setGlycanList(glycanList);
        result.setLayout(slideLayout);
        result.setLayoutList(layoutList);
        return result;
    }

    /**
     * extract Feature from the sequence given sequenceType and concentration
     * this will also add glycans and linkers as necessary for the feature
     * 
     * @param sequence
     * @param sequenceType
     * @param types 
     * @param concentration
     * @param glycanList 
     * @param linkerList
     * @return
     */
    private Feature parseSequenceForFeature(String featureType, 
            String name, 
            String internalId,
            String sequence, 
            String sequenceType, 
            LevelUnit concentration, 
            List<Glycan> glycanList, 
            List<Linker> linkerList) {
        if (featureType == null)
            return null;
        if (featureType.equalsIgnoreCase("control") ||
                featureType.equalsIgnoreCase("landing light") ||
                featureType.equalsIgnoreCase("negative control")) {
            // create Linker with the given name
            // create a feature with linker only
            SmallMoleculeLinker linker = new SmallMoleculeLinker();
            linker.setName(name);
            Linker existing = findLinkerInList (linker, linkerList);
            //TODO what to do if existing is null
            Feature feature = new Feature();
            if (featureType.equalsIgnoreCase("control"))
                feature.setType(FeatureType.CONTROL);
            else if (featureType.equalsIgnoreCase("landing light"))
                feature.setType(FeatureType.LANDING_LIGHT);
            else if (featureType.equalsIgnoreCase("negative control"))
                feature.setType(FeatureType.NEGATIVE_CONTROL);
            feature.setLinker(existing);
            feature.setName(name);
            return feature;
        } else if (featureType.equalsIgnoreCase("organic compound")) {
            SmallMoleculeLinker linker = new SmallMoleculeLinker();
            linker.setName(name);
            if (sequenceType.equalsIgnoreCase("SMILES"))
                linker.setSmiles(sequence);
            Linker existing = findLinkerInList (linker, linkerList);
            //TODO what to do if existing is null
            Feature feature = new Feature();
            feature.setType(FeatureType.COMPOUND);
            feature.setLinker(existing);
            feature.setName(name);
            return feature;
            
        } else if (featureType.equalsIgnoreCase("glycan")) {
            Glycan glycan = new SequenceDefinedGlycan();
            glycan.setName(name);
            if (sequenceType != null && sequenceType.equalsIgnoreCase("cfg")) {
                // parse the sequence
                CFGMasterListParser parser = new CFGMasterListParser();
                String glycanSequence = getSequence(sequence);
                String glycoCT = parser.translateSequence(glycanSequence);
                ((SequenceDefinedGlycan) glycan).setSequence(glycoCT);
                ((SequenceDefinedGlycan) glycan).setSequenceType(GlycanSequenceFormat.GLYCOCT);
                glycanList.add(glycan);
                List<Glycan> glycans = new ArrayList<Glycan>();
                glycans.add(glycan);
                Feature feature = new Feature();
                feature.setType(FeatureType.NORMAL);
                feature.setName(name);
                String linkerName = getLinker(sequence);
                if (linkerName != null) {
                    SmallMoleculeLinker linker = new SmallMoleculeLinker();
                    linker.setName(linkerName);
                    Linker existing = findLinkerInList(linker, linkerList);
                    feature.setLinker(existing);
                }
                return feature;
            }
            
        } else if (featureType.equalsIgnoreCase("glycopeptide")) {
            PeptideLinker linker = new PeptideLinker();
            linker.setSequence(sequence);
            Map<Integer, Glycan> positionMap = linker.extractGlycans();
            List<Glycan> glycans = new ArrayList<Glycan>();
            for (Glycan g: positionMap.values()) {
                if (!glycans.contains(g))
                    glycans.add(g);
            }
            // remove the glycan sequences from the peptide sequence
            // only position markers should be there
            linker.setSequence(replaceGlycans (sequence));
            Feature feature = new Feature();
            feature.setName(name);
            feature.setType(FeatureType.NORMAL);
            feature.setLinker(linker);
            feature.setGlycans(glycans);
            return feature;
        }
        
        return null;
    }

    private String replaceGlycans(String sequence) {
        int position = 0;
        String newSequence = "";
        boolean glycan = false;
        for (int i=0; i < sequence.length(); i++) {
            if (sequence.charAt(i) == '{') {
                // start removing
                glycan = true;
                position ++;
                newSequence += "{";
            } else if (sequence.charAt(i) == '}') {
                glycan = false;
                newSequence += sequence.charAt(i-2);
                newSequence += sequence.charAt(i-1);
                newSequence += "}";
            }
            else if (glycan) {
                if (!newSequence.contains(position+"")) {
                    newSequence += position;
                }
            } else {
                newSequence += sequence.charAt(i);
            }
        }
        return newSequence;
    }

    private Linker findLinkerInList(SmallMoleculeLinker linker, List<Linker> linkerList) {
        Linker existing = null;
        for (Linker l: linkerList) {
            if (l.getName() != null && l.getName().equalsIgnoreCase(linker.getName())) {
                existing = l;
                break;
            }
            if (l instanceof SmallMoleculeLinker) {
                if (linker.getSmiles() != null && linker.getSmiles().equalsIgnoreCase(((SmallMoleculeLinker) l).getSmiles())) {
                    existing = l;
                    break;
                }
            }
        }
        
        return existing;
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
            UnitOfLevels unitLevel = UnitOfLevels.lookUp(unit);
            if (unit.equals("uM"))
                unitLevel = UnitOfLevels.MICROMOL;
            
            
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
    
    private String getSequence(String a_sequence) {
        int t_index = a_sequence.lastIndexOf("-");
        return a_sequence.substring(0, t_index).trim();
    }
    
    private String getLinker(String a_sequence) {
        int t_index = a_sequence.lastIndexOf("-");
        return a_sequence.substring(t_index+1).trim();
    }
    
    public static void main(String[] args) {
        ExtendedGalFileParser parser = new ExtendedGalFileParser();
        System.out.println(parser.replaceGlycans("AcNH-{GalNAca1-T}{GalNAca1-T}{GalNAca1-T}-CONH(CH2)3NH2"));
        System.out.println(parser.replaceGlycans("FlNH-KVAL{GlcNAcb1-2Mana1-6(GlcNAcb1-2Mana1-3)Manb1-4GlcNAcb1-4GlcNAcb1-N}KTA-COOH"));
        System.out.println(parser.replaceGlycans("FlNH-KPTPSPSA-COOMe"));
        System.out.println(parser.replaceGlycans("FlNH-KVPS{GalNAca1-T}PP{GalNAca1-S}PSPSA-COOH"));
        System.out.println(parser.replaceGlycans("AcNH-KTSTTATPPV{[3Ac][4Ac][6Ac]GlcNAcb1-S}QASSTTTSTWA-COOH"));
    }
}