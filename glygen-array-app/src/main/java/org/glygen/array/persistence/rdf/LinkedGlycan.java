package org.glygen.array.persistence.rdf;

import java.util.ArrayList;
import java.util.List;

public class LinkedGlycan extends Feature {
    
    List<GlycanInFeature> glycans;
    
    Range range; // only used when this linkedGlycan is part of a glycoprotein
    
    public LinkedGlycan() {
        this.type = FeatureType.LINKEDGLYCAN;
    }
    
    /**
     * @return the glycan
     */
    public List<GlycanInFeature> getGlycans() {
        return glycans;
    }
    /**
     * @param glycan the glycan to set
     */
    public void setGlycans(List<GlycanInFeature>glycan) {
        this.glycans = glycan;
    }
    
    public void addGlycan (GlycanInFeature glycan) {
        if (this.glycans == null)
            glycans = new ArrayList<GlycanInFeature>();
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
