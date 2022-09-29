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
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.SlideLayoutEntity;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.SlideLayoutRepository;
import org.glygen.array.persistence.rdf.Block;
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.Creator;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.GPLinkedGlycoPeptide;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.GlycanInFeature;
import org.glygen.array.persistence.rdf.GlycanSubsumtionType;
import org.glygen.array.persistence.rdf.GlycoLipid;
import org.glygen.array.persistence.rdf.GlycoPeptide;
import org.glygen.array.persistence.rdf.GlycoProtein;
import org.glygen.array.persistence.rdf.LinkedGlycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.RatioConcentration;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.data.FileWrapper;
import org.glygen.array.util.SparqlUtils;
import org.grits.toolbox.glycanarray.library.om.layout.LevelUnit;
import org.grits.toolbox.glycanarray.om.model.UnitOfLevels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional(value="sesameTransactionManager", rollbackFor = SparqlException.class) 
public class LayoutRepositoryImpl extends GlygenArrayRepositoryImpl implements LayoutRepository {
	
	@Autowired
	GlycanRepository glycanRepository;
	
	@Autowired
	LinkerRepository linkerRepository;
	
	@Autowired
	FeatureRepository featureRepository;
	
	@Autowired
	SlideLayoutRepository slideLayoutRepository;
	
	@Autowired
    MetadataRepository metadataRepository;
	
	Map<String, SlideLayout> slideLayoutCache = new HashMap<String, SlideLayout>();
	Map<String, BlockLayout> blockLayoutCache = new HashMap<String, BlockLayout>();
	Map<String, Feature> featureCache = new HashMap<String, Feature>();
	Map<Long, String> linkerCache = new HashMap<Long, String>();
	Map<String, String> glycanCache = new HashMap<String, String>();
	Map<LevelUnit, String> concentrationCache = new HashMap<LevelUnit, String>();
	
	final static String hasRatioPredicate = ontPrefix + "has_ratio";
	final static String hasRatioContextPredicate = ontPrefix + "has_feature_ratio";
	final static String hasConcentrationContextPredicate = ontPrefix + "has_feature_concentration";
	
	
	private String addBlock(Block b, UserEntity user, String graph) throws SparqlException, SQLException {
	    String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
		String blockURI = generateUniqueURI(uriPrefix + "B", allGraphs);
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
            layoutFromRepository = blockLayoutCache.get(blockLayout.getUri());
        }
            
        if (layoutFromRepository == null) {  // first time loading
            if (blockLayout.getId() != null && !blockLayout.getId().isEmpty()) {
                layoutFromRepository = getBlockLayoutById(blockLayout.getId().trim(), user, false);
            }
            else if (blockLayout.getName() != null && !blockLayout.getName().isEmpty()) {
                layoutFromRepository = getBlockLayoutByName(blockLayout.getName().trim(), user, false);
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
		String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
		String blockLayoutURI = generateUniqueURI(uriPrefix + "BL", allGraphs);
		
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
		
		// add it to blockLayoutCache
		blockLayoutCache.put(blockLayoutURI, b);
		
		return blockLayoutURI;
	}
	
	String addSpot (Spot s, UserEntity user, String graph) throws SparqlException, SQLException {
	    ValueFactory f = sparqlDAO.getValueFactory();
	    String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
	    IRI graphIRI = f.createIRI(graph);
	    IRI hasFeature = f.createIRI(ontPrefix + "has_feature");
        IRI hasConcentration = f.createIRI(ontPrefix + "has_concentration");
        IRI hasConcentrationValue = f.createIRI(ontPrefix + "concentration_value");
        IRI hasConcentrationUnit = f.createIRI(ontPrefix + "has_concentration_unit");
        IRI hasGroup = f.createIRI(ontPrefix + "has_group");
        IRI hasFlag = f.createIRI(ontPrefix + "has_flag");
        IRI hasRow = f.createIRI(ontPrefix + "has_row");
        IRI hasColumn = f.createIRI(ontPrefix + "has_column");
        IRI spotType = f.createIRI(ontPrefix + "Spot");
        IRI hasSpotMetadata = f.createIRI(hasSpotMetadataPredicate);
	    List<Statement> statements = new ArrayList<Statement>();
        String spotURI = generateUniqueURI(uriPrefix + "S", allGraphs);
        IRI spot = f.createIRI(spotURI);
        Literal row = f.createLiteral(s.getRow());
        Literal column = f.createLiteral(s.getColumn());
        Literal group = s.getGroup() == null ? null : f.createLiteral(s.getGroup());
        Literal flag = s.getFlag() == null ? null : f.createLiteral(s.getFlag());
        
        statements.add(f.createStatement(spot, RDF.TYPE, spotType, graphIRI));
        statements.add(f.createStatement(spot, hasRow, row));
        statements.add(f.createStatement(spot, hasColumn, column));
        if (group != null) statements.add(f.createStatement(spot, hasGroup, group, graphIRI));
        if (flag != null) statements.add(f.createStatement(spot, hasFlag, flag, graphIRI));
        
        sparqlDAO.addStatements(statements, graphIRI);
        
        if (s.getFeatures() != null) {
            List<Feature> features = s.getFeatures();
            for (Feature feat : features) {
                statements = new ArrayList<Statement>();
                Feature existing = null;
                if (feat.getUri() != null) {
                    existing = feat;
                } else {
                    existing = featureRepository.getFeatureByLabel(feat.getName(), user);
                }
                if (existing != null) {
                    IRI feature = f.createIRI(existing.getUri());
                    statements.add(f.createStatement(spot, hasFeature, feature, graphIRI));
                    Double ratio = s.getFeatureRatioMap().get(feat);
                    if (ratio == null) {
                        RatioConcentration rc = s.getRatioConcentration(existing.getId());
                        if (rc != null) {
                            ratio = rc.getRatio();
                        }
                    }
                    if (ratio != null) {
                        // add ratio for the feature
                        Literal ratioL = f.createLiteral(ratio);
                        String positionContextURI = generateUniqueURI(uriPrefix + "PC", allGraphs);
                        IRI hasRatio = f.createIRI (hasRatioPredicate);
                        IRI hasRatioContext = f.createIRI(hasRatioContextPredicate);
                        IRI positionContext = f.createIRI(positionContextURI);
                        statements.add(f.createStatement(spot, hasRatioContext, positionContext, graphIRI));
                        statements.add(f.createStatement(positionContext, hasFeature, feature, graphIRI));
                        statements.add(f.createStatement(positionContext, hasRatio, ratioL, graphIRI));
                    }
                    
                    // add the concentration
                    LevelUnit concentration = s.getFeatureConcentrationMap().get(feat);
                    if (concentration == null) {
                        RatioConcentration rc = s.getRatioConcentration(existing.getId());
                        if (rc != null) {
                            concentration = rc.getConcentration();
                        }
                    }
                    if (concentration != null) {
                        // add concentration for the feature
                        // check if it has already been created before
                        String concentrationURI = concentrationCache.get(concentration);
                        if (concentrationURI == null) {
                            concentrationURI = generateUniqueURI(uriPrefix + "C", allGraphs);
                            concentrationCache.put(concentration, concentrationURI);
                        }
                        IRI concentrationL = f.createIRI(concentrationURI);
                    
                        Literal concentrationUnit = f.createLiteral(concentration.getLevelUnit().getLabel());
                        Literal concentrationValue = concentration.getConcentration() == null ? null : f.createLiteral(concentration.getConcentration());
                        if (concentrationValue != null) {
                            String concentrationContextURI = generateUniqueURI(uriPrefix + "CC", allGraphs);    
                            IRI hasConcentrationContext = f.createIRI(hasConcentrationContextPredicate);
                            IRI concentrationContext = f.createIRI(concentrationContextURI);
                            statements.add(f.createStatement(spot, hasConcentrationContext, concentrationContext, graphIRI));
                            statements.add(f.createStatement(concentrationContext, hasFeature, feature, graphIRI));
                            statements.add(f.createStatement(concentrationContext, hasConcentration, concentrationL, graphIRI));
                            statements.add(f.createStatement(concentrationL, hasConcentrationValue, concentrationValue, graphIRI));
                            statements.add(f.createStatement(concentrationL, hasConcentrationUnit, concentrationUnit, graphIRI));
                        }
                        //statements.add(f.createStatement(spot, hasConcentration, concentrationL, graphIRI));
                    }
                } else {
                    // error
                    throw new SparqlException ("Feature with label " + feat.getName() + " cannot be found!");
                }
                sparqlDAO.addStatements(statements, graphIRI);
            }
        }
        
        if (s.getMetadata() != null) {
            if (s.getMetadata().getUri() != null) {
                statements.add(f.createStatement(spot, hasSpotMetadata, f.createIRI(s.getMetadata().getUri()), graphIRI));
            } else {
                String metadataURI = metadataRepository.addSpotMetadataValue(s.getMetadata(), user);
                statements.add(f.createStatement(spot, hasSpotMetadata, f.createIRI(metadataURI), graphIRI));
            }
            sparqlDAO.addStatements(statements, graphIRI);
        }
        
        return spotURI;
	}
	
	@Override
    public String addSlideLayout(SlideLayout s, UserEntity user) throws SparqlException, SQLException {
	    return addSlideLayout(s, user, false);
	}
	
	@Override
	public String addSlideLayout(SlideLayout s, UserEntity user, Boolean layoutOnly) throws SparqlException, SQLException {
		String graph = null;
		try {
			// check if there is already a private graph for user
			graph = getGraphForUser(user);
		} catch (SQLException e) {
			throw new SparqlException ("Cannot add the private graph for the user: " + user.getUsername(), e);
		}
		blockLayoutCache.clear();
		String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
		String slideLayoutURI = generateUniqueURI(uriPrefix + "SL", allGraphs);
		
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
		
		if (!layoutOnly && s.getBlocks() != null) {
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
		
		if (s.getFile() != null)
            saveFile (s.getFile(), slideLayoutURI, graph);
		
		if (!layoutOnly) {
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
    		slideLayoutCache.put(slideLayoutURI, s);
		}
		
		return slideLayoutURI;
	}
	
	@Override
	public String addBlocksToSlideLayout (SlideLayout s, UserEntity user) throws SparqlException, SQLException {
	    String graph = null;
        String uriPre = uriPrefix;
        if (user == null) {
            uriPre = uriPrefixPublic;
            graph = DEFAULT_GRAPH;
        } else {
            // check if there is already a private graph for user
            graph = getGraphForUser(user);
        }
        ValueFactory f = sparqlDAO.getValueFactory();
        List<Statement> statements = new ArrayList<Statement>();
	    String uri = s.getUri();
        if (uri == null && s.getId() != null) {
            uri = uriPre + s.getId();
        }
        if (uri != null) {
            IRI slideLayout = f.createIRI(uri);
            IRI graphIRI = f.createIRI(graph);
            
            IRI hasBlock = f.createIRI(ontPrefix + "has_block");
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
                    // also update the slide layout object with the blocklayout info with spots if they are already in the cache
                    if (b.getBlockLayout() != null) {
                        BlockLayout fromCache = blockLayoutCache.get(b.getBlockLayout().getId());
                        if (fromCache != null && fromCache.getSpots() != null && !fromCache.getSpots().isEmpty())
                            b.setBlockLayout(fromCache);
                    }
                }
            }
            sparqlDAO.addStatements(statements, graphIRI);
           
            // add it to the slidelayoutrepository as well
            Date date = new Date();
            s.setUri(uri);
            s.setId(uri.substring(uri.lastIndexOf("/") + 1));
            s.setDateAddedToLibrary(date);
            s.setDateCreated(date);
            s.setDateModified(date);
            Creator creator = new Creator();
            creator.setName(user.getUsername());
            creator.setUserId(user.getUserId());
            s.setUser(creator);
            SlideLayoutEntity slideLayoutEntity = new SlideLayoutEntity();
            slideLayoutEntity.setUri(uri);
            try {
                slideLayoutEntity.setJsonValue(new ObjectMapper().writeValueAsString(s));
                slideLayoutRepository.save(slideLayoutEntity);
            } catch (JsonProcessingException e) {
                logger.error("Could not serialize Slide layout into JSON for caching", e);
            }
            slideLayoutCache.put(uri, s);
        }
        
        return uri;
	}
	
