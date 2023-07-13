package org.glygen.array.drs;

import java.util.List;

import org.glygen.array.persistence.rdf.data.Checksum;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class DrsObject {
    List<AccessMethod> accessMethods;   // required for single blobs, optional for bundles
    List<String> aliases;   // optional
    List<Checksum> checksums;   // required
    List<ContentsObject> contents;   // required for bundles. if empty, this is a single blob
    String created_time;  // required
    String description;   // optional
    String id;    // required
    String mime_type = "application/json";    // optional
    String name;   //optional
    String self_uri;   // required
    Integer size;    // required
    String updated_time;   // optional
    String version;  //optional
    /**
     * @return the accessMethods
     */
    public List<AccessMethod> getAccessMethods() {
        return accessMethods;
    }
    /**
     * @param accessMethods the accessMethods to set
     */
    public void setAccessMethods(List<AccessMethod> accessMethods) {
        this.accessMethods = accessMethods;
    }
    /**
     * @return the aliases
     */
    public List<String> getAliases() {
        return aliases;
    }
    /**
     * @param aliases the aliases to set
     */
    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }
    /**
     * @return the cheksums
     */
    public List<Checksum> getChecksums() {
        return checksums;
    }
    /**
     * @param cheksums the cheksums to set
     */
    public void setChecksums(List<Checksum> cheksums) {
        this.checksums = cheksums;
    }
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
     * @return the created_time
     */
    public String getCreated_time() {
        return created_time;
    }
    /**
     * @param created_time the created_time to set
     */
    public void setCreated_time(String created_time) {
        this.created_time = created_time;
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
     * @return the mime_type
     */
    public String getMime_type() {
        return mime_type;
    }
    /**
     * @param mime_type the mime_type to set
     */
    public void setMime_type(String mime_type) {
        this.mime_type = mime_type;
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
     * @return the self_uri
     */
    public String getSelf_uri() {
        return self_uri;
    }
    /**
     * @param self_uri the self_uri to set
     */
    public void setSelf_uri(String self_uri) {
        this.self_uri = self_uri;
    }
    /**
     * @return the size
     */
    public Integer getSize() {
        return size;
    }
    /**
     * @param size the size to set
     */
    public void setSize(Integer size) {
        this.size = size;
    }
    /**
     * @return the updated_time
     */
    public String getUpdated_time() {
        return updated_time;
    }
    /**
     * @param updated_time the updated_time to set
     */
    public void setUpdated_time(String updated_time) {
        this.updated_time = updated_time;
    }
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
}
