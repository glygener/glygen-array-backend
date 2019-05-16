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
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.Spot;
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
		String existing = getGlycanBySequence(g.getSequence());
		if (existing == null) {
			String seqURI = generateUniqueURI(uriPrefix + "Seq");
			glycanURI = generateUniqueURI(uriPrefix) + "GAR";
			
			IRI sequence = f.createIRI(seqURI);
			IRI glycan = f.createIRI(glycanURI);
			IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
			String id = glycanURI.substring(glycanURI.lastIndexOf("/")+1);  //TODO remove this later
			Literal glytoucanId = g.getGlyTouCanId() == null ? f.createLiteral(id) : f.createLiteral(g.getGlyTouCanId());
			Literal sequenceValue = f.createLiteral(g.getSequence());
			Literal format = f.createLiteral(g.getSequenceType());
			Literal date = f.createLiteral(new Date());
			Literal mass = g.getMass() == null ? null : f.createLiteral(g.getMass());
			IRI hasSequence = f.createIRI(ontPrefix + "has_sequence");
			IRI hasGlytoucanId = f.createIRI(ontPrefix + "has_glytoucan_id");
			IRI hasSequenceValue = f.createIRI(ontPrefix + "has_sequence_value");
			IRI hasSequenceFormat = f.createIRI(ontPrefix + "has_sequence_format");
			IRI sequenceType = f.createIRI(ontPrefix + "Sequence");
			IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
			IRI glycanType = f.createIRI(ontPrefix + "Glycan");
			IRI hasMass = f.createIRI(ontPrefix + "has_mass");
			
			List<Statement> statements = new ArrayList<Statement>();
			statements.add(f.createStatement(sequence, RDF.TYPE, sequenceType, defaultGraphIRI));
			statements.add(f.createStatement(glycan, RDF.TYPE, glycanType, defaultGraphIRI));
			statements.add(f.createStatement(glycan, hasSequence, sequence, defaultGraphIRI));
			statements.add(f.createStatement(glycan, hasCreatedDate, date, defaultGraphIRI));
			statements.add(f.createStatement(glycan, hasGlytoucanId, glytoucanId, defaultGraphIRI));
			statements.add(f.createStatement(sequence, hasSequenceValue, sequenceValue, defaultGraphIRI));
			statements.add(f.createStatement(sequence, hasSequenceFormat, format, defaultGraphIRI));
			if (mass != null) statements.add(f.createStatement(glycan, hasMass, mass, defaultGraphIRI));
			
			sparqlDAO.addStatements(statements, defaultGraphIRI);
		} else {
			logger.debug("The glycan already exists in global repository. URI: " + existing);
			glycanURI = existing;
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
		IRI hasMass = f.createIRI(ontPrefix + "has_mass");
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
			} else if (st.getPredicate().equals(hasMass)) {
				Value mass = st.getObject();
				try {
					if (mass != null && mass.stringValue() != null && !mass.stringValue().isEmpty())
						glycanObject.setMass(Double.parseDouble(mass.stringValue())); 
				} catch (NumberFormatException e) {
					logger.warn ("Glycan mass is invalid", e);
				}
			} else if (st.getPredicate().equals(hasCreatedDate)) {
				Value value = st.getObject();
			    if (value instanceof Literal) {
			    	Literal literal = (Literal)value;
			    	XMLGregorianCalendar calendar = literal.calendarValue();
			    	Date date = calendar.toGregorianCalendar().getTime();
			    	glycanObject.setDateCreated(date);
			    }
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
		
		return glycanObject;
	}
	
	/*@Override
	public Glycan getGlycan(String glytoucanId, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		return findGlycanInGraph(glytoucanId, graph);
	}*/

	@Override
	public String addBlockLayout(BlockLayout b, UserEntity user) throws SparqlException {
		String graph = null;
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
		IRI hasBlockLayout = f.createIRI(ontPrefix + "has_block_layout");
		IRI hasSpot = f.createIRI(ontPrefix + "has_spot");
		IRI hasFeature = f.createIRI(ontPrefix + "has_feature");
		IRI hasConcentration = f.createIRI(ontPrefix + "has_concentration");
		IRI hasConcentrationValue = f.createIRI(ontPrefix + "concentration_value");
		IRI hasConcentrationUnit = f.createIRI(ontPrefix + "has_concentration_unit");
		IRI hasGroup = f.createIRI(ontPrefix + "has_group");
		IRI hasRow = f.createIRI(ontPrefix + "has_row");
		IRI hasColumn = f.createIRI(ontPrefix + "has_column");
		IRI hasGlycan = f.createIRI(ontPrefix + "has_molecule");
		IRI hasLinker = f.createIRI(ontPrefix + "has_linker");
		IRI hasRatio = f.createIRI(ontPrefix + "has_ratio");
		IRI featureType = f.createIRI(ontPrefix + "Feature");
		IRI spotType = f.createIRI(ontPrefix + "Spot");
		IRI blockType = f.createIRI(ontPrefix + "Block");
		IRI blockLayoutType = f.createIRI(ontPrefix + "BlockLayout");
		
		Literal blockLayoutLabel = f.createLiteral(b.getName());
		Literal blockLayoutComment = f.createLiteral(b.getDescription());
		
		List<Statement> statements = new ArrayList<Statement>();
		statements.add(f.createStatement(block, RDF.TYPE, blockType));
		statements.add(f.createStatement(blockLayout, RDF.TYPE, blockLayoutType));
		statements.add(f.createStatement(blockLayout, RDFS.LABEL, blockLayoutLabel));
		statements.add(f.createStatement(blockLayout, RDFS.COMMENT, blockLayoutComment));
		statements.add(f.createStatement(block, hasBlockLayout, blockLayout));
		
		List<Feature> processed = new ArrayList<Feature>();
		for (Spot s : b.getSpots()) {
			String spotURI = generateUniqueURI(uriPrefix + "S");
			String concentrationURI = generateUniqueURI(uriPrefix + "C");
			IRI spot = f.createIRI(spotURI);
			IRI concentration = f.createIRI(concentrationURI);
			Literal concentrationUnit = f.createLiteral(s.getConcentration().getLevelUnit().getLabel());
			Literal concentrationValue = f.createLiteral(s.getConcentration().getConcentration());
			Literal row = f.createLiteral(s.getRow());
			Literal column = f.createLiteral(s.getColumn());
			Literal group = f.createLiteral(s.getGroup());
			statements.add(f.createStatement(spot, RDF.TYPE, spotType));
			statements.add(f.createStatement(block, hasSpot, spot));
			statements.add(f.createStatement(spot, hasConcentration, concentration));
			statements.add(f.createStatement(spot, hasRow, row));
			statements.add(f.createStatement(spot, hasColumn, column));
			statements.add(f.createStatement(spot, hasGroup, group));
			statements.add(f.createStatement(concentration, hasConcentrationValue, concentrationValue));
			statements.add(f.createStatement(concentration, hasConcentrationUnit, concentrationUnit));
			
			List<Feature> features = s.getFeatures();
			for (Feature feat : features) {
				if (!processed.contains(feat)) {
					String featureURI = generateUniqueURI(uriPrefix + "F");
					IRI feature = f.createIRI(featureURI);
					String glycanURI = feat.getGlycan().getUri();
					String linkerURI = feat.getLinker().getUri();
					IRI glycan = f.createIRI(glycanURI);
					IRI linker = f.createIRI(linkerURI);
					Literal ratio = feat.getRatio() != null ? f.createLiteral(feat.getRatio()) : f.createLiteral(1.0) ;
					statements.add(f.createStatement(feature, RDF.TYPE, featureType));
					feat.setUri(featureURI);
					statements.add(f.createStatement(spot, hasFeature, feature));
					statements.add(f.createStatement(feature, hasGlycan, glycan));
					statements.add(f.createStatement(feature, hasLinker, linker));
					statements.add(f.createStatement(feature, hasRatio, ratio));
					processed.add(feat);   // processed
				} else {
					Feature existing = processed.get(processed.indexOf(feat));  // existing will have the uri
					IRI feature = f.createIRI(existing.getUri());
					statements.add(f.createStatement(spot, hasFeature, feature));
				}
			}
		}
		
		sparqlDAO.addStatements(statements, graphIRI);
		
		return blockLayoutURI;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getGlycanBySequence(String sequence) throws SparqlException {
		return findGlycanInGraphBySequence(sequence, DEFAULT_GRAPH);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getGlycanBySequence(String sequence, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		return findGlycanInGraphBySequence(sequence, graph);
	}
	
	private String findGlycanInGraphBySequence (String sequence, String graph) throws SparqlException {
		String fromString = "FROM <" + DEFAULT_GRAPH + ">\n";
		String where = "WHERE { " + 
				"				    ?s gadr:has_sequence ?o .\n" +
				"                    ?o gadr:has_sequence_value \"\"\"" + sequence + "\"\"\"^^xsd:string .\n";
		if (!graph.equals(DEFAULT_GRAPH)) {
			// check if the user's private graph has this glycan
			fromString += "FROM <" + graph + ">\n";
			where += "              ?s gadr:has_date_addedtolibrary ?d .\n";
			
		}
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?s ?o\n");
		queryBuf.append (fromString);
		queryBuf.append (where + 
				"				}\n" + 
				"				LIMIT 10");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.size() == 0) 
			return null;
		
		SparqlEntity result = results.get(0);
		String glycanURI = result.getValue("s");
		
		return glycanURI;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Glycan> getGlycanByUser(UserEntity user) throws SQLException, SparqlException {
		return getGlycanByUser(user, 0, -1, "id", 0 );  // no limit
	}
	
	/**
	 * {@inheritDoc}
	 */
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
					" ?s rdf:type  <http://purl.org/gadr/data#Glycan>. \n" +
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
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Linker> getLinkerByUser(UserEntity user) throws SQLException, SparqlException {
		return getLinkerByUser(user, 0, -1, "id", 0 );  // no limit
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Linker> getLinkerByUser(UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException {
		List<Linker> linkers = new ArrayList<Linker>();
		
		String sortPredicate = getSortPredicateForLinker (field);
		// get all linkerURIs from user's private graph
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
					" ?s rdf:type  <http://purl.org/gadr/data#Linker>. \n" +
				    "}\n" +
					 orderByLine + 
					((limit == -1) ? " " : " LIMIT " + limit) +
					" OFFSET " + offset);
			
			List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
			
			for (SparqlEntity sparqlEntity : results) {
				String linkerURI = sparqlEntity.getValue("s");
				Linker linker = getLinkerFromURI(linkerURI, graph);
				linkers.add(linker);
			}
		}
		
		return linkers;
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
		else if (field.equalsIgnoreCase("mass"))
			return "gadr:has_mass";
		else if (field.equalsIgnoreCase("id"))
			return null;
		return null;
	}
	
	private String getSortPredicateForLinker (String field) {
		if (field == null || field.equalsIgnoreCase("name")) 
			return "rdfs:label";
		else if (field.equalsIgnoreCase("comment")) 
			return "rdfs:comment";
		else if (field.equalsIgnoreCase("pubChemId"))
			return "gadr:has_pubchem_compound_id";
		else if (field.equalsIgnoreCase("inChiSequence"))
			return "gadr:has_inChI_sequence";
		else if (field.equalsIgnoreCase("inChiKey"))
			return "gadr:has_inChI_key";
		else if (field.equalsIgnoreCase("iupacName"))
			return "gadr:has_iupac_name";
		else if (field.equalsIgnoreCase("mass"))
			return "gadr:has_mass";
		else if (field.equalsIgnoreCase("molecularFormula"))
			return "gadr:has_molecular_formula";
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
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(glycan, null, null, graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
	}
	
	@Override
	public void deleteLinker(String linkerId, UserEntity user) throws SQLException, SparqlException {
		String graph;
		
		graph = getGraphForUser(user);
		if (graph != null) {
			// check to see if the given glycanId is in this graph
			Linker existing = getLinkerFromURI (uriPrefix + linkerId, graph);
			if (existing != null) {
				deleteLinkerByURI (uriPrefix + linkerId, graph);
				return;
			}
		}
	}
	
	private void deleteLinkerByURI(String uri, String graph) throws SparqlException {
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI linker = f.createIRI(uri);
		IRI graphIRI = f.createIRI(graph);
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(linker, null, null, graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
	}

	@Override
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
		
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(glycan, RDFS.LABEL, null, graphIRI)), graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(glycan, RDFS.COMMENT, null, graphIRI)), graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(glycan, hasInternalId, null, graphIRI)), graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(glycan, hasModifiedDate, null, graphIRI)), graphIRI);
		
		List<Statement> statements = new ArrayList<Statement>();
		
		statements.add(f.createStatement(glycan, RDFS.LABEL, glycanLabel, graphIRI));
		statements.add(f.createStatement(glycan, RDFS.COMMENT, glycanComment, graphIRI));
		statements.add(f.createStatement(glycan, hasInternalId, internalId, graphIRI));
		statements.add(f.createStatement(glycan, hasModifiedDate, date, graphIRI));
		
		sparqlDAO.addStatements(statements, graphIRI);
	}
	
	@Override
	public void updateLinker(Linker g, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		Linker existing = getLinkerFromURI(g.getUri(), graph);
		if (graph != null && existing !=null) {
			updateLinkerInGraph(g, graph);
		}
	}
	
	void updateLinkerInGraph (Linker g, String graph) throws SparqlException {	
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI graphIRI = f.createIRI(graph);
		String linkerURI = g.getUri();
		IRI linker = f.createIRI(linkerURI);
		Literal label = f.createLiteral(g.getName());
		Literal comment = g.getComment() == null ? f.createLiteral("") : f.createLiteral(g.getComment());
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		Literal date = f.createLiteral(new Date());
		
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(linker, RDFS.LABEL, null, graphIRI)), graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(linker, RDFS.COMMENT, null, graphIRI)), graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(linker, hasModifiedDate, null, graphIRI)), graphIRI);
		
		List<Statement> statements = new ArrayList<Statement>();
		
		statements.add(f.createStatement(linker, RDFS.LABEL, label, graphIRI));
		statements.add(f.createStatement(linker, RDFS.COMMENT, comment, graphIRI));
		statements.add(f.createStatement(linker, hasModifiedDate, date, graphIRI));
		
		sparqlDAO.addStatements(statements, graphIRI);
	}



	@Override
	public int getGlycanCountByUser(UserEntity user) throws SQLException, SparqlException {
		String graph = getGraphForUser(user);
		return getCountByUserByType(graph, "Glycan");
	}
	
	@Override
	public int getLinkerCountByUser(UserEntity user) throws SQLException, SparqlException {
		String graph = getGraphForUser(user);
		return getCountByUserByType (graph, "Linker");
	}
	
	/**
	 * 
	 * @param graph graph for the user
	 * @param type "Linker" or "Glycan" (rdf type of the subject)
	 * @return total number of triples with that rdf:type as the subject and date_addedToLibrary as the predicate
	 * @throws SparqlException
	 */
	private int getCountByUserByType (String graph, String type) throws SparqlException {
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
	public Linker getLinkerById(String linkerId, UserEntity user) throws SparqlException, SQLException {
		// make sure the glycan belongs to this user
		String graph = getGraphForUser(user);
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?d \n");
		queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n");
		queryBuf.append ( "<" +  uriPrefix + linkerId + "> gadr:has_date_addedtolibrary ?d . }\n");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.isEmpty())
			return null;
		else {
			return getLinkerFromURI(uriPrefix + linkerId, graph);
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
		queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/data#Glycan>. \n");
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
		queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/data#Glycan>. \n");
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

	@Override
	public String addLinker(Linker l, UserEntity user) throws SparqlException {
		
		String graph = null;
		if (user == null) {
			// cannot add 
			throw new SparqlException ("The user must be provided to put data into private repository");
		}
		
		try {
			// check if there is already a private graph for user
			graph = getGraphForUser(user);
			if (graph == null)
				graph = addPrivateGraphForUser(user);
		} catch (SQLException e) {
			throw new SparqlException ("Cannot add the private graph for the user: " + user.getUsername(), e);
		}
		
		String linkerURI;
		ValueFactory f = sparqlDAO.getValueFactory();
		
		// check if the glycan already exists in "default-graph", then we only need to add it to the user's graph
		String existing = getLinkerByPubChemId(l.getPubChemId());
		if (existing == null) {
			linkerURI = generateUniqueURI(uriPrefix + "L");
			
			IRI linker = f.createIRI(linkerURI);
			IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
			IRI hasInchiSequence = f.createIRI(ontPrefix + "has_inChI_sequence");
			IRI hasInchiKey = f.createIRI(ontPrefix + "has_inChI_key");
			IRI hasIupacName = f.createIRI(ontPrefix + "has_iupac_name");
			IRI hasMass = f.createIRI(ontPrefix + "has_mass");
			IRI hasImageUrl = f.createIRI(ontPrefix + "has_image_url");
			IRI hasPubChemId = f.createIRI(ontPrefix + "has_pubchem_compound_id");
			IRI hasMolecularFormula = f.createIRI(ontPrefix + "has_molecular_formula");
			IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
			
			IRI linkerType = f.createIRI(ontPrefix + "Linker");
			
			Literal pubChemId = f.createLiteral(l.getPubChemId());
			Literal inchiSequence = l.getInChiSequence() == null ? f.createLiteral("") : f.createLiteral(l.getInChiSequence());
			Literal inchiKey = l.getInChiKey() == null ? f.createLiteral("") : f.createLiteral(l.getInChiKey());
			Literal imageUrl = l.getImageURL() == null ? f.createLiteral("") : f.createLiteral(l.getImageURL());
			Literal mass = l.getMass() == null ? f.createLiteral("") : f.createLiteral(l.getMass());
			Literal molecularFormula = l.getMolecularFormula() == null ? f.createLiteral("") : f.createLiteral(l.getMolecularFormula());
			Literal iupacName = l.getIupacName() == null ? f.createLiteral("") : f.createLiteral(l.getIupacName());
			Literal date = f.createLiteral(new Date());
			
			List<Statement> statements = new ArrayList<Statement>();
			statements.add(f.createStatement(linker, RDF.TYPE, linkerType, defaultGraphIRI));
			statements.add(f.createStatement(linker, hasInchiSequence, inchiSequence, defaultGraphIRI));
			statements.add(f.createStatement(linker, hasCreatedDate, date, defaultGraphIRI));
			statements.add(f.createStatement(linker, hasInchiKey, inchiKey, defaultGraphIRI));
			statements.add(f.createStatement(linker, hasIupacName, iupacName, defaultGraphIRI));
			statements.add(f.createStatement(linker, hasMass, mass, defaultGraphIRI));
			statements.add(f.createStatement(linker, hasImageUrl, imageUrl, defaultGraphIRI));
			statements.add(f.createStatement(linker, hasPubChemId, pubChemId, defaultGraphIRI));
			statements.add(f.createStatement(linker, hasMolecularFormula, molecularFormula, defaultGraphIRI));
			
			sparqlDAO.addStatements(statements, defaultGraphIRI);
		} else {
			logger.debug("The linker already exists in global repository. URI: " + existing);
			linkerURI = existing;
		}
		
		// add linker details to the user's private graph
		IRI graphIRI = f.createIRI(graph);
		IRI linker = f.createIRI(linkerURI);
		Literal label = l.getName() == null ? f.createLiteral("") : f.createLiteral(l.getName());
		Literal comment = l.getComment() == null ? f.createLiteral("") : f.createLiteral(l.getComment());
		IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		Literal date = f.createLiteral(new Date());
		
		List<Statement> statements = new ArrayList<Statement>();
		
		statements.add(f.createStatement(linker, RDFS.LABEL, label, graphIRI));
		statements.add(f.createStatement(linker, RDFS.COMMENT, comment, graphIRI));
		statements.add(f.createStatement(linker, hasAddedToLibrary, date, graphIRI));
		statements.add(f.createStatement(linker, hasModifiedDate, date, graphIRI));
		
		sparqlDAO.addStatements(statements, graphIRI);
		
		return linkerURI;
	}


	public String getLinkerByPubChemId(String pubChemId) throws SparqlException {
		return findLinkerInGraphByPubChem(pubChemId, DEFAULT_GRAPH);
	}
	
	public String getLinkerByPubChemId (String pubChemId, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		return findLinkerInGraphByPubChem (pubChemId, graph);
	}
	
	private String findLinkerInGraphByPubChem (String pubChemId, String graph) throws SparqlException {
		String fromString = "FROM <" + DEFAULT_GRAPH + ">\n";
		String where = "WHERE { " + 
				"				    ?s gadr:has_pubchem_compound_id \"" + pubChemId + "\"^^xsd:string .\n";
		if (!graph.equals(DEFAULT_GRAPH)) {
			// check if the user's private graph has this glycan
			fromString += "FROM <" + graph + ">\n";
			where += "              ?s gadr:has_date_addedtolibrary ?d .\n";
			
		}
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?s \n");
		queryBuf.append (fromString);
		queryBuf.append (where + 
				"				}\n" + 
				"				LIMIT 10");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.size() == 0) 
			return null;
		
		SparqlEntity result = results.get(0);
		String linkerURI = result.getValue("s");
		
		return linkerURI;
	}

	@Override
	public Linker getLinkerByLabel(String label, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?s \n");
		queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n");
		queryBuf.append ( " ?s gadr:has_date_addedtolibrary ?d . \n");
		queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/data#Linker>. \n");
		queryBuf.append ( " ?s rdfs:label \"" + label + "\"^^xsd:string . \n"
				+ "}\n");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.isEmpty())
			return null;
		else {
			String linkerURI = results.get(0).getValue("s");
			return getLinkerFromURI(linkerURI, graph);
		}
	}

	private Linker getLinkerFromURI(String linkerURI, String graph) {
		Linker linkerObject = null;
		ValueFactory f = sparqlDAO.getValueFactory();
		
		IRI linker = f.createIRI(linkerURI);
		IRI graphIRI = f.createIRI(graph);
		IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
		IRI hasInchiSequence = f.createIRI(ontPrefix + "has_inChI_sequence");
		IRI hasInchiKey = f.createIRI(ontPrefix + "has_inChI_key");
		IRI hasIupacName = f.createIRI(ontPrefix + "has_iupac_name");
		IRI hasMass = f.createIRI(ontPrefix + "has_mass");
		IRI hasImageUrl = f.createIRI(ontPrefix + "has_image_url");
		IRI hasPubChemId = f.createIRI(ontPrefix + "has_pubchem_compound_id");
		IRI hasMolecularFormula = f.createIRI(ontPrefix + "has_molecular_formula");
		IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
		IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(linker, null, null, defaultGraphIRI);
		if (statements.hasNext()) {
			linkerObject = new Linker();
			linkerObject.setUri(linkerURI);
		}
		while (statements.hasNext()) {
			Statement st = statements.next();
			if (st.getPredicate().equals(hasInchiSequence)) {
				Value seq = st.getObject();
				linkerObject.setInChiSequence(seq.stringValue()); 
			} else if (st.getPredicate().equals(hasInchiKey)) {
				Value val = st.getObject();
				linkerObject.setInChiKey(val.stringValue()); 
			} else if (st.getPredicate().equals(hasIupacName)) {
				Value val = st.getObject();
				linkerObject.setIupacName(val.stringValue()); 
			} else if (st.getPredicate().equals(hasImageUrl)) {
				Value val = st.getObject();
				linkerObject.setImageURL(val.stringValue()); 
			} else if (st.getPredicate().equals(hasPubChemId)) {
				Value val = st.getObject();
				linkerObject.setPubChemId(val.stringValue()); 
			} else if (st.getPredicate().equals(hasMolecularFormula)) {
				Value val = st.getObject();
				linkerObject.setMolecularFormula(val.stringValue()); 
			} else if (st.getPredicate().equals(hasMass)) {
				Value mass = st.getObject();
				try {
					if (mass != null && mass.stringValue() != null && !mass.stringValue().isEmpty())
						linkerObject.setMass(Double.parseDouble(mass.stringValue())); 
				} catch (NumberFormatException e) {
					logger.warn ("Glycan mass is invalid", e);
				}
			} else if (st.getPredicate().equals(hasCreatedDate)) {
				Value value = st.getObject();
			    if (value instanceof Literal) {
			    	Literal literal = (Literal)value;
			    	XMLGregorianCalendar calendar = literal.calendarValue();
			    	Date date = calendar.toGregorianCalendar().getTime();
			    	linkerObject.setDateCreated(date);
			    }
			}
		}
		
		if (linkerObject != null) {
			statements = sparqlDAO.getStatements(linker, null, null, graphIRI);
			while (statements.hasNext()) {
				Statement st = statements.next();
				if (st.getPredicate().equals(RDFS.LABEL)) {
					Value label = st.getObject();
					linkerObject.setName(label.stringValue());
				} else if (st.getPredicate().equals(RDFS.COMMENT)) {
					Value comment = st.getObject();
					linkerObject.setComment(comment.stringValue());
				} else if (st.getPredicate().equals(hasModifiedDate)) {
					Value value = st.getObject();
				    if (value instanceof Literal) {
				    	Literal literal = (Literal)value;
				    	XMLGregorianCalendar calendar = literal.calendarValue();
				    	Date date = calendar.toGregorianCalendar().getTime();
				    	linkerObject.setDateModified(date);
				    }
				} else if (st.getPredicate().equals(hasAddedToLibrary)) {
					Value value = st.getObject();
				    if (value instanceof Literal) {
				    	Literal literal = (Literal)value;
				    	XMLGregorianCalendar calendar = literal.calendarValue();
				    	Date date = calendar.toGregorianCalendar().getTime();
				    	linkerObject.setDateAddedToLibrary(date);
				    }
				} 
			}
		}
		
		return linkerObject;
	}
}
