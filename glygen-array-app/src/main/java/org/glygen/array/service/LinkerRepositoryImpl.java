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
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.LinkerClassification;
import org.glygen.array.persistence.rdf.LinkerType;
import org.glygen.array.persistence.rdf.PeptideLinker;
import org.glygen.array.persistence.rdf.ProteinLinker;
import org.glygen.array.persistence.rdf.SequenceBasedLinker;
import org.glygen.array.persistence.rdf.SmallMoleculeLinker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(value="sesameTransactionManager") 
public class LinkerRepositoryImpl extends GlygenArrayRepositoryImpl implements LinkerRepository {
	
	public final static String hasPubChemIdProperty = "has_pubchem_compound_id";
	public final static String hasInchiKeyProperty = "has_inChI_key";
	
	final static String hasSequencePredicate = ontPrefix + "has_sequence";
	final static String hasPdbIdPredicate = ontPrefix + "has_pdbId";
	final static String hasUniprotIdPredicate = ontPrefix + "has_uniProtId";
	final static String hasInchiSequencePredicate = ontPrefix + "has_inChI_sequence";
	final static String hasInchiKeyPredicate = ontPrefix + hasInchiKeyProperty;
	final static String hasIupacNamePredicate = ontPrefix + "has_iupac_name";
	final static String hasMassPredicate = ontPrefix + "has_mass";
	final static String hasImageUrlPredicate = ontPrefix + "has_image_url";
	final static String hasPubChemIdPredicate = ontPrefix + hasPubChemIdProperty;
	final static String hasMolecularFormulaPredicate = ontPrefix + "has_molecular_formula";
	final static String hasClassificationPredicate = ontPrefix + "has_classification";
	final static String hasChebiIdPredicate = ontPrefix+ "has_chEBI";
	final static String hasClassificationValuePredicate = ontPrefix+ "has_classification_value";
	final static String opensRingPredicate = ontPrefix + "opens_ring";
	final static String linkerTypePredicate = ontPrefix + "Linker";
	final static String hasURLPredicate = ontPrefix + "has_url";
	
