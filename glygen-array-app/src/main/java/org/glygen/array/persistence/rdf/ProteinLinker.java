package org.glygen.array.persistence.rdf;

public class ProteinLinker extends SequenceBasedLinker {
	
	String uniProtId;
	String pdbId;
	
	public ProteinLinker() {
		this.type = LinkerType.PROTEIN_LINKER;
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
