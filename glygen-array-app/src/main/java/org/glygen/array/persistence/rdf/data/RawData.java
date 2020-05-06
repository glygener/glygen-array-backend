package org.glygen.array.persistence.rdf.data;

import java.util.List;
import java.util.Map;

import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.metadata.ImageAnalysisSoftware;

public class RawData {
    
    String id;
    String uri;
    
    Map<Measurement, Spot> dataMap;
    ImageAnalysisSoftware metadata;
    List<Image> images;
    
    String filename;  // name of the raw data file in uploaded file folder or any other designated data folder

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @return the dataMap
     */
    public Map<Measurement, Spot> getDataMap() {
        return dataMap;
    }

    /**
     * @param dataMap the dataMap to set
     */
    public void setDataMap(Map<Measurement, Spot> dataMap) {
        this.dataMap = dataMap;
    }

    /**
     * @return the metadata
     */
    public ImageAnalysisSoftware getMetadata() {
        return metadata;
    }

    /**
     * @param metadata the metadata to set
     */
    public void setMetadata(ImageAnalysisSoftware metadata) {
        this.metadata = metadata;
    }

    /**
     * @return the images
     */
    public List<Image> getImages() {
        return images;
    }

    /**
     * @param images the images to set
     */
    public void setImages(List<Image> images) {
        this.images = images;
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

}
