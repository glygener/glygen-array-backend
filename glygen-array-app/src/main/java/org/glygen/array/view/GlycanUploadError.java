package org.glygen.array.view;

import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;

@XmlRootElement
public class GlycanUploadError {
    Glycan glycan;
    ErrorMessage error;
    /**
     * @return the glycan
     */
    public Glycan getGlycan() {
        return glycan;
    }
    /**
     * @param glycan the glycan to set
     */
    public void setGlycan(Glycan glycan) {
        this.glycan = glycan;
    }
    /**
     * @return the error
     */
    public ErrorMessage getError() {
        return error;
    }
    /**
     * @param error the error to set
     */
    public void setError(ErrorMessage error) {
        this.error = error;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Glycan) {
            if (glycan.getInternalId() != null) {
                return glycan.getInternalId().equalsIgnoreCase(((Glycan) obj).getInternalId());
            } else if (glycan instanceof SequenceDefinedGlycan && obj instanceof SequenceDefinedGlycan) {
                return ((SequenceDefinedGlycan) glycan).getSequence().equals(((SequenceDefinedGlycan) obj).getSequence());
            }
        }
        return super.equals(obj);
    }
    
    @Override
    public int hashCode() {
        if (glycan.getInternalId() != null) {
            return glycan.getInternalId().hashCode();
        } else if (glycan instanceof SequenceDefinedGlycan) 
            return ((SequenceDefinedGlycan) glycan).getSequence().hashCode();
        return super.hashCode();
    }
}
