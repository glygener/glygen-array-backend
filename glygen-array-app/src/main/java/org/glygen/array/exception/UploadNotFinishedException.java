package org.glygen.array.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@SuppressWarnings("serial")
@ResponseStatus(value=HttpStatus.PARTIAL_CONTENT, reason="The task has been interrupted") 
public class UploadNotFinishedException extends RuntimeException {
	public UploadNotFinishedException() { super(); }
	public UploadNotFinishedException(String s) { super(s); }
	public UploadNotFinishedException(String s, Throwable throwable) { super(s, throwable); }
	public UploadNotFinishedException(Throwable throwable) { super(throwable); }
}