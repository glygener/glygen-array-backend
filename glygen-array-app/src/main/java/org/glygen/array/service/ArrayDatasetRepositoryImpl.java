package org.glygen.array.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.data.ArrayDataset;
import org.glygen.array.persistence.rdf.data.Image;
import org.glygen.array.persistence.rdf.data.Intensity;
import org.glygen.array.persistence.rdf.data.Measurement;
import org.glygen.array.persistence.rdf.data.PrintedSlide;
import org.glygen.array.persistence.rdf.data.ProcessedData;
import org.glygen.array.persistence.rdf.data.RawData;
import org.glygen.array.persistence.rdf.data.Slide;
import org.glygen.array.persistence.rdf.metadata.DataProcessingSoftware;
import org.glygen.array.persistence.rdf.metadata.Description;
import org.glygen.array.persistence.rdf.metadata.Descriptor;
import org.glygen.array.persistence.rdf.metadata.DescriptorGroup;
import org.glygen.array.persistence.rdf.metadata.ImageAnalysisSoftware;
import org.glygen.array.persistence.rdf.metadata.MetadataCategory;
import org.glygen.array.persistence.rdf.metadata.Printer;
import org.glygen.array.persistence.rdf.metadata.Sample;
import org.glygen.array.persistence.rdf.metadata.ScannerMetadata;
import org.glygen.array.persistence.rdf.metadata.SlideMetadata;
import org.glygen.array.persistence.rdf.template.DescriptionTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(value="sesameTransactionManager") 
public class ArrayDatasetRepositoryImpl extends GlygenArrayRepositoryImpl implements ArrayDatasetRepository {
    
    public final static String datasetTypePredicate = ontPrefix + "array_dataset";
    public final static String printedSlideTypePredicate = ontPrefix + "printed_slide";
    public final static String sampleTypePredicate = ontPrefix + "sample";
    public final static String rawdataTypePredicate = ontPrefix + "raw_data";
    public final static String processedDataTypePredicate = ontPrefix + "processed_data";
    public final static String slideTypePredicate = ontPrefix + "slide";
    public final static String printerTypePredicate = ontPrefix + "printer";
    public final static String scannerTypePredicate = ontPrefix + "scanner";
    public final static String slideTemplateTypePredicate = ontPrefix + "slide_metadata";
    public final static String imageAnalysisTypePredicate = ontPrefix + "image_analysis_software";
    public final static String dataProcessingTypePredicate = ontPrefix + "data_processing_software";
    public final static String simpleDescriptionTypePredicate = ontPrefix + "simple_description";
    public final static String complexDescriptionTypePredicate = ontPrefix + "complex_description";
    
    
    //final static String hasDescriptorPredicate = ontPrefix + "has_descriptor";
    //final static String hasDescriptorGroupPredicate = ontPrefix + "has_descriptor_group";
    public final static String describedbyPredicate = ontPrefix + "described_by";
    //public final static String namespacePredicate = ontPrefix + "has_namespace";
    public final static String unitPredicate = ontPrefix + "has_unit_of_measurement";
    public final static String valuePredicate = ontPrefix + "has_value";
    public final static String keyPredicate = ontPrefix + "has_key";
    public final static String rfuPredicate = ontPrefix + "has_rfu";
    public final static String stdevPredicate = ontPrefix + "has_stdev";
    public final static String cvPredicate = ontPrefix + "has_cv";
    public final static String hasIntensityPredicate = ontPrefix + "has_intensity";
    public final static String bindingValuePredicate = ontPrefix + "binding_value_of";
    public final static String integratedByPredicate = ontPrefix + "integrated_by";
    public final static String integratesPredicate = ontPrefix + "integrates";
    
    public final static String derivedFromPredicate = ontPrefix + "derived_from";
    public final static String hasMeasurementPredicate = ontPrefix + "has_measurement";
    public final static String measurementOfPredicate = ontPrefix + "measurement_of";
    public final static String imageProcessingMetadataPredicate = ontPrefix + "has_image_processing_metadata";
    public final static String processingSoftwareMetadataPredicate = ontPrefix + "has_processing_software_metadata";
    public final static String slideMetadataPredicate = ontPrefix + "has_slide_metadata";
    public final static String printerMetadataPredicate = ontPrefix + "printed_by";
    public final static String scannerMetadataPredicate = ontPrefix + "has_scanner_metadata";
    public final static String hasFilePredicate = ontPrefix + "has_file";
    public final static String scanOfPredicate = ontPrefix + "scan_of";
    public final static String hasImagePredicate = ontPrefix + "has_image";
    public final static String hasPrintedSlidePredicate = ontPrefix + "has_printed_slide";
    public final static String hasMeanPredicate = ontPrefix + "has_mean";
    public final static String hasBMeanPredicate = ontPrefix + "has_bMean";
    public final static String hasBMedianPredicate = ontPrefix + "has_bMedian";
    public final static String hasMeanMinusBPredicate = ontPrefix + "has_meanminusB";
    public final static String hasMedianMinusBPredicate = ontPrefix + "has_medianminusB";
    public final static String hasDiameterPredicate = ontPrefix + "has_diameter";
    public final static String hasXCoordinatePredicate = ontPrefix + "has_x_coordinate";
    public final static String hasYCoordinatePredicate = ontPrefix + "has_y_coordinate";
    public final static String hasMedianPredicate = ontPrefix + "has_median";
    
    // Template ontology stuff
    public final static String hasSampleTemplatePredicate = MetadataTemplateRepository.templatePrefix + "has_sample_template";
    public final static String hasSlideTemplatePredicate = MetadataTemplateRepository.templatePrefix + "has_slide_template";
    public final static String hasScannerleTemplatePredicate = MetadataTemplateRepository.templatePrefix + "has_scanner_template";
    public final static String hasPrinterTemplatePredicate = MetadataTemplateRepository.templatePrefix + "has_printer_template";
    public final static String hasImageTemplatePredicate = MetadataTemplateRepository.templatePrefix + "has_image_analysis_software_template";
    public final static String hasDataprocessingTemplatePredicate = MetadataTemplateRepository.templatePrefix + "has_data_processing_software_template";
    
    
    @Autowired
    QueryHelper queryHelper;
    
    @Autowired
    MetadataTemplateRepository templateRepository;
    
    @Autowired
    FeatureRepository featureRepository;
    
    @Autowired
    LayoutRepository layoutRepository;
    
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
            IRI hasImage = f.createIRI(hasImagePredicate);
            String sampleURI = addSample(dataset.getSample(), user);
            IRI type = f.createIRI(datasetTypePredicate);
            
            IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
            IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
            IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
            Literal date = f.createLiteral(new Date());
            
