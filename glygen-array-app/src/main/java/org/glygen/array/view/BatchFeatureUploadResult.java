package org.glygen.array.view;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.Feature;

@XmlRootElement
public class BatchFeatureUploadResult {
    List<Feature> addedFeatures = new ArrayList<Feature>();
    String successMessage;
    List<Feature> duplicateFeatures = new ArrayList<Feature>();
    List<ErrorMessage> errors = new ArrayList<>();
    
    /**
     * @return the addedFeatures
     */
    public List<Feature> getAddedFeatures() {
        return addedFeatures;
    }
    /**
     * @param addedFeatures the addedFeatures to set
     */
    public void setAddedFeatures(List<Feature> addedFeatures) {
        this.addedFeatures = addedFeatures;
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
    /**
     * @return the duplicateFeatures
     */
    public List<Feature> getDuplicateFeatures() {
        return duplicateFeatures;
    }
    /**
     * @param duplicateFeatures the duplicateFeatures to set
     */
    public void setDuplicateFeatures(List<Feature> duplicateFeatures) {
        this.duplicateFeatures = duplicateFeatures;
    }
    /**
     * @return the errors
     */
    public List<ErrorMessage> getErrors() {
        return errors;
    }
    /**
     * @param errors the errors to set
     */
    public void setErrors(List<ErrorMessage> errors) {
        this.errors = errors;
    }

}
