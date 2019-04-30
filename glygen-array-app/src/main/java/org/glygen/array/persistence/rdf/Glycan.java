package org.glygen.array.persistence.rdf;

import java.util.Date;

public class Glycan {
	
	String uri;
	String glyTouCanId;
	String internalId;
	String name;
	String comment;
	String sequence;
	String sequenceType;
	Date dateModified;
	
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
	public String getGlyTouCanId() {
		return glyTouCanId;
	}
	/**
	 * @param glyTouCanId the glyTouCanId to set
	 */
	public void setGlyTouCanId(String glyTouCanId) {
		this.glyTouCanId = glyTouCanId;
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
