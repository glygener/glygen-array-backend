package org.glygen.array.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR, reason="Cannot connect to the repository")  // 500
@SuppressWarnings("serial")
public class GlycanRepositoryException extends RuntimeException {
	public GlycanRepositoryException() { super(); }
	public GlycanRepositoryException(String s) { super(s); }
	public GlycanRepositoryException(String s, Throwable throwable) { super(s, throwable); }
	public GlycanRepositoryException(Throwable throwable) { super(throwable); }
}
