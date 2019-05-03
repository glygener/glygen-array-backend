package org.glygen.array.client.model;

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
}
