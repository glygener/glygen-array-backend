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
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.PrivateGraphEntity;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.GraphPermissionRepository;
import org.glygen.array.persistence.dao.PrivateGraphRepository;
import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.data.ChangeLog;
import org.glygen.array.persistence.rdf.data.ChangeTrackable;
import org.glygen.array.persistence.rdf.data.ChangeType;
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
	GraphPermissionRepository permissionRepository;
	
	@Autowired
	UserRepository userRepository;
	
	Random random = new Random();
	
	public static String prefix="PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>"
			+ "\nPREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
			+ "\nPREFIX gadr: <http://purl.org/gadr/data#>"
			+ "\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
			+ "\nPREFIX template: <http://purl.org/gadr/template#>";
	
	final static String hasURLPredicate = ontPrefix + "has_url";
	final static String hasOrganizationPredicate = ontPrefix + "has_organization";
	final static String hasIdentiferPredicate = ontPrefix + "has_identifier";
	
	final static String hasChangeLogPredicate = ontPrefix + "has_change_log";
	final static String hasChangeTypePredicate = ontPrefix + "has_change_type";
	final static String hasChangeFieldPredicate = ontPrefix + "has_field_change";
	final static String createdByPredicate = ontPrefix + "created_by";
	
	final static String hasDescriptionPredicate = ontPrefix + "has_description";
	final static String hasCreatedDatePredicate = ontPrefix + "has_date_created";
	final static String hasAddedToLibraryPredicate = ontPrefix + "has_date_addedtolibrary";
	final static String hasModifiedDatePredicate = ontPrefix + "has_date_modified";
	final static String hasPublicURIPredicate = ontPrefix + "has_public_uri";
	final static String hasTypePredicate = ontPrefix + "has_type";
	final static String hasPublication = ontPrefix + "has_publication";
	final static String hasGrant = ontPrefix + "has_grant";
	final static String hasCollaborator = ontPrefix + "has_collaborator";
    
    final static String hasTitlePredicate = ontPrefix + "has_title";
    final static String hasAuthorPredicate = ontPrefix + "has_author_list";
    final static String hasYearPredicate = ontPrefix + "has_year";
    final static String hasVolumePredicate = ontPrefix + "has_volume";
    final static String hasJournalPredicate = ontPrefix + "has_journal";
    final static String hasNumberPredicate = ontPrefix + "has_number";
    final static String hasStartPagePredicate = ontPrefix + "has_start_page";
    final static String hasEndPagePredicate = ontPrefix + "has_end_page";
    final static String hasDOIPredicate = ontPrefix + "has_doi";
    final static String hasPubMedPredicate = ontPrefix + "has_pubmed_id";
    
    
    public final static String sampleTypePredicate = ontPrefix + "sample";
   
    public final static String featureMetadataTypePredicate = ontPrefix + "feature_metadata";
    public final static String slideTypePredicate = ontPrefix + "slide";
    public final static String printerTypePredicate = ontPrefix + "printer";
    public final static String scannerTypePredicate = ontPrefix + "scanner";
    public final static String assayTypePredicate = ontPrefix + "assay";
    public final static String spotMetadataTypePredicate = ontPrefix + "spot_metadata";
    public final static String spotMetadataValueTypePredicate = ontPrefix + "spot_metadata_value";
    public final static String slideTemplateTypePredicate = ontPrefix + "slide_metadata";
    public final static String imageAnalysisTypePredicate = ontPrefix + "image_analysis_software";
    public final static String dataProcessingTypePredicate = ontPrefix + "data_processing_software";
    public final static String simpleDescriptionTypePredicate = ontPrefix + "simple_description";
    public final static String complexDescriptionTypePredicate = ontPrefix + "complex_description";
    
    public final static String imageProcessingMetadataPredicate = ontPrefix + "has_image_processing_metadata";
    public final static String processingSoftwareMetadataPredicate = ontPrefix + "has_processing_software_metadata";
    public final static String slideMetadataPredicate = ontPrefix + "has_slide_metadata";
    public final static String assayMetadataPredicate = ontPrefix + "has_assay_metadata";
    public final static String printerMetadataPredicate = ontPrefix + "printed_by";
    public final static String scannerMetadataPredicate = ontPrefix + "has_scanner_metadata";
    public final static String featureMetadataPredicate = ontPrefix + "has_feature_metadata";
    
    public final static String orderPredicate = ontPrefix + "has_order";
    public final static String valuePredicate = ontPrefix + "has_value";
    public final static String keyPredicate = ontPrefix + "has_key";
    public final static String unitPredicate = ontPrefix + "has_unit_of_measurement";
    public final static String describedbyPredicate = ontPrefix + "described_by";
    
    // Template ontology stuff
    public final static String hasSampleTemplatePredicate = MetadataTemplateRepository.templatePrefix + "has_sample_template";
    public final static String hasSlideTemplatePredicate = MetadataTemplateRepository.templatePrefix + "has_slide_template";
    public final static String hasScannerleTemplatePredicate = MetadataTemplateRepository.templatePrefix + "has_scanner_template";
    public final static String hasPrinterTemplatePredicate = MetadataTemplateRepository.templatePrefix + "has_printer_template";
    public final static String hasImageTemplatePredicate = MetadataTemplateRepository.templatePrefix + "has_image_analysis_software_template";
    public final static String hasDataprocessingTemplatePredicate = MetadataTemplateRepository.templatePrefix + "has_data_processing_software_template";
    public final static String hasAssayTemplatePredicate = MetadataTemplateRepository.templatePrefix + "has_assay_template";
    public final static String hasSpotMetadataTemplatePredicate = MetadataTemplateRepository.templatePrefix + "has_spot_template";
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String addPrivateGraphForUser (UserEntity uEntity) throws SQLException {
		String URI = sparqlDAO.addGraph(PRIVATE_GRAPH, uEntity.getUsername());
		PrivateGraphEntity graph = new PrivateGraphEntity();
		graph.setUser(uEntity);
		graph.setGraphIRI(URI);
		graphRepository.save (graph);
		return URI;
	}
	
	/**
	 * generate a random number and check it against existing ids in public graph (DEFAULT)
	 * 
	 * @param pre prefix to add to the beginning to the random number generated
	 * @return a unique id string starting with the pre
	 * @throws SparqlException if sparql query fails to execute
	 */
	protected String generateUniqueURI (String pre) throws SparqlException {
		return generateUniqueURI(pre, (String[])null);
	}
	
	/**
	 * generate a random number and check it against existing ids in public graph (DEFAULT) and all the graphs provided if any
	 * 
	 * @param pre prefix to add to the beginning to the random number generated
	 * @param graph graphs to search against, public graph is searched by default
	 * @return a unique id string starting with the pre
	 * @throws SparqlException if sparql query fails to execute
	 */
	protected String generateUniqueURI (String pre, String... graph) throws SparqlException {
		// check the repository to see if the generated URI is unique
		boolean unique = false;
		String newURI = null;
		do {
			newURI = pre + (1000000 + random.nextInt(9999999));
			StringBuffer queryBuf = new StringBuffer();
			queryBuf.append (prefix + "\n");
			queryBuf.append ("SELECT DISTINCT ?o\n");
			queryBuf.append("FROM <" + DEFAULT_GRAPH + ">\n");
			if (graph != null) {
				for (int i=0; i < graph.length; i++) {
					queryBuf.append ("FROM <" + graph[i] + ">\n");
				}
			}
			queryBuf.append ("WHERE {\n" + 
					"<"+ newURI + "> ?p ?o .\n" + 
					"				}\n" + 
					"				LIMIT 1");
			List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
			unique = results.size() == 0;
		} while (!unique);
		
		return newURI;
	}
	
	/**
	 * 
	 * @param graph graph for the user
	 * @param type the rdf type of the entity http://purl.org/gadr/data#Glycan, http://purl.org/gadr/template#SlideLayout,
	 * @return total number of triples with that rdf:type as the subject and date_addedToLibrary as the predicate
	 * @throws SparqlException
	 */
	protected int getCountByUserByType (String graph, String type) throws SparqlException {
		int total = 0;
		if (graph != null) {
			StringBuffer queryBuf = new StringBuffer();
			queryBuf.append (prefix + "\n");
			queryBuf.append ("SELECT COUNT(DISTINCT ?s) as ?count \n");
			//queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
			queryBuf.append ("FROM <" + graph + ">\n");
			queryBuf.append ("WHERE {\n");
			queryBuf.append (" ?s gadr:has_date_addedtolibrary ?d . \n");
			queryBuf.append (" ?s rdf:type  <" + type +">. }");
			List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
			logger.info("count query:" + queryBuf.toString());
			for (SparqlEntity sparqlEntity : results) {
				String count = sparqlEntity.getValue("count");
				if (count == null) {
				    logger.error("Cannot get the count from repository");
				} 
				else {
    				try {
    					total = Integer.parseInt(count);
    					break;
    				} catch (NumberFormatException e) {
    					throw new SparqlException("Count query returned invalid result", e);
    				}
				}
				
			}
		}
		return total;
	}
	
	/**
	 * return the uri of the entity with the given label and type in the given graph
	 * 
	 * @param label the label to search
	 * @param graph the graph to search into
	 * @param type the type of the entity, i.e. http://purl.org/gadr/data#Glycan, http://purl.org/gadr/data#SlideLayout, http://purl.org/gadr/data#ArrayDataset 
	 * @return the uri of the entity 
	 * @throws SparqlException
	 */
	protected String getEntityByLabel (String label, String graph, String type) throws SparqlException { 
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        //queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
        if (graph != null) {
            queryBuf.append ("FROM <" + graph + ">\n");
        }
        queryBuf.append ("WHERE {\n");
        queryBuf.append (" ?s gadr:has_date_addedtolibrary ?d . \n");
        queryBuf.append (" ?s rdf:type  <" + type +">. ");
        queryBuf.append (" ?s rdfs:label ?l FILTER (lcase(str(?l)) = \"" + label.toLowerCase() + "\") \n }");
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        if (results.isEmpty())
            return null;
        else {
            String uri = results.get(0).getValue("s");
            return uri;
        }
	}

	/**
     * {@inheritDoc}
     */
	@Override
	public String getGraphForUser (UserEntity user) throws SQLException {
		PrivateGraphEntity graph = graphRepository.findByUser(user);
		if (graph != null) 
			return graph.getGraphIRI();
		else { // try to create for the first time 
			return addPrivateGraphForUser(user);
		}
	}
	
	public List<String> getAllUserGraphs () throws SQLException {
	    List<PrivateGraphEntity> list = graphRepository.findAll();
	    List<String> graphs = new ArrayList<>();
	    for (PrivateGraphEntity e: list) {
	        graphs.add(e.getGraphIRI());
	    }
	    return graphs;
	}
	
	/**
	 * a generic delete method that removes all the triples for the given uri as the subject from the given graph
	 * 
	 * @param uri subject uri for the triples to remove
	 * @param graph graph to delete from
	 * @throws SparqlException 
	 */
	protected void deleteByURI(String uri, String graph) throws SparqlException {
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI subject = f.createIRI(uri);
		IRI graphIRI = f.createIRI(graph);
		RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(subject, null, null, graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(statements2), graphIRI);
	}

	/**
	 * removes all the data from the repository
	 * This removes necessary initialized data as well, so it is necessary to restart virtuoso container after resetting the 
	 * repository!!!
	 */
    @Override
    public void resetRepository() throws SQLException {
        sparqlDAO.deleteAll();   
    }
    
    protected String getSearchPredicate (String searchValue, String queryLabel) {
        String predicates = "";
        
        predicates += queryLabel + " rdfs:label ?value1 .\n";
        predicates += "OPTIONAL {" + queryLabel + " rdfs:comment ?value2} \n";
        
        int numberOfValues = 3;
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
    
    protected String getSortPredicate(String field) {
        if (field == null || field.equalsIgnoreCase("name")) 
            return "rdfs:label";
        else if (field.equalsIgnoreCase("comment")) 
            return "rdfs:comment";
        else if (field.equalsIgnoreCase("dateModified"))
            return "gadr:has_date_modified";
        else if (field.equalsIgnoreCase("dateAddedToLibrary"))
            return "gadr:has_date_addedtolibrary";
        else if (field.equalsIgnoreCase("dateCreated"))
            return "gadr:has_date_created";
        else if (field.equalsIgnoreCase("id"))
            return null;
        return null;
    }

    protected List<SparqlEntity> retrieveByTypeAndUser(int offset, int limit, String field, int order, String searchValue,
            String graph, String type) throws SparqlException {
        
        String sortPredicate = getSortPredicate (field);
        String searchPredicate = "";
        String publicSearchPredicate = "";
        if (searchValue != null) {
            searchPredicate = getSearchPredicate(searchValue, "?s");
            publicSearchPredicate = getSearchPredicate(searchValue, "?public");
        }
        
        String sortLine = "";
        String publicSortLine = "";
        if (sortPredicate != null) {
            sortLine = "OPTIONAL {?s " + sortPredicate + " ?sortBy } .\n";  
            sortLine += "filter (bound (?sortBy) or !bound(?public)) . \n";
            publicSortLine = "OPTIONAL {?public " + sortPredicate + " ?sortBy } .\n";  
        }
        
        
        String orderByLine = " ORDER BY " + (order == 0 ? "DESC" : "ASC") + (sortPredicate == null ? "(?s)": "(?sortBy)");  
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s");
        if (sortPredicate != null) {
            //queryBuf.append(", ?sortBy");
        }
        queryBuf.append ("\nFROM <" + graph + ">\n");
        if (!graph.equals(GlygenArrayRepository.DEFAULT_GRAPH))  {
            queryBuf.append ("FROM NAMED <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n");
        }
        queryBuf.append ("WHERE {\n {\n");
        queryBuf.append (
                " ?s gadr:has_date_addedtolibrary ?d .\n" +
                " ?s rdf:type <" + type + "> . \n" +
                " OPTIONAL {?s gadr:has_public_uri ?public  } .\n" + 
                        sortLine + searchPredicate + 
                "}\n" );
         if (!graph.equals(GlygenArrayRepository.DEFAULT_GRAPH))  {             
             queryBuf.append ("UNION {" +
                "?s gadr:has_public_uri ?public . \n" +
                "GRAPH <" + GlygenArrayRepository.DEFAULT_GRAPH + "> {\n" +
                " ?public rdf:type <" + type + "> . \n" +
                    publicSortLine + publicSearchPredicate + 
                "}}\n"); 
         }
         queryBuf.append ("}" + 
                 orderByLine + 
                ((limit == -1) ? " " : " LIMIT " + limit) +
                " OFFSET " + offset);
        
        return sparqlDAO.query(queryBuf.toString());
    }
    
    protected String addGenericInfo (String name, String description, Date createdDate, List<Statement> statements, String prefix, String graph) throws SparqlException, SQLException {
        ValueFactory f = sparqlDAO.getValueFactory();
        String uriPre = uriPrefix;
        if (graph.equals (DEFAULT_GRAPH)) {
            uriPre = uriPrefixPublic;
        }
        String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
        // add to user's local repository
        String uri = generateUniqueURI(uriPre + prefix, allGraphs);
        IRI iri = f.createIRI(uri);
        Literal date = f.createLiteral(new Date());
        IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
        IRI graphIRI = f.createIRI(graph);
        Literal label = name == null || name.isEmpty() ? null : f.createLiteral(name);
        Literal comment = description == null ? null : f.createLiteral(description);
        IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
        IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
        Literal createdDateLit = f.createLiteral(createdDate);
        
        statements.add(f.createStatement(iri, hasCreatedDate, date, graphIRI));
        if (label != null) statements.add(f.createStatement(iri, RDFS.LABEL, label, graphIRI));
        
        if (comment != null) statements.add(f.createStatement(iri, RDFS.COMMENT, comment, graphIRI));
        statements.add(f.createStatement(iri, hasAddedToLibrary, createdDateLit, graphIRI));
        statements.add(f.createStatement(iri, hasModifiedDate, date, graphIRI));
        
        return uri;
    }

    @Override
    public String saveChangeLog(ChangeLog change, String entryURI, String graph) throws SparqlException, SQLException {
        ValueFactory f = sparqlDAO.getValueFactory();
        String uriPre = uriPrefix;
        if (graph.equals (DEFAULT_GRAPH)) {
            uriPre = uriPrefixPublic;
        }
        String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
        // add to user's local repository
        String uri = generateUniqueURI(uriPre + "CL", allGraphs);
        IRI entryIRI = f.createIRI(entryURI);
        IRI iri = f.createIRI(uri);
        Literal date = f.createLiteral(new Date());
        IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
        IRI createdBy = f.createIRI(createdByPredicate);
        IRI hasType = f.createIRI(hasChangeFieldPredicate);
        IRI hasChangeLog = f.createIRI(hasChangeLogPredicate);
        IRI hasFieldChange = f.createIRI(hasChangeFieldPredicate);
        IRI graphIRI = f.createIRI(graph);
        Literal comment = change.getSummary() == null ? null : f.createLiteral(change.getSummary());
        Literal user = f.createLiteral(change.getUser());
        Literal type = f.createLiteral(change.getChangeType().name());
        
        List<Statement> statements = new ArrayList<Statement>();
        statements.add(f.createStatement(iri, hasCreatedDate, date, graphIRI));
        if (comment != null) statements.add(f.createStatement(iri, RDFS.COMMENT, comment, graphIRI));
        statements.add(f.createStatement(iri, createdBy, user, graphIRI));
        statements.add(f.createStatement(iri, hasType, type, graphIRI));
        statements.add(f.createStatement(entryIRI, hasChangeLog, iri, graphIRI));
        
        if (change.getChangedFields() != null) {
            for (String fieldChange: change.getChangedFields()) {
                statements.add(f.createStatement(iri, hasFieldChange, f.createLiteral(fieldChange), graphIRI));
            }
        }
        
        sparqlDAO.addStatements(statements, graphIRI);
        return uri;
    }

    @Override
    public void retrieveChangeLog(ChangeTrackable entity, String entityUri, String graph) throws SparqlException, SQLException {
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI entityIRI = f.createIRI(entityUri);
        IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
        IRI createdBy = f.createIRI(createdByPredicate);
        IRI hasType = f.createIRI(hasChangeTypePredicate);
        IRI hasFieldChange = f.createIRI(hasChangeFieldPredicate);
        IRI graphIRI = f.createIRI(graph);
        
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(entityIRI, null, null, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next();
            ChangeLog change = new ChangeLog();
            List<String> changedFields = new ArrayList<String>();
            if (st.getPredicate().equals(RDFS.COMMENT)) {
                change.setSummary(st.getObject().stringValue());
            } else if (st.getPredicate().equals(hasCreatedDate)) {
                Value value = st.getObject();
                if (value instanceof Literal) {
                    Literal literal = (Literal)value;
                    XMLGregorianCalendar calendar = literal.calendarValue();
                    Date date = calendar.toGregorianCalendar().getTime();
                    change.setDate(date);
                }
            } else if (st.getPredicate().equals(hasType)) {
                change.setChangeType(ChangeType.valueOf(st.getObject().stringValue()));
            } else if (st.getPredicate().equals(createdBy)) {
                change.setUser(st.getObject().stringValue());
            } else if (st.getPredicate().equals(hasFieldChange)) {
                changedFields.add(st.getObject().stringValue());
            }
            entity.addChange(change);
         }
    }
}
