package org.glygen.array.view;

import java.util.List;

import org.glygen.array.persistence.rdf.metadata.MetadataCategory;

public class AllMetadataView {
    String version;
    List<MetadataCategory> metadataList;
    
    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }
    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }
    /**
     * @return the metadataList
     */
    public List<MetadataCategory> getMetadataList() {
        return metadataList;
    }
    /**
     * @param metadataList the metadataList to set
     */
    public void setMetadataList(List<MetadataCategory> metadataList) {
        this.metadataList = metadataList;
    }

}
