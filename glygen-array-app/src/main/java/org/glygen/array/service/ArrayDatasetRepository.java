package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.data.ArrayDataset;
import org.glygen.array.persistence.rdf.data.FutureTask;
import org.glygen.array.persistence.rdf.data.PrintedSlide;
import org.glygen.array.persistence.rdf.data.ProcessedData;
import org.glygen.array.persistence.rdf.data.RawData;
import org.glygen.array.persistence.rdf.data.Slide;
import org.glygen.array.persistence.rdf.metadata.AssayMetadata;
import org.glygen.array.persistence.rdf.metadata.DataProcessingSoftware;
import org.glygen.array.persistence.rdf.metadata.ImageAnalysisSoftware;
import org.glygen.array.persistence.rdf.metadata.MetadataCategory;
import org.glygen.array.persistence.rdf.metadata.Printer;
import org.glygen.array.persistence.rdf.metadata.Sample;
import org.glygen.array.persistence.rdf.metadata.ScannerMetadata;
import org.glygen.array.persistence.rdf.metadata.SlideMetadata;

public interface ArrayDatasetRepository {
    
    String addArrayDataset (ArrayDataset dataset, UserEntity user) throws SparqlException, SQLException;
    ArrayDataset getArrayDataset (String datasetId, UserEntity user) throws SparqlException, SQLException;
    ArrayDataset getArrayDataset(String datasetId, Boolean loadAll, UserEntity user)
            throws SparqlException, SQLException;
    List<ArrayDataset> getArrayDatasetByUser (UserEntity user) throws SparqlException, SQLException;
    List<ArrayDataset> getArrayDatasetByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<ArrayDataset> getArrayDatasetByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    List<ArrayDataset> getArrayDatasetByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue, boolean loadAll) throws SparqlException, SQLException;
    int getArrayDatasetCountByUser(UserEntity user) throws SQLException, SparqlException;
    void deleteArrayDataset (String datasetId, UserEntity user) throws SparqlException, SQLException;
    ArrayDataset getArrayDatasetByLabel (String label, UserEntity user) throws SparqlException, SQLException;
    ArrayDataset getArrayDatasetByLabel(String label, Boolean loadAll, UserEntity user)
            throws SparqlException, SQLException;
    void updateArrayDataset (ArrayDataset dataset, UserEntity user) throws SparqlException, SQLException;
    
    String addProcessedData(ProcessedData processedData, String datasetId, UserEntity user)
            throws SparqlException, SQLException;
    String addSlide(Slide slide, String datasetId, UserEntity user) throws SparqlException, SQLException;
    String addRawData(RawData rawData, String datasetId, UserEntity user) throws SparqlException, SQLException;
    
    String addPrintedSlide(PrintedSlide printedSlide, UserEntity user) throws SparqlException, SQLException;
    List<PrintedSlide> getPrintedSlideByUser (UserEntity user) throws SparqlException, SQLException;
    List<PrintedSlide> getPrintedSlideByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<PrintedSlide> getPrintedSlideByUser (UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    PrintedSlide getPrintedSlideFromURI (String uri, UserEntity user) throws SparqlException, SQLException;
    int getPrintedSlideCountByUser(UserEntity user) throws SQLException, SparqlException;
    void deletePrintedSlide(String slideId, UserEntity user) throws SparqlException, SQLException;
    void updatePrintedSlide (PrintedSlide slide, UserEntity user) throws SparqlException, SQLException;
    PrintedSlide getPrintedSlideByLabel (String label, UserEntity user) throws SparqlException, SQLException;
    
    String addSample (Sample sample, UserEntity user) throws SparqlException, SQLException; 
    List<Sample> getSampleByUser (UserEntity user) throws SparqlException, SQLException;
    List<Sample> getSampleByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<Sample> getSampleByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    int getSampleCountByUser(UserEntity user) throws SQLException, SparqlException;
    Sample getSampleFromURI(String uri, UserEntity user) throws SparqlException, SQLException;
    Sample getSampleFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException;
    Sample getSampleByLabel(String label, UserEntity user) throws SparqlException, SQLException;
    
    String addPrinter (Printer metadata, UserEntity user) throws SparqlException, SQLException;
    List<Printer> getPrinterByUser (UserEntity user) throws SparqlException, SQLException;
    List<Printer> getPrinterByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<Printer> getPrinterByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    int getPrinterCountByUser(UserEntity user) throws SQLException, SparqlException;
    Printer getPrinterFromURI(String uri, UserEntity user) throws SparqlException, SQLException;
    Printer getPrinterFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException;
    Printer getPrinterByLabel(String label, UserEntity user) throws SparqlException, SQLException;
    
    String addScannerMetadata (ScannerMetadata metadata, UserEntity user) throws SparqlException, SQLException;
    List<ScannerMetadata> getScannerMetadataByUser (UserEntity user) throws SparqlException, SQLException;
    List<ScannerMetadata> getScannerMetadataByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<ScannerMetadata> getScannerMetadataByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    int getScannerMetadataCountByUser(UserEntity user) throws SQLException, SparqlException;
    ScannerMetadata getScannerMetadataFromURI(String uri, UserEntity user) throws SparqlException, SQLException;
    ScannerMetadata getScannerMetadataFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException;
    ScannerMetadata getScannerMetadataByLabel(String label, UserEntity user) throws SparqlException, SQLException;
    
    String addSlideMetadata (SlideMetadata metadata, UserEntity user) throws SparqlException, SQLException;
    List<SlideMetadata> getSlideMetadataByUser (UserEntity user) throws SparqlException, SQLException;
    List<SlideMetadata> getSlideMetadataByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<SlideMetadata> getSlideMetadataByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    int getSlideMetadataCountByUser(UserEntity user) throws SQLException, SparqlException;
    SlideMetadata getSlideMetadataFromURI(String uri, UserEntity user) throws SparqlException, SQLException;
    SlideMetadata getSlideMetadataFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException;
    SlideMetadata getSlideMetadataByLabel(String label, UserEntity user) throws SparqlException, SQLException;
    
    String addImageAnalysisSoftware (ImageAnalysisSoftware metadata, UserEntity user) throws SparqlException, SQLException;
    List<ImageAnalysisSoftware> getImageAnalysisSoftwareByUser (UserEntity user) throws SparqlException, SQLException;
    List<ImageAnalysisSoftware> getImageAnalysisSoftwareByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<ImageAnalysisSoftware> getImageAnalysisSoftwareByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    int getImageAnalysisSoftwareCountByUser(UserEntity user) throws SQLException, SparqlException;
    ImageAnalysisSoftware getImageAnalysisSoftwareFromURI(String uri, UserEntity user) throws SparqlException, SQLException;
    ImageAnalysisSoftware getImageAnalysisSoftwareFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException;
    ImageAnalysisSoftware getImageAnalysisSoftwarByLabel(String label, UserEntity user) throws SparqlException, SQLException;
    
    String addDataProcessingSoftware (DataProcessingSoftware metadata, UserEntity user) throws SparqlException, SQLException;
    List<DataProcessingSoftware> getDataProcessingSoftwareByUser (UserEntity user) throws SparqlException, SQLException;
    List<DataProcessingSoftware> getDataProcessingSoftwareByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<DataProcessingSoftware> getDataProcessingSoftwareByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    int getDataProcessingSoftwareCountByUser(UserEntity user) throws SQLException, SparqlException;
    DataProcessingSoftware getDataProcessingSoftwareFromURI(String uri, UserEntity user) throws SparqlException, SQLException;
    DataProcessingSoftware getDataProcessingSoftwareFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException;
    DataProcessingSoftware getDataProcessingSoftwareByLabel(String label, UserEntity user) throws SparqlException, SQLException;
    
    String addAssayMetadata(AssayMetadata metadata, UserEntity user) throws SparqlException, SQLException;
    List<AssayMetadata> getAssayMetadataByUser (UserEntity user) throws SparqlException, SQLException;
    List<AssayMetadata> getAssayMetadataByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<AssayMetadata> getAssayMetadataByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    int getAssayMetadataCountByUser(UserEntity user) throws SQLException, SparqlException;
    AssayMetadata getAssayMetadataFromURI(String uri, UserEntity user) throws SparqlException, SQLException;
    AssayMetadata getAssayMetadataFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException;
    AssayMetadata getAssayMetadataByLabel(String label, UserEntity user) throws SparqlException, SQLException;
    
    MetadataCategory getMetadataByLabel(String label, String typePredicate, UserEntity user) throws SparqlException, SQLException;
    void updateMetadata(MetadataCategory metadata, UserEntity user) throws SparqlException, SQLException;
    void deleteMetadata (String metadataId, UserEntity user) throws SparqlException, SQLException;
    void updateMetadataMirage(MetadataCategory metadata, UserEntity user) throws SQLException, SparqlException;
    boolean canDeletePrintedSlide(String uri, UserEntity user) throws SparqlException, SQLException;
    boolean canDeletePrintedSlide(String uri, String parentURI, UserEntity user) throws SparqlException, SQLException;
    void deleteRawData(String rawDataId, String parentId, UserEntity user) throws SQLException, SparqlException;
    void deleteProcessedData(String rawDataId, String datasetId, UserEntity user) throws SQLException, SparqlException;
    
    RawData getRawDataFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException;
    
    void updateStatus(String uri, FutureTask task, UserEntity user) throws SparqlException, SQLException;
    String addIntensitiesToProcessedData(ProcessedData processedData, UserEntity user)
            throws SparqlException, SQLException;
    ProcessedData getProcessedDataFromURI(String uriValue, Boolean loadAll, UserEntity user)
            throws SQLException, SparqlException;
    String makePublicArrayDataset(ArrayDataset dataset, UserEntity user) throws SparqlException, SQLException;
}
