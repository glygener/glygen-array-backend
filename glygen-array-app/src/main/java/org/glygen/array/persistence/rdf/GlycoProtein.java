package org.glygen.array.persistence.rdf;

import java.util.ArrayList;
import java.util.List;

public class GlycoProtein extends Feature {
    List<LinkedGlycan> glycans;
    ProteinLinker protein;
    
    public GlycoProtein() {
        this.type = FeatureType.GLYCOPROTEIN;
    }

    /**
     * @return the glycans
     */
    public List<LinkedGlycan> getGlycans() {
        return glycans;
    }

    /**
     * @param glycans the glycans to set
     */
    public void setGlycans(List<LinkedGlycan> glycans) {
        this.glycans = glycans;
    }
    
    public void addGlycan (LinkedGlycan glycan) {
        if (this.glycans == null)
            glycans = new ArrayList<LinkedGlycan>();
        glycans.add(glycan);
    }

    /**
     * @return the protein
     */
    public ProteinLinker getProtein() {
        return protein;
    }

    /**
     * @param protein the protein to set
     */
    public void setProtein(ProteinLinker protein) {
        this.protein = protein;
    }

}
