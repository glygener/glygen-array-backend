package org.glygen.array.persistence.rdf.metadata;

import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.template.MetadataTemplateType;

@XmlRootElement
public class FeatureMetadata extends MetadataCategory {
    public FeatureMetadata() {
        this.type = MetadataTemplateType.FEATURE;
    }
    
    public FeatureMetadata(MetadataCategory metadata) {
        super (metadata);
        this.type = MetadataTemplateType.FEATURE;
    }
}
