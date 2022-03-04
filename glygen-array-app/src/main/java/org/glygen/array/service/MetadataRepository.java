package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.metadata.AssayMetadata;
import org.glygen.array.persistence.rdf.metadata.DataProcessingSoftware;
import org.glygen.array.persistence.rdf.metadata.ImageAnalysisSoftware;
import org.glygen.array.persistence.rdf.metadata.MetadataCategory;
import org.glygen.array.persistence.rdf.metadata.PrintRun;
import org.glygen.array.persistence.rdf.metadata.Printer;
import org.glygen.array.persistence.rdf.metadata.Sample;
import org.glygen.array.persistence.rdf.metadata.ScannerMetadata;
import org.glygen.array.persistence.rdf.metadata.SlideMetadata;
import org.glygen.array.persistence.rdf.metadata.SpotMetadata;
import org.glygen.array.persistence.rdf.template.MetadataTemplateType;

public interface MetadataRepository {

    int getSpotMetadataCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException;

    String addSpotMetadata(SpotMetadata metadata, UserEntity user) throws SparqlException, SQLException;

    List<SpotMetadata> getSpotMetadataByUser(UserEntity user) throws SparqlException, SQLException;

    List<SpotMetadata> getSpotMetadataByUser(UserEntity user, int offset, int limit, String field, int order)
            throws SparqlException, SQLException;

