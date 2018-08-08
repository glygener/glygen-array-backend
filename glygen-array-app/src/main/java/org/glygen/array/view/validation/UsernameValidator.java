package org.glygen.array.view.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
 
public class UsernameValidator implements ConstraintValidator<Username, String>{
	private Pattern pattern;
	private Matcher matcher;
 
	private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_-]{5,20}$";
 
	@Override
	public void initialize(Username arg0) {
		pattern = Pattern.compile(USERNAME_PATTERN);
	}
	
	/**
	   * Validate username with regular expression
	   * @param username username for validation
	   * @return true valid username, false invalid username
	   */
	@Override
	public boolean isValid(String username, ConstraintValidatorContext context) {
		matcher = pattern.matcher(username);
		return matcher.matches();
	}
 
}