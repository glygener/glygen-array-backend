package org.glygen.array.controller;

import java.security.Principal;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.glygen.array.config.SecurityConstants;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.LoginRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Authentication API specification for Swagger documentation and Code Generation.
 * Implemented by Spring Security.
 */
@Tag(name="Authentication")
@Controller
@RequestMapping(value = "/", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class Authentication {
    
    @Value("${glygen.token-secret}")
    String tokenSecret;
    
    /**
     * Implemented by Spring Security
     */
    @Operation(summary = "Login", description = "Login with the given credentials.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "")})
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public void login(@RequestBody LoginRequest loginRequest)
    {
        throw new IllegalStateException("Add Spring Security to handle authentication");
    }

    /**
     * Implemented by Spring Security
     */
    @Operation(summary = "Logout", description = "Logout the current user.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "")})
    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public void logout() {
        throw new IllegalStateException("Add Spring Security to handle authentication");
    }
    
    @Operation(summary = "Refresh", description = "Refreshes the token required for authentication.")
    @RequestMapping(value="/refreshtoken", method = RequestMethod.POST) 
    public ResponseEntity<Confirmation> refresh(HttpServletRequest request, Principal p) {
        String token = request.getHeader(SecurityConstants.HEADER_STRING);
        if (token != null && token.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            String user = Jwts.parser()
                    .setSigningKey(tokenSecret.getBytes())
                    .parseClaimsJws(token.replace(SecurityConstants.TOKEN_PREFIX, ""))
                    .getBody()
                    .getSubject();
            if (p.getName().equals(user)) {
                // token is valid
                // create a new token with updated expiration date
                String newToken = Jwts.builder()
                        .setSubject(p.getName())
                        .setExpiration(new Date(System.currentTimeMillis() + SecurityConstants.EXPIRATION_TIME))
                        .signWith(SignatureAlgorithm.HS512, tokenSecret.getBytes())
                        .compact();
                // put the new token into the response header
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.set(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + newToken);
                //IMPORTANT! Needed for CORS request, otherwise header will not be visible to the client
                responseHeaders.set("Access-Control-Expose-Headers", SecurityConstants.HEADER_STRING);  
                Confirmation confirmation = new Confirmation("Token refreshed succesfully", HttpStatus.OK.value());
                ResponseEntity<Confirmation> res = new ResponseEntity<Confirmation>(confirmation, responseHeaders, HttpStatus.OK);
                return res;
            }
        }
        throw new IllegalStateException("Illegal token");
    }
}