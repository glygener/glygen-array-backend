package org.glygen.array.persistence.rdf.template;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlRootElement
@JsonTypeName("descriptorgrouptemplate")
public class DescriptorGroupTemplate extends DescriptionTemplate {

    List<DescriptionTemplate> descriptors;
    
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    public boolean isGroup() {
        return true;
    }

    /**
     * @return the descriptors
     */
    public List<DescriptionTemplate> getDescriptors() {
        return descriptors;
    }

    /**
     * @param descriptors the descriptors to set
     */
    public void setDescriptors(List<DescriptionTemplate> descriptors) {
        this.descriptors = descriptors;
    }

}
