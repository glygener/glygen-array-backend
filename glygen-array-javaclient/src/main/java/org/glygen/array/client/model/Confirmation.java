package org.glygen.array.client.model;

public class Confirmation {
	final static String status = "success";
	
	int statusCode;
	String message;
	
	public Confirmation() {
	}
	
	public Confirmation(String message, int statusCode) {
		this.statusCode = statusCode;
		this.message = message;
	}	
	
	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	
	
	/**
	 * @return the statusCode
	 */
	public int getStatusCode() {
		return statusCode;
	}
	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}
}
