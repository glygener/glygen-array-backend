package org.glygen.array;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.service.GlygenArrayRepository;
import org.glygen.array.util.PubChemAPI;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(loader=MySpringBootContextLoader.class)
@SpringBootTest(classes = GlygenArrayApplication.class)
public class GlygenArrayRepositoryTest {
	
	@Autowired
	GlygenArrayRepository repository;

	@Autowired
	UserRepository userRepository;
	
	@Test
	public void testUpdate() {
		UserEntity user = userRepository.findByUsername("user");
		
		try {
			//add a test glycan first
			Glycan g = addTestGlycan(user);
			
			// then test update
			g.setName("updatedGlycan");
			g.setComment(null);
			g.setInternalId("TestSource2");
			
			String glycanId = g.getUri().substring(g.getUri().lastIndexOf("/")+1);
		
			repository.updateGlycan(g, user);
			Glycan updated = repository.getGlycanById(glycanId, user);
			assertTrue(updated.getName().equals("updatedGlycan"));
			assertTrue(updated.getInternalId().equals("TestSource2"));
			assertTrue(updated.getComment() == null || updated.getComment().equals(""));
			
			repository.deleteGlycan(glycanId, user);
			Glycan deleted = repository.getGlycanById(glycanId, user);
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
		UserEntity user = userRepository.findByUsername("user");
		try {
			//add a test glycan first
			Glycan g = addTestGlycan(user);
			int total = repository.getGlycanCountByUser(user);
			assertTrue ("total > 0", total > 0);
			
			repository.deleteGlycan(g.getUri().substring(g.getUri().lastIndexOf("/")+1), user);
		} catch (SparqlException | SQLException e) {
			e.printStackTrace();
			assertFalse("Failed to get count", true);
		}
	}
	
	@Test
	public void testLinkerCount() {
		UserEntity user = userRepository.findByUsername("user");
		try {
			//add a test linker first
			Linker g = addTestLinker(user);
			int total = repository.getLinkerCountByUser(user);
			assertTrue ("total > 0", total > 0);
			
			repository.deleteLinker(g.getUri().substring(g.getUri().lastIndexOf("/")+1), user);
		} catch (SparqlException | SQLException e) {
			e.printStackTrace();
			assertFalse("Failed to get count", true);
		}
	}
	
	@Test
	public void testListGlycans() {
		UserEntity user = userRepository.findByUsername("user");
		
		try {
			//add a test glycan first
			Glycan g = addTestGlycan(user);
			
			List<Glycan> glycans = repository.getGlycanByUser(user);
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
			repository.deleteGlycan(g.getUri().substring(g.getUri().lastIndexOf("/")+1), user);
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
		UserEntity user = userRepository.findByUsername("user");
		
		try {
			//add a test linker first
			Linker g = addTestLinker(user);
			
			List<Linker> linkers = repository.getLinkerByUser(user);
			assertTrue("List is not empty", !linkers.isEmpty());
			boolean found = false;
			for (Linker g1: linkers) {
				if (g1.getUri().contains(g.getUri())) {
					found = true;
					assertTrue("Name exists", g1.getName() != null);
					assertTrue("Name correct", g1.getName().equals(g.getName()));
				}
				
			}
			assertTrue ("Added glycan is in the list", found);
			
			// clean up
			repository.deleteGlycan(g.getUri().substring(g.getUri().lastIndexOf("/")+1), user);
		} catch (SparqlException e) {
			e.printStackTrace();
			assertFalse("Failed to get glycans", true);
		} catch (SQLException e) {
			e.printStackTrace();
			assertFalse("Failed to get glycans", true);
		}
			
	}
	
	private Glycan addTestGlycan (UserEntity user) throws SparqlException {
		//add a test glycan first
		Glycan g = new Glycan();
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
		g.setSequenceType("GlycoCT");
		g.setInternalId("TestSource");
		g.setComment("My Comment");
		g.setMass(100.0);
		
		String glycanId = repository.addGlycan(g, user);
		g.setUri(glycanId);
		return g;
	}
	
	
	@Test
	public void testUpdateLinker() {
		UserEntity user = userRepository.findByUsername("user");
		
		try {
			//add a test linker first
			Linker l = addTestLinker(user);
			
			// then test update
			l.setName("updatedLinker");
			l.setComment(null);
			
			String linkerId = l.getUri().substring(l.getUri().lastIndexOf("/")+1);
		
			repository.updateLinker(l, user);
			Linker updated = repository.getLinkerById(linkerId, user);
			assertTrue(updated.getName().equals("updatedLinker"));
			assertTrue(updated.getComment() == null || updated.getComment().equals(""));
			
			repository.deleteLinker(linkerId, user);
			Glycan deleted = repository.getGlycanById(linkerId, user);
			assertTrue("Deleted test linker", deleted == null);    // since name is stored in private graph, it should be cleared after delete
		} catch (SparqlException e) {
			e.printStackTrace();
			assertFalse("Failed to update linker", true);
		} catch (SQLException e) {
			e.printStackTrace();
			assertFalse("Failed to update linker", true);
		}
	}
	
	
	public Linker addTestLinker (UserEntity user) throws SparqlException { 
		
		Linker l = PubChemAPI.getLinkerDetailsFromPubChem("2444");
		l.setName("TestLinker");
		String linkerURI = repository.addLinker(l, user);
		l.setUri(linkerURI);
		
		return l;
	}
}
