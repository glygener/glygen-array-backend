package org.glygen.array.persistence.rdf.data;

import java.util.List;

import org.glygen.array.persistence.rdf.Feature;

public class Intensity {
    
    // integrates multiple measurements
    List<Measurement> measurements;
    Feature feature;
    
    Double rfu;
    Double stDev;
    Double percentCV;
    /**
     * @return the measurements
     */
    public List<Measurement> getMeasurements() {
        return measurements;
    }
    /**
     * @param measurements the measurements to set
     */
    public void setMeasurements(List<Measurement> measurements) {
        this.measurements = measurements;
    }
    /**
     * @return the feature
     */
    public Feature getFeature() {
        return feature;
    }
    /**
     * @param feature the feature to set
     */
    public void setFeature(Feature feature) {
        this.feature = feature;
    }
    /**
     * @return the rfu
     */
    public Double getRfu() {
        return rfu;
    }
    /**
     * @param rfu the rfu to set
     */
    public void setRfu(Double rfu) {
        this.rfu = rfu;
    }
    /**
     * @return the stDev
     */
    public Double getStDev() {
        return stDev;
    }
    /**
     * @param stDev the stDev to set
     */
    public void setStDev(Double stDev) {
        this.stDev = stDev;
    }
    /**
     * @return the percentCV
     */
    public Double getPercentCV() {
        return percentCV;
    }
    /**
     * @param percentCV the percentCV to set
     */
    public void setPercentCV(Double percentCV) {
        this.percentCV = percentCV;
    }
    
}
