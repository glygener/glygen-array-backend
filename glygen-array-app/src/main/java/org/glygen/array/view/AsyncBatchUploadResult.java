package org.glygen.array.view;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.data.FutureTask;

@XmlRootElement
public class AsyncBatchUploadResult extends FutureTask {
    
    String id;
    String uri;
    
    Date accessedDate = null;
    String type;
    String successMessage = null;
    
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
     * @return the accessedDate
     */
    public Date getAccessedDate() {
        return accessedDate;
    }
    /**
     * @param accessedDate the accessedDate to set
     */
    public void setAccessedDate(Date accessedDate) {
        this.accessedDate = accessedDate;
    }
    /**
     * @return the type
     */
    public String getType() {
        return type;
    }
    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }
    /**
     * @return the successMessage
     */
    public String getSuccessMessage() {
        return successMessage;
    }
    /**
     * @param successMessage the successMessage to set
     */
    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }
    

}
