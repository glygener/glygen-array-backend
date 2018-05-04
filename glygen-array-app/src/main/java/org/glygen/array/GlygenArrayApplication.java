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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glygen.array.security.MyBasicAuthenticationEntryPoint;
import org.glygen.array.security.MyOAuth2AuthenticationEntryPoint;
import org.glygen.array.security.MyOAuth2AuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CompositeFilter;

@SpringBootApplication
public class GlygenArrayApplication {
	
	@Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }

	public static void main(String[] args) {
		new SpringApplicationBuilder(GlygenArrayApplication.class).run(args);
	}
	

	@Configuration
	@EnableOAuth2Client
	protected static class ApplicationSecurity extends WebSecurityConfigurerAdapter {
		
		@Autowired
		OAuth2ClientContextFilter oAuth2ClientContextFilter;
		
		@Autowired
		OAuth2ClientContext oauth2ClientContext;
		
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
		
		@Override
		protected void configure(HttpSecurity http) throws Exception {

			http.authorizeRequests()
	            .antMatchers("/users/signup").permitAll()
	            .antMatchers("/login**").permitAll()
	            .antMatchers("/users/registrationConfirm*").permitAll()
	            .anyRequest().fullyAuthenticated()
	            .and().httpBasic().authenticationEntryPoint(new MyBasicAuthenticationEntryPoint())
	            .and().cors()
	            .and().csrf().disable()
			    .addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class);
						
		//			new GlygenOauth2AuthorizationFilter("/oauth/token") , BasicAuthenticationFilter.class);
			//http.authorizeRequests().antMatchers("/css/**").permitAll().anyRequest()
			//		.fullyAuthenticated().and().formLogin().loginPage("/login")
			//		.failureUrl("/login?error").permitAll().and().logout().permitAll();
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

		
	/*	@Configuration
		@EnableOAuth2Sso
		@Order(1)
		static class OAuth2SecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {			
			@Autowired
			OAuth2ClientContextFilter oAuth2ClientContextFilter;
			
			@Autowired
			OAuth2ClientContext oauth2ClientContext;
			
			@Bean
			@ConfigurationProperties("google")
			public ClientResources google() {
				return new ClientResources();
			}
			
			@Bean
			AuthenticationEntryPoint authenticationEntryPoint() {
				return new MyOAuth2AuthenticationEntryPoint();
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
				UserInfoTokenServices tokenServices = new UserInfoTokenServices(client.getResource().getUserInfoUri(),
						client.getClient().getClientId());
				tokenServices.setRestTemplate(oAuth2RestTemplate);
				oAuth2ClientAuthenticationFilter.setTokenServices(tokenServices);
				return oAuth2ClientAuthenticationFilter;
			}
		//	@Bean
		//	OpenIDConnectAuthenticationFilter openIdConnectAuthenticationFilter() {
		// OpenIDConnectAuthenticationFilter(LOGIN_URL)
		//	}
			
			@Override
			protected void configure(HttpSecurity http) throws Exception {
				http
					.antMatcher("/oauth2*")
					.addFilterAfter(oAuth2ClientContextFilter, AbstractPreAuthenticatedProcessingFilter.class)
				//	.addFilterAfter(openIdConnectAuthenticationFilter(), OAuth2ClientContextFilter.class)
				.exceptionHandling().authenticationEntryPoint(authenticationEntryPoint())
				.and()
					.authorizeRequests()
						.anyRequest().fullyAuthenticated();
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
		
		
		
		@Configuration
		@Order(2)
		static class BasicAuthWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
			
			@Autowired
			OAuth2ClientContextFilter oAuth2ClientContextFilter;
			
			@Autowired
			OAuth2ClientContext oauth2ClientContext;
			
			@Bean
			@ConfigurationProperties("google")
			public ClientResources google() {
				return new ClientResources();
			}
			
			@Bean
			AuthenticationEntryPoint authenticationEntryPoint() {
				return new MyOAuth2AuthenticationEntryPoint();
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
				UserInfoTokenServices tokenServices = new UserInfoTokenServices(client.getResource().getUserInfoUri(),
						client.getClient().getClientId());
				tokenServices.setRestTemplate(oAuth2RestTemplate);
				oAuth2ClientAuthenticationFilter.setTokenServices(tokenServices);
				return oAuth2ClientAuthenticationFilter;
			}
		
			@Bean
		    CorsConfigurationSource corsConfigurationSource() {
		        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		        source.registerCorsConfiguration("/**", new CorsConfiguration().applyPermitDefaultValues());
		        return source;
		    }
			
			@Override
			protected void configure(HttpSecurity http) throws Exception {
	
				http.authorizeRequests()
		            .antMatchers("/signup").permitAll()
		            .antMatchers("/login**").permitAll()
		            .antMatchers("/registrationConfirm*").permitAll()
		            .anyRequest().fullyAuthenticated()
		            .and().httpBasic().authenticationEntryPoint(new MyBasicAuthenticationEntryPoint())
		            .and().cors()
		            .and().csrf().disable()
				    .addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class);
				
			//			new GlygenOauth2AuthorizationFilter("/oauth/token") , BasicAuthenticationFilter.class);
				//http.authorizeRequests().antMatchers("/css/**").permitAll().anyRequest()
				//		.fullyAuthenticated().and().formLogin().loginPage("/login")
				//		.failureUrl("/login?error").permitAll().and().logout().permitAll();
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
		
		*/

}
