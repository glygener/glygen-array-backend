package org.glygen.array.persistence.rdf;

public class GlycanInFeature extends Glycan {
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
    
    @Override
    public void copyTo (Glycan glycan) {
        super.copyTo (glycan);
        if (glycan instanceof GlycanInFeature) {
            ((GlycanInFeature) glycan).source = this.source;
            ((GlycanInFeature) glycan).reducingEndConfiguration = this.reducingEndConfiguration;
        }
         
    }

}
