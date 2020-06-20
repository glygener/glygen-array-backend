package org.glygen.array.view;

import java.util.List;

import org.glygen.array.persistence.rdf.metadata.Sample;

public class SampleListResultView {
    int total;
    List<Sample> rows;
    int filteredTotal;
    
    public int getTotal() {
        return total;
    }
    
    public void setTotal(int total) {
        this.total = total;
    }
    
    public List<Sample> getRows() {
        return rows;
    }
    
    public void setRows(List<Sample> rows) {
        this.rows = rows;
    }
    
    public int getFilteredTotal() {
        return filteredTotal;
    }
    
    public void setFilteredTotal(int filteredTotal) {
        this.filteredTotal = filteredTotal;
    }
}
