package org.glygen.array.persistence.rdf.metadata;

import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.template.MetadataTemplateType;

@XmlRootElement
public class Printer extends MetadataCategory {
    
    public Printer() {
        this.type = MetadataTemplateType.PRINTER;
    }

    public Printer(MetadataCategory metadata) {
        super (metadata);
        this.type = MetadataTemplateType.PRINTER;
    }
}
