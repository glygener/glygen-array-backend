package org.glygen.array.controller;

import java.security.Principal;

import javax.persistence.EntityNotFoundException;

import org.glygen.array.persistence.cfgdata.CFGExperimentRepository;
import org.glygen.array.persistence.cfgdata.Experiment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/cfg")
public class CFGController {
    
    @Autowired
    CFGExperimentRepository cfgRepository;
    
    @Operation(summary = "retrieve experiment data (with sample information) for the given experiment", 
            security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/getSampleData", method = RequestMethod.GET)
    @ApiResponses(value ={@ApiResponse(responseCode="200", description="Successfully retrieved experiment data"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve info"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Experiment getSampleDataForExperiment (
            @Parameter(required=true, description="primscreen number for the CFG experiment")
            @RequestParam 
            String experimentId, Principal p) {
        Experiment experiment = cfgRepository.findByPrimScreen(experimentId);
        if (experiment == null) {
            // error
            throw new EntityNotFoundException("Experiment with id " + experimentId + " is not found");
        } else
            return experiment;
    }
}
