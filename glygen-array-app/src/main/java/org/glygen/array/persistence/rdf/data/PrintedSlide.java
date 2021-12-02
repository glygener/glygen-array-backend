package org.glygen.array.persistence.rdf.data;

import java.util.Date;

import javax.validation.constraints.Size;

import org.glygen.array.config.ValidationConstants;
import org.glygen.array.persistence.rdf.Creator;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.metadata.PrintRun;
import org.glygen.array.persistence.rdf.metadata.Printer;
import org.glygen.array.persistence.rdf.metadata.SlideMetadata;

public class PrintedSlide {
    String id;
    String uri;
    String name;
    String description;
    SlideLayout layout;
    SlideMetadata metadata;
    Printer printer;
    PrintRun printRun;
    
    boolean isPublic = false;
    Creator user;
    
    Date dateModified;
    Date dateCreated;
    Date dateAddedToLibrary;
    
    Boolean inUse = false;
    
    /**
     * @return the layout
     */
    public SlideLayout getLayout() {
        return layout;
    }
    /**
     * @param layout the layout to set
     */
    public void setLayout(SlideLayout layout) {
        this.layout = layout;
    }
    /**
     * @return the metadata
     */
    public SlideMetadata getMetadata() {
        return metadata;
    }
    /**
     * @param metadata the metadata to set
     */
    public void setMetadata(SlideMetadata metadata) {
        this.metadata = metadata;
    }
    /**
     * @return the printer
     */
    public Printer getPrinter() {
        return printer;
    }
    /**
     * @param printer the printer to set
     */
    public void setPrinter(Printer printer) {
        this.printer = printer;
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
    @Size(max=ValidationConstants.DESCRIPTION_LIMIT, message="Description cannot exceed " + ValidationConstants.DESCRIPTION_LIMIT + " characters")
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
     * @return the inUse
     */
    public Boolean getInUse() {
        return inUse;
    }
    /**
     * @param inUse the inUse to set
     */
    public void setInUse(Boolean inUse) {
        this.inUse = inUse;
    }
    /**
     * @return the printRun
     */
    public PrintRun getPrintRun() {
        return printRun;
    }
    /**
     * @param printRun the printRun to set
     */
    public void setPrintRun(PrintRun printRun) {
        this.printRun = printRun;
    }
}
