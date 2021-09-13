package org.glygen.array.persistence.rdf;

public class OtherGlycan extends Glycan {
    
    String sequence;
    String inChiKey;
    String inChiSequence;
    String smiles;
    String molFile;
    
    public OtherGlycan() {
        this.type = GlycanType.OTHER;
    }
    
    /**
     * @return the sequence
     */
    public String getSequence() {
        return sequence;
    }
    /**
     * @param sequence the sequence to set
     */
    public void setSequence(String sequence) {
        this.sequence = sequence;
    }
    /**
     * @return the inChiKey
     */
    public String getInChiKey() {
        return inChiKey;
    }
    /**
     * @param inChiKey the inChiKey to set
     */
    public void setInChiKey(String inChiKey) {
        this.inChiKey = inChiKey;
    }
    /**
     * @return the inChiSequence
     */
    public String getInChiSequence() {
        return inChiSequence;
    }
    /**
     * @param inChiSequence the inChiSequence to set
     */
    public void setInChiSequence(String inChiSequence) {
        this.inChiSequence = inChiSequence;
    }
    /**
     * @return the smiles
     */
    public String getSmiles() {
        return smiles;
    }
    /**
     * @param smiles the smiles to set
     */
    public void setSmiles(String smiles) {
        this.smiles = smiles;
    }
    /**
     * @return the molFile
     */
    public String getMolFile() {
        return molFile;
    }
    /**
     * @param molFile the molFile to set
     */
    public void setMolFile(String molFile) {
        this.molFile = molFile;
    }

}
