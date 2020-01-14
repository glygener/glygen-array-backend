package org.glygen.array.view;

import java.util.List;

import org.glygen.array.persistence.rdf.Linker;

public class LinkerListResultView {
	int total;
	List<Linker> rows;
	int filteredTotal;
	
	public int getTotal() {
		return total;
	}
	
	public void setTotal(int total) {
		this.total = total;
	}
	
	public List<Linker> getRows() {
		return rows;
	}
	
	public void setRows(List<Linker> rows) {
		this.rows = rows;
	}
	
	public int getFilteredTotal() {
        return filteredTotal;
    }
	
	public void setFilteredTotal(int filteredTotal) {
        this.filteredTotal = filteredTotal;
    }
}
