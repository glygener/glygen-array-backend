package org.glygen.array.persistence.rdf.template;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonCreator;

@XmlRootElement
public enum MetadataTemplateType {
    
    SAMPLE("sample_template"),
    PRINTER("printer_template"),
    SCANNER("scanner_template"),
    SLIDE("slide_template"),
    DATAPROCESSINGSOFTWARE("data_processing_software_template"),
    IMAGEANALYSISSOFTWARE("image_analysis_software_template"),
    ASSAY("assay_template"),
    FEATURE("feature_template"),
    SPOT("spot_template"),
    PRINTRUN("printrun_template");
    
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
    
    @JsonCreator
    public static MetadataTemplateType forValue(String value) {
        if (value.equals("sample_template") || value.equals("SAMPLE"))
            return SAMPLE;
        else if (value.equals("printer_template") || value.equals("PRINTER"))
            return PRINTER;
        else if (value.equals("scanner_template") || value.equals("SCANNER"))
            return SCANNER;
        else if (value.equals("slide_template") || value.equals("SLIDE"))
            return SLIDE;
        else if (value.equals("data_processing_software_template") || value.equals("DATAPROCESSINGSOFTWARE"))
            return DATAPROCESSINGSOFTWARE;
        else if (value.equals("image_analysis_software_template") || value.equals("IMAGEANALYSISSOFTWARE"))
            return IMAGEANALYSISSOFTWARE;
        else if (value.equals("assay_template") || value.equals("ASSAY"))
            return ASSAY;
        else if (value.equals("spot_template") || value.equals("SPOT"))
            return SPOT;
        else if (value.equals("feature_template") || value.equals("FEATURE"))
            return FEATURE;
        else if (value.equals("printrun_template") || value.equals("PRINTRUN"))
            return PRINTRUN;
        return SAMPLE;
    }
}
