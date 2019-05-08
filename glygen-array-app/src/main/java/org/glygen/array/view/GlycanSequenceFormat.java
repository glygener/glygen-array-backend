package org.glygen.array.view;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GlycanSequenceFormat {
	GLYCOCT("GlycoCT"),
	GWS("GlycoWorkbench");
	
	String label;
	
	@JsonCreator
	public static GlycanSequenceFormat forValue(String value) {
		if (value.equals("GlycoCT"))
			return GLYCOCT;
		else if (value.equals("GlycoWorkbench"))
			return GWS;
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
