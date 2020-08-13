package org.glygen.array.client.model.template;

import java.util.List;

public class DescriptorGroupTemplate extends DescriptionTemplate {

    List<DescriptionTemplate> descriptors;
    
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
