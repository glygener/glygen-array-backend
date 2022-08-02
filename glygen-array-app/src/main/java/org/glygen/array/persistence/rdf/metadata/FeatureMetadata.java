package org.glygen.array.persistence.rdf.metadata;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FeatureMetadata extends MetadataCategory {
    public FeatureMetadata() {
        // TODO Auto-generated constructor stub
    }
    
    public FeatureMetadata(MetadataCategory metadata) {
        super (metadata);
    }
}
