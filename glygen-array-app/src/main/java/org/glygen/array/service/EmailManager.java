package org.glygen.array.service;

import org.glygen.array.persistence.FeedbackEntity;
import org.glygen.array.persistence.UserEntity;

public interface EmailManager {
	void sendPasswordReminder (UserEntity user);
	void sendVerificationToken(UserEntity user);
	void sendUserName(UserEntity user);
	void sendEmailChangeNotification (UserEntity user);
	void sendFeedbackNotice (FeedbackEntity feedback);
    void sendFeedback(FeedbackEntity feedback, String... emails);
}
