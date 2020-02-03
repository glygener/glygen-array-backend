package org.glygen.array.config;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.glygen.array.logging.filter.GlygenRequestAndResponseLoggingFilter;
import org.glygen.array.persistence.SettingEntity;
import org.glygen.array.persistence.dao.SettingsRepository;
import org.glygen.array.security.GooglePrincipalExtractor;
import org.glygen.array.security.MyOAuth2AuthenticationEntryPoint;
import org.glygen.array.security.MyOAuth2AuthenticationSuccessHandler;
import org.glygen.array.security.MyOAuth2ClientAuthenticationProcessingFilter;
import org.glygen.array.security.MyUsernamePasswordAuthenticationFilter;
import org.glygen.array.security.TokenAuthenticationFilter;
import org.glygen.array.security.validation.AccessTokenValidator;
import org.glygen.array.security.validation.GoogleAccessTokenValidator;
import org.glygen.array.service.GlygenUserDetailsService;
import org.glygen.array.util.GlytoucanUtil;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
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
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CompositeFilter;

import com.fasterxml.jackson.databind.ObjectMapper;


@Configuration
@Order(1)
public class ApplicationSecurity extends WebSecurityConfigurerAdapter {
	
	final static Logger logger = LoggerFactory.getLogger("event-logger");
	
