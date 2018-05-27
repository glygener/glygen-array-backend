package org.glygen.array.exception;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

@SuppressWarnings("serial")
@ResponseStatus(value=HttpStatus.NOT_FOUND, reason="User does not exist")  // 404
public class UserNotFoundException extends RuntimeException {
	public UserNotFoundException() { super(); }
	public UserNotFoundException(String s) { super(s); }
	public UserNotFoundException(String s, Throwable throwable) { super(s, throwable); }
	public UserNotFoundException(Throwable throwable) { super(throwable); }
}
