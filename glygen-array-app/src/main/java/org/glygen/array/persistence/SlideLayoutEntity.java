package org.glygen.array.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotEmpty;

@Entity(name="slidelayout")
public class SlideLayoutEntity {
    
    String uri;
    String jsonValue;
    
    /**
     * @return the uri
     */
    @Column(nullable=false, unique=true, length=255)
    @NotEmpty
    @Id
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
     * @return the jsonValue
     */
    @Column(name="jsonvalue", nullable=false)
    @NotEmpty
    public String getJsonValue() {
        return jsonValue;
    }
    /**
     * @param jsonValue the jsonValue to set
     */
    public void setJsonValue(String jsonValue) {
        this.jsonValue = jsonValue;
    }
    
    

}
