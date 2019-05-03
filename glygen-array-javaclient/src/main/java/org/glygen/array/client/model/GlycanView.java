package org.glygen.array.client.model;

public class GlycanView {
	String id;
	String internalId;
	String glytoucanId;
	String name;
	String comment;
	String sequence;
	GlycanSequenceFormat sequenceFormat = GlycanSequenceFormat.GLYCOCT;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getInternalId() {
		return internalId;
	}
	
	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}
	
	/**
	 * @return the glytoucanId
	 */
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
	 * @return the sequenceFormat
	 */
	public GlycanSequenceFormat getSequenceFormat() {
		return sequenceFormat;
	}
	/**
	 * @param sequenceFormat the sequenceFormat to set
	 */
	public void setSequenceFormat(GlycanSequenceFormat sequenceFormat) {
		this.sequenceFormat = sequenceFormat;
	}
}
