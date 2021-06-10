package org.glygen.array.client.model;

public class User {
	private String userName;
	private String password;
	private String firstName;
    private String lastName;
    private String email;
    private String affiliation;
    private String affiliationWebsite;
    private Boolean publicFlag;
    
	/**
	 * @return the userName
	 */
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

}
