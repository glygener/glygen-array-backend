package org.glygen.array.persistence.rdf.metadata;

import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.template.MetadataTemplateType;

import com.fasterxml.jackson.annotation.JsonIgnore;

@XmlRootElement
public class Sample extends MetadataCategory {
    
    String internalId;
    
    public Sample() {
        this.type = MetadataTemplateType.SAMPLE;
    }
    
    public Sample(MetadataCategory metadata) {
        super (metadata);
        if (metadata instanceof Sample)
            this.internalId = ((Sample) metadata).internalId;
        this.type = MetadataTemplateType.SAMPLE;
    }
    
    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }
    
    @JsonIgnore
    @Size(max=30, message="Id cannot exceed 30 characters")
    public String getInternalId() {
        return internalId;
    }
    
}
