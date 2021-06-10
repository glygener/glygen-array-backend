package org.glygen.array.client.model.data;

import java.util.List;

import org.glygen.array.client.model.Spot;
import org.grits.toolbox.glycanarray.library.om.layout.LevelUnit;

public class Intensity {
    
    String uri;
    String id;
    
    // integrates multiple measurements
    List<Measurement> measurements;
    Spot spot;
    
    Double rfu;
    Double stDev;
    Double percentCV;
    
    LevelUnit concentrationLevel;
    
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
    
    public void setConcentrationLevel(LevelUnit concentrationLevel) {
        this.concentrationLevel = concentrationLevel;
    }
    
    public LevelUnit getConcentrationLevel() {
        return concentrationLevel;
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
     * @return the spot
     */
    public Spot getSpot() {
        return spot;
    }
    /**
     * @param spot the spot to set
     */
    public void setSpot(Spot spot) {
        this.spot = spot;
    }
}
