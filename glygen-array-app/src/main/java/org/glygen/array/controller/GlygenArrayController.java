package org.glygen.array.controller;

import java.security.Principal;
import java.sql.SQLException;
import java.util.List;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;

import org.glygen.array.config.SesameTransactionConfig;
import org.glygen.array.exception.BindingNotFoundException;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.service.GlygenArrayRepository;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.GlycanBinding;
import org.glygen.array.view.GlycanView;
import org.grits.toolbox.glycanarray.library.om.feature.Glycan;
import org.grits.toolbox.glycanarray.library.om.layout.SlideLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;

@Import(SesameTransactionConfig.class)
@RestController
@RequestMapping("/array")
public class GlygenArrayController {
	final static Logger logger = LoggerFactory.getLogger("event-logger");
	
	@Autowired
	SesameSparqlDAO sparqlDAO;
	
	@Autowired
	GlygenArrayRepository repository;
	
	@Autowired
	UserRepository userRepository;
	
	@RequestMapping(value = "/addbinding", method = RequestMethod.POST, 
    		consumes={"application/json", "application/xml"},
    		produces={"application/json", "application/xml"})
	@Authorization (value="Bearer", scopes={@AuthorizationScope (scope="write:glygenarray", description="Add a new glycan binding")})
	public Confirmation addGlycanBinding (@RequestBody GlycanBinding glycan) throws Exception {
		try {
			StringBuffer sparqlbuf = new StringBuffer();
			String prefix="PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \nPREFIX glygenarray: <http://array.glygen.org/demoprefix>\n PREFIX glycan: <http://purl.jp/bio/12/glyco/glycan>\n";
			sparqlbuf.append(prefix);
			sparqlbuf.append("INSERT ");
			sparqlbuf.append("{ GRAPH <" + "http://array.glygen.org/demo/test" + ">\n");
			// sparqlbuf.append(getUsing());
			sparqlbuf.append("{ " + "<" +  "http://array.glygen.org/" + glycan.getGlycanId().hashCode() + "> glycan:has_binding \"" + glycan.getBindingValue() + "\" ." + " }\n");
			sparqlbuf.append("}\n");
			sparqlDAO.insert(sparqlbuf.toString());
			return new Confirmation("Binding added successfully", HttpStatus.CREATED.value());
		} catch (SparqlException se) {
			logger.error("Cannot insert into the Triple store", se);
			throw new GlycanRepositoryException("Binding cannot be added", se);
		} catch (Exception e) {
			logger.error("Cannot generate unique URI", e);
			throw new GlycanRepositoryException("Binding cannot be added", e);
		}
	}

	@Authorization (value="Bearer", scopes={@AuthorizationScope (scope="read:glygenarray", description="Access to glycan binding")})
	@RequestMapping(value="/getbinding/{glycanId}", method = RequestMethod.GET, produces={"application/json", "application/xml"})
	public GlycanBinding getGlycanBinding (@ApiParam(required=true, value="id of the glycan to retrieve the binding for") @PathVariable("glycanId") String glycanId) throws Exception {
		String uri = "http://array.glygen.org/" + glycanId.hashCode();
		StringBuffer query = new StringBuffer();
		// note the carriage return
		String prefix="PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \nPREFIX glygenarray: <http://array.glygen.org/demoprefix>\n PREFIX glycan: <http://purl.jp/bio/12/glyco/glycan>\n";
		query.append(prefix + "\n");
		query.append("SELECT ?" + "BINDING_VALUE" + "\n");
		query.append("FROM <http://array.glygen.org/demo/test>\n");
		query.append("{ <" + uri + "> glycan:has_binding ?" + "BINDING_VALUE" + " .}");
		try {
			List<SparqlEntity> results = sparqlDAO.query(query.toString());
			if (results == null || results.isEmpty()) {
				throw new BindingNotFoundException("Binding does not exist for the glycan " + glycanId);
			}
			SparqlEntity result = results.get(0);
			String binding = result.getValue("BINDING_VALUE");
			GlycanBinding bindingResult = new GlycanBinding();
			bindingResult.setGlycanId(glycanId);
			bindingResult.setBindingValue(Double.parseDouble(binding));
			return bindingResult;
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Binding cannot be retrieved", e);
		} catch (Exception e) {
			throw new GlycanRepositoryException("Binding cannot be retrieved", e);
		}
	}
	
	@RequestMapping(value="/addslidelayout", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	public Confirmation addSlideLayout (@RequestBody SlideLayout layout) {
		//TODO
		return new Confirmation("Slide Layout added successfully", HttpStatus.CREATED.value());
	}
	
	@RequestMapping(value="/addglycan", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Glycan added successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register glycans"),
			@ApiResponse(code=409, message="A glycan with the given sequence already exists!"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation addGlycan (@RequestBody GlycanView glycan, Boolean privateOnly, Principal p) {
		try {
			UserEntity user = userRepository.findByUsername(p.getName());
			Glycan g = new Glycan();
			g.setName(glycan.getName());
			g.setGlyTouCanId(glycan.getGlytoucanId());
			g.setComment(glycan.getComment());
			g.setSequence(glycan.getSequence());
			g.setSequenceType(glycan.getSequenceFormat());
			boolean isPrivate = privateOnly != null && privateOnly ? true: false;
			Glycan existing = repository.getGlycanBySequence(glycan.getSequence(), user, isPrivate);
			if (existing == null) {
				//TODO check if the given sequence is valid
				//TODO if there is a glytoucanId, check if it is valid
				repository.addGlycan(g, user, isPrivate);
			}
			else throw new EntityExistsException("There is already a glycan with the same sequence in the repository!");
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Glycan cannot be added for user " + p.getName(), e);
		}
		return new Confirmation("Glycan added successfully", HttpStatus.CREATED.value());
	}
	
	@RequestMapping(value="/getglycan/{glytoucanId}", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	public GlycanView getGlycan (
			@ApiParam(required=true, value="glytoucanId of the glycan to retrieve") 
			@PathVariable("glytoucanId") String glytoucanId, Principal p) {
		try {
			UserEntity user = userRepository.findByUsername(p.getName());
			GlycanView g = new GlycanView();
			
			Glycan glycan = repository.getGlycan(glytoucanId);
			if (glycan == null)
				throw new EntityNotFoundException("Glycan with glytoucan id : " + glytoucanId + " does not exist in the repository");
			g.setName(glycan.getName());
			g.setGlytoucanId(glycan.getGlyTouCanId());
			g.setComment(glycan.getComment());
			return g;
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Glycan cannot be retrieved for user " + p.getName(), e);
		}
		
	}
	
	@RequestMapping(value="/addgraph", method = RequestMethod.PUT, produces={"application/json", "application/xml"})
	public Confirmation addPrivateGraphForUser (Principal p) {
		try {
			UserEntity user = userRepository.findByUsername(p.getName());
			String newGraphIRI = repository.addPrivateGraphForUser(user);
			return new Confirmation("Private graph " + newGraphIRI + " added successfully", HttpStatus.CREATED.value());
		} catch (SQLException e) {
			throw new GlycanRepositoryException("Private Graph cannot be added for user " + p.getName(), e);
		}
		
	}
}
