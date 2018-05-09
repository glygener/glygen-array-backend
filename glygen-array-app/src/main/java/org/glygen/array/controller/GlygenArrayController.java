package org.glygen.array.controller;

import java.util.List;

import org.glycoinfo.rdf.SelectSparqlBean;
import org.glycoinfo.rdf.SparqlException;
import org.glycoinfo.rdf.dao.SparqlDAO;
import org.glycoinfo.rdf.dao.SparqlEntity;
import org.glycoinfo.rdf.dao.virt.VirtSesameTransactionConfig;
import org.glygen.array.rdf.GlycanBindingInsertSparql;
import org.glygen.array.rdf.GlycanBindingKeys;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.GlycanBinding;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ch.qos.logback.classic.Logger;
import io.swagger.annotations.ApiParam;

@Import(VirtSesameTransactionConfig.class)
@RestController
@RequestMapping("/array")
public class GlygenArrayController {
	public static Logger logger=(Logger) LoggerFactory.getLogger(GlygenArrayController.class);
	
	@Autowired
	SparqlDAO sparqlDAO;
	
	@RequestMapping(value = "/addbinding", method = RequestMethod.POST, 
    		consumes={"application/xml", "application/json"})
	public Confirmation addGlycanBinding (@RequestBody GlycanBinding glycan) throws Exception {
		try {
			GlycanBindingInsertSparql ins = new GlycanBindingInsertSparql();
			SparqlEntity sparqlentity = new SparqlEntity();
			sparqlentity.setValue(GlycanBindingInsertSparql.URI, "http://array.glygen.org/" + glycan.getGlycanId().hashCode());
			sparqlentity.setValue(GlycanBindingKeys.BINDING_VALUE, glycan.getBindingValue());
			ins.setGraph("http://array.glygen.org/demo/test");
			ins.setSparqlEntity(sparqlentity);
			
			sparqlDAO.insert(ins);
			return new Confirmation("Binding added successfully", HttpStatus.CREATED.value());
		} catch (SparqlException se) {
			logger.error("Cannot insert into the Triple store", se);
			throw new Exception("Binding cannot be added", se);
		} catch (Exception e) {
			logger.error("Cannot generate unique URI", e);
			throw new Exception("Binding cannot be added", e);
		}
	}

	@GetMapping("/getbinding/{glycanId}")
	public GlycanBinding getGlycanBinding (@ApiParam(required=true, value="id of the glycan to retrieve the binding for") @PathVariable("glycanId") String glycanId) throws Exception {
		String uri = "http://array.glygen.org/" + glycanId.hashCode();
		SelectSparqlBean query = new SelectSparqlBean();
		// note the carriage return
		String prefix="PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \nPREFIX glygenarray: <http://array.glygen.org/demoprefix>\n PREFIX glycan: <http://purl.jp/bio/12/glyco/glycan>\n";
		query.setPrefix(prefix);
		query.setSelect("?" + GlycanBindingInsertSparql.BINDING_VALUE + "\n");
		query.setFrom("FROM <http://array.glygen.org/demo/test>");
		query.setWhere("{ <" + uri + "> glycan:has_binding ?" + GlycanBindingInsertSparql.BINDING_VALUE + " .}");
		try {
			List<SparqlEntity> results = sparqlDAO.query(query);
			if (results == null || results.isEmpty()) {
				throw new Exception ("Binding does not exist for the glycan " + glycanId);
			}
			SparqlEntity result = results.get(0);
			String binding = result.getValue(GlycanBindingInsertSparql.BINDING_VALUE);
			GlycanBinding bindingResult = new GlycanBinding();
			bindingResult.setGlycanId(glycanId);
			bindingResult.setBindingValue(Double.parseDouble(binding));
			return bindingResult;
		} catch (SparqlException e) {
			throw new Exception("Binding cannot be retrieved", e);
		} catch (Exception e) {
			throw new Exception("Binding cannot be retrieved", e);
		}
	}
}
