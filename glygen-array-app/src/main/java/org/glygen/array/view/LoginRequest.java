package org.glygen.array.view;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "LoginRequest", description = "Login request")
public class LoginRequest {
	@ApiModelProperty
	public String username;
	@ApiModelProperty
    public String password;
	
    public String getUsername() {
		return username;
	}
    
    public String getPassword() {
		return password;
	}
    
    public void setUsername(String username) {
		this.username = username;
	}
    
    public void setPassword(String password) {
		this.password = password;
	}
}
