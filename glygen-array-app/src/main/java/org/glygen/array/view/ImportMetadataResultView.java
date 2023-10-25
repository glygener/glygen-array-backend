package org.glygen.array.view;

import java.util.ArrayList;
import java.util.List;

import org.glygen.array.persistence.rdf.metadata.MetadataCategory;

public class ImportMetadataResultView {
    
    List<MetadataCategory>  addedMetadata = new ArrayList<MetadataCategory>();
    List<MetadataCategory> duplicates = new ArrayList<MetadataCategory>();
    List<MetadataError> errors = new ArrayList<MetadataError>();
    String successMessage;
    
    /**
     * @return the addedMetadata
     */
    public List<MetadataCategory> getAddedMetadata() {
        return addedMetadata;
    }
    /**
     * @param addedMetadata the addedMetadata to set
     */
    public void setAddedMetadata(List<MetadataCategory> addedMetadata) {
        this.addedMetadata = addedMetadata;
    }
    /**
     * @return the duplicates
     */
    public List<MetadataCategory> getDuplicates() {
        return duplicates;
    }
    /**
     * @param duplicates the duplicates to set
     */
    public void setDuplicates(List<MetadataCategory> duplicates) {
        this.duplicates = duplicates;
    }
    /**
     * @return the errors
     */
    public List<MetadataError> getErrors() {
        return errors;
    }
    /**
     * @param errors the errors to set
     */
    public void setErrors(List<MetadataError> errors) {
        this.errors = errors;
    }
    /**
     * @return the successMessage
     */
    public String getSuccessMessage() {
        return successMessage;
    }
    /**
     * @param successMessage the successMessage to set
     */
    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }

}
