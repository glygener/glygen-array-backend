package org.glygen.array.view;

import javax.validation.constraints.NotEmpty;

import org.glygen.array.view.validation.EmailWithTld;

import io.swagger.v3.oas.annotations.media.Schema;

public class FeedbackView {
	String firstName;
    String lastName;
    String email;
    String page;
    String subject;
    String message;
    String website;
    
    /**
     * @return the firstName
     */
    @NotEmpty
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
     * @return the page
     */
    @NotEmpty
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
     * @return the subject
     */
    public String getSubject() {
        return subject;
    }
    /**
     * @param subject the subject to set
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }
    /**
     * @return the message
     */
    @NotEmpty
    public String getMessage() {
        return message;
    }
    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Schema(description = "Reserved. Must be left empty or omitted.")
    public String getWebsite() {
		return website;
	}
    
    public void setWebsite(String website) {
		this.website = website;
	}
}
