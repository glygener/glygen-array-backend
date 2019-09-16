package org.glygen.array.view;

import java.time.LocalDate;

public class WebLogger {
	
	private Long log_id;
	private String user;
	private String page;
	private String session_id;
	private String event_type;
	private String params;
	private String info;
	private String comments;
	private LocalDate log_timestamp;
	
	/**
	 * @return the log_id
	 */
	public Long getLog_id() {
		return log_id;
	}
	/**
	 * @param log_id the log_id to set
	 */
	public void setLog_id(Long log_id) {
		this.log_id = log_id;
	}
	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}
	/**
	 * @param user the user to set
	 */
	public void setUser(String user) {
		this.user = user;
	}
	/**
	 * @return the page
	 */
	public String getPage() {
		return page;
	}
	/**
	 * @param page the page to set
	 */
	public void setPage(String page) {
		this.page = page;
	}
	/**
	 * @return the session_id
	 */
	public String getSession_id() {
		return session_id;
	}
	/**
	 * @param session_id the session_id to set
	 */
	public void setSession_id(String session_id) {
		this.session_id = session_id;
	}
	/**
	 * @return the event_type
	 */
	public String getEvent_type() {
		return event_type;
	}
	/**
	 * @param event_type the event_type to set
	 */
	public void setEvent_type(String event_type) {
		this.event_type = event_type;
	}
	/**
	 * @return the params
	 */
	public String getParams() {
		return params;
	}
	/**
	 * @param params the params to set
	 */
	public void setParams(String params) {
		this.params = params;
	}
	/**
	 * @return the info
	 */
	public String getInfo() {
		return info;
	}
	/**
	 * @param info the info to set
	 */
	public void setInfo(String info) {
		this.info = info;
	}
	/**
	 * @return the comments
	 */
	public String getComments() {
		return comments;
	}
	/**
	 * @param comments the comments to set
	 */
	public void setComments(String comments) {
		this.comments = comments;
	}
	/**
	 * @return the log_timestamp
	 */
	public LocalDate getLog_timestamp() {
		return log_timestamp;
	}
	/**
	 * @param log_timestamp the log_timestamp to set
	 */
	public void setLog_timestamp(LocalDate log_timestamp) {
		this.log_timestamp = log_timestamp;
	}

}