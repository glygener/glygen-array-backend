package org.glygen.array.view;

import java.util.List;

import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.data.FileWrapper;

public class LibraryImportInput {
    FileWrapper file;
    List<SlideLayout> slideLayouts;
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
     * @return the slideLayouts
     */
    public List<SlideLayout> getSlideLayouts() {
        return slideLayouts;
    }
    /**
     * @param slideLayouts the slideLayouts to set
     */
    public void setSlideLayouts(List<SlideLayout> slideLayouts) {
        this.slideLayouts = slideLayouts;
    }

}
