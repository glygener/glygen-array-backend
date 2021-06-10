package org.glygen.array.persistence.rdf.template;

import java.util.List;

public class MetadataTemplate {
    
    String id;
    String uri;
    String name;
    String description;
    MetadataTemplateType type;
    List<DescriptionTemplate> descriptors;
    
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
     * @return the descriptors
     */
    public List<DescriptionTemplate> getDescriptors() {
        return descriptors;
    }
    /**
     * @param descriptors the descriptors to set
     */
    public void setDescriptors(List<DescriptionTemplate> descriptors) {
        this.descriptors = descriptors;
    }
    /**
     * @return the type
     */
    public MetadataTemplateType getType() {
        return type;
    }
    /**
     * @param type the type to set
     */
    public void setType(MetadataTemplateType type) {
        this.type = type;
    }

}
