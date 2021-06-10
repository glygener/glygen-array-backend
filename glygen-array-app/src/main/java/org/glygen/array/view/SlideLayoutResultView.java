package org.glygen.array.view;

import java.util.List;

import org.glygen.array.persistence.rdf.SlideLayout;

public class SlideLayoutResultView {
	int total;
	List<SlideLayout> rows;
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
	 * @return the rows
	 */
	public List<SlideLayout> getRows() {
		return rows;
	}
	/**
	 * @param rows the rows to set
	 */
	public void setRows(List<SlideLayout> rows) {
		this.rows = rows;
	}
	
	public void setFilteredTotal(int filteredTotal) {
        this.filteredTotal = filteredTotal;
    }
	
	public int getFilteredTotal() {
        return filteredTotal;
    }

}
