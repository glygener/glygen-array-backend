package org.glygen.array.config;

import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.glygen.array.view.LoginRequest;
import org.springframework.http.HttpMethod;

import com.fasterxml.classmate.TypeResolver;

import springfox.documentation.builders.OperationBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiDescription;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ApiListingScannerPlugin;
import springfox.documentation.spi.service.contexts.DocumentationContext;
import springfox.documentation.spring.web.readers.operation.CachingOperationNameGenerator;

public class LoginEndpointsSwaggerScanner implements ApiListingScannerPlugin {
	  
	  public LoginEndpointsSwaggerScanner() {//<9>
	  }

	@Override
	public boolean supports(DocumentationType arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<ApiDescription> apply(DocumentationContext arg0) {
		// TODO Auto-generated method stub
		return null;
	}

/*	  @Override
	  public List<ApiDescription> apply(DocumentationContext context) {
	    return new ArrayList<ApiDescription>(
	        Arrays.asList( //<1>
	            new ApiDescription(
	            	"Login",
	                "/login",
	                "User Login ",
	                newArrayList(
	                		new OperationBuilder(new CachingOperationNameGenerator())
	                		        .method(HttpMethod.POST)
	                		        .uniqueId("login")
	                		        .parameters(Arrays.asList(new ParameterBuilder()
	                		            .name("loginrequest")
	                		            .required(true)
	                		            .description("The body of request")
	                		            .parameterType("body")            
	                		            .type(new TypeResolver().resolve(LoginRequest.class))
	                		            .modelRef(new ModelRef("LoginRequest"))
	                		            .build()))
	                		        .summary("Log in") // 
	                		        .notes("Here you can log in")
	                		        .build()
                        
                    ),
                    false),
	            new ApiDescription(
	            	"Login",
	                "/logout",
	                "User Logout",
	                Arrays.asList(
	                    new OperationBuilder(
	                    		new CachingOperationNameGenerator())
	                        .method(HttpMethod.GET)
	                        .uniqueId("logout")
	                        .notes("You can logout here")
	                        .summary("Log out") 
	                        .responseModel(new ModelRef("org.glygen.array.view.Confirmation"))
	                        .build()),
	                false)));
	  }

	  @Override
	  public boolean supports(DocumentationType delimiter) {
	    return DocumentationType.SWAGGER_2.equals(delimiter);
	  }*/
}
