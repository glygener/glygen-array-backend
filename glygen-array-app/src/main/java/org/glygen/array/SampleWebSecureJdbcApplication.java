/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glygen.array;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.RoleRepository;
import org.glygen.array.persistence.dao.VerificationTokenRepository;
import org.glygen.array.service.EmailManager;
import org.glygen.array.service.GlygenUserDetailsService;
import org.glygen.array.service.UserManager;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.Greeting;
import org.glygen.array.view.User;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import ch.qos.logback.classic.Logger;

@SpringBootApplication
@RestController
public class SampleWebSecureJdbcApplication {
	
	public static Logger logger=(Logger) LoggerFactory.getLogger(SampleWebSecureJdbcApplication.class);

	@Autowired
	UserManager userManager;
	
	@Autowired
	RoleRepository roleRepository;
	
	@Autowired
	EmailManager emailManager;
	
	@Autowired
	VerificationTokenRepository tokenRepository;
	
	@GetMapping("/")
	public Greeting home() {
		Greeting greet = new Greeting("Hello World", "Hello Home", new Date());
		return greet;
	}

	@RequestMapping("/foo")
	public String foo() {
		throw new RuntimeException("Expected exception in controller");
	}
	
	@RequestMapping(value = "/signup", method = RequestMethod.POST, 
    		consumes={"application/xml", "application/json"})
	public Confirmation signup (HttpServletRequest request, @RequestBody(required=true) User user) {
		UserEntity newUser = new UserEntity();
		newUser.setUsername(user.getUserName());		
		newUser.setPassword(user.getPassword());
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
        emailManager.sendVerificationToken(newUser, request.getContextPath());
        logger.info("New user {} is added to the system", newUser.getUsername());
        return new Confirmation("User added successfully", HttpStatus.CREATED.value());
	}
	
	@GetMapping(value = "/registrationConfirm")
    public Confirmation confirmRegistration(@RequestParam("token") final String token) throws UnsupportedEncodingException {
        final String result = userManager.validateVerificationToken(token);
        if (result.equals("valid")) {
            final UserEntity user = userManager.getUserByToken(token);
            return new Confirmation("User is confirmed", HttpStatus.OK.value());
        }
        return new Confirmation("User verification link is expired", HttpStatus.EXPECTATION_FAILED.value());
    }
	
	@GetMapping("/login")
	public @ResponseBody Confirmation signin() {
		return new Confirmation("User is authorized", HttpStatus.ACCEPTED.value());
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(SampleWebSecureJdbcApplication.class).run(args);
	}

	@Configuration
	protected static class ApplicationSecurity extends WebSecurityConfigurerAdapter {
		
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			 http.authorizeRequests()
	            .antMatchers("/signup").permitAll()
	            .antMatchers("/registrationConfirm*").permitAll()
	            .antMatchers("/login").hasAuthority("ROLE_USER")
	            .anyRequest().fullyAuthenticated()
	            .and().httpBasic()
	            .and().csrf().disable();
			//http.authorizeRequests().antMatchers("/css/**").permitAll().anyRequest()
			//		.fullyAuthenticated().and().formLogin().loginPage("/login")
			//		.failureUrl("/login?error").permitAll().and().logout().permitAll();
		}

	}

}
