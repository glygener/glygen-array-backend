package org.glygen.array.service;

import java.util.UUID;

import org.glygen.array.persistence.SettingEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.SettingsRepository;
import org.glygen.array.persistence.dao.VerificationTokenRepository;
import org.glygen.array.util.RandomPasswordGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailManagerImpl implements EmailManager {
	
	@Value("${glygen.frontend.basePath}")
	String frontEndbasePath;
	
	@Value("${glygen.frontend.host}")
	String frontEndHost;
	
	@Value("${glygen.frontend.scheme}")
	String frontEndScheme;
	
	@Value("${glygen.frontend.emailVerificationPage}")
	String emailVerificationPage;
	
	@Autowired
	UserManager userManager;
	
	@Autowired
	VerificationTokenRepository tokenRepository;
	
	@Autowired
	SettingsRepository settingsRepository;
	
	@Autowired
    private JavaMailSender mailSender;

    private String username;
    private String password;
    
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

    @Override
    @Transactional(value="jpaTransactionManager")
    public void sendPasswordReminder(UserEntity user) {
    	init(); // if username/password have not been initialized, this will get them from DB
        
        // this criteria should match with the PasswordValidator's criteria
        // password should have minimum of 5, max of 12 characters, 1 numeric, 1 special character, 1 capital letter and 1 lowercase letter at least
        char[] pswd = RandomPasswordGenerator.generatePswd(5, 20, 1, 1, 1);
        String newPassword = new String(pswd);
        // encrypt the password
 		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
 		String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(hashedPassword);
        userManager.createUser(user);
     // Create a thread safe "copy" of the template message and customize it
        SimpleMailMessage msg = constructPasswordReminderMessage(user);
        msg.setTo(user.getEmail());
        msg.setText(
            "Dear " + user.getFirstName() + " " + user.getLastName()
                + ", \n\nYour Glygen password is reset. This is your temporary password: \n\n" + new String(pswd) 
            	+ "\n\nPlease change it as soon as possible. \n\nGlygen.org");
        this.mailSender.send(msg);
    }

	private SimpleMailMessage constructPasswordReminderMessage(UserEntity user) {
        final String recipientAddress = user.getEmail();
        final String subject = "Password Reset";
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipientAddress);
        email.setSubject(subject);
        email.setFrom(this.username);
        return email;
	}


	@Override
	public void sendVerificationToken(UserEntity user) {
		init(); // if username/password have not been initialized, this will get them from DB
		
		final String token = UUID.randomUUID().toString();
        userManager.createVerificationTokenForUser(user, token);
        final SimpleMailMessage email = constructVerificationEmailMessage(user, token);
        mailSender.send(email);
    }

    private final SimpleMailMessage constructVerificationEmailMessage(final UserEntity user, final String token) {
    	String verificationURL = frontEndScheme + frontEndHost + frontEndbasePath + emailVerificationPage;
        final String recipientAddress = user.getEmail();
        final String subject = "Registration Confirmation";
        final String confirmationUrl = verificationURL+ "?token=" + token;
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
