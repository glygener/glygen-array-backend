package org.glygen.array.persistence.rdf.metadata;

public class SpotMetadata extends MetadataCategory {
    
    Boolean isTemplate = false;
    
    public SpotMetadata() {
    }
    
    public SpotMetadata(MetadataCategory m) {
        super(m);
    }
    
    public void setIsTemplate(Boolean isTemplate) {
        this.isTemplate = isTemplate;
    }
    
    public Boolean getIsTemplate() {
        return isTemplate;
    }
}
