package org.glygen.array.view;

import java.util.List;

import org.glygen.array.persistence.rdf.data.IntensityData;

public class IntensityDataResultView {
    int total;
    List<IntensityData> rows;
    int filteredTotal;
    
    public int getTotal() {
        return total;
    }
    
    public void setTotal(int total) {
        this.total = total;
    }
    
    public List<IntensityData> getRows() {
        return rows;
    }
    
    public void setRows(List<IntensityData> rows) {
        this.rows = rows;
    }
    
    public int getFilteredTotal() {
        return filteredTotal;
    }
    
    public void setFilteredTotal(int filteredTotal) {
        this.filteredTotal = filteredTotal;
    }

}
