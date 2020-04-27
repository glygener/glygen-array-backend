package org.glygen.array.persistence.rdf.data;

import java.util.List;

import org.glygen.array.persistence.rdf.metadata.DataProcessingSoftware;

public class ProcessedData {
    
    List<Intensity> intensity;
    DataProcessingSoftware metadata;
    
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
}
