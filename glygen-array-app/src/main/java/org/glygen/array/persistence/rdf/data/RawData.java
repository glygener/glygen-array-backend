package org.glygen.array.persistence.rdf.data;

import java.util.Date;
import java.util.Map;
import java.util.List;

import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.metadata.ImageAnalysisSoftware;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class RawData {
    
    String id;
    String uri;
    
    List<Measurement> measurements;
    Map<Measurement, Spot> dataMap;
    Map<String, String> measurementToSpotIdMap;
    ImageAnalysisSoftware metadata;
    Image image;
    Slide slide;
    
    String filename;  // name of the raw data file in uploaded file folder or any other designated data folder
    String fileFormat; // GenePix or Proscan
    Double powerLevel = 100.0;  // 100% or less

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

    /**
     * @return the measurements
     */
    public List<Measurement> getMeasurements() {
        return measurements;
    }

    /**
     * @param measurements the measurements to set
     */
    public void setMeasurements(List<Measurement> measurements) {
        this.measurements = measurements;
    }

    /**
     * @return the measurementToSpotIdMap
     */
    public Map<String, String> getMeasurementToSpotIdMap() {
        return measurementToSpotIdMap;
    }

    /**
     * @param measurementToSpotIdMap the measurementToSpotIdMap to set
     */
    public void setMeasurementToSpotIdMap(Map<String, String> measurementToSpotIdMap) {
        this.measurementToSpotIdMap = measurementToSpotIdMap;
    }
    
    @JsonAnySetter
    public void setSpot (String measurementId, String spotId) {
        this.measurementToSpotIdMap.put(measurementId, spotId);
    }

    /**
     * @return the fileFormat
     */
    public String getFileFormat() {
        return fileFormat;
    }

    /**
     * @param fileFormat the fileFormat to set
     */
    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    /**
     * @return the dataMap
     */
    @JsonIgnore
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
     * @return the powerLevel
     */
    public Double getPowerLevel() {
        return powerLevel;
    }

    /**
     * @param powerLevel the powerLevel to set
     */
    public void setPowerLevel(Double powerLevel) {
        this.powerLevel = powerLevel;
    }

}
