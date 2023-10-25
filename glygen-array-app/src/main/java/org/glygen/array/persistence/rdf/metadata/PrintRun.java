package org.glygen.array.persistence.rdf.metadata;

import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.template.MetadataTemplateType;

@XmlRootElement
public class PrintRun extends MetadataCategory {
    
    public PrintRun() {
        this.type = MetadataTemplateType.PRINTRUN;
    }
    
    public PrintRun(MetadataCategory metadata) {
        super (metadata);
        this.type = MetadataTemplateType.PRINTRUN;
    }

}
