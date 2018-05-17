package org.glygen.array.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.glygen.array.config.SecurityConstants;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.service.GlygenUserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.GenericFilterBean;

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
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
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
        		logger.debug("token expired for id : " + e.getClaims().getId());
        	}
        }

        chain.doFilter(request, response);
    }

  }