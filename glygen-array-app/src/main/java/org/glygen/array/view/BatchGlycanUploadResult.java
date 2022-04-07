package org.glygen.array.view;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.springframework.http.HttpStatus;
import org.springframework.validation.ObjectError;

@XmlRootElement
public class BatchGlycanUploadResult {
	
	List<GlycanUploadError> wrongSequences = new ArrayList<>();
	List<Glycan> duplicateSequences = new ArrayList<Glycan>();
	String successMessage;
	List<Glycan> addedGlycans = new ArrayList<Glycan>();
	
	public void addWrongSequence (GlycanUploadError seq) {
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
	
	public List<GlycanUploadError> getWrongSequences() {
		return wrongSequences;
	}
	
	public void setWrongSequences(List<GlycanUploadError> wrongSequences) {
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

    public void addWrongSequence(String id, int count, String sequence, 
            String message) {
        GlycanUploadError error = new GlycanUploadError();
        if (sequence != null) {
            Glycan g = new SequenceDefinedGlycan();
            ((SequenceDefinedGlycan) g).setSequence (sequence);
            g.setInternalId(id);
            g.setId(id);
            error.setGlycan(g);
        } else {
            Glycan g = new Glycan();
            g.setInternalId(id);
            g.setId(id);
            error.setGlycan(g);
        }
        ErrorMessage errorMessage = new ErrorMessage(message);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        String[] codes = new String[] {count+""};
        errorMessage.addError(new ObjectError("sequence", codes, null, message));
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        error.setError(errorMessage);
        addWrongSequence(error);
    }
}
