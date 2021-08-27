package org.glygen.array.view;

import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;

public class CompareByGlytoucanId implements Comparator<Glycan> {

    @Override
    public int compare(Glycan o1, Glycan o2) {
        if (o1 instanceof SequenceDefinedGlycan && o2 instanceof SequenceDefinedGlycan) {
            return StringUtils.compare(((SequenceDefinedGlycan) o1).getGlytoucanId(), ((SequenceDefinedGlycan) o2).getGlytoucanId());
        }
        return 0;
    }
}
