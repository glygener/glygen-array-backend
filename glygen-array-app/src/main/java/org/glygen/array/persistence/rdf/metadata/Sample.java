package org.glygen.array.persistence.rdf.metadata;

import org.grits.toolbox.glycanarray.om.model.Concentration;

public class Sample implements MetadataCategory {
    
    String id;
    String uri;
    String name;
    String internalName;
    String description;
    Concentration concentration;
    /**
     * @return the id
     */
    public String getId() {
        return id;
    }
    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }
    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }
    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return the concentration
     */
    public Concentration getConcentration() {
        return concentration;
    }
    /**
     * @param concentration the concentration to set
     */
    public void setConcentration(Concentration concentration) {
        this.concentration = concentration;
    }
    /**
     * @return the internalName
     */
    public String getInternalName() {
        return internalName;
    }
    /**
     * @param internalName the internalName to set
     */
    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }
    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

}
