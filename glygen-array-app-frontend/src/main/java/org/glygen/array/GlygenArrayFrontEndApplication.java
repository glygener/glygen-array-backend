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

import java.util.Date;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 
 * This application is a demonstration of OAuth2 and the GlycanBuilderVaadin integration.
 * 
 * @author aoki
 *
 */
@SpringBootApplication
@Controller
public class GlygenArrayFrontEndApplication implements WebMvcConfigurer {
	private static final Logger logger = LoggerFactory.getLogger(GlygenArrayFrontEndApplication.class);

	/**
	 * 
	 * Simple demonstration of Spring MVC.  Dummy bean data will be output to the /src/main/resources/templates/home.html
	 * 
	 * @param model
	 * @return home for home.html
	 */
	@GetMapping("/")
	public String home(Map<String, Object> model) {
		model.put("message", "Hello World");
		model.put("title", "Hello Home");
		model.put("date", new Date());
		return "home";
	}

	/**
	 * 
	 * Simple demonstration of Spring MVC.  Dummy bean data will be output to the /src/main/resources/templates/home.html
	 * 
	 * @param model
	 * @return home for home.html
	 */
	@GetMapping("/graphical")
	public String graphical(Map<String, Object> model) {
		return "glycanbuilder";
	}
	
	/**
	 * 
	 * Demonstration of exception handing and error page generation.
	 * 
	 * @return
	 */
	@RequestMapping("/foo")
	public String foo() {
		throw new RuntimeException("Expected exception in controller");
	}
	
	/**
	 * 
	 * Submission from glycanbuilder.
	 * 
	 * @return
	 */
	@RequestMapping("/Submit")
	public String submit(Model model, @RequestParam("sequenceInput") String sequenceInput) {
		logger.debug("sequenceInput:>" + sequenceInput + "<");
		model.addAttribute("sequenceInput", sequenceInput);
		return "completion";
	}

	/* (non-Javadoc)
	 * 
	 * Quick method of supporting adding a view for login.  HTML template in /src/main/resources/templates/login.html
	 * 
	 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurer#addViewControllers(org.springframework.web.servlet.config.annotation.ViewControllerRegistry)
	 */
	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/login").setViewName("login");
	}

	/**
	 * 
	 * Utilize Spring Boot to initialize spring and startup default a Java web application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		new SpringApplicationBuilder(GlygenArrayFrontEndApplication.class).run(args);
	}

	/**
	 * 
	 * Inner class configuration demonstrating Spring Security.  Detailed path and Oauth2 configuration setup is in configure method.
	 * 
	 * @author aoki
	 *
	 */
	@Configuration
	protected static class ApplicationSecurity extends WebSecurityConfigurerAdapter {

		/* (non-Javadoc)
		 * 
		 * @see org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter#configure(org.springframework.security.config.annotation.web.builders.HttpSecurity)
		 */
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests()
.antMatchers("/css/**").permitAll()
.antMatchers("/graphical**").permitAll()
.antMatchers("/complete**").permitAll()
.antMatchers("/GlycanBuilder**").permitAll()
.antMatchers("/Submit**").permitAll()
.anyRequest().fullyAuthenticated().and()
.formLogin().loginPage("/login").failureUrl("/login?error").and()
.oauth2Login().loginPage("/login").failureUrl("/login?error").permitAll().and().logout().permitAll();
		}
	}
}
