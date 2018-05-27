package org.glygen.array.controller;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.glygen.array.exception.LinkExpiredException;
import org.glygen.array.exception.UserNotFoundException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.RoleRepository;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.dao.VerificationTokenRepository;
import org.glygen.array.service.EmailManager;
import org.glygen.array.service.UserManager;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.User;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import ch.qos.logback.classic.Logger;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;

@RestController
@RequestMapping("/users")
public class UserController {
	public static Logger logger=(Logger) LoggerFactory.getLogger(UserController.class);
	
	@Autowired
	UserManager userManager;
	
	@Autowired
	RoleRepository roleRepository;
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	EmailManager emailManager;
	
	@Autowired
	VerificationTokenRepository tokenRepository;
	
	PasswordEncoder passwordEncoder =
		    PasswordEncoderFactories.createDelegatingPasswordEncoder();
	
	
	@RequestMapping(value = "/signup", method = RequestMethod.POST, 
    		consumes={"application/xml", "application/json"})
	public Confirmation signup (@RequestBody(required=true) User user) {
		UserEntity newUser = new UserEntity();
		newUser.setUsername(user.getUserName());		
		newUser.setPassword(passwordEncoder.encode(user.getPassword()));
		newUser.setEnabled(false);
		newUser.setFirstName(user.getFirstName());
		newUser.setLastName(user.getLastName());
		newUser.setEmail(user.getEmail());
		newUser.setAffiliation(user.getAffiliation());
		newUser.setAffiliationWebsite(user.getAffiliationWebsite()); 
		newUser.setPublicFlag(user.getPublicFlag());
		newUser.setRoles(Arrays.asList(roleRepository.findByRoleName("ROLE_USER")));
    	
        userManager.createUser(newUser);  
        // send email confirmation
        emailManager.sendVerificationToken(newUser);
        logger.info("New user {} is added to the system", newUser.getUsername());
        return new Confirmation("User added successfully", HttpStatus.CREATED.value());
	}
	
	@GetMapping(value = "/registrationConfirm")
    public Confirmation confirmRegistration(@RequestParam("token") final String token) throws UnsupportedEncodingException, LinkExpiredException {
        final String result = userManager.validateVerificationToken(token);
        if (result.equals("valid")) {
            final UserEntity user = userManager.getUserByToken(token);
            return new Confirmation("User " + user.getUsername() + " is confirmed", HttpStatus.OK.value());
        }
        throw new LinkExpiredException("User verification link is expired");
    }

	@Authorization (value="basicAuth", scopes={@AuthorizationScope (scope="GlygenArray", description="Access to Glygen Array")})
	@ApiOperation(value="Check if the user's credentials are acceptable", response=Confirmation.class, notes="If the user is authorized, this does not necessarily mean that s/he is allowed to access all the resources")
	@GetMapping("/signin")
	public @ResponseBody Confirmation signin() {
		return new Confirmation("User is authorized", HttpStatus.OK.value());
	}

	@RequestMapping(value="/get/{userName}", method=RequestMethod.GET, produces={"application/xml", "application/json"})
    @ApiOperation(value="Retrieve the information for the given user", response=UserEntity.class)
    @ApiResponses (value ={@ApiResponse(code=200, message="User retrieved successfully"), 
    		@ApiResponse(code=401, message="Unauthorized"),
    		@ApiResponse(code=403, message="Not enough privileges"),
    		@ApiResponse(code=404, message="User with given login name does not exist"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
    public @ResponseBody UserEntity getUser (
    		@io.swagger.annotations.ApiParam(required=true, value="login name of the user")
    		@PathVariable("userName")
    		String userName) {
    	UserEntity user = null;
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth != null) { 
    		// username of the authenticated user should match the username parameter
    		// a user can only see his/her own user information
    		// but admin can access all the users' information
    		
    		if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")))
    		{
    			user = userRepository.findByUsername(userName);
    		}
    		else if (auth.getName().equals(userName)) {
    			user = userRepository.findByUsername(userName);
    		}
    		else {
    			logger.info("The user: " + auth.getName() + " is not authorized to access " + userName + "'s information");
    			throw new AccessDeniedException("The user: " + auth.getName() + " is not authorized to access " + userName + "'s information");
    		}
    	}
    	else { // should not reach here at all
    		throw new BadCredentialsException ("The user has not been authenticated");
    	}
    	if (user == null) 
    		throw new UserNotFoundException ("A user with loginId " + userName + " does not exist");
    	return user;
    }
}
