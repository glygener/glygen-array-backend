package org.glygen.array;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.Block;
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.GlycanInFeature;
import org.glygen.array.persistence.rdf.GlycanSequenceFormat;
import org.glygen.array.persistence.rdf.GlycoPeptide;
import org.glygen.array.persistence.rdf.LinkedGlycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.PeptideLinker;
import org.glygen.array.persistence.rdf.Publication;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.metadata.Sample;
import org.glygen.array.service.FeatureRepository;
import org.glygen.array.service.GlycanRepository;
import org.glygen.array.service.GlygenArrayRepository;
import org.glygen.array.service.LayoutRepository;
import org.glygen.array.service.LinkerRepository;
import org.glygen.array.util.pubchem.PubChemAPI;
import org.glygen.array.util.pubmed.DTOPublication;
import org.glygen.array.util.pubmed.PubmedUtil;
import org.grits.toolbox.glycanarray.library.om.layout.LevelUnit;
import org.grits.toolbox.glycanarray.om.model.UnitOfLevels;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(loader=MySpringBootContextLoader.class)
@SpringBootTest(classes = GlygenArrayApplication.class)
public class GlygenArrayRepositoryTest {
	
	@Autowired
	@Qualifier("glygenArrayRepositoryImpl")
	GlygenArrayRepository repository;
	
	@Autowired
	GlycanRepository glycanRepository;
	
	@Autowired
	LinkerRepository linkerRepository;
	
	@Autowired
	LayoutRepository layoutRepository;
	
	@Autowired
	FeatureRepository featureRepository;

	@Autowired
	UserRepository userRepository;
	
	@Test
	public void testUpdate() {
		UserEntity user = userRepository.findByUsernameIgnoreCase("user");
		
		try {
			//add a test glycan first
			Glycan g = addTestGlycan(user);
			
			// then test update
			g.setName("updatedGlycan");
			g.setDescription(null);
			g.setInternalId("TestSource2");
			
			String glycanId = g.getUri().substring(g.getUri().lastIndexOf("/")+1);
		
			glycanRepository.updateGlycan(g, user);
			Glycan updated = glycanRepository.getGlycanById(glycanId, user);
			assertTrue(updated.getName().equals("updatedGlycan"));
			assertTrue(updated.getInternalId().equals("TestSource2"));
			assertTrue(updated.getDescription() == null || updated.getDescription().equals(""));
			
			glycanRepository.deleteGlycan(glycanId, user);
			Glycan deleted = glycanRepository.getGlycanById(glycanId, user);
			assertTrue("Deleted test glycan", deleted == null);    // since name is stored in private graph, it should be cleared after delete
		} catch (SparqlException e) {
			e.printStackTrace();
			assertFalse("Failed to update", true);
		} catch (SQLException e) {
			e.printStackTrace();
			assertFalse("Failed to update", true);
		}
	}
	
	@Test
	public void testGlycanCount() {
		UserEntity user = userRepository.findByUsernameIgnoreCase("user");
		try {
			//add a test glycan first
			Glycan g = addTestGlycan(user);
			int total = glycanRepository.getGlycanCountByUser(user);
			assertTrue ("total > 0", total > 0);
			
			glycanRepository.deleteGlycan(g.getUri().substring(g.getUri().lastIndexOf("/")+1), user);
		} catch (SparqlException | SQLException e) {
			e.printStackTrace();
			assertFalse("Failed to get count", true);
		}
	}
	
	@Test
	public void testLinkerCount() {
		UserEntity user = userRepository.findByUsernameIgnoreCase("user");
		try {
			//add a test linker first
			Linker g = addTestLinker(user);
			int total = linkerRepository.getLinkerCountByUser(user);
			assertTrue ("total > 0", total > 0);
			
			linkerRepository.deleteLinker(g.getUri().substring(g.getUri().lastIndexOf("/")+1), user);
		} catch (SparqlException | SQLException e) {
			e.printStackTrace();
			assertFalse("Failed to get count", true);
		}
	}
	
