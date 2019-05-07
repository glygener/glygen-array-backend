package org.glygen.array.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
"PropertyTable"
})
public class PubChemResult {
	
	PropertyTable propertyTable;

	/**
	 * @return the propertyTable
	 */
	
	public PropertyTable getPropertyTable() {
		return propertyTable;
	}

	/**
	 * @param propertyTable the propertyTable to set
	 */
	@JsonProperty("PropertyTable")
	public void setPropertyTable(PropertyTable propertyTable) {
		this.propertyTable = propertyTable;
	}

}
