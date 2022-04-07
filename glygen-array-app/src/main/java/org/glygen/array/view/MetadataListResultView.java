package org.glygen.array.view;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.metadata.MetadataCategory;

@XmlRootElement
public class MetadataListResultView {
    int total;
    List<MetadataCategory> rows;
    int filteredTotal;
    
    public int getTotal() {
        return total;
    }
    
    public void setTotal(int total) {
        this.total = total;
    }
    
    public List<MetadataCategory> getRows() {
        return rows;
    }
    
    public void setRows(List<MetadataCategory> rows) {
        this.rows = rows;
    }
    
    public int getFilteredTotal() {
        return filteredTotal;
    }
    
    public void setFilteredTotal(int filteredTotal) {
        this.filteredTotal = filteredTotal;
    }
}
