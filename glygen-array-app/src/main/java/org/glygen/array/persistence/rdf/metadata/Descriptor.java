package org.glygen.array.persistence.rdf.metadata;

public class Descriptor {
    String id;
    String uri;
    String name;
    String value;
    String description;
    String namespaceURI;
    String unitURI;
    
    
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
     * @return the value
     */
    public String getValue() {
        return value;
    }
    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }
    /**
     * @return the namespaceURI
     */
    public String getNamespaceURI() {
        return namespaceURI;
    }
    /**
     * @param namespaceURI the namespaceURI to set
     */
    public void setNamespaceURI(String namespaceURI) {
        this.namespaceURI = namespaceURI;
    }
    /**
     * @return the unitURI
     */
    public String getUnitURI() {
        return unitURI;
    }
    /**
     * @param unitURI the unitURI to set
     */
    public void setUnitURI(String unitURI) {
        this.unitURI = unitURI;
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
