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
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.Block;
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.Spot;
import org.grits.toolbox.glycanarray.library.om.layout.LevelUnit;
import org.grits.toolbox.glycanarray.om.model.UnitOfLevels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(value="sesameTransactionManager") 
public class LayoutRepositoryImpl extends GlygenArrayRepositoryImpl implements LayoutRepository {
	
	@Autowired
	GlycanRepository glycanRepository;
	
	@Autowired
	LinkerRepository linkerRepository;
	
	Map<String, BlockLayout> blockLayoutCache = new HashMap<String, BlockLayout>();
	Map<Long, String> linkerCache = new HashMap<Long, String>();
	Map<String, String> glycanCache = new HashMap<String, String>();
	Map<LevelUnit, String> concentrationCache = new HashMap<LevelUnit, String>();
	
	private String addBlock(Block b, UserEntity user, String graph) throws SparqlException, SQLException {

		String blockURI = generateUniqueURI(uriPrefix + "B", graph);
		ValueFactory f = sparqlDAO.getValueFactory();
		
		IRI graphIRI = f.createIRI(graph);
		IRI block = f.createIRI(blockURI);
		IRI hasBlockLayout = f.createIRI(ontPrefix + "has_block_layout");
		IRI blockType = f.createIRI(ontPrefix + "Block");
		IRI hasRow = f.createIRI(ontPrefix + "has_row");
		IRI hasColumn = f.createIRI(ontPrefix + "has_column");
//		IRI spotType = f.createIRI(ontPrefix + "Spot");
		IRI hasSpot = f.createIRI(ontPrefix + "has_spot");
//		IRI hasFeature = f.createIRI(ontPrefix + "has_feature");
//		IRI hasConcentration = f.createIRI(ontPrefix + "has_concentration");
//		IRI hasConcentrationValue = f.createIRI(ontPrefix + "concentration_value");
//		IRI hasConcentrationUnit = f.createIRI(ontPrefix + "has_concentration_unit");
//		IRI hasGroup = f.createIRI(ontPrefix + "has_group");
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
			IRI blockLayoutIRI = f.createIRI(layoutFromRepository.getUri());
			// create Block and copy spots from Layout
			List<Statement> statements = new ArrayList<Statement>();
			statements.add(f.createStatement(block, RDF.TYPE, blockType));
			statements.add(f.createStatement(block, hasBlockLayout, blockLayoutIRI));
			statements.add(f.createStatement(block, hasRow, row));
			statements.add(f.createStatement(block, hasColumn, column));
			
			sparqlDAO.addStatements(statements, graphIRI);
			// copy spots from layout
			for (Spot s : layoutFromRepository.getSpots()) {
				statements = new ArrayList<Statement>();
				/*String spotURI = generateUniqueURI(uriPrefix + "S", graph);
				IRI spot = f.createIRI(spotURI);
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
						statements.add(f.createStatement(concentration, hasConcentrationValue, concentrationValue));
						statements.add(f.createStatement(concentration, hasConcentrationUnit, concentrationUnit));
					}
					statements.add(f.createStatement(spot, hasConcentration, concentration));
				}
				
				row = f.createLiteral(s.getRow());
				column = f.createLiteral(s.getColumn());
				Literal group = s.getGroup() == null ? null : f.createLiteral(s.getGroup());
				statements.add(f.createStatement(spot, RDF.TYPE, spotType));
				statements.add(f.createStatement(block, hasSpot, spot));
				statements.add(f.createStatement(spot, hasRow, row));
				statements.add(f.createStatement(spot, hasColumn, column));
				if (group != null) statements.add(f.createStatement(spot, hasGroup, group));
				
				List<Feature> features = s.getFeatures();
				for (Feature feat : features) {
					IRI featureIRI = f.createIRI(feat.getUri());
					statements.add(f.createStatement(spot, hasFeature, featureIRI));
				}*/
				IRI spot = f.createIRI(s.getUri());
				statements.add(f.createStatement(block, hasSpot, spot));
				sparqlDAO.addStatements(statements, graphIRI);
			}
			
		} else 
			throw new SparqlException ("Block layout cannot be found in repository");
		
