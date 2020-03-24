package org.glygen.array.persistence.rdf.data;

import java.util.List;

import org.glygen.array.persistence.rdf.Feature;
import org.grits.toolbox.glycanarray.om.model.StatisticalMethod;
import org.grits.toolbox.glycanarray.om.model.ValueType;

public class Intensity {
    
    // integrates multiple measurements
    List<Measurement> measurements;
    StatisticalMethod method;
    ValueType valueType;
    Feature feature;
}
