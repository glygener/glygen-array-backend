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
import org.glygen.array.persistence.rdf.GlycoPeptide;
import org.glygen.array.persistence.rdf.LinkedGlycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.PeptideLinker;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.SmallMoleculeLinker;
import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.metadata.Description;
import org.glygen.array.persistence.rdf.metadata.Descriptor;
import org.glygen.array.persistence.rdf.metadata.DescriptorGroup;
import org.glygen.array.persistence.rdf.metadata.SpotMetadata;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.grits.toolbox.glycanarray.library.om.layout.LevelUnit;
import org.grits.toolbox.glycanarray.om.model.UnitOfLevels;
import org.grits.toolbox.glycanarray.om.parser.cfg.CFGMasterListParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
        Map <String, Feature> glycanMap = new HashMap<>();
        Map <String, Integer> glycanGroupMap = new HashMap<>();
        
        // these are the new structures to be imported into the repository
        List<Glycan> glycanList = new ArrayList<>();
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
        
        //TODO detect parser configuration
        /*ParserConfiguration config = new ParserConfiguration();
        config.setBlockColumn(0);
        config.setCoordinateColumnY(1);
        config.setCoordinateColumnX(2);
        config.setIdColumn(3);
        config.setNameColumn(4);
        config.setSequenceTypeColumn(5);
        config.setSequenceColumn(6);
        config.setConcentrationColumn(7);
        config.setTypeColumn(8);
        config.setMixtureColumn(9);
        config.setBufferColumn(10);
        config.setRatioColumn(11);
        config.setCarrierColumn(14);
        config.setMethodColumn(15);
        config.setReferenceColumn(16);
        config.setVolumeColumn(17);
        config.setDispensesColumn(18);*/
        
        
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
                    spot.setMetadata(addSpotMetadata(name + "metadata-" + x + ":" + y, splitted));
                    
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
                            String[] types = sequenceType.split("\\|");
                            String[] seqs = sequence.split("\\|");
                            String[] concentrations = concentration.split("\\|");
                            String[] featureTypes = featureType.split("\\|");
                            String[] featureNames = featureName.split("\\|");
                            LevelUnit[] levelUnits = new LevelUnit[concentrations.length];
                            int i=0;
                            for (String c: concentrations) {
                                levelUnits[i++] = addLevel(c, levels); 
                            }
                            i = 0;
                            for (String fName: featureNames) {
                                Feature feature = new Feature();
                                feature.setName(fName.trim());
                                String fType = featureTypes[i];
                                if (fType.equalsIgnoreCase("landing_light")) {
                                    feature.setType(FeatureType.LANDING_LIGHT);
                                } else if (fType.equalsIgnoreCase("control")) {
                                    feature.setType(FeatureType.CONTROL);
                                } else if (fType.equalsIgnoreCase("negative control")) {
                                    feature.setType(FeatureType.NEGATIVE_CONTROL);
                                } else if (fType.equalsIgnoreCase("organic compound")) {
                                    feature.setType(FeatureType.COMPOUND);
                                } else if (fType.equalsIgnoreCase("glycan")){
                                    feature.setType(FeatureType.LINKEDGLYCAN);
                                } else if (fType.equalsIgnoreCase("glycopeptide")) {
                                    feature.setType(FeatureType.GLYCOPEPTIDE);
                                }
                                i++;
                                spotFeatures.add(feature);
                                featureList.add(feature);
                            }
                            /*for (String seq: seqs) {
                                // create a feature for each seq
                                // and add to the spot
                                Feature feature = parseSequenceForFeature (featureTypes[i].trim(), featureName, 
                                        internalId, seq, types[i].trim(), levelUnits[i], glycanList, linkerList, errorList);
                                i++;
                                spotFeatures.add(feature);
                                featureList.add(feature);
                            }*/
                        } else {
                            if (glycanMap.get(internalId) != null) {
                                // already created the feature
                                spotFeatures.add(glycanMap.get(internalId));
                                spot.setFeatures(spotFeatures);
                                spot.setGroup(glycanGroupMap.get(internalId));
                                if (levelUnit != null)
                                    spot.setConcentration(levelUnit);    
                            } else {
                                //Feature feature = parseSequenceForFeature (featureType, featureName, 
                                //        internalId, sequence, sequenceType, levelUnit, glycanList, linkerList, errorList);
                                Feature feature = new Feature();
                                feature.setName(featureName.trim());
                                if (featureType.equalsIgnoreCase("landing_light")) {
                                    feature.setType(FeatureType.LANDING_LIGHT);
                                } else if (featureType.equalsIgnoreCase("control")) {
                                    feature.setType(FeatureType.CONTROL);
                                } else if (featureType.equalsIgnoreCase("negative control")) {
                                    feature.setType(FeatureType.NEGATIVE_CONTROL);
                                } else if (featureType.equalsIgnoreCase("organic compound")) {
                                    feature.setType(FeatureType.COMPOUND);
                                } else if (featureType.equalsIgnoreCase("glycan")){
                                    feature.setType(FeatureType.LINKEDGLYCAN);
                                } else if (featureType.equalsIgnoreCase("glycopeptide")) {
                                    feature.setType(FeatureType.GLYCOPEPTIDE);
                                }
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
        Descriptor desc4 = new Descriptor();
        desc4.setName(reference);
        desc4.setValue(splittedRow[config.referenceColumn]);
        group.getDescriptors().add(desc4);
        String volume = metadataConfig.getVolumeDescription();
        Descriptor desc5 = new Descriptor();
        desc5.setName(volume);
        desc5.setValue(splittedRow[config.volumeColumn]);
        descriptors.add(desc5);
        String dispenses = metadataConfig.getNumberDispensesDescription();
        Descriptor desc6 = new Descriptor();
        desc6.setName(dispenses);
        desc6.setValue(splittedRow[config.dispensesColumn]);
        descriptors.add(desc6);
        
        return spotMetadata;
        
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
            List<Linker> linkerList,
            List<ErrorMessage> errors) {
        if (featureType == null)
            return null;
        if (featureType.equalsIgnoreCase("landing_light")) {
            Feature feature = new Feature();
            feature.setName(name);
            feature.setType(FeatureType.LANDING_LIGHT);
            return feature;
        }
        else if (featureType.equalsIgnoreCase("control") ||
                featureType.equalsIgnoreCase("negative control")) {
            // create Linker with the given name
            // create a feature with linker only
            SmallMoleculeLinker linker = new SmallMoleculeLinker();
            linker.setName(name);
            Linker existing = findLinkerInList (linker, linkerList);
            if (existing == null) {
                ErrorMessage error = new ErrorMessage();
                error.setErrorCode(ErrorCodes.PARSE_ERROR);
                error.setStatus(HttpStatus.BAD_REQUEST.value());
                error.addError(new ObjectError("linker "+name, "NotFound"));
                if (!error.getErrors().isEmpty())
                    errors.add(error);
                return null;
            }
            Feature feature = new Feature();
            if (featureType.equalsIgnoreCase("control"))
                feature.setType(FeatureType.CONTROL);
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
            if (existing == null) {
                ErrorMessage error = new ErrorMessage();
                error.setErrorCode(ErrorCodes.PARSE_ERROR);
                error.setStatus(HttpStatus.BAD_REQUEST.value());
                error.addError(new ObjectError("linker "+name, "NotFound"));
                if (!error.getErrors().isEmpty())
                    errors.add(error);
                return null;
            }
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
                try {
                    CFGMasterListParser parser = new CFGMasterListParser();
                    String glycanSequence = getSequence(sequence);
                    String glycoCT = parser.translateSequence(glycanSequence);
                    ((SequenceDefinedGlycan) glycan).setSequence(glycoCT);
                    ((SequenceDefinedGlycan) glycan).setSequenceType(GlycanSequenceFormat.GLYCOCT);
                } catch (Exception e) {
                    ErrorMessage error = new ErrorMessage();
                    error.setErrorCode(ErrorCodes.PARSE_ERROR);
                    error.setStatus(HttpStatus.BAD_REQUEST.value());
                    error.addError(new ObjectError("sequence " + name, e.getMessage()));
                    if (!error.getErrors().isEmpty())
                        errors.add(error);
                    return null;
                }
                glycanList.add(glycan);
                List<Glycan> glycans = new ArrayList<Glycan>();
                glycans.add(glycan);
                Feature feature = new LinkedGlycan();
                feature.setType(FeatureType.LINKEDGLYCAN);
                feature.setName(name);
                ((LinkedGlycan) feature).setGlycans(glycans);
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
            Feature feature = new GlycoPeptide();
            feature.setName(name);
            feature.setType(FeatureType.GLYCOPEPTIDE);
            feature.setLinker(linker);
            List<LinkedGlycan> list = new ArrayList<LinkedGlycan>();
            for (Glycan g: glycans) {
                LinkedGlycan lg = new LinkedGlycan();
                lg.addGlycan(g);
                list.add(lg);
            }
            ((GlycoPeptide) feature).setGlycans(list);
            return feature;
        } else {
            // error in file format, the types should have been one of the previous ones
            ErrorMessage error = new ErrorMessage();
            error.setErrorCode(ErrorCodes.PARSE_ERROR);
            error.setStatus(HttpStatus.BAD_REQUEST.value());
            error.addError(new ObjectError("file ", "NotValid - featureType is not correct"));
            if (!error.getErrors().isEmpty())
                errors.add(error);
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
    
    public static void main(String[] args) {
        ExtendedGalFileParser parser = new ExtendedGalFileParser();
        System.out.println(parser.replaceGlycans("AcNH-{GalNAca1-T}{GalNAca1-T}{GalNAca1-T}-CONH(CH2)3NH2"));
        System.out.println(parser.replaceGlycans("FlNH-KVAL{GlcNAcb1-2Mana1-6(GlcNAcb1-2Mana1-3)Manb1-4GlcNAcb1-4GlcNAcb1-N}KTA-COOH"));
        System.out.println(parser.replaceGlycans("FlNH-KPTPSPSA-COOMe"));
        System.out.println(parser.replaceGlycans("FlNH-KVPS{GalNAca1-T}PP{GalNAca1-S}PSPSA-COOH"));
        System.out.println(parser.replaceGlycans("AcNH-KTSTTATPPV{[3Ac][4Ac][6Ac]GlcNAcb1-S}QASSTTTSTWA-COOH"));
    }
}
