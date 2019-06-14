package org.glygen.array.exception;

@SuppressWarnings("serial")
public class UploadNotFinishedException extends RuntimeException {
	public UploadNotFinishedException() { super(); }
	public UploadNotFinishedException(String s) { super(s); }
	public UploadNotFinishedException(String s, Throwable throwable) { super(s, throwable); }
	public UploadNotFinishedException(Throwable throwable) { super(throwable); }
}