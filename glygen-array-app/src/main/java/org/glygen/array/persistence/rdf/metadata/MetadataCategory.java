package org.glygen.array.persistence.rdf.metadata;

import java.util.List;

public class MetadataCategory {
    String id;
    String uri;
    String template;

    List<Descriptor> descriptors;
    List<DescriptorGroup> descriptorGroups;
    /**
     * @return the descriptors
     */
    public List<Descriptor> getDescriptors() {
        return descriptors;
    }
    /**
     * @param descriptors the descriptors to set
     */
    public void setDescriptors(List<Descriptor> descriptors) {
        this.descriptors = descriptors;
    }
    /**
     * @return the descriptorGroups
     */
    public List<DescriptorGroup> getDescriptorGroups() {
        return descriptorGroups;
    }
    /**
     * @param descriptorGroups the descriptorGroups to set
     */
    public void setDescriptorGroups(List<DescriptorGroup> descriptorGroups) {
        this.descriptorGroups = descriptorGroups;
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
     * @return the template
     */
    public String getTemplate() {
        return template;
    }
    /**
     * @param template the template to set
     */
    public void setTemplate(String template) {
        this.template = template;
    }
}
