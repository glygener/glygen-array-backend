package org.glygen.array.service;

import org.glygen.array.persistence.UserEntity;

public interface EmailManager {
	void sendPasswordReminder (UserEntity user);
	void sendVerificationToken(UserEntity user, String baseUrl);
}
