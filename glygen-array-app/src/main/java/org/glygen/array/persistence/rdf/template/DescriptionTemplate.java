package org.glygen.array.persistence.rdf.template;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
      @JsonSubTypes.Type(value = DescriptorTemplate.class),
      @JsonSubTypes.Type(value = DescriptorGroupTemplate.class)})
public abstract class DescriptionTemplate {
    
    String uri;
    String id;
    String name;
    String description;
    Boolean mandatory;
    Integer maxOccurrence;
    Integer order;
    String example;
    String wikiLink;
    Integer mandateGroup = null;
    Boolean xorMandate = true;
    Boolean mirage = false;
    
    public abstract boolean isGroup();

    /**
     * @return the mandatory
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * @param mandatory the mandatory to set
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * @return the maxOccurrence
     */
    public Integer getMaxOccurrence() {
        return maxOccurrence;
    }

    /**
     * @param maxOccurrence the maxOccurrence to set
     */
    public void setMaxOccurrence(Integer maxOccurrence) {
        this.maxOccurrence = maxOccurrence;
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
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the example
     */
    public String getExample() {
        return example;
    }

    /**
     * @param example the example to set
     */
    public void setExample(String example) {
        this.example = example;
    }

    /**
     * @return the wikiLink
     */
    public String getWikiLink() {
        return wikiLink;
    }

    /**
     * @param wikiLink the wikiLink to set
     */
    public void setWikiLink(String wikiLink) {
        this.wikiLink = wikiLink;
    }

    /**
     * @return the mandateGroup
     */
    public Integer getMandateGroup() {
        return mandateGroup;
    }

    /**
     * @param mandateGroup the mandateGroup to set
     */
    public void setMandateGroup(Integer mandateGroup) {
        this.mandateGroup = mandateGroup;
    }

    /**
     * @return the mirage
     */
    public Boolean isMirage() {
        return mirage;
    }

    /**
     * @param mirage the mirage to set
     */
    public void setMirage(Boolean mirage) {
        this.mirage = mirage;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof DescriptionTemplate) { 
            if (this.id != null && ((DescriptionTemplate) obj).getId() != null) {
                return this.id.equals(((DescriptionTemplate) obj).getId());
            } else if (this.uri != null && ((DescriptionTemplate) obj).getUri() != null) {
                return this.uri.equals(((DescriptionTemplate) obj).getUri());
            }
        } 
        return super.equals(obj);
    }

    /**
     * @return the xorMandate
     */
    public Boolean getXorMandate() {
        return xorMandate;
    }

    /**
     * @param xorMandate the xorMandate to set
     */
    public void setXorMandate(Boolean xorMandate) {
        this.xorMandate = xorMandate;
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
}
