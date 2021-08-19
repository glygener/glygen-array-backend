package org.glygen.array.persistence.rdf;

import java.util.ArrayList;
import java.util.List;

public class GlycoPeptide extends Feature {
    
    List<LinkedGlycan> glycans;
    PeptideLinker peptide;
    
    Range range; // only used when this glycopeptide is part of a gplinkedglycipeptide
    
    public GlycoPeptide() {
        this.type = FeatureType.GLYCOPEPTIDE;
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
     * @return the peptide
     */
    public PeptideLinker getPeptide() {
        return peptide;
    }

    /**
     * @param peptide the peptide to set
     */
    public void setPeptide(PeptideLinker peptide) {
        this.peptide = peptide;
    }
    
    /**
     * @return the range
     */
    public Range getRange() {
        return range;
    }

    /**
     * @param range the range to set
     */
    public void setRange(Range range) {
        this.range = range;
    }

}
