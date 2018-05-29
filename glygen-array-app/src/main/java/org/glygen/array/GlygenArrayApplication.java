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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.glygen.array.security.MyOAuth2AuthenticationEntryPoint;
import org.glygen.array.security.MyOAuth2AuthenticationSuccessHandler;
import org.glygen.array.security.MyUsernamePasswordAuthenticationFilter;
import org.glygen.array.security.TokenAuthenticationFilter;
import org.glygen.array.service.GlygenUserDetailsService;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CompositeFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Logger;

@SpringBootApplication
@EnableOAuth2Sso
public class GlygenArrayApplication {
	public static Logger logger=(Logger) LoggerFactory.getLogger(GlygenArrayApplication.class);
	
	@Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }

	public static void main(String[] args) {
		new SpringApplicationBuilder(GlygenArrayApplication.class).run(args);
	}
	

	@Configuration
	@Order(1)
	protected static class ApplicationSecurity extends WebSecurityConfigurerAdapter {
		
		private static final String[] AUTH_WHITELIST = {
	            // -- swagger ui
	            "/v2/api-docs",
	            "/swagger-resources",
	            "/swagger-resources/**",
	            "/configuration/ui",
	            "/configuration/**",
	            "/swagger-ui.html",
	            "/swagger-ui.html/**",
	            "/webjars/**"
	    };
		
		@Autowired
		OAuth2ClientContextFilter oAuth2ClientContextFilter;
		
		@Autowired
		OAuth2ClientContext oauth2ClientContext;
		
		@Autowired 
		AuthenticationManager authenticationManager;
		
		@Autowired 
		GlygenUserDetailsService userService;
		
		@Bean
	    @Override
	    public AuthenticationManager authenticationManagerBean() throws Exception {
	        return super.authenticationManagerBean();
	    }
		
		@Bean
		@ConfigurationProperties("google")
		public ClientResources google() {
			return new ClientResources();
		}
		
		@Bean
		AuthenticationEntryPoint authenticationEntryPoint() {
			return new MyOAuth2AuthenticationEntryPoint();
		}
		
		@Bean
		public FilterRegistrationBean<OAuth2ClientContextFilter> oauth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
			FilterRegistrationBean<OAuth2ClientContextFilter> registration = new FilterRegistrationBean<OAuth2ClientContextFilter>();
			registration.setFilter(filter);
			registration.setOrder(-100);
			return registration;
		}
		
		private Filter ssoFilter() {
			CompositeFilter filter = new CompositeFilter();
			List<Filter> filters = new ArrayList<>();
			filters.add(ssoFilter(google(), "/login/google"));
			filter.setFilters(filters);
			return filter;
		}

		private Filter ssoFilter(ClientResources client, String path) {
			OAuth2ClientAuthenticationProcessingFilter oAuth2ClientAuthenticationFilter = new OAuth2ClientAuthenticationProcessingFilter(path);
			OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(client.getClient(), oauth2ClientContext);
			oAuth2ClientAuthenticationFilter.setRestTemplate(oAuth2RestTemplate);
			oAuth2ClientAuthenticationFilter.setAuthenticationSuccessHandler(oAuth2authenticationSuccessHandler());
			UserInfoTokenServices tokenServices = new UserInfoTokenServices(client.getResource().getUserInfoUri(),
					client.getClient().getClientId());
			tokenServices.setRestTemplate(oAuth2RestTemplate);
			oAuth2ClientAuthenticationFilter.setTokenServices(tokenServices);
			return oAuth2ClientAuthenticationFilter;
		}
		
		@Bean
		SimpleUrlAuthenticationSuccessHandler oAuth2authenticationSuccessHandler() {
			return new MyOAuth2AuthenticationSuccessHandler();
		}
	
		@Bean
	    CorsConfigurationSource corsConfigurationSource() {
	        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	        source.registerCorsConfiguration("/**", new CorsConfiguration().applyPermitDefaultValues());
	        return source;
	    }
		
		@Bean
	    public MyUsernamePasswordAuthenticationFilter authenticationFilter() throws Exception {
			MyUsernamePasswordAuthenticationFilter authenticationFilter
	            = new MyUsernamePasswordAuthenticationFilter(this.authenticationManager());
	        authenticationFilter.setAuthenticationSuccessHandler(this::loginSuccessHandler);
	        authenticationFilter.setAuthenticationFailureHandler(this::loginFailureHandler);
	        authenticationFilter.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/login", "POST"));
	        authenticationFilter.setAuthenticationManager(authenticationManagerBean());
	        return authenticationFilter;
	    }
		
		private void loginSuccessHandler(
	        HttpServletRequest request,
	        HttpServletResponse response,
	        Authentication authentication) throws IOException {
			response.setStatus(HttpStatus.OK.value());
	        ObjectMapper jsonMapper = new ObjectMapper();          
			response.setContentType("application/json;charset=UTF-8");         
			PrintWriter out = response.getWriter();  
			Confirmation confirmation = new Confirmation("User is authorized", HttpStatus.OK.value());
			out.print(jsonMapper.writeValueAsString(confirmation));   
	    }
	 
	    private void loginFailureHandler(
	        HttpServletRequest request,
	        HttpServletResponse response,
	        AuthenticationException e) throws IOException {
	    	
	    	ErrorMessage errorMessage = new ErrorMessage();
		    errorMessage.setStatus(HttpStatus.UNAUTHORIZED.value());
		    errorMessage.setErrorCode(ErrorCodes.UNAUTHORIZED);
		    
	    	String acceptString = request.getHeader("Accept");
			if (acceptString.contains("xml")) {
				response.setContentType("application/xml;charset=UTF-8");
				response.setStatus(HttpStatus.UNAUTHORIZED.value());           
				PrintWriter out = response.getWriter();    
				try {
					JAXBContext errorContext = JAXBContext.newInstance(ErrorMessage.class);
					Marshaller errorMarshaller = errorContext.createMarshaller();
					errorMarshaller.marshal(errorMessage, out);  
				} catch (JAXBException jex) {
					logger.error("Cannot generate error message in xml", jex);
				}
			} else if (acceptString.contains("json")) {
		        response.setStatus(HttpStatus.UNAUTHORIZED.value());
		        ObjectMapper jsonMapper = new ObjectMapper();          
				response.setContentType("application/json;charset=UTF-8");                 
				PrintWriter out = response.getWriter();   
				out.print(jsonMapper.writeValueAsString(errorMessage));  
			}
	    }
	    
	    private void logoutSuccessHandler(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
            
	    	response.setStatus(HttpStatus.OK.value());
            Confirmation confirmation = new Confirmation("User is signed out", HttpStatus.OK.value());
            String acceptString = request.getHeader("Accept");
			if (acceptString.contains("xml")) {
				response.setContentType("application/xml;charset=UTF-8");
				PrintWriter out = response.getWriter();    
				try {
					JAXBContext errorContext = JAXBContext.newInstance(Confirmation.class);
					Marshaller errorMarshaller = errorContext.createMarshaller();
					errorMarshaller.marshal(confirmation, out);  
				} catch (JAXBException jex) {
					logger.error("Cannot generate error message in xml", jex);
				}
			} else if (acceptString.contains("json")) {
				response.setContentType("application/json;charset=UTF-8");          
	            ObjectMapper jsonMapper = new ObjectMapper();
	            jsonMapper.writeValue(response.getWriter(), confirmation);
			}
        }
		
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			List<RequestMatcher> matcherList = new ArrayList<>();
			for(int i=0; i < AUTH_WHITELIST.length; i++) {
				RequestMatcher newMatcher = new AntPathRequestMatcher(AUTH_WHITELIST[i]);
				matcherList.add(newMatcher);
			}
			RequestMatcher ignored = new OrRequestMatcher(matcherList);
			final TokenAuthenticationFilter tokenFilter = new TokenAuthenticationFilter(userService, ignored);
			
			http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
			
			http.cors().and().authorizeRequests()
			    .antMatchers(AUTH_WHITELIST).permitAll()
			    .antMatchers("/error").permitAll()
	            .antMatchers("/users/signup").permitAll()
	            .antMatchers("/users/recover").permitAll()
	            .antMatchers("/users/**/password").permitAll()
	            .antMatchers("/login**").permitAll()
	            .antMatchers("/users/registrationConfirm*").permitAll()
	            .anyRequest().fullyAuthenticated()
	            .and().csrf().disable()
	            .httpBasic().disable()
			    .addFilterBefore(ssoFilter(), UsernamePasswordAuthenticationFilter.class)
			    .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)
		        .addFilterBefore(authenticationFilter(), UsernamePasswordAuthenticationFilter.class)
			    .logout()
	            .logoutUrl("/logout")
	            .logoutSuccessHandler(this::logoutSuccessHandler);
			
			
	       // http.addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);
	      //  http.addFilterBefore(authenticationFilter(), UsernamePasswordAuthenticationFilter.class);
	        //Creating token when basic authentication is successful and the same token can be used to authenticate for further requests
	       // final MyBasicAuthenticationFilter customBasicAuthFilter = new MyBasicAuthenticationFilter(this.authenticationManager() );
	        //http.addFilter(customBasicAuthFilter);
		}
		
		@Override
		public void configure(WebSecurity web) throws Exception {
			web.ignoring().antMatchers(AUTH_WHITELIST);
		}
		
		class ClientResources {

			@NestedConfigurationProperty
			private AuthorizationCodeResourceDetails client = new AuthorizationCodeResourceDetails();

			@NestedConfigurationProperty
			private ResourceServerProperties resource = new ResourceServerProperties();

			public AuthorizationCodeResourceDetails getClient() {
				return client;
			}

			public ResourceServerProperties getResource() {
				return resource;
			}
		}
	}
}