	@Override
	public String addLinker(Linker l, UserEntity user) throws SparqlException, SQLException {
		
		String graph = null;
		if (user == null) {
			// cannot add 
			throw new SparqlException ("The user must be provided to put data into private repository");
		}
		
		
		// check if there is already a private graph for user
		graph = getGraphForUser(user);
		
		switch (l.getType()) {
		case SMALLMOLECULE_LINKER:
			return addSmallMoleculeLinker((SmallMoleculeLinker) l, graph);
		case PEPTIDE_LINKER:
		case PROTEIN_LINKER:
			return addSequenceBasedLinker (l, graph);
		default:
			throw new SparqlException(l.getType() + " type is not supported");
		}
	}
		
	
	private String addSequenceBasedLinker(Linker l, String graph) throws SparqlException {
		String linkerURI;
		ValueFactory f = sparqlDAO.getValueFactory();
		
		// check if the linker already exists in "default-graph"
		String existing = null;
		String sequence = null;
		if (l.getType() == LinkerType.PROTEIN_LINKER || l.getType() == LinkerType.PEPTIDE_LINKER) {
			sequence = ((SequenceBasedLinker)l).getSequence();
		}
		
		if (sequence == null) {
			// cannot add 
			throw new SparqlException ("Not enough information is provided to register a linker");
		}
		
		existing = getLinkerByField(sequence, "has_sequence", "string");
		
		if (existing == null) {
			linkerURI = generateUniqueURI(uriPrefix + "L");
			
			IRI linker = f.createIRI(linkerURI);
			IRI graphIRI = f.createIRI(graph);
			IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
			IRI opensRing = f.createIRI(opensRingPredicate);
			IRI hasDescription = f.createIRI(hasDescriptionPredicate);
			IRI linkerType = f.createIRI(linkerTypePredicate);
			IRI hasLinkerType = f.createIRI(hasTypePredicate);
			IRI hasUrl = f.createIRI(hasURLPredicate);
			Literal type = f.createLiteral(l.getType().name());
			IRI hasSequence = f.createIRI(hasSequencePredicate);
			Literal label = l.getName() == null ? f.createLiteral("") : f.createLiteral(l.getName());
			Literal comment = l.getComment() == null ? f.createLiteral("") : f.createLiteral(l.getComment());
			Literal description = null;
			if (l.getDescription() != null)
				description = f.createLiteral(l.getDescription());
			
			IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
			IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
			Literal opensRingValue = l.getOpensRing() == null ? f.createLiteral(2) : f.createLiteral(l.getOpensRing());
			Literal date = f.createLiteral(new Date());
			
			Literal sequenceL= f.createLiteral(sequence);
			
			List<Statement> statements = new ArrayList<Statement>();
			statements.add(f.createStatement(linker, RDF.TYPE, linkerType, graphIRI));
			statements.add(f.createStatement(linker, hasLinkerType, type, graphIRI));
			statements.add(f.createStatement(linker, RDFS.LABEL, label, graphIRI));
			statements.add(f.createStatement(linker, RDFS.COMMENT, comment, graphIRI));
			statements.add(f.createStatement(linker, hasAddedToLibrary, date, graphIRI));
			statements.add(f.createStatement(linker, hasModifiedDate, date, graphIRI));
			statements.add(f.createStatement(linker, hasCreatedDate, date, graphIRI));
			statements.add(f.createStatement(linker, opensRing, opensRingValue, graphIRI));
			statements.add(f.createStatement(linker, hasSequence, sequenceL, graphIRI));
			if (description != null) statements.add(f.createStatement(linker, hasDescription, description, graphIRI));
			
			if (l.getType() == LinkerType.PROTEIN_LINKER) {
				if (((ProteinLinker)l).getUniProtId() != null) {
					IRI hasUniProtId = f.createIRI(hasUniprotIdPredicate);
					Literal uniProt = f.createLiteral(((ProteinLinker)l).getUniProtId());
					statements.add(f.createStatement(linker, hasUniProtId, uniProt, graphIRI));
				}
				if (((ProteinLinker)l).getPdbId() != null) {
					IRI hasPDBId = f.createIRI(hasPdbIdPredicate);
					Literal pdb = f.createLiteral(((ProteinLinker)l).getPdbId());
					statements.add(f.createStatement(linker, hasPDBId, pdb, graphIRI));
				}
			}
			
			if (l.getUrls() != null) {
				for (String url: l.getUrls()) {
					Literal urlLit = f.createLiteral(url);
					statements.add(f.createStatement(linker, hasUrl, urlLit, graphIRI));
				}
			}
			
			sparqlDAO.addStatements(statements, graphIRI);
		} else {
			logger.debug("The linker already exists in global repository. URI: " + existing);
			linkerURI = existing;
			// add has_public_uri to point to the global one, add details to local
			
			IRI linker = f.createIRI(linkerURI);
			
			linkerURI = generateUniqueURI(uriPrefix) + "L";
			IRI localLinker = f.createIRI(linkerURI);
			IRI graphIRI = f.createIRI(graph);
			IRI hasPublicURI = f.createIRI(hasPublicURIPredicate);
			Literal date = f.createLiteral(new Date());
			IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
			IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
			Literal label = l.getName() == null ? f.createLiteral("") : f.createLiteral(l.getName());
			Literal comment = l.getComment() == null ? f.createLiteral("") : f.createLiteral(l.getComment());
			
			List<Statement> statements = new ArrayList<Statement>();
			
			IRI linkerType = f.createIRI(linkerTypePredicate);
			IRI hasLinkerType = f.createIRI(hasTypePredicate);
			Literal type = f.createLiteral(l.getType().name());
			
			statements.add(f.createStatement(linker, RDF.TYPE, linkerType, graphIRI));
			statements.add(f.createStatement(linker, hasLinkerType, type, graphIRI));
			statements.add(f.createStatement(localLinker, hasPublicURI, linker, graphIRI));
			statements.add(f.createStatement(localLinker, hasAddedToLibrary, date, graphIRI));
			statements.add(f.createStatement(localLinker, hasModifiedDate, date, graphIRI));
			statements.add(f.createStatement(localLinker, RDFS.LABEL, label, graphIRI));
			statements.add(f.createStatement(localLinker, RDFS.COMMENT, comment, graphIRI));
			
			sparqlDAO.addStatements(statements, graphIRI);
			// TODO add this linker's name as an alias to the global one???
		}
		
		return linkerURI;
	}

