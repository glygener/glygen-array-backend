package org.glygen.array.view;

import org.glygen.array.persistence.rdf.metadata.MetadataCategory;

public class MetadataError {
    MetadataCategory metadata;
    ErrorMessage error;
    
    /**
     * @return the metadata
     */
    public MetadataCategory getMetadata() {
        return metadata;
    }
    /**
     * @param metadata the metadata to set
     */
    public void setMetadata(MetadataCategory metadata) {
        this.metadata = metadata;
    }
    /**
     * @return the error
     */
    public ErrorMessage getError() {
        return error;
    }
    /**
     * @param error the error to set
     */
    public void setError(ErrorMessage error) {
        this.error = error;
    }
}
