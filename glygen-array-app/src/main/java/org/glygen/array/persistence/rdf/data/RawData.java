package org.glygen.array.persistence.rdf.data;

import java.util.Map;

import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.metadata.ImageAnalysisSoftware;

public class RawData {
    
    Map<Measurement, Spot> dataMap;
    ImageAnalysisSoftware metadata;
    Image image;
    
    String filename;  // name of the file in uploaded file folder or any other designated data folder

}
