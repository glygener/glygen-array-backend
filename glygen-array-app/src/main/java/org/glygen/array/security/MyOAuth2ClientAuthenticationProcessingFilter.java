package org.glygen.array.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;

public class MyOAuth2ClientAuthenticationProcessingFilter extends OAuth2ClientAuthenticationProcessingFilter {

	public MyOAuth2ClientAuthenticationProcessingFilter(String defaultFilterProcessesUrl) {
		super(defaultFilterProcessesUrl);
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException, ServletException {
		logger.info("Attempting authentication for request: " + request.getRequestURL());
		try {
			if (this.restTemplate != null && this.restTemplate.getResource() != null) {
				logger.debug(this.restTemplate.getResource().getAccessTokenUri());
			} else
				logger.debug("resttemplate is invalid");
			return super.attemptAuthentication(request, response);
		} catch (Exception e) {
			logger.error("Exception in attempt: ", e);
			throw e;
		}
	}
}
