package org.glygen.array.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

public class MyOAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
	
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException, ServletException {
		setDefaultTargetUrl("/users/signin");
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
