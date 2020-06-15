package org.glygen.array.persistence.rdf.template;

public enum MetadataTemplateType {
    
    SAMPLE("SampleTemplate"),
    PRINTER("PrinterTemplate"),
    SCANNER("ScannerTemplate"),
    SLIDE("SlideTemplate"),
    DATAPROCESSINGSOFTWARE("DataProcessingSoftwareTemplate"),
    IMAGEANALYSISSOFTWARE("ImageAnaylsisSoftwareTemplate");
    
    String label;
    
    private MetadataTemplateType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
    
    
    public void setLabel(String label) {
        this.label = label;
    }
}
