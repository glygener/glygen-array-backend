package org.glygen.array.service;

import java.io.IOException;
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
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.glygen.array.exception.GlycanExistsException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.SlideLayoutEntity;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.SlideLayoutRepository;
import org.glygen.array.persistence.rdf.Block;
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.Creator;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.Spot;
import org.grits.toolbox.glycanarray.library.om.layout.LevelUnit;
import org.grits.toolbox.glycanarray.om.model.UnitOfLevels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional(value="sesameTransactionManager") 
public class LayoutRepositoryImpl extends GlygenArrayRepositoryImpl implements LayoutRepository {
	
	@Autowired
	GlycanRepository glycanRepository;
	
	@Autowired
	LinkerRepository linkerRepository;
	
	@Autowired
	FeatureRepository featureRepository;
	
	@Autowired
	SlideLayoutRepository slideLayoutRepository;
	
	Map<String, SlideLayout> slideLayoutCache = new HashMap<String, SlideLayout>();
	Map<String, BlockLayout> blockLayoutCache = new HashMap<String, BlockLayout>();
	Map<String, Feature> featureCache = new HashMap<String, Feature>();
	Map<Long, String> linkerCache = new HashMap<Long, String>();
	Map<String, String> glycanCache = new HashMap<String, String>();
	Map<LevelUnit, String> concentrationCache = new HashMap<LevelUnit, String>();
	
	final static String hasRatioPredicate = ontPrefix + "has_ratio";
	final static String hasRatioContextPredicate = ontPrefix + "has_feature_ratio";
	
	private String addBlock(Block b, UserEntity user, String graph) throws SparqlException, SQLException {

		String blockURI = generateUniqueURI(uriPrefix + "B", graph);
		ValueFactory f = sparqlDAO.getValueFactory();
		
		IRI graphIRI = f.createIRI(graph);
		IRI block = f.createIRI(blockURI);
		IRI hasBlockLayout = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_block_layout");
		IRI blockType = f.createIRI(ontPrefix + "Block");
		IRI hasRow = f.createIRI(ontPrefix + "has_row");
		IRI hasColumn = f.createIRI(ontPrefix + "has_column");
		Literal row = f.createLiteral(b.getRow());
		Literal column = f.createLiteral(b.getColumn());
		
		BlockLayout layoutFromRepository = null;
        BlockLayout blockLayout = b.getBlockLayout();
        if (blockLayout == null)
            return null;
        if (blockLayout.getId() != null && !blockLayout.getId().isEmpty()) {
            layoutFromRepository = blockLayoutCache.get(blockLayout.getId().trim());
        }
        else if (blockLayout.getName() != null && !blockLayout.getName().isEmpty()) 
            layoutFromRepository = blockLayoutCache.get(blockLayout.getName().trim());
            
        if (layoutFromRepository == null) {  // first time loading
            if (blockLayout.getId() != null && !blockLayout.getId().isEmpty()) {
                layoutFromRepository = getBlockLayoutById(blockLayout.getId().trim(), user);
                blockLayoutCache.put(blockLayout.getId().trim(), layoutFromRepository);
            }
            else if (blockLayout.getName() != null && !blockLayout.getName().isEmpty()) {
                layoutFromRepository = getBlockLayoutByName(blockLayout.getName().trim(), user);
                blockLayoutCache.put(blockLayout.getName().trim(), layoutFromRepository);
            }
        }
        
        if (layoutFromRepository != null) {
            b.setBlockLayout(layoutFromRepository);
            IRI blockLayoutIRI = f.createIRI(layoutFromRepository.getUri());
            // create Block and copy spots from Layout
            List<Statement> statements = new ArrayList<Statement>();
            statements.add(f.createStatement(block, RDF.TYPE, blockType, graphIRI));
            statements.add(f.createStatement(block, hasBlockLayout, blockLayoutIRI, graphIRI));
            statements.add(f.createStatement(block, hasRow, row, graphIRI));
            statements.add(f.createStatement(block, hasColumn, column, graphIRI));
            
            sparqlDAO.addStatements(statements, graphIRI);
        } else 
            throw new SparqlException ("Block layout cannot be found in repository");
        
        return blockURI;
	}
	
	@Override
	public String addBlockLayout(BlockLayout b, UserEntity user) throws SparqlException, SQLException {
		String graph = null;
		try {
			// check if there is already a private graph for user
			graph = getGraphForUser(user);
		} catch (SQLException e) {
			throw new SparqlException ("Cannot add the private graph for the user: " + user.getUsername(), e);
		}
		
		String blockLayoutURI = generateUniqueURI(uriPrefix + "BL", graph);
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI blockLayout = f.createIRI(blockLayoutURI);
		IRI graphIRI = f.createIRI(graph);
		IRI hasSpot = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_spot");
		IRI hasWidth = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_width");
		IRI hasHeight = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_height");
		IRI blockLayoutType = f.createIRI(MetadataTemplateRepository.templatePrefix + "block_layout");
		IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		Date date = new Date();
		Literal dateCreated = f.createLiteral(date);
		
		Literal blockLayoutLabel = f.createLiteral(b.getName().trim());
		Literal blockLayoutComment = b.getDescription() == null ? null: f.createLiteral(b.getDescription().trim());
		Literal blockLayoutWidth = b.getWidth() == null ? null : f.createLiteral(b.getWidth());
		Literal blockLayoutHeight = b.getHeight() == null ? null : f.createLiteral(b.getHeight());
		
		List<Statement> statements = new ArrayList<Statement>();
		statements.add(f.createStatement(blockLayout, RDF.TYPE, blockLayoutType, graphIRI));
		statements.add(f.createStatement(blockLayout, RDFS.LABEL, blockLayoutLabel, graphIRI));
		if (blockLayoutComment != null) statements.add(f.createStatement(blockLayout, RDFS.COMMENT, blockLayoutComment, graphIRI));
		statements.add(f.createStatement(blockLayout, hasCreatedDate, dateCreated, graphIRI));
		statements.add(f.createStatement(blockLayout, hasModifiedDate, dateCreated, graphIRI));
		if (blockLayoutWidth != null) statements.add(f.createStatement(blockLayout, hasWidth, blockLayoutWidth, graphIRI));
		if (blockLayoutHeight != null) statements.add(f.createStatement(blockLayout, hasHeight, blockLayoutHeight, graphIRI));
		
		sparqlDAO.addStatements(statements, graphIRI);
		
		statements = new ArrayList<Statement>();
		for (Spot s : b.getSpots()) {
			if (s == null)
				continue;
			String spotURI = addSpot (s, user, graph);
			IRI spot = f.createIRI(spotURI);
			statements.add(f.createStatement(blockLayout, hasSpot, spot, graphIRI));
		}
		sparqlDAO.addStatements(statements, graphIRI);
		
		return blockLayoutURI;
	}
	
	String addSpot (Spot s, UserEntity user, String graph) throws SparqlException, SQLException {
	    ValueFactory f = sparqlDAO.getValueFactory();
	    IRI graphIRI = f.createIRI(graph);
	    IRI hasFeature = f.createIRI(ontPrefix + "has_feature");
        IRI hasConcentration = f.createIRI(ontPrefix + "has_concentration");
        IRI hasConcentrationValue = f.createIRI(ontPrefix + "concentration_value");
        IRI hasConcentrationUnit = f.createIRI(ontPrefix + "has_concentration_unit");
        IRI hasGroup = f.createIRI(ontPrefix + "has_group");
        IRI hasRow = f.createIRI(ontPrefix + "has_row");
        IRI hasColumn = f.createIRI(ontPrefix + "has_column");
        IRI spotType = f.createIRI(ontPrefix + "Spot");
	    List<Statement> statements = new ArrayList<Statement>();
        String spotURI = generateUniqueURI(uriPrefix + "S", graph);
        IRI spot = f.createIRI(spotURI);
        Literal row = f.createLiteral(s.getRow());
        Literal column = f.createLiteral(s.getColumn());
        Literal group = s.getGroup() == null ? null : f.createLiteral(s.getGroup());
        
        statements.add(f.createStatement(spot, RDF.TYPE, spotType, graphIRI));
        statements.add(f.createStatement(spot, hasRow, row));
        statements.add(f.createStatement(spot, hasColumn, column));
        if (group != null) statements.add(f.createStatement(spot, hasGroup, group, graphIRI));
        if (s.getConcentration() != null) {
            // check if it has already been created before
            String concentrationURI = concentrationCache.get(s.getConcentration());
            if (concentrationURI == null) {
                concentrationURI = generateUniqueURI(uriPrefix + "C", graph);
                concentrationCache.put(s.getConcentration(), concentrationURI);
            }
            IRI concentration = f.createIRI(concentrationURI);
            Literal concentrationUnit = f.createLiteral(s.getConcentration().getLevelUnit().getLabel());
            Literal concentrationValue = s.getConcentration().getConcentration() == null ? null : f.createLiteral(s.getConcentration().getConcentration());
            if (concentrationValue != null) {
                statements.add(f.createStatement(concentration, hasConcentrationValue, concentrationValue, graphIRI));
                statements.add(f.createStatement(concentration, hasConcentrationUnit, concentrationUnit, graphIRI));
            }
            statements.add(f.createStatement(spot, hasConcentration, concentration, graphIRI));
        }
        sparqlDAO.addStatements(statements, graphIRI);
        
        if (s.getFeatures() != null) {
            List<Feature> features = s.getFeatures();
            for (Feature feat : features) {
                statements = new ArrayList<Statement>();
                Feature existing = featureRepository.getFeatureByLabel(feat.getName(), user);
                if (existing != null) {
                    IRI feature = f.createIRI(existing.getUri());
                    statements.add(f.createStatement(spot, hasFeature, feature, graphIRI));
                    Double ratio = s.getFeatureRatioMap().get(feat);
                    if (ratio == null) {
                        ratio = s.getRatio(existing.getId());
                    }
                    if (ratio != null) {
                        // add ratio for the feature
                        Literal ratioL = f.createLiteral(ratio);
                        String positionContextURI = generateUniqueURI(uriPrefix + "PC", graph);
                        IRI hasRatio = f.createIRI (hasRatioPredicate);
                        IRI hasRatioContext = f.createIRI(hasRatioContextPredicate);
                        IRI positionContext = f.createIRI(positionContextURI);
                        statements.add(f.createStatement(spot, hasRatioContext, positionContext, graphIRI));
                        statements.add(f.createStatement(positionContext, hasFeature, feature, graphIRI));
                        statements.add(f.createStatement(positionContext, hasRatio, ratioL, graphIRI));
                    }
                } else {
                    // error
                    throw new SparqlException ("Feature with label " + feat.getName() + " cannot be found!");
                }
                sparqlDAO.addStatements(statements, graphIRI);
            }
        }
        
        
        
        
        return spotURI;
	}
	
