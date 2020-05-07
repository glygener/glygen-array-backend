package org.glygen.array.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.data.ArrayDataset;
import org.glygen.array.persistence.rdf.data.Image;
import org.glygen.array.persistence.rdf.data.Intensity;
import org.glygen.array.persistence.rdf.data.Measurement;
import org.glygen.array.persistence.rdf.data.ProcessedData;
import org.glygen.array.persistence.rdf.data.RawData;
import org.glygen.array.persistence.rdf.data.Slide;
import org.glygen.array.persistence.rdf.metadata.Descriptor;
import org.glygen.array.persistence.rdf.metadata.DescriptorGroup;
import org.glygen.array.persistence.rdf.metadata.MetadataCategory;
import org.glygen.array.persistence.rdf.metadata.Sample;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(value="sesameTransactionManager") 
public class ArrayDatasetRepositoryImpl extends GlygenArrayRepositoryImpl implements ArrayDatasetRepository {
    
    final static String datasetTypePredicate = ontPrefix + "ArrayDataset";
    final static String sampleTypePredicate = ontPrefix + "Sample";
    final static String rawdataTypePredicate = ontPrefix + "RawData";
    final static String processedDataTypePredicate = ontPrefix + "ProcessedData";
    final static String slideTypePredicate = ontPrefix + "Slide";
    final static String hasDescriptorPredicate = ontPrefix + "has_descriptor";
    final static String hasDescriptorGroupPredicate = ontPrefix + "has_descriptor_group";
    final static String namespacePredicate = ontPrefix + "has_namespace";
    final static String unitPredicate = ontPrefix + "has_unit_of_measurement";
    final static String valuePredicate = ontPrefix + "has_value";
    final static String rfuPredicate = ontPrefix + "has_rfu";
    final static String stdevPredicate = ontPrefix + "has_stdev";
    final static String cvPredicate = ontPrefix + "has_cv";
    final static String hasIntensityPredicate = ontPrefix + "has_intensity";
    final static String bindingValuePredicate = ontPrefix + "binding_value_of";
    final static String integratedByPredicate = ontPrefix + "integrated_by";
    final static String integratesPredicate = ontPrefix + "has_integrates";
    
    final static String derivedFromPredicate = ontPrefix + "derived_from";
    final static String hasMeasurementPredicate = ontPrefix + "has_measurement";
    final static String measurementOfPredicate = ontPrefix + "measurement_of";
    final static String imageProcessingMetadataPredicate = ontPrefix + "has_image_processing_metadata";
    final static String processingSoftwareMetadataPredicate = ontPrefix + "has_processing_software_metadata";
    final static String slideMetadataPredicate = ontPrefix + "has_slide_metadata";
    final static String printerMetadataPredicate = ontPrefix + "printed_by";
    final static String scannerMetadataPredicate = ontPrefix + "has_scanner_metadata";
    final static String hasFilePredicate = ontPrefix + "has_filename";
    final static String scanOfPredicate = ontPrefix + "scan_of";
    final static String hasSlideTemplatePredicate = ontPrefix + "has_slide_template";
    

