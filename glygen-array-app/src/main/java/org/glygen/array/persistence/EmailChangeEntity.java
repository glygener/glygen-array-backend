package org.glygen.array.persistence;

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
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Entity(name="email")
@XmlRootElement (name="email")
@JsonSerialize
public class EmailChangeEntity {
	@Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="email_seq")
    @SequenceGenerator(name="email_seq", sequenceName="EMAIL_SEQ")
    private Long id;

    @OneToOne(targetEntity = UserEntity.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "userid", foreignKey = @ForeignKey(name = "FK_VERIFY_USER"))
    private UserEntity user;
    
    @Column(name="oldemail")
	String oldEmail;
    
    @Column(name="newemail")
	String newEmail;
	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
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
	 * @return the oldEmail
	 */
	public String getOldEmail() {
		return oldEmail;
	}
	/**
	 * @param oldEmail the oldEmail to set
	 */
	public void setOldEmail(String oldEmail) {
		this.oldEmail = oldEmail;
	}
	/**
	 * @return the newEmail
	 */
	public String getNewEmail() {
		return newEmail;
	}
	/**
	 * @param newEmail the newEmail to set
	 */
	public void setNewEmail(String newEmail) {
		this.newEmail = newEmail;
	}
	
}
