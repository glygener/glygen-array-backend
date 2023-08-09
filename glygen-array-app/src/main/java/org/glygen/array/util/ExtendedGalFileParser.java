package org.glygen.array.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.rdf.Block;
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.RatioConcentration;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.metadata.Description;
import org.glygen.array.persistence.rdf.metadata.Descriptor;
import org.glygen.array.persistence.rdf.metadata.DescriptorGroup;
import org.glygen.array.persistence.rdf.metadata.SpotMetadata;
import org.glygen.array.persistence.rdf.template.DescriptionTemplate;
import org.glygen.array.persistence.rdf.template.DescriptorGroupTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplateType;
import org.glygen.array.service.MetadataTemplateRepository;
import org.glygen.array.view.ErrorMessage;
import org.grits.toolbox.glycanarray.library.om.layout.LevelUnit;
import org.grits.toolbox.glycanarray.om.model.UnitOfLevels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.ObjectError;

@Component
public class ExtendedGalFileParser { 

    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    @Autowired
    SpotMetadataConfig metadataConfig;
    
    @Autowired
    ParserConfiguration config;
    
    @Autowired
    MetadataTemplateRepository templateRepository;
    
    String defaultConcentration = null;
    String defaultBuffer = null;
    String defaultVolume = null;
    String defaultDispenses = null;
    String defaultCarrier = null;
    String defaultMethod = null;
    String defaultReference = null;
    
    MetadataTemplate spotMetadataTemplate = null;
    
