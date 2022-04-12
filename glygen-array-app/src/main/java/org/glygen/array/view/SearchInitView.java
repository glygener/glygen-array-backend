package org.glygen.array.view;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SearchInitView {
    
    Double minGlycanMass=1.0;
    Double maxGlycanMass=10000.0;
    /**
     * @return the minGlycanMass
     */
    public Double getMinGlycanMass() {
        return minGlycanMass;
    }
    /**
     * @param minGlycanMass the minGlycanMass to set
     */
    public void setMinGlycanMass(Double minMass) {
        this.minGlycanMass = minMass;
    }
    /**
     * @return the maxMass
     */
    public Double getMaxGlycanMass() {
        return maxGlycanMass;
    }
    /**
     * @param maxMass the maxMass to set
     */
    public void setMaxGlycanMass(Double maxMass) {
        this.maxGlycanMass = maxMass;
    }
    

}
