package org.glygen.array.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityNotFoundException;
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
import org.glygen.array.persistence.rdf.metadata.AssayMetadata;
import org.glygen.array.persistence.rdf.metadata.DataProcessingSoftware;
import org.glygen.array.persistence.rdf.metadata.Description;
import org.glygen.array.persistence.rdf.metadata.Descriptor;
import org.glygen.array.persistence.rdf.metadata.DescriptorGroup;
import org.glygen.array.persistence.rdf.metadata.FeatureMetadata;
import org.glygen.array.persistence.rdf.metadata.ImageAnalysisSoftware;
import org.glygen.array.persistence.rdf.metadata.MetadataCategory;
import org.glygen.array.persistence.rdf.metadata.PrintRun;
import org.glygen.array.persistence.rdf.metadata.Printer;
import org.glygen.array.persistence.rdf.metadata.Sample;
import org.glygen.array.persistence.rdf.metadata.ScannerMetadata;
import org.glygen.array.persistence.rdf.metadata.SlideMetadata;
import org.glygen.array.persistence.rdf.metadata.SpotMetadata;
import org.glygen.array.persistence.rdf.template.DescriptionTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(value="sesameTransactionManager", rollbackFor = SparqlException.class) 
public class MetadataRepositoryImpl extends GlygenArrayRepositoryImpl implements MetadataRepository {
    
    @Autowired
    MetadataTemplateRepository templateRepository;
    
    Map<String, MetadataCategory> metadataCache = new HashMap<String, MetadataCategory>();
    
    @Override
    public String addAssayMetadata(AssayMetadata metadata, UserEntity user) throws SparqlException, SQLException {
        return addMetadataCategory (metadata, MetadataTemplateType.ASSAY, hasAssayTemplatePredicate, assayTypePredicate, "A", user);
    }


    @Override
    public String addDataProcessingSoftware(DataProcessingSoftware metadata, UserEntity user)
            throws SparqlException, SQLException {
        return addMetadataCategory (metadata, MetadataTemplateType.DATAPROCESSINGSOFTWARE, hasDataprocessingTemplatePredicate, dataProcessingTypePredicate, "DPS", user);
    }


    private String addDescriptor (Descriptor descriptor, List<Statement> statements, String graph) throws SparqlException, SQLException {
        ValueFactory f = sparqlDAO.getValueFactory();
        String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
        IRI graphIRI = f.createIRI(graph);
        String uriPre = uriPrefix;
        if (graph.equals (DEFAULT_GRAPH)) {
            uriPre = uriPrefixPublic;
        }
        String descrURI = generateUniqueURI(uriPre + "D", allGraphs);
        IRI descr = f.createIRI(descrURI);
        IRI hasValue = f.createIRI(valuePredicate);
        IRI hasKey = f.createIRI(keyPredicate);
        IRI hasUnit = f.createIRI(unitPredicate);
        IRI hasOrder = f.createIRI(orderPredicate);
        IRI notRecorded = f.createIRI(notRecordedPredicate);
        IRI notApplicable = f.createIRI(notApplicablePredicate);
        
        Literal unit = descriptor.getUnit() == null ? null : f.createLiteral(descriptor.getUnit());
        Literal order = descriptor.getOrder() == null || descriptor.getOrder() == -1 ? null : f.createLiteral(descriptor.getOrder());
        Literal notRec = descriptor.getNotRecorded() == null ? f.createLiteral(false) : f.createLiteral(descriptor.getNotRecorded());
        Literal notApp = descriptor.getNotApplicable() == null ? f.createLiteral(false) : f.createLiteral(descriptor.getNotApplicable());
        
        if (descriptor.getKey() != null && descriptor.getKey().getUri() == null) {
            // try to get the uri from id
            if (descriptor.getKey().getId() != null) {
                descriptor.getKey().setUri(MetadataTemplateRepository.templatePrefix + descriptor.getKey().getId()) ;
            } else {
                throw new SparqlException ("Descriptor template info is missing for descriptor " + descriptor.getName());
            }
        } else if (descriptor.getKey() == null) {
            throw new SparqlException ("Descriptor template info is missing for descriptor " + descriptor.getName());
        }
        // check if template actually exists!
        DescriptionTemplate descTemplate = templateRepository.getDescriptionFromURI(descriptor.getKey().getUri());
        if (descTemplate == null) {
            throw new SparqlException ("Descriptor template " + descriptor.getKey().getId() + " does not exist in the repository");
        }
        
        if (order == null) {
            if (descTemplate.getOrder() != null) {
                order = f.createLiteral(descTemplate.getOrder());
            }
        }
        
        IRI descriptorTemplateIRI = f.createIRI(descriptor.getKey().getUri());
        Literal dValue = descriptor.getValue() != null ? f.createLiteral(descriptor.getValue()) : null;
        Literal name = f.createLiteral(descTemplate.getName());
        statements.add(f.createStatement(descr, RDF.TYPE, f.createIRI(simpleDescriptionTypePredicate), graphIRI));
        statements.add(f.createStatement(descr, RDFS.LABEL, name, graphIRI));
        statements.add(f.createStatement(descr, hasKey, descriptorTemplateIRI, graphIRI));
        if (dValue != null) statements.add(f.createStatement(descr, hasValue, dValue, graphIRI));
        if (unit != null) statements.add(f.createStatement(descr, hasUnit, unit, graphIRI));
        if (order != null) statements.add(f.createStatement(descr, hasOrder, order, graphIRI));
        statements.add(f.createStatement(descr, notRecorded, notRec, graphIRI));
        statements.add(f.createStatement(descr, notApplicable, notApp, graphIRI));
        
        return descrURI;     
    }


