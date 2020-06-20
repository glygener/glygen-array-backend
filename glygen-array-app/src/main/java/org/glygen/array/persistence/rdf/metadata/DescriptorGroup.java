package org.glygen.array.persistence.rdf.metadata;

import java.util.List;

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
    public boolean isGroup() {
        return true;
    }
}
