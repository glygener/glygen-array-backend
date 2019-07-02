package org.glygen.array.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.glygen.array.client.exception.CustomClientException;
import org.glygen.array.client.model.BlockLayout;
import org.glygen.array.client.model.Confirmation;
import org.glygen.array.client.model.GlycanView;
import org.glygen.array.client.model.LinkerView;
import org.glygen.array.client.model.LoginRequest;
import org.glygen.array.client.model.SlideLayout;
import org.glygen.array.client.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import ch.qos.logback.classic.Logger;

@Component
public class GlycanRestClientImpl implements GlycanRestClient {
	
	@Value("${glygen.scheme}")
	//String scheme="https://";
	String scheme="http://";
	
	@Value("${glygen.host}")
	//String host="glygen.ccrc.uga.edu";
	String host="localhost:8080";
	
	@Value("${glygen.basePath}")
	//String basePath="/ggarray/api/";
	String basePath="/";
	
	private RestTemplate restTemplate = new RestTemplate();
	
	String token=null;
	String username;
	String password;
	
	List<String> duplicates = new ArrayList<String>();
	List<String> empty = new ArrayList<String>();
	
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
		
		try {
			ResponseEntity<Confirmation> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, Confirmation.class);
			return response.getBody();
		} catch (HttpClientErrorException e) {
			System.out.println("Exception adding glycan " + ((HttpClientErrorException) e).getResponseBodyAsString());
			if (e.getResponseBodyAsString().contains("Duplicate") && e.getResponseBodyAsString().contains("sequence")) {
				duplicates.add(glycan.getName());
				try {
					url = scheme + host + basePath + "array/getGlycanBySequence?sequence=" 
							+ URLEncoder.encode(glycan.getSequence(), StandardCharsets.UTF_8.name());
					HttpEntity<Map<String, String>> requestEntity1 = new HttpEntity<>(headers);
					ResponseEntity<String> glycanId = this.restTemplate.exchange(url, HttpMethod.GET, requestEntity1, String.class);
					if (glycanId.getBody() != null) {
						HttpEntity<String> requestEntity2 = new HttpEntity<String>(glycan.getName(), headers);
						// add as alias to the existing one
						url = scheme + host + basePath + "array/addAlias/" + glycanId.getBody();
						try {
							ResponseEntity<Confirmation> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity2, Confirmation.class);
							System.out.println ("added alias " + glycan.getName());
							return response.getBody();
						} catch (HttpClientErrorException ex) {
							System.out.println ("Could not add alias for " + glycan.getName());
						}
					}
				} catch (UnsupportedEncodingException e1) {
					System.out.println ("Error encoding the sequence: " + e1.getMessage());
				}
			}
			if (e.getResponseBodyAsString().contains("NoEmpty") && e.getResponseBodyAsString().contains("sequence")) {
				empty.add(glycan.getName());
			}
		}
		return null;
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

	@Override
	public Confirmation addBlockLayout(BlockLayout layout, User user) {
		if (token == null) login(this.username, this.password);
		//set the header with token
		HttpHeaders headers = new HttpHeaders();
	    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	    headers.add("Authorization", token);
	    HttpEntity<BlockLayout> requestEntity = new HttpEntity<BlockLayout>(layout, headers);
		String url = scheme + host + basePath + "array/addblocklayout";
		System.out.println("URL: " + url);
		ResponseEntity<Confirmation> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, Confirmation.class);
		return response.getBody();
	}

	@Override
	public Confirmation addLinker(LinkerView linker, User user) {
		if (token == null) login(this.username, this.password);
		//set the header with token
		HttpHeaders headers = new HttpHeaders();
	    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	    headers.add("Authorization", token);
	    HttpEntity<LinkerView> requestEntity = new HttpEntity<LinkerView>(linker, headers);
		String url = scheme + host + basePath + "array/addlinker";
		System.out.println("URL: " + url);
		try {
			ResponseEntity<Confirmation> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, Confirmation.class);
			return response.getBody();
		} catch (HttpClientErrorException e) {
			String errorMessage = e.getResponseBodyAsString();
			if (errorMessage.contains("Duplicate") && !errorMessage.contains("pubChemId") 
					&& errorMessage.contains("name")) {
				linker.setName(linker.getName()+"B");
				requestEntity = new HttpEntity<LinkerView>(linker, headers);
				ResponseEntity<Confirmation> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, Confirmation.class);
				return response.getBody();
			}
		}
		return null;	
	}

	@Override
	public Confirmation addSlideLayout(SlideLayout layout, User user) {
		if (token == null) login(this.username, this.password);
		//set the header with token
		HttpHeaders headers = new HttpHeaders();
	    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	    headers.add("Authorization", token);
	    HttpEntity<SlideLayout> requestEntity = new HttpEntity<SlideLayout>(layout, headers);
		String url = scheme + host + basePath + "array/addslidelayout";
		System.out.println("URL: " + url);
		try {
			ResponseEntity<Confirmation> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, Confirmation.class);
			return response.getBody();
		} catch (HttpServerErrorException e) {
			String errorMessage = e.getResponseBodyAsString();
			if (errorMessage != null) {
				System.out.println("server error: " + errorMessage);
			}
		} catch (HttpClientErrorException e) {
			String errorMessage = e.getResponseBodyAsString();
			if (errorMessage != null) {
				System.out.println("client error: " + errorMessage);
			}
		}
		
		return null;
	}
	
	public List<String> getDuplicates() {
		return duplicates;
	}
	
	public List<String> getEmpty() {
		return empty;
	}
}
