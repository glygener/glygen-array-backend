package org.glygen.array.controller;

import java.util.List;
import java.util.Random;

import org.glycoinfo.rdf.SparqlException;
import org.glycoinfo.rdf.dao.SparqlDAO;
import org.glycoinfo.rdf.dao.SparqlEntity;
import org.glygen.array.rdf.GlycanBindingInsertSparql;
import org.glygen.array.rdf.GlycanBindingKeys;
import org.glygen.array.rdf.GlycanBindingSelectSparql;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.GlycanBinding;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ch.qos.logback.classic.Logger;

@RestController
@RequestMapping("/array")
public class GlygenArrayController {
	public static Logger logger=(Logger) LoggerFactory.getLogger(GlygenArrayController.class);
	
	@Autowired
	SparqlDAO sparqlDAO;
	
	@RequestMapping(value = "/addbinding", method = RequestMethod.POST, 
    		consumes={"application/xml", "application/json"})
	public Confirmation addGlycanBinding (GlycanBinding glycan) {
		try {
			GlycanBindingInsertSparql ins = new GlycanBindingInsertSparql();
			SparqlEntity sparqlentity = new SparqlEntity();
			sparqlentity.setValue(GlycanBindingInsertSparql.URI, generateUniqueURI("http://array.glygen.org/", glycan.getGlycanId()));
			sparqlentity.setValue(GlycanBindingKeys.BINDING_VALUE, glycan.getBindingValue());
			ins.setSparqlEntity(sparqlentity);
			
			sparqlDAO.insert(ins);
			return new Confirmation("Binding added successfully", HttpStatus.CREATED.value());
		} catch (SparqlException se) {
			logger.error("Cannot insert into the Triple store", se);
			return new Confirmation("Binding cannot be added", HttpStatus.EXPECTATION_FAILED.value());
		} catch (Exception e) {
			logger.error("Cannot generate unique URI", e);
			return new Confirmation("Binding cannot be added", HttpStatus.EXPECTATION_FAILED.value());
		}
	}

	private String generateUniqueURI(String baseURI, String glycanId) throws Exception {
		String uri = baseURI + glycanId.toLowerCase().replaceAll(" ", "_");
		Random random = new Random();
		int randomLength = 8;
		boolean notUnique = true;
		String randomSuffix = null;
		char randamCharacter;
		String newURI = null; 
		while(notUnique)
		{
			randomSuffix = "";
			for(int i = 0; i< randomLength; i++)
			{
				randamCharacter = (char) (97 + random.nextInt(26));
				randomSuffix = randomSuffix + randamCharacter;
			}
			newURI = uri + "_" + randomSuffix;
			GlycanBindingSelectSparql selectGlycanId = new GlycanBindingSelectSparql();
			selectGlycanId.setPrefix("PREFIX glycan: <http://purl.jp/bio/12/glyco/glycan#>\n");
			selectGlycanId.setSelect("DISTINCT ?o\n");
			selectGlycanId.setFrom("FROM <http://array.glygen.org/demo/test>");
			selectGlycanId.setWhere("{{<" + newURI + "> ?p ?o . }\n" +
					"UNION\n" + 
					"{?s ?p <" + newURI + "> . }}");
			List<SparqlEntity> results = sparqlDAO.query(selectGlycanId);
			notUnique = results.size() > 0;
		}
		return newURI;
	}
}
