package org.glygen.array.view;

import java.sql.Timestamp;

public class WebLogger {

	private String page;
	private String level;
	private String message;
	private String comment;
	private String user;
	
	public String getPage() {
		return page;
	}
	public void setPage(String page) {
		this.page = page;
	}
	public String getLevel() {
		return level;
	}
	public void setLevel(String level) {
		this.level = level;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
		
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
		
}
