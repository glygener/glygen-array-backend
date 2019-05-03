package org.glygen.array.view;

import java.util.ArrayList;
import java.util.List;

public class BatchGlycanUploadResult {
	
	List<String> wrongSequences = new ArrayList<>();
	List<String> duplicateSequences = new ArrayList<String>();
	String successMessage;
	
	public void addWrongSequence (String seq) {
		if (!wrongSequences.contains(seq))
			wrongSequences.add(seq);
	}
	
	public void addDuplicateSequence (String seq) {
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
	
	public List<String> getDuplicateSequences() {
		return duplicateSequences;
	}
	
	public void setDuplicateSequences(List<String> duplicateSequences) {
		this.duplicateSequences = duplicateSequences;
	}

}
