package org.glygen.array.persistence.rdf.metadata;

import java.util.Date;
import java.util.List;

import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.config.ValidationConstants;
import org.glygen.array.persistence.rdf.Creator;
import org.glygen.array.persistence.rdf.template.MetadataTemplateType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;


@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, 
        include = JsonTypeInfo.As.PROPERTY, 
        property = "type",
        visible = true)
    @JsonSubTypes({ 
        @Type(value = Sample.class, name = "SAMPLE"), 
        @Type(value = Printer.class, name = "PRINTER"),
        @Type(value = ScannerMetadata.class, name = "SCANNER"),
        @Type(value = SlideMetadata.class, name = "SLIDE"),
        @Type(value = DataProcessingSoftware.class, name = "DATAPROCESSINGSOFTWARE"),
        @Type(value = ImageAnalysisSoftware.class, name = "IMAGEANALYSISSOFTWARE"),
        @Type(value = AssayMetadata.class, name = "ASSAY"),
        @Type(value = FeatureMetadata.class, name = "FEATURE"),
        @Type(value = SpotMetadata.class, name = "SPOT"),
        @Type(value = PrintRun.class, name = "PRINTRUN")
    })
@XmlRootElement
public class MetadataCategory {
    String id;
    String uri;
    String template;
    String templateType;
    
    MetadataTemplateType type;

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
    @Size(max=ValidationConstants.NAME_LIMIT, message="Name cannot exceed " + ValidationConstants.NAME_LIMIT + " characters")
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
    @Size(max=ValidationConstants.DESCRIPTION_LIMIT, message="DescriptionTemplate cannot exceed " + ValidationConstants.DESCRIPTION_LIMIT + " characters")
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
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetadataCategory)
            return this.type != null && this.type.equals(((MetadataCategory)obj).type) 
                && this.id != null && this.id.equals(((MetadataCategory) obj).id);
        return super.equals(obj);
    }
    
    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }
        return super.hashCode();
    }
}
