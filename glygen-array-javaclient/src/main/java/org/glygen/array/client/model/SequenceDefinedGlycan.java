package org.glygen.array.client.model;


public class SequenceDefinedGlycan extends MassOnlyGlycan {
	String glytoucanId;
	String sequence;
	GlycanSequenceFormat sequenceType;
	
	public SequenceDefinedGlycan() {
		this.type = GlycanType.SEQUENCE_DEFINED;
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
	public GlycanSequenceFormat getSequenceType() {
		return sequenceType;
	}
	/**
	 * @param sequenceType the sequenceType to set
	 */
	public void setSequenceType(GlycanSequenceFormat sequenceType) {
		this.sequenceType = sequenceType;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SequenceDefinedGlycan))
			return super.equals(obj);
		if (sequence != null) { // check if sequences are the same
			return sequence.equals(((SequenceDefinedGlycan)obj).getSequence());
		}
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		if (sequence != null) 
			return sequence.hashCode();
		
		return super.hashCode();
	}
}
