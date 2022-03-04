package org.glygen.array.persistence.rdf.data;

import java.util.List;

import org.glygen.array.persistence.rdf.Feature;

public class FilterExclusionInfo { 
    List<Feature> features;
    FilterExclusionReasonType reason;
    String otherReason;
    
    /**
     * @return the reason
     */
    public FilterExclusionReasonType getReason() {
        return reason;
    }
    /**
     * @param reason the reason to set
     */
    public void setReason(FilterExclusionReasonType reason) {
        this.reason = reason;
    }
    /**
     * @return the otherReason
     */
    public String getOtherReason() {
        return otherReason;
    }
    /**
     * @param otherReason the otherReason to set
     */
    public void setOtherReason(String otherReason) {
        this.otherReason = otherReason;
    }
    /**
     * @return the features
     */
    public List<Feature> getFeatures() {
        return features;
    }
    /**
     * @param features the features to set
     */
    public void setFeatures(List<Feature> features) {
        this.features = features;
    }
}
