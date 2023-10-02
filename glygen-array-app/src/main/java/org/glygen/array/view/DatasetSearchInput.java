package org.glygen.array.view;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DatasetSearchInput {
    
    String datasetName;
    String pmid;
    String printedSlideName;
    String username;
    String lastName;
    String firstName;
    String institution;
    String groupName;
    Boolean coOwner;
    String keyword;
    
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
    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }
    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }
    /**
     * @return the lastName
     */
    public String getLastName() {
        return lastName;
    }
    /**
     * @param lastName the lastName to set
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    /**
     * @return the institution
     */
    public String getInstitution() {
        return institution;
    }
    /**
     * @param institution the institution to set
     */
    public void setInstitution(String institution) {
        this.institution = institution;
    }
    /**
     * @return the groupName
     */
    public String getGroupName() {
        return groupName;
    }
    /**
     * @param groupName the groupName to set
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    /**
     * @return the coOwner
     */
    public Boolean getCoOwner() {
        return coOwner;
    }
    /**
     * @param coOwner the coOwner to set
     */
    public void setCoOwner(Boolean coOwner) {
        this.coOwner = coOwner;
    }
    /**
     * @return the firstName
     */
    public String getFirstName() {
        return firstName;
    }
    /**
     * @param firstName the firstName to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    /**
     * @return the keyword
     */
    public String getKeyword() {
        return keyword;
    }
    /**
     * @param keyword the keyword to set
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    
    

}
