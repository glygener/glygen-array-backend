package org.glygen.array.persistence.rdf;

public class CommercialSource extends Source {
    
    String vendor;
    String catalogueNumber;
    
    public CommercialSource() {
        this.type = SourceType.COMMERCIAL;
        this.notRecorded = false;
    }

    /**
     * @return the vendor
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * @param vendor the vendor to set
     */
    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    /**
     * @return the catalogueNumber
     */
    public String getCatalogueNumber() {
        return catalogueNumber;
    }

    /**
     * @param catalogueNumber the catalogueNumber to set
     */
    public void setCatalogueNumber(String catalogueNumber) {
        this.catalogueNumber = catalogueNumber;
    }

}
