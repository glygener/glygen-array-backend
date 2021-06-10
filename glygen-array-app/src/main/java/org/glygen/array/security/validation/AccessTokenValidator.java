package org.glygen.array.security.validation;

public interface AccessTokenValidator {
	AccessTokenValidationResult validate (String token);
}
