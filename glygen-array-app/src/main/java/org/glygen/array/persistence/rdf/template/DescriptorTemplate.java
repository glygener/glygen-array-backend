package org.glygen.array.persistence.rdf.template;

import java.util.List;

import org.glygen.array.persistence.rdf.metadata.Description;
import org.grits.toolbox.glycanarray.om.model.UnitOfMeasurement;

public class DescriptorTemplate extends Description {
    
    Namespace namespace;
    List<String> selectionList;
    List<UnitOfMeasurement> units;
    UnitOfMeasurement defaultUnit;
        
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
    public List<UnitOfMeasurement> getUnits() {
        return units;
    }

    /**
     * @param units the units to set
     */
    public void setUnits(List<UnitOfMeasurement> units) {
        this.units = units;
    }

    /**
     * @return the defaultUnit
     */
    public UnitOfMeasurement getDefaultUnit() {
        return defaultUnit;
    }

    /**
     * @param defaultUnit the defaultUnit to set
     */
    public void setDefaultUnit(UnitOfMeasurement defaultUnit) {
        this.defaultUnit = defaultUnit;
    }
    

}