	@Test
	public void testListGlycans() {
		UserEntity user = userRepository.findByUsernameIgnoreCase("user");
		
		try {
			//add a test glycan first
			Glycan g = addTestGlycan(user);
			
			List<Glycan> glycans = glycanRepository.getGlycanByUser(user);
			assertTrue("List is not empty", !glycans.isEmpty());
			boolean found = false;
			for (Glycan g1: glycans) {
				if (g1.getUri().contains(g.getUri())) {
					found = true;
					assertTrue("Name exists", g1.getName() != null);
					assertTrue("Name correct", g1.getName().equals(g.getName()));
				}
				
			}
			assertTrue ("Added glycan is in the list", found);
			
			// clean up
			glycanRepository.deleteGlycan(g.getUri().substring(g.getUri().lastIndexOf("/")+1), user);
		} catch (SparqlException e) {
			e.printStackTrace();
			assertFalse("Failed to get glycans", true);
		} catch (SQLException e) {
			e.printStackTrace();
			assertFalse("Failed to get glycans", true);
		}
			
	}
	
	@Test
	public void testListLinkers() {
		UserEntity user = userRepository.findByUsernameIgnoreCase("user");
		
		try {
			//add a test linker first
			Linker g = addTestLinker(user);
			
			List<Linker> linkers = linkerRepository.getLinkerByUser(user);
			assertTrue("List is not empty", !linkers.isEmpty());
			boolean found = false;
			for (Linker g1: linkers) {
				if (g1.getUri().contains(g.getUri())) {
					found = true;
					assertTrue("Name exists", g1.getName() != null);
					assertTrue("Name correct", g1.getName().equals(g.getName()));
				}
				
			}
			assertTrue ("Added linker is in the list", found);
			
			// clean up
			linkerRepository.deleteLinker(g.getUri().substring(g.getUri().lastIndexOf("/")+1), user);
		} catch (SparqlException e) {
			e.printStackTrace();
			assertFalse("Failed to get glycans", true);
		} catch (SQLException e) {
			e.printStackTrace();
			assertFalse("Failed to get glycans", true);
		}
			
	}
	
	private Glycan addTestGlycan (UserEntity user) throws SparqlException, SQLException {
		//add a test glycan first
		SequenceDefinedGlycan g = new SequenceDefinedGlycan();
		g.setName("Ttest Glycan");
		g.setSequence("RES\n" + 
				"\n" + 
				"1b:b-dglc-HEX-1:5\n" + 
				"\n" + 
				"2b:b-dgal-HEX-1:5\n" + 
				"\n" + 
				"3b:a-dgal-HEX-1:5\n" + 
				"\n" + 
				"LIN\n" + 
				"\n" + 
				"1:1o(4+1)2d\n" + 
				"\n" + 
				"2:2o(4+1)3d");
		g.setSequenceType(GlycanSequenceFormat.GLYCOCT);
		g.setInternalId("TestSource");
		g.setDescription("My Comment");
		g.setMass(100.0);
		
		String glycanId = glycanRepository.addGlycan(g, user);
		g.setUri(glycanId);
		return g;
	}
	
	
	@Test
	public void testUpdateLinker() {
		UserEntity user = userRepository.findByUsernameIgnoreCase("user");
		
		try {
			//add a test linker first
			Linker l = addTestLinker(user);
			
			// then test update
			l.setName("updatedLinker");
			l.setComment(null);
			
			String linkerId = l.getUri().substring(l.getUri().lastIndexOf("/")+1);
		
			linkerRepository.updateLinker(l, user);
			Linker updated = linkerRepository.getLinkerById(linkerId, user);
			assertTrue(updated.getName().equals("updatedLinker"));
			assertTrue(updated.getComment() == null || updated.getComment().equals(""));
			
			linkerRepository.deleteLinker(linkerId, user);
			Linker deleted = linkerRepository.getLinkerById(linkerId, user);
			assertTrue("Deleted test linker", deleted == null);    // since name is stored in private graph, it should be cleared after delete
		} catch (SparqlException e) {
			e.printStackTrace();
			assertFalse("Failed to update linker", true);
		} catch (SQLException e) {
			e.printStackTrace();
			assertFalse("Failed to update linker", true);
		}
	}
	
