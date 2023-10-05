package org.glygen.array.persistence.rdf.metadata;

import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.template.MetadataTemplateType;

@XmlRootElement
public class ScannerMetadata extends MetadataCategory {
    
    public ScannerMetadata() {
        this.type = MetadataTemplateType.SCANNER;
    }
    
    public ScannerMetadata(MetadataCategory metadata) {
        super (metadata);
        this.type = MetadataTemplateType.SCANNER;
    }
}
