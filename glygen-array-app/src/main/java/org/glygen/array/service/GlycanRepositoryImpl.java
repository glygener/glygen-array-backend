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
import org.eurocarbdb.MolecularFramework.io.SugarImporterException;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorException;
import org.glycoinfo.WURCSFramework.io.GlycoCT.WURCSExporterGlycoCT;
import org.glycoinfo.WURCSFramework.util.WURCSException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.Creator;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.GlycanSequenceFormat;
import org.glygen.array.persistence.rdf.GlycanType;
import org.glygen.array.persistence.rdf.MassOnlyGlycan;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.glygen.array.persistence.rdf.UnknownGlycan;
import org.glygen.array.persistence.rdf.data.ChangeLog;
import org.glygen.array.util.GlytoucanUtil;
import org.jline.utils.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(value="sesameTransactionManager") 
public class GlycanRepositoryImpl extends GlygenArrayRepositoryImpl implements GlycanRepository  {
    
    @Autowired
    QueryHelper queryHelper;
    
    @org.springframework.beans.factory.annotation.Value("${glygen.glytoucanregistration}")
    String glytoucanregistration;
    

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
	
	String addBasicInfoForGlycan (Glycan g, String graph) throws SparqlException, SQLException {
	    String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
		ValueFactory f = sparqlDAO.getValueFactory();
		String glycanURI = generateUniqueURI(uriPrefix, allGraphs) + "GAR";
		IRI glycan = f.createIRI(glycanURI);
		Literal date = f.createLiteral(new Date());
		IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
		IRI hasGlycanType = f.createIRI(ontPrefix + "has_type");
		Literal type = f.createLiteral(g.getType().name());
		IRI graphIRI = f.createIRI(graph);
		Literal glycanLabel = g.getName() == null ? null : f.createLiteral(g.getName());
		Literal glycanComment = g.getDescription() == null ? null : f.createLiteral(g.getDescription());
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
			String seqURI = generateUniqueURI(uriPrefix + "Seq", graph);
		
			IRI sequence = f.createIRI(seqURI);
			IRI glycan = f.createIRI(glycanURI);
			
			String glyToucanId = null;
			String glyToucanHash = null;
			if (g.getGlytoucanId() == null && !noGlytoucanRegistration) {
				// check and register to GlyToucan
				try {
				    String wurcs = null;
                    if (((SequenceDefinedGlycan) g).getSequenceType() == GlycanSequenceFormat.GLYCOCT) {
    					WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
    					exporter.start(g.getSequence());
    					wurcs = exporter.getWURCS();
                    } else if (((SequenceDefinedGlycan) g).getSequenceType() == GlycanSequenceFormat.WURCS) {
                        wurcs = g.getSequence();
                    }
                    if (wurcs != null) {
    					glyToucanId = GlytoucanUtil.getInstance().registerGlycan(wurcs);
    					logger.info("Got glytoucan id after registering the glycan:" + glyToucanId);
    					if (glyToucanId == null || glyToucanId.length() > 10) {
    					    // this is new registration, hash returned
    					    glyToucanHash = glyToucanId;
    					    glyToucanId = null;
    					    logger.info("got glytoucan hash, no accession number!");
    					}
                    }
				} catch (Exception e) {
					logger.warn("Cannot register glytoucanId with the given sequence", g.getSequence());
				}
			} else if (g.getGlytoucanId() == null) {
				// check if it is already in GlyToucan
				try {
				    String wurcs = null;
                    if (((SequenceDefinedGlycan) g).getSequenceType() == GlycanSequenceFormat.GLYCOCT) {
                        WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
                        exporter.start(g.getSequence());
                        wurcs = exporter.getWURCS();
                    } else if (((SequenceDefinedGlycan) g).getSequenceType() == GlycanSequenceFormat.WURCS) {
                        wurcs = g.getSequence();
                    }
                    if (wurcs != null) {
    					glyToucanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
    					logger.info("Got glytoucan id for new glycan:" + glyToucanId);
    					if (glyToucanId == null || glyToucanId.length() > 10) {
                            // this is new registration, hash returned
                            glyToucanHash = glyToucanId;
                            glyToucanId = null;
                            logger.info("got glytoucan hash, no accession number!");
                        }
                    }
				} catch (Exception e) {
					logger.warn("Cannot get glytoucanId with the given sequence", g.getSequence());
				}
			}
			
			Literal glytoucanLit = glyToucanId == null ? null : f.createLiteral(glyToucanId);
			Literal glytoucanHashLit = glyToucanHash == null ? null : f.createLiteral(glyToucanHash);
			Literal sequenceValue = f.createLiteral(g.getSequence());
			Literal format = f.createLiteral(g.getSequenceType().getLabel());
			
			IRI hasSequence = f.createIRI(ontPrefix + "has_sequence");
			IRI hasGlytoucanId = f.createIRI(ontPrefix + "has_glytoucan_id");
			IRI hasGlytoucanHash = f.createIRI(ontPrefix + "has_glytoucan_registration_hash");
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
			if (glytoucanHashLit != null) statements.add(f.createStatement(glycan, hasGlytoucanHash, glytoucanHashLit, graphIRI));
			statements.add(f.createStatement(sequence, hasSequenceValue, sequenceValue, graphIRI));
			statements.add(f.createStatement(sequence, hasSequenceFormat, format, graphIRI));
			if (mass != null) statements.add(f.createStatement(glycan, hasMass, mass, graphIRI));
			
			sparqlDAO.addStatements(statements, graphIRI);
		} else {
			logger.debug("The glycan already exists in global repository. URI: " + existing);
			String publicURI = existing;
			IRI glycan = f.createIRI(publicURI);
			
			glycanURI = generateUniqueURI(uriPrefix, graph) + "GAR";
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
			Literal glycanComment = g.getDescription() == null ? f.createLiteral("") : f.createLiteral(g.getDescription());
			IRI hasGlycanType = f.createIRI(ontPrefix + "has_type");
			Literal type = f.createLiteral(g.getType().name());
			IRI glycanType = f.createIRI(ontPrefix + "Glycan");
			
			statements.add(f.createStatement(localGlycan, RDF.TYPE, glycanType, graphIRI));
			statements.add(f.createStatement(localGlycan, hasGlycanType, type, graphIRI));
			statements.add(f.createStatement(localGlycan, hasPublicURI, glycan, graphIRI));
			statements.add(f.createStatement(localGlycan, hasAddedToLibrary, date, graphIRI));
			statements.add(f.createStatement(localGlycan, hasModifiedDate, date, graphIRI));
			statements.add(f.createStatement(localGlycan, RDFS.LABEL, glycanLabel, graphIRI));
			statements.add(f.createStatement(localGlycan, hasInternalId, internalId, graphIRI));
			statements.add(f.createStatement(localGlycan, RDFS.COMMENT, glycanComment, graphIRI));
			
			sparqlDAO.addStatements(statements, graphIRI);
			
			//addAliasForGlycan(g.getUri().substring(g.getUri().lastIndexOf("/")+1), g.getName(), user);
		}
		