	@Override
	public String addSlideLayout(SlideLayout s, UserEntity user) throws SparqlException, SQLException {
		String graph = null;
		try {
			// check if there is already a private graph for user
			graph = getGraphForUser(user);
		} catch (SQLException e) {
			throw new SparqlException ("Cannot add the private graph for the user: " + user.getUsername(), e);
		}
		blockLayoutCache.clear();
		
		String slideLayoutURI = generateUniqueURI(uriPrefix + "SL", graph);
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI slideLayout = f.createIRI(slideLayoutURI);
		IRI graphIRI = f.createIRI(graph);
		IRI slideLayoutType = f.createIRI(MetadataTemplateRepository.templatePrefix + "slide_layout");
		IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
		IRI hasWidth = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_width");
		IRI hasHeight = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_height");
		IRI hasBlock = f.createIRI(ontPrefix + "has_block");
		Date date = new Date();
		Literal dateCreated = f.createLiteral(date);
		IRI createdBy = f.createIRI(ontPrefix + "created_by");
		Literal userLit = f.createLiteral(user.getUsername());
		
		Literal slideLayoutLabel = f.createLiteral(s.getName().trim());
		Literal slideLayoutComment = s.getDescription() == null ? null: f.createLiteral(s.getDescription().trim());
		Literal slideLayoutWidth = s.getWidth() == null ? null : f.createLiteral(s.getWidth());
		Literal slideLayoutHeight = s.getHeight() == null ? null : f.createLiteral(s.getHeight());
		
		List<Statement> statements = new ArrayList<Statement>();
		statements.add(f.createStatement(slideLayout, RDF.TYPE, slideLayoutType, graphIRI));
		statements.add(f.createStatement(slideLayout, RDFS.LABEL, slideLayoutLabel, graphIRI));
		if (slideLayoutComment != null) statements.add(f.createStatement(slideLayout, RDFS.COMMENT, slideLayoutComment, graphIRI));
		statements.add(f.createStatement(slideLayout, hasCreatedDate, dateCreated, graphIRI));
		statements.add(f.createStatement(slideLayout, hasModifiedDate, dateCreated, graphIRI));
		statements.add(f.createStatement(slideLayout, hasAddedToLibrary, dateCreated, graphIRI));
		statements.add(f.createStatement(slideLayout, createdBy, userLit, graphIRI));
		if (slideLayoutWidth != null) statements.add(f.createStatement(slideLayout, hasWidth, slideLayoutWidth, graphIRI));
		if (slideLayoutHeight != null) statements.add(f.createStatement(slideLayout, hasHeight, slideLayoutHeight, graphIRI));
		
		if (s.getBlocks() != null) {
			for (Block b: s.getBlocks()) {
				if (b == null)
					continue;
				String blockURI = addBlock (b, user, graph);
				if (blockURI == null)
					continue;
				b.setUri(blockURI);
				IRI block = f.createIRI(blockURI);
				statements.add(f.createStatement(slideLayout, hasBlock, block, graphIRI));
			}
		}
		
		sparqlDAO.addStatements(statements, graphIRI);
		
		// add it to the slidelayoutrepository as well
		s.setUri(slideLayoutURI);
		s.setId(slideLayoutURI.substring(slideLayoutURI.lastIndexOf("/") + 1));
		s.setDateAddedToLibrary(date);
		s.setDateCreated(date);
		s.setDateModified(date);
		Creator creator = new Creator();
		creator.setName(user.getUsername());
		creator.setUserId(user.getUserId());
		s.setUser(creator);
		SlideLayoutEntity slideLayoutEntity = new SlideLayoutEntity();
		slideLayoutEntity.setUri(slideLayoutURI);
		try {
            slideLayoutEntity.setJsonValue(new ObjectMapper().writeValueAsString(s));
            slideLayoutRepository.save(slideLayoutEntity);
        } catch (JsonProcessingException e) {
            logger.error("Could not serialize Slide layout into JSON for caching", e);
        }
		
		return slideLayoutURI;
	}
	
	@Override
	public void deleteBlockLayout(String blockLayoutId, UserEntity user) throws SparqlException, SQLException {
		String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
		if (graph != null) {
		    if (canDeleteBlockLayout(uriPrefix + blockLayoutId, graph)) {
    			// check to see if the given blockLayoutId is in this graph
    			BlockLayout existing = getBlockLayoutFromURI (uriPrefix + blockLayoutId, user);
    			if (existing != null) {
    				deleteBlockLayoutByURI (uriPrefix + blockLayoutId, graph);
    				return;
    			}
		    } else {
		        throw new IllegalArgumentException("Cannot delete block layout " + blockLayoutId + ". It is used in a slide layout");
		    }
		}
	}