    List<SpotMetadata> getSpotMetadataByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue) throws SparqlException, SQLException;
    List<SpotMetadata> getSpotMetadataByUser(UserEntity user, int offset, int limit, String field, int order,
            String searchValue, Boolean loadAll) throws SparqlException, SQLException;

    SpotMetadata getSpotMetadataFromURI(String uri, UserEntity user) throws SparqlException, SQLException;

    SpotMetadata getSpotMetadataFromURI(String uri, Boolean loadAll, UserEntity user)
            throws SparqlException, SQLException;

    SpotMetadata getSpotMetadataByLabel(String label, UserEntity user) throws SparqlException, SQLException;

    List<Sample> getSampleByUser(UserEntity user, int offset, int limit, String field, int order)
            throws SparqlException, SQLException;

    int getSlideMetadataCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException;
    
    String addSample (Sample sample, UserEntity user) throws SparqlException, SQLException; 
    List<Sample> getSampleByUser (UserEntity user) throws SparqlException, SQLException;
    List<Sample> getSampleByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    List<Sample> getSampleByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue, Boolean loadAll)
            throws SparqlException, SQLException;
    int getSampleCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException;
    Sample getSampleFromURI(String uri, UserEntity user) throws SparqlException, SQLException;
    Sample getSampleFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException;
    Sample getSampleByLabel(String label, UserEntity user) throws SparqlException, SQLException;
    
    String addPrinter (Printer metadata, UserEntity user) throws SparqlException, SQLException;
    List<Printer> getPrinterByUser (UserEntity user) throws SparqlException, SQLException;
    List<Printer> getPrinterByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<Printer> getPrinterByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    List<Printer> getPrinterByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue, Boolean loadAll)
            throws SparqlException, SQLException;
    int getPrinterCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException;
    Printer getPrinterFromURI(String uri, UserEntity user) throws SparqlException, SQLException;
    Printer getPrinterFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException;
    Printer getPrinterByLabel(String label, UserEntity user) throws SparqlException, SQLException;
    
    
    String addPrintRun(PrintRun metadata, UserEntity user) throws SparqlException, SQLException;
    List<PrintRun> getPrintRunByUser (UserEntity user) throws SparqlException, SQLException;
    List<PrintRun> getPrintRunByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<PrintRun> getPrintRunByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    List<PrintRun> getPrintRunByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue, Boolean loadAll)
            throws SparqlException, SQLException;
    int getPrintRunCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException;
    PrintRun getPrintRunFromURI(String uri, UserEntity user) throws SparqlException, SQLException;
    PrintRun getPrintRunFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException;
    PrintRun getPrintRunByLabel(String label, UserEntity user) throws SparqlException, SQLException;
    
    String addScannerMetadata (ScannerMetadata metadata, UserEntity user) throws SparqlException, SQLException;
    List<ScannerMetadata> getScannerMetadataByUser (UserEntity user) throws SparqlException, SQLException;
    List<ScannerMetadata> getScannerMetadataByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<ScannerMetadata> getScannerMetadataByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    List<ScannerMetadata> getScannerMetadataByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue, Boolean loadAll)
            throws SparqlException, SQLException;
    int getScannerMetadataCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException;
    ScannerMetadata getScannerMetadataFromURI(String uri, UserEntity user) throws SparqlException, SQLException;
    ScannerMetadata getScannerMetadataFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException;
    ScannerMetadata getScannerMetadataByLabel(String label, UserEntity user) throws SparqlException, SQLException;
    
    String addSlideMetadata (SlideMetadata metadata, UserEntity user) throws SparqlException, SQLException;
    List<SlideMetadata> getSlideMetadataByUser (UserEntity user) throws SparqlException, SQLException;
    List<SlideMetadata> getSlideMetadataByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<SlideMetadata> getSlideMetadataByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    List<SlideMetadata> getSlideMetadataByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue, Boolean loadAll)
            throws SparqlException, SQLException;
    SlideMetadata getSlideMetadataFromURI(String uri, UserEntity user) throws SparqlException, SQLException;
    SlideMetadata getSlideMetadataFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException;
    SlideMetadata getSlideMetadataByLabel(String label, UserEntity user) throws SparqlException, SQLException;
    
    String addImageAnalysisSoftware (ImageAnalysisSoftware metadata, UserEntity user) throws SparqlException, SQLException;
    List<ImageAnalysisSoftware> getImageAnalysisSoftwareByUser (UserEntity user) throws SparqlException, SQLException;
    List<ImageAnalysisSoftware> getImageAnalysisSoftwareByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<ImageAnalysisSoftware> getImageAnalysisSoftwareByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    List<ImageAnalysisSoftware> getImageAnalysisSoftwareByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue, Boolean loadAll)
            throws SparqlException, SQLException;
    int getImageAnalysisSoftwareCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException;
    ImageAnalysisSoftware getImageAnalysisSoftwareFromURI(String uri, UserEntity user) throws SparqlException, SQLException;
    ImageAnalysisSoftware getImageAnalysisSoftwareFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException;
    ImageAnalysisSoftware getImageAnalysisSoftwarByLabel(String label, UserEntity user) throws SparqlException, SQLException;
    
    String addDataProcessingSoftware (DataProcessingSoftware metadata, UserEntity user) throws SparqlException, SQLException;
    List<DataProcessingSoftware> getDataProcessingSoftwareByUser (UserEntity user) throws SparqlException, SQLException;
    List<DataProcessingSoftware> getDataProcessingSoftwareByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<DataProcessingSoftware> getDataProcessingSoftwareByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    List<DataProcessingSoftware> getDataProcessingSoftwareByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue, Boolean loadAll)
            throws SparqlException, SQLException;
    int getDataProcessingSoftwareCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException;
    DataProcessingSoftware getDataProcessingSoftwareFromURI(String uri, UserEntity user) throws SparqlException, SQLException;
    DataProcessingSoftware getDataProcessingSoftwareFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException;
    DataProcessingSoftware getDataProcessingSoftwareByLabel(String label, UserEntity user) throws SparqlException, SQLException;
    
    String addAssayMetadata(AssayMetadata metadata, UserEntity user) throws SparqlException, SQLException;
    List<AssayMetadata> getAssayMetadataByUser (UserEntity user) throws SparqlException, SQLException;
    List<AssayMetadata> getAssayMetadataByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
    List<AssayMetadata> getAssayMetadataByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    List<AssayMetadata> getAssayMetadataByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue, Boolean loadAll)
            throws SparqlException, SQLException;
    int getAssayMetadataCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException;
    AssayMetadata getAssayMetadataFromURI(String uri, UserEntity user) throws SparqlException, SQLException;
    AssayMetadata getAssayMetadataFromURI(String uri, Boolean loadAll, UserEntity user) throws SparqlException, SQLException;
    AssayMetadata getAssayMetadataByLabel(String label, UserEntity user) throws SparqlException, SQLException;
    
    MetadataCategory getMetadataByLabel(String label, String typePredicate, UserEntity user) throws SparqlException, SQLException;
    void updateMetadata(MetadataCategory metadata, UserEntity user) throws SparqlException, SQLException;
    void deleteMetadata (String metadataId, UserEntity user) throws SparqlException, SQLException;
    void updateMetadataMirage(MetadataCategory metadata, UserEntity user) throws SQLException, SparqlException;

    MetadataCategory getMetadataCategoryFromURI(String uri, String typePredicate, Boolean loadAll, UserEntity user)
            throws SparqlException, SQLException;

    String addMetadataCategory(MetadataCategory metadata, MetadataTemplateType metadataType, String templatePredicate,
            String typePredicate, String prefix, UserEntity user) throws SparqlException, SQLException;

    void deleteMetadataById(String metadataId, String graph) throws SparqlException;

    SpotMetadata getSpotMetadataValueFromURI(String uri, UserEntity user) throws SparqlException, SQLException;

    SpotMetadata getSpotMetadataValueFromURI(String uri, Boolean loadAll, UserEntity user)
            throws SparqlException, SQLException;

    String addSpotMetadataValue(SpotMetadata metadata, UserEntity user) throws SparqlException, SQLException;
    

}