    @Override
    public String addArrayDataset(ArrayDataset dataset, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (user == null) {
            // cannot add 
            throw new SparqlException ("The user must be provided to put data into private repository");
        }
        
        
        // check if there is already a private graph for user
        graph = getGraphForUser(user);
        
        ValueFactory f = sparqlDAO.getValueFactory();
        // check if the array dataset already exists in "default-graph", then we need to add a triple dataset->has_public_uri->existingURI to the private repo
        String existing = getEntityByLabel(dataset.getName(), graph, datasetTypePredicate);
        if (existing == null) {
            // add to user's local repository
            List<Statement> statements = new ArrayList<Statement>();
            String datasetURI = addGenericInfo(dataset.getName(), dataset.getDescription(), statements, "AD", graph);
            IRI arraydataset = f.createIRI(datasetURI);
            IRI graphIRI = f.createIRI(graph);
            
            IRI hasSample = f.createIRI(ontPrefix + "has_sample");
            IRI hasProcessedData = f.createIRI(ontPrefix + "has_processed_data");
            IRI hasRawData = f.createIRI(ontPrefix + "has_raw_data");
            IRI hasSlide = f.createIRI(ontPrefix + "has_slide");
            String sampleURI = addSample(dataset.getSample(), graph);
            if (sampleURI != null) {
                IRI sample = f.createIRI(sampleURI);
                statements.add(f.createStatement(arraydataset, hasSample, sample, graphIRI));
                
            }
            String processedDataURI = addProcessedData(dataset.getProcessedData(), graph);
            if (processedDataURI != null) {
                IRI processed = f.createIRI(processedDataURI);
                statements.add(f.createStatement(arraydataset, hasProcessedData, processed, graphIRI));
            }
            if (dataset.getRawDataList() != null && !dataset.getRawDataList().isEmpty()) {
                for (RawData rawData: dataset.getRawDataList()) {
                    String rawDataURI = addRawData(rawData, graph);
                    if (rawDataURI != null) {
                        IRI raw = f.createIRI(rawDataURI);
                        statements.add(f.createStatement(arraydataset, hasRawData, raw, graphIRI));
                    }
                }
            }
            
            if (dataset.getSlides() != null) {
                for (Slide slide: dataset.getSlides()) {
                    String slideURI = addSlide (slide, graph);
                    if (slideURI != null) {
                        IRI slideIRI = f.createIRI(slideURI);
                        statements.add(f.createStatement(arraydataset, hasSlide, slideIRI, graphIRI));
                    }
                }
            }
            
            sparqlDAO.addStatements(statements, graphIRI);
            return datasetURI;
            
        } else {
            // add details to the user's repo and add a link "has_public_uri" to the one in the public graph 
        }
        return null;
    }


    private String addSlide(Slide slide, String graph) throws SparqlException {
        ValueFactory f = sparqlDAO.getValueFactory();
        List<Statement> statements = new ArrayList<Statement>();
        String slideURI = generateUniqueURI(uriPrefix + "S", graph);
        //String imageURI = addImage(slide.getImage(), statements, graph);
        String printerMetadataURI = generateUniqueURI(uriPrefix + "PM", graph);
        String slideMetadataURI = generateUniqueURI(uriPrefix + "SM", graph);
        
        IRI hasSlideMetadata = f.createIRI(slideMetadataPredicate);
        IRI scanOf = f.createIRI(scanOfPredicate);
        IRI printedBy = f.createIRI(printerMetadataPredicate);
        IRI hasSlideTemplate = f.createIRI(hasSlideTemplatePredicate);
        IRI slideIRI = f.createIRI(slideURI);
        IRI graphIRI = f.createIRI(graph);
        
        if (slide.getImage() != null && slide.getImage().getUri() != null) {
            statements.add(f.createStatement(f.createIRI(slide.getImage().getUri()), scanOf, slideIRI, graphIRI));
        }
        
        if (slide.getLayout() != null && slide.getLayout().getUri() != null) {
            statements.add(f.createStatement(slideIRI, hasSlideTemplate, f.createIRI(slide.getLayout().getUri()), graphIRI));
        }
        
        addMetadata(printerMetadataURI, slide.getPrinter(), statements, graph);
        addMetadata(slideMetadataURI, slide.getMetadata(), statements, graph);
        statements.add(f.createStatement(slideIRI, printedBy, f.createIRI(printerMetadataURI), graphIRI));
        statements.add(f.createStatement(slideIRI, hasSlideMetadata, f.createIRI(slideMetadataURI), graphIRI));
        
        sparqlDAO.addStatements(statements, graphIRI);
        return slideURI;
    }


