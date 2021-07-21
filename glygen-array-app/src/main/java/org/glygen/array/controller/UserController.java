package org.glygen.array.controller;

import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.EntityExistsException;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.glygen.array.exception.EmailExistsException;
import org.glygen.array.exception.LinkExpiredException;
import org.glygen.array.exception.UserNotFoundException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.UserLoginType;
import org.glygen.array.persistence.dao.RoleRepository;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.dao.VerificationTokenRepository;
import org.glygen.array.service.EmailManager;
import org.glygen.array.service.UserManager;
import org.glygen.array.service.UserManagerImpl;
import org.glygen.array.typeahead.NamespaceHandler;
import org.glygen.array.view.ChangePassword;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.glygen.array.view.User;
import org.glygen.array.view.validation.PasswordValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/users")
public class UserController {
	//public static Logger logger=(Logger) LoggerFactory.getLogger(UserController.class);
	final static Logger logger = LoggerFactory.getLogger("event-logger");
	
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
	
	@Autowired
	Validator validator;
	
	PasswordEncoder passwordEncoder =
		    PasswordEncoderFactories.createDelegatingPasswordEncoder();
	
	
	@RequestMapping(value = "/signup", method = RequestMethod.POST, 
    		consumes={"application/xml", "application/json"})
	@ApiOperation(value="Adds the given user to the system. \"username\", \"email\" and \"password\" cannot be left blank", response=Confirmation.class)
	@ApiResponses (value ={@ApiResponse(code=201, message="User added successfully"), 
			    @ApiResponse(code=400, message="Username, email or password cannot be left blank (ErrorCode=4002 Invalid Input"),
	    		@ApiResponse(code=409, message="User with given login name already exists "
	    				+ "or user with the given email already exists (ErrorCode=4006 Not Allowed)"),
	    		@ApiResponse(code=415, message="Media type is not supported"),
	    		@ApiResponse(code=500, message="Internal Server Error (Mail cannot be sent)")})
	public Confirmation signup (@RequestBody(required=true) User user) {
		if (validator != null) {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
			errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
			if (user.getEmail() == null || user.getEmail().isEmpty()) {
				Set<ConstraintViolation<User>> violations = validator.validateValue(User.class, "email", user.getEmail());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("email", "NoEmpty"));
				}	
			}
			if (user.getUserName() == null || user.getUserName().isEmpty()) {
				errorMessage.addError(new ObjectError("userName", "NoEmpty"));
			}
			if (user.getUserName() != null && !user.getUserName().isEmpty()) {
				Set<ConstraintViolation<User>> violations = validator.validateValue(User.class, "userName", user.getUserName());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("userName", "NotValid"));
				}
			}
			if (user.getPassword() == null || user.getPassword().isEmpty()) {
				Set<ConstraintViolation<User>> violations = validator.validateValue(User.class, "password", user.getPassword());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("password", "NoEmpty"));
				}	
			}
			if (user.getPassword() != null && !user.getPassword().isEmpty()) {
				Set<ConstraintViolation<User>> violations = validator.validateValue(User.class, "password", user.getPassword());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("password", "NotValid"));
				}	
			}
			if  (user.getEmail() != null && !user.getEmail().isEmpty()) {
				Set<ConstraintViolation<User>> violations = validator.validateValue(User.class, "email", user.getEmail());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("email", "NotValid"));
				}		
			}
			if (user.getAffiliation() != null) {
				Set<ConstraintViolation<User>> violations = validator.validateValue(User.class, "affiliation", user.getAffiliation());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("affiliation", "LengthExceeded"));
				}		
			}
			if (user.getGroupName() != null) {
                Set<ConstraintViolation<User>> violations = validator.validateValue(User.class, "groupName", user.getGroupName());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("groupName", "LengthExceeded"));
                }       
            }
			if (user.getDepartment() != null) {
                Set<ConstraintViolation<User>> violations = validator.validateValue(User.class, "department", user.getDepartment());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("department", "LengthExceeded"));
                }       
            }
			if (user.getAffiliationWebsite() != null) {
				Set<ConstraintViolation<User>> violations = validator.validateValue(User.class, "affiliationWebsite", user.getAffiliationWebsite());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("affiliationWebsite", "LengthExceeded"));
				}		
			}
			if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
				Set<ConstraintViolation<User>> violations = validator.validateValue(User.class, "firstName", user.getFirstName());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("firstName", "LengthExceeded"));
				}		
			}
			if (user.getLastName() != null && !user.getLastName().isEmpty()) {
				Set<ConstraintViolation<User>> violations = validator.validateValue(User.class, "lastName", user.getLastName());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("lastName", "LengthExceeded"));
				}		
			}
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid user information", errorMessage);
		
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		
		if (user.getPublicFlag() == null)   // set to default, not public
			user.setPublicFlag(false);
		
		UserEntity newUser = new UserEntity();
		newUser.setUsername(user.getUserName());		
		newUser.setPassword(passwordEncoder.encode(user.getPassword()));
		newUser.setEnabled(false);
		newUser.setFirstName(user.getFirstName());
		newUser.setLastName(user.getLastName());
		newUser.setEmail(user.getEmail());
		newUser.setAffiliation(user.getAffiliation());
		newUser.setGroupName(user.getGroupName());
		newUser.setDepartment(user.getDepartment());
		newUser.setAffiliationWebsite(user.getAffiliationWebsite()); 
		newUser.setPublicFlag(user.getPublicFlag());
		newUser.setRoles(Arrays.asList(roleRepository.findByRoleName("ROLE_USER")));
		newUser.setLoginType(UserLoginType.LOCAL); 
		
		// clean up expired tokens if any
		userManager.cleanUpExpiredSignup();
		
    	// check if the user already exists
		UserEntity existing = userRepository.findByUsernameIgnoreCase(user.getUserName());
		if (existing != null) {
			logger.info("This user " + user.getUserName() + " already exists!");
			ErrorMessage errorMessage = new ErrorMessage("Cannot add duplicate user");
			errorMessage.addError(new ObjectError("username", "Duplicate"));
			throw new EntityExistsException("This user " + user.getUserName() + " already exists!", errorMessage);
		}
		
		// check if the email is already in the system
		existing = userRepository.findByEmailIgnoreCase(user.getEmail());
		if (existing != null) {
			logger.info("There is already an account with this email: " + user.getEmail());
			ErrorMessage errorMessage = new ErrorMessage("There is already an account with this email, please use a different one!");
			errorMessage.addError(new ObjectError("email", "Duplicate"));
			throw new EmailExistsException ("There is already an account with this email: " + user.getEmail(), errorMessage);
		}
		
		logger.info("user affiliation" + newUser.getAffiliation());
			
        userManager.createUser(newUser);  
        // send email confirmation
        try {
        	emailManager.sendVerificationToken(newUser);
        } catch (MailSendException e) {
        	// email cannot be sent, remove the user
        	logger.error("Mail cannot be sent: ", e);
        	userManager.deleteUser(newUser);
        	throw e;
        }
        logger.info("New user {} is added to the system", newUser.getUsername());
        return new Confirmation("User added successfully", HttpStatus.CREATED.value());
	}
	
	@RequestMapping(value = "/update/{userName}", method = RequestMethod.POST, 
    		consumes={"application/xml", "application/json"})
	@ApiOperation(value="Updates the information for the given user. Only the non-empty fields will be updated. "
			+ "\"username\" cannot be changed", response=Confirmation.class)
	@ApiResponses (value ={@ApiResponse(code=200, message="User updated successfully"), 
			 	@ApiResponse(code=400, message="Illegal arguments, username should match the submitted user info"),
				@ApiResponse(code=401, message="Unauthorized"),
				@ApiResponse(code=403, message="Not enough privileges to update users"),
	    		@ApiResponse(code=404, message="User with given login name does not exist"),
	    		@ApiResponse(code=415, message="Media type is not supported"),
	    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation updateUser (@RequestBody(required=true) User user, @PathVariable("userName") String loginId) {
		UserEntity userEntity = userRepository.findByUsernameIgnoreCase(loginId);
		if (userEntity == null) {
		    // find it with email
		    userEntity = userRepository.findByEmailIgnoreCase(loginId);
		    if (userEntity == null) {
		        ErrorMessage errorMessage = new ErrorMessage ("No user is associated with this loginId");
	            errorMessage.addError(new ObjectError("username", "NotFound"));
	            throw new UserNotFoundException ("A user with loginId " + loginId + " does not exist", errorMessage);
		    }
	    	
		}
		if ((user.getUserName() == null || user.getUserName().isEmpty()) || (!loginId.equalsIgnoreCase(user.getUserName()) && !loginId.equalsIgnoreCase(user.getEmail()))) {
			throw new IllegalArgumentException("userName (path variable) and the submitted user information do not match");
		}
		
		if (validator != null) {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
			errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
			if  (user.getEmail() != null && !user.getEmail().isEmpty()) {
				Set<ConstraintViolation<User>> violations = validator.validateValue(User.class, "email", user.getEmail());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("email", "NotValid"));
				}		
			}
			if (user.getAffiliation() != null) {
				Set<ConstraintViolation<User>> violations = validator.validateValue(User.class, "affiliation", user.getAffiliation());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("affiliation", "LengthExceeded"));
				}		
			}
			if (user.getGroupName() != null) {
                Set<ConstraintViolation<User>> violations = validator.validateValue(User.class, "groupName", user.getGroupName());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("groupName", "LengthExceeded"));
                }       
            }
            if (user.getDepartment() != null) {
                Set<ConstraintViolation<User>> violations = validator.validateValue(User.class, "department", user.getDepartment());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("department", "LengthExceeded"));
                }       
            }
			if (user.getAffiliationWebsite() != null) {
				Set<ConstraintViolation<User>> violations = validator.validateValue(User.class, "affiliationWebsite", user.getAffiliationWebsite());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("affiliationWebsite", "LengthExceeded"));
				}		
			}
			if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
				Set<ConstraintViolation<User>> violations = validator.validateValue(User.class, "firstName", user.getFirstName());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("firstName", "LengthExceeded"));
				}		
			}
			if (user.getLastName() != null && !user.getLastName().isEmpty()) {
				Set<ConstraintViolation<User>> violations = validator.validateValue(User.class, "lastName", user.getLastName());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("lastName", "LengthExceeded"));
				}		
			}
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid user information", errorMessage);
		
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth != null) { 
    		// a user can only update his/her own user information
    		// username of the authenticated user should match the username of the user retrieved from the db
    		
    		if (auth.getName().equalsIgnoreCase(loginId) || auth.getName().equalsIgnoreCase(userEntity.getUsername())) {
    			if (user.getEmail() != null && !user.getEmail().isEmpty() && !user.getEmail().trim().equals(userEntity.getEmail())) {
    				// send email confirmation
    		        try {
    		        	// make sure the new email is not assigned to a different user
    		        	// check if the email is already in the system
    		    		UserEntity existing = userRepository.findByEmailIgnoreCase(user.getEmail().trim());
    		    		if (existing != null && !existing.getUserId().equals(userEntity.getUserId())) {
    		    			logger.info("There is already an account with this email: " + user.getEmail());
    		    			ErrorMessage errorMessage = new ErrorMessage("There is already an account with this email, please use a different one!");
    		    			errorMessage.addError(new ObjectError("email", "Duplicate"));
    		    			throw new EmailExistsException ("There is already an account with this email: " + user.getEmail(), errorMessage);
    		    		} 
    		        	
    		        	emailManager.sendEmailChangeNotification(userEntity);
    		        	UserEntity userWithNewEmail = new UserEntity();
    		        	userWithNewEmail.setEmail(user.getEmail().trim());
    		        	userWithNewEmail.setUserId(userEntity.getUserId());
    		        	userWithNewEmail.setUsername(userEntity.getUsername());
    		        	userWithNewEmail.setRoles(userEntity.getRoles());
    		        	emailManager.sendVerificationToken(userWithNewEmail);
    		        	userManager.changeEmail(userEntity, userEntity.getEmail(), user.getEmail().trim());
    		        } catch (MailSendException e) {
    		        	// email cannot be sent, do not update the user
    		        	logger.error("Mail cannot be sent: ", e);
    		        	throw e;
    		        }
    			}
    			
    			if (user.getAffiliation() != null) userEntity.setAffiliation(user.getAffiliation());
    			if (user.getGroupName() != null) userEntity.setGroupName(user.getGroupName());
    			if (user.getDepartment() != null) userEntity.setDepartment(user.getDepartment());
    			if (user.getAffiliationWebsite() != null) userEntity.setAffiliationWebsite(user.getAffiliationWebsite());
    			if (user.getFirstName() != null && !user.getFirstName().isEmpty()) userEntity.setFirstName(user.getFirstName());
    			if (user.getLastName() != null && !user.getLastName().isEmpty()) userEntity.setLastName(user.getLastName());
    			if (user.getPublicFlag() != null) userEntity.setPublicFlag(user.getPublicFlag());
    	    	userRepository.save(userEntity);
    		}
    		else {
    			logger.info("The user: " + auth.getName() + " is not authorized to update user " + loginId);
    			throw new AccessDeniedException("The user: " + auth.getName() + " is not authorized to update user with id " + loginId);
    		}
    	}
    	else { // should not reach here at all
    		throw new BadCredentialsException ("The user has not been authenticated");
    	}
    	
		return new Confirmation("User updated successfully", HttpStatus.OK.value());
	}
	
	@GetMapping(value = "/registrationConfirm")
	@ApiOperation(value="Enables the user by checking the confirmation token, removes the user if token is expired already", response=Confirmation.class)
	@ApiResponses (value ={@ApiResponse(code=200, message="User is confirmed successfully"), 
    		@ApiResponse(code=400, message="Link already expired (ErrorCode=4050 Expired)"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation confirmRegistration(@RequestParam("token") final String token) throws UnsupportedEncodingException, LinkExpiredException {
        final String result = userManager.validateVerificationToken(token);
        if (result.equals(UserManagerImpl.TOKEN_VALID)) {
            final UserEntity user = userManager.getUserByToken(token);
            // we don't need the token after confirmation
            userManager.deleteVerificationToken(token);
            return new Confirmation("User " + user.getUsername() + " is confirmed", HttpStatus.OK.value());
        } else if (result.equals(UserManagerImpl.TOKEN_INVALID)) {
        	logger.error("Token entered is not valid!");
    		ErrorMessage errorMessage = new ErrorMessage("Please enter a valid token!");
			errorMessage.addError(new ObjectError("token", "Invalid"));
			throw new IllegalArgumentException("Token entered is not valid", errorMessage);
        } else if (result.equals(UserManagerImpl.TOKEN_EXPIRED)) {
        	logger.error("Token is expired, please signup again!");
    		ErrorMessage errorMessage = new ErrorMessage("Token is expired, please signup again!");
			errorMessage.addError(new ObjectError("token", "Expired"));
			throw new IllegalArgumentException("Token is expired, please signup again!", errorMessage);
        }
        throw new LinkExpiredException("User verification link is expired");
    }
	
	@GetMapping("/availableUsername")
	@ApiOperation(value="Checks whether the given username is available to be used (returns true if available, false if alredy in use", response=Boolean.class)
	@ApiResponses (value ={@ApiResponse(code=200, message="Check performed successfully"), 
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Boolean checkUserName(@RequestParam("username") final String username) {
		userManager.cleanUpExpiredSignup(); // to make sure we are not holding onto any user name which is not verified and expired
		UserEntity user = userRepository.findByUsernameIgnoreCase(username);
		if(user!=null) {
			ErrorMessage errorMessage = new ErrorMessage("Cannot add duplicate user");
			errorMessage.addError(new ObjectError("username", "Duplicate"));
			throw new EntityExistsException("This user " + username + " already exists!", errorMessage);
		}
		return user == null;
	}

	@ApiIgnore
	@GetMapping("/signin")
	public @ResponseBody Confirmation signin() {
		return new Confirmation("User is authorized", HttpStatus.OK.value());
	}

	@Authorization (value="Bearer", scopes={@AuthorizationScope (scope="read:glygenarray", description="Access to user profile")})
	@RequestMapping(value="/get/{userName}", method=RequestMethod.GET, produces={"application/xml", "application/json"})
    @ApiOperation(value="Retrieve the information for the given user", response=User.class)
    @ApiResponses (value ={@ApiResponse(code=200, message="User retrieved successfully"), 
    		@ApiResponse(code=401, message="Unauthorized"),
    		@ApiResponse(code=403, message="Not enough privileges"),
    		@ApiResponse(code=404, message="User with given login name does not exist"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
    public @ResponseBody User getUser (
    		@io.swagger.annotations.ApiParam(required=true, value="login name of the user")
    		@PathVariable("userName")
    		String userName) {
    	UserEntity user = null;
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth != null) { 
    		// username of the authenticated user should match the username parameter
    		// a user can only see his/her own user information
    		// but admin can access all the users' information
    	    user = userRepository.findByUsernameIgnoreCase(userName);
    	    if (user == null) {
    	        // try with email
    	        user = userRepository.findByEmailIgnoreCase(userName);
    	        if (user == null) {
    	            ErrorMessage errorMessage = new ErrorMessage ("No user is associated with this loginId");
    	            errorMessage.addError(new ObjectError("username", "NotFound"));
    	            throw new UserNotFoundException ("A user with loginId " + userName + " does not exist", errorMessage);
    	        }
    	    }
    		if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
    			// no issues, the admin can access any profile
    		} else if (auth.getName().equalsIgnoreCase(userName)) {
    			// the user can display his/her own details
    		} else if (user.getEmail().equalsIgnoreCase(userName)) {
    		    // the user can retrieve his/her own details
    		} else {
    			logger.info("The user: " + auth.getName() + " is not authorized to access " + userName + "'s information");
    			throw new AccessDeniedException("The user: " + auth.getName() + " is not authorized to access " + userName + "'s information");
    		}
    	}
    	else { // should not reach here at all
    		throw new BadCredentialsException ("The user has not been authenticated");
    	}
    	
    	User userView = new User();
    	userView.setAffiliation(user.getAffiliation());
    	userView.setAffiliationWebsite(user.getAffiliationWebsite());
    	userView.setEmail(user.getEmail());
    	userView.setFirstName(user.getFirstName());
    	userView.setLastName(user.getLastName());
    	userView.setPublicFlag(user.getPublicFlag());
    	userView.setUserName(user.getUsername());
    	userView.setUserType(user.getLoginType().name());
    	userView.setGroupName(user.getGroupName());
    	userView.setDepartment(user.getDepartment());
    	return userView;
    }
	
	
    @RequestMapping(value="/listusernames", method=RequestMethod.GET, produces={"application/xml", "application/json"})
    @ApiOperation(value="Retrieve the list of usernames matching the entered username value")
    @ApiResponses (value ={@ApiResponse(code=200, message="User list retrieved successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public @ResponseBody List<String> getUserNamesWithTypeAhead (
            @ApiParam(required=true, value="value to match") 
            @RequestParam("value")
            String key, 
            @ApiParam(required=false, value="limit of number of matches") 
            @RequestParam(name="limit", required=false)
            Integer limit) {
        List<UserEntity> userList = userRepository.findAll();
        List<String> userNames = new ArrayList<String>();
        for (UserEntity user: userList) {
            userNames.add(user.getUsername());
        }
        PatriciaTrie<String> trie = NamespaceHandler.createNamespaceFromList(userNames);
        return UtilityController.getSuggestions(trie, key, limit);
    }
	
	@RequestMapping(value="/recover", method = RequestMethod.GET)
    @ApiOperation(value="Recovers the user's username. Sends an email to the email provided by the user if it has valid account", response=String.class)
    @ApiResponses (value ={@ApiResponse(code=200, message="Username recovered successfully"), 
    		@ApiResponse(code=400, message="Illegal argument - valid email has to be provided"),
            @ApiResponse(code=404, message="User with given email does not exist"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public @ResponseBody Confirmation recoverUsername (@RequestParam(value="email", required=true) String email) {
		UserEntity user = userManager.recoverLogin(email);
    	
    	if (user == null) {
    		ErrorMessage errorMessage = new ErrorMessage ("No user is associated with this email");
    		errorMessage.addError(new ObjectError("email", "NotFound"));
    		throw new UserNotFoundException ("A user with email " + email + " does not exist", errorMessage);
    	}
    	
    	String userEmail = user.getEmail();
    	emailManager.sendUserName(user);
    	
    	logger.info("UserName Recovery email is sent to {}", userEmail);
    	return new Confirmation("Email with UserName was sent", HttpStatus.OK.value());
    }
    
	@Authorization (value="Bearer", scopes={@AuthorizationScope (scope="write:glygenarray", description="Access to user profile")})
    @RequestMapping(value="/{userName}/password", method = RequestMethod.GET)
    @ApiOperation(value="Recovers the user's password. Sends an email to the registered email of the user", response=Confirmation.class)
    @ApiResponses (value ={@ApiResponse(code=200, message="Password recovered successfully"), 
    		@ApiResponse(code=404, message="User with given login name does not exist"),
    		@ApiResponse(code=500, message="Internal Server Error")})
    public @ResponseBody Confirmation recoverPassword (
    		@PathVariable("userName") String loginId) {
    	UserEntity user = userRepository.findByUsernameIgnoreCase(loginId);
    	if (user == null) {
    	    user = userRepository.findByEmailIgnoreCase(loginId);
    	    if (user == null) {
        		ErrorMessage errorMessage = new ErrorMessage ("No user is associated with this loginId");
        		errorMessage.addError(new ObjectError("username", "NotFound"));
        		throw new UserNotFoundException ("A user with loginId " + loginId + " does not exist", errorMessage);
    	    }
    	}
    	emailManager.sendPasswordReminder(user);
    	logger.info("Password reminder email is sent to {}", loginId);
		return new Confirmation("Password reminder email is sent", HttpStatus.OK.value());
    	
    }
    
	@Authorization (value="Bearer", scopes={@AuthorizationScope (scope="write:glygenarray", description="Access to user profile")})
    @RequestMapping(value="/{userName}/password", method = RequestMethod.PUT)
    @ApiOperation(value="Changes the password for the given user", response=Confirmation.class, notes="Only authenticated user can change his/her password")
    @ApiResponses (value ={@ApiResponse(code=200, message="Password changed successfully"), 
    		@ApiResponse(code=400, message="Illegal argument - new password should be valid"),
    		@ApiResponse(code=401, message="Unauthorized"),
    		@ApiResponse(code=403, message="Not enough privileges to update password"),
    		@ApiResponse(code=404, message="User with given login name does not exist"),
    		@ApiResponse(code=500, message="Internal Server Error")})
    public @ResponseBody Confirmation changePassword (
    		Principal p,
    		@ApiParam(value = "your password", type = "string", format = "password")
    		@RequestBody(required=true) 
    		ChangePassword changePassword, 
    		@PathVariable("userName") String userName) {
    	if (p == null) {
    		// not authenticated
    		throw new BadCredentialsException("Unauthorized to change the password");
    	}
    	UserEntity user = userManager.getUserByUsername(userName);
    	if (user == null) {
    	    user = userRepository.findByEmailIgnoreCase(userName);
    	    if (user == null) {
    	        ErrorMessage errorMessage = new ErrorMessage ("No user is associated with this loginId");
                errorMessage.addError(new ObjectError("username", "NotFound"));
                throw new UserNotFoundException ("A user with loginId " + userName + " does not exist", errorMessage);
    	    }
    	}
    	
    	if (!p.getName().equalsIgnoreCase(userName) && !p.getName().equalsIgnoreCase(user.getUsername())) {
    		logger.warn("The user: " + p.getName() + " is not authorized to change " + userName + "'s password");
    		throw new AccessDeniedException("The user: " + p.getName() + " is not authorized to change " + userName + "'s password");
    	}
    	
    	// using @NotEmpty for newPassword didn't work, so have to handle it here
    	if (null == changePassword.getNewPassword() || changePassword.getNewPassword().isEmpty()) {
    		ErrorMessage errorMessage = new ErrorMessage ("new password cannot be empty");
    		errorMessage.addError(new ObjectError("password", "NoEmpty"));
    		throw new IllegalArgumentException("Invalid Input: new password cannot be empty", errorMessage);
    	}
    	
    	if (null == changePassword.getCurrentPassword() || changePassword.getCurrentPassword().isEmpty()) {
    		ErrorMessage errorMessage = new ErrorMessage ("current password cannot be empty");
    		errorMessage.addError(new ObjectError("currentpassword", "NoEmpty"));
    		throw new IllegalArgumentException("Invalid Input: current password cannot be empty", errorMessage);
    	}
    	
    	//password validation 
    	Pattern pattern = Pattern.compile(PasswordValidator.PASSWORD_PATTERN);
    	
    	if (!pattern.matcher(changePassword.getNewPassword()).matches()) {
    		logger.debug("Password fails pattern: " + changePassword.getNewPassword());
    		ErrorMessage errorMessage = new ErrorMessage ("new password is not valid. The password length must be greater than or equal to 5, must contain one or more uppercase characters, \n " + 
    				"must contain one or more lowercase characters, must contain one or more numeric values and must contain one or more special characters");
    		errorMessage.addError(new ObjectError("password", "NotValid"));
    		throw new IllegalArgumentException("Invalid Input: Password is not valid", errorMessage);
    	}
    	
    	if(passwordEncoder.matches(changePassword.getCurrentPassword(), user.getPassword())) {
    		// encrypt the password
    		String hashedPassword = passwordEncoder.encode(changePassword.getNewPassword());
        	logger.debug("new password is {}", hashedPassword);
        	userManager.changePassword(user, hashedPassword);
    	} else {
    		logger.error("Current Password is not valid!");
    		ErrorMessage errorMessage = new ErrorMessage("Current Password is not valid. Please try again!");
			errorMessage.addError(new ObjectError("currentPassword", "Invalid"));
			throw new IllegalArgumentException("Current password s invalid", errorMessage);
    	}
    	return new Confirmation("Password changed successfully", HttpStatus.OK.value()); 
    }
}

