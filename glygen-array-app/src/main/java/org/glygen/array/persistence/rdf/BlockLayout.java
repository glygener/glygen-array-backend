package org.glygen.array.persistence.rdf;

import java.util.List;

import org.grits.toolbox.glycanarray.om.model.SpotData;

public class BlockLayout {
	String name;
	String description;
	Integer width;
	Integer height;
	Integer numberOfReplicates;
	List<SpotData> spots;
	/**
	 * @return the name
	 */
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
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @return the width
	 */
	public Integer getWidth() {
		return width;
	}
	/**
	 * @param width the width to set
	 */
	public void setWidth(Integer width) {
		this.width = width;
	}
	/**
	 * @return the height
	 */
	public Integer getHeight() {
		return height;
	}
	/**
	 * @param height the height to set
	 */
	public void setHeight(Integer height) {
		this.height = height;
	}
	/**
	 * @return the numberOfReplicates
	 */
	public Integer getNumberOfReplicates() {
		return numberOfReplicates;
	}
	/**
	 * @param numberOfReplicates the numberOfReplicates to set
	 */
	public void setNumberOfReplicates(Integer numberOfReplicates) {
		this.numberOfReplicates = numberOfReplicates;
	}
	/**
	 * @return the spots
	 */
	public List<SpotData> getSpots() {
		return spots;
	}
	/**
	 * @param spots the spots to set
	 */
	public void setSpots(List<SpotData> spots) {
		this.spots = spots;
	}

}
