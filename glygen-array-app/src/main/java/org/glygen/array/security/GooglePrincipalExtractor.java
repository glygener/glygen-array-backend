package org.glygen.array.security;

import java.util.Arrays;
import java.util.Map;

import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.UserLoginType;
import org.glygen.array.persistence.dao.RoleRepository;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.service.UserManager;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Logger;

@Component
public class GooglePrincipalExtractor implements PrincipalExtractor {
	Logger logger = (Logger) LoggerFactory.getLogger(GooglePrincipalExtractor.class);
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	UserManager userManager;
	
	@Autowired
	RoleRepository roleRepository;
	
	PasswordEncoder passwordEncoder =
		    PasswordEncoderFactories.createDelegatingPasswordEncoder();

	@Override
	public Object extractPrincipal(Map<String, Object> map) {
        String principalId = (String) map.get("id");
        if (principalId == null)
        	principalId = (String) map.get("sub");
        UserEntity user = userRepository.findByUsername(principalId);
        if (user == null) {
            logger.info("No user found, generating profile for {}", principalId);
            user = new UserEntity();
            user.setUsername(principalId);
            user.setEmail((String) map.get("email"));
            user.setFirstName((String) map.get("given_name"));
            user.setLastName((String) map.get("family_name"));
            user.setLoginType(UserLoginType.GOOGLE);  
            user.setEnabled(true);
            user.setPublicFlag(false);
            user.setRoles(Arrays.asList(roleRepository.findByRoleName("ROLE_USER")));
            userManager.createUser(user);
        } 
       
        return user;
	}
}
