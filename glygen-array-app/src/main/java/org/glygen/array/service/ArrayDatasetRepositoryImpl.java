package org.glygen.array.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import org.glygen.array.persistence.rdf.Creator;
import org.glygen.array.persistence.rdf.GlycanSequenceFormat;
import org.glygen.array.persistence.rdf.MassOnlyGlycan;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.data.ArrayDataset;
import org.glygen.array.persistence.rdf.data.Image;
import org.glygen.array.persistence.rdf.data.Intensity;
import org.glygen.array.persistence.rdf.data.Measurement;
import org.glygen.array.persistence.rdf.data.ProcessedData;
import org.glygen.array.persistence.rdf.data.RawData;
import org.glygen.array.persistence.rdf.data.Slide;
import org.glygen.array.persistence.rdf.metadata.Description;
import org.glygen.array.persistence.rdf.metadata.Descriptor;
import org.glygen.array.persistence.rdf.metadata.DescriptorGroup;
import org.glygen.array.persistence.rdf.metadata.MetadataCategory;
import org.glygen.array.persistence.rdf.metadata.Sample;
import org.glygen.array.persistence.rdf.template.MetadataTemplate;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    final static String hasSampleTemplatePredicate = ontPrefix + "has_sample_template";
    
    final static String hasMeanPredicate = ontPrefix + "has_mean";
    final static String hasBMeanPredicate = ontPrefix + "has_bMean";
    final static String hasBMedianPredicate = ontPrefix + "has_bMedian";
    final static String hasMeanMinusBPredicate = ontPrefix + "has_meanminusB";
    final static String hasMedianMinusBPredicate = ontPrefix + "has_medianminusB";
    final static String hasDiameterPredicate = ontPrefix + "has_diameter";
    final static String hasXCoordinatePredicate = ontPrefix + "has_x_coordinate";
    final static String hasYCoordinatePredicate = ontPrefix + "has_y_coordinate";
    final static String hasMedianPredicate = ontPrefix + "has_median";
    

    @Autowired
    QueryHelper queryHelper;
    
    @Autowired
    MetadataTemplateRepository templateRepository;
    
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
            String sampleURI = addSample(dataset.getSample(), user);
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


    private String addMeasurement(Measurement measurement, List<Statement> statements, String graph) throws SparqlException {
        ValueFactory f = sparqlDAO.getValueFactory();
     
        String measurementURI = generateUniqueURI(uriPrefix + "M", graph);
        IRI measurementIRI = f.createIRI(measurementURI);
        IRI graphIRI = f.createIRI(graph);
        // add intensities
        IRI hasMean = f.createIRI(hasMeanPredicate);
        IRI hasMedian = f.createIRI(hasMedianPredicate);
        IRI hasMeanMinusB = f.createIRI(hasMeanMinusBPredicate);
        IRI hasMedianMinusB = f.createIRI(hasMedianMinusBPredicate);
        IRI hasBMean = f.createIRI(hasBMeanPredicate);
        IRI hasBMedian = f.createIRI(hasBMedianPredicate);
        IRI hasXCoordinate = f.createIRI(hasXCoordinatePredicate);
        IRI hasYCoordinate = f.createIRI(hasYCoordinatePredicate);
        IRI hasStdev = f.createIRI(stdevPredicate);
        IRI hasDiameter = f.createIRI(hasDiameterPredicate);
        
        Literal stdev = measurement.getStdev() == null ? null : f.createLiteral(measurement.getbStDev());
        Literal mean = measurement.getMean() == null ? null : f.createLiteral(measurement.getMean());
        Literal median = measurement.getMedian() == null ? null : f.createLiteral(measurement.getMedian());
        Literal meanMinusB = measurement.getMeanMinusB() == null ? null : f.createLiteral(measurement.getMeanMinusB());
        Literal medianMinusB = measurement.getMedianMinusB() == null ? null : f.createLiteral(measurement.getMedianMinusB());
        Literal bMean = measurement.getbMean()== null ? null : f.createLiteral(measurement.getbMean());
        Literal bMedian = measurement.getbMedian() == null ? null : f.createLiteral(measurement.getbMedian());
        Literal diameter = measurement.getCoordinates().getDiameter() == null ? null : f.createLiteral(measurement.getCoordinates().getDiameter());
        Literal xcoordinate = measurement.getCoordinates().getxCoord() == null ? null : f.createLiteral(measurement.getCoordinates().getxCoord());
        Literal ycoordinate = measurement.getCoordinates().getyCoord() == null ? null : f.createLiteral(measurement.getCoordinates().getyCoord());
        
        if (stdev != null) statements.add(f.createStatement(measurementIRI, hasStdev, stdev, graphIRI));
        if (mean != null) statements.add(f.createStatement(measurementIRI, hasMean, mean, graphIRI));
        if (median != null) statements.add(f.createStatement(measurementIRI, hasMedian, median, graphIRI));
        if (meanMinusB != null) statements.add(f.createStatement(measurementIRI, hasMeanMinusB, meanMinusB, graphIRI));
        if (medianMinusB != null) statements.add(f.createStatement(measurementIRI, hasMedianMinusB, medianMinusB, graphIRI));
        if (bMean != null) statements.add(f.createStatement(measurementIRI, hasBMean, bMean, graphIRI));
        if (bMedian != null) statements.add(f.createStatement(measurementIRI, hasBMedian, bMedian, graphIRI));
        if (diameter != null) statements.add(f.createStatement(measurementIRI, hasDiameter, diameter, graphIRI));
        if (xcoordinate != null) statements.add(f.createStatement(measurementIRI, hasXCoordinate, xcoordinate, graphIRI));
        if (ycoordinate != null) statements.add(f.createStatement(measurementIRI, hasYCoordinate, ycoordinate, graphIRI));
        
        return measurementURI;
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

    @Override
    public String addSample(Sample sample, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (user == null) {
            // cannot add 
            throw new SparqlException ("The user must be provided to put data into private repository");
        }
        
        // check if there is already a private graph for user
        graph = getGraphForUser(user);
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI hasTemplate = f.createIRI(hasSampleTemplatePredicate);
        
        // check if the sample already exists in "default-graph", then we need to add a triple sample->has_public_uri->existingURI to the private repo
        String existing = getEntityByLabel(sample.getName(), graph, datasetTypePredicate);
        if (existing == null) {
            // add to user's local repository
            List<Statement> statements = new ArrayList<Statement>();
            String sampleURI = addGenericInfo(sample.getName(), sample.getDescription(), statements, "SA", graph);
            // add template
            // find the template with given name and add the object property from sample to the metadatatemplate
            String templateURI = templateRepository.getTemplateByName(sample.getTemplate());
            if (templateURI != null) 
                statements.add(f.createStatement(f.createIRI(sampleURI), hasTemplate, f.createIRI(templateURI)));
            addMetadata (sampleURI, sample, statements, graph);
            IRI graphIRI = f.createIRI(graph);
            sparqlDAO.addStatements(statements, graphIRI);
            
        } else {
            //TODO
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
        
        for (Description descriptor: descriptorGroup.getDescriptors()) {
            if (descriptor.isGroup()) {
                String descrURI = addDescriptorGroup((DescriptorGroup)descriptor, statements, graph);
                statements.add(f.createStatement(descrGroup, hasDescriptor, f.createIRI(descrURI), graphIRI));
            } else {
                String descrURI = addDescriptor((Descriptor)descriptor, statements, graph);
                statements.add(f.createStatement(descrGroup, hasDescriptor, f.createIRI(descrURI), graphIRI));
            }
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
     // make sure the glycan belongs to this user
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else
            graph = getGraphForUser(user);
        List<SparqlEntity> results = queryHelper.retrieveById(datasetId, graph);
        if (results.isEmpty())
            return null;
        else {
            return getDatasetFromURI(uriPrefix + datasetId, user);
        }
    }

    private ArrayDataset getDatasetFromURI(String uri, UserEntity user) throws SQLException {
        
        ArrayDataset datasetObject = null;
        
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        if (graph == null) {
           return null;
        }
        
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI dataset = f.createIRI(uri);
        IRI graphIRI = f.createIRI(graph);
        IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
        IRI hasPublicURI = f.createIRI(ontPrefix + "has_public_uri");
        IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
        IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
        IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
        IRI createdBy= f.createIRI(ontPrefix + "created_by");
        IRI hasSample = f.createIRI(ontPrefix + "has_sample");
        IRI hasProcessedData = f.createIRI(ontPrefix + "has_processed_data");
        IRI hasRawData = f.createIRI(ontPrefix + "has_raw_data");
        IRI hasSlide = f.createIRI(ontPrefix + "has_slide");
        
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(dataset, null, null, graphIRI);
        if (statements.hasNext()) {
            datasetObject = new ArrayDataset();
            datasetObject.setUri(uri);
            datasetObject.setId(uri.substring(uri.lastIndexOf("/")+1));
            if (user != null) {
                Creator owner = new Creator ();
                owner.setUserId(user.getUserId());
                owner.setName(user.getUsername());
                datasetObject.setUser(owner);
            } else {
                datasetObject.setIsPublic(true);
            }
            datasetObject.setRawDataList(new ArrayList<RawData>());
            datasetObject.setSlides(new ArrayList<Slide>());
        }
        
        while (statements.hasNext()) {
            Statement st = statements.next();
            if (st.getPredicate().equals(RDFS.LABEL)) {
                Value label = st.getObject();
                datasetObject.setName(label.stringValue());
            } else if (st.getPredicate().equals(createdBy)) {
                Value label = st.getObject();
                Creator creator = new Creator();
                creator.setName(label.stringValue());
                datasetObject.setUser(creator);
            } else if (st.getPredicate().equals(RDFS.COMMENT)) {
                Value comment = st.getObject();
                datasetObject.setDescription(comment.stringValue());
            } else if (st.getPredicate().equals(hasCreatedDate)) {
                Value value = st.getObject();
                if (value instanceof Literal) {
                    Literal literal = (Literal)value;
                    XMLGregorianCalendar calendar = literal.calendarValue();
                    Date date = calendar.toGregorianCalendar().getTime();
                    datasetObject.setDateCreated(date);
                }
            } else if (st.getPredicate().equals(hasModifiedDate)) {
                Value value = st.getObject();
                if (value instanceof Literal) {
                    Literal literal = (Literal)value;
                    XMLGregorianCalendar calendar = literal.calendarValue();
                    Date date = calendar.toGregorianCalendar().getTime();
                    datasetObject.setDateModified(date);
                }
            } else if (st.getPredicate().equals(hasAddedToLibrary)) {
                Value value = st.getObject();
                if (value instanceof Literal) {
                    Literal literal = (Literal)value;
                    XMLGregorianCalendar calendar = literal.calendarValue();
                    Date date = calendar.toGregorianCalendar().getTime();
                    datasetObject.setDateAddedToLibrary(date);
                }
            } else if (st.getPredicate().equals(hasSample)) {
                Value uriValue = st.getObject();
                datasetObject.setSample(getSample(uriValue, graph));            
            } else if (st.getPredicate().equals(hasRawData)) {
                Value uriValue = st.getObject();
                datasetObject.getRawDataList().add(getRawData(uriValue, graph));        
            } else if (st.getPredicate().equals(hasSlide)) {
                Value uriValue = st.getObject();
                datasetObject.getSlides().add(getSlide(uriValue, graph));            
            } else if (st.getPredicate().equals(hasProcessedData)) {
                Value uriValue = st.getObject();
                datasetObject.setProcessedData(getProcessedData(uriValue, graph));            
            } else if (st.getPredicate().equals(hasPublicURI)) {
                // need to retrieve additional information from DEFAULT graph
                // that means the arrray dataset is already public
                datasetObject.setIsPublic(true);  
                Value uriValue = st.getObject();
                String publicURI = uriValue.stringValue();
                IRI publicIRI = f.createIRI(publicURI);
                RepositoryResult<Statement> statementsPublic = sparqlDAO.getStatements(publicIRI, null, null, defaultGraphIRI);
                while (statementsPublic.hasNext()) {
                    Statement stPublic = statementsPublic.next();
                    if (stPublic.getPredicate().equals(hasSample)) {
                        uriValue = st.getObject();
                        datasetObject.setSample(getSample(uriValue, DEFAULT_GRAPH));            
                    } else if (stPublic.getPredicate().equals(hasRawData)) {
                        uriValue = st.getObject();
                        datasetObject.getRawDataList().add(getRawData(uriValue, DEFAULT_GRAPH));        
                    } else if (stPublic.getPredicate().equals(hasSlide)) {
                        uriValue = st.getObject();
                        datasetObject.getSlides().add(getSlide(uriValue, DEFAULT_GRAPH));            
                    } else if (stPublic.getPredicate().equals(hasProcessedData)) {
                        uriValue = st.getObject();
                        datasetObject.setProcessedData(getProcessedData(uriValue, DEFAULT_GRAPH));            
                    }
                }
            }
        }
        
        return null;
    }
    
    private ProcessedData getProcessedData(Value uriValue, String graph) {
        // TODO Auto-generated method stub
        return null;
    }


    private Slide getSlide(Value uriValue, String graph) {
        // TODO Auto-generated method stub
        return null;
    }


    private RawData getRawData(Value uriValue, String graph) {
        // TODO Auto-generated method stub
        return null;
    }


    private Sample getSample(Value uriValue, String graph) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void deleteArrayDataset(String datasetId, UserEntity user) throws SparqlException, SQLException {
        // TODO Auto-generated method stub  
    }

    @Override
    public List<ArrayDataset> getArrayDatasetByUser(UserEntity user) throws SparqlException, SQLException {
        return getArrayDatasetByUser(user, 0, -1, "id", 0 );
    }

    @Override
    public List<ArrayDataset> getArrayDatasetByUser(UserEntity user, int offset, int limit, String field, int order)
            throws SparqlException, SQLException {
        
        return getArrayDatasetByUser(user, offset, limit, field, order, null);
    }


    @Override
    public List<ArrayDataset> getArrayDatasetByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue) throws SparqlException, SQLException {
        List<ArrayDataset> datasets = new ArrayList<ArrayDataset>();
        
        // get all glycanURIs from user's private graph
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else
            graph = getGraphForUser(user);
        if (graph != null) {
            
            List<SparqlEntity> results = retrieveDatasetByUser(offset, limit, field, order, searchValue, graph);
            
            for (SparqlEntity sparqlEntity : results) {
                String uri = sparqlEntity.getValue("s");
                ArrayDataset dataset = getDatasetFromURI(uri, user);
                if (dataset != null)
                    datasets.add(dataset);    
            }
        }
        
        return datasets;
    }
    
    public List<SparqlEntity> retrieveDatasetByUser(int offset, int limit, String field, int order, String searchValue, String graph) throws SparqlException {
        String sortPredicate = getSortPredicate (field);
        
        String searchPredicate = "";
        if (searchValue != null)
            searchPredicate = getSearchPredicate(searchValue);
        
        String sortLine = "";
        if (sortPredicate != null)
            sortLine = "OPTIONAL {?s " + sortPredicate + " ?sortBy } .\n";  
        String orderByLine = " ORDER BY " + (order == 0 ? "DESC" : "ASC") + (sortPredicate == null ? "(?s)": "(?sortBy)");  
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        queryBuf.append ("FROM <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n");
        queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append (
                " ?s gadr:has_date_addedtolibrary ?d .\n" +
                " ?s rdf:type <" + datasetTypePredicate + "> . \n" +
                        sortLine + searchPredicate + 
                "}\n" +
                 orderByLine + 
                ((limit == -1) ? " " : " LIMIT " + limit) +
                " OFFSET " + offset);
        
        return sparqlDAO.query(queryBuf.toString());
    }


    private String getSearchPredicate (String searchValue) {
        String predicates = "";
        
        predicates += "?s rdfs:label ?value1 .\n";
        predicates += "OPTIONAL {?s rdfs:comment ?value2} \n";
        
        int numberOfValues = 3;
        String filterClause = "filter (";
        for (int i=1; i < numberOfValues; i++) {
            filterClause += "regex (str(?value" + i + "), '" + searchValue + "', 'i')";
            if (i + 1 < numberOfValues)
                filterClause += " || ";
        }
        filterClause += ")\n";
            
        predicates += filterClause;
        return predicates;
    }
    
    private String getSortPredicate(String field) {
        if (field == null || field.equalsIgnoreCase("name")) 
            return "rdfs:label";
        else if (field.equalsIgnoreCase("comment")) 
            return "rdfs:comment";
        else if (field.equalsIgnoreCase("dateModified"))
            return "gadr:has_date_modified";
        else if (field.equalsIgnoreCase("id"))
            return null;
        return null;
    }

}