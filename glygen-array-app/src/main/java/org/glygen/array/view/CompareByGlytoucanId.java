package org.glygen.array.view;

import java.util.Comparator;

import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;

public class CompareByGlytoucanId implements Comparator<Glycan> {

    @Override
    public int compare(Glycan o1, Glycan o2) {
        if (o1 instanceof SequenceDefinedGlycan && o2 instanceof SequenceDefinedGlycan) {
            return ((SequenceDefinedGlycan) o1).getGlytoucanId().compareTo (((SequenceDefinedGlycan) o2).getGlytoucanId());
        }
        return 0;
    }
}
