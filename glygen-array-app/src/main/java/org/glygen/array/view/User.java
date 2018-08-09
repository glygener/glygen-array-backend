package org.glygen.array.view;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import org.glygen.array.view.validation.EmailWithTld;
import org.glygen.array.view.validation.Password;
import org.glygen.array.view.validation.Username;


public class User {
	private String userName;
	private String password;
	private String firstName;
    private String lastName;
    private String email;
    private String affiliation;
    private String affiliationWebsite;
    private Boolean publicFlag;
    private String userType;
    
	/**
	 * @return the userName
	 */
    @NotEmpty
    @Username
    @Size(min=5, max=20, message="userName should have atleast 5 and at most 20 characters")
	public String getUserName() {
		return userName;
	}
	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}
	/**
	 * @return the password
	 */
	@NotEmpty
	@Password
	public String getPassword() {
		return password;
	}
	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	/**
	 * @return the firstName
	 */
	@Size(max=100, message="First name cannot exceed 100 characters")
	public String getFirstName() {
		return firstName;
	}
	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	/**
	 * @return the lastName
	 */
	@Size(max=100, message="Last name cannot exceed 100 characters")
	public String getLastName() {
		return lastName;
	}
	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	/**
	 * @return the email
	 */
	@NotEmpty
	@EmailWithTld
	public String getEmail() {
		return email;
	}
	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}
	/**
	 * @return the affiliation
	 */
	@Size(max=255, message="Affiliation cannot exceed 255 characters")
	public String getAffiliation() {
		return affiliation;
	}
	/**
	 * @param affiliation the affiliation to set
	 */
	public void setAffiliation(String affiliation) {
		this.affiliation = affiliation;
	}
	/**
	 * @return the affiliationWebsite
	 */
	@Size(max=255, message="Affiliation Website cannot exceed 255 characters")
	public String getAffiliationWebsite() {
		return affiliationWebsite;
	}
	/**
	 * @param affiliationWebsite the affiliationWebsite to set
	 */
	public void setAffiliationWebsite(String affiliationWebsite) {
		this.affiliationWebsite = affiliationWebsite;
	}
	/**
	 * @return the publicFlag
	 */
	public Boolean getPublicFlag() {
		return publicFlag;
	}
	/**
	 * @param publicFlag the publicFlag to set
	 */
	public void setPublicFlag(Boolean publicFlag) {
		this.publicFlag = publicFlag;
	}
	
	/**
	 * 
	 * @return the user type: LOCAL, GOOGLE etc.
	 */
	public String getUserType() {
		return userType;
	}
	
	/**
	 * 
	 * @param userType the userType to set
	 */
	public void setUserType(String userType) {
		this.userType = userType;
	}

}
