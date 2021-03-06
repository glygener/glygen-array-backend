package org.glygen.array.view;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.BlockLayout;

@XmlRootElement
public class BlockLayoutResultView {
	int total;
	List<BlockLayout> rows;
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
	public List<BlockLayout> getRows() {
		return rows;
	}
	/**
	 * @param rows the rows to set
	 */
	public void setRows(List<BlockLayout> rows) {
		this.rows = rows;
	}
	
	public int getFilteredTotal() {
        return filteredTotal;
    }
	
	public void setFilteredTotal(int filteredTotal) {
        this.filteredTotal = filteredTotal;
    }
	
}
