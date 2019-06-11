package org.glygen.array.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.glygen.array.client.model.GlycanSequenceFormat;
import org.glygen.array.client.model.GlycanView;
import org.glygen.array.client.model.LinkerView;
import org.glygen.array.client.model.User;
import org.grits.toolbox.glycanarray.library.om.ArrayDesignLibrary;
import org.grits.toolbox.glycanarray.library.om.LibraryInterface;
import org.grits.toolbox.glycanarray.library.om.feature.Feature;
import org.grits.toolbox.glycanarray.library.om.feature.Glycan;
import org.grits.toolbox.glycanarray.library.om.feature.GlycanProbe;
import org.grits.toolbox.glycanarray.library.om.feature.Linker;
import org.grits.toolbox.glycanarray.library.om.feature.Ratio;
import org.grits.toolbox.glycanarray.library.om.layout.BlockLayout;
import org.grits.toolbox.glycanarray.library.om.layout.Spot;
import org.grits.toolbox.util.structure.glycan.util.FilterUtils;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpClientErrorException;

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
		UserRestClient userClient = new UserRestClientImpl();
		if (args == null || args.length < 3) {
			log.error("need to pass username and password and library file as arguments");
			return;
		}
		userClient.login(args[0], args[1]);
		User user = userClient.getUser(args[0]);
		log.info("got user information:" + user.getEmail());
		
		String importType = "Glycan";
		if (args.length == 4) {
			importType = args[3];
		}
		
		GlycanRestClient glycanClient = new GlycanRestClientImpl();
		glycanClient.setUsername(args[0]);
		glycanClient.setPassword(args[1]);
		
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
					GlycanView view = new GlycanView();
					view.setInternalId(glycan.getId()+ "");
					view.setName(glycan.getName());
					view.setComment(glycan.getComment());
					view.setGlytoucanId(glycan.getGlyTouCanId());
					view.setSequence(glycan.getSequence());
					view.setSequenceFormat(GlycanSequenceFormat.GLYCOCT);
					try {
						glycanClient.addGlycan(view, user);
					} catch (HttpClientErrorException e) {
						log.info("Glycan " + glycan.getId() + " cannot be added", e);
					}
				}
	        } else if (importType.equals("Linker")) {
		        List<Linker> linkerList = library.getFeatureLibrary().getLinker();
		        for (Linker linker : linkerList) {
					LinkerView view = new LinkerView();
					if (linker.getPubChemId() != null) view.setPubChemId(linker.getPubChemId().longValue());
					view.setName(linker.getName());
					view.setComment(linker.getComment());
					try {
						glycanClient.addLinker(view, user);
					} catch (HttpClientErrorException e) {
						log.info ("Linker " + linker.getId() + " cannot be added", e);
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
		        	List<org.glygen.array.client.model.Spot> spots = new ArrayList<>();
		        	for (Spot spot: blockLayout.getSpot()) {
		        		org.glygen.array.client.model.Spot s = new org.glygen.array.client.model.Spot();
		        		s.setRow(spot.getY());
		        		s.setColumn(spot.getX());
		        		s.setGroup(spot.getGroup());
		        		s.setConcentration(spot.getConcentration());
		        		Feature feature = LibraryInterface.getFeature(library, spot.getFeatureId());
		        		List<org.glygen.array.client.model.Feature> features = new ArrayList<>();
		        		if (feature != null) {
			        		for (Ratio r : feature.getRatio()) {
			        			GlycanProbe probe = null;
			        			for (GlycanProbe p : library.getFeatureLibrary().getGlycanProbe()) {
			        				if (p.getId().equals(r.getItemId())) {
			        					probe = p;
			        					break;
			        				}
			        			}
			        			if (probe != null) {
			        				for (Ratio r1 : probe.getRatio()) {
			        					org.glygen.array.client.model.Feature myFeature = new org.glygen.array.client.model.Feature();
			        					Glycan glycan = LibraryInterface.getGlycan(library, r1.getItemId());
			        					if (glycan != null) {
					        				org.glygen.array.client.model.Glycan myGlycan = new org.glygen.array.client.model.Glycan();
					        				myGlycan.setSequence(glycan.getSequence());  // sequence is sufficient to locate this Glycan in the repository
					        				myFeature.setGlycan(myGlycan);
			        					}
					        			Linker linker = LibraryInterface.getLinker(library, probe.getLinker());
					        			if (linker != null) {
					        				org.glygen.array.client.model.Linker myLinker = new org.glygen.array.client.model.Linker();
					        				if (myLinker.getPubChemId() != null) myLinker.setPubChemId(linker.getPubChemId().longValue());  // pubChemId is sufficient to locate this Linker in the repository
					        				myFeature.setLinker(myLinker);
					        			}
					        			myFeature.setRatio(r1.getItemRatio());
					        			features.add(myFeature);
			        				}
			        			}			
			        		}
		        		}
		        		s.setFeatures(features);
		        		spots.add(s);
		        	}
		        	myLayout.setSpots(spots);
		        	
		        	try {
		        		glycanClient.addBlockLayout (myLayout, user);
					} catch (HttpClientErrorException e) {
						log.info("BlockLayout " + blockLayout.getId() + " cannot be added", e);
					}
				}
	        }
		}
		
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
