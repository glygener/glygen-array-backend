package org.glygen.array.persistence.rdf;

import java.util.Date;

public class Linker {
	
	String uri;
	String name;
	String comment;
	Long pubChemId;
	String imageURL;
	String inChiKey;
	String inChiSequence;
	String iupacName;
	Double mass;
	String molecularFormula;
	Date dateModified;
	Date dateCreated;
	Date dateAddedToLibrary;
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}
	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
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
	/**
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}
	/**
	 * @param uri the uri to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}
	/**
	 * @return the dateModified
	 */
	public Date getDateModified() {
		return dateModified;
	}
	/**
	 * @param dateModified the dateModified to set
	 */
	public void setDateModified(Date dateModified) {
		this.dateModified = dateModified;
	}
	/**
	 * @return the dateCreated
	 */
	public Date getDateCreated() {
		return dateCreated;
	}
	/**
	 * @param dateCreated the dateCreated to set
	 */
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	/**
	 * @return the dateAddedToLibrary
	 */
	public Date getDateAddedToLibrary() {
		return dateAddedToLibrary;
	}
	/**
	 * @param dateAddedToLibrary the dateAddedToLibrary to set
	 */
	public void setDateAddedToLibrary(Date dateAddedToLibrary) {
		this.dateAddedToLibrary = dateAddedToLibrary;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Linker))
			return false;
		if (uri != null && ((Linker)obj).getUri() != null)
			return  uri.equals(((Linker)obj).getUri());
		else if (pubChemId  != null){ // check if pubchemids are the same
			return pubChemId.equals(((Linker)obj).getPubChemId());
		}
		return false;
	}

}
