package org.glygen.array.persistence.rdf;

import javax.validation.constraints.Size;

import org.glygen.array.config.ValidationConstants;

public class SmallMoleculeLinker extends Linker {
	Long pubChemId;
	String imageURL;
	String inChiKey;
	String inChiSequence;
	String iupacName;
	String isomericSmiles;
	String smiles;
	Double mass;
	String molecularFormula;
	LinkerClassification classification;
	
	public SmallMoleculeLinker() {
		this.type = LinkerType.SMALLMOLECULE_LINKER;
	}
	
	/**
	 * @return the pubChemId
	 */
	public Long getPubChemId() {
		return pubChemId;
	}
	/**
	 * @param pubChemId the pubChemId to set
	 */
	public void setPubChemId(Long pubChemId) {
		this.pubChemId = pubChemId;
	}
	/**
	 * @return the imageURL
	 */
	public String getImageURL() {
		return imageURL;
	}
	/**
	 * @param imageURL the imageURL to set
	 */
	public void setImageURL(String imageURL) {
		this.imageURL = imageURL;
	}
	/**
	 * @return the inChiKey
	 */
	@Size(max=27, message="Inchikey cannot exceed 27 characters")
	public String getInChiKey() {
		return inChiKey;
	}
	/**
	 * @param inChiKey the inChiKey to set
	 */
	public void setInChiKey(String inChiKey) {
		this.inChiKey = inChiKey;
	}
	/**
	 * @return the inChiSequence
	 */
	@Size(max=ValidationConstants.SEQUENCE_LIMIT, message="InChiSequence cannot exceed " + ValidationConstants.SEQUENCE_LIMIT + " characters")
	public String getInChiSequence() {
		return inChiSequence;
	}
	/**
	 * @param inChiSequence the inChiSequence to set
	 */
	public void setInChiSequence(String inChiSequence) {
		this.inChiSequence = inChiSequence;
	}
	/**
	 * @return the iupacName
	 */
	@Size(max=ValidationConstants.DESCRIPTION_LIMIT, message="InChiSequence cannot exceed " + ValidationConstants.DESCRIPTION_LIMIT + " characters")
	public String getIupacName() {
		return iupacName;
	}
	/**
	 * @param iupacName the iupacName to set
	 */
	public void setIupacName(String iupacName) {
		this.iupacName = iupacName;
	}
	/**
	 * @return the mass
	 */
	public Double getMass() {
		return mass;
	}
	/**
	 * @param mass the mass to set
	 */
	public void setMass(Double mass) {
		this.mass = mass;
	}
	/**
	 * @return the molecularFormula
	 */
	@Size(max=256, message="InChiSequence cannot exceed 256 characters")
	public String getMolecularFormula() {
		return molecularFormula;
	}
	/**
	 * @param molecularFormula the molecularFormula to set
	 */
	public void setMolecularFormula(String molecularFormula) {
		this.molecularFormula = molecularFormula;
	}
	
	public LinkerClassification getClassification() {
		return classification;
	}
	
	public void setClassification(LinkerClassification classification) {
		this.classification = classification;
	}
	
	@Size(max=ValidationConstants.SEQUENCE_LIMIT, message="SMILES cannot exceed " + ValidationConstants.SEQUENCE_LIMIT + " characters")
	public String getSmiles() {
        return smiles;
    }
	
	public void setSmiles(String smiles) {
        this.smiles = smiles;
    }
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SmallMoleculeLinker))
			return super.equals(obj);
		if (pubChemId  != null){ // check if pubchemids are the same
			return pubChemId.equals(((SmallMoleculeLinker)obj).getPubChemId());
		}
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		if (pubChemId != null)
			return pubChemId.hashCode();
		return super.hashCode();
	}

    /**
     * @return the isomericSmiles
     */
	@Size(max=ValidationConstants.SEQUENCE_LIMIT, message="SMILES cannot exceed " + ValidationConstants.SEQUENCE_LIMIT + " characters")
    public String getIsomericSmiles() {
        return isomericSmiles;
    }

    /**
     * @param isomericSmiles the isomericSmiles to set
     */
    public void setIsomericSmiles(String isomericSmiles) {
        this.isomericSmiles = isomericSmiles;
    }
}
