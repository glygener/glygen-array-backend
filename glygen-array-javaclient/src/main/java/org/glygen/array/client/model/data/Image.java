package org.glygen.array.client.model.data;

import org.glygen.array.client.model.metadata.ScannerMetadata;

public class Image {
    
    String id;
    String uri;
    String fileName;
    ScannerMetadata scanner;
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
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }
    /**
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    /**
     * @return the scanner
     */
    public ScannerMetadata getScanner() {
        return scanner;
    }
    /**
     * @param scanner the scanner to set
     */
    public void setScanner(ScannerMetadata scanner) {
        this.scanner = scanner;
    }

}
