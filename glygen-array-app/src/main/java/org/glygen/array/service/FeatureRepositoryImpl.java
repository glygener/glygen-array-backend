package org.glygen.array.service;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

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
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.FeatureType;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.Linker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(value="sesameTransactionManager") 
public class FeatureRepositoryImpl extends GlygenArrayRepositoryImpl implements FeatureRepository {
	
	final static String featureTypePredicate = ontPrefix + "Feature";
	final static String hasLinkerPredicate = ontPrefix + "has_linker";
	final static String hasMoleculePredicate = ontPrefix + "has_molecule";
	
	final static String hasPositionPredicate = ontPrefix + "has_molecule_position";
	final static String hasPositionValuePredicate = ontPrefix + "has_position";
	
	@Autowired
	GlycanRepository glycanRepository;
	
	@Autowired
	LinkerRepository linkerRepository;
	
	Map<String, Feature> featureCache = new HashMap<String, Feature>();

	@Override
	public String addFeature(Feature feature, UserEntity user) throws SparqlException, SQLException {
		String graph = null;
		if (feature == null || feature.getLinker() == null)
			// cannot add 
			throw new SparqlException ("Not enough information is provided to register a feature");
		
		
		// check if there is already a private graph for user
		graph = getGraphForUser(user);
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI featureType = f.createIRI(featureTypePredicate);
		String featureURI = generateUniqueURI(uriPrefix + "F", graph);
		IRI feat = f.createIRI(featureURI);
		IRI graphIRI = f.createIRI(graph);
		IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
		IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
		IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
		IRI hasLinker = f.createIRI(hasLinkerPredicate);
		IRI hasMolecule = f.createIRI(hasMoleculePredicate);
		IRI hasPositionContext = f.createIRI(hasPositionPredicate);
		IRI hasPosition = f.createIRI(hasPositionValuePredicate);
		Literal date = f.createLiteral(new Date());
		IRI hasFeatureType = f.createIRI(hasTypePredicate);
        Literal type = f.createLiteral(feature.getType().name());
        
		if (feature.getName() == null || feature.getName().trim().isEmpty()) {
		    feature.setName(featureURI.substring(featureURI.lastIndexOf("/")+1));
		}
		Literal label = f.createLiteral(feature.getName());
		
		List<Statement> statements = new ArrayList<Statement>();
		
		statements.add(f.createStatement(feat, RDFS.LABEL, label, graphIRI));
		statements.add(f.createStatement(feat, RDF.TYPE, featureType, graphIRI));
		statements.add(f.createStatement(feat, hasCreatedDate, date, graphIRI));
		statements.add(f.createStatement(feat, hasAddedToLibrary, date, graphIRI));
		statements.add(f.createStatement(feat, hasModifiedDate, date, graphIRI));
		statements.add(f.createStatement(feat, hasFeatureType, type, graphIRI));
		
		Linker linker = feature.getLinker();
		if (linker.getUri() == null) {
		    if (linker.getId() != null)
		        linker.setUri(uriPrefix + linker.getId());
		    else {
		        throw new SparqlException ("No enough information is provided to add the feature, linker cannot be found!"); 
		    }
		}
		
		IRI linkerIRI = f.createIRI(linker.getUri());
		statements.add(f.createStatement(feat, hasLinker, linkerIRI, graphIRI));
		
		if (feature.getGlycans() != null) {
    		for (Glycan g: feature.getGlycans()) {
    			if (g.getUri() == null) {
    				if (g.getId() != null) {
    					g.setUri(uriPrefix + g.getId());
    				} else {
    				    throw new SparqlException ("No enough information is provided to add the feature, glycan " + g.getName() + " cannot be found!");

    				}
    			}
    			
    			IRI glycanIRI = f.createIRI(g.getUri());
    			statements.add(f.createStatement(feat, hasMolecule, glycanIRI, graphIRI));
    		}
		}
		
		if (feature.getPositionMap() != null) {
			for (String position: feature.getPositionMap().keySet()) {
				String glycanId = feature.getPositionMap().get(position);
				IRI glycanIRI = f.createIRI(uriPrefix + glycanId);
				Literal pos = f.createLiteral(position);
				String positionContextURI = generateUniqueURI(uriPrefix + "PC", graph);
				IRI positionContext = f.createIRI(positionContextURI);
				statements.add(f.createStatement(feat, hasPositionContext, positionContext, graphIRI));
				statements.add(f.createStatement(positionContext, hasMolecule, glycanIRI, graphIRI));
				statements.add(f.createStatement(positionContext, hasPosition, pos, graphIRI));
			}
		}
		
		sparqlDAO.addStatements(statements, graphIRI);
		
		return featureURI;
	}
	
