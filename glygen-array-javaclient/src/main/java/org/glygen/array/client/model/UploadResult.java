package org.glygen.array.client.model;

import org.glygen.array.client.model.data.FileWrapper;

public class UploadResult {
	
	int statusCode;
	FileWrapper file;
	
	/**
	 * this is the assigned filename (unique) for the given upload file, use this to access this file later
	 * 
	 * @return the generated filename 
	 */
	public String getAssignedFileName() {
	    if (file != null)
	        return file.getIdentifier();
	    return null;
	}
	
	public void setFile(FileWrapper file) {
        this.file = file;
    }
	
	public FileWrapper getFile() {
        return file;
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
