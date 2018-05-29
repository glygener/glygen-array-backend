package org.glygen.array.config;

import static com.google.common.collect.Lists.newArrayList;
import static springfox.documentation.builders.PathSelectors.ant;

import java.util.List;

import org.glygen.array.view.LoginRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.classmate.TypeResolver;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ImplicitGrantBuilder;
import springfox.documentation.builders.OAuthBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.GrantType;
import springfox.documentation.service.LoginEndpoint;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.ApiKeyVehicle;
import springfox.documentation.swagger.web.SecurityConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {    
	
	@Value("${glygen.host}")
	private String host;
	
	@Value("${glygen.basePath}")
	private String basePath;
	
	@Value("${google.client.client-id}")
    private String swaggerAppClientId;

    @Value("${google.client.client-secret}")
    private String swaggerClientSecret;

    @Value("${google.client.accessTokenUri}")
    private String swaggerTokenURL;
    
    @Bean
    public Docket api() { 
        return new Docket(DocumentationType.SWAGGER_2)
           .apiInfo(apiInfo())
          .host(host)
          .pathMapping(basePath)
          .additionalModels(new TypeResolver().resolve(LoginRequest.class))
          .select()                                  
          .apis( RequestHandlerSelectors.basePackage( "org.glygen.array" ) )          
          .paths(PathSelectors.any())  
          .build()
          .securitySchemes(newArrayList(apiKey()))
          .securityContexts(newArrayList(securityContext()));
    }
    
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Glygen Array Repository")
                .description("Glygen Array Repository Web Services")
                .build();
    }
    
    private ApiKey apiKey() {
        return new ApiKey("Bearer", "Authorization", "header");
    }
    
    @Bean
    public SecurityConfiguration security() {
    	return new SecurityConfiguration(null, null, "Realm",
    			"Glygen",
    			"Bearer",
    			ApiKeyVehicle.HEADER, "Authorization",
    			null);
    }
    
    @Bean
    SecurityScheme oauth() {
        return new OAuthBuilder()
                .name("google-oauth")
                .grantTypes(grantTypes())
                .scopes(scopes())
                .build();
    }
    
    List<AuthorizationScope> scopes() {
        return newArrayList(
                new AuthorizationScope("write:glygenarray", "modify array data"),
                new AuthorizationScope("read:glygenarray", "read array data"));
    }

    List<GrantType> grantTypes() {
        GrantType grantType = new ImplicitGrantBuilder()
                .loginEndpoint(new LoginEndpoint(host + "/login/google"))
                .build();
        return newArrayList(grantType);
    }
    
    @Bean
    SecurityContext securityContext() {
        SecurityReference securityReference = SecurityReference.builder()
                .reference("google_auth")
                .scopes(scopes().toArray(new AuthorizationScope[2]))
                .build();
        
        SecurityReference securityReference2 = SecurityReference.builder()
                .reference("Bearer")
                .scopes(scopes().toArray(new AuthorizationScope[2]))
                .build();

        return SecurityContext.builder()
                .securityReferences(newArrayList(securityReference2))
                .forPaths(ant("/array**"))
                .forPaths(ant("/users/get**"))
                .build();
    }
}
