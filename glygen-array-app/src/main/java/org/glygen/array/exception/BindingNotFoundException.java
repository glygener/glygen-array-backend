package org.glygen.array.exception;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

@SuppressWarnings("serial")
@ResponseStatus(value=HttpStatus.NOT_FOUND, reason="Binding does not exist")  // 404
public class BindingNotFoundException extends RuntimeException {
	public BindingNotFoundException() { super(); }
	public BindingNotFoundException(String s) { super(s); }
	public BindingNotFoundException(String s, Throwable throwable) { super(s, throwable); }
	public BindingNotFoundException(Throwable throwable) { super(throwable); }
}
