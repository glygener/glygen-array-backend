package org.glygen.array.config;

import org.glycoinfo.rdf.dao.SparqlDAO;
import org.glycoinfo.rdf.dao.virt.VirtRepositoryConnectionFactory;
import org.glycoinfo.rdf.dao.virt.VirtSesameConnectionFactory;
import org.glycoinfo.rdf.dao.virt.VirtSesameTransactionManager;
import org.glycoinfo.rdf.utils.TripleStoreProperties;
import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import virtuoso.sesame2.driver.VirtuosoRepository;

@Configuration
public class SesameTransactionConfig {
	@Bean
 	SparqlDAO sparqlDAO() {
  		return new SesameSparqlDAO();
  	}
  	
  	@Bean
  	public Repository getRepository() {
  		return new VirtuosoRepository(
  				getTripleStoreProperties().getUrl(), 
  				getTripleStoreProperties().getUsername(),
  				getTripleStoreProperties().getPassword());
  	}
  
  	@Bean(name = "sesameTransactionManager")
  	VirtSesameTransactionManager transactionManager() throws RepositoryException {
  		return new VirtSesameTransactionManager(getSesameConnectionFactory());
  	}
  	
  	@Bean
  	TripleStoreProperties getTripleStoreProperties() {
  		return new TripleStoreProperties();
  	}
  	
  	@Bean
  	VirtSesameConnectionFactory getSesameConnectionFactory() {
  		return new VirtRepositoryConnectionFactory(getRepository());
  	}
}
