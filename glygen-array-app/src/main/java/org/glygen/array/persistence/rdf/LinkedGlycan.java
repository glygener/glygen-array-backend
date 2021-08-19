package org.glygen.array.persistence.rdf;

import java.util.ArrayList;
import java.util.List;

public class LinkedGlycan extends Feature {
    
    List<Glycan> glycans;
    
    Range range; // only used when this linkedGlycan is part of a glycoprotein
    
    public LinkedGlycan() {
        this.type = FeatureType.LINKEDGLYCAN;
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
