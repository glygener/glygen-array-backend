package org.glygen.array.rdf;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.List;

import org.glycoinfo.rdf.DeleteSparql;
import org.glycoinfo.rdf.DeleteSparqlBean;
import org.glycoinfo.rdf.InsertSparqlBean;
import org.glycoinfo.rdf.SelectSparqlBean;
import org.glycoinfo.rdf.SparqlException;
import org.glycoinfo.rdf.dao.SparqlDAO;
import org.glycoinfo.rdf.dao.SparqlEntity;
import org.glycoinfo.rdf.dao.virt.VirtSesameTransactionConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
//import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
// @SpringApplicationConfiguration(classes =
// {GlycanBindingInsertSparqlTest.class, VirtSesameTransactionConfig.class })
@SpringBootTest(classes = { VirtSesameTransactionConfig.class })
@EnableAutoConfiguration
public class SparqlDAOImplTest {

	private static final Logger logger = LoggerFactory.getLogger(SparqlDAOImplTest.class);

	@Autowired
	SparqlDAO schemaDAO;

	public static final String prefix = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
			+ "PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n"
			+ "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n" + "PREFIX dcterms: <http://purl.org/dc/terms/> \n"
			+ "PREFIX dbpedia2: <http://dbpedia.org/property/> \n" + "PREFIX dbpedia: <http://dbpedia.org/> \n"
			+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n" + "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
			+ "PREFIX glycan: <http://purl.jp/bio/12/glyco/glycan#> \n"
			+ "PREFIX glytoucan:  <http://www.glytoucan.org/glyco/owl/glytoucan#> \n";

	public static final String from = "from <http://rdf.glytoucan.org/core>\n"
			+ "from <http://glytoucan.org/rdf/demo/msdb/7>\n"
			+ "from <http://purl.jp/bio/12/glyco/glycan/ontology/0.18>\n"
			+ "from <http://www.glytoucan.org/glyco/owl/glytoucan>\n";

	public static final String using = "USING <http://glytoucan.org/rdf/demo/0.8>\n"
			+ "USING <http://glytoucan.org/rdf/demo/msdb/8>\n"
			+ "USING <http://purl.jp/bio/12/glyco/glycan/ontology/0.18>\n"
			+ "USING <http://www.glytoucan.org/glyco/owl/glytoucan>\n";

	/**
	 * 
	 * SELECT statement unit test.
	 * 
	 */
	@Test
	@Transactional
	public void testQuery() {
		String query = "SELECT  ?s ?v ?o WHERE\n" + "  { ?s ?v ?o . }\n" + "LIMIT   5\n" + "";
		try {
			List<SparqlEntity> list = schemaDAO.query(new SelectSparqlBean(query));
			SparqlEntity row = list.get(0);
			logger.debug("Node:>" + row.getValue("s"));
			logger.debug("graph:>" + row.getValue("type"));
		} catch (Exception e) {
			e.printStackTrace();
			assertFalse("Exception occurred while querying schema.", true);
		}
	}

	/**
	 * 
	 * Blank node test case.
	 * 
	 */
	@Test
	@Transactional
	public void testQuery2() {
		String query = "SELECT distinct ?s WHERE  {[] a ?s}  LIMIT 100";
		try {
			List<SparqlEntity> list = schemaDAO.query(new SelectSparqlBean(query));
			SparqlEntity row = list.get(0);
			logger.debug("Node:>" + row.getValue("s"));
			logger.debug("graph:>" + row.getValue("graph"));
		} catch (Exception e) {
			e.printStackTrace();
			assertFalse("Exception occurred while querying schema.", true);
		}
	}

	/**
	 * 
	 * Test query with optional clause.
	 * 
	 */
	public void testQueryOptional() {
		String query = prefix + "SELECT DISTINCT  ?AccessionNumber ?img ?Mass ?Motif ?ID ?Contributor ?time\n" + from
				+ "WHERE {\n" + "?s a glycan:saccharide .\n" + "?s glytoucan:has_primary_id ?AccessionNumber .\n"
				+ "?s glycan:has_image ?img .\n" + "?img a glycan:image .\n"
				+ "?img dc:format \"image/png\"^^xsd:string .\n"
				+ "?img glycan:has_symbol_format glycan:symbol_format_cfg .\n"
				+ "?s glytoucan:has_derivatized_mass ?dmass.\n" + "?dmass a glytoucan:derivatized_mass .\n"
				+ "?dmass glytoucan:has_derivatization_type glytoucan:derivatization_type_none .\n"
				+ "?dmass glytoucan:has_mass ?Mass .\n" + "OPTIONAL{\n" + "?s glycan:has_motif ?motif .\n"
				+ "?motif a glycan:glycan_motif .\n" + "?motif foaf:name ?Motif .\n"
				+ "?motif glytoucan:has_primary_id ?ID .\n" + "}\n" + "?s glycan:has_resource_entry ?entry.\n"
				+ "?entry a glycan:resource_entry .\n" + "?entry glytoucan:contributor ?contributor .\n"
				+ "?contributor foaf:name ?Contributor .\n" + "?entry glytoucan:date_registered ?time .\n"
				+ "} LIMIT 10";
		try {
			logger.debug("query:>" + query + "<");
			List<SparqlEntity> list = schemaDAO.query(new SelectSparqlBean(query));
			if (list.size() > 0) {
				SparqlEntity row = list.get(0);
				logger.debug("Node:>" + row.getValue("s"));
				logger.debug("graph:>" + row.getValue("graph"));
			} else {
				fail();
			}
		} catch (Exception e) {
			e.printStackTrace();
			assertFalse("Exception occurred while querying schema.", true);
		}
	}

