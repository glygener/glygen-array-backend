package org.glygen.array.persistence.rdf.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FilterExclusionReasonType {
    Probe_unqualifed ("Signals from a questionable probe (Probe did not pass QC)"),
    Unrelated_feature ("Signals from probes of unrelated studies"),
    Lack_of_Signals ("Lack of signals from non-arrayed area on slide (Empty)");
    
    String label;
    
    FilterExclusionReasonType (String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }
    
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    @JsonCreator
    public static FilterExclusionReasonType forValue(String value) {
        if (value.equals("Signals from a questionable probe (Probe did not pass QC)"))
            return Probe_unqualifed;
        else if (value.equals("Signals from probes of unrelated studies"))
            return Unrelated_feature;
        else if (value.equals("Lack of signals from non-arrayed area on slide (Empty)"))
            return Lack_of_Signals;
        return null;
    }
    
    @JsonValue
    public String external() { return label; }
}