	String addSmallMoleculeLinker (SmallMoleculeLinker l, String graph) throws SparqlException {
		
		String linkerURI;
		ValueFactory f = sparqlDAO.getValueFactory();
		
		// check if the linker already exists in "default-graph"
		String existing = null;
		if (l.getPubChemId() != null) {
			existing = getLinkerByField(l.getPubChemId().toString(), hasPubChemIdProperty, "long");
		} else if (l.getInChiKey() != null) {
			existing = getLinkerByField(l.getInChiKey(), hasInchiKeyProperty, "string");
		}
	
		if (existing == null) {
			linkerURI = generateUniqueURI(uriPrefix + "L");
			
			IRI linker = f.createIRI(linkerURI);
			IRI graphIRI = f.createIRI(graph);
			IRI hasInchiSequence = f.createIRI(hasInchiSequencePredicate);
			IRI hasInchiKey = f.createIRI(hasInchiKeyPredicate);
			IRI hasIupacName = f.createIRI(hasIupacNamePredicate);
			IRI hasMass = f.createIRI(hasMassPredicate);
			IRI hasImageUrl = f.createIRI(hasImageUrlPredicate);
			IRI hasPubChemId = f.createIRI(hasPubChemIdPredicate);
			IRI hasMolecularFormula = f.createIRI(hasMolecularFormulaPredicate);
			IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
			IRI hasClassification = f.createIRI(hasClassificationPredicate);
			IRI hasChebiId = f.createIRI(hasChebiIdPredicate);
			IRI hasClassificationValue = f.createIRI(hasClassificationValuePredicate);
			IRI opensRing = f.createIRI(opensRingPredicate);
			IRI hasDescription = f.createIRI(hasDescriptionPredicate);
			IRI hasUrl = f.createIRI(hasURLPredicate);
			
			IRI linkerType = f.createIRI(linkerTypePredicate);
			IRI hasLinkerType = f.createIRI(hasTypePredicate);
			Literal type = f.createLiteral(l.getType().name());
			Literal label = l.getName() == null ? f.createLiteral("") : f.createLiteral(l.getName());
			Literal comment = l.getComment() == null ? f.createLiteral("") : f.createLiteral(l.getComment());
			Literal description = null;
			if (l.getDescription() != null)
				description = f.createLiteral(l.getDescription());
			
			IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
			IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
			
			Literal pubChemId = null;
			if (l.getPubChemId() != null)
				pubChemId =  f.createLiteral(l.getPubChemId());
			Literal inchiSequence = null;
			if (l.getInChiSequence() != null)
				inchiSequence = f.createLiteral(l.getInChiSequence());
			Literal inchiKey = null;
			if (l.getInChiKey() != null)
				inchiKey = f.createLiteral(l.getInChiKey());
			Literal imageUrl = null;
			if (l.getImageURL() != null) 
				imageUrl =  f.createLiteral(l.getImageURL());
			Literal mass = null;
			if (l.getMass() != null) 
				mass =  f.createLiteral(l.getMass());
			Literal molecularFormula = null;
			if (l.getMolecularFormula() != null)
				molecularFormula = f.createLiteral(l.getMolecularFormula());
			Literal iupacName = null;
			if (l.getIupacName() != null) 
				iupacName = f.createLiteral(l.getIupacName());
		
			Literal opensRingValue = l.getOpensRing() == null ? f.createLiteral(2) : f.createLiteral(l.getOpensRing());
			Literal date = f.createLiteral(new Date());
			
			List<Statement> statements = new ArrayList<Statement>();
			statements.add(f.createStatement(linker, RDF.TYPE, linkerType, graphIRI));
			statements.add(f.createStatement(linker, hasLinkerType, type, graphIRI));
			statements.add(f.createStatement(linker, RDFS.LABEL, label, graphIRI));
			statements.add(f.createStatement(linker, RDFS.COMMENT, comment, graphIRI));
			statements.add(f.createStatement(linker, hasAddedToLibrary, date, graphIRI));
			statements.add(f.createStatement(linker, hasModifiedDate, date, graphIRI));
			statements.add(f.createStatement(linker, hasCreatedDate, date, graphIRI));
			statements.add(f.createStatement(linker, opensRing, opensRingValue, graphIRI));
			if (description != null) statements.add(f.createStatement(linker, hasDescription, description, graphIRI));
			if (inchiSequence != null) statements.add(f.createStatement(linker, hasInchiSequence, inchiSequence, graphIRI));
			if (inchiKey != null) statements.add(f.createStatement(linker, hasInchiKey, inchiKey, graphIRI));
			if (iupacName != null) statements.add(f.createStatement(linker, hasIupacName, iupacName, graphIRI));
			if (mass != null) statements.add(f.createStatement(linker, hasMass, mass, graphIRI));
			if (imageUrl != null) statements.add(f.createStatement(linker, hasImageUrl, imageUrl, graphIRI));
			if (pubChemId != null) statements.add(f.createStatement(linker, hasPubChemId, pubChemId, graphIRI));
			if (molecularFormula != null) statements.add(f.createStatement(linker, hasMolecularFormula, molecularFormula, graphIRI));
			
			if (l.getClassification() != null) {
				String classificationIRI = null;
				if (l.getClassification().getUri() != null) {
					classificationIRI = l.getClassification().getUri();
				}
				else {
					//TODO search for existing classification first
					classificationIRI = generateUniqueURI(uriPrefix + "LC");
				}
				IRI classification = f.createIRI(classificationIRI);
				statements.add(f.createStatement(linker, hasClassification, classification, graphIRI));
				if (l.getClassification().getChebiId() != null) {
					Literal chebiId = f.createLiteral(l.getClassification().getChebiId());
					Literal value = f.createLiteral(l.getClassification().getClassification());
					statements.add(f.createStatement(classification, hasChebiId, chebiId, graphIRI));
					statements.add(f.createStatement(classification, hasClassificationValue, value, graphIRI));
				}
			}
			
			if (l.getUrls() != null) {
				for (String url: l.getUrls()) {
					Literal urlLit = f.createLiteral(url);
					statements.add(f.createStatement(linker, hasUrl, urlLit, graphIRI));
				}
			}
			
			sparqlDAO.addStatements(statements, graphIRI);
			
		} else {
			logger.debug("The linker already exists in global repository. URI: " + existing);
			linkerURI = existing;
			// add has_public_uri to point to the global one, add details to local
			
			IRI linker = f.createIRI(linkerURI);
			
			linkerURI = generateUniqueURI(uriPrefix) + "L";
			IRI localLinker = f.createIRI(linkerURI);
			IRI graphIRI = f.createIRI(graph);
			IRI hasPublicURI = f.createIRI(hasPublicURIPredicate);
			Literal date = f.createLiteral(new Date());
			List<Statement> statements = new ArrayList<Statement>();
			IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
			IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
			Literal label = l.getName() == null ? f.createLiteral("") : f.createLiteral(l.getName());
			Literal comment = l.getComment() == null ? f.createLiteral("") : f.createLiteral(l.getComment());
			IRI linkerType = f.createIRI(linkerTypePredicate);
			IRI hasLinkerType = f.createIRI(hasTypePredicate);
			Literal type = f.createLiteral(l.getType().name());
			
			statements.add(f.createStatement(linker, RDF.TYPE, linkerType, graphIRI));
			statements.add(f.createStatement(linker, hasLinkerType, type, graphIRI));
			statements.add(f.createStatement(localLinker, hasPublicURI, linker, graphIRI));
			statements.add(f.createStatement(localLinker, hasAddedToLibrary, date, graphIRI));
			statements.add(f.createStatement(localLinker, hasModifiedDate, date, graphIRI));
			statements.add(f.createStatement(localLinker, RDFS.LABEL, label, graphIRI));
			statements.add(f.createStatement(localLinker, RDFS.COMMENT, comment, graphIRI));
			
			sparqlDAO.addStatements(statements, graphIRI);
			// TODO add this linker's name as an alias to the global one???
		}
		
		return linkerURI;
	}
	
