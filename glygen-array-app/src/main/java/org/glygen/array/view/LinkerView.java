package org.glygen.array.view;

import java.util.Date;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

public class LinkerView {
	
	String id;
	String name;
	String comment;
	String pubChemId;
	String imageURL;
	String inChiKey;
	String inChiSequence;
	String iupacName;
	Double mass;
	String molecularFormula;
	Date dateModified;
	
	
	/**
	 * @return the name
	 */
	@Size(max=100, message="Name cannot exceed 100 characters")
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
	@Size(max=250, message="Comment cannot exceed 250 characters")
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
	@NotEmpty
	public String getPubChemId() {
		return pubChemId;
	}
	/**
	 * @param pubChemId the pubChemId to set
	 */
	public void setPubChemId(String pubChemId) {
		this.pubChemId = pubChemId;
	}
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
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
	
	

}