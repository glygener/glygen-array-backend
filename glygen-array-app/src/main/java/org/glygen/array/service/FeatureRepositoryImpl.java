package org.glygen.array.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.LinkerClassification;
import org.glygen.array.persistence.rdf.LinkerType;
import org.glygen.array.persistence.rdf.ProteinLinker;
import org.glygen.array.persistence.rdf.SequenceBasedLinker;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.glygen.array.persistence.rdf.SmallMoleculeLinker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(value="sesameTransactionManager") 
public class FeatureRepositoryImpl extends GlygenArrayRepositoryImpl implements FeatureRepository {
	
	final static String featureTypePredicate = ontPrefix + "Feature";
	final static String hasLinkerPredicate = ontPrefix + "has_linker";
	final static String hasMoleculePredicate = ontPrefix + "has_molecule";
	final static String hasRatioPredicate = ontPrefix + "has_ratio";
	final static String hasPositionPredicate = ontPrefix + "has_molecule_position";
	final static String hasPositionValuePredicate = ontPrefix + "has_position";
	
	@Autowired
	GlycanRepository glycanRepository;
	
	@Autowired
	LinkerRepository linkerRepository;

	@Override
	public String addFeature(Feature feature, UserEntity user) throws SparqlException, SQLException {
		String graph = null;
		if (feature == null || feature.getLinker() == null)
			// cannot add 
			throw new SparqlException ("Not enough information is provided to register a feature");
		
		if (feature.getGlycans() == null || feature.getGlycans().isEmpty()) {
			// check if the linker is a sequence-based linker and if so, try to extract the glycans
			// from the linker sequence and populate positionMap
			if (feature.getLinker().getType() == LinkerType.PEPTIDE_LINKER || feature.getLinker().getType() == LinkerType.PROTEIN_LINKER) {
				Map<Glycan, Integer>  positionMap = ((SequenceBasedLinker)feature.getLinker()).extractGlycans();
				feature.setPositionMap(positionMap);
				for (Glycan g: positionMap.keySet()) {
					String seq = ((SequenceDefinedGlycan)g).getSequence();
					if (seq != null) {
						String existing = glycanRepository.getGlycanBySequence(((SequenceDefinedGlycan)g).getSequence(), user);
						if (existing == null) {
							// add the glycan
							existing = glycanRepository.addGlycan(g, user);
						}
						g.setUri(existing);
						feature.addGlycan(g);
					} else {
						logger.error("Glycan in the feature with the following sequence cannot be located: " + seq);
					}
				}
			}
		}
		// check if there is already a private graph for user
		graph = getGraphForUser(user);
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI featureType = f.createIRI(featureTypePredicate);
		String featureURI = generateUniqueURI(uriPrefix + "F");
		IRI feat = f.createIRI(featureURI);
		IRI graphIRI = f.createIRI(graph);
		IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
		IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
		IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
		IRI hasLinker = f.createIRI(hasLinkerPredicate);
		IRI hasMolecule = f.createIRI(hasMoleculePredicate);
		IRI hasRatio = f.createIRI (hasRatioPredicate);
		IRI hasPositionContext = f.createIRI(hasPositionPredicate);
		IRI hasPosition = f.createIRI(hasPositionValuePredicate);
		Literal date = f.createLiteral(new Date());
		
		List<Statement> statements = new ArrayList<Statement>();
		
		statements.add(f.createStatement(feat, RDF.TYPE, featureType, graphIRI));
		statements.add(f.createStatement(feat, hasCreatedDate, date, graphIRI));
		statements.add(f.createStatement(feat, hasAddedToLibrary, date, graphIRI));
		statements.add(f.createStatement(feat, hasModifiedDate, date, graphIRI));
		
		Linker linker = feature.getLinker();
		if (linker.getUri() == null) {
		    if (linker.getId() != null)
		        linker.setUri(uriPrefix + linker.getId());	
		}
	    if (linker.getUri() == null) {
	        if (linker.getType() == LinkerType.SMALLMOLECULE_LINKER) {
		        String existing = null;
		        if (((SmallMoleculeLinker) linker).getPubChemId() != null) {
		            existing = linkerRepository.getLinkerByField(((SmallMoleculeLinker) linker).getPubChemId().toString(), LinkerRepositoryImpl.hasPubChemIdProperty, "long", user);
		        } else if (((SmallMoleculeLinker) linker).getInChiKey() != null) {
		            existing = linkerRepository.getLinkerByField(((SmallMoleculeLinker) linker).getInChiKey(), LinkerRepositoryImpl.hasInchiKeyProperty, "string", user);
		        }
		        linker.setUri(existing);
		        if (existing == null) {
		            String uri = linkerRepository.addLinker(linker, user);
	                linker.setUri(uri);
		        } 
	        } else if (linker.getType() == LinkerType.PEPTIDE_LINKER || linker.getType() == LinkerType.PROTEIN_LINKER) {
	            String sequence = ((SequenceBasedLinker)linker).getSequence();
	            if (sequence != null) {
	                String existing = linkerRepository.getLinkerByField(sequence, "has_sequence", "string", user);
	                linker.setUri(existing);
	                if (existing == null) {
	                    String uri = linkerRepository.addLinker(linker, user);
	                    linker.setUri(uri);
	                }
	            }
	        }
	    }
		
		IRI linkerIRI = f.createIRI(linker.getUri());
		statements.add(f.createStatement(feat, hasLinker, linkerIRI, graphIRI));
		
		for (Glycan g: feature.getGlycans()) {
			if (g.getUri() == null) {
				if (g.getId() != null) {
					g.setUri(uriPrefix + g.getId());
				} else {
					String uri = glycanRepository.addGlycan(g, user, true);
					g.setUri(uri);
				}
			}
			else {
			    String uri = glycanRepository.addGlycan(g, user, true);
                g.setUri(uri); 
			}
			
			IRI glycanIRI = f.createIRI(g.getUri());
			statements.add(f.createStatement(feat, hasMolecule, glycanIRI, graphIRI));
			
			Integer position = feature.getPosition(g);
			if (position != null) {
				Literal pos = f.createLiteral(position);
				String positionContextURI = generateUniqueURI(uriPrefix + "PC");
				IRI positionContext = f.createIRI(positionContextURI);
				statements.add(f.createStatement(feat, hasPositionContext, positionContext, graphIRI));
				statements.add(f.createStatement(positionContext, hasMolecule, glycanIRI, graphIRI));
				statements.add(f.createStatement(positionContext, hasPosition, pos, graphIRI));
			}
		}
		
		if (feature.getRatio() != null) {
			Literal ratio = f.createLiteral(feature.getRatio());
			statements.add(f.createStatement(feat, hasRatio, ratio, graphIRI));
		}
		
		sparqlDAO.addStatements(statements, graphIRI);
		
		return featureURI;
	}

