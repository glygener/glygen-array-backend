package org.glygen.array.persistence.rdf;

import java.util.List;

public class Mixture extends Feature {
    
    List<Feature> features;
    
    public List<Feature> getFeatures() {
        return features;
    }
    
    public void setFeatures(List<Feature> features) {
        this.features = features;
    }
}
