package org.glygen.array.persistence.cfgdata;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table (name="glytoucan", schema="core")
public class GlytoucanInfo {
	
	@Id
	Long id;
	@Column
	String glytoucan_id;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getGlytoucan_id() {
		return glytoucan_id;
	}
	public void setGlytoucan_id(String glytoucan_id) {
		this.glytoucan_id = glytoucan_id;
	}
}
