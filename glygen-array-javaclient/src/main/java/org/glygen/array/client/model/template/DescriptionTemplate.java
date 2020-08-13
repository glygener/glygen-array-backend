package org.glygen.array.client.model.template;

public abstract class DescriptionTemplate {
    
    String uri;
    String id;
    String name;
    String description;
    boolean mandatory;
    Integer maxOccurrence;
    String example;
    String wikiLink;
    
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

}
