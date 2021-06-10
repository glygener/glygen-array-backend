package org.glygen.array.client.model;

import java.util.Date;
import java.util.List;

import org.glygen.array.client.GlycanRestClient;



public class BlockLayout {
	String id;
	String uri;
	String name;
	String description;
	Integer width;
	Integer height;
	List<Spot> spots;
	Date dateCreated;
	Date dateModified;
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
	 * @return the spots
	 */
	public List<Spot> getSpots() {
		return spots;
	}
	/**
	 * @param spots the spots to set
	 */
	public void setSpots(List<Spot> spots) {
		this.spots = spots;
	}
	/**
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}
	/**
	 * @param uri the uri to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
		if (uri != null) 
			this.id = uri.substring(uri.lastIndexOf("/")+1);
	}
	
	public String getId () {
		return id;
	}
	
	public void setURIfromId (String id) {
		this.uri = GlycanRestClient.uriPrefix + id;
	}
	/**
	 * @return the dateCreated
	 */
	public Date getDateCreated() {
		return dateCreated;
	}
	/**
	 * @param dateCreated the dateCreated to set
	 */
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	/**
	 * @return the dateModified
	 */
	public Date getDateModified() {
		return dateModified;
	}
	/**
	 * @param dateModified the dateModified to set
	 */
	public void setDateModified(Date dateModified) {
		this.dateModified = dateModified;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

}
