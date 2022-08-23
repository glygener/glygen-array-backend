package org.glygen.array.persistence.cfgdata;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table (name="structure", schema="core")
public class Structure {
	
	@Id
	Long structure_id;
	@Column
	String glyco_ct;
	@Column
	Integer sequence_length;
	
	public Long getStructure_id() {
		return structure_id;
	}
	public void setStructure_id(Long structure_id) {
		this.structure_id = structure_id;
	}
	public String getGlyco_ct() {
		return glyco_ct;
	}
	public void setGlyco_ct(String glyco_ct) {
		this.glyco_ct = glyco_ct;
	}
	public Integer getSequence_length() {
		return sequence_length;
	}
	public void setSequence_length(Integer sequence_length) {
		this.sequence_length = sequence_length;
	}
	
}
