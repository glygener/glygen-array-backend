package org.glygen.array.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

public class MyOAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
	
	@Value("${glygen.frontend.basePath}")
	String basePath;
	
	@Value("${glygen.frontend.host}")
	String host;
	
	@Value("${glygen.frontend.scheme}")
	String scheme;
	
	@Value("${glygen.frontend.loginPage}")
	String loginPage;
	
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException, ServletException {
		String redirectURL = scheme + host + basePath + loginPage;
		setDefaultTargetUrl(redirectURL);
        super.onAuthenticationSuccess(request, response, authentication);
        
        /*
          String redirectUrl = request.getParameter(REDIRECT_URL_PARAM);
          if (redirectUrl == null) {
            redirectUrl = DEFAULT_REDIRECT_URL;
          } else {
            if (!redirectUrlValidator.validateRedirectUrl(redirectUrl)) {
              request.setAttribute(MESSAGE_ATTRIBUTE_NAME,
                  messageSource.getMessage("ivalid.redirect.url", new String[] { redirectUrl }, LocaleContextHolder.getLocale()));
              response.sendError(HttpStatus.FORBIDDEN.value());
            }
          }
          defaultRedirectStrategy.sendRedirect(request, response, redirectUrl);
         */
    }
}
