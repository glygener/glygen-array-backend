package org.glygen.array.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.springframework.validation.ObjectError;

import com.fasterxml.jackson.annotation.JsonIgnore;

@XmlRootElement(name="result")
public class ErrorMessage extends Error {
	private static final long serialVersionUID = 1L;
	final static String status = "error";
	
	/**
	 * @return the statusCode
	 */
	@XmlAttribute(name="statusCode")
	public int getStatusCode() {
		return statusCode;
	}

	private List<ObjectError> errors;
	private int statusCode;
	private ErrorCodes errorCode;
	
	public ErrorMessage() {
	}
	 
	public ErrorMessage(String message) {
		super(message);
	}
	 
	public ErrorMessage(List<ObjectError> errors) {
		this.errors = errors;
	}
	 
	public ErrorMessage(ObjectError error) {
		this(Collections.singletonList(error));
	}
	 
	public ErrorMessage(ObjectError ... errors) {
		this(Arrays.asList(errors));
	}
	
	@XmlElement(name="error")
	public List<ObjectError> getErrors() {
		return errors;
	}
	 
	public void setErrors(List<ObjectError> errors) {
		this.errors = errors;
	}
	
	public void addError(ObjectError error) {
		if (this.errors == null)
			this.errors = new ArrayList<>();
		
		if (!this.errors.contains(error))
		    this.errors.add(error);
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
		String errorsString = getMessage() + " ";
		if (errors != null) {
			for (Iterator<ObjectError> iterator = errors.iterator(); iterator.hasNext();) {
				ObjectError error = (ObjectError) iterator.next();
				errorsString += error.toString() ;
				if (iterator.hasNext()) {
					errorsString += ", ";
				}
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
	
	@Override
	@XmlTransient
	@JsonIgnore
	public StackTraceElement[] getStackTrace() {
		return super.getStackTrace();
	}
}