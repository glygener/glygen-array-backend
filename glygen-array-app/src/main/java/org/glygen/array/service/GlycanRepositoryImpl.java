package org.glygen.array.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import org.glycoinfo.GlycanFormatconverter.io.GlycoCT.WURCSExporterGlycoCT;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.GlycanSequenceFormat;
import org.glygen.array.persistence.rdf.GlycanType;
import org.glygen.array.persistence.rdf.MassOnlyGlycan;
import org.glygen.array.persistence.rdf.Owner;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.glygen.array.persistence.rdf.UnknownGlycan;
import org.glygen.array.util.GlytoucanUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(value="sesameTransactionManager") 
public class GlycanRepositoryImpl extends GlygenArrayRepositoryImpl implements GlycanRepository  {

	@Override
	public void addAliasForGlycan(String glycanId, String alias, UserEntity user) throws SparqlException, SQLException {
		if (alias == null || alias.isEmpty())
			return;
		
		String graph;
		graph = getGraphForUser(user);
		if (graph != null) {
			// check to see if the given glycanId is in this graph
			String glycanURI = uriPrefix + glycanId;
			Glycan existing = getGlycanFromURI (glycanURI, user);
			if (existing != null) {
				// check if the alias is unique
				if (existing.getAliases().contains(alias))
					return;
				Glycan byAlias = getGlycanByLabel (alias, user);  // checks the alias as well
				if (byAlias != null)
					return; // cannot add
				
				ValueFactory f = sparqlDAO.getValueFactory();
				IRI glycan = f.createIRI(glycanURI);
				IRI graphIRI = f.createIRI(graph);
				Literal aliasLiteral = f.createLiteral(alias);
				IRI hasAlias = f.createIRI(ontPrefix + "has_alias");
				
				List<Statement> statements = new ArrayList<Statement>();
				statements.add(f.createStatement(glycan, hasAlias, aliasLiteral, graphIRI));
				
				sparqlDAO.addStatements(statements, graphIRI);
			}
		}
	}
	
	@Override
	public String addGlycan(Glycan g, UserEntity user) throws SparqlException, SQLException {
		return addGlycan(g, user, true);
	}
	
	@Override
	public String addGlycan(Glycan g, UserEntity user, boolean noGlytoucanRegistration)
			throws SparqlException, SQLException {
		if (user == null) {
			// cannot add 
			throw new SparqlException ("The user must be provided to put data into private repository");
		}
		
		switch (g.getType()) {
			case SEQUENCE_DEFINED:
				return addSequenceDefinedGlycan ((SequenceDefinedGlycan) g, user, noGlytoucanRegistration);
			case UNKNOWN:
				return addUnknownGlycan(g, user);	
			case MASS_ONLY:
				return addMassOnlyGlycan ((MassOnlyGlycan)g, user);
			default:
				throw new SparqlException (g.getType() + " type is not supported yet!");	
		}
	}
	
	private String addMassOnlyGlycan(MassOnlyGlycan g, UserEntity user) throws SparqlException, SQLException {
		
		String graph = null;
		if (g == null || g.getMass() == null)
			// cannot add 
			throw new SparqlException ("Not enough information is provided to register a glycan");
		
		// check if there is already a private graph for user
		graph = getGraphForUser(user);
		
		// check if there is a glycan with the same name
		// if so, do not add
		if (g.getName() != null && !g.getName().isEmpty()) { 
			Glycan existing = getGlycanByLabel(g.getName(), user);
			if (existing != null)
				return existing.getUri();
		}
		
		String glycanURI = addBasicInfoForGlycan(g, graph);
		
		ValueFactory f = sparqlDAO.getValueFactory();
		Literal mass = g.getMass() == null ? null : f.createLiteral(g.getMass());
		IRI hasMass = f.createIRI(ontPrefix + "has_mass");
		IRI glycan = f.createIRI(glycanURI);
		IRI graphIRI = f.createIRI(graph);
		
		List<Statement> statements = new ArrayList<Statement>();
		if (mass != null) statements.add(f.createStatement(glycan, hasMass, mass, graphIRI));
		sparqlDAO.addStatements(statements, graphIRI);
		
		return glycanURI;
	}
	
