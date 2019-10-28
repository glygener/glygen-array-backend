package org.glygen.array.view;

import java.util.List;

import org.glygen.array.persistence.rdf.Glycan;

public class GlycanListResultView {
	int total;
	List<Glycan> rows;
	
	public int getTotal() {
		return total;
	}
	
	public void setTotal(int total) {
		this.total = total;
	}
	
	public List<Glycan> getRows() {
		return rows;
	}
	
	public void setRows(List<Glycan> rows) {
		this.rows = rows;
	}
}
