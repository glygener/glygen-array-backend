package org.glygen.array.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.glygen.array.client.model.FeatureType;
import org.glygen.array.client.model.GlycanSequenceFormat;
import org.glygen.array.client.model.ImportGRITSLibraryResult;
import org.glygen.array.client.model.LinkerClassification;
import org.glygen.array.client.model.SequenceDefinedGlycan;
import org.glygen.array.client.model.SmallMoleculeLinker;
import org.glygen.array.client.model.UnknownGlycan;
import org.glygen.array.client.model.User;
import org.grits.toolbox.glycanarray.library.om.ArrayDesignLibrary;
import org.grits.toolbox.glycanarray.library.om.LibraryInterface;
import org.grits.toolbox.glycanarray.library.om.feature.Feature;
import org.grits.toolbox.glycanarray.library.om.feature.Glycan;
import org.grits.toolbox.glycanarray.library.om.feature.GlycanProbe;
import org.grits.toolbox.glycanarray.library.om.feature.Linker;
import org.grits.toolbox.glycanarray.library.om.feature.Ratio;
import org.grits.toolbox.glycanarray.library.om.layout.Block;
import org.grits.toolbox.glycanarray.library.om.layout.BlockLayout;
import org.grits.toolbox.glycanarray.library.om.layout.SlideLayout;
import org.grits.toolbox.util.structure.glycan.util.FilterUtils;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ch.qos.logback.classic.Logger;

@SpringBootApplication
@Configuration
public class Application implements CommandLineRunner {

	private static final Logger log = (Logger) LoggerFactory.getLogger(Application.class);
	
	@Bean
	@ConfigurationProperties("glygen")
	public GlygenSettings glygen() {
		return new GlygenSettings();
	}

	public static void main(String args[]) {
		new SpringApplicationBuilder(Application.class).run(args);
	}

