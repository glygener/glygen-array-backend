package org.glygen.array.view;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BatchGlycanFileType {
    TABSEPARATED ("Tab separated"),
    XML ("Library XML"),
    GWS ("GlycoWorkbench");
    
    String label;
    
    @JsonCreator
    public static BatchGlycanFileType forValue(String value) {
        if (value.equals("Tab separated"))
            return TABSEPARATED;
        else if (value.equals("GlycoWorkbench"))
            return GWS;
        else if (value.equals("Library XML"))
            return XML;
        return GWS;
    }
    
    private BatchGlycanFileType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
    
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    @JsonValue
    public String external() { return label; }

}
