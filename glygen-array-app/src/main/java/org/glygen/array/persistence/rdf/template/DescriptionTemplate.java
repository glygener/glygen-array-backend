package org.glygen.array.persistence.rdf.template;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@XmlRootElement
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
      @JsonSubTypes.Type(value = DescriptorTemplate.class),
      @JsonSubTypes.Type(value = DescriptorGroupTemplate.class)})
public abstract class DescriptionTemplate implements Comparable<DescriptionTemplate>{
    
    String uri;
    String id;
    String name;
    String description;
    Boolean mandatory = false;
    Integer maxOccurrence;
    Integer order;
    String example;
    String wikiLink;
    MandateGroup mandateGroup;
    Boolean mirage = false;
    Boolean allowNotRecorded = false;
    Boolean allowNotApplicable = false;
    Boolean review = false;
    String displayLabel;
    
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
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
    public int compareTo(DescriptionTemplate o) {
        return this.order.compareTo(o.order);
    }

    /**
     * @param mandateGroup the mandateGroup to set
     */
    public void setMandateGroup(MandateGroup mandateGroup) {
        this.mandateGroup = mandateGroup;
    }

    /**
     * @return the mandateGroup
     */
    public MandateGroup getMandateGroup() {
        return mandateGroup;
    }

    /**
     * @return the allowNotRecorded
     */
    public Boolean getAllowNotRecorded() {
        return allowNotRecorded;
    }

    /**
     * @param allowNotRecorded the allowNotRecorded to set
     */
    public void setAllowNotRecorded(Boolean allowNotRecorded) {
        this.allowNotRecorded = allowNotRecorded;
    }

    /**
     * @return the allowNotApplicable
     */
    public Boolean getAllowNotApplicable() {
        return allowNotApplicable;
    }

    /**
     * @param allowNotApplicable the allowNotApplicable to set
     */
    public void setAllowNotApplicable(Boolean allowNotApplicable) {
        this.allowNotApplicable = allowNotApplicable;
    }

    /**
     * @return the review
     */
    public Boolean getReview() {
        return review;
    }

    /**
     * @param review the review to set
     */
    public void setReview(Boolean review) {
        this.review = review;
    }

    /**
     * @return the displayLabel
     */
    public String getDisplayLabel() {
        return displayLabel;
    }

    /**
     * @param displayLabel the displayLabel to set
     */
    public void setDisplayLabel(String displayLabel) {
        this.displayLabel = displayLabel;
    }
}
