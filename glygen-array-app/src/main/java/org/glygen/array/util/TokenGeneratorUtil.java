package org.glygen.array.util;

import org.springframework.beans.factory.annotation.Value;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class TokenGeneratorUtil {
	
	@Value("${glygen.token-secret}")
	String tokenSecret;
	
	static TokenGeneratorUtil instance = new TokenGeneratorUtil();
	
	public static TokenGeneratorUtil getInstance() {
		return instance;
	}
	
	public String generateToken (String graphURI, String resourceURI) {
		String token = Jwts.builder()
                .setSubject(graphURI + ";" + resourceURI)
                .signWith(SignatureAlgorithm.HS512, tokenSecret.getBytes())
                .compact();
		return token;
	}

}
