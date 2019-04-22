package org.glygen.array;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.service.GlygenArrayRepository;
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
			
			String glycanId = repository.addGlycan(g, user);
			
			// then test update
			g.setUri(glycanId);
			g.setName("updatedGlycan");
			g.setComment(null);
			g.setInternalId("TestSource2");
		
			repository.updateGlycan(g, user);
			Glycan updated = repository.getGlycanFromURI(g.getUri());
			assertTrue(updated.getName().equals("updatedGlycan"));
			assertTrue(updated.getInternalId().equals("TestSource2"));
			assertTrue(updated.getComment() == null || updated.getComment().equals(""));
			
			repository.deleteGlycan(glycanId.substring(glycanId.lastIndexOf("/")+1), user);
			Glycan deleted = repository.getGlycanFromURI(g.getUri());
			assertTrue("Deleted test glycan", deleted == null);
		} catch (SparqlException e) {
			assertFalse("Failed to update", true);
		}
	}
}
