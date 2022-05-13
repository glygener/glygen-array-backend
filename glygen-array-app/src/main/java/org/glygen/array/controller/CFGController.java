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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@RestController
@RequestMapping("/cfg")
public class CFGController {
    
    @Autowired
    CFGExperimentRepository cfgRepository;
    
    @ApiOperation(value = "retrieve experiment data (with sample information) for the given experiment", authorizations = { @Authorization(value="Authorization") })
    @RequestMapping(value="/getSampleData", method = RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(code=200, message="Successfully retrieved experiment data"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to retrieve info"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Experiment getSampleDataForExperiment (
            @ApiParam(required=true, value="primscreen number for the CFG experiment")
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
