package org.glygen.array.client;

import org.glygen.array.client.model.data.ArrayDataset;
import org.glygen.array.client.model.data.FileWrapper;
import org.glygen.array.client.model.data.PrintedSlide;
import org.glygen.array.client.model.data.RawData;
import org.glygen.array.client.model.data.StatisticalMethod;
import org.glygen.array.client.model.metadata.AssayMetadata;
import org.glygen.array.client.model.metadata.DataProcessingSoftware;
import org.glygen.array.client.model.metadata.ImageAnalysisSoftware;
import org.glygen.array.client.model.metadata.Printer;
import org.glygen.array.client.model.metadata.Sample;
import org.glygen.array.client.model.metadata.ScannerMetadata;
import org.glygen.array.client.model.metadata.SlideMetadata;

public interface DatasetRestClient {
    /**
     * sets the user name of the user
     * @param username name of the user
     */
    void setUsername(String username);
    
    /**
     * sets the password to be used for the user
     * @param password password of the user
     */
    void setPassword(String password);
    
    /**
     * sets the url for the web services, e.g https://glygen.ccrc.uga.edu/ggarray/api/
     * @param url url to be used to access the web services (with the trailing /)
     */
    void setURL (String url);
    
    /**
     * adds the given array dataset to the repository
     * 
     * @param datasetName name of the experiment/dataset
     * @param sample Sample used in the experiment, Sample should already be in the repository
     * Either id or name should be specified for the sample, the other fields might be left empty/null
     * @return the id of the newly added array dataset
     */
    String addDataset(String datasetName, Sample sample);
    
    /**
     * add the processed data extracted from the file to the dataset
     * 
     * @param file FileWrapper of the uploaded processed data file that contains the assigned identifier, original name and file format if applicable
     * @param method statistical method used for processing the data (eliminate/average), label of the StatisticalMethod
     * @param datasetId id of the dataset to add to (dataset should already be in the repository)
     * @param metadataId id of the data processing software metadata used (should already be in the repository)
     * @return the id of the newly added processed data
     */
    String addProcessedDataFromExcel(FileWrapper file, String statisticalMethod, String datasetId,
            String metadataId);
    
    /**
     * add the given RawData to the dataset
     * 
     * @param rawData RawData to be added. RawData should have a slide
     * and slide should have an existing printedSlide (specified by name or uri or id)
     * it should have an Image (specified with filename (already uploaded) and an existing Scanner metadata (by name, uri or id))
     * it should have a filename (already uploaded), file format and a power level
     * and it should have an existing ImageAnalysisSoftwareMetadata (specified by name, id or uri"
     * @param datasetId id of the dataset to add to (dataset should already be in the repository)
     * @return the id of the newly added raw data
     */
    String addRawdataToDataset (RawData rawData, String datasetId);
    
    /**
     * add the given sample to the repository
     * @param sample Sample with all the required metadata
     * @return the id of the newly added sample
     */
    String addSample(Sample sample);
    
    /**
     * add the given sample to the repository
     * @param sample Sample 
     * @param validate true if sample metadata should be validated or false to bypass validation (should only be done for legacy data)
     * @return the id of the newly added sample
     */
    String addSample(Sample sample, boolean validate);
    
    /**
     * add the given printer to the repository
     * @param printer Printer with all the required metadata
     * @return the id of the newly added printer
     */
    String addPrinterMetadata (Printer printer);
    
    /**
     * add the given printer to the repository
     * @param printer Printer with all the required metadata
     * @param validate true if printer metadata should be validated or false to bypass validation (should only be done for legacy data)
     * @return the id of the newly added printer
     */
    String addPrinterMetadata (Printer printer, boolean validate);
    
    /**
     * 
     * @param scanner
     * @return
     */
    String addScannerMetadata (ScannerMetadata scanner);
    
    /**
     * 
     * @param scanner
     * @param validate
     * @return
     */
    String addScannerMetadata (ScannerMetadata scanner, boolean validate);
    
    String addSlideMetadata (SlideMetadata slideMetadata);
    String addSlideMetadata (SlideMetadata slideMetadata, boolean validate);
    
    String addImageAnalysisMetadata (ImageAnalysisSoftware metadata);
    String addImageAnalysisMetadata (ImageAnalysisSoftware metadata, boolean validate);
    
    String addDataProcessingMetadata (DataProcessingSoftware metadata); 
    String addDataProcessingMetadata (DataProcessingSoftware metadata, boolean validate);
    
    String adAssayMetadata (AssayMetadata metadata); 
    String addAssayMetadata (AssayMetadata metadata, boolean validate);

    /**
     * add the given printed slide to the repository
     * @param printedSlide printed slide with name, description, 
     * name/id/uri for an already created slide metadata, name/id/uri for an already created printer metadata, 
     * name/id/uri for an already created slide layout should all be specified (only one of name, id or uri is sufficient)
     * @return the id of the newly added printed slide
     */
    String addPrintedSlide (PrintedSlide printedSlide);
    
    /**
     * return the id of the data processing software metadata with the given name, if exists
     * @param name name of the metadata
     * @return the id of the metadata or null if it does not exists
     */
    String getDataProcessingMetadataByLabel (String name);
    String getAssayMetadataByLabel (String name);
    String getImageAnalysisMetadataByLabel (String name);
    String getSlideMetadataByLabel (String name);
    String getScannerByLabel (String name);
    String getPrinterByLabel (String name);
    String getSampleByLabel (String name);
    
    ArrayDataset getDatasetByLabel (String name);
    
}
