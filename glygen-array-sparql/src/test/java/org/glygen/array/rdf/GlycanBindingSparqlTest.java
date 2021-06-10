package org.glygen.array.rdf;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glycoinfo.rdf.InsertSparql;
import org.glycoinfo.rdf.SelectSparqlBean;
import org.glycoinfo.rdf.SparqlException;
import org.glycoinfo.rdf.dao.SparqlDAO;
import org.glycoinfo.rdf.dao.SparqlEntity;
import org.glycoinfo.rdf.dao.virt.SparqlDAOVirtSesameImpl;
import org.glycoinfo.rdf.dao.virt.VirtSesameTransactionConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
//import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * 
 * Glycan Binding Demo Test Case.
 * 
 * @author aoki
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {VirtSesameTransactionConfig.class, GlycanBindingConfig.class })
@EnableAutoConfiguration
public class GlycanBindingSparqlTest {

	@Autowired
	SparqlDAO sparqlDAO;

	@Autowired
	GlycanBindingInsertSparql insertSparql;
	
	@Autowired
	GlycanBindingSelectSparql selectSparql;

	/**
	 * 
	 * InsertBean test case.  Executes a query on the URI to assert insertion was done correctly.
	 * 
	 * @throws SparqlException
	 */
	@Test
	@Transactional
	public void insertSparql() throws SparqlException {
		sparqlDAO.insert(insertSparql);
		
		List<SparqlEntity> list = sparqlDAO.query(new SelectSparqlBean(insertSparql.getPrefix()
				+ "select ?"+ GlycanBindingInsertSparql.URI + " ?" + GlycanBindingInsertSparql.BINDING_VALUE + " from <http://array.glygen.org/demo> where { ?" + GlycanBindingInsertSparql.URI + " glycan:has_binding  ?" + GlycanBindingInsertSparql.BINDING_VALUE + " .}"));
		
		for (SparqlEntity sparqlEntity : list) {
			String output = sparqlEntity.getValue(GlycanBindingInsertSparql.URI);
			Assert.assertEquals("http://array.glygen.org/TEST001", output);
		}
	}
	
	/**
	 * 
	 * InsertBean test case.  Executes a query on the URI to assert insertion was done correctly.
	 * 
	 * @throws SparqlException
	 */
	@Test
	@Transactional
	public void insertSelectSparql() throws SparqlException {
		sparqlDAO.insert(insertSparql);
		
		sparqlDAO.query(selectSparql);
		List<SparqlEntity> list = sparqlDAO.query(selectSparql);
		
		for (SparqlEntity sparqlEntity : list) {
			String output = sparqlEntity.getValue(GlycanBindingInsertSparql.URI);
			Assert.assertEquals("http://array.glygen.org/TEST001", output);
		}
	}
	
}