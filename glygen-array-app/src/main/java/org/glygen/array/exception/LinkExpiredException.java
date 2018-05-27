package org.glygen.array.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@SuppressWarnings("serial")
@ResponseStatus(value=HttpStatus.EXPECTATION_FAILED, reason="Confirmation link has expired")  // 415
public class LinkExpiredException extends RuntimeException  {
	public LinkExpiredException() { super(); }
	public LinkExpiredException(String s) { super(s); }
	public LinkExpiredException(String s, Throwable throwable) { super(s, throwable); }
	public LinkExpiredException(Throwable throwable) { super(throwable); }
}
