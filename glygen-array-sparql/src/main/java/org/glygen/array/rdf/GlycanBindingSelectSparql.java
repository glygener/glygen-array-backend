package org.glygen.array.rdf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glycoinfo.rdf.SelectSparqlBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * 
 * SelectSparql for retrieving the Wurcs of 
 * The filter removes any existing sequences in the getTo() of the GlyConvert.
 * 
 * For instance: Retrieving of original glycoct by using
 * org.glycoinfo.conversion.wurcs.GlycoctToWurcsConverter.
 * 
 * @author aoki
 *
 */
@Component
public class GlycanBindingSelectSparql extends SelectSparqlBean implements GlycanBindingKeys {
	
	private static final Logger logger = LoggerFactory.getLogger(GlycanBindingSelectSparql.class);


	public GlycanBindingSelectSparql(String sparql) {
		super(sparql);
	}

	public GlycanBindingSelectSparql() {
		super();
		this.prefix = "PREFIX glycan: <http://purl.jp/bio/12/glyco/glycan#>\n";
		this.select = "DISTINCT ?" + URI + 
//				" ?" + AccessionNumber + 
				" ?" + Binding;
		this.from = "FROM <http://array.glygen.org/demo/test>";
		this.where = "?" + URI + " glycan:has_binding ?" + Binding
//				+ " glytoucan:has_primary_id ?" + AccessionNumber + " .\n"
				;
	}
}