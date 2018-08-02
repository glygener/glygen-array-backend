package org.glygen.array.config;


import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.glygen.array.virtuoso.RepositoryConnectionFactory;
import org.glygen.array.virtuoso.SesameConnectionFactory;
import org.glygen.array.virtuoso.SesameTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;

import virtuoso.rdf4j.driver.VirtuosoRepository;


@Configuration
public class SesameTransactionConfig {
	@Bean
 	SesameSparqlDAO sparqlDAO() {
  		return new SesameSparqlDAO();
  	}
  	
  	@Bean
  	public Repository getRepository() {
  		return new VirtuosoRepository(
  				getTripleStoreProperties().getUrl(), 
  				getTripleStoreProperties().getUsername(),
  				getTripleStoreProperties().getPassword());
  	}
  
  	@Bean(name="sesameTransactionManager")
  	SesameTransactionManager transactionManager() throws RepositoryException {
  		return new SesameTransactionManager(getSesameConnectionFactory());
  	}
  	
  	@Bean
  	TripleStoreProperties getTripleStoreProperties() {
  		return new TripleStoreProperties();
  	}
  	
  	@Bean
  	SesameConnectionFactory getSesameConnectionFactory() {
  		return new RepositoryConnectionFactory(getRepository());
  	}
}
