package org.glygen.array.view;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.data.FileWrapper;
import org.glygen.array.persistence.rdf.metadata.MetadataCategory;

@XmlRootElement
public class MetadataImportInput {
    FileWrapper file;
    List<MetadataCategory> selectedMetadata;
    
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
     * @return the selectedMetadata
     */
    public List<MetadataCategory> getSelectedMetadata() {
        return selectedMetadata;
    }
    /**
     * @param selectedMetadata the selectedMetadata to set
     */
    public void setSelectedMetadata(List<MetadataCategory> selectedMetadata) {
        this.selectedMetadata = selectedMetadata;
    }

}
