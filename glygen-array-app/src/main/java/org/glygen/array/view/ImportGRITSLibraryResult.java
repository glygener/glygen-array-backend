package org.glygen.array.view;

import java.util.ArrayList;
import java.util.List;

import org.glygen.array.persistence.rdf.SlideLayout;

public class ImportGRITSLibraryResult {
	
	List<SlideLayout> addedLayouts = new ArrayList<SlideLayout>();
	List<SlideLayout> duplicates = new ArrayList<SlideLayout>();
	List<SlideLayoutError> errors = new ArrayList<>();
	
	String successMessage;

	/**
	 * @return the addedLayouts
	 */
	public List<SlideLayout> getAddedLayouts() {
		return addedLayouts;
	}

	/**
	 * @param addedLayouts the addedLayouts to set
	 */
	public void setAddedLayouts(List<SlideLayout> addedLayouts) {
		this.addedLayouts = addedLayouts;
	}

	/**
	 * @return the duplicates
	 */
	public List<SlideLayout> getDuplicates() {
		return duplicates;
	}

	/**
	 * @param duplicates the duplicates to set
	 */
	public void setDuplicates(List<SlideLayout> duplicates) {
		this.duplicates = duplicates;
	}

	/**
	 * @return the errors
	 */
	public List<SlideLayoutError> getErrors() {
		return errors;
	}

	/**
	 * @param errors the errors to set
	 */
	public void setErrors(List<SlideLayoutError> errors) {
		this.errors = errors;
	}

	/**
	 * @return the successMessage
	 */
	public String getSuccessMessage() {
		return successMessage;
	}

	/**
	 * @param successMessage the successMessage to set
	 */
	public void setSuccessMessage(String successMessage) {
		this.successMessage = successMessage;
	}
}