	/**
	 * 
	 * Insert statement test using literals and other special characters.
	 * 
	 * @throws SparqlException
	 */
	@Test
	@Transactional
	public void testInsert() throws SparqlException {
		schemaDAO.insert(new InsertSparqlBean(
				"insert data { graph <http://bluetree.jp/nobutest> { <http://bluetree.jp/nobutest/aa> <http://bluetree.jp/nobutest/bb> \"cc\" . \n"
						+ "<http://bluetree.jp/nobutest/xx> <http://bluetree.jp/nobutest/yy> <http://bluetree.jp/nobutest/zz> . \n"
						+ "<http://bluetree.jp/nobutest/mm> <http://bluetree.jp/nobutest/nn> \"Some\\nlong\\nliteral\\nwith language\" . \n"
						+ "<http://bluetree.jp/nobutest/oo> <http://bluetree.jp/nobutest/pp> \"12345\"^^<http://www.w3.org/2001/XMLSchema#int>\n  } }"));
		String query = prefix + "SELECT ?s ?v ?o\n" + "from <http://bluetree.jp/nobutest>\n"
				+ "WHERE { ?s ?v ?o } limit 10";

		try {
			logger.debug("query:>" + query);
			List<SparqlEntity> list = schemaDAO.query(new SelectSparqlBean(query));
			if (list.size() > 0) {
				SparqlEntity row = list.get(0);
				logger.debug("s:>" + row.getValue("s"));
				logger.debug("v:>" + row.getValue("v"));
				logger.debug("o:>" + row.getValue("o"));
			} else
				fail();
		} catch (Exception e) {
			e.printStackTrace();
			assertFalse("Exception occurred while querying schema.", true);
		}
	}

	/**
	 * 
	 * Simple delete data test case.
	 * 
	 * @throws SparqlException
	 */
	@Test
	@Transactional
	public void testDelete() throws SparqlException {
		schemaDAO.delete(new DeleteSparqlBean("PREFIX glycan: <http://purl.jp/bio/12/glyco/glycan#> "
				+ "DELETE DATA { graph <http://bluetree.jp/nobutest> {"
				+ "<http://bluetree.jp/nobutest/aa> <http://bluetree.jp/nobutest/bb> \"cc\" . } }"));

	}

	/**
	 * 
	 * PLEASE NOTE: This test case is an example of a the assertion embedded inside
	 * the org.openrdf library. The fastest workaround is to remove the "-ea"
	 * parameter in the Test Case configuration. In Eclipse this is in "Run
	 * Configurations..."
	 * 
	 * @throws SparqlException
	 */
	@Test
	@Transactional
	public void testDelete2() throws SparqlException {
		schemaDAO.insert(
				new InsertSparqlBean("insert data { graph <http://bluetree.jp/nobutest> { <1> <2> \"3\" . \n } }"));
		String query = prefix + "SELECT ?s ?v ?o\n" + "from <http://bluetree.jp/nobutest>\n"
				+ "WHERE { <1> ?v ?o } limit 10";

		try {
			logger.debug("query:>" + query);
			List<SparqlEntity> list = schemaDAO.query(new SelectSparqlBean(query));
			if (list.size() > 0) {
				SparqlEntity row = list.get(0);
				logger.debug("s:>" + row.getValue("s"));
				logger.debug("v:>" + row.getValue("v"));
				logger.debug("o:>" + row.getValue("o"));
			} else
				fail();
		} catch (Exception e) {
			e.printStackTrace();
			assertFalse("Exception occurred while querying schema.", true);
		}

		schemaDAO.delete(new DeleteSparqlBean("PREFIX glycan: <http://purl.jp/bio/12/glyco/glycan#> "
				+ "DELETE DATA { graph <http://bluetree.jp/nobutest> {" + "<1> <2> \"3\" . } }"));

		try {
			logger.debug("query:>" + query);
			List<SparqlEntity> list = schemaDAO.query(new SelectSparqlBean(query));
			if (list.size() > 0) {
				SparqlEntity row = list.get(0);
				logger.debug("s:>" + row.getValue("s"));
				logger.debug("v:>" + row.getValue("v"));
				logger.debug("o:>" + row.getValue("o"));
				fail();
			} else
				return;
		} catch (Exception e) {
			e.printStackTrace();
			assertFalse("Exception occurred while querying schema.", true);
		}

	}

