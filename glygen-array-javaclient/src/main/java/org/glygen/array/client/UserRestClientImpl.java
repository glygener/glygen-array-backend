package org.glygen.array.client;

import java.util.Arrays;

import org.glygen.array.client.exception.CustomClientException;
import org.glygen.array.client.model.Confirmation;
import org.glygen.array.client.model.LoginRequest;
import org.glygen.array.client.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class UserRestClientImpl implements UserRestClient {
	
	@Value("${glygen.scheme}")
	String scheme="https://";
	//String scheme="http://";
	
	@Value("${glygen.host}")
	String host="glygen.ccrc.uga.edu";
	//String host="localhost:8080";
	
	@Value("${glygen.basePath}")
	String basePath="/ggarray/api/";
	//String basePath="/";
	
	@Autowired
	RestTemplateBuilder builder;
	
	private RestTemplate restTemplate;
	
	String token=null;
	String username;
	String password;

	public UserRestClientImpl() {
		this.restTemplate = new RestTemplate();
	}
	@Override
	public Confirmation changePassword(String newPassword) throws CustomClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Confirmation addUser(User user) throws CustomClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String recoverUsername(String email) throws CustomClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Confirmation recoverPassword(String username) throws CustomClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public User getUser(String username) throws CustomClientException {
		if (token == null) login(this.username, this.password);
		//set the header with token
		HttpHeaders headers = new HttpHeaders();
	    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	    headers.add("Authorization", token);
	    HttpEntity<Void> requestEntity = new HttpEntity<Void>(null, headers);
		String url = scheme + host + basePath + "users/get/" + username;
		ResponseEntity<User> response = this.restTemplate.exchange(url, HttpMethod.GET, requestEntity, User.class);
		return response.getBody();
	}

	@Override
	public void login(String username, String password) throws CustomClientException {
		// login to the system and set the token
		this.username = username;
		this.password = password;
		String url = scheme + host + basePath + "login";
		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setUsername(username);
		loginRequest.setPassword(password);
		HttpEntity<LoginRequest> requestEntity = new HttpEntity<LoginRequest>(loginRequest);
		HttpEntity<Void> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, Void.class);
		HttpHeaders header = response.getHeaders();
		this.token = header.getFirst("Authorization");
	}

}