	public static final String[] AUTH_WHITELIST = {
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
	
	@Value("${glygen.token-secret}")
	String tokenSecret;
	
	@Value("${glygen.basePath}")
	private String basePath;
	
	@Value("${glytoucan.api-key}")
	String apiKey;
	
	@Value("${glytoucan.user-id}")
	String userId;
	
	@Autowired
	SettingsRepository settingsRepository;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Bean
	public FilterRegistrationBean loggingFilter() {
 	    GlygenRequestAndResponseLoggingFilter loggingFilter = new GlygenRequestAndResponseLoggingFilter();
 	    final FilterRegistrationBean registration = new FilterRegistrationBean();
 	    registration.setFilter(loggingFilter);
 	    registration.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
 	    registration.addUrlPatterns("/users/*");
 	    registration.addUrlPatterns("/array/*");
 	    registration.addUrlPatterns("/login");
 	    registration.addUrlPatterns(basePath + "login/*");
 	    registration.setOrder(1);
 	    return registration;
 	}
	
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
	public PrincipalExtractor googleExtractor() {
		return new GooglePrincipalExtractor();
	}
	
	@Bean
    public AccessTokenValidator googleTokenValidator() {
		ClientResources clientResources = google();
        GoogleAccessTokenValidator accessTokenValidator = new GoogleAccessTokenValidator();
        accessTokenValidator.setClientId(clientResources.getClient().getClientId());
        accessTokenValidator.setCheckTokenUrl(clientResources.getResource().getTokenInfoUri());
        return accessTokenValidator;
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
	
	/*
	 * add more validators as it becomes necessary - with each 3rd party authorization server inclusion
	 */
	private List<AccessTokenValidator> accessTokenValidators() {
		List<AccessTokenValidator> myList = new ArrayList<>();
		myList.add(googleTokenValidator());
		return myList;
	}
	
	/*
	 * add more filter as it becomes necessary - with each 3rd party authorization server inclusion
	 */
	private Filter ssoFilter() {
		CompositeFilter filter = new CompositeFilter();
		List<Filter> filters = new ArrayList<>();
		filters.add(ssoFilter(google(), googleExtractor(), "/login/google"));
		filter.setFilters(filters);
		return filter;
	}

	private Filter ssoFilter(ClientResources client, PrincipalExtractor extractor, String path) {
		OAuth2ClientAuthenticationProcessingFilter oAuth2ClientAuthenticationFilter = new MyOAuth2ClientAuthenticationProcessingFilter(path);
		OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(client.getClient(), oauth2ClientContext);
		oAuth2ClientAuthenticationFilter.setRestTemplate(oAuth2RestTemplate);
		oAuth2ClientAuthenticationFilter.setAuthenticationSuccessHandler(oAuth2authenticationSuccessHandler());
		UserInfoTokenServices tokenServices = new UserInfoTokenServices(client.getResource().getUserInfoUri(),
				client.getClient().getClientId());
		tokenServices.setRestTemplate(oAuth2RestTemplate);
		tokenServices.setPrincipalExtractor(extractor);
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
        CorsConfiguration config = new CorsConfiguration();
        config.applyPermitDefaultValues();
        config.addAllowedMethod(HttpMethod.DELETE);
        config.addAllowedMethod(HttpMethod.PUT);
        source.registerCorsConfiguration("/**", config);
        return source;
    }
	
	@Bean
    public MyUsernamePasswordAuthenticationFilter authenticationFilter() throws Exception {
		SettingEntity tokenExpirationSetting = settingsRepository.findByName("token.expiration");
		long expiration = SecurityConstants.EXPIRATION_TIME;
		if (tokenExpirationSetting != null) {
			try {
				expiration = Long.parseLong(tokenExpirationSetting.getValue());
			} catch (NumberFormatException e) {
				logger.warn("Setting for token.expiration is not a valid value (expected long): " + tokenExpirationSetting.getValue());
			}
		}
		MyUsernamePasswordAuthenticationFilter authenticationFilter
            = new MyUsernamePasswordAuthenticationFilter(this.authenticationManager());
        authenticationFilter.setAuthenticationSuccessHandler(this::loginSuccessHandler);
        authenticationFilter.setAuthenticationFailureHandler(this::loginFailureHandler);
        authenticationFilter.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/login", "POST"));
        authenticationFilter.setAuthenticationManager(authenticationManagerBean());
        authenticationFilter.setExpiration(expiration);
        return authenticationFilter;
    }
	
	private void loginSuccessHandler(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication) throws IOException {
		response.setStatus(HttpStatus.OK.value());
		
		Confirmation confirmation = new Confirmation("User is authorized", HttpStatus.OK.value());
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
		} else {
	        ObjectMapper jsonMapper = new ObjectMapper();          
			response.setContentType("application/json;charset=UTF-8");         
			PrintWriter out = response.getWriter();  
			out.print(jsonMapper.writeValueAsString(confirmation));   
		}
    }
 
    private void loginFailureHandler(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException e) throws IOException {
    	
    	ErrorMessage errorMessage = new ErrorMessage();
	    errorMessage.setStatus(HttpStatus.UNAUTHORIZED.value());
	    
	    if (e instanceof DisabledException)
	    	errorMessage.setErrorCode(ErrorCodes.DISABLED);
	    else
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
		
		// settings for GlytoucanUtil
		GlytoucanUtil.getInstance().setApiKey(apiKey);
		GlytoucanUtil.getInstance().setUserId(userId);
		
		List<RequestMatcher> matcherList = new ArrayList<>();
		for(int i=0; i < AUTH_WHITELIST.length; i++) {
			RequestMatcher newMatcher = new AntPathRequestMatcher(AUTH_WHITELIST[i]);
			matcherList.add(newMatcher);
		}
		RequestMatcher ignored = new OrRequestMatcher(matcherList);
		RequestMatcher PUBLIC_URLS = new OrRequestMatcher(ignored, 
				new AntPathRequestMatcher("/error"),
				new AntPathRequestMatcher("/login**"),
				//new AntPathRequestMatcher(basePath + "login**"),
				//new AntPathRequestMatcher("**/login**"),
				new AntPathRequestMatcher("/users/signup"),
				new AntPathRequestMatcher("/users/availableUsername"),
				new AntPathRequestMatcher("/users/recover"),
				new AntPathRequestMatcher("/users/**/password", HttpMethod.GET.name()),
				new AntPathRequestMatcher("/glycan/parseSequence"),
				new AntPathRequestMatcher("/array/getGlycanFromGlytoucan/**"),
				new AntPathRequestMatcher("/array/getimage/**"),
				new AntPathRequestMatcher("/array/public/**"),
				new AntPathRequestMatcher("/array/getlinkerFromPubChem/**"),
				new AntPathRequestMatcher("/array/getSequenceFromUniprot/**"),
				new AntPathRequestMatcher("/array/getPublicationFromPubmed/**"),
				new AntPathRequestMatcher("/array/getLinkerClassifications"),
				new AntPathRequestMatcher("/users/registrationConfirm"));
		
		final RequestMatcher PROTECTED_URLS = new NegatedRequestMatcher(PUBLIC_URLS);
		final TokenAuthenticationFilter tokenFilter = new TokenAuthenticationFilter(userService, PROTECTED_URLS, tokenSecret);
		tokenFilter.setValidators(accessTokenValidators());
		
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		
		http.cors().and().authorizeRequests()
		    .requestMatchers(PUBLIC_URLS).permitAll()
            .anyRequest().fullyAuthenticated()
            .and().csrf().disable()
            .httpBasic().disable()
		    .addFilterBefore(ssoFilter(), UsernamePasswordAuthenticationFilter.class)
		    .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)
	        .addFilterBefore(authenticationFilter(), UsernamePasswordAuthenticationFilter.class)
		    .logout()
            .logoutUrl("/logout")
            .logoutSuccessHandler(this::logoutSuccessHandler);
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
