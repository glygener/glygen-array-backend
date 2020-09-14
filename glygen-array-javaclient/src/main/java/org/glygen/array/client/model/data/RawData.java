package org.glygen.array.client.model.data;

import java.util.Date;
import java.util.Map;

import org.glygen.array.client.model.Spot;
import org.glygen.array.client.model.metadata.ImageAnalysisSoftware;


public class RawData {
    
    String id;
    String uri;
    
    Map<Measurement, Spot> dataMap;
    ImageAnalysisSoftware metadata;
    Image image;
    Slide slide;
    
    String filename;  // name of the raw data file in uploaded file folder or any other designated data folder

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
     * @return the dataMap
     */
    public Map<Measurement, Spot> getDataMap() {
        return dataMap;
    }

    /**
     * @param dataMap the dataMap to set
     */
    public void setDataMap(Map<Measurement, Spot> dataMap) {
        this.dataMap = dataMap;
    }

    /**
     * @return the metadata
     */
    public ImageAnalysisSoftware getMetadata() {
        return metadata;
    }

    /**
     * @param metadata the metadata to set
     */
    public void setMetadata(ImageAnalysisSoftware metadata) {
        this.metadata = metadata;
    }

    /**
     * @return the image
     */
    public Image getImage() {
        return image;
    }

    /**
     * @param image the image to set
     */
    public void setImage(Image image) {
        this.image = image;
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * @return the slide
     */
    public Slide getSlide() {
        return slide;
    }

    /**
     * @param slide the slide to set
     */
    public void setSlide(Slide slide) {
        this.slide = slide;
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
