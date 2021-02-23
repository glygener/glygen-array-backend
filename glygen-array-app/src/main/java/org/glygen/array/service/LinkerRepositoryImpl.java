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
import org.glygen.array.persistence.rdf.Creator;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.LinkerClassification;
import org.glygen.array.persistence.rdf.LinkerType;
import org.glygen.array.persistence.rdf.PeptideLinker;
import org.glygen.array.persistence.rdf.ProteinLinker;
import org.glygen.array.persistence.rdf.Publication;
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
	final static String hasSmilesPredicate = ontPrefix + "has_smiles";
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
	final static String createdByPredicate = ontPrefix + "created_by";
	
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
		
	
	private void addLinkerPublications(Linker l, String uri, String graph) throws SparqlException {
	    String uriPre = uriPrefix;
	    if (graph.equals(DEFAULT_GRAPH)) {
	        uriPre = uriPrefixPublic;
	    }
	    ValueFactory f = sparqlDAO.getValueFactory();
	    
	    IRI linker = f.createIRI(uri);
	    IRI graphIRI = f.createIRI(graph);
        IRI hasTitle = f.createIRI(hasTitlePredicate);
        IRI hasAuthor = f.createIRI(hasAuthorPredicate);
        IRI hasYear = f.createIRI(hasYearPredicate);
        IRI hasVolume = f.createIRI(hasVolumePredicate);
        IRI hasJournal = f.createIRI(hasJournalPredicate);
        IRI hasNumber = f.createIRI(hasNumberPredicate);
        IRI hasStartPage = f.createIRI(hasStartPagePredicate);
        IRI hasEndPage = f.createIRI(hasEndPagePredicate);
        IRI hasDOI = f.createIRI(hasDOIPredicate);
        IRI hasPubMed = f.createIRI(hasPubMedPredicate);
        IRI hasPub = f.createIRI(hasPublication);
        
	    
	    if (l.getPublications() != null) {
	        for (Publication pub : l.getPublications()) {
	            List<Statement> statements = new ArrayList<Statement>();
	            String publicationURI = generateUniqueURI(uriPre + "P", graph);
	            IRI publication = f.createIRI(publicationURI);
	            Literal title = pub.getTitle() == null ? f.createLiteral("") : f.createLiteral(pub.getTitle());
	            Literal authors = pub.getAuthors() == null ? f.createLiteral("") : f.createLiteral(pub.getAuthors());
	            Literal number = pub.getNumber() == null ? f.createLiteral("") : f.createLiteral(pub.getNumber());
	            Literal volume = pub.getVolume() == null ? f.createLiteral("") : f.createLiteral(pub.getVolume());
	            Literal year = pub.getYear() == null ? f.createLiteral("") : f.createLiteral(pub.getYear());
	            Literal journal = pub.getJournal() == null ? f.createLiteral("") : f.createLiteral(pub.getJournal());
	            Literal startPage = pub.getStartPage() == null ? f.createLiteral("") : f.createLiteral(pub.getStartPage());
	            Literal endPage = pub.getEndPage() == null ? f.createLiteral("") : f.createLiteral(pub.getEndPage());
	            Literal pubMed = pub.getPubmedId() == null ? f.createLiteral("") : f.createLiteral(pub.getPubmedId());
	            Literal doi = pub.getDoiId() == null ? f.createLiteral("") : f.createLiteral(pub.getDoiId());
	            
	            if (title != null) statements.add(f.createStatement(publication, hasTitle, title, graphIRI));
	            if (authors != null) statements.add(f.createStatement(publication, hasAuthor, authors, graphIRI));
	            if (number != null) statements.add(f.createStatement(publication, hasNumber, number, graphIRI));
	            if (volume != null) statements.add(f.createStatement(publication, hasVolume, volume, graphIRI));
	            if (journal != null) statements.add(f.createStatement(publication, hasJournal, journal, graphIRI));
	            if (startPage != null) statements.add(f.createStatement(publication, hasStartPage, startPage, graphIRI));
	            if (endPage != null) statements.add(f.createStatement(publication, hasEndPage, endPage, graphIRI));
	            if (year != null) statements.add(f.createStatement(publication, hasYear, year, graphIRI));
	            if (pubMed != null) statements.add(f.createStatement(publication, hasPubMed, pubMed, graphIRI));
	            if (doi != null) statements.add(f.createStatement(publication, hasDOI, doi, graphIRI));
	            
	            statements.add(f.createStatement(linker, hasPub, publication, graphIRI));
	            sparqlDAO.addStatements(statements, graphIRI);
	        }
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
			linkerURI = generateUniqueURI(uriPrefix + "L", graph);
			
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
				if (((ProteinLinker)l).getPdbIds() != null) {
				    for (String pdbId: ((ProteinLinker)l).getPdbIds()) {
				        IRI hasPDBId = f.createIRI(hasPdbIdPredicate);
				        Literal pdb = f.createLiteral(pdbId);
				        statements.add(f.createStatement(linker, hasPDBId, pdb, graphIRI));
				    }
				}
			}
			
			if (l.getUrls() != null) {
				for (String url: l.getUrls()) {
					Literal urlLit = f.createLiteral(url);
					statements.add(f.createStatement(linker, hasUrl, urlLit, graphIRI));
				}
			}
			
			sparqlDAO.addStatements(statements, graphIRI);
			
			if (l.getPublications() != null && !l.getPublications().isEmpty()) {
	            addLinkerPublications(l, linkerURI, graph);
	        }
		} else {
			logger.debug("The linker already exists in global repository. URI: " + existing);
			linkerURI = existing;
			// add has_public_uri to point to the global one, add details to local
			
			IRI linker = f.createIRI(linkerURI);
			
			linkerURI = generateUniqueURI(uriPrefix + "L", graph);
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
			linkerURI = generateUniqueURI(uriPrefix + "L", graph);
			
			IRI linker = f.createIRI(linkerURI);
			IRI graphIRI = f.createIRI(graph);
			IRI hasInchiSequence = f.createIRI(hasInchiSequencePredicate);
			IRI hasInchiKey = f.createIRI(hasInchiKeyPredicate);
			IRI hasIupacName = f.createIRI(hasIupacNamePredicate);
			IRI hasMass = f.createIRI(hasMassPredicate);
			IRI hasImageUrl = f.createIRI(hasImageUrlPredicate);
			IRI hasPubChemId = f.createIRI(hasPubChemIdPredicate);
			IRI hasMolecularFormula = f.createIRI(hasMolecularFormulaPredicate);
			IRI hasSmiles = f.createIRI(hasSmilesPredicate);
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
			Literal smiles = null;
			if (l.getSmiles() != null) 
			    smiles = f.createLiteral(l.getSmiles());
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
			if (smiles != null) statements.add(f.createStatement(linker, hasSmiles, smiles, graphIRI));
			
			if (l.getClassification() != null) {
				String classificationIRI = null;
				if (l.getClassification().getUri() != null) {
					classificationIRI = l.getClassification().getUri();
				}
				else {
				    if (l.getClassification().getChebiId() != null) {
				        classificationIRI = getClassificationByField(
				                l.getClassification().getChebiId() + "", 
				                hasChebiIdPredicate.substring(hasChebiIdPredicate.lastIndexOf("#")+1), "integer", graph);
				    } 
				    if (classificationIRI == null && l.getClassification().getClassification() != null) {
				        classificationIRI = getClassificationByField(
                                l.getClassification().getClassification(), 
                                hasClassificationValuePredicate.substring(hasClassificationValuePredicate.lastIndexOf("#")+1), "string", graph);
				    }
				    if (classificationIRI == null) {
				        classificationIRI = generateUniqueURI(uriPrefix + "LC", graph);
				    } 
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
			
			if (l.getPublications() != null && !l.getPublications().isEmpty()) {
	            addLinkerPublications(l, linkerURI, graph);
	        }
			
		} else {
			logger.debug("The linker already exists in global repository. URI: " + existing);
			linkerURI = existing;
			// add has_public_uri to point to the global one, add details to local
			
			IRI linker = f.createIRI(linkerURI);
			
			linkerURI = generateUniqueURI(uriPrefix + "L", graph);
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
	
	String getClassificationByField (String field, String predicate, String type, String graph) throws SparqlException {
	    
	    String fromString = "FROM <" + DEFAULT_GRAPH + ">\n";
        String whereClause = "WHERE {";
        String where = " { " + 
                "                   ?s gadr:" + predicate + "\"" + field + "\"^^xsd:" + type + " . }";
        if (!graph.equals(DEFAULT_GRAPH)) {
            // check if the user's private graph has this glycan
            fromString += "FROM <" + graph + ">\n";
            where += "  UNION { "
                    + " ?s gadr:has_public_uri ?p . \n" 
                    + "?p gadr:" + predicate + " \"" + field + "\"^^xsd:" + type + ".\n}";
            
        } else {
            where += "}";
        }
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        queryBuf.append (fromString);
        queryBuf.append (whereClause + where + 
                "               }\n" + 
                "               LIMIT 10");
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        if (results.size() == 0) 
            return null;
        
        SparqlEntity result = results.get(0);
        String classificationURI = result.getValue("s");
        
        return classificationURI;
	}
	
	
	@Override
	public void deleteLinker(String linkerId, UserEntity user) throws SQLException, SparqlException {
		String graph = null;
		String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        }
        else {
            graph = getGraphForUser(user);
        }
		if (graph != null) {
		    if (canDelete(uriPre + linkerId, graph)) {
    			// check to see if the given linkerId is in this graph
    			Linker existing = getLinkerFromURI (uriPre + linkerId, user);
    			if (existing != null) {
    			    //delete publications first, then delete the linker
    			    ValueFactory f = sparqlDAO.getValueFactory();
    		        IRI linker = f.createIRI(uriPre + linkerId);
    		        IRI graphIRI = f.createIRI(graph);
    		        IRI hasPub = f.createIRI(hasPublication);
    		        RepositoryResult<Statement> statements = sparqlDAO.getStatements(linker, hasPub, null, graphIRI);
    		        while (statements.hasNext()) {
    		            Statement st = statements.next();
    		            Value v = st.getObject();
    		            String publicationURI = v.stringValue();
    		            IRI pub = f.createIRI(publicationURI);
    		            RepositoryResult<Statement> statements1 = sparqlDAO.getStatements(pub, null, null, graphIRI);
    		            sparqlDAO.removeStatements(Iterations.asList(statements1), graphIRI); 
    		        }
    				deleteByURI (uriPre + linkerId, graph);
    				return;
    			}
		    } else {
		        throw new IllegalArgumentException("Cannot delete linker " + linkerId + ". It is used in a feature");
		    }
		}
	}
	
	boolean canDelete (String linkerURI, String graph) throws SparqlException, SQLException { 
        boolean canDelete = true;
        
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        //queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
        queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ("?s gadr:has_linker  <" +  linkerURI + "> . } LIMIT 1");
        
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        if (!results.isEmpty())
            canDelete = false;
        
        return canDelete;
    }
	
	private String findLinkerInGraphByField (String field, String predicate, String type, String graph) throws SparqlException {
		String fromString = "FROM <" + DEFAULT_GRAPH + ">\n";
		String whereClause = "WHERE {";
		String where = " { " + 
				"				    ?s gadr:" + predicate + " \"\"\"" + field + "\"\"\"^^xsd:" + type + ".\n";
		if (!graph.equals(DEFAULT_GRAPH)) {
			// check if the user's private graph has this glycan
			fromString += "FROM <" + graph + ">\n";
			where += "              ?s gadr:has_date_addedtolibrary ?d .\n }";
			where += "  UNION { ?s gadr:has_date_addedtolibrary ?d .\n"
					+ " ?s gadr:has_public_uri ?p . \n" 
					+ "?p gadr:" + predicate + " \"\"\"" + field + "\"\"\"^^xsd:" + type + ".\n}";
			
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
		
		return results.get(0).getValue("s");
	}
	
	@Override
	public Linker getLinkerById(String linkerId, UserEntity user) throws SparqlException, SQLException {
		// make sure the glycan belongs to this user
	    String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?d \n");
		//queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n");
		queryBuf.append ( "<" +  uriPrefix + linkerId + "> gadr:has_date_addedtolibrary ?d . }\n");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.isEmpty())
			return null;
		else {
			return getLinkerFromURI(uriPrefix + linkerId, user);
		}
	}

	@Override
	public Linker getLinkerByLabel(String label, UserEntity user) throws SparqlException, SQLException {
		if (label == null || label.isEmpty())
			return null;
		String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
		List<SparqlEntity> results = retrieveLinkerByLabel(label, graph);
		if (results.isEmpty())
			return null;
		else {
		    for (SparqlEntity result: results) {
    		    String linkerURI = result.getValue("s");
    		    if (graph.equals(DEFAULT_GRAPH) && linkerURI.contains("public")) {
    		        return getLinkerFromURI(linkerURI, null);
    		    } else if (!linkerURI.contains("public"))
    		        return getLinkerFromURI(linkerURI, user);
		    }
		}
		return null;
	}
	
	List<SparqlEntity> retrieveLinkerByLabel (String label, String graph) throws SparqlException {
	    StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
        queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ( " ?s gadr:has_date_addedtolibrary ?d . \n");
        queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/data#Linker>. \n");
        queryBuf.append ( " ?s rdfs:label ?l FILTER (lcase(str(?l)) = \"" + label.toLowerCase() + "\") \n"
                + "}\n");
        return sparqlDAO.query(queryBuf.toString());
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
		return getLinkerByUser(user, offset, limit, field, order, null);
	}

    @Override
    public String getSearchPredicate(String searchValue) {
        String predicates = "";
        
        predicates += "?s rdfs:label ?value1 .\n";
        predicates += "OPTIONAL {?s rdfs:comment ?value2} \n";
        predicates += "OPTIONAL {?s gadr:has_sequence ?value3} \n";
        predicates += "OPTIONAL {?s gadr:has_pdbId ?value4} \n";
        predicates += "OPTIONAL {?s gadr:has_uniProtId ?value5} \n";
        predicates += "OPTIONAL {?s gadr:has_inChI_sequence ?value6} \n";
        predicates += "OPTIONAL {?s gadr:has_iupac_name ?value7} \n";
        predicates += "OPTIONAL {?s gadr:has_smiles ?value8} \n";
        predicates += "OPTIONAL {?s gadr:has_mass ?value9} \n";
        predicates += "OPTIONAL {?s gadr:has_molecular_formula ?value10} \n";
        predicates += "OPTIONAL {?s gadr:has_pubchem_compound_id ?value11} \n";
        predicates += "OPTIONAL {?s gadr:has_inChI_key ?value12} \n";
        predicates += "OPTIONAL {?s gadr:has_classification ?c . ?c gadr:has_classification_value ?value13 . ?c gadr:has_chEBI ?value14} \n";
        predicates += "OPTIONAL {?s gadr:has_type ?value15} \n";
       
        int numberOfValues = 16; // need to match the total values used above value1 - value15
        String filterClause = "filter (";
        for (int i=1; i < numberOfValues; i++) {
            filterClause += "regex (str(?value" + i + "), '" + searchValue + "', 'i')";
            if (i + 1 < numberOfValues)
                filterClause += " || ";
        }
        filterClause += ")\n";
            
        predicates += filterClause;
        return predicates;
    }


    @Override
    public List<Linker> getLinkerByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue) throws SparqlException, SQLException {
        List<Linker> linkers = new ArrayList<Linker>();
        
        String sortPredicate = getSortPredicateForLinker (field);
        String searchPredicate = "";
        if (searchValue != null) {
            searchPredicate = getSearchPredicate(searchValue);
        }
        
        // get all linkerURIs from user's private graph
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        if (graph != null) {
            String sortLine = "";
            if (sortPredicate != null)
                sortLine = "OPTIONAL {?s " + sortPredicate + " ?sortBy } .\n";  
            String orderByLine = " ORDER BY " 
                    + (order == 0 ? " DESC" : " ASC") + (sortPredicate == null ? "(?s)": "(?sortBy)");
            StringBuffer queryBuf = new StringBuffer();
            queryBuf.append (prefix + "\n");
            queryBuf.append ("SELECT DISTINCT ?s \n");
            //queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
            queryBuf.append ("FROM <" + graph + ">\n");
            queryBuf.append ("WHERE {\n");
            queryBuf.append (" ?s gadr:has_date_addedtolibrary ?d .\n" +
                    " ?s rdf:type  <http://purl.org/gadr/data#Linker>. \n" +
                    sortLine + searchPredicate + "}\n" +
                     orderByLine + 
                    ((limit == -1) ? " " : " LIMIT " + limit) +
                    " OFFSET " + offset);
            
            List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
            
            for (SparqlEntity sparqlEntity : results) {
                String linkerURI = sparqlEntity.getValue("s");
                Linker linker = getLinkerFromURI(linkerURI, user);
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
		else if (field.equalsIgnoreCase("smiles"))
            return "gadr:has_smiles";
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
		else if (field.equalsIgnoreCase("type"))
            return "gadr:has_type";
		
		return null;
	}
	
	@Override
	public int getLinkerCountByUser(UserEntity user) throws SQLException, SparqlException {
	    String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
		return getCountByUserByType (graph, linkerTypePredicate);
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
	public Linker getLinkerFromURI(String linkerURI, UserEntity user) throws SparqlException, SQLException {
		Linker linkerObject = null;
		
		String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            if (linkerURI.contains("public"))
                graph = DEFAULT_GRAPH;
            else
                graph = getGraphForUser(user);
        }
        
		LinkerType type = getLinkerTypeForLinker(linkerURI, graph);
		
		ValueFactory f = sparqlDAO.getValueFactory();
		
		IRI linker = f.createIRI(linkerURI);
		IRI graphIRI = f.createIRI(graph);
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(linker, null, null, graphIRI);
		if (statements.hasNext()) {
			switch (type) {
			case PEPTIDE_LINKER:
				linkerObject = new PeptideLinker();
				break;
			case PROTEIN_LINKER:
				linkerObject = new ProteinLinker();
				((ProteinLinker) linkerObject).setPdbIds(new ArrayList<String>());
				break;
			case SMALLMOLECULE_LINKER:
				linkerObject = new SmallMoleculeLinker();
				break;
			}
			
			linkerObject.setUri(linkerURI);
			linkerObject.setId(linkerURI.substring(linkerURI.lastIndexOf("/")+1));
			if (user != null) {
    			Creator owner = new Creator ();
                owner.setUserId(user.getUserId());
                owner.setName(user.getUsername());
                linkerObject.setUser(owner);
			} else {
			    linkerObject.setIsPublic(true);
			}
			linkerObject.setUrls(new ArrayList<String>());
	        linkerObject.setPublications(new ArrayList<>());
	        extractFromStatements (statements, linkerObject, graph);
		}
		
		return linkerObject;
	}
	
	private void extractFromStatements (RepositoryResult<Statement> statements, Linker linkerObject, String graph) {
	    ValueFactory f = sparqlDAO.getValueFactory();
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
        IRI hasSmiles = f.createIRI(hasSmilesPredicate);
        IRI hasClassification = f.createIRI(hasClassificationPredicate);
        IRI hasChebiId = f.createIRI(hasChebiIdPredicate);
        IRI hasClassificationValue = f.createIRI(hasClassificationValuePredicate);
        IRI opensRing = f.createIRI(opensRingPredicate);
        IRI hasDescription = f.createIRI(hasDescriptionPredicate);
        IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
        IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
        IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
        IRI hasUrl = f.createIRI(hasURLPredicate);
        IRI hasPub = f.createIRI(hasPublication);
        
        IRI hasTitle = f.createIRI(hasTitlePredicate);
        IRI hasAuthor = f.createIRI(hasAuthorPredicate);
        IRI hasYear = f.createIRI(hasYearPredicate);
        IRI hasVolume = f.createIRI(hasVolumePredicate);
        IRI hasJournal = f.createIRI(hasJournalPredicate);
        IRI hasNumber = f.createIRI(hasNumberPredicate);
        IRI hasStartPage = f.createIRI(hasStartPagePredicate);
        IRI hasEndPage = f.createIRI(hasEndPagePredicate);
        IRI hasDOI = f.createIRI(hasDOIPredicate);
        IRI hasPubMed = f.createIRI(hasPubMedPredicate);
        IRI createdBy= f.createIRI(createdByPredicate);
        
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
            } else if (st.getPredicate().equals(hasSmiles)) {
                Value val = st.getObject();
                if (linkerObject instanceof SmallMoleculeLinker)
                    ((SmallMoleculeLinker)linkerObject).setSmiles(val.stringValue()); 
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
                    ((ProteinLinker) linkerObject).getPdbIds().add(val.stringValue()); 
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
            } else if (st.getPredicate().equals(createdBy)) {
                Value label = st.getObject();
                Creator creator = new Creator();
                creator.setName(label.stringValue());
                linkerObject.setUser(creator);
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
                    linkerObject.getUrls().add(val.stringValue());
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
            } else if (st.getPredicate().equals(hasPub)) {
                Value pub = st.getObject();
                String pubURI = pub.stringValue();
                IRI p = f.createIRI(pubURI);
                Publication publication = new Publication();
                publication.setUri(pubURI);
                linkerObject.getPublications().add(publication);
                RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(p, null, null, graphIRI);
                while (statements2.hasNext()) {
                    Statement st2 = statements2.next();
                    if (st2.getPredicate().equals(hasTitle)) {
                        Value val = st2.getObject();
                        if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                            publication.setTitle(val.stringValue());
                        }
                    } else if (st2.getPredicate().equals(hasAuthor)) {
                        Value val = st2.getObject();
                        if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                            publication.setAuthors(val.stringValue());
                        }
                    } else if (st2.getPredicate().equals(hasYear)) {
                        Value val = st2.getObject();
                        if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                            publication.setYear(Integer.parseInt(val.stringValue()));
                        }
                    } else if (st2.getPredicate().equals(hasDOI)) {
                        Value val = st2.getObject();
                        if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                            publication.setDoiId(val.stringValue());
                        }
                    } else if (st2.getPredicate().equals(hasVolume)) {
                        Value val = st2.getObject();
                        if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                            publication.setVolume(val.stringValue());
                        }
                    } else if (st2.getPredicate().equals(hasJournal)) {
                        Value val = st2.getObject();
                        if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                            publication.setJournal(val.stringValue());
                        }
                    } else if (st2.getPredicate().equals(hasNumber)) {
                        Value val = st2.getObject();
                        if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                            publication.setNumber(val.stringValue());
                        }
                    } else if (st2.getPredicate().equals(hasStartPage)) {
                        Value val = st2.getObject();
                        if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                            publication.setStartPage(val.stringValue());
                        }
                    } else if (st2.getPredicate().equals(hasEndPage)) {
                        Value val = st2.getObject();
                        if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                            publication.setEndPage(val.stringValue());
                        }
                    } else if (st2.getPredicate().equals(hasPubMed)) {
                        Value val = st2.getObject();
                        if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                            publication.setPubmedId(Integer.parseInt(val.stringValue()));
                        }
                    } 
                }
            } else if (st.getPredicate().equals(hasPublicURI)) {
                // need to retrieve additional information from DEFAULT graph
                linkerObject.setIsPublic(true);
                Value uriValue = st.getObject();
                String publicLinkerURI = uriValue.stringValue();
                IRI publicLinker = f.createIRI(publicLinkerURI);
                RepositoryResult<Statement> statementsPublic = sparqlDAO.getStatements(publicLinker, null, null, defaultGraphIRI);
                extractFromStatements (statementsPublic, linkerObject, DEFAULT_GRAPH);
            }
	    }
	}
	
	@Override
	public void updateLinker(Linker g, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		Linker existing = getLinkerFromURI(g.getUri(), user);
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
	
	public String makePublic(Linker linker, UserEntity user) throws SparqlException, SQLException {
        String graph = getGraphForUser(user);
        String existingURI = null;
        
        switch (linker.getType()) {
        case SMALLMOLECULE_LINKER:
            if (((SmallMoleculeLinker) linker).getPubChemId() != null) {
                existingURI = getLinkerByField(((SmallMoleculeLinker) linker).getPubChemId().toString(), "has_pubchem_compound_id", "long");
            } else if (((SmallMoleculeLinker) linker).getInChiKey() != null) {
                existingURI = getLinkerByField(((SmallMoleculeLinker) linker).getInChiKey(), "has_inChI_key", "string");
            } else if (((SmallMoleculeLinker) linker).getSmiles() != null) {
                existingURI = getLinkerByField(((SmallMoleculeLinker) linker).getSmiles(), "has_smiles", "string");
            }
            
            break;
        case PROTEIN_LINKER:
            if (((SequenceBasedLinker) linker).getSequence() != null) {
                existingURI = getLinkerByField(((SequenceBasedLinker) linker).getSequence(), "has_sequence", "string");
            } else if (((ProteinLinker) linker).getUniProtId() != null) {
                existingURI = getLinkerByField(((ProteinLinker) linker).getUniProtId(), "has_uniProtId", "string");
            }
            break;
        case PEPTIDE_LINKER:
            if (((SequenceBasedLinker) linker).getSequence() != null) {
                existingURI = getLinkerByField(((SequenceBasedLinker) linker).getSequence(), "has_sequence", "string");
            }
            break;
        }
        
        if (existingURI == null) {
            // check by label if any
            if (linker.getName() != null && !linker.getName().isEmpty()) {
                List <SparqlEntity> results = retrieveLinkerByLabel(linker.getName(), null);
                if (results.isEmpty()) {
                    // make it public
                    deleteByURI(uriPrefix + linker.getId(), graph);
                    updateLinkerInGraph(linker, graph);
                    // need to create the linker in the public graph, link the user's version to public one
                    return addPublicLinker(linker, null, graph, user.getUsername());
                } else {
                    // same name linker exist in public graph
                    // throw exception
                    logger.debug("Linker with name " + linker.getName() + " exist in the public repository");
                    //throw new GlycanExistsException("Linker with name " + linker.getName() + " already exists in public graph");
                    return null;
                }
            } else {
                // make it public
                deleteByURI(uriPrefix + linker.getId(), graph);
                updateLinkerInGraph(linker, graph);
                // need to create the linker in the public graph, link the user's version to public one
                return addPublicLinker(linker, null, graph, user.getUsername());
            }
        } else {
            deleteByURI(uriPrefix + linker.getId(), graph);
            updateLinkerInGraph(linker, graph);
            // need to link the user's version to the existing URI
            return addPublicLinker(linker, existingURI, graph, user.getUsername());
        }
    }
    
    public String addPublicLinker (Linker linker, String publicURI, String userGraph, String creator) throws SparqlException {
        // add has_public_uri predicate to user's graph
    	boolean existing = (publicURI != null);
        ValueFactory f = sparqlDAO.getValueFactory();
        if (publicURI == null) {
            publicURI = generateUniqueURI(uriPrefixPublic + "L", userGraph);
        } 
        IRI local = f.createIRI(linker.getUri());
        IRI publicLinker = f.createIRI(publicURI);
        IRI publicGraphIRI = f.createIRI(DEFAULT_GRAPH);
        IRI graphIRI = f.createIRI(userGraph);
        IRI hasPublicURI = f.createIRI(ontPrefix + "has_public_uri");
        Literal date = f.createLiteral(new Date());
        IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
        IRI opensRing = f.createIRI(opensRingPredicate);
        IRI hasDescription = f.createIRI(hasDescriptionPredicate);
        IRI linkerType = f.createIRI(linkerTypePredicate);
        IRI hasLinkerType = f.createIRI(hasTypePredicate);
        IRI hasUrl = f.createIRI(hasURLPredicate);
        IRI createdBy= f.createIRI(ontPrefix + "created_by");
        Literal user = f.createLiteral(creator);
        
        Literal type = f.createLiteral(linker.getType().name());
        Literal label = linker.getName() == null ? f.createLiteral("") : f.createLiteral(linker.getName());
        Literal comment =linker.getComment() == null ? f.createLiteral("") : f.createLiteral(linker.getComment());
        Literal description = null;
        if (linker.getDescription() != null)
            description = f.createLiteral(linker.getDescription());
        
        IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
        IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
        Literal opensRingValue = linker.getOpensRing() == null ? f.createLiteral(2) : f.createLiteral(linker.getOpensRing());
        Literal dateAdded = f.createLiteral(linker.getDateAddedToLibrary());
        
        List<Statement> statements = new ArrayList<Statement>();
        
        if (!existing) { // otherwise, this information should already be in the public graph
	        statements.add(f.createStatement(publicLinker, RDF.TYPE, linkerType, publicGraphIRI));
	        statements.add(f.createStatement(publicLinker, hasLinkerType, type, publicGraphIRI));
	        statements.add(f.createStatement(publicLinker, RDFS.LABEL, label, publicGraphIRI));
	        statements.add(f.createStatement(publicLinker, RDFS.COMMENT, comment, publicGraphIRI));
	        statements.add(f.createStatement(publicLinker, hasAddedToLibrary, dateAdded, publicGraphIRI));
	        statements.add(f.createStatement(publicLinker, hasModifiedDate, date, publicGraphIRI));
	        statements.add(f.createStatement(publicLinker, hasCreatedDate, date, publicGraphIRI));
	        statements.add(f.createStatement(publicLinker, opensRing, opensRingValue, publicGraphIRI));
	        statements.add(f.createStatement(publicLinker, createdBy, user, publicGraphIRI));
	        if (description != null) statements.add(f.createStatement(publicLinker, hasDescription, description, publicGraphIRI));
        }
        
        List<Statement> statements2 = new ArrayList<Statement>();
        statements2.add(f.createStatement(local, hasPublicURI, publicLinker, graphIRI));
        statements2.add(f.createStatement(local, hasModifiedDate, date, graphIRI));
        statements2.add(f.createStatement(local, hasAddedToLibrary, dateAdded, graphIRI));
        statements2.add(f.createStatement(local, hasLinkerType, type, graphIRI));
        statements2.add(f.createStatement(local, RDF.TYPE, linkerType, graphIRI));
        
        if (!existing) {
	        // add additionalInfo based on the type of Glycan
	        switch (linker.getType()) {
	        case SMALLMOLECULE_LINKER:
	            
	            IRI hasInchiSequence = f.createIRI(hasInchiSequencePredicate);
	            IRI hasInchiKey = f.createIRI(hasInchiKeyPredicate);
	            IRI hasIupacName = f.createIRI(hasIupacNamePredicate);
	            IRI hasMass = f.createIRI(hasMassPredicate);
	            IRI hasImageUrl = f.createIRI(hasImageUrlPredicate);
	            IRI hasPubChemId = f.createIRI(hasPubChemIdPredicate);
	            IRI hasMolecularFormula = f.createIRI(hasMolecularFormulaPredicate);
	            IRI hasSmiles = f.createIRI(hasSmilesPredicate);
	            IRI hasClassification = f.createIRI(hasClassificationPredicate);
	            IRI hasChebiId = f.createIRI(hasChebiIdPredicate);
	            IRI hasClassificationValue = f.createIRI(hasClassificationValuePredicate);
	            
	            Literal pubChemId = null;
	            if (((SmallMoleculeLinker) linker).getPubChemId() != null)
	                pubChemId =  f.createLiteral(((SmallMoleculeLinker) linker).getPubChemId());
	            Literal inchiSequence = null;
	            if (((SmallMoleculeLinker) linker).getInChiSequence() != null)
	                inchiSequence = f.createLiteral(((SmallMoleculeLinker) linker).getInChiSequence());
	            Literal inchiKey = null;
	            if (((SmallMoleculeLinker) linker).getInChiKey() != null)
	                inchiKey = f.createLiteral(((SmallMoleculeLinker) linker).getInChiKey());
	            Literal imageUrl = null;
	            if (((SmallMoleculeLinker) linker).getImageURL() != null) 
	                imageUrl =  f.createLiteral(((SmallMoleculeLinker) linker).getImageURL());
	            Literal mass = null;
	            if (((SmallMoleculeLinker) linker).getMass() != null) 
	                mass =  f.createLiteral(((SmallMoleculeLinker) linker).getMass());
	            Literal molecularFormula = null;
	            if (((SmallMoleculeLinker) linker).getMolecularFormula() != null)
	                molecularFormula = f.createLiteral(((SmallMoleculeLinker) linker).getMolecularFormula());
	            Literal smiles = null;
	            if (((SmallMoleculeLinker) linker).getSmiles() != null) 
	                smiles = f.createLiteral(((SmallMoleculeLinker) linker).getSmiles());
	            Literal iupacName = null;
	            if (((SmallMoleculeLinker) linker).getIupacName() != null) 
	                iupacName = f.createLiteral(((SmallMoleculeLinker) linker).getIupacName());
	            if (inchiSequence != null) statements.add(f.createStatement(publicLinker, hasInchiSequence, inchiSequence, publicGraphIRI));
	            if (inchiKey != null) statements.add(f.createStatement(publicLinker, hasInchiKey, inchiKey, publicGraphIRI));
	            if (iupacName != null) statements.add(f.createStatement(publicLinker, hasIupacName, iupacName, publicGraphIRI));
	            if (mass != null) statements.add(f.createStatement(publicLinker, hasMass, mass, publicGraphIRI));
	            if (imageUrl != null) statements.add(f.createStatement( publicLinker, hasImageUrl, imageUrl, publicGraphIRI));
	            if (pubChemId != null) statements.add(f.createStatement(publicLinker, hasPubChemId, pubChemId, publicGraphIRI));
	            if (molecularFormula != null) statements.add(f.createStatement(publicLinker, hasMolecularFormula, molecularFormula, publicGraphIRI));
	            if (smiles != null) statements.add(f.createStatement(publicLinker, hasSmiles, smiles, publicGraphIRI));
	            
	            if (((SmallMoleculeLinker) linker).getClassification() != null) {
	                String classificationIRI = null;
	                if (((SmallMoleculeLinker) linker).getClassification().getUri() != null) {
	                    classificationIRI = ((SmallMoleculeLinker) linker).getClassification().getUri();
	                }
	                else {
	                    if (((SmallMoleculeLinker) linker).getClassification().getChebiId() != null) {
	                        classificationIRI = getClassificationByField(
	                                ((SmallMoleculeLinker) linker).getClassification().getChebiId() + "", 
	                                hasChebiIdPredicate.substring(hasChebiIdPredicate.lastIndexOf("#")+1), "integer", DEFAULT_GRAPH);
	                    } 
	                    if (classificationIRI == null && ((SmallMoleculeLinker) linker).getClassification().getClassification() != null) {
	                        classificationIRI = getClassificationByField(
	                                ((SmallMoleculeLinker) linker).getClassification().getClassification(), 
	                                hasClassificationValuePredicate.substring(hasClassificationValuePredicate.lastIndexOf("#")+1), "string", DEFAULT_GRAPH);
	                    }
	                    if (classificationIRI == null) {
	                        classificationIRI = generateUniqueURI(uriPrefix + "LC", userGraph);
	                    } 
	                }
	                IRI classification = f.createIRI(classificationIRI);
	                statements.add(f.createStatement(publicLinker, hasClassification, classification, publicGraphIRI));
	                if (((SmallMoleculeLinker) linker).getClassification().getChebiId() != null) {
	                    Literal chebiId = f.createLiteral(((SmallMoleculeLinker) linker).getClassification().getChebiId());
	                    Literal value = f.createLiteral(((SmallMoleculeLinker) linker).getClassification().getClassification());
	                    statements.add(f.createStatement(classification, hasChebiId, chebiId, publicGraphIRI));
	                    statements.add(f.createStatement(classification, hasClassificationValue, value, publicGraphIRI));
	                }
	            }
	                        
	            break;
	        case PEPTIDE_LINKER:
	        case PROTEIN_LINKER:
	            IRI hasSequence = f.createIRI(hasSequencePredicate);
	            Literal sequenceL= f.createLiteral(((SequenceBasedLinker) linker).getSequence());
	            statements.add(f.createStatement(publicLinker, hasSequence, sequenceL, publicGraphIRI));
	            if (description != null) statements.add(f.createStatement(publicLinker, hasDescription, description, publicGraphIRI));
	            
	            if (linker.getType() == LinkerType.PROTEIN_LINKER) {
	                if (((ProteinLinker)linker).getUniProtId() != null) {
	                    IRI hasUniProtId = f.createIRI(hasUniprotIdPredicate);
	                    Literal uniProt = f.createLiteral(((ProteinLinker)linker).getUniProtId());
	                    statements.add(f.createStatement(publicLinker, hasUniProtId, uniProt, publicGraphIRI));
	                }
	                if (((ProteinLinker)linker).getPdbIds() != null) {
	                    for (String pdbId: ((ProteinLinker)linker).getPdbIds()) {
	                        IRI hasPDBId = f.createIRI(hasPdbIdPredicate);
	                        Literal pdb = f.createLiteral(pdbId);
	                        statements.add(f.createStatement(publicLinker, hasPDBId, pdb, publicGraphIRI));
	                    }
	                }
	            }
	            
	            break;
	        }
	        
	        if (linker.getUrls() != null) {
	            for (String url: linker.getUrls()) {
	                Literal urlLit = f.createLiteral(url);
	                statements.add(f.createStatement(publicLinker, hasUrl, urlLit, publicGraphIRI));
	            }
	        }
	        sparqlDAO.addStatements(statements, publicGraphIRI);
        }
        sparqlDAO.addStatements(statements2, graphIRI);
        
        if (!existing && linker.getPublications() != null && !linker.getPublications().isEmpty()) {
            addLinkerPublications(linker, publicURI, DEFAULT_GRAPH);
        }
        
        return publicURI;
    }
}
