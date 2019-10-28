package org.glygen.array.client.model;

import java.util.List;

import org.grits.toolbox.glycanarray.library.om.layout.LevelUnit;

public class Spot {
	List<Feature> features;
	Integer row;
	Integer column;
	LevelUnit concentration;
	Integer group;
	String uri;
	SpotFlag flag;
	
	public void setFlag(SpotFlag flag) {
		this.flag = flag;
	}
	
	public SpotFlag getFlag() {
		return flag;
	}
	
	/**
	 * @return the feature
	 */
	public List<Feature> getFeatures() {
		return features;
	}
	/**
	 * @param feature the feature to set
	 */
	public void setFeatures(List<Feature> feature) {
		this.features = feature;
	}
	/**
	 * @return the row
	 */
	public Integer getRow() {
		return row;
	}
	/**
	 * @param row the row to set
	 */
	public void setRow(Integer row) {
		this.row = row;
	}
	/**
	 * @return the column
	 */
	public Integer getColumn() {
		return column;
	}
	/**
	 * @param column the column to set
	 */
	public void setColumn(Integer column) {
		this.column = column;
	}
	/**
	 * @return the concentration
	 */
	public LevelUnit getConcentration() {
		return concentration;
	}
	/**
	 * @param concentration the concentration to set
	 */
	public void setConcentration(LevelUnit concentration) {
		this.concentration = concentration;
	}
	/**
	 * @return the group
	 */
	public Integer getGroup() {
		return group;
	}
	/**
	 * @param group the group to set
	 */
	public void setGroup(Integer group) {
		this.group = group;
	}
	
	public void setUri(String uri) {
		this.uri = uri;
	}
	
	public String getUri() {
		return uri;
	}
}
