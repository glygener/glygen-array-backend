package org.glygen.array.persistence.rdf;

public class NonCommercialSource extends Source {
    
    String providerLab;
    String method;
    String comment;

    public NonCommercialSource() {
        this.type = SourceType.NONCOMMERCIAL;
        this.notRecorded = false;
    }

    /**
     * @return the providerLab
     */
    public String getProviderLab() {
        return providerLab;
    }

    /**
     * @param providerLab the providerLab to set
     */
    public void setProviderLab(String providerLab) {
        this.providerLab = providerLab;
    }

    /**
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    /**
     * @param method the method to set
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @param comment the comment to set
     */
    public void setComment(String comment) {
        this.comment = comment;
    }
}
