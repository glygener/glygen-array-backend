package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Random;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.PrivateGraphEntity;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.GraphPermissionRepository;
import org.glygen.array.persistence.dao.PrivateGraphRepository;
import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.glygen.array.persistence.dao.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(value="sesameTransactionManager") 
public class GlygenArrayRepositoryImpl implements GlygenArrayRepository {
	final static Logger logger = LoggerFactory.getLogger("event-logger");
	
	@Autowired
	SesameSparqlDAO sparqlDAO;
	
	@Autowired
	PrivateGraphRepository graphRepository;
	
	@Autowired
	GraphPermissionRepository permissionRepository;
	
	@Autowired
	UserRepository userRepository;
	
	Random random = new Random();
	
	public static String prefix="PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>"
			+ "\nPREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
			+ "\nPREFIX gadr: <http://purl.org/gadr/data#>"
			+ "\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>";
	
	final static String hasDescriptionPredicate = ontPrefix + "has_description";
	final static String hasCreatedDatePredicate = ontPrefix + "has_date_created";
	final static String hasAddedToLibraryPredicate = ontPrefix + "has_date_addedtolibrary";
	final static String hasModifiedDatePredicate = ontPrefix + "has_date_modified";
	final static String hasPublicURIPredicate = ontPrefix + "has_public_uri";
	final static String hasTypePredicate = ontPrefix + "has_type";
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String addPrivateGraphForUser (UserEntity uEntity) throws SQLException {
		String URI = sparqlDAO.addGraph(PRIVATE_GRAPH, uEntity.getUsername());
		PrivateGraphEntity graph = new PrivateGraphEntity();
		graph.setUser(uEntity);
		graph.setGraphIRI(URI);
		graphRepository.save (graph);
		return URI;
	}
	
	/**
	 * generate a random number and check it against existing ids in public graph (DEFAULT)
	 * 
	 * @param pre prefix to add to the beginning to the random number generated
	 * @return a unique id string starting with the pre
	 * @throws SparqlException if sparql query fails to execute
	 */
	protected String generateUniqueURI (String pre) throws SparqlException {
		return generateUniqueURI(pre, (String[])null);
	}
	
	/**
	 * generate a random number and check it against existing ids in public graph (DEFAULT) and all the graphs provided if any
	 * 
	 * @param pre prefix to add to the beginning to the random number generated
	 * @param graph graphs to search against, public graph is searched by default
	 * @return a unique id string starting with the pre
	 * @throws SparqlException if sparql query fails to execute
	 */
	protected String generateUniqueURI (String pre, String... graph) throws SparqlException {
		// check the repository to see if the generated URI is unique
		boolean unique = false;
		String newURI = null;
		do {
			newURI = pre + (1000000 + random.nextInt(9999999));
			StringBuffer queryBuf = new StringBuffer();
			queryBuf.append (prefix + "\n");
			queryBuf.append ("SELECT DISTINCT ?o\n");
			queryBuf.append("FROM <" + DEFAULT_GRAPH + ">\n");
			if (graph != null) {
				for (int i=0; i < graph.length; i++) {
					queryBuf.append ("FROM <" + graph[i] + ">\n");
				}
			}
			queryBuf.append ("WHERE {\n" + 
					"<"+ newURI + "> ?p ?o .\n" + 
					"				}\n" + 
					"				LIMIT 1");
			List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
			unique = results.size() == 0;
		} while (!unique);
		
		return newURI;
	}
	
	/**
	 * 
	 * @param graph graph for the user
	 * @param type "Linker" or "Glycan" (rdf type of the subject)
	 * @return total number of triples with that rdf:type as the subject and date_addedToLibrary as the predicate
	 * @throws SparqlException
	 */
	protected int getCountByUserByType (String graph, String type) throws SparqlException {
		int total = 0;
		if (graph != null) {
			StringBuffer queryBuf = new StringBuffer();
			queryBuf.append (prefix + "\n");
			queryBuf.append ("SELECT COUNT(DISTINCT ?s) as ?count \n");
			queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
			queryBuf.append ("FROM <" + graph + ">\n");
			queryBuf.append ("WHERE {\n");
			queryBuf.append (" ?s gadr:has_date_addedtolibrary ?d . \n");
			queryBuf.append (" ?s rdf:type  <http://purl.org/gadr/data#" + type +">. }");
			List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
			for (SparqlEntity sparqlEntity : results) {
				String count = sparqlEntity.getValue("count");
				try {
					total = Integer.parseInt(count);
					break;
				} catch (NumberFormatException e) {
					throw new SparqlException("Count query returned invalid result", e);
				}
				
			}
		}
		return total;
	}

	/**
     * {@inheritDoc}
     */
	@Override
	public String getGraphForUser (UserEntity user) throws SQLException {
		PrivateGraphEntity graph = graphRepository.findByUser(user);
		if (graph != null) 
			return graph.getGraphIRI();
		else { // try to create for the first time 
			return addPrivateGraphForUser(user);
		}
	}
	
	/**
	 * a generic delete method that removes all the triples for the given uri as the subject from the given graph
	 * 
	 * @param uri subject uri for the triples to remove
	 * @param graph graph to delete from
	 * @throws SparqlException 
	 */
	protected void deleteByURI(String uri, String graph) throws SparqlException {
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI subject = f.createIRI(uri);
		IRI graphIRI = f.createIRI(graph);
		RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(subject, null, null, graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(statements2), graphIRI);
	}

    @Override
    public void resetRepository() throws SQLException {
        sparqlDAO.deleteAll();   
    }
}
