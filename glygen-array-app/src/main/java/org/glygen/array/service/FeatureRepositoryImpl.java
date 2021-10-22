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
import org.glygen.array.persistence.rdf.CommercialSource;
import org.glygen.array.persistence.rdf.CompoundFeature;
import org.glygen.array.persistence.rdf.ControlFeature;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.FeatureType;
import org.glygen.array.persistence.rdf.GPLinkedGlycoPeptide;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.GlycanInFeature;
import org.glygen.array.persistence.rdf.GlycoLipid;
import org.glygen.array.persistence.rdf.GlycoPeptide;
import org.glygen.array.persistence.rdf.GlycoProtein;
import org.glygen.array.persistence.rdf.LandingLight;
import org.glygen.array.persistence.rdf.LinkedGlycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.Lipid;
import org.glygen.array.persistence.rdf.NegControlFeature;
import org.glygen.array.persistence.rdf.NonCommercialSource;
import org.glygen.array.persistence.rdf.PeptideLinker;
import org.glygen.array.persistence.rdf.ProteinLinker;
import org.glygen.array.persistence.rdf.Publication;
import org.glygen.array.persistence.rdf.Range;
import org.glygen.array.persistence.rdf.ReducingEndConfiguration;
import org.glygen.array.persistence.rdf.ReducingEndType;
import org.glygen.array.persistence.rdf.Source;
import org.glygen.array.persistence.rdf.SourceType;
import org.glygen.array.persistence.rdf.metadata.FeatureMetadata;
import org.glygen.array.persistence.rdf.template.MetadataTemplateType;
import org.glygen.array.util.SparqlUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(value="sesameTransactionManager") 
public class FeatureRepositoryImpl extends GlygenArrayRepositoryImpl implements FeatureRepository {
	
	final static String featureTypePredicate = ontPrefix + "Feature";
	final static String hasLinkerPredicate = ontPrefix + "has_linker";
	final static String hasLipidPredicate = ontPrefix + "has_lipid";
	final static String hasProteinPredicate = ontPrefix + "has_protein";
	final static String hasPeptidePredicate = ontPrefix + "has_peptide";
	final static String hasMoleculePredicate = ontPrefix + "has_molecule";
	final static String hasLinkedGlycanPredicate = ontPrefix + "has_linked_glycan";
	final static String hasGlycoPeptidePredicate = ontPrefix + "has_glycopeptide";
	final static String hasMaxPredicate = ontPrefix + "has_max";
	final static String hasMinPredicate = ontPrefix + "has_min";
	
	
	
	final static String hasPositionPredicate = ontPrefix + "has_molecule_position";
	final static String hasPositionValuePredicate = ontPrefix + "has_position";
    
	
	@Autowired
	GlycanRepository glycanRepository;
	
	@Autowired
	LinkerRepository linkerRepository;
	
	@Autowired
    MetadataRepository metadataRepository;
	
	Map<String, Feature> featureCache = new HashMap<String, Feature>();

	@Override
	public String addFeature(Feature feature, UserEntity user) throws SparqlException, SQLException {
		String graph = null;
		if (feature == null)
			// cannot add 
			throw new SparqlException ("Not enough information is provided to register a feature");
		
		
		// check if there is already a private graph for user
		graph = getGraphForUser(user);
		
		ValueFactory f = sparqlDAO.getValueFactory();
		IRI featureType = f.createIRI(featureTypePredicate);
		String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
		String featureURI = generateUniqueURI(uriPrefix + "F", allGraphs);
		IRI feat = f.createIRI(featureURI);
		IRI graphIRI = f.createIRI(graph);
		IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
		IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
		IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
		IRI hasLinker = f.createIRI(hasLinkerPredicate);
		IRI hasMolecule = f.createIRI(hasMoleculePredicate);
		IRI hasLinkedGlycan = f.createIRI(hasLinkedGlycanPredicate);
        IRI hasGlycoPeptide = f.createIRI(hasGlycoPeptidePredicate);
        IRI hasLipid = f.createIRI(hasLipidPredicate);
        IRI hasProtein = f.createIRI(hasProteinPredicate);
        IRI hasPeptide = f.createIRI(hasPeptidePredicate);
        IRI hasPositionContext = f.createIRI(hasPositionPredicate);
        IRI hasPosition = f.createIRI(hasPositionValuePredicate);
        IRI hasMax = f.createIRI(hasMaxPredicate);
        IRI hasMin = f.createIRI(hasMinPredicate);
		IRI hasFeatureMetadata = f.createIRI(featureMetadataPredicate);
		Literal date = f.createLiteral(new Date());
		IRI hasFeatureType = f.createIRI(hasTypePredicate);
        Literal type = f.createLiteral(feature.getType().name());
        IRI hasInternalId = f.createIRI(ontPrefix + "has_internal_id");
        Literal internalId = feature.getInternalId() == null ? null : f.createLiteral(feature.getInternalId());
        
		//if (feature.getName() == null || feature.getName().trim().isEmpty()) {
		//    feature.setName(featureURI.substring(featureURI.lastIndexOf("/")+1));
		//}
		
		Literal label = feature.getName() == null ? null : f.createLiteral(feature.getName());
		
		List<Statement> statements = new ArrayList<Statement>();
		
		if (label != null) statements.add(f.createStatement(feat, RDFS.LABEL, label, graphIRI));
		statements.add(f.createStatement(feat, RDF.TYPE, featureType, graphIRI));
		statements.add(f.createStatement(feat, hasCreatedDate, date, graphIRI));
		statements.add(f.createStatement(feat, hasAddedToLibrary, date, graphIRI));
		statements.add(f.createStatement(feat, hasModifiedDate, date, graphIRI));
		statements.add(f.createStatement(feat, hasFeatureType, type, graphIRI));
		if (internalId != null) statements.add(f.createStatement(feat, hasInternalId, internalId, graphIRI));
		
		Linker linker = feature.getLinker();
		if (linker != null) {   // linker is optional for some feature types
    		if (linker.getUri() == null) {
    		    if (linker.getId() != null)
    		        linker.setUri(uriPrefix + linker.getId());
    		    else {
    		        throw new SparqlException ("No enough information is provided to add the feature, linker cannot be found!"); 
    		    }
    		}
    		
    		IRI linkerIRI = f.createIRI(linker.getUri());
    		statements.add(f.createStatement(feat, hasLinker, linkerIRI, graphIRI));
		}
		
		switch (feature.getType()) {
		case LINKEDGLYCAN:
		    if (((LinkedGlycan) feature).getGlycans() != null) {
	            for (GlycanInFeature gf: ((LinkedGlycan) feature).getGlycans()) {
	                Glycan g = gf.getGlycan();
	                if (g.getUri() == null) {
	                    if (g.getId() != null) {
	                        g.setUri(uriPrefix + g.getId());
	                    } else {
	                        throw new SparqlException ("No enough information is provided to add the feature, glycan " + g.getName() + " cannot be found!");

	                    }
	                }
	                
	                IRI glycanIRI = f.createIRI(g.getUri());
	                statements.add(f.createStatement(feat, hasMolecule, glycanIRI, graphIRI));
	                addSourceContext (gf, feat, glycanIRI, statements, graph);
	            }
	        }
            break;
        case GLYCOLIPID:
            if (((GlycoLipid)feature).getGlycans() != null) {
                for (LinkedGlycan g: ((GlycoLipid)feature).getGlycans()) {
                    if (g.getUri() == null) {
                        if (g.getId() != null) {
                            g.setUri(uriPrefix + g.getId());
                        } else {
                            throw new SparqlException ("No enough information is provided to add the feature, glycan " + g.getName() + " cannot be found!");
    
                        }
                    }
                    
                    IRI glycanIRI = f.createIRI(g.getUri());
                    statements.add(f.createStatement(feat, hasLinkedGlycan, glycanIRI, graphIRI));
                }
            }
            if (((GlycoLipid)feature).getLipid() == null) {
                throw new SparqlException ("No enough information is provided to add the feature, lipid should be provided");
            } else {
                Lipid lipid = ((GlycoLipid)feature).getLipid();
                if (lipid.getUri() == null) {
                    if (lipid.getId() != null) {
                        lipid.setUri(uriPrefix + lipid.getId());
                    } else {
                        throw new SparqlException ("No enough information is provided to add the feature, lipid " + lipid.getName() + " cannot be found!");

                    }   
                }
                IRI lipidIRI = f.createIRI(lipid.getUri());
                statements.add(f.createStatement(feat, hasLipid, lipidIRI, graphIRI));
            }
            break;
        case GLYCOPEPTIDE:
            if (((GlycoPeptide)feature).getGlycans() != null) {
                for (LinkedGlycan g: ((GlycoPeptide)feature).getGlycans()) {
                    if (g.getUri() == null) {
                        if (g.getId() != null) {
                            g.setUri(uriPrefix + g.getId());
                        } else {
                            throw new SparqlException ("No enough information is provided to add the feature, glycan " + g.getName() + " cannot be found!");
    
                        }
                    }
                    
                    IRI glycanIRI = f.createIRI(g.getUri());
                    statements.add(f.createStatement(feat, hasLinkedGlycan, glycanIRI, graphIRI));
                }
            }
            if (((GlycoPeptide)feature).getPeptide() == null) {
                throw new SparqlException ("No enough information is provided to add the feature, peptide should be provided");
            } else {
                PeptideLinker pl = ((GlycoPeptide)feature).getPeptide();
                if (pl.getUri() == null) {
                    if (pl.getId() != null) {
                        pl.setUri(uriPrefix + pl.getId());
                    } else {
                        throw new SparqlException ("No enough information is provided to add the feature, peptide " + pl.getName() + " cannot be found!");

                    }
                }
                IRI peptideIRI = f.createIRI(pl.getUri());
                statements.add(f.createStatement(feat, hasPeptide, peptideIRI, graphIRI));
            }
            break;
        case GLYCOPROTEIN:
            if (((GlycoProtein)feature).getGlycans() != null) {
                for (LinkedGlycan g: ((GlycoProtein)feature).getGlycans()) {
                    if (g.getUri() == null) {
                        if (g.getId() != null) {
                            g.setUri(uriPrefix + g.getId());
                        } else {
                            throw new SparqlException ("No enough information is provided to add the feature, glycan " + g.getName() + " cannot be found!");
    
                        }
                    }
                    
                    IRI glycanIRI = f.createIRI(g.getUri());
                    statements.add(f.createStatement(feat, hasLinkedGlycan, glycanIRI, graphIRI));
                }
            }
            if (((GlycoProtein)feature).getProtein() == null) {
                throw new SparqlException ("No enough information is provided to add the feature, protein should be provided");
            } else {
                ProteinLinker pl = ((GlycoProtein)feature).getProtein();
                if (pl.getUri() == null) {
                    if (pl.getId() != null) {
                        pl.setUri(uriPrefix + pl.getId());
                    } else {
                        throw new SparqlException ("No enough information is provided to add the feature, protein " + pl.getName() + " cannot be found!");

                    }
                }
                IRI proteinIRI = f.createIRI(pl.getUri());
                statements.add(f.createStatement(feat, hasProtein, proteinIRI, graphIRI));
            }
            break;
        case GPLINKEDGLYCOPEPTIDE:
            if (((GPLinkedGlycoPeptide)feature).getPeptides() != null) {
                for (GlycoPeptide g: ((GPLinkedGlycoPeptide)feature).getPeptides()) {
                    if (g.getUri() == null) {
                        if (g.getId() != null) {
                            g.setUri(uriPrefix + g.getId());
                        } else {
                            throw new SparqlException ("No enough information is provided to add the feature, glycopeptide " + g.getName() + " cannot be found!");
    
                        }
                    }
                    
                    IRI glycoPeptideIRI = f.createIRI(g.getUri());
                    statements.add(f.createStatement(feat, hasGlycoPeptide, glycoPeptideIRI, graphIRI));
                }
            }
            if (((GPLinkedGlycoPeptide)feature).getProtein() == null) {
                throw new SparqlException ("No enough information is provided to add the feature, protein should be provided");
            } else {
                ProteinLinker pl = ((GPLinkedGlycoPeptide)feature).getProtein();
                if (pl.getUri() == null) {
                    if (pl.getId() != null) {
                        pl.setUri(uriPrefix + pl.getId());
                    } else {
                        throw new SparqlException ("No enough information is provided to add the feature, protein " + pl.getName() + " cannot be found!");

                    }
                }
                IRI proteinIRI = f.createIRI(pl.getUri());
                statements.add(f.createStatement(feat, hasProtein, proteinIRI, graphIRI));
            }
            break;
        
        default:
            break;
		
		}
		
		String positionContextURI = generateUniqueURI(uriPrefix + "PC", graph);
        IRI positionContext = f.createIRI(positionContextURI);
		if (feature.getPositionMap() != null) {
			for (String position: feature.getPositionMap().keySet()) {
			    // need to check if the position is valid
                try {
                    Integer.parseInt(position);
                } catch (NumberFormatException e) {
                    logger.info("got invalid position for the feature's glycans", e);
                    continue;
                }
				String glycanId = feature.getPositionMap().get(position);
				IRI glycanIRI = f.createIRI(uriPrefix + glycanId);
				Literal pos = f.createLiteral(position);
				
				statements.add(f.createStatement(feat, hasPositionContext, positionContext, graphIRI));
				statements.add(f.createStatement(positionContext, hasMolecule, glycanIRI, graphIRI));
				statements.add(f.createStatement(positionContext, hasPosition, pos, graphIRI));
			}
		}
		
		Range range = null;
		if (feature.getType() == FeatureType.LINKEDGLYCAN) {
		    range = ((LinkedGlycan) feature).getRange();
		} else if (feature.getType() == FeatureType.GLYCOPEPTIDE) {
            range = ((GlycoPeptide) feature).getRange();
        }
		if (range != null) {
		    IRI max = range.getMax() != null ? f.createIRI(range.getMax()+"") : null;
		    IRI min = range.getMin() != null ? f.createIRI(range.getMin()+"") : null;
		    if (max != null) statements.add(f.createStatement(positionContext, hasMax, max, graphIRI));
		    if (min != null) statements.add(f.createStatement(positionContext, hasMin, min, graphIRI));
		}
		
		if (feature.getMetadata() != null) {
		    if (feature.getMetadata().getUri() != null) {
		        statements.add(f.createStatement(feat, hasFeatureMetadata, f.createIRI(feature.getMetadata().getUri()), graphIRI));
		    } else {
		        String metadataURI = metadataRepository.addMetadataCategory(feature.getMetadata(), MetadataTemplateType.FEATURE, featureMetadataPredicate, featureMetadataTypePredicate, "FM", user);
		        statements.add(f.createStatement(feat, hasFeatureMetadata, f.createIRI(metadataURI), graphIRI));
		    }
		}
		
		sparqlDAO.addStatements(statements, graphIRI);
		
		return featureURI;
	}
	