    /**
     * parses the given extended GAL file to create a Slide Layout with all its blocks. 
     * Features should exist in the repository already, and they are identified by features' "internal id" field
     * 
     * @param filePath file path of the GAL file to be parsed
     * @param name name of the slide layout to be created 
     * @param height the height of the slide layout
     * @param width the width of the slide layout
     * @return import result with slide layout and all the block layouts to be created
     * @throws IOException if the file cannot be found or invalid
     */
    public GalFileImportResult parse (String filePath, String name, Integer height, Integer width) throws IOException {
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
        Map <String, String> featureGroupMap = new HashMap<>();
        boolean useBlockCount = false;
        
        // these are the new structures to be imported into the repository
        List<BlockLayout> layoutList = new ArrayList<>();
        
        List<ErrorMessage> errorList = new ArrayList<ErrorMessage>();
        
        SlideLayout slideLayout = new SlideLayout();
        slideLayout.setName(name);
        if (height == null) {
            // rely on the block count
            useBlockCount = true;
        } else {
            slideLayout.setHeight(height);
        }
        if (width == null) { 
            slideLayout.setWidth(1);   // 1 dimensional by default
            useBlockCount = true;
        } else {
            slideLayout.setWidth(width);
        }
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
                        Integer h = (int) Double.parseDouble(blockCount.substring(0, blockCount.length()-1));
                        if (useBlockCount)
                            slideLayout.setHeight(h);
                        if (width != null && height != null) {
                            if ((width * height) != h) {
                                ErrorMessage error = new ErrorMessage("Width and height do not match the GAL file");
                                error.addError(new ObjectError ("dimensions", "NoMatch"));
                                errorList.add(error);
                            }
                        }
                    } else { // no quotes at the beginning and end
                        String blockCount = firstColumn.substring(firstColumn.indexOf("BlockCount=")+11);
                        Integer h = (int) Double.parseDouble(blockCount);
                        if (useBlockCount)
                            slideLayout.setHeight(h);
                        if (width != null && height != null) {
                            if ((width * height) != h) {
                                ErrorMessage error = new ErrorMessage("Width and height do not match the block count in GAL file");
                                error.addError(new ObjectError ("dimensions", "NoMatch"));
                                errorList.add(error);
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    slideLayout.setHeight(height);   // default
                }
            }
            // get the default values if exists
            if (firstColumn.contains("DefaultConcentration=")) {
            	defaultConcentration = firstColumn.substring(firstColumn.indexOf("DefaultConcentration=")+22).trim();
            }
            if (firstColumn.contains("DefaultBuffer=")) {
            	defaultBuffer = firstColumn.substring(firstColumn.indexOf("DefaultBuffer=")+14).trim();
            }
            if (firstColumn.contains("DefaultVolume=")) {
            	defaultVolume = firstColumn.substring(firstColumn.indexOf("DefaultVolume=")+14).trim();
            }
            if (firstColumn.contains("DefaultDispenses=")) {
            	defaultDispenses = firstColumn.substring(firstColumn.indexOf("DefaultDispenses=")+17).trim();
            }
            if (firstColumn.contains("DefaultCarrier=")) {
            	defaultCarrier = firstColumn.substring(firstColumn.indexOf("DefaultCarrier=")+15).trim();
            }
            if (firstColumn.contains("DefaultMethod=")) {
            	defaultMethod = firstColumn.substring(firstColumn.indexOf("DefaultMethod=")+14).trim();
            }
            if (firstColumn.contains("DefaultReference=")) {
            	defaultReference = firstColumn.substring(firstColumn.indexOf("DefaultReference=")+17).trim();
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
                        int blockLocation = (int) Double.parseDouble(blockColumn.trim());
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
                            block.setColumn(1);   // we assume one dimension for the slide, we need to fix it at the end
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
                    int x = (int) Double.parseDouble(splitted[config.getCoordinateColumnX()].trim()); // column
                    int y = (int) Double.parseDouble(splitted[config.getCoordinateColumnY()].trim()); // row
                    String glycanName = splitted[config.getNameColumn()].trim(); // name of the probe
                    String concentration = (config.getConcentrationColumn() != -1 && splitted.length > config.getConcentrationColumn()) ? 
                            splitted[config.getConcentrationColumn()].trim() : "";
                    String featureId = splitted[config.getIdColumn()].trim();
                    String repoId = (config.getRepoIdColumn() != -1 && splitted.length > config.getRepoIdColumn()) ? splitted[config.getRepoIdColumn()].trim() : "";
                    String ratio = (config.getRatioColumn() != -1 && splitted.length > config.getRatioColumn()) ? splitted[config.getRatioColumn()].trim() : "";
                    LevelUnit levelUnit = null;
                    boolean mixture = featureId != null && featureId.contains("||");
                    String printingFlags = (config.getFlagColumn() != -1 && splitted.length > config.getFlagColumn()) ? splitted[config.flagColumn].trim() : "";
                    String group = (config.getGroupColumn() != -1 && splitted.length > config.getGroupColumn()) ? splitted[config.groupColumn].trim() : ""; 
                    
                    if (!repoId.isEmpty()) {
                    	//overrides featureId
                    	featureId = repoId;
                    }
                    
                    if (y > maxRow)
                        maxRow = y;
                    if (x > maxColumn)
                        maxColumn = x;
                    
                    Spot spot = new Spot();
                    spot.setColumn(x);
                    spot.setRow(y);
                    spot.setFlag(printingFlags);
                    
                    if (!mixture) {
                    	if (concentration.isEmpty()) {
                        	// use default concentration if exists
                        	if (defaultConcentration != null && !defaultConcentration.isEmpty()) {
                        		concentration = defaultConcentration;
                        	}
                        }
                        levelUnit = addLevel(concentration, levels);   
                    }
                    
                    List<Feature> spotFeatures = new ArrayList<Feature>();
                
                    blockLayout.getSpots().add(spot);
                    spot.setFeatures(spotFeatures);
                    Map<Feature, LevelUnit> featureConcentrationMap = new HashMap<Feature, LevelUnit>();
                    spot.setFeatureConcentrationMap(featureConcentrationMap);
                    Map<Feature, Double> featureRatioMap = new HashMap<Feature, Double>();
                    spot.setFeatureRatioMap(featureRatioMap);
                    
                    if (glycanName.equals("0") || glycanName.equals("\"0\"") 
                            || glycanName.equalsIgnoreCase("empty") || glycanName.equalsIgnoreCase("\"empty\"")) {
                        spot.setFeatures(null);    //what to set for empty spots ==> no feature assigned for the spot
                    }
                    else {
                        spot.setMetadata(addSpotMetadata(name + "metadata-" + x + ":" + y, splitted, errorList));
                        if (mixture) {
                            String[] concentrations = concentration.split("\\|\\|");
                            String[] featureIds = featureId.split("\\|\\|");
                            LevelUnit[] levelUnits = null;
                            if (concentrations.length == 0 && defaultConcentration != null) {
                            	levelUnits = new LevelUnit[featureIds.length];
                            	for (int i=0; i < levelUnits.length; i++) {
                            		levelUnits[i++] = addLevel(defaultConcentration, levels); 
                            	}
                            } else {
                            	levelUnits = new LevelUnit[concentrations.length];
                            	int i=0;
                            	for (String c: concentrations) {
                            		levelUnits[i++] = addLevel(c, levels); 
                            	}
                            }
                            
                            int i = 0;
                            for (String fId: featureIds) {
                                Feature feature = new Feature();
                                feature.setInternalId(fId.trim());
                                spotFeatures.add(feature);
                                if (levelUnits != null && i < levelUnits.length)
                                    featureConcentrationMap.put(feature, levelUnits[i]);
                                i++;
                            }
                            try {
                                if (!group.isEmpty())
                                    spot.setGroup(group.trim());
                        	} catch (NumberFormatException e) {
                        		ErrorMessage error = new ErrorMessage("Group should be a number: " + group);
                                String[] codes = new String[] {"Row " + x, "Row " + y};
                                error.addError(new ObjectError ("group", codes, null, "NotValid"));
                                errorList.add(error);
                        	}
                            
                            if (ratio != null && !ratio.isEmpty()) {
                                String[] ratios = ratio.split(":");
                                if (ratios.length != featureIds.length) {
                                    logger.error("Ratio is not given correctly for all features on the spot:" + ratio);
                                    ErrorMessage error = new ErrorMessage("Ratio is not given correctly for all features on the spot: " + x +"-" + y);
                                    String[] codes = new String[] {"Row " + x, "Row " + y};
                                    error.addError(new ObjectError ("ratio", codes, null, "NotValid"));
                                    errorList.add(error);
                                } else {
                                    
                                    int k=0;
                                    for (String r: ratios) {
                                        try {
                                            Double rD = Double.parseDouble(r);
                                            featureRatioMap.put(spotFeatures.get(k++), rD);
                                        } catch (NumberFormatException e) {
                                            logger.warn("Ratio is incorrect:" + ratio, e);
                                            ErrorMessage error = new ErrorMessage("Ratio is incorrect: " + x +"-" + y);
                                            String[] codes = new String[] {"Row " + x, "Row " + y};
                                            error.addError(new ObjectError ("ratio", codes, null, "NotValid"));
                                            errorList.add(error);
                                        }
                                    }
                                }
                            }
                            
                        } else {
                            if (featureMap.get(featureId) != null) {
                                // already created the feature
                                spotFeatures.add(featureMap.get(featureId));
                                spot.setFeatures(spotFeatures);
                                spot.setGroup(featureGroupMap.get(featureId));
                                if (levelUnit != null) {
                                    spot.getFeatureConcentrationMap().put(featureMap.get(featureId), levelUnit);  
                                }
                            } else {
                                Feature feature = new Feature();
                                feature.setInternalId(featureId);
                                spotFeatures.add(feature);
                                if (groupId > maxGroup)
                                    maxGroup = groupId;
                                if (!group.isEmpty()) {
                                	// use the group from the file
                                	try {
                                		spot.setGroup(group.trim());
                                	} catch (NumberFormatException e) {
                                		ErrorMessage error = new ErrorMessage("Group should be a number: " + group);
                                        String[] codes = new String[] {"Row " + x, "Row " + y};
                                        error.addError(new ObjectError ("group", codes, null, "NotValid"));
                                        errorList.add(error);
                                	}
                                } else {
                                	spot.setGroup("" + groupId++);
                                }
                                if (levelUnit != null)
                                    spot.getFeatureConcentrationMap().put(feature, levelUnit);  
                                
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
        
        if (!useBlockCount) {
            // check if the total number of blocks and width/height agrees
            if ((width * height) != slideLayout.getBlocks().size()) {
                ErrorMessage error = new ErrorMessage("Width and height do not match the GAL file");
                error.addError(new ObjectError ("dimensions", "NoMatch"));
                errorList.add(error);
            } else {
                // need to arrange blocks based on width and height
                int i=1;
                int j=1;
                for (Block b: slideLayout.getBlocks()) {
                    b.setColumn(i);
                    b.setRow(j);
                    i++;
                    if (i > width) {
                        j++;
                        i = 1;
                    }
                }
            }
        }
        
        GalFileImportResult result = new GalFileImportResult();
        result.setLayout(slideLayout);
        result.setLayoutList(layoutList);
        result.setErrors(errorList);
        return result;
    }
    
    
    SpotMetadata addSpotMetadata (String metadataName, String[] splittedRow, List<ErrorMessage> errorList) {
        SpotMetadata spotMetadata = new SpotMetadata();
        spotMetadata.setName(metadataName);
        String template = metadataConfig.getTemplate();
        spotMetadata.setTemplate(template);
        // retrieve Template
        if (this.spotMetadataTemplate == null) {
        	try {
				String uri = templateRepository.getTemplateByName(template, MetadataTemplateType.SPOT);
				if (uri != null)
					this.spotMetadataTemplate = templateRepository.getTemplateFromURI(uri);
				else {
					// template retrieval error!!!
	        		ErrorMessage error = new ErrorMessage("spot metadata template cannot be found. Check the configuration!");
	        		String[] codes = new String[] {metadataName};
	                error.addError(new ObjectError ("template", codes, null, "NotFound"));
	                errorList.add(error);
				}
			} catch (SparqlException | SQLException e) {
				// template retrieval error!!!
        		ErrorMessage error = new ErrorMessage("spot metadata template cannot be found. Check the configuration!");
        		String[] codes = new String[] {metadataName};
                error.addError(new ObjectError ("template", codes, null, "NotFound"));
                errorList.add(error); 
			}
        }
        List<DescriptorGroup> descriptorGroups = new ArrayList<DescriptorGroup>();
        List<Descriptor> descriptors = new ArrayList<Descriptor>();
        spotMetadata.setDescriptorGroups(descriptorGroups);
        spotMetadata.setDescriptors(descriptors);
        
        String metadata1 = metadataConfig.getFormulationSolutionDescription();
        String[] splitted = metadata1.split("::");
        String descriptorGroupName = splitted[0];
        
        DescriptorGroup group = new DescriptorGroup();
        group.setName(descriptorGroupName);
        DescriptionTemplate key1 = getKeyFromTemplate (descriptorGroupName, this.spotMetadataTemplate);
        group.setKey(key1);
        group.setDescriptors(new ArrayList<Description>());
        String buffer = splitted[1];
        Descriptor desc1 = new Descriptor();
        desc1.setName(buffer);
        DescriptionTemplate key2 = getKeyFromTemplate (buffer, this.spotMetadataTemplate);
        desc1.setKey(key2);
        String bufferValue = (config.getBufferColumn() != -1 && splitted.length > config.getBufferColumn()) ? splittedRow[config.bufferColumn].trim() : "";
        if (bufferValue.isEmpty()) {
        	if (defaultBuffer != null && !defaultBuffer.isEmpty()) {
        		desc1.setValue(defaultBuffer);	
        	} 
        } else if (!bufferValue.contains("#N/A")) {
        	desc1.setValue(bufferValue);
        }
        if (desc1.getValue() != null) {
        	group.getDescriptors().add(desc1);
        }
        String carrier = metadataConfig.getFormulationCarrierDescription().split("::")[1];
        Descriptor desc2 = new Descriptor();
        desc2.setName(carrier);
        DescriptionTemplate key3 = getKeyFromTemplate (carrier, this.spotMetadataTemplate);
        desc2.setKey(key3);
        String carrierValue = (config.getCarrierColumn() != -1 && splitted.length > config.getCarrierColumn()) ? splittedRow[config.carrierColumn].trim() : "";
        if (carrierValue.isEmpty()) {
        	if (defaultCarrier != null && !defaultCarrier.isEmpty()) {
        		desc2.setValue(defaultCarrier);	
        	}
        } else {
        	if (!carrierValue.contains("#N/A")) {
        		desc2.setValue(carrierValue);
        	}
        }
        if (desc2.getValue() != null)
        	group.getDescriptors().add(desc2);
        String method = metadataConfig.getFormulationMethodDescription().split("::")[1];
        Descriptor desc3 = new Descriptor();
        desc3.setName(method);
        DescriptionTemplate key4 = getKeyFromTemplate (method, this.spotMetadataTemplate);
        desc3.setKey(key4);
        String methodValue = (config.getMethodColumn() != -1 && splitted.length > config.getMethodColumn()) ? splittedRow[config.methodColumn].trim() : "";
        if (methodValue.isEmpty()) {
        	if (defaultMethod != null && !defaultMethod.isEmpty()) {
        		desc3.setValue(defaultMethod);	
        	} 
        } else if (!methodValue.contains("#N/A")) {
        	desc3.setValue(methodValue);
        }
        if (desc3.getValue() != null) {
        	group.getDescriptors().add(desc3);
        }
       
        String reference = metadataConfig.getFormulationReferenceDescription().split("::")[1];
        DescriptorGroup desc4 = new DescriptorGroup();
        desc4.setName(reference);
        DescriptionTemplate key5 = getKeyFromTemplate (reference, this.spotMetadataTemplate);
        desc4.setKey(key5);
        desc4.setDescriptors(new ArrayList<Description>());
        Descriptor subDesc4 = new Descriptor();
        subDesc4.setName("Value");
        DescriptionTemplate key6 = getKeyFromTemplate ("Value", this.spotMetadataTemplate);
        subDesc4.setKey(key6);
        String referenceValue =  (config.getReferenceColumn() != -1 && splitted.length > config.getReferenceColumn()) ? splittedRow[config.referenceColumn].trim() : "";
        if (referenceValue.isEmpty()) {
        	if (defaultReference != null && !defaultReference.isEmpty()) {
        		subDesc4.setValue(defaultReference);	
        	} 
        } else if (!methodValue.contains("#N/A")) {
        	subDesc4.setValue(referenceValue);
        }
        if (subDesc4.getValue() != null) {
            Descriptor sub1Desc4 = new Descriptor();
	        sub1Desc4.setName("Type");
	        sub1Desc4.setValue("PMID");  
	        desc4.getDescriptors().add(subDesc4);
	        desc4.getDescriptors().add(sub1Desc4);
	        group.getDescriptors().add(desc4);
        }
        String volume = metadataConfig.getVolumeDescription().split("::")[1];
        Descriptor desc5 = new Descriptor();
        desc5.setName(volume);
        DescriptionTemplate key7 = getKeyFromTemplate (volume, this.spotMetadataTemplate);
        desc5.setKey(key7);
        String volumeValue =  (config.getVolumeColumn() != -1 && splitted.length > config.getVolumeColumn()) ? splittedRow[config.volumeColumn].trim() : "";
        if (volumeValue.isEmpty()) {
        	if (defaultVolume != null && !defaultVolume.isEmpty()) {
        		desc5.setValue(defaultVolume);	
        	} 
        } else if (!volumeValue.contains("#N/A")) {
        	desc5.setValue(volumeValue);
        }
        if (desc5.getValue() != null) {
        	group.getDescriptors().add(desc5);
        }
        
        String dispenses = metadataConfig.getNumberDispensesDescription().split("::")[1];
        Descriptor desc6 = new Descriptor();
        desc6.setName(dispenses);
        DescriptionTemplate key8 = getKeyFromTemplate (dispenses, this.spotMetadataTemplate);
        desc6.setKey(key8);
        String dispensesValue =  (config.getDispensesColumn() != -1 && splitted.length > config.getDispensesColumn()) ? splittedRow[config.dispensesColumn].trim() : "";
        if (dispensesValue.isEmpty()) {
        	if (defaultDispenses != null && !defaultDispenses.isEmpty()) {
        		desc6.setValue(defaultDispenses);	
        	} 
        } else if (!dispensesValue.contains("#N/A")) {
        	desc6.setValue(dispensesValue);
        } else {
        	desc6.setNotRecorded(true);
        }
        if (desc6.getValue() != null || desc6.getNotRecorded()) {
        	group.getDescriptors().add(desc6);
        } 
        
        if (desc6.getNotRecorded()) {
        	// if there is buffer, then the group exists
        	// else, the whole group is not recorded
        	if (desc1.getValue() == null) {
        		group.setNotRecorded(true);
        	}
        } else if (desc6.getValue() != null) {
        	// there is number dispenses, buffer must be there
        	if (desc1.getValue() == null) {
        		// validation error!!!
        		ErrorMessage error = new ErrorMessage("Buffer is mandatory");
        		String[] codes = new String[] {metadataName};
                error.addError(new ObjectError ("buffer", codes, null, "NotFound"));
                errorList.add(error);  		
        	}
        }
        
        descriptorGroups.add(group);
        
        Descriptor comment = new Descriptor();
        comment.setName(metadataConfig.getCommentDescription());
        DescriptionTemplate key9 = getKeyFromTemplate (comment.getName(), this.spotMetadataTemplate);
        comment.setKey(key9);
        comment.setValue( (config.getCommentColumn() != -1 && splitted.length > config.getCommentColumn()) ? splittedRow[config.commentColumn]: "");
        descriptors.add(comment);
        
        return spotMetadata; 
    }

    public static DescriptionTemplate getKeyFromTemplate(String descriptorName, MetadataTemplate metadataTemplate) {
    	DescriptionTemplate template = null;
		if (metadataTemplate != null) {
			for (DescriptionTemplate desc: metadataTemplate.getDescriptors()) {
				if (desc.getName().equalsIgnoreCase(descriptorName)) 
					template = desc;
				if (template == null && desc instanceof DescriptorGroupTemplate) {
					template =  getKeyFromTemplate ((DescriptorGroupTemplate)desc, descriptorName);
				}
			}
		}
		return template;
	}

	public static DescriptionTemplate getKeyFromTemplate(DescriptorGroupTemplate group, String descriptorName) {
		DescriptionTemplate template = null;
		for (DescriptionTemplate desc: group.getDescriptors()) {
			if (desc.getName().equalsIgnoreCase(descriptorName)) 
				template = desc;
			if (template == null && desc instanceof DescriptorGroupTemplate) {
				template = getKeyFromTemplate ((DescriptorGroupTemplate)desc, descriptorName);
			}
		}	
		return template;
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
    
    
    public void exportToFile (SlideLayout layout, String outputFile) throws IOException {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
        //write header
        out.append("Block\tColumn\tRow\tID\tName\tRepoID\tGroup\tConcentration\tRatio\tBuffer\tVolume\tDispenses\tCarrier\tMethod\tReference\tComment\tPrinting Flags");
        out.append("\n");
        
        int noBlocks = 0;
        for (Block block: layout.getBlocks()) {
            noBlocks ++;
            for (Spot s: block.getBlockLayout().getSpots()) {
                StringBuffer row = new StringBuffer();
                row.append(noBlocks + "\t");
                row.append (s.getColumn() + "\t" + s.getRow() + "\t");
                if (s.getFeatures() != null && !s.getFeatures().isEmpty()) {
                    int i=0;
                    for (Feature f: s.getFeatures()) {
                        row.append(f.getInternalId() != null ? f.getInternalId() : f.getId());
                        if (i < s.getFeatures().size()-1) {
                            row.append("||");
                            
                        }
                        i++;
                    }
                    row.append("\t");
                    i=0;
                    for (Feature f: s.getFeatures()) {
                        row.append(f.getName());
                        if (i < s.getFeatures().size()-1) {
                            row.append("||");
                            
                        }
                        i++;
                    }
                    row.append("\t");
                    
                } else {
                    row.append("Empty\tEmpty\t");
                }
                // repoID - no need leave it empty
                row.append("\t");
                if (s.getGroup() != null) {
                    row.append(s.getGroup() + "\t");
                } else {
                    row.append("\t");
                }
                if (s.getRatioConcentrationMap() != null && !s.getRatioConcentrationMap().isEmpty()) {
                    int i = 0;
                    for (Feature f: s.getFeatures()) {
                        RatioConcentration con = s.getRatioConcentrationMap().get(f.getId());
                        if (con != null && con.getConcentration() != null) {
                            row.append(con.getConcentration().getConcentration() + con.getConcentration().getLevelUnit().getLabel());
                        } 
                        if (i < s.getFeatures().size()-1) {
                            row.append("||");
                            
                        }
                        i++;
                    }
                    row.append("\t");
                    i = 0;
                    for (Feature f: s.getFeatures()) {
                        RatioConcentration con = s.getRatioConcentrationMap().get(f.getId());
                        if (con != null && con.getRatio() != null) {
                            row.append(con.getRatio());
                        } 
                        if (i < s.getFeatures().size()-1) {
                            row.append(":");
                            
                        }
                        i++;
                    }
                    row.append("\t");
                } else {
                    row.append("\t\t");
                }
                    
                if (s.getMetadata() != null) {
                    String value = getMetadataValue (s.getMetadata(), metadataConfig.formulationSolutionDescription);
                    if (value != null) {
                        row.append(value + "\t");
                    } else {
                        row.append("\t");
                    }
                    
                    value = getMetadataValue (s.getMetadata(), metadataConfig.volumeDescription);
                    if (value != null) {
                        row.append(value + "\t");
                    } else {
                        row.append("\t");
                    }
                    
                    value = getMetadataValue (s.getMetadata(), metadataConfig.numberDispensesDescription);
                    if (value != null) {
                        row.append(value + "\t");
                    } else {
                        row.append("\t");
                    }
                    
                    value = getMetadataValue (s.getMetadata(), metadataConfig.formulationCarrierDescription);
                    if (value != null) {
                        row.append(value + "\t");
                    } else {
                        row.append("\t");
                    }
                    
                    value = getMetadataValue (s.getMetadata(), metadataConfig.formulationMethodDescription);
                    if (value != null) {
                        row.append(value + "\t");
                    } else {
                        row.append("\t");
                    }
                    
                    value = getMetadataValue (s.getMetadata(), metadataConfig.formulationReferenceDescription);
                    if (value != null) {
                        row.append(value + "\t");
                    } else {
                        row.append("\t");
                    }
                    value = getMetadataValue (s.getMetadata(), metadataConfig.commentDescription);
                    if (value != null) {
                        row.append(value + "\t");
                    } else {
                        row.append("\t");
                    }
                }
                
                if (s.getFlag() != null) {
                    row.append(s.getFlag() + "\t");
                } else {    
                    row.append("\t");
                }
                out.println(row.toString());
            }
        }
        out.close();
    }


    private String getMetadataValue(SpotMetadata metadata, String descriptorInfo) {
        String[] groups = descriptorInfo.split("::");
        if (groups.length > 1) {
            // find the descriptor group
            String group = groups[0];
            String descriptor = groups[1];
            for (DescriptorGroup descG: metadata.getDescriptorGroups()) {
                if ((descG.getName() != null && descG.getName().equals(group)) ||
                        descG.getKey() != null && descG.getKey().getName().equals(group)) {
                    if (descG.getDescriptors() != null) {
                        for (Description d: descG.getDescriptors()) {
                            if (d instanceof Descriptor && d.getName().equals(descriptor)) {
                                return ((Descriptor)d).getValue();
                            }
                        }
                    }
                }
            }
        } else {
            if (metadata.getDescriptors() != null) {
                for (Descriptor desc: metadata.getDescriptors()) {
                    if (desc.getName().equals(descriptorInfo)) {
                        return desc.getValue();
                    }
                 }
            }
        }
        return null;
    }
}
