
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
    "NodeID",
    "ParentID",
    "Information"
})
public class Node {

    @JsonProperty("NodeID")
    private String nodeID;
    @JsonProperty("ParentID")
    private List<String> parentID = null;
    @JsonProperty("Information")
    private Information_ information;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("NodeID")
    public String getNodeID() {
        return nodeID;
    }

    @JsonProperty("NodeID")
    public void setNodeID(String nodeID) {
        this.nodeID = nodeID;
    }

    @JsonProperty("ParentID")
    public List<String> getParentID() {
        return parentID;
    }

    @JsonProperty("ParentID")
    public void setParentID(List<String> parentID) {
        this.parentID = parentID;
    }

    @JsonProperty("Information")
    public Information_ getInformation() {
        return information;
    }

    @JsonProperty("Information")
    public void setInformation(Information_ information) {
        this.information = information;
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
