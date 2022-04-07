package org.glygen.array.persistence.rdf;

import java.util.List;

import javax.validation.constraints.Size;

import org.glygen.array.config.ValidationConstants;

public class ProteinLinker extends SequenceBasedLinker {
	
	String uniProtId;
	List<String> pdbIds;
	
	public ProteinLinker() {
		//this.type = LinkerType.PROTEIN;
	}

	/**
	 * @return the uniProtId
	 */
	@Size(max=ValidationConstants.NAME_LIMIT, message="Uniprot ID cannot exceed " + ValidationConstants.NAME_LIMIT + " characters")
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
