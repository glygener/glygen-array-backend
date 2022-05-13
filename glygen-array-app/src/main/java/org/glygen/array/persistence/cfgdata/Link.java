package org.glygen.array.persistence.cfgdata;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Entity
@Table (name="link", schema="cfg_5_2")
@XmlRootElement (name="link")
@JsonSerialize
public class Link {
    
    @Id
    Long id;
    
    @Column
    String linkText;
    
    @Column
    String href;
    
    @Column
    Long parentCell;
    
    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }
    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }
    /**
     * @return the linkText
     */
    public String getLinkText() {
        return linkText;
    }
    /**
     * @param linkText the linkText to set
     */
    public void setLinkText(String linkText) {
        this.linkText = linkText;
    }
    /**
     * @return the href
     */
    public String getHref() {
        return href;
    }
    /**
     * @param href the href to set
     */
    public void setHref(String href) {
        this.href = href;
    }
    /**
     * @return the parentCell
     */
    public Long getParentCell() {
        return parentCell;
    }
    /**
     * @param parentCell the parentCell to set
     */
    public void setParentCell(Long parentCell) {
        this.parentCell = parentCell;
    }
    

}
