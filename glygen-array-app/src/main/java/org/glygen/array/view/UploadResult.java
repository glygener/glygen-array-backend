package org.glygen.array.view;

public class UploadResult {
	
	int statusCode;
	String assignedFileName;
	
	public String getAssignedFileName() {
		return assignedFileName;
	}
	
	public void setAssignedFileName(String assignedFileName) {
		this.assignedFileName = assignedFileName;
	}
	
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	
	public int getStatusCode() {
		return statusCode;
	}
}