	@Override
	public void run(String... args) throws Exception {
	    GlygenSettings settings = glygen();
		UserRestClient userClient = new UserRestClientImpl();
		userClient.setURL(settings.scheme + settings.host + settings.basePath);
		if (args == null || args.length < 3) {
			log.error("need to pass username and password and library file as arguments");
			return;
		}
		userClient.login(args[0], args[1]);
		User user = userClient.getUser(args[0]);
		log.info("got user information:" + user.getEmail());
		
		String importType = "All";
		if (args.length == 4) {
			importType = args[3];
		}
		
		GlycanRestClient glycanClient = new GlycanRestClientImpl();
		glycanClient.setUsername(args[0]);
		glycanClient.setPassword(args[1]);
		glycanClient.setURL(settings.scheme + settings.host + settings.basePath);
		
		Map<String, String> linkerClassificationMap = readLinkerClassificationMap ();
		
		// read Library and create glycans in the repository
		File libraryFile = new File (args[2]);
		if (libraryFile.exists()) {
			FileInputStream inputStream2 = new FileInputStream(libraryFile);
	        InputStreamReader reader2 = new InputStreamReader(inputStream2, "UTF-8");
	        List<Class> contextList = new ArrayList<Class>(Arrays.asList(FilterUtils.filterClassContext));
    		contextList.add(ArrayDesignLibrary.class);
	        JAXBContext context2 = JAXBContext.newInstance(contextList.toArray(new Class[contextList.size()]));
	        Unmarshaller unmarshaller2 = context2.createUnmarshaller();
	        ArrayDesignLibrary library = (ArrayDesignLibrary) unmarshaller2.unmarshal(reader2);
	        if (importType.equals("Glycan")) {
		        List<Glycan> glycanList = library.getFeatureLibrary().getGlycan();
		        for (Glycan glycan : glycanList) {
		        	org.glygen.array.client.model.Glycan view = null;
		        	if (glycan.getSequence() == null) {
		        	    if (glycan.getOrigSequence() == null && (glycan.getClassification() == null || glycan.getClassification().isEmpty())) {
		                    // this is not a glycan, it is either control or a flag
		        	        // do not create a glycan
		                } else {
		                    view = new UnknownGlycan();
		                }
		        	} else {
						view = new SequenceDefinedGlycan();
						((SequenceDefinedGlycan) view).setGlytoucanId(glycan.getGlyTouCanId());
						((SequenceDefinedGlycan) view).setSequence(glycan.getSequence());
						((SequenceDefinedGlycan) view).setSequenceType(GlycanSequenceFormat.GLYCOCT);
		        	}
		        	if (view != null) {
    		        	view.setInternalId(glycan.getId()+ "");
    					view.setName(glycan.getName());
    					view.setComment(glycan.getComment());
    					try {
    						glycanClient.addGlycan(view);
    					} catch (HttpClientErrorException e) {
    						log.info("Glycan " + glycan.getId() + " cannot be added", e);
    					}
		        	}
				}
	        } else if (importType.equals("Linker")) {
		        List<Linker> linkerList = library.getFeatureLibrary().getLinker();
		        List<LinkerClassification> classificationList = glycanClient.getLinkerClassifications();
		        for (Linker linker : linkerList) {
					SmallMoleculeLinker view = new SmallMoleculeLinker();
					if (linker.getPubChemId() != null) view.setPubChemId(linker.getPubChemId().longValue());
					view.setName(linker.getName());
					if (linker.getSequence() != null)
						view.setDescription(linker.getSequence());
					view.setComment(linker.getComment());
					if (linker.getPubChemId() == null) {
					    String classification = linkerClassificationMap.get(linker.getName());
		                if (classification != null) {
		                    LinkerClassification lClass = new LinkerClassification();
		                    lClass.setClassification(classification);
		                    view.setClassification(lClass);
		                }
		                else {   
		                    // assign a random classification
		                    Collections.shuffle(classificationList);
		                    view.setClassification((LinkerClassification) classificationList.get(0));
		                }
					}
					try {
						glycanClient.addLinker(view);
					} catch (HttpClientErrorException e) {
						log.info ("Linker " + linker.getId() + " cannot be added", e);
					}
				}
	        } else if (importType.equals("Feature")) {
                List<Feature> features = library.getFeatureLibrary().getFeature();
                List<LinkerClassification> classificationList = glycanClient.getLinkerClassifications();
                for (Feature f: features) {
                    org.glygen.array.client.model.Feature myFeature = new org.glygen.array.client.model.Feature();
                    myFeature.setName(f.getName());
                    List<Ratio> ratios = f.getRatio();
                    for (Ratio ratio : ratios) {
                        GlycanProbe probe = null;
                        for (GlycanProbe p : library.getFeatureLibrary().getGlycanProbe()) {
                            if (p.getId().equals(ratio.getItemId())) {
                                probe = p;
                                break;
                            }
                        }
                        if (probe != null) {
                            boolean linkerOnly = true;
                            for (Ratio r1 : probe.getRatio()) {
                                Glycan glycan = LibraryInterface.getGlycan(library, r1.getItemId());
                                if (glycan != null) {
                                    org.glygen.array.client.model.Glycan myGlycan = null;
                                    if (glycan.getSequence() == null) {
                                        myGlycan = new UnknownGlycan();
                                        myGlycan.setName(glycan.getName()); // name is sufficient to locate the unknown glycan
                                    } else {
                                        myGlycan = new org.glygen.array.client.model.SequenceDefinedGlycan();
                                        ((SequenceDefinedGlycan) myGlycan).setSequence(glycan.getSequence());  // sequence is sufficient to locate this Glycan in the repository
                                        ((SequenceDefinedGlycan) myGlycan).setSequenceType(GlycanSequenceFormat.GLYCOCT);
                                    }
                                    myFeature.addGlycan(myGlycan);
                                    myFeature.setType(FeatureType.NORMAL);
                                    linkerOnly = false;
                                }  
                            }
                            Linker linker = LibraryInterface.getLinker(library, probe.getLinker());
                            if (linker != null) {
                                org.glygen.array.client.model.SmallMoleculeLinker myLinker = new org.glygen.array.client.model.SmallMoleculeLinker();
                                if (linker.getPubChemId() != null) myLinker.setPubChemId(linker.getPubChemId().longValue());  // pubChemId is sufficient to locate this Linker in the repository
                                else {
                                    // need to set random classification and name
                                    myLinker.setName(linker.getName());
                                    String classification = linkerClassificationMap.get(linker.getName());
                                    if (classification != null) {
                                        LinkerClassification lClass = new LinkerClassification();
                                        lClass.setClassification(classification);
                                        myLinker.setClassification(lClass);
                                    }
                                    else {   
                                        // assign a random classification
                                        Collections.shuffle(classificationList);
                                        myLinker.setClassification((LinkerClassification) classificationList.get(0));
                                    }
                                }
                                myFeature.setLinker(myLinker);
                                if (linkerOnly) myFeature.setType(FeatureType.CONTROL);
                            } 
                        }
                    }
                    try {
                        glycanClient.addFeature(myFeature);
                    } catch (HttpClientErrorException e) {
                        log.info("Feature " + f.getName() + " cannot be added", e);
                    }
                }
           
	        } else if (importType.equals("BlockLayout")) {
		        List<BlockLayout> blockLayouts = library.getLayoutLibrary().getBlockLayout();
		        for (BlockLayout blockLayout : blockLayouts) {
		        	org.glygen.array.client.model.BlockLayout myLayout = new org.glygen.array.client.model.BlockLayout();
		        	myLayout.setName(blockLayout.getName());
		        	myLayout.setDescription(blockLayout.getComment());
		        	myLayout.setWidth(blockLayout.getColumnNum());
		        	myLayout.setHeight(blockLayout.getRowNum());
		        	myLayout.setSpots(GlycanRestClientImpl.getSpotsFromBlockLayout(library, blockLayout));
		        	
		        	try {
		        		glycanClient.addBlockLayout (myLayout);
					} catch (HttpClientErrorException e) {
						log.info("BlockLayout " + blockLayout.getId() + " cannot be added", e);
					}
				}
	        } else if (importType.equals("SlideLayout")) {
	        	List<SlideLayout> layouts = library.getLayoutLibrary().getSlideLayout();
	        	for (SlideLayout slideLayout : layouts) {
	        		org.glygen.array.client.model.SlideLayout mySlideLayout = new org.glygen.array.client.model.SlideLayout();
	        		mySlideLayout.setName(slideLayout.getName());
	        		mySlideLayout.setDescription(slideLayout.getDescription());
	        		
	        		List<org.glygen.array.client.model.Block> blocks = new ArrayList<org.glygen.array.client.model.Block>();
	        		int width = 0;
	        		int height = 0;
	        		for (Block block: slideLayout.getBlock()) {
	        			org.glygen.array.client.model.Block myBlock = new org.glygen.array.client.model.Block();
	        			myBlock.setColumn(block.getColumn());
	        			myBlock.setRow(block.getRow());
	        			if (block.getColumn() > width)
	        				width = block.getColumn();
	        			if (block.getRow() > height)
	        				height = block.getRow();
	        			Integer blockLayoutId = block.getLayoutId();
	        			BlockLayout blockLayout = LibraryInterface.getBlockLayout(library, blockLayoutId);
	        			org.glygen.array.client.model.BlockLayout myLayout = new org.glygen.array.client.model.BlockLayout();
	        			myLayout.setName(blockLayout.getName());
	        			myBlock.setBlockLayout(myLayout);
	        			myLayout.setSpots(GlycanRestClientImpl.getSpotsFromBlockLayout(library, blockLayout));
	        			blocks.add(myBlock);
	        		}
	        		
	        		mySlideLayout.setHeight(slideLayout.getHeight() == null ? height: slideLayout.getHeight());
	        		mySlideLayout.setWidth(slideLayout.getWidth() == null ? width: slideLayout.getWidth());
	        		mySlideLayout.setBlocks(blocks);
	        		
	        		try {
		        		glycanClient.addSlideLayout (mySlideLayout);
					} catch (HttpClientErrorException e) {
						log.info("SlideLayout " + slideLayout.getId() + " cannot be added", e);
					} catch (HttpServerErrorException e) {
						log.info("SlideLayout " + slideLayout.getId() + " cannot be added", e);
					}
				}
	        } else if (importType.equals("All")) {
	            ImportGRITSLibraryResult result = glycanClient.addFromLibrary(library, linkerClassificationMap, null);
	            for (org.glygen.array.client.model.SlideLayout layout: result.getAddedLayouts()) {
	                log.info("Added: " + layout.getName());
	            }
	            for (org.glygen.array.client.model.SlideLayout layout: result.getDuplicates()) {
                    log.info("Duplicate: " + layout.getName());
                }
	            for (org.glygen.array.client.model.SlideLayout layout: result.getErrors()) {
                    log.info("Error: " + layout.getName());
                }
	        }
		}
	}
	
