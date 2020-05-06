package org.glygen.array.persistence.rdf.data;

import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.metadata.Printer;
import org.glygen.array.persistence.rdf.metadata.SlideMetadata;

public class Slide {
    
    String id;
    String uri;
    SlideLayout layout;
    SlideMetadata metadata;
    Printer printer;
    Image image;
    
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
    

}