	private void addSourceContext(GlycanInFeature g, IRI feat, IRI glycanIRI, List<Statement> statements, String graph) throws SparqlException {
	    ValueFactory f = sparqlDAO.getValueFactory();
	    IRI graphIRI = f.createIRI(graph);
	    IRI hasGlycanContext = f.createIRI(hasGlycanContextPredicate);
        IRI hasSource = f.createIRI(hasSourcePredicate);
        IRI hasReducingEndConfig = f.createIRI(hasReducingEndConfigPredicate);
        IRI hasBatchId = f.createIRI(hasBatchIdPredicate);
        IRI hasVendor = f.createIRI(hasVendorPredicate);
        IRI hasProviderLab = f.createIRI(hasProviderLabPredicate);
        IRI hasCatalogNumber = f.createIRI(hasCatalogueNumberPredicate);
        IRI hasMethod = f.createIRI(hasMethodPredicate);
        IRI hasType = f.createIRI(hasTypePredicate);
        IRI hasMolecule = f.createIRI(hasMoleculePredicate);
        String glycanContextURI = generateUniqueURI(uriPrefix + "PC", graph);
        IRI glycanContext = f.createIRI(glycanContextURI);
        IRI hasUrl = f.createIRI(hasURLPredicate);
        if (g.getSource() != null) {
            statements.add(f.createStatement(feat, hasGlycanContext, glycanContext, graphIRI));
            statements.add(f.createStatement(glycanContext, hasMolecule, glycanIRI, graphIRI));
            String sourceURI = generateUniqueURI(uriPrefix + "SO", graph);
            IRI source = f.createIRI(sourceURI);
            statements.add(f.createStatement(glycanContext, hasSource, source, graphIRI));
            statements.add(f.createStatement(source, hasType, f.createLiteral(g.getSource().getType().name()), graphIRI));
            switch (g.getSource().getType()) {
            case COMMERCIAL:
                Literal vendor = ((CommercialSource) g.getSource()).getVendor() != null ? 
                        f.createLiteral(((CommercialSource) g.getSource()).getVendor()) : null;
                Literal batchId = ((CommercialSource) g.getSource()).getBatchId() != null ? 
                        f.createLiteral(((CommercialSource) g.getSource()).getBatchId()) : null;
                Literal catalogNo = ((CommercialSource) g.getSource()).getCatalogueNumber() != null ? 
                        f.createLiteral(((CommercialSource) g.getSource()).getCatalogueNumber()) : null;
                        
                if (vendor != null) statements.add(f.createStatement(source, hasVendor, vendor, graphIRI));
                if (batchId != null) statements.add(f.createStatement(source, hasBatchId, batchId, graphIRI));
                if (catalogNo != null) statements.add(f.createStatement(source, hasCatalogNumber, catalogNo, graphIRI));
                break;
            case NONCOMMERCIAL:
                Literal providerLab = ((NonCommercialSource) g.getSource()).getProviderLab() != null ? 
                        f.createLiteral(((NonCommercialSource) g.getSource()).getProviderLab()) : null;
                batchId = ((NonCommercialSource) g.getSource()).getBatchId() != null ? 
                        f.createLiteral(((NonCommercialSource) g.getSource()).getBatchId()) : null;
                Literal method = ((NonCommercialSource) g.getSource()).getMethod() != null ? 
                        f.createLiteral(((NonCommercialSource) g.getSource()).getMethod()) : null;
                Literal comment = ((NonCommercialSource) g.getSource()).getComment() != null ? 
                        f.createLiteral(((NonCommercialSource) g.getSource()).getComment()) : null;
                if (providerLab != null) statements.add(f.createStatement(source, hasProviderLab, providerLab, graphIRI));
                if (batchId != null) statements.add(f.createStatement(source, hasBatchId, batchId, graphIRI));
                if (method != null) statements.add(f.createStatement(source, hasMethod, method, graphIRI));
                if (comment != null) statements.add(f.createStatement(source, RDFS.COMMENT, comment, graphIRI));
                break;
            case NOTRECORDED:
            default:
                break;
            
            }
        }
        if (g.getReducingEndConfiguration() != null) {
            String redEndConURI = generateUniqueURI(uriPrefix + "REC", graph);
            IRI redEndCon = f.createIRI(redEndConURI);
            statements.add(f.createStatement(glycanContext, hasReducingEndConfig, redEndCon, graphIRI));
            Literal redEndType = f.createLiteral(g.getReducingEndConfiguration().getType().name());
            Literal redEndComment = g.getReducingEndConfiguration().getComment() != null ? 
                            f.createLiteral(g.getReducingEndConfiguration().getComment()) : null;
            statements.add(f.createStatement(redEndCon, hasType, redEndType, graphIRI));
            if (redEndComment != null)
                statements.add(f.createStatement(redEndCon, RDFS.COMMENT, redEndComment, graphIRI));
        }
         
        addGlycanInFeaturePublications (g, glycanContext, graph);
        
        if (g.getUrls() != null) {
            for (String url: g.getUrls()) {
                Literal urlLit = f.createLiteral(url);
                statements.add(f.createStatement(glycanContext, hasUrl, urlLit, graphIRI));
            }
        }
    }
	
	
	void addGlycanInFeaturePublications (GlycanInFeature g, IRI glycan, String graph) throws SparqlException {
	    String uriPre = uriPrefix;
        if (graph.equals(DEFAULT_GRAPH)) {
            uriPre = uriPrefixPublic;
        }
        
	    ValueFactory f = sparqlDAO.getValueFactory();
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
        
        if (g.getPublications() != null) {
            for (Publication pub : g.getPublications()) {
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
                
                statements.add(f.createStatement(glycan, hasPub, publication, graphIRI));
                sparqlDAO.addStatements(statements, graphIRI);
            }
        }
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
	    return getFeatureByLabel(label, "rdfs:label", user);
	}
	
