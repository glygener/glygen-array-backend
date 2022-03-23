package org.glygen.array.view;

import java.util.ArrayList;
import java.util.List;

import org.glygen.array.persistence.rdf.Linker;

public class BatchLinkerUploadResult {
    
    List<Linker> addedLinkers = new ArrayList<Linker>();
    String successMessage;
    List<Linker> duplicateLinkers = new ArrayList<Linker>();
    List<ErrorMessage> errors = new ArrayList<>();
    
    /**
     * @return the addedLinkers
     */
    public List<Linker> getAddedLinkers() {
        return addedLinkers;
    }
    /**
     * @param addedLinkers the addedLinkers to set
     */
    public void setAddedLinkers(List<Linker> addedLinkers) {
        this.addedLinkers = addedLinkers;
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
     * @return the duplicateLinkers
     */
    public List<Linker> getDuplicateLinkers() {
        return duplicateLinkers;
    }
    /**
     * @param duplicateLinkers the duplicateLinkers to set
     */
    public void setDuplicateLinkers(List<Linker> duplicateLinkers) {
        this.duplicateLinkers = duplicateLinkers;
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
