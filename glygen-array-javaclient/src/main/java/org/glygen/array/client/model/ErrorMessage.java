package org.glygen.array.client.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="result")
public class ErrorMessage {
	final static String status = "error";
	
	/**
	 * @return the statusCode
	 */
	@XmlAttribute(name="statusCode")
	public int getStatusCode() {
		return statusCode;
	}

	private List<String> errors;
	private int statusCode;
	private ErrorCodes errorCode;
	 
	public ErrorMessage() {
	}
	 
	public ErrorMessage(List<String> errors) {
		this.errors = errors;
	}
	 
	public ErrorMessage(String error) {
		this(Collections.singletonList(error));
	}
	 
	public ErrorMessage(String ... errors) {
		this(Arrays.asList(errors));
	}
	
	@XmlElement(name="error")
	public List<String> getErrors() {
		return errors;
	}
	 
	public void setErrors(List<String> errors) {
		this.errors = errors;
	}
	
	/**
	 * @return the status
	 */
	@XmlAttribute(name="status")
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(int status) {
		this.statusCode = status;
	}

	@Override
	public String toString() {
		String errorsString ="";
		for (Iterator<String> iterator = errors.iterator(); iterator.hasNext();) {
			String error = (String) iterator.next();
			errorsString += error ;
			if (iterator.hasNext()) {
				errorsString += ", ";
			}
		}
		return errorsString;
	}

	/**
	 * @return the errorCode
	 */
	@XmlAttribute
	public ErrorCodes getErrorCode() {
		return errorCode;
	}

	/**
	 * @param errorCode the errorCode to set
	 */
	public void setErrorCode(ErrorCodes errorCode) {
		this.errorCode = errorCode;
	}
}