package org.glygen.array.persistence.rdf.metadata;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AssayMetadata extends MetadataCategory {
    
    public AssayMetadata() {
        
    }
    
    public AssayMetadata(MetadataCategory metadata) {
        super (metadata);
    }

}