	@Test
	public void testAddBlockLayout() {
		UserEntity user = userRepository.findByUsernameIgnoreCase("user");
		try {
			Glycan g1 = addTestGlycan(user);
			
			// add another test glycan
			//add a test glycan first
			SequenceDefinedGlycan g = new SequenceDefinedGlycan();
			g.setName("Test Glycan2");
			g.setSequence("RES\n" + 
					"\n" + 
					"1b:x-dglc-HEX-1:5\n" + 
					"\n" + 
					"2s:n-acetyl\n" + 
					"\n" + 
					"3b:b-dgal-HEX-1:5\n" + 
					"\n" + 
					"4b:a-dgro-dgal-NON-2:6|1:a|2:keto|3:d\n" + 
					"\n" + 
					"5s:n-acetyl\n" + 
					"\n" + 
					"LIN\n" + 
					"\n" + 
					"1:1d(2+1)2n\n" + 
					"\n" + 
					"2:1o(4+1)3d\n" + 
					"\n" + 
					"3:3o(3+2)4d\n" + 
					"\n" + 
					"4:4d(5+1)5n");
			g.setSequenceType(GlycanSequenceFormat.GLYCOCT);
			g.setInternalId("TestSource2");
			g.setDescription("My Comment2");
			g.setMass(210.0);
			
			String glycanId = glycanRepository.addGlycan(g, user);
			g.setUri(glycanId);
			
			Linker linker1 = addTestLinker(user);
			String linkerId1 = linker1.getUri().substring(linker1.getUri().lastIndexOf("/")+1);
			
			Linker l = PubChemAPI.getLinkerDetailsFromPubChem(2341L);
			l.setName("TestLinker2");
			String linkerURI = linkerRepository.addLinker(l, user);
			l.setUri(linkerURI);
			String linkerId2 = l.getUri().substring(l.getUri().lastIndexOf("/")+1);
			
			BlockLayout blockLayout= addTestBlockLayout(user, g1, g, linker1, l);
			// add features first
			for (Spot s: blockLayout.getSpots()) {
			    for (Feature f: s.getFeatures()) {
			        featureRepository.addFeature(f, user);
			    }
			}
			String blockLayoutURI = layoutRepository.addBlockLayout(blockLayout, user);
			
			BlockLayout existing = layoutRepository.getBlockLayoutById(blockLayoutURI.substring(blockLayoutURI.lastIndexOf("/")+1), user);
			assertTrue ("Can retrieve added block layout", existing != null);
			assertTrue ("Spots size is 4", existing.getSpots() != null && existing.getSpots().size() == 4);
			
			List<BlockLayout> layouts = layoutRepository.getBlockLayoutByUser(user);
			assertTrue ("Users layouts is not empty", layouts != null && !layouts.isEmpty());
			
			// delete the glycans and linker and the block layout
			linkerRepository.deleteLinker(linkerId1, user);
			linkerRepository.deleteLinker(linkerId2, user);
			glycanRepository.deleteGlycan(g.getUri().substring(g.getUri().lastIndexOf("/")+1), user);
			glycanRepository.deleteGlycan(g1.getUri().substring(g1.getUri().lastIndexOf("/")+1), user);
			layoutRepository.deleteBlockLayout (blockLayoutURI.substring(blockLayoutURI.lastIndexOf("/")+1), user);
			
			existing = layoutRepository.getBlockLayoutById(blockLayoutURI.substring(blockLayoutURI.lastIndexOf("/")+1), user);
			assertTrue("Should be deleted", existing == null);
			
		} catch (SparqlException e) {
			e.printStackTrace();
			assertFalse("Failed to create block layout", true);
		} catch (SQLException e) {
			e.printStackTrace();
			assertFalse("Failed to create block layout", true);
		}
	}

