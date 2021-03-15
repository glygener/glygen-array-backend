package org.glygen.array.view;

public class StatisticsView {
    
    String apiVersion = "1.0.0";
    String portalVersion = "1.0.0";
    Long userCount=0L;
    Long datasetCount=0L;
    Long sampleCount=0L;
    Long slideCount=0L;
    /**
     * @return the apiVersion
     */
    public String getApiVersion() {
        return apiVersion;
    }
    /**
     * @param apiVersion the apiVersion to set
     */
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }
    /**
     * @return the portalVersion
     */
    public String getPortalVersion() {
        return portalVersion;
    }
    /**
     * @param portalVersion the portalVersion to set
     */
    public void setPortalVersion(String portalVersion) {
        this.portalVersion = portalVersion;
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
    
}
