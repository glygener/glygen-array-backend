package org.glygen.array.persistence.rdf.metadata;

import java.util.Date;

import javax.validation.constraints.Size;

import org.glygen.array.config.ValidationConstants;
import org.glygen.array.persistence.rdf.Creator;

public class Sample extends MetadataCategory {
    String name;
    String description;
    Creator user;
    Date dateModified;
    Date dateCreated;
    Date dateAddedToLibrary;
    boolean isPublic = false;
    
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
}
