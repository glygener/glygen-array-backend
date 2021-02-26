package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.Publication;
import org.glygen.array.persistence.rdf.data.ArrayDataset;
import org.glygen.array.persistence.rdf.data.FutureTask;
import org.glygen.array.persistence.rdf.data.IntensityData;
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
import org.glygen.array.persistence.rdf.metadata.SpotMetadata;

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
    String addPublication (Publication publication, String datasetId, UserEntity user) throws SparqlException, SQLException;
    
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
    
    boolean canDeletePrintedSlide(String uri, UserEntity user) throws SparqlException, SQLException;
    boolean canDeletePrintedSlide(String uri, String parentURI, UserEntity user) throws SparqlException, SQLException;
    void deleteRawData(String rawDataId, String parentId, UserEntity user) throws SQLException, SparqlException;
    void deleteProcessedData(String rawDataId, String datasetId, UserEntity user) throws SQLException, SparqlException;
    void deleteSlide(String slideId, String datasetId, UserEntity user) throws SQLException, SparqlException;
    
    RawData getRawDataFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException;
    Publication getPublicationFromURI (String uri, UserEntity user) throws SparqlException, SQLException;
    void deletePublication(String publicationId, String datasetId, UserEntity user)
            throws SparqlException, SQLException;
    
    void updateStatus(String uri, FutureTask task, UserEntity user) throws SparqlException, SQLException;
    String addIntensitiesToProcessedData(ProcessedData processedData, UserEntity user)
            throws SparqlException, SQLException;
    ProcessedData getProcessedDataFromURI(String uriValue, Boolean loadAll, UserEntity user)
            throws SQLException, SparqlException;
    CompletableFuture<String> makePublicArrayDataset(ArrayDataset dataset, UserEntity user) throws SparqlException, SQLException;
    PrintedSlide getPrintedSlideFromURI(String uri, Boolean loadAll, UserEntity user)
            throws SparqlException, SQLException;
    CompletableFuture<String> addMeasurementsToRawData(RawData rawData, UserEntity user)
            throws SparqlException, SQLException;
    List<IntensityData> getIntensityDataList(String processedDataId, UserEntity user, int offset, int limit,
            String field, int order, String searchValue) throws SparqlException, SQLException;
    int getIntensityDataListCount(String processedDataId, UserEntity user) throws SparqlException, SQLException;
    Slide getSlideFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException;
    List<PrintedSlide> getPrintedSlideByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue, Boolean loadAll) throws SparqlException, SQLException;
    
    boolean isDatasetPublic (String datasetId) throws SparqlException;
}
