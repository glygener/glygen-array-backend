package org.glygen.array.persistence.rdf;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, 
        include = JsonTypeInfo.As.PROPERTY, 
        property = "type")
    @JsonSubTypes({ 
        @Type(value = CommercialSource.class, name = "COMMERCIAL"), 
        @Type(value = NonCommercialSource.class, name = "NONCOMMERCIAL"),
        @Type(value = Source.class, name = "NOTRECORDED"),
    })
public class Source {
    String batchId;
    Boolean notRecorded = true;
    SourceType type = SourceType.NOTRECORDED;
    /**
     * @return the batchId
     */
    public String getBatchId() {
        return batchId;
    }
    /**
     * @param batchId the batchId to set
     */
    public void setBatchId(String batch_id) {
        this.batchId = batch_id;
    }
    /**
     * @return the notRecorded
     */
    public Boolean getNotRecorded() {
        return notRecorded;
    }
    /**
     * @param notRecorded the notRecorded to set
     */
    public void setNotRecorded(Boolean notRecorded) {
        this.notRecorded = notRecorded;
    }
    /**
     * @return the type
     */
    public SourceType getType() {
        return type;
    }
    /**
     * @param type the type to set
     */
    public void setType(SourceType type) {
        this.type = type;
    }
}
