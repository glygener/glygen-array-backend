package org.glygen.array.client.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class Feature {
	String id;
	String uri;
	String name;
	List<Glycan> glycans;
	Linker linker;
	
	Map<String, String> positionMap = new HashMap<>(); // position to glycanId map
	
	FeatureType type = FeatureType.NORMAL;
	
	Date dateModified;
	Date dateCreated;
	Date dateAddedToLibrary;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
        return name;
    }
	
	public void setName(String name) {
        this.name = name;
    }
	
	//@JsonAnyGetter
	public Map<String, String> getPositionMap() {
		return positionMap;
	}
	
	@JsonAnySetter
	public void setGlycan (String key, String value) {
	    this.positionMap.put(key, value);
	}
	
	public String getGlycan (String position) {
		return positionMap.get(position);
	}
	/**
	 * @return the glycan
	 */
	public List<Glycan> getGlycans() {
		return glycans;
	}
	/**
	 * @param glycan the glycan to set
	 */
	public void setGlycans(List<Glycan>glycan) {
		this.glycans = glycan;
	}
	
	public void addGlycan (Glycan glycan) {
		if (this.glycans == null)
			glycans = new ArrayList<Glycan>();
		glycans.add(glycan);
	}
	/**
	 * @return the linker
	 */
	public Linker getLinker() {
		return linker;
	}
	/**
	 * @param linker the linker to set
	 */
	public void setLinker(Linker linker) {
		this.linker = linker;
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
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Feature))
			return false;
		if (uri != null && ((Feature)obj).getUri() != null)
			return  uri.equals(((Feature)obj).getUri());
		return super.equals(obj);
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
	 * @return the dateAddedToLibrary
	 */
	public Date getDateAddedToLibrary() {
		return dateAddedToLibrary;
	}

	/**
	 * @param dateAddedToLibrary the dateAddedToLibrary to set
	 */
	public void setDateAddedToLibrary(Date dateAddedToLibrary) {
		this.dateAddedToLibrary = dateAddedToLibrary;
	}

	/**
	 * @param positionMap the positionMap to set
	 */
	public void setPositionMap(Map<String, String> positionMap) {
		this.positionMap = positionMap;
	}
	
	public void setType(FeatureType type) {
        this.type = type;
    }
	
	public FeatureType getType() {
        return type;
    }
}
