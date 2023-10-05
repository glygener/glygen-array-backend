package org.glygen.array.persistence.rdf.metadata;

import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.template.MetadataTemplateType;

@XmlRootElement
public class ImageAnalysisSoftware extends MetadataCategory {
    
    public ImageAnalysisSoftware() {
        this.type = MetadataTemplateType.IMAGEANALYSISSOFTWARE;
    }

    public ImageAnalysisSoftware(MetadataCategory metadata) {
        super (metadata);
        this.type = MetadataTemplateType.IMAGEANALYSISSOFTWARE;
    }
}
