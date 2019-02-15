package org.glygen.array.service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Random;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.GlygenUser;
import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.glygen.array.virtuoso.RepositoryConnectionFactory;
import org.glygen.array.virtuoso.SesameConnectionFactory;
import org.grits.toolbox.glycanarray.library.om.layout.BlockLayout;
import org.grits.toolbox.glycanarray.library.om.layout.SlideLayout;
import org.grits.toolbox.glycanarray.om.model.GlycanMoiety;
import org.springframework.beans.factory.annotation.Autowired;

public class GlygenArrayRepositoryImpl implements GlygenArrayRepository {
	
	@Autowired
	SesameSparqlDAO sparqlDAO;
	
	Random random = new Random();

	@Autowired(required=false)
  	protected SesameConnectionFactory sesameConnectionFactory;
	
	@Override
	public void addSlideLayout(SlideLayout s, GlygenUser u) throws SparqlException {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void addGlycan(GlycanMoiety g, GlygenUser u, boolean isPrivate) throws SparqlException {
		
		if (isPrivate && u == null) {
			// cannot add 
			throw new SparqlException ("The user must be provided to put data into private repository");
		}
		String privateGraph = null;
		if (isPrivate) {
			try {
				privateGraph = addPrivateGraphForUser(u);
			} catch (SQLException e) {
				throw new SparqlException ("Cannot add the private graph for the user: " + u.getUsername(), e);
			}
		}
		
		// add object property between Glycan and Sequence
		StringBuffer sparqlbuf = new StringBuffer();
		
		String seqURI = "http://glygen.org/glygenarray/" + generateUniqueURI("Seq");
		String glycanURI = generateUniqueURI("http://glygen.org/glygenarray/G");
		String prefix="PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
				+ "\nPREFIX glygenarray: <http://glygen.org/glygenarray#>";
		sparqlbuf.append(prefix);
		sparqlbuf.append("INSERT ");
		if (isPrivate)
			sparqlbuf.append("{ GRAPH <" + privateGraph + ">\n");
		else 
			sparqlbuf.append("{ GRAPH <" + DEFAULT_GRAPH + ">\n");
		sparqlbuf.append("{ " + "<" +  glycanURI + "> glygenarray:has_sequence <" + seqURI + "> ." + " }\n");
		sparqlbuf.append("}\n");
		sparqlDAO.insert(sparqlbuf.toString());
		
		// add a dataproperty for Sequence object - sequence string
		sparqlbuf = new StringBuffer();
		sparqlbuf.append(prefix);
		sparqlbuf.append("INSERT ");
		if (isPrivate)
			sparqlbuf.append("{ GRAPH <" + privateGraph + ">\n");
		else 
			sparqlbuf.append("{ GRAPH <" + DEFAULT_GRAPH + ">\n");
		sparqlbuf.append("{ " + "<" +  seqURI + "> glygenarray:has_sequence_value \"" + g.getSequence() + "\" ." + " }\n");
		sparqlbuf.append("}\n");
		sparqlDAO.insert(sparqlbuf.toString());
		
		
	}

	@Override
	public void addGlycan(GlycanMoiety g) throws SparqlException {
		addGlycan (g, null, false);
	}

	@Override
	public SlideLayout findSlideLayoutByName(String name) throws SparqlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BlockLayout findBlockLayoutByName(String name) throws SparqlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<SlideLayout> findSlideLayoutByUser(GlygenUser user) throws SparqlException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String addPrivateGraphForUser (GlygenUser user) throws SQLException {
		if (sesameConnectionFactory != null  && sesameConnectionFactory.getSqlConnection()!= null) {
			String graphName = PRIVATE_GRAPH + user.getUsername();
			Connection connection = sesameConnectionFactory.getSqlConnection();
			CallableStatement statement = connection.prepareCall("RDF_GRAPH_GROUP_INS (?,?)");
			statement.setString(1, PRIVATE_GRAPH);
			statement.setString(2, graphName);
			statement.execute();
			return graphName;
			
		}
		else {
			throw new SQLException ("No sql connection is active!");
		}
	}
	
	private String generateUniqueURI (String prefix) {
		return prefix + (1000000 + random.nextInt(9999999));
	}

	@Override
	public void addSlideLayout(SlideLayout s, GlygenUser u, boolean isPrivate) throws SparqlException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SlideLayout findSlideLayoutByName(String name, GlygenUser user) throws SparqlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BlockLayout findBlockLayoutByName(String name, GlygenUser user) throws SparqlException {
		// TODO Auto-generated method stub
		return null;
	}

	

}
