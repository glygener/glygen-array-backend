package org.glygen.array.view;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BatchGlycanFileType {
    TABSEPARATED ("Tab separated Glycan file"),
    XML ("Library XML"),
    GWS ("GlycoWorkbench"),
    WURCS ("WURCS (line by line)"),
    CFG ("CFG IUPAC (line by line)"),
    REPOSITORYEXPORT ("Repositiry Export (.json)");
    
    String label;
    
    @JsonCreator
    public static BatchGlycanFileType forValue(String value) {
        if (value.toLowerCase().startsWith("tab separated"))
            return TABSEPARATED;
        else if (value.toLowerCase().startsWith("glycoworkbench"))
            return GWS;
        else if (value.toLowerCase().startsWith("library xml"))
            return XML;
        else if (value.toLowerCase().startsWith("wurcs"))
            return WURCS;
        else if (value.toLowerCase().startsWith("cfg"))
            return CFG;
        else if (value.toLowerCase().contains("export"))
            return REPOSITORYEXPORT;
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
