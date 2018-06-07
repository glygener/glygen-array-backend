package org.glygen.array.client;

import org.glygen.array.client.exception.CustomClientException;
import org.glygen.array.client.model.Confirmation;
import org.glygen.array.client.model.User;

public interface UserRestClient {
	void login(String username, String password) throws CustomClientException;
	Confirmation changePassword(String newPassword) throws CustomClientException;
	Confirmation addUser (User user) throws CustomClientException;
	String recoverUsername (String email) throws CustomClientException;
	Confirmation recoverPassword (String username) throws CustomClientException;
	User getUser (String username) throws CustomClientException;
}
