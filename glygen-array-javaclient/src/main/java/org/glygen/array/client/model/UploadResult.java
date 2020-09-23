package org.glygen.array.client.model;

public class UploadResult {
	
	int statusCode;
	String assignedFileName;
	
	/**
	 * this is the assigned filename (unique) for the given upload file, use this to access this file later
	 * 
	 * @return the generated filename 
	 */
	public String getAssignedFileName() {
		return assignedFileName;
	}
	
	public void setAssignedFileName(String assignedFileName) {
		this.assignedFileName = assignedFileName;
	}
	
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	
	/**
	 * Http status code
	 * @return HttpStatus code
	 */
	public int getStatusCode() {
		return statusCode;
	}
}
