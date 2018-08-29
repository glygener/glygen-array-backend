package org.glygen.array.persistence;

import java.sql.Date;
import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name="web_logging_access")
public class WebAccessLoggingEntity {

	@Id
    @Column(name="event_id", unique = true, nullable = false)
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="web_access_seq")
    @SequenceGenerator(name="web_access_seq", sequenceName="web_access_id_seq", initialValue=1, allocationSize=1)
	private Long eventId;
	
	@Column(name="dates", nullable = false)
	private LocalDate date;
	
	@Column(name="level_string", nullable = false)
	private String levelString;
	
	@Column(name="page", nullable = false)
	private String page;
		
	@Column(name="message", nullable = false)
	private String message;
	
	@Column(name="comment")
	private String comment;
	
	@Column(name="caller_user", nullable = false)
	private String user;

	public Long getEventId() {
		return eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public String getLevelString() {
		return levelString;
	}

	public void setLevelString(String levelString) {
		this.levelString = levelString;
	}

	public String getPage() {
		return page;
	}

	public void setPage(String page) {
		this.page = page;
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
