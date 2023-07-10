package org.glygen.array.controller;

import org.glygen.array.drs.Organization;
import org.glygen.array.drs.ServiceInfo;
import org.glygen.array.drs.ServiceType;
import org.glygen.array.view.ErrorMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/ga4gh/drs/v1")
public class DRSServer {
    
    @Value("${glygen.scheme}")
    String scheme;
    
    @Value("${glygen.host}")
    String host;
    
    @Value("${glygen.basePath}")
    String basePath;
    
    
    @Operation(summary = "DRS API service information")
    @RequestMapping(value="/service-info", method = RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="service information retrieved successfully", content = {
            @Content( schema = @Schema(implementation = ServiceInfo.class))}), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public ServiceInfo getServiceInfo() {
        ServiceInfo info = new ServiceInfo();
        info.setId("org.glygen.array");
        info.setName("Glycan Array Repository DRS API");
        info.setType(new ServiceType());
        Organization org = new Organization();
        org.setName("University of Georgia");
        org.setUrl("https://glygen.ccrc.uga.edu");
        info.setOrganization(org);
        info.setVersion("1.0.0");
        info.setDocumentationUrl(scheme+host+basePath+"swagger-ui/index.html#/drs-server/");
        info.setContactUrl("mailto:glygenarray.api@gmail.com");
        return info;
    }
}
