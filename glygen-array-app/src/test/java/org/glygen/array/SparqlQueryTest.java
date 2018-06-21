package org.glygen.array;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.glycoinfo.rdf.SparqlException;
import org.glycoinfo.rdf.dao.SparqlDAO;
import org.glycoinfo.rdf.dao.SparqlEntity;
import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SparqlQueryTest {
	
	@Autowired
	SparqlDAO sparqlDAO;
	
	@Test
	public void testUnionQuery () {
		String newURI = "http://array.glygen.org/" + "test1234";
		String query = "PREFIX glycan: <http://purl.jp/bio/12/glyco/glycan#>\n";
		query += "SELECT DISTINCT ?o\n";
		query += "FROM <http://array.glygen.org/demo/test> \n";
		query += "{{<" + newURI + "> ?p ?o . }\n" + 
									"UNION\n" +
								"{?s ?p <" + newURI + "> . }}";
		
		List<SparqlEntity> results;
		try {
			results = ((SesameSparqlDAO)sparqlDAO).query(query);
			assertEquals(results.size(), 0);
		} catch (SparqlException e) {
			assertFalse("failed to execute query: " + e, true);
		}
		
	}

}
