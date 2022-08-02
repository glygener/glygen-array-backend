package org.glygen.array.persistence.rdf.metadata;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlRootElement
@JsonTypeName("descriptorgroup")
public class DescriptorGroup extends Description {
    
    List<Description> descriptors;
    
    /**
     * @return the descriptors
     */
    public List<Description> getDescriptors() {
        return descriptors;
    }
    /**
     * @param descriptors the descriptors to set
     */
    public void setDescriptors(List<Description> descriptors) {
        this.descriptors = descriptors;
    }
    
    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean isGroup() {
        return true;
    }
}
