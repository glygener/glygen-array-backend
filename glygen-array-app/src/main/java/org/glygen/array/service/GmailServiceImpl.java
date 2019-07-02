package org.glygen.array.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.VerificationTokenRepository;
import org.glygen.array.util.RandomPasswordGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

@Service
public final class GmailServiceImpl implements EmailManager {

    private static final String APPLICATION_NAME = "Glygen";

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    
    @Value("${glygen.frontend.basePath}")
	String frontEndbasePath;
	
	@Value("${glygen.frontend.host}")
	String frontEndHost;
	
	@Value("${glygen.frontend.scheme}")
	String frontEndScheme;
	
	@Value("${glygen.frontend.emailVerificationPage}")
	String emailVerificationPage;
	
	@Value("${google.gmail.email}")
	String email;
	
	@Value("${google.gmail.client-id}")
	String clientId;
	
	@Value("${google.gmail.client-secret}")
	String clientSecret;
	
	@Value("${google.gmail.access-token}")
	String accessToken;
	
	@Value("${google.gmail.refresh-token}")
	String refreshToken;

	@Value("${google.gmail.email}")
	private String username;
	

	@Autowired
	UserManager userManager;
	
	@Autowired
	VerificationTokenRepository tokenRepository;
	
	final HttpTransport httpTransport;
	
	
	
	public GmailServiceImpl() {
		try {
			this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		} catch (GeneralSecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}
    
   /* public void init () {
    	if (username == null && password == null) { // load them from db the first time
    	
    		SettingEntity userNameSetting = settingsRepository.findByName("server.email");
    		SettingEntity passwordSetting = settingsRepository.findByName("server.email.password");
    		if (userNameSetting != null && passwordSetting != null) {
    			username = userNameSetting.getValue();
    			password = passwordSetting.getValue();
    			StandardPBEStringEncryptor decryptor = new StandardPBEStringEncryptor();
    	        decryptor.setPassword(JASYPT_SECRET);
    	        password = decryptor.decrypt(password);
    		
    		} else {
    			throw new RuntimeException("Internal Server Error: email server settings are not in the database");
    		}
    	}
    }*/

    
    public boolean sendMessage(String recipientAddress, String subject, String body) throws MessagingException,
            IOException {
        Message message = createMessageWithEmail(
                createEmail(recipientAddress, username, subject, body));

        return createGmail().users()
                .messages()
                .send(username, message)
                .execute()
                .getLabelIds().contains("SENT");
    }

    private Gmail createGmail() {
        Credential credential = authorize();
        return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private MimeMessage createEmail(String to, String from, String subject, String bodyText) throws MessagingException {
        MimeMessage email = new MimeMessage(Session.getDefaultInstance(new Properties(), null));
        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }

    private Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);

        return new Message()
                .setRaw(Base64.encodeBase64URLSafeString(buffer.toByteArray()));
    }

    private Credential authorize() {
        return new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken);
    }
    
    @Override
	public void sendUserName(UserEntity user) {
		final String recipientAddress = user.getEmail();
		final String subject = "UserName Recovery";
		try {
				sendMessage(recipientAddress, subject,  "Dear " + user.getFirstName() + " " + user.getLastName()
                + ", \n\n Your Username is: \n\n" + user.getUsername());
			} catch (MessagingException | IOException e) {
				throw new RuntimeException("Cannot send email.", e);
			}
	}

	@Override
	public void sendPasswordReminder(UserEntity user) {
		//init(); // if username/password have not been initialized, this will get them from DB
        
        // this criteria should match with the PasswordValidator's criteria
        // password should have minimum of 5, max of 12 characters, 1 numeric, 1 special character, 1 capital letter and 1 lowercase letter at least
        char[] pswd = RandomPasswordGenerator.generatePswd(5, 20, 1, 1, 1);
        String newPassword = new String(pswd);
        // encrypt the password
        PasswordEncoder passwordEncoder =
    		    PasswordEncoderFactories.createDelegatingPasswordEncoder();
 		String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(hashedPassword);
        userManager.createUser(user);
        
        final String recipientAddress = user.getEmail();
        final String subject = "Password Reset";
        
        try {
			sendMessage(recipientAddress, subject, "Dear " + user.getFirstName() + " " + user.getLastName()
			+ ", \n\nYour Glygen password is reset. This is your temporary password: \n\n" + new String(pswd) 
			+ "\n\nPlease change it as soon as possible. \n\nGlygen.org");
		} catch (MessagingException | IOException e) {
			throw new RuntimeException("Cannot send email.", e);
		}
	}

	@Override
	public void sendVerificationToken(UserEntity user) {
		//init(); // if username/password have not been initialized, this will get them from DB
		
		final String token = UUID.randomUUID().toString();
        userManager.createVerificationTokenForUser(user, token);
		String verificationURL = frontEndScheme + frontEndHost + frontEndbasePath + emailVerificationPage;
        final String recipientAddress = user.getEmail();
        final String subject = "Registration Confirmation";
        final String confirmationUrl = verificationURL+ "?token=" + token;
        final String message = "Click on the link below to verify your email";
	    
        try {
			sendMessage(recipientAddress, subject, message + " \r\n" + confirmationUrl);
		} catch (MessagingException | IOException e) {
			throw new RuntimeException("Cannot send email.", e);
		}
	}

	@Override
	public void sendEmailChangeNotification(UserEntity user) {
		final String recipientAddress = user.getEmail();
		final String subject = "Email Change Notification";
	        
		try {
			sendMessage(recipientAddress, subject,  "Dear " + user.getFirstName() + " " + user.getLastName()
                + ", \n\nYour Glygen account email has been changed. If you have not made this change, please contact us at " + email);
		} catch (MessagingException | IOException e) {
			throw new RuntimeException("Cannot send email.", e);
		}
	}
}