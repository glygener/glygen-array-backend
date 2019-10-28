package org.glygen.array.client.model;

public class PeptideLinker extends Linker {
	
	String sequence;
	
	public String getSequence() {
		return sequence;
	}
	
	public void setSequence(String sequence) {
		this.sequence = sequence;
	}
		
	public PeptideLinker() {
		this.type = LinkerType.PEPTIDE_LINKER;
	}
}
