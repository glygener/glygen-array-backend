package org.glygen.array.service;

import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.VerificationToken;

public interface UserManager {

	UserEntity getUserByToken(String verificationToken);

	VerificationToken getVerificationToken(String VerificationToken);

	void createVerificationTokenForUser(UserEntity user, String token);

	VerificationToken generateNewVerificationToken(String existingVerificationToken);

	String validateVerificationToken(String token);

	void createUser(UserEntity newUser);
	
	String recoverLogin(String email);
	
	void changePassword (String username, String newPassword);

}