	@Override
	public void deleteBlockLayout(String blockLayoutId, UserEntity user) throws SparqlException, SQLException {
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
		    if (canDeleteBlockLayout(uriPre + blockLayoutId, graph)) {
    			// check to see if the given blockLayoutId is in this graph
    			BlockLayout existing = getBlockLayoutFromURI (uriPre + blockLayoutId, user);
    			if (existing != null) {
    				deleteBlockLayoutByURI (uriPre + blockLayoutId, graph);
    				// remove from the cache
    				blockLayoutCache.remove(uriPre + blockLayoutId);
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
		
		RepositoryResult<Statement> statements4 = sparqlDAO.getStatements(blockLayout, hasSpot, null, graphIRI);
		while (statements4.hasNext()) {
			Statement st = statements4.next();
			String spotURI = st.getObject().stringValue(); // get the spot and delete it
			deleteSpotByURI (spotURI, graph);
		}
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(blockLayout, null, null, graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
	}
	
	private void deleteSpotByURI(String spotURI, String graph) throws RepositoryException, SparqlException {
	    ValueFactory f = sparqlDAO.getValueFactory();
        IRI hasConcentrationContext = f.createIRI(hasConcentrationContextPredicate);
        IRI hasRatioContext = f.createIRI(hasRatioContextPredicate);
        IRI spot = f.createIRI(spotURI);
        IRI graphIRI = f.createIRI(graph);
        
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(spot, hasConcentrationContext, null, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next(); 
            String contextURI = st.getObject().stringValue();
            // delete all triples for the context
            IRI concentrationContext = f.createIRI(contextURI);
            RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(concentrationContext, null, null, graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(statements2), graphIRI);
        }
        
        statements = sparqlDAO.getStatements(spot, hasRatioContext, null, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next(); 
            String contextURI = st.getObject().stringValue();
            // delete all triples for the context
            IRI ratioContext = f.createIRI(contextURI);
            RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(ratioContext, null, null, graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(statements2), graphIRI);
        }
        
        statements = sparqlDAO.getStatements(spot, null, null, graphIRI);
        sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
    }

    boolean canDeleteBlockLayout (String blockURI, String graph) throws SparqlException, SQLException { 
        boolean canDelete = true;
        
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        //queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
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
        //queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
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
		String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        }
        else {
            graph = getGraphForUser(user);
        }
		if (graph != null) {
		    if (canDeleteSlideLayout(uriPre + slideLayoutId, graph)) {
    			// check to see if the given slideLayoutId is in this graph
    			SlideLayout existing = getSlideLayoutFromURI (uriPre + slideLayoutId, false, user);
    			if (existing != null) {
    				deleteSlideLayoutByURI (uriPre + slideLayoutId, graph);
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
		
		// delete file
		deleteFiles(uri, graph);
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(slideLayout, null, null, graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
		
		// delete from SlideLayoutRepository too
		SlideLayoutEntity entity = slideLayoutRepository.findByUri(uri);
		if (entity != null) {
		    slideLayoutRepository.delete(entity);
		    slideLayoutCache.remove(uri);
		}
	}
	
	@Override
	public String getPublicBlockURI(String blockURI, String publicSlideLayoutURI, UserEntity user) throws SparqlException, SQLException {
        Block block = getBlock(blockURI, false, user);
        
        // find the same block in the given slide layout in the public repository
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?b \n");
        queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ( "<" + publicSlideLayoutURI + "> gadr:has_block ?b");
        queryBuf.append ( "?b gadr:has_row \"" + block.getRow() + "\"^^xsd:int . ?b gadr:has_column \"" + block.getColumn() + "\"^^xsd:int .  }\n");
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        for (SparqlEntity sparqlEntity : results) {
            String blockLayoutURI = sparqlEntity.getValue("b");
            return blockLayoutURI;
        }
        return null;
    }
	
	private Block getBlock (String blockURI, boolean loadAll, UserEntity user) throws SparqlException, SQLException {
	    String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            if (blockURI.contains("public"))
                graph = DEFAULT_GRAPH;
            else
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
				if (loadAll && blockLayoutCache.containsKey(blockLayoutURI)) {
				    blockLayout = blockLayoutCache.get(blockLayoutURI);
				    if (loadAll && (blockLayout.getSpots() == null || blockLayout.getSpots().isEmpty())) {
				        // need to reload
				        blockLayout = getBlockLayoutFromURI(blockLayoutURI, loadAll, user);    // need the spots
	                    blockLayoutCache.put(blockLayoutURI, blockLayout);
				    }
				} else {
				    blockLayout = getBlockLayoutFromURI(blockLayoutURI, loadAll, user);    
				    if (loadAll) blockLayoutCache.put(blockLayoutURI, blockLayout);
				}
				blockObject.setBlockLayout(blockLayout);
			} else if (st.getPredicate().equals(hasRow)) {
				Value v = st.getObject();
				blockObject.setRow(Integer.parseInt(v.stringValue()));
			} else if (st.getPredicate().equals(hasColumn)) {
				Value v = st.getObject();
				blockObject.setColumn(Integer.parseInt(v.stringValue()));
			}
		}
		
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
		queryBuf.append ("SELECT DISTINCT ?o \n");
		//queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n");
		queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/template#block_layout>. \n");
		queryBuf.append ( "<" +  uriPre + blockLayoutId + "> rdfs:label ?o . }\n");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.isEmpty())
			return null;
		else {
		    // first check the cache if it is already loaded
	        if (loadAll && blockLayoutCache.containsKey(uriPre + blockLayoutId)) {
	            BlockLayout b = blockLayoutCache.get(uriPre + blockLayoutId);
	            // check if the spots are there
                if (b.getSpots() != null && !b.getSpots().isEmpty()) {
                    return b;
                } 
	        }
			return getBlockLayoutFromURI(uriPre + blockLayoutId, loadAll, user);
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
		
		// get all blockLayoutURIs from user's private graph
		String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
		if (graph != null) {
		    String sortPredicate = getSortPredicateForLayout (field);
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
                    " ?s rdf:type  <http://purl.org/gadr/template#block_layout>. \n" +
                    " OPTIONAL {?s gadr:has_public_uri ?public  } .\n" + 
                            sortLine + searchPredicate + 
                    "}\n" );
             if (!graph.equals(GlygenArrayRepository.DEFAULT_GRAPH))  {             
                 queryBuf.append ("UNION {" +
                    "?s gadr:has_public_uri ?public . \n" +
                    "GRAPH <" + GlygenArrayRepository.DEFAULT_GRAPH + "> {\n" +
                    " ?public rdf:type  <http://purl.org/gadr/template#block_layout>. \n" +
                        publicSortLine + publicSearchPredicate + 
                    "}}\n"); 
             }
             queryBuf.append ("}" + 
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
	public int getBlockLayoutCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException {
	    String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        
        return getCountByUserByType(graph, "http://purl.org/gadr/template#block_layout", searchValue, false);
		
	/*	int total = 0;
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
		return total;*/
	}
	
	@Override
	public BlockLayout getBlockLayoutFromURI(String blockLayoutURI, Boolean loadAll, UserEntity user) throws SparqlException, SQLException {
		BlockLayout blockLayoutObject = null;
		String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            if (blockLayoutURI.contains("public"))
                graph = DEFAULT_GRAPH;
            else
                graph = getGraphForUser(user);
        }
        
        featureCache.clear();
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI blockLayout = f.createIRI(blockLayoutURI);
		IRI graphIRI = f.createIRI(graph);
		IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
		IRI hasSpot = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_spot");
		IRI hasWidth = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_width");
		IRI hasHeight = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_height");
		IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		IRI hasPublicURI = f.createIRI(ontPrefix + "has_public_uri");
		
		
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
					Spot s = getSpotFromURI(spotURI, user);
					if (s != null)
					    spots.add(s);
				} else if (st.getPredicate().equals(hasPublicURI)) {
				    String publicURI = st.getObject().stringValue();
				    IRI publicBlockLayout = f.createIRI(publicURI);
				    RepositoryResult<Statement> statementsPublic = sparqlDAO.getStatements(publicBlockLayout, null, null, defaultGraphIRI);
				    while (statementsPublic.hasNext()) {
				        Statement stPublic = statementsPublic.next();
				        if (stPublic.getPredicate().equals(RDFS.LABEL)) {
		                    Value v = stPublic.getObject();
		                    blockLayoutObject.setName(v.stringValue());
		                } else if (stPublic.getPredicate().equals(RDFS.COMMENT)) {
		                    Value v = stPublic.getObject();
		                    blockLayoutObject.setDescription(v.stringValue());
		                } else if (stPublic.getPredicate().equals(hasWidth)) {
		                    Value v = stPublic.getObject();
		                    if (v != null) blockLayoutObject.setWidth(Integer.parseInt(v.stringValue()));
		                } else if (stPublic.getPredicate().equals(hasHeight)) {
		                    Value v = stPublic.getObject();
		                    if (v != null) blockLayoutObject.setHeight(Integer.parseInt(v.stringValue()));
		                } else if ((loadAll == null || loadAll) && stPublic.getPredicate().equals(hasSpot)) {
		                    Value v = stPublic.getObject();
		                    String spotURI = v.stringValue();
		                    Spot s = getSpotFromURI(spotURI, user);
		                    if (s != null)
		                        spots.add(s);
		                }
				    }
				}
			}
			blockLayoutObject.setSpots(spots);
		}
		
		if (loadAll) {
		    // add it to the cache
		    blockLayoutCache.put(blockLayoutURI, blockLayoutObject);
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
		queryBuf.append ("SELECT DISTINCT ?o \n");
		//queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n");
		queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/template#slide_layout>. \n");
		queryBuf.append ( "<" +  uriPre + slideLayoutId.trim() + "> rdfs:label ?o . }\n");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.isEmpty())
			return null;
		else {
			return getSlideLayoutFromURI(uriPre + slideLayoutId, loadAll, user);
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
		
		// get all blockLayoutURIs from user's private graph
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
		if (graph != null) {
		    String sortPredicate = getSortPredicateForLayout (field);
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
                            " ?s rdf:type  <http://purl.org/gadr/template#slide_layout>. \n" +
                    " OPTIONAL {?s gadr:has_public_uri ?public  } .\n" + 
                            sortLine + searchPredicate + 
                    "}\n" );
             if (!graph.equals(GlygenArrayRepository.DEFAULT_GRAPH))  {             
                 queryBuf.append ("UNION {" +
                    "?s gadr:has_public_uri ?public . \n" +
                    "GRAPH <" + GlygenArrayRepository.DEFAULT_GRAPH + "> {\n" +
                    " ?public rdf:type  <http://purl.org/gadr/template#slide_layout>. \n" +
                        publicSortLine + publicSearchPredicate + 
                    "}}\n"); 
             }
             queryBuf.append ("}" + 
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
	public int getSlideLayoutCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException {
	    String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        
        return getCountByUserByType(graph, "http://purl.org/gadr/template#slide_layout", searchValue, false);
	}

	@Override
	public SlideLayout getSlideLayoutFromURI(String slideLayoutURI, Boolean loadAll, UserEntity user) throws SparqlException, SQLException {
		SlideLayout slideLayoutObject = null;
		String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            if (slideLayoutURI.contains("public"))
                graph = DEFAULT_GRAPH;
            else
                graph = getGraphForUser(user);
        }
        
        // check the slideLayoutRepository first, if loadAll = true, if not loading all, this takes longer than getting from repo
        if (loadAll) {
            SlideLayoutEntity entity = slideLayoutRepository.findByUri(slideLayoutURI);
            if (entity != null) {
                try {
                    SlideLayout s = new ObjectMapper().readValue(entity.getJsonValue(), SlideLayout.class);
                    if (slideLayoutURI.contains("public")) 
                        s.setIsPublic(true);
                    // check if the blocks/spots are there to make sure we have the full layout
                    if (s.getBlocks() != null && !s.getBlocks().isEmpty() && s.getBlocks().get(0).getBlockLayout() != null &&
                            s.getBlocks().get(0).getBlockLayout().getSpots() != null && !s.getBlocks().get(0).getBlockLayout().getSpots().isEmpty()) {
                        slideLayoutCache.put(s.getUri(), s);
                        return s;
                    }
                } catch (Exception e) {
                    logger.error("Could not read slide layout from serialized value", e);
                }
            }
        }
        
       // if (loadAll) blockLayoutCache.clear();
        
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
			} 
			if (slideLayoutURI.contains("public")) {
				slideLayoutObject.setIsPublic(true);
			}
		}
		if (slideLayoutObject != null) {
		    List<Block> blocks = new ArrayList<>();
		    slideLayoutObject.setBlocks(blocks); // extractFromStatements will add to this list as necessary
			extractFromStatements (statements, slideLayoutObject, loadAll, user);
		}
		
		// put it in the cache, if loadAll is true
		if (loadAll && slideLayoutObject != null && slideLayoutObject.getUri() != null) {
		    slideLayoutCache.put(slideLayoutObject.getUri(), slideLayoutObject);
		    SlideLayoutEntity entity = slideLayoutRepository.findByUri(slideLayoutURI);
            if (entity == null) {
    		    entity = new SlideLayoutEntity();
    		    entity.setUri(slideLayoutURI);
            }
            // update it anyway
	        try {
	            entity.setJsonValue(new ObjectMapper().writeValueAsString(slideLayoutObject));
	            slideLayoutRepository.save(entity);
	        } catch (JsonProcessingException e) {
	            logger.error("Could not serialize Slide layout into JSON for caching", e);
	        }
		}
		if (slideLayoutObject != null)
		    getStatusFromURI (slideLayoutObject.getUri(), slideLayoutObject, graph);
		return slideLayoutObject;
	}
	
	void extractFromStatements (RepositoryResult<Statement> statements, SlideLayout slideLayoutObject, Boolean loadAll, UserEntity user) throws SparqlException, SQLException {
	    String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            if (slideLayoutObject.getUri() != null && slideLayoutObject.getUri().contains("public"))
                graph = DEFAULT_GRAPH;
            else
                graph = getGraphForUser(user);
        }
	    ValueFactory f = sparqlDAO.getValueFactory();
        
	    IRI graphIRI = f.createIRI(graph);
        IRI hasBlock = f.createIRI(ontPrefix + "has_block");
        IRI hasWidth = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_width");
        IRI hasHeight = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_height");
        IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
        IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
        IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
        IRI createdBy= f.createIRI(ontPrefix + "created_by");
        IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
        IRI hasPublicURI = f.createIRI(ontPrefix + "has_public_uri");
        IRI hasFile = f.createIRI(hasFilePredicate);
        IRI hasFileName = f.createIRI(hasFileNamePredicate);
        IRI hasOriginalFileName = f.createIRI(hasOriginalFileNamePredicate);
        IRI hasFolder = f.createIRI(hasFolderPredicate);
        IRI hasFileFormat = f.createIRI(hasFileFormatPredicate);
        IRI hasSize = f.createIRI(hasSizePredicate);
        
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
            } else if (st.getPredicate().equals(hasBlock)) {
                Value v = st.getObject();
                String blockURI = v.stringValue();
                Block block = getBlock (blockURI, loadAll, user);
                slideLayoutObject.getBlocks().add(block);
            } else if (st.getPredicate().equals(hasFile)) {
                Value value = st.getObject();
                if (!value.stringValue().startsWith("http"))
                    continue;
                // retrieve file details
                FileWrapper file = getFileFromURI(value.stringValue(), graph);
                slideLayoutObject.setFile(file);    
            } else if (st.getPredicate().equals(hasPublicURI)) {
                // need to retrieve additional information from DEFAULT graph
                slideLayoutObject.setIsPublic(true);
                Value uriValue = st.getObject();
                String publicLayoutURI = uriValue.stringValue();
                
                // check the slideLayoutRepository first, if loadAll = true
                if (loadAll) {
                    SlideLayoutEntity entity = slideLayoutRepository.findByUri(publicLayoutURI);
                    if (entity != null) {
                        try {
                            SlideLayout s = new ObjectMapper().readValue(entity.getJsonValue(), SlideLayout.class);
                            // check if the blocks/spots are there to make sure we have the full layout
                            if (s.getBlocks() != null && !s.getBlocks().isEmpty() && s.getBlocks().get(0).getBlockLayout() != null &&
                                    s.getBlocks().get(0).getBlockLayout().getSpots() != null && !s.getBlocks().get(0).getBlockLayout().getSpots().isEmpty()) {
                                slideLayoutObject.getBlocks().addAll(s.getBlocks());
                                slideLayoutObject.setName(s.getName());
                                slideLayoutObject.setDescription(s.getDescription());
                                slideLayoutObject.setWidth(s.getWidth());
                                slideLayoutObject.setHeight(s.getHeight());
                                slideLayoutObject.setUser(s.getUser());
                                return;
                            }
                        } catch (IOException e) {
                            logger.error("Could not read slide layout from serialized value", e);
                        }
                    }
                }
                
                IRI publicLayout = f.createIRI(publicLayoutURI);
                RepositoryResult<Statement> statementsPublic = sparqlDAO.getStatements(publicLayout, null, null, defaultGraphIRI);
                extractFromStatements (statementsPublic, slideLayoutObject, loadAll, user);
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

	void updateSlideLayoutInGraph (SlideLayout layout, String graph) throws SparqlException, SQLException {	
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI graphIRI = f.createIRI(graph);
		String layoutURI = layout.getUri();
		IRI slideLayout = f.createIRI(layoutURI);
		Literal label = f.createLiteral(layout.getName().trim());
		Literal comment = layout.getDescription() == null ? f.createLiteral("") : f.createLiteral(layout.getDescription().trim());
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
		
		if (layout.getFile() != null) {
		    deleteFiles(layoutURI, graph);
		    saveFile (layout.getFile(), layoutURI, graph);
		}
		
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
		else if (field.equalsIgnoreCase("height")) 
		    return "template:has_height";
		else if (field.equalsIgnoreCase("width")) 
            return "template:has_width";
		else if (field.equalsIgnoreCase("id"))
			return null;	
		return null;
	}
	
	@Override
    public String getSearchPredicate (String searchValue, String queryLabel) {
	    if (searchValue != null) {
            searchValue = SparqlUtils.escapeSpecialCharacters (searchValue.trim());
        }
        String predicates = "";
        
        predicates += queryLabel + " rdfs:label ?value1 .\n";
        predicates += "OPTIONAL {" + queryLabel + " rdfs:comment ?value2} \n";
       
        
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
			// remove from cache
			blockLayoutCache.remove(layout.getUri());
		}
	}

	void updateBlockLayoutInGraph (BlockLayout layout, String graph) throws SparqlException {	
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI graphIRI = f.createIRI(graph);
		String layoutURI = layout.getUri();
		IRI blockLayout = f.createIRI(layoutURI);
		Literal label = f.createLiteral(layout.getName().trim());
		Literal comment = layout.getDescription() == null ? f.createLiteral("") : f.createLiteral(layout.getDescription().trim());
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
	public String makePublic(SlideLayout layout, UserEntity user, Map<String, String> uriMapOldToNew) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
        Map<String, Glycan > processedGlycans = new HashMap<>();
        Map<String, Linker > processedLinkers = new HashMap<>();
        Map<String, Feature > processedFeatures = new HashMap<>();
    	// first make other components public
        List<Block> publicBlocks = new ArrayList<Block>();
    	for (Block block: layout.getBlocks()) {
    		BlockLayout blockLayout = block.getBlockLayout();
    		String prevURI = block.getUri();
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
    		} else {
    			String blockLayoutURI = results2.get(0).getValue("s");
    			deleteByURI (uriPrefix + blockLayout.getId(), graph);
    			publicURI = addPublicBlockLayout (blockLayout, blockLayoutURI, user, processedGlycans, processedLinkers, processedFeatures);
    		}
    		
    		deleteByURI (uriPrefix + block.getId(), graph);
    		BlockLayout blockL = null;
    		if (blockLayoutCache.containsKey(publicURI))
    		    blockL = blockLayoutCache.get(publicURI);
    		else
    		    blockL = getBlockLayoutFromURI(publicURI, null);
    		block.setBlockLayout(blockL);
    		String blockURI = addPublicBlock (block, graph);
    		Block newBlock = getBlock(blockURI, true, null);
    		uriMapOldToNew.put(prevURI, blockURI);
    		publicBlocks.add(newBlock);
    	}
    	
    	layout.setBlocks(publicBlocks);
        // make it public
        deleteByURI(uriPrefix + layout.getId(), graph);
        updateSlideLayoutInGraph(layout, graph);
        // need to create the slidelayout in the public graph, link the user's version to public one
        return addPublicSlideLayout(layout, null, graph, user.getUsername()); 
	}

