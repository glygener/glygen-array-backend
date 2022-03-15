package org.glygen.array.view;

import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.data.FileWrapper;

public class LibraryImportInput {
    FileWrapper file;
    SlideLayout slideLayout;
    /**
     * @return the file
     */
    public FileWrapper getFile() {
        return file;
    }
    /**
     * @param file the file to set
     */
    public void setFile(FileWrapper file) {
        this.file = file;
    }
    /**
     * @return the slideLayout
     */
    public SlideLayout getSlideLayout() {
        return slideLayout;
    }
    /**
     * @param slideLayout the slideLayout to set
     */
    public void setSlideLayout(SlideLayout slideLayout) {
        this.slideLayout = slideLayout;
    }

}