    private String addRawData(RawData rawData, String graph) throws SparqlException {
        ValueFactory f = sparqlDAO.getValueFactory();
        List<Statement> statements = new ArrayList<Statement>();
        String rawDataURI = generateUniqueURI(uriPrefix + "R", graph);
        String imageProcessingMetadataURI = generateUniqueURI(uriPrefix + "IPM", graph);
        
        IRI hasimageProcessingMetadata = f.createIRI(imageProcessingMetadataPredicate);
        IRI derivedFrom = f.createIRI(derivedFromPredicate);
        IRI hasMeasurement = f.createIRI(hasMeasurementPredicate);
        IRI hasFile = f.createIRI(hasFilePredicate);
        IRI measurementOf = f.createIRI(measurementOfPredicate);
        IRI graphIRI = f.createIRI(graph);
        IRI raw = f.createIRI(rawDataURI);
        addMetadata (imageProcessingMetadataURI, rawData.getMetadata(), statements, graph);
        statements.add(f.createStatement(raw, hasimageProcessingMetadata, f.createIRI(imageProcessingMetadataURI), graphIRI));
        for (Image image: rawData.getImages()) {
            String imageURI = addImage (image, statements, graph);
            statements.add(f.createStatement(raw, derivedFrom, f.createIRI(imageURI), graphIRI));
        }
        for (Measurement measurement: rawData.getDataMap().keySet()) {
            String measurementURI = addMeasurement (measurement, statements, graph);
            Spot spot = rawData.getDataMap().get(measurement);
            if (spot.getUri() != null)
                statements.add(f.createStatement(f.createIRI(measurementURI), measurementOf, f.createIRI(spot.getUri()), graphIRI));
            statements.add(f.createStatement(raw, hasMeasurement, f.createIRI(measurementURI), graphIRI));
        }
        if (rawData.getFilename() != null) {
            Literal fileLit = f.createLiteral(rawData.getFilename());
            statements.add(f.createStatement(raw, hasFile, fileLit, graphIRI));
        }
        
        sparqlDAO.addStatements(statements, graphIRI);
        return rawDataURI;
    }


    private String addMeasurement(Measurement measurement, List<Statement> statements, String graph) {
        // TODO Auto-generated method stub
        return null;
    }


    private String addImage(Image image, List<Statement> statements, String graph) {
        // TODO Auto-generated method stub
        return null;
    }


    private String addProcessedData(ProcessedData processedData, String graph) throws SparqlException {
        ValueFactory f = sparqlDAO.getValueFactory();
        List<Statement> statements = new ArrayList<Statement>();
        // add to user's local repository
        String processedURI = generateUniqueURI(uriPrefix + "P", graph);
        IRI processed = f.createIRI(processedURI);
        IRI graphIRI = f.createIRI(graph);
        // add intensities
        IRI hasRFU = f.createIRI(rfuPredicate);
        IRI hasStdev = f.createIRI(stdevPredicate);
        IRI hasCV = f.createIRI(cvPredicate);
        IRI hasIntensity = f.createIRI(hasIntensityPredicate);
        IRI bindingValueOf = f.createIRI(bindingValuePredicate);
        IRI integrates = f.createIRI(integratesPredicate);
        IRI integratedBy = f.createIRI(integratedByPredicate);   //TODO should we add statisticalMethod to the intensity???
        IRI hasProcessingSWMetadata = f.createIRI(processingSoftwareMetadataPredicate);
        
        for (Intensity intensity: processedData.getIntensity()) {
            String intensityURI = generateUniqueURI(uriPrefix + "I", graph);
            IRI intensityIRI = f.createIRI(intensityURI);
            Literal rfu = f.createLiteral(intensity.getRfu());
            Literal stdev = intensity.getStDev() == null ? null : f.createLiteral(intensity.getStDev());
            Literal cv = intensity.getPercentCV() == null ? null : f.createLiteral(intensity.getPercentCV());
            statements.add(f.createStatement(intensityIRI, hasRFU, rfu, graphIRI));
            if (stdev != null) statements.add(f.createStatement(intensityIRI, hasStdev, stdev, graphIRI));
            if (cv != null) statements.add(f.createStatement(intensityIRI, hasCV, cv, graphIRI));
            if (intensity.getFeature() != null && intensity.getFeature().getUri() != null) {
                IRI feature = f.createIRI(intensity.getFeature().getUri());
                statements.add(f.createStatement(intensityIRI, bindingValueOf, feature, graphIRI));
            }
            for (Measurement measurement: intensity.getMeasurements()) {
                if (measurement.getUri() != null) {
                    IRI measurementIRI = f.createIRI(measurement.getUri());
                    statements.add(f.createStatement(intensityIRI, integrates, measurementIRI, graphIRI));
                }
            }
            statements.add(f.createStatement(processed, hasIntensity, intensityIRI, graphIRI));
        }
        // add metadata
        String processingSoftwareMetadata = generateUniqueURI(uriPrefix + "PSM", graph);
        addMetadata(processingSoftwareMetadata, processedData.getMetadata(), statements, graph);
        statements.add(f.createStatement(processed, hasProcessingSWMetadata, f.createIRI(processingSoftwareMetadata), graphIRI));
        
        sparqlDAO.addStatements(statements, graphIRI);
        return processedURI;
    }
    