            statements.add(f.createStatement(arraydataset, RDF.TYPE, type, graphIRI));
            
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
                    String rawDataURI = addRawData(rawData, user);
                    if (rawDataURI != null) {
                        IRI raw = f.createIRI(rawDataURI);
                        statements.add(f.createStatement(arraydataset, hasRawData, raw, graphIRI));
                    }
                }
            }
            
            if (dataset.getSlides() != null) {
                for (Slide slide: dataset.getSlides()) {
                    String slideURI = addSlide (slide, user);
                    if (slideURI != null) {
                        IRI slideIRI = f.createIRI(slideURI);
                        statements.add(f.createStatement(arraydataset, hasSlide, slideIRI, graphIRI));
                    }
                }
            }
            
            //add images
            if (dataset.getImages() != null) {
                for (Image image: dataset.getImages()) {
                    String imageURI = addImage (image, statements, graph);
                    if (imageURI != null) {
                        IRI imageIRI = f.createIRI(imageURI);
                        statements.add(f.createStatement(arraydataset, hasImage, imageIRI, graphIRI));
                    }
                }
            }
            
            statements.add(f.createStatement(arraydataset, hasCreatedDate, date, graphIRI));
            statements.add(f.createStatement(arraydataset, hasAddedToLibrary, date, graphIRI));
            statements.add(f.createStatement(arraydataset, hasModifiedDate, date, graphIRI));
            
            sparqlDAO.addStatements(statements, graphIRI);
            return datasetURI;
            
        } else {
            // add details to the user's repo and add a link "has_public_uri" to the one in the public graph 
        }
        return null;
    }


    @Override
    public String addSlide(Slide slide, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (user == null) {
            // cannot add 
            throw new SparqlException ("The user must be provided to put data into private repository");
        }
        
        // check if there is already a private graph for user
        graph = getGraphForUser(user);
        ValueFactory f = sparqlDAO.getValueFactory();
        String slideURI = generateUniqueURI(uriPrefix + "S", graph);
        
        IRI scanOf = f.createIRI(scanOfPredicate);
        IRI hasPrintedSlide = f.createIRI(hasPrintedSlidePredicate);
        IRI slideIRI = f.createIRI(slideURI);
        IRI graphIRI = f.createIRI(graph);
        
        List<Statement> statements = new ArrayList<Statement>();
        if (slide.getImage() != null && slide.getImage().getUri() != null) {
            statements.add(f.createStatement(f.createIRI(slide.getImage().getUri()), scanOf, slideIRI, graphIRI));
        }
        
        if (slide.getPrintedSlide() != null) {
            PrintedSlide printedSlide = slide.getPrintedSlide();
            String printedSlideURI = null;
            if (printedSlide.getUri() != null) {
                printedSlideURI = printedSlide.getUri();
            } else if (printedSlide.getId() != null) {
                printedSlideURI = uriPrefix + printedSlide.getId();
            } else { // create it the first time
                printedSlideURI = addPrintedSlide (slide.getPrintedSlide(), user);
            }
            statements.add(f.createStatement(slideIRI, hasPrintedSlide, f.createIRI(printedSlideURI), graphIRI));
        }
        
        sparqlDAO.addStatements(statements, graphIRI);
        return slideURI;
    }
    
    @Override
    public String addPrintedSlide (PrintedSlide printedSlide, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (user == null) {
            // cannot add 
            throw new SparqlException ("The user must be provided to put data into private repository");
        }
        
        // check if there is already a private graph for user
        graph = getGraphForUser(user);
        ValueFactory f = sparqlDAO.getValueFactory();
        
        IRI graphIRI = f.createIRI(graph);
        String printedSlideURI = generateUniqueURI(uriPrefix + "PS", graph);
        IRI iri = f.createIRI(printedSlideURI);
        Literal label = printedSlide.getName() == null ? null : f.createLiteral(printedSlide.getName());
        Literal comment = printedSlide.getDescription() == null ? null : f.createLiteral(printedSlide.getDescription());
        IRI hasSlideMetadata = f.createIRI(slideMetadataPredicate);
        IRI printedBy = f.createIRI(printerMetadataPredicate);
        IRI hasSlideLayout = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_slide_layout");
        
        IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
        IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
        IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
        Literal date = f.createLiteral(new Date());
        IRI type = f.createIRI(printedSlideTypePredicate);
        
        List<Statement> statements = new ArrayList<Statement>();
        statements.add(f.createStatement(iri, RDF.TYPE, type));
        if (label != null) 
            statements.add(f.createStatement(iri, RDFS.LABEL, label, graphIRI));
        if (comment != null)
            statements.add(f.createStatement(iri, RDFS.COMMENT, comment, graphIRI));
        if (printedSlide.getLayout() != null && printedSlide.getLayout().getUri() != null) {
            statements.add(f.createStatement(iri, hasSlideLayout, f.createIRI(printedSlide.getLayout().getUri()), graphIRI));
        }
        if (printedSlide.getPrinter() != null && printedSlide.getPrinter().getUri() != null) {
            statements.add(f.createStatement(iri, printedBy, f.createIRI(printedSlide.getPrinter().getUri()), graphIRI));
        } 
        if (printedSlide.getMetadata() != null && printedSlide.getMetadata().getUri() != null) {
            statements.add(f.createStatement(iri, hasSlideMetadata, f.createIRI(printedSlide.getMetadata().getUri()), graphIRI));
        }
        
        statements.add(f.createStatement(iri, hasCreatedDate, date, graphIRI));
        statements.add(f.createStatement(iri, hasAddedToLibrary, date, graphIRI));
        statements.add(f.createStatement(iri, hasModifiedDate, date, graphIRI));
        
        return printedSlideURI;
        
    }

    @Override
    public String addRawData(RawData rawData, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (user == null) {
            // cannot add 
            throw new SparqlException ("The user must be provided to put data into private repository");
        }
        
        // check if there is already a private graph for user
        graph = getGraphForUser(user);
        ValueFactory f = sparqlDAO.getValueFactory();
        List<Statement> statements = new ArrayList<Statement>();
        String rawDataURI = generateUniqueURI(uriPrefix + "R", graph);
        String imageProcessingMetadataURI = generateUniqueURI(uriPrefix + "IPM", graph);
        
        IRI hasimageProcessingMetadata = f.createIRI(imageProcessingMetadataPredicate);
        IRI derivedFrom = f.createIRI(derivedFromPredicate);
        IRI hasMeasurement = f.createIRI(hasMeasurementPredicate);
        IRI hasFile = f.createIRI(hasFilePredicate);
        IRI measurementOf = f.createIRI(measurementOfPredicate);
        IRI hasSlide = f.createIRI(ontPrefix + "has_slide");
        IRI graphIRI = f.createIRI(graph);
        IRI raw = f.createIRI(rawDataURI);
        addMetadata (imageProcessingMetadataURI, rawData.getMetadata(), statements, graph);
        statements.add(f.createStatement(raw, hasimageProcessingMetadata, f.createIRI(imageProcessingMetadataURI), graphIRI));
        if (rawData.getImage() != null) {
            String imageURI = addImage (rawData.getImage(), statements, graph);
            statements.add(f.createStatement(raw, derivedFrom, f.createIRI(imageURI), graphIRI));
        }
        
        if (rawData.getSlide() != null) {
            String slideURI = rawData.getSlide().getUri();
            if (slideURI == null) 
                slideURI = uriPrefix + rawData.getSlide().getId();
            statements.add(f.createStatement(raw, hasSlide, f.createIRI(slideURI), graphIRI));
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
            if (intensity == null) continue;
            String intensityURI = generateUniqueURI(uriPrefix + "I", graph);
            IRI intensityIRI = f.createIRI(intensityURI);
            Literal rfu = f.createLiteral(intensity.getRfu());
            Literal stdev = intensity.getStDev() == null ? null : f.createLiteral(intensity.getStDev());
            Literal cv = intensity.getPercentCV() == null ? null : f.createLiteral(intensity.getPercentCV());
            statements.add(f.createStatement(intensityIRI, hasRFU, rfu, graphIRI));
            if (stdev != null) statements.add(f.createStatement(intensityIRI, hasStdev, stdev, graphIRI));
            if (cv != null) statements.add(f.createStatement(intensityIRI, hasCV, cv, graphIRI));
            if (intensity.getSpot() != null && intensity.getSpot().getUri() != null) {
                IRI spot = f.createIRI(intensity.getSpot().getUri());
                statements.add(f.createStatement(intensityIRI, bindingValueOf, spot, graphIRI));
            }
            if (intensity.getMeasurements() != null) {
                for (Measurement measurement: intensity.getMeasurements()) {
                    if (measurement.getUri() != null) {
                        IRI measurementIRI = f.createIRI(measurement.getUri());
                        statements.add(f.createStatement(intensityIRI, integrates, measurementIRI, graphIRI));
                    }
                }
            }
            statements.add(f.createStatement(processed, hasIntensity, intensityIRI, graphIRI));
        }
        if (processedData.getMetadata() != null) {
            // add metadata
            String processingSoftwareMetadata = generateUniqueURI(uriPrefix + "PSM", graph);
            addMetadata(processingSoftwareMetadata, processedData.getMetadata(), statements, graph);
            statements.add(f.createStatement(processed, hasProcessingSWMetadata, f.createIRI(processingSoftwareMetadata), graphIRI));
        }
        
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
        IRI graphIRI = f.createIRI(graph);
        Literal label = name == null ? null : f.createLiteral(name);
        Literal comment = description == null ? null : f.createLiteral(description);
        IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
        IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
        
        statements.add(f.createStatement(iri, hasCreatedDate, date, graphIRI));
        if (label != null) statements.add(f.createStatement(iri, RDFS.LABEL, label, graphIRI));
        
        if (comment != null) statements.add(f.createStatement(iri, RDFS.COMMENT, comment, graphIRI));
        statements.add(f.createStatement(iri, hasAddedToLibrary, date, graphIRI));
        statements.add(f.createStatement(iri, hasModifiedDate, date, graphIRI));
        
        return uri;
    }
    
    String addMetadataCategory (MetadataCategory metadata, MetadataTemplateType metadataType, 
            String templatePredicate, String typePredicate, String prefix, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (user == null) {
            // cannot add 
            throw new SparqlException ("The user must be provided to put data into private repository");
        }
        
        // check if there is already a private graph for user
        graph = getGraphForUser(user);
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI graphIRI = f.createIRI(graph);
        
        IRI hasTemplate = f.createIRI(templatePredicate);
        IRI type = f.createIRI(typePredicate);
        // check if the sample already exists in "default-graph", then we need to add a triple sample->has_public_uri->existingURI to the private repo
        String existing = getEntityByLabel(metadata.getName(), graph, sampleTypePredicate);
        if (existing == null) {
            // add to user's local repository
            List<Statement> statements = new ArrayList<Statement>();
            String metadataURI = addGenericInfo(metadata.getName(), metadata.getDescription(), statements, prefix, graph);
            // add template
            // find the template with given name and add the object property from sample to the metadatatemplate
            String templateURI = templateRepository.getTemplateByName(metadata.getTemplate(), metadataType);
            IRI sampleIRI = f.createIRI(metadataURI);
            if (templateURI != null) 
                statements.add(f.createStatement(sampleIRI, hasTemplate, f.createIRI(templateURI), graphIRI));
            statements.add(f.createStatement(sampleIRI, RDF.TYPE, type, graphIRI));
            addMetadata (metadataURI, metadata, statements, graph);
            sparqlDAO.addStatements(statements, graphIRI);
            
            return metadataURI;
        } else {
            //TODO
        }
        return existing;
    }

    @Override
    public String addSample(Sample sample, UserEntity user) throws SparqlException, SQLException {
        return addMetadataCategory (sample, MetadataTemplateType.SAMPLE, hasSampleTemplatePredicate, sampleTypePredicate, "SA", user);
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
        IRI hasDescriptor = f.createIRI(describedbyPredicate);
        if (metadata.getDescriptors() != null) {
            for (Descriptor descriptor: metadata.getDescriptors()) {
                if (descriptor == null) continue;
                if (descriptor.getValue() == null || descriptor.getValue().isEmpty()) continue;
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
    
    private String addDescriptorGroup(DescriptorGroup descriptorGroup, List<Statement> statements, String graph) throws SparqlException {
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI graphIRI = f.createIRI(graph);
        String descrGroupURI = generateUniqueURI(uriPrefix + "D", graph);
        IRI descrGroup = f.createIRI(descrGroupURI);
        
        IRI hasKey = f.createIRI(keyPredicate);
        IRI type = f.createIRI(complexDescriptionTypePredicate);
        IRI hasDescriptor = f.createIRI(hasDescriptionPredicate);
        
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
        
        statements.add(f.createStatement(descrGroup, RDF.TYPE, type, graphIRI)); 
        statements.add(f.createStatement(descrGroup, hasKey, templateIRI, graphIRI));
        
        for (Description descriptor: descriptorGroup.getDescriptors()) {
            if (descriptor == null) {
                continue; // skip null entries if any
            }
            
            if (descriptor.isGroup()) {
                String descrURI = addDescriptorGroup((DescriptorGroup)descriptor, statements, graph);
                statements.add(f.createStatement(descrGroup, hasDescriptor, f.createIRI(descrURI), graphIRI));
            } else {
                if (((Descriptor) descriptor).getValue() == null || ((Descriptor) descriptor).getValue().isEmpty()) continue;
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
        IRI hasValue = f.createIRI(valuePredicate);
        IRI hasKey = f.createIRI(keyPredicate);
        IRI hasUnit = f.createIRI(unitPredicate);
        
        Literal unit = descriptor.getUnit() == null ? null : f.createLiteral(descriptor.getUnit());
        
        if (descriptor.getKey().getUri() == null) {
            // try to get the uri from id
            if (descriptor.getKey().getId() != null) {
                descriptor.getKey().setUri(MetadataTemplateRepository.templatePrefix + descriptor.getKey().getId()) ;
            } else {
                throw new SparqlException ("Descriptor template info is missing");
            }
        }
        // check if template actually exists!
        DescriptionTemplate descTemplate = templateRepository.getDescriptionFromURI(descriptor.getKey().getUri());
        if (descTemplate == null) {
            throw new SparqlException ("Descriptor template " + descriptor.getKey().getId() + " does not exist in the repository");
        }
        
        IRI descriptorTemplateIRI = f.createIRI(descriptor.getKey().getUri());
        Literal dValue = f.createLiteral(descriptor.getValue());
        statements.add(f.createStatement(descr, RDF.TYPE, f.createIRI(simpleDescriptionTypePredicate), graphIRI));
        statements.add(f.createStatement(descr, hasKey, descriptorTemplateIRI, graphIRI));
        statements.add(f.createStatement(descr, hasValue, dValue, graphIRI));
        if (unit != null) statements.add(f.createStatement(descr, hasUnit, unit, graphIRI));
        
        return descrURI;     
    }


    @Override
    public ArrayDataset getArrayDataset(String datasetId, UserEntity user) throws SparqlException, SQLException {
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

    private ArrayDataset getDatasetFromURI(String uri, UserEntity user) throws SparqlException, SQLException {
        
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
                datasetObject.setSample((Sample) getMetadataCategoryFromURI(uriValue.stringValue(), sampleTypePredicate, user));            
            } else if (st.getPredicate().equals(hasRawData)) {
                Value uriValue = st.getObject();
                datasetObject.getRawDataList().add(getRawData(uriValue.stringValue(), graph));        
            } else if (st.getPredicate().equals(hasSlide)) {
                Value uriValue = st.getObject();
                datasetObject.getSlides().add(getSlide(uriValue.stringValue(), graph));            
            } else if (st.getPredicate().equals(hasProcessedData)) {
                Value uriValue = st.getObject();
                datasetObject.setProcessedData(getProcessedDataFromURI(uriValue.stringValue(), user));            
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
                        datasetObject.setSample((Sample) getMetadataCategoryFromURI(uriValue.stringValue(), sampleTypePredicate, null));            
                    } else if (stPublic.getPredicate().equals(hasRawData)) {
                        uriValue = st.getObject();
                        datasetObject.getRawDataList().add(getRawData(uriValue.stringValue(), DEFAULT_GRAPH));        
                    } else if (stPublic.getPredicate().equals(hasSlide)) {
                        uriValue = st.getObject();
                        datasetObject.getSlides().add(getSlide(uriValue.stringValue(), DEFAULT_GRAPH));            
                    } else if (stPublic.getPredicate().equals(hasProcessedData)) {
                        uriValue = st.getObject();
                        datasetObject.setProcessedData(getProcessedDataFromURI(uriValue.stringValue(), null));            
                    }
                }
            }
        }
        
        return datasetObject;
    }
    
    private Description getDescriptionFromURI(String uri, String graph) throws SparqlException {
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
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(descriptorIRI, null, null, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next();
            if (st.getPredicate().equals(RDF.TYPE)) {
                String value = st.getObject().stringValue();
                if (value.contains("simple")) {
                    descriptorObject = new Descriptor();
                } else if (value.contains("complex")) {
                    descriptorObject = new DescriptorGroup();
                }
            } else if (st.getPredicate().equals(hasKey)) {
                // retrieve descriptorTemplate from template repository
                String tempURI = st.getObject().stringValue();
                DescriptionTemplate key = templateRepository.getDescriptionFromURI(tempURI);
                descriptorObject.setKey(key);
            } else if (st.getPredicate().equals(hasValue)) {
                String val = st.getObject().stringValue();
                ((Descriptor)descriptorObject).setValue(val);
            } else if (st.getPredicate().equals(hasUnit)) {
                String val = st.getObject().stringValue();
                ((Descriptor)descriptorObject).setUnit(val);
            } else if (st.getPredicate().equals(hasDescriptor)) {
                String descURI = st.getObject().stringValue();
                Description d = getDescriptionFromURI(descURI, graph);
                if (d.isGroup()) 
                    descriptorGroupList.add(d);
                else 
                    descriptorList.add(d);
            }
        }
        descriptorObject.setUri(uri);
        descriptorObject.setId(uri.substring(uri.lastIndexOf("/")+1));
        if (descriptorObject.isGroup()) {
            ((DescriptorGroup) descriptorObject).setDescriptors(new ArrayList<Description>());
            ((DescriptorGroup) descriptorObject).getDescriptors().addAll(descriptorList);
            ((DescriptorGroup) descriptorObject).getDescriptors().addAll(descriptorGroupList);
        }
        return descriptorObject;
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
        
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else
            graph = getGraphForUser(user);
        if (graph != null) {
            
            List<SparqlEntity> results = retrieveByTypeAndUser(offset, limit, field, order, searchValue, graph, datasetTypePredicate);
            
            for (SparqlEntity sparqlEntity : results) {
                String uri = sparqlEntity.getValue("s");
                ArrayDataset dataset = getDatasetFromURI(uri, user);
                if (dataset != null)
                    datasets.add(dataset);    
            }
        }
        
        return datasets;
    }
    
    @Override
    public int getArrayDatasetCountByUser(UserEntity user) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        return getCountByUserByType(graph, datasetTypePredicate);
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


    @Override
    public List<Sample> getSampleByUser(UserEntity user) throws SparqlException, SQLException {
        return getSampleByUser(user, 0, -1, "id", 0 );
    }


    @Override
    public List<Sample> getSampleByUser(UserEntity user, int offset, int limit, String field, int order)
            throws SparqlException, SQLException {
        return getSampleByUser(user, offset, limit, field, order, null);
    }

    public List<MetadataCategory> getMetadataCategoryByUser (UserEntity user, int offset, int limit, String field, int order,
            String searchValue, String typePredicate) throws SparqlException, SQLException {
        List<MetadataCategory> list = new ArrayList<>();
        
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else
            graph = getGraphForUser(user);
        if (graph != null) {
            
            List<SparqlEntity> results = retrieveByTypeAndUser(offset, limit, field, order, searchValue, graph, typePredicate);
            
            for (SparqlEntity sparqlEntity : results) {
                String uri = sparqlEntity.getValue("s");
                MetadataCategory metadata = getMetadataCategoryFromURI(uri, typePredicate, user);
                if (metadata != null)
                    list.add(metadata);    
            }
        }
        
        return list;
    }

    private MetadataCategory getMetadataCategoryFromURI(String uri, String typePredicate, UserEntity user) throws SparqlException, SQLException {
        MetadataCategory sampleObject = null;
        
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
        IRI sample = f.createIRI(uri);
        IRI graphIRI = f.createIRI(graph);
        IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
        IRI hasPublicURI = f.createIRI(ontPrefix + "has_public_uri");
        IRI hasCreatedDate = f.createIRI(ontPrefix + "has_date_created");
        IRI hasAddedToLibrary = f.createIRI(ontPrefix + "has_date_addedtolibrary");
        IRI hasModifiedDate = f.createIRI(ontPrefix + "has_date_modified");
        IRI createdBy= f.createIRI(ontPrefix + "created_by");
        IRI hasDescriptor = f.createIRI(describedbyPredicate);
        
        String templatePredicate = null;
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(sample, null, null, graphIRI);
        if (statements.hasNext()) {
            if (typePredicate.equals(sampleTypePredicate)) {
                sampleObject = new Sample();
                templatePredicate = hasSampleTemplatePredicate;
            } else if (typePredicate.equals(scannerTypePredicate)) {
                sampleObject = new ScannerMetadata();
                templatePredicate = hasScannerleTemplatePredicate;
            } else if (typePredicate.equals(printerTypePredicate)) {
                sampleObject = new Printer();
                templatePredicate = hasPrinterTemplatePredicate;
            } else if (typePredicate.equals(slideTemplateTypePredicate)) {
                sampleObject = new SlideMetadata();
                templatePredicate = hasSlideTemplatePredicate;
            } else if (typePredicate.equals(imageAnalysisTypePredicate)) {
                sampleObject = new ImageAnalysisSoftware();
                templatePredicate = hasImageTemplatePredicate;
            } else if (typePredicate.equals(dataProcessingTypePredicate)) {
                sampleObject = new DataProcessingSoftware();
                templatePredicate = hasDataprocessingTemplatePredicate;
            }
            sampleObject.setUri(uri);
            sampleObject.setId(uri.substring(uri.lastIndexOf("/")+1));
            sampleObject.setDescriptors(new ArrayList<Descriptor>());
            sampleObject.setDescriptorGroups(new ArrayList<DescriptorGroup>());
            if (user != null) {
                Creator owner = new Creator ();
                owner.setUserId(user.getUserId());
                owner.setName(user.getUsername());
                sampleObject.setUser(owner);
            } else {
                sampleObject.setPublic(true);
            }
        }
        
        IRI hasTemplate = f.createIRI(templatePredicate);
        
        while (statements.hasNext()) {
            Statement st = statements.next();
            if (st.getPredicate().equals(RDFS.LABEL)) {
                Value label = st.getObject();
                sampleObject.setName(label.stringValue());
            } else if (st.getPredicate().equals(createdBy)) {
                Value label = st.getObject();
                Creator creator = new Creator();
                creator.setName(label.stringValue());
                sampleObject.setUser(creator);
            } else if (st.getPredicate().equals(RDFS.COMMENT)) {
                Value comment = st.getObject();
                sampleObject.setDescription(comment.stringValue());
            } else if (st.getPredicate().equals(hasCreatedDate)) {
                Value value = st.getObject();
                if (value instanceof Literal) {
                    Literal literal = (Literal)value;
                    XMLGregorianCalendar calendar = literal.calendarValue();
                    Date date = calendar.toGregorianCalendar().getTime();
                    sampleObject.setDateCreated(date);
                }
            } else if (st.getPredicate().equals(hasModifiedDate)) {
                Value value = st.getObject();
                if (value instanceof Literal) {
                    Literal literal = (Literal)value;
                    XMLGregorianCalendar calendar = literal.calendarValue();
                    Date date = calendar.toGregorianCalendar().getTime();
                    sampleObject.setDateModified(date);
                }
            } else if (st.getPredicate().equals(hasAddedToLibrary)) {
                Value value = st.getObject();
                if (value instanceof Literal) {
                    Literal literal = (Literal)value;
                    XMLGregorianCalendar calendar = literal.calendarValue();
                    Date date = calendar.toGregorianCalendar().getTime();
                    sampleObject.setDateAddedToLibrary(date);
                }
            } else if (st.getPredicate().equals(hasTemplate)) {
                Value uriValue = st.getObject();
                String templateuri = uriValue.stringValue();
                String id = templateuri.substring(templateuri.lastIndexOf("#")+1);
                sampleObject.setTemplate(id);  
                MetadataTemplate template = templateRepository.getTemplateFromURI(templateuri);
                if (template != null)
                    sampleObject.setTemplateType(template.getName());
            } else if (st.getPredicate().equals(hasDescriptor)) {
                Value value = st.getObject();
                Description descriptor = getDescriptionFromURI (value.stringValue(), graph);
                if (descriptor.isGroup()) {
                    sampleObject.getDescriptorGroups().add((DescriptorGroup)descriptor);
                } else {
                    sampleObject.getDescriptors().add((Descriptor) descriptor);
                }
            } else if (st.getPredicate().equals(hasPublicURI)) {
                // need to retrieve additional information from DEFAULT graph
                // that means the sample is already public
                sampleObject.setPublic(true);  
                Value uriValue = st.getObject();
                String publicURI = uriValue.stringValue();
                IRI publicIRI = f.createIRI(publicURI);
                RepositoryResult<Statement> statementsPublic = sparqlDAO.getStatements(publicIRI, null, null, defaultGraphIRI);
                while (statementsPublic.hasNext()) {
                    Statement stPublic = statementsPublic.next();
                    if (stPublic.getPredicate().equals(hasTemplate)) {
                        uriValue = st.getObject();
                        String id = uriValue.stringValue().substring(uriValue.stringValue().lastIndexOf("#")+1);
                        sampleObject.setTemplate(id);     
                        MetadataTemplate template = templateRepository.getTemplateFromURI(uriValue.stringValue());
                        if (template != null)
                            sampleObject.setTemplateType(template.getName());
                    } else if (stPublic.getPredicate().equals(RDFS.LABEL)) {
                        Value label = stPublic.getObject();
                        sampleObject.setName(label.stringValue());
                    }  else if (stPublic.getPredicate().equals(RDFS.COMMENT)) {
                        Value comment = stPublic.getObject();
                        sampleObject.setDescription(comment.stringValue());
                    } else if (stPublic.getPredicate().equals(hasDescriptor)) {
                        Value value = stPublic.getObject();
                        Description descriptor = getDescriptionFromURI (value.stringValue(), DEFAULT_GRAPH);
                        if (descriptor.isGroup()) {
                            sampleObject.getDescriptorGroups().add((DescriptorGroup)descriptor);
                        } else {
                            sampleObject.getDescriptors().add((Descriptor) descriptor);
                        }
                    }
                }
            }
        }
        
        return sampleObject;
    }


    @Override
    public List<Sample> getSampleByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue) throws SparqlException, SQLException {
        List<Sample> samples = new ArrayList<>();
        
        List<MetadataCategory> list = getMetadataCategoryByUser(user, offset, limit, field, order, searchValue, sampleTypePredicate);
        for (MetadataCategory m: list) {
            samples.add(new Sample(m));
        }
        
        return samples;
    }


    private List<SparqlEntity> retrieveByTypeAndUser(int offset, int limit, String field, int order, String searchValue,
            String graph, String type) throws SparqlException {
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
                " ?s rdf:type <" + type + "> . \n" +
                        sortLine + searchPredicate + 
                "}\n" +
                 orderByLine + 
                ((limit == -1) ? " " : " LIMIT " + limit) +
                " OFFSET " + offset);
        
        return sparqlDAO.query(queryBuf.toString());
    }

    @Override
    public int getSampleCountByUser(UserEntity user) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        return getCountByUserByType(graph, sampleTypePredicate);
    }


    @Override
    public String addPrinter(Printer metadata, UserEntity user) throws SparqlException, SQLException {
        return addMetadataCategory (metadata, MetadataTemplateType.PRINTER, hasPrinterTemplatePredicate, printerTypePredicate, "Pr", user);
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
        List<Printer> printers = new ArrayList<>();
        
        List<MetadataCategory> list = getMetadataCategoryByUser(user, offset, limit, field, order, searchValue, printerTypePredicate);
        for (MetadataCategory m: list) {
            printers.add(new Printer(m));
        }
        
        return printers;
    }

    @Override
    public int getPrinterCountByUser(UserEntity user) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        return getCountByUserByType(graph, printerTypePredicate);
    }


    @Override
    public String addScannerMetadata(ScannerMetadata metadata, UserEntity user) throws SparqlException, SQLException {
        return addMetadataCategory (metadata, MetadataTemplateType.SCANNER, hasScannerleTemplatePredicate, scannerTypePredicate, "Sc", user);
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
        List<ScannerMetadata> metadataList = new ArrayList<>();
        
        List<MetadataCategory> list = getMetadataCategoryByUser(user, offset, limit, field, order, searchValue, scannerTypePredicate);
        for (MetadataCategory m: list) {
            metadataList.add(new ScannerMetadata(m));
        }
        
        return metadataList;
    }

    @Override
    public int getScannerMetadataCountByUser(UserEntity user) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        return getCountByUserByType(graph, scannerTypePredicate);
    }


    @Override
    public String addSlideMetadata(SlideMetadata metadata, UserEntity user) throws SparqlException, SQLException {
        return addMetadataCategory (metadata, MetadataTemplateType.SCANNER, hasSlideTemplatePredicate, slideTemplateTypePredicate, "Slm", user);
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
        List<SlideMetadata> metadataList = new ArrayList<>();
        
        List<MetadataCategory> list = getMetadataCategoryByUser(user, offset, limit, field, order, searchValue, slideTemplateTypePredicate);
        for (MetadataCategory m: list) {
            metadataList.add(new SlideMetadata(m));
        }
        
        return metadataList;
    }

    @Override
    public int getSlideMetadataCountByUser(UserEntity user) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        return getCountByUserByType(graph, slideTemplateTypePredicate);
    }


    @Override
    public String addImageAnalysisSoftware(ImageAnalysisSoftware metadata, UserEntity user)
            throws SparqlException, SQLException {
        return addMetadataCategory (metadata, MetadataTemplateType.IMAGEANALYSISSOFTWARE, hasImageTemplatePredicate, imageAnalysisTypePredicate, "Im", user);
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
        List<ImageAnalysisSoftware> metadataList = new ArrayList<>();
        
        List<MetadataCategory> list = getMetadataCategoryByUser(user, offset, limit, field, order, searchValue, imageAnalysisTypePredicate);
        for (MetadataCategory m: list) {
            metadataList.add(new ImageAnalysisSoftware(m));
        }
        
        return metadataList;
    }

    @Override
    public int getImageAnalysisSoftwareCountByUser(UserEntity user) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        return getCountByUserByType(graph, imageAnalysisTypePredicate);
    }


    @Override
    public String addDataProcessingSoftware(DataProcessingSoftware metadata, UserEntity user)
            throws SparqlException, SQLException {
        return addMetadataCategory (metadata, MetadataTemplateType.DATAPROCESSINGSOFTWARE, hasDataprocessingTemplatePredicate, dataProcessingTypePredicate, "DPS", user);
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
        List<DataProcessingSoftware> metadataList = new ArrayList<>();
        
        List<MetadataCategory> list = getMetadataCategoryByUser(user, offset, limit, field, order, searchValue, dataProcessingTypePredicate);
        for (MetadataCategory m: list) {
            metadataList.add(new DataProcessingSoftware(m));
        }
        
        return metadataList;
    }

    @Override
    public int getDataProcessingSoftwareCountByUser(UserEntity user) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        return getCountByUserByType(graph, dataProcessingTypePredicate);
    }
    
    private String addImage(Image image, List<Statement> statements, String graph) throws SparqlException {
        ValueFactory f = sparqlDAO.getValueFactory();
        String imageURI = generateUniqueURI(uriPrefix + "I", graph);
        String scannerMetadataURI = null;
        if (image.getScanner() != null) {
            scannerMetadataURI = image.getScanner().getUri();
        }
        
        IRI graphIRI = f.createIRI(graph);
        IRI imageIRI = f.createIRI(imageURI);
        IRI metadataIRI = scannerMetadataURI == null ? null : f.createIRI(scannerMetadataURI);
        IRI hasFileName = f.createIRI(hasFilePredicate);
        IRI hasScanner = f.createIRI(scannerMetadataPredicate);
        
        Literal filename = image.getFileName() == null ? null : f.createLiteral(image.getFileName());
        
        
        if (filename != null) statements.add(f.createStatement(imageIRI, hasFileName, filename, graphIRI));
        if (metadataIRI != null) statements.add(f.createStatement(imageIRI, hasScanner, metadataIRI, graphIRI));
    
        return imageURI;
    }
    
    public ProcessedData getProcessedDataFromURI(String uriValue, UserEntity user) throws SQLException, SparqlException {
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
        IRI processedData = f.createIRI(uriValue);
        IRI graphIRI = f.createIRI(graph);
        IRI hasRFU = f.createIRI(rfuPredicate);
        IRI hasStdev = f.createIRI(stdevPredicate);
        IRI hasCV = f.createIRI(cvPredicate);
        IRI hasIntensity = f.createIRI(hasIntensityPredicate);
        IRI bindingValueOf = f.createIRI(bindingValuePredicate);
        IRI integrates = f.createIRI(integratesPredicate);
        IRI integratedBy = f.createIRI(integratedByPredicate);   //TODO should we add statisticalMethod to the intensity???
        //TODO what about concentration level
        IRI hasProcessingSWMetadata = f.createIRI(processingSoftwareMetadataPredicate);
        
        ProcessedData processedObject = new ProcessedData();
        List<Intensity> intensities = new ArrayList<Intensity>();
        processedObject.setIntensity(intensities);
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(processedData, null, null, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next();
            if (st.getPredicate().equals(hasIntensity)) {
                String intensityURI = st.getObject().stringValue();
                Intensity intensity = new Intensity();
                RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(f.createIRI(intensityURI), null, null, graphIRI);
                if (statements2.hasNext()) {
                    intensities.add(intensity);
                }
                while (statements2.hasNext()) {
                    Statement st2 = statements2.next();
                    if (st2.getPredicate().equals(bindingValueOf)) {
                        String spotURI = st2.getObject().stringValue();
                        intensity.setSpot(layoutRepository.getSpotFromURI(spotURI, user));
                    } else if (st2.getPredicate().equals(hasRFU)) {
                        Value val = st2.getObject();
                        try {
                            intensity.setRfu(Double.parseDouble(val.stringValue()));
                        } catch (NumberFormatException e) {
                            logger.error("rfu should be a double in the repository!", e);
                        }
                    } else if (st2.getPredicate().equals(hasStdev)) {
                        Value val = st2.getObject();
                        try {
                            intensity.setStDev(Double.parseDouble(val.stringValue()));
                        } catch (NumberFormatException e) {
                            logger.error("stdev should be a double in the repository!", e);
                        }
                    } else if (st2.getPredicate().equals(integrates)) {
                        // TODO get measurements
                        
                    } else if (st2.getPredicate().equals(hasCV)) {
                        Value val = st2.getObject();
                        try {
                            intensity.setPercentCV(Double.parseDouble(val.stringValue()));
                        } catch (NumberFormatException e) {
                            logger.error("%CV should be a double in the repository!", e);
                        }
                    }
                }
                                
            } else if (st.getPredicate().equals(hasProcessingSWMetadata)) {
                String metadataURI = st.getObject().stringValue();
                DataProcessingSoftware metadata = getDataProcessingSoftwareFromURI(metadataURI, user);
                processedObject.setMetadata(metadata);
            }
        }
        
        return processedObject;
    }


    private Slide getSlide(String uriValue, String graph) {
        // TODO Auto-generated method stub
        return null;
    }


    private RawData getRawData(String uriValue, String graph) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void deleteArrayDataset(String datasetId, UserEntity user) throws SparqlException, SQLException {
        // TODO Auto-generated method stub  
    }
    
    @Override
    public void deleteMetadata (String metadataId, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        if (graph != null) {
            if (canDeleteMetadata(uriPrefix + metadataId, graph)) {
                String uri = uriPrefix + metadataId;
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
            } else {
                throw new IllegalArgumentException("Cannot delete metadata " + metadataId + ". It is used in an experiment");
            }
        }
        
        
    }
    
    private boolean canDeleteMetadata(String uri, String graph) throws SparqlException {
        boolean canDelete = true;
        
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
        queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ("{?s gadr:has_sample <" +  uri + "> } ");
        queryBuf.append ("UNION {?s gadr:has_image_processing_metadata <" + uri +">  } ");
        queryBuf.append ("UNION {?s gadr:has_slide_metadata <" + uri +">  } ");
        queryBuf.append ("UNION {?s gadr:has_scanner_metadata <" + uri +">  } ");
        queryBuf.append ("UNION {?s gadr:printed_by <" + uri +">  } ");
        queryBuf.append ("UNION {?s gadr:has_processing_software_metadata <" + uri +">  } ");
        queryBuf.append ("} LIMIT 1");
        
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        if (!results.isEmpty())
            canDelete = false;
        
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
            String uri = results.get(0).getValue("s");
            return getMetadataCategoryFromURI(uri, typePredicate, user);
        }
    }


    @Override
    public Sample getSampleByLabel(String label, UserEntity user) throws SparqlException, SQLException {
        MetadataCategory metadata = getMetadataByLabel(label, sampleTypePredicate, user);
        return metadata == null? null : (Sample) metadata;
    }


    @Override
    public Sample getSampleFromURI(String uri, UserEntity user) throws SparqlException, SQLException {
        return (Sample) getMetadataCategoryFromURI(uri, sampleTypePredicate, user);
    }


    @Override
    public Printer getPrinterFromURI(String uri, UserEntity user) throws SparqlException, SQLException {
        return (Printer) getMetadataCategoryFromURI(uri, printerTypePredicate, user);
    }


    @Override
    public ScannerMetadata getScannerMetadataFromURI(String uri, UserEntity user) throws SparqlException, SQLException {
        return (ScannerMetadata) getMetadataCategoryFromURI(uri, scannerTypePredicate, user);
    }


    @Override
    public SlideMetadata getSlideMetadataFromURI(String uri, UserEntity user) throws SparqlException, SQLException {
        return (SlideMetadata) getMetadataCategoryFromURI(uri, slideTemplateTypePredicate, user);
    }


    @Override
    public ImageAnalysisSoftware getImageAnalysisSoftwareFromURI(String uri, UserEntity user)
            throws SparqlException, SQLException {
        return (ImageAnalysisSoftware) getMetadataCategoryFromURI(uri, imageAnalysisTypePredicate, user);
    }


    @Override
    public DataProcessingSoftware getDataProcessingSoftwareFromURI(String uri, UserEntity user)
            throws SparqlException, SQLException {
        return (DataProcessingSoftware) getMetadataCategoryFromURI(uri, dataProcessingTypePredicate, user);
    }
    
    @Override
    public void updateMetadata (MetadataCategory metadata, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        
        // delete all existing descriptors for the sample and add them back
        String uri = null;
        if (metadata.getUri() != null)
            uri = metadata.getUri();
        else
            uri = uriPrefix + metadata.getId();
        
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
        Literal label = metadata.getName() == null ? null : f.createLiteral(metadata.getName());
        Literal comment = metadata.getDescription() == null ? null : f.createLiteral(metadata.getDescription());
        
        // add updated name/description
        List<Statement> statements = new ArrayList<Statement>();
        if (label != null) statements.add(f.createStatement(metadataIRI, RDFS.LABEL, label, graphIRI));
        if (comment != null) statements.add(f.createStatement(metadataIRI, RDFS.COMMENT, comment, graphIRI));
        statements.add(f.createStatement(metadataIRI, hasModifiedDate, date, graphIRI));
        
        // add descriptors back
        addMetadata (uri, metadata, statements, graph);
        sparqlDAO.addStatements(statements, graphIRI);
    }


    @Override
    public Printer getPrinterByLabel(String label, UserEntity user) throws SparqlException, SQLException {
        MetadataCategory metadata = getMetadataByLabel(label, printerTypePredicate, user);
        return metadata == null? null : (Printer) metadata;
    }


    @Override
    public ScannerMetadata getScannerMetadataByLabel(String label, UserEntity user) throws SparqlException, SQLException {
        MetadataCategory metadata = getMetadataByLabel(label, scannerTypePredicate, user);
        return metadata == null? null : (ScannerMetadata) metadata;
    }


    @Override
    public SlideMetadata getSlideMetadataByLabel(String label, UserEntity user) throws SparqlException, SQLException {
        MetadataCategory metadata = getMetadataByLabel(label, slideTemplateTypePredicate, user);
        return metadata == null? null : (SlideMetadata) metadata;
    }


    @Override
    public ImageAnalysisSoftware getImageAnalysisSoftwarByLabel(String label, UserEntity user) throws SparqlException, SQLException {
        MetadataCategory metadata = getMetadataByLabel(label, imageAnalysisTypePredicate, user);
        return metadata == null? null : (ImageAnalysisSoftware) metadata;
    }


    @Override
    public DataProcessingSoftware getDataProcessingSoftwareByLabel(String label, UserEntity user) throws SparqlException, SQLException {
        MetadataCategory metadata = getMetadataByLabel(label, dataProcessingTypePredicate, user);
        return metadata == null? null : (DataProcessingSoftware) metadata;
    }


    @Override
    public void updateMetadataMirage(MetadataCategory metadata, UserEntity user) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        
        String uri = null;
        if (metadata.getUri() != null)
            uri = metadata.getUri();
        else
            uri = uriPrefix + metadata.getId();
        
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


    @Override
    public List<PrintedSlide> getPrintedSlideByUser(UserEntity user) throws SparqlException, SQLException {
        return getPrintedSlideByUser(user, 0, -1, "id", 0);
    }


    @Override
    public List<PrintedSlide> getPrintedSlideByUser(UserEntity user, int offset, int limit, String field, int order)
            throws SparqlException, SQLException {
        return getPrintedSlideByUser(user, offset, limit, field, order, null);
    }


    @Override
    public List<PrintedSlide> getPrintedSlideByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue) throws SparqlException, SQLException {
        List<PrintedSlide> slides = new ArrayList<>();
        
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else
            graph = getGraphForUser(user);
        if (graph != null) {
            List<SparqlEntity> results = retrieveByTypeAndUser(offset, limit, field, order, searchValue, graph, printedSlideTypePredicate);
            
            for (SparqlEntity sparqlEntity : results) {
                String uri = sparqlEntity.getValue("s");
                PrintedSlide slide = getPrintedSlideFromURI(uri, user);
                if (slide != null)
                    slides.add(slide);    
            }
        }
        
        return slides;
    }


    @Override
    public PrintedSlide getPrintedSlideFromURI(String uri, UserEntity user) throws SparqlException, SQLException {
        PrintedSlide slideObject = new PrintedSlide();
        
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
        IRI graphIRI = f.createIRI(graph);
        IRI defaultGraphIRI = f.createIRI(DEFAULT_GRAPH);
        IRI iri = f.createIRI(uri);
        IRI hasSlideMetadata = f.createIRI(slideMetadataPredicate);
        IRI printedBy = f.createIRI(printerMetadataPredicate);
        IRI hasSlideLayout = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_slide_layout");
        IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
        IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
        IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
        IRI hasPublicURI = f.createIRI(ontPrefix + "has_public_uri");
        IRI createdBy= f.createIRI(ontPrefix + "created_by");
        
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(iri, null, null, graphIRI);
        if (statements.hasNext()) {
            slideObject.setUri(uri);
            slideObject.setId(uri.substring(uri.lastIndexOf("/")+1));
            if (user != null) {
                Creator owner = new Creator ();
                owner.setUserId(user.getUserId());
                owner.setName(user.getUsername());
                slideObject.setUser(owner);
            } else {
                slideObject.setPublic(true);
            }
        }
    
        while (statements.hasNext()) {
            Statement st = statements.next();
            if (st.getPredicate().equals(RDFS.LABEL)) {
                Value label = st.getObject();
                slideObject.setName(label.stringValue());
            } else if (st.getPredicate().equals(createdBy)) {
                Value label = st.getObject();
                Creator creator = new Creator();
                creator.setName(label.stringValue());
                slideObject.setUser(creator);
            } else if (st.getPredicate().equals(RDFS.COMMENT)) {
                Value comment = st.getObject();
                slideObject.setDescription(comment.stringValue());
            } else if (st.getPredicate().equals(hasCreatedDate)) {
                Value value = st.getObject();
                if (value instanceof Literal) {
                    Literal literal = (Literal)value;
                    XMLGregorianCalendar calendar = literal.calendarValue();
                    Date date = calendar.toGregorianCalendar().getTime();
                    slideObject.setDateCreated(date);
                }
            } else if (st.getPredicate().equals(hasModifiedDate)) {
                Value value = st.getObject();
                if (value instanceof Literal) {
                    Literal literal = (Literal)value;
                    XMLGregorianCalendar calendar = literal.calendarValue();
                    Date date = calendar.toGregorianCalendar().getTime();
                    slideObject.setDateModified(date);
                }
            } else if (st.getPredicate().equals(hasAddedToLibrary)) {
                Value value = st.getObject();
                if (value instanceof Literal) {
                    Literal literal = (Literal)value;
                    XMLGregorianCalendar calendar = literal.calendarValue();
                    Date date = calendar.toGregorianCalendar().getTime();
                    slideObject.setDateAddedToLibrary(date);
                }
            } else if (st.getPredicate().equals(hasSlideLayout)) {
                Value uriValue = st.getObject();
                String layoutURI = uriValue.stringValue();
                String id = layoutURI.substring(layoutURI.lastIndexOf("#")+1);
                SlideLayout layout = layoutRepository.getSlideLayoutById(id, user, false);
                slideObject.setLayout(layout);
            } else if (st.getPredicate().equals(hasSlideMetadata)) {
                Value value = st.getObject();
                SlideMetadata slideMetadata = getSlideMetadataFromURI(value.stringValue(), user);
                slideObject.setMetadata(slideMetadata);
            } else if (st.getPredicate().equals(printedBy)) {
                Value value = st.getObject();
                Printer metadata = getPrinterFromURI(value.stringValue(), user);
                slideObject.setPrinter(metadata);
            } else if (st.getPredicate().equals(hasPublicURI)) {
                // need to retrieve additional information from DEFAULT graph
                // that means the printed slide is already public
                slideObject.setPublic(true);  
                Value uriValue = st.getObject();
                String publicURI = uriValue.stringValue();
                IRI publicIRI = f.createIRI(publicURI);
                RepositoryResult<Statement> statementsPublic = sparqlDAO.getStatements(publicIRI, null, null, defaultGraphIRI);
                while (statementsPublic.hasNext()) {
                    Statement stPublic = statementsPublic.next();
                    if (stPublic.getPredicate().equals(hasSlideLayout)) {
                        uriValue = st.getObject();
                        String layoutURI = uriValue.stringValue();
                        String id = layoutURI.substring(layoutURI.lastIndexOf("#")+1);
                        SlideLayout layout = layoutRepository.getSlideLayoutById(id, user, false);
                        slideObject.setLayout(layout);   
                    } else if (stPublic.getPredicate().equals(RDFS.LABEL)) {
                        Value label = stPublic.getObject();
                        slideObject.setName(label.stringValue());
                    }  else if (stPublic.getPredicate().equals(RDFS.COMMENT)) {
                        Value comment = stPublic.getObject();
                        slideObject.setDescription(comment.stringValue());
                    } else if (stPublic.getPredicate().equals(hasSlideMetadata)) {
                        Value value = stPublic.getObject();
                        SlideMetadata slideMetadata = getSlideMetadataFromURI(value.stringValue(), user);
                        slideObject.setMetadata(slideMetadata);
                    } else if (stPublic.getPredicate().equals(printedBy)) {
                        Value value = stPublic.getObject();
                        Printer metadata = getPrinterFromURI(value.stringValue(), user);
                        slideObject.setPrinter(metadata);
                    }
                }
            }
        }
        
        return slideObject;
    }


    @Override
    public int getPrintedSlideCountByUser(UserEntity user) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        return getCountByUserByType(graph, printedSlideTypePredicate);
    }
    
    @Override
    public void deletePrintedSlide (String slideId, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        if (graph != null) {
            if (canDeletePrintedSlide(uriPrefix + slideId, graph)) {
                String uri = uriPrefix + slideId;
                ValueFactory f = sparqlDAO.getValueFactory();
                IRI slide = f.createIRI(uri);
                IRI graphIRI = f.createIRI(graph);
                
                RepositoryResult<Statement> statements = sparqlDAO.getStatements(slide, null, null, graphIRI);
                sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
            } else {
                throw new IllegalArgumentException("Cannot delete printed slide " + slideId + ". It is used in an experiment");
            }
        }
        
        
    }
    
    private boolean canDeletePrintedSlide(String uri, String graph) throws SparqlException {
        boolean canDelete = true;
        
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
        queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ("{?s gadr:has_printed_slide <" +  uri + "> } ");
        queryBuf.append ("} LIMIT 1");
        
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        if (!results.isEmpty())
            canDelete = false;
        
        return canDelete;
    }
}
