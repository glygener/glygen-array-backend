package org.glygen.array.persistence.rdf;

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
	
	@Override
	public void copyTo(Glycan glycan) {
        super.copyTo (glycan);
        if (glycan instanceof MassOnlyGlycan) {
            ((MassOnlyGlycan) glycan).mass = this.mass;
        }
	}
}
