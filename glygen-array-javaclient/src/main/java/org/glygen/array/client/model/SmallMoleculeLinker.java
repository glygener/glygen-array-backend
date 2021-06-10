package org.glygen.array.client.model;

public class SmallMoleculeLinker extends Linker {
	
	public final static String PUBCHEM_CID = "https://pubchem.ncbi.nlm.nih.gov/compound/";
	
	Long pubChemId;
	String imageURL;
	String inChiKey;
	String inChiSequence;
	String iupacName;
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
	public String getMolecularFormula() {
		return molecularFormula;
	}
	/**
	 * @param molecularFormula the molecularFormula to set
	 */
	public void setMolecularFormula(String molecularFormula) {
		this.molecularFormula = molecularFormula;
	}
	
	public String getPubChemUrl () {
		return PUBCHEM_CID + pubChemId; 
	}
	
	public LinkerClassification getClassification() {
		return classification;
	}
	
	public void setClassification(LinkerClassification classification) {
		this.classification = classification;
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
}
