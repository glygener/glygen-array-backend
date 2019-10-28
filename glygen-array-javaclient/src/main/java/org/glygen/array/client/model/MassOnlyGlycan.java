package org.glygen.array.client.model;

public class MassOnlyGlycan extends Glycan {
	
	Double mass;
	
	public MassOnlyGlycan() {
		this.type = GlycanType.MASS_ONLY;
	}
	
	/**
	 * @return the mass
	 */
	public Double getMass() {
		return mass;
	}
	/**
	 * @param mass the mass to set
	 */
	public void setMass(Double mass) {
		this.mass = mass;
	}
}
