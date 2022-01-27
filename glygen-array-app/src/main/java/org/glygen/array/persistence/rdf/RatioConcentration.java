package org.glygen.array.persistence.rdf;

import org.grits.toolbox.glycanarray.library.om.layout.LevelUnit;

public class RatioConcentration {
    Double ratio;
    LevelUnit concentration;
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
    /**
     * @return the concentration
     */
    public LevelUnit getConcentration() {
        return concentration;
    }
    /**
     * @param concentration the concentration to set
     */
    public void setConcentration(LevelUnit concentration) {
        this.concentration = concentration;
    }

}
