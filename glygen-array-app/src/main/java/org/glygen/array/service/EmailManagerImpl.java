package org.glygen.array.service;

import java.util.UUID;

import org.glygen.array.persistence.SettingEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.SettingsRepository;
import org.glygen.array.persistence.dao.VerificationTokenRepository;
import org.glygen.array.util.RandomPasswordGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConfigurationProperties(prefix="glygen")
public class EmailManagerImpl implements EmailManager {
	String baseURL;
	
	@Autowired
	UserManager userManager;
	
	@Autowired
	VerificationTokenRepository tokenRepository;
	
	@Autowired
	SettingsRepository settingsRepository;
	
	@Autowired
    private JavaMailSender mailSender;

    private SimpleMailMessage templateMessage;
    private String username;
    private String password;
    
    public String getBaseURL() {
		return baseURL;
	}
    
    public void setBaseURL(String baseURL) {
		this.baseURL = baseURL;
	}
    
    public void init () {
    	if (username == null && password == null) { // load them from db the first time
    	
    		SettingEntity userNameSetting = settingsRepository.findByName("server.email");
    		SettingEntity passwordSetting = settingsRepository.findByName("server.email.password");
    		if (userNameSetting != null && passwordSetting != null) {
    			username = userNameSetting.getValue();
    			password = passwordSetting.getValue();
    		
    			((JavaMailSenderImpl)this.mailSender).setPassword(password);
    			((JavaMailSenderImpl)this.mailSender).setUsername(username);
    			/*Properties props = ((JavaMailSenderImpl) mailSender).getJavaMailProperties();
    		    props.put("mail.transport.protocol", "smtp");
    		    props.put("mail.smtp.auth", "true");
    		    props.put("mail.smtp.starttls.enable", "true");
    		    props.put("mail.debug", "true");*/
    			//this.templateMessage.setFrom(username);
    		} else {
    			throw new RuntimeException("Internal Server Error: email server settings are not in the database");
    		}
    	}
    }


    public void setTemplateMessage(SimpleMailMessage templateMessage) {
        this.templateMessage = templateMessage;
    }

    @Override
    @Transactional
    public void sendPasswordReminder(UserEntity user) {
    	init(); // if username/password have not been initialized, this will get them from DB
        
    	// Create a thread safe "copy" of the template message and customize it
        SimpleMailMessage msg = new SimpleMailMessage(this.templateMessage);
        msg.setTo(user.getEmail());
        
        // this criteria should match with the PasswordValidator's criteria
        // password should have minimum of 5, max of 12 characters, 1 numeric, 1 special character, 1 capital letter and 1 lowercase letter at least
        char[] pswd = RandomPasswordGenerator.generatePswd(5, 12, 1, 1, 1);
        String newPassword = new String(pswd);
        // encrypt the password
 		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
 		String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(hashedPassword);
        userManager.createUser(user);
        msg.setText(
            "Dear " + user.getUsername()
                + ", \n\nYour Glygen password is reset. This is your temporary password: \n\n" + new String(pswd) 
            	+ "\n\nPlease change it as soon as possible. \n\nGlygen.org");
        this.mailSender.send(msg);
    }

	@Override
	public void sendVerificationToken(UserEntity user) {
		init(); // if username/password have not been initialized, this will get them from DB
		
		final String token = UUID.randomUUID().toString();
        userManager.createVerificationTokenForUser(user, token);
        final SimpleMailMessage email = constructEmailMessage(user, token);
        mailSender.send(email);
    }

    private final SimpleMailMessage constructEmailMessage(final UserEntity user, final String token) {
        final String recipientAddress = user.getEmail();
        final String subject = "Registration Confirmation";
        final String confirmationUrl = baseURL + "/registrationConfirm.html?token=" + token;
        //final String message = messages.getMessage("message.regSucc", null, event.getLocale());
        final String message = "Click on the link below to verify your email";
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipientAddress);
        email.setSubject(subject);
        email.setText(message + " \r\n" + confirmationUrl);
        email.setFrom(this.username);
        return email;
    }
}
