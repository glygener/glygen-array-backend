package org.glygen.array.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {    
    
    @Value("${glygen.host}")
    private String host;
    
    @Value("${glygen.basePath}")
    private String basePath;
    
    @Value("${glygen.scheme}")
    private String scheme;
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Glygen Array Repository")
                .description("Glygen Array Repository Web Services"))
                .addServersItem(new Server().url(scheme + host + basePath))
                .components(new Components()
                        .addSecuritySchemes("bearer-key",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER).name("Authorization")));
    }
}
