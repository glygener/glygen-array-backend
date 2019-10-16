package org.glygen.array.persistence.rdf;

public class ProteinLinker extends Linker {
	
	String sequence;
	String uniProtId;
	String pdbId;
	
	public ProteinLinker() {
		this.type = LinkerType.PROTEIN_LINKER;
	}

	/**
	 * @return the sequence
	 */
	public String getSequence() {
		return sequence;
	}

	/**
	 * @param sequence the sequence to set
	 */
	public void setSequence(String sequence) {
		this.sequence = sequence;
	}

	/**
	 * @return the uniProtId
	 */
	public String getUniProtId() {
		return uniProtId;
	}

	/**
	 * @param uniProtId the uniProtId to set
	 */
	public void setUniProtId(String uniProtId) {
		this.uniProtId = uniProtId;
	}

	/**
	 * @return the pdbId
	 */
	public String getPdbId() {
		return pdbId;
	}

	/**
	 * @param pdbId the pdbId to set
	 */
	public void setPdbId(String pdbId) {
		this.pdbId = pdbId;
	}
}
