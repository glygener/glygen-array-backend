package org.glygen.array.persistence.rdf.data;

import java.util.Date;
import java.util.List;

public class ChangeLog {
    
    Date date;
    ChangeType changeType;
    String user;
    String summary;
    List<String> changedFields;
    
    /**
     * @return the date
     */
    public Date getDate() {
        return date;
    }
    /**
     * @param date the date to set
     */
    public void setDate(Date date) {
        this.date = date;
    }
    /**
     * @return the changeType
     */
    public ChangeType getChangeType() {
        return changeType;
    }
    /**
     * @param changeType the changeType to set
     */
    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }
    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }
    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }
    /**
     * @return the summary
     */
    public String getSummary() {
        return summary;
    }
    /**
     * @param summary the summary to set
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }
    /**
     * @return the changedFields
     */
    public List<String> getChangedFields() {
        return changedFields;
    }
    /**
     * @param changedFields the changedFields to set
     */
    public void setChangedFields(List<String> changedFields) {
        this.changedFields = changedFields;
    }
    
    

}
