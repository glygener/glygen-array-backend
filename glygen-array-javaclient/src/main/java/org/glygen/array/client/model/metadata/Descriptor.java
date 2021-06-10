package org.glygen.array.client.model.metadata;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("descriptor")
public class Descriptor extends Description{
    
    String value;
    String unit;
    
    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }
    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }
    
    /**
     * @return the unit
     */
    public String getUnit() {
        return unit;
    }
    /**
     * @param unit the unit to set
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    @Override
    public boolean isGroup() {
        return false;
    }
}
