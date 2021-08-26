package org.glygen.array.persistence.rdf;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SequenceDefinedGlycan extends MassOnlyGlycan {
	String glytoucanId;
	String sequence;
	GlycanSequenceFormat sequenceType;
	String glytoucanHash;
	
	public SequenceDefinedGlycan() {
		this.type = GlycanType.SEQUENCE_DEFINED;
	}
	
	/**
	 * @return the glyTouCanId
	 */
	@Size(min=8, max=11, message="GlytoucanId should be 8 characters long")
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
	
	@JsonIgnore
	public String getGlytoucanHash() {
        return glytoucanHash;
    }
	
	public void setGlytoucanHash(String glytoucanHash) {
        this.glytoucanHash = glytoucanHash;
    }
	
	public void copyTo (Glycan glycan) {
	    super.copyTo (glycan);
	    if (glycan instanceof SequenceDefinedGlycan) {
	        ((SequenceDefinedGlycan) glycan).glytoucanHash = this.glytoucanHash;
	        ((SequenceDefinedGlycan) glycan).glytoucanId = this.glytoucanId;
	        ((SequenceDefinedGlycan) glycan).sequence = this.sequence;
	        ((SequenceDefinedGlycan) glycan).sequenceType = this.sequenceType;
	    }
	}
}
