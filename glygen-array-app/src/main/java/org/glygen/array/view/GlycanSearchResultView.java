package org.glygen.array.view;

import java.util.List;

public class GlycanSearchResultView {
    
    List<GlycanSearchResult> rows;
    int total;
    int filteredTotal;
    
    
    /**
     * @return the total
     */
    public int getTotal() {
        return total;
    }
    /**
     * @param total the total to set
     */
    public void setTotal(int total) {
        this.total = total;
    }
    /**
     * @return the filteredTotal
     */
    public int getFilteredTotal() {
        return filteredTotal;
    }
    /**
     * @param filteredTotal the filteredTotal to set
     */
    public void setFilteredTotal(int filteredTotal) {
        this.filteredTotal = filteredTotal;
    }
    /**
     * @return the rows
     */
    public List<GlycanSearchResult> getRows() {
        return rows;
    }
    /**
     * @param rows the rows to set
     */
    public void setRows(List<GlycanSearchResult> rows) {
        this.rows = rows;
    }

}