	String addBasicInfoForGlycan (Glycan g, String graph) throws SparqlException {
		ValueFactory f = sparqlDAO.getValueFactory();
		String glycanURI = generateUniqueURI(uriPrefix) + "GAR";
		IRI glycan = f.createIRI(glycanURI);
		Literal date = f.createLiteral(new Date());
		IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
		IRI hasGlycanType = f.createIRI(ontPrefix + "has_type");
		Literal type = f.createLiteral(g.getType().name());
		IRI graphIRI = f.createIRI(graph);
		Literal glycanLabel = g.getName() == null ? null : f.createLiteral(g.getName());
		Literal glycanComment = g.getComment() == null ? null : f.createLiteral(g.getComment());
		Literal internalId = g.getInternalId() == null ? null : f.createLiteral(g.getInternalId());
		IRI hasInternalId = f.createIRI(ontPrefix + "has_internal_id");
		IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		IRI glycanType = f.createIRI(ontPrefix + "Glycan");
		
		List<Statement> statements = new ArrayList<Statement>();
		
		statements.add(f.createStatement(glycan, RDF.TYPE, glycanType, graphIRI));
		statements.add(f.createStatement(glycan, hasGlycanType, type, graphIRI));
		statements.add(f.createStatement(glycan, hasCreatedDate, date, graphIRI));
		if (glycanLabel != null) statements.add(f.createStatement(glycan, RDFS.LABEL, glycanLabel, graphIRI));
		if (internalId != null) statements.add(f.createStatement(glycan, hasInternalId, internalId, graphIRI));
		if (glycanComment != null) statements.add(f.createStatement(glycan, RDFS.COMMENT, glycanComment, graphIRI));
		statements.add(f.createStatement(glycan, hasAddedToLibrary, date, graphIRI));
		statements.add(f.createStatement(glycan, hasModifiedDate, date, graphIRI));
		
		sparqlDAO.addStatements(statements, graphIRI);
		return glycanURI;
	}

	private String addUnknownGlycan(Glycan g, UserEntity user) throws SparqlException, SQLException {
		String graph = null;
		if (g == null || g.getName() == null)
			// cannot add 
			throw new SparqlException ("Not enough information is provided to register a glycan");
		
		// check if there is already a private graph for user
		graph = getGraphForUser(user);
		
		// check if there is a glycan with the same name
		// if so, do not add
		if (g.getName() != null && !g.getName().isEmpty()) { 
			Glycan existing = getGlycanByLabel(g.getName(), user);
			if (existing != null)
				return existing.getUri();
		}
		
		return addBasicInfoForGlycan(g, graph);
	}

