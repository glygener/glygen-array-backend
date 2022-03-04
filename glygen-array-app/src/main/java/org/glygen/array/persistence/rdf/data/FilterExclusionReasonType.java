package org.glygen.array.persistence.rdf.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FilterExclusionReasonType {
    QCERROR ("Signals from a questionable probe (Probe did not pass QC)"),
    UNRELATED ("Signals from probes of unrelated studies");
    
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
            return QCERROR;
        else if (value.equals("Signals from probes of unrelated studies"))
            return UNRELATED;
        return null;
    }
    
    @JsonValue
    public String external() { return label; }
}
