package org.glygen.array.controller;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;

import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.RoleRepository;
import org.glygen.array.persistence.dao.VerificationTokenRepository;
import org.glygen.array.service.EmailManager;
import org.glygen.array.service.UserManager;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.Greeting;
import org.glygen.array.view.User;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
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
	EmailManager emailManager;
	
	@Autowired
	VerificationTokenRepository tokenRepository;
	
	PasswordEncoder passwordEncoder =
		    PasswordEncoderFactories.createDelegatingPasswordEncoder();
	
	/*@GetMapping("/")
	public Greeting home() {
		Greeting greet = new Greeting("Hello World", "Hello Home", new Date());
		return greet;
	}*/
	
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
    public Confirmation confirmRegistration(@RequestParam("token") final String token) throws UnsupportedEncodingException {
        final String result = userManager.validateVerificationToken(token);
        if (result.equals("valid")) {
            final UserEntity user = userManager.getUserByToken(token);
            return new Confirmation("User " + user.getUsername() + " is confirmed", HttpStatus.OK.value());
        }
        return new Confirmation("User verification link is expired", HttpStatus.EXPECTATION_FAILED.value());
    }

	@Authorization (value="basicAuth", scopes={@AuthorizationScope (scope="GlygenArray", description="Access to Glygen Array")})
	@ApiOperation(value="Check if the user's credentials are acceptable", response=Confirmation.class, notes="If the user is authorized, this does not necessarily mean that s/he is allowed to access all the resources")
	@GetMapping("/signin")
	public @ResponseBody Confirmation signin() {
		return new Confirmation("User is authorized", HttpStatus.OK.value());
	}

}
