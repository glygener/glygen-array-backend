package org.glygen.array.persistence.rdf;

public class GlycanInFeature  {
    Glycan glycan;
    Source source;
    ReducingEndConfiguration reducingEndConfiguration;
    
    /**
     * @return the source
     */
    public Source getSource() {
        return source;
    }
    /**
     * @param source the source to set
     */
    public void setSource(Source source) {
        this.source = source;
    }
    /**
     * @return the reducingEndConfiguration
     */
    public ReducingEndConfiguration getReducingEndConfiguration() {
        return reducingEndConfiguration;
    }
    /**
     * @param reducingEndConfiguration the reducingEndConfiguration to set
     */
    public void setReducingEndConfiguration(ReducingEndConfiguration ringInfo) {
        this.reducingEndConfiguration = ringInfo;
    }

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

}