	private void deleteBlockLayoutByURI(String uri, String graph) throws SparqlException {
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI blockLayout = f.createIRI(uri);
		IRI graphIRI = f.createIRI(graph);
		IRI hasSpot = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_spot");
		IRI hasFeature = f.createIRI(ontPrefix + "has_feature");
		IRI hasConcentration = f.createIRI(ontPrefix + "has_concentration");
		
		RepositoryResult<Statement> statements4 = sparqlDAO.getStatements(blockLayout, hasSpot, null, graphIRI);
		while (statements4.hasNext()) {
			Statement st = statements4.next();
			if (st.getPredicate().equals(hasFeature)) {
				Value v = st.getObject();
				String featureURI = v.stringValue();
				IRI feature = f.createIRI(featureURI);
				RepositoryResult<Statement> statements5 = sparqlDAO.getStatements(feature, null, null, graphIRI);
				sparqlDAO.removeStatements(Iterations.asList(statements5), graphIRI);
			} else if (st.getPredicate().equals(hasConcentration)) {
				Value v = st.getObject();
				String conURI = v.stringValue();
				IRI concentration = f.createIRI(conURI);
				RepositoryResult<Statement> statements5 = sparqlDAO.getStatements(concentration, null, null, graphIRI);
				sparqlDAO.removeStatements(Iterations.asList(statements5), graphIRI);
			}
		}
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(blockLayout, null, null, graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
	}
	
	
	boolean canDeleteBlockLayout (String blockURI, String graph) throws SparqlException, SQLException { 
        boolean canDelete = true;
        
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
        queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ("?s gadr:has_block ?block . ?block template:has_block_layout <" +  blockURI + "> . } LIMIT 1");
        
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        if (!results.isEmpty())
            canDelete = false;
        
        return canDelete;
    }
	
	boolean canDeleteSlideLayout (String slideURI, String graph) throws SparqlException, SQLException { 
        boolean canDelete = true;
                
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
        queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ("?s template:has_slide_layout <" +  slideURI + "> . } LIMIT 1");
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        if (!results.isEmpty())
            canDelete = false;
        
        return canDelete;
    }
	
	@Override
	public void deleteSlideLayout(String slideLayoutId, UserEntity user) throws SparqlException, SQLException {
		String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
		if (graph != null) {
		    if (canDeleteSlideLayout(uriPrefix + slideLayoutId, graph)) {
    			// check to see if the given slideLayoutId is in this graph
    			SlideLayout existing = getSlideLayoutFromURI (uriPrefix + slideLayoutId, false, user);
    			if (existing != null) {
    				deleteSlideLayoutByURI (uriPrefix + slideLayoutId, graph);
    				return;
    			}
		    } else {
		        throw new IllegalArgumentException("Cannot delete slide layout " + slideLayoutId + ". It is used in an experiment");
		    }
		}
	}
	
	private void deleteSlideLayoutByURI(String uri, String graph) throws SparqlException {
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI slideLayout = f.createIRI(uri);
		IRI graphIRI = f.createIRI(graph);
		IRI hasBlock = f.createIRI(ontPrefix + "has_block");
		
		RepositoryResult<Statement> statements3 = sparqlDAO.getStatements(slideLayout, hasBlock, null, graphIRI);
		while (statements3.hasNext()) {
			Statement st = statements3.next();
			Value v = st.getObject();
			String blockURI = v.stringValue();
			IRI block = f.createIRI(blockURI);
			RepositoryResult<Statement> statements4 = sparqlDAO.getStatements(block, null, null, graphIRI);
			sparqlDAO.removeStatements(Iterations.asList(statements4), graphIRI);
		}
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(slideLayout, null, null, graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
		
		// delete from SlideLayoutRepository too
		SlideLayoutEntity entity = slideLayoutRepository.findByUri(uri);
		if (entity != null)
		    slideLayoutRepository.delete(entity);
	}
	
	private Block getBlock (String blockURI, boolean loadAll, UserEntity user) throws SparqlException, SQLException {
	    String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
		Block blockObject = new Block();
		ValueFactory f = sparqlDAO.getValueFactory();
		
		IRI block = f.createIRI(blockURI);
		IRI graphIRI = f.createIRI(graph);
		IRI hasBlockLayout = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_block_layout");
		IRI hasRow = f.createIRI(ontPrefix + "has_row");
		IRI hasColumn = f.createIRI(ontPrefix + "has_column");
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(block, null, null, graphIRI);
		if (statements.hasNext()) {
			blockObject = new Block();
			blockObject.setUri(blockURI);
		}
		
		while (statements.hasNext()) {
			Statement st = statements.next();
			if (st.getPredicate().equals(hasBlockLayout)) {
				Value v = st.getObject();
				String blockLayoutURI = v.stringValue();
				BlockLayout blockLayout = null;
				if (blockLayoutCache.containsKey(blockLayoutURI)) {
				    blockLayout = blockLayoutCache.get(blockLayoutURI);
				} else {
				    blockLayout = getBlockLayoutFromURI(blockLayoutURI, loadAll, user);    // need the spots
				    blockLayoutCache.put(blockLayoutURI, blockLayout);
				}
				blockObject.setBlockLayout(blockLayout);
			} else if (st.getPredicate().equals(hasRow)) {
				Value v = st.getObject();
				blockObject.setRow(Integer.parseInt(v.stringValue()));
			} else if (st.getPredicate().equals(hasColumn)) {
				Value v = st.getObject();
				blockObject.setColumn(Integer.parseInt(v.stringValue()));
			} /*else if (st.getPredicate().equals(hasSpot)) {
				Value v = st.getObject();
				String spotURI = v.stringValue();
				// get spot from the BlockLayout
				Spot s = new Spot();
				s.setUri(spotURI);
				IRI spot = f.createIRI(spotURI);
				RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(spot, null, null, graphIRI);
				List<Feature> features = new ArrayList<Feature>();
				s.setFeatures(features);
				while (statements2.hasNext()) {
					Statement st2 = statements2.next();
					if (st2.getPredicate().equals(hasRow)) {
						v = st2.getObject();
						s.setRow(Integer.parseInt(v.stringValue()));
					} else if (st2.getPredicate().equals(hasColumn)) {
						v = st2.getObject();
						s.setColumn(Integer.parseInt(v.stringValue()));
					} else if (st2.getPredicate().equals(hasGroup)) {
						v = st2.getObject();
						s.setGroup(Integer.parseInt(v.stringValue()));
					} else if (st2.getPredicate().equals(hasConcentration)) {
						LevelUnit c = new LevelUnit();
						v = st2.getObject();
						String conURI = v.stringValue();
						IRI concentration = f.createIRI(conURI);
						RepositoryResult<Statement> statements3 = sparqlDAO.getStatements(concentration, null, null, graphIRI);
						while (statements3.hasNext()) {
							Statement st3 = statements3.next();
							if (st3.getPredicate().equals(hasConcentrationValue)) {
								v = st3.getObject();
								c.setConcentration(Double.parseDouble(v.stringValue()));
							} else if (st3.getPredicate().equals(hasConcentrationUnit)) {
								v = st3.getObject();
								c.setLevelUnit(UnitOfLevels.lookUp(v.stringValue()));
							}
						}
						s.setConcentration(c);
					} else if (st2.getPredicate().equals(hasFeature)) {
						v = st2.getObject();
						String featureURI = v.stringValue();
						Feature feat = featureRepository.getFeatureFromURI(featureURI, user);
						if (feat == null) {
							throw new SparqlException("Feature with uri " + featureURI + " cannot be found!");
						}
						features.add(feat);
					}
				}
				spots.add(s);
			}*/
		}
		//blockObject.setSpots (spots);
		
		return blockObject;
	}
	
	@Override
	public BlockLayout getBlockLayoutById(String blockLayoutId, UserEntity user) throws SparqlException, SQLException {
		return getBlockLayoutById(blockLayoutId, user, true);
	}

	@Override
	public BlockLayout getBlockLayoutById(String blockLayoutId, UserEntity user, boolean loadAll)
			throws SparqlException, SQLException {
		// make sure the blocklayout belongs to this user
	    String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?o \n");
		queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n");
		queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/template#block_layout>. \n");
		queryBuf.append ( "<" +  uriPrefix + blockLayoutId + "> rdfs:label ?o . }\n");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.isEmpty())
			return null;
		else {
			return getBlockLayoutFromURI(uriPrefix + blockLayoutId, loadAll, user);
		}
	}
	
	List<SparqlEntity> retrieveBlockLayoutByName (String name, String graph) throws SparqlException {
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?s \n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n");
		queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/template#block_layout>. \n");
		queryBuf.append ( " ?s rdfs:label ?l FILTER (lcase(str(?l)) = \"\"\"" + name.toLowerCase() + "\"\"\") \n"
				+ "}\n");
		return sparqlDAO.query(queryBuf.toString());
	}
	
	@Override
	public BlockLayout getBlockLayoutByName (String name, UserEntity user) throws SparqlException, SQLException {
	    return getBlockLayoutByName(name, user, true);
	}
	
	@Override
    public BlockLayout getBlockLayoutByName (String name, UserEntity user, boolean loadAll) throws SparqlException, SQLException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        List<SparqlEntity> results = retrieveBlockLayoutByName(name, graph);
        if (results.isEmpty())
            return null;
        else {
            String blockLayoutURI = results.get(0).getValue("s");
            return getBlockLayoutFromURI(blockLayoutURI, loadAll, user);
        }
    }
	
	
	@Override
	public List<BlockLayout> getBlockLayoutByUser(UserEntity user) throws SparqlException, SQLException {
		return getBlockLayoutByUser(user, 0, -1, "id", 0);
	}
	
	@Override
	public List<BlockLayout> getBlockLayoutByUser(UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException {
		return getBlockLayoutByUser(user, offset, limit, field, true, order);
	}
	
	@Override
	public List<BlockLayout> getBlockLayoutByUser(UserEntity user, Integer offset, Integer limit, String field,
			Boolean loadAll, Integer order) throws SparqlException, SQLException {
	    return getBlockLayoutByUser(user, offset, limit, field, loadAll, order, null);
	}
	    
	@Override
    public List<BlockLayout> getBlockLayoutByUser(UserEntity user, Integer offset, Integer limit, String field,
            Boolean loadAll, Integer order, String searchValue) throws SparqlException, SQLException {
		
		List<BlockLayout> layouts = new ArrayList<BlockLayout>();
		
		String sortPredicate = getSortPredicateForLayout (field);
		String searchPredicate = "";
		if (searchValue != null)
		    searchPredicate = getSearchPredicate(searchValue);
		// get all blockLayoutURIs from user's private graph
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
			queryBuf.append ("FROM <" + graph + ">\n");
			queryBuf.append ("WHERE {\n");
			queryBuf.append (
					" ?s rdf:type  <http://purl.org/gadr/template#block_layout>. \n" +
							sortLine + searchPredicate +
				    "}\n" +
					 orderByLine + 
					((limit == -1) ? " " : " LIMIT " + limit) +
					" OFFSET " + offset);
			
			List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
			
			for (SparqlEntity sparqlEntity : results) {
				String blockLayoutURI = sparqlEntity.getValue("s");
				BlockLayout layout = getBlockLayoutFromURI(blockLayoutURI, loadAll, user);
				layouts.add(layout);
			}
		}
		
		return layouts;
	}
	
