package org.glygen.array.persistence.rdf.data;

import java.util.List;
import java.util.Map;

import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.metadata.ImageAnalysisSoftware;

public class RawData {
    
    Map<Measurement, Spot> dataMap;
    ImageAnalysisSoftware metadata;
    List<Image> images;
    
    List<String> files;  // name of the files in uploaded file folder or any other designated data folder

}
