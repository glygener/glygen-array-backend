package org.glygen.array.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotEmpty;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Entity(name="glycansearchresult")
@XmlRootElement (name="glycansearchresult")
@JsonSerialize
public class GlycanSearchResultEntity {
    String sequence;
    String idList;
    
    public GlycanSearchResultEntity() {
    }
    
    public GlycanSearchResultEntity(String n, String v) {
        this.sequence = n;
        this.idList = v;
    }
    
    /**
     * @return the name
     */
    @Column(name="sequence", nullable=false, unique=true, length=4000)
    @NotEmpty
    @Id
    public String getSequence() {
        return sequence;
    }
    /**
     * @param sequence the sequence to set
     */
    public void setSequence(String sequence) {
        this.sequence = sequence;
    }
    /**
     * @return the value
     */
    @Column(name="idlist", nullable=true)
    public String getIdList() {
        return idList;
    }
    /**
     * @param idList the list of ids to set
     */
    public void setIdList(String idList) {
        this.idList = idList;
    }

}
