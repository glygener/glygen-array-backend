package org.glygen.array.persistence.rdf.metadata;

import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.template.DescriptionTemplate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@XmlRootElement
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
      @JsonSubTypes.Type(value = Descriptor.class),
      @JsonSubTypes.Type(value = DescriptorGroup.class)})
public abstract class Description implements Comparable<Description>{
    String uri;
    String id;
    String name;
    DescriptionTemplate key;
    Integer order = -1;
    Boolean notRecorded = false;
    Boolean notApplicable = false;
    
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public abstract boolean isGroup();
    
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
     * @return the key
     */
    public DescriptionTemplate getKey() {
        return key;
    }
    /**
     * @param key the key to set
     */
    public void setKey(DescriptionTemplate key) {
        this.key = key;
    }
    /**
     * @return the order
     */
    public Integer getOrder() {
        return order;
    }
    /**
     * @param order the order to set
     */
    public void setOrder(Integer order) {
        this.order = order;
    }
    
    @Override
    public int compareTo(Description o) {
        return this.order.compareTo(o.order);
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
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
     * @return the notApplicable
     */
    public Boolean getNotApplicable() {
        return notApplicable;
    }

    /**
     * @param notApplicable the notApplicable to set
     */
    public void setNotApplicable(Boolean notApplicable) {
        this.notApplicable = notApplicable;
    }    
}
