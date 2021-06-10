package org.glygen.array.client.model;

import java.util.List;

public class ProteinLinker extends SequenceBasedLinker {
	
	String uniProtId;
	List<String> pdbIds;
	
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

	public List<String> getPdbIds() {
        return pdbIds;
    }
	
	public void setPdbIds(List<String> pdbIds) {
        this.pdbIds = pdbIds;
    }
}
