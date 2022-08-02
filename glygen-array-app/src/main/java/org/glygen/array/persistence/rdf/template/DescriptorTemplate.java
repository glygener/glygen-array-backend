package org.glygen.array.persistence.rdf.template;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlRootElement
@JsonTypeName("descriptortemplate")
public class DescriptorTemplate extends DescriptionTemplate {
    
    Namespace namespace;
    List<String> selectionList;
    List<String> units;
        
    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean isGroup() {
        return false;
    }

    /**
     * @return the namespace
     */
    public Namespace getNamespace() {
        return namespace;
    }

    /**
     * @param namespace the namespace to set
     */
    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
    }

    /**
     * @return the selectionList
     */
    public List<String> getSelectionList() {
        return selectionList;
    }

    /**
     * @param selectionList the selectionList to set
     */
    public void setSelectionList(List<String> selectionList) {
        this.selectionList = selectionList;
    }
    
    public void addSelection (String selection) {
        if (this.selectionList == null)
            this.selectionList = new ArrayList<String>();
        if (!this.selectionList.contains(selection))
            this.selectionList.add(selection);
    }

    /**
     * @return the units
     */
    public List<String> getUnits() {
        return units;
    }

    /**
     * @param units the units to set
     */
    public void setUnits(List<String> units) {
        this.units = units;
    }
}