	private String addPublicBlock(Block block, String graph) throws SparqlException, SQLException {
	    String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
		String blockURI = generateUniqueURI(uriPrefixPublic + "B", allGraphs);
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
			    String featurePublicURI = getPublicUri(previous, user);
			    if (featurePublicURI != null || previous.contains("public")) {
			        Feature newFeature = feature;
			        // already public
			        if (!previous.contains("public")) {
			            newFeature = featureRepository.getFeatureFromURI(featurePublicURI, null);
			        }
			        publicFeatures.add(newFeature);
			        processedFeatures.put(previous, newFeature);
			    } else {
    			    if (!processedFeatures.containsKey(previous)) {
        				populatePublicGlycansForFeature(feature, processedGlycans, processedFeatures, processedLinkers, user);
        				Linker l = feature.getLinker();
        				if (l != null) {
        				    String publicURI  = getPublicUri(l.getUri(), user);
        				    if (publicURI != null || l.getUri().contains("public")) {
        				        if (!l.getUri().contains("public")) {
        				            l.setUri(publicURI);
        				            l.setIsPublic(true);
        				        }
        				        if (!processedLinkers.containsKey(l.getUri())) {
        				            processedLinkers.put(l.getUri(), l);
        				            feature.setLinker(l); // get the public one
        				        }
        				    } else {
            				    String previousL = l.getUri();
                                if (!processedLinkers.containsKey(previousL)) {
                					String linkerURI = linkerRepository.makePublic(l, user);
                					if (linkerURI != null) {
                    					Linker newLinker = linkerRepository.getLinkerFromURI(linkerURI, null);
                    					feature.setLinker(newLinker); // get the public one
                    					processedLinkers.put(previousL, newLinker);
                					} else {
                    					// retrieve the existing one
                    					Linker existing = linkerRepository.getLinkerByLabel(l.getName(), l.getType(), null);
                    					if (existing != null) {
                    					    feature.setLinker(existing);
                    					    processedLinkers.put(previousL, existing);
                    					}
                					}
                                } else {
                                    feature.setLinker(processedLinkers.get(previousL));
                                }
        				    }
        				}
        				// make feature public
        				featureURI = featureRepository.addPublicFeature (feature, user);
        				Feature newFeature = featureRepository.getFeatureFromURI(featureURI, null);
        				publicFeatures.add(newFeature);
        				processedFeatures.put(previous, newFeature);
    			    } else {
    	                publicFeatures.add(processedFeatures.get(previous));
    	            }
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
		String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
		String userGraph = getGraphForUser(user);
		IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
        IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
        IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
        IRI createdBy = f.createIRI(ontPrefix + "created_by");
        Literal owner = f.createLiteral(user.getUsername());
        Date date = new Date();
        Literal dateModified = f.createLiteral(date);
        IRI hasPublicURI = f.createIRI(ontPrefix + "has_public_uri");
        IRI blockLayoutType = f.createIRI(MetadataTemplateRepository.templatePrefix + "block_layout");
        Literal dateCreated = blockLayout.getDateCreated() == null ? f.createLiteral(date) : f.createLiteral(blockLayout.getDateCreated());
		
		if (blockLayoutURI == null) {
			blockLayoutURI = generateUniqueURI(uriPrefixPublic + "BL", allGraphs);
			IRI publicGraphIRI = f.createIRI(DEFAULT_GRAPH);
			IRI blockLayoutIRI = f.createIRI(blockLayoutURI);
			IRI hasSpot = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_spot");
			IRI hasWidth = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_width");
			IRI hasHeight = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_height");
			
			Literal blockLayoutLabel = f.createLiteral(blockLayout.getName().trim());
			Literal blockLayoutComment = blockLayout.getDescription() == null ? null: f.createLiteral(blockLayout.getDescription().trim());
			Literal blockLayoutWidth = blockLayout.getWidth() == null ? null : f.createLiteral(blockLayout.getWidth());
			Literal blockLayoutHeight = blockLayout.getHeight() == null ? null : f.createLiteral(blockLayout.getHeight());
		
			List<Statement> statements = new ArrayList<Statement>();
			statements.add(f.createStatement(blockLayoutIRI, RDF.TYPE, blockLayoutType, publicGraphIRI));
			statements.add(f.createStatement(blockLayoutIRI, RDFS.LABEL, blockLayoutLabel, publicGraphIRI));
			if (blockLayoutComment != null) statements.add(f.createStatement(blockLayoutIRI, RDFS.COMMENT, blockLayoutComment, publicGraphIRI));
			statements.add(f.createStatement(blockLayoutIRI, hasCreatedDate, dateCreated, publicGraphIRI));
			statements.add(f.createStatement(blockLayoutIRI, hasModifiedDate, dateModified, publicGraphIRI));
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
	
		IRI userGraphIRI = f.createIRI(userGraph);
		IRI local = f.createIRI(blockLayout.getUri());
		IRI publicBlockLayout = f.createIRI(blockLayoutURI);
		
		// link local one to public uri
        List<Statement> statements2 = new ArrayList<Statement>();
        statements2.add(f.createStatement(local, hasPublicURI, publicBlockLayout, userGraphIRI));
        statements2.add(f.createStatement(local, hasModifiedDate, dateModified, userGraphIRI));
        statements2.add(f.createStatement(local, hasCreatedDate, dateCreated, userGraphIRI));
        statements2.add(f.createStatement(local, RDF.TYPE, blockLayoutType, userGraphIRI));
        sparqlDAO.addStatements(statements2, userGraphIRI);
		
		return blockLayoutURI;
	}

	private void populatePublicGlycansForFeature(Feature feature, Map<String, Glycan> processedGlycans, 
	        Map<String, Feature> processedFeatures, 
	        Map<String, Linker> processedLinkers, UserEntity user) throws SparqlException, SQLException {
	    
	    switch (feature.getType()) {
	    case LINKEDGLYCAN:
	        List<GlycanInFeature> publicGlycans = new ArrayList<>();
	        for (GlycanInFeature gf: ((LinkedGlycan) feature).getGlycans()) {
	            Glycan g = gf.getGlycan();
	            if (g instanceof SequenceDefinedGlycan) {
	                Glycan baseGlycan = gf.getBaseGlycan();
	                if (baseGlycan == null) {
	                    if (((SequenceDefinedGlycan) g).getSubType() == GlycanSubsumtionType.BASE) {
	                        baseGlycan = g;
	                    } else {
	                        baseGlycan = glycanRepository.retrieveBaseType(g, user);
	                    }
	                }
	                String publicURI = getPublicUri(g.getUri(), user);
	                if (publicURI != null || g.getUri().contains("public")) {
	                    // already public
	                    if (!g.getUri().contains("public")) {
	                        // need to get the public uri
	                        g.setUri(publicURI);
	                    }
                        if (!processedGlycans.containsKey(g.getUri())) {
                            publicGlycans.add(gf); 
                            processedGlycans.put(g.getUri(), g);
                        }
	                } else {
	                    String previousG = g.getUri();
                        if (!processedGlycans.containsKey(previousG)) {
                            // always make the baseGlycan public, the other sub types will become public as well
                            String baseGlycanURI = glycanRepository.makePublic(baseGlycan, user);    baseGlycan.setUri(baseGlycanURI);
                            Glycan newGlycan = glycanRepository.retrieveOtherSubType(baseGlycan, ((SequenceDefinedGlycan) g).getSubType(), null);
                            if (newGlycan != null) {
                                gf.setGlycan(newGlycan);
                                publicGlycans.add(gf); // get the public one
                                processedGlycans.put(previousG, newGlycan);
                            } else {
                                Glycan existing = glycanRepository.getGlycanByLabel(g.getName(), null);
                                if (existing != null) {
                                    gf.setGlycan(existing);
                                    publicGlycans.add(gf);
                                    processedGlycans.put(previousG, existing);
                                }
                            }
                        } else {
                            gf.setGlycan(processedGlycans.get(previousG));
                            publicGlycans.add(gf); // get the public one
                        }
	                }
	            } else {
    	            if (g.getIsPublic()) {
    	                // already public
    	                if (!g.getUri().contains("public")) {
                            // need to get the public uri
                            g.setUri(getPublicUri(g.getUri(), user));
                        }
    	                if (!processedGlycans.containsKey(g.getUri())) {
    	                    publicGlycans.add(gf); 
    	                    processedGlycans.put(g.getUri(), g);
    	                }
    	            } else {
    	                String previousG = g.getUri();
    	                if (!processedGlycans.containsKey(previousG)) {
    	                    String glycanURI = glycanRepository.makePublic(g, user);
    	                    if (glycanURI != null) {
    	                        Glycan newGlycan = glycanRepository.getGlycanFromURI(glycanURI, null);
    	                        gf.setGlycan(g);
    	                        publicGlycans.add(gf); // get the public one
    	                        processedGlycans.put(previousG, newGlycan);
    	                    } else {
    	                        Glycan existing = glycanRepository.getGlycanByLabel(g.getName(), null);
    	                        if (existing != null) {
    	                            gf.setGlycan(existing);
    	                            publicGlycans.add(gf);
    	                            processedGlycans.put(previousG, existing);
    	                        }
    	                    } 
    	                }
    	                else {
    	                    gf.setGlycan(processedGlycans.get(previousG));
    	                    publicGlycans.add(gf); // get the public one
    	                }
    	            }
	            }
	        }
	        ((LinkedGlycan)feature).setGlycans(publicGlycans);
	        break;
	    case GLYCOLIPID:
	        List<LinkedGlycan> publicFeatures = new ArrayList<>();
	        for (LinkedGlycan f: ((GlycoLipid)feature).getGlycans()) {
	            if (f.getUri() != null && f.getUri().contains("public")) {
	                // already public
	                if (!processedFeatures.containsKey(f.getUri())) {
	                    publicFeatures.add(f);
	                    processedFeatures.put(f.getUri(), f);
	                }
	            } else {
	                String previousG = f.getUri();
	                // check if there is a public uri
	                String publicG = previousG != null ? getPublicUri(previousG, user) : null;
                    if (publicG != null) {
                        if (!processedFeatures.containsKey(previousG)) {
                            Feature newLinkedGlycan = featureRepository.getFeatureFromURI(publicG, null);
                            if (newLinkedGlycan != null && newLinkedGlycan instanceof LinkedGlycan) {
                                publicFeatures.add((LinkedGlycan) newLinkedGlycan); // get the public one
                                processedFeatures.put(previousG, newLinkedGlycan);
                            }
                        } 
                        else {
                            publicFeatures.add((LinkedGlycan) processedFeatures.get(previousG)); // get the public one
                        }
                    } else {
                        if (!processedFeatures.containsKey(previousG)) {
                            String glycanURI = featureRepository.addPublicFeature(f, user);
                            if (glycanURI != null) {
                                Feature newLinkedGlycan = featureRepository.getFeatureFromURI(glycanURI, null);
                                if (newLinkedGlycan != null && newLinkedGlycan instanceof LinkedGlycan) {
                                    publicFeatures.add((LinkedGlycan) newLinkedGlycan); // get the public one
                                    processedFeatures.put(previousG, newLinkedGlycan);
                                }
                            } 
                        }
                        else {
                            publicFeatures.add((LinkedGlycan) processedFeatures.get(previousG)); // get the public one
                        }
                    }
	            }
	        }
	        ((GlycoLipid)feature).setGlycans(publicFeatures);
	        break;
        case GLYCOPEPTIDE:
            publicFeatures = new ArrayList<>();
            for (LinkedGlycan f: ((GlycoPeptide)feature).getGlycans()) {
                if (f.getUri() != null && f.getUri().contains("public")) {
                    // already public
                    if (!processedFeatures.containsKey(f.getUri())) {
                        publicFeatures.add(f);
                        processedFeatures.put(f.getUri(), f);
                    }
                } else {
                    String previousG = f.getUri();
                    // check if there is a public uri
                    String publicG = previousG != null ? getPublicUri(previousG, user) : null;
                    if (publicG != null) {
                        if (!processedFeatures.containsKey(previousG)) {
                            Feature newLinkedGlycan = featureRepository.getFeatureFromURI(publicG, null);
                            if (newLinkedGlycan != null && newLinkedGlycan instanceof LinkedGlycan) {
                                publicFeatures.add((LinkedGlycan) newLinkedGlycan); // get the public one
                                processedFeatures.put(previousG, newLinkedGlycan);
                            }
                        } 
                        else {
                            publicFeatures.add((LinkedGlycan) processedFeatures.get(previousG)); // get the public one
                        }
                    } else {
                        if (!processedFeatures.containsKey(previousG)) {
                            String glycanURI = featureRepository.addPublicFeature(f, user);
                            if (glycanURI != null) {
                                Feature newLinkedGlycan = featureRepository.getFeatureFromURI(glycanURI, null);
                                if (newLinkedGlycan != null && newLinkedGlycan instanceof LinkedGlycan) {
                                    publicFeatures.add((LinkedGlycan) newLinkedGlycan); // get the public one
                                    processedFeatures.put(previousG, newLinkedGlycan);
                                }
                            } 
                        }
                        else {
                            publicFeatures.add((LinkedGlycan) processedFeatures.get(previousG)); // get the public one
                        }
                    }
                }
            }
            ((GlycoPeptide)feature).setGlycans(publicFeatures);
            break;
        case GLYCOPROTEIN:
            publicFeatures = new ArrayList<>();
            for (LinkedGlycan f: ((GlycoProtein)feature).getGlycans()) {
                if (f.getUri() != null && f.getUri().contains("public")) {
                    // already public
                    if (!processedFeatures.containsKey(f.getUri())) {
                        publicFeatures.add(f);
                        processedFeatures.put(f.getUri(), f);
                    }
                } else {
                    String previousG = f.getUri();
                    // check if there is a public uri
                    String publicG = previousG != null ? getPublicUri(previousG, user) : null;
                    if (publicG != null) {
                        if (!processedFeatures.containsKey(previousG)) {
                            Feature newLinkedGlycan = featureRepository.getFeatureFromURI(publicG, null);
                            if (newLinkedGlycan != null && newLinkedGlycan instanceof LinkedGlycan) {
                                publicFeatures.add((LinkedGlycan) newLinkedGlycan); // get the public one
                                processedFeatures.put(previousG, newLinkedGlycan);
                            }
                        } 
                        else {
                            publicFeatures.add((LinkedGlycan) processedFeatures.get(previousG)); // get the public one
                        }
                    } else {
                        if (!processedFeatures.containsKey(previousG)) {
                            String glycanURI = featureRepository.addPublicFeature(f, user);
                            if (glycanURI != null) {
                                Feature newLinkedGlycan = featureRepository.getFeatureFromURI(glycanURI, null);
                                if (newLinkedGlycan != null && newLinkedGlycan instanceof LinkedGlycan) {
                                    publicFeatures.add((LinkedGlycan) newLinkedGlycan); // get the public one
                                    processedFeatures.put(previousG, newLinkedGlycan);
                                }
                            } 
                        }
                        else {
                            publicFeatures.add((LinkedGlycan) processedFeatures.get(previousG)); // get the public one
                        }
                    }
                }
            }
            ((GlycoProtein)feature).setGlycans(publicFeatures);
            break;
        case GPLINKEDGLYCOPEPTIDE:
            List<GlycoPeptide> publicFeatures2 = new ArrayList<>();
            for (GlycoPeptide f: ((GPLinkedGlycoPeptide)feature).getPeptides()) {
                if (f.getUri() != null && f.getUri().contains("public")) {
                    // already public
                    if (!processedFeatures.containsKey(f.getUri())) {
                        publicFeatures2.add(f);
                        processedFeatures.put(f.getUri(), f);
                    }
                } else {
                    String previousG = f.getUri();
                    // check if there is a public uri
                    String publicG = previousG != null ? getPublicUri(previousG, user) : null;
                    if (publicG != null) {
                        if (!processedFeatures.containsKey(previousG)) {
                            Feature newLinkedGlycan = featureRepository.getFeatureFromURI(publicG, null);
                            if (newLinkedGlycan != null && newLinkedGlycan instanceof GlycoPeptide) {
                                publicFeatures2.add((GlycoPeptide) newLinkedGlycan); // get the public one
                                processedFeatures.put(previousG, newLinkedGlycan);
                            }
                        } 
                        else {
                            publicFeatures2.add((GlycoPeptide) processedFeatures.get(previousG)); // get the public one
                        }
                    } else {
                        if (!processedFeatures.containsKey(previousG)) {
                            String glycanURI = featureRepository.addPublicFeature(f, user);
                            if (glycanURI != null) {
                                Feature newLinkedGlycan = featureRepository.getFeatureFromURI(glycanURI, null);
                                if (newLinkedGlycan != null && newLinkedGlycan instanceof GlycoPeptide) {
                                    publicFeatures2.add((GlycoPeptide) newLinkedGlycan); // get the public one
                                    processedFeatures.put(previousG, newLinkedGlycan);
                                }
                            } 
                        }
                        else {
                            publicFeatures2.add((GlycoPeptide) processedFeatures.get(previousG)); // get the public one
                        }
                    }
                }
            }
            ((GPLinkedGlycoPeptide)feature).setPeptides(publicFeatures2);
            break;
        default: 
            break;
	        
	    }
    }
	
	@Override
    public Spot getSpotFromURI(String spotURI, UserEntity user) throws SQLException, SparqlException {
	    return getSpotFromURI(spotURI, true, user);
	}

    @Override
	public Spot getSpotFromURI(String spotURI, Boolean loadAll, UserEntity user) throws SQLException, SparqlException {
		String graph = null;
		if (spotURI.contains("public"))
            graph = DEFAULT_GRAPH;
        else {
            if (user != null)
                graph = getGraphForUser(user);
            else 
                graph = DEFAULT_GRAPH;
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
        IRI hasConcentrationContext = f.createIRI(hasConcentrationContextPredicate);
        IRI hasSpot = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_spot");
        IRI hasSpotMetadata = f.createIRI(hasSpotMetadataPredicate);
        
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
				s.setGroup(v.stringValue().trim());
			} else if (st2.getPredicate().equals(hasConcentrationContext)) {
                Value positionContext = st2.getObject();
                String contextURI = positionContext.stringValue();
                IRI ctx = f.createIRI(contextURI);
                RepositoryResult<Statement> statements3 = sparqlDAO.getStatements(ctx, null, null, graphIRI);
                LevelUnit concentration = new LevelUnit();
                Feature featureInContext = null;
                while (statements3.hasNext()) {
                    Statement st3 = statements3.next();
                    if (st3.getPredicate().equals(hasConcentration)) {
                        Value value = st3.getObject();   // this is the concentration URI
                        String conURI = value.stringValue();
                        IRI concentrationIRI = f.createIRI(conURI);
                        RepositoryResult<Statement> statements4 = sparqlDAO.getStatements(concentrationIRI, null, null, graphIRI);
                        while (statements4.hasNext()) {
                            Statement st4 = statements4.next();
                            if (st4.getPredicate().equals(hasConcentrationValue)) {
                                value = st4.getObject();
                                concentration.setConcentration(Double.parseDouble(value.stringValue()));
                            } else if (st4.getPredicate().equals(hasConcentrationUnit)) {
                                value = st4.getObject();
                                concentration.setLevelUnit(UnitOfLevels.lookUp(value.stringValue()));
                            }
                        }
                         
                    } else if (st3.getPredicate().equals(hasFeature)) {
                        Value val = st3.getObject();
                        featureInContext = featureRepository.getFeatureFromURI(val.stringValue(), user);
                    }  
                }
                if (concentration.getConcentration() != null && featureInContext != null) {
                    String featureId = featureInContext.getUri().substring(featureInContext.getUri().lastIndexOf("/")+1);
                    RatioConcentration rc = s.getRatioConcentration(featureId);
                    if (rc == null) {
                        rc = new RatioConcentration();
                    }
                    rc.setConcentration(concentration);
                    s.setRatioConcentration(featureInContext.getUri().substring(featureInContext.getUri().lastIndexOf("/")+1), rc);
                }
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
                    Statement st3 = statements3.next();
                    if (st3.getPredicate().equals(hasRatio)) {
                        Value value = st3.getObject();
                        if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
                            ratio = Double.parseDouble(value.stringValue());
                        }   
                    } else if (st3.getPredicate().equals(hasFeature)) {
                        Value val = st3.getObject();
                        featureInContext = featureRepository.getFeatureFromURI(val.stringValue(), user);
                    }  
                }
                if (ratio != null && featureInContext != null) {
                    String featureId = featureInContext.getUri().substring(featureInContext.getUri().lastIndexOf("/")+1);
                    RatioConcentration rc = s.getRatioConcentration(featureId);
                    if (rc == null) {
                        rc = new RatioConcentration();
                    }
                    rc.setRatio(ratio);
                    s.setRatioConcentration(featureId, rc);
                }
            } else if (st2.getPredicate().equals(hasSpotMetadata)) {
                Value uriValue = st2.getObject();
                s.setMetadata(metadataRepository.getSpotMetadataValueFromURI(uriValue.stringValue(), loadAll, user));
            }
		}
		/*
		// find its blockLayoutId
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(null, hasSpot, spot, graphIRI);
		while (statements.hasNext()) {
            Statement st = statements.next();
            String blockLayoutURI = st.getSubject().stringValue();
            s.setBlockLayoutUri(blockLayoutURI);
		}*/
		return s;
	}

	private String addPublicSpot(Spot s) throws SparqlException, SQLException {
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI graphIRI = f.createIRI(DEFAULT_GRAPH);
		String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
		String spotURI = generateUniqueURI(uriPrefixPublic + "S", allGraphs);
		IRI spot = f.createIRI(spotURI);
		Literal row = f.createLiteral(s.getRow());
		Literal column = f.createLiteral(s.getColumn());
		Literal group = s.getGroup() == null ? null : f.createLiteral(s.getGroup());
		Literal flag = s.getFlag() == null ? null : f.createLiteral(s.getFlag());
		IRI hasConcentration = f.createIRI(ontPrefix + "has_concentration");
		IRI hasConcentrationValue = f.createIRI(ontPrefix + "concentration_value");
		IRI hasConcentrationUnit = f.createIRI(ontPrefix + "has_concentration_unit");
		IRI hasGroup = f.createIRI(ontPrefix + "has_group");
		IRI hasRow = f.createIRI(ontPrefix + "has_row");
		IRI hasColumn = f.createIRI(ontPrefix + "has_column");
		IRI spotType = f.createIRI(ontPrefix + "Spot");
		IRI hasFeature = f.createIRI(ontPrefix + "has_feature");
		IRI hasSpotMetadata = f.createIRI(hasSpotMetadataPredicate);
		IRI hasFlag = f.createIRI(ontPrefix + "has_flag");
		
		List<Statement> statements = new ArrayList<Statement>();
		statements.add(f.createStatement(spot, RDF.TYPE, spotType, graphIRI));
		
		statements.add(f.createStatement(spot, hasRow, row));
		statements.add(f.createStatement(spot, hasColumn, column));
		if (group != null) statements.add(f.createStatement(spot, hasGroup, group, graphIRI));
		if (flag != null) statements.add(f.createStatement(spot, hasFlag, flag, graphIRI));
		
		if (s.getFeatures() != null) {
			for (Feature feat : s.getFeatures()) {
				IRI feature = f.createIRI(feat.getUri());
				statements.add(f.createStatement(spot, hasFeature, feature, graphIRI));
				
				Double ratio = s.getFeatureRatioMap().get(feat);
                if (ratio == null) {
                    RatioConcentration rc = s.getRatioConcentration(feat.getId());
                    if (rc != null) {
                        ratio = rc.getRatio();
                    }
                }
                if (ratio != null) {
                    // add ratio for the feature
                    Literal ratioL = f.createLiteral(ratio);
                    String positionContextURI = generateUniqueURI(uriPrefix + "PC", allGraphs);
                    IRI hasRatio = f.createIRI (hasRatioPredicate);
                    IRI hasRatioContext = f.createIRI(hasRatioContextPredicate);
                    IRI positionContext = f.createIRI(positionContextURI);
                    statements.add(f.createStatement(spot, hasRatioContext, positionContext, graphIRI));
                    statements.add(f.createStatement(positionContext, hasFeature, feature, graphIRI));
                    statements.add(f.createStatement(positionContext, hasRatio, ratioL, graphIRI));
                }
                
                // add the concentration
                LevelUnit concentration = s.getFeatureConcentrationMap().get(feat);
                if (concentration == null) {
                    RatioConcentration rc = s.getRatioConcentration(feat.getId());
                    if (rc != null) {
                        concentration = rc.getConcentration();
                    }
                }
                if (concentration != null) {
                    // add concentration for the feature
                    // check if it has already been created before
                    String concentrationURI = concentrationCache.get(concentration);
                    if (concentrationURI == null) {
                        concentrationURI = generateUniqueURI(uriPrefix + "C", allGraphs);
                        concentrationCache.put(concentration, concentrationURI);
                    }
                    IRI concentrationL = f.createIRI(concentrationURI);
                
                    Literal concentrationUnit = f.createLiteral(concentration.getLevelUnit().getLabel());
                    Literal concentrationValue = concentration.getConcentration() == null ? null : f.createLiteral(concentration.getConcentration());
                    if (concentrationValue != null) {
                        String concentrationContextURI = generateUniqueURI(uriPrefix + "CC", allGraphs);    
                        IRI hasConcentrationContext = f.createIRI(hasConcentrationContextPredicate);
                        IRI concentrationContext = f.createIRI(concentrationContextURI);
                        statements.add(f.createStatement(spot, hasConcentrationContext, concentrationContext, graphIRI));
                        statements.add(f.createStatement(concentrationContext, hasFeature, feature, graphIRI));
                        statements.add(f.createStatement(concentrationContext, hasConcentration, concentrationL, graphIRI));
                        statements.add(f.createStatement(concentrationL, hasConcentrationValue, concentrationValue, graphIRI));
                        statements.add(f.createStatement(concentrationL, hasConcentrationUnit, concentrationUnit, graphIRI));
                    }
                    
                }
				
				
			} 
		}
		
		if (s.getMetadata() != null) {
            if (s.getMetadata().getUri() != null) {
                statements.add(f.createStatement(spot, hasSpotMetadata, f.createIRI(s.getMetadata().getUri()), graphIRI));
            } else {
                String metadataURI = metadataRepository.addSpotMetadataValue(s.getMetadata(), null);
                statements.add(f.createStatement(spot, hasSpotMetadata, f.createIRI(metadataURI), graphIRI));
            }
        }
		
		sparqlDAO.addStatements(statements, graphIRI);
		return spotURI;
	}

	private String addPublicSlideLayout(SlideLayout s, String publicURI, String graph, String username) throws SparqlException, SQLException {
		boolean existing = publicURI != null;
		String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
		if (publicURI == null)
			publicURI = generateUniqueURI(uriPrefixPublic + "SL", allGraphs);
		
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
			
			if (s.getFile() != null)
	            saveFile (s.getFile(), publicURI, graph);
		} 
		
		// link local one to public uri
		List<Statement> statements2 = new ArrayList<Statement>();
        statements2.add(f.createStatement(local, hasPublicURI, slideLayout, userGraphIRI));
        statements2.add(f.createStatement(local, hasModifiedDate, dateCreated, userGraphIRI));
        statements2.add(f.createStatement(local, hasAddedToLibrary, dateAdded, userGraphIRI));
        statements2.add(f.createStatement(local, RDF.TYPE, slideLayoutType, userGraphIRI));
        sparqlDAO.addStatements(statements2, userGraphIRI);
        
        
        // add it to the slidelayoutrepository as well
        s.setUri(publicURI);
        s.setId(publicURI.substring(publicURI.lastIndexOf("/") + 1));
        s.setDateAddedToLibrary(date);
        s.setDateCreated(date);
        s.setDateModified(date);
        Creator creator = new Creator();
        creator.setName(username);
        s.setUser(creator);
        SlideLayoutEntity slideLayoutEntity = new SlideLayoutEntity();
        slideLayoutEntity.setUri(publicURI);
        try {
            slideLayoutEntity.setJsonValue(new ObjectMapper().writeValueAsString(s));
            slideLayoutRepository.save(slideLayoutEntity);
        } catch (JsonProcessingException e) {
            logger.error("Could not serialize Slide layout into JSON for caching", e);
        }
		return publicURI;
	}

    @Override
    public List<Spot> getSpotByFeatures(List<Feature> features, String slideLayoutURI,
            String groupId, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (user == null) {
            graph = DEFAULT_GRAPH;
        }
        else {
            graph = getGraphForUser(user);
        }
        
     //   if (slideLayoutURI != null) {
            // load the slide layout from cache and find the features there
    //        SlideLayoutEntity entity = slideLayoutRepository.findByUri(slideLayoutURI);
    //        if (entity != null) {
    //            List<Spot> spots = findSpotInEntity (entity, features, blockLayoutURI);
    //            if (spots != null &&  !spots.isEmpty())
     //               return spots;
      //      }
    //    } 
    /*else {
            // check all slide layouts
            List<SlideLayoutEntity> layouts = slideLayoutRepository.findAll();
            for (SlideLayoutEntity entity: layouts) {
                List<Spot> spots  = findSpotInEntity (entity, features, blockLayoutId);
                if (spots != null &&  !spots.isEmpty())
                    return spots;
            }
        }*/
        
        String fromString = "FROM <" + DEFAULT_GRAPH + ">\n";
     
        String whereClause = "WHERE {";
        String where = " { ";
        for (Feature f: features) {
            where += "?s gadr:has_feature <" + f.getUri() + "> . \n";
        }
        if (groupId != null && !groupId.isEmpty()) {
            where += " ?s gadr:has_group \'" + groupId + "'^^xsd:string . \n";
        }
        if (slideLayoutURI != null) {
            where += "<" + slideLayoutURI + "> gadr:has_block ?b . ?b template:has_block_layout ?bl . ?bl template:has_spot ?s .  \n";
        }
        where += "}";
        
        if (!graph.equals(DEFAULT_GRAPH)) {
            fromString += "FROM <" + graph + ">\n";
            where += "  UNION { ";
            int i=0;
            for (Feature f: features) {
                where += "?s gadr:has_feature ?f" + i + " . \n";
                where += "<" + f.getUri() + "> gadr:has_public_uri ?f"+ i  +". \n"; 
                i++;
            }
            if (groupId != null && !groupId.isEmpty()) {
                where += " ?s gadr:has_group \'" + groupId + "'^^xsd:string . \n";
            }
            
            if (slideLayoutURI != null) {
                if (slideLayoutURI.contains("public")) {
                    where += "<" + slideLayoutURI + "> gadr:has_block ?b . ?b template:has_block_layout ?bl . ?bl template:has_spot ?s .  \n";
                } else {
                    where += "<" + slideLayoutURI + "> gadr:has_public_uri ?ps . ?ps gadr:has_block ?b . ?b template:has_block_layout ?bl . ?bl template:has_spot ?s .  \n";
                }
            }
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
        if (results.isEmpty()) {
            //logger.warn("Query: " + queryBuf.toString() + " returned 0 results!");
            return null;
        }
        else {
            List<Spot> spots = new ArrayList<Spot>();
            for (SparqlEntity result: results) {
                String spotURI = result.getValue("s");
                if (spotURI.contains("public") ) {
                    spots.add(getSpotFromURI(spotURI, null));
                }
                else {
                    spots.add(getSpotFromURI(spotURI, user));
                }
            }
            return spots;
        }
    }
    
    private List<Spot> findSpotInEntity(SlideLayoutEntity entity, List<Feature> features, String blockLayoutURI) {
        try {
            SlideLayout s;
            if (slideLayoutCache.get(entity.getUri()) != null)
                s = slideLayoutCache.get(entity.getUri());
            else {
                s = new ObjectMapper().readValue(entity.getJsonValue(), SlideLayout.class);
                slideLayoutCache.put(entity.getUri(), s);
            }
            List<Spot> spots = new ArrayList<>();
            if (s.getBlocks() != null) {
                for (Block b: s.getBlocks()) {
                    if (blockLayoutURI != null && b.getBlockLayout().getUri().equals(blockLayoutURI)) {
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
                                spots.add(spot);
                            }
                        }
                        
                    } else if (blockLayoutURI == null) {
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
                                spots.add(spot);
                            }
                        }
                    }
                }
            }
            return spots;
        } catch (IOException e) {
            logger.error("Could not read slide layout from serialized value", e);
        }
        
        return null;
    }

    @Override
    public String getSpotByPosition (String slideLayoutURI, int row, int column, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (user == null) {
            graph = DEFAULT_GRAPH;
        }
        else {
            graph = getGraphForUser(user);
        }
        
        /*if (slideLayoutURI != null) {
            // load the slide layout from cache and find the spots there
            SlideLayoutEntity entity = slideLayoutRepository.findByUri(slideLayoutURI);
            if (entity != null) {
                Spot spot = findSpotInEntity (entity, row, column, blockLayoutURI);
                if (spot != null)
                    return spot.getUri();
            }
        } else {
            // check all slide layouts
            List<SlideLayoutEntity> layouts = slideLayoutRepository.findAll();
            for (SlideLayoutEntity entity: layouts) {
                Spot spot = findSpotInEntity (entity, row, column, blockLayoutId);
                if (spot != null)
                    return spot;
            }
        }*/
        
        String whereClause = "{";
        String fromString = "FROM <" + DEFAULT_GRAPH + ">\n";
        whereClause += "?s gadr:has_row \"" + row + "\"^^xsd:int . \n"; 
        whereClause += "?s gadr:has_column \"" + column + "\"^^xsd:int . \n";
        if (slideLayoutURI != null) {
            whereClause += "<" + slideLayoutURI + "> gadr:has_block ?b . ?b template:has_block_layout ?bl . ?bl template:has_spot ?s .  \n";
        }   
        whereClause += "}\n";
        if (!graph.equals(DEFAULT_GRAPH)) {
            fromString += "FROM <" + graph + ">\n";
            whereClause += " UNION {";
            whereClause += "?s gadr:has_row \"" + row + "\"^^xsd:int . \n"; 
            whereClause += "?s gadr:has_column \"" + column + "\"^^xsd:int . \n";
            if (slideLayoutURI != null) {
                whereClause += "<" + slideLayoutURI + "> gadr:has_public_uri ?ps . ?ps gadr:has_block ?b . ?b template:has_block_layout ?bl . ?bl template:has_spot ?s .  \n";
            } 
            whereClause += "}";
        }
        
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        queryBuf.append (fromString);
        queryBuf.append ("WHERE { " + whereClause + " }" + 
                "               LIMIT 10");
        
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        //logger.info("Query for spot position: " + queryBuf.toString());
        if (results.isEmpty())
            return null;
        else {
            String spotURI = results.get(0).getValue("s");
            return spotURI;
           /* if (spotURI.contains("public")) {
                return getSpotFromURI(spotURI, null);
            }
            return getSpotFromURI(spotURI, user);*/
        }
    }

    private Spot findSpotInEntity(SlideLayoutEntity entity, int row, int column, String blockLayoutURI) {
        try {
            SlideLayout s;
            if (slideLayoutCache.get(entity.getUri()) != null)
                s = slideLayoutCache.get(entity.getUri());
            else {
                s = new ObjectMapper().readValue(entity.getJsonValue(), SlideLayout.class);
                slideLayoutCache.put(entity.getUri(), s);
            }
            for (Block b: s.getBlocks()) {
                if (blockLayoutURI != null && b.getBlockLayout().getUri().equals(blockLayoutURI)) {
                    // check only in this block
                    for (Spot spot: b.getBlockLayout().getSpots()) {
                        if (spot.getRow() == row && spot.getColumn() == column)
                            return spot;
                    }
                    
                } else if (blockLayoutURI == null) {
                    // check this block
                    for (Spot spot: b.getBlockLayout().getSpots()) {
                        if (spot.getRow() == row && spot.getColumn() == column)
                            return spot;
                    }
                }
                
            }
        } catch (IOException e) {
            logger.error("Could not read slide layout from serialized value", e);
        }
        
        return null;
    }
}
