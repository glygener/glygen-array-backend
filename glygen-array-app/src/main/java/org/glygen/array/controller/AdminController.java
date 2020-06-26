package org.glygen.array.controller;

import java.security.Principal;
import java.sql.SQLException;

import org.glygen.array.config.SesameTransactionConfig;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.RoleEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.service.GlygenArrayRepository;
import org.glygen.array.service.MetadataTemplateRepository;
import org.glygen.array.view.Confirmation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Import(SesameTransactionConfig.class)
@RestController
@RequestMapping("/admin")
public class AdminController {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    @Autowired
    @Qualifier("glygenArrayRepositoryImpl")
    GlygenArrayRepository repository;
    
    @Autowired
    MetadataTemplateRepository templateRepository;
    
    @Autowired
    UserRepository userRepository;
    
    
    
    @RequestMapping(value="/reset", method=RequestMethod.DELETE, 
            produces={"application/json", "application/xml"})
    @ApiResponses(value= {@ApiResponse(code=500, message="Internal Server Error")})
    public Confirmation resetRepository (Principal p) {
        // check to make sure the logged in user has the "admin" role
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        try {
            repository.resetRepository();
            return new Confirmation("emptied the repository", HttpStatus.OK.value());
        } catch (SQLException e) {
            throw new GlycanRepositoryException(e);
        }
    }
    
    @RequestMapping(value="/populateTemplates", method = RequestMethod.POST)
    public void populateTemplates (Principal p) { 
        try {
            // cleanup first
            templateRepository.deleteTemplates();
            templateRepository.populateTemplateOntology();
        } catch (SparqlException e) {
            logger.error("Error populating templates", e);
            throw new GlycanRepositoryException("Error populating templates", e);
        }
    }

}
