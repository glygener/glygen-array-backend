package org.glygen.array.view;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class GlycanSearchInput {
    
    List<String> glytoucanIds;
    Double minMass;
    Double maxMass;
    Sequence structure;
    Sequence substructure;
    /**
     * @return the glytoucanIds
     */
    public List<String> getGlytoucanIds() {
        return glytoucanIds;
    }
    /**
     * @param glytoucanIds the glytoucanIds to set
     */
    public void setGlytoucanIds(List<String> glytoucanIds) {
        this.glytoucanIds = glytoucanIds;
    }
    /**
     * @return the minGlycanMass
     */
    public Double getMinMass() {
        return minMass;
    }
    /**
     * @param minGlycanMass the minGlycanMass to set
     */
    public void setMinMass(Double minMass) {
        this.minMass = minMass;
    }
    /**
     * @return the maxMass
     */
    public Double getMaxMass() {
        return maxMass;
    }
    /**
     * @param maxMass the maxMass to set
     */
    public void setMaxMass(Double maxMass) {
        this.maxMass = maxMass;
    }
    /**
     * @return the structure
     */
    public Sequence getStructure() {
        return structure;
    }
    /**
     * @param structure the structure to set
     */
    public void setStructure(Sequence structure) {
        this.structure = structure;
    }
    /**
     * @return the substructure
     */
    public Sequence getSubstructure() {
        return substructure;
    }
    /**
     * @param substructure the substructure to set
     */
    public void setSubstructure(Sequence substructure) {
        this.substructure = substructure;
    }
    
    
}
