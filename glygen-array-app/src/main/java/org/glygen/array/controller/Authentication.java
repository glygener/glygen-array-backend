package org.glygen.array.controller;

import org.glygen.array.view.LoginRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Authentication API specification for Swagger documentation and Code Generation.
 * Implemented by Spring Security.
 */
@Api("Authentication")
@Controller
@RequestMapping(value = "/", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class Authentication {
    /**
     * Implemented by Spring Security
     */
    @ApiOperation(value = "Login", notes = "Login with the given credentials.")
    @ApiResponses({@ApiResponse(code = 200, message = "")})
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public void login(@RequestBody LoginRequest loginRequest)
    {
        throw new IllegalStateException("Add Spring Security to handle authentication");
    }

    /**
     * Implemented by Spring Security
     */
    @ApiOperation(value = "Logout", notes = "Logout the current user.")
    @ApiResponses({@ApiResponse(code = 200, message = "")})
    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public void logout() {
        throw new IllegalStateException("Add Spring Security to handle authentication");
    }
}