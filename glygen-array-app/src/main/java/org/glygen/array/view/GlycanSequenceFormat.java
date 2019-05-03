package org.glygen.array.view;

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
