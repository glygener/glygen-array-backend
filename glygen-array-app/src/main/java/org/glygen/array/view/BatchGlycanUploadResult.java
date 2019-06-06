package org.glygen.array.view;

import java.util.ArrayList;
import java.util.List;

public class BatchGlycanUploadResult {
	
	List<String> wrongSequences = new ArrayList<>();
	List<GlycanView> duplicateSequences = new ArrayList<GlycanView>();
	String successMessage;
	List<GlycanView> addedGlycans = new ArrayList<GlycanView>();
	
	public void addWrongSequence (String seq) {
		if (!wrongSequences.contains(seq))
			wrongSequences.add(seq);
	}
	
	public void addDuplicateSequence (GlycanView seq) {
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
	
	public List<GlycanView> getDuplicateSequences() {
		return duplicateSequences;
	}
	
	public void setDuplicateSequences(List<GlycanView> duplicateSequences) {
		this.duplicateSequences = duplicateSequences;
	}
	
	public void setAddedGlycans(List<GlycanView> addedGlycans) {
		this.addedGlycans = addedGlycans;
	}
	
	public List<GlycanView> getAddedGlycans() {
		return addedGlycans;
	}

}
