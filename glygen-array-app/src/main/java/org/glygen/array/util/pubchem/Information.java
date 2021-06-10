
package org.glygen.array.util.pubchem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "Name",
    "Description",
    "Comments",
    "URL",
    "HNID",
    "ChildID",
    "HasCountsOfType",
    "Counts"
})
public class Information {

    @JsonProperty("Name")
    private String name;
    @JsonProperty("Description")
    private List<String> description = null;
    @JsonProperty("Comments")
    private List<String> comments = null;
    @JsonProperty("URL")
    private String uRL;
    @JsonProperty("HNID")
    private Long hNID;
    @JsonProperty("ChildID")
    private List<String> childID = null;
    @JsonProperty("HasCountsOfType")
    private List<String> hasCountsOfType = null;
    @JsonProperty("Counts")
    private List<Count> counts = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("Name")
    public String getName() {
        return name;
    }

    @JsonProperty("Name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("Description")
    public List<String> getDescription() {
        return description;
    }

    @JsonProperty("Description")
    public void setDescription(List<String> description) {
        this.description = description;
    }

    @JsonProperty("Comments")
    public List<String> getComments() {
        return comments;
    }

    @JsonProperty("Comments")
    public void setComments(List<String> comments) {
        this.comments = comments;
    }

    @JsonProperty("URL")
    public String getURL() {
        return uRL;
    }

    @JsonProperty("URL")
    public void setURL(String uRL) {
        this.uRL = uRL;
    }

    @JsonProperty("HNID")
    public Long getHNID() {
        return hNID;
    }

    @JsonProperty("HNID")
    public void setHNID(Long hNID) {
        this.hNID = hNID;
    }

    @JsonProperty("ChildID")
    public List<String> getChildID() {
        return childID;
    }

    @JsonProperty("ChildID")
    public void setChildID(List<String> childID) {
        this.childID = childID;
    }

    @JsonProperty("HasCountsOfType")
    public List<String> getHasCountsOfType() {
        return hasCountsOfType;
    }

    @JsonProperty("HasCountsOfType")
    public void setHasCountsOfType(List<String> hasCountsOfType) {
        this.hasCountsOfType = hasCountsOfType;
    }

    @JsonProperty("Counts")
    public List<Count> getCounts() {
        return counts;
    }

    @JsonProperty("Counts")
    public void setCounts(List<Count> counts) {
        this.counts = counts;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