	private Map<String, String> readLinkerClassificationMap() {
	    Map<String, String> map = new HashMap<String, String>();
	    Resource resource = new ClassPathResource("linkerClassifications.txt");
        try {
            File file = resource.getFile();
            Scanner scanner = new Scanner(file);
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                String[] mapping = line.split("\\t");
                if (mapping.length >= 2 && mapping[1].trim().length() > 0) {
                    map.put(mapping[0].trim(), mapping[1].trim());
                } 
            }
            scanner.close();
        } catch (IOException e) {
            log.error("Cannot read linker classification mapping file", e);
        }
        return map;
    }

	
	public class GlygenSettings {
		String host;
		String scheme;
		String basePath;
		/**
		 * @return the host
		 */
		public String getHost() {
			return host;
		}
		/**
		 * @param host the host to set
		 */
		public void setHost(String host) {
			this.host = host;
		}
		/**
		 * @return the scheme
		 */
		public String getScheme() {
			return scheme;
		}
		/**
		 * @param scheme the scheme to set
		 */
		public void setScheme(String scheme) {
			this.scheme = scheme;
		}
		/**
		 * @return the basePath
		 */
		public String getBasePath() {
			return basePath;
		}
		/**
		 * @param basePath the basePath to set
		 */
		public void setBasePath(String basePath) {
			this.basePath = basePath;
		}
	}
}
