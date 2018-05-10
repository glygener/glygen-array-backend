package org.glygen.array.config;

import static com.google.common.collect.Lists.newArrayList;
import static springfox.documentation.builders.PathSelectors.ant;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ImplicitGrantBuilder;
import springfox.documentation.builders.OAuthBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.BasicAuth;
import springfox.documentation.service.GrantType;
import springfox.documentation.service.LoginEndpoint;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
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
          .host(host)
          .pathMapping(basePath)
          .select()                                  
          .apis(RequestHandlerSelectors.any())              
          .paths(PathSelectors.any())                          
          .build()
          .securitySchemes(newArrayList(oauth(), new BasicAuth("basic")))
          .securityContexts(newArrayList(securityContext()));
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
                .reference("basic")
                .scopes(scopes().toArray(new AuthorizationScope[2]))
                .build();

        return SecurityContext.builder()
                .securityReferences(newArrayList(securityReference, securityReference2))
                .forPaths(ant("/array*"))
                .build();
    }
}
