package org.glygen.array.security;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.glygen.array.config.SecurityConstants;
import org.glygen.array.service.GlygenUserDetailsService;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.GenericFilterBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;

public class TokenAuthenticationFilter extends GenericFilterBean
{
	GlygenUserDetailsService userService;
	
	public TokenAuthenticationFilter (GlygenUserDetailsService userService) {
		this.userService = userService;
	}
	
    @Override
    public void doFilter(final ServletRequest request, ServletResponse response, final FilterChain chain)
            throws IOException, ServletException
    {
        final HttpServletRequest httpRequest = (HttpServletRequest)request;

        String token = httpRequest.getHeader(SecurityConstants.HEADER_STRING);
        if (token != null) {
            // parse the token
        	try {
	            String user = Jwts.parser()
	                    .setSigningKey(SecurityConstants.SECRET.getBytes())
	                    .parseClaimsJws(token.replace(SecurityConstants.TOKEN_PREFIX, ""))
	                    .getBody()
	                    .getSubject();
	            // retrieve the user's details from the database and set the authorities
	            UserDetails userDetails = userService.loadUserByUsername(user);
	            if (userDetails != null) {
		            final UsernamePasswordAuthenticationToken authentication =
		                    new UsernamePasswordAuthenticationToken(user, null, userDetails.getAuthorities());
		            SecurityContextHolder.getContext().setAuthentication(authentication);
	            }
        	} catch (MalformedJwtException e) {
        		logger.debug("Not a valid token. Trying other authorization methods");
        	} catch (ExpiredJwtException e) {
        		logger.debug("token expired for id : " + e.getClaims().getId() + " message:" + e.getMessage());
        		sendError (httpRequest, (HttpServletResponse)response, e);
        		return;
        	}
        }

        chain.doFilter(request, response);
    }
    
    void sendError (HttpServletRequest request, HttpServletResponse response, Exception authEx) throws IOException {
    	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		
		Throwable mostSpecificCause = authEx.getCause();
	    ErrorMessage errorMessage;
	    if (mostSpecificCause != null) {
	    	//String exceptionName = mostSpecificCause.getClass().getName();
	    	String message = mostSpecificCause.getMessage();
	        errorMessage = new ErrorMessage();
	        List<String> errors = new ArrayList<>();
	        errors.add(message);
	        errorMessage.setErrors(errors);
	    } else {
	    	errorMessage = new ErrorMessage(authEx.getMessage());
	    }
	    if (authEx instanceof ExpiredJwtException) 
	    	errorMessage.setErrorCode(ErrorCodes.EXPIRED);
	    else
	    	errorMessage.setErrorCode(ErrorCodes.UNAUTHORIZED);
	    
	    errorMessage.setStatus(HttpStatus.UNAUTHORIZED.value());
		String acceptString = request.getHeader("Accept");
		if (acceptString.contains("xml")) {
			response.setContentType("application/xml;charset=UTF-8");
			response.setStatus(HttpStatus.UNAUTHORIZED.value());           
			PrintWriter out = response.getWriter();    
			try {
				JAXBContext errorContext = JAXBContext.newInstance(ErrorMessage.class);
				Marshaller errorMarshaller = errorContext.createMarshaller();
				errorMarshaller.marshal(errorMessage, out);  
			} catch (JAXBException jex) {
				logger.error("Cannot generate error message in xml", jex);
			}
 		} else if (acceptString.contains("json")) {
			ObjectMapper jsonMapper = new ObjectMapper();          
			response.setContentType("application/json;charset=UTF-8");         
			response.setStatus(HttpStatus.UNAUTHORIZED.value());           
			PrintWriter out = response.getWriter();         
			out.print(jsonMapper.writeValueAsString(errorMessage));       
		} else {
			response.sendError (HttpStatus.UNAUTHORIZED.value(), request.getUserPrincipal() + " is not allowed to access " + request.getRequestURI() + ": " + authEx.getMessage());
		}
    }
}