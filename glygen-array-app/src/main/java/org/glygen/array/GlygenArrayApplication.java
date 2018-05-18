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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;

import org.glygen.array.security.MyBasicAuthenticationEntryPoint;
import org.glygen.array.security.MyBasicAuthenticationFilter;
import org.glygen.array.security.MyOAuth2AuthenticationEntryPoint;
import org.glygen.array.security.MyOAuth2AuthenticationSuccessHandler;
import org.glygen.array.security.TokenAuthenticationFilter;
import org.glygen.array.service.GlygenUserDetailsService;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CompositeFilter;

@SpringBootApplication
@EnableOAuth2Sso
public class GlygenArrayApplication {
	
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
	            "/configuration/security",
	            "/swagger-ui.html",
	            "/webjars/**"
	            // other public endpoints of API may be appended to this array
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
		
		@Override
		protected void configure(HttpSecurity http) throws Exception {

			http.authorizeRequests()
			    .antMatchers(AUTH_WHITELIST).permitAll()
	            .antMatchers("/users/signup").permitAll()
	            .antMatchers("/login**").permitAll()
	            .antMatchers("/users/registrationConfirm*").permitAll()
	            .anyRequest().fullyAuthenticated()
	            .and().httpBasic().authenticationEntryPoint(new MyBasicAuthenticationEntryPoint())
	            .and().cors()
	            .and().csrf().disable()
			    .addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class);
			
			final TokenAuthenticationFilter tokenFilter = new TokenAuthenticationFilter(userService);
	        http.addFilterBefore(tokenFilter, BasicAuthenticationFilter.class);
	        //Creating token when basic authentication is successful and the same token can be used to authenticate for further requests
	        final MyBasicAuthenticationFilter customBasicAuthFilter = new MyBasicAuthenticationFilter(this.authenticationManager() );
	        http.addFilter(customBasicAuthFilter);
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
