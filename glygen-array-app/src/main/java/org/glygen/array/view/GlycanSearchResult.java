package org.glygen.array.view;

import java.util.Date;

import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.MassOnlyGlycan;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;

public class GlycanSearchResult {
    Glycan glycan;
    Integer datasetCount;
    /**
     * @return the glycan
     */
    public Glycan getGlycan() {
        return glycan;
    }
    /**
     * @param glycan the glycan to set
     */
    public void setGlycan(Glycan glycan) {
        this.glycan = glycan;
    }
    /**
     * @return the datasetCount
     */
    public Integer getDatasetCount() {
        return datasetCount;
    }
    /**
     * @param datasetCount the datasetCount to set
     */
    public void setDatasetCount(Integer datasetCount) {
        this.datasetCount = datasetCount;
    }
    
    public String getId() {
        return glycan.getId();
    }
    
    public String getName() {
        return glycan.getName();
    }
    
    public String getDescription () {
        return glycan.getDescription();
    }
    
    public String getGlytoucanId() {
        if (glycan instanceof SequenceDefinedGlycan) {
            return ((SequenceDefinedGlycan) glycan).getGlytoucanId();
        }
        return null;
    }
    
    public Double getMass() {
        if (glycan instanceof MassOnlyGlycan)
            return ((MassOnlyGlycan) glycan).getMass();
        return null;
    }
    
    public Date getDateModified () {
        return glycan.getDateModified();
    }
    
    public Date getDateCreated () {
        return glycan.getDateCreated();
    }

}