	@Override
	public int getBlockLayoutCountByUser(UserEntity user) throws SQLException, SparqlException {
	    String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
		
		int total = 0;
		if (graph != null) {
			StringBuffer queryBuf = new StringBuffer();
			queryBuf.append (prefix + "\n");
			queryBuf.append ("SELECT COUNT(DISTINCT ?s) as ?count \n");
			queryBuf.append ("FROM <" + graph + ">\n");
			queryBuf.append ("WHERE {\n");
			queryBuf.append (" ?s rdf:type  <http://purl.org/gadr/template#block_layout> . }");
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
	public BlockLayout getBlockLayoutFromURI(String blockLayoutURI, Boolean loadAll, UserEntity user) throws SparqlException, SQLException {
		BlockLayout blockLayoutObject = null;
		String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        
        featureCache.clear();
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI blockLayout = f.createIRI(blockLayoutURI);
		IRI graphIRI = f.createIRI(graph);
		IRI hasSpot = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_spot");
		IRI hasFeature = f.createIRI(ontPrefix + "has_feature");
		IRI hasConcentration = f.createIRI(ontPrefix + "has_concentration");
		IRI hasConcentrationValue = f.createIRI(ontPrefix + "concentration_value");
		IRI hasConcentrationUnit = f.createIRI(ontPrefix + "has_concentration_unit");
		IRI hasGroup = f.createIRI(ontPrefix + "has_group");
		IRI hasRow = f.createIRI(ontPrefix + "has_row");
		IRI hasColumn = f.createIRI(ontPrefix + "has_column");
		IRI hasWidth = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_width");
		IRI hasHeight = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_height");
		IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(blockLayout, null, null, graphIRI);
		if (statements.hasNext()) {
			blockLayoutObject = new BlockLayout();
			blockLayoutObject.setUri(blockLayoutURI);
		}
		if (blockLayoutObject != null) {
			List<Spot> spots = new ArrayList<Spot>();
			while (statements.hasNext()) {
				Statement st = statements.next();
				if (st.getPredicate().equals(RDFS.LABEL)) {
					Value v = st.getObject();
					blockLayoutObject.setName(v.stringValue());
				} else if (st.getPredicate().equals(RDFS.COMMENT)) {
					Value v = st.getObject();
					blockLayoutObject.setDescription(v.stringValue());
				} else if (st.getPredicate().equals(hasWidth)) {
					Value v = st.getObject();
					if (v != null) blockLayoutObject.setWidth(Integer.parseInt(v.stringValue()));
				} else if (st.getPredicate().equals(hasHeight)) {
					Value v = st.getObject();
					if (v != null) blockLayoutObject.setHeight(Integer.parseInt(v.stringValue()));
				} else if (st.getPredicate().equals(hasCreatedDate)) {
					Value value = st.getObject();
				    if (value instanceof Literal) {
				    	Literal literal = (Literal)value;
				    	XMLGregorianCalendar calendar = literal.calendarValue();
				    	Date date = calendar.toGregorianCalendar().getTime();
				    	blockLayoutObject.setDateCreated(date);
				    }
				} else if (st.getPredicate().equals(hasModifiedDate)) {
					Value value = st.getObject();
				    if (value instanceof Literal) {
				    	Literal literal = (Literal)value;
				    	XMLGregorianCalendar calendar = literal.calendarValue();
				    	Date date = calendar.toGregorianCalendar().getTime();
				    	blockLayoutObject.setDateModified(date);
				    }
				} else if ((loadAll == null || loadAll) && st.getPredicate().equals(hasSpot)) {
					Value v = st.getObject();
					String spotURI = v.stringValue();
					Spot s = new Spot();
					s.setUri(spotURI);
					IRI spot = f.createIRI(spotURI);
					RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(spot, null, null, graphIRI);
					List<Feature> features = new ArrayList<Feature>();
					s.setFeatures(features);
					while (statements2.hasNext()) {
						Statement st2 = statements2.next();
						if (st2.getPredicate().equals(hasRow)) {
							v = st2.getObject();
							s.setRow(Integer.parseInt(v.stringValue()));
						} else if (st2.getPredicate().equals(hasColumn)) {
							v = st2.getObject();
							s.setColumn(Integer.parseInt(v.stringValue()));
						} else if (st2.getPredicate().equals(hasGroup)) {
							v = st2.getObject();
							s.setGroup(Integer.parseInt(v.stringValue()));
						} else if (st2.getPredicate().equals(hasConcentration)) {
							LevelUnit c = new LevelUnit();
							v = st2.getObject();
							String conURI = v.stringValue();
							IRI concentration = f.createIRI(conURI);
							RepositoryResult<Statement> statements3 = sparqlDAO.getStatements(concentration, null, null, graphIRI);
							while (statements3.hasNext()) {
								Statement st3 = statements3.next();
								if (st3.getPredicate().equals(hasConcentrationValue)) {
									v = st3.getObject();
									c.setConcentration(Double.parseDouble(v.stringValue()));
								} else if (st3.getPredicate().equals(hasConcentrationUnit)) {
									v = st3.getObject();
									c.setLevelUnit(UnitOfLevels.lookUp(v.stringValue()));
								}
							}
							s.setConcentration(c);
						} else if (st2.getPredicate().equals(hasFeature)) {
							v = st2.getObject();
							String featureURI = v.stringValue();
							Feature feat = null;
							if (featureCache.containsKey(featureURI)) {
							    feat = featureCache.get(featureURI);
							} else {
							    feat = featureRepository.getFeatureFromURI(featureURI, user);
							    featureCache.put(featureURI, feat);
							}
							if (feat == null) {
								throw new SparqlException("Feature with given uri " + featureURI + " cannot be found!");
							}
							features.add(feat);
						}
					}
					if (!features.isEmpty())
					    spots.add(s);
				}
			}
			
			blockLayoutObject.setSpots(spots);
		}
		
		return blockLayoutObject;
	}

	private BlockLayout getBlockLayoutFromURI(String blockLayoutURI, UserEntity user) throws SparqlException, SQLException {
		return getBlockLayoutFromURI(blockLayoutURI, true, user);
	}
	
	@Override
	public SlideLayout getSlideLayoutById(String slideLayoutId, UserEntity user) throws SparqlException, SQLException {
		return getSlideLayoutById(slideLayoutId, user, true);
	}

	@Override
	public SlideLayout getSlideLayoutById(String slideLayoutId, UserEntity user, boolean loadAll)
			throws SparqlException, SQLException {
		// make sure the slidelayout belongs to this user
	    String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?o \n");
		queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n");
		queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/template#slide_layout>. \n");
		queryBuf.append ( "<" +  uriPrefix + slideLayoutId + "> rdfs:label ?o . }\n");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.isEmpty())
			return null;
		else {
			return getSlideLayoutFromURI(uriPrefix + slideLayoutId, loadAll, user);
		}
	}
	
	List<SparqlEntity> retrieveSlideLayoutByName (String name, String graph) throws SparqlException {
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?s \n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n");
		queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/template#slide_layout>. \n");
		queryBuf.append ( " ?s rdfs:label ?l FILTER (lcase(str(?l)) = \"\"\"" + name.toLowerCase() + "\"\"\") \n"
				+ "}\n");
		return sparqlDAO.query(queryBuf.toString());
	}

	@Override
	public SlideLayout getSlideLayoutByName(String name, UserEntity user) throws SparqlException, SQLException {
	    String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        List<SparqlEntity> results = retrieveSlideLayoutByName(name, graph);
		if (results.isEmpty())
			return null;
		else {
			String slideLayoutURI = results.get(0).getValue("s");
			return getSlideLayoutFromURI(slideLayoutURI, false, user);
		}
	}
	

	@Override
	public List<SlideLayout> getSlideLayoutByUser(UserEntity user) throws SparqlException, SQLException {
		return getSlideLayoutByUser(user, 0, -1, "id", 0);
	}
	
	@Override
	public List<SlideLayout> getSlideLayoutByUser(UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException {
		return getSlideLayoutByUser(user, offset, limit, field, true, order);
	}

	@Override
	public List<SlideLayout> getSlideLayoutByUser(UserEntity user, Integer offset, Integer limit, String field,
			Boolean loadAll, Integer order) throws SparqlException, SQLException {
	    return getSlideLayoutByUser(user, offset, limit, field, loadAll, order, null);
	}
	
    @Override
    public List<SlideLayout> getSlideLayoutByUser(UserEntity user, Integer offset, Integer limit, String field,
            Boolean loadAll, Integer order, String searchValue) throws SparqlException, SQLException {
	 
		List<SlideLayout> layouts = new ArrayList<SlideLayout>();
		
		String sortPredicate = getSortPredicateForLayout (field);
		String searchPredicate = "";
        if (searchValue != null)
            searchPredicate = getSearchPredicate(searchValue);
		// get all blockLayoutURIs from user's private graph
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
			queryBuf.append ("FROM <" + graph + ">\n");
			queryBuf.append ("WHERE {\n");
			queryBuf.append (
					" ?s rdf:type  <http://purl.org/gadr/template#slide_layout>. \n" +
							sortLine + searchPredicate + 
				    "}\n" +
					 orderByLine + 
					((limit == -1) ? " " : " LIMIT " + limit) +
					" OFFSET " + offset);
			
			List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
			
			for (SparqlEntity sparqlEntity : results) {
				String slideLayoutURI = sparqlEntity.getValue("s");
				SlideLayout layout = getSlideLayoutFromURI(slideLayoutURI, loadAll, user);
				layouts.add(layout);
			}
		}
		
		return layouts;
	}

	@Override
	public int getSlideLayoutCountByUser(UserEntity user) throws SQLException, SparqlException {
	    String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
		
		int total = 0;
		if (graph != null) {
			StringBuffer queryBuf = new StringBuffer();
			queryBuf.append (prefix + "\n");
			queryBuf.append ("SELECT COUNT(DISTINCT ?s) as ?count \n");
			queryBuf.append ("FROM <" + graph + ">\n");
			queryBuf.append ("WHERE {\n");
			queryBuf.append (" ?s rdf:type  <http://purl.org/gadr/template#slide_layout> . }");
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

	private SlideLayout getSlideLayoutFromURI(String slideLayoutURI, Boolean loadAll, UserEntity user) throws SparqlException, SQLException {
		SlideLayout slideLayoutObject = null;
		String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        
        // check the slideLayoutRepository first, if loadAll = true
        if (loadAll) {
            SlideLayoutEntity entity = slideLayoutRepository.findByUri(slideLayoutURI);
            if (entity != null) {
                try {
                    SlideLayout s = new ObjectMapper().readValue(entity.getJsonValue(), SlideLayout.class);
                    return s;
                } catch (IOException e) {
                    logger.error("Could not read slide layout from serialized value", e);
                }
            }
        }
        
        blockLayoutCache.clear();
        
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI slideLayout = f.createIRI(slideLayoutURI);
		IRI graphIRI = f.createIRI(graph);
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(slideLayout, null, null, graphIRI);
		if (statements.hasNext()) {
			slideLayoutObject = new SlideLayout();
			slideLayoutObject.setUri(slideLayoutURI);
			if (user != null) {
    			Creator owner = new Creator ();
                owner.setUserId(user.getUserId());
                owner.setName(user.getUsername());
                slideLayoutObject.setUser(owner);
			} else {
				slideLayoutObject.setIsPublic(true);
			}
		}
		if (slideLayoutObject != null) {
		    List<Block> blocks = new ArrayList<>();
		    slideLayoutObject.setBlocks(blocks); // extractFromStatements will add to this list as necessary
			extractFromStatements (statements, slideLayoutObject, loadAll, user);
		}
		return slideLayoutObject;
	}
	
	void extractFromStatements (RepositoryResult<Statement> statements, SlideLayout slideLayoutObject, Boolean loadAll, UserEntity user) throws SparqlException, SQLException {
	    ValueFactory f = sparqlDAO.getValueFactory();
        
        IRI hasBlock = f.createIRI(ontPrefix + "has_block");
        IRI hasWidth = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_width");
        IRI hasHeight = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_height");
        IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
        IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
        IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
        IRI createdBy= f.createIRI(ontPrefix + "created_by");
        IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
        IRI hasPublicURI = f.createIRI(ontPrefix + "has_public_uri");
        
        while (statements.hasNext()) {
            Statement st = statements.next();
            if (st.getPredicate().equals(RDFS.LABEL)) {
                Value v = st.getObject();
                slideLayoutObject.setName(v.stringValue());
            } else if (st.getPredicate().equals(RDFS.COMMENT)) {
                Value v = st.getObject();
                slideLayoutObject.setDescription(v.stringValue());
            } else if (st.getPredicate().equals(createdBy)) {
                Value label = st.getObject();
                Creator creator = new Creator();
                creator.setName(label.stringValue());
                slideLayoutObject.setUser(creator);
            } else if (st.getPredicate().equals(hasWidth)) {
                Value v = st.getObject();
                if (v != null) slideLayoutObject.setWidth(Integer.parseInt(v.stringValue()));
            } else if (st.getPredicate().equals(hasHeight)) {
                Value v = st.getObject();
                if (v != null) slideLayoutObject.setHeight(Integer.parseInt(v.stringValue()));
            } else if (st.getPredicate().equals(hasCreatedDate)) {
                Value value = st.getObject();
                if (value instanceof Literal) {
                    Literal literal = (Literal)value;
                    XMLGregorianCalendar calendar = literal.calendarValue();
                    Date date = calendar.toGregorianCalendar().getTime();
                    slideLayoutObject.setDateCreated(date);
                }
            } else if (st.getPredicate().equals(hasModifiedDate)) {
                Value value = st.getObject();
                if (value instanceof Literal) {
                    Literal literal = (Literal)value;
                    XMLGregorianCalendar calendar = literal.calendarValue();
                    Date date = calendar.toGregorianCalendar().getTime();
                    slideLayoutObject.setDateModified(date);
                }
            } else if (st.getPredicate().equals(hasAddedToLibrary)) {
                Value value = st.getObject();
                if (value instanceof Literal) {
                    Literal literal = (Literal)value;
                    XMLGregorianCalendar calendar = literal.calendarValue();
                    Date date = calendar.toGregorianCalendar().getTime();
                    slideLayoutObject.setDateAddedToLibrary(date);
                }
            } else if (/*(loadAll == null || loadAll) &&*/ st.getPredicate().equals(hasBlock)) {
                Value v = st.getObject();
                String blockURI = v.stringValue();
                Block block = getBlock (blockURI, loadAll, user);
                slideLayoutObject.getBlocks().add(block);
            } else if (st.getPredicate().equals(hasPublicURI)) {
                // need to retrieve additional information from DEFAULT graph
                slideLayoutObject.setIsPublic(true);
                Value uriValue = st.getObject();
                String publicLayoutURI = uriValue.stringValue();
                IRI publicLayout = f.createIRI(publicLayoutURI);
                RepositoryResult<Statement> statementsPublic = sparqlDAO.getStatements(publicLayout, null, null, defaultGraphIRI);
                extractFromStatements (statementsPublic, slideLayoutObject, loadAll, null);
            }
        }
	}
	
	@Override
	public void updateSlideLayout(SlideLayout layout, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		SlideLayout existing = getSlideLayoutFromURI(layout.getUri(), false, user);
		if (graph != null && existing !=null) {
			updateSlideLayoutInGraph(layout, graph);
		}
	}

	void updateSlideLayoutInGraph (SlideLayout layout, String graph) throws SparqlException {	
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI graphIRI = f.createIRI(graph);
		String layoutURI = layout.getUri();
		IRI slideLayout = f.createIRI(layoutURI);
		Literal label = f.createLiteral(layout.getName());
		Literal comment = layout.getDescription() == null ? f.createLiteral("") : f.createLiteral(layout.getDescription());
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		Date today = new Date();
		Literal date = f.createLiteral(today);
		
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(slideLayout, RDFS.LABEL, null, graphIRI)), graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(slideLayout, RDFS.COMMENT, null, graphIRI)), graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(slideLayout, hasModifiedDate, null, graphIRI)), graphIRI);
		