	@Test
	public void testAddSlideLayout() {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase("user");
			Glycan g1 = addTestGlycan(user);
			
			// add another test glycan
			//add a test glycan first
			SequenceDefinedGlycan g = new SequenceDefinedGlycan();
			g.setName("Test Glycan2");
			g.setSequence("RES\n" + 
					"\n" + 
					"1b:x-dglc-HEX-1:5\n" + 
					"\n" + 
					"2s:n-acetyl\n" + 
					"\n" + 
					"3b:b-dgal-HEX-1:5\n" + 
					"\n" + 
					"4b:a-dgro-dgal-NON-2:6|1:a|2:keto|3:d\n" + 
					"\n" + 
					"5s:n-acetyl\n" + 
					"\n" + 
					"LIN\n" + 
					"\n" + 
					"1:1d(2+1)2n\n" + 
					"\n" + 
					"2:1o(4+1)3d\n" + 
					"\n" + 
					"3:3o(3+2)4d\n" + 
					"\n" + 
					"4:4d(5+1)5n");
			g.setSequenceType(GlycanSequenceFormat.GLYCOCT);
			g.setInternalId("TestSource2");
			g.setDescription("My Comment2");
			g.setMass(210.0);
			
			String glycanId = glycanRepository.addGlycan(g, user);
			g.setUri(glycanId);
			
			Linker linker1 = addTestLinker(user);
			String linkerId1 = linker1.getUri().substring(linker1.getUri().lastIndexOf("/")+1);
			
			Linker l = PubChemAPI.getLinkerDetailsFromPubChem(2341L);
			l.setName("TestLinker2");
			String linkerURI = linkerRepository.addLinker(l, user);
			l.setUri(linkerURI);
			String linkerId2 = l.getUri().substring(l.getUri().lastIndexOf("/")+1);
			
			BlockLayout blockLayout= addTestBlockLayout(user, g1, g, linker1, l);
			// add features first
            for (Spot s: blockLayout.getSpots()) {
                for (Feature f: s.getFeatures()) {
                    featureRepository.addFeature(f, user);
                }
            }
			String blockLayoutURI = layoutRepository.addBlockLayout(blockLayout, user);
			
			SlideLayout slideLayout = addTestSlideLayout(user, blockLayout);
			
			String slideLayoutURI = layoutRepository.addSlideLayout(slideLayout, user);
			SlideLayout existing = layoutRepository.getSlideLayoutById(slideLayoutURI.substring(slideLayoutURI.lastIndexOf("/")+1), user);
			assertTrue ("Can retrieve added slide layout", existing != null);
			assertTrue ("Blocks size is 2", existing.getBlocks() != null && existing.getBlocks().size() == 2);
			assertTrue ("Spots size is 4", existing.getBlocks().get(0).getBlockLayout().getSpots() != null 
					&& existing.getBlocks().get(0).getBlockLayout().getSpots().size() == 4);
			
			List<SlideLayout> layouts = layoutRepository.getSlideLayoutByUser(user);
			assertTrue ("Users layouts is not empty", layouts != null && !layouts.isEmpty());
			
			int count = layoutRepository.getSlideLayoutCountByUser(user);
			assertTrue(count >= 1);
			
			// delete the glycans and linker and the block layout and slide layout
			linkerRepository.deleteLinker(linkerId1, user);
			linkerRepository.deleteLinker(linkerId2, user);
			glycanRepository.deleteGlycan(g.getUri().substring(g.getUri().lastIndexOf("/")+1), user);
			glycanRepository.deleteGlycan(g1.getUri().substring(g1.getUri().lastIndexOf("/")+1), user);
			layoutRepository.deleteBlockLayout(blockLayoutURI.substring(blockLayoutURI.lastIndexOf("/")+1), user);
			layoutRepository.deleteSlideLayout(slideLayoutURI.substring(slideLayoutURI.lastIndexOf("/")+1), user);
			
		} catch (SparqlException e) {
			e.printStackTrace();
			assertFalse("Failed to create slide layout", true);
		} catch (SQLException e) {
			e.printStackTrace();
			assertFalse("Failed to create slide layout", true);
		}
	}
	
	public Linker addTestLinker (UserEntity user) throws SparqlException, SQLException { 
		
		Linker l = PubChemAPI.getLinkerDetailsFromPubChem(2444L);
		l.setName("TestLinker");
		try {
		    DTOPublication publication = new PubmedUtil().createFromPubmedId(6726807);
		    l.addPublication (getPublicationFrom(publication));
		} catch (Exception e) {
		    e.printStackTrace();
		}
		String linkerURI = linkerRepository.addLinker(l, user);
		l.setUri(linkerURI);
		
		return l;
	}
	
	Publication getPublicationFrom (DTOPublication pub) {
        Publication publication = new Publication ();
        publication.setAuthors(pub.getFormattedAuthor());
        publication.setDoiId(pub.getDoiId());
        publication.setEndPage(pub.getEndPage());
        publication.setJournal(pub.getJournal());
        publication.setNumber(pub.getNumber());
        publication.setPubmedId(pub.getPubmedId());
        publication.setStartPage(pub.getStartPage());
        publication.setTitle(pub.getTitle());
        publication.setVolume(pub.getVolume());
        publication.setYear(pub.getYear());
        
        return publication;
    }
	
	public Feature addTestFeature (UserEntity user, Glycan g1, Glycan g, Linker linker1) throws SparqlException, SQLException {
		Feature feature = new LinkedGlycan();
		if (g != null) {
		    GlycanInFeature featureGlycan = new GlycanInFeature();
		    featureGlycan.setGlycan(g);
		    ((LinkedGlycan) feature).addGlycan(featureGlycan);
		}
		if (g1 != null) {
		    GlycanInFeature featureGlycan = new GlycanInFeature();
            featureGlycan.setGlycan(g1);;
		    ((LinkedGlycan) feature).addGlycan(featureGlycan);
		}
		feature.setLinker(linker1);
		
		String uri = featureRepository.addFeature(feature, user);
		feature.setUri(uri);
		return feature;
		
	}
	
	@Test
	public void testAddFeature() {
		UserEntity user = userRepository.findByUsernameIgnoreCase("user");
		try {
			Glycan g1 = addTestGlycan(user);
			
			// add another test glycan
			//add a test glycan first
			SequenceDefinedGlycan g = new SequenceDefinedGlycan();
			g.setName("Test Glycan2");
			g.setSequence("RES\n" + 
					"\n" + 
					"1b:x-dglc-HEX-1:5\n" + 
					"\n" + 
					"2s:n-acetyl\n" + 
					"\n" + 
					"3b:b-dgal-HEX-1:5\n" + 
					"\n" + 
					"4b:a-dgro-dgal-NON-2:6|1:a|2:keto|3:d\n" + 
					"\n" + 
					"5s:n-acetyl\n" + 
					"\n" + 
					"LIN\n" + 
					"\n" + 
					"1:1d(2+1)2n\n" + 
					"\n" + 
					"2:1o(4+1)3d\n" + 
					"\n" + 
					"3:3o(3+2)4d\n" + 
					"\n" + 
					"4:4d(5+1)5n");
			g.setSequenceType(GlycanSequenceFormat.GLYCOCT);
			g.setInternalId("TestSource2");
			g.setDescription("My Comment2");
			g.setMass(210.0);
			
			String glycanId = glycanRepository.addGlycan(g, user);
			g.setUri(glycanId);
			
			Linker linker1 = addTestLinker(user);
			String linkerId1 = linker1.getUri().substring(linker1.getUri().lastIndexOf("/")+1);
			Feature added = addTestFeature(user, g1, g, linker1);
			String featureId = added.getUri().substring(added.getUri().lastIndexOf("/")+1);
			
			List<Feature> features = featureRepository.getFeatureByUser(user);
			assertTrue ("Can retrieve features", features != null);
			boolean found = false;
			for (Feature f: features) {
				if (f.getUri().equals(added.getUri())) {
					found = true;
					assertTrue("feature has 2 glycans", f instanceof LinkedGlycan && ((LinkedGlycan) f).getGlycans().size() == 2);
					assertTrue("feature has the added linker", f.getLinker().getUri().equals(linker1.getUri()));
				}
			}
			assertTrue ("added feature is in the list", found);
				
			// delete created linkers and glycans and the feature
			featureRepository.deleteFeature(featureId, user);
			linkerRepository.deleteLinker(linkerId1, user);
			glycanRepository.deleteGlycan(g.getUri().substring(g.getUri().lastIndexOf("/")+1), user);
			glycanRepository.deleteGlycan(g1.getUri().substring(g1.getUri().lastIndexOf("/")+1), user);
		} catch (SparqlException | SQLException e) {
			e.printStackTrace();
			assertFalse("Failed to create feature", true);
		}
	}
	
	
	@Test
	public void testAddPeptideFeature() {
		UserEntity user = userRepository.findByUsernameIgnoreCase("user");
		try {
			SequenceDefinedGlycan g = new SequenceDefinedGlycan();
			g.setSequence("RES\n" +
			"1b:b-dglc-HEX-1:5\n" +
			"2s:n-acetyl\n" +
			"LIN\n" + 
			"1:1d(2+1)2n");
			g.setSequenceType(GlycanSequenceFormat.GLYCOCT);
			
			String glycanId = glycanRepository.addGlycan(g, user);
			g.setUri(glycanId);
			
			PeptideLinker linker1 = new PeptideLinker();
			linker1.setSequence("AcNH-KTTKIP{GlcNAcb1-S}DSPQSA-COOH");
			String linkerURI = linkerRepository.addLinker(linker1, user);
			linker1.setUri(linkerURI);
			String linkerId1 = linker1.getUri().substring(linker1.getUri().lastIndexOf("/")+1);
			
			GlycoPeptide added = new GlycoPeptide();
			added.setName("test glycopeptide");
			added.setPeptide(linker1);
			LinkedGlycan lg = new LinkedGlycan();
			GlycanInFeature featureGlycan = new GlycanInFeature();
	        featureGlycan.setGlycan(g);
			lg.addGlycan(featureGlycan);
			added.addGlycan(lg);
			String lgURI = featureRepository.addFeature(lg, user);
			lg.setUri(lgURI);
			
			String uri = featureRepository.addFeature(added, user);
			added.setUri(uri);
			
			String featureId = added.getUri().substring(added.getUri().lastIndexOf("/")+1);
			
			List<Feature> features = featureRepository.getFeatureByUser(user);
			assertTrue ("Can retrieve features", features != null);
			boolean found = false;
			for (Feature f: features) {
				if (f.getUri().equals(added.getUri())) {
					found = true;
					assertTrue("feature has 1 glycan", f instanceof GlycoPeptide && ((GlycoPeptide) f).getGlycans().size() == 1);
					assertTrue("feature has the added linker", f instanceof GlycoPeptide && ((GlycoPeptide) f).getPeptide().getUri().equals(linker1.getUri()));
					assertTrue("feature has position map", f.getPositionMap().size() >= 0);
					break;
				}
			}
			assertTrue ("added feature is in the list", found);
				
			// delete created linkers and glycans and the feature
			featureRepository.deleteFeature(featureId, user);
			linkerRepository.deleteLinker(linkerId1, user);
			featureRepository.deleteFeature(lgURI.substring(lgURI.lastIndexOf("/")+1), user);
			glycanRepository.deleteGlycan(g.getUri().substring(g.getUri().lastIndexOf("/")+1), user);
			
		} catch (SparqlException | SQLException e) {
			e.printStackTrace();
			assertFalse("Failed to create feature", true);
		}
	}
	
	public BlockLayout addTestBlockLayout (UserEntity user, Glycan g1, Glycan g, Linker linker1, Linker l) throws SparqlException {
		BlockLayout blockLayout = new BlockLayout();
		blockLayout.setName("test layout");
		blockLayout.setDescription("Test comments");
		blockLayout.setHeight(2);
		blockLayout.setWidth(2);
		
		LevelUnit concentration = new LevelUnit();
		concentration.setConcentration(10.0);
		concentration.setLevelUnit(UnitOfLevels.FMOL);
		
		List<Spot> spots = new ArrayList<Spot> ();
		for (int i=0; i < blockLayout.getWidth(); i++) {
			Feature feature = new LinkedGlycan();
			if (i==0) {
			    GlycanInFeature featureGlycan = new GlycanInFeature();
	            featureGlycan.setGlycan(g1);
				((LinkedGlycan) feature).addGlycan(featureGlycan);
				feature.setLinker(linker1);
			}
			if (i==1) {
			    GlycanInFeature featureGlycan = new GlycanInFeature();
	            featureGlycan.setGlycan(g);
				((LinkedGlycan) feature).addGlycan(featureGlycan);
				feature.setLinker(l);
			}
			for (int j=0; j < blockLayout.getHeight(); j++) {
				Spot spot = new Spot();
				spot.setRow(j);
				spot.setColumn(i);
				spot.setConcentration(concentration);
				spot.setGroup(j);
				List<Feature> features = new ArrayList<Feature>();
				features.add(feature);
				spot.setFeatures(features);
				spots.add(spot);
			}
		}
		blockLayout.setSpots(spots);
		return blockLayout;
	}
	
	public SlideLayout addTestSlideLayout (UserEntity user, BlockLayout b) throws SparqlException {
		SlideLayout slideLayout = new SlideLayout();
		slideLayout.setName("TestSlideLayout");
		slideLayout.setDescription("SlideLayout with two columns - one row - same block layout");
		slideLayout.setWidth(2);
		slideLayout.setHeight(1);
		Block block1 = new Block();
		block1.setRow(0);
		block1.setColumn(0);
		block1.setBlockLayout(b);
		
		Block block2 = new Block();
		block2.setRow(0);
		block2.setColumn(1);
		block2.setBlockLayout(b);
		
		List<Block> blocks = new ArrayList<Block>();
		blocks.add(block1);
		blocks.add(block2);
		slideLayout.setBlocks(blocks);
		
		return slideLayout;
	}
}
