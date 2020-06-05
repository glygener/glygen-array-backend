package org.glygen.array.persistence.rdf.template;

import java.util.List;

import org.glygen.array.persistence.rdf.metadata.Description;

public class DescriptorGroupTemplate extends Description {

    List<Description> descriptors;
    
    @Override
    public boolean isGroup() {
        return true;
    }

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

}
