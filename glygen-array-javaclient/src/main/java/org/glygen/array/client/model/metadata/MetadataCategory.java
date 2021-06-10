package org.glygen.array.client.model.metadata;

import java.util.Date;
import java.util.List;

import org.glygen.array.client.model.Creator;


public class MetadataCategory {
    String id;
    String uri;
    String template;
    String templateType;

    List<Descriptor> descriptors;
    List<DescriptorGroup> descriptorGroups;
    
    String name;
    String description;
    Creator user;
    Date dateModified;
    Date dateCreated;
    Date dateAddedToLibrary;
    boolean isPublic = false;
    Boolean isMirage = false;
    Boolean inUse = false;
    
    public MetadataCategory() {
    }
    
    public MetadataCategory (MetadataCategory metadata) {
        this.id = metadata.id;
        this.uri = metadata.uri;
        this.dateAddedToLibrary = metadata.dateAddedToLibrary;
        this.dateCreated = metadata.dateCreated;
        this.dateModified = metadata.dateModified;
        this.description = metadata.description;
        this.descriptorGroups = metadata.descriptorGroups;
        this.descriptors = metadata.descriptors;
        this.isPublic = metadata.isPublic;
        this.name = metadata.name;
        this.template = metadata.template;
        this.user = metadata.user;
        this.templateType = metadata.templateType;
        this.isMirage = metadata.isMirage;
    }
    
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
     * @return the user
     */
    public Creator getUser() {
        return user;
    }
    /**
     * @param user the user to set
     */
    public void setUser(Creator user) {
        this.user = user;
    }
    /**
     * @return the dateModified
     */
    public Date getDateModified() {
        return dateModified;
    }
    /**
     * @param dateModified the dateModified to set
     */
    public void setDateModified(Date dateModified) {
        this.dateModified = dateModified;
    }
    /**
     * @return the dateCreated
     */
    public Date getDateCreated() {
        return dateCreated;
    }
    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }
    /**
     * @return the dateAddedToLibrary
     */
    public Date getDateAddedToLibrary() {
        return dateAddedToLibrary;
    }
    /**
     * @param dateAddedToLibrary the dateAddedToLibrary to set
     */
    public void setDateAddedToLibrary(Date dateAddedToLibrary) {
        this.dateAddedToLibrary = dateAddedToLibrary;
    }
    /**
     * @return the isPublic
     */
    public boolean isPublic() {
        return isPublic;
    }
    /**
     * @param isPublic the isPublic to set
     */
    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }
    /**
     * @return the templateType
     */
    public String getTemplateType() {
        return templateType;
    }
    /**
     * @param templateType the templateType to set
     */
    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }
    /**
     * @return the isMirage
     */
    public Boolean getIsMirage() {
        return isMirage;
    }
    /**
     * @param isMirage the isMirage to set
     */
    public void setIsMirage(Boolean isMirage) {
        this.isMirage = isMirage;
    }
}
