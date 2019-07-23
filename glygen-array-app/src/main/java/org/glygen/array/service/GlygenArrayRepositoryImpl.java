package org.glygen.array.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
import org.glygen.array.persistence.PrivateGraphEntity;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.PrivateGraphRepository;
import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.Block;
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.Spot;
import org.grits.toolbox.glycanarray.library.om.layout.LevelUnit;
import org.grits.toolbox.glycanarray.om.model.UnitOfLevels;
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
	UserRepository userRepository;
	
	Random random = new Random();
	
	String prefix="PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>"
			+ "\nPREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
			+ "\nPREFIX gadr: <http://purl.org/gadr/data#>"
			+ "\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>";
	
	Map<String, BlockLayout> blockLayoutCache = new HashMap<String, BlockLayout>();
	Map<Long, String> linkerCache = new HashMap<Long, String>();
	Map<String, String> glycanCache = new HashMap<String, String>();
	Map<LevelUnit, String> concentrationCache = new HashMap<LevelUnit, String>();
	
	@Override
	public void addAliasForGlycan(String glycanId, String alias, UserEntity user) throws SparqlException, SQLException {
		if (alias == null || alias.isEmpty())
			return;
		
		String graph;
		
		graph = getGraphForUser(user);
		if (graph != null) {
			// check to see if the given glycanId is in this graph
			String glycanURI = uriPrefix + glycanId;
			Glycan existing = getGlycanFromURI (glycanURI, graph);
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
			List<Feature> features = s.getFeatures();
			for (Feature feat : features) {
				if (!processed.contains(feat)) {
					statements = new ArrayList<Statement>();
					String featureURI = generateUniqueURI(uriPrefix + "F", graph);
					IRI feature = f.createIRI(featureURI);
					String glycanURI = null;
					if (feat.getGlycan() != null) {
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
					sparqlDAO.addStatements(statements, graphIRI);
				} else {
					statements = new ArrayList<Statement>();
					Feature existing = processed.get(processed.indexOf(feat));  // existing will have the uri
					IRI feature = f.createIRI(existing.getUri());
					statements.add(f.createStatement(spot, hasFeature, feature));
					sparqlDAO.addStatements(statements, graphIRI);
				}
			}
		}
		
		return blockLayoutURI;
	}

	@Override
	public String addGlycan(Glycan g, UserEntity user) throws SparqlException {
		String graph = null;
		if (user == null) {
			// cannot add 
			throw new SparqlException ("The user must be provided to put data into private repository");
		}
		
		if (g == null || g.getSequence() == null || g.getSequence().isEmpty() || g.getSequenceType() == null)
			// cannot add 
			throw new SparqlException ("Not enough information is provided to register a glycan");
		
		try {
			// check if there is already a private graph for user
			graph = getGraphForUser(user);
		} catch (SQLException e) {
			throw new SparqlException ("Cannot add the private graph for the user: " + user.getUsername(), e);
		}
		
		ValueFactory f = sparqlDAO.getValueFactory();
		String glycanURI;
		
		// check if the glycan already exists in "default-graph", then we only need to add it to the user's graph
		String existing = getGlycanBySequence(g.getSequence());
		if (existing == null) {
			String seqURI = generateUniqueURI(uriPrefix + "Seq");
			glycanURI = generateUniqueURI(uriPrefix) + "GAR";
			
			IRI sequence = f.createIRI(seqURI);
			IRI glycan = f.createIRI(glycanURI);
			IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
			String id = glycanURI.substring(glycanURI.lastIndexOf("/")+1);  //TODO remove this later
			Literal glytoucanId = g.getGlytoucanId() == null ? f.createLiteral(id) : f.createLiteral(g.getGlytoucanId());
			Literal sequenceValue = f.createLiteral(g.getSequence());
			Literal format = f.createLiteral(g.getSequenceType());
			Literal date = f.createLiteral(new Date());
			Literal mass = g.getMass() == null ? null : f.createLiteral(g.getMass());
			IRI hasSequence = f.createIRI(ontPrefix + "has_sequence");
			IRI hasGlytoucanId = f.createIRI(ontPrefix + "has_glytoucan_id");
			IRI hasSequenceValue = f.createIRI(ontPrefix + "has_sequence_value");
			IRI hasSequenceFormat = f.createIRI(ontPrefix + "has_sequence_format");
			IRI sequenceType = f.createIRI(ontPrefix + "Sequence");
			IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
			IRI glycanType = f.createIRI(ontPrefix + "Glycan");
			IRI hasMass = f.createIRI(ontPrefix + "has_mass");
			
			List<Statement> statements = new ArrayList<Statement>();
			statements.add(f.createStatement(sequence, RDF.TYPE, sequenceType, defaultGraphIRI));
			statements.add(f.createStatement(glycan, RDF.TYPE, glycanType, defaultGraphIRI));
			statements.add(f.createStatement(glycan, hasSequence, sequence, defaultGraphIRI));
			statements.add(f.createStatement(glycan, hasCreatedDate, date, defaultGraphIRI));
			statements.add(f.createStatement(glycan, hasGlytoucanId, glytoucanId, defaultGraphIRI));
			statements.add(f.createStatement(sequence, hasSequenceValue, sequenceValue, defaultGraphIRI));
			statements.add(f.createStatement(sequence, hasSequenceFormat, format, defaultGraphIRI));
			if (mass != null) statements.add(f.createStatement(glycan, hasMass, mass, defaultGraphIRI));
			
			sparqlDAO.addStatements(statements, defaultGraphIRI);
		} else {
			logger.debug("The glycan already exists in global repository. URI: " + existing);
			glycanURI = existing;
		}
		
		// add glycan details to the user's private graph
		IRI graphIRI = f.createIRI(graph);
		IRI glycan = f.createIRI(glycanURI);
		Literal glycanLabel = g.getName() == null ? f.createLiteral("") : f.createLiteral(g.getName());
		Literal glycanComment = g.getComment() == null ? f.createLiteral("") : f.createLiteral(g.getComment());
		Literal internalId = g.getInternalId() == null ? f.createLiteral("") : f.createLiteral(g.getInternalId());
		IRI hasInternalId = f.createIRI(ontPrefix + "has_internal_id");
		IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		Literal date = f.createLiteral(new Date());
		
		List<Statement> statements = new ArrayList<Statement>();
		
		statements.add(f.createStatement(glycan, RDFS.LABEL, glycanLabel, graphIRI));
		statements.add(f.createStatement(glycan, hasInternalId, internalId, graphIRI));
		statements.add(f.createStatement(glycan, RDFS.COMMENT, glycanComment, graphIRI));
		statements.add(f.createStatement(glycan, hasAddedToLibrary, date, graphIRI));
		statements.add(f.createStatement(glycan, hasModifiedDate, date, graphIRI));
		
		sparqlDAO.addStatements(statements, graphIRI);
		
		return glycanURI;
	}
	
	@Override
	public String addLinker(Linker l, UserEntity user) throws SparqlException {
		
		String graph = null;
		if (user == null) {
			// cannot add 
			throw new SparqlException ("The user must be provided to put data into private repository");
		}
		
		try {
			// check if there is already a private graph for user
			graph = getGraphForUser(user);
		} catch (SQLException e) {
			throw new SparqlException ("Cannot add the private graph for the user: " + user.getUsername(), e);
		}
		
		String linkerURI;
		ValueFactory f = sparqlDAO.getValueFactory();
		
		// check if the glycan already exists in "default-graph", then we only need to add it to the user's graph
		String existing = getLinkerByPubChemId(l.getPubChemId());
		if (existing == null) {
			linkerURI = generateUniqueURI(uriPrefix + "L");
			
			IRI linker = f.createIRI(linkerURI);
			IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
			IRI hasInchiSequence = f.createIRI(ontPrefix + "has_inChI_sequence");
			IRI hasInchiKey = f.createIRI(ontPrefix + "has_inChI_key");
			IRI hasIupacName = f.createIRI(ontPrefix + "has_iupac_name");
			IRI hasMass = f.createIRI(ontPrefix + "has_mass");
			IRI hasImageUrl = f.createIRI(ontPrefix + "has_image_url");
			IRI hasPubChemId = f.createIRI(ontPrefix + "has_pubchem_compound_id");
			IRI hasMolecularFormula = f.createIRI(ontPrefix + "has_molecular_formula");
			IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
			
			IRI linkerType = f.createIRI(ontPrefix + "Linker");
			
			Literal pubChemId = f.createLiteral(l.getPubChemId());
			Literal inchiSequence = l.getInChiSequence() == null ? f.createLiteral("") : f.createLiteral(l.getInChiSequence());
			Literal inchiKey = l.getInChiKey() == null ? f.createLiteral("") : f.createLiteral(l.getInChiKey());
			Literal imageUrl = l.getImageURL() == null ? f.createLiteral("") : f.createLiteral(l.getImageURL());
			Literal mass = l.getMass() == null ? f.createLiteral("") : f.createLiteral(l.getMass());
			Literal molecularFormula = l.getMolecularFormula() == null ? f.createLiteral("") : f.createLiteral(l.getMolecularFormula());
			Literal iupacName = l.getIupacName() == null ? f.createLiteral("") : f.createLiteral(l.getIupacName());
			Literal date = f.createLiteral(new Date());
			
			List<Statement> statements = new ArrayList<Statement>();
			statements.add(f.createStatement(linker, RDF.TYPE, linkerType, defaultGraphIRI));
			statements.add(f.createStatement(linker, hasInchiSequence, inchiSequence, defaultGraphIRI));
			statements.add(f.createStatement(linker, hasCreatedDate, date, defaultGraphIRI));
			statements.add(f.createStatement(linker, hasInchiKey, inchiKey, defaultGraphIRI));
			statements.add(f.createStatement(linker, hasIupacName, iupacName, defaultGraphIRI));
			statements.add(f.createStatement(linker, hasMass, mass, defaultGraphIRI));
			statements.add(f.createStatement(linker, hasImageUrl, imageUrl, defaultGraphIRI));
			statements.add(f.createStatement(linker, hasPubChemId, pubChemId, defaultGraphIRI));
			statements.add(f.createStatement(linker, hasMolecularFormula, molecularFormula, defaultGraphIRI));
			
			sparqlDAO.addStatements(statements, defaultGraphIRI);
		} else {
			logger.debug("The linker already exists in global repository. URI: " + existing);
			linkerURI = existing;
		}
		
		// add linker details to the user's private graph
		IRI graphIRI = f.createIRI(graph);
		IRI linker = f.createIRI(linkerURI);
		Literal label = l.getName() == null ? f.createLiteral("") : f.createLiteral(l.getName());
		Literal comment = l.getComment() == null ? f.createLiteral("") : f.createLiteral(l.getComment());
		IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		Literal date = f.createLiteral(new Date());
		
		List<Statement> statements = new ArrayList<Statement>();
		
		statements.add(f.createStatement(linker, RDFS.LABEL, label, graphIRI));
		statements.add(f.createStatement(linker, RDFS.COMMENT, comment, graphIRI));
		statements.add(f.createStatement(linker, hasAddedToLibrary, date, graphIRI));
		statements.add(f.createStatement(linker, hasModifiedDate, date, graphIRI));
		
		sparqlDAO.addStatements(statements, graphIRI);
		
		return linkerURI;
	}
	
	@Override
	public String addPrivateGraphForUser (UserEntity uEntity) throws SQLException {
		String URI = sparqlDAO.addGraph(PRIVATE_GRAPH, uEntity.getUsername());
		PrivateGraphEntity graph = new PrivateGraphEntity();
		graph.setUser(uEntity);
		graph.setGraphIRI(URI);
		graphRepository.save (graph);
		return URI;
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
		
		for (Block b: s.getBlocks()) {
			if (b == null)
				continue;
			String blockURI = addBlock (b, user, graph);
			IRI block = f.createIRI(blockURI);
			statements.add(f.createStatement(slideLayout, hasBlock, block));
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
	public void deleteGlycan(String glycanId, UserEntity user) throws SQLException, SparqlException {
		String graph;
		
		graph = getGraphForUser(user);
		if (graph != null) {
			// check to see if the given glycanId is in this graph
			Glycan existing = getGlycanFromURI (uriPrefix + glycanId, graph);
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
	}

	@Override
	public void deleteLinker(String linkerId, UserEntity user) throws SQLException, SparqlException {
		String graph;
		
		graph = getGraphForUser(user);
		if (graph != null) {
			// check to see if the given linkerId is in this graph
			Linker existing = getLinkerFromURI (uriPrefix + linkerId, graph);
			if (existing != null) {
				deleteLinkerByURI (uriPrefix + linkerId, graph);
				return;
			}
		}
	}

	private void deleteLinkerByURI(String uri, String graph) throws SparqlException {
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI linker = f.createIRI(uri);
		IRI graphIRI = f.createIRI(graph);
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(linker, null, null, graphIRI);
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
	
	private String findGlycanInGraphBySequence (String sequence, String graph) throws SparqlException {
		String fromString = "FROM <" + DEFAULT_GRAPH + ">\n";
		String where = "WHERE { " + 
				"				    ?s gadr:has_sequence ?o .\n" +
				"                    ?o gadr:has_sequence_value \"\"\"" + sequence + "\"\"\"^^xsd:string .\n";
		if (!graph.equals(DEFAULT_GRAPH)) {
			// check if the user's private graph has this glycan
			fromString += "FROM <" + graph + ">\n";
			where += "              ?s gadr:has_date_addedtolibrary ?d .\n";
			
		}
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?s ?o\n");
		queryBuf.append (fromString);
		queryBuf.append (where + 
				"				}\n" + 
				"				LIMIT 10");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.size() == 0) 
			return null;
		
		SparqlEntity result = results.get(0);
		String glycanURI = result.getValue("s");
		
		return glycanURI;
	}
	
	private String findLinkerInGraphByPubChem (Long pubChemId, String graph) throws SparqlException {
		String fromString = "FROM <" + DEFAULT_GRAPH + ">\n";
		String where = "WHERE { " + 
				"				    ?s gadr:has_pubchem_compound_id \"" + pubChemId + "\"^^xsd:long .\n";
		if (!graph.equals(DEFAULT_GRAPH)) {
			// check if the user's private graph has this glycan
			fromString += "FROM <" + graph + ">\n";
			where += "              ?s gadr:has_date_addedtolibrary ?d .\n";
			
		}
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append (prefix + "\n");
		queryBuf.append ("SELECT DISTINCT ?s \n");
		queryBuf.append (fromString);
		queryBuf.append (where + 
				"				}\n" + 
				"				LIMIT 10");
		List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
		if (results.size() == 0) 
			return null;
		
		SparqlEntity result = results.get(0);
		String linkerURI = result.getValue("s");
		
		return linkerURI;
	}
	
	private String generateUniqueURI (String pre) throws SparqlException {
		return generateUniqueURI(pre, null);
	}
	
	private String generateUniqueURI (String pre, String graph) throws SparqlException {
		// check the repository to see if the generated URI is unique
		boolean unique = false;
		String newURI = null;
		do {
			newURI = pre + (1000000 + random.nextInt(9999999));
			StringBuffer queryBuf = new StringBuffer();
			queryBuf.append (prefix + "\n");
			queryBuf.append ("SELECT DISTINCT ?o\n");
			queryBuf.append("FROM <" + DEFAULT_GRAPH + ">\n");
			if (graph != null) queryBuf.append ("FROM <" + graph + ">\n");
			queryBuf.append ("WHERE {\n" + 
					"<"+ newURI + "> ?p ?o .\n" + 
					"				}\n" + 
					"				LIMIT 1");
			List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
			unique = results.size() == 0;
		} while (!unique);
		
		return newURI;
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
								feat.setGlycan(getGlycanFromURI(glycanURI, graph));
							} else if (st3.getPredicate().equals(hasLinker)) {
								v = st3.getObject();
								String linkerURI = v.stringValue();
								feat.setLinker(getLinkerFromURI(linkerURI, graph));
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
									feat.setGlycan(getGlycanFromURI(glycanURI, graph));
								} else if (st3.getPredicate().equals(hasLinker)) {
									v = st3.getObject();
									String linkerURI = v.stringValue();
									feat.setLinker(getLinkerFromURI(linkerURI, graph));
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
	
	/**
	 * 
	 * @param graph graph for the user
	 * @param type "Linker" or "Glycan" (rdf type of the subject)
	 * @return total number of triples with that rdf:type as the subject and date_addedToLibrary as the predicate
	 * @throws SparqlException
	 */
	private int getCountByUserByType (String graph, String type) throws SparqlException {
		int total = 0;
		if (graph != null) {
			StringBuffer queryBuf = new StringBuffer();
			queryBuf.append (prefix + "\n");
			queryBuf.append ("SELECT COUNT(DISTINCT ?s) as ?count \n");
			queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
			queryBuf.append ("FROM <" + graph + ">\n");
			queryBuf.append ("WHERE {\n");
			queryBuf.append (" ?s gadr:has_date_addedtolibrary ?d . \n");
			queryBuf.append (" ?s rdf:type  <http://purl.org/gadr/data#" + type +">. }");
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
			return getGlycanFromURI(uriPrefix + glycanId, graph);
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
			return getGlycanFromURI(glycanURI, graph);
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
			return getGlycanFromURI(glycanURI, graph);
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
				Glycan glycan = getGlycanFromURI(glycanURI, graph);
				glycans.add(glycan);	
			}
		}
		
		return glycans;
	}
	
	@Override
	public int getGlycanCountByUser(UserEntity user) throws SQLException, SparqlException {
		String graph = getGraphForUser(user);
		return getCountByUserByType(graph, "Glycan");
	}


	private Glycan getGlycanFromURI (String glycanURI, String graph) throws SparqlException {
		Glycan glycanObject = null;
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI glycan = f.createIRI(glycanURI);
		IRI graphIRI = f.createIRI(graph);
		IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
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
		
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(glycan, null, null, defaultGraphIRI);
		if (statements.hasNext()) {
			glycanObject = new Glycan();
			glycanObject.setUri(glycanURI);
		}
		while (statements.hasNext()) {
			Statement st = statements.next();
			if (st.getPredicate().equals(hasGlytoucanId)) {
				Value glytoucanId = st.getObject();
				glycanObject.setGlytoucanId(glytoucanId.stringValue()); 
			} else if (st.getPredicate().equals(hasMass)) {
				Value mass = st.getObject();
				try {
					if (mass != null && mass.stringValue() != null && !mass.stringValue().isEmpty())
						glycanObject.setMass(Double.parseDouble(mass.stringValue())); 
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
				RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(seq, null, null, defaultGraphIRI);
				while (statements2.hasNext()) {
					Statement st2 = statements2.next();
					if (st2.getPredicate().equals(hasSequenceValue)) {
						Value seqString = st2.getObject();
						glycanObject.setSequence(seqString.stringValue());
					} else if (st2.getPredicate().equals(hasSequenceFormat)) {
						Value formatString = st2.getObject();
						glycanObject.setSequenceType(formatString.stringValue());
					}  
				}
			} 
		}
		
		if (glycanObject != null) {
			statements = sparqlDAO.getStatements(glycan, null, null, graphIRI);
			while (statements.hasNext()) {
				Statement st = statements.next();
				if (st.getPredicate().equals(RDFS.LABEL)) {
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
				} 
			}
		}
		
		return glycanObject;
	}

	@Override
	public String getGraphForUser (UserEntity user) throws SQLException {
		PrivateGraphEntity graph = graphRepository.findByUser(user);
		if (graph != null) 
			return graph.getGraphIRI();
		else { // try to create for the first time 
			return addPrivateGraphForUser(user);
		}
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


	public String getLinkerByPubChemId(Long long1) throws SparqlException {
		return findLinkerInGraphByPubChem(long1, DEFAULT_GRAPH);
	}
	
	public String getLinkerByPubChemId (Long pubChemId, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		return findLinkerInGraphByPubChem (pubChemId, graph);
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
	
	@Override
	public int getLinkerCountByUser(UserEntity user) throws SQLException, SparqlException {
		String graph = getGraphForUser(user);
		return getCountByUserByType (graph, "Linker");
	}
	
	private Linker getLinkerFromURI(String linkerURI, String graph) {
		Linker linkerObject = null;
		ValueFactory f = sparqlDAO.getValueFactory();
		
		IRI linker = f.createIRI(linkerURI);
		IRI graphIRI = f.createIRI(graph);
		IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
		IRI hasInchiSequence = f.createIRI(ontPrefix + "has_inChI_sequence");
		IRI hasInchiKey = f.createIRI(ontPrefix + "has_inChI_key");
		IRI hasIupacName = f.createIRI(ontPrefix + "has_iupac_name");
		IRI hasMass = f.createIRI(ontPrefix + "has_mass");
		IRI hasImageUrl = f.createIRI(ontPrefix + "has_image_url");
		IRI hasPubChemId = f.createIRI(ontPrefix + "has_pubchem_compound_id");
		IRI hasMolecularFormula = f.createIRI(ontPrefix + "has_molecular_formula");
		IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
		IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
		
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(linker, null, null, defaultGraphIRI);
		if (statements.hasNext()) {
			linkerObject = new Linker();
			linkerObject.setUri(linkerURI);
		}
		while (statements.hasNext()) {
			Statement st = statements.next();
			if (st.getPredicate().equals(hasInchiSequence)) {
				Value seq = st.getObject();
				linkerObject.setInChiSequence(seq.stringValue()); 
			} else if (st.getPredicate().equals(hasInchiKey)) {
				Value val = st.getObject();
				linkerObject.setInChiKey(val.stringValue()); 
			} else if (st.getPredicate().equals(hasIupacName)) {
				Value val = st.getObject();
				linkerObject.setIupacName(val.stringValue()); 
			} else if (st.getPredicate().equals(hasImageUrl)) {
				Value val = st.getObject();
				linkerObject.setImageURL(val.stringValue()); 
			} else if (st.getPredicate().equals(hasPubChemId)) {
				Value val = st.getObject();
				if (val != null)
					linkerObject.setPubChemId(Long.parseLong(val.stringValue())); 
			} else if (st.getPredicate().equals(hasMolecularFormula)) {
				Value val = st.getObject();
				linkerObject.setMolecularFormula(val.stringValue()); 
			} else if (st.getPredicate().equals(hasMass)) {
				Value mass = st.getObject();
				try {
					if (mass != null && mass.stringValue() != null && !mass.stringValue().isEmpty())
						linkerObject.setMass(Double.parseDouble(mass.stringValue())); 
				} catch (NumberFormatException e) {
					logger.warn ("Glycan mass is invalid", e);
				}
			} else if (st.getPredicate().equals(hasCreatedDate)) {
				Value value = st.getObject();
			    if (value instanceof Literal) {
			    	Literal literal = (Literal)value;
			    	XMLGregorianCalendar calendar = literal.calendarValue();
			    	Date date = calendar.toGregorianCalendar().getTime();
			    	linkerObject.setDateCreated(date);
			    }
			}
		}
		
		if (linkerObject != null) {
			statements = sparqlDAO.getStatements(linker, null, null, graphIRI);
			while (statements.hasNext()) {
				Statement st = statements.next();
				if (st.getPredicate().equals(RDFS.LABEL)) {
					Value label = st.getObject();
					linkerObject.setName(label.stringValue());
				} else if (st.getPredicate().equals(RDFS.COMMENT)) {
					Value comment = st.getObject();
					linkerObject.setComment(comment.stringValue());
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
				} 
			}
		}
		
		return linkerObject;
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

	private SlideLayout getSlideLayoutFromURI(String slideLayoutURI, String graph) throws SparqlException, SQLException {
		return getSlideLayoutFromURI(slideLayoutURI, true, graph);
	}
	
	private String getSortPredicate(String field) {
		if (field == null || field.equalsIgnoreCase("name")) 
			return "rdfs:label";
		else if (field.equalsIgnoreCase("comment")) 
			return "rdfs:comment";
		else if (field.equalsIgnoreCase("glytoucanId"))
			return "gadr:has_glytoucan_id";
		else if (field.equalsIgnoreCase("internalId"))
			return "gadr:has_internal_id";
		else if (field.equalsIgnoreCase("dateModified"))
			return "gadr:has_date_modified";
		else if (field.equalsIgnoreCase("mass"))
			return "gadr:has_mass";
		else if (field.equalsIgnoreCase("id"))
			return null;
		return null;
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
		return null;
	}
	
	@Override
	public void updateGlycan(Glycan g, UserEntity user) throws SparqlException, SQLException {
		String graph = getGraphForUser(user);
		Glycan existing = getGlycanFromURI(g.getUri(), graph);
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
		IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
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

	
}
