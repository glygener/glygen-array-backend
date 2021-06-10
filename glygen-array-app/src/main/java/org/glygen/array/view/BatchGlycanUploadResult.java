package org.glygen.array.view;

import java.util.ArrayList;
import java.util.List;

import org.glygen.array.persistence.rdf.Glycan;

public class BatchGlycanUploadResult {
	
	List<String> wrongSequences = new ArrayList<>();
	List<Glycan> duplicateSequences = new ArrayList<Glycan>();
	String successMessage;
	List<Glycan> addedGlycans = new ArrayList<Glycan>();
	
	public void addWrongSequence (String seq) {
		if (!wrongSequences.contains(seq))
			wrongSequences.add(seq);
	}
	
	public void addDuplicateSequence (Glycan seq) {
		if (!duplicateSequences.contains(seq))
			duplicateSequences.add(seq);
	}
	
	public void setSuccessMessage(String successMessage) {
		this.successMessage = successMessage;
	}
	
	public String getSuccessMessage() {
		return successMessage;
	}
	
	public List<String> getWrongSequences() {
		return wrongSequences;
	}
	
	public void setWrongSequences(List<String> wrongSequences) {
		this.wrongSequences = wrongSequences;
	}
	
	public List<Glycan> getDuplicateSequences() {
		return duplicateSequences;
	}
	
	public void setDuplicateSequences(List<Glycan> duplicateSequences) {
		this.duplicateSequences = duplicateSequences;
	}
	
	public void setAddedGlycans(List<Glycan> addedGlycans) {
		this.addedGlycans = addedGlycans;
	}
	
	public List<Glycan> getAddedGlycans() {
		return addedGlycans;
	}

}
