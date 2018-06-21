package org.glygen.array.persistence.dao;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.glycoinfo.rdf.DeleteSparql;
import org.glycoinfo.rdf.InsertSparql;
import org.glycoinfo.rdf.SparqlBean;
import org.glycoinfo.rdf.SparqlException;
import org.glycoinfo.rdf.dao.SparqlDAO;
import org.glycoinfo.rdf.dao.SparqlEntity;
import org.glycoinfo.rdf.dao.virt.VirtSesameConnectionFactory;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ch.qos.logback.classic.Logger;

@Repository
public class SesameSparqlDAO implements SparqlDAO {
	
	Logger logger = (Logger) LoggerFactory.getLogger(SesameSparqlDAO.class);
	
	@Autowired(required=false)
  	protected VirtSesameConnectionFactory sesameConnectionFactory;
	
	@Override
	public void archive(SparqlBean arg0) throws SparqlException {
	}

	@Override
	@Transactional(value="sesameTransactionManager")
	public void delete(SparqlBean deletesparql) throws SparqlException {
		if (!(deletesparql instanceof DeleteSparql))
			throw new SparqlException("expected delete SPARQL");
		DeleteSparql delete = (DeleteSparql)deletesparql;		
		RepositoryConnection connection = sesameConnectionFactory.getConnection();

		String format = delete.getFormat();
		String statement = delete.getSparql();
		logger.debug("format:>"+format);
		logger.debug(statement);
		
		try {
			if (format.equals(InsertSparql.SPARQL)) {
				Update update;
				update = connection.prepareUpdate(QueryLanguage.SPARQL, statement);
				update.execute();
				BindingSet bindings = update.getBindings();
				bindings.iterator();
				for (Binding binding : bindings) {
					logger.debug("binding Name:>" + binding.getName());
					logger.debug("binding Value:>" + binding.getValue());
				}
			} else if (format.equals(InsertSparql.Turtle)) {
				StringReader reader = new StringReader(statement);
				ValueFactory f = connection.getValueFactory();
				Resource res = f.createURI(delete.getGraph());
				connection.add(reader, "", RDFFormat.TURTLE, res);
			}
		} catch (RepositoryException | MalformedQueryException | UpdateExecutionException | RDFParseException | IOException e) {
			throw new SparqlException(e);
		}
	}
	
	@Transactional(value="sesameTransactionManager")
	public void delete(String delete) throws SparqlException {
		update (delete);
	}

	@Override
	@Transactional(value="sesameTransactionManager")
	public void execute(SparqlBean insert) throws SparqlException {
		String statement = insert.getSparql();
		String format = InsertSparql.SPARQL;
		String graph = null;
		if (insert instanceof InsertSparql) {
			InsertSparql thisInsert = (InsertSparql)insert;
			format = thisInsert.getFormat();
			graph = thisInsert.getGraph();
		}
		RepositoryConnection connection = sesameConnectionFactory.getConnection();
		logger.debug("format:>"+format);
		logger.debug(statement);
		
		try {
			if (format.equals(InsertSparql.SPARQL)) {
				Update update;
				update = connection.prepareUpdate(QueryLanguage.SPARQL, statement);
				update.execute();
				BindingSet bindings = update.getBindings();
				bindings.iterator();
				for (Binding binding : bindings) {
					logger.debug("binding Name:>" + binding.getName());
					logger.debug("binding Value:>" + binding.getValue());
				}
			} else if (format.equals(InsertSparql.Turtle)) {
				StringReader reader = new StringReader(statement);
				ValueFactory f = connection.getValueFactory();
				Resource res = f.createURI(graph);
				connection.add(reader, "", RDFFormat.TURTLE, res);
			}
		} catch (RepositoryException | MalformedQueryException | UpdateExecutionException | RDFParseException | IOException e) {
			throw new SparqlException(e);
		}
	}

	@Override
	@Transactional(value="sesameTransactionManager")
	public void insert(SparqlBean insert) throws SparqlException {
		execute(insert);
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

	@Override
	public int load(String arg0) throws SparqlException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	@Transactional(value="sesameTransactionManager")
	public List<SparqlEntity> query(SparqlBean select) throws SparqlException {
		return query(select.getSparql());
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
}
