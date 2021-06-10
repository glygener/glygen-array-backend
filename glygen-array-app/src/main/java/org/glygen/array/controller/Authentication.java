package org.glygen.array.controller;

import java.security.Principal;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.glygen.array.config.SecurityConstants;
import org.glygen.array.persistence.GlygenUser;
import org.glygen.array.security.MyUsernamePasswordAuthenticationFilter;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Authentication API specification for Swagger documentation and Code Generation.
 * Implemented by Spring Security.
 */
@Api("Authentication")
@Controller
@RequestMapping(value = "/", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class Authentication {
    
    @Value("${glygen.token-secret}")
    String tokenSecret;
    
    /**
     * Implemented by Spring Security
     */
    @ApiOperation(value = "Login", notes = "Login with the given credentials.")
    @ApiResponses({@ApiResponse(code = 200, message = "")})
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public void login(@RequestBody LoginRequest loginRequest)
    {
        throw new IllegalStateException("Add Spring Security to handle authentication");
    }

    /**
     * Implemented by Spring Security
     */
    @ApiOperation(value = "Logout", notes = "Logout the current user.")
    @ApiResponses({@ApiResponse(code = 200, message = "")})
    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public void logout() {
        throw new IllegalStateException("Add Spring Security to handle authentication");
    }
    
    @ApiOperation(value = "Refresh", notes = "Refreshes the token required for authentication.")
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