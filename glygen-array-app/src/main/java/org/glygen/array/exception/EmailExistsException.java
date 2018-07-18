package org.glygen.array.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@SuppressWarnings("serial")
@ResponseStatus(value=HttpStatus.CONFLICT, reason="An account with the given email already exists")  // 409
public class EmailExistsException extends RuntimeException {
	public EmailExistsException() { super(); }
	public EmailExistsException(String s) { super(s); }
	public EmailExistsException(String s, Throwable throwable) { super(s, throwable); }
	public EmailExistsException(Throwable throwable) { super(throwable); }
}
