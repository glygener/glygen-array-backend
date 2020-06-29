package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.data.ArrayDataset;
import org.glygen.array.persistence.rdf.metadata.DataProcessingSoftware;
import org.glygen.array.persistence.rdf.metadata.ImageAnalysisSoftware;
import org.glygen.array.persistence.rdf.metadata.Printer;
import org.glygen.array.persistence.rdf.metadata.Sample;
import org.glygen.array.persistence.rdf.metadata.ScannerMetadata;
import org.glygen.array.persistence.rdf.metadata.SlideMetadata;

public interface ArrayDatasetRepository {
    
    String addArrayDataset (ArrayDataset dataset, UserEntity user) throws SparqlException, SQLException;
    ArrayDataset getArrayDataset (String datasetId, UserEntity user) throws SparqlException, SQLException;
    
    List<ArrayDataset> getArrayDatasetByUser (UserEntity user) throws SparqlException, SQLException;
    
    List<ArrayDataset> getArrayDatasetByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<ArrayDataset> getArrayDatasetByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    
    void deleteArrayDataset (String datasetId, UserEntity user) throws SparqlException, SQLException;
    
    String addSample (Sample sample, UserEntity user) throws SparqlException, SQLException;
    
    List<Sample> getSampleByUser (UserEntity user) throws SparqlException, SQLException;
    
    List<Sample> getSampleByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<Sample> getSampleByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    void deleteSample (String sampleId, UserEntity user) throws SparqlException, SQLException;
    int getSampleCountByUser(UserEntity user) throws SQLException, SparqlException;
    
    String addPrinter (Printer metadata, UserEntity user) throws SparqlException, SQLException;
    List<Printer> getPrinterByUser (UserEntity user) throws SparqlException, SQLException;
    List<Printer> getPrinterByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<Printer> getPrinterByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    void deletePrinter (String id, UserEntity user) throws SparqlException, SQLException;
    int getPrinterCountByUser(UserEntity user) throws SQLException, SparqlException;
    
    String addScannerMetadata (ScannerMetadata metadata, UserEntity user) throws SparqlException, SQLException;
    List<ScannerMetadata> getScannerMetadataByUser (UserEntity user) throws SparqlException, SQLException;
    List<ScannerMetadata> getScannerMetadataByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<ScannerMetadata> getScannerMetadataByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    void deleteScannerMetadata (String id, UserEntity user) throws SparqlException, SQLException;
    int getScannerMetadataCountByUser(UserEntity user) throws SQLException, SparqlException;
    
    String addSlideMetadata (SlideMetadata metadata, UserEntity user) throws SparqlException, SQLException;
    List<SlideMetadata> getSlideMetadataByUser (UserEntity user) throws SparqlException, SQLException;
    List<SlideMetadata> getSlideMetadataByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<SlideMetadata> getSlideMetadataByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    void deleteSlideMetadata (String id, UserEntity user) throws SparqlException, SQLException;
    int getSlideMetadataCountByUser(UserEntity user) throws SQLException, SparqlException;
    
    String addImageAnalysisSoftware (ImageAnalysisSoftware metadata, UserEntity user) throws SparqlException, SQLException;
    List<ImageAnalysisSoftware> getImageAnalysisSoftwareByUser (UserEntity user) throws SparqlException, SQLException;
    List<ImageAnalysisSoftware> getImageAnalysisSoftwareByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<ImageAnalysisSoftware> getImageAnalysisSoftwareByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    void deleteImageAnalysisSoftware (String id, UserEntity user) throws SparqlException, SQLException;
    int getImageAnalysisSoftwareCountByUser(UserEntity user) throws SQLException, SparqlException;
    
    String addDataProcessingSoftware (DataProcessingSoftware metadata, UserEntity user) throws SparqlException, SQLException;
    List<DataProcessingSoftware> getDataProcessingSoftwareByUser (UserEntity user) throws SparqlException, SQLException;
    List<DataProcessingSoftware> getDataProcessingSoftwareByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<DataProcessingSoftware> getDataProcessingSoftwareByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    void deleteDataProcessingSoftware (String id, UserEntity user) throws SparqlException, SQLException;
    int getDataProcessingSoftwareCountByUser(UserEntity user) throws SQLException, SparqlException;
    
    
}
