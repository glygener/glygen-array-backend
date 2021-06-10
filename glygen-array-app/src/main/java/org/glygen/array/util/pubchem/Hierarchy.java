
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
    "SourceName",
    "SourceID",
    "RootID",
    "HID",
    "Information",
    "Node"
})
public class Hierarchy {

    @JsonProperty("SourceName")
    private String sourceName;
    @JsonProperty("SourceID")
    private String sourceID;
    @JsonProperty("RootID")
    private String rootID;
    @JsonProperty("HID")
    private Long hID;
    @JsonProperty("Information")
    private Information information;
    @JsonProperty("Node")
    private List<Node> node = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("SourceName")
    public String getSourceName() {
        return sourceName;
    }

    @JsonProperty("SourceName")
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    @JsonProperty("SourceID")
    public String getSourceID() {
        return sourceID;
    }

    @JsonProperty("SourceID")
    public void setSourceID(String sourceID) {
        this.sourceID = sourceID;
    }

    @JsonProperty("RootID")
    public String getRootID() {
        return rootID;
    }

    @JsonProperty("RootID")
    public void setRootID(String rootID) {
        this.rootID = rootID;
    }

    @JsonProperty("HID")
    public Long getHID() {
        return hID;
    }

    @JsonProperty("HID")
    public void setHID(Long hID) {
        this.hID = hID;
    }

    @JsonProperty("Information")
    public Information getInformation() {
        return information;
    }

    @JsonProperty("Information")
    public void setInformation(Information information) {
        this.information = information;
    }

    @JsonProperty("Node")
    public List<Node> getNode() {
        return node;
    }

    @JsonProperty("Node")
    public void setNode(List<Node> node) {
        this.node = node;
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
