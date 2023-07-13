package org.glygen.array.drs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class AccessMethod {
    
    String access_id;
    AccessURL access_url;   // one of access_id or access_url is required
    String region;    // optional
    String type; // enumeration
    /**
     * @return the access_id
     */
    public String getAccess_id() {
        return access_id;
    }
    /**
     * @param access_id the access_id to set
     */
    public void setAccess_id(String access_id) {
        this.access_id = access_id;
    }
    /**
     * @return the access_url
     */
    public AccessURL getAccess_url() {
        return access_url;
    }
    /**
     * @param access_url the access_url to set
     */
    public void setAccess_url(AccessURL access_url) {
        this.access_url = access_url;
    }
    /**
     * @return the region
     */
    public String getRegion() {
        return region;
    }
    /**
     * @param region the region to set
     */
    public void setRegion(String region) {
        this.region = region;
    }
    /**
     * @return the type
     */
    public String getType() {
        return type;
    }
    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

}