	@Override
	public void deleteLinker(String linkerId, UserEntity user) throws SQLException, SparqlException {
		String graph;
		
		graph = getGraphForUser(user);
		if (graph != null) {
			// check to see if the given linkerId is in this graph
			Linker existing = getLinkerFromURI (uriPrefix + linkerId, graph);
			if (existing != null) {
				//TODO what to do with linkerClassifications
				deleteByURI (uriPrefix + linkerId, graph);
				return;
			}
		}
	}
	
	private String findLinkerInGraphByField (String field, String predicate, String type, String graph) throws SparqlException {
		String fromString = "FROM <" + DEFAULT_GRAPH + ">\n";
		String whereClause = "WHERE {";
		String where = " { " + 
				"				    ?s gadr:" + predicate + "\"" + field + "\"^^xsd:" + type + ".\n";
		if (!graph.equals(DEFAULT_GRAPH)) {
			// check if the user's private graph has this glycan
			fromString += "FROM <" + graph + ">\n";
			where += "              ?s gadr:has_date_addedtolibrary ?d .\n }";
			where += "  UNION { ?s gadr:has_date_addedtolibrary ?d .\n"
					+ " ?s gadr:has_public_uri ?p . \n" 
					+ "?p gadr:" + predicate + "\"" + field + "\"^^xsd:" + type + ".\n}";
			
		} else {
			where += "}";
		}
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?s \n");
		queryBuf.append (fromString);
		queryBuf.append (whereClause + where + 
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
	public Linker getLinkerByLabel(String label, UserEntity user) throws SparqlException, SQLException {
		if (label == null || label.isEmpty())
			return null;
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


	@Override
	public String getLinkerByField(String field, String predicate, String type) throws SparqlException {
		return findLinkerInGraphByField(field, predicate, type, DEFAULT_GRAPH);
	}
	
	@Override
	public String getLinkerByField (String field, String predicate, String type, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		return findLinkerInGraphByField (field, predicate, type, graph);
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
				sortLine = "OPTIONAL {?s " + sortPredicate + " ?sortBy } .\n";	
			String orderByLine = " ORDER BY " 
			        + (order == 0 ? " DESC" : " ASC") + (sortPredicate == null ? "(?s)": "(?sortBy)");
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
		else if (field.equalsIgnoreCase("uniProtId")) 
			return "gadr:has_uniProtId";
		else if (field.equalsIgnoreCase("pdbId"))
			return "gadr:has_pdbId";
		else if (field.equalsIgnoreCase("sequence"))
			return "gadr:has_sequence";
		
		return null;
	}
	
	@Override
	public int getLinkerCountByUser(UserEntity user) throws SQLException, SparqlException {
		String graph = getGraphForUser(user);
		return getCountByUserByType (graph, "Linker");
	}
	
	private LinkerType getLinkerTypeForLinker (String linkerURI, String graph) throws SparqlException {
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?t \n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n");
		queryBuf.append ("<" +  linkerURI + "> gadr:has_type ?t . }");

		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.isEmpty())
			return null;
		else {
			String type = results.get(0).getValue("t");
			return LinkerType.valueOf(type);
		}
	}
	
	@Override
	public Linker getLinkerFromURI(String linkerURI, String graph) throws SparqlException {
		Linker linkerObject = null;
		
		LinkerType type = getLinkerTypeForLinker(linkerURI, graph);
		
		ValueFactory f = sparqlDAO.getValueFactory();
		
		IRI linker = f.createIRI(linkerURI);
		IRI graphIRI = f.createIRI(graph);
		IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
		IRI hasPublicURI = f.createIRI(hasPublicURIPredicate);
		IRI hasSequence = f.createIRI(hasSequencePredicate);
		IRI hasPdbId = f.createIRI(hasPdbIdPredicate);
		IRI hasUniprotId = f.createIRI(hasUniprotIdPredicate);
		IRI hasInchiSequence = f.createIRI(hasInchiSequencePredicate);
		IRI hasInchiKey = f.createIRI(hasInchiKeyPredicate);
		IRI hasIupacName = f.createIRI(hasIupacNamePredicate);
		IRI hasMass = f.createIRI(hasMassPredicate);
		IRI hasImageUrl = f.createIRI(hasImageUrlPredicate);
		IRI hasPubChemId = f.createIRI(hasPubChemIdPredicate);
		IRI hasMolecularFormula = f.createIRI(hasMolecularFormulaPredicate);
		IRI hasClassification = f.createIRI(hasClassificationPredicate);
		IRI hasChebiId = f.createIRI(hasChebiIdPredicate);
		IRI hasClassificationValue = f.createIRI(hasClassificationValuePredicate);
		IRI opensRing = f.createIRI(opensRingPredicate);
		IRI hasDescription = f.createIRI(hasDescriptionPredicate);
		IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
		IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
		IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
		IRI hasUrl = f.createIRI(hasURLPredicate);
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(linker, null, null, graphIRI);
		if (statements.hasNext()) {
			switch (type) {
			case PEPTIDE_LINKER:
				linkerObject = new PeptideLinker();
				break;
			case PROTEIN_LINKER:
				linkerObject = new ProteinLinker();
				break;
			case SMALLMOLECULE_LINKER:
				linkerObject = new SmallMoleculeLinker();
				break;
			}
			
			linkerObject.setUri(linkerURI);
			linkerObject.setId(linkerURI.substring(linkerURI.lastIndexOf("/")+1));
		}
		List<String> urls = new ArrayList<String>();
		while (statements.hasNext()) {
			Statement st = statements.next();
			if (st.getPredicate().equals(hasInchiSequence)) {
				Value seq = st.getObject();
				if (linkerObject instanceof SmallMoleculeLinker)
					((SmallMoleculeLinker)linkerObject).setInChiSequence(seq.stringValue()); 
			} else if (st.getPredicate().equals(hasInchiKey)) {
				Value val = st.getObject();
				if (linkerObject instanceof SmallMoleculeLinker)
					((SmallMoleculeLinker)linkerObject).setInChiKey(val.stringValue()); 
			} else if (st.getPredicate().equals(hasIupacName)) {
				Value val = st.getObject();
				if (linkerObject instanceof SmallMoleculeLinker)
					((SmallMoleculeLinker)linkerObject).setIupacName(val.stringValue()); 
			} else if (st.getPredicate().equals(hasImageUrl)) {
				Value val = st.getObject();
				if (linkerObject instanceof SmallMoleculeLinker)
					((SmallMoleculeLinker)linkerObject).setImageURL(val.stringValue()); 
			} else if (st.getPredicate().equals(hasPubChemId)) {
				Value val = st.getObject();
				if (val != null) {
					if (linkerObject instanceof SmallMoleculeLinker)
						((SmallMoleculeLinker)linkerObject).setPubChemId(Long.parseLong(val.stringValue())); 
				}
			} else if (st.getPredicate().equals(hasMolecularFormula)) {
				Value val = st.getObject();
				if (linkerObject instanceof SmallMoleculeLinker)
					((SmallMoleculeLinker)linkerObject).setMolecularFormula(val.stringValue()); 
			} else if (st.getPredicate().equals(hasMass)) {
				Value mass = st.getObject();
				try {
					if (mass != null && mass.stringValue() != null && !mass.stringValue().isEmpty()) {
						if (linkerObject instanceof SmallMoleculeLinker)
							((SmallMoleculeLinker)linkerObject).setMass(Double.parseDouble(mass.stringValue())); 
					}
				} catch (NumberFormatException e) {
					logger.warn ("Glycan mass is invalid", e);
				}
			} else if (st.getPredicate().equals(hasPdbId)) {
				Value val = st.getObject();
				if (linkerObject instanceof ProteinLinker)
					((ProteinLinker)linkerObject).setPdbId(val.stringValue()); 
			} else if (st.getPredicate().equals(hasUniprotId)) {
				Value val = st.getObject();
				if (linkerObject instanceof ProteinLinker)
					((ProteinLinker)linkerObject).setUniProtId(val.stringValue()); 
			} else if (st.getPredicate().equals(hasSequence)) {
				Value val = st.getObject();
				if (linkerObject instanceof ProteinLinker)
					((ProteinLinker)linkerObject).setSequence(val.stringValue()); 
				else if (linkerObject instanceof PeptideLinker) 
					((PeptideLinker)linkerObject).setSequence(val.stringValue()); 
			} else if (st.getPredicate().equals(hasCreatedDate)) {
				Value value = st.getObject();
			    if (value instanceof Literal) {
			    	Literal literal = (Literal)value;
			    	XMLGregorianCalendar calendar = literal.calendarValue();
			    	Date date = calendar.toGregorianCalendar().getTime();
			    	linkerObject.setDateCreated(date);
			    }
			} else if (st.getPredicate().equals(RDFS.LABEL)) {
				Value label = st.getObject();
				linkerObject.setName(label.stringValue());
			} else if (st.getPredicate().equals(RDFS.COMMENT)) {
				Value comment = st.getObject();
				linkerObject.setComment(comment.stringValue());
			} else if (st.getPredicate().equals(hasDescription)) {
				Value comment = st.getObject();
				linkerObject.setDescription(comment.stringValue());
			} else if (st.getPredicate().equals(opensRing)) {
				Value val = st.getObject();
				if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
					linkerObject.setOpensRing(Integer.parseInt(val.stringValue()));
				}
			} else if (st.getPredicate().equals(hasUrl)) {
				Value val = st.getObject();
				if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
					urls.add(val.stringValue());
				}
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
			} else if (st.getPredicate().equals(hasClassification)) {
				if (linkerObject instanceof SmallMoleculeLinker) {
					Value classification = st.getObject();
					String classificationURI = classification.stringValue();
					IRI cls = f.createIRI(classificationURI);
					LinkerClassification linkerCls = new LinkerClassification();
					linkerCls.setUri(classificationURI);
					((SmallMoleculeLinker) linkerObject).setClassification(linkerCls);
					RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(cls, null, null, graphIRI);
					while (statements2.hasNext()) {
						Statement st2 = statements2.next();
						if (st2.getPredicate().equals(hasClassificationValue)) {
							Value valueString = st2.getObject();
							linkerCls.setClassification(valueString.stringValue());
						} else if (st2.getPredicate().equals(hasChebiId)) {
							Value val = st2.getObject();
							if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
								linkerCls.setChebiId(Integer.parseInt(val.stringValue()));
							}
						}  
					}
				}
			} else if (st.getPredicate().equals(hasPublicURI)) {
				// need to retrieve additional information from DEFAULT graph
				Value uriValue = st.getObject();
				String publicLinkerURI = uriValue.stringValue();
				IRI publicLinker = f.createIRI(publicLinkerURI);
				RepositoryResult<Statement> statementsPublic = sparqlDAO.getStatements(publicLinker, null, null, defaultGraphIRI);
				while (statementsPublic.hasNext()) {
					Statement stPublic = statementsPublic.next();
					
					if (stPublic.getPredicate().equals(hasInchiSequence)) {
						Value seq = stPublic.getObject();
						if (linkerObject instanceof SmallMoleculeLinker)
							((SmallMoleculeLinker)linkerObject).setInChiSequence(seq.stringValue()); 
					} else if (stPublic.getPredicate().equals(hasInchiKey)) {
						Value val = stPublic.getObject();
						if (linkerObject instanceof SmallMoleculeLinker)
							((SmallMoleculeLinker)linkerObject).setInChiKey(val.stringValue()); 
					} else if (stPublic.getPredicate().equals(hasIupacName)) {
						Value val = stPublic.getObject();
						if (linkerObject instanceof SmallMoleculeLinker)
							((SmallMoleculeLinker)linkerObject).setIupacName(val.stringValue()); 
					} else if (stPublic.getPredicate().equals(hasImageUrl)) {
						Value val = stPublic.getObject();
						if (linkerObject instanceof SmallMoleculeLinker)
							((SmallMoleculeLinker)linkerObject).setImageURL(val.stringValue()); 
					} else if (stPublic.getPredicate().equals(hasPubChemId)) {
						Value val = stPublic.getObject();
						if (val != null) {
							if (linkerObject instanceof SmallMoleculeLinker)
								((SmallMoleculeLinker)linkerObject).setPubChemId(Long.parseLong(val.stringValue())); 
						}
					} else if (stPublic.getPredicate().equals(hasMolecularFormula)) {
						Value val = stPublic.getObject();
						if (linkerObject instanceof SmallMoleculeLinker)
							((SmallMoleculeLinker)linkerObject).setMolecularFormula(val.stringValue()); 
					} else if (stPublic.getPredicate().equals(hasMass)) {
						Value mass = stPublic.getObject();
						try {
							if (mass != null && mass.stringValue() != null && !mass.stringValue().isEmpty()) {
								if (linkerObject instanceof SmallMoleculeLinker)
									((SmallMoleculeLinker)linkerObject).setMass(Double.parseDouble(mass.stringValue())); 
							}
						} catch (NumberFormatException e) {
							logger.warn ("Glycan mass is invalid", e);
						}
					} else if (stPublic.getPredicate().equals(hasPdbId)) {
						Value val = stPublic.getObject();
						if (linkerObject instanceof ProteinLinker)
							((ProteinLinker)linkerObject).setPdbId(val.stringValue()); 
					} else if (stPublic.getPredicate().equals(hasUniprotId)) {
						Value val = stPublic.getObject();
						if (linkerObject instanceof ProteinLinker)
							((ProteinLinker)linkerObject).setUniProtId(val.stringValue()); 
					} else if (stPublic.getPredicate().equals(hasSequence)) {
						Value val = stPublic.getObject();
						if (linkerObject instanceof ProteinLinker)
							((ProteinLinker)linkerObject).setSequence(val.stringValue()); 
						else if (linkerObject instanceof PeptideLinker) 
							((PeptideLinker)linkerObject).setSequence(val.stringValue()); 
					} else if (stPublic.getPredicate().equals(hasDescription)) {
						Value comment = stPublic.getObject();
						linkerObject.setDescription(comment.stringValue());
					} else if (stPublic.getPredicate().equals(opensRing)) {
						Value val = stPublic.getObject();
						if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
							linkerObject.setOpensRing(Integer.parseInt(val.stringValue()));
						}
					} else if (stPublic.getPredicate().equals(hasClassification)) {
						if (linkerObject instanceof SmallMoleculeLinker) {
							Value classification = stPublic.getObject();
							String classificationURI = classification.stringValue();
							IRI cls = f.createIRI(classificationURI);
							LinkerClassification linkerCls = new LinkerClassification();
							((SmallMoleculeLinker) linkerObject).setClassification(linkerCls);
							RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(cls, null, null, defaultGraphIRI);
							while (statements2.hasNext()) {
								Statement st2 = statements2.next();
								if (st2.getPredicate().equals(hasClassificationValue)) {
									Value valueString = st2.getObject();
									linkerCls.setClassification(valueString.stringValue());
								} else if (st2.getPredicate().equals(hasChebiId)) {
									Value val = st2.getObject();
									if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
										linkerCls.setChebiId(Integer.parseInt(val.stringValue()));
									}
								}  
							}
						}
					}
				}
			}
		}
		
		if (!urls.isEmpty() && linkerObject != null)
			linkerObject.setUrls(urls);
		
		return linkerObject;
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
		IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
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
}