package org.glygen.array.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.validation.constraints.NotEmpty;
import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.view.validation.EmailWithTld;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Entity(name="feedback")
@XmlRootElement (name="feedback")
@JsonSerialize
public class FeedbackEntity {
    
    Long id;
    String firstName;
    String lastName;
    String email;
    String page;
    String subject;
    String message;
    
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="token_seq")
    @SequenceGenerator(name="token_seq", sequenceName="TOKEN_SEQ", allocationSize=1)
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    /**
     * @return the firstName
     */
    @Column(name="firstname", nullable=false)
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
    @Column(name="lastname", nullable=true)
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
    @Column(name="email", nullable=false)
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
    @Column(name="page", nullable=false)
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
    @Column(name="subject", nullable=true)
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
    @Column(name="message", nullable=false)
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

}
