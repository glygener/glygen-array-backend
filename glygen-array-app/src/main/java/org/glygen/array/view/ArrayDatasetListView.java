package org.glygen.array.view;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.data.ArrayDataset;

@XmlRootElement
public class ArrayDatasetListView {
    int total;
    List<ArrayDataset> rows;
    int filteredTotal;
    
    public int getTotal() {
        return total;
    }
    
    public void setTotal(int total) {
        this.total = total;
    }
    
    public List<ArrayDataset> getRows() {
        return rows;
    }
    
    public void setRows(List<ArrayDataset> rows) {
        this.rows = rows;
    }
    
    public int getFilteredTotal() {
        return filteredTotal;
    }
    
    public void setFilteredTotal(int filteredTotal) {
        this.filteredTotal = filteredTotal;
    }
}
