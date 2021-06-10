package org.glygen.array.client.model.template;

import java.util.List;

import org.grits.toolbox.glycanarray.om.model.UnitOfMeasurement;

public class DescriptorTemplate extends DescriptionTemplate {
    
    Namespace namespace;
    List<String> selectionList;
    List<String> units;
        
    @Override
    public boolean isGroup() {
        return false;
    }

    /**
     * @return the namespace
     */
    public Namespace getNamespace() {
        return namespace;
    }

    /**
     * @param namespace the namespace to set
     */
    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
    }

    /**
     * @return the selectionList
     */
    public List<String> getSelectionList() {
        return selectionList;
    }

    /**
     * @param selectionList the selectionList to set
     */
    public void setSelectionList(List<String> selectionList) {
        this.selectionList = selectionList;
    }

    /**
     * @return the units
     */
    public List<String> getUnits() {
        return units;
    }

    /**
     * @param units the units to set
     */
    public void setUnits(List<String> units) {
        this.units = units;
    }
}