    private String addGenericInfo (String name, String description, List<Statement> statements, String prefix, String graph) throws SparqlException {
        ValueFactory f = sparqlDAO.getValueFactory();
        
        // add to user's local repository
        String uri = generateUniqueURI(uriPrefix + prefix, graph);
        IRI iri = f.createIRI(uri);
        Literal date = f.createLiteral(new Date());
        IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
        IRI hasType = f.createIRI(hasTypePredicate);
        IRI graphIRI = f.createIRI(graph);
        Literal label = name == null ? null : f.createLiteral(name);
        Literal comment = description == null ? null : f.createLiteral(description);
        IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
        IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
        IRI type = f.createIRI(datasetTypePredicate);
        
        statements.add(f.createStatement(iri, RDF.TYPE, type, graphIRI));
        statements.add(f.createStatement(iri, hasType, type, graphIRI));
        statements.add(f.createStatement(iri, hasCreatedDate, date, graphIRI));
        if (label != null) statements.add(f.createStatement(iri, RDFS.LABEL, label, graphIRI));
        
        if (comment != null) statements.add(f.createStatement(iri, RDFS.COMMENT, comment, graphIRI));
        statements.add(f.createStatement(iri, hasAddedToLibrary, date, graphIRI));
        statements.add(f.createStatement(iri, hasModifiedDate, date, graphIRI));
        
        return uri;
    }

    private String addSample(Sample sample, String graph) throws SparqlException {
        ValueFactory f = sparqlDAO.getValueFactory();
        // check if the sample already exists in "default-graph", then we need to add a triple sample->has_public_uri->existingURI to the private repo
        String existing = getEntityByLabel(sample.getName(), graph, datasetTypePredicate);
        if (existing == null) {
            // add to user's local repository
            List<Statement> statements = new ArrayList<Statement>();
            String sampleURI = addGenericInfo(sample.getName(), sample.getDescription(), statements, "SA", graph);
            addMetadata (sampleURI, sample, statements, graph);
            IRI graphIRI = f.createIRI(graph);
            sparqlDAO.addStatements(statements, graphIRI);
            
        } else {
            
        }
        return null;
    }
    
