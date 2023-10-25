package org.glygen.array.persistence.rdf.metadata;

import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.template.MetadataTemplateType;

@XmlRootElement
public class SpotMetadata extends MetadataCategory {
    
    Boolean isTemplate = false;
    
    public SpotMetadata() {
        this.type = MetadataTemplateType.SPOT;
    }
    
    public SpotMetadata(MetadataCategory m) {
        super(m);
        this.type = MetadataTemplateType.SPOT;
    }
    
    public void setIsTemplate(Boolean isTemplate) {
        this.isTemplate = isTemplate;
    }
    
    public Boolean getIsTemplate() {
        return isTemplate;
    }
}
