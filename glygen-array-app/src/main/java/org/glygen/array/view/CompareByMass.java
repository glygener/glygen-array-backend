package org.glygen.array.view;

import java.util.Comparator;

import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.MassOnlyGlycan;

public class CompareByMass implements Comparator<Glycan> {

    @Override
    public int compare(Glycan o1, Glycan o2) {
        if (o1 instanceof MassOnlyGlycan && o2 instanceof MassOnlyGlycan) {
            final boolean f1, f2;
            return (f1 = ((MassOnlyGlycan) o1).getMass() == null) ^ (f2 = ((MassOnlyGlycan) o2).getMass() == null) ? 
                    f1 ? -1 : 1 : f1 && f2 ? 0 : ((MassOnlyGlycan) o1).getMass().compareTo(((MassOnlyGlycan) o2).getMass());  
        }
        return 0;
    }

}