    private String addDescriptorGroup(DescriptorGroup descriptorGroup, List<Statement> statements, String graph) throws SparqlException, SQLException {
        ValueFactory f = sparqlDAO.getValueFactory();
        String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
        IRI graphIRI = f.createIRI(graph);
        String uriPre = uriPrefix;
        if (graph.equals (DEFAULT_GRAPH)) {
            uriPre = uriPrefixPublic;
        }
        String descrGroupURI = generateUniqueURI(uriPre + "D", allGraphs);
        IRI descrGroup = f.createIRI(descrGroupURI);
        
        IRI hasKey = f.createIRI(keyPredicate);
        IRI type = f.createIRI(complexDescriptionTypePredicate);
        IRI hasDescriptor = f.createIRI(hasDescriptionPredicate);
        IRI hasOrder = f.createIRI(orderPredicate);
        IRI notRecorded = f.createIRI(notRecordedPredicate);
        IRI notApplicable = f.createIRI(notApplicablePredicate);
        
        Literal order = descriptorGroup.getOrder() == null || descriptorGroup.getOrder() == -1 ? null : f.createLiteral(descriptorGroup.getOrder());
        Literal notRec = descriptorGroup.getNotRecorded() == null ? f.createLiteral(false) : f.createLiteral(descriptorGroup.getNotRecorded());
        Literal notApp = descriptorGroup.getNotApplicable() == null ? f.createLiteral(false) : f.createLiteral(descriptorGroup.getNotApplicable());
        
        if (descriptorGroup.getKey().getUri() == null) {
            // try to get the uri from id
            if (descriptorGroup.getKey().getId() != null) {
                descriptorGroup.getKey().setUri(MetadataTemplateRepository.templatePrefix + descriptorGroup.getKey().getId()) ;
            } else {
                throw new SparqlException ("Descriptor template info is missing");
            }
        }
        IRI templateIRI = f.createIRI(descriptorGroup.getKey().getUri());
        
        // check if template actually exists!
        DescriptionTemplate descTemplate = templateRepository.getDescriptionFromURI(descriptorGroup.getKey().getUri());
        if (descTemplate == null) {
            throw new SparqlException ("Descriptor template " + descriptorGroup.getKey().getId() + " does not exist in the repository");
        }
        
        if (order == null) {
            if (descTemplate.getOrder() != null) {
                order = f.createLiteral(descTemplate.getOrder());
            }
        }
        Literal name = f.createLiteral(descTemplate.getName());
        statements.add(f.createStatement(descrGroup, RDFS.LABEL, name, graphIRI));
        statements.add(f.createStatement(descrGroup, RDF.TYPE, type, graphIRI)); 
        statements.add(f.createStatement(descrGroup, hasKey, templateIRI, graphIRI));
        if (order != null) statements.add(f.createStatement(descrGroup, hasOrder, order, graphIRI));
        statements.add(f.createStatement(descrGroup, notRecorded, notRec, graphIRI));
        statements.add(f.createStatement(descrGroup, notApplicable, notApp, graphIRI));
        
        if (descriptorGroup.getDescriptors() != null) {
            for (Description descriptor: descriptorGroup.getDescriptors()) {
                if (descriptor == null) {
                    continue; // skip null entries if any
                }
                
                if (descriptor.isGroup()) {
                    String descrURI = addDescriptorGroup((DescriptorGroup)descriptor, statements, graph);
                    statements.add(f.createStatement(descrGroup, hasDescriptor, f.createIRI(descrURI), graphIRI));
                } else {
                    //if (((Descriptor) descriptor).getValue() == null || ((Descriptor) descriptor).getValue().isEmpty()) continue;   // it can be notRecorded!!!
                    String descrURI = addDescriptor((Descriptor)descriptor, statements, graph);
                    statements.add(f.createStatement(descrGroup, hasDescriptor, f.createIRI(descrURI), graphIRI));
                }
            }
        }
        
        return descrGroupURI;
    }


    @Override
    public String addImageAnalysisSoftware(ImageAnalysisSoftware metadata, UserEntity user)
            throws SparqlException, SQLException {
        return addMetadataCategory (metadata, MetadataTemplateType.IMAGEANALYSISSOFTWARE, hasImageTemplatePredicate, imageAnalysisTypePredicate, "Im", user);
    }


    /**
     * add all descriptors and descriptor groups to the given IRI in the given graph
     * @param uri URI of the entity in the repository to add metadata
     * @param metadata descriptors and descriptor groups 
     * @param statements list of statements generated by adding the metadata
     * @param graph graph to insert data
     * @throws SparqlException 
     * @throws SQLException 
     */
    private void addMetadata (String uri, MetadataCategory metadata, List<Statement> statements, String graph) throws SparqlException, SQLException {
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI graphIRI = f.createIRI(graph);
        IRI iri = f.createIRI(uri);
        IRI hasDescriptor = f.createIRI(describedbyPredicate);
        if (metadata.getDescriptors() != null) {
            for (Descriptor descriptor: metadata.getDescriptors()) {
                if (descriptor == null) continue;
                //if (descriptor.getValue() == null || descriptor.getValue().isEmpty()) continue;
                String descriptorURI = addDescriptor(descriptor, statements, graph);
                IRI descrIRI = f.createIRI(descriptorURI);
                statements.add(f.createStatement(iri, hasDescriptor, descrIRI, graphIRI));
            }
        }
        
        if (metadata.getDescriptorGroups() != null) {
            for (DescriptorGroup descriptorGroup: metadata.getDescriptorGroups()) {
                if (descriptorGroup == null) continue;
                String descriptorGroupURI = addDescriptorGroup(descriptorGroup, statements, graph);
                IRI descrGroupIRI = f.createIRI(descriptorGroupURI);
                statements.add(f.createStatement(iri, hasDescriptor, descrGroupIRI, graphIRI));
            }
        }
    }


    /**
     * add metadata to repository and return the uri, if it already exists (same label), return the exising uri
     * @param metadata
     * @param metadataType
     * @param templatePredicate
     * @param typePredicate
     * @param prefix
     * @param user
     * @return uri of the newly added metadata or the existing one
     * @throws SparqlException
     * @throws SQLException
     */
    @Override
    public String addMetadataCategory (MetadataCategory metadata, MetadataTemplateType metadataType, 
            String templatePredicate, String typePredicate, String prefix, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (user == null) {
            graph = DEFAULT_GRAPH;
        } else {
            // check if there is already a private graph for user
            graph = getGraphForUser(user);
        }
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI graphIRI = f.createIRI(graph);
        
        IRI hasTemplate = f.createIRI(templatePredicate);
        IRI type = f.createIRI(typePredicate);
        // TODO check if the metadata already exists in "default-graph", then we need to add a triple sample->has_public_uri->existingURI to the private repo
        String existing = null;
        if (metadata.getName() != null)
        	existing = getEntityByLabel(metadata.getName().trim(), graph, typePredicate);
        if (existing == null) {
            // add to user's local repository
            List<Statement> statements = new ArrayList<Statement>();
            Date addedToLibrary = metadata.getDateAddedToLibrary() == null ? new Date() : metadata.getDateAddedToLibrary();
            String metadataURI = addGenericInfo(metadata.getName(), metadata.getDescription(), addedToLibrary, statements, prefix, graph);
            IRI sampleIRI = f.createIRI(metadataURI);
            // add template
            // find the template with given name and add the object property from sample to the metadatatemplate
            if (metadata.getTemplate() != null) {
                String templateURI = templateRepository.getTemplateByName(metadata.getTemplate(), metadataType);
                if (templateURI != null) 
                    statements.add(f.createStatement(sampleIRI, hasTemplate, f.createIRI(templateURI), graphIRI));
            } else {
                // ERROR!
                logger.warn("metadata template is missing for " + metadata.getName());
            }
            statements.add(f.createStatement(sampleIRI, RDF.TYPE, type, graphIRI));
            addMetadata (metadataURI, metadata, statements, graph);
            sparqlDAO.addStatements(statements, graphIRI);
            
            return metadataURI;
        } 
        return existing;
    }  
    
    @Override
    public String addPrinter(Printer metadata, UserEntity user) throws SparqlException, SQLException {
        return addMetadataCategory (metadata, MetadataTemplateType.PRINTER, hasPrinterTemplatePredicate, printerTypePredicate, "Pr", user);
    }
    
    @Override
    public String addPrintRun(PrintRun metadata, UserEntity user) throws SparqlException, SQLException {
        return addMetadataCategory (metadata, MetadataTemplateType.PRINTRUN, hasPrintRunTemplatePredicate, printRunTypePredicate, "Prr", user);
    }
    
    @Override
    public String addSample(Sample sample, UserEntity user) throws SparqlException, SQLException {
        String sampleURI =  addMetadataCategory (sample, MetadataTemplateType.SAMPLE, hasSampleTemplatePredicate, sampleTypePredicate, "SA", user);
        String graph = null;
        if (user == null) {
            graph = DEFAULT_GRAPH;
        } else {
            // check if there is already a private graph for user
            graph = getGraphForUser(user);
        }
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI graphIRI = f.createIRI(graph);
        Literal internalId = sample.getInternalId() == null ? null : f.createLiteral(sample.getInternalId().trim());
        IRI hasInternalId = f.createIRI(ontPrefix + "has_internal_id");
        if (internalId != null) {
            List<Statement> statements = new ArrayList<Statement>();
            statements.add(f.createStatement(f.createIRI(sampleURI), hasInternalId, internalId, graphIRI));
            sparqlDAO.addStatements(statements, graphIRI);
        }
        return sampleURI;
    }