	private String addSequenceDefinedGlycan(SequenceDefinedGlycan g, UserEntity user, boolean noGlytoucanRegistration) throws SparqlException, SQLException {
		String graph = null;
		if (g == null || g.getSequence() == null || g.getSequence().isEmpty())
			// cannot add 
			throw new SparqlException ("Not enough information is provided to register a glycan");
		
		// check if there is already a private graph for user
		graph = getGraphForUser(user);
		
		
		ValueFactory f = sparqlDAO.getValueFactory();
		String glycanURI;
		
		// check if the glycan already exists in "default-graph", then we need to add a triple glycan->has_public_uri->existingURI to the private repo
		String existing = getGlycanBySequence(g.getSequence());
		if (existing == null) {
			glycanURI = addBasicInfoForGlycan(g, graph);	
			String seqURI = generateUniqueURI(uriPrefix + "Seq");
		
			IRI sequence = f.createIRI(seqURI);
			IRI glycan = f.createIRI(glycanURI);
			
			String glyToucanId = null;
			if (g.getGlytoucanId() == null && !noGlytoucanRegistration) {
				// check and register to GlyToucan
				try {
					WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
					exporter.start(g.getSequence());
					String wurcs = exporter.getWURCS();
					glyToucanId = GlytoucanUtil.getInstance().registerGlycan(wurcs);
				} catch (Exception e) {
					logger.warn("Cannot register glytoucanId with the given sequence", g.getSequence());
				}
			} else if (g.getGlytoucanId() == null) {
				// check if it is already in GlyToucan
				try {
					WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
					exporter.start(g.getSequence());
					String wurcs = exporter.getWURCS();
					glyToucanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);	
				} catch (Exception e) {
					logger.warn("Cannot get glytoucanId with the given sequence", g.getSequence());
				}
			}
			
			Literal glytoucanLit = glyToucanId == null ? null : f.createLiteral(glyToucanId);
			Literal sequenceValue = f.createLiteral(g.getSequence());
			Literal format = f.createLiteral(g.getSequenceType().getLabel());
			
			IRI hasSequence = f.createIRI(ontPrefix + "has_sequence");
			IRI hasGlytoucanId = f.createIRI(ontPrefix + "has_glytoucan_id");
			IRI hasSequenceValue = f.createIRI(ontPrefix + "has_sequence_value");
			IRI hasSequenceFormat = f.createIRI(ontPrefix + "has_sequence_format");
			IRI sequenceType = f.createIRI(ontPrefix + "Sequence");
			IRI graphIRI = f.createIRI(graph);
			Literal mass = g.getMass() == null ? null : f.createLiteral(g.getMass());
			IRI hasMass = f.createIRI(ontPrefix + "has_mass");
			
			List<Statement> statements = new ArrayList<Statement>();
			
			statements.add(f.createStatement(sequence, RDF.TYPE, sequenceType, graphIRI));
			statements.add(f.createStatement(glycan, hasSequence, sequence, graphIRI));
			if (glytoucanLit != null) statements.add(f.createStatement(glycan, hasGlytoucanId, glytoucanLit, graphIRI));
			statements.add(f.createStatement(sequence, hasSequenceValue, sequenceValue, graphIRI));
			statements.add(f.createStatement(sequence, hasSequenceFormat, format, graphIRI));
			if (mass != null) statements.add(f.createStatement(glycan, hasMass, mass, graphIRI));
			
			sparqlDAO.addStatements(statements, graphIRI);
		} else {
			logger.debug("The glycan already exists in global repository. URI: " + existing);
			String publicURI = existing;
			IRI glycan = f.createIRI(publicURI);
			
			glycanURI = generateUniqueURI(uriPrefix) + "GAR";
			IRI localGlycan = f.createIRI(glycanURI);
			IRI graphIRI = f.createIRI(graph);
			IRI hasPublicURI = f.createIRI(ontPrefix + "has_public_uri");
			Literal date = f.createLiteral(new Date());
			List<Statement> statements = new ArrayList<Statement>();
			IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
			IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
			Literal internalId = g.getInternalId() == null ? f.createLiteral("") : f.createLiteral(g.getInternalId());
			IRI hasInternalId = f.createIRI(ontPrefix + "has_internal_id");
			Literal glycanLabel = g.getName() == null ? f.createLiteral("") : f.createLiteral(g.getName());
			Literal glycanComment = g.getComment() == null ? f.createLiteral("") : f.createLiteral(g.getComment());
			IRI hasGlycanType = f.createIRI(ontPrefix + "has_type");
			Literal type = f.createLiteral(g.getType().name());
			IRI glycanType = f.createIRI(ontPrefix + "Glycan");
			
			statements.add(f.createStatement(glycan, RDF.TYPE, glycanType, graphIRI));
			statements.add(f.createStatement(glycan, hasGlycanType, type, graphIRI));
			statements.add(f.createStatement(localGlycan, hasPublicURI, glycan, graphIRI));
			statements.add(f.createStatement(localGlycan, hasAddedToLibrary, date, graphIRI));
			statements.add(f.createStatement(localGlycan, hasModifiedDate, date, graphIRI));
			statements.add(f.createStatement(localGlycan, RDFS.LABEL, glycanLabel, graphIRI));
			statements.add(f.createStatement(localGlycan, hasInternalId, internalId, graphIRI));
			statements.add(f.createStatement(localGlycan, RDFS.COMMENT, glycanComment, graphIRI));
			
			sparqlDAO.addStatements(statements, graphIRI);
			
			addAliasForGlycan(g.getUri().substring(g.getUri().lastIndexOf("/")+1), g.getName(), user);
		}
		
