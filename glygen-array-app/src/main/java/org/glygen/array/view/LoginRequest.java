package org.glygen.array.view;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginRequest", description = "Login request")
public class LoginRequest {
	@Schema
	public String username;
	@Schema
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
