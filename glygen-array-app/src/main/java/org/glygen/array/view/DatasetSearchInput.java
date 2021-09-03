package org.glygen.array.view;

public class DatasetSearchInput {
    
    String datasetName;
    String pmid;
    String printedSlideName;
    String owner;
    String institution;
    String groupName;
    Boolean coOwner;
    
    /**
     * @return the datasetName
     */
    public String getDatasetName() {
        return datasetName;
    }
    /**
     * @param datasetName the datasetName to set
     */
    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }
    /**
     * @return the pmid
     */
    public String getPmid() {
        return pmid;
    }
    /**
     * @param pmid the pmid to set
     */
    public void setPmid(String pmid) {
        this.pmid = pmid;
    }
    /**
     * @return the printedSlideName
     */
    public String getPrintedSlideName() {
        return printedSlideName;
    }
    /**
     * @param printedSlideName the printedSlideName to set
     */
    public void setPrintedSlideName(String printedSlideName) {
        this.printedSlideName = printedSlideName;
    }
    
    

}
