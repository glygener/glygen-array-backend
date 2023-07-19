package org.glygen.array.persistence.rdf.data;

import java.util.Date;

public class FileWrapper {
    
    String id;
    String uri;
    String identifier;
    String drsId;
    String originalName;
    String fileFolder;
    String fileFormat;
    String extension;
    Long fileSize;
    String description;
    Checksum checksum;
    Date createdDate;
    
    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }
    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String indentifier) {
        this.identifier = indentifier;
    }
    /**
     * @return the originalName
     */
    public String getOriginalName() {
        return originalName;
    }
    /**
     * @param originalName the originalName to set
     */
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }
    /**
     * @return the fileFolder
     */
    public String getFileFolder() {
        return fileFolder;
    }
    /**
     * @param fileFolder the fileFolder to set
     */
    public void setFileFolder(String fileFolder) {
        this.fileFolder = fileFolder;
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
     * @return the fileSize
     */
    public Long getFileSize() {
        return fileSize;
    }
    /**
     * @param fileSize the fileSize to set
     */
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
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
     * @return the checksum
     */
    public Checksum getChecksum() {
        return checksum;
    }
    /**
     * @param checksum the checksum to set
     */
    public void setChecksum(Checksum checksum) {
        this.checksum = checksum;
    }
    /**
     * @return the createdDate
     */
    public Date getCreatedDate() {
        return createdDate;
    }
    /**
     * @param createdDate the createdDate to set
     */
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
    /**
     * @return the drsId
     */
    public String getDrsId() {
        return drsId;
    }
    /**
     * @param drsId the drsId to set
     */
    public void setDrsId(String drsId) {
        this.drsId = drsId;
    }
    /**
     * @return the extension
     */
    public String getExtension() {
        return extension;
    }
    /**
     * @param extension the extension to set
     */
    public void setExtension(String extension) {
        this.extension = extension;
    }
    
    

}
