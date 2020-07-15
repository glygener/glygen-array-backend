package org.glygen.array.persistence.rdf.data;

import java.util.Date;
import java.util.List;

import javax.validation.constraints.Size;

import org.glygen.array.config.ValidationConstants;
import org.glygen.array.persistence.rdf.Creator;
import org.glygen.array.persistence.rdf.metadata.Sample;

public class ArrayDataset {
    String id;
    String uri;
    String name;
    String description;
    
    Sample sample;
    List<RawData> rawDataList;
    ProcessedData processedData;
    
    // Should these become part of RawData only??
    List<Image> images;
    List<Slide> slides;
    
    boolean isPublic = false;
    Creator user;
    
    Date dateModified;
    Date dateCreated;
    Date dateAddedToLibrary;

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

    public List<RawData> getRawDataList() {
        return rawDataList;
    }
    
    public void setRawDataList(List<RawData> rawDataList) {
        this.rawDataList = rawDataList;
    }

    /**
     * @return the processedData
     */
    public ProcessedData getProcessedData() {
        return processedData;
    }

    /**
     * @param processedData the processedData to set
     */
    public void setProcessedData(ProcessedData processedData) {
        this.processedData = processedData;
    }

    /**
     * @return the images
     */
    public List<Image> getImages() {
        return images;
    }

    /**
     * @param images the images to set
     */
    public void setImage(List<Image> images) {
        this.images = images;
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

}
