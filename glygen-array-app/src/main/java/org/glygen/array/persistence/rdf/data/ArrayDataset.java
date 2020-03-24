package org.glygen.array.persistence.rdf.data;

import java.util.List;

import org.glygen.array.persistence.rdf.metadata.Sample;

public class ArrayDataset {
    String id;
    String uri;
    String name;
    String description;
    
    Sample sample;
    RawData rawData;
    ProcessedData processedData;
    Image image;
    
    List<Slide> slides;

}