    @Override
    public String addScannerMetadata(ScannerMetadata metadata, UserEntity user) throws SparqlException, SQLException {
        return addMetadataCategory (metadata, MetadataTemplateType.SCANNER, hasScannerleTemplatePredicate, scannerTypePredicate, "Sc", user);
    }
    
    @Override
    public String addSlideMetadata(SlideMetadata metadata, UserEntity user) throws SparqlException, SQLException {
        return addMetadataCategory (metadata, MetadataTemplateType.SLIDE, hasSlideTemplatePredicate, slideTemplateTypePredicate, "Slm", user);
    }
    
    @Override
    public String addSpotMetadata(SpotMetadata metadata, UserEntity user) throws SparqlException, SQLException {
        return addMetadataCategory (metadata, MetadataTemplateType.SPOT, ArrayDatasetRepositoryImpl.hasSpotMetadataTemplatePredicate, ArrayDatasetRepositoryImpl.spotMetadataTypePredicate, "Spm", user);
    }
    
    @Override
    public String addSpotMetadataValue(SpotMetadata metadata, UserEntity user) throws SparqlException, SQLException {
        return addMetadataCategory (metadata, MetadataTemplateType.SPOT, ArrayDatasetRepositoryImpl.hasSpotMetadataTemplatePredicate, ArrayDatasetRepositoryImpl.spotMetadataValueTypePredicate, "Spm", user);
    }


    private boolean canDeleteMetadata(String uri, String graph) throws SparqlException {
        return canDeleteMetadata(uri, null, graph);
    }
    
    private boolean canDeleteMetadata(String uri, String parentURI, String graph) throws SparqlException {
        boolean canDelete = true;
        
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        //queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
        queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ("{?s gadr:has_sample <" +  uri + "> } ");
        queryBuf.append ("UNION {?s gadr:has_image_processing_metadata <" + uri +">  } ");
        queryBuf.append ("UNION {?s gadr:has_slide_metadata <" + uri +">  } ");
        queryBuf.append ("UNION {?s gadr:has_scanner_metadata <" + uri +">  } ");
        queryBuf.append ("UNION {?s gadr:printed_by <" + uri +">  } ");
        queryBuf.append ("UNION {?s gadr:has_processing_software_metadata <" + uri +">  } ");
        queryBuf.append ("UNION {?s gadr:has_assay_metadata <" + uri +">  } ");
        queryBuf.append ("UNION {?s gadr:printed_by_run <" + uri +">  } ");
        queryBuf.append ("UNION {?s gadr:has_feature_metadata <" + uri +">  } ");
        queryBuf.append ("UNION {?s gadr:has_spot_metadata <" + uri +">  } ");
        queryBuf.append ("} LIMIT 1");
        
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        if (!results.isEmpty()) {
            if (parentURI != null) {
                // check if the one preventing the delete is the given parentURI
                String sURI = results.get(0).getValue("s");
                if (!sURI.equals(parentURI)) {
                    canDelete = false;
                }
            } else {
                canDelete = false;
            }
        }
        
        return canDelete;
    }

