package org.glygen.array.persistence.rdf.data;

import org.grits.toolbox.glycanarray.om.model.Coordinate;

public class Measurement {
    Coordinate coordinates;
    
    Integer fPixels;
    Integer bPixels;
    
    Integer flags;
    
    Double mean;
    Double median;
    Double stdev;
    
    Double bMean;
    Double bMedian;
    Double bStDev;
    
    Double meanMinusB;
    Double medianMinusB;
    
    Double percentageOneSD;
    Double percentageTwoSD;
    Double percentageSaturated;
    
    Double snRatio;

}
