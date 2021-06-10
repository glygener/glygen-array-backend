package org.glygen.array.view;

import java.util.List;

import org.glygen.array.persistence.rdf.data.PrintedSlide;

public class PrintedSlideListView {
    int total;
    List<PrintedSlide> rows;
    int filteredTotal;
    
    public int getTotal() {
        return total;
    }
    
    public void setTotal(int total) {
        this.total = total;
    }
    
    public List<PrintedSlide> getRows() {
        return rows;
    }
    
    public void setRows(List<PrintedSlide> rows) {
        this.rows = rows;
    }
    
    public int getFilteredTotal() {
        return filteredTotal;
    }
    
    public void setFilteredTotal(int filteredTotal) {
        this.filteredTotal = filteredTotal;
    }
}
