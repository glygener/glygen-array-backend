package org.glygen.array.exception;

public class SparqlException extends Exception {
	protected String msg;

	/**
	 * @param msg
	 */
	public SparqlException(String msg) {
		super(msg);
		this.msg = msg;
	}

	/**
	 * @param throwable 
	 */
	public SparqlException(Throwable throwable) {
		super(throwable);
	}
	
	/**
	 * @param msg
	 * @param throwable 
	 */
	public SparqlException(String msg,Throwable throwable) {
		super(msg,throwable);
		this.msg = msg;
	}

	public String getErrorMessage()
	{
		return this.msg;
	}

	private static final long serialVersionUID = 1L;
}