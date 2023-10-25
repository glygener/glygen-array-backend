package org.glygen.array.persistence.rdf.metadata;

import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.template.MetadataTemplateType;

@XmlRootElement
public class SlideMetadata extends MetadataCategory {

    public SlideMetadata() {
        this.type = MetadataTemplateType.SLIDE;
    }
    
    public SlideMetadata(MetadataCategory metadata) {
        super (metadata);
        this.type = MetadataTemplateType.SLIDE;
    }
}
