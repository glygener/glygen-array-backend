package org.glygen.array.view;

import java.util.Date;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

public class GlycanView {
	String id;
	String internalId;
	String glytoucanId;
	String name;
	String comment;
	String sequence;
	GlycanSequenceFormat sequenceFormat=GlycanSequenceFormat.GWS;
	Date dateModified;
	byte[] cartoon;
	Double mass;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	@Size(max=100, message="Id cannot exceed 100 characters")
	public String getInternalId() {
		return internalId;
	}
	
	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}
	
	/**
	 * @return the glytoucanId
	 */
	@Size(min=8, max=11, message="GlytoucanId should be 8 characters long")
	public String getGlytoucanId() {
		return glytoucanId;
	}
	/**
	 * @param glytoucanId the glytoucanId to set
	 */
	public void setGlytoucanId(String glytoucanId) {
		this.glytoucanId = glytoucanId;
	}
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
	 * @return the sequence
	 */
	@NotEmpty
	public String getSequence() {
		return sequence;
	}
	/**
	 * @param sequence the sequence to set
	 */
	public void setSequence(String sequence) {
		this.sequence = sequence;
	}
	
	public GlycanSequenceFormat getSequenceFormat() {
		return sequenceFormat;
	}
	
	public void setSequenceFormat(GlycanSequenceFormat sequenceFormat) {
		this.sequenceFormat = sequenceFormat;
	}
	
	/**
	 * @return the modified date
	 */
	public Date getDateModified() {
		return dateModified;
	}
	
	/**
	 * @param dateModified date to set
	 */
	public void setDateModified(Date dateModified) {
		this.dateModified = dateModified;
	}
	
	public byte[] getCartoon() {
		return cartoon;
	}
	
	public void setCartoon(byte[] cartoon) {
		this.cartoon = cartoon;
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
}
