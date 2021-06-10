package org.glygen.array.client.model.data;

import java.util.List;

import org.glygen.array.client.model.metadata.DataProcessingSoftware;


public class ProcessedData {
    
    String id;
    String uri;
    
    List<Intensity> intensity;
    DataProcessingSoftware metadata;
    
    StatisticalMethod method;
    FileWrapper file;
    
    /**
     * @return the intensity
     */
    public List<Intensity> getIntensity() {
        return intensity;
    }
    /**
     * @param intensity the intensity to set
     */
    public void setIntensity(List<Intensity> intensity) {
        this.intensity = intensity;
    }
    /**
     * @return the metadata
     */
    public DataProcessingSoftware getMetadata() {
        return metadata;
    }
    /**
     * @param metadata the metadata to set
     */
    public void setMetadata(DataProcessingSoftware metadata) {
        this.metadata = metadata;
    }
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
     * @return the method
     */
    public StatisticalMethod getMethod() {
        return method;
    }
    /**
     * @param method the method to set
     */
    public void setMethod(StatisticalMethod method) {
        this.method = method;
    }
    /**
     * @return the file
     */
    public FileWrapper getFile() {
        return file;
    }
    /**
     * @param file the file to set
     */
    public void setFile(FileWrapper file) {
        this.file = file;
    }
}
