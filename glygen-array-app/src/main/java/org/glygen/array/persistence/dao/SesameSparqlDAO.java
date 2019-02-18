package org.glygen.array.persistence.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.virtuoso.SesameConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
public class SesameSparqlDAO {
	
	final static Logger logger = LoggerFactory.getLogger("event-logger");
	
	@Autowired(required=false)
  	protected SesameConnectionFactory sesameConnectionFactory;
	
	
	@Transactional(value="sesameTransactionManager")
	public void delete(String delete) throws SparqlException {
		update (delete);
	}
	
	/**
	 * execute sparql update statement
	 * @param statement
	 * @throws SparqlException
	 */
	private void update (String statement) throws SparqlException {
		RepositoryConnection connection = sesameConnectionFactory.getConnection();
		try {
			Update update;
			update = connection.prepareUpdate(QueryLanguage.SPARQL, statement);
			update.execute();
			BindingSet bindings = update.getBindings();
			bindings.iterator();
			for (Binding binding : bindings) {
				logger.debug("binding Name:>" + binding.getName());
				logger.debug("binding Value:>" + binding.getValue());
			}
			
		} catch (RepositoryException | MalformedQueryException | UpdateExecutionException e) {
			throw new SparqlException(e);
		}
	}
	
	/**
	 * execute sparql insert statement
	 * 
	 * @param insert insert statement
	 * @throws SparqlException
	 */
	@Transactional(value="sesameTransactionManager")
	public void insert (String insert) throws SparqlException {
		update (insert);
	}

	@Transactional(value="sesameTransactionManager")
	public List<SparqlEntity> query(String query) throws SparqlException {
		List<SparqlEntity> al = null;
 		RepositoryConnection con = sesameConnectionFactory.getConnection();
 		try {
 			logger.debug("query:>" + query);
 			try {
 				al = doTupleQuery(con, query);
 			} catch (MalformedQueryException | QueryEvaluationException e) {
 				logger.debug("Error[" + e + "]", e);
 				throw new SparqlException(e);
 			}
 		} catch (RepositoryException e) {
 			logger.debug("Error[" + e + "]", e);
 			throw new SparqlException(e);
 		}
 		return al;
	}

	private List<SparqlEntity> doTupleQuery(RepositoryConnection con, String query) throws MalformedQueryException, QueryEvaluationException, RepositoryException {
		TupleQuery resultsTable = con.prepareTupleQuery(QueryLanguage.SPARQL,
				query);
		TupleQueryResult bindings = resultsTable.evaluate();

		ArrayList<SparqlEntity> results = new ArrayList<SparqlEntity>();
		for (int row = 0; bindings.hasNext(); row++) {
			logger.debug("RESULT " + (row + 1) + ": ");
			BindingSet pairs = bindings.next();
			List<String> names = bindings.getBindingNames();
			Value[] rv = new Value[names.size()];

			SparqlEntity values = new SparqlEntity();
			for (int i = 0; i < names.size(); i++) {
				String name = names.get(i);
				Value value = pairs.getValue(name);
				Binding bind = pairs.getBinding(name);

				rv[i] = value;
				// if(column > 0) System.out.print(", ");
				logger.debug("\t" + name + "=" + value);
				// vars.add(value);
				// if(column + 1 == names.size()) logger.debug(";");
				String stringvalue = null;
				if (null == value)
					stringvalue = "";
				else
					stringvalue = value.stringValue();

				values.setValue(name, stringvalue);
			}
			results.add(values);
		}
		return results;
	}
	
	public String addGraph (String graphGroup, String username) throws SQLException {
		if (sesameConnectionFactory != null  && sesameConnectionFactory.getSqlConnection()!= null) {
			String graphName = graphGroup + "/" + username;
			Connection connection = sesameConnectionFactory.getSqlConnection();
			CallableStatement statement = connection.prepareCall("RDF_GRAPH_GROUP_INS (?,?)");
			statement.setString(1, graphGroup);
			statement.setString(2, graphName);
			statement.execute();
			return graphName;
			
		}
		else {
			throw new SQLException ("No sql connection is active!");
		}
	}
}
