package org.glygen.array.client.model;

public class ProcessedResultConfiguration {
    
    Integer sheetNumber = 1;
    Integer featureColumnId;
    Integer rfuColumnId;
    Integer stDevColumnId;
    Integer cvColumnId = -1;
    Integer startRow = 1;
    Integer featureNameColumnId;
    Integer concentrationLevelColumnId;
    
    String resultFileType = "CFG";
    
    
    /**
     * @return the sheetNumber
     */
    public Integer getSheetNumber() {
        return sheetNumber;
    }
    /**
     * @param sheetNumber the sheetNumber to set
     */
    public void setSheetNumber(Integer sheetNumber) {
        this.sheetNumber = sheetNumber;
    }
    /**
     * @return the featureColumnId
     */
    public Integer getFeatureColumnId() {
        return featureColumnId;
    }
    /**
     * @param featureColumnId the featureColumnId to set
     */
    public void setFeatureColumnId(Integer featureColumnId) {
        this.featureColumnId = featureColumnId;
    }
    /**
     * @return the rfuColumnId
     */
    public Integer getRfuColumnId() {
        return rfuColumnId;
    }
    /**
     * @param rfuColumnId the rfuColumnId to set
     */
    public void setRfuColumnId(Integer rfuColumnId) {
        this.rfuColumnId = rfuColumnId;
    }
    /**
     * @return the stDevColumnId
     */
    public Integer getStDevColumnId() {
        return stDevColumnId;
    }
    /**
     * @param stDevColumnId the stDevColumnId to set
     */
    public void setStDevColumnId(Integer stDevColumnId) {
        this.stDevColumnId = stDevColumnId;
    }
    /**
     * @return the cvColumnId
     */
    public Integer getCvColumnId() {
        return cvColumnId;
    }
    /**
     * @param cvColumnId the cvColumnId to set
     */
    public void setCvColumnId(Integer cvColumnId) {
        this.cvColumnId = cvColumnId;
    }
    /**
     * @return the startRow
     */
    public Integer getStartRow() {
        return startRow;
    }
    /**
     * @param startRow the startRow to set
     */
    public void setStartRow(Integer startRow) {
        this.startRow = startRow;
    }
    /**
     * @return the featureNameColumnId
     */
    public Integer getFeatureNameColumnId() {
        return featureNameColumnId;
    }
    /**
     * @param featureNameColumnId the featureNameColumnId to set
     */
    public void setFeatureNameColumnId(Integer featureNameColumnId) {
        this.featureNameColumnId = featureNameColumnId;
    }
    /**
     * @return the resultFileType
     */
    public String getResultFileType() {
        return resultFileType;
    }
    /**
     * @param resultFileType the resultFileType to set
     */
    public void setResultFileType(String resultFileType) {
        this.resultFileType = resultFileType;
    }
    
    public void setConcentrationLevelColumnId(Integer concentrationLevelColumnId) {
        this.concentrationLevelColumnId = concentrationLevelColumnId;
    }
    
    public Integer getConcentrationLevelColumnId() {
        return concentrationLevelColumnId;
    }
}