	@Override
    public Feature getFeatureByLabel(String label, String predicate, UserEntity user) throws SparqlException, SQLException {
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
        queryBuf.append ( " ?s " + predicate + " ?l FILTER (lcase(str(?l)) = \"\"\"" + label.toLowerCase() + "\"\"\") \n"
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
	    return getFeatureByUser(user, offset, limit, field, order, null, null);
	}
	
	@Override
    public List<Feature> getFeatureByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException {
        return getFeatureByUser(user, offset, limit, field, order, searchValue, null);
    }	
	
    @Override
    public List<Feature> getFeatureByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue, FeatureType featureType) throws SparqlException, SQLException {
		List<Feature> features = new ArrayList<Feature>();
		
		// get all featureURIs from user's private graph
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
		if (graph != null) {
		    
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
                sortLine += "filter (bound (?sortBy) or !bound(?public) ) . \n";
                publicSortLine = "OPTIONAL {?public " + sortPredicate + " ?sortBy } .\n";  
            }
            
            
            String orderByLine = " ORDER BY " + (order == 0 ? "DESC" : "ASC") + (sortPredicate == null ? "(?s)": "(?sortBy)");  
            StringBuffer queryBuf = new StringBuffer();
            queryBuf.append (prefix + "\n");
            queryBuf.append ("SELECT DISTINCT ?s");
            if (sortPredicate != null) {
               // queryBuf.append(", ?sortBy");
            }
            queryBuf.append ("\nFROM <" + graph + ">\n");
            if (!graph.equals(GlygenArrayRepository.DEFAULT_GRAPH))  {
                queryBuf.append ("FROM NAMED <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n");
            }
            queryBuf.append ("WHERE {\n {\n");
            queryBuf.append (
                    " ?s gadr:has_date_addedtolibrary ?d .\n" +
                    " ?s rdf:type  <http://purl.org/gadr/data#Feature>. \n" +
                    " ?s rdfs:label ?name . \n"); 
            if (featureType != null) {
                queryBuf.append("?s gadr:has_type \"" + featureType.name() + "\"^^xsd:string . \n");
            }
            queryBuf.append(
                    " OPTIONAL {?s gadr:has_public_uri ?public  } .\n" + 
                            sortLine + searchPredicate + 
                    "}\n" );
             if (!graph.equals(GlygenArrayRepository.DEFAULT_GRAPH))  {             
                 queryBuf.append ("UNION {" +
                    "?s gadr:has_public_uri ?public . \n" +
                    "GRAPH <" + GlygenArrayRepository.DEFAULT_GRAPH + "> {\n" +
                    " ?public rdf:type  <http://purl.org/gadr/data#Feature>. \n" + 
                    " ?public rdfs:label ?name . \n"); 
                 if (featureType != null) {
                     queryBuf.append("?public gadr:has_type \"" + featureType.name() + "\"^^xsd:string . \n");
                 }
                 queryBuf.append(
                        publicSortLine + publicSearchPredicate + 
                    "}}\n"); 
             }
             queryBuf.append ("}" + 
                     orderByLine + 
                    ((limit == -1) ? " " : " LIMIT " + limit) +
                    " OFFSET " + offset);
			
			List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
			
			for (SparqlEntity sparqlEntity : results) {
				String featureURI = sparqlEntity.getValue("s");
				//logger.info("Getting " +  featureURI + " from repository");
				Feature feature = getFeatureFromURI(featureURI, user);
				features.add(feature);	
			}
		}
		
