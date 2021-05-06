package org.glygen.array.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.GraphPermissionEntity;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.Creator;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.Publication;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.data.ArrayDataset;
import org.glygen.array.persistence.rdf.data.FileWrapper;
import org.glygen.array.persistence.rdf.data.FutureTask;
import org.glygen.array.persistence.rdf.data.FutureTaskStatus;
import org.glygen.array.persistence.rdf.data.Grant;
import org.glygen.array.persistence.rdf.data.Image;
import org.glygen.array.persistence.rdf.data.Intensity;
import org.glygen.array.persistence.rdf.data.IntensityData;
import org.glygen.array.persistence.rdf.data.Measurement;
import org.glygen.array.persistence.rdf.data.PrintedSlide;
import org.glygen.array.persistence.rdf.data.ProcessedData;
import org.glygen.array.persistence.rdf.data.RawData;
import org.glygen.array.persistence.rdf.data.Slide;
import org.glygen.array.persistence.rdf.data.StatisticalMethod;
import org.glygen.array.persistence.rdf.metadata.AssayMetadata;
import org.glygen.array.persistence.rdf.metadata.DataProcessingSoftware;
import org.glygen.array.persistence.rdf.metadata.ImageAnalysisSoftware;
import org.glygen.array.persistence.rdf.metadata.MetadataCategory;
import org.glygen.array.persistence.rdf.metadata.Printer;
import org.glygen.array.persistence.rdf.metadata.Sample;
import org.glygen.array.persistence.rdf.metadata.ScannerMetadata;
import org.glygen.array.persistence.rdf.metadata.SlideMetadata;
import org.glygen.array.persistence.rdf.template.MetadataTemplateType;
import org.glygen.array.view.ErrorMessage;
import org.grits.toolbox.glycanarray.om.model.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(value="sesameTransactionManager") 
public class ArrayDatasetRepositoryImpl extends GlygenArrayRepositoryImpl implements ArrayDatasetRepository {
    
    public final static String datasetTypePredicate = ontPrefix + "array_dataset";
    public final static String printedSlideTypePredicate = ontPrefix + "printed_slide";
    public final static String rawdataTypePredicate = ontPrefix + "raw_data";
    public final static String processedDataTypePredicate = ontPrefix + "processed_data";
    
    
    //final static String hasDescriptorPredicate = ontPrefix + "has_descriptor";
    //final static String hasDescriptorGroupPredicate = ontPrefix + "has_descriptor_group";
    //public final static String namespacePredicate = ontPrefix + "has_namespace";
   
    public final static String rfuPredicate = ontPrefix + "has_rfu";
    public final static String stdevPredicate = ontPrefix + "has_stdev";
    public final static String cvPredicate = ontPrefix + "has_cv";
    public final static String hasIntensityPredicate = ontPrefix + "has_intensity";
    public final static String bindingValuePredicate = ontPrefix + "binding_value_of";
    public final static String integratedByPredicate = ontPrefix + "integrated_by";
    public final static String integratesPredicate = ontPrefix + "integrates";
    
    public final static String processedFromPredicate = ontPrefix + "processed_from";
    public final static String derivedFromPredicate = ontPrefix + "derived_from";
    public final static String hasMeasurementPredicate = ontPrefix + "has_measurement";
    public final static String measurementOfPredicate = ontPrefix + "measurement_of";
    
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
    
    public final static String hasFileNamePredicate = ontPrefix + "has_filename";
    public final static String hasOriginalFileNamePredicate = ontPrefix + "has_original_name";
    public final static String hasFolderPredicate = ontPrefix + "has_folder";
    public final static String hasFileFormatPredicate = ontPrefix + "has_file_format";
    public final static String hasSizePredicate = ontPrefix + "has_size";
    
    
    @Autowired
    QueryHelper queryHelper;
    
    @Autowired
    MetadataTemplateRepository templateRepository;
    
    @Autowired
    FeatureRepository featureRepository;
    
    @Autowired
    LayoutRepository layoutRepository;
    
