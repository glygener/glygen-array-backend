package org.glygen.array.persistence;

import java.time.LocalDate;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name="web_log_access")
public class WebAccessLoggingEntity {

	@Id
    @Column(name="log_id", unique = true, nullable = false)
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="web_acc_seq")
    @SequenceGenerator(name="web_acc_seq", sequenceName="web_acc_id_seq", initialValue=1, allocationSize=1)
	private Long log_id;
	
	@OneToOne(targetEntity = UserEntity.class, fetch = FetchType.EAGER, cascade=CascadeType.MERGE)
    @JoinColumn(nullable = false, name = "userid", foreignKey = @ForeignKey(name = "FK_LOG_ACCESS_USER"))
	private UserEntity user;
	
	@Column(name="page", nullable = false)
	private String page;
	
	@Column(name="session_id", nullable = false)
	private String session_id;
	
	@Column(name="log_timestamp", nullable = false)
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
	public UserEntity getUser() {
		return user;
	}

	/**
	 * @param user the user to set
	 */
	public void setUser(UserEntity user) {
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
