package org.glygen.array.persistence.rdf.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.config.ValidationConstants;
import org.glygen.array.persistence.rdf.Creator;
import org.glygen.array.persistence.rdf.Publication;
import org.glygen.array.persistence.rdf.metadata.Sample;

@XmlRootElement
public class ArrayDataset extends FutureTask implements ChangeTrackable {
    String id;
    String uri;
    String name;
    String description;
   
    Sample sample;
    List<Slide> slides = new ArrayList<Slide>();
    List<Publication> publications = new ArrayList<Publication>();
    List<Creator> collaborators = new ArrayList<Creator>();
    List<Grant> grants = new ArrayList<Grant>();
    List<String> keywords = new ArrayList<String>();
    List<FileWrapper> files = new ArrayList<FileWrapper>();
    
    boolean isPublic = false;
    Creator user;
    
    Date dateModified;
    Date dateCreated;
    Date dateAddedToLibrary;
    
    String publicURI;
    String publicId;
    
    List<ChangeLog> changes = new ArrayList<>();
    
    FutureTaskStatus uploadStatus = FutureTaskStatus.NOTSTARTED;

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
     * @return the sample
     */
    public Sample getSample() {
        return sample;
    }

    /**
     * @param sample the sample to set
     */
    public void setSample(Sample sample) {
        this.sample = sample;
    }

    /**
     * @return the slides
     */
    public List<Slide> getSlides() {
        return slides;
    }

    /**
     * @param slides the slides to set
     */
    public void setSlides(List<Slide> slides) {
        this.slides = slides;
    }

    /**
     * @return the isPublic
     */
    public boolean getIsPublic() {
        return isPublic;
    }

    /**
     * @param isPublic the isPublic to set
     */
    public void setIsPublic(boolean isPublic) {
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
     * @return the keywords
     */
    public List<String> getKeywords() {
        return keywords;
    }

    /**
     * @param keywords the keywords to set
     */
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }
    
    @Override
    public FutureTaskStatus getStatus() {
        return status;
    }

    /**
     * @return the publications
     */
    public List<Publication> getPublications() {
        return publications;
    }

    /**
     * @param publications the publications to set
     */
    public void setPublications(List<Publication> publications) {
        this.publications = publications;
    }

    /**
     * @return the collaborators
     */
    public List<Creator> getCollaborators() {
        return collaborators;
    }

    /**
     * @param collaborators the collaborators to set
     */
    public void setCollaborators(List<Creator> collaborators) {
        this.collaborators = collaborators;
    }

    /**
     * @return the grants
     */
    public List<Grant> getGrants() {
        return grants;
    }

    /**
     * @param grants the grants to set
     */
    public void setGrants(List<Grant> grants) {
        this.grants = grants;
    }

    @Override
    public List<ChangeLog> getChanges() {
        return this.changes;
    }

    @Override
    public void setChanges(List<ChangeLog> changes) {
        this.changes = changes;
    }

    @Override
    public void addChange(ChangeLog change) {
        this.changes.add(change);
    }

    /**
     * @return the publicURI
     */
    public String getPublicURI() {
        return publicURI;
    }

    /**
     * @param publicURI the publicURI to set
     */
    public void setPublicURI(String publicURI) {
        this.publicURI = publicURI;
    }

    /**
     * @return the publicId
     */
    public String getPublicId() {
        if (publicURI != null) {
            return publicURI.substring(publicURI.lastIndexOf("/")+1);
        }
        return publicId;
    }

    /**
     * @param publicId the publicId to set
     */
    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    /**
     * @return the files
     */
    public List<FileWrapper> getFiles() {
        return files;
    }

    /**
     * @param files the files to set
     */
    public void setFiles(List<FileWrapper> files) {
        this.files = files;
    }

    /**
     * @return the uploadStatus
     */
    public FutureTaskStatus getUploadStatus() {
        return uploadStatus;
    }

    /**
     * @param uploadStatus the uploadStatus to set
     */
    public void setUploadStatus(FutureTaskStatus uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

}
