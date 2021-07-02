package org.glygen.array.view;

import org.glygen.array.persistence.rdf.Glycan;

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
    
    

}
