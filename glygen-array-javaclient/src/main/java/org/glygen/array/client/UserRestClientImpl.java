package org.glygen.array.client;

import java.util.Arrays;

import org.glygen.array.client.exception.CustomClientException;
import org.glygen.array.client.model.LoginRequest;
import org.glygen.array.client.model.User;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class UserRestClientImpl implements UserRestClient {
	
	private RestTemplate restTemplate;
	
	String token=null;
	String username;
	String password;
	String url = "http://localhost:8080/";

	public UserRestClientImpl() {
		this.restTemplate = new RestTemplate();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public User getUser(String username) throws CustomClientException {
		if (token == null) login(this.username, this.password);
		//set the header with token
		HttpHeaders headers = new HttpHeaders();
	    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	    headers.add("Authorization", token);
	    HttpEntity<Void> requestEntity = new HttpEntity<Void>(null, headers);
		String url = this.url + "users/get/" + username;
		ResponseEntity<User> response = this.restTemplate.exchange(url, HttpMethod.GET, requestEntity, User.class);
		return response.getBody();
	}

	/**
     * {@inheritDoc}
     */
	@Override
	public void login(String username, String password) throws CustomClientException {
		// login to the system and set the token
		this.username = username;
		this.password = password;
		String url = this.url + "login";
		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setUsername(username);
		loginRequest.setPassword(password);
		HttpEntity<LoginRequest> requestEntity = new HttpEntity<LoginRequest>(loginRequest);
		HttpEntity<Void> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, Void.class);
		HttpHeaders header = response.getHeaders();
		this.token = header.getFirst("Authorization");
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
	public void setURL(String url) {
		this.url = url;
		
	}
	
	
	/**
     * {@inheritDoc}
     */
    @Override
    public String getToken() {
        return this.token;
    }

}
