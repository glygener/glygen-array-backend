package org.glygen.array.util;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
"Properties"
})
public class PropertyTable {
	
	@JsonProperty("Properties")
	List<PubChemProperty> properties;

	/**
	 * @return the properties
	 */
	@JsonProperty("Properties")
	public List<PubChemProperty> getProperties() {
		return properties;
	}

	/**
	 * @param properties the properties to set
	 */
	@JsonProperty("Properties")
	public void setProperties(List<PubChemProperty> properties) {
		this.properties = properties;
	}

}