		return features;
	}
	
	@Override
	protected String getSortPredicate(String field) {
	    String predicate = super.getSortPredicate(field);
	    if (predicate == null) {
	        if (field != null && field.equalsIgnoreCase("linker")) {
	            return "gadr:has_linker ?l . ?l rdfs:label";
	        } else if (field != null && field.startsWith("glycan")) {
	            return "gadr:has_molecule ?g . ?g rdfs:label";
	        } else if (field != null && field.equalsIgnoreCase("type")) {
	            return "gadr:has_type";
	        } else if (field != null && field.equalsIgnoreCase("internalId")) {
                return "gadr:has_internal_id";
            }
	    } 
        return predicate;
	}
	
	public String getSearchPredicate (String searchValue, String queryLabel) {
	    if (searchValue != null) {
            searchValue = SparqlUtils.escapeSpecialCharacters (searchValue.trim());
        }
        String predicates = "";
        
        predicates += queryLabel + " rdfs:label ?value1 .\n";
        predicates += "OPTIONAL {" + queryLabel + " gadr:has_type ?value2 .}\n";
        predicates += "OPTIONAL {" + queryLabel + " gadr:has_internal_id ?value7 .}\n";
        predicates += "OPTIONAL {" + queryLabel + " gadr:has_molecule ?g . ?g gadr:has_glytoucan_id ?value3 . ?g rdfs:label ?value4} \n";
        predicates += "OPTIONAL {" + queryLabel + " gadr:has_linker ?l . ?l gadr:has_pubchem_compound_id ?value5 . ?l rdfs:label ?value6} \n";
        
        int numberOfValues = 7; // need to match with the total values (?value1 - ?value7) specified in above predicates
        
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
    public int getFeatureCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
		return getCountByUserByType(graph, featureTypePredicate, searchValue);
	}
    
    @Override
    public int getFeatureCountByUserByType(UserEntity user, FeatureType featureType, String searchValue) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        
        int total = 0;
        if (graph != null) {
            String sortPredicate = getSortPredicate (null);
            
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
                sortLine += "filter (bound (?sortBy) or !bound(?public) ) . \n";
                publicSortLine = "OPTIONAL {?public " + sortPredicate + " ?sortBy } .\n";  
            }
            
            
            StringBuffer queryBuf = new StringBuffer();
            queryBuf.append (prefix + "\n");
            queryBuf.append ("SELECT COUNT(DISTINCT ?s) as ?count \n");
            //queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
            queryBuf.append ("FROM <" + graph + ">\n");
            if (!graph.equals(GlygenArrayRepository.DEFAULT_GRAPH))  {
                queryBuf.append ("FROM NAMED <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n");
            }
            queryBuf.append ("WHERE {\n {\n");
            queryBuf.append (" ?s gadr:has_date_addedtolibrary ?d . \n");
            queryBuf.append (" ?s rdf:type  <" + featureTypePredicate +">. ");
            if (featureType != null) {
                queryBuf.append(" ?s gadr:has_type \"" + featureType.toString() + "\"^^xsd:string . \n");
            }
            queryBuf.append(
                    " OPTIONAL {?s gadr:has_public_uri ?public  } .\n");
            queryBuf.append (sortLine + searchPredicate + "} ");
            
            if (!graph.equals(GlygenArrayRepository.DEFAULT_GRAPH))  {             
                 queryBuf.append ("UNION {" +
                    "?s gadr:has_public_uri ?public . \n" +
                    "GRAPH <" + GlygenArrayRepository.DEFAULT_GRAPH + "> {\n");
                 queryBuf.append (" ?public rdf:type  <" + featureTypePredicate +">. ");
                 if (featureType != null) {
                     queryBuf.append(" ?public gadr:has_type \"" + featureType.toString() + "\"^^xsd:string . \n");
                 }
                 queryBuf.append (publicSortLine + publicSearchPredicate + "}}\n");
            }
            queryBuf.append("}");
                    
            List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
            
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
	
	private FeatureType getFeatureTypeForFeature (String featureURI, String graph) throws SparqlException {
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?t \n");
        queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ("<" +  featureURI + "> gadr:has_type ?t . }");

        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        if (results.isEmpty())
            return null;
        else {
            String type = results.get(0).getValue("t");
            return FeatureType.valueOf(type);
        }
    }

	@Override
	public Feature getFeatureFromURI(String featureURI, UserEntity user) throws SparqlException, SQLException {
	     //check the cache first
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
		IRI hasInternalId = f.createIRI(ontPrefix + "has_internal_id");
		IRI hasFeatureMetadata = f.createIRI(featureMetadataPredicate);
		IRI hasLinkedGlycan = f.createIRI(hasLinkedGlycanPredicate);
        IRI hasGlycoPeptide = f.createIRI(hasGlycoPeptidePredicate);
        IRI hasLipid = f.createIRI(hasLipidPredicate);
        IRI hasProtein = f.createIRI(hasProteinPredicate);
        IRI hasPeptide = f.createIRI(hasPeptidePredicate);
        IRI hasMax = f.createIRI(hasMaxPredicate);
        IRI hasMin = f.createIRI(hasMinPredicate);
        IRI hasGlycanContext = f.createIRI(hasGlycanContextPredicate);
        IRI hasSource = f.createIRI(hasSourcePredicate);
        IRI hasReducingEndConfig = f.createIRI(hasReducingEndConfigPredicate);
		IRI hasBatchId = f.createIRI(hasBatchIdPredicate);
		IRI hasVendor = f.createIRI(hasVendorPredicate);
		IRI hasProviderLab = f.createIRI(hasProviderLabPredicate);
		IRI hasCatalogNumber = f.createIRI(hasCatalogueNumberPredicate);
		IRI hasMethod = f.createIRI(hasMethodPredicate);
		IRI hasType = f.createIRI(hasTypePredicate);
		
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
		
		FeatureType featureType = getFeatureTypeForFeature (featureURI, graph);
		Map<String, Range> rangeMap = new HashMap<String, Range>();
		RepositoryResult<Statement> statements = sparqlDAO.getStatements(feature, null, null, graphIRI);
		Map<String, String> positionMap = new HashMap<>();
		if (statements.hasNext()) {
		    
		    switch (featureType) {
            case LINKEDGLYCAN:
                featureObject = new LinkedGlycan();
                ((LinkedGlycan) featureObject).setGlycans(new ArrayList<GlycanInFeature>());
                break;
            case COMPOUND:
                featureObject = new CompoundFeature();
                break;
            case CONTROL:
                featureObject = new ControlFeature();
                break;
            case GLYCOLIPID:
                featureObject = new GlycoLipid();
                ((GlycoLipid) featureObject).setGlycans(new ArrayList<LinkedGlycan>());
                break;
            case GLYCOPEPTIDE:
                featureObject = new GlycoPeptide();
                ((GlycoPeptide) featureObject).setGlycans(new ArrayList<LinkedGlycan>());
                break;
            case GLYCOPROTEIN:
                featureObject = new GlycoProtein();
                ((GlycoProtein) featureObject).setGlycans(new ArrayList<LinkedGlycan>());
                break;
            case GPLINKEDGLYCOPEPTIDE:
                featureObject = new GPLinkedGlycoPeptide();
                ((GPLinkedGlycoPeptide) featureObject).setPeptides(new ArrayList<GlycoPeptide>());
                break;
            case LANDING_LIGHT:
                featureObject = new LandingLight();
                break;
            case NEGATIVE_CONTROL:
                featureObject = new NegControlFeature();
                break;
            default:
                featureObject = new ControlFeature();
                break;
            
            }
			featureObject.setUri(featureURI);
			featureObject.setId(featureURI.substring(featureURI.lastIndexOf("/")+1));
			featureObject.setPositionMap(positionMap);
		}
		
		boolean legacy = true;
		// check if this is a feature without glycanContext
		RepositoryResult<Statement> statementsContext = sparqlDAO.getStatements(feature, hasGlycanContext, null, graphIRI);
		if (statementsContext.hasNext()) {
		    legacy = false;
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
			} else if (st.getPredicate().equals(hasInternalId)) { 
			    Value label = st.getObject();
                featureObject.setInternalId(label.stringValue());
			} else if (st.getPredicate().equals(hasLinker)) {
				Value value = st.getObject();
				if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
					String linkerURI = value.stringValue();
					Linker linker = linkerRepository.getLinkerFromURI(linkerURI, user);
					featureObject.setLinker(linker);
				}
			} else if (st.getPredicate().equals(hasLipid)) {
                Value value = st.getObject();
                if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
                    String linkerURI = value.stringValue();
                    Linker linker = linkerRepository.getLinkerFromURI(linkerURI, user);
                    if (featureObject instanceof GlycoLipid && linker != null && linker instanceof Lipid) 
                        ((GlycoLipid) featureObject).setLipid((Lipid) linker);
                }
            } else if (st.getPredicate().equals(hasPeptide)) {
                Value value = st.getObject();
                if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
                    String linkerURI = value.stringValue();
                    Linker linker = linkerRepository.getLinkerFromURI(linkerURI, user);
                    if (featureObject instanceof GlycoPeptide && linker != null && linker instanceof PeptideLinker) 
                        ((GlycoPeptide) featureObject).setPeptide((PeptideLinker) linker);
                    
                }
            } else if (st.getPredicate().equals(hasProtein)) {
                Value value = st.getObject();
                if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
                    String linkerURI = value.stringValue();
                    Linker linker = linkerRepository.getLinkerFromURI(linkerURI, user);
                    if (featureObject instanceof GlycoProtein && linker != null && linker instanceof ProteinLinker) 
                        ((GlycoProtein) featureObject).setProtein((ProteinLinker) linker);
                    else if (featureObject instanceof GPLinkedGlycoPeptide && linker != null && linker instanceof ProteinLinker) 
                        ((GPLinkedGlycoPeptide) featureObject).setProtein((ProteinLinker) linker);
                }
            } else if (legacy && st.getPredicate().equals(hasMolecule)) { 
                // handle the old way!!!
                Value value = st.getObject();
                if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
                    String glycanURI = value.stringValue();
                    if (featureObject.getType() == FeatureType.LINKEDGLYCAN) {
                        Glycan glycan = glycanRepository.getGlycanFromURI(glycanURI, user);
                        GlycanInFeature glycanFeature = new GlycanInFeature();
                        glycanFeature.setGlycan(glycan);
                        ((LinkedGlycan) featureObject).getGlycans().add(glycanFeature);
                    }
                }
            } else if (st.getPredicate().equals(hasGlycanContext)) {
				Value value = st.getObject();
				if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
					String contextURI = value.stringValue();
					IRI ctx = f.createIRI(contextURI);
	                RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(ctx, null, null, graphIRI);
	                GlycanInFeature glycanFeature = new GlycanInFeature();
	                glycanFeature.setUrls(new ArrayList<String>());
	                glycanFeature.setPublications(new ArrayList<Publication>());
	                while (statements2.hasNext()) {
	                    Statement st2 = statements2.next();
	                    if (st2.getPredicate().equals(hasMolecule)) {
	                        value = st2.getObject();
	                        String glycanURI = value.stringValue();
        					if (featureObject.getType() == FeatureType.LINKEDGLYCAN) {
        					    Glycan glycan = glycanRepository.getGlycanFromURI(glycanURI, user);
        					    glycanFeature.setGlycan(glycan);
        					    ((LinkedGlycan) featureObject).getGlycans().add(glycanFeature);
        					}
	                    } else if (st2.getPredicate().equals(hasSource)) {
                            value = st2.getObject();
                            IRI sourceIRI = f.createIRI(value.stringValue());
                            RepositoryResult<Statement> statements3 = sparqlDAO.getStatements(sourceIRI, hasType, null, graphIRI);
                            Source source = null;
                            // get the source type first
                            if (statements3.hasNext()) {
                                Statement st3 = statements3.next();
                                String type = st3.getObject().stringValue();
                                SourceType sourceType = SourceType.valueOf(type);
                                switch (sourceType) {
                                case COMMERCIAL:
                                    source = new CommercialSource();
                                    break;
                                case NONCOMMERCIAL:
                                    source = new NonCommercialSource();
                                    break;
                                case NOTRECORDED:
                                    source = new Source();
                                    break;
                                default:
                                    source = new Source();
                                    break;
                                }
                            }
                            
                            if (source != null) {
                                statements3 = sparqlDAO.getStatements(sourceIRI, null, null, graphIRI);
                                while (statements3.hasNext()) {
                                    Statement st3 = statements3.next();
                                    if (st3.getPredicate().equals(hasBatchId)) {
                                        source.setBatchId(st3.getObject().stringValue());
                                    } else if (st3.getPredicate().equals(hasVendor)) {
                                        if (source instanceof CommercialSource) 
                                            ((CommercialSource) source).setVendor(st3.getObject().stringValue());
                                    } else if (st3.getPredicate().equals(hasProviderLab)) {
                                        if (source instanceof NonCommercialSource) 
                                            ((NonCommercialSource) source).setProviderLab(st3.getObject().stringValue());
                                    } else if (st3.getPredicate().equals(hasCatalogNumber)) {
                                        if (source instanceof CommercialSource) 
                                            ((CommercialSource) source).setCatalogueNumber(st3.getObject().stringValue());
                                    } else if (st3.getPredicate().equals(hasMethod)) {
                                        if (source instanceof NonCommercialSource) 
                                            ((NonCommercialSource) source).setMethod(st3.getObject().stringValue());
                                    } else if (st3.getPredicate().equals(RDFS.COMMENT)) {
                                        if (source instanceof NonCommercialSource) 
                                            ((NonCommercialSource) source).setComment(st3.getObject().stringValue());
                                    } 
                                }
                                glycanFeature.setSource(source);
                            }
	                    } else if (st2.getPredicate().equals(hasReducingEndConfig)) {
                            value = st2.getObject();
                            IRI redEndIRI = f.createIRI(value.stringValue());
                            RepositoryResult<Statement> statements3 = sparqlDAO.getStatements(redEndIRI, null, null, graphIRI);
                            ReducingEndConfiguration config = new ReducingEndConfiguration();
                            while (statements3.hasNext()) {
                                Statement st3 = statements3.next();
                                if (st3.getPredicate().equals(hasType)) {
                                    try {
                                        config.setType(ReducingEndType.valueOf(st3.getObject().stringValue()));
                                    } catch (Exception e) {
                                        // enum constant is wrong in the repository
                                        logger.warn ("enumeration constant is incorrect" + e.getMessage());
                                        config.setType(ReducingEndType.UNKNOWN);
                                    }
                                } else if (st3.getPredicate().equals(RDFS.COMMENT)) {
                                    config.setComment(st3.getObject().stringValue());
                                }
                            }
                            glycanFeature.setReducingEndConfiguration(config);
	                    } else if (st2.getPredicate().equals(hasUrl)) {
	                        Value val = st2.getObject();
	                        if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
	                            glycanFeature.getUrls().add(val.stringValue());
	                        }
	                    } else if (st2.getPredicate().equals(hasPub)) {
	                        Value pub = st2.getObject();
	                        String pubURI = pub.stringValue();
	                        IRI p = f.createIRI(pubURI);
	                        Publication publication = new Publication();
	                        publication.setUri(pubURI);
	                        publication.setId(pubURI.substring(pubURI.lastIndexOf("/")+1));
	                        glycanFeature.getPublications().add(publication);
	                        RepositoryResult<Statement> statements3 = sparqlDAO.getStatements(p, null, null, graphIRI);
	                        while (statements3.hasNext()) {
	                            Statement st3 = statements2.next();
	                            if (st3.getPredicate().equals(hasTitle)) {
	                                Value val = st3.getObject();
	                                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
	                                    publication.setTitle(val.stringValue());
	                                }
	                            } else if (st3.getPredicate().equals(hasAuthor)) {
	                                Value val = st3.getObject();
	                                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
	                                    publication.setAuthors(val.stringValue());
	                                }
	                            } else if (st3.getPredicate().equals(hasYear)) {
	                                Value val = st3.getObject();
	                                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
	                                    publication.setYear(Integer.parseInt(val.stringValue()));
	                                }
	                            } else if (st3.getPredicate().equals(hasDOI)) {
	                                Value val = st3.getObject();
	                                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
	                                    publication.setDoiId(val.stringValue());
	                                }
	                            } else if (st3.getPredicate().equals(hasVolume)) {
	                                Value val = st3.getObject();
	                                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
	                                    publication.setVolume(val.stringValue());
	                                }
	                            } else if (st3.getPredicate().equals(hasJournal)) {
	                                Value val = st3.getObject();
	                                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
	                                    publication.setJournal(val.stringValue());
	                                }
	                            } else if (st3.getPredicate().equals(hasNumber)) {
	                                Value val = st3.getObject();
	                                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
	                                    publication.setNumber(val.stringValue());
	                                }
	                            } else if (st3.getPredicate().equals(hasStartPage)) {
	                                Value val = st3.getObject();
	                                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
	                                    publication.setStartPage(val.stringValue());
	                                }
	                            } else if (st3.getPredicate().equals(hasEndPage)) {
	                                Value val = st3.getObject();
	                                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
	                                    publication.setEndPage(val.stringValue());
	                                }
	                            } else if (st3.getPredicate().equals(hasPubMed)) {
	                                Value val = st3.getObject();
	                                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
	                                    publication.setPubmedId(Integer.parseInt(val.stringValue()));
	                                }
	                            } 
	                        }
	                    }
	                }
				}
			} else if (st.getPredicate().equals(hasLinkedGlycan)) {
                Value value = st.getObject();
                if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
                    String glycanURI = value.stringValue();
                    if (featureObject.getType() == FeatureType.GLYCOLIPID) {
                        Feature linkedGlycan = getFeatureFromURI(glycanURI, user);
                        if (linkedGlycan != null && linkedGlycan instanceof LinkedGlycan)
                            ((GlycoLipid) featureObject).getGlycans().add((LinkedGlycan) linkedGlycan);
                    } else if (featureObject.getType() == FeatureType.GLYCOPEPTIDE) {
                        Feature linkedGlycan = getFeatureFromURI(glycanURI, user);
                        if (linkedGlycan != null && linkedGlycan instanceof LinkedGlycan)
                            ((GlycoPeptide) featureObject).getGlycans().add((LinkedGlycan) linkedGlycan);
                    } else if (featureObject.getType() == FeatureType.GLYCOPROTEIN) {
                        Feature linkedGlycan = getFeatureFromURI(glycanURI, user);
                        if (linkedGlycan != null && linkedGlycan instanceof LinkedGlycan)
                            ((GlycoProtein) featureObject).getGlycans().add((LinkedGlycan) linkedGlycan);
                    }
                }
            } else if (st.getPredicate().equals(hasGlycoPeptide)) {
                Value value = st.getObject();
                if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
                    String peptideURI = value.stringValue();
                    if (featureObject instanceof GPLinkedGlycoPeptide) {
                        Feature glycoPeptide = getFeatureFromURI(peptideURI, user);
                        if (glycoPeptide != null && glycoPeptide instanceof GlycoPeptide)
                            ((GPLinkedGlycoPeptide) featureObject).getPeptides().add((GlycoPeptide) glycoPeptide);
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
				String moleculeInContextURI = null;
				Range range = new Range();
				while (statements2.hasNext()) {
					Statement st2 = statements2.next();
					if (st2.getPredicate().equals(hasPosition)) {
						Value value = st2.getObject();
						if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
						    try {
						        position = Integer.parseInt(value.stringValue());
						    } catch (NumberFormatException e) {
						        logger.error("glycan position is invalid", e);
						    }
						}	
					} else if (st2.getPredicate().equals(hasMolecule)) {
						Value val = st2.getObject();
						moleculeInContextURI = val.stringValue();
						
					} else if (st2.getPredicate().equals(hasMax)) {
					    Value val = st2.getObject();
					    try {
					        Integer max = Integer.parseInt(val.stringValue());
					        range.setMax(max);
					    } catch (NumberFormatException e) {
					        logger.error("glycan range is invalid");
					    }
                    } else if (st2.getPredicate().equals(hasMin)) {
                        Value val = st2.getObject();
                        try {
                            Integer min = Integer.parseInt(val.stringValue());
                            range.setMin(min);
                        } catch (NumberFormatException e) {
                            logger.error("glycan range is invalid");
                        }
                    }
				}
				if (position != null && moleculeInContextURI != null) {
					positionMap.put (position +"", moleculeInContextURI.substring(moleculeInContextURI.lastIndexOf("/")+1));
				}
				if (range.getMax() != null && moleculeInContextURI != null) {
				    rangeMap.put(moleculeInContextURI, range);
				}
			} else if (st.getPredicate().equals(hasFeatureMetadata)) {
                Value uriValue = st.getObject();
                featureObject.setMetadata((FeatureMetadata) metadataRepository.getMetadataCategoryFromURI(uriValue.stringValue(), featureMetadataTypePredicate, true, user));
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
                    } else if (stPublic.getPredicate().equals(hasLipid)) {
                        value = stPublic.getObject();
                        if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
                            String linkerURI = value.stringValue();
                            Linker linker = linkerRepository.getLinkerFromURI(linkerURI, user);
                            if (featureObject instanceof GlycoLipid && linker != null && linker instanceof Lipid) 
                                ((GlycoLipid) featureObject).setLipid((Lipid) linker);
                        }
                    } else if (stPublic.getPredicate().equals(hasPeptide)) {
                        value = stPublic.getObject();
                        if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
                            String linkerURI = value.stringValue();
                            Linker linker = linkerRepository.getLinkerFromURI(linkerURI, user);
                            if (featureObject instanceof GlycoPeptide && linker != null && linker instanceof PeptideLinker) 
                                ((GlycoPeptide) featureObject).setPeptide((PeptideLinker) linker);
                        }
                    } else if (stPublic.getPredicate().equals(hasProtein)) {
                        value = stPublic.getObject();
                        if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
                            String linkerURI = value.stringValue();
                            Linker linker = linkerRepository.getLinkerFromURI(linkerURI, user);
                            if (featureObject instanceof GlycoProtein && linker != null && linker instanceof ProteinLinker) 
                                ((GlycoProtein) featureObject).setProtein((ProteinLinker) linker);
                            else if (featureObject instanceof GPLinkedGlycoPeptide && linker != null && linker instanceof ProteinLinker) 
                                ((GPLinkedGlycoPeptide) featureObject).setProtein((ProteinLinker) linker);
                        }
                    } else if (stPublic.getPredicate().equals(hasGlycanContext)) {
                        value = stPublic.getObject();
                        if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
                            String contextURI = value.stringValue();
                            IRI ctx = f.createIRI(contextURI);
                            RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(ctx, null, null, defaultGraphIRI);
                            GlycanInFeature glycanFeature = new GlycanInFeature();
                            while (statements2.hasNext()) {
                                Statement st2 = statements2.next();
                                if (st2.getPredicate().equals(hasMolecule)) {
                                    value = st2.getObject();
                                    String glycanURI = value.stringValue();
                                    if (featureObject.getType() == FeatureType.LINKEDGLYCAN) {
                                        Glycan glycan = glycanRepository.getGlycanFromURI(glycanURI, user);
                                        glycanFeature.setGlycan(glycan);
                                        ((LinkedGlycan) featureObject).getGlycans().add(glycanFeature);
                                    }
                                } else if (st2.getPredicate().equals(hasSource)) {
                                    value = st2.getObject();
                                    IRI sourceIRI = f.createIRI(value.stringValue());
                                    RepositoryResult<Statement> statements3 = sparqlDAO.getStatements(sourceIRI, hasType, null, defaultGraphIRI);
                                    Source source = null;
                                    // get the source type first
                                    if (statements3.hasNext()) {
                                        Statement st3 = statements3.next();
                                        String type = st3.getObject().stringValue();
                                        SourceType sourceType = SourceType.valueOf(type);
                                        switch (sourceType) {
                                        case COMMERCIAL:
                                            source = new CommercialSource();
                                            break;
                                        case NONCOMMERCIAL:
                                            source = new NonCommercialSource();
                                            break;
                                        case NOTRECORDED:
                                            source = new Source();
                                            break;
                                        default:
                                            source = new Source();
                                            break;
                                        }
                                    }
                                    
                                    if (source != null) {
                                        statements3 = sparqlDAO.getStatements(sourceIRI, null, null, defaultGraphIRI);
                                        while (statements3.hasNext()) {
                                            Statement st3 = statements3.next();
                                            if (st3.getPredicate().equals(hasBatchId)) {
                                                source.setBatchId(st3.getObject().stringValue());
                                            } else if (st3.getPredicate().equals(hasVendor)) {
                                                if (source instanceof CommercialSource) 
                                                    ((CommercialSource) source).setVendor(st3.getObject().stringValue());
                                            } else if (st3.getPredicate().equals(hasProviderLab)) {
                                                if (source instanceof NonCommercialSource) 
                                                    ((NonCommercialSource) source).setProviderLab(st3.getObject().stringValue());
                                            } else if (st3.getPredicate().equals(hasCatalogNumber)) {
                                                if (source instanceof CommercialSource) 
                                                    ((CommercialSource) source).setCatalogueNumber(st3.getObject().stringValue());
                                            } else if (st3.getPredicate().equals(hasMethod)) {
                                                if (source instanceof NonCommercialSource) 
                                                    ((NonCommercialSource) source).setMethod(st3.getObject().stringValue());
                                            } else if (st3.getPredicate().equals(RDFS.COMMENT)) {
                                                if (source instanceof NonCommercialSource) 
                                                    ((NonCommercialSource) source).setComment(st3.getObject().stringValue());
                                            } 
                                        }
                                        glycanFeature.setSource(source);
                                    }
                                } else if (st2.getPredicate().equals(hasReducingEndConfig)) {
                                    value = st2.getObject();
                                    IRI redEndIRI = f.createIRI(value.stringValue());
                                    RepositoryResult<Statement> statements3 = sparqlDAO.getStatements(redEndIRI, null, null, defaultGraphIRI);
                                    ReducingEndConfiguration config = new ReducingEndConfiguration();
                                    while (statements3.hasNext()) {
                                        Statement st3 = statements3.next();
                                        if (st3.getPredicate().equals(hasType)) {
                                            config.setType(ReducingEndType.valueOf(st3.getObject().stringValue()));
                                        } else if (st3.getPredicate().equals(RDFS.COMMENT)) {
                                            config.setComment(st3.getObject().stringValue());
                                        }
                                    }
                                    glycanFeature.setReducingEndConfiguration(config);
                                } else if (st2.getPredicate().equals(hasUrl)) {
                                    Value val = st2.getObject();
                                    if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                                        glycanFeature.getUrls().add(val.stringValue());
                                    }
                                } else if (st2.getPredicate().equals(hasPub)) {
                                    Value pub = st2.getObject();
                                    String pubURI = pub.stringValue();
                                    IRI p = f.createIRI(pubURI);
                                    Publication publication = new Publication();
                                    publication.setUri(pubURI);
                                    publication.setId(pubURI.substring(pubURI.lastIndexOf("/")+1));
                                    glycanFeature.getPublications().add(publication);
                                    RepositoryResult<Statement> statements3 = sparqlDAO.getStatements(p, null, null, graphIRI);
                                    while (statements3.hasNext()) {
                                        Statement st3 = statements2.next();
                                        if (st3.getPredicate().equals(hasTitle)) {
                                            Value val = st3.getObject();
                                            if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                                                publication.setTitle(val.stringValue());
                                            }
                                        } else if (st3.getPredicate().equals(hasAuthor)) {
                                            Value val = st3.getObject();
                                            if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                                                publication.setAuthors(val.stringValue());
                                            }
                                        } else if (st3.getPredicate().equals(hasYear)) {
                                            Value val = st3.getObject();
                                            if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                                                publication.setYear(Integer.parseInt(val.stringValue()));
                                            }
                                        } else if (st3.getPredicate().equals(hasDOI)) {
                                            Value val = st3.getObject();
                                            if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                                                publication.setDoiId(val.stringValue());
                                            }
                                        } else if (st3.getPredicate().equals(hasVolume)) {
                                            Value val = st3.getObject();
                                            if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                                                publication.setVolume(val.stringValue());
                                            }
                                        } else if (st3.getPredicate().equals(hasJournal)) {
                                            Value val = st3.getObject();
                                            if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                                                publication.setJournal(val.stringValue());
                                            }
                                        } else if (st3.getPredicate().equals(hasNumber)) {
                                            Value val = st3.getObject();
                                            if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                                                publication.setNumber(val.stringValue());
                                            }
                                        } else if (st3.getPredicate().equals(hasStartPage)) {
                                            Value val = st3.getObject();
                                            if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                                                publication.setStartPage(val.stringValue());
                                            }
                                        } else if (st3.getPredicate().equals(hasEndPage)) {
                                            Value val = st3.getObject();
                                            if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                                                publication.setEndPage(val.stringValue());
                                            }
                                        } else if (st3.getPredicate().equals(hasPubMed)) {
                                            Value val = st3.getObject();
                                            if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                                                publication.setPubmedId(Integer.parseInt(val.stringValue()));
                                            }
                                        } 
                                    }
                                }
                            }
                        }
                    } else if (stPublic.getPredicate().equals(hasLinkedGlycan)) {
                        value = stPublic.getObject();
                        if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
                            String glycanURI = value.stringValue();
                            if (featureObject.getType() == FeatureType.GLYCOLIPID) {
                                Feature linkedGlycan = getFeatureFromURI(glycanURI, user);
                                if (linkedGlycan != null && linkedGlycan instanceof LinkedGlycan)
                                    ((GlycoLipid) featureObject).getGlycans().add((LinkedGlycan) linkedGlycan);
                            } else if (featureObject.getType() == FeatureType.GLYCOPEPTIDE) {
                                Feature linkedGlycan = getFeatureFromURI(glycanURI, user);
                                if (linkedGlycan != null && linkedGlycan instanceof LinkedGlycan)
                                    ((GlycoPeptide) featureObject).getGlycans().add((LinkedGlycan) linkedGlycan);
                            } else if (featureObject.getType() == FeatureType.GLYCOPROTEIN) {
                                Feature linkedGlycan = getFeatureFromURI(glycanURI, user);
                                if (linkedGlycan != null && linkedGlycan instanceof LinkedGlycan)
                                    ((GlycoProtein) featureObject).getGlycans().add((LinkedGlycan) linkedGlycan);
                            }
                        }
                    } else if (stPublic.getPredicate().equals(hasGlycoPeptide)) {
                        value = stPublic.getObject();
                        if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
                            String peptideURI = value.stringValue();
                            if (featureObject instanceof GPLinkedGlycoPeptide) {
                                Feature glycoPeptide = getFeatureFromURI(peptideURI, user);
                                if (glycoPeptide != null && glycoPeptide instanceof GlycoPeptide)
                                    ((GPLinkedGlycoPeptide) featureObject).getPeptides().add((GlycoPeptide) glycoPeptide);
                            }
                        }
                    } else if (stPublic.getPredicate().equals(hasPositionContext)) {
                        Value positionContext = stPublic.getObject();
                        String contextURI = positionContext.stringValue();
                        IRI ctx = f.createIRI(contextURI);
                        RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(ctx, null, null, defaultGraphIRI);
                        Integer position = null;
                        Range range = new Range();
                        String moleculeInContextURI = null;
                        while (statements2.hasNext()) {
                            Statement st2 = statements2.next();
                            if (st2.getPredicate().equals(hasPosition)) {
                                value = st2.getObject();
                                if (value != null && value.stringValue() != null && !value.stringValue().isEmpty()) {
                                    try {
                                        position = Integer.parseInt(value.stringValue());
                                    } catch (NumberFormatException e) {
                                        logger.error("glycan position is invalid", e);
                                    }
                                }   
                            } else if (st2.getPredicate().equals(hasMolecule)) {
                                Value val = st2.getObject();
                                moleculeInContextURI = val.stringValue();
                            } else if (st2.getPredicate().equals(hasMax)) {
                                Value val = st2.getObject();
                                try {
                                    Integer max = Integer.parseInt(val.stringValue());
                                    range.setMax(max);
                                } catch (NumberFormatException e) {
                                    logger.error("glycan range is invalid");
                                }
                            } else if (st2.getPredicate().equals(hasMin)) {
                                Value val = st2.getObject();
                                try {
                                    Integer min = Integer.parseInt(val.stringValue());
                                    range.setMin(min);
                                } catch (NumberFormatException e) {
                                    logger.error("glycan range is invalid");
                                }
                            }
                        }
                        if (position != null && moleculeInContextURI != null) {
                            positionMap.put (position +"", moleculeInContextURI.substring(moleculeInContextURI.lastIndexOf("/")+1));
                        }
                        if (range.getMax() != null && moleculeInContextURI != null) {
                            rangeMap.put(moleculeInContextURI, range);
                        }
                    } else if (stPublic.getPredicate().equals(hasFeatureMetadata)) {
                        Value uriValue = stPublic.getObject();
                        featureObject.setMetadata((FeatureMetadata) metadataRepository.getMetadataCategoryFromURI(uriValue.stringValue(), featureMetadataTypePredicate, true, null));
                    }
                }
			}
			//logger.info("done");
		}
		
		// for features with optional ranges, go through their lists and update the ranges
		if (featureObject instanceof GlycoProtein) {
		    List<LinkedGlycan> glycans = ((GlycoProtein) featureObject).getGlycans();
		    for (LinkedGlycan g: glycans) {
		        Range r = rangeMap.get(g.getUri());
		        g.setRange(r);
		    }
		} else if (featureObject instanceof GPLinkedGlycoPeptide) {
            List<GlycoPeptide> peptides = ((GPLinkedGlycoPeptide) featureObject).getPeptides();
            for (GlycoPeptide g: peptides) {
                Range r = rangeMap.get(g.getUri());
                g.setRange(r);
            }
        }
		
		// for the private graph retrievals, only keep the non-public ones
		if (user != null && !graph.equals(DEFAULT_GRAPH) && featureObject != null) {
		    List<LinkedGlycan> finalGlycans;
		    
            switch (featureObject.getType()) {
            case GLYCOLIPID:
                finalGlycans = new ArrayList<LinkedGlycan>();
                for (LinkedGlycan glycan: ((GlycoLipid) featureObject).getGlycans()) {
                    if (glycan.getUri().contains("public"))
                         continue;
                    finalGlycans.add(glycan);
                }
                ((GlycoLipid) featureObject).setGlycans(finalGlycans);
                break;
            case GLYCOPEPTIDE:
                finalGlycans = new ArrayList<LinkedGlycan>();
                for (LinkedGlycan glycan: ((GlycoPeptide) featureObject).getGlycans()) {
                    if (glycan.getUri().contains("public"))
                         continue;
                    finalGlycans.add(glycan);
                }
                ((GlycoPeptide) featureObject).setGlycans(finalGlycans);
                break;
            case GLYCOPROTEIN:
                finalGlycans = new ArrayList<LinkedGlycan>();
                for (LinkedGlycan glycan: ((GlycoProtein) featureObject).getGlycans()) {
                    if (glycan.getUri().contains("public"))
                         continue;
                    finalGlycans.add(glycan);
                }
                ((GlycoProtein) featureObject).setGlycans(finalGlycans);
                break;
            case GPLINKEDGLYCOPEPTIDE:
                ArrayList<GlycoPeptide> finalPeptides = new ArrayList<GlycoPeptide>();
                for (GlycoPeptide glycan: ((GPLinkedGlycoPeptide) featureObject).getPeptides()) {
                    if (glycan.getUri().contains("public"))
                         continue;
                    finalPeptides.add(glycan);
                }
                ((GPLinkedGlycoPeptide) featureObject).setPeptides(finalPeptides);
                break;
            case LINKEDGLYCAN:
                ArrayList<GlycanInFeature> finalGlycans2 = new ArrayList<GlycanInFeature>();
                for (GlycanInFeature glycanF: ((LinkedGlycan) featureObject).getGlycans()) {
                    Glycan glycan = glycanF.getGlycan();
                    if (glycan.getUri().contains("public"))
                         continue;
                    finalGlycans2.add(glycanF);
                }
                ((LinkedGlycan) featureObject).setGlycans(finalGlycans2);
                break;
            default:
                break;
		    
		    }
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
		IRI hasFeatureMetadata = f.createIRI(featureMetadataPredicate);
		IRI hasLinkedGlycan = f.createIRI(hasLinkedGlycanPredicate);
        IRI hasGlycoPeptide = f.createIRI(hasGlycoPeptidePredicate);
        IRI hasLipid = f.createIRI(hasLipidPredicate);
        IRI hasProtein = f.createIRI(hasProteinPredicate);
        IRI hasPeptide = f.createIRI(hasPeptidePredicate);
        IRI hasMax = f.createIRI(hasMaxPredicate);
        IRI hasMin = f.createIRI(hasMinPredicate);
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
		
		switch (feature.getType()) {
        case LINKEDGLYCAN:
            if (((LinkedGlycan) feature).getGlycans() != null) {
                for (GlycanInFeature gf: ((LinkedGlycan) feature).getGlycans()) {
                    Glycan g = gf.getGlycan();
                    if (g.getUri() == null) {
                        if (g.getId() != null) {
                            g.setUri(uriPrefix + g.getId());
                        } else {
                            throw new SparqlException ("No enough information is provided to add the feature, glycan " + g.getName() + " cannot be found!");

                        }
                    }
                    
                    IRI glycanIRI = f.createIRI(g.getUri());
                    statements.add(f.createStatement(feat, hasMolecule, glycanIRI, graphIRI));
                    //add source info etc. 
                    addSourceContext (gf, feat, glycanIRI, statements, DEFAULT_GRAPH);
                }
            }
            break;
        case GLYCOLIPID:
            if (((GlycoLipid)feature).getGlycans() != null) {
                for (LinkedGlycan g: ((GlycoLipid)feature).getGlycans()) {
                    if (g.getId() != null) {
                        g.setUri(uriPrefix + g.getId());
                    } else {
                        throw new SparqlException ("No enough information is provided to add the feature, glycan " + g.getName() + " cannot be found!");

                    }
                    
                    IRI glycanIRI = f.createIRI(g.getUri());
                    statements.add(f.createStatement(feat, hasLinkedGlycan, glycanIRI, graphIRI));
                }
            }
            if (((GlycoLipid)feature).getLipid() == null) {
                throw new SparqlException ("No enough information is provided to add the feature, lipid should be provided");
            } else {
                Lipid lipid = ((GlycoLipid)feature).getLipid();
                if (lipid.getUri() == null) {
                    if (lipid.getId() != null) {
                        lipid.setUri(uriPrefix + lipid.getId());
                    } else {
                        throw new SparqlException ("No enough information is provided to add the feature, lipid " + lipid.getName() + " cannot be found!");

                    }
                    
                    IRI lipidIRI = f.createIRI(lipid.getUri());
                    statements.add(f.createStatement(feat, hasLipid, lipidIRI, graphIRI));
                }
            }
            break;
        case GLYCOPEPTIDE:
            if (((GlycoPeptide)feature).getGlycans() != null) {
                for (LinkedGlycan g: ((GlycoPeptide)feature).getGlycans()) {
                    if (g.getId() != null) {
                        g.setUri(uriPrefix + g.getId());
                    } else {
                        throw new SparqlException ("No enough information is provided to add the feature, glycan " + g.getName() + " cannot be found!");

                    }
                    
                    IRI glycanIRI = f.createIRI(g.getUri());
                    statements.add(f.createStatement(feat, hasLinkedGlycan, glycanIRI, graphIRI));
                }
            }
            if (((GlycoPeptide)feature).getPeptide() == null) {
                throw new SparqlException ("No enough information is provided to add the feature, peptide should be provided");
            } else {
                PeptideLinker pl = ((GlycoPeptide)feature).getPeptide();
                if (pl.getUri() == null) {
                    if (pl.getId() != null) {
                        pl.setUri(uriPrefix + pl.getId());
                    } else {
                        throw new SparqlException ("No enough information is provided to add the feature, peptide " + pl.getName() + " cannot be found!");

                    }
                    
                    IRI peptideIRI = f.createIRI(pl.getUri());
                    statements.add(f.createStatement(feat, hasPeptide, peptideIRI, graphIRI));
                }
            }
            break;
        case GLYCOPROTEIN:
            if (((GlycoProtein)feature).getGlycans() != null) {
                for (LinkedGlycan g: ((GlycoProtein)feature).getGlycans()) {
                    if (g.getId() != null) {
                        g.setUri(uriPrefix + g.getId());
                    } else {
                        throw new SparqlException ("No enough information is provided to add the feature, glycan " + g.getName() + " cannot be found!");

                    }
                    
                    IRI glycanIRI = f.createIRI(g.getUri());
                    statements.add(f.createStatement(feat, hasLinkedGlycan, glycanIRI, graphIRI));
                }
            }
            if (((GlycoProtein)feature).getProtein() == null) {
                throw new SparqlException ("No enough information is provided to add the feature, protein should be provided");
            } else {
                ProteinLinker pl = ((GlycoProtein)feature).getProtein();
                if (pl.getUri() == null) {
                    if (pl.getId() != null) {
                        pl.setUri(uriPrefix + pl.getId());
                    } else {
                        throw new SparqlException ("No enough information is provided to add the feature, protein " + pl.getName() + " cannot be found!");

                    }
                    
                    IRI proteinIRI = f.createIRI(pl.getUri());
                    statements.add(f.createStatement(feat, hasProtein, proteinIRI, graphIRI));
                }
            }
            break;
        case GPLINKEDGLYCOPEPTIDE:
            if (((GPLinkedGlycoPeptide)feature).getPeptides() != null) {
                for (GlycoPeptide g: ((GPLinkedGlycoPeptide)feature).getPeptides()) {
                    if (g.getId() != null) {
                        g.setUri(uriPrefix + g.getId());
                    } else {
                        throw new SparqlException ("No enough information is provided to add the feature, glycopeptide " + g.getName() + " cannot be found!");

                    }
                    
                    IRI glycoPeptideIRI = f.createIRI(g.getUri());
                    statements.add(f.createStatement(feat, hasGlycoPeptide, glycoPeptideIRI, graphIRI));
                }
            }
            if (((GPLinkedGlycoPeptide)feature).getProtein() == null) {
                throw new SparqlException ("No enough information is provided to add the feature, protein should be provided");
            } else {
                ProteinLinker pl = ((GPLinkedGlycoPeptide)feature).getProtein();
                if (pl.getUri() == null) {
                    if (pl.getId() != null) {
                        pl.setUri(uriPrefix + pl.getId());
                    } else {
                        throw new SparqlException ("No enough information is provided to add the feature, protein " + pl.getName() + " cannot be found!");

                    }
                    
                    IRI proteinIRI = f.createIRI(pl.getUri());
                    statements.add(f.createStatement(feat, hasProtein, proteinIRI, graphIRI));
                }
            }
            break;
        
        default:
            break;
        
        }
		
		String positionContextURI = generateUniqueURI(uriPrefixPublic + "PC");
        IRI positionContext = f.createIRI(positionContextURI);
		if (feature.getPositionMap() != null) {
			for (String position: feature.getPositionMap().keySet()) {
			    // need to check if the position is valid
			    try {
			        Integer.parseInt(position);
			    } catch (NumberFormatException e) {
			        logger.info("got invalid position for the feature's glycans", e);
			        continue;
			    }
				String glycanId = feature.getPositionMap().get(position);
				IRI glycanIRI = f.createIRI(uriPrefixPublic + glycanId);
				Literal pos = f.createLiteral(position);
				
				statements.add(f.createStatement(feat, hasPositionContext, positionContext, graphIRI));
				statements.add(f.createStatement(positionContext, hasMolecule, glycanIRI, graphIRI));
				statements.add(f.createStatement(positionContext, hasPosition, pos, graphIRI));
			}
		}
		
		Range range = null;
        if (feature.getType() == FeatureType.LINKEDGLYCAN) {
            range = ((LinkedGlycan) feature).getRange();
        } else if (feature.getType() == FeatureType.GLYCOPEPTIDE) {
            range = ((GlycoPeptide) feature).getRange();
        }
        if (range != null) {
            IRI max = f.createIRI(range.getMax()+"");
            IRI min = f.createIRI(range.getMin()+"");
            statements.add(f.createStatement(positionContext, hasMax, max, graphIRI));
            statements.add(f.createStatement(positionContext, hasMin, min, graphIRI));
        }
		
		if (feature.getMetadata() != null) {
		    if (feature.getMetadata().getUri() != null) {
                statements.add(f.createStatement(feat, hasFeatureMetadata, f.createIRI(feature.getMetadata().getUri()), graphIRI));
            } else {
                String metadataURI = metadataRepository.addMetadataCategory(feature.getMetadata(), MetadataTemplateType.FEATURE, featureMetadataPredicate, featureMetadataTypePredicate, "FM", null);
                statements.add(f.createStatement(feat, hasFeatureMetadata, f.createIRI(metadataURI), graphIRI));    
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
        
        boolean glycanPublic = glycan.getUri() != null ? glycan.getUri().contains("public") : false;
        boolean linkerPublic = linker.getUri() != null ? linker.getUri().contains("public") : false;
        boolean slideLayoutPublic = slideLayoutURI != null ? slideLayoutURI.contains("public") : false;
        boolean blockLayoutPublic = blockLayoutURI != null ? blockLayoutURI.contains("public") : false;
        
        String fromString = "FROM <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n";
        String whereClause = "WHERE {";
        String where = " { " + 
                "                   ?f gadr:has_molecule <" + glycan.getUri() +"> . \n" +
                "                   ?f gadr:has_linker <" + linker.getUri() + "> . \n";
        
        if (blockLayoutURI != null) {
            where +=  "<" + blockLayoutURI + "> template:has_spot ?s . ?s gadr:has_feature ?f . \n";
            
        } else if (slideLayoutURI != null) {
            where += "<" + slideLayoutURI + "> gadr:has_block ?b . ?b template:has_block_layout ?bl . "
                    + "?bl template:has_spot ?s .  ?s gadr:has_feature ?f . \n";
        }
        
        if (!graph.equals(GlygenArrayRepository.DEFAULT_GRAPH)) {
            // check if the user's private graph has this glycan
            fromString += "FROM <" + graph + ">\n";
            where += "              ?f gadr:has_date_addedtolibrary ?d .\n }";
            where += "  UNION { ?f gadr:has_date_addedtolibrary ?d .\n";
            if (blockLayoutURI != null) {
                if (blockLayoutPublic) {
                    if (glycanPublic) {
                        where +=  "<" + blockLayoutURI + "> template:has_spot ?s . ?s gadr:has_feature ?pf . \n"
                                + " ?f gadr:has_public_uri ?pf . ?pf gadr:has_molecule <" + glycan.getUri() +"> . ";
                        if (linkerPublic) {
                            where += " ?pf gadr:has_linker <" + linker.getUri() + "> . \n";
                        } else {
                            where += " ?f gadr:has_linker <" + linker.getUri() + "> . \n";
                        }
                    } else {
                        where +=  "<" + blockLayoutURI + "> template:has_spot ?s . ?s gadr:has_feature ?pf . \n"
                                + " ?f gadr:has_public_uri ?pf . ?f gadr:has_molecule <" + glycan.getUri() +"> . ";
                        if (linkerPublic) {
                            where += " ?pf gadr:has_linker <" + linker.getUri() + "> . \n";
                        } else {
                            where += " ?f gadr:has_linker <" + linker.getUri() + "> . \n";
                        } 
                    }
                } else {
                    where +=  "<" + blockLayoutURI + "> template:has_spot ?s . ?s gadr:has_feature ?f . \n";
                    if (glycanPublic) {
                        where += " ?f gadr:has_public_uri ?pf . ?pf gadr:has_molecule <" + glycan.getUri() +"> . ";
                    } else {
                        where += " ?f gadr:has_molecule <" + glycan.getUri() +"> . ";
                    }
                    if (linkerPublic) {
                        where += " ?f gadr:has_public_uri ?pf . ?pf gadr:has_linker <\" + linker.getUri() + \"> . \n";
                    } else {
                        where += " ?f gadr:has_linker <\" + linker.getUri() + \"> . \n";
                    }
                }
            } else if (slideLayoutURI != null) {
                if (slideLayoutPublic) {
                    where += "<" + slideLayoutURI + "> gadr:has_block ?b . ?b template:has_block_layout ?bl . ?bl template:has_spot ?s .  ?s gadr:has_feature ?pf . \n";
                    if (glycanPublic) {
                        where += " ?f gadr:has_public_uri ?pf . ?pf gadr:has_molecule <" + glycan.getUri() +"> . ";
                        if (linkerPublic) {
                            where += " ?pf gadr:has_linker <" + linker.getUri() + "> . \n";
                        } else {
                            where += " ?f gadr:has_linker <" + linker.getUri() + "> . \n";
                        }
                    } else {
                        where += " ?f gadr:has_public_uri ?pf . ?f gadr:has_molecule <" + glycan.getUri() +"> . ";
                        if (linkerPublic) {
                            where += " ?pf gadr:has_linker <" + linker.getUri() + "> . \n";
                        } else {
                            where += " ?f gadr:has_linker <" + linker.getUri() + "> . \n";
                        } 
                    }
                } else {
                    where += "<" + slideLayoutURI + "> gadr:has_block ?b . ?b template:has_block_layout ?bl . ?bl template:has_spot ?s .  ?s gadr:has_feature ?f . \n";
                    if (glycanPublic) {
                        where += " ?f gadr:has_public_uri ?pf . ?pf gadr:has_molecule <" + glycan.getUri() +"> . ";
                    } else {
                        where += " ?f gadr:has_molecule <" + glycan.getUri() +"> . ";
                    }
                    if (linkerPublic) {
                        where += " ?f gadr:has_public_uri ?pf . ?pf gadr:has_linker <\" + linker.getUri() + \"> . \n";
                    } else {
                        where += " ?f gadr:has_linker <\" + linker.getUri() + \"> . \n";
                    }
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
        if (results.isEmpty()) {
            logger.warn ("query: " + queryBuf.toString() + " returned 0 results!");
            return null;
        }
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
