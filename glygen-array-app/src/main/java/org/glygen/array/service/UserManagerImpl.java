package org.glygen.array.service;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.glygen.array.exception.UserNotFoundException;
import org.glygen.array.persistence.EmailChangeEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.VerificationToken;
import org.glygen.array.persistence.dao.EmailRepository;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.dao.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(value="jpaTransactionManager")
public class UserManagerImpl implements UserManager {
	public static final String TOKEN_INVALID = "invalidToken";
    public static final String TOKEN_EXPIRED = "expired";
    public static final String TOKEN_VALID = "valid";
	
    @Autowired
    private UserRepository repository;

    @Autowired
    private VerificationTokenRepository tokenRepository;
    
    @Autowired
    private EmailRepository emailRepository;
 
    @Override
    public UserEntity getUserByToken(final String verificationToken) {
        final VerificationToken token = tokenRepository.findByToken(verificationToken);
        if (token != null) {
            return token.getUser();
        }
        return null;
    }

    @Override
    public VerificationToken getVerificationToken(final String VerificationToken) {
        return tokenRepository.findByToken(VerificationToken);
    }
    
    @Override
    public void createVerificationTokenForUser(final UserEntity user, final String token) {
        final VerificationToken myToken = new VerificationToken(token, user);
        tokenRepository.save(myToken);
    }

    @Override
    public VerificationToken generateNewVerificationToken(final String existingVerificationToken) {
        VerificationToken vToken = tokenRepository.findByToken(existingVerificationToken);
        vToken.updateToken(UUID.randomUUID().toString());
        vToken = tokenRepository.save(vToken);
        return vToken;
    }
    
    @Override
    public String validateVerificationToken(String token) {
        final VerificationToken verificationToken = tokenRepository.findByToken(token);
        if (verificationToken == null) {
            return TOKEN_INVALID;
        }

        final UserEntity user = verificationToken.getUser();
        final Calendar cal = Calendar.getInstance();
        if ((verificationToken.getExpiryDate().getTime() - cal.getTime().getTime()) <= 0) {
            tokenRepository.delete(verificationToken);
            if (user.getEnabled()) {
            	// do not delete the user, possible email change
            	// delete email change request, if any
            	EmailChangeEntity emailChange = emailRepository.findByUser(user);
            	if (emailChange != null) {
            		emailRepository.delete(emailChange);
            	}
            } else 
            	repository.delete(user);
            return TOKEN_EXPIRED;
        }
        
        if (user.getEnabled()) {
        	// already enabled, possible email change
        	EmailChangeEntity emailChange = emailRepository.findByUser(user);
        	if (emailChange != null) {
        		user.setEmail(emailChange.getNewEmail());
        		emailRepository.delete(emailChange);
        	}
        } else 
        	user.setEnabled(true);
        repository.save(user);
        return TOKEN_VALID;
    }

	@Override
	public void createUser(UserEntity newUser) {
		repository.save(newUser);
	}
	
	public void deleteUser (UserEntity user) {
		VerificationToken token = tokenRepository.findByUser(user);
		if (token != null) {
			tokenRepository.delete(token);
		}
		repository.delete(user);
	}

	@Override
	public UserEntity recoverLogin(String email) {
		UserEntity user = repository.findByEmail(email);
		if (user == null) {
			throw new UserNotFoundException("No user is associated with " + email);
		}
		return user;
	}

	@Override
	public void changePassword(UserEntity user, String newPassword) {
		user.setPassword(newPassword);
		repository.save(user);
	}

	@Override
	public void changeEmail(UserEntity user, String oldEmail, String newEmail) {
		// store old email and new email in a separate table
		EmailChangeEntity emailChange = new EmailChangeEntity();
		emailChange.setUser(user);
		emailChange.setOldEmail(oldEmail);
		emailChange.setNewEmail(newEmail);
		emailRepository.save(emailChange);
	}    
	
	@Override
	public UserEntity getUserByUsername(String userName) {
		UserEntity user = repository.findByUsername(userName);
		return user;
	}

	@Override
	public void cleanUpExpiredSignup() {
		final Calendar cal = Calendar.getInstance();
		// get all expired token and delete their users
		for (VerificationToken verificationToken: tokenRepository.findAll()) {
			final UserEntity user = verificationToken.getUser();
	        if ((verificationToken.getExpiryDate().getTime() - cal.getTime().getTime()) <= 0) {
	            tokenRepository.delete(verificationToken);
	            if (user.getEnabled()) {
	            	// do not delete the user, possible email change
	            	// delete email change request, if any
	            	EmailChangeEntity emailChange = emailRepository.findByUser(user);
	            	if (emailChange != null) {
	            		emailRepository.delete(emailChange);
	            	}
	            } else 
	            	repository.delete(user);
	        }
		}
	}
}
