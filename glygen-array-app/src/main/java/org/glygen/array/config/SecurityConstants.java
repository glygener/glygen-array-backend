package org.glygen.array.config;

public class SecurityConstants {
    public static final String SECRET = "GlygenSecretForJWSTokens";
    public static final long EXPIRATION_TIME = 8640000; // 24 hours
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
  
}