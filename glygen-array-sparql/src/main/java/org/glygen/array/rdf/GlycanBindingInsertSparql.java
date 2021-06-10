package org.glygen.array.rdf;

import org.glycoinfo.rdf.InsertSparqlBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * 
 * Demo purpose Glycan->Binding Insert statement.
 * 
 * @author aoki
 *
 */
public class GlycanBindingInsertSparql extends InsertSparqlBean implements GlycanBindingKeys {
	
	/**
	 * 
	 * Insert Sparql Constructor for Glycan-Binding statement.
	 * prefix and graph base are initialiazed.  Note that prefix requires a carriage return.
	 * 
	 */
	public GlycanBindingInsertSparql() {
		super();
		setGraphBase("http://array.glygen.org/demo");
		
		// note the carriage return
		this.prefix="PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \nPREFIX glygenarray: <http://array.glygen.org/demoprefix>\n PREFIX glycan: <http://purl.jp/bio/12/glyco/glycan>\n";
	}
	
	/**
	 * 
	 * Example of overriding any parent methods to construct an inheritable insert clause.
	 * 
	 * @return
	 */
	@Override
	public String getInsert() {
		return "<" + getSparqlEntity().getValue(URI) + "> glycan:has_binding \"" +getSparqlEntity().getValue(BINDING_VALUE) + "\" ." ;
	}
	
	
}