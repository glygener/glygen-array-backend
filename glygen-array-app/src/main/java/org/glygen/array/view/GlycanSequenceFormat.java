package org.glygen.array.view;

import com.fasterxml.jackson.annotation.JsonValue;

public enum GlycanSequenceFormat {
	GLYCOCT("GlycoCT"),
	GWS("GlycoWorkbench");
	
	String label;
	
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