		List<Statement> statements = new ArrayList<Statement>();
		
		statements.add(f.createStatement(slideLayout, RDFS.LABEL, label, graphIRI));
		statements.add(f.createStatement(slideLayout, RDFS.COMMENT, comment, graphIRI));
		statements.add(f.createStatement(slideLayout, hasModifiedDate, date, graphIRI));
		
		sparqlDAO.addStatements(statements, graphIRI);
		
		// update in SlideLayoutRepository as well
		SlideLayoutEntity entity = slideLayoutRepository.findByUri(layout.getUri());
		if (entity != null) {
    		layout.setDateModified(today);
    		try {
                entity.setJsonValue(new ObjectMapper().writeValueAsString(layout));
                slideLayoutCache.remove(entity.getUri());
                slideLayoutRepository.save(entity);
            } catch (JsonProcessingException e) {
                logger.error("Could not update slide layout serialization", e);
            }
		}
	}
	
	private String getSortPredicateForLayout (String field) {
		if (field == null || field.equalsIgnoreCase("name")) 
			return "rdfs:label";
		else if (field.equalsIgnoreCase("description")) 
			return "rdfs:comment";
		else if (field.equalsIgnoreCase("dateModified"))
			return "gadr:has_date_modified";
		else if (field.equalsIgnoreCase("id"))
			return null;	
		return null;
	}
	
    public String getSearchPredicate (String searchValue) {
        String predicates = "";
        
        predicates += "?s rdfs:label ?value1 .\n";
        predicates += "OPTIONAL {?s rdfs:comment ?value2} \n";
       
        
        String filterClause = "filter (";
        for (int i=1; i < 3; i++) {
            filterClause += "regex (str(?value" + i + "), '" + searchValue + "', 'i')";
            if (i + 1 < 3)
                filterClause += " || ";
        }
        filterClause += ")\n";
            
        predicates += filterClause;
        return predicates;
    }
	
	@Override
	public void updateBlockLayout(BlockLayout layout, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		BlockLayout existing = getBlockLayoutFromURI(layout.getUri(), false, user);
		if (graph != null && existing !=null) {
			updateBlockLayoutInGraph(layout, graph);
		}
	}

	void updateBlockLayoutInGraph (BlockLayout layout, String graph) throws SparqlException {	
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI graphIRI = f.createIRI(graph);
		String layoutURI = layout.getUri();
		IRI blockLayout = f.createIRI(layoutURI);
		Literal label = f.createLiteral(layout.getName());
		Literal comment = layout.getDescription() == null ? f.createLiteral("") : f.createLiteral(layout.getDescription());
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		Literal date = f.createLiteral(new Date());
		
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(blockLayout, RDFS.LABEL, null, graphIRI)), graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(blockLayout, RDFS.COMMENT, null, graphIRI)), graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(blockLayout, hasModifiedDate, null, graphIRI)), graphIRI);
		
		List<Statement> statements = new ArrayList<Statement>();
		
		statements.add(f.createStatement(blockLayout, RDFS.LABEL, label, graphIRI));
		statements.add(f.createStatement(blockLayout, RDFS.COMMENT, comment, graphIRI));
		statements.add(f.createStatement(blockLayout, hasModifiedDate, date, graphIRI));
		
		sparqlDAO.addStatements(statements, graphIRI);
	}

	@Override
	public String makePublic(SlideLayout layout, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
        String existingURI = null;
        
        if (layout.getName() != null && !layout.getName().isEmpty()) {
        	SlideLayout existing = getSlideLayoutByName(layout.getName(), null);
        	if (existing != null)
        		existingURI = existing.getUri();
        }
        
        Map<String, Glycan > processedGlycans = new HashMap<>();
        Map<String, Linker > processedLinkers = new HashMap<>();
        Map<String, Feature > processedFeatures = new HashMap<>();
        blockLayoutCache.clear();
        
        if (existingURI == null) {
        	// first make other components public
            List<Block> publicBlocks = new ArrayList<Block>();
        	Map<String, String> uriMapOldToNew = new HashMap<>();
        	for (Block block: layout.getBlocks()) {
        		BlockLayout blockLayout = block.getBlockLayout();
        		if (uriMapOldToNew.get(blockLayout.getUri()) == null) {
            		// check if it already exists
            		List <SparqlEntity> results2 = retrieveBlockLayoutByName (blockLayout.getName(), null);
            		String publicURI = null;
            		if (results2.isEmpty()) {
            		    if (blockLayout.getSpots() == null || blockLayout.getSpots().isEmpty()) {
            	            String uri = uriPrefix + blockLayout.getId();
            	            // load them
            	            blockLayout = blockLayoutCache.get(uri);
            	            if (blockLayout == null || blockLayout.getSpots() == null || blockLayout.getSpots().isEmpty()) {
            	                blockLayout = getBlockLayoutFromURI(uri, user);
            	            }
            	        }
            			deleteByURI (uriPrefix + blockLayout.getId(), graph);
            			publicURI = addPublicBlockLayout (blockLayout, null, user, processedGlycans, processedLinkers, processedFeatures);
            			uriMapOldToNew.put(blockLayout.getUri(), publicURI);
            		} else {
            			String blockLayoutURI = results2.get(0).getValue("s");
            			deleteByURI (uriPrefix + blockLayout.getId(), graph);
            			publicURI = addPublicBlockLayout (blockLayout, blockLayoutURI, user, processedGlycans, processedLinkers, processedFeatures);
            			uriMapOldToNew.put(blockLayout.getUri(), publicURI);
            		}
        		}
        		deleteByURI (uriPrefix + block.getId(), graph);
        		String uri = uriMapOldToNew.get(blockLayout.getUri());
        		BlockLayout blockL = null;
        		if (blockLayoutCache.containsKey(uri))
        		    blockL = blockLayoutCache.get(uri);
        		else
        		    blockL = getBlockLayoutFromURI(uri, null);
        		block.setBlockLayout(blockL);
        		String blockURI = addPublicBlock (block, graph);
        		Block newBlock = getBlock(blockURI, true, null);
        		publicBlocks.add(newBlock);
        	}
        	
        	layout.setBlocks(publicBlocks);
            // make it public
            deleteByURI(uriPrefix + layout.getId(), graph);
            updateSlideLayoutInGraph(layout, graph);
            // need to create the slidelayout in the public graph, link the user's version to public one
            return addPublicSlideLayout(layout, null, graph, user.getUsername()); 
        } else {
            deleteByURI(uriPrefix + layout.getId(), graph);
            updateSlideLayoutInGraph(layout, graph);
            // need to link the user's version to the existing URI
            return addPublicSlideLayout(layout, existingURI, graph, user.getUsername());
        }
	}

	private String addPublicBlock(Block block, String graph) throws SparqlException {
		String blockURI = generateUniqueURI(uriPrefix + "B", graph);
		ValueFactory f = sparqlDAO.getValueFactory();
		
		IRI graphIRI = f.createIRI(DEFAULT_GRAPH);
		IRI blockIRI = f.createIRI(blockURI);
		IRI hasBlockLayout = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_block_layout");
		IRI blockType = f.createIRI(ontPrefix + "Block");
		IRI hasRow = f.createIRI(ontPrefix + "has_row");
		IRI hasColumn = f.createIRI(ontPrefix + "has_column");
		//IRI hasSpot = f.createIRI(ontPrefix + "has_spot");
		Literal row = f.createLiteral(block.getRow());
		Literal column = f.createLiteral(block.getColumn());
		
		BlockLayout blockLayout = block.getBlockLayout();
		IRI blockLayoutIRI = f.createIRI(blockLayout.getUri());
		// create Block and copy spots from Layout
		List<Statement> statements = new ArrayList<Statement>();
		statements.add(f.createStatement(blockIRI, RDF.TYPE, blockType, graphIRI));
		statements.add(f.createStatement(blockIRI, hasBlockLayout, blockLayoutIRI, graphIRI));
		statements.add(f.createStatement(blockIRI, hasRow, row, graphIRI));
		statements.add(f.createStatement(blockIRI, hasColumn, column, graphIRI));
		
		// copy spots from layout
		/*for (Spot s : blockLayout.getSpots()) {
			IRI spot = f.createIRI(s.getUri());
			statements.add(f.createStatement(blockIRI, hasSpot, spot, graphIRI));
		} */
		sparqlDAO.addStatements(statements, graphIRI);
		return blockURI;
	}

	private String addPublicBlockLayout(BlockLayout blockLayout, String blockLayoutURI, UserEntity user,
	        Map<String, Glycan > processedGlycans,
            Map<String, Linker > processedLinkers,
            Map<String, Feature > processedFeatures) throws SparqlException, SQLException {
		List<Spot> publicSpots = new ArrayList<>();
		for (Spot spot: blockLayout.getSpots()) {
			List<Feature> publicFeatures = new ArrayList<>();
			for (Feature feature: spot.getFeatures()) {
			    String featureURI = null;
			    String previous = feature.getUri();
			    if (!processedFeatures.containsKey(previous)) {
    				List<Glycan> publicGlycans = new ArrayList<>();
    				for (Glycan g: feature.getGlycans()) {
    				    String previousG = g.getUri();
    				    if (!processedGlycans.containsKey(previousG)) {
    						String glycanURI = glycanRepository.makePublic(g, user);
    						if (glycanURI != null) {
        						Glycan newGlycan = glycanRepository.getGlycanFromURI(glycanURI, null);
        						publicGlycans.add(newGlycan); // get the public one
        						processedGlycans.put(previousG, newGlycan);
    						} else {
        						Glycan existing = glycanRepository.getGlycanByLabel(g.getName(), null);
        						if (existing != null) {
        						    publicGlycans.add(existing);
        						    processedGlycans.put(previousG, existing);
        						}
        					} 
    				    }
    				    else {
    				        publicGlycans.add(processedGlycans.get(previousG)); // get the public one
    				    }
    				}
    				feature.setGlycans(publicGlycans);
    				Linker l = feature.getLinker();
    				if (l != null) {
    				    String previousL = l.getUri();
                        if (!processedLinkers.containsKey(previousL)) {
        					String linkerURI = linkerRepository.makePublic(l, user);
        					if (linkerURI != null) {
            					Linker newLinker = linkerRepository.getLinkerFromURI(linkerURI, null);
            					feature.setLinker(newLinker); // get the public one
            					processedLinkers.put(previousL, newLinker);
        					} else {
            					// retrieve the existing one
            					Linker existing = linkerRepository.getLinkerByLabel(l.getName(), null);
            					if (existing != null) {
            					    feature.setLinker(existing);
            					    processedLinkers.put(previousL, existing);
            					}
        					}
                        } else {
                            feature.setLinker(processedLinkers.get(previousL));
                        }
    				}
    				// make feature public
    				featureURI = featureRepository.addPublicFeature (feature);
    				Feature newFeature = featureRepository.getFeatureFromURI(featureURI, null);
    				publicFeatures.add(newFeature);
    				processedFeatures.put(previous, newFeature);
			    } else {
	                publicFeatures.add(processedFeatures.get(previous));
	            }
			} 
			spot.setFeatures(publicFeatures);
			// make spot public
			String spotURI = addPublicSpot (spot);
			publicSpots.add(getSpotFromURI (spotURI, null));
		}
		blockLayout.setSpots(publicSpots);
		// make block layout public
		ValueFactory f = sparqlDAO.getValueFactory();
		
		String userGraph = getGraphForUser(user);
		
		if (blockLayoutURI == null) {
			blockLayoutURI = generateUniqueURI(uriPrefix + "BL", userGraph);
			IRI publicGraphIRI = f.createIRI(DEFAULT_GRAPH);
			IRI blockLayoutIRI = f.createIRI(blockLayoutURI);
			IRI hasSpot = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_spot");
			IRI hasWidth = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_width");
			IRI hasHeight = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_height");
			IRI blockLayoutType = f.createIRI(MetadataTemplateRepository.templatePrefix + "block_layout");
			IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
			IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
			Date date = new Date();
			Literal dateCreated = f.createLiteral(date);
		
			Literal blockLayoutLabel = f.createLiteral(blockLayout.getName().trim());
			Literal blockLayoutComment = blockLayout.getDescription() == null ? null: f.createLiteral(blockLayout.getDescription().trim());
			Literal blockLayoutWidth = blockLayout.getWidth() == null ? null : f.createLiteral(blockLayout.getWidth());
			Literal blockLayoutHeight = blockLayout.getHeight() == null ? null : f.createLiteral(blockLayout.getHeight());
		
			List<Statement> statements = new ArrayList<Statement>();
			statements.add(f.createStatement(blockLayoutIRI, RDF.TYPE, blockLayoutType, publicGraphIRI));
			statements.add(f.createStatement(blockLayoutIRI, RDFS.LABEL, blockLayoutLabel, publicGraphIRI));
			if (blockLayoutComment != null) statements.add(f.createStatement(blockLayoutIRI, RDFS.COMMENT, blockLayoutComment, publicGraphIRI));
			statements.add(f.createStatement(blockLayoutIRI, hasCreatedDate, dateCreated, publicGraphIRI));
			statements.add(f.createStatement(blockLayoutIRI, hasModifiedDate, dateCreated, publicGraphIRI));
			if (blockLayoutWidth != null) statements.add(f.createStatement(blockLayoutIRI, hasWidth, blockLayoutWidth, publicGraphIRI));
			if (blockLayoutHeight != null) statements.add(f.createStatement(blockLayoutIRI, hasHeight, blockLayoutHeight, publicGraphIRI));
			
			for (Spot s : blockLayout.getSpots()) {
				if (s == null)
					continue;
				
				IRI spot = f.createIRI(s.getUri());
				statements.add(f.createStatement(blockLayoutIRI, hasSpot, spot, publicGraphIRI));
			}
			sparqlDAO.addStatements(statements, publicGraphIRI);
		}
		
		return blockLayoutURI;
	}

	@Override
	public Spot getSpotFromURI(String spotURI, UserEntity user) throws SQLException, SparqlException {
		String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
		Spot s = new Spot();
		s.setUri(spotURI);
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI graphIRI = f.createIRI(graph);
		IRI spot = f.createIRI(spotURI);
		IRI hasRow = f.createIRI(ontPrefix + "has_row");
		IRI hasColumn = f.createIRI(ontPrefix + "has_column");
		IRI hasFeature = f.createIRI(ontPrefix + "has_feature");
		IRI hasConcentration = f.createIRI(ontPrefix + "has_concentration");
		IRI hasConcentrationValue = f.createIRI(ontPrefix + "concentration_value");
		IRI hasConcentrationUnit = f.createIRI(ontPrefix + "has_concentration_unit");
		IRI hasGroup = f.createIRI(ontPrefix + "has_group");
		IRI hasRatio = f.createIRI (hasRatioPredicate);
        IRI hasRatioContext = f.createIRI(hasRatioContextPredicate);
		RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(spot, null, null, graphIRI);
		List<Feature> features = new ArrayList<Feature>();
		s.setFeatures(features);
		while (statements2.hasNext()) {
			Statement st2 = statements2.next();
			if (st2.getPredicate().equals(hasRow)) {
				Value v = st2.getObject();
				s.setRow(Integer.parseInt(v.stringValue()));
			} else if (st2.getPredicate().equals(hasColumn)) {
				Value v = st2.getObject();
				s.setColumn(Integer.parseInt(v.stringValue()));
			} else if (st2.getPredicate().equals(hasGroup)) {
				Value v = st2.getObject();
				s.setGroup(Integer.parseInt(v.stringValue()));
			} else if (st2.getPredicate().equals(hasConcentration)) {
				LevelUnit c = new LevelUnit();
				Value v = st2.getObject();
				String conURI = v.stringValue();
				IRI concentration = f.createIRI(conURI);
				RepositoryResult<Statement> statements3 = sparqlDAO.getStatements(concentration, null, null, graphIRI);
				while (statements3.hasNext()) {
					Statement st3 = statements3.next();
					if (st3.getPredicate().equals(hasConcentrationValue)) {
						v = st3.getObject();
						c.setConcentration(Double.parseDouble(v.stringValue()));
					} else if (st3.getPredicate().equals(hasConcentrationUnit)) {
						v = st3.getObject();
						c.setLevelUnit(UnitOfLevels.lookUp(v.stringValue()));
					}
				}
				s.setConcentration(c);
			} else if (st2.getPredicate().equals(hasFeature)) {
				Value v = st2.getObject();
				String featureURI = v.stringValue();
				Feature feat = featureRepository.getFeatureFromURI(featureURI, user);
				if (feat == null) {
					throw new SparqlException("Feature with uri " + featureURI + " cannot be found!");
				}
				features.add(feat);
			} else if (st2.getPredicate().equals(hasRatioContext)) {
                Value positionContext = st2.getObject();
                String contextURI = positionContext.stringValue();
                IRI ctx = f.createIRI(contextURI);
                RepositoryResult<Statement> statements3 = sparqlDAO.getStatements(ctx, null, null, graphIRI);
                Double ratio = null;
                Feature featureInContext = null;
                while (statements3.hasNext()) {
                    Statement st3 = statements2.next();
                    if (st3.getPredicate().equals(hasRatio)) {
                        Value value = st2.getObject();
                        if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
                            ratio = Double.parseDouble(value.stringValue());
                        }   
                    } else if (st3.getPredicate().equals(hasFeature)) {
                        Value val = st2.getObject();
                        featureInContext = featureRepository.getFeatureFromURI(val.stringValue(), user);
                    }  
                }
                if (ratio != null && featureInContext != null) {
                    s.setRatio(featureInContext.getUri().substring(featureInContext.getUri().lastIndexOf("/")+1), ratio);
                }
            } 
		}
		return s;
	}

	private String addPublicSpot(Spot s) throws SparqlException {
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI graphIRI = f.createIRI(DEFAULT_GRAPH);
		String spotURI = generateUniqueURI(uriPrefix + "S");
		IRI spot = f.createIRI(spotURI);
		Literal row = f.createLiteral(s.getRow());
		Literal column = f.createLiteral(s.getColumn());
		Literal group = s.getGroup() == null ? null : f.createLiteral(s.getGroup());
		IRI hasConcentration = f.createIRI(ontPrefix + "has_concentration");
		IRI hasConcentrationValue = f.createIRI(ontPrefix + "concentration_value");
		IRI hasConcentrationUnit = f.createIRI(ontPrefix + "has_concentration_unit");
		IRI hasGroup = f.createIRI(ontPrefix + "has_group");
		IRI hasRow = f.createIRI(ontPrefix + "has_row");
		IRI hasColumn = f.createIRI(ontPrefix + "has_column");
		IRI spotType = f.createIRI(ontPrefix + "Spot");
		IRI hasFeature = f.createIRI(ontPrefix + "has_feature");
		
		List<Statement> statements = new ArrayList<Statement>();
		statements.add(f.createStatement(spot, RDF.TYPE, spotType, graphIRI));
		
		statements.add(f.createStatement(spot, hasRow, row));
		statements.add(f.createStatement(spot, hasColumn, column));
		if (group != null) statements.add(f.createStatement(spot, hasGroup, group, graphIRI));
		if (s.getConcentration() != null) {
			// check if it has already been created before
			String concentrationURI = concentrationCache.get(s.getConcentration());
			if (concentrationURI == null) {
				concentrationURI = generateUniqueURI(uriPrefix + "C");
				concentrationCache.put(s.getConcentration(), concentrationURI);
			}
			IRI concentration = f.createIRI(concentrationURI);
			Literal concentrationUnit = f.createLiteral(s.getConcentration().getLevelUnit().getLabel());
			Literal concentrationValue = s.getConcentration().getConcentration() == null ? null : f.createLiteral(s.getConcentration().getConcentration());
			if (concentrationValue != null) {
				statements.add(f.createStatement(concentration, hasConcentrationValue, concentrationValue, graphIRI));
				statements.add(f.createStatement(concentration, hasConcentrationUnit, concentrationUnit, graphIRI));
			}
			statements.add(f.createStatement(spot, hasConcentration, concentration, graphIRI));
		}
		
		if (s.getFeatures() != null) {
			for (Feature feat : s.getFeatures()) {
				IRI feature = f.createIRI(feat.getUri());
				statements.add(f.createStatement(spot, hasFeature, feature, graphIRI));		
			} 
		}
		sparqlDAO.addStatements(statements, graphIRI);
		return spotURI;
	}

	private String addPublicSlideLayout(SlideLayout s, String publicURI, String graph, String username) throws SparqlException {
		boolean existing = publicURI != null;
		if (publicURI == null)
			publicURI = generateUniqueURI(uriPrefix + "SL", graph);
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI slideLayout = f.createIRI(publicURI);
		IRI graphIRI = f.createIRI(DEFAULT_GRAPH);
		IRI userGraphIRI = f.createIRI(graph);
		IRI slideLayoutType = f.createIRI(MetadataTemplateRepository.templatePrefix + "slide_layout");
		IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		IRI hasWidth = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_width");
		IRI hasHeight = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_height");
		IRI hasBlock = f.createIRI(ontPrefix + "has_block");
		IRI hasPublicURI = f.createIRI(ontPrefix + "has_public_uri");
		IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
		IRI createdBy = f.createIRI(ontPrefix + "created_by");
		Literal user = f.createLiteral(username);
		Date date = new Date();
		Literal dateCreated = f.createLiteral(date);
		Literal dateAdded = s.getDateAddedToLibrary() == null ? f.createLiteral(date) : f.createLiteral(s.getDateAddedToLibrary());
		IRI local = f.createIRI(s.getUri());
		if (!existing) {
			Literal slideLayoutLabel = f.createLiteral(s.getName().trim());
			Literal slideLayoutComment = s.getDescription() == null ? null: f.createLiteral(s.getDescription().trim());
			Literal slideLayoutWidth = s.getWidth() == null ? null : f.createLiteral(s.getWidth());
			Literal slideLayoutHeight = s.getHeight() == null ? null : f.createLiteral(s.getHeight());
			
			List<Statement> statements = new ArrayList<Statement>();
			statements.add(f.createStatement(slideLayout, RDF.TYPE, slideLayoutType, graphIRI));
			statements.add(f.createStatement(slideLayout, RDFS.LABEL, slideLayoutLabel, graphIRI));
			if (slideLayoutComment != null) statements.add(f.createStatement(slideLayout, RDFS.COMMENT, slideLayoutComment, graphIRI));
			statements.add(f.createStatement(slideLayout, hasCreatedDate, dateCreated, graphIRI));
			statements.add(f.createStatement(slideLayout, hasModifiedDate, dateCreated, graphIRI));
			statements.add(f.createStatement(slideLayout, hasAddedToLibrary, dateAdded, graphIRI));
			statements.add(f.createStatement(slideLayout, createdBy, user, graphIRI));
			if (slideLayoutWidth != null) statements.add(f.createStatement(slideLayout, hasWidth, slideLayoutWidth, graphIRI));
			if (slideLayoutHeight != null) statements.add(f.createStatement(slideLayout, hasHeight, slideLayoutHeight, graphIRI));
			
			if (s.getBlocks() != null) {
				for (Block b: s.getBlocks()) {
					IRI block = f.createIRI(b.getUri());
					statements.add(f.createStatement(slideLayout, hasBlock, block, graphIRI));
				}
			}
			
			sparqlDAO.addStatements(statements, graphIRI);
		} 
		// link local one to public uri
		List<Statement> statements2 = new ArrayList<Statement>();
        statements2.add(f.createStatement(local, hasPublicURI, slideLayout, userGraphIRI));
        statements2.add(f.createStatement(local, hasModifiedDate, dateCreated, userGraphIRI));
        statements2.add(f.createStatement(local, hasAddedToLibrary, dateAdded, userGraphIRI));
        statements2.add(f.createStatement(local, RDF.TYPE, slideLayoutType, userGraphIRI));
        sparqlDAO.addStatements(statements2, userGraphIRI);
		return publicURI;
	}

    @Override
    public Spot getSpotByFeatures(List<Feature> features, String slideLayoutId, String blockId,
            UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        
        
        String slideLayoutURI = null;
        if (slideLayoutId != null) {
            slideLayoutURI = prefix + slideLayoutId;
        }
        
        String blockURI = null;
        if (blockId != null) {
            blockURI = prefix + blockId;
        }
        
        if (slideLayoutURI != null) {
            // load the slide layout from cache and find the features there
            SlideLayoutEntity entity = slideLayoutRepository.findByUri(slideLayoutURI);
            if (entity != null) {
                Spot spot = findSpotInEntity (entity, features, blockId);
                if (spot != null)
                    return spot;
            }
        } else {
            // check all slide layouts
            List<SlideLayoutEntity> layouts = slideLayoutRepository.findAll();
            for (SlideLayoutEntity entity: layouts) {
                Spot spot = findSpotInEntity (entity, features, blockId);
                if (spot != null)
                    return spot;
            }
        }
        
        String whereClause = "";
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
        queryBuf.append ("FROM <" + graph + ">\n");
        
        for (Feature f: features) {
            whereClause += "?s gadr:has_feature <" + f.getUri() + "> . \n";
        }
        if (blockURI != null) {
            whereClause +=  "<" + blockURI + "> template:has_spot ?s .  \n";
        } else if (slideLayoutURI != null) {
            whereClause += "<" + slideLayoutURI + "> gadr:has_block ?b . ?b template:has_spot ?s .  \n";
        }
        if (!whereClause.isEmpty()) {
            queryBuf.append ("WHERE { " + whereClause + "}");
        }
            
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        if (results.isEmpty())
            return null;
        else {
            String spotURI = results.get(0).getValue("s");
            return getSpotFromURI(spotURI, user);
        }
    }
    
    private Spot findSpotInEntity(SlideLayoutEntity entity, List<Feature> features, String blockId) {
        try {
            SlideLayout s;
            if (slideLayoutCache.get(entity.getUri()) != null)
                s = slideLayoutCache.get(entity.getUri());
            else {
                s = new ObjectMapper().readValue(entity.getJsonValue(), SlideLayout.class);
                slideLayoutCache.put(entity.getUri(), s);
            }
            for (Block b: s.getBlocks()) {
                if (blockId != null && b.getId().equals(blockId)) {
                    // check only in this block
                    for (Spot spot: b.getBlockLayout().getSpots()) {
                        boolean match = true;
                        for (Feature feature: features) {
                            boolean found = false;
                            for (Feature f: spot.getFeatures()) {
                                if (f.getId().equals(feature.getId())) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found)
                                match = false;
                        }
                        if (match) {
                            return spot;
                        }
                    }
                    
                } else if (blockId == null) {
                    // check this block
                    for (Spot spot: b.getBlockLayout().getSpots()) {
                        boolean match = true;
                        for (Feature feature: features) {
                            boolean found = false;
                            for (Feature f: spot.getFeatures()) {
                                if (f.getId().equals(feature.getId())) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found)
                                match = false;
                        }
                        if (match) {
                            return spot;
                        }
                    }
                }
                
            }
        } catch (IOException e) {
            logger.error("Could not read slide layout from serialized value", e);
        }
        
        return null;
    }

    @Override
    public Spot getSpotByPosition (String slideLayoutId, String blockId, int row, int column, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        String slideLayoutURI = null;
        if (slideLayoutId != null) {
            slideLayoutURI = uriPrefix + slideLayoutId;
        }
        String blockURI = null;
        if (blockId != null) {
            blockURI = uriPrefix + blockId;
        }
        
        String whereClause = "";
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
        queryBuf.append ("FROM <" + graph + ">\n");
        
        whereClause += "?s gadr:has_row \"" + row + "\"^^xsd:int . \n"; 
        whereClause += "?s gadr:has_column \"" + column + "\"^^xsd:int . \n";
        if (blockURI != null) {
            whereClause += "<" + blockURI + "> template:has_block_layout ?bl . ?bl template:has_spot ?s .  \n";
        } else if (slideLayoutURI != null) {
            whereClause += "<" + slideLayoutURI + "> gadr:has_block ?b . ?b template:has_block_layout ?bl . ?bl template:has_spot ?s .  \n";
        }   
        queryBuf.append ("WHERE { " + whereClause + " }");
        
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        if (results.isEmpty())
            return null;
        else {
            String spotURI = results.get(0).getValue("s");
            return getSpotFromURI(spotURI, user);
        }
    }
}
