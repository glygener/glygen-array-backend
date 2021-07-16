package org.glygen.array.view;

import java.util.ArrayList;
import java.util.List;

public class StatisticsView {
    
    List<Version> version;
    Long userCount=0L;
    Long datasetCount=0L;
    Long sampleCount=0L;
    Long slideCount=0L;
    Long glycanCount=0L;
    
    public void addVersion(Version v) {
        if (version == null)
            version = new ArrayList<Version>();
        version.add(v);
    }
    
    public List<Version> getVersion() {
        return version;
    }
    
    public void setVersion(List<Version> version) {
        this.version = version;
    }
    
    /**
     * @return the userCount
     */
    public Long getUserCount() {
        return userCount;
    }
    /**
     * @param userCount the userCount to set
     */
    public void setUserCount(Long userCount) {
        this.userCount = userCount;
    }
    /**
     * @return the datasetCount
     */
    public Long getDatasetCount() {
        return datasetCount;
    }
    /**
     * @param datasetCount the datasetCount to set
     */
    public void setDatasetCount(Long datasetCount) {
        this.datasetCount = datasetCount;
    }
    /**
     * @return the sampleCount
     */
    public Long getSampleCount() {
        return sampleCount;
    }
    /**
     * @param sampleCount the sampleCount to set
     */
    public void setSampleCount(Long sampleCount) {
        this.sampleCount = sampleCount;
    }
    /**
     * @return the slideCount
     */
    public Long getSlideCount() {
        return slideCount;
    }
    /**
     * @param slideCount the slideCount to set
     */
    public void setSlideCount(Long slideCount) {
        this.slideCount = slideCount;
    }
    /**
     * @return the glycanCount
     */
    public Long getGlycanCount() {
        return glycanCount;
    }
    /**
     * @param glycanCount the glycanCount to set
     */
    public void setGlycanCount(Long glycanCount) {
        this.glycanCount = glycanCount;
    }
    
}