		return glycanURI;
		
	}
	
	@Override
	public void deleteGlycan(String glycanId, UserEntity user) throws SQLException, SparqlException {
		String graph = null;
		String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        }
        else
            graph = getGraphForUser(user);
		if (graph != null) {
		    if (canDelete(uriPre + glycanId, graph)) {
    			// check to see if the given glycanId is in this graph
    			Glycan existing = getGlycanFromURI (uriPre + glycanId, user);
    			if (existing != null) {
    				deleteGlycanByURI (uriPre + glycanId, graph);
    				return;
    			}
		    } else {
		        throw new IllegalArgumentException("Cannot delete glycan " + glycanId + ". It is used in a feature"); 
		    }
		}
	}
	
	boolean canDelete (String glycanURI, String graph) throws SparqlException, SQLException { 
        boolean canDelete = true;
        List<SparqlEntity> results = queryHelper.canDeleteQuery(glycanURI, graph);
        if (!results.isEmpty())
            canDelete = false;
        
        return canDelete;
    }

	private void deleteGlycanByURI(String uri, String graph) throws SparqlException {
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI glycan = f.createIRI(uri);
		IRI graphIRI = f.createIRI(graph);
		IRI hasSequence = f.createIRI(ontPrefix + "has_sequence");
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(glycan, hasSequence, null, graphIRI);
		while (statements.hasNext()) {
		    Statement st = statements.next();
		    Value v = st.getObject();
            String sequenceURI = v.stringValue();
            IRI seq = f.createIRI(sequenceURI);
            RepositoryResult<Statement> statements1 = sparqlDAO.getStatements(seq, null, null, graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(statements1), graphIRI); 
		}
		RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(glycan, null, null, graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(statements2), graphIRI);
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
		List<SparqlEntity> results = queryHelper.findGlycanInGraphBySequence(sequence, graph);
		if (results.size() == 0) 
			return null;
		
		for (SparqlEntity result: results) {
		    String glycanURI = result.getValue("s");
		    // return the non-public one
		    if (!glycanURI.contains("public"))
		        return glycanURI;
		}
		
		// if there is only the public one
		for (SparqlEntity result: results) {
            String glycanURI = result.getValue("s");
            return glycanURI;
        }
		
		return null;
	}
	
	@Override
	public Glycan getGlycanById(String glycanId, UserEntity user) throws SparqlException, SQLException {
		// make sure the glycan belongs to this user
	    String graph = null;
	    String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        } else
            graph = getGraphForUser(user);
		List<SparqlEntity> results = queryHelper.retrieveById(uriPre + glycanId, graph);
		if (results.isEmpty())
			return null;
		else {
			return getGlycanFromURI(uriPre + glycanId, user);
		}
	}
	
	@Override
	public Glycan getGlycanByInternalId(String internalId, UserEntity user) throws SparqlException, SQLException {
	    String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else
            graph = getGraphForUser(user);
		List<SparqlEntity> results = queryHelper.retrieveGlycanByInternalId(internalId, graph);
		if (results.isEmpty())
			return null;
		else {
			String glycanURI = results.get(0).getValue("s");
			return getGlycanFromURI(glycanURI, user);
		}
	}

	@Override
	public Glycan getGlycanByLabel(String label, UserEntity user) throws SparqlException, SQLException {
	    String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else
            graph = getGraphForUser(user);
		List<SparqlEntity> results = queryHelper.retrieveByLabel(label, ontPrefix + "Glycan", graph);
		if (results.isEmpty())
			return null;
		else {
		    String glycanURI = results.get(0).getValue("s");
		    if (glycanURI.contains("public")) {
		        return getGlycanFromURI(glycanURI, null);
		    }
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
	    String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else
            graph = getGraphForUser(user);
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
		return getGlycanByUser(user, offset, limit, field, order, null);
	}
	
	
	
	
	@Override
    public List<Glycan> getGlycanByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue) throws SparqlException, SQLException {
        List<Glycan> glycans = new ArrayList<Glycan>();
        
        // get all glycanURIs from user's private graph
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else
            graph = getGraphForUser(user);
        if (graph != null) {
            
            List<SparqlEntity> results = queryHelper.retrieveGlycanByUser(offset, limit, field, order, searchValue, graph);
            int i=0;
            for (SparqlEntity sparqlEntity : results) {
                String glycanURI = sparqlEntity.getValue("s");
                if (user == null || !glycanURI.contains("public")) {
                    Glycan glycan = getGlycanFromURI(glycanURI, user);
                    if (glycan != null && i < limit) {
                        glycans.add(glycan);    
                        i++;
                    }
                }
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
		String graph = null;
		if (user == null)
		    graph = DEFAULT_GRAPH;
		else
		    graph = getGraphForUser(user);
		return getCountByUserByType(graph, ontPrefix + "Glycan");
	}
	
	private GlycanType getGlycanTypeForGlycan (String glycanURI, String graph) throws SparqlException {
		List<SparqlEntity> results = queryHelper.retrieveGlycanTypeByGlycan(glycanURI, graph);
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
		
		String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            if (glycanURI.contains("public"))
                graph = DEFAULT_GRAPH;
            else
                graph = getGraphForUser(user);
        }
		
		GlycanType type = getGlycanTypeForGlycan(glycanURI, graph);
		
		if (type == null)
		    type = GlycanType.UNKNOWN;
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI glycan = f.createIRI(glycanURI);
		IRI graphIRI = f.createIRI(graph);
		IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
		IRI hasPublicURI = f.createIRI(ontPrefix + "has_public_uri");
		IRI hasSequence = f.createIRI(ontPrefix + "has_sequence");
		IRI hasGlytoucanId = f.createIRI(ontPrefix + "has_glytoucan_id");
		IRI hasGlytoucanHash = f.createIRI(ontPrefix + "has_glytoucan_registration_hash");
		IRI hasMass = f.createIRI(ontPrefix + "has_mass");
		IRI hasAlias = f.createIRI(ontPrefix + "has_alias");
		IRI hasInternalId = f.createIRI(ontPrefix + "has_internal_id");
		IRI hasSequenceValue = f.createIRI(ontPrefix + "has_sequence_value");
		IRI hasSequenceFormat = f.createIRI(ontPrefix + "has_sequence_format");
		IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
		IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		IRI createdBy= f.createIRI(ontPrefix + "created_by");
		
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
			if (user != null) {
    			Creator owner = new Creator ();
    			owner.setUserId(user.getUserId());
    			owner.setName(user.getUsername());
    			glycanObject.setUser(owner);
			} 
			if (glycanURI.contains("public"))
			    glycanObject.setIsPublic(true);
		}
		while (statements.hasNext()) {
			Statement st = statements.next();
			if (st.getPredicate().equals(hasGlytoucanId)) {
				Value glytoucanId = st.getObject();
				if (glycanObject instanceof SequenceDefinedGlycan)
					((SequenceDefinedGlycan)glycanObject).setGlytoucanId(glytoucanId.stringValue()); 
			} else if (st.getPredicate().equals(hasGlytoucanHash)) {
			    // need to check if the accession number is available and update glycan
			    Value glytoucanHash = st.getObject();
			    if (glycanObject instanceof SequenceDefinedGlycan) {
                    ((SequenceDefinedGlycan)glycanObject).setGlytoucanHash(glytoucanHash.stringValue()); 
			    }
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
			} else if (st.getPredicate().equals(createdBy)) {
				Value label = st.getObject();
				Creator creator = new Creator();
				creator.setName(label.stringValue());
				glycanObject.setUser(creator);
			} else if (st.getPredicate().equals(RDFS.COMMENT)) {
				Value comment = st.getObject();
				glycanObject.setDescription(comment.stringValue());
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
									((SequenceDefinedGlycan)glycanObject).setSequenceType(GlycanSequenceFormat.forValue(formatString.stringValue()));
							}  
						}
					}
					
				}
			}
		}
		
		// check if glytoucanHash exists, if so we need to check if accession number is available now
		if (glycanObject instanceof SequenceDefinedGlycan) {
		    if (((SequenceDefinedGlycan) glycanObject).getGlytoucanHash() != null && !((SequenceDefinedGlycan) glycanObject).getGlytoucanHash().isEmpty()) {
                try {
                    String wurcs = null;
                    if (((SequenceDefinedGlycan) glycanObject).getSequenceType() == GlycanSequenceFormat.GLYCOCT) {
                        WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
                        exporter.start(((SequenceDefinedGlycan) glycanObject).getSequence());
                        wurcs = exporter.getWURCS();
                    }
                    else if (((SequenceDefinedGlycan) glycanObject).getSequenceType() == GlycanSequenceFormat.WURCS) {
                        wurcs = ((SequenceDefinedGlycan) glycanObject).getSequence();
                    }
                    if (wurcs != null) {
                        String glyToucanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
                        if (glyToucanId != null && glyToucanId.length() < 10) {
                            ((SequenceDefinedGlycan) glycanObject).setGlytoucanId(glyToucanId);
                            ((SequenceDefinedGlycan) glycanObject).setGlytoucanHash(null);
                            // need to update glycan in the repository
                            Literal glytoucanLit = f.createLiteral(glyToucanId);
                            // remove glytoucanhash
                            sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(glycan, hasGlytoucanHash, null, graphIRI)), graphIRI);
                            List<Statement> statements2 = new ArrayList<Statement>();
                            statements2.add(f.createStatement(glycan, hasGlytoucanId, glytoucanLit, graphIRI));
                            sparqlDAO.addStatements(statements2, graphIRI);
                        }
                    }
                } catch (SugarImporterException | GlycoVisitorException | WURCSException e) {
                    logger.error("Cannot convert to WURCS", e);
                }
                
		    }
		}
		retrieveChangeLog (glycanObject, glycanObject.getUri(), graph);
		return glycanObject;
	}
	
	@Override
    public void updateGlycan(Glycan g, UserEntity user, ChangeLog change) throws SparqlException, SQLException {
        String graph = getGraphForUser(user);
        Glycan existing = getGlycanFromURI(g.getUri(), user);
        if (graph != null && existing !=null) {
            updateGlycanInGraph(g, graph);
            if (change != null) {
                saveChangeLog(change, existing.getUri(), graph);
            }
        }
    }
	
	@Override
	public void updateGlycan(Glycan g, UserEntity user) throws SparqlException, SQLException {
		updateGlycan(g, user, null);
	}

	void updateGlycanInGraph (Glycan g, String graph) throws SparqlException {	
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI graphIRI = f.createIRI(graph);
		String glycanURI = g.getUri();
		IRI glycan = f.createIRI(glycanURI);
		Literal glycanLabel = f.createLiteral(g.getName());
		Literal glycanComment = g.getDescription() == null ? f.createLiteral("") : f.createLiteral(g.getDescription());
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

	
    public String makePublic(Glycan glycan, UserEntity user) throws SparqlException, SQLException {
        String graph = getGraphForUser(user);
        String existingURI = null;
        switch (glycan.getType()) {
        case SEQUENCE_DEFINED:
            existingURI = getGlycanBySequence(((SequenceDefinedGlycan) glycan).getSequence());
            if (existingURI == null) {
                // check by label if any
                if (glycan.getName() != null && !glycan.getName().isEmpty()) {
                    List <SparqlEntity> results = queryHelper.retrieveByLabel(glycan.getName(), ontPrefix + "Glycan", null);
                    if (results.isEmpty()) {
                        // make it public
                        // need to create the glycan in the public graph, link the user's version to public one
                        deleteGlycanByURI(uriPrefix + glycan.getId(), graph);  // delete existing info
                        updateGlycanInGraph(glycan, graph);  // only keep user specific info in the local repository
                        return addPublicGlycan(glycan, null, graph, user.getUsername());
                        
                    } else {
                        // same name glycan exist in public graph
                        // throw exception
                        logger.debug("Glycan " + glycan.getName() +" is already public");
                        return null;
                        //throw new GlycanExistsException("Glycan with name " + glycan.getName() + " already exists in public graph");
                    }
                } else {
                    // make it public
                    // need to create the glycan in the public graph, link the user's version to public one
                    deleteGlycanByURI(uriPrefix + glycan.getId(), graph);  // delete existing info
                    updateGlycanInGraph(glycan, graph);  // only keep user specific info in the local repository
                    return addPublicGlycan(glycan, null, graph, user.getUsername());
                }
            } else {
                deleteGlycanByURI(uriPrefix + glycan.getId(), graph); // delete existing info
                updateGlycanInGraph(glycan, graph);  // only keep user specific info in the local repository
                // need to link the user's version to the existing URI
                return addPublicGlycan(glycan, existingURI, graph, user.getUsername());
            }
        default:
            // check by label if any
            if (glycan.getName() != null && !glycan.getName().isEmpty()) {
                List <SparqlEntity> results = queryHelper.retrieveByLabel(glycan.getName(), ontPrefix + "Glycan", null);
                if (results.isEmpty()) {
                    // make it public
                    // need to create the glycan in the public graph, link the user's version to public one
                    deleteGlycanByURI(uriPrefix + glycan.getId(), graph);  // delete existing info
                    updateGlycanInGraph(glycan, graph);  // only keep user specific info in the local repository
                    return addPublicGlycan(glycan, null, graph, user.getUsername());
                } else {
                    // same name glycan exist in public graph
                    // throw exception
                    logger.debug("Glycan " + glycan.getName() +" is already public");
                    return null;
                    //throw new GlycanExistsException("Glycan with name " + glycan.getName() + " already exists in public graph");
                }
            } else {
                // make it public
                // need to create the glycan in the public graph, link the user's version to public one
                deleteGlycanByURI(uriPrefix + glycan.getId(), graph);  // delete existing info
                updateGlycanInGraph(glycan, graph);  // only keep user specific info in the local repository
                return addPublicGlycan(glycan, null, graph, user.getUsername());
            }   
        }
    }
    
    public String addPublicGlycan (Glycan glycan, String publicURI, String userGraph, String creator) throws SparqlException {
    	boolean existing = publicURI != null;
        if (publicURI == null) {
            publicURI = generateUniqueURI(uriPrefixPublic, userGraph) + "GAR";  
        } 
        
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI publicGlycan = f.createIRI(publicURI);
       
        IRI graphIRI = f.createIRI(userGraph);
        IRI publicGraphIRI = f.createIRI(DEFAULT_GRAPH);
        Literal date = f.createLiteral(new Date());
        IRI createdBy= f.createIRI(ontPrefix + "created_by");
        IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
        IRI hasGlycanType = f.createIRI(ontPrefix + "has_type");
        Literal type = f.createLiteral(glycan.getType().name());
        Literal glycanLabel = glycan.getName() == null ? null : f.createLiteral(glycan.getName());
        IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
        IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
        IRI glycanType = f.createIRI(ontPrefix + "Glycan");
        Literal user = f.createLiteral(creator);
        Literal dateAdded = f.createLiteral(glycan.getDateAddedToLibrary());
        
        List<Statement> statements = new ArrayList<Statement>();
        
        if (!existing) {
	        statements.add(f.createStatement(publicGlycan, RDF.TYPE, glycanType, publicGraphIRI));
	        statements.add(f.createStatement(publicGlycan, hasGlycanType, type, publicGraphIRI));
	        statements.add(f.createStatement(publicGlycan, hasCreatedDate, date, publicGraphIRI));
	        if (glycanLabel != null) statements.add(f.createStatement(publicGlycan, RDFS.LABEL, glycanLabel, publicGraphIRI));
	        statements.add(f.createStatement(publicGlycan, hasAddedToLibrary, dateAdded, publicGraphIRI));
	        statements.add(f.createStatement(publicGlycan, hasModifiedDate, date, publicGraphIRI));
	        statements.add(f.createStatement(publicGlycan, createdBy, user, publicGraphIRI));
        }
        // add has_public_uri predicate to user's graph
        
        IRI localGlycan = f.createIRI(glycan.getUri());
        
        IRI hasPublicURI = f.createIRI(ontPrefix + "has_public_uri");
        
        
        List<Statement> statements2 = new ArrayList<Statement>();
        statements2.add(f.createStatement(localGlycan, hasPublicURI, publicGlycan, graphIRI));
        statements2.add(f.createStatement(localGlycan, hasModifiedDate, date, graphIRI));
        statements2.add(f.createStatement(localGlycan, hasAddedToLibrary, dateAdded, graphIRI));
        statements2.add(f.createStatement(localGlycan, hasGlycanType, type, graphIRI));
        statements2.add(f.createStatement(localGlycan, RDF.TYPE, glycanType, graphIRI));
        
        if (!existing) {
	        // add additionalInfo based on the type of Glycan
	        switch (glycan.getType()) {
	        case SEQUENCE_DEFINED:
	            // if glytoucanid is null, register with glytoucan or retrieve it
	            String glyToucanId = null;
	            String glyToucanHash = null;
	            if (((SequenceDefinedGlycan) glycan).getGlytoucanId() == null) {
	                // check and register to GlyToucan
	                try {
	                    String wurcs = null;
	                    if (((SequenceDefinedGlycan) glycan).getSequenceType() == GlycanSequenceFormat.GLYCOCT) {
    	                    WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
    	                    exporter.start(((SequenceDefinedGlycan) glycan).getSequence().trim());
    	                    wurcs = exporter.getWURCS();
	                    } else if (((SequenceDefinedGlycan) glycan).getSequenceType() == GlycanSequenceFormat.WURCS) {
	                        wurcs = ((SequenceDefinedGlycan) glycan).getSequence().trim();
	                    }
	                    if (wurcs != null) {
    	                    glyToucanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);    
    	                    if (glyToucanId == null) { // need to register
    	                        if (glytoucanregistration != null && glytoucanregistration.equalsIgnoreCase("true")) {
        	                        glyToucanId = GlytoucanUtil.getInstance().registerGlycan(wurcs);
        	                        if (glyToucanId == null || glyToucanId.length() > 10) {
        	                            // this is new registration, hash returned
        	                            glyToucanHash = glyToucanId;
        	                            glyToucanId = null;
        	                            ((SequenceDefinedGlycan) glycan).setGlytoucanHash(glyToucanHash);
        	                        } else { // in case it returns an id immediately, it does not happen though
        	                            ((SequenceDefinedGlycan) glycan).setGlytoucanId(glyToucanId);
        	                        }
    	                        }
    	                    } else if (glyToucanId.length() < 10) { // it might return the hash instead of glytoucan id 
    	                        ((SequenceDefinedGlycan) glycan).setGlytoucanId (glyToucanId);
    	                    }
	                    }
	                } catch (Exception e) {
	                    logger.warn("Cannot register glytoucanId with the given sequence", ((SequenceDefinedGlycan) glycan).getSequence());
	                }
	            } 
	            // add sequence and glytoucanid if any
	            String seqURI = generateUniqueURI(uriPrefixPublic + "Seq", userGraph);
	            IRI sequence = f.createIRI(seqURI);
	            Literal glytoucanLit = ((SequenceDefinedGlycan) glycan).getGlytoucanId() == null ? 
	                    null : f.createLiteral(((SequenceDefinedGlycan) glycan).getGlytoucanId());
	            Literal glytoucanHashLit = ((SequenceDefinedGlycan) glycan).getGlytoucanHash() == null ? 
	                    null : f.createLiteral(((SequenceDefinedGlycan) glycan).getGlytoucanHash());
	            Literal sequenceValue = f.createLiteral(((SequenceDefinedGlycan) glycan).getSequence());
	            Literal format = f.createLiteral(((SequenceDefinedGlycan) glycan).getSequenceType().getLabel());
	            
	            IRI hasSequence = f.createIRI(ontPrefix + "has_sequence");
	            IRI hasGlytoucanId = f.createIRI(ontPrefix + "has_glytoucan_id");
	            IRI hasGlytoucanHash = f.createIRI(ontPrefix + "has_glytoucan_registration_hash");
	            IRI hasSequenceValue = f.createIRI(ontPrefix + "has_sequence_value");
	            IRI hasSequenceFormat = f.createIRI(ontPrefix + "has_sequence_format");
	            IRI sequenceType = f.createIRI(ontPrefix + "Sequence");
	            Literal mass = ((MassOnlyGlycan) glycan).getMass() == null ? null : f.createLiteral(((MassOnlyGlycan) glycan).getMass());
	            IRI hasMass = f.createIRI(ontPrefix + "has_mass");
	            
	            statements.add(f.createStatement(sequence, RDF.TYPE, sequenceType, publicGraphIRI));
	            statements.add(f.createStatement(publicGlycan, hasSequence, sequence, publicGraphIRI));
	            if (glytoucanLit != null) statements.add(f.createStatement(publicGlycan, hasGlytoucanId, glytoucanLit, publicGraphIRI));
	            if (glytoucanHashLit != null) statements.add(f.createStatement(publicGlycan, hasGlytoucanHash, glytoucanHashLit, publicGraphIRI));
	            statements.add(f.createStatement(sequence, hasSequenceValue, sequenceValue, publicGraphIRI));
	            statements.add(f.createStatement(sequence, hasSequenceFormat, format, publicGraphIRI));
	            if (mass != null) statements.add(f.createStatement(publicGlycan, hasMass, mass, publicGraphIRI));
	            break;
	        case MASS_ONLY:
	            // add mass
	            mass = ((MassOnlyGlycan) glycan).getMass() == null ? null : f.createLiteral(((MassOnlyGlycan) glycan).getMass());
	            hasMass = f.createIRI(ontPrefix + "has_mass");
	            if (mass != null) statements.add(f.createStatement(publicGlycan, hasMass, mass, publicGraphIRI));
	            break;
	        default:
	            break;
	        }
	        
	        sparqlDAO.addStatements(statements, publicGraphIRI);
        }
        sparqlDAO.addStatements(statements2, graphIRI);
        
        return publicURI;
    }
}
