package org.glygen.array.persistence.rdf;

import java.util.List;

public class GlycoLipid extends Feature {
    
    List<LinkedGlycan> glycans;
    Lipid lipid;
    
    public GlycoLipid() {
        this.type = FeatureType.GLYCOLIPID;
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

    /**
     * @return the lipid
     */
    public Lipid getLipid() {
        return lipid;
    }

    /**
     * @param lipid the lipid to set
     */
    public void setLipid(Lipid lipid) {
        this.lipid = lipid;
    }
    

}
