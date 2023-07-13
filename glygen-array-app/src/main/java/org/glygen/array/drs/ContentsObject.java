package org.glygen.array.drs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class ContentsObject {

    List<ContentsObject> contents;     // optional
    List<String> drs_uri;    // optional
    String id;    // optional if inside another contents object
    String name;    // required
    /**
     * @return the contents
     */
    public List<ContentsObject> getContents() {
        return contents;
    }
    /**
     * @param contents the contents to set
     */
    public void setContents(List<ContentsObject> contents) {
        this.contents = contents;
    }
    /**
     * @return the drs_uri
     */
    public List<String> getDrs_uri() {
        return drs_uri;
    }
    /**
     * @param drs_uri the drs_uri to set
     */
    public void setDrs_uri(List<String> drs_uri) {
        this.drs_uri = drs_uri;
    }
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
}