		return glycanURI;
		
	}
	
	@Override
	public void deleteGlycan(String glycanId, UserEntity user) throws SQLException, SparqlException {
		String graph;
		
		graph = getGraphForUser(user);
		if (graph != null) {
			// check to see if the given glycanId is in this graph
			Glycan existing = getGlycanFromURI (uriPrefix + glycanId, user);
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
		RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(glycan, null, null, graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(statements2), graphIRI);
		//TODO what to do with sequence value and format
	}

	/**
	 * return the glycanURI (from the user's graph) for the glycan with the given sequence
	 * check for the cases where some details are in the user's graph but the sequence is in public graph
	 * if graph passed is the public graph, then it will look only in public glycans and the returned URI 
	 * will be from the public graph, otherwise it will always be the URI from the user's graph even if the glycan is public as well.
	 * 
	 * @param sequence to search for
	 * @param graph to look into (still check the public graph)
	 * @return glycan URI
	 * @throws SparqlException
	 */
	private String findGlycanInGraphBySequence (String sequence, String graph) throws SparqlException {
		String fromString = "FROM <" + DEFAULT_GRAPH + ">\n";
		String whereClause = "WHERE {";
		String where = " { " + 
				"				    ?s gadr:has_sequence ?o .\n" +
				"                    ?o gadr:has_sequence_value \"\"\"" + sequence + "\"\"\"^^xsd:string .\n";
		if (!graph.equals(DEFAULT_GRAPH)) {
			// check if the user's private graph has this glycan
			fromString += "FROM <" + graph + ">\n";
			where += "              ?s gadr:has_date_addedtolibrary ?d .\n }";
			where += "  UNION { ?s gadr:has_date_addedtolibrary ?d .\n"
					+ " ?s gadr:has_public_uri ?p . \n" 
			        + " ?p gadr:has_sequence ?o . \n"
					+ " ?o gadr:has_sequence_value \"\"\"" + sequence + "\"\"\"^^xsd:string . \n}";
			
		} else {
			where += "}";
		}
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?s ?o\n");
		queryBuf.append (fromString);
		queryBuf.append (whereClause + where + 
				"				}\n" + 
				"				LIMIT 10");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.size() == 0) 
			return null;
		
		SparqlEntity result = results.get(0);
		String glycanURI = result.getValue("s");
		
		return glycanURI;
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
			return getGlycanFromURI(uriPrefix + glycanId, user);
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
			return getGlycanFromURI(glycanURI, user);
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
		queryBuf.append ( " {?s rdfs:label \"" + label + "\"^^xsd:string . \n }");
		queryBuf.append ( " UNION {?s gadr:has_alias \"" + label + "\"^^xsd:string . \n }");
		queryBuf.append ( "}\n");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.isEmpty())
			return null;
		else {
			String glycanURI = results.get(0).getValue("s");
			return getGlycanFromURI(glycanURI, user);
		}
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
				Glycan glycan = getGlycanFromURI(glycanURI, user);
				glycans.add(glycan);	
			}
		}
		
		return glycans;
	}
	
	public List<Glycan> getSharedGlycansByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException {
		List<Glycan> glycans = new ArrayList<Glycan>();
		
		return glycans;
	}
	
	@Override
	public int getGlycanCountByUser(UserEntity user) throws SQLException, SparqlException {
		String graph = getGraphForUser(user);
		return getCountByUserByType(graph, "Glycan");
	}
	
	private GlycanType getGlycanTypeForGlycan (String glycanURI, String graph) throws SparqlException {
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?t \n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n");
		queryBuf.append ("<" +  glycanURI + "> gadr:has_type ?t . }");

		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.isEmpty())
			return null;
		else {
			String type = results.get(0).getValue("t");
			return GlycanType.valueOf(type);
		}
	}


	@Override
	public Glycan getGlycanFromURI (String glycanURI, UserEntity user) throws SparqlException, SQLException {
		Glycan glycanObject = null;
		String graph = getGraphForUser(user);
		if (graph == null) return null;
		
		GlycanType type = getGlycanTypeForGlycan(glycanURI, graph);
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI glycan = f.createIRI(glycanURI);
		IRI graphIRI = f.createIRI(graph);
		IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
		IRI hasPublicURI = f.createIRI(ontPrefix + "has_public_uri");
		IRI hasSequence = f.createIRI(ontPrefix + "has_sequence");
		IRI hasGlytoucanId = f.createIRI(ontPrefix + "has_glytoucan_id");
		IRI hasMass = f.createIRI(ontPrefix + "has_mass");
		IRI hasAlias = f.createIRI(ontPrefix + "has_alias");
		IRI hasInternalId = f.createIRI(ontPrefix + "has_internal_id");
		IRI hasSequenceValue = f.createIRI(ontPrefix + "has_sequence_value");
		IRI hasSequenceFormat = f.createIRI(ontPrefix + "has_sequence_format");
		IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
		IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(glycan, null, null, graphIRI);
		
		
		if (statements.hasNext()) {
			switch (type) {
			case SEQUENCE_DEFINED:
				glycanObject = new SequenceDefinedGlycan();
				break;
			case MASS_ONLY:
				glycanObject = new MassOnlyGlycan();
				break;
			case UNKNOWN:
				glycanObject = new UnknownGlycan();
				break;
			case CLASSIFICATION_BASED:   //TODO: change later when we start supporting these types
			case COMPOSITION_BASED:
			case FRAGMENT_ONLY:
				glycanObject = new Glycan();
				break;
			}
			glycanObject.setUri(glycanURI);
			glycanObject.setId(glycanURI.substring(glycanURI.lastIndexOf("/")+1));
			Owner owner = new Owner ();
			owner.setUserId(user.getUserId());
			owner.setName(user.getUsername());
			glycanObject.setOwner(owner);
		}
		while (statements.hasNext()) {
			Statement st = statements.next();
			if (st.getPredicate().equals(hasGlytoucanId)) {
				Value glytoucanId = st.getObject();
				if (glycanObject instanceof SequenceDefinedGlycan)
					((SequenceDefinedGlycan)glycanObject).setGlytoucanId(glytoucanId.stringValue()); 
			} else if (st.getPredicate().equals(hasMass)) {
				Value mass = st.getObject();
				try {
					if (mass != null && mass.stringValue() != null && !mass.stringValue().isEmpty()) {
						if (glycanObject instanceof MassOnlyGlycan)
							((MassOnlyGlycan)glycanObject).setMass(Double.parseDouble(mass.stringValue())); 
					}
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
				RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(seq, null, null, graphIRI);
				while (statements2.hasNext()) {
					Statement st2 = statements2.next();
					if (st2.getPredicate().equals(hasSequenceValue)) {
						Value seqString = st2.getObject();
						if (glycanObject instanceof SequenceDefinedGlycan)
							((SequenceDefinedGlycan)glycanObject).setSequence(seqString.stringValue());
					} else if (st2.getPredicate().equals(hasSequenceFormat)) {
						Value formatString = st2.getObject();
						if (glycanObject instanceof SequenceDefinedGlycan)
							((SequenceDefinedGlycan)glycanObject).setSequenceType(GlycanSequenceFormat.forValue(formatString.stringValue()));
					}  
				}
			} else if (st.getPredicate().equals(RDFS.LABEL)) {
				Value label = st.getObject();
				glycanObject.setName(label.stringValue());
			} else if (st.getPredicate().equals(RDFS.COMMENT)) {
				Value comment = st.getObject();
				glycanObject.setComment(comment.stringValue());
			} else if (st.getPredicate().equals(hasInternalId)) {
				Value internalId = st.getObject();
				glycanObject.setInternalId(internalId.stringValue());
			} else if (st.getPredicate().equals(hasAlias)) {
				Value alias = st.getObject();
				glycanObject.addAlias(alias.stringValue());
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
			}  else if (st.getPredicate().equals(hasPublicURI)) {
				// need to retrieve additional information from DEFAULT graph
				// that means the glycan is already make public
				glycanObject.setIsPublic(true);  
				Value uriValue = st.getObject();
				String publicGlycanURI = uriValue.stringValue();
				IRI publicGlycan = f.createIRI(publicGlycanURI);
				RepositoryResult<Statement> statementsPublic = sparqlDAO.getStatements(publicGlycan, null, null, defaultGraphIRI);
				while (statementsPublic.hasNext()) {
					Statement stPublic = statementsPublic.next();
					if (stPublic.getPredicate().equals(hasGlytoucanId)) {
						Value glytoucanId = stPublic.getObject();
						if (glycanObject instanceof SequenceDefinedGlycan)
							((SequenceDefinedGlycan)glycanObject).setGlytoucanId(glytoucanId.stringValue()); 
					} else if (stPublic.getPredicate().equals(hasMass)) {
						Value mass = stPublic.getObject();
						try {
							if (mass != null && mass.stringValue() != null && !mass.stringValue().isEmpty()) {
								if (glycanObject instanceof MassOnlyGlycan)
									((MassOnlyGlycan)glycanObject).setMass(Double.parseDouble(mass.stringValue())); 
							}
						} catch (NumberFormatException e) {
							logger.warn ("Glycan mass is invalid", e);
						}
					} else if (stPublic.getPredicate().equals(hasSequence)) {
						Value sequence = stPublic.getObject();
						String sequenceURI = sequence.stringValue();
						IRI seq = f.createIRI(sequenceURI);
						RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(seq, null, null, defaultGraphIRI);
						while (statements2.hasNext()) {
							Statement st2 = statements2.next();
							if (st2.getPredicate().equals(hasSequenceValue)) {
								Value seqString = st2.getObject();
								if (glycanObject instanceof SequenceDefinedGlycan)
									((SequenceDefinedGlycan)glycanObject).setSequence(seqString.stringValue());
							} else if (st2.getPredicate().equals(hasSequenceFormat)) {
								Value formatString = st2.getObject();
								if (glycanObject instanceof SequenceDefinedGlycan)
									((SequenceDefinedGlycan)glycanObject).setSequenceType(GlycanSequenceFormat.valueOf(formatString.stringValue()));
							}  
						}
					}
					
				}
			}
		}
		return glycanObject;
	}
	
	@Override
	public void updateGlycan(Glycan g, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		Glycan existing = getGlycanFromURI(g.getUri(), user);
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
}