    /**
     * add all descriptors and descriptor groups to the given IRI in the given graph
     * @param uri URI of the entity in the repository to add metadata
     * @param metadata descriptors and descriptor groups 
     * @param statements list of statements generated by adding the metadata
     * @param graph graph to insert data
     * @throws SparqlException 
     */
    private void addMetadata (String uri, MetadataCategory metadata, List<Statement> statements, String graph) throws SparqlException {
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI graphIRI = f.createIRI(graph);
        IRI iri = f.createIRI(uri);
        IRI hasDescriptor = f.createIRI(hasDescriptorPredicate);
        for (Descriptor descriptor: metadata.getDescriptors()) {
            String descriptorURI = addDescriptor(descriptor, statements, graph);
            IRI descrIRI = f.createIRI(descriptorURI);
            statements.add(f.createStatement(iri, hasDescriptor, descrIRI, graphIRI));
        }
        
        for (DescriptorGroup descriptorGroup: metadata.getDescriptorGroups()) {
            String descriptorGroupURI = addDescriptorGroup(descriptorGroup, statements, graph);
            IRI descrGroupIRI = f.createIRI(descriptorGroupURI);
            statements.add(f.createStatement(iri, hasDescriptor, descrGroupIRI, graphIRI));
        }
    }
    
    private String addDescriptorGroup(DescriptorGroup descriptorGroup, List<Statement> statements, String graph) throws SparqlException {
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI graphIRI = f.createIRI(graph);
        String descrGroupURI = generateUniqueURI(uriPrefix + "D", graph);
        IRI descrGroup = f.createIRI(descrGroupURI);
        Literal dName = f.createLiteral(descriptorGroup.getName());
        Literal dComment = descriptorGroup.getDescription() == null ? null : f.createLiteral(descriptorGroup.getDescription());
        IRI hasDescriptor = f.createIRI(hasDescriptorPredicate);
        
        statements.add(f.createStatement(descrGroup, RDFS.LABEL, dName, graphIRI)); 
        if (dComment != null) statements.add(f.createStatement(descrGroup, RDFS.COMMENT, dComment, graphIRI));
        
        for (Descriptor descriptor: descriptorGroup.getDescriptors()) {
            String descrURI = addDescriptor(descriptor, statements, graph);
            statements.add(f.createStatement(descrGroup, hasDescriptor, f.createIRI(descrURI), graphIRI));
        }
        
        return descrGroupURI;
    }


    private String addDescriptor (Descriptor descriptor, List<Statement> statements, String graph) throws SparqlException {
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI graphIRI = f.createIRI(graph);
        String descrURI = generateUniqueURI(uriPrefix + "D", graph);
        IRI descr = f.createIRI(descrURI);
        Literal dName = f.createLiteral(descriptor.getName());
        Literal dComment = descriptor.getDescription() == null ? null : f.createLiteral(descriptor.getDescription());
        Literal dValue = f.createLiteral(descriptor.getValue());
        IRI namespaceIRI = descriptor.getNamespaceURI() == null ? null: f.createIRI(descriptor.getNamespaceURI());
        IRI unitIRI = descriptor.getUnitURI() == null ? null : f.createIRI(descriptor.getUnitURI());
        IRI hasValue = f.createIRI(valuePredicate);
        IRI hasNamespace = f.createIRI(namespacePredicate);
        IRI hasUnit = f.createIRI(unitPredicate);
        
        statements.add(f.createStatement(descr, RDFS.LABEL, dName, graphIRI));
        statements.add(f.createStatement(descr, hasValue, dValue, graphIRI));
        if (dComment != null) statements.add(f.createStatement(descr, RDFS.COMMENT, dComment, graphIRI));
        if (namespaceIRI != null) statements.add(f.createStatement(descr, hasNamespace, namespaceIRI, graphIRI));
        if (unitIRI != null) statements.add(f.createStatement(descr, hasUnit, unitIRI, graphIRI));
        
        return descrURI;     
    }


    @Override
    public ArrayDataset getArrayDataset(String datasetId, UserEntity user) throws SparqlException, SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ArrayDataset> getArrayDatasetByUser(UserEntity user) throws SparqlException, SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteArrayDataset(String datasetId, UserEntity user) throws SparqlException, SQLException {
        // TODO Auto-generated method stub
        
    }

}
