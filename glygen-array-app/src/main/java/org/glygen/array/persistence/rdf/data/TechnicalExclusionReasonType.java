package org.glygen.array.persistence.rdf.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TechnicalExclusionReasonType {
    MISPRINTED ("Signals from misprinted or misshapen spot"),
    ARTEFACT ("Signals caused by defect on slide (Artefact on slide)"),
    MISSING ("Missing spots due to the printer fault");
    
    String label;
    
    TechnicalExclusionReasonType (String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }
    
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    @JsonCreator
    public static TechnicalExclusionReasonType forValue(String value) {
        if (value.equals("Signals from misprinted or misshapen spot"))
            return MISPRINTED;
        else if (value.equals("Signals caused by defect on slide (Artefact on slide)"))
            return ARTEFACT;
        else if (value.equals("Missing spots due to the printer fault"))
            return MISSING;
        return null;
    }
    
    @JsonValue
    public String external() { return label; }

}
