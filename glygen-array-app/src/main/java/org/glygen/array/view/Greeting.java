package org.glygen.array.view;

import java.util.Date;

public class Greeting {
	final String message;
	final String title;
	final Date date;
	
	public Greeting(String message, String title, Date date) {
		this.message = message;
		this.title = title;
		this.date = date;
	}
	
	public String getMessage() {
		return message;
	}
	
	public String getTitle() {
		return title;
	}
	
	public Date getDate() {
		return date;
	}
}
