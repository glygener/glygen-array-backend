package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Random;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.PrivateGraphEntity;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.PrivateGraphRepository;
import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.glygen.array.persistence.dao.UserRepository;
import org.grits.toolbox.glycanarray.library.om.feature.Glycan;
import org.grits.toolbox.glycanarray.library.om.layout.BlockLayout;
import org.grits.toolbox.glycanarray.library.om.layout.SlideLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(value="sesameTransactionManager") 
public class GlygenArrayRepositoryImpl implements GlygenArrayRepository {
	
	@Autowired
	SesameSparqlDAO sparqlDAO;
	
	@Autowired
	PrivateGraphRepository graphRepository;
	
	@Autowired
	UserRepository userRepository;
	
	Random random = new Random();
	
	String prefix="PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
			+ "\nPREFIX gadr: <http://purl.org/gadr/data#>"
			+ "\nPREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>";
	
	String uriPrefix = "http://glygen.org/glygenarray/";
	
	@Override
	public void addGlycan(Glycan g, UserEntity user, boolean isPrivate) throws SparqlException {
		
		String graph = DEFAULT_GRAPH;
		if (isPrivate && user == null) {
			// cannot add 
			throw new SparqlException ("The user must be provided to put data into private repository");
		}
		if (isPrivate) {
			try {
				graph = addPrivateGraphForUser(user);
			} catch (SQLException e) {
				throw new SparqlException ("Cannot add the private graph for the user: " + user.getUsername(), e);
			}
		}
		
		// add object property between Glycan and Sequence
		StringBuffer sparqlbuf = new StringBuffer();
		
		String seqURI = generateUniqueURI(uriPrefix + "Seq", isPrivate ? graph: null);
		String glycanURI = generateUniqueURI(uriPrefix + "G", isPrivate ? graph: null);
		sparqlbuf.append(prefix);
		sparqlbuf.append("INSERT ");
		sparqlbuf.append("{ GRAPH <" + graph + ">\n");
		sparqlbuf.append("{ " + "<" +  glycanURI + "> gadr:has_sequence <" + seqURI + "> ." + " }\n");
		sparqlbuf.append("}\n");
		sparqlDAO.insert(sparqlbuf.toString());
		
		// add label
		sparqlbuf = new StringBuffer();
		sparqlbuf.append(prefix);
		sparqlbuf.append("INSERT ");
		sparqlbuf.append("{ GRAPH <" + graph + ">\n");
		sparqlbuf.append("{ " + "<" +  glycanURI + "> rdfs:label \"" + g.getName() + "\" ." + " }\n");
		sparqlbuf.append("}\n");
		sparqlDAO.insert(sparqlbuf.toString());
		
		// add comment
		if (g.getComment() != null) {
			sparqlbuf = new StringBuffer();
			sparqlbuf.append(prefix);
			sparqlbuf.append("INSERT ");
			sparqlbuf.append("{ GRAPH <" + graph + ">\n");
			sparqlbuf.append("{ " + "<" +  glycanURI + "> rdfs:comment \"" + g.getComment() + "\" ." + " }\n");
			sparqlbuf.append("}\n");
			sparqlDAO.insert(sparqlbuf.toString());
		}
		
		// add a dataproperty for Glycan -> glytoucanId
		if (g.getGlyTouCanId() != null) {
			sparqlbuf = new StringBuffer();
			sparqlbuf.append(prefix);
			sparqlbuf.append("INSERT ");
			sparqlbuf.append("{ GRAPH <" + graph + ">\n");
			sparqlbuf.append("{ " + "<" +  glycanURI + "> gadr:has_glytoucan_id \"" + g.getGlyTouCanId() + "\" ." + " }\n");
			sparqlbuf.append("}\n");
			sparqlDAO.insert(sparqlbuf.toString());
		}
		
		if (g.getSequence() != null) {
			// add a dataproperty for Sequence object -> sequence string
			sparqlbuf = new StringBuffer();
			sparqlbuf.append(prefix);
			sparqlbuf.append("INSERT ");
			sparqlbuf.append("{ GRAPH <" + graph + ">\n");
			sparqlbuf.append("{ " + "<" +  seqURI + "> gadr:has_sequence_value \"" + g.getSequence() + "\" ." + " }\n");
			sparqlbuf.append("}\n");
			sparqlDAO.insert(sparqlbuf.toString());
		}
		if (g.getSequenceType() != null) {
			// add a dataproperty for Sequence object -> sequence format
			sparqlbuf = new StringBuffer();
			sparqlbuf.append(prefix);
			sparqlbuf.append("INSERT ");
			sparqlbuf.append("{ GRAPH <" + graph + ">\n");
			sparqlbuf.append("{ " + "<" +  seqURI + "> gadr:has_sequence_format \"" + g.getSequenceType() + "\" ." + " }\n");
			sparqlbuf.append("}\n");
			sparqlDAO.insert(sparqlbuf.toString());
		}
	}

	@Override
	public void addGlycan(Glycan g) throws SparqlException {
		addGlycan (g, null, false);
	}


	@Override
	public String addPrivateGraphForUser (UserEntity uEntity) throws SQLException {
		String URI = sparqlDAO.addGraph(PRIVATE_GRAPH, uEntity.getUsername());
		PrivateGraphEntity graph = new PrivateGraphEntity();
		graph.setUser(uEntity);
		graph.setGraphIRI(URI);
		graphRepository.save (graph);
		return URI;
	}
	
	private String generateUniqueURI (String pre, String privateGraph) throws SparqlException {
		// check the repository to see if the generated URI is unique
		boolean unique = false;
		String newURI = null;
		do {
			newURI = pre + (1000000 + random.nextInt(9999999));
			StringBuffer queryBuf = new StringBuffer();
			queryBuf.append (prefix + "\n");
			queryBuf.append ("SELECT DISTINCT ?s\n");
			queryBuf.append ("FROM " + DEFAULT_GRAPH);
			queryBuf.append ("WHERE {\n" + 
					"				    ?s ?p ?o .\n" + 
					"				  FILTER (?s = '" + pre + "'))\n" + 
					"				}\n" + 
					"				LIMIT 10");
			List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
			unique = results.size() == 0;
			
			if (unique && privateGraph != null) {   // check the private graph as well
				queryBuf = new StringBuffer();
				queryBuf.append (prefix + "\n");
				queryBuf.append ("SELECT DISTINCT ?s\n");
				queryBuf.append ("FROM " + privateGraph);
				queryBuf.append ("WHERE {\n" + 
						"				    ?s ?p ?o .\n" + 
						"				  FILTER (?s = '" + pre + "'))\n" + 
						"				}\n" + 
						"				LIMIT 10");
				results = sparqlDAO.query(queryBuf.toString());
				unique = results.size() == 0;
			}
		} while (!unique);
		
		return newURI;
	}
	
	@Override
	public SlideLayout findSlideLayoutByName(String name) throws SparqlException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public SlideLayout findSlideLayoutByName(String name, String username) throws SparqlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BlockLayout findBlockLayoutByName(String name, String username) throws SparqlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BlockLayout findBlockLayoutByName(String name) throws SparqlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<SlideLayout> findSlideLayoutByUser( String username) throws SparqlException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void addSlideLayout(SlideLayout s, String username, boolean isPrivate) throws SparqlException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void addSlideLayout(SlideLayout s, String username) throws SparqlException {
		// TODO Auto-generated method stub

	}
}
