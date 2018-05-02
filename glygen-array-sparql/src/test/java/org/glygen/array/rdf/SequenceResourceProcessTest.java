package org.glygen.array.rdf;
///**
// * 
// */
//package org.glycoinfo.rdf.glycan;
//
//import java.util.List;
//
//import org.glycoinfo.rdf.ResourceProcessException;
//import org.glycoinfo.rdf.ResourceProcessResult;
//import org.glycoinfo.rdf.SelectSparqlBean;
//import org.glycoinfo.rdf.SparqlException;
//import org.glycoinfo.rdf.dao.SparqlEntity;
//import org.glycoinfo.rdf.dao.virt.VirtSesameTransactionConfig;
//import org.junit.Assert;
//import org.springframework.boot.test.SpringApplicationConfiguration;
//import org.springframework.transaction.annotation.Transactional;
//
//import jp.bluetree.log.LevelType;
//
///**
// * @author aoki
// *
// * This work is licensed under the Creative Commons Attribution 4.0 International License. 
// * To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
// *
// */
////@RunWith(SpringJUnit4ClassRunner.class)
//@SpringApplicationConfiguration(classes = { SequenceResourceProcessConfig.class, VirtSesameTransactionConfig.class })
//public class SequenceResourceProcessTest {
//	
////	@Autowired
////	GlycoSequenceResourceProcess sequenceResourceProcess;
//	
////	@Test
//	@Transactional
//	public void testResourceProcess() {
//		// check if glycoSequence exists already
//		SelectSparqlBean select = new SelectSparqlBean("PREFIX glycan: <http://purl.jp/bio/12/glyco/glycan#>\n"
//				+ "PREFIX glytoucan:  <http://www.glytoucan.org/glyco/owl/glytoucan#>\n"
//				+ "SELECT\n"
//				+ "?Sequence\n"
//				+ "WHERE {\n"
//				+ "?GlycanSequenceURI a glycan:glycosequence .\n"
//				+ "?GlycanSequenceURI glycan:has_sequence ?Sequence .\n"
//				+ "?GlycanSequenceURI glycan:has_sequence \"TESTSEQUENCE\"^^xsd:string .\n"
//				+ "}"); 
//		try {
//			// This is an example of how to use SparqlBeans.  All that is needed is a valid SPARQL string, and the query will execute.
//			List<SparqlEntity> sparqlEntity = sequenceResourceProcess.getSparqlDAO().query(select);
//
//			// The results are a list of SparqlEntities, which is basically a hashmap.  
//			Assert.assertNotNull(sparqlEntity);
//			
//			// this should not exist.
//			Assert.assertEquals(0, sparqlEntity.size());
//		} catch (SparqlException e) {
//			e.printStackTrace();
//			Assert.fail();
//		}
//		
//		// now try adding it
//		try {
//			ResourceProcessResult result = sequenceResourceProcess.processGlycoSequence("TESTSEQUENCE", "0");
//			Assert.assertNotNull(result);
//			Assert.assertEquals(LevelType.DEBUG, result.getLogMessage().getLevel());
//		} catch (ResourceProcessException e1) {
//			e1.printStackTrace();
//			Assert.fail();
//		}
//		
//		// confirm it's there
//		try {
//			List<SparqlEntity> results = sequenceResourceProcess.getSparqlDAO().query(select);
//			Assert.assertNotNull(results);
//			Assert.assertEquals(1, results.size());
//
//			// The keys in the SparqlEntity are the same as the variable names use in the SPARQL query.
//			Assert.assertEquals("TESTSEQUENCE", results.iterator().next().getValue("Sequence"));
//		} catch (SparqlException e) {
//			e.printStackTrace();
//			Assert.fail();
//		}
//	}
//	
//}