	@Override
	public List<Feature> getFeatureByUser(UserEntity user) throws SparqlException, SQLException {
		return getFeatureByUser(user, 0, -1, "id", 0);
	}

	@Override
	public List<Feature> getFeatureByUser(UserEntity user, int offset, int limit, String field, int order)
			throws SparqlException, SQLException {
		List<Feature> features = new ArrayList<Feature>();
		
		String sortPredicate = getSortPredicate (field);
		
		// get all featureURIs from user's private graph
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
					" ?s rdf:type  <http://purl.org/gadr/data#Feature>. \n" +
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

	@Override
	public int getFeatureCountByUser(UserEntity user) throws SQLException, SparqlException {
		String graph = getGraphForUser(user);
		return getCountByUserByType(graph, "Feature");
	}

	@Override
	public void deleteFeature(String featureId, UserEntity user) throws SparqlException, SQLException {
		String graph;
		
		graph = getGraphForUser(user);
		if (graph != null) {
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
				return;
			}
		}
	}

	@Override
	public Feature getFeatureFromURI(String featureURI, UserEntity user) throws SparqlException, SQLException {
		Feature featureObject = null;
		String graph = getGraphForUser(user);
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI feature = f.createIRI(featureURI);
		IRI graphIRI = f.createIRI(graph);
		IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
		IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
		IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
		IRI hasLinker = f.createIRI(hasLinkerPredicate);
		IRI hasMolecule = f.createIRI(hasMoleculePredicate);
		IRI hasRatio = f.createIRI (hasRatioPredicate);
		IRI hasPositionContext = f.createIRI(hasPositionPredicate);
		IRI hasPosition = f.createIRI(hasPositionValuePredicate);
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(feature, null, null, graphIRI);
		List<Glycan> glycans = new ArrayList<Glycan>();
		Map<Glycan, Integer> positionMap = new HashMap<Glycan, Integer>();
		if (statements.hasNext()) {
			featureObject = new Feature();
			featureObject.setUri(featureURI);
			featureObject.setId(featureURI.substring(featureURI.lastIndexOf("/")+1));
			featureObject.setGlycans(glycans);
			featureObject.setPositionMap(positionMap);
		}
		while (statements.hasNext()) {
			Statement st = statements.next();
			if (st.getPredicate().equals(hasLinker)) {
				Value value = st.getObject();
				if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
					String linkerURI = value.stringValue();
					Linker linker = linkerRepository.getLinkerFromURI(linkerURI, graph);
					featureObject.setLinker(linker);
				}
			} else if (st.getPredicate().equals(hasMolecule)) {
				Value value = st.getObject();
				if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
					String glycanURI = value.stringValue();
					Glycan glycan = glycanRepository.getGlycanFromURI(glycanURI, user);
					glycans.add(glycan);
				}
			} else if (st.getPredicate().equals(hasRatio)) {
				Value value = st.getObject();
				if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
					try {
						double ratio = Double.parseDouble(value.stringValue());
						featureObject.setRatio(ratio);
					} catch (NumberFormatException e) {
						logger.warn ("Feature ratio is invalid", e);
					}
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
					positionMap.put(glycanInContext, position);
				}
			}
		}
		
		return featureObject;
	}
}