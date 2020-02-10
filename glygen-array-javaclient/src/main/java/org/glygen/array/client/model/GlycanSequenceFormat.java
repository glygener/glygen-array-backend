package org.glygen.array.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GlycanSequenceFormat {
	GLYCOCT("GlycoCT"),
	GWS("GlycoWorkbench"),
	WURCS("Wurcs"),
	IUPAC("IUPAC");
	
	String label;
	
	@JsonCreator
	public static GlycanSequenceFormat forValue(String value) {
		if (value.equals("GlycoCT"))
			return GLYCOCT;
		else if (value.equals("GlycoWorkbench"))
			return GWS;
		else if (value.equals("Wurcs"))
            return WURCS;
		else if (value.equals("IUPAC"))
            return IUPAC;
		return GLYCOCT;
	}
	
	private GlycanSequenceFormat(String label) {
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
