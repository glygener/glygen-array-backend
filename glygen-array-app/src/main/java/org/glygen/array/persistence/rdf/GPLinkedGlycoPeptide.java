package org.glygen.array.persistence.rdf;

import java.util.List;

public class GPLinkedGlycoPeptide extends Feature {
    
    List<GlycoPeptide> peptides;
    ProteinLinker protein;

    public GPLinkedGlycoPeptide() {
        this.type = FeatureType.GPLINKEDGLYCOPEPTIDE;
    }

    /**
     * @return the peptides
     */
    public List<GlycoPeptide> getPeptides() {
        return peptides;
    }

    /**
     * @param peptides the peptides to set
     */
    public void setPeptides(List<GlycoPeptide> peptides) {
        this.peptides = peptides;
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
