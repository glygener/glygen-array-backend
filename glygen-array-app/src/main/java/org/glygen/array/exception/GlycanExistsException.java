package org.glygen.array.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@SuppressWarnings("serial")
@ResponseStatus(value=HttpStatus.CONFLICT, reason = "Glycan already exists in the repository") 
public class GlycanExistsException extends RuntimeException {
	public GlycanExistsException() { super(); }
	public GlycanExistsException(String s) { super(s); }
	public GlycanExistsException(String s, Throwable throwable) { super(s, throwable); }
	public GlycanExistsException(Throwable throwable) { super(throwable); }
}