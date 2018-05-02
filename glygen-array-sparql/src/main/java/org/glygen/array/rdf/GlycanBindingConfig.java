package org.glygen.array.rdf;

import org.glycoinfo.rdf.InsertSparql;
import org.glycoinfo.rdf.dao.SparqlDAO;
import org.glycoinfo.rdf.dao.SparqlEntity;
import org.glycoinfo.rdf.dao.virt.SparqlDAOVirtSesameImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GlycanBindingConfig implements GlycanBindingKeys {
	
	
	@Bean
	SparqlDAO getSparqlDAO() {
		return new SparqlDAOVirtSesameImpl();
	}
	
	@Bean
	GlycanBindingInsertSparql glycanBindingInsertSparql() {
		GlycanBindingInsertSparql ins = new GlycanBindingInsertSparql();
		SparqlEntity sparqlentity = new SparqlEntity();
		sparqlentity.setValue(GlycanBindingInsertSparql.URI, "http://array.glygen.org/TEST001");
		sparqlentity.setValue(BINDING_VALUE, "TESTBINDING001");
		ins.setSparqlEntity(sparqlentity);

		// example of overriding the graph at application-layer.
		ins.setGraph("http://application-level-override/test");
		return ins;
	}
	
	@Bean
	GlycanBindingSelectSparql glycanBindingSelectSparql() {
		GlycanBindingSelectSparql bean = new GlycanBindingSelectSparql();
		bean.setFrom("FROM <http://application-level-override/test>");
		SparqlEntity sparqlentity = new SparqlEntity();
		sparqlentity.setValue(GlycanBindingInsertSparql.URI, "http://array.glygen.org/TEST001");
		sparqlentity.setValue(BINDING_VALUE, "TESTBINDING001");
		bean.setSparqlEntity(sparqlentity);

		return bean;
	}
}
