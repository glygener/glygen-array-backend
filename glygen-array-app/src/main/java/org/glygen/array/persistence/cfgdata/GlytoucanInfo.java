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
	@Column(name="glytoucan_id")
	String glytoucanId;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getGlytoucanId() {
		return glytoucanId;
	}
	public void setGlytoucanId(String glytoucanId) {
		this.glytoucanId = glytoucanId;
	}
}
