package org.glygen.array.client.model.data;

public class Slide {
    
    String id;
    String uri;
    PrintedSlide printedSlide;
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
     * @return the printedSlide
     */
    public PrintedSlide getPrintedSlide() {
        return printedSlide;
    }
    /**
     * @param printedSlide the printedSlide to set
     */
    public void setPrintedSlide(PrintedSlide printedSlide) {
        this.printedSlide = printedSlide;
    }
    

}
