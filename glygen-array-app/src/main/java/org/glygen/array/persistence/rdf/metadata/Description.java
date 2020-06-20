package org.glygen.array.persistence.rdf.metadata;

import org.glygen.array.persistence.rdf.template.DescriptionTemplate;

public abstract class Description {
    String uri;
    String id;
    DescriptionTemplate key;
    
    public abstract boolean isGroup();
    
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
     * @return the key
     */
    public DescriptionTemplate getKey() {
        return key;
    }
    /**
     * @param key the key to set
     */
    public void setKey(DescriptionTemplate key) {
        this.key = key;
    }
    
    
}
