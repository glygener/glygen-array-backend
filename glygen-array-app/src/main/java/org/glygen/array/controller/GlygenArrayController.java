package org.glygen.array.controller;

import java.util.List;

import org.glygen.array.config.SesameTransactionConfig;
import org.glygen.array.exception.BindingNotFoundException;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.GlycanBinding;
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
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;

@Import(SesameTransactionConfig.class)
@RestController
@RequestMapping("/array")
public class GlygenArrayController {
	final static Logger logger = LoggerFactory.getLogger("event-logger");
	
	@Autowired
	SesameSparqlDAO sparqlDAO;
	
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
	
	public Confirmation addSlideLayout (@RequestBody SlideLayout layout) {
		
		return new Confirmation("Slide Layout added successfully", HttpStatus.CREATED.value());
	}
}
