package org.glygen.array.persistence.rdf.data;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ExclusionReasonType {
    MISPRINTED ("Signals from misprinted or misshapen spot"),
    ARTEFACT ("Signals caused by defect on slide (Artefact on slide)"),
    QCERROR ("Signals from a questionable probe (Probe did not pass QC)"),
    UNRELATED ("Signals from probes of unrelated studies"),
    MISSING ("Missing spots due to the printer fault");
    
    String label;
    
    ExclusionReasonType (String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }
    
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    @JsonCreator
    public static ExclusionReasonType forValue(String value) {
        if (value.equals("Signals from misprinted or misshapen spot"))
            return MISPRINTED;
        else if (value.equals("Signals caused by defect on slide (Artefact on slide)"))
            return ARTEFACT;
        else if (value.equals("Signals from a questionable probe (Probe did not pass QC)"))
            return QCERROR;
        else if (value.equals("Signals from probes of unrelated studies"))
            return UNRELATED;
        else if (value.equals("Missing spots due to the printer fault"))
            return MISSING;
        return null;
    }

}