	/**
	 * 
	 * More complex delete data test case.
	 * 
	 * @throws SparqlException
	 */
	@Test
	@Transactional
	public void testDelete3() throws SparqlException {
		schemaDAO.insert(
				new InsertSparqlBean("insert data { graph <http://bluetree.jp/nobutest> { <1> <2> \"3\" . \n } }"));
		String query = prefix + "SELECT ?v ?o\n" + "from <http://bluetree.jp/nobutest>\n"
				+ "WHERE { <1> ?v ?o } limit 10";

		try {
			logger.debug("query:>" + query);
			List<SparqlEntity> list = schemaDAO.query(new SelectSparqlBean(query));
			if (list.size() > 0) {
				SparqlEntity row = list.get(0);
				// logger.debug("s:>" + row.getValue("s"));
				logger.debug("v:>" + row.getValue("v"));
				logger.debug("o:>" + row.getValue("o"));
			} else
				fail();
		} catch (Exception e) {
			e.printStackTrace();
			assertFalse("Exception occurred while querying schema.", true);
		}

		schemaDAO.delete(new DeleteSparqlBean("PREFIX glycan: <http://purl.jp/bio/12/glyco/glycan#> "
				+ "DELETE WHERE { graph <http://bluetree.jp/nobutest> {" + "<1> ?v ?o . } }"));

		try {
			logger.debug("query:>" + query);
			List<SparqlEntity> list = schemaDAO.query(new SelectSparqlBean(query));
			if (list.size() > 0) {
				SparqlEntity row = list.get(0);
				// logger.debug("s:>" + row.getValue("s"));
				logger.debug("v:>" + row.getValue("v"));
				logger.debug("o:>" + row.getValue("o"));
				fail();
			} else
				return;
		} catch (Exception e) {
			e.printStackTrace();
			assertFalse("Exception occurred while querying schema.", true);
		}

	}

	/**
	 * 
	 * Delete data test case.
	 * 
	 * @throws SparqlException
	 */
	@Test
	@Transactional
	public void testDeleteBean() throws SparqlException {
		DeleteSparql ds = new DeleteSparqlBean();

		ds.setPrefix(prefix);
		ds.setDelete("<http://bluetree.jp/nobutest/aa> <http://bluetree.jp/nobutest/bb> \"cc\"");
		ds.setGraph("http://bluetree.jp/nobutest");
		schemaDAO.delete(ds);
	}

	/**
	 * 
	 * Example of a graph clear update.  Test case is commented out so that it does not run automatically.
	 * 
	 * @throws SparqlException
	 */
	// @Test
	@Transactional
	public void testClearGraph() throws SparqlException {
		schemaDAO.execute(new InsertSparqlBean("clear graph <http://bluetree.jp/nobutest>"));
	}

	/**
	 * 
	 * Example of a test case that does not rollback because the Transactional
	 * annotation does not exist.  Test annotation is commented out so that it does not run automatically.
	 * 
	 * @throws SparqlException
	 */
//	@Test
	public void testInsertWurcs() throws SparqlException {
		schemaDAO.insert(new InsertSparqlBean("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
				+ "		PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
				+ "		PREFIX glycan: <http://purl.jp/bio/12/glyco/glycan#>"
				+ "		PREFIX glytoucan:  <http://www.glytoucan.org/glyco/owl/glytoucan#>" + "		INSERT INTO"
				+ "		GRAPH <http://deleteme>"
				+ "		{ <http://www.glycoinfo.org/rdf/glycan/G36373OJ> glycan:has_glycosequence <http://www.glycoinfo.org/rdf/glycan/G36373OJ/seq> ."
				+ "		<http://www.glycoinfo.org/rdf/glycan/G36373OJ/seq> glycan:has_sequence \"WURCS=2.0/5,4/[12211m-1a_1-5][12211m-1a_1-5][12211m-1a_1-5][12112m-1b_1-5][12122h-1b_1-5]a2-b1_b4-c1_c3-d1_c4-e1\"^^xsd:string ."
				+ "		<http://www.glycoinfo.org/rdf/glycan/G36373OJ/seq> glycan:in_carbohydrate_format glycan:carbohydrate_format_wurcs ."
				+ "		<http://www.glycoinfo.org/rdf/glycan/G36373OJ/seq> glytoucan:is_glycosequence_of <http://www.glycoinfo.org/rdf/glycan/G36373OJ> ."
				+ "		 }"));
	}
}