package org.glygen.array.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.xml.datatype.XMLGregorianCalendar;

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
import org.grits.toolbox.glycanarray.library.om.layout.BlockLayout;
import org.grits.toolbox.glycanarray.library.om.layout.SlideLayout;
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
	UserRepository userRepository;
	
	Random random = new Random();
	
	String prefix="PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>"
			+ "\nPREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
			+ "\nPREFIX gadr: <http://purl.org/gadr/data#>"
			+ "\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>";
	
	String uriPrefix = "http://glygen.org/glygenarray/";
	String ontPrefix = "http://purl.org/gadr/data#";
	
	@Override
	public String addGlycan(Glycan g, UserEntity user) throws SparqlException {
		String graph = null;
		if (user == null) {
			// cannot add 
			throw new SparqlException ("The user must be provided to put data into private repository");
		}
		
		if (g == null || g.getSequence() == null || g.getSequence().isEmpty() || g.getSequenceType() == null)
			// cannot add 
			throw new SparqlException ("Not enough information is provided to register a glycan");
		
		try {
			// check if there is already a private graph for user
			graph = getGraphForUser(user);
			if (graph == null)
				graph = addPrivateGraphForUser(user);
		} catch (SQLException e) {
			throw new SparqlException ("Cannot add the private graph for the user: " + user.getUsername(), e);
		}
		
		ValueFactory f = sparqlDAO.getValueFactory();
		String glycanURI;
		
		// check if the glycan already exists in "default-graph", then we only need to add it to the user's graph
		Glycan existing = getGlycanBySequence(g.getSequence());
		if (existing == null) {
			String seqURI = generateUniqueURI(uriPrefix + "Seq");
			glycanURI = generateUniqueURI(uriPrefix) + "GAR";
			
			IRI sequence = f.createIRI(seqURI);
			IRI glycan = f.createIRI(glycanURI);
			IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
			Literal glytoucanId = g.getGlyTouCanId() == null ? f.createLiteral("") : f.createLiteral(g.getGlyTouCanId());
			Literal sequenceValue = f.createLiteral(g.getSequence());
			Literal format = f.createLiteral(g.getSequenceType());
			Literal date = f.createLiteral(new Date());
			IRI hasSequence = f.createIRI(ontPrefix + "has_sequence");
			IRI hasGlytoucanId = f.createIRI(ontPrefix + "has_glytoucan_id");
			IRI hasSequenceValue = f.createIRI(ontPrefix + "has_sequence_value");
			IRI hasSequenceFormat = f.createIRI(ontPrefix + "has_sequence_format");
			IRI sequenceType = f.createIRI(ontPrefix + "Sequence");
			IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
			IRI glycanType = f.createIRI(ontPrefix + "Glycan");
			
			List<Statement> statements = new ArrayList<Statement>();
			statements.add(f.createStatement(sequence, RDF.TYPE, sequenceType, defaultGraphIRI));
			statements.add(f.createStatement(glycan, RDF.TYPE, glycanType, defaultGraphIRI));
			statements.add(f.createStatement(glycan, hasSequence, sequence, defaultGraphIRI));
			statements.add(f.createStatement(glycan, hasCreatedDate, date, defaultGraphIRI));
			statements.add(f.createStatement(glycan, hasGlytoucanId, glytoucanId, defaultGraphIRI));
			statements.add(f.createStatement(sequence, hasSequenceValue, sequenceValue, defaultGraphIRI));
			statements.add(f.createStatement(sequence, hasSequenceFormat, format, defaultGraphIRI));
			
			sparqlDAO.addStatements(statements, defaultGraphIRI);
		} else {
			logger.debug("The glycan already exists in global repository. URI: " + existing.getUri());
			glycanURI = existing.getUri();
		}
		
		// add glycan details to the user's private graph
		IRI graphIRI = f.createIRI(graph);
		IRI glycan = f.createIRI(glycanURI);
		Literal glycanLabel = g.getName() == null ? f.createLiteral("") : f.createLiteral(g.getName());
		Literal glycanComment = g.getComment() == null ? f.createLiteral("") : f.createLiteral(g.getComment());
		Literal internalId = g.getInternalId() == null ? f.createLiteral("") : f.createLiteral(g.getInternalId());
		IRI hasInternalId = f.createIRI(ontPrefix + "has_internal_id");
		IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		Literal date = f.createLiteral(new Date());
		
		List<Statement> statements = new ArrayList<Statement>();
		
		statements.add(f.createStatement(glycan, RDFS.LABEL, glycanLabel, graphIRI));
		statements.add(f.createStatement(glycan, hasInternalId, internalId, graphIRI));
		statements.add(f.createStatement(glycan, RDFS.COMMENT, glycanComment, graphIRI));
		statements.add(f.createStatement(glycan, hasAddedToLibrary, date, graphIRI));
		statements.add(f.createStatement(glycan, hasModifiedDate, date, graphIRI));
		
		sparqlDAO.addStatements(statements, graphIRI);
		
		return glycanURI;
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
	
	private String generateUniqueURI (String pre) throws SparqlException {
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
	public void addSlideLayout(SlideLayout s, UserEntity user) throws SparqlException {
		// TODO Auto-generated method stub

	}

	/*@Override
	public Glycan getGlycan(String glytoucanId) throws SparqlException, SQLException {
		return findGlycanInGraph(glytoucanId, DEFAULT_GRAPH);
	}
	
	private Glycan findGlycanInGraph (String glytoucanId, String graph) throws SparqlException, SQLException {
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?s\n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
		queryBuf.append ("WHERE {\n" + 
				"				    ?s gadr:has_glytoucan_id \"" + glytoucanId + "\"^^xsd:string .\n" + 
				"				}\n" + 
				"				LIMIT 10");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.size() == 0) 
			return null;
		
		SparqlEntity result = results.get(0);
		String glycanURI = result.getValue("s");
		
		Glycan glycanObject = getGlycanFromURI(glycanURI, null);
		glycanObject.setGlyTouCanId(glytoucanId);
				
		return glycanObject;
	}*/
	
	/*private Owner findOwnerInGraph (Long userId, String graph) throws SparqlException {
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
	}*/

	private Glycan getGlycanFromURI (String glycanURI, String graph) throws SparqlException {
		Glycan glycanObject = null;
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI glycan = f.createIRI(glycanURI);
		IRI graphIRI = f.createIRI(graph);
		IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
		IRI hasSequence = f.createIRI(ontPrefix + "has_sequence");
		IRI hasGlytoucanId = f.createIRI(ontPrefix + "has_glytoucan_id");
		IRI hasInternalId = f.createIRI(ontPrefix + "has_internal_id");
		IRI hasSequenceValue = f.createIRI(ontPrefix + "has_sequence_value");
		IRI hasSequenceFormat = f.createIRI(ontPrefix + "has_sequence_format");
		IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
		IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(glycan, null, null, defaultGraphIRI);
		if (statements.hasNext()) {
			glycanObject = new Glycan();
			glycanObject.setUri(glycanURI);
		}
		while (statements.hasNext()) {
			Statement st = statements.next();
			if (st.getPredicate().equals(hasGlytoucanId)) {
				Value glytoucanId = st.getObject();
				glycanObject.setGlyTouCanId(glytoucanId.stringValue()); 
			} else if (st.getPredicate().equals(hasSequence)) {
				Value sequence = st.getObject();
				String sequenceURI = sequence.stringValue();
				IRI seq = f.createIRI(sequenceURI);
				RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(seq, null, null, defaultGraphIRI);
				while (statements2.hasNext()) {
					Statement st2 = statements2.next();
					if (st2.getPredicate().equals(hasSequenceValue)) {
						Value seqString = st2.getObject();
						glycanObject.setSequence(seqString.stringValue());
					} else if (st2.getPredicate().equals(hasSequenceFormat)) {
						Value formatString = st2.getObject();
						glycanObject.setSequenceType(formatString.stringValue());
					} else if (st.getPredicate().equals(hasCreatedDate)) {
						Value value = st.getObject();
					    if (value instanceof Literal) {
					    	Literal literal = (Literal)value;
					    	XMLGregorianCalendar calendar = literal.calendarValue();
					    	Date date = calendar.toGregorianCalendar().getTime();
					    	glycanObject.setDateCreated(date);
					    }
					} 
				}
			} 
		}
		
		if (glycanObject != null) {
			statements = sparqlDAO.getStatements(glycan, null, null, graphIRI);
			while (statements.hasNext()) {
				Statement st = statements.next();
				if (st.getPredicate().equals(RDFS.LABEL)) {
					Value label = st.getObject();
					glycanObject.setName(label.stringValue());
				} else if (st.getPredicate().equals(RDFS.COMMENT)) {
					Value comment = st.getObject();
					glycanObject.setComment(comment.stringValue());
				} else if (st.getPredicate().equals(hasInternalId)) {
					Value internalId = st.getObject();
					glycanObject.setInternalId(internalId.stringValue());
				} else if (st.getPredicate().equals(hasModifiedDate)) {
					Value value = st.getObject();
				    if (value instanceof Literal) {
				    	Literal literal = (Literal)value;
				    	XMLGregorianCalendar calendar = literal.calendarValue();
				    	Date date = calendar.toGregorianCalendar().getTime();
				    	glycanObject.setDateModified(date);
				    }
				} else if (st.getPredicate().equals(hasAddedToLibrary)) {
					Value value = st.getObject();
				    if (value instanceof Literal) {
				    	Literal literal = (Literal)value;
				    	XMLGregorianCalendar calendar = literal.calendarValue();
				    	Date date = calendar.toGregorianCalendar().getTime();
				    	glycanObject.setDateAddedToLibrary(date);
				    }
				} 
			}
		}
		
		// TODO - remove this later: for now glytoucanId should be the same as our own id
		if (glycanObject.getGlyTouCanId() == null || glycanObject.getGlyTouCanId().isEmpty())
			glycanObject.setGlyTouCanId(glycanObject.getUri().substring(glycanObject.getUri().lastIndexOf("/")+1));
		
		return glycanObject;
	}
	
	/*@Override
	public Glycan getGlycan(String glytoucanId, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		return findGlycanInGraph(glytoucanId, graph);
	}*/

	@Override
	public void addBlockLayout(BlockLayout b, UserEntity user) throws SparqlException {
		String graph = DEFAULT_GRAPH;
		
			try {
				// check if there is already a private graph for user
				graph = getGraphForUser(user);
				if (graph == null)
					graph = addPrivateGraphForUser(user);
			} catch (SQLException e) {
				throw new SparqlException ("Cannot add the private graph for the user: " + user.getUsername(), e);
			}
		
		
		String blockLayoutURI = generateUniqueURI(uriPrefix + "BL");
		String blockURI = generateUniqueURI(uriPrefix + "B");
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
	public Glycan getGlycanBySequence(String sequence) throws SparqlException {
		return findGlycanInGraphBySequence(sequence, DEFAULT_GRAPH);
	}

	@Override
	public Glycan getGlycanBySequence(String sequence, UserEntity user) throws SparqlException {
		return findGlycanInGraphBySequence(sequence, DEFAULT_GRAPH);
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
		IRI graphIRI = f.createIRI(graph);
		IRI glycan = f.createIRI(glycanURI);
		IRI seq = f.createIRI(sequenceURI);
		IRI hasInternalId = f.createIRI(ontPrefix + "has_internal_id");
		IRI hasGlytoucanId = f.createIRI(ontPrefix + "has_glytoucan_id");
		IRI hasSequenceValue = f.createIRI(ontPrefix + "has_sequence_value");
		IRI hasSequenceFormat = f.createIRI(ontPrefix + "has_sequence_format");
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(glycan, null, null, graphIRI);
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
		
		RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(seq, null, null, graphIRI);
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
	public List<Glycan> getGlycanByUser(UserEntity user) throws SQLException, SparqlException {
		return getGlycanByUser(user, 0, -1, "id", 0 );  // no limit
	}
	
	@Override
	public List<Glycan> getGlycanByUser(UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException {
		List<Glycan> glycans = new ArrayList<Glycan>();
		
		String sortPredicate = getSortPredicate (field);
		// get all glycanURIs from user's private graph
		String graph = getGraphForUser(user);
		if (graph != null) {
			String sortLine = "";
			if (sortPredicate != null)
				sortLine = "?s " + sortPredicate + " ?sortBy .\n";	
			String orderByLine = " ORDER BY " + (order == 0 ? "DESC" : "ASC") + (sortPredicate == null ? "(?s)": "(?sortBy)");	
			StringBuffer queryBuf = new StringBuffer();
			queryBuf.append (prefix + "\n");
			queryBuf.append ("SELECT DISTINCT ?s \n");
			queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
			queryBuf.append ("FROM <" + graph + ">\n");
			queryBuf.append ("WHERE {\n");
			queryBuf.append (sortLine + 
					" ?s gadr:has_date_addedtolibrary ?d .\n" +
				    "}\n" +
					 orderByLine + 
					((limit == -1) ? " " : " LIMIT " + limit) +
					" OFFSET " + offset);
			
			List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
			for (SparqlEntity sparqlEntity : results) {
				String glycanURI = sparqlEntity.getValue("s");
				Glycan glycan = getGlycanFromURI(glycanURI, graph);
				glycans.add(glycan);
			}
		}
		
		return glycans;
	}
	
	private String getSortPredicate(String field) {
		if (field == null || field.equalsIgnoreCase("name")) 
			return "rdfs:label";
		else if (field.equalsIgnoreCase("comment")) 
			return "rdfs:comment";
		else if (field.equalsIgnoreCase("glytoucanId"))
			return "gadr:has_glytoucan_id";
		else if (field.equalsIgnoreCase("internalId"))
			return "gadr:has_internal_id";
		else if (field.equalsIgnoreCase("dateModified"))
			return "gadr:has_date_modified";
		else if (field.equalsIgnoreCase("id"))
			return null;
		return null;
	}

	@Override
	public void deleteGlycan(String glycanId, UserEntity user) throws SQLException, SparqlException {
		String graph;
		
		graph = getGraphForUser(user);
		if (graph != null) {
			// check to see if the given glycanId is in this graph
			Glycan existing = getGlycanFromURI (uriPrefix + glycanId, graph);
			if (existing != null) {
				deleteGlycanByURI (uriPrefix + glycanId, graph);
				return;
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

	
	public void updateGlycan(Glycan g, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		Glycan existing = getGlycanFromURI(g.getUri(), graph);
		if (graph != null && existing !=null) {
			updateGlycanInGraph(g, graph);
		}
	}
	
	void updateGlycanInGraph (Glycan g, String graph) throws SparqlException {	
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI graphIRI = f.createIRI(graph);
		String glycanURI = g.getUri();
		IRI glycan = f.createIRI(glycanURI);
		Literal glycanLabel = f.createLiteral(g.getName());
		Literal glycanComment = g.getComment() == null ? f.createLiteral("") : f.createLiteral(g.getComment());
		Literal internalId = g.getInternalId() == null? f.createLiteral("") : f.createLiteral(g.getInternalId());
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		IRI hasInternalId = f.createIRI(ontPrefix + "has_internal_id");
		Literal date = f.createLiteral(new Date());
		
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(glycan, RDFS.LABEL, graphIRI)), graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(glycan, RDFS.COMMENT, graphIRI)), graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(glycan, hasInternalId, graphIRI)), graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(glycan, hasModifiedDate, graphIRI)), graphIRI);
		
		List<Statement> statements = new ArrayList<Statement>();
		
		statements.add(f.createStatement(glycan, RDFS.LABEL, glycanLabel, graphIRI));
		statements.add(f.createStatement(glycan, RDFS.COMMENT, glycanComment, graphIRI));
		statements.add(f.createStatement(glycan, hasInternalId, internalId, graphIRI));
		statements.add(f.createStatement(glycan, hasModifiedDate, date, graphIRI));
		
		sparqlDAO.addStatements(statements, graphIRI);
	}



	@Override
	public int getGlycanCountByUser(UserEntity user) throws SQLException, SparqlException {
		int total = 0;
		String graph = getGraphForUser(user);
		if (graph != null) {
			StringBuffer queryBuf = new StringBuffer();
			queryBuf.append (prefix + "\n");
			queryBuf.append ("SELECT COUNT(DISTINCT ?s) as ?count \n");
			queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
			queryBuf.append ("FROM <" + graph + ">\n");
			queryBuf.append ("WHERE {\n");
			queryBuf.append (" ?s gadr:has_date_addedtolibrary ?d . }");
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

	@Override
	public Glycan getGlycanById(String glycanId, UserEntity user) throws SparqlException, SQLException {
		// make sure the glycan belongs to this user
		String graph = getGraphForUser(user);
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?d \n");
		queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n");
		queryBuf.append ( "<" +  uriPrefix + glycanId + "> gadr:has_date_addedtolibrary ?d . }\n");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.isEmpty())
			return null;
		else {
			return getGlycanFromURI(uriPrefix + glycanId, graph);
		}
	}



	@Override
	public Glycan getGlycanByInternalId(String internalId, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?s \n");
		queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n");
		queryBuf.append ( " ?s gadr:has_date_addedtolibrary ?d . \n");
		queryBuf.append ( " ?s gadr:has_internal_id \"" + internalId + "\"^^xsd:string . \n"
				+ "}\n");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.isEmpty())
			return null;
		else {
			String glycanURI = results.get(0).getValue("s");
			return getGlycanFromURI(glycanURI, graph);
		}
	}



	@Override
	public Glycan getGlycanByLabel(String label, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?s \n");
		queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n");
		queryBuf.append ( " ?s gadr:has_date_addedtolibrary ?d . \n");
		queryBuf.append ( " ?s rdfs:label \"" + label + "\"^^xsd:string . \n"
				+ "}\n");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.isEmpty())
			return null;
		else {
			String glycanURI = results.get(0).getValue("s");
			return getGlycanFromURI(glycanURI, graph);
		}
	}
}
