package org.glygen.array.client.model;

import java.util.ArrayList;
import java.util.List;

public class ImportGRITSLibraryResult {
	
	List<SlideLayout> addedLayouts = new ArrayList<SlideLayout>();
	List<SlideLayout> duplicates = new ArrayList<SlideLayout>();
	List<SlideLayout> errors = new ArrayList<SlideLayout>();
	
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
	public List<SlideLayout> getErrors() {
		return errors;
	}

	/**
	 * @param errors the errors to set
	 */
	public void setErrors(List<SlideLayout> errors) {
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