    void deleteDescription (String descriptionURI, String graph) throws SparqlException {
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI graphIRI = f.createIRI(graph);
        IRI descriptor = f.createIRI(descriptionURI);
        IRI hasDescriptor = f.createIRI(hasDescriptionPredicate);
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(descriptor, hasDescriptor, null, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next();
            Value v = st.getObject();
            String uri = v.stringValue();
            deleteDescription (uri, graph);
        }        
        statements = sparqlDAO.getStatements(descriptor, null, null, graphIRI);
        sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI); 
    }


    @Override
    public void deleteMetadata (String metadataId, UserEntity user) throws SparqlException, SQLException {
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
            if (canDeleteMetadata(uriPre + metadataId, graph)) {
                deleteMetadataById(metadataId, graph);
            } else {
                throw new IllegalArgumentException("Cannot delete metadata " + metadataId + ". It is used in an experiment");
            }
        }
    }


    @Override
    public void deleteMetadataById (String metadataId, String graph) throws SparqlException {
        String uriPre = uriPrefix;
        if (graph.equals(DEFAULT_GRAPH)) {       
            uriPre = uriPrefixPublic;
        }
        String uri = uriPre + metadataId;
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI metadata = f.createIRI(uri);
        IRI graphIRI = f.createIRI(graph);
        IRI hasDescriptor = f.createIRI(describedbyPredicate);
        
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(metadata, hasDescriptor, null, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next();
            Value v = st.getObject();
            String descriptorURI = v.stringValue();
            deleteDescription(descriptorURI, graph);
        }
        
        statements = sparqlDAO.getStatements(metadata, null, null, graphIRI);
        sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
        
        // remove from the cache
        metadataCache.remove(uri);
    }


    @Override
    public AssayMetadata getAssayMetadataByLabel(String label, UserEntity user) throws SparqlException, SQLException {
        MetadataCategory metadata = getMetadataByLabel(label, assayTypePredicate, user);
        return metadata == null? null : (AssayMetadata) metadata;
    }


    @Override
    public List<AssayMetadata> getAssayMetadataByUser(UserEntity user) throws SparqlException, SQLException {
       return getAssayMetadataByUser(user, 0, -1, "id", 0);
    }


    @Override
    public List<AssayMetadata> getAssayMetadataByUser(UserEntity user, int offset, int limit, String field, int order)
            throws SparqlException, SQLException {
        return getAssayMetadataByUser(user, offset, limit, field, order, null);
    }


    @Override
    public List<AssayMetadata> getAssayMetadataByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue) throws SparqlException, SQLException {
        return getAssayMetadataByUser(user, offset, limit, field, order, searchValue, true);
    }
    
    @Override
    public List<AssayMetadata> getAssayMetadataByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue, Boolean loadAll) throws SparqlException, SQLException {
        List<AssayMetadata> metadataList = new ArrayList<>();
        
        List<MetadataCategory> list = getMetadataCategoryByUser(user, offset, limit, field, order, searchValue, assayTypePredicate, loadAll);
        for (MetadataCategory m: list) {
            metadataList.add(new AssayMetadata(m));
        }
        
        return metadataList;
    }

    @Override
    public int getAssayMetadataCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        return getCountByUserByType(graph, assayTypePredicate, searchValue, false);
    }

    @Override
    public AssayMetadata getAssayMetadataFromURI(String uri, Boolean loadAll, UserEntity user)
            throws SparqlException, SQLException {
        return (AssayMetadata) getMetadataCategoryFromURI(uri, assayTypePredicate, loadAll, user);
    }


    @Override
    public AssayMetadata getAssayMetadataFromURI(String uri, UserEntity user) throws SparqlException, SQLException {
        return (AssayMetadata) getMetadataCategoryFromURI(uri, assayTypePredicate, true, user);
    }


    @Override
    public DataProcessingSoftware getDataProcessingSoftwareByLabel(String label, UserEntity user) throws SparqlException, SQLException {
        MetadataCategory metadata = getMetadataByLabel(label, dataProcessingTypePredicate, user);
        return metadata == null? null : (DataProcessingSoftware) metadata;
    }


    @Override
    public List<DataProcessingSoftware> getDataProcessingSoftwareByUser(UserEntity user)
            throws SparqlException, SQLException {
        return getDataProcessingSoftwareByUser(user, 0, -1, "id", 0);
    }


    @Override
    public List<DataProcessingSoftware> getDataProcessingSoftwareByUser(UserEntity user, int offset, int limit,
            String field, int order) throws SparqlException, SQLException {
        return getDataProcessingSoftwareByUser(user, offset, limit, field, order, null);
    }

    @Override
    public List<DataProcessingSoftware> getDataProcessingSoftwareByUser(UserEntity user, int offset, int limit,
            String field, int order, String searchValue) throws SparqlException, SQLException {
        return getDataProcessingSoftwareByUser(user, offset, limit, field, order, searchValue, true);
    }


    @Override
    public List<DataProcessingSoftware> getDataProcessingSoftwareByUser(UserEntity user, int offset, int limit,
            String field, int order, String searchValue, Boolean loadAll) throws SparqlException, SQLException {
        List<DataProcessingSoftware> metadataList = new ArrayList<>();
        
        List<MetadataCategory> list = getMetadataCategoryByUser(user, offset, limit, field, order, searchValue, dataProcessingTypePredicate, loadAll);
        for (MetadataCategory m: list) {
            metadataList.add(new DataProcessingSoftware(m));
        }
        
        return metadataList;
    }


    @Override
    public int getDataProcessingSoftwareCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        return getCountByUserByType(graph, dataProcessingTypePredicate, searchValue, false);
    }


    @Override
    public DataProcessingSoftware getDataProcessingSoftwareFromURI(String uri, Boolean loadAll, UserEntity user)
            throws SparqlException, SQLException {
        return (DataProcessingSoftware) getMetadataCategoryFromURI(uri, dataProcessingTypePredicate, loadAll, user);
    }


    @Override
    public DataProcessingSoftware getDataProcessingSoftwareFromURI(String uri, UserEntity user)
            throws SparqlException, SQLException {
        return (DataProcessingSoftware) getMetadataCategoryFromURI(uri, dataProcessingTypePredicate, true, user);
    }

    private Description getDescriptionFromURI(String uri, String graph) throws SparqlException {
        if (uri.contains("public")) {
            graph = DEFAULT_GRAPH;
        }
        List<Description> descriptorList = new ArrayList<Description>();
        List<Description> descriptorGroupList = new ArrayList<Description>();
        Description descriptorObject = null;
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI descriptorIRI = f.createIRI(uri);
        IRI graphIRI = f.createIRI(graph);
        IRI hasValue = f.createIRI(valuePredicate);
        IRI hasKey = f.createIRI(keyPredicate);
        IRI hasUnit = f.createIRI(unitPredicate);
        IRI hasDescriptor = f.createIRI(hasDescriptionPredicate);
        IRI hasOrder = f.createIRI(orderPredicate);
        IRI notRecorded = f.createIRI(notRecordedPredicate);
        IRI notApplicable = f.createIRI(notApplicablePredicate);
        
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(descriptorIRI, RDF.TYPE, null, graphIRI);
        if (statements.hasNext()) {
            Statement st = statements.next();
            String value = st.getObject().stringValue();
            if (value.contains("simple")) {
                descriptorObject = new Descriptor();
                descriptorObject.setUri(uri);
                descriptorObject.setId(uri.substring(uri.lastIndexOf("/")+1));
            } else if (value.contains("complex")) {
                descriptorObject = new DescriptorGroup();
                descriptorObject.setUri(uri);
                descriptorObject.setId(uri.substring(uri.lastIndexOf("/")+1));
            }
        }
        
        if (descriptorObject != null) {
            statements = sparqlDAO.getStatements(descriptorIRI, null, null, graphIRI);
            while (statements.hasNext()) {
                Statement st = statements.next();
                if (st.getPredicate().equals(RDFS.LABEL)) {
                    String value = st.getObject().stringValue();
                    descriptorObject.setName(value);
                } else if (st.getPredicate().equals(hasKey)) {
                    // retrieve descriptorTemplate from template repository
                    String tempURI = st.getObject().stringValue();
                    DescriptionTemplate key = templateRepository.getDescriptionFromURI(tempURI);
                    descriptorObject.setKey(key);
                    if (key != null) descriptorObject.setName(key.getName());
                } else if (st.getPredicate().equals(hasValue)) {
                    String val = st.getObject().stringValue();
                    ((Descriptor)descriptorObject).setValue(val);
                } else if (st.getPredicate().equals(hasUnit)) {
                    String val = st.getObject().stringValue();
                    ((Descriptor)descriptorObject).setUnit(val);
                } else if (st.getPredicate().equals(notRecorded)) {
                    String val = st.getObject().stringValue();
                    descriptorObject.setNotRecorded(Boolean.parseBoolean(val));
                } else if (st.getPredicate().equals(notApplicable)) {
                    String val = st.getObject().stringValue();
                    descriptorObject.setNotApplicable(Boolean.parseBoolean(val));
                } else if (st.getPredicate().equals(hasDescriptor)) {
                    String descURI = st.getObject().stringValue();
                    Description d = getDescriptionFromURI(descURI, graph);
                    if (d.isGroup()) 
                        descriptorGroupList.add(d);
                    else 
                        descriptorList.add(d);
                } else if (st.getPredicate().equals(hasOrder)) {
                    String val = st.getObject().stringValue();
                    try {
                        descriptorObject.setOrder(Integer.parseInt(val));
                    } catch (NumberFormatException e) {
                        logger.warn("order is not valid for " + descriptorObject.getUri(), e);
                    }
                }
            }
        }
        if (descriptorObject != null) {
            descriptorObject.setUri(uri);
            descriptorObject.setId(uri.substring(uri.lastIndexOf("/")+1));
            if (descriptorObject.isGroup()) {
                ((DescriptorGroup) descriptorObject).setDescriptors(new ArrayList<Description>());
                ((DescriptorGroup) descriptorObject).getDescriptors().addAll(descriptorList);
                ((DescriptorGroup) descriptorObject).getDescriptors().addAll(descriptorGroupList);
            }
        }
        return descriptorObject;
    }


    @Override
    public ImageAnalysisSoftware getImageAnalysisSoftwarByLabel(String label, UserEntity user) throws SparqlException, SQLException {
        MetadataCategory metadata = getMetadataByLabel(label, imageAnalysisTypePredicate, user);
        return metadata == null? null : (ImageAnalysisSoftware) metadata;
    }


    @Override
    public List<ImageAnalysisSoftware> getImageAnalysisSoftwareByUser(UserEntity user)
            throws SparqlException, SQLException {
        return getImageAnalysisSoftwareByUser(user, 0, -1, "id", 0 );
    }


    @Override
    public List<ImageAnalysisSoftware> getImageAnalysisSoftwareByUser(UserEntity user, int offset, int limit,
            String field, int order) throws SparqlException, SQLException {
        return getImageAnalysisSoftwareByUser(user, offset, limit, field, order, null);
    }


    @Override
    public List<ImageAnalysisSoftware> getImageAnalysisSoftwareByUser(UserEntity user, int offset, int limit,
            String field, int order, String searchValue) throws SparqlException, SQLException {
        return getImageAnalysisSoftwareByUser(user, offset, limit, field, order, searchValue, true);
    }

    @Override
    public List<ImageAnalysisSoftware> getImageAnalysisSoftwareByUser(UserEntity user, int offset, int limit,
            String field, int order, String searchValue, Boolean loadAll) throws SparqlException, SQLException {
        List<ImageAnalysisSoftware> metadataList = new ArrayList<>();
        
        List<MetadataCategory> list = getMetadataCategoryByUser(user, offset, limit, field, order, searchValue, imageAnalysisTypePredicate, loadAll);
        for (MetadataCategory m: list) {
            metadataList.add(new ImageAnalysisSoftware(m));
        }
        
        return metadataList;
    }


    @Override
    public int getImageAnalysisSoftwareCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        return getCountByUserByType(graph, imageAnalysisTypePredicate, searchValue, false);
    }


    @Override
    public ImageAnalysisSoftware getImageAnalysisSoftwareFromURI(String uri, Boolean loadAll, UserEntity user)
            throws SparqlException, SQLException {
        return (ImageAnalysisSoftware) getMetadataCategoryFromURI(uri, imageAnalysisTypePredicate, loadAll, user);
    }


    @Override
    public ImageAnalysisSoftware getImageAnalysisSoftwareFromURI(String uri, UserEntity user)
            throws SparqlException, SQLException {
        return (ImageAnalysisSoftware) getMetadataCategoryFromURI(uri, imageAnalysisTypePredicate, true, user);
    }


    @Override
    public MetadataCategory getMetadataByLabel(String label, String typePredicate, UserEntity user) throws SparqlException, SQLException {
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
        queryBuf.append ( " ?s rdf:type  <" + typePredicate + "> . \n");
        queryBuf.append ( " ?s rdfs:label ?l FILTER (lcase(str(?l)) = \"\"\"" + label.toLowerCase() + "\"\"\") \n"
                + "}\n");
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        if (results.isEmpty())
            return null;
        else {
            for (SparqlEntity result: results) {
                String uri = result.getValue("s");
                if (!uri.contains("public")) {
                    return getMetadataCategoryFromURI(uri, typePredicate, false, user);
                }
            }
            
            String uri = results.get(0).getValue("s");
            if (uri.contains("public"))
                return getMetadataCategoryFromURI(uri, typePredicate, false, null);
        }
        return null;
    }

    public List<MetadataCategory> getMetadataCategoryByUser (UserEntity user, int offset, int limit, String field, int order,
            String searchValue, String typePredicate) throws SparqlException, SQLException {
        return getMetadataCategoryByUser(user, offset, limit, field, order, searchValue, typePredicate, true);
    }


    @Override
    public List<MetadataCategory> getMetadataCategoryByUser (UserEntity user, int offset, int limit, String field, int order,
            String searchValue, String typePredicate, Boolean loadAll) throws SparqlException, SQLException {
        List<MetadataCategory> list = new ArrayList<>();
        
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else
            graph = getGraphForUser(user);
        if (graph != null) {
            
            List<SparqlEntity> results = retrieveByTypeAndUser(offset, limit, field, order, searchValue, graph, typePredicate, false);
            
            for (SparqlEntity sparqlEntity : results) {
                String uri = sparqlEntity.getValue("s");
                if (user != null && uri.contains("public"))
                    continue;
                MetadataCategory metadata = getMetadataCategoryFromURI(uri, typePredicate, loadAll, user);
                if (metadata != null)
                    list.add(metadata);    
            }
        }
        
        return list;
    }


    @Override
    public MetadataCategory getMetadataCategoryFromURI(String uri, String typePredicate, Boolean loadAll, UserEntity user) throws SparqlException, SQLException {
        if (metadataCache.containsKey(uri)) {
            MetadataCategory metadata = metadataCache.get(uri);
            if (!loadAll)
                return metadata;
            else if ((metadata.getDescriptors() != null && !metadata.getDescriptors().isEmpty()) ||
                    (metadata.getDescriptorGroups() != null && !metadata.getDescriptorGroups().isEmpty())) {
                return metadata;
            }     
        }
        
        MetadataCategory metadataObject = null;
        
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            if (uri.contains("public"))
                graph = DEFAULT_GRAPH;
            else
                graph = getGraphForUser(user);
        }
        
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI metadata = f.createIRI(uri);
        IRI graphIRI = f.createIRI(graph);
        IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
        IRI hasPublicURI = f.createIRI(ontPrefix + "has_public_uri");
        IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
        IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
        IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
        IRI createdBy= f.createIRI(ontPrefix + "created_by");
        IRI hasDescriptor = f.createIRI(describedbyPredicate);
        IRI hasInternalId = f.createIRI(ontPrefix + "has_internal_id");
        
        String templatePredicate = null;
        IRI hasTemplate = null;
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(metadata, null, null, graphIRI);
        if (statements.hasNext()) {
            if (typePredicate.equals(sampleTypePredicate)) {
                metadataObject = new Sample();
                templatePredicate = hasSampleTemplatePredicate;
            } else if (typePredicate.equals(scannerTypePredicate)) {
                metadataObject = new ScannerMetadata();
                templatePredicate = hasScannerleTemplatePredicate;
            } else if (typePredicate.equals(printerTypePredicate)) {
                metadataObject = new Printer();
                templatePredicate = hasPrinterTemplatePredicate;
            } else if (typePredicate.equals(slideTemplateTypePredicate)) {
                metadataObject = new SlideMetadata();
                templatePredicate = hasSlideTemplatePredicate;
            } else if (typePredicate.equals(imageAnalysisTypePredicate)) {
                metadataObject = new ImageAnalysisSoftware();
                templatePredicate = hasImageTemplatePredicate;
            } else if (typePredicate.equals(dataProcessingTypePredicate)) {
                metadataObject = new DataProcessingSoftware();
                templatePredicate = hasDataprocessingTemplatePredicate;
            } else if (typePredicate.equals(assayTypePredicate)) {
                metadataObject = new AssayMetadata();
                templatePredicate = hasAssayTemplatePredicate;
            } else if (typePredicate.equals(spotMetadataTypePredicate) || typePredicate.equals(spotMetadataValueTypePredicate)) {
                metadataObject = new SpotMetadata();
                templatePredicate = hasSpotMetadataTemplatePredicate;
            } else if (typePredicate.equals(featureMetadataTypePredicate)) {
                metadataObject = new FeatureMetadata();
                templatePredicate = hasFeatureMetadataTemplatePredicate;
            } else if (typePredicate.equals(printRunTypePredicate)) {
                metadataObject = new PrintRun();
                templatePredicate = hasPrintRunTemplatePredicate;
            } else {
                logger.error("unknown template predicate" + typePredicate);
            }
            if (metadataObject != null) {
                metadataObject.setUri(uri);
                metadataObject.setId(uri.substring(uri.lastIndexOf("/")+1));
                metadataObject.setDescriptors(new ArrayList<Descriptor>());
                metadataObject.setDescriptorGroups(new ArrayList<DescriptorGroup>());
                if (user != null) {
                    Creator owner = new Creator ();
                    owner.setUserId(user.getUserId());
                    owner.setName(user.getUsername());
                    metadataObject.setUser(owner);
                } else {
                    metadataObject.setPublic(true);
                }
                hasTemplate = templatePredicate == null ? null : f.createIRI(templatePredicate);
            }
        } 
        
        if (metadataObject != null) {
            while (statements.hasNext()) {
                Statement st = statements.next();
                if (st.getPredicate().equals(RDFS.LABEL)) {
                    Value label = st.getObject();
                    metadataObject.setName(label.stringValue());
                } else if (st.getPredicate().equals(createdBy)) {
                    Value label = st.getObject();
                    Creator creator = new Creator();
                    creator.setName(label.stringValue());
                    metadataObject.setUser(creator);
                } else if (st.getPredicate().equals(RDFS.COMMENT)) {
                    Value comment = st.getObject();
                    metadataObject.setDescription(comment.stringValue());
                } else if (st.getPredicate().equals(hasCreatedDate)) {
                    Value value = st.getObject();
                    if (value instanceof Literal) {
                        Literal literal = (Literal)value;
                        XMLGregorianCalendar calendar = literal.calendarValue();
                        Date date = calendar.toGregorianCalendar().getTime();
                        metadataObject.setDateCreated(date);
                    }
                } else if (st.getPredicate().equals(hasModifiedDate)) {
                    Value value = st.getObject();
                    if (value instanceof Literal) {
                        Literal literal = (Literal)value;
                        XMLGregorianCalendar calendar = literal.calendarValue();
                        Date date = calendar.toGregorianCalendar().getTime();
                        metadataObject.setDateModified(date);
                    }
                } else if (st.getPredicate().equals(hasAddedToLibrary)) {
                    Value value = st.getObject();
                    if (value instanceof Literal) {
                        Literal literal = (Literal)value;
                        XMLGregorianCalendar calendar = literal.calendarValue();
                        Date date = calendar.toGregorianCalendar().getTime();
                        metadataObject.setDateAddedToLibrary(date);
                    }
                } else if (st.getPredicate().equals(hasTemplate)) {
                   /* if (loadAll != null && !loadAll) 
                        continue;*/
                    Value uriValue = st.getObject();
                    String templateuri = uriValue.stringValue();
                    String id = templateuri.substring(templateuri.lastIndexOf("#")+1);
                    metadataObject.setTemplateType(id);  
                    MetadataTemplate template = templateRepository.getTemplateFromURI(templateuri, loadAll);
                    if (template != null)
                        metadataObject.setTemplate(template.getName());
                } else if (st.getPredicate().equals(hasDescriptor)) {
                    if (loadAll != null && !loadAll) 
                        continue;
                    Value value = st.getObject();
                    Description descriptor = getDescriptionFromURI (value.stringValue(), graph);
                    if (descriptor != null) {
                        if (descriptor.isGroup()) {
                            metadataObject.getDescriptorGroups().add((DescriptorGroup)descriptor);
                        } else {
                            metadataObject.getDescriptors().add((Descriptor) descriptor);
                        }
                    } else {
                        logger.error("descriptor with uri " + value + " is not found!");
                    }
                } else if (st.getPredicate().equals(hasInternalId)) {
                    if (metadataObject instanceof Sample) {
                        ((Sample) metadataObject).setInternalId(st.getObject().stringValue());
                    }
                } else if (st.getPredicate().equals(hasPublicURI)) {
                    // need to retrieve additional information from DEFAULT graph
                    // that means the sample is already public
                    metadataObject.setPublic(true);  
                    Value uriValue = st.getObject();
                    String publicURI = uriValue.stringValue();
                    IRI publicIRI = f.createIRI(publicURI);
                    RepositoryResult<Statement> statementsPublic = sparqlDAO.getStatements(publicIRI, null, null, defaultGraphIRI);
                    while (statementsPublic.hasNext()) {
                        Statement stPublic = statementsPublic.next();
                        if (stPublic.getPredicate().equals(hasTemplate)) {
                            /*if (loadAll != null && !loadAll) 
                                continue;*/
                            uriValue = stPublic.getObject();
                            String id = uriValue.stringValue().substring(uriValue.stringValue().lastIndexOf("#")+1);
                            metadataObject.setTemplateType(id);     
                            MetadataTemplate template = templateRepository.getTemplateFromURI(uriValue.stringValue(), loadAll);
                            if (template != null)
                                metadataObject.setTemplate(template.getName());
                        } else if (stPublic.getPredicate().equals(RDFS.LABEL)) {
                            Value label = stPublic.getObject();
                            metadataObject.setName(label.stringValue());
                        }  else if (stPublic.getPredicate().equals(RDFS.COMMENT)) {
                            Value comment = stPublic.getObject();
                            metadataObject.setDescription(comment.stringValue());
                        } else if (stPublic.getPredicate().equals(hasDescriptor)) {
                            if (loadAll != null && !loadAll) 
                                continue;
                            Value value = stPublic.getObject();
                            String descriptorURI = value.stringValue();
                            Description descriptor;
                            if (descriptorURI.contains("public")) {
                                descriptor = getDescriptionFromURI (descriptorURI, DEFAULT_GRAPH);
                            } else {
                                descriptor = getDescriptionFromURI (descriptorURI, graph);
                            }
                            if (descriptor != null && descriptor.isGroup()) {
                                metadataObject.getDescriptorGroups().add((DescriptorGroup)descriptor);
                            } else if (descriptor != null){
                                metadataObject.getDescriptors().add((Descriptor) descriptor);
                            }
                        }
                    }
                }
            }
        }
        
        if (metadataObject != null && metadataObject.getUri() != null && loadAll)
            metadataCache.put(metadataObject.getUri(), metadataObject);
        return metadataObject;
    }


    @Override
    public Printer getPrinterByLabel(String label, UserEntity user) throws SparqlException, SQLException {
        MetadataCategory metadata = getMetadataByLabel(label, printerTypePredicate, user);
        return metadata == null? null : (Printer) metadata;
    }


    @Override
    public List<Printer> getPrinterByUser(UserEntity user) throws SparqlException, SQLException {
        return getPrinterByUser(user, 0, -1, "id", 0 );
    }
    
    @Override
    public List<Printer> getPrinterByUser(UserEntity user, int offset, int limit, String field, int order)
            throws SparqlException, SQLException {
        return getPrinterByUser(user, offset, limit, field, order, null);
    }


    @Override
    public List<Printer> getPrinterByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue) throws SparqlException, SQLException {
        return getPrinterByUser(user, offset, limit, field, order, searchValue, true);
    }


    @Override
    public List<Printer> getPrinterByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue, Boolean loadAll) throws SparqlException, SQLException {
        List<Printer> printers = new ArrayList<>();
        
        List<MetadataCategory> list = getMetadataCategoryByUser(user, offset, limit, field, order, searchValue, printerTypePredicate, loadAll);
        for (MetadataCategory m: list) {
            printers.add(new Printer(m));
        }
        
        return printers;
    }


    @Override
    public int getPrinterCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        return getCountByUserByType(graph, printerTypePredicate, searchValue, false);
    }
    
    @Override
    public Printer getPrinterFromURI(String uri, Boolean loadAll, UserEntity user)
            throws SparqlException, SQLException {
        return (Printer) getMetadataCategoryFromURI(uri, printerTypePredicate, loadAll, user);
    }
    
    @Override
    public Printer getPrinterFromURI(String uri, UserEntity user) throws SparqlException, SQLException {
        return (Printer) getMetadataCategoryFromURI(uri, printerTypePredicate, true, user);
    }
    


    @Override
    public PrintRun getPrintRunByLabel(String label, UserEntity user) throws SparqlException, SQLException {
        MetadataCategory metadata = getMetadataByLabel(label, ArrayDatasetRepositoryImpl.printRunTypePredicate, user);
        return metadata == null? null : (PrintRun) metadata;
    }
    
    @Override
    public List<PrintRun> getPrintRunByUser(UserEntity user) throws SparqlException, SQLException {
        return getPrintRunByUser(user, 0, -1, "id", 0);
    }
    
    @Override
    public List<PrintRun> getPrintRunByUser(UserEntity user, int offset, int limit, String field, int order)
            throws SparqlException, SQLException {
        return getPrintRunByUser(user, offset, limit, field, order, null);
    }
    
    @Override
    public List<PrintRun> getPrintRunByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue) throws SparqlException, SQLException {
        return getPrintRunByUser(user, offset, limit, field, order, searchValue, true);
    }
    
    @Override
    public List<PrintRun> getPrintRunByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue, Boolean loadAll) throws SparqlException, SQLException {
        List<PrintRun> printers = new ArrayList<>();
        
        List<MetadataCategory> list = getMetadataCategoryByUser(user, offset, limit, field, order, searchValue, printRunTypePredicate, loadAll);
        for (MetadataCategory m: list) {
            printers.add(new PrintRun(m));
        }
        
        return printers;
    }


    @Override
    public int getPrintRunCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        return getCountByUserByType(graph, printRunTypePredicate, searchValue, false);
    }
    
    


    @Override
    public PrintRun getPrintRunFromURI(String uri, Boolean loadAll, UserEntity user)
            throws SparqlException, SQLException {
        return (PrintRun) getMetadataCategoryFromURI(uri, printRunTypePredicate, true, user);
    }


    @Override
    public PrintRun getPrintRunFromURI(String uri, UserEntity user) throws SparqlException, SQLException {
        return getPrintRunFromURI(uri, true, user);
    }


    @Override
    public Sample getSampleByLabel(String label, UserEntity user) throws SparqlException, SQLException {
        MetadataCategory metadata = getMetadataByLabel(label, sampleTypePredicate, user);
        return metadata == null? null : (Sample) metadata;
    }


    @Override
    public List<Sample> getSampleByUser(UserEntity user) throws SparqlException, SQLException {
        return getSampleByUser(user, 0, -1, "id", 0 );
    }


    @Override
    public List<Sample> getSampleByUser(UserEntity user, int offset, int limit, String field, int order)
            throws SparqlException, SQLException {
        return getSampleByUser(user, offset, limit, field, order, null);
    }


    @Override
    public List<Sample> getSampleByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue) throws SparqlException, SQLException {
        return getSampleByUser(user, offset, limit, field, order, searchValue, true);
    }


    @Override
    public List<Sample> getSampleByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue, Boolean loadAll) throws SparqlException, SQLException {
        List<Sample> samples = new ArrayList<>();
        
        List<MetadataCategory> list = getMetadataCategoryByUser(user, offset, limit, field, order, searchValue, sampleTypePredicate, loadAll);
        for (MetadataCategory m: list) {
            samples.add(new Sample(m));
        }
        
        return samples;
    }
    
    @Override
    public int getSampleCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        return getCountByUserByType(graph, sampleTypePredicate, searchValue, false);
    }
    
    @Override
    public Sample getSampleFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException {
        return (Sample) getMetadataCategoryFromURI(uri, sampleTypePredicate, loadAll, user);
    }


    @Override
    public Sample getSampleFromURI(String uri, UserEntity user) throws SparqlException, SQLException {
        return (Sample) getMetadataCategoryFromURI(uri, sampleTypePredicate, true, user);
    }


    @Override
    public ScannerMetadata getScannerMetadataByLabel(String label, UserEntity user) throws SparqlException, SQLException {
        MetadataCategory metadata = getMetadataByLabel(label, scannerTypePredicate, user);
        return metadata == null? null : (ScannerMetadata) metadata;
    }


    @Override
    public List<ScannerMetadata> getScannerMetadataByUser(UserEntity user) throws SparqlException, SQLException {
        return getScannerMetadataByUser(user, 0, -1, "id", 0 );
    }


    @Override
    public List<ScannerMetadata> getScannerMetadataByUser(UserEntity user, int offset, int limit, String field,
            int order) throws SparqlException, SQLException {
        return getScannerMetadataByUser(user, offset, limit, field, order, null);
    }


    @Override
    public List<ScannerMetadata> getScannerMetadataByUser(UserEntity user, int offset, int limit, String field,
            int order, String searchValue) throws SparqlException, SQLException {
        return getScannerMetadataByUser(user, offset, limit, field, order, searchValue, true);
    }
    
    @Override
    public List<ScannerMetadata> getScannerMetadataByUser(UserEntity user, int offset, int limit, String field,
            int order, String searchValue, Boolean loadAll) throws SparqlException, SQLException {
        List<ScannerMetadata> metadataList = new ArrayList<>();
        
        List<MetadataCategory> list = getMetadataCategoryByUser(user, offset, limit, field, order, searchValue, scannerTypePredicate, loadAll);
        for (MetadataCategory m: list) {
            metadataList.add(new ScannerMetadata(m));
        }
        
        return metadataList;
    }


    @Override
    public int getScannerMetadataCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        return getCountByUserByType(graph, scannerTypePredicate, searchValue, false);
    }
    
    @Override
    public ScannerMetadata getScannerMetadataFromURI(String uri, Boolean loadAll, UserEntity user)
            throws SparqlException, SQLException {
        return (ScannerMetadata) getMetadataCategoryFromURI(uri, scannerTypePredicate, loadAll, user);
    }
    
    @Override
    public ScannerMetadata getScannerMetadataFromURI(String uri, UserEntity user) throws SparqlException, SQLException {
        return (ScannerMetadata) getMetadataCategoryFromURI(uri, scannerTypePredicate, true, user);
    }
    
    @Override
    public SlideMetadata getSlideMetadataByLabel(String label, UserEntity user) throws SparqlException, SQLException {
        MetadataCategory metadata = getMetadataByLabel(label, slideTemplateTypePredicate, user);
        return metadata == null? null : (SlideMetadata) metadata;
    }


    @Override
    public List<SlideMetadata> getSlideMetadataByUser(UserEntity user) throws SparqlException, SQLException {
        return getSlideMetadataByUser(user, 0, -1, "id", 0 );
    }


    @Override
    public List<SlideMetadata> getSlideMetadataByUser(UserEntity user, int offset, int limit, String field, int order)
            throws SparqlException, SQLException {
        return getSlideMetadataByUser(user, offset, limit, field, order, null);
    }


    @Override
    public List<SlideMetadata> getSlideMetadataByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue) throws SparqlException, SQLException {
        return getSlideMetadataByUser(user, offset, limit, field, order, searchValue, true);
    }

    @Override
    public List<SlideMetadata> getSlideMetadataByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue, Boolean loadAll) throws SparqlException, SQLException {
        List<SlideMetadata> metadataList = new ArrayList<>();
        
        List<MetadataCategory> list = getMetadataCategoryByUser(user, offset, limit, field, order, searchValue, slideTemplateTypePredicate, loadAll);
        for (MetadataCategory m: list) {
            metadataList.add(new SlideMetadata(m));
        }
        
        return metadataList;
    }


    @Override
    public int getSlideMetadataCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        return getCountByUserByType(graph, slideTemplateTypePredicate, searchValue, false);
    }


    @Override
    public SlideMetadata getSlideMetadataFromURI(String uri, Boolean loadAll, UserEntity user)
            throws SparqlException, SQLException {
        return (SlideMetadata) getMetadataCategoryFromURI(uri, slideTemplateTypePredicate, loadAll, user);
    }
    
    @Override
    public SlideMetadata getSlideMetadataFromURI(String uri, UserEntity user) throws SparqlException, SQLException {
        return (SlideMetadata) getMetadataCategoryFromURI(uri, slideTemplateTypePredicate, true, user);
    }


    @Override
    public SpotMetadata getSpotMetadataByLabel(String label, UserEntity user) throws SparqlException, SQLException {
        MetadataCategory metadata = getMetadataByLabel(label, ArrayDatasetRepositoryImpl.spotMetadataTypePredicate, user);
        return metadata == null? null : (SpotMetadata) metadata;
    }


    @Override
    public List<SpotMetadata> getSpotMetadataByUser(UserEntity user) throws SparqlException, SQLException {
        return getSpotMetadataByUser(user, 0, -1, "id", 0);
    }
    
    @Override
    public List<SpotMetadata> getSpotMetadataByUser(UserEntity user, int offset, int limit, String field, int order)
            throws SparqlException, SQLException {
        return getSpotMetadataByUser(user, offset, limit, field, order, null);
    }


    @Override
    public List<SpotMetadata> getSpotMetadataByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue) throws SparqlException, SQLException {
        return getSpotMetadataByUser(user, offset, limit, field, order, searchValue, true);
    }
    
    @Override
    public List<SpotMetadata> getSpotMetadataByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue, Boolean loadAll) throws SparqlException, SQLException {
        List<SpotMetadata> metadataList = new ArrayList<>();
        
        List<MetadataCategory> list = getMetadataCategoryByUser(user, offset, limit, field, order, searchValue, ArrayDatasetRepositoryImpl.spotMetadataTypePredicate, loadAll);
        for (MetadataCategory m: list) {
            metadataList.add(new SpotMetadata(m));
        }
        
        return metadataList;
    }


    @Override
    public int getSpotMetadataCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        return getCountByUserByType(graph, ArrayDatasetRepositoryImpl.spotMetadataTypePredicate, searchValue, false);
    }


    @Override
    public SpotMetadata getSpotMetadataFromURI(String uri, Boolean loadAll, UserEntity user)
            throws SparqlException, SQLException {
        return (SpotMetadata) getMetadataCategoryFromURI(uri, ArrayDatasetRepositoryImpl.spotMetadataTypePredicate, loadAll, user);
    }


    @Override
    public SpotMetadata getSpotMetadataFromURI(String uri, UserEntity user) throws SparqlException, SQLException {
       return getSpotMetadataFromURI(uri, true, user);
    }


    @Override
    public SpotMetadata getSpotMetadataValueFromURI(String uri, Boolean loadAll, UserEntity user)
            throws SparqlException, SQLException {
        SpotMetadata metadata = (SpotMetadata) getMetadataCategoryFromURI(uri, ArrayDatasetRepositoryImpl.spotMetadataValueTypePredicate, loadAll, user);
        if (metadata != null) metadata.setIsTemplate(false);
        return metadata;
    }


    @Override
    public SpotMetadata getSpotMetadataValueFromURI(String uri, UserEntity user) throws SparqlException, SQLException {
       return getSpotMetadataFromURI(uri, true, user);
    }


    @Override
    public void updateMetadata (MetadataCategory metadata, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        }
        else {
            graph = getGraphForUser(user);
        }
        
        // delete all existing descriptors for the sample and add them back
        String uri = null;
        if (metadata.getUri() != null)
            uri = metadata.getUri();
        else
            uri = uriPre + metadata.getId();
        
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI graphIRI = f.createIRI(graph);
        IRI hasDescriptor = f.createIRI(describedbyPredicate);
        IRI metadataIRI = f.createIRI(uri);
        IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
        
        // check if it exists
        RepositoryResult<Statement> result = sparqlDAO.getStatements(metadataIRI, null, null, graphIRI);
        if (!result.hasNext()) {
            // does not exist
            throw new EntityNotFoundException("metadata with the given id is not found in the repository");
        }
        
        result = sparqlDAO.getStatements(metadataIRI, hasDescriptor, null, graphIRI);
        while (result.hasNext()) {
            Statement st = result.next();
            Value v = st.getObject();
            String descriptorURI = v.stringValue();
            deleteDescription(descriptorURI, graph);
        }
    
        // delete name/description and connection to the old descriptors
        sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(metadataIRI, RDFS.LABEL, null, graphIRI)), graphIRI);
        sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(metadataIRI, RDFS.COMMENT, null, graphIRI)), graphIRI);
        sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(metadataIRI, hasModifiedDate, null, graphIRI)), graphIRI);
        sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(metadataIRI, hasDescriptor, null, graphIRI)), graphIRI);
        Literal date = f.createLiteral(new Date());
        Literal label = metadata.getName() == null ? null : f.createLiteral(metadata.getName().trim());
        Literal comment = metadata.getDescription() == null ? null : f.createLiteral(metadata.getDescription().trim());
        
        // add updated name/description
        List<Statement> statements = new ArrayList<Statement>();
        if (label != null) statements.add(f.createStatement(metadataIRI, RDFS.LABEL, label, graphIRI));
        if (comment != null) statements.add(f.createStatement(metadataIRI, RDFS.COMMENT, comment, graphIRI));
        statements.add(f.createStatement(metadataIRI, hasModifiedDate, date, graphIRI));
        
        // add descriptors back
        addMetadata (uri, metadata, statements, graph);
        sparqlDAO.addStatements(statements, graphIRI);
        
        // remove from the cache
        metadataCache.remove(uri);
    }


    @Override
    public void updateMetadataMirage(MetadataCategory metadata, UserEntity user) throws SQLException, SparqlException {
        String graph = null;
        String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        }
        else {
            graph = getGraphForUser(user);
        }
        
        String uri = null;
        if (metadata.getUri() != null)
            uri = metadata.getUri();
        else
            uri = uriPre + metadata.getId();
        
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI graphIRI = f.createIRI(graph);
        IRI isMirage = f.createIRI(MetadataTemplateRepository.templatePrefix + "is_mirage");
        IRI metadataIRI = f.createIRI(uri);
        IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
        
        Literal mirage = f.createLiteral(metadata.getIsMirage());
        
        // check if it exists
        RepositoryResult<Statement> result = sparqlDAO.getStatements(metadataIRI, null, null, graphIRI);
        if (!result.hasNext()) {
            // does not exist
            throw new EntityNotFoundException("metadata with the given id is not found in the repository");
        }
           
        // delete isMirage predicate
        sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(metadataIRI, isMirage, null, graphIRI)), graphIRI);
        sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(metadataIRI, hasModifiedDate, null, graphIRI)), graphIRI);
        
        List<Statement> statements = new ArrayList<Statement>();
        statements.add(f.createStatement(metadataIRI, isMirage, mirage, graphIRI));
        Literal date = f.createLiteral(new Date());
        statements.add(f.createStatement(metadataIRI, hasModifiedDate, date, graphIRI));
        
        sparqlDAO.addStatements(statements, graphIRI);
        
    }

}
