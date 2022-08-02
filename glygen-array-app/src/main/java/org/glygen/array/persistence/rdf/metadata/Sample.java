package org.glygen.array.persistence.rdf.metadata;

import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Sample extends MetadataCategory {
    
    String internalId;
    
    public Sample() {
    }
    
    public Sample(MetadataCategory metadata) {
        super (metadata);
        if (metadata instanceof Sample)
            this.internalId = ((Sample) metadata).internalId;
    }
    
    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }
    
    @Size(max=30, message="Id cannot exceed 30 characters")
    public String getInternalId() {
        return internalId;
    }
    
}
