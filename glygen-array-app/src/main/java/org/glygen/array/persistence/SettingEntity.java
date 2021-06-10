package org.glygen.array.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotEmpty;


@Entity(name="settings")
public class SettingEntity {

	String name;
	String value;
	
	public SettingEntity() {
	}
	
	public SettingEntity(String n, String v) {
		this.name = n;
		this.value = v;
	}
	
	/**
	 * @return the name
	 */
	@Column(nullable=false, unique=true, length=255)
	@NotEmpty
	@Id
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the value
	 */
	@Column(nullable=false, length=255)
	@NotEmpty
	public String getValue() {
		return value;
	}
	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

}
