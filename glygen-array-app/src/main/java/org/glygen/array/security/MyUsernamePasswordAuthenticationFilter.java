package org.glygen.array.security;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.glygen.array.config.SecurityConstants;
import org.glygen.array.persistence.GlygenUser;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.LoginRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class MyUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private AuthenticationManager authenticationManager;

    public MyUsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req,
                                                HttpServletResponse res) throws AuthenticationException {
        try {
            LoginRequest creds = new ObjectMapper()
                    .readValue(req.getInputStream(), LoginRequest.class);

            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            creds.getUsername(),
                            creds.getPassword(),
                            new ArrayList<>())
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest req,
                                            HttpServletResponse res,
                                            FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {

    	String token = Jwts.builder()
                .setSubject(((GlygenUser) auth.getPrincipal()).getUsername())
                .setExpiration(new Date(System.currentTimeMillis() + SecurityConstants.EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS512, SecurityConstants.SECRET.getBytes())
                .compact();
        res.addHeader(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token);
        //IMPORTANT! Needed for CORS request, otherwise header will not be visible to the client
        res.addHeader("Access-Control-Expose-Headers", SecurityConstants.HEADER_STRING);   
        Confirmation confirmation = new Confirmation ("Signed in successfully", HttpStatus.OK.value());
        String acceptString = req.getHeader("Accept");
		if (acceptString.contains("xml")) {
			res.setContentType("application/xml;charset=UTF-8");
			res.setStatus(HttpStatus.OK.value());           
			PrintWriter out = res.getWriter();    
			try {
				JAXBContext errorContext = JAXBContext.newInstance(Confirmation.class);
				Marshaller errorMarshaller = errorContext.createMarshaller();
				errorMarshaller.marshal(confirmation, out);  
			} catch (JAXBException jex) {
				logger.error("Cannot generate error message in xml", jex);
			}
		}else if (acceptString.contains("json")) {
			ObjectMapper jsonMapper = new ObjectMapper();          
			res.setContentType("application/json;charset=UTF-8");         
			res.setStatus(HttpStatus.OK.value());           
			PrintWriter out = res.getWriter();         
			out.print(jsonMapper.writeValueAsString(confirmation));       
		}
    }
}
