package org.glygen.array.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.persistence.EntityNotFoundException;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.PrivateGraphEntity;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.PrivateGraphRepository;
import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.Owner;
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
	
	String prefix="PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>"
			+ "\nPREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
			+ "\nPREFIX gadr: <http://purl.org/gadr/data#>"
			+ "\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>";
	
	String uriPrefix = "http://glygen.org/glygenarray/";
	String ontPrefix = "http://purl.org/gadr/data#";
	
	@Override
	public String addGlycan(Glycan g, UserEntity user, boolean isPrivate) throws SparqlException {
		
		String graph = DEFAULT_GRAPH;
		if (isPrivate && user == null) {
			// cannot add 
			throw new SparqlException ("The user must be provided to put data into private repository");
		}
		if (isPrivate) {
			try {
				// check if there is already a private graph for user
				graph = getGraphForUser(user);
				if (graph == null)
					graph = addPrivateGraphForUser(user);
			} catch (SQLException e) {
				throw new SparqlException ("Cannot add the private graph for the user: " + user.getUsername(), e);
			}
		}
		
		String seqURI = generateUniqueURI(uriPrefix + "Seq", isPrivate ? graph: null);
		String glycanURI = generateUniqueURI(uriPrefix + "G", isPrivate ? graph: null);
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI sequence = f.createIRI(seqURI);
		IRI glycan = f.createIRI(glycanURI);
		IRI graphIRI = f.createIRI(graph);
		
		Owner existing = findOwnerInGraph (user.getUserId(), graph);
		IRI owner = existing == null ? 
				f.createIRI(generateUniqueURI(uriPrefix + "U", isPrivate ? graph: null)) : f.createIRI(existing.getUri());
		Literal glycanLabel = f.createLiteral(g.getName());
		Literal glycanComment = g.getComment() == null ? f.createLiteral("") : f.createLiteral(g.getComment());
		Literal glytoucanId = g.getGlyTouCanId() == null ? f.createLiteral("") : f.createLiteral(g.getGlyTouCanId());
		Literal internalId = g.getInternalId() == null ? f.createLiteral("") : f.createLiteral(g.getInternalId());
		Literal sequenceValue = f.createLiteral(g.getSequence());
		Literal format = f.createLiteral(g.getSequenceType());
		Literal userId = f.createLiteral(user.getUserId());
		Literal username = f.createLiteral(user.getUsername());
		Literal institution = f.createLiteral(user.getAffiliation());
		IRI hasSequence = f.createIRI(ontPrefix + "has_sequence");
		IRI hasGlytoucanId = f.createIRI(ontPrefix + "has_glytoucan_id");
		IRI hasSequenceValue = f.createIRI(ontPrefix + "has_sequence_value");
		IRI hasSequenceFormat = f.createIRI(ontPrefix + "has_sequence_format");
		IRI hasInternalId = f.createIRI(ontPrefix + "has_internal_id");
		IRI sequenceType = f.createIRI(ontPrefix + "Sequence");
		IRI glycanType = f.createIRI(ontPrefix + "Glycan");
		IRI ownerType = f.createIRI(ontPrefix + "Owner");
		IRI createdBy = f.createIRI(ontPrefix + "created_by");
		IRI hasUserId = f.createIRI(ontPrefix + "has_user_id");
		IRI hasUserName = f.createIRI(ontPrefix + "has_username");
		IRI hasInstitution = f.createIRI(ontPrefix + "has_institution_name");
		
		List<Statement> statements = new ArrayList<Statement>();
		statements.add(f.createStatement(sequence, RDF.TYPE, sequenceType));
		statements.add(f.createStatement(glycan, RDF.TYPE, glycanType));
		statements.add(f.createStatement(glycan, RDFS.LABEL, glycanLabel));
		statements.add(f.createStatement(glycan, RDFS.COMMENT, glycanComment));
		statements.add(f.createStatement(glycan, hasSequence, sequence));
		statements.add(f.createStatement(glycan, hasGlytoucanId, glytoucanId));
		statements.add(f.createStatement(glycan, hasInternalId, internalId));
		statements.add(f.createStatement(sequence, hasSequenceValue, sequenceValue));
		statements.add(f.createStatement(sequence, hasSequenceFormat, format));
		statements.add(f.createStatement(glycan, createdBy, owner));
		
		if (existing == null) {
			statements.add(f.createStatement(owner, RDF.TYPE, ownerType));
			statements.add(f.createStatement(owner, hasUserId, userId));
			statements.add(f.createStatement(owner, hasUserName, username));
			statements.add(f.createStatement(owner, hasInstitution, institution));
		}
		
		sparqlDAO.addStatements(statements, graphIRI);
		
		return glycanURI;
	}

	@Override
	public String addGlycan(Glycan g, UserEntity user) throws SparqlException {
		return addGlycan (g, user, false);
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
	
	@Override
	public String getGraphForUser (UserEntity user) throws SQLException {
		PrivateGraphEntity graph = graphRepository.findByUser(user);
		if (graph != null) 
			return graph.getGraphIRI();
		return null;
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
			queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
			queryBuf.append ("WHERE {\n" + 
					"				    ?s ?p ?o .\n" + 
					"				  FILTER (?s = '" + pre + "')\n" + 
					"				}\n" + 
					"				LIMIT 10");
			List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
			unique = results.size() == 0;
			
			if (unique && privateGraph != null) {   // check the private graph as well
				queryBuf = new StringBuffer();
				queryBuf.append (prefix + "\n");
				queryBuf.append ("SELECT DISTINCT ?s\n");
				queryBuf.append ("FROM <" + privateGraph + ">\n");
				queryBuf.append ("WHERE {\n" + 
						"				    ?s ?p ?o .\n" + 
						"				  FILTER (?s = '" + pre + "')\n" + 
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
	public void addSlideLayout(SlideLayout s, UserEntity user, boolean isPrivate) throws SparqlException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void addSlideLayout(SlideLayout s, UserEntity user) throws SparqlException {
		// TODO Auto-generated method stub

	}

	@Override
	public Glycan getGlycan(String glytoucanId) throws SparqlException {
		return findGlycanInGraph(glytoucanId, DEFAULT_GRAPH);
	}
	
	private Glycan findGlycanInGraph (String glytoucanId, String graph) throws SparqlException {
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?s\n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n" + 
				"				    ?s gadr:has_glytoucan_id \"" + glytoucanId + "\"^^xsd:string .\n" + 
				"				}\n" + 
				"				LIMIT 10");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.size() == 0) 
			return null;
		
		SparqlEntity result = results.get(0);
		String glycanURI = result.getValue("s");
		
		Glycan glycanObject = getGlycanFromURI(glycanURI);
		glycanObject.setGlyTouCanId(glytoucanId);
				
		return glycanObject;
	}
	
	private Owner findOwnerInGraph (Long userId, String graph) throws SparqlException {
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?s\n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n" + 
				"				    ?s gadr:has_user_id \"" + userId + "\"^^xsd:long .\n" + 
				"				}\n" + 
				"				LIMIT 10");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.size() == 0) 
			return null;
		
		SparqlEntity result = results.get(0);
		String ownerUri = result.getValue("s");
		
		Owner owner = getOwnerFromURI(ownerUri);
				
		return owner;
	}

	private Owner getOwnerFromURI(String ownerUri) {
		Owner ownerObject = null;
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI owner = f.createIRI(ownerUri);
		
		IRI hasUserId = f.createIRI(ontPrefix + "has_user_id");
		IRI hasUserName = f.createIRI(ontPrefix + "has_username");
		IRI hasInstitution = f.createIRI(ontPrefix + "has_institution_name");
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(owner, null, null);
		if (statements.hasNext()) {
			ownerObject = new Owner();
			ownerObject.setUri(ownerUri);
		}
		while (statements.hasNext()) {
			Statement st = statements.next();
			if (st.getPredicate().equals(hasUserId)) {
				Value id = st.getObject();
				ownerObject.setUserId(Long.parseLong(id.stringValue()));
			} else if (st.getPredicate().equals(hasUserName)) {
				Value name = st.getObject();
				ownerObject.setName(name.stringValue());
			} else if (st.getPredicate().equals(hasInstitution)) {
				Value inst = st.getObject();
				ownerObject.setInstitution(inst.stringValue()); 
			} 
		}
		
		return ownerObject;
	}

	@Override
	public Glycan getGlycanFromURI (String glycanURI) {
		Glycan glycanObject = null;
		
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI glycan = f.createIRI(glycanURI);
		
		IRI hasSequence = f.createIRI(ontPrefix + "has_sequence");
		IRI hasGlytoucanId = f.createIRI(ontPrefix + "has_glytoucan_id");
		IRI hasInternalId = f.createIRI(ontPrefix + "has_internal_id");
		IRI hasSequenceValue = f.createIRI(ontPrefix + "has_sequence_value");
		IRI hasSequenceFormat = f.createIRI(ontPrefix + "has_sequence_format");
		//IRI sequenceType = f.createIRI(ontPrefix + "Sequence");
		//IRI glycanType = f.createIRI(ontPrefix + "Glycan");
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(glycan, null, null);
		if (statements.hasNext()) {
			glycanObject = new Glycan();
			glycanObject.setUri(glycanURI);
		}
		while (statements.hasNext()) {
			Statement st = statements.next();
			if (st.getPredicate().equals(RDFS.LABEL)) {
				Value label = st.getObject();
				glycanObject.setName(label.stringValue());
			} else if (st.getPredicate().equals(RDFS.COMMENT)) {
				Value comment = st.getObject();
				glycanObject.setComment(comment.stringValue());
			} else if (st.getPredicate().equals(hasGlytoucanId)) {
				Value glytoucanId = st.getObject();
				glycanObject.setGlyTouCanId(glytoucanId.stringValue()); 
			} else if (st.getPredicate().equals(hasInternalId)) {
				Value internalId = st.getObject();
				glycanObject.setInternalId(internalId.stringValue());
			} else if (st.getPredicate().equals(hasSequence)) {
				Value sequence = st.getObject();
				String sequenceURI = sequence.stringValue();
				IRI seq = f.createIRI(sequenceURI);
				RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(seq, null, null);
				while (statements2.hasNext()) {
					Statement st2 = statements2.next();
					if (st2.getPredicate().equals(hasSequenceValue)) {
						Value seqString = st2.getObject();
						glycanObject.setSequence(seqString.stringValue());
					} else if (st2.getPredicate().equals(hasSequenceFormat)) {
						Value formatString = st2.getObject();
						glycanObject.setSequenceType(formatString.stringValue());
					}
				}
			}
		}
		
		return glycanObject;
	}
	@Override
	public Glycan getGlycan(String glytoucanId, UserEntity user, boolean isPrivate) throws SparqlException {
		String graph = DEFAULT_GRAPH;
		if (isPrivate) {
			try {
				// check if there is already a private graph for user
				graph = getGraphForUser(user);
				if (graph == null)
					graph = addPrivateGraphForUser(user);
			} catch (SQLException e) {
				throw new SparqlException ("Cannot add the private graph for the user: " + user.getUsername(), e);
			}
		}
		return findGlycanInGraph(glytoucanId, graph);
	}

	@Override
	public void addBlockLayout(BlockLayout b, UserEntity user, boolean isPrivate) throws SparqlException {
		String graph = DEFAULT_GRAPH;
		if (isPrivate && user == null) {
			// cannot add 
			throw new SparqlException ("The user must be provided to put data into private repository");
		}
		if (isPrivate) {
			try {
				// check if there is already a private graph for user
				graph = getGraphForUser(user);
				if (graph == null)
					graph = addPrivateGraphForUser(user);
			} catch (SQLException e) {
				throw new SparqlException ("Cannot add the private graph for the user: " + user.getUsername(), e);
			}
		}
		
		String blockLayoutURI = generateUniqueURI(uriPrefix + "BL", isPrivate ? graph: null);
		String blockURI = generateUniqueURI(uriPrefix + "B", isPrivate ? graph: null);
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI blockLayout = f.createIRI(blockLayoutURI);
		IRI block = f.createIRI(blockURI);
		IRI graphIRI = f.createIRI(graph);
		
	/*	Literal blockLayoutLabel = f.createLiteral(b.getName());
		Literal blockLayoutComment = f.createLiteral(b.getComment());

		Literal glytoucanId = f.createLiteral(g.getGlyTouCanId());
		Literal sequenceValue = f.createLiteral(g.getSequence());
		Literal format = f.createLiteral(g.getSequenceType());
		IRI hasSequence = f.createIRI(ontPrefix + "has_sequence");
		IRI hasGlytoucanId = f.createIRI(ontPrefix + "has_glytoucan_id");
		IRI hasSequenceValue = f.createIRI(ontPrefix + "has_sequence_value");
		IRI hasSequenceFormat = f.createIRI(ontPrefix + "has_sequence_format");
		IRI sequenceType = f.createIRI(ontPrefix + "Sequence");
		IRI glycanType = f.createIRI(ontPrefix + "Glycan");
		
		List<Statement> statements = new ArrayList<Statement>();
		statements.add(f.createStatement(sequence, RDF.TYPE, sequenceType));
		statements.add(f.createStatement(glycan, RDF.TYPE, glycanType));
		statements.add(f.createStatement(glycan, RDFS.LABEL, glycanLabel));
		statements.add(f.createStatement(glycan, RDFS.COMMENT, glycanComment));
		statements.add(f.createStatement(glycan, hasSequence, sequence));
		statements.add(f.createStatement(glycan, hasGlytoucanId, glytoucanId));
		statements.add(f.createStatement(sequence, hasSequenceValue, sequenceValue));
		statements.add(f.createStatement(sequence, hasSequenceFormat, format));
		
		sparqlDAO.addStatements(statements, graphIRI);*/
		
		
	}

	@Override
	public void addBlockLayout(BlockLayout b, UserEntity user) throws SparqlException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Glycan getGlycanBySequence(String sequence) throws SparqlException {
		return findGlycanInGraphBySequence(sequence, DEFAULT_GRAPH);
	}

	@Override
	public Glycan getGlycanBySequence(String sequence, UserEntity user, boolean isPrivate) throws SparqlException {
		String graph = DEFAULT_GRAPH;
		if (isPrivate) {
			try {
				// check if there is already a private graph for user
				graph = getGraphForUser(user);
				if (graph == null)
					graph = addPrivateGraphForUser(user);
			} catch (SQLException e) {
				throw new SparqlException ("Cannot add the private graph for the user: " + user.getUsername(), e);
			}
		}
		return findGlycanInGraphBySequence(sequence, graph);
	}
	
	private Glycan findGlycanInGraphBySequence (String sequence, String graph) throws SparqlException {
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?s ?o\n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n" + 
				"				    ?s gadr:has_sequence ?o .\n" +
				"                    ?o gadr:has_sequence_value \"\"\"" + sequence + "\"\"\"^^xsd:string .\n" + 
				"				}\n" + 
				"				LIMIT 10");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.size() == 0) 
			return null;
		
		Glycan glycanObject = new Glycan();
		
		SparqlEntity result = results.get(0);
		String glycanURI = result.getValue("s");
		String sequenceURI = result.getValue("o");
		
		glycanObject.setUri(glycanURI);
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI glycan = f.createIRI(glycanURI);
		IRI seq = f.createIRI(sequenceURI);
		IRI hasInternalId = f.createIRI(ontPrefix + "has_internal_id");
		IRI hasGlytoucanId = f.createIRI(ontPrefix + "has_glytoucan_id");
		IRI hasSequenceValue = f.createIRI(ontPrefix + "has_sequence_value");
		IRI hasSequenceFormat = f.createIRI(ontPrefix + "has_sequence_format");
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(glycan, null, null);
		while (statements.hasNext()) {
			Statement st = statements.next();
			if (st.getPredicate().equals(RDFS.LABEL)) {
				Value label = st.getObject();
				glycanObject.setName(label.stringValue());
			} else if (st.getPredicate().equals(RDFS.COMMENT)) {
				Value comment = st.getObject();
				glycanObject.setComment(comment.stringValue());
			} else if (st.getPredicate().equals(hasGlytoucanId)) {
				Value glytoucanId = st.getObject();
				glycanObject.setGlyTouCanId(glytoucanId.stringValue());
			} else if (st.getPredicate().equals(hasInternalId)) {
				Value internalId = st.getObject();
				glycanObject.setInternalId(internalId.stringValue());
			} 
			
		}
		
		RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(seq, null, null);
		while (statements2.hasNext()) {
			Statement st2 = statements2.next();
			if (st2.getPredicate().equals(hasSequenceValue)) {
				Value seqString = st2.getObject();
				glycanObject.setSequence(seqString.stringValue());
			} else if (st2.getPredicate().equals(hasSequenceFormat)) {
				Value formatString = st2.getObject();
				glycanObject.setSequenceType(formatString.stringValue());
			}
		}
		
		return glycanObject;
	}

	@Override
	public List<Glycan> getGlycanByUser(UserEntity user) throws SparqlException {
		return getGlycanByUser(user, 0, -1);  // no limit
	}
	
	@Override
	public List<Glycan> getGlycanByUser(UserEntity user, int offset, int limit) throws SparqlException {
		List<Glycan> glycans = new ArrayList<Glycan>();
		
		// first check the DEFAULT_GRAPH
		glycans.addAll(getGlycanByUserInGraph(user, DEFAULT_GRAPH, offset, limit));
		// then add from the user's private graph, if any
		String graph;
		try {
			graph = getGraphForUser(user);
			if (graph != null) {
				glycans.addAll(getGlycanByUserInGraph(user, graph, offset, limit));
			}
		} catch (SQLException e) {
			throw new SparqlException("Cannot retrieve private graph for user", e);
		}
		
		return glycans;
	}
	
	private List<Glycan> getGlycanByUserInGraph(UserEntity user, String graph, int offset, int limit) throws SparqlException {
		List<Glycan> glycans = new ArrayList<Glycan>();
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?s ?label\n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n" + 
				"				    ?s gadr:created_by ?o .\n" +
				"                   ?s rdfs:label ?label . \n" +
				"                    ?o gadr:has_user_id \"" + user.getUserId() + "\"^^xsd:long .\n" + 
				"				}\n" +
				" ORDER BY (LCASE(?label))" + 
				((limit == -1) ? " " : " LIMIT " + limit) +
				" OFFSET " + offset);
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		for (SparqlEntity sparqlEntity : results) {
			Glycan glycan = getGlycanFromURI(sparqlEntity.getValue("s"));
			glycans.add(glycan);
		}
		
		return glycans;
	}

	@Override
	public void deleteGlycan(String glycanId, UserEntity user) throws SparqlException {
		String graph;
		
		try {
			graph = getGraphForUser(user);
			if (graph != null) {
				// check to see if the given glycanId is in this graph
				Glycan existing = getGlycanFromURI (uriPrefix + glycanId);
				if (existing != null) {
					deleteGlycanByURI (uriPrefix + glycanId, graph);
					return;
				}
			}
		} catch (SQLException e) {
			// nothing to do
		}
		// check if the glycan is owned by the user
		Glycan existing = getGlycanFromURI (uriPrefix + glycanId);
		if (existing != null) {
			// find the owner
			StringBuffer queryBuf = new StringBuffer();
			queryBuf.append (prefix + "\n");
			queryBuf.append ("SELECT DISTINCT ?o\n");
			queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
			queryBuf.append ("WHERE {\n <" + uriPrefix + glycanId + ">\n" + 
					" gadr:created_by ?o .\n" +
					"                    ?o gadr:has_user_id \"" + user.getUserId() + "\"^^xsd:long .\n" + 
					"				}\n" + 
					"				LIMIT 10");
			List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
			if (results.size() == 0) {
				// this user cannot update the given glycan
				throw new SparqlException("The user is not allowed to update this glycan");
			}
			else {
				deleteGlycanByURI (existing.getUri(), DEFAULT_GRAPH);
			}
		}
	}
	
	

	private void deleteGlycanByURI(String uri, String graph) throws SparqlException {
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI glycan = f.createIRI(uri);
		IRI graphIRI = f.createIRI(graph);
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(glycan, null, null);
		sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
	}

	@Override
	public void updateGlycan(Glycan g, UserEntity user) throws SparqlException {
		String graph = DEFAULT_GRAPH;
		Glycan existing = null;
		try {
			graph = getGraphForUser(user);
			if (graph != null) {
				// check to see if the given glycanId is in this graph
				existing = getGlycanFromURI(g.getUri());
				if (existing != null) {
					updateGlycanInGraph(g, graph);
					return;
				}
			}
		} catch (SQLException e) {
			// nothing to do, continue checking the default-graph
		}
		
		existing = getGlycanFromURI (g.getUri());
		if (existing == null)
			throw new EntityNotFoundException("Glycan " + g.getUri() + " does not exist! Cannot modify!");
		updateGlycanInGraph(g, DEFAULT_GRAPH);
	}
	
	void updateGlycanInGraph (Glycan g, String graph) throws SparqlException {
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI graphIRI = f.createIRI(graph);
		String glycanURI = g.getUri();
		IRI glycan = f.createIRI(glycanURI);
		Literal glycanLabel = f.createLiteral(g.getName());
		Literal glycanComment = g.getComment() == null ? f.createLiteral("") : f.createLiteral(g.getComment());
		Literal internalId = g.getInternalId() == null? f.createLiteral("") : f.createLiteral(g.getInternalId());
		
		IRI hasInternalId = f.createIRI(ontPrefix + "has_internal_id");
		
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(glycan, RDFS.LABEL, null)), graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(glycan, RDFS.COMMENT, null)), graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(glycan, hasInternalId, null)), graphIRI);
		
		List<Statement> statements = new ArrayList<Statement>();
		
		statements.add(f.createStatement(glycan, RDFS.LABEL, glycanLabel));
		statements.add(f.createStatement(glycan, RDFS.COMMENT, glycanComment));
		statements.add(f.createStatement(glycan, hasInternalId, internalId));
		
		sparqlDAO.addStatements(statements, graphIRI);
	}
}
