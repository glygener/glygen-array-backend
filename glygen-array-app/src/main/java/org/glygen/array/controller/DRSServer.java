package org.glygen.array.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.glygen.array.drs.AccessMethod;
import org.glygen.array.drs.AccessURL;
import org.glygen.array.drs.DrsError;
import org.glygen.array.drs.DrsObject;
import org.glygen.array.drs.ServiceInfo;
import org.glygen.array.persistence.rdf.data.Checksum;
import org.glygen.array.persistence.rdf.data.FileWrapper;
import org.glygen.array.service.GlygenArrayRepository;
import org.glygen.array.view.ErrorMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    
    @Autowired
    @Qualifier("glygenArrayRepositoryImpl")
    GlygenArrayRepository repository;
    
    
    @Operation(summary = "DRS API service information")
    @RequestMapping(value="/service-info", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="service information retrieved successfully", content = {
            @Content( schema = @Schema(implementation = ServiceInfo.class))}), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public ServiceInfo getServiceInfo() {
        ServiceInfo info = new ServiceInfo();
        info.setDocumentationUrl(scheme+host+basePath+"/swagger-ui/index.html#/drs-server/");
        return info;
    }
    
    @Operation(summary = "Retrieve DRS object")
    @RequestMapping(value="/objects/{object_id}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Drs object retrieved successfully", content = {
            @Content( schema = @Schema(implementation = DrsObject.class))}), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public ResponseEntity<Object>  getDrsObject(
            @Parameter(required=true, description="id of drs object to be retrieved") 
            @PathVariable("object_id") String id) {
        if (id == null || id.isEmpty()) {
            DrsError error = new DrsError("Invalid object id", 400);
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
        String identifier = id;
        if (id.contains("-")) {
            // separate dataset id and file identifier
            identifier = id.split("-")[1];
        }
        try {
            FileWrapper file = repository.getFileByIdentifier(identifier, null);
            if (file == null) {
                DrsError error = new DrsError("The requested object is not found in the repository", 404);
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
            }
            DrsObject drs = new DrsObject();
            drs.setId(identifier);
            drs.setName("File_" + identifier);
            drs.setCreated_time(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(file.getCreatedDate() != null ? file.getCreatedDate() : new Date()));
            drs.setDescription(file.getDescription());
            drs.setSize(file.getFileSize().intValue());
            List<Checksum> checksums = new ArrayList<Checksum>();
            if (file.getChecksum() == null) {
                GlygenArrayController.calculateChecksum(file);
            }
            checksums.add(file.getChecksum());
            drs.setChecksums(checksums);
            AccessMethod method = new AccessMethod();
            method.setType(scheme);
            method.setAccess_id(identifier);
            List<AccessMethod> accessMethods = new ArrayList<AccessMethod>();
            accessMethods.add(method);
            drs.setAccessMethods(accessMethods);
            drs.setSelf_uri("drs://"+host+basePath+identifier);
            return new ResponseEntity<>(drs, HttpStatus.OK);
        } catch (Exception e) {
            DrsError error = new DrsError(e.getMessage(), 500);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    @Operation(summary = "Retrieve DRS object's access url")
    @RequestMapping(value="/objects/{object_id}/access/{access_id}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Access URL retrieved successfully", content = {
            @Content( schema = @Schema(implementation = DrsObject.class))}), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public ResponseEntity<Object>  getAccessURL(
            @Parameter(required=true, description="id of drs object") 
            @PathVariable("object_id") String id, 
            @Parameter(required=true, description="access id of drs object to be retrieved") 
            @PathVariable("access_id") String accessId) {
        
        if (id == null || id.isEmpty() || accessId == null || accessId.isEmpty()) {
            DrsError error = new DrsError("Invalid object id", 400);
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
        
        try {
            FileWrapper file = repository.getFileByIdentifier(accessId, null);
            if (file == null) {
                DrsError error = new DrsError("The requested file is not found in the repository", 404);
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
            }
            AccessURL accessURL = new AccessURL();
            accessURL.setUrl(scheme+host+basePath+"array/public/download?fileFolder=" + file.getFileFolder() + "&fileIdentifier=" + accessId);
            return new ResponseEntity<>(accessURL, HttpStatus.OK);
        } catch (Exception e) {
            DrsError error = new DrsError(e.getMessage(), 500);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
    }
}
