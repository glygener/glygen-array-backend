package org.glygen.array.persistence.rdf.metadata;

import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.template.MetadataTemplateType;

@XmlRootElement
public class DataProcessingSoftware extends MetadataCategory {

    public DataProcessingSoftware() {
        this.type = MetadataTemplateType.DATAPROCESSINGSOFTWARE;
    }
    
    public DataProcessingSoftware(MetadataCategory metadata) {
        super (metadata);
        this.type = MetadataTemplateType.DATAPROCESSINGSOFTWARE;
    }
}
