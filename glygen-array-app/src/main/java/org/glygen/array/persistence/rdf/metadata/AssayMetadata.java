package org.glygen.array.persistence.rdf.metadata;

import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.template.MetadataTemplateType;

@XmlRootElement
public class AssayMetadata extends MetadataCategory {
    
    public AssayMetadata() {
        this.type = MetadataTemplateType.ASSAY;
    }
    
    public AssayMetadata(MetadataCategory metadata) {
        super (metadata);
        this.type = MetadataTemplateType.ASSAY;
    }

}
