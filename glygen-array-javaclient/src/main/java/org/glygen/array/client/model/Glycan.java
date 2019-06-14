package org.glygen.array.client.model;

import java.util.Date;

public class Glycan {
	
	String uri;
	String glytoucanId;
	String internalId;
	String name;
	String comment;
	String sequence;
	String sequenceType;
	Double mass;
	Date dateModified;
	Date dateCreated;
	Date dateAddedToLibrary;
	
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
	 * @return the glyTouCanId
	 */
	public String getGlytoucanId() {
		return glytoucanId;
	}
	/**
	 * @param glyTouCanId the glyTouCanId to set
	 */
	public void setGlytoucanId(String glyTouCanId) {
		this.glytoucanId = glyTouCanId;
	}
	/**
	 * @return the internalId
	 */
	public String getInternalId() {
		return internalId;
	}
	/**
	 * @param internalId the internalId to set
	 */
	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}
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
	 * @return the sequenceType
	 */
	public String getSequenceType() {
		return sequenceType;
	}
	/**
	 * @param sequenceType the sequenceType to set
	 */
	public void setSequenceType(String sequenceType) {
		this.sequenceType = sequenceType;
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
		if (!(obj instanceof Glycan))
			return false;
		if (uri != null && ((Glycan)obj).getUri()!= null)
			return  uri.equals(((Glycan)obj).getUri());
		else { // check if sequences are the same
			return sequence.equals(((Glycan)obj).getSequence());
		}
	}
}
