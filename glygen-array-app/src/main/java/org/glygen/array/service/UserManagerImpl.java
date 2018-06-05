package org.glygen.array.service;

import java.util.Calendar;
import java.util.UUID;

import org.glygen.array.exception.UserNotFoundException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.VerificationToken;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.dao.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
            return TOKEN_EXPIRED;
        }
        
        user.setEnabled(true);
        repository.save(user);
        return TOKEN_VALID;
    }

	@Override
	public void createUser(UserEntity newUser) {
		repository.save(newUser);
	}

	@Override
	public String recoverLogin(String email) {
		UserEntity user = repository.findByEmail(email);
		if (user == null) {
			throw new UserNotFoundException("No user is associated with " + email);
		}
		return user.getUsername();
	}

	@Override
	public void changePassword(String username, String newPassword) {
		UserEntity user = repository.findByUsername(username);
		if (user == null) {
			throw new UserNotFoundException("No user is associated with " + username);
		}
		// encrypt the password
		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		String hashedPassword = passwordEncoder.encode(newPassword);
		user.setPassword(hashedPassword);
		repository.save(user);
	}    
}