    @Autowired
    MetadataRepository metadataRepository;
    
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
        String existing = getEntityByLabel(dataset.getName(), graph, datasetTypePredicate);
        if (existing == null) {
            // add to user's local repository
            List<Statement> statements = new ArrayList<Statement>();
            Date addedToLibrary = dataset.getDateAddedToLibrary() == null ? new Date() : dataset.getDateAddedToLibrary();
            String datasetURI = addGenericInfo(dataset.getName(), dataset.getDescription(), addedToLibrary, statements, "AD", graph);
            IRI arraydataset = f.createIRI(datasetURI);
            IRI graphIRI = f.createIRI(graph);
            IRI hasSample = f.createIRI(ontPrefix + "has_sample");
            IRI hasImage = f.createIRI(hasImagePredicate);
            IRI hasSlide = f.createIRI(ontPrefix + "has_slide");
            IRI type = f.createIRI(datasetTypePredicate);
            statements.add(f.createStatement(arraydataset, RDF.TYPE, type, graphIRI));
            
            String sampleURI = null;
            if (dataset.getSample().getUri() != null) {
                sampleURI = dataset.getSample().getUri();
                if (sampleURI == null && dataset.getSample().getId() != null) {
                    sampleURI = uriPrefix + dataset.getSample().getId();
                } 
                if (sampleURI == null) {
                    sampleURI = metadataRepository.addSample(dataset.getSample(), user);
                }
            }
            if (sampleURI != null) {
                IRI sample = f.createIRI(sampleURI);
                statements.add(f.createStatement(arraydataset, hasSample, sample, graphIRI));
                
            }
            
            String datasetId = datasetURI.substring(datasetURI.lastIndexOf("/")+1);
            if (dataset.getProcessedData() != null && dataset.getProcessedData().isEmpty()) {
                for (ProcessedData processedData: dataset.getProcessedData()) {
                    addProcessedData(processedData, datasetId, user);
                }
            }
            
            if (dataset.getRawDataList() != null && !dataset.getRawDataList().isEmpty()) {
                for (RawData rawData: dataset.getRawDataList()) {
                    addRawData(rawData, datasetId, user);
                }
            }
            
            if (dataset.getPublications() != null && !dataset.getPublications().isEmpty()) {
                for (Publication pub: dataset.getPublications()) {
                    addPublication(pub, datasetId, user);
                }
            }
            
            if (dataset.getCollaborators() != null && !dataset.getCollaborators().isEmpty()) {
                for (Creator collab: dataset.getCollaborators()) {
                    addCollaborator(collab, datasetId, user);
                }
            }
            
            if (dataset.getGrants() != null && !dataset.getGrants().isEmpty()) {
                for (Grant grant: dataset.getGrants()) {
                    addGrant(grant, datasetId, user);
                }
            }
            
            // slides and images are created with rawData
            // here we are only creating the relationship to the dataset
            if (dataset.getSlides() != null) {
                for (Slide slide: dataset.getSlides()) {
                    String slideURI = slide.getUri();
                    if (slideURI != null) {
                        IRI slideIRI = f.createIRI(slideURI);
                        statements.add(f.createStatement(arraydataset, hasSlide, slideIRI, graphIRI));
                    }
                }
            }
            
            //add images
            if (dataset.getImages() != null) {
                for (Image image: dataset.getImages()) {
                    String imageURI = image.getUri();
                    if (imageURI != null) {
                        IRI imageIRI = f.createIRI(imageURI);
                        statements.add(f.createStatement(arraydataset, hasImage, imageIRI, graphIRI));
                    }
                }
            }
            
            sparqlDAO.addStatements(statements, graphIRI);
            return datasetURI;
            
        } 
        return null;
    }
    
    String[] getGraphsForUser (UserEntity user, String datasetId) throws SQLException {
        if (user == null) {
            String[] allGraphs = new String[1];
            allGraphs[0] = DEFAULT_GRAPH;
            return allGraphs;
        } else {
            String coOwnerGraph = getCoownerGraphForUser(user, datasetId);
            String graph = getGraphForUser(user);
            if (coOwnerGraph != null) {
                String[] allGraphs = new String[2];
                allGraphs[0] = graph;
                allGraphs[1] = coOwnerGraph;
                return allGraphs;
            }
            else {
                String[] allGraphs = new String[1];
                allGraphs[0] = graph;
                return allGraphs;
            }
        }
    }

    @Override
    public String addGrant(Grant grant, String datasetId, UserEntity user) throws SparqlException, SQLException {
        String graph;
        String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        } else {
            graph = getGraphForUser(user);
        }
        String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI dataset = f.createIRI(uriPre + datasetId);
        IRI graphIRI = f.createIRI(graph);
        IRI hasIdentifier = f.createIRI(hasIdentiferPredicate);
        IRI hasOrg = f.createIRI(hasOrganizationPredicate);
        IRI hasGrantPred = f.createIRI(hasGrant);
        IRI hasURL = f.createIRI(hasURLPredicate);
        
        List<Statement> statements = new ArrayList<Statement>();
        String grantURI = generateUniqueURI(uriPre + "P", allGraphs);
        IRI grantIRI = f.createIRI(grantURI);
        Literal label = grant.getTitle() == null ? f.createLiteral("") : f.createLiteral(grant.getTitle());
        Literal organization = grant.getFundingOrganization() == null ? f.createLiteral("") : f.createLiteral(grant.getFundingOrganization());
        Literal identifier = grant.getIdentifier() == null ? f.createLiteral("") : f.createLiteral(grant.getIdentifier());
        Literal url = grant.getURL() == null ? f.createLiteral("") : f.createLiteral(grant.getURL());
        
        
        if (organization != null) statements.add(f.createStatement(grantIRI, hasOrg, organization, graphIRI));
        if (url != null) statements.add(f.createStatement(grantIRI, hasURL, url, graphIRI));
        if (identifier != null) statements.add(f.createStatement(grantIRI, hasIdentifier, identifier, graphIRI));
        if (label != null) statements.add(f.createStatement(grantIRI, RDFS.LABEL, label, graphIRI));
        
        statements.add(f.createStatement(dataset, hasGrantPred, grantIRI, graphIRI));
        sparqlDAO.addStatements(statements, graphIRI);
        return grantURI;
        
    }

    @Override
    public String getCoownerGraphForUser(UserEntity user, String datasetURI) {
        List<GraphPermissionEntity> entities = permissionRepository.findByUserAndResourceIRI(user, datasetURI);
        if (entities != null && !entities.isEmpty()) {
            return entities.get(0).getGraphIRI();
        }
        return null;
    }

    @Override
    public void addCollaborator(Creator collab, String datasetId, UserEntity user) throws SparqlException, SQLException {
        String graph;
        String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        } else {
            graph = getGraphForUser(user);
        }
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI dataset = f.createIRI(uriPre + datasetId);
        IRI graphIRI = f.createIRI(graph);
        IRI hasCollab = f.createIRI(hasCollaborator);
        
        
        List<Statement> statements = new ArrayList<Statement>();
        Literal username = collab.getName() == null ? f.createLiteral("") : f.createLiteral(collab.getName());
        
        
        statements.add(f.createStatement(dataset, hasCollab, username, graphIRI));
        sparqlDAO.addStatements(statements, graphIRI);
    }


    @Override
    public String addSlide(Slide slide, String datasetId, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        } else {
            // check if there is already a private graph for user
            graph = getGraphForUser(user);
        }
        String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
        ValueFactory f = sparqlDAO.getValueFactory();
        String slideURI = generateUniqueURI(uriPre + "S", allGraphs);
        
        IRI scanOf = f.createIRI(scanOfPredicate);
        IRI hasPrintedSlide = f.createIRI(hasPrintedSlidePredicate);
        IRI slideIRI = f.createIRI(slideURI);
        IRI graphIRI = f.createIRI(graph);
        IRI hasSlide = f.createIRI(ontPrefix + "has_slide");
        IRI hasAssay = f.createIRI(assayMetadataPredicate);
        
        List<Statement> statements = new ArrayList<Statement>();
        if (slide.getImages() != null) {
            for (Image image: slide.getImages()) {
                if (image.getUri() != null) {
                    statements.add(f.createStatement(f.createIRI(image.getUri()), scanOf, slideIRI, graphIRI));
                } else {
                    String imageURI = addImage (image, datasetId, user);
                    statements.add(f.createStatement(f.createIRI(imageURI), scanOf, slideIRI, graphIRI));
                }
            }
        }
        
        if (slide.getMetadata() != null && slide.getMetadata().getUri() != null) {
            statements.add(f.createStatement(slideIRI, hasAssay, f.createIRI(slide.getMetadata().getUri()), graphIRI));
        }
        
        if (slide.getPrintedSlide() != null) {
            PrintedSlide printedSlide = slide.getPrintedSlide();
            String printedSlideURI = null;
            if (printedSlide.getUri() != null) {
                printedSlideURI = printedSlide.getUri();
            } else {
                throw new SparqlException ("The printed slide should be provided");
            }
            statements.add(f.createStatement(slideIRI, hasPrintedSlide, f.createIRI(printedSlideURI), graphIRI));
        }
        
        IRI arraydataset = f.createIRI(uriPre + datasetId);
        statements.add(f.createStatement(arraydataset, hasSlide, slideIRI, graphIRI));
        sparqlDAO.addStatements(statements, graphIRI);
        return slideURI;
    }
    
    @Override
    public String addPrintedSlide (PrintedSlide printedSlide, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        } else {
            // check if there is already a private graph for user
            graph = getGraphForUser(user);
        }
        ValueFactory f = sparqlDAO.getValueFactory();
        String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
        IRI graphIRI = f.createIRI(graph);
        String printedSlideURI = generateUniqueURI(uriPre + "PS", allGraphs);
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
        Literal createdDate = date;
        if (printedSlide.getDateCreated() != null) { // this is the original created date, kept when it is made public
            // keep this 
            createdDate = f.createLiteral(printedSlide.getDateCreated());
        }
        
        List<Statement> statements = new ArrayList<Statement>();
        statements.add(f.createStatement(iri, RDF.TYPE, type));
        if (label != null) 
            statements.add(f.createStatement(iri, RDFS.LABEL, label, graphIRI));
        if (comment != null)
            statements.add(f.createStatement(iri, RDFS.COMMENT, comment, graphIRI));
        if (printedSlide.getLayout() != null) {
            String slideURI = printedSlide.getLayout().getUri();
            if (slideURI != null) {
                statements.add(f.createStatement(iri, hasSlideLayout, f.createIRI(printedSlide.getLayout().getUri()), graphIRI));
            }
        }
        if (printedSlide.getPrinter() != null && printedSlide.getPrinter().getUri() != null) {
            statements.add(f.createStatement(iri, printedBy, f.createIRI(printedSlide.getPrinter().getUri()), graphIRI));
        } 
        if (printedSlide.getMetadata() != null && printedSlide.getMetadata().getUri() != null) {
            statements.add(f.createStatement(iri, hasSlideMetadata, f.createIRI(printedSlide.getMetadata().getUri()), graphIRI));
        }
        
        statements.add(f.createStatement(iri, hasCreatedDate, date, graphIRI));
        statements.add(f.createStatement(iri, hasAddedToLibrary, createdDate, graphIRI));
        statements.add(f.createStatement(iri, hasModifiedDate, date, graphIRI));
        
        sparqlDAO.addStatements(statements, graphIRI);
        
        return printedSlideURI;
        
    }

    /**
     * adds the raw data to the repository but measurements are not added here. you need to call {@link addMeasurementsToRawData} to add those.
     * It also assumes, processed data were already added into the repository
     */
    @Override
    public String addRawData(RawData rawData, String datasetId, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        String uriPre = uriPrefix;
        if (user == null) {
            uriPre = uriPrefixPublic;
            graph = DEFAULT_GRAPH;
        } else {
            // check if there is already a private graph for user
            graph = getGraphForUser(user);
        }
        String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
        ValueFactory f = sparqlDAO.getValueFactory();
        List<Statement> statements = new ArrayList<Statement>();
        String rawDataURI = generateUniqueURI(uriPre + "R", allGraphs);
        
        IRI hasimageProcessingMetadata = f.createIRI(imageProcessingMetadataPredicate);
        IRI hasMeasurement = f.createIRI(hasMeasurementPredicate);
        IRI hasFile = f.createIRI(hasFilePredicate);
        IRI measurementOf = f.createIRI(measurementOfPredicate);
        IRI hasSlide = f.createIRI(ontPrefix + "has_slide");
        IRI hasFileName = f.createIRI(hasFileNamePredicate);
        IRI hasOriginalFileName = f.createIRI(hasOriginalFileNamePredicate);
        IRI hasFolder = f.createIRI(hasFolderPredicate);
        IRI hasFileFormat = f.createIRI(hasFileFormatPredicate);
        IRI graphIRI = f.createIRI(graph);
        IRI raw = f.createIRI(rawDataURI);
        IRI hasRawData = f.createIRI(ontPrefix + "has_raw_data");
        IRI arraydataset = f.createIRI(uriPre + datasetId);
        IRI hasSize = f.createIRI(hasSizePredicate);
        IRI processedFrom = f.createIRI(processedFromPredicate);
        
        if (rawData.getMetadata() != null) {
            String imageProcessingMetadataURI = rawData.getMetadata().getUri();
            statements.add(f.createStatement(raw, hasimageProcessingMetadata, f.createIRI(imageProcessingMetadataURI), graphIRI));
        }
        
        if (rawData.getSlide() != null) {
            String slideURI = rawData.getSlide().getUri();
            if (slideURI == null && rawData.getSlide().getId() != null) 
                slideURI = uriPre + rawData.getSlide().getId();
            if (slideURI != null) {
                statements.add(f.createStatement(raw, hasSlide, f.createIRI(slideURI), graphIRI));
            }
            // measurements are to be added asynchronously later
            /*if (rawData.getDataMap() != null && !rawData.getDataMap().isEmpty()) {
                for (Measurement measurement: rawData.getDataMap().keySet()) {
                    String measurementURI = addMeasurement (measurement, statements, graph);
                    Spot spot = rawData.getDataMap().get(measurement);
                    if (spot.getUri() != null)
                        statements.add(f.createStatement(f.createIRI(measurementURI), measurementOf, f.createIRI(spot.getUri()), graphIRI));
                    else {
                        // find the spot
                        if (rawData.getSlide().getPrintedSlide() == null || rawData.getSlide().getPrintedSlide().getLayout() == null)
                            throw new SparqlException ("The slide layout should be provided");
                        
                        String slideLayoutId = rawData.getSlide().getPrintedSlide().getLayout().getId();
                        if (slideLayoutId == null) {
                            slideLayoutId = rawData.getSlide().getPrintedSlide().getLayout().getUri().substring(
                                    rawData.getSlide().getPrintedSlide().getLayout().getUri().lastIndexOf("/")+1);
                        }
                        
                        Spot existing;
                        if (rawData.getSlide().getPrintedSlide().getLayout().getIsPublic())
                            existing = layoutRepository.getSpotByPosition(slideLayoutId, 
                                spot.getBlockLayoutId(), spot.getRow(), spot.getColumn(), null);
                        else 
                            existing = layoutRepository.getSpotByPosition(slideLayoutId, 
                                    spot.getBlockLayoutId(), spot.getRow(), spot.getColumn(), user);
                        if (existing != null)
                            statements.add(f.createStatement(f.createIRI(measurementURI), measurementOf, f.createIRI(existing.getUri()), graphIRI));
                        else {
                            throw new SparqlException ("The spot cannot be located in the repository");
                        }
                    }
                    statements.add(f.createStatement(raw, hasMeasurement, f.createIRI(measurementURI), graphIRI));
                }
            }*/
        }
        
        if (rawData.getFile() != null) {
            String fileURI = generateUniqueURI(uriPre + "FILE", allGraphs);
            Literal fileName = f.createLiteral(rawData.getFile().getIdentifier());
            Literal fileFolder = rawData.getFile().getFileFolder() == null ? null : f.createLiteral(rawData.getFile().getFileFolder());
            Literal fileFormat = rawData.getFile().getFileFormat() == null ? null : f.createLiteral(rawData.getFile().getFileFormat());
            Literal originalName = rawData.getFile().getOriginalName() == null ? null : f.createLiteral(rawData.getFile().getOriginalName());
            Literal size = rawData.getFile().getFileSize() == null ? null : f.createLiteral(rawData.getFile().getFileSize());
            IRI fileIRI = f.createIRI(fileURI);
            statements.add(f.createStatement(raw, hasFile, fileIRI, graphIRI));
            statements.add(f.createStatement(fileIRI, hasFileName, fileName, graphIRI));
            if (fileFolder != null) statements.add(f.createStatement(fileIRI, hasFolder, fileFolder, graphIRI));
            if (fileFormat != null) statements.add(f.createStatement(fileIRI, hasFileFormat, fileFormat, graphIRI));
            if (originalName != null) statements.add(f.createStatement(fileIRI, hasOriginalFileName, originalName, graphIRI));
            if (size != null) statements.add(f.createStatement(fileIRI, hasSize, size, graphIRI));
        }
        
        if (rawData.getProcessedDataList() != null) {
            for (ProcessedData processedData: rawData.getProcessedDataList()) {
                if (processedData.getUri() != null) {
                    IRI processed = f.createIRI(processedData.getUri());
                    statements.add(f.createStatement(processed, processedFrom, raw, graphIRI));
                }
            }
        }
        
        // TODO should we create this predicate ???
        statements.add(f.createStatement(arraydataset, hasRawData, raw, graphIRI));
        sparqlDAO.addStatements(statements, graphIRI);
        return rawDataURI;
    }


    private String addMeasurement(Measurement measurement, List<Statement> statements, String graph) throws SparqlException, SQLException {
        ValueFactory f = sparqlDAO.getValueFactory();
        String uriPre = uriPrefix;
        if (graph.equals (DEFAULT_GRAPH)) {
            uriPre = uriPrefixPublic;
        }
        String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
        String measurementURI = generateUniqueURI(uriPre + "M", allGraphs);
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
        
        Literal stdev = measurement.getStdev() == null ? null : f.createLiteral(measurement.getStdev());
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
    
    @Async("GlygenArrayAsyncExecutor")
    @Override
    public CompletableFuture<String> addMeasurementsToRawData (RawData rawData, UserEntity user) throws SparqlException, SQLException {
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
        String uri = rawData.getUri();
        if (uri == null && rawData.getId() != null) {
            uri = uriPre + rawData.getId();
        }
        if (uri != null) {
            IRI hasMeasurement = f.createIRI(hasMeasurementPredicate);
            IRI measurementOf = f.createIRI(measurementOfPredicate);
            IRI graphIRI = f.createIRI(graph);
            IRI raw = f.createIRI(uri);
            if (rawData.getDataMap() != null && !rawData.getDataMap().isEmpty()) {
                for (Measurement measurement: rawData.getDataMap().keySet()) {
                    String measurementURI = addMeasurement (measurement, statements, graph);
                    Spot spot = rawData.getDataMap().get(measurement);
                    if (spot.getUri() != null)
                        statements.add(f.createStatement(f.createIRI(measurementURI), measurementOf, f.createIRI(spot.getUri()), graphIRI));
                    else {
                        // find the spot
                        if (rawData.getSlide().getPrintedSlide() == null || rawData.getSlide().getPrintedSlide().getLayout() == null)
                            throw new SparqlException ("The slide layout should be provided");
                        
                        String slideLayoutId = rawData.getSlide().getPrintedSlide().getLayout().getId();
                        if (slideLayoutId == null) {
                            slideLayoutId = rawData.getSlide().getPrintedSlide().getLayout().getUri().substring(
                                    rawData.getSlide().getPrintedSlide().getLayout().getUri().lastIndexOf("/")+1);
                        }
                        
                        Spot existing;
                        if (rawData.getSlide().getPrintedSlide().getLayout().getIsPublic())
                            existing = layoutRepository.getSpotByPosition(slideLayoutId, 
                                spot.getBlockLayoutUri(), spot.getRow(), spot.getColumn(), null);
                        else 
                            existing = layoutRepository.getSpotByPosition(slideLayoutId, 
                                    spot.getBlockLayoutUri(), spot.getRow(), spot.getColumn(), user);
                        if (existing != null)
                            statements.add(f.createStatement(f.createIRI(measurementURI), measurementOf, f.createIRI(existing.getUri()), graphIRI));
                        else {
                            throw new SparqlException ("The spot cannot be located in the repository");
                        }
                    }
                    statements.add(f.createStatement(raw, hasMeasurement, f.createIRI(measurementURI), graphIRI));
                }
            }
            sparqlDAO.addStatements(statements, graphIRI);
        }
        
        return CompletableFuture.completedFuture(uri);
    }
    
    @Override
    public String addIntensitiesToProcessedData (ProcessedData processedData, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        } else {
            // check if there is already a private graph for user
            graph = getGraphForUser(user);
        }
        ValueFactory f = sparqlDAO.getValueFactory();
        List<Statement> statements = new ArrayList<Statement>();
        String uri = processedData.getUri();
        if (uri == null && processedData.getId() != null) {
            uri = uriPre + processedData.getId();
        }
        if (uri != null) {
            IRI processed = f.createIRI(uri);
            IRI graphIRI = f.createIRI(graph);
            // add intensities
            IRI hasRFU = f.createIRI(rfuPredicate);
            IRI hasStdev = f.createIRI(stdevPredicate);
            IRI hasCV = f.createIRI(cvPredicate);
            IRI hasIntensity = f.createIRI(hasIntensityPredicate);
            IRI bindingValueOf = f.createIRI(bindingValuePredicate);
            IRI integrates = f.createIRI(integratesPredicate); 
            IRI hasFileName = f.createIRI(hasFileNamePredicate);
            IRI hasOriginalFileName = f.createIRI(hasOriginalFileNamePredicate);
            IRI hasFolder = f.createIRI(hasFolderPredicate);
            IRI hasFileFormat = f.createIRI(hasFileFormatPredicate);
            IRI hasFile = f.createIRI(hasFilePredicate);
            IRI hasSize = f.createIRI(hasSizePredicate);
            
            // Delete the existing intensities, if any
            RepositoryResult<Statement> results = sparqlDAO.getStatements(processed, hasIntensity, null, graphIRI);
            while (results.hasNext()) {
                Statement st = results.next();
                String intensityURI = st.getObject().stringValue();
                // delete the intensity
                RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(f.createIRI(intensityURI), null, null, graphIRI);
                sparqlDAO.removeStatements(Iterations.asList(statements2), graphIRI);
            }
            String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
            if (processedData.getIntensity() != null) {
                for (Intensity intensity: processedData.getIntensity()) {
                    if (intensity == null) continue;
                    String intensityURI = generateUniqueURI(uriPre + "I", allGraphs);
                    IRI intensityIRI = f.createIRI(intensityURI);
                    Literal rfu = f.createLiteral(intensity.getRfu());
                    Literal stdev = intensity.getStDev() == null ? null : f.createLiteral(intensity.getStDev());
                    Literal cv = intensity.getPercentCV() == null ? null : f.createLiteral(intensity.getPercentCV());
                    statements.add(f.createStatement(intensityIRI, hasRFU, rfu, graphIRI));
                    if (stdev != null) statements.add(f.createStatement(intensityIRI, hasStdev, stdev, graphIRI));
                    if (cv != null) statements.add(f.createStatement(intensityIRI, hasCV, cv, graphIRI));
                    if (intensity.getSpots() != null) {
                        for (Spot spot: intensity.getSpots()) {
                            if (spot.getUri() != null) {
                                IRI spotIRI = f.createIRI(spot.getUri());
                                statements.add(f.createStatement(intensityIRI, bindingValueOf, spotIRI, graphIRI));
                            } else {
                                // need to locate the spot
                                List<Spot> existing = layoutRepository.getSpotByFeatures(spot.getFeatures(), null, spot.getBlockLayoutUri(), user);
                                if (existing != null && !existing.isEmpty()) {
                                    for (Spot s: existing) {
                                        IRI spotIRI = f.createIRI(s.getUri());
                                        statements.add(f.createStatement(intensityIRI, bindingValueOf, spotIRI, graphIRI));
                                    }
                                } else {
                                    logger.warn("spot for the intensity cannot be located");
                                }
                            }
                        }
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
            }
            
            if (processedData.getFile() != null) {
                String fileURI = null;
                RepositoryResult<Statement> results2 = sparqlDAO.getStatements(processed, hasFile, null, graphIRI);
                if (results2.hasNext()) {
                    Statement st = results2.next();
                    fileURI = st.getObject().stringValue();
                    
                } else {
                    fileURI = generateUniqueURI(uriPre + "FILE", allGraphs);
                }
                
                Literal fileName = f.createLiteral(processedData.getFile().getIdentifier());
                Literal fileFolder = processedData.getFile().getFileFolder() == null ? null : f.createLiteral(processedData.getFile().getFileFolder());
                Literal fileFormat = processedData.getFile().getFileFormat() == null ? null : f.createLiteral(processedData.getFile().getFileFormat());
                Literal originalName = processedData.getFile().getOriginalName() == null ? null : f.createLiteral(processedData.getFile().getOriginalName());
                Literal size = processedData.getFile().getFileSize() == null ? null : f.createLiteral(processedData.getFile().getFileSize());
                IRI fileIRI = f.createIRI(fileURI);
                sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(processed, hasFile, null, graphIRI)), graphIRI);
                sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(fileIRI, null, null, graphIRI)), graphIRI);
                statements.add(f.createStatement(processed, hasFile, fileIRI, graphIRI));
                statements.add(f.createStatement(fileIRI, hasFileName, fileName, graphIRI));
                if (fileFolder != null) statements.add(f.createStatement(fileIRI, hasFolder, fileFolder, graphIRI));
                if (fileFormat != null) statements.add(f.createStatement(fileIRI, hasFileFormat, fileFormat, graphIRI));
                if (originalName != null) statements.add(f.createStatement(fileIRI, hasOriginalFileName, originalName, graphIRI));
                if (size != null) statements.add(f.createStatement(fileIRI, hasSize, size, graphIRI));
            }
            
            sparqlDAO.addStatements(statements, graphIRI);
        }
        
        return uri;
    }

    @Override
    public String addProcessedData(ProcessedData processedData, String datasetId, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        } else {
            // check if there is already a private graph for user
            graph = getGraphForUser(user);
        }
        String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
        ValueFactory f = sparqlDAO.getValueFactory();
        List<Statement> statements = new ArrayList<Statement>();
        // add to user's local repository
        String processedURI = generateUniqueURI(uriPre + "P", allGraphs);
        IRI processed = f.createIRI(processedURI);
        IRI graphIRI = f.createIRI(graph);
        // add intensities
        IRI hasRFU = f.createIRI(rfuPredicate);
        IRI hasStdev = f.createIRI(stdevPredicate);
        IRI hasCV = f.createIRI(cvPredicate);
        IRI hasIntensity = f.createIRI(hasIntensityPredicate);
        IRI bindingValueOf = f.createIRI(bindingValuePredicate);
        IRI integrates = f.createIRI(integratesPredicate);
        IRI integratedBy = f.createIRI(integratedByPredicate);   
        IRI hasProcessingSWMetadata = f.createIRI(processingSoftwareMetadataPredicate);
        IRI hasProcessedData = f.createIRI(ontPrefix + "has_processed_data");
        IRI hasFileName = f.createIRI(hasFileNamePredicate);
        IRI hasOriginalFileName = f.createIRI(hasOriginalFileNamePredicate);
        IRI hasFolder = f.createIRI(hasFolderPredicate);
        IRI hasFileFormat = f.createIRI(hasFileFormatPredicate);
        IRI hasFile = f.createIRI(hasFilePredicate);
        IRI hasSize = f.createIRI(hasSizePredicate);
        
        if (processedData.getIntensity() != null) {
            for (Intensity intensity: processedData.getIntensity()) {
                if (intensity == null) continue;
                String intensityURI = generateUniqueURI(uriPre + "I", allGraphs);
                IRI intensityIRI = f.createIRI(intensityURI);
                Literal rfu = f.createLiteral(intensity.getRfu());
                Literal stdev = intensity.getStDev() == null ? null : f.createLiteral(intensity.getStDev());
                Literal cv = intensity.getPercentCV() == null ? null : f.createLiteral(intensity.getPercentCV());
                statements.add(f.createStatement(intensityIRI, hasRFU, rfu, graphIRI));
                if (stdev != null) statements.add(f.createStatement(intensityIRI, hasStdev, stdev, graphIRI));
                if (cv != null) statements.add(f.createStatement(intensityIRI, hasCV, cv, graphIRI));
                if (intensity.getSpots() != null) {
                    for (Spot spot: intensity.getSpots()) {
                        if (spot.getUri() != null) {
                            IRI spotIRI = f.createIRI(spot.getUri());
                            statements.add(f.createStatement(intensityIRI, bindingValueOf, spotIRI, graphIRI));
                        } else {
                            // need to locate the spot
                            List<Spot> existing = layoutRepository.getSpotByFeatures(spot.getFeatures(), null, spot.getBlockLayoutUri(), user);
                            if (existing != null && !existing.isEmpty()) {
                                for (Spot s: existing) {
                                    IRI spotIRI = f.createIRI(s.getUri());
                                    statements.add(f.createStatement(intensityIRI, bindingValueOf, spotIRI, graphIRI));
                                }
                            } else {
                                logger.warn("spot for the intensity cannot be located");
                            }
                        }
                    }
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
        }
        
        if (processedData.getMetadata() != null && processedData.getMetadata().getUri() != null) {
            statements.add(f.createStatement(processed, hasProcessingSWMetadata, f.createIRI(processedData.getMetadata().getUri()), graphIRI));
        }
        
        IRI method = processedData.getMethod() == null ? null: f.createIRI(processedData.getMethod().getUri());
        if (method != null) {
            statements.add(f.createStatement(processed, integratedBy, method, graphIRI));
        }
        
        if (processedData.getFile() != null) {
            String fileURI = generateUniqueURI(uriPre + "FILE", allGraphs);
            Literal fileName = f.createLiteral(processedData.getFile().getIdentifier());
            Literal fileFolder = processedData.getFile().getFileFolder() == null ? null : f.createLiteral(processedData.getFile().getFileFolder());
            Literal fileFormat = processedData.getFile().getFileFormat() == null ? null : f.createLiteral(processedData.getFile().getFileFormat());
            Literal originalName = processedData.getFile().getOriginalName() == null ? null : f.createLiteral(processedData.getFile().getOriginalName());
            Literal size = processedData.getFile().getFileSize() == null ? null : f.createLiteral(processedData.getFile().getFileSize());
            IRI fileIRI = f.createIRI(fileURI);
            statements.add(f.createStatement(processed, hasFile, fileIRI, graphIRI));
            statements.add(f.createStatement(fileIRI, hasFileName, fileName, graphIRI));
            if (fileFolder != null) statements.add(f.createStatement(fileIRI, hasFolder, fileFolder, graphIRI));
            if (fileFormat != null) statements.add(f.createStatement(fileIRI, hasFileFormat, fileFormat, graphIRI));
            if (originalName != null) statements.add(f.createStatement(fileIRI, hasOriginalFileName, originalName, graphIRI));
            if (size != null) statements.add(f.createStatement(fileIRI, hasSize, size, graphIRI));
            
        }
        
        // don't link the to dataset, it should be linked to the raw data
        //IRI arraydataset = f.createIRI(uriPre + datasetId);
        //statements.add(f.createStatement(arraydataset, hasProcessedData, processed, graphIRI));
        sparqlDAO.addStatements(statements, graphIRI);
        return processedURI;
    }
 
    @Override
    public ArrayDataset getArrayDataset(String datasetId, UserEntity user) throws SparqlException, SQLException {
        return getArrayDataset(datasetId, true, user);
    }
    
    @Override
    public ArrayDataset getArrayDataset(String datasetId, Boolean loadAll, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        }
        else
            graph = getGraphForUser(user);
        List<SparqlEntity> results = queryHelper.retrieveById(uriPre + datasetId, graph);
        if (results.isEmpty()) {     
           return null;
        }
        else {
            return getDatasetFromURI(uriPre + datasetId, loadAll, user);
        }
    }
        
    private ArrayDataset getDatasetFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException {
        
        ArrayDataset datasetObject = null;
        
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
        IRI hasImage = f.createIRI(hasImagePredicate);
        IRI hasPub = f.createIRI(hasPublication);
        IRI hasGrantPred = f.createIRI(hasGrant);
        IRI hasCollab = f.createIRI(hasCollaborator);
        
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
            } 
            if (uri.contains("public")) {
                datasetObject.setIsPublic(true);
            }
            datasetObject.setRawDataList(new ArrayList<RawData>());
            datasetObject.setProcessedData(new ArrayList<ProcessedData>());
            datasetObject.setSlides(new ArrayList<Slide>());
            datasetObject.setImages(new ArrayList<Image>());
            datasetObject.setPublications(new ArrayList<Publication>());
            datasetObject.setGrants(new ArrayList<>());
            datasetObject.setCollaborators(new ArrayList<>());
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
                datasetObject.setSample((Sample) metadataRepository.getMetadataCategoryFromURI(uriValue.stringValue(), sampleTypePredicate, loadAll, user));            
            } else if (st.getPredicate().equals(hasRawData)) {
                Value uriValue = st.getObject();
                RawData rawData = getRawDataFromURI(uriValue.stringValue(), loadAll, user);
                datasetObject.getRawDataList().add(rawData);
            } else if (st.getPredicate().equals(hasSlide)) {
                Value uriValue = st.getObject();
                datasetObject.getSlides().add(getSlideFromURI(uriValue.stringValue(), loadAll, user));            
            } else if (st.getPredicate().equals(hasPub)) {
                Value uriValue = st.getObject();
                datasetObject.getPublications().add(getPublicationFromURI(uriValue.stringValue(), user));            
            } else if (st.getPredicate().equals(hasCollab)) {
                Value label = st.getObject();
                Creator collab = new Creator();
                collab.setName(label.stringValue());
                UserEntity entity = userRepository.findByUsernameIgnoreCase(collab.getName());
                if (entity != null) {
                    collab.setFirstName(entity.getFirstName());
                    collab.setLastName(entity.getLastName());
                    collab.setAffiliation(entity.getAffiliation());
                }
                datasetObject.getCollaborators().add(collab);       
            } else if (st.getPredicate().equals(hasGrantPred)) {
                Value uriValue = st.getObject();
                datasetObject.getGrants().add(getGrantFromURI(uriValue.stringValue(), user));            
            } else if (st.getPredicate().equals(hasImage)) {
                Value uriValue = st.getObject();
                datasetObject.getImages().add(getImageFromURI(uriValue.stringValue(), loadAll, user));      
            } else if (st.getPredicate().equals(hasProcessedData)) {
                Value uriValue = st.getObject();
                datasetObject.getProcessedData().add(getProcessedDataFromURI(uriValue.stringValue(), loadAll, user));            
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
                    if (stPublic.getPredicate().equals(RDFS.LABEL)) {
                        Value label = stPublic.getObject();
                        datasetObject.setName(label.stringValue());
                    } else if (stPublic.getPredicate().equals(RDFS.COMMENT)) {
                        Value comment = stPublic.getObject();
                        datasetObject.setDescription(comment.stringValue());
                    } else if (stPublic.getPredicate().equals(hasSample)) {
                        uriValue = stPublic.getObject();
                        datasetObject.setSample((Sample) metadataRepository.getMetadataCategoryFromURI(uriValue.stringValue(), sampleTypePredicate, loadAll, user));            
                    } else if (stPublic.getPredicate().equals(hasRawData)) {
                        uriValue = stPublic.getObject();
                        RawData rawData = getRawDataFromURI(uriValue.stringValue(), loadAll, user);
                        datasetObject.getRawDataList().add(rawData);
                    } else if (stPublic.getPredicate().equals(hasSlide)) {
                        uriValue = stPublic.getObject();
                        datasetObject.getSlides().add(getSlideFromURI(uriValue.stringValue(), loadAll, user));        
                    } else if (stPublic.getPredicate().equals(hasPub)) {
                        uriValue = stPublic.getObject();
                        datasetObject.getPublications().add(getPublicationFromURI(uriValue.stringValue(), user));            
                    } else if (stPublic.getPredicate().equals(hasCollab)) {
                        Value label = stPublic.getObject();
                        Creator collab = new Creator();
                        collab.setName(label.stringValue());
                        UserEntity entity = userRepository.findByUsernameIgnoreCase(collab.getName());
                        if (entity != null) {
                            collab.setFirstName(entity.getFirstName());
                            collab.setLastName(entity.getLastName());
                            collab.setAffiliation(entity.getAffiliation());
                        }
                        datasetObject.getCollaborators().add(collab);       
                    } else if (stPublic.getPredicate().equals(hasGrantPred)) {
                        uriValue = stPublic.getObject();
                        datasetObject.getGrants().add(getGrantFromURI(uriValue.stringValue(), user));            
                    } else if (stPublic.getPredicate().equals(hasImage)) {
                        uriValue = stPublic.getObject();
                        datasetObject.getImages().add(getImageFromURI(uriValue.stringValue(), loadAll, user));      
                    } else if (stPublic.getPredicate().equals(hasProcessedData)) {
                        uriValue = stPublic.getObject();
                        datasetObject.getProcessedData().add(getProcessedDataFromURI(uriValue.stringValue(), loadAll, user));            
                    }
                }
            }
        }
        
        getStatusFromURI (datasetObject.getUri(), datasetObject, graph);
        return datasetObject;
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
        return getArrayDatasetByUser(user, offset, limit, field, order, searchValue, true);
    }

    @Override
    public List<ArrayDataset> getArrayDatasetByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue, boolean loadAll) throws SparqlException, SQLException {
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
                ArrayDataset dataset = getDatasetFromURI(uri, loadAll, user);
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

    

    
    
    /**
     * raw data must have been added already 
     *    
     * @param image
     * @param datasetId
     * @param statements
     * @param graph
     * @return
     * @throws SparqlException
     */
    private String addImage(Image image, String datasetId, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        } else {
            // check if there is already a private graph for user
            graph = getGraphForUser(user);
        }
        String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
        ValueFactory f = sparqlDAO.getValueFactory();
        String imageURI = generateUniqueURI(uriPre + "I", allGraphs);
        String scannerMetadataURI = null;
        if (image.getScanner() != null) {
            scannerMetadataURI = image.getScanner().getUri();
        }
        
        IRI graphIRI = f.createIRI(graph);
        IRI imageIRI = f.createIRI(imageURI);
        IRI metadataIRI = scannerMetadataURI == null ? null : f.createIRI(scannerMetadataURI);
        IRI hasFile = f.createIRI(hasFilePredicate);
        IRI hasScanner = f.createIRI(scannerMetadataPredicate);
        IRI hasFileName = f.createIRI(hasFileNamePredicate);
        IRI hasOriginalFileName = f.createIRI(hasOriginalFileNamePredicate);
        IRI hasFolder = f.createIRI(hasFolderPredicate);
        IRI hasFileFormat = f.createIRI(hasFileFormatPredicate);
        IRI hasSize = f.createIRI(hasSizePredicate);
        IRI derivedFrom = f.createIRI(derivedFromPredicate);
        
        List<Statement> statements = new ArrayList<Statement>();
        
        if (image.getFile() != null) {
            String fileURI = generateUniqueURI(uriPre + "FILE", allGraphs);
            Literal fileName = f.createLiteral(image.getFile().getIdentifier());
            Literal fileFolder = image.getFile().getFileFolder() == null ? null : f.createLiteral(image.getFile().getFileFolder());
            Literal fileFormat = image.getFile().getFileFormat() == null ? null : f.createLiteral(image.getFile().getFileFormat());
            Literal originalName = image.getFile().getOriginalName() == null ? null : f.createLiteral(image.getFile().getOriginalName());
            Literal size = image.getFile().getFileSize() == null ? null : f.createLiteral(image.getFile().getFileSize());
            IRI fileIRI = f.createIRI(fileURI);
            statements.add(f.createStatement(imageIRI, hasFile, fileIRI, graphIRI));
            statements.add(f.createStatement(fileIRI, hasFileName, fileName, graphIRI));
            if (fileFolder != null) statements.add(f.createStatement(fileIRI, hasFolder, fileFolder, graphIRI));
            if (fileFormat != null) statements.add(f.createStatement(fileIRI, hasFileFormat, fileFormat, graphIRI));
            if (originalName != null) statements.add(f.createStatement(fileIRI, hasOriginalFileName, originalName, graphIRI));
            if (size != null) statements.add(f.createStatement(fileIRI, hasSize, size, graphIRI));
        }
        
        IRI arraydataset = f.createIRI(uriPre + datasetId);
        IRI hasImage = f.createIRI(hasImagePredicate);
        // add the image to its dataset as well
        statements.add(f.createStatement(arraydataset, hasImage, imageIRI, graphIRI));
        if (image.getRawData() != null) {    
            String rawDataURI = image.getRawData().getUri();
            if (rawDataURI == null) {
                throw new SparqlException ("Raw data should have been added already");
            }
            IRI raw = f.createIRI(rawDataURI);
            statements.add(f.createStatement(raw, derivedFrom, imageIRI, graphIRI));
        }
        
        if (metadataIRI != null) statements.add(f.createStatement(imageIRI, hasScanner, metadataIRI, graphIRI));
    
        sparqlDAO.addStatements(statements, graphIRI);
        return imageURI;
    }
    
    @Override
    public ProcessedData getProcessedDataFromURI(String uriValue, Boolean loadAll, UserEntity user) throws SQLException, SparqlException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            if (uriValue.contains("public"))
                graph = DEFAULT_GRAPH;
            else
                graph = getGraphForUser(user);
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
        IRI integratedBy = f.createIRI(integratedByPredicate);   
        //TODO what about concentration level
        IRI hasProcessingSWMetadata = f.createIRI(processingSoftwareMetadataPredicate);
        IRI hasFile = f.createIRI(hasFilePredicate);
        IRI hasFileName = f.createIRI(hasFileNamePredicate);
        IRI hasOriginalFileName = f.createIRI(hasOriginalFileNamePredicate);
        IRI hasFolder = f.createIRI(hasFolderPredicate);
        IRI hasFileFormat = f.createIRI(hasFileFormatPredicate);
        IRI hasSize = f.createIRI(hasSizePredicate);
        
        ProcessedData processedObject = new ProcessedData();
        processedObject.setUri(uriValue);
        processedObject.setId(uriValue.substring(uriValue.lastIndexOf("/")+ 1));
        List<Intensity> intensities = new ArrayList<Intensity>();
        processedObject.setIntensity(intensities);
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(processedData, null, null, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next();
            if (st.getPredicate().equals(hasIntensity)) {
                if (loadAll != null && !loadAll) 
                    continue;    // skip loading intensities
                String intensityURI = st.getObject().stringValue();
                Intensity intensity = new Intensity();
                intensity.setUri(intensityURI);
                intensity.setId(intensityURI.substring(intensityURI.lastIndexOf("/")+1));
                intensity.setSpots (new ArrayList<Spot>());
                RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(f.createIRI(intensityURI), null, null, graphIRI);
                if (statements2.hasNext()) {
                    intensities.add(intensity);
                }
                while (statements2.hasNext()) {
                    Statement st2 = statements2.next();
                    if (st2.getPredicate().equals(bindingValueOf)) {
                        String spotURI = st2.getObject().stringValue();
                        intensity.getSpots().add(layoutRepository.getSpotFromURI(spotURI, user));
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
                DataProcessingSoftware metadata = metadataRepository.getDataProcessingSoftwareFromURI(metadataURI, loadAll, user);
                processedObject.setMetadata(metadata);
            } else if (st.getPredicate().equals(integratedBy)) {
                String methodURI = st.getObject().stringValue();
                StatisticalMethod method = new StatisticalMethod();
                method.setUri(methodURI);
                RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(f.createIRI(methodURI), null, null, graphIRI);
                while (statements2.hasNext()) {
                    Statement st2 = statements2.next();
                    if (st2.getPredicate().equals(RDFS.LABEL)) {
                        method.setName(st2.getObject().stringValue());
                        break;
                    }
                }
                processedObject.setMethod(method);
            } else if (st.getPredicate().equals(hasFile)) {
                Value value = st.getObject();
                if (!value.stringValue().startsWith("http"))
                    continue;
                // retrieve file details
                FileWrapper file = new FileWrapper();
                RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(f.createIRI(value.stringValue()), null, null, graphIRI);
                while (statements2.hasNext()) {
                    Statement st2 = statements2.next();
                    if (st2.getPredicate().equals(hasFileName)) {
                        Value val = st2.getObject();
                        file.setIdentifier(val.stringValue());
                    } else if (st2.getPredicate().equals(hasFileFormat)) {
                        Value val = st2.getObject();
                        file.setFileFormat(val.stringValue());
                    } else if (st2.getPredicate().equals(hasFolder)) {
                        Value val = st2.getObject();
                        file.setFileFolder(val.stringValue());
                    } else if (st2.getPredicate().equals(hasOriginalFileName)) {
                        Value val = st2.getObject();
                        file.setOriginalName(val.stringValue());
                    }  else if (st2.getPredicate().equals(hasSize)) {
                        Value val = st2.getObject();
                        try {
                            file.setFileSize(Long.parseLong(val.stringValue()));
                        } catch (NumberFormatException e) {
                            logger.warn ("file size is not valid");
                        }
                    }
                }
                processedObject.setFile(file);    
            }
        }
        
        getStatusFromURI (processedObject.getUri(), processedObject, graph);
        return processedObject;
    }


    @Override
    public Slide getSlideFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (uri.contains("public"))
            graph = DEFAULT_GRAPH;
        else {
            if (user != null)
                graph = getGraphForUser(user);
            else 
                graph = DEFAULT_GRAPH;
        }
        
        Slide slideObject = null;
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI slideIRI = f.createIRI(uri);
        IRI graphIRI = f.createIRI(graph);
        
        IRI scanOf = f.createIRI(scanOfPredicate);
        IRI hasPrintedSlide = f.createIRI(hasPrintedSlidePredicate);
        IRI hasAssay = f.createIRI(assayMetadataPredicate);
        
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(slideIRI, null, null, graphIRI);
        if (statements.hasNext()) {
            slideObject = new Slide();
            slideObject.setUri(uri);
            slideObject.setId(uri.substring(uri.lastIndexOf("/")+1));
            slideObject.setImages(new ArrayList<Image>());
        }
        
        while (statements.hasNext()) {
            Statement st = statements.next();
            if (st.getPredicate().equals(hasPrintedSlide)) {
                Value uriValue = st.getObject();
                slideObject.setPrintedSlide(getPrintedSlideFromURI(uriValue.stringValue(), loadAll, user));   
            } else if (st.getPredicate().equals(hasAssay)) {
                Value uriValue = st.getObject();
                slideObject.setMetadata(metadataRepository.getAssayMetadataFromURI(uriValue.stringValue(), loadAll, user));
            }
        }
        
        if (slideObject != null) {
            statements = sparqlDAO.getStatements(null, scanOf, slideIRI, graphIRI);
            while (statements.hasNext()) {
                Statement st = statements.next();
                Value uriValue = st.getSubject();
                slideObject.getImages().add(getImageFromURI(uriValue.stringValue(), loadAll, user));
            }
        }
        
        return slideObject;
    }


    private Image getImageFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException  {
        String graph = null;
        if (uri.contains("public"))
            graph = DEFAULT_GRAPH;
        else {
            if (user != null)
                graph = getGraphForUser(user);
            else 
                graph = DEFAULT_GRAPH;
        }
        
        Image imageObject = null;
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI imageIRI = f.createIRI(uri);
        IRI graphIRI = f.createIRI(graph);
        IRI hasScanner = f.createIRI(scannerMetadataPredicate);
        IRI derivedFrom = f.createIRI(derivedFromPredicate);
        IRI hasFile = f.createIRI(hasFilePredicate);
        IRI hasFileName = f.createIRI(hasFileNamePredicate);
        IRI hasOriginalFileName = f.createIRI(hasOriginalFileNamePredicate);
        IRI hasFolder = f.createIRI(hasFolderPredicate);
        IRI hasFileFormat = f.createIRI(hasFileFormatPredicate);
        IRI hasSize = f.createIRI(hasSizePredicate);
        
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(imageIRI, null, null, graphIRI);
        if (statements.hasNext()) {
            imageObject = new Image();
            imageObject.setUri(uri);
            imageObject.setId(uri.substring(uri.lastIndexOf("/")+1));
        }
        
        while (statements.hasNext()) {
            Statement st = statements.next();
            if (st.getPredicate().equals(hasFile)) {
                Value value = st.getObject();
                if (!value.stringValue().startsWith("http")) {
                    // ignore the file (not a valid file predicate)
                    continue;
                }
                // retrieve file details
                FileWrapper file = new FileWrapper();
                RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(f.createIRI(value.stringValue()), null, null, graphIRI);
                while (statements2.hasNext()) {
                    Statement st2 = statements2.next();
                    if (st2.getPredicate().equals(hasFileName)) {
                        Value val = st2.getObject();
                        file.setIdentifier(val.stringValue());
                    } else if (st2.getPredicate().equals(hasFileFormat)) {
                        Value val = st2.getObject();
                        file.setFileFormat(val.stringValue());
                    } else if (st2.getPredicate().equals(hasFolder)) {
                        Value val = st2.getObject();
                        file.setFileFolder(val.stringValue());
                    } else if (st2.getPredicate().equals(hasOriginalFileName)) {
                        Value val = st2.getObject();
                        file.setOriginalName(val.stringValue());
                    } else if (st2.getPredicate().equals(hasSize)) {
                        Value val = st2.getObject();
                        try {
                            file.setFileSize(Long.parseLong(val.stringValue()));
                        } catch (NumberFormatException e) {
                            logger.warn ("file size is not valid");
                        }
                    }
                }
                imageObject.setFile(file);    
            } else if (st.getPredicate().equals(hasScanner)) {
                Value uriValue = st.getObject();
                imageObject.setScanner(metadataRepository.getScannerMetadataFromURI(uriValue.stringValue(), loadAll, user));   
            } 
        }
        
        // retrieve the rawData
        statements = sparqlDAO.getStatements(null, derivedFrom, imageIRI, graphIRI);
        if (statements.hasNext()) {
            Statement st = statements.next();
            String rawDataURI = st.getSubject().stringValue();
            imageObject.setRawData(getRawDataFromURI(rawDataURI, loadAll, user));
        }
            
        return imageObject;
    }

    @Override
    public RawData getRawDataFromURI(String uri,  Boolean loadAll, UserEntity user) throws SparqlException, SQLException  {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            if (uri.contains("public"))
                graph = DEFAULT_GRAPH;
            else
                graph = getGraphForUser(user);
        }
        
        RawData rawDataObject = null;
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI hasimageProcessingMetadata = f.createIRI(imageProcessingMetadataPredicate);
        
        IRI hasMeasurement = f.createIRI(hasMeasurementPredicate);
        IRI hasFile = f.createIRI(hasFilePredicate);
        IRI measurementOf = f.createIRI(measurementOfPredicate);
        IRI graphIRI = f.createIRI(graph);
        IRI raw = f.createIRI(uri);
        IRI hasFileName = f.createIRI(hasFileNamePredicate);
        IRI hasOriginalFileName = f.createIRI(hasOriginalFileNamePredicate);
        IRI hasFolder = f.createIRI(hasFolderPredicate);
        IRI hasFileFormat = f.createIRI(hasFileFormatPredicate);
        IRI hasSize = f.createIRI(hasSizePredicate);
        IRI processedFrom = f.createIRI(processedFromPredicate);
        IRI hasSlide = f.createIRI(ontPrefix + "has_slide");
        
        Map<Measurement, Spot> dataMap = new HashMap<Measurement, Spot>();
        Map<String, String> measurementToSpotIdMap = new HashMap<String, String>();
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(raw, null, null, graphIRI);
        if (statements.hasNext()) {
            rawDataObject = new RawData();
            rawDataObject.setUri(uri);
            rawDataObject.setId(uri.substring(uri.lastIndexOf("/")+1));
            rawDataObject.setMeasurements(new ArrayList<Measurement>());
            rawDataObject.setDataMap(dataMap);
            rawDataObject.setMeasurementToSpotIdMap(measurementToSpotIdMap);
            rawDataObject.setProcessedDataList(new ArrayList<ProcessedData>());
        }
        else {
            return null;
        }
        
        while (statements.hasNext()) {
            Statement st = statements.next();
            if (st.getPredicate().equals(hasFile)) {
                Value value = st.getObject();
                if (!value.stringValue().startsWith("http"))
                    continue;
                // retrieve file details
                FileWrapper file = new FileWrapper();
                RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(f.createIRI(value.stringValue()), null, null, graphIRI);
                while (statements2.hasNext()) {
                    Statement st2 = statements2.next();
                    if (st2.getPredicate().equals(hasFileName)) {
                        Value val = st2.getObject();
                        file.setIdentifier(val.stringValue());
                    } else if (st2.getPredicate().equals(hasFileFormat)) {
                        Value val = st2.getObject();
                        file.setFileFormat(val.stringValue());
                    } else if (st2.getPredicate().equals(hasFolder)) {
                        Value val = st2.getObject();
                        file.setFileFolder(val.stringValue());
                    } else if (st2.getPredicate().equals(hasOriginalFileName)) {
                        Value val = st2.getObject();
                        file.setOriginalName(val.stringValue());
                    }  else if (st2.getPredicate().equals(hasSize)) {
                        Value val = st2.getObject();
                        try {
                            file.setFileSize(Long.parseLong(val.stringValue()));
                        } catch (NumberFormatException e) {
                            logger.warn ("file size is not valid");
                        }
                    }
                }
                rawDataObject.setFile(file);    
            } else if (st.getPredicate().equals(hasimageProcessingMetadata)) {
                Value uriValue = st.getObject();
                rawDataObject.setMetadata(metadataRepository.getImageAnalysisSoftwareFromURI(uriValue.stringValue(), loadAll, user));   
            } else if (st.getPredicate().equals(hasMeasurement)) {
                if (loadAll != null && !loadAll)
                    continue;
                Value uriValue = st.getObject();
                Measurement measurement = getMeasurementFromURI(uriValue.stringValue(), user);
                rawDataObject.getMeasurements().add(measurement);
                RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(f.createIRI(uriValue.stringValue()), null, null, graphIRI);
                while (statements2.hasNext()) {
                    Statement st2 = statements2.next();
                    if (st2.getPredicate().equals(measurementOf)) {
                        String spotURI = st2.getObject().stringValue();
                        Spot spot = layoutRepository.getSpotFromURI(spotURI, user);
                        dataMap.put(measurement, spot);
                        rawDataObject.setSpot(measurement.getId(), spotURI.substring(spotURI.lastIndexOf("/")+1));
                    }
                } 
            } 
        }
        
        // retrieve the processedData
        statements = sparqlDAO.getStatements(null, processedFrom, raw, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next();
            String processedDataURI = st.getSubject().stringValue();
            rawDataObject.getProcessedDataList().add(getProcessedDataFromURI(processedDataURI, loadAll, user));
        }
        getStatusFromURI (rawDataObject.getUri(), rawDataObject, graph);
        return rawDataObject;
    }
    
    private Measurement getMeasurementFromURI(String uri, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (uri.contains("public"))
            graph = DEFAULT_GRAPH;
        else {
            if (user != null)
                graph = getGraphForUser(user);
            else 
                graph = DEFAULT_GRAPH;
        }
        
        Measurement measurementObject = null;
        
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI measurementIRI = f.createIRI(uri);
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
        
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(measurementIRI, null, null, graphIRI);
        if (statements.hasNext()) {
            measurementObject = new Measurement();
            measurementObject.setUri(uri);
            measurementObject.setId(uri.substring(uri.lastIndexOf("/")+1));
            measurementObject.setCoordinates(new Coordinate());
        }
        
        while (statements.hasNext()) {
            Statement st = statements.next();
            if (st.getPredicate().equals(hasMean)) {
                Value value = st.getObject();
                measurementObject.setMean(Double.parseDouble(value.stringValue()));    
            } else if (st.getPredicate().equals(hasMedian)) {
                Value value = st.getObject();
                measurementObject.setMedian(Double.parseDouble(value.stringValue()));   
            } else if (st.getPredicate().equals(hasMeanMinusB)) {
                Value value = st.getObject();
                measurementObject.setMeanMinusB(Double.parseDouble(value.stringValue()));   
            } else if (st.getPredicate().equals(hasMedianMinusB)) {
                Value value = st.getObject();
                measurementObject.setMedianMinusB(Double.parseDouble(value.stringValue()));   
            } else if (st.getPredicate().equals(hasBMean)) {
                Value value = st.getObject();
                measurementObject.setbMean(Double.parseDouble(value.stringValue()));   
            } else if (st.getPredicate().equals(hasBMedian)) {
                Value value = st.getObject();
                measurementObject.setbMedian(Double.parseDouble(value.stringValue()));   
            } else if (st.getPredicate().equals(hasXCoordinate)) {
                Value value = st.getObject();
                measurementObject.getCoordinates().setxCoord(Double.parseDouble(value.stringValue()));   
            } else if (st.getPredicate().equals(hasYCoordinate)) {
                Value value = st.getObject();
                measurementObject.getCoordinates().setyCoord(Double.parseDouble(value.stringValue()));   
            } else if (st.getPredicate().equals(hasStdev)) {
                Value value = st.getObject();
                measurementObject.setStdev(Double.parseDouble(value.stringValue()));   
            } else if (st.getPredicate().equals(hasDiameter)) {
                Value value = st.getObject();
                measurementObject.getCoordinates().setDiameter(Double.parseDouble(value.stringValue()));   
            }
        }
        
        return measurementObject;
    }


    @Override
    public void deleteArrayDataset(String datasetId, UserEntity user) throws SparqlException, SQLException {
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
            String uri = uriPre + datasetId;
            ValueFactory f = sparqlDAO.getValueFactory();
            IRI dataset = f.createIRI(uri);
            IRI graphIRI = f.createIRI(graph);
            IRI hasRawData = f.createIRI(ontPrefix + "has_raw_data");
            IRI hasProcessedData = f.createIRI(ontPrefix + "has_processed_data");
            IRI hasSlide = f.createIRI(ontPrefix + "has_slide");
            IRI hasImage = f.createIRI(hasImagePredicate);
            IRI hasPub = f.createIRI(hasPublication);
            
            // delete rawData
            RepositoryResult<Statement> statements = sparqlDAO.getStatements(dataset, hasRawData, null, graphIRI);
            while (statements.hasNext()) {
                Statement st = statements.next();
                String rawDataURI = st.getObject().stringValue();
                deleteRawData(rawDataURI, uri, graph);
            }
            // delete processedData
            statements = sparqlDAO.getStatements(dataset, hasProcessedData, null, graphIRI);
            while (statements.hasNext()) {
                Statement st = statements.next();
                String processedDataURI = st.getObject().stringValue();
                deleteProcessedData(processedDataURI, graph);
            }
            // delete slides
            statements = sparqlDAO.getStatements(dataset, hasSlide, null, graphIRI);
            while (statements.hasNext()) {
                Statement st = statements.next();
                String slideURI = st.getObject().stringValue();
                // delete the slide
                RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(f.createIRI(slideURI), null, null, graphIRI);
                sparqlDAO.removeStatements(Iterations.asList(statements2), graphIRI);
            }
            // delete images
            statements = sparqlDAO.getStatements(dataset, hasImage, null, graphIRI);
            while (statements.hasNext()) {
                Statement st = statements.next();
                String imageURI = st.getObject().stringValue();
                // delete the image
                RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(f.createIRI(imageURI), null, null, graphIRI);
                sparqlDAO.removeStatements(Iterations.asList(statements2), graphIRI);
            }
            // delete publications
            statements = sparqlDAO.getStatements(dataset, hasPub, null, graphIRI);
            while (statements.hasNext()) {
                Statement st = statements.next();
                String publicationURI = st.getObject().stringValue();
                RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(f.createIRI(publicationURI), null, null, graphIRI);
                sparqlDAO.removeStatements(Iterations.asList(statements2), graphIRI);
            }
          
            statements = sparqlDAO.getStatements(dataset, null, null, graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
        }
    }
    
    private void deleteProcessedData(String processedDataURI, String graph) throws RepositoryException, SparqlException {    
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI processedData = f.createIRI(processedDataURI);
        IRI graphIRI = f.createIRI(graph);
        IRI hasIntensity = f.createIRI(hasIntensityPredicate);
        
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(processedData, hasIntensity, null, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next();
            String intensityURI = st.getObject().stringValue();
            // delete the intensity
            RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(f.createIRI(intensityURI), null, null, graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(statements2), graphIRI);
        }
        
        // delete files
        deleteFiles (processedDataURI, graph);
        
        statements = sparqlDAO.getStatements(processedData, null, null, graphIRI);
        sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);     
    }
    
    private void deleteFiles(String uri, String graph) throws SparqlException {
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI iri = f.createIRI(uri);
        IRI graphIRI = f.createIRI(graph);
        IRI hasFile = f.createIRI(hasFilePredicate);
                
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(iri, hasFile, null, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next();
            if (st.getObject().stringValue().startsWith("http")) {
                // file url
                IRI file = f.createIRI(st.getObject().stringValue());
                RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(file, hasFile, null, graphIRI);
                sparqlDAO.removeStatements(Iterations.asList(statements2), graphIRI);
            }
        }
    }


    @Override
    public void deleteRawData (String rawDataId, String datasetId, UserEntity user) throws SQLException, SparqlException {
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
            String uri = uriPre + datasetId;
            ValueFactory f = sparqlDAO.getValueFactory();
            IRI hasRawData = f.createIRI(ontPrefix + "has_raw_data");
            IRI graphIRI = f.createIRI(graph);
            IRI dataset = f.createIRI(uri);
            
            deleteRawData(uriPre + rawDataId, uri, graph);
            
            RepositoryResult<Statement>statements = sparqlDAO.getStatements(dataset, hasRawData, f.createIRI(uriPre + rawDataId), graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
        }
    }
    
    @Override
    public void deleteProcessedData (String processedDataId, String datasetId, UserEntity user) throws SQLException, SparqlException {
        
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
            String uri = uriPre + datasetId;
            ValueFactory f = sparqlDAO.getValueFactory();
            IRI hasProcessedData = f.createIRI(ontPrefix + "has_processed_data");
            IRI graphIRI = f.createIRI(graph);
            IRI dataset = f.createIRI(uri);
            String processedDataURI = uriPre + processedDataId;
            IRI processedDataIRI = f.createIRI(processedDataURI);
            
            deleteProcessedData(processedDataURI, graph);
            
            RepositoryResult<Statement>statements = sparqlDAO.getStatements(dataset, hasProcessedData, processedDataIRI, graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
        }
    }

    private void deleteRawData(String rawDataURI, String datasetURI, String graph) throws RepositoryException, SparqlException {
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI rawData = f.createIRI(rawDataURI);
        IRI graphIRI = f.createIRI(graph);
        IRI hasMeasurement = f.createIRI(hasMeasurementPredicate);
        //IRI hasSlide = f.createIRI(ontPrefix + "has_slide");
        //IRI hasImage = f.createIRI(derivedFromPredicate);
        //IRI hasImage2 = f.createIRI(hasImagePredicate);
        IRI processedFrom = f.createIRI(processedFromPredicate);
        
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(rawData, hasMeasurement, null, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next();
            String measurementURI = st.getObject().stringValue();
            // delete the measurement
            RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(f.createIRI(measurementURI), null, null, graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(statements2), graphIRI);
        }
        
        // delete processed data
        statements = sparqlDAO.getStatements(null, processedFrom, rawData, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next();
            String processedDataURI = st.getSubject().stringValue();
            deleteProcessedData(processedDataURI, graph);
        }
        
       /* // delete slides
        statements = sparqlDAO.getStatements(rawData, hasSlide, null, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next();
            String slideURI = st.getObject().stringValue();
            // delete the slide
            RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(f.createIRI(slideURI), null, null, graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(statements2), graphIRI);
            // delete it from the dataset as well
            RepositoryResult<Statement> statements3 = sparqlDAO.getStatements(f.createIRI(datasetURI), hasSlide, f.createIRI(slideURI), graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(statements3), graphIRI);
        }
        // delete images
        statements = sparqlDAO.getStatements(rawData, hasImage, null, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next();
            String imageURI = st.getObject().stringValue();
            // delete the image
            RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(f.createIRI(imageURI), null, null, graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(statements2), graphIRI);
            // delete it from the dataset as well
            RepositoryResult<Statement> statements3 = sparqlDAO.getStatements(f.createIRI(datasetURI), hasImage2, f.createIRI(imageURI), graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(statements3), graphIRI);
            deleteFiles (imageURI, graph);
        }*/
        
        deleteFiles(rawDataURI, graph);
        
        statements = sparqlDAO.getStatements(rawData, null, null, graphIRI);
        sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);    
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
        return getPrintedSlideByUser(user, offset, limit, field, order, searchValue, true);
    }
    
    @Override
    public List<PrintedSlide> getPrintedSlideByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue, Boolean loadAll) throws SparqlException, SQLException {
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
                PrintedSlide slide = getPrintedSlideFromURI(uri, loadAll, user);
                if (slide != null)
                    slides.add(slide);    
            }
        }
        
        return slides;
    }


    @Override
    public PrintedSlide getPrintedSlideFromURI(String uri, UserEntity user) throws SparqlException, SQLException {
        return getPrintedSlideFromURI(uri, true, user);
    }
        
    @Override
    public PrintedSlide getPrintedSlideFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException {
        PrintedSlide slideObject = new PrintedSlide();
        
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
                SlideLayout layout = layoutRepository.getSlideLayoutFromURI(layoutURI, loadAll, user);
                slideObject.setLayout(layout);
            } else if (st.getPredicate().equals(hasSlideMetadata)) {
                Value value = st.getObject();
                SlideMetadata slideMetadata = metadataRepository.getSlideMetadataFromURI(value.stringValue(), loadAll, user);
                slideObject.setMetadata(slideMetadata);
            } else if (st.getPredicate().equals(printedBy)) {
                Value value = st.getObject();
                Printer metadata = metadataRepository.getPrinterFromURI(value.stringValue(), loadAll, user);
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
                        uriValue = stPublic.getObject();
                        String layoutURI = uriValue.stringValue();
                        SlideLayout layout = layoutRepository.getSlideLayoutFromURI(layoutURI, loadAll, user);
                        slideObject.setLayout(layout);
                    } else if (stPublic.getPredicate().equals(RDFS.LABEL)) {
                        Value label = stPublic.getObject();
                        slideObject.setName(label.stringValue());
                    }  else if (stPublic.getPredicate().equals(RDFS.COMMENT)) {
                        Value comment = stPublic.getObject();
                        slideObject.setDescription(comment.stringValue());
                    } else if (stPublic.getPredicate().equals(hasSlideMetadata)) {
                        Value value = stPublic.getObject();
                        SlideMetadata slideMetadata = metadataRepository.getSlideMetadataFromURI(value.stringValue(), loadAll, user);
                        slideObject.setMetadata(slideMetadata);
                    } else if (stPublic.getPredicate().equals(printedBy)) {
                        Value value = stPublic.getObject();
                        Printer metadata = metadataRepository.getPrinterFromURI(value.stringValue(), loadAll, user);
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
        String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        }
        else {
            graph = getGraphForUser(user);
        }
        if (graph != null) {
            if (canDeletePrintedSlide(uriPre + slideId, user)) {
                deletePrintedSlideById (slideId, graph);
            } else {
                throw new IllegalArgumentException("Cannot delete printed slide " + slideId + ". It is used in an experiment");
            }
        }
    }
    
    private void deletePrintedSlideById (String slideId, String graph) throws RepositoryException, SparqlException {
        String uriPre = uriPrefix;
        if (graph.equals(DEFAULT_GRAPH)) {
            uriPre = uriPrefixPublic;
        }
        String uri = uriPre + slideId;
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI slide = f.createIRI(uri);
        IRI graphIRI = f.createIRI(graph);
        
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(slide, null, null, graphIRI);
        sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
    }
    
    @Override
    public boolean canDeletePrintedSlide(String uri, UserEntity user) throws SparqlException, SQLException {
        return canDeletePrintedSlide(uri, null, user);
    }
    
    @Override
    public boolean canDeletePrintedSlide(String uri, String parentURI, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        
        boolean canDelete = true;
        
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        //queryBuf.append ("FROM <" + DEFAULT_GRAPH + ">\n");
        queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ("{?s gadr:has_printed_slide <" +  uri + "> } ");
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


    @Override
    public void updatePrintedSlide(PrintedSlide printedSlide, UserEntity user) throws SparqlException, SQLException {
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
        if (printedSlide.getUri() != null)
            uri = printedSlide.getUri();
        else
            uri = uriPre + printedSlide.getId();
        
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI graphIRI = f.createIRI(graph);
        IRI slideIRI = f.createIRI(uri);
        IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
        
        IRI hasSlideMetadata = f.createIRI(slideMetadataPredicate);
        IRI printedBy = f.createIRI(printerMetadataPredicate);
        IRI hasSlideLayout = f.createIRI(MetadataTemplateRepository.templatePrefix + "has_slide_layout");
        
        
        // check if it exists
        RepositoryResult<Statement> result = sparqlDAO.getStatements(slideIRI, null, null, graphIRI);
        if (!result.hasNext()) {
            // does not exist
            throw new EntityNotFoundException("printed slide with the given id is not found in the repository");
        }
           
        // delete existing predicates
        sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(slideIRI, RDFS.LABEL, null, graphIRI)), graphIRI);
        sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(slideIRI, RDFS.COMMENT, null, graphIRI)), graphIRI);
        sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(slideIRI, hasSlideMetadata, null, graphIRI)), graphIRI);
        sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(slideIRI, printedBy, null, graphIRI)), graphIRI);
        sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(slideIRI, hasSlideLayout, null, graphIRI)), graphIRI);
        sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(slideIRI, hasModifiedDate, null, graphIRI)), graphIRI);
        
        List<Statement> statements = new ArrayList<Statement>();
        Literal date = f.createLiteral(new Date());
        statements.add(f.createStatement(slideIRI, hasModifiedDate, date, graphIRI));
        
        Literal label = printedSlide.getName() == null ? null : f.createLiteral(printedSlide.getName());
        Literal comment = printedSlide.getDescription() == null ? null : f.createLiteral(printedSlide.getDescription());
        
        if (label != null) 
            statements.add(f.createStatement(slideIRI, RDFS.LABEL, label, graphIRI));
        if (comment != null)
            statements.add(f.createStatement(slideIRI, RDFS.COMMENT, comment, graphIRI));
        if (printedSlide.getLayout() != null && printedSlide.getLayout().getUri() != null) {
            statements.add(f.createStatement(slideIRI, hasSlideLayout, f.createIRI(printedSlide.getLayout().getUri()), graphIRI));
        }
        if (printedSlide.getPrinter() != null && printedSlide.getPrinter().getUri() != null) {
            statements.add(f.createStatement(slideIRI, printedBy, f.createIRI(printedSlide.getPrinter().getUri()), graphIRI));
        } 
        if (printedSlide.getMetadata() != null && printedSlide.getMetadata().getUri() != null) {
            statements.add(f.createStatement(slideIRI, hasSlideMetadata, f.createIRI(printedSlide.getMetadata().getUri()), graphIRI));
        }
        
        statements.add(f.createStatement(slideIRI, hasModifiedDate, date, graphIRI));
        
        sparqlDAO.addStatements(statements, graphIRI);
    }

    @Override
    public ArrayDataset getArrayDatasetByLabel(String label, UserEntity user) throws SparqlException, SQLException {
        return getArrayDatasetByLabel(label, true, user);
    }

    @Override
    public ArrayDataset getArrayDatasetByLabel(String label, Boolean loadAll, UserEntity user) throws SparqlException, SQLException {
        ArrayDataset dataset = null;
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        if (label != null) {
            List<SparqlEntity> results = queryHelper.retrieveByLabel(label, datasetTypePredicate, graph);
            if (!results.isEmpty()) {
                dataset = getDatasetFromURI(results.get(0).getValue("s"), loadAll, user);
            }
        }
        
        return dataset;
    }


    @Override
    public PrintedSlide getPrintedSlideByLabel(String label, UserEntity user) throws SparqlException, SQLException {
        PrintedSlide slide = null;
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        if (label != null) {
            List<SparqlEntity> results = queryHelper.retrieveByLabel(label, printedSlideTypePredicate, graph);
            if (!results.isEmpty()) {
                // prefer the private result, if any
                for (SparqlEntity result: results) {
                    String uri = result.getValue("s");
                    if (!uri.contains("public")) {
                        return getPrintedSlideFromURI(uri, user);
                    }
                }
                
                // return the first result
                for (SparqlEntity result: results) {
                    String uri = result.getValue("s");
                    return getPrintedSlideFromURI(uri, user);
                } 
                        
            }
        }
        
        return slide;
    }


    @Override
    public void updateStatus(String uri, FutureTask task, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (user == null)
            graph = DEFAULT_GRAPH;
        else {
            graph = getGraphForUser(user);
        }
        
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI graphIRI = f.createIRI(graph);
        IRI taskIRI = f.createIRI(uri);
        IRI hasStatus = f.createIRI(ontPrefix + "has_status");
        IRI hasError = f.createIRI(ontPrefix + "has_error");
        IRI hasStatusDate = f.createIRI(ontPrefix + "has_status_date");
        
        // delete existing predicates
        sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(taskIRI, hasStatus, null, graphIRI)), graphIRI);
        sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(taskIRI, hasError, null, graphIRI)), graphIRI);
        sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(taskIRI, hasStatusDate, null, graphIRI)), graphIRI);
        
        
        Literal status = f.createLiteral(task.getStatus().name());
        Literal date = f.createLiteral(new Date());
        List<Statement> statements = new ArrayList<Statement>();
        
        statements.add(f.createStatement(taskIRI, hasStatus, status, graphIRI));
        statements.add(f.createStatement(taskIRI, hasStatusDate, date, graphIRI));
        
        if (task.getError() != null) {
            String error = task.getError().toString();
            Literal errorMessage = f.createLiteral(error);
            statements.add(f.createStatement(taskIRI, hasError, errorMessage, graphIRI));
        }
        
        sparqlDAO.addStatements(statements, graphIRI);
        
    }
    
    private void getStatusFromURI(String uri, FutureTask task, String graph) {
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI graphIRI = f.createIRI(graph);
        IRI taskIRI = f.createIRI(uri);
       
        IRI hasStatus = f.createIRI(ontPrefix + "has_status");
        IRI hasError = f.createIRI(ontPrefix + "has_error");
        IRI hasStatusDate = f.createIRI(ontPrefix + "has_status_date");
        
        RepositoryResult<Statement> result = sparqlDAO.getStatements(taskIRI, hasStatus, null, graphIRI);
        while (result.hasNext()) {
            Statement st = result.next();
            task.setStatus(FutureTaskStatus.valueOf(st.getObject().stringValue()));
        }
        result = sparqlDAO.getStatements(taskIRI, hasStatusDate, null, graphIRI);
        while (result.hasNext()) {
            Statement st = result.next();
            if (st.getObject() instanceof Literal) {
                Literal literal = (Literal)st.getObject();
                XMLGregorianCalendar calendar = literal.calendarValue();
                Date date = calendar.toGregorianCalendar().getTime();
                task.setStartDate(date);
            }
        }
        result = sparqlDAO.getStatements(taskIRI, hasError, null, graphIRI);
        while (result.hasNext()) {
            Statement st = result.next();
            String errorMessage = st.getObject().stringValue();
            ErrorMessage error = ErrorMessage.fromString(errorMessage);
            task.setError(error);  
        }
    }


    @Override
    public void updateArrayDataset(ArrayDataset dataset, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        }
        else {
            graph = getGraphForUser(user);
        }
        
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI graphIRI = f.createIRI(graph);
        String datasetURI = dataset.getUri();
        if (datasetURI == null && dataset.getId() != null) {
            datasetURI = uriPre + dataset.getId();
        }
        if (datasetURI != null) {
            String publicURI = null;
            
            IRI hasSample = f.createIRI(ontPrefix + "has_sample");
            IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
            IRI hasPub = f.createIRI(hasPublication);
            IRI hasGrantPredicate = f.createIRI(hasGrant);
            IRI hasCollab = f.createIRI(hasCollaborator);
            IRI datasetIRI = f.createIRI(datasetURI);
            IRI publicGraphIRI = f.createIRI(DEFAULT_GRAPH);
            
            // check if the dataset is made public by checking the has_public_uri predicate
            IRI hasPublicURI = f.createIRI(ontPrefix + "has_public_uri");
            RepositoryResult<Statement> results = sparqlDAO.getStatements(datasetIRI, hasPublicURI, null, graphIRI);
            if (results.hasNext()) {
                Statement st = results.next();
                publicURI = st.getObject().stringValue();
            }
            results.close();
            
            IRI publicIRI = publicURI == null ? null : f.createIRI(publicURI);
            
            List<Statement> statements = new ArrayList<Statement>();
            List<Statement> publicStatements = new ArrayList<Statement>();
            
            sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(datasetIRI, RDFS.LABEL, null, graphIRI)), graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(datasetIRI, RDFS.COMMENT, null, graphIRI)), graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(datasetIRI, hasModifiedDate, null, graphIRI)), graphIRI);
            
            if (publicIRI != null) {
                sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(publicIRI, RDFS.LABEL, null, publicGraphIRI)), publicGraphIRI);
                sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(publicIRI, RDFS.COMMENT, null, publicGraphIRI)), publicGraphIRI);
                sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(publicIRI, hasModifiedDate, null, publicGraphIRI)), publicGraphIRI);
            }
            
            Literal date = f.createLiteral(new Date());
            statements.add(f.createStatement(datasetIRI, hasModifiedDate, date, graphIRI));
            
            Literal label = dataset.getName() == null ? null : f.createLiteral(dataset.getName());
            Literal comment = dataset.getDescription() == null ? null : f.createLiteral(dataset.getDescription());
            
            if (label != null) {
                statements.add(f.createStatement(datasetIRI, RDFS.LABEL, label, graphIRI));
                if (publicIRI != null)
                    publicStatements.add(f.createStatement(publicIRI, RDFS.LABEL, label, publicGraphIRI));
            }
            if (comment != null) {
                statements.add(f.createStatement(datasetIRI, RDFS.COMMENT, comment, graphIRI));
                if (publicIRI != null)
                    publicStatements.add(f.createStatement(publicIRI, RDFS.COMMENT, comment, publicGraphIRI));
            }
            
            if (dataset.getSample() != null && publicIRI == null) { // do not allow changing the sample if the dataset is public
                String sampleURI = dataset.getSample().getUri();
                if (sampleURI == null && dataset.getSample().getId() != null) {
                    sampleURI = uriPre + dataset.getSample().getId();
                }
                if (sampleURI != null) {
                    sparqlDAO.removeStatements(Iterations.asList(sparqlDAO.getStatements(datasetIRI, hasSample, null, graphIRI)), graphIRI);
                    IRI sample = f.createIRI(sampleURI);
                    statements.add(f.createStatement(datasetIRI, hasSample, sample, graphIRI));
                }
            }
            if (dataset.getPublications() != null) {
                RepositoryResult<Statement> results2 = null;
                // get existing publications
                if (publicIRI == null) {
                    results2 = sparqlDAO.getStatements(datasetIRI, hasPub, null, graphIRI);
                }
                else {
                    // get from public graph
                    results2 = sparqlDAO.getStatements(publicIRI, hasPub, null, publicGraphIRI);
                }
                while (results2.hasNext()) {
                    Statement st = results2.next();
                    String pub = st.getObject().stringValue();
                    if (publicIRI == null) {
                        deletePublication(pub.substring(pub.lastIndexOf("/")+1), dataset.getId(), user);
                    } else {
                        deletePublication(pub.substring(pub.lastIndexOf("/")+1), publicURI.substring(publicURI.lastIndexOf("/")+1), null);
                    }
                }
                for (Publication pub: dataset.getPublications()) {
                    if (publicIRI == null) {
                        addPublication(pub, dataset.getId(), user);
                    } else {
                        addPublication(pub, publicURI.substring(publicURI.lastIndexOf("/")+1), null);
                    }
                }
            }
            
            if (dataset.getGrants() != null) {
                RepositoryResult<Statement> results2 = null;
                // get existing grants
                if (publicIRI == null) {
                    results2 = sparqlDAO.getStatements(datasetIRI, hasGrantPredicate, null, graphIRI);
                }
                else {
                    // get from public graph
                    results2 = sparqlDAO.getStatements(publicIRI, hasGrantPredicate, null, publicGraphIRI);
                }
                while (results2.hasNext()) {
                    Statement st = results2.next();
                    String pub = st.getObject().stringValue();
                    if (publicIRI == null) {
                        deleteGrant(pub.substring(pub.lastIndexOf("/")+1), dataset.getId(), user);
                    } else {
                        deleteGrant(pub.substring(pub.lastIndexOf("/")+1), publicURI.substring(publicURI.lastIndexOf("/")+1), null);
                    }
                }
                for (Grant grant: dataset.getGrants()) {
                    if (publicIRI == null) {
                        addGrant(grant, dataset.getId(), user);
                    } else {
                        addGrant(grant, publicURI.substring(publicURI.lastIndexOf("/")+1), null);
                    }
                }
            }
            
            if (dataset.getCollaborators() != null) {
                RepositoryResult<Statement> results2 = null;
                // get existing collaborators
                if (publicIRI == null) {
                    results2 = sparqlDAO.getStatements(datasetIRI, hasCollab, null, graphIRI);
                }
                else {
                    // get from public graph
                    results2 = sparqlDAO.getStatements(publicIRI, hasCollab, null, publicGraphIRI);
                }
                while (results2.hasNext()) {
                    Statement st = results2.next();
                    String username = st.getObject().stringValue();
                    if (publicIRI == null) {
                        deleteCollaborator(username, dataset.getId(), user);
                    } else {
                        deleteCollaborator(username, publicURI.substring(publicURI.lastIndexOf("/")+1), null);
                    }
                }
                for (Creator collab: dataset.getCollaborators()) {
                    if (publicIRI == null) {
                        addCollaborator(collab, dataset.getId(), user);
                    } else {
                        addCollaborator(collab, publicURI.substring(publicURI.lastIndexOf("/")+1), null);
                    }
                }
            }
            
            sparqlDAO.addStatements(statements, graphIRI);
            sparqlDAO.addStatements(publicStatements, publicGraphIRI);
        }
    }
    
    @Override
    @Async("GlygenArrayAsyncExecutor")
    public CompletableFuture<String> makePublicArrayDataset (ArrayDataset dataset, UserEntity user) throws SparqlException, SQLException {
        if (user == null) 
            throw new SparqlException ("The user must be provided to put data into private repository");
        
        String graph = getGraphForUser(user);
        
        // reload dataset with all the data
        dataset = getDatasetFromURI(dataset.getUri(), true, user);
        
        for (Slide slide: dataset.getSlides()) {
            // make assay metadata public
            AssayMetadata assayMetadata = slide.getMetadata();
            String assayPublicURI = makeMetadataPublic(assayMetadata, MetadataTemplateType.ASSAY, 
                    hasAssayTemplatePredicate, assayTypePredicate, "A", graph);
            AssayMetadata publicAssayMetadata = new AssayMetadata();
            publicAssayMetadata.setUri(assayPublicURI);
            slide.setMetadata(publicAssayMetadata);
            // clear out the uri so that it will be added again in the public graph
            slide.setUri(null);
            slide.setId(null);
            if (slide.getPrintedSlide() != null) {
                String publicPSURI = null;
                if (!slide.getPrintedSlide().isPublic()) {
                    // make its slide layout public
                    Map<String, String> blockLayoutUriMap = new HashMap<String, String>();
                    String slideLayoutPublicURI = layoutRepository.makePublic(slide.getPrintedSlide().getLayout(), user, blockLayoutUriMap);
                    SlideLayout publicLayout = new SlideLayout();
                    publicLayout.setUri(slideLayoutPublicURI);
                    slide.getPrintedSlide().setLayout(publicLayout);
                    
                    // need to reset rawData spots so they will be loaded again from the public repository during "add"
                    for (Image image: slide.getImages()) {
                        RawData rawData = image.getRawData();
                        if (rawData.getDataMap() != null && !rawData.getDataMap().isEmpty()) {
                            for (Measurement measurement: rawData.getDataMap().keySet()) {
                                Spot spot = rawData.getDataMap().get(measurement);
                                spot.setUri(null);
                                //fix its blocklayoutid
                                String blockLayoutUri = blockLayoutUriMap.get(spot.getBlockLayoutUri());
                                String blockLayoutId = null;
                                if (blockLayoutUri != null) {
                                    blockLayoutId = blockLayoutUri.substring(blockLayoutUri.lastIndexOf("/")+1);
                                }
                                if (blockLayoutId == null)
                                    blockLayoutId = layoutRepository.getPublicBlockLayoutUri(spot.getBlockLayoutUri(), user);
                                if (blockLayoutId != null)
                                    spot.setBlockLayoutUri(blockLayoutId);
                                else {
                                    throw new SparqlException ("public block layout for the spot cannot be set");
                                }
                            }
                        }
                        if (rawData.getProcessedDataList() != null) {
                            for (ProcessedData processedData: rawData.getProcessedDataList()) {
                                List<Feature> modifiedFeatures = new ArrayList<>();
                                if (processedData.getIntensity() != null) {
                                    for (Intensity intensity: processedData.getIntensity()) {
                                        for (Spot spot: intensity.getSpots()) {
                                            spot.setUri(null);
                                            //fix its blocklayoutid
                                            if (!spot.getBlockLayoutUri().contains("public"))
                                                spot.setBlockLayoutUri(layoutRepository.getPublicBlockLayoutUri(spot.getBlockLayoutUri(), user));
                                            // fix its features
                                            for (Feature f: spot.getFeatures()) {
                                                if (!modifiedFeatures.contains(f)) {
                                                    f.setId(featureRepository.getPublicFeatureId(f.getId(), user));
                                                    modifiedFeatures.add(f);
                                                }
                                            }
                                        } 
                                    }
                                }
                            }
                        }
                    }
                    
                    // make printed slide metadata public
                    SlideMetadata sMetadata = slide.getPrintedSlide().getMetadata();
                    String sPublicURI = makeMetadataPublic(sMetadata, MetadataTemplateType.SLIDE, 
                            hasSlideTemplatePredicate, slideTemplateTypePredicate, "Slm", graph);
                    sMetadata = new SlideMetadata();
                    sMetadata.setUri(sPublicURI);
                    slide.getPrintedSlide().setMetadata(sMetadata);
                    Printer printer = slide.getPrintedSlide().getPrinter();
                    String printerPublicURI = makeMetadataPublic(printer, MetadataTemplateType.PRINTER,
                            hasPrinterTemplatePredicate, printerTypePredicate, "P", graph);
                    printer = new Printer();
                    printer.setUri(printerPublicURI);
                    slide.getPrintedSlide().setPrinter(printer);
                    
                    // delete printedSlide from user's graph and add it to the public graph
                    publicPSURI = makePrintedSlidePublic (slide.getPrintedSlide(), graph);
                }
                else {
                    publicPSURI = slide.getPrintedSlide().getUri();
                }
                slide.getPrintedSlide().setUri(publicPSURI);
                
                for (Image image: slide.getImages()) {
                    ScannerMetadata scanner = image.getScanner();
                    String scannerPublicURI = makeMetadataPublic(scanner, MetadataTemplateType.SCANNER, 
                            hasScannerleTemplatePredicate, scannerTypePredicate, "Sc", graph);
                    scanner = new ScannerMetadata();
                    scanner.setUri(scannerPublicURI);
                    image.setScanner(scanner);
                    // clear out the uri so that it will be added again in the public graph
                    image.setUri(null);
                    image.setId(null);
                    RawData rawData = image.getRawData();
                    // make its metadata public 
                    ImageAnalysisSoftware metadata = rawData.getMetadata();
                    if (metadata != null) {
                        String metadataPublicURI = makeMetadataPublic (metadata, 
                                MetadataTemplateType.IMAGEANALYSISSOFTWARE, hasImageTemplatePredicate, imageAnalysisTypePredicate, "Im", graph);
                        ImageAnalysisSoftware publicMetadata = new ImageAnalysisSoftware();
                        publicMetadata.setUri(metadataPublicURI);
                        rawData.setMetadata(publicMetadata);
                    }
                    
                    // make processed data public
                    for (ProcessedData processedData: rawData.getProcessedDataList()) {
                        DataProcessingSoftware dataPRocessingMetadata = processedData.getMetadata();
                        if (dataPRocessingMetadata != null) {
                            String publicURI = makeMetadataPublic(dataPRocessingMetadata, MetadataTemplateType.DATAPROCESSINGSOFTWARE, 
                                    hasDataprocessingTemplatePredicate, dataProcessingTypePredicate, "DPM", graph);
                            dataPRocessingMetadata = new DataProcessingSoftware();
                            dataPRocessingMetadata.setUri(publicURI);
                            processedData.setMetadata(dataPRocessingMetadata);
                            
                        }
                    }
                }
            }
        }
        
        // make sample public
        String samplePublicURI = makeMetadataPublic(dataset.getSample(), MetadataTemplateType.SAMPLE, 
                hasSampleTemplatePredicate, sampleTypePredicate, "SA", graph);
        Sample publicSample = new Sample();
        publicSample.setUri(samplePublicURI);
        dataset.setSample(publicSample);
        
        // delete the dataset from user's graph
        deleteArrayDataset(dataset.getId(), user);
        String publicURI  = addPublicDataset(dataset, graph);
        return CompletableFuture.completedFuture(publicURI);  
    }

    private String makePrintedSlidePublic(PrintedSlide printedSlide, String graph) throws SparqlException, SQLException {
        deletePrintedSlideById(printedSlide.getId(), graph);
        String publicURI = addPrintedSlide(printedSlide, null);
        addPublicURI (printedSlide.getUri(), printedSlide.getName(), publicURI, printedSlide.getDateCreated(), printedSlide.getDateAddedToLibrary(), printedSlideTypePredicate, graph);
        return publicURI;
    }

    private String addPublicDataset(ArrayDataset dataset, String graph) throws SparqlException, SQLException {
        // add it to the public graph
        List<Statement> statements = new ArrayList<Statement>();
        ValueFactory f = sparqlDAO.getValueFactory();
        String publicURI = generateUniqueURI(uriPrefixPublic + "AD", graph);
        Literal date = f.createLiteral(new Date());
        Literal createdDate = f.createLiteral(dataset.getDateAddedToLibrary());
        IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
       
        Literal label = dataset.getName() == null ? null : f.createLiteral(dataset.getName());
        Literal comment = dataset.getDescription() == null ? null : f.createLiteral(dataset.getDescription());
        IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
        IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
        IRI publicDataset = f.createIRI(publicURI);
        IRI graphIRI = f.createIRI(DEFAULT_GRAPH);
        IRI hasSample = f.createIRI(ontPrefix + "has_sample");
        IRI hasImage = f.createIRI(hasImagePredicate);
        IRI hasSlide = f.createIRI(ontPrefix + "has_slide");
        IRI createdBy= f.createIRI(ontPrefix + "created_by");
        IRI type = f.createIRI(datasetTypePredicate);
        
        Literal owner = f.createLiteral(dataset.getUser().getName());
        
        // add a link from user's graph to the public graph
        addPublicURI(dataset.getUri(), dataset.getName(), publicURI, dataset.getDateCreated(), dataset.getDateAddedToLibrary(), datasetTypePredicate, graph);
        
        // add other parts to the public graph
        if (dataset.getSample() != null && dataset.getSample().getUri() != null) {
            IRI sample = f.createIRI(dataset.getSample().getUri());
            statements.add(f.createStatement(publicDataset, hasSample, sample, graphIRI));
        }
        
        String datasetId = publicURI.substring(publicURI.lastIndexOf("/")+1);
        
        if (dataset.getSlides() != null && !dataset.getSlides().isEmpty()) {
            for (Slide slide: dataset.getSlides()) {
                for (Image image: slide.getImages()) {
                    RawData rawData = image.getRawData();
                    if (rawData != null) {
                        if (rawData.getProcessedDataList() != null) {
                            for (ProcessedData processedData: rawData.getProcessedDataList()) {
                                String processedDataURI = addProcessedData(processedData, datasetId, null);
                                processedData.setUri(processedDataURI);
                            }
                        } else {
                            logger.warn("Processed data is not found for rawData: " + rawData.getUri());
                        }
                        String rawDataURI = addRawData(rawData, datasetId, null);
                        rawData.setSlide(slide);
                        rawData.setUri(rawDataURI);
                        addMeasurementsToRawData(rawData, null);
                    }
                }
                addSlide(slide, datasetId, null);
            }
        }
        
        statements.add(f.createStatement(publicDataset, hasCreatedDate, date, graphIRI));
        if (label != null) statements.add(f.createStatement(publicDataset, RDFS.LABEL, label, graphIRI));
        if (comment != null) statements.add(f.createStatement(publicDataset, RDFS.COMMENT, comment, graphIRI));
        statements.add(f.createStatement(publicDataset, hasAddedToLibrary, createdDate, graphIRI));
        statements.add(f.createStatement(publicDataset, hasModifiedDate, date, graphIRI));
        statements.add(f.createStatement(publicDataset, createdBy, owner, graphIRI));
        statements.add(f.createStatement(publicDataset, RDF.TYPE, type, graphIRI));
        sparqlDAO.addStatements(statements, graphIRI);
        
        return publicURI;
    }
    
    private void addPublicURI (String uri, String label, String publicURI, Date dateCreated, Date dateAdded, String dataType, String graph) throws SparqlException {
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI local = f.createIRI(uri);
        IRI publicDataset = f.createIRI(publicURI);
        IRI userGraphIRI = f.createIRI(graph);
        IRI hasPublicURI = f.createIRI(ontPrefix + "has_public_uri");
        IRI type = f.createIRI(dataType);
        IRI hasCreatedDate = f.createIRI(hasCreatedDatePredicate);
        IRI hasAddedToLibrary = f.createIRI(hasAddedToLibraryPredicate);
        IRI hasModifiedDate = f.createIRI(hasModifiedDatePredicate);
        Literal date = f.createLiteral(new Date());
        Literal addedDate = f.createLiteral(dateAdded);
        Literal createdDate = f.createLiteral(dateCreated);
        Literal labelLit = label == null ? null : f.createLiteral(label);
        
        
        // create a triple "has_public_uri" from the uri in user's graph to the public one
        List<Statement> statements2 = new ArrayList<Statement>();
        statements2.add(f.createStatement(local, hasPublicURI, publicDataset, userGraphIRI));
        statements2.add(f.createStatement(local, RDF.TYPE, type, userGraphIRI));
        if (labelLit != null) statements2.add(f.createStatement(local, RDFS.LABEL, labelLit, userGraphIRI));
        statements2.add(f.createStatement(local, hasCreatedDate, createdDate, userGraphIRI));
        statements2.add(f.createStatement(local, hasAddedToLibrary, addedDate, userGraphIRI));
        statements2.add(f.createStatement(local, hasModifiedDate, date, userGraphIRI));
        sparqlDAO.addStatements(statements2, userGraphIRI);
    }


    private String makeMetadataPublic(MetadataCategory metadata, MetadataTemplateType metadataType, String templatePredicate, String typePredicate, String pre, String graph) throws SparqlException, SQLException { 
        // check if it is already public, then return the uri
        String publicURI = null;
        MetadataCategory existing = metadataRepository.getMetadataByLabel(metadata.getName(), typePredicate, null);
        if (existing == null) {
            metadataRepository.deleteMetadataById(metadata.getId(), graph);
            // add it to the public graph
            publicURI = metadataRepository.addMetadataCategory(metadata, metadataType, templatePredicate, typePredicate, pre, null);
        } else {
            publicURI = existing.getUri();
        }
        // add a link from user's graph to the public graph
        addPublicURI(metadata.getUri(), metadata.getName(), publicURI, metadata.getDateCreated(), metadata.getDateAddedToLibrary(), typePredicate, graph);
        return publicURI;
    }


    @Override
    public String addPublication(Publication pub, String datasetId, UserEntity user)
            throws SparqlException, SQLException {
        String graph;
        String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        } else {
            graph = getGraphForUser(user);
        }
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI dataset = f.createIRI(uriPre + datasetId);
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
        
        String[] allGraphs = (String[]) getAllUserGraphs().toArray(new String[0]);
        List<Statement> statements = new ArrayList<Statement>();
        String publicationURI = generateUniqueURI(uriPre + "P", allGraphs);
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
        
        statements.add(f.createStatement(dataset, hasPub, publication, graphIRI));
        sparqlDAO.addStatements(statements, graphIRI);
        pub.setUri(publicationURI);
        pub.setId(publicationURI.substring(publicationURI.lastIndexOf("/")+1));
        return publicationURI;
    }
    
    @Override
    public Grant getGrantFromURI(String uri, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (uri.contains("public"))
            graph = DEFAULT_GRAPH;
        else {
            if (user != null)
                graph = getGraphForUser(user);
            else 
                graph = DEFAULT_GRAPH;
        }
        
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI graphIRI = f.createIRI(graph);
        IRI hasIdentifier = f.createIRI(hasIdentiferPredicate);
        IRI hasOrg = f.createIRI(hasOrganizationPredicate);
        IRI hasURL = f.createIRI(hasURLPredicate);
        
        IRI p = f.createIRI(uri);
        Grant grant = new Grant();
        grant.setUri(uri);
        grant.setId(uri.substring(uri.lastIndexOf("/")+1));
        RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(p, null, null, graphIRI);
        while (statements2.hasNext()) {
            Statement st2 = statements2.next();
            if (st2.getPredicate().equals(RDFS.LABEL)) {
                Value val = st2.getObject();
                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                    grant.setTitle(val.stringValue());
                }
            } else if (st2.getPredicate().equals(hasIdentifier)) {
                Value val = st2.getObject();
                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                    grant.setIdentifier(val.stringValue());
                }
            } else if (st2.getPredicate().equals(hasURL)) {
                Value val = st2.getObject();
                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                    grant.setURL(val.stringValue());
                }
            } else if (st2.getPredicate().equals(hasOrg)) {
                Value val = st2.getObject();
                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                    grant.setFundingOrganization(val.stringValue());
                }
            } 
        }
        return grant;
    }

    @Override
    public Publication getPublicationFromURI(String uri, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        if (uri.contains("public"))
            graph = DEFAULT_GRAPH;
        else {
            if (user != null)
                graph = getGraphForUser(user);
            else 
                graph = DEFAULT_GRAPH;
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
        
        IRI p = f.createIRI(uri);
        Publication publication = new Publication();
        publication.setUri(uri);
        publication.setId(uri.substring(uri.lastIndexOf("/")+1));
        RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(p, null, null, graphIRI);
        while (statements2.hasNext()) {
            Statement st2 = statements2.next();
            if (st2.getPredicate().equals(hasTitle)) {
                Value val = st2.getObject();
                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                    publication.setTitle(val.stringValue());
                }
            } else if (st2.getPredicate().equals(hasAuthor)) {
                Value val = st2.getObject();
                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                    publication.setAuthors(val.stringValue());
                }
            } else if (st2.getPredicate().equals(hasYear)) {
                Value val = st2.getObject();
                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                    publication.setYear(Integer.parseInt(val.stringValue()));
                }
            } else if (st2.getPredicate().equals(hasDOI)) {
                Value val = st2.getObject();
                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                    publication.setDoiId(val.stringValue());
                }
            } else if (st2.getPredicate().equals(hasVolume)) {
                Value val = st2.getObject();
                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                    publication.setVolume(val.stringValue());
                }
            } else if (st2.getPredicate().equals(hasJournal)) {
                Value val = st2.getObject();
                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                    publication.setJournal(val.stringValue());
                }
            } else if (st2.getPredicate().equals(hasNumber)) {
                Value val = st2.getObject();
                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                    publication.setNumber(val.stringValue());
                }
            } else if (st2.getPredicate().equals(hasStartPage)) {
                Value val = st2.getObject();
                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                    publication.setStartPage(val.stringValue());
                }
            } else if (st2.getPredicate().equals(hasEndPage)) {
                Value val = st2.getObject();
                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                    publication.setEndPage(val.stringValue());
                }
            } else if (st2.getPredicate().equals(hasPubMed)) {
                Value val = st2.getObject();
                if (val != null && val.stringValue() != null && !val.stringValue().isEmpty()) {
                    publication.setPubmedId(Integer.parseInt(val.stringValue()));
                }
            } 
        }
        return publication;
    }
    
    @Override
    public void deletePublication (String publicationId, String datasetId, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        }
        else {
            graph = getGraphForUser(user);
        }
        
        String uri = uriPre + publicationId;
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI pub = f.createIRI(uri);
        IRI graphIRI = f.createIRI(graph);
        IRI hasPub = f.createIRI(hasPublication);
        IRI dataset = f.createIRI(uriPre + datasetId);
        
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(pub, null, null, graphIRI);
        if (statements != null) {
            sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
            statements = sparqlDAO.getStatements(dataset, hasPub, pub, graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
        }
        else {
            //need to check all the graphs that this user can access
            String otherGraph = getCoownerGraphForUser(user, uriPre + datasetId);
            if (otherGraph != null) {
                graphIRI = f.createIRI(otherGraph);
                statements = sparqlDAO.getStatements(pub, null, null, graphIRI);
                sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
                statements = sparqlDAO.getStatements(dataset, hasPub, pub, graphIRI);
                sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
            }
            
        }        
    }
    
    @Override
    public void deleteGrant (String grantId, String datasetId, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        }
        else {
            graph = getGraphForUser(user);
        }
        
        String uri = uriPre + grantId;
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI grant = f.createIRI(uri);
        IRI graphIRI = f.createIRI(graph);
        IRI hasGrantPred = f.createIRI(hasGrant);
        IRI dataset = f.createIRI(uriPre + datasetId);
        
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(grant, null, null, graphIRI);
        if (statements != null) {
            sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
            statements = sparqlDAO.getStatements(dataset, hasGrantPred, grant, graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
        }
        else {
            //need to check all the graphs that this user can access
            String otherGraph = getCoownerGraphForUser(user, uriPre + datasetId);
            if (otherGraph != null) {
                graphIRI = f.createIRI(otherGraph);
                statements = sparqlDAO.getStatements(grant, null, null, graphIRI);
                sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
                statements = sparqlDAO.getStatements(dataset, hasGrantPred, grant, graphIRI);
                sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
            }
        }
    }
    
    @Override
    public void deleteCollaborator (String username, String datasetId, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        }
        else {
            graph = getGraphForUser(user);
        }
        
        ValueFactory f = sparqlDAO.getValueFactory();
        Literal collab = f.createLiteral(username);
        IRI graphIRI = f.createIRI(graph);
        IRI hasCollab = f.createIRI(hasCollaborator);
        IRI dataset = f.createIRI(uriPre + datasetId);
        
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(dataset, hasCollab, collab, graphIRI);
        if (statements != null)
            sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
        else {
            //need to check all the graphs that this user can access
            String otherGraph = getCoownerGraphForUser(user, uriPre + datasetId);
            if (otherGraph != null) {
                statements = sparqlDAO.getStatements(dataset, hasCollab, collab, f.createIRI(otherGraph));
                sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
            }
        }
    }
    
    @Override
    public void deleteCoowner (UserEntity coowner, String datasetURI, UserEntity user) throws SQLException {
        List<GraphPermissionEntity> entities = permissionRepository.findByUserAndResourceIRI(coowner, datasetURI);
        if (entities != null) {
            for (GraphPermissionEntity entity: entities)
                permissionRepository.delete(entity);
        }
    }
    
    @Override
    public int getIntensityDataListCount(String processedDataId, UserEntity user) throws SparqlException, SQLException {
        String graph = null;
        String uriPre = uriPrefix;
        if (user == null) {
            graph = DEFAULT_GRAPH;
            uriPre = uriPrefixPublic;
        }
        else {
            graph = getGraphForUser(user);
        }
        int total = 0;
        
        if (graph != null) {
            StringBuffer queryBuf = new StringBuffer();
            queryBuf.append (prefix + "\n");
            queryBuf.append ("SELECT count(DISTINCT ?feature) as ?count \n");
            queryBuf.append ("FROM <" + graph + ">\n");
            queryBuf.append ("WHERE {\n");
            queryBuf.append (
                    "<" + uriPre + processedDataId +"> gadr:has_intensity ?intensity .\n" +
                    " ?intensity gadr:binding_value_of ?spot . \n" +
                    " ?spot gadr:has_feature ?feature . \n" +
                    " ?intensity gadr:has_rfu ?rfu . \n" + 
                    " }");
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
    public List<IntensityData> getIntensityDataList (String processedDataId, UserEntity user, int offset, int limit, String field, int order,
            String searchValue) throws SparqlException, SQLException {
        List<IntensityData> data = new ArrayList<>();
        
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
            
            String sortPredicate = getSortPredicate (field);
            
            String searchPredicate = "";
            if (searchValue != null && !searchValue.isEmpty())
                searchPredicate = getSearchPredicate(searchValue);
            
            String sortLine = "";
            if (sortPredicate != null)
                sortLine = "OPTIONAL {?s " + sortPredicate + " ?sortBy } .\n";  
            String orderByLine = " ORDER BY " + (order == 0 ? "DESC" : "ASC") + (sortPredicate == null ? "(?s)": "(?sortBy)");  
            StringBuffer queryBuf = new StringBuffer();
            queryBuf.append (prefix + "\n");
            queryBuf.append ("SELECT DISTINCT ?feature, ?intensity, ?rfu, ?stdev, ?cv \n");
            queryBuf.append ("FROM <" + graph + ">\n");
            queryBuf.append ("WHERE {\n");
            queryBuf.append (
                    "<" + uriPre + processedDataId +"> gadr:has_intensity ?intensity .\n" +
                    " ?intensity gadr:binding_value_of ?spot . \n" +
                    " ?spot gadr:has_feature ?feature . \n" +
                    " ?intensity gadr:has_rfu ?rfu . \n" + 
                    " OPTIONAL {?intensity gadr:has_stdev ?stdev . } \n" + 
                    " OPTIONAL { ?intensity gadr:has_cv ?cv .} \n" + 
                            sortLine + searchPredicate + 
                    "}\n" +
                     orderByLine + 
                    ((limit == -1) ? " " : " LIMIT " + limit) +
                    " OFFSET " + offset);
            
            List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
            for (SparqlEntity result: results) {
                String featureURI = result.getValue("feature");
                String intensityURI = result.getValue("intensity");
                String rfu = result.getValue("rfu");
                String stdev = result.getValue("stdev");
                String cv = result.getValue("cv");
                Feature feature = featureRepository.getFeatureFromURI(featureURI, user);
                Intensity intensity = new Intensity();
                intensity.setUri(intensityURI);
                intensity.setId(intensityURI.substring(intensityURI.lastIndexOf("/")+1));
                try {
                    intensity.setRfu(Double.parseDouble(rfu));
                    if (cv != null && !cv.isEmpty()) intensity.setPercentCV(Double.parseDouble(cv));
                    if (stdev != null && !stdev.isEmpty()) intensity.setStDev(Double.parseDouble(stdev));
                } catch (NumberFormatException e) {
                    logger.error("this number(rfu/stdev/cv) should be a double in the repository!", e);
                } 
                IntensityData i = new IntensityData();
                i.setFeature(feature);
                i.setIntensity(intensity);
                data.add(i);
            }
        }
        
        return data;
    }


    @Override
    public void deleteSlide(String slideId, String datasetId, UserEntity user) throws SQLException, SparqlException {
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
            String uri = uriPre + datasetId;
            ValueFactory f = sparqlDAO.getValueFactory();
            IRI hasSlide = f.createIRI(ontPrefix + "has_slide");
            IRI graphIRI = f.createIRI(graph);
            IRI dataset = f.createIRI(uri);
            IRI scanOf = f.createIRI(scanOfPredicate);
            
            IRI slide = f.createIRI(uriPre + slideId);
            
            RepositoryResult<Statement> statements;
            // delete images
            statements = sparqlDAO.getStatements(null, scanOf, slide, graphIRI);
            while (statements.hasNext()) {
                Statement st = statements.next();
                String imageURI = st.getSubject().stringValue();
                deleteImage (imageURI, datasetId, user);
            }
            
            // delete the slide
            statements = sparqlDAO.getStatements(slide, null, null, graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
            
            statements = sparqlDAO.getStatements(dataset, hasSlide, f.createIRI(uriPre + slideId), graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
        }
        
    }

    private void deleteImage(String imageURI, String datasetId, UserEntity user) throws SQLException, SparqlException {
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
            ValueFactory f = sparqlDAO.getValueFactory();
            IRI derivedFrom = f.createIRI(derivedFromPredicate);
            IRI hasImage = f.createIRI(ontPrefix + "has_image");
            IRI graphIRI = f.createIRI(graph);
            deleteFiles (imageURI, graph);
            
            IRI image = f.createIRI(imageURI);
            IRI dataset = f.createIRI(uriPre + datasetId);
            
            // delete RawData
            RepositoryResult<Statement> statements;
            statements = sparqlDAO.getStatements(null, derivedFrom, image, graphIRI);
            while (statements.hasNext()) {
                Statement st = statements.next();
                String rawDataURI = st.getSubject().stringValue();
                deleteRawData(rawDataURI.substring(rawDataURI.lastIndexOf("/")+1), datasetId, user);
            }
            
            statements = sparqlDAO.getStatements(image, null, null, graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
            
            statements = sparqlDAO.getStatements(dataset, hasImage, image, graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
        } 
    }

    @Override
    public String getDatasetPublicId(String datasetId) throws SparqlException {
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?publicURI \n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ("<" + uriPrefix + datasetId + "> gadr:has_public_uri ?publicURI . }");
        
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        if (results != null && !results.isEmpty()) {
            String publicURI = results.get(0).getValue("publicURI");
            return publicURI.substring(publicURI.lastIndexOf("/")+1);
        }
        return null;
    }

    @Override
    public List<ArrayDataset> getArrayDatasetByCoOwner(UserEntity user, int offset, int limit, String field, int order,
            String searchValue, boolean loadAll) throws SparqlException, SQLException {
        List<ArrayDataset> datasets = new ArrayList<ArrayDataset>();
        List<String> graphs = new ArrayList<String>();
        List<GraphPermissionEntity> permissions = permissionRepository.findByUser(user);
        for (GraphPermissionEntity entity: permissions) {
            String graph = entity.getGraphIRI();
            if (!graphs.contains(graph))
                graphs.add(graph);
        }
        
        for (String graph: graphs) {
            List<SparqlEntity> results = retrieveByTypeAndUser(offset, limit, field, order, searchValue, graph, datasetTypePredicate);    
            for (SparqlEntity sparqlEntity : results) {
                String uri = sparqlEntity.getValue("s");
                for (GraphPermissionEntity entity: permissions) {
                    if (graph.equals(entity.getGraphIRI()) && uri.equalsIgnoreCase(entity.getResourceIRI())) {
                        // find the user
                        UserEntity original = userRepository.findByUsernameIgnoreCase(graph.substring(graph.lastIndexOf("/")+1));
                        ArrayDataset dataset = getDatasetFromURI(uri, loadAll, original);
                        if (dataset != null)
                            datasets.add(dataset);   
                    }
                }
                
            }
        }
         
        return datasets;
    }


    @Override
    public int getArrayDatasetCountByCoOwner(UserEntity user) throws SQLException, SparqlException {
        List<GraphPermissionEntity> permissions = permissionRepository.findByUser(user);
        if (permissions != null)
            return permissions.size();
        else
            return 0;
       /* int count = 0;
        for (GraphPermissionEntity entity: permissions) {
            String graph = entity.getGraphIRI();
            count += getCountByUserByType(graph, datasetTypePredicate);
        }
        
        return count;*/
    }


    @Override
    public void addCowner(UserEntity coowner, String datasetURI, UserEntity user) throws SparqlException, SQLException {
        //TODO check for duplicates??
        String graph = getGraphForUser(user);
        GraphPermissionEntity entity = new GraphPermissionEntity();
        entity.setResourceIRI(datasetURI);
        entity.setUser(coowner);
        entity.setGraphIRI(graph);
        entity.setAdditionDate(new Date());
        permissionRepository.save(entity);
    }
}