		return blockURI;
	}
	
	@Override
	public String addBlockLayout(BlockLayout b, UserEntity user) throws SparqlException {
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
		IRI hasSpot = f.createIRI(ontPrefix + "has_spot");
		IRI hasFeature = f.createIRI(ontPrefix + "has_feature");
		IRI hasConcentration = f.createIRI(ontPrefix + "has_concentration");
		IRI hasConcentrationValue = f.createIRI(ontPrefix + "concentration_value");
		IRI hasConcentrationUnit = f.createIRI(ontPrefix + "has_concentration_unit");
		IRI hasGroup = f.createIRI(ontPrefix + "has_group");
		IRI hasRow = f.createIRI(ontPrefix + "has_row");
		IRI hasColumn = f.createIRI(ontPrefix + "has_column");
		IRI hasWidth = f.createIRI(ontPrefix + "has_width");
		IRI hasHeight = f.createIRI(ontPrefix + "has_height");
		IRI hasGlycan = f.createIRI(ontPrefix + "has_molecule");
		IRI hasLinker = f.createIRI(ontPrefix + "has_linker");
		IRI hasRatio = f.createIRI(ontPrefix + "has_ratio");
		IRI featureType = f.createIRI(ontPrefix + "Feature");
		IRI spotType = f.createIRI(ontPrefix + "Spot");
		
		IRI blockLayoutType = f.createIRI(ontPrefix + "BlockLayout");
		IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		Date date = new Date();
		Literal dateCreated = f.createLiteral(date);
		
		Literal blockLayoutLabel = f.createLiteral(b.getName().trim());
		Literal blockLayoutComment = b.getDescription() == null ? null: f.createLiteral(b.getDescription().trim());
		Literal blockLayoutWidth = b.getWidth() == null ? null : f.createLiteral(b.getWidth());
		Literal blockLayoutHeight = b.getHeight() == null ? null : f.createLiteral(b.getHeight());
		
		List<Statement> statements = new ArrayList<Statement>();
		statements.add(f.createStatement(blockLayout, RDF.TYPE, blockLayoutType));
		statements.add(f.createStatement(blockLayout, RDFS.LABEL, blockLayoutLabel));
		if (blockLayoutComment != null) statements.add(f.createStatement(blockLayout, RDFS.COMMENT, blockLayoutComment));
		statements.add(f.createStatement(blockLayout, hasCreatedDate, dateCreated));
		statements.add(f.createStatement(blockLayout, hasModifiedDate, dateCreated));
		if (blockLayoutWidth != null) statements.add(f.createStatement(blockLayout, hasWidth, blockLayoutWidth));
		if (blockLayoutHeight != null) statements.add(f.createStatement(blockLayout, hasHeight, blockLayoutHeight));
		
		sparqlDAO.addStatements(statements, graphIRI);
		
		List<Feature> processed = new ArrayList<Feature>();
		for (Spot s : b.getSpots()) {
			if (s == null)
				continue;
			statements = new ArrayList<Statement>();
			String spotURI = generateUniqueURI(uriPrefix + "S", graph);
			IRI spot = f.createIRI(spotURI);
			Literal row = f.createLiteral(s.getRow());
			Literal column = f.createLiteral(s.getColumn());
			Literal group = s.getGroup() == null ? null : f.createLiteral(s.getGroup());
			statements.add(f.createStatement(spot, RDF.TYPE, spotType));
			statements.add(f.createStatement(blockLayout, hasSpot, spot));
			
			statements.add(f.createStatement(spot, hasRow, row));
			statements.add(f.createStatement(spot, hasColumn, column));
			if (group != null) statements.add(f.createStatement(spot, hasGroup, group));
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
					statements.add(f.createStatement(concentration, hasConcentrationValue, concentrationValue));
					statements.add(f.createStatement(concentration, hasConcentrationUnit, concentrationUnit));
				}
				statements.add(f.createStatement(spot, hasConcentration, concentration));
			}
			
			sparqlDAO.addStatements(statements, graphIRI);
			if (s.getFeatures() != null) {
				List<Feature> features = s.getFeatures();
				for (Feature feat : features) {
					if (!processed.contains(feat)) {
						statements = new ArrayList<Statement>();
						String featureURI = generateUniqueURI(uriPrefix + "F", graph);
						IRI feature = f.createIRI(featureURI);
						String glycanURI = null;
						//TODO
					/*	if (feat.getGlycan() != null) {
							glycanURI = feat.getGlycan().getUri();
							if (glycanURI == null) {
								if (feat.getGlycan().getSequence() != null) {
									String seq = feat.getGlycan().getSequence().trim();
									glycanURI = glycanCache.get(seq);
									if (glycanURI == null) {
										glycanURI = getGlycanBySequence(seq);
										glycanCache.put(seq, glycanURI);
									}
								}
							}
						}
						String linkerURI = null;
						if (feat.getLinker() != null) {
							linkerURI = feat.getLinker().getUri();
							if (linkerURI == null) {
								if (feat.getLinker().getPubChemId() != null) {
									linkerURI = linkerCache.get(feat.getLinker().getPubChemId());
									if (linkerURI == null) {
										linkerURI = getLinkerByPubChemId(feat.getLinker().getPubChemId());
										linkerCache.put(feat.getLinker().getPubChemId(), linkerURI);
									}
								}
							}
						}
						
						Literal ratio = feat.getRatio() != null ? f.createLiteral(feat.getRatio()) : f.createLiteral(1.0) ;
						feat.setUri(featureURI);
						
						statements.add(f.createStatement(feature, RDF.TYPE, featureType));
						statements.add(f.createStatement(spot, hasFeature, feature));
						if (glycanURI != null) {
							IRI glycan = f.createIRI(glycanURI);
							statements.add(f.createStatement(feature, hasGlycan, glycan));	
						}
						if (linkerURI != null) {
							IRI linker = f.createIRI(linkerURI);
							statements.add(f.createStatement(feature, hasLinker, linker));
						}
						if (ratio != null) statements.add(f.createStatement(feature, hasRatio, ratio));
						processed.add(feat);   // processed
						sparqlDAO.addStatements(statements, graphIRI);*/
					} else {
						statements = new ArrayList<Statement>();
						Feature existing = processed.get(processed.indexOf(feat));  // existing will have the uri
						IRI feature = f.createIRI(existing.getUri());
						statements.add(f.createStatement(spot, hasFeature, feature));
						sparqlDAO.addStatements(statements, graphIRI);
					}
				}
			}
		}
		
		return blockLayoutURI;
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
		
		String slideLayoutURI = generateUniqueURI(uriPrefix + "SL", graph);
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI slideLayout = f.createIRI(slideLayoutURI);
		IRI graphIRI = f.createIRI(graph);
		IRI slideLayoutType = f.createIRI(ontPrefix + "SlideLayout");
		IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		IRI hasWidth = f.createIRI(ontPrefix + "has_width");
		IRI hasHeight = f.createIRI(ontPrefix + "has_height");
		IRI hasBlock = f.createIRI(ontPrefix + "has_block");
		Date date = new Date();
		Literal dateCreated = f.createLiteral(date);
		
		Literal slideLayoutLabel = f.createLiteral(s.getName().trim());
		Literal slideLayoutComment = s.getDescription() == null ? null: f.createLiteral(s.getDescription().trim());
		Literal slideLayoutWidth = s.getWidth() == null ? null : f.createLiteral(s.getWidth());
		Literal slideLayoutHeight = s.getHeight() == null ? null : f.createLiteral(s.getHeight());
		
		List<Statement> statements = new ArrayList<Statement>();
		statements.add(f.createStatement(slideLayout, RDF.TYPE, slideLayoutType));
		statements.add(f.createStatement(slideLayout, RDFS.LABEL, slideLayoutLabel));
		if (slideLayoutComment != null) statements.add(f.createStatement(slideLayout, RDFS.COMMENT, slideLayoutComment));
		statements.add(f.createStatement(slideLayout, hasCreatedDate, dateCreated));
		statements.add(f.createStatement(slideLayout, hasModifiedDate, dateCreated));
		if (slideLayoutWidth != null) statements.add(f.createStatement(slideLayout, hasWidth, slideLayoutWidth));
		if (slideLayoutHeight != null) statements.add(f.createStatement(slideLayout, hasHeight, slideLayoutHeight));
		
		if (s.getBlocks() != null) {
			for (Block b: s.getBlocks()) {
				if (b == null)
					continue;
				String blockURI = addBlock (b, user, graph);
				if (blockURI == null)
					continue;
				IRI block = f.createIRI(blockURI);
				statements.add(f.createStatement(slideLayout, hasBlock, block));
			}
		}
		
		sparqlDAO.addStatements(statements, graphIRI);
		return slideLayoutURI;
	}
	
	@Override
	public void deleteBlockLayout(String blockLayoutId, UserEntity user) throws SparqlException, SQLException {
		String graph;
		
		graph = getGraphForUser(user);
		if (graph != null) {
			// check to see if the given blockLayoutId is in this graph
			BlockLayout existing = getBlockLayoutFromURI (uriPrefix + blockLayoutId, graph);
			if (existing != null) {
				deleteBlockLayoutByURI (uriPrefix + blockLayoutId, graph);
				return;
			}
		}
	}

	private void deleteBlockLayoutByURI(String uri, String graph) throws SparqlException {
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI blockLayout = f.createIRI(uri);
		IRI graphIRI = f.createIRI(graph);
		IRI hasSpot = f.createIRI(ontPrefix + "has_spot");
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
	
	
	
	
	@Override
	public void deleteSlideLayout(String slideLayoutId, UserEntity user) throws SparqlException, SQLException {
		String graph;
		
		graph = getGraphForUser(user);
		if (graph != null) {
			// check to see if the given slideLayoutId is in this graph
			SlideLayout existing = getSlideLayoutFromURI (uriPrefix + slideLayoutId, false, graph);
			if (existing != null) {
				deleteSlideLayoutByURI (uriPrefix + slideLayoutId, graph);
				return;
			}
		}
	}
	
	private void deleteSlideLayoutByURI(String uri, String graph) throws SparqlException {
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI slideLayout = f.createIRI(uri);
		IRI graphIRI = f.createIRI(graph);
		IRI hasBlock = f.createIRI(ontPrefix + "has_block");
		IRI hasSpot = f.createIRI(ontPrefix + "has_spot");
		IRI hasConcentration = f.createIRI(ontPrefix + "has_concentration");
		
		RepositoryResult<Statement> statements3 = sparqlDAO.getStatements(slideLayout, hasBlock, null, graphIRI);
		while (statements3.hasNext()) {
			Statement st = statements3.next();
			Value v = st.getObject();
			String blockURI = v.stringValue();
			IRI block = f.createIRI(blockURI);
			RepositoryResult<Statement> statements4 = sparqlDAO.getStatements(block, hasSpot, null, graphIRI);
			while (statements4.hasNext()) {
				st = statements4.next();
				if (st.getPredicate().equals(hasConcentration)) {
					v = st.getObject();
					String conURI = v.stringValue();
					IRI concentration = f.createIRI(conURI);
					RepositoryResult<Statement> statements5 = sparqlDAO.getStatements(concentration, null, null, graphIRI);
					sparqlDAO.removeStatements(Iterations.asList(statements5), graphIRI);
				}
			}
			statements4 = sparqlDAO.getStatements(block, null, null, graphIRI);
			sparqlDAO.removeStatements(Iterations.asList(statements4), graphIRI);
		}
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(slideLayout, null, null, graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
	}
	
	private Block getBlock (String blockURI, String graph) throws SparqlException {
		
		Block blockObject = new Block();
		ValueFactory f = sparqlDAO.getValueFactory();
		
		IRI block = f.createIRI(blockURI);
		IRI graphIRI = f.createIRI(graph);
		IRI hasBlockLayout = f.createIRI(ontPrefix + "has_block_layout");
		IRI hasRow = f.createIRI(ontPrefix + "has_row");
		IRI hasColumn = f.createIRI(ontPrefix + "has_column");
		IRI hasSpot = f.createIRI(ontPrefix + "has_spot");
		IRI hasFeature = f.createIRI(ontPrefix + "has_feature");
		IRI hasConcentration = f.createIRI(ontPrefix + "has_concentration");
		IRI hasConcentrationValue = f.createIRI(ontPrefix + "concentration_value");
		IRI hasConcentrationUnit = f.createIRI(ontPrefix + "has_concentration_unit");
		IRI hasGroup = f.createIRI(ontPrefix + "has_group");
		IRI hasGlycan = f.createIRI(ontPrefix + "has_molecule");
		IRI hasLinker = f.createIRI(ontPrefix + "has_linker");
		IRI hasRatio = f.createIRI(ontPrefix + "has_ratio");
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(block, null, null, graphIRI);
		if (statements.hasNext()) {
			blockObject = new Block();
			blockObject.setUri(blockURI);
		}
		List<Spot> spots = new ArrayList<Spot>();
		while (statements.hasNext()) {
			Statement st = statements.next();
			if (st.getPredicate().equals(hasBlockLayout)) {
				Value v = st.getObject();
				String blockLayoutURI = v.stringValue();
				BlockLayout blockLayout = getBlockLayoutFromURI(blockLayoutURI, false, graph);    // no need to load all, just the name 
				blockObject.setBlockLayout(blockLayout);
			} else if (st.getPredicate().equals(hasRow)) {
				Value v = st.getObject();
				blockObject.setRow(Integer.parseInt(v.stringValue()));
			} else if (st.getPredicate().equals(hasColumn)) {
				Value v = st.getObject();
				blockObject.setColumn(Integer.parseInt(v.stringValue()));
			} else if (st.getPredicate().equals(hasSpot)) {
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
						Feature feat = new Feature();
						v = st2.getObject();
						String featureURI = v.stringValue();
						IRI feature = f.createIRI(featureURI);
						feat.setUri(featureURI);
						RepositoryResult<Statement> statements3 = sparqlDAO.getStatements(feature, null, null, graphIRI);
						while (statements3.hasNext()) {
							Statement st3 = statements3.next();
							if (st3.getPredicate().equals(hasGlycan)) {
								v = st3.getObject();
								String glycanURI = v.stringValue();
								//TODO fix it
								//feat.setGlycan(glycanRepository.getGlycanFromURI(glycanURI, graph));
							} else if (st3.getPredicate().equals(hasLinker)) {
								v = st3.getObject();
								String linkerURI = v.stringValue();
								feat.setLinker(linkerRepository.getLinkerFromURI(linkerURI, graph));
							} else if (st3.getPredicate().equals(hasRatio)) {
								v = st3.getObject();
								feat.setRatio(Double.parseDouble(v.stringValue()));
							}
						}
						features.add(feat);
					}
				}
				spots.add(s);
			}
		}
		blockObject.setSpots (spots);
		
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
		String graph = getGraphForUser(user);
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?o \n");
		queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n");
		queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/data#BlockLayout>. \n");
		queryBuf.append ( "<" +  uriPrefix + blockLayoutId + "> rdfs:label ?o . }\n");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.isEmpty())
			return null;
		else {
			return getBlockLayoutFromURI(uriPrefix + blockLayoutId, loadAll, graph);
		}
	}
	
	@Override
	public BlockLayout getBlockLayoutByName (String name, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?s \n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n");
		queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/data#BlockLayout>. \n");
		queryBuf.append ( " ?s rdfs:label \"" + name + "\"^^xsd:string . \n"
				+ "}\n");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.isEmpty())
			return null;
		else {
			String blockLayoutURI = results.get(0).getValue("s");
			return getBlockLayoutFromURI(blockLayoutURI, false, graph);
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
		
		List<BlockLayout> layouts = new ArrayList<BlockLayout>();
		
		String sortPredicate = getSortPredicateForLayout (field);
		// get all blockLayoutURIs from user's private graph
		String graph = getGraphForUser(user);
		if (graph != null) {
			String sortLine = "";
			if (sortPredicate != null)
				sortLine = "?s " + sortPredicate + " ?sortBy .\n";	
			String orderByLine = " ORDER BY " + (order == 0 ? "DESC" : "ASC") + (sortPredicate == null ? "(?s)": "(?sortBy)");	
			StringBuffer queryBuf = new StringBuffer();
			queryBuf.append (prefix + "\n");
			queryBuf.append ("SELECT DISTINCT ?s \n");
			queryBuf.append ("FROM <" + graph + ">\n");
			queryBuf.append ("WHERE {\n");
			queryBuf.append (sortLine + 
					" ?s rdf:type  <http://purl.org/gadr/data#BlockLayout>. \n" +
				    "}\n" +
					 orderByLine + 
					((limit == -1) ? " " : " LIMIT " + limit) +
					" OFFSET " + offset);
			
			List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
			
			for (SparqlEntity sparqlEntity : results) {
				String blockLayoutURI = sparqlEntity.getValue("s");
				BlockLayout layout = getBlockLayoutFromURI(blockLayoutURI, loadAll, graph);
				layouts.add(layout);
			}
		}
		
		return layouts;
	}
	
	@Override
	public int getBlockLayoutCountByUser(UserEntity user) throws SQLException, SparqlException {
		String graph = getGraphForUser(user);
		
		int total = 0;
		if (graph != null) {
			StringBuffer queryBuf = new StringBuffer();
			queryBuf.append (prefix + "\n");
			queryBuf.append ("SELECT COUNT(DISTINCT ?s) as ?count \n");
			queryBuf.append ("FROM <" + graph + ">\n");
			queryBuf.append ("WHERE {\n");
			queryBuf.append (" ?s rdf:type  <http://purl.org/gadr/data#BlockLayout> . }");
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
	
	private BlockLayout getBlockLayoutFromURI(String blockLayoutURI, Boolean loadAll, String graph) throws SparqlException {
		BlockLayout blockLayoutObject = null;
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI blockLayout = f.createIRI(blockLayoutURI);
		IRI graphIRI = f.createIRI(graph);
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
		IRI hasWidth = f.createIRI(ontPrefix + "has_width");
		IRI hasHeight = f.createIRI(ontPrefix + "has_height");
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
							Feature feat = new Feature();
							v = st2.getObject();
							String featureURI = v.stringValue();
							IRI feature = f.createIRI(featureURI);
							feat.setUri(featureURI);
							RepositoryResult<Statement> statements3 = sparqlDAO.getStatements(feature, null, null, graphIRI);
							while (statements3.hasNext()) {
								Statement st3 = statements3.next();
								if (st3.getPredicate().equals(hasGlycan)) {
									v = st3.getObject();
									String glycanURI = v.stringValue();
									//TODO fix it
									//feat.setGlycan(glycanRepository.getGlycanFromURI(glycanURI, graph));
								} else if (st3.getPredicate().equals(hasLinker)) {
									v = st3.getObject();
									String linkerURI = v.stringValue();
									feat.setLinker(linkerRepository.getLinkerFromURI(linkerURI, graph));
								} else if (st3.getPredicate().equals(hasRatio)) {
									v = st3.getObject();
									feat.setRatio(Double.parseDouble(v.stringValue()));
								}
							}
							features.add(feat);
						}
					}
					spots.add(s);
				}
			}
			
			blockLayoutObject.setSpots(spots);
		}
		
		return blockLayoutObject;
	}

	private BlockLayout getBlockLayoutFromURI(String blockLayoutURI, String graph) throws SparqlException {
		return getBlockLayoutFromURI(blockLayoutURI, true, graph);
	}
	
	@Override
	public SlideLayout getSlideLayoutById(String slideLayoutId, UserEntity user) throws SparqlException, SQLException {
		return getSlideLayoutById(slideLayoutId, user, true);
	}

	@Override
	public SlideLayout getSlideLayoutById(String slideLayoutId, UserEntity user, boolean loadAll)
			throws SparqlException, SQLException {
		// make sure the slidelayout belongs to this user
		String graph = getGraphForUser(user);
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?o \n");
		queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n");
		queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/data#SlideLayout>. \n");
		queryBuf.append ( "<" +  uriPrefix + slideLayoutId + "> rdfs:label ?o . }\n");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.isEmpty())
			return null;
		else {
			return getSlideLayoutFromURI(uriPrefix + slideLayoutId, loadAll, graph);
		}
	}

	@Override
	public SlideLayout getSlideLayoutByName(String name, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?s \n");
		queryBuf.append ("FROM <" + graph + ">\n");
		queryBuf.append ("WHERE {\n");
		queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/data#SlideLayout>. \n");
		queryBuf.append ( " ?s rdfs:label \"" + name + "\"^^xsd:string . \n"
				+ "}\n");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.isEmpty())
			return null;
		else {
			String slideLayoutURI = results.get(0).getValue("s");
			return getSlideLayoutFromURI(slideLayoutURI, false, graph);
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
		List<SlideLayout> layouts = new ArrayList<SlideLayout>();
		
		String sortPredicate = getSortPredicateForLayout (field);
		// get all blockLayoutURIs from user's private graph
		String graph = getGraphForUser(user);
		if (graph != null) {
			String sortLine = "";
			if (sortPredicate != null)
				sortLine = "?s " + sortPredicate + " ?sortBy .\n";	
			String orderByLine = " ORDER BY " + (order == 0 ? "DESC" : "ASC") + (sortPredicate == null ? "(?s)": "(?sortBy)");	
			StringBuffer queryBuf = new StringBuffer();
			queryBuf.append (prefix + "\n");
			queryBuf.append ("SELECT DISTINCT ?s \n");
			queryBuf.append ("FROM <" + graph + ">\n");
			queryBuf.append ("WHERE {\n");
			queryBuf.append (sortLine + 
					" ?s rdf:type  <http://purl.org/gadr/data#SlideLayout>. \n" +
				    "}\n" +
					 orderByLine + 
					((limit == -1) ? " " : " LIMIT " + limit) +
					" OFFSET " + offset);
			
			List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
			
			for (SparqlEntity sparqlEntity : results) {
				String slideLayoutURI = sparqlEntity.getValue("s");
				SlideLayout layout = getSlideLayoutFromURI(slideLayoutURI, loadAll, graph);
				layouts.add(layout);
			}
		}
		
		return layouts;
	}

	@Override
	public int getSlideLayoutCountByUser(UserEntity user) throws SQLException, SparqlException {
		String graph = getGraphForUser(user);
		
		int total = 0;
		if (graph != null) {
			StringBuffer queryBuf = new StringBuffer();
			queryBuf.append (prefix + "\n");
			queryBuf.append ("SELECT COUNT(DISTINCT ?s) as ?count \n");
			queryBuf.append ("FROM <" + graph + ">\n");
			queryBuf.append ("WHERE {\n");
			queryBuf.append (" ?s rdf:type  <http://purl.org/gadr/data#SlideLayout> . }");
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

	private SlideLayout getSlideLayoutFromURI(String slideLayoutURI, Boolean loadAll, String graph) throws SparqlException, SQLException {
		SlideLayout slideLayoutObject = null;
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI slideLayout = f.createIRI(slideLayoutURI);
		IRI graphIRI = f.createIRI(graph);
		IRI hasBlock = f.createIRI(ontPrefix + "has_block");
		IRI hasWidth = f.createIRI(ontPrefix + "has_width");
		IRI hasHeight = f.createIRI(ontPrefix + "has_height");
		IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(slideLayout, null, null, graphIRI);
		if (statements.hasNext()) {
			slideLayoutObject = new SlideLayout();
			slideLayoutObject.setUri(slideLayoutURI);
		}
		if (slideLayoutObject != null) {
			List<Block> blocks = new ArrayList<>();
			while (statements.hasNext()) {
				Statement st = statements.next();
				if (st.getPredicate().equals(RDFS.LABEL)) {
					Value v = st.getObject();
					slideLayoutObject.setName(v.stringValue());
				} else if (st.getPredicate().equals(RDFS.COMMENT)) {
					Value v = st.getObject();
					slideLayoutObject.setDescription(v.stringValue());
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
				} else if ((loadAll == null || loadAll) && st.getPredicate().equals(hasBlock)) {
					Value v = st.getObject();
					String blockURI = v.stringValue();
					Block block = getBlock (blockURI, graph);
					blocks.add(block);
				}
			}
			
			slideLayoutObject.setBlocks(blocks);
		}
		return slideLayoutObject;
	}
	
	@Override
	public void updateSlideLayout(SlideLayout layout, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		SlideLayout existing = getSlideLayoutFromURI(layout.getUri(), false, graph);
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
		Literal date = f.createLiteral(new Date());
		
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(slideLayout, RDFS.LABEL, null, graphIRI)), graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(slideLayout, RDFS.COMMENT, null, graphIRI)), graphIRI);
		sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(slideLayout, hasModifiedDate, null, graphIRI)), graphIRI);
		
		List<Statement> statements = new ArrayList<Statement>();
		
		statements.add(f.createStatement(slideLayout, RDFS.LABEL, label, graphIRI));
		statements.add(f.createStatement(slideLayout, RDFS.COMMENT, comment, graphIRI));
		statements.add(f.createStatement(slideLayout, hasModifiedDate, date, graphIRI));
		
		sparqlDAO.addStatements(statements, graphIRI);
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
	
	@Override
	public void updateBlockLayout(BlockLayout layout, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		BlockLayout existing = getBlockLayoutFromURI(layout.getUri(), false, graph);
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
	
	private SlideLayout getSlideLayoutFromURI(String slideLayoutURI, String graph) throws SparqlException, SQLException {
		return getSlideLayoutFromURI(slideLayoutURI, true, graph);
	}

}
