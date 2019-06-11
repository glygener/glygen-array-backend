package org.glygen.array.client.model;

public class Feature {
	String uri;
	Glycan glycan;
	Linker linker;
	Double ratio = 1.0;
	
	/**
	 * @return the glycan
	 */
	public Glycan getGlycan() {
		return glycan;
	}
	/**
	 * @param glycan the glycan to set
	 */
	public void setGlycan(Glycan glycan) {
		this.glycan = glycan;
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
		else { // check if Glycan, Linker and the ratio are the same
			return linker.equals(((Feature)obj).getLinker()) && glycan.equals(((Feature)obj).getGlycan()) 
					&& ratio.equals(((Feature)obj).getRatio());
		}
	}
}
