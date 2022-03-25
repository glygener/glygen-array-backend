package org.glygen.array.persistence.rdf.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TechnicalExclusionReasonType {
    Spot_Issues ("Signals from misprinted or misshapen spot"),
    Artifact ("Signals caused by defect on slide (Artifact on slide)"),
    Missing_Spot("Missing spots due to the printer fault");
    
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
            return Spot_Issues;
        else if (value.equals("Signals caused by defect on slide (Artifact on slide)"))
            return Artifact;
        else if (value.equals("Missing spots due to the printer fault"))
            return Missing_Spot;
        return null;
    }
    
    @JsonValue
    public String external() { return label; }

}