	@Override
    public Feature getFeatureById(String featureId, UserEntity user) throws SparqlException, SQLException {
        // make sure the glycan belongs to this user
	    String graph = null;
	    String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        }
        else {
            graph = getGraphForUser(user);
        }
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?d \n");
        //queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
        queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ( "<" +  uriPre + featureId + "> gadr:has_date_addedtolibrary ?d . }\n");
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        if (results.isEmpty())
            return null;
        else {
            return getFeatureFromURI(uriPre + featureId, user);
        }
    }
	
	@Override
    public Feature getFeatureByLabel(String label, UserEntity user) throws SparqlException, SQLException {
        if (label == null || label.isEmpty())
            return null;
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
        queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ( " ?s gadr:has_date_addedtolibrary ?d . \n");
        queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/data#Feature>. \n");
        queryBuf.append ( " ?s rdfs:label ?l FILTER (lcase(str(?l)) = \"\"\"" + label.toLowerCase() + "\"\"\") \n"
                + "}\n");
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        if (results.isEmpty())
            return null;
        else {
            String uri = results.get(0).getValue("s");
            return getFeatureFromURI(uri, user);
        }
    }

	@Override
	public List<Feature> getFeatureByUser(UserEntity user) throws SparqlException, SQLException {
		return getFeatureByUser(user, 0, -1, "id", 0);
	}

	@Override
	public List<Feature> getFeatureByUser(UserEntity user, int offset, int limit, String field, int order)
			throws SparqlException, SQLException {
	    return getFeatureByUser(user, offset, limit, field, order, null);
	}
	
	@Override
    public List<Feature> getFeatureByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException {
		List<Feature> features = new ArrayList<Feature>();
		
		String sortPredicate = getSortPredicate (field);
		String searchPredicate = "";
        if (searchValue != null)
            searchPredicate = getSearchPredicate(searchValue);
		// get all featureURIs from user's private graph
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
			String orderByLine = " ORDER BY " + (order == 0 ? "DESC" : "ASC") + (sortPredicate == null ? "(?s)": "(?sortBy)");	
			StringBuffer queryBuf = new StringBuffer();
			queryBuf.append (prefix + "\n");
			queryBuf.append ("SELECT DISTINCT ?s \n");
			//queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
			queryBuf.append ("FROM <" + graph + ">\n");
			queryBuf.append ("WHERE {\n");
			queryBuf.append ( 
					" ?s gadr:has_date_addedtolibrary ?d .\n" +
					" ?s rdf:type  <http://purl.org/gadr/data#Feature>. \n" +
					sortLine + searchPredicate +
				    "}\n" +
					 orderByLine + 
					((limit == -1) ? " " : " LIMIT " + limit) +
					" OFFSET " + offset);
			
			List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
			
			for (SparqlEntity sparqlEntity : results) {
				String featureURI = sparqlEntity.getValue("s");
				Feature feature = getFeatureFromURI(featureURI, user);
				features.add(feature);	
			}
		}
		
		return features;
	}
	
	public String getSearchPredicate (String searchValue) {
        String predicates = "";
        
        predicates += "?s rdfs:label ?value1 .\n";
        predicates += "OPTIONAL {?s gadr:has_type ?value2 .}\n";
        predicates += "OPTIONAL {?s gadr:has_molecule ?g . ?g gadr:has_glytoucan_id ?value3 . ?g rdfs:label ?value4} \n";
        predicates += "OPTIONAL {?s gadr:has_linker ?l . ?l gadr:has_pubchem_compound_id ?value5 . ?l rdfs:label ?value6} \n";
        
        int numberOfValues = 6; // need to match with the total values (?value1 - ?value6) specified in above predicates
        
        String filterClause = "filter (";
        for (int i=1; i <= numberOfValues; i++) {
            filterClause += "regex (str(?value" + i + "), '" + searchValue + "', 'i')";
            if (i < numberOfValues)
                filterClause += " || ";
        }
        filterClause += ")\n";
            
        predicates += filterClause;
        return predicates;
    }

    @Override
	public int getFeatureCountByUser(UserEntity user) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
		return getCountByUserByType(graph, featureTypePredicate);
	}

	@Override
	public void deleteFeature(String featureId, UserEntity user) throws SparqlException, SQLException {
		String graph;
		
		graph = getGraphForUser(user);
		if (graph != null) {
		    if (canDelete(uriPrefix + featureId, graph)) {
    			// check to see if the given featureId is in this graph
    			Feature existing = getFeatureFromURI (uriPrefix + featureId, user);
    			if (existing != null) {
    				if (existing.getPositionMap() != null && !existing.getPositionMap().isEmpty()) {
    					// need to delete position context
    					ValueFactory f = sparqlDAO.getValueFactory();
    					IRI object = f.createIRI(existing.getUri());
    					IRI graphIRI = f.createIRI(graph);
    					IRI hasPositionContext = f.createIRI(hasPositionPredicate);
    					RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(object, hasPositionContext, null, graphIRI);
    					while (statements2.hasNext()) {
    						Statement st = statements2.next();
    						Value positionContext = st.getSubject();
    						deleteByURI (positionContext.stringValue(), graph);	
    					}
    				}
    				deleteByURI (uriPrefix + featureId, graph);
    				featureCache.remove(uriPrefix + featureId);
    				return;
    			}
		    } else {
		        throw new IllegalArgumentException("Cannot delete feature " + featureId + ". It is used in a block layout");
		    }
		}
	}
	
	boolean canDelete (String featureURI, String graph) throws SparqlException, SQLException { 
	    boolean canDelete = true;
	    
	    StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        //queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
        queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ("?s gadr:has_spot ?spot . ?spot gadr:has_feature <" +  featureURI + "> . } LIMIT 1");
        
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        if (!results.isEmpty())
            canDelete = false;
	    
	    return canDelete;
	}

	@Override
	public Feature getFeatureFromURI(String featureURI, UserEntity user) throws SparqlException, SQLException {
	    // check the cache first
	    if (featureCache.get(featureURI) != null)
	        return featureCache.get(featureURI);
	    
		Feature featureObject = null;
		String graph = null;
		if (featureURI.contains("public"))
            graph = DEFAULT_GRAPH;
        else {
            if (user != null)
                graph = getGraphForUser(user);
            else 
                graph = DEFAULT_GRAPH;
        }
        /*if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }*/
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI feature = f.createIRI(featureURI);
		IRI graphIRI = f.createIRI(graph);
		IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
		IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
		IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
		IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
		IRI hasLinker = f.createIRI(hasLinkerPredicate);
		IRI hasMolecule = f.createIRI(hasMoleculePredicate);
		IRI hasPositionContext = f.createIRI(hasPositionPredicate);
		IRI hasPosition = f.createIRI(hasPositionValuePredicate);
		IRI hasFeatureType = f.createIRI(hasTypePredicate);
		IRI hasPublicURI = f.createIRI(ontPrefix + "has_public_uri");
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(feature, null, null, graphIRI);
		List<Glycan> glycans = new ArrayList<Glycan>();
		Map<String, String> positionMap = new HashMap<>();
		if (statements.hasNext()) {
			featureObject = new Feature();
			featureObject.setUri(featureURI);
			featureObject.setId(featureURI.substring(featureURI.lastIndexOf("/")+1));
			featureObject.setGlycans(glycans);
			featureObject.setPositionMap(positionMap);
		}
		while (statements.hasNext()) {
			Statement st = statements.next();
			if (st.getPredicate().equals(RDFS.LABEL)) {
			    Value label = st.getObject();
                featureObject.setName(label.stringValue());
			} else if (st.getPredicate().equals(hasFeatureType)) {
			    Value value = st.getObject();
			    if (value != null) {
			        FeatureType type = FeatureType.valueOf(value.stringValue());
			        featureObject.setType(type);
			    }
			} else if (st.getPredicate().equals(hasLinker)) {
				Value value = st.getObject();
				if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
					String linkerURI = value.stringValue();
					Linker linker = linkerRepository.getLinkerFromURI(linkerURI, user);
					featureObject.setLinker(linker);
				}
			} else if (st.getPredicate().equals(hasMolecule)) {
				Value value = st.getObject();
				if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
					String glycanURI = value.stringValue();
					Glycan glycan = glycanRepository.getGlycanFromURI(glycanURI, user);
					glycans.add(glycan);
				}
			} else if (st.getPredicate().equals(hasCreatedDate)) {
				Value value = st.getObject();
			    if (value instanceof Literal) {
			    	Literal literal = (Literal)value;
			    	XMLGregorianCalendar calendar = literal.calendarValue();
			    	Date date = calendar.toGregorianCalendar().getTime();
			    	featureObject.setDateCreated(date);
			    }
			} else if (st.getPredicate().equals(hasModifiedDate)) {
				Value value = st.getObject();
			    if (value instanceof Literal) {
			    	Literal literal = (Literal)value;
			    	XMLGregorianCalendar calendar = literal.calendarValue();
			    	Date date = calendar.toGregorianCalendar().getTime();
			    	featureObject.setDateModified(date);
			    }
			} else if (st.getPredicate().equals(hasAddedToLibrary)) {
				Value value = st.getObject();
			    if (value instanceof Literal) {
			    	Literal literal = (Literal)value;
			    	XMLGregorianCalendar calendar = literal.calendarValue();
			    	Date date = calendar.toGregorianCalendar().getTime();
			    	featureObject.setDateAddedToLibrary(date);
			    }
			} else if (st.getPredicate().equals(hasPositionContext)) {
				Value positionContext = st.getObject();
				String contextURI = positionContext.stringValue();
				IRI ctx = f.createIRI(contextURI);
				RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(ctx, null, null, graphIRI);
				Integer position = null;
				Glycan glycanInContext = null;
				while (statements2.hasNext()) {
					Statement st2 = statements2.next();
					if (st2.getPredicate().equals(hasPosition)) {
						Value value = st2.getObject();
						if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
							position = Integer.parseInt(value.stringValue());
						}	
					} else if (st2.getPredicate().equals(hasMolecule)) {
						Value val = st2.getObject();
						glycanInContext = glycanRepository.getGlycanFromURI(val.stringValue(), user);
					}  
				}
				if (position != null && glycanInContext != null) {
					positionMap.put (position +"", glycanInContext.getUri().substring(glycanInContext.getUri().lastIndexOf("/")+1));
				}
			} else if (st.getPredicate().equals(hasPublicURI)) {
			    Value value = st.getObject();
			    String publicURI = value.stringValue();
			    IRI publicFeature = f.createIRI(publicURI);
                RepositoryResult<Statement> statementsPublic = sparqlDAO.getStatements(publicFeature, null, null, defaultGraphIRI);
                while (statementsPublic.hasNext()) {
                    Statement stPublic = statementsPublic.next();
                    if (stPublic.getPredicate().equals(RDFS.LABEL)) {
                        Value label = stPublic.getObject();
                        featureObject.setName(label.stringValue());
                    } else if (stPublic.getPredicate().equals(hasFeatureType)) {
                        value = stPublic.getObject();
                        if (value != null) {
                            FeatureType type = FeatureType.valueOf(value.stringValue());
                            featureObject.setType(type);
                        }
                    } else if (stPublic.getPredicate().equals(hasLinker)) {
                        value = stPublic.getObject();
                        if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
                            String linkerURI = value.stringValue();
                            Linker linker = linkerRepository.getLinkerFromURI(linkerURI, user);
                            featureObject.setLinker(linker);
                        }
                    } else if (stPublic.getPredicate().equals(hasMolecule)) {
                        value = stPublic.getObject();
                        if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
                            String glycanURI = value.stringValue();
                            Glycan glycan = glycanRepository.getGlycanFromURI(glycanURI, user);
                            glycans.add(glycan);
                        }
                    } else if (stPublic.getPredicate().equals(hasPositionContext)) {
                        Value positionContext = stPublic.getObject();
                        String contextURI = positionContext.stringValue();
                        IRI ctx = f.createIRI(contextURI);
                        RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(ctx, null, null, defaultGraphIRI);
                        Integer position = null;
                        Glycan glycanInContext = null;
                        while (statements2.hasNext()) {
                            Statement st2 = statements2.next();
                            if (st2.getPredicate().equals(hasPosition)) {
                                value = st2.getObject();
                                if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
                                    position = Integer.parseInt(value.stringValue());
                                }   
                            } else if (st2.getPredicate().equals(hasMolecule)) {
                                Value val = st2.getObject();
                                glycanInContext = glycanRepository.getGlycanFromURI(val.stringValue(), user);
                            }  
                        }
                        if (position != null && glycanInContext != null) {
                            positionMap.put (position +"", glycanInContext.getUri().substring(glycanInContext.getUri().lastIndexOf("/")+1));
                        }
                    }
                }
			}
		}
		
		// for the private graph retrievals, only keep the non-public ones
		if (user != null && !graph.equals(DEFAULT_GRAPH) && featureObject != null) {
		    List<Glycan> finalGlycans = new ArrayList<Glycan>();
		    for (Glycan glycan: featureObject.getGlycans()) {
		        if (glycan.getUri().contains("public"))
		             continue;
		        finalGlycans.add(glycan);
		    }
		    featureObject.setGlycans(finalGlycans);
		}
		
		featureCache.put(featureURI, featureObject);
		return featureObject;
	}
	
	/**
	 * difference from addFeature is that this assumes linkers and glycans are already made public and Feature object
	 * contains their corresponding public URIs 
	 * @throws SQLException 
	 * 
	 */
	@Override
	public String addPublicFeature(Feature feature, UserEntity user) throws SparqlException, SQLException {
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI featureType = f.createIRI(featureTypePredicate);
		String featureURI = generateUniqueURI(uriPrefixPublic + "F");
		IRI feat = f.createIRI(featureURI);
		IRI graphIRI = f.createIRI(DEFAULT_GRAPH);
		IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
		IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
		IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
		IRI hasLinker = f.createIRI(hasLinkerPredicate);
		IRI hasMolecule = f.createIRI(hasMoleculePredicate);
		IRI hasPositionContext = f.createIRI(hasPositionPredicate);
		IRI hasPosition = f.createIRI(hasPositionValuePredicate);
		Literal date = f.createLiteral(new Date());
		IRI hasFeatureType = f.createIRI(hasTypePredicate);
        Literal type = f.createLiteral(feature.getType().name());
		Literal label = f.createLiteral(feature.getName());
		
		List<Statement> statements = new ArrayList<Statement>();
		
		statements.add(f.createStatement(feat, RDFS.LABEL, label, graphIRI));
		statements.add(f.createStatement(feat, RDF.TYPE, featureType, graphIRI));
		statements.add(f.createStatement(feat, hasCreatedDate, date, graphIRI));
		statements.add(f.createStatement(feat, hasAddedToLibrary, date, graphIRI));
		statements.add(f.createStatement(feat, hasModifiedDate, date, graphIRI));
		statements.add(f.createStatement(feat, hasFeatureType, type, graphIRI));
		
		if (feature.getLinker() != null) {
			IRI linkerIRI = f.createIRI(feature.getLinker().getUri());
    		statements.add(f.createStatement(feat, hasLinker, linkerIRI, graphIRI));
		}
		
    		
		for (Glycan g: feature.getGlycans()) {
			IRI glycanIRI = f.createIRI(g.getUri());
			statements.add(f.createStatement(feat, hasMolecule, glycanIRI, graphIRI));
		}
		
		if (feature.getPositionMap() != null) {
			for (String position: feature.getPositionMap().keySet()) {
				String glycanId = feature.getPositionMap().get(position);
				IRI glycanIRI = f.createIRI(uriPrefixPublic + glycanId);
				Literal pos = f.createLiteral(position);
				String positionContextURI = generateUniqueURI(uriPrefixPublic + "PC");
				IRI positionContext = f.createIRI(positionContextURI);
				statements.add(f.createStatement(feat, hasPositionContext, positionContext, graphIRI));
				statements.add(f.createStatement(positionContext, hasMolecule, glycanIRI, graphIRI));
				statements.add(f.createStatement(positionContext, hasPosition, pos, graphIRI));
			}
		}
		
		sparqlDAO.addStatements(statements, graphIRI);
		
		// create a link from the local one -> has_public_uri -> public one
		String userGraph = getGraphForUser(user);
        
        IRI hasPublicURI = f.createIRI(ontPrefix + "has_public_uri");
        Literal dateCreated = feature.getDateAddedToLibrary() == null ? date : f.createLiteral(feature.getDateAddedToLibrary());
        
        IRI userGraphIRI = f.createIRI(userGraph);
        IRI local = f.createIRI(feature.getUri());
        
        // link local one to public uri
        List<Statement> statements2 = new ArrayList<Statement>();
        statements2.add(f.createStatement(local, hasPublicURI, feat, userGraphIRI));
        statements2.add(f.createStatement(local, hasModifiedDate, date, userGraphIRI));
        statements2.add(f.createStatement(local, hasCreatedDate, dateCreated, userGraphIRI));
        statements2.add(f.createStatement(local, RDF.TYPE, featureType, userGraphIRI));
        statements2.add(f.createStatement(local, hasFeatureType, type, userGraphIRI));
        sparqlDAO.addStatements(statements2, userGraphIRI);
        
		return featureURI;
	}

    @Override
    public Feature getFeatureByGlycanLinker(Glycan glycan, Linker linker, String slideLayoutURI, String blockLayoutURI, UserEntity user)
            throws SparqlException, SQLException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        
        String fromString = "FROM <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n";
        String whereClause = "WHERE {";
        String where = " { " + 
                "                   ?f gadr:has_molecule <" + glycan.getUri() +"> . \n" +
                "                   ?f gadr:has_linker <" + linker.getUri() + "> . \n";
        
        if (blockLayoutURI != null) {
            if (blockLayoutURI.contains("public")) {
                where +=  "<" + blockLayoutURI + "> . template:has_spot ?s . ?s gadr:has_feature ?pf . ?f gadr:has_public_uri ?pf . \n";
            } else {
                where +=  "<" + blockLayoutURI + "> template:has_spot ?s . ?s gadr:has_feature ?f . \n";
            }
        } else if (slideLayoutURI != null) {
            if (slideLayoutURI.contains("public")) {
                where += "<" + slideLayoutURI + ">  gadr:has_block ?b . ?b template:has_block_layout ?bl . "
                        + "?bl template:has_spot ?s .  ?s gadr:has_feature ?pf . ?f gadr:has_public_uri ?pf . \n";
            } else {
                where += "<" + slideLayoutURI + "> gadr:has_block ?b . ?b template:has_block_layout ?bl . "
                        + "?bl template:has_spot ?s .  ?s gadr:has_feature ?f . \n";
            }
        }
        
        if (!graph.equals(GlygenArrayRepository.DEFAULT_GRAPH)) {
            // check if the user's private graph has this glycan
            fromString += "FROM <" + graph + ">\n";
            where += "              ?f gadr:has_date_addedtolibrary ?d .\n }";
            where += "  UNION { ?f gadr:has_date_addedtolibrary ?d .\n" 
                    + " ?f gadr:has_public_uri ?pf . ?pf gadr:has_molecule <" + glycan.getUri() +"> . "
                    + " ?pf gadr:has_linker <" + linker.getUri() + "> . \n";
            if (blockLayoutURI != null) {
                if (blockLayoutURI.contains("public")) {
                    where +=  "<" + blockLayoutURI + "> template:has_spot ?s . ?s gadr:has_feature ?pf . \n";
                } else {
                    where +=  "<" + blockLayoutURI + "> template:has_spot ?s . ?s gadr:has_feature ?f . \n";
                }
            } else if (slideLayoutURI != null) {
                if (slideLayoutURI.contains("public")) {
                    where += "<" + slideLayoutURI + "> gadr:has_block ?b . ?b template:has_block_layout ?bl . ?bl template:has_spot ?s .  ?s gadr:has_feature ?pf . \n";
                } else {
                    where += "<" + slideLayoutURI + "> gadr:has_block ?b . ?b template:has_block_layout ?bl . ?bl template:has_spot ?s .  ?s gadr:has_feature ?f . \n";
                }
            } 
            where += "}"; // close union
            
        } else {
            where += "}";
        }
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?f\n");
        queryBuf.append (fromString);
        queryBuf.append (whereClause + where + 
                "               }\n" );
        
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        if (results.isEmpty())
            return null;
        else {
            String featureURI = results.get(0).getValue("f");
            if (featureURI.contains("public")) {
                // retrieve from the public graph
                return getFeatureFromURI(featureURI, null);
            }
            return getFeatureFromURI(featureURI, user);
        }
    }
    
    @Override
    public String getPublicFeatureId(String featureId, UserEntity user) throws SQLException, SparqlException {
        String publicId = null;
        String graph = getGraphForUser(user);
        
        String featureURI = uriPrefix + featureId;
        
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI userGraphIRI = f.createIRI(graph);
        IRI hasPublicURI = f.createIRI(ontPrefix + "has_public_uri");
        IRI feature = f.createIRI(featureURI);
        
        RepositoryResult<Statement> results = sparqlDAO.getStatements(feature, hasPublicURI, null, userGraphIRI);
        while (results.hasNext()) {
            Statement st = results.next();
            String publicURI = st.getObject().stringValue();
            publicId = publicURI.substring(publicURI.lastIndexOf("/")+1);
        }
        
        return publicId;
    }
}
