package org.glygen.array.client.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Feature {
	String uri;
	List<Glycan> glycans;
	Linker linker;
	Double ratio = null;   // in percentages like 100.0%, 50.0% etc.
	
	Map<Glycan, Integer> positionMap = new HashMap<>();
	
	
	public Map<Glycan, Integer> getPositionMap() {
		return positionMap;
	}
	
	public Integer getPosition (Glycan g) {
		return positionMap.get(g);
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
	/**
	 * @return the ratio
	 */
	public Double getRatio() {
		return ratio;
	}
	/**
	 * @param ratio the ratio to set
	 */
	public void setRatio(Double ratio) {
		this.ratio = ratio;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Feature))
			return false;
		if (uri != null && ((Feature)obj).getUri() != null)
			return  uri.equals(((Feature)obj).getUri());
		return super.equals(obj);
	}
}
