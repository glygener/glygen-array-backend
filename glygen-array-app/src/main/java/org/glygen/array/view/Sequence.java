package org.glygen.array.view;

import org.glygen.array.persistence.rdf.GlycanSequenceFormat;

public class Sequence {
    
    GlycanSequenceFormat format;
    String sequence;
    Boolean reducingEnd = null;
    
    /**
     * @return the format
     */
    public GlycanSequenceFormat getFormat() {
        return format;
    }
    /**
     * @param format the format to set
     */
    public void setFormat(GlycanSequenceFormat format) {
        this.format = format;
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
     * @return the reducingEnd
     */
    public Boolean getReducingEnd() {
        return reducingEnd;
    }
    /**
     * @param reducingEnd the reducingEnd to set
     */
    public void setReducingEnd(Boolean reducingEnd) {
        this.reducingEnd = reducingEnd;
    }
    
    

}
