
package org.glygen.array.util.pubchem;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
"CID",
"MolecularFormula",
"InChI",
"InChIKey",
"IUPACName",
"MonoisotopicMass"
})
public class PubChemProperty {

	@JsonProperty("CID")
	private Integer cID;
	@JsonProperty("MolecularFormula")
	private String molecularFormula;
	@JsonProperty("InChI")
	private String inChI;
	@JsonProperty("InChIKey")
	private String inChIKey;
	@JsonProperty("IUPACName")
	private String iUPACName;
	@JsonProperty("MonoisotopicMass")
	private Double mass;


	@JsonProperty("CID")
	public Integer getCID() {
		return cID;
	}
	
	@JsonProperty("CID")
	public void setCID(Integer cID) {
		this.cID = cID;
	}
	
	@JsonProperty("MolecularFormula")
	public String getMolecularFormula() {
		return molecularFormula;
	}
	
	@JsonProperty("MolecularFormula")
	public void setMolecularFormula(String molecularFormula) {
		this.molecularFormula = molecularFormula;
	}
	
	@JsonProperty("InChI")
	public String getInChI() {
		return inChI;
	}
	
	@JsonProperty("InChI")
	public void setInChI(String inChI) {
		this.inChI = inChI;
	}
	
	@JsonProperty("InChIKey")
	public String getInChIKey() {
		return inChIKey;
	}
	
	@JsonProperty("InChIKey")
	public void setInChIKey(String inChIKey) {
		this.inChIKey = inChIKey;
	}
	
	@JsonProperty("IUPACName")
	public String getIUPACName() {
		return iUPACName;
	}
	
	@JsonProperty("IUPACName")
	public void setIUPACName(String iUPACName) {
		this.iUPACName = iUPACName;
	}
	
	@JsonProperty("MonoisotopicMass")
	public Double getMass() {
		return mass;
	}
	
	@JsonProperty("MonoisotopicMass")
	public void setMass(Double mass) {
		this.mass = mass;
	}

}
