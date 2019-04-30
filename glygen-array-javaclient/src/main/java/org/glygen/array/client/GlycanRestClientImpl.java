package org.glygen.array.client;

import java.util.Arrays;

import org.glygen.array.client.exception.CustomClientException;
import org.glygen.array.client.model.Confirmation;
import org.glygen.array.client.model.GlycanView;
import org.glygen.array.client.model.LoginRequest;
import org.glygen.array.client.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GlycanRestClientImpl implements GlycanRestClient {
	
	@Value("${glygen.scheme}")
	String scheme="http://";
	
	@Value("${glygen.host}")
	String host="localhost:8080";
	
	@Value("${glygen.basePath}")
	String basePath="/";
	
	private RestTemplate restTemplate = new RestTemplate();
	
	String token=null;
	String username;
	String password;
	
	@Override
	public Confirmation addGlycan(GlycanView glycan, User user) {
		if (token == null) login(this.username, this.password);
		//set the header with token
		HttpHeaders headers = new HttpHeaders();
	    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	    headers.add("Authorization", token);
	    HttpEntity<GlycanView> requestEntity = new HttpEntity<GlycanView>(glycan, headers);
		String url = scheme + host + basePath + "array/addglycan";
		System.out.println("URL: " + url);
		ResponseEntity<Confirmation> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, Confirmation.class);
		return response.getBody();

	}
	
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
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
}
