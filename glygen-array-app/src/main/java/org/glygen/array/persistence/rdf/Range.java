package org.glygen.array.persistence.rdf;

public class Range {
    
    Integer min = -1;
    Integer max = -1;
    
    public Range() {
        // TODO Auto-generated constructor stub
    }
    
    public Range(Integer min, Integer max) {
        if (min != null)
            this.min = min;
        if (max != null)
            this.max = max;
    }

    /**
     * @return the min
     */
    public Integer getMin() {
        return min;
    }

    /**
     * @param min the min to set
     */
    public void setMin(Integer min) {
        this.min = min;
    }

    /**
     * @return the max
     */
    public Integer getMax() {
        return max;
    }

    /**
     * @param max the max to set
     */
    public void setMax(Integer max) {
        this.max = max;
    }
    
    

}
