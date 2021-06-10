package org.glygen.array.client;

import org.glygen.array.client.exception.CustomClientException;
import org.glygen.array.client.model.User;

public interface UserRestClient {
    
    /**
     * url of the web services e.g. https://glygen.ccrc.uga.edu/ggarray/api/
     * @param url url for the web services
     */
	void setURL (String url);
	
	/**
	 * login to the application with the given username and password
	 * @param username user name
	 * @param password password
	 * @throws CustomClientException if there are errors during login
	 */
	void login(String username, String password) throws CustomClientException;
	
	/**
	 * get the information about the logged in user
	 * 
	 * @param username user name
	 * @return the User with details
	 * @throws CustomClientException if there are errors retrieving the user details
	 */
	User getUser (String username) throws CustomClientException;
	
	/**
	 * retrieve the login token after logging into the application, 
	 * to be used for the rest of the web service calls
	 * 
	 * @return the token for login
	 */
	String getToken();
}
