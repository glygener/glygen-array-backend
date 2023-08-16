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

import org.glygen.array.persistence.FeedbackEntity;
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
		final String subject = "GlyGen array data repository: Username recovery";
		try {
				sendMessage(recipientAddress, subject,  "Dear " + user.getFirstName() + " " + user.getLastName()
                + ", \n\n The Username for your account associated with this email is: \n\n" + user.getUsername()
                + "\n\n If you did not request your username, please ignore this email. \n\n "
                + "The GlyGen array data repository team");
			} catch (MessagingException | IOException e) {
				throw new RuntimeException("Cannot send email.", e);
			}
	}
    
    @Override
    public void sendFeedback(FeedbackEntity feedback, String... emails) {
        if (emails == null) {
            throw new IllegalArgumentException("email list cannot be null");
        }
        String subject = "Glycan Array Repository " + feedback.getSubject();
        String message = "Feedback received for page: " + feedback.getPage() + "\nwith the message: " + feedback.getMessage();
        message += "\nFrom: " + feedback.getFirstName() + 
                " " + (feedback.getLastName() == null || feedback.getLastName().equals("not given") ? "" : feedback.getLastName()) + "\nEmail: " + feedback.getEmail();
        for (String email: emails) {
           try {
               sendMessage(email, subject, message);
           } catch (MessagingException | IOException e) {
               throw new RuntimeException("Cannot send feedback.", e);
           }
        }
        
    }

    @Override
    public void sendFeedbackNotice(FeedbackEntity feedback) {
        String subject = "Feedback received";
        String message = "Your feedback for the Glycan Array Repository has been recorded. Thank you!";
        message += "\n\nFeedback received for page: " + feedback.getPage() + "\nwith the message: " + feedback.getMessage();
        message += "\nFrom: " + feedback.getFirstName() + 
                " " + (feedback.getLastName() == null || feedback.getLastName().equals("not given") ? "" : feedback.getLastName()) + "\nEmail: " + feedback.getEmail();
        try {
            sendMessage(feedback.getEmail(), subject, message);
        } catch (MessagingException | IOException e) {
            throw new RuntimeException("Cannot send feedback notification to user email " + feedback.getEmail(), e);
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
        final String subject = "GlyGen array data repository: Password recovery";
        
        try {
			sendMessage(recipientAddress, subject, "Dear " + user.getFirstName() + " " + user.getLastName()
			+ ", \n\nYour Glygen array data repository password has been reset to: \n\n" + new String(pswd) 
			+ "\n\nPlease login to the respository and change the password as soon as possible. \n\nThe Glygen array data repository team");
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
        final String subject = "GlyGen array repository account activation";
        final String confirmationUrl = verificationURL+ "/" + token;
        String message = "Dear " + user.getFirstName() + " " + user.getLastName();
        message += "\n\nThank you for signing up to the GlyGen array data repository. If you have not created an account for your email address (" 
                + user.getEmail() + ") you can ignore this message. If you did create the account, please use the following link to activate the account:"
                + "\n\n" + confirmationUrl + "\n\nAlternatively, you can use the following activation code in the web frontend:"
                + "\n\n" + token + "\n\nThe GlyGen array repository Team";
         
        try {
			sendMessage(recipientAddress, subject, message);
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