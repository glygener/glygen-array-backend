package org.glygen.array.view;

import java.util.List;

public class GlycanListResultView {
	int total;
	List<GlycanView> rows;
	
	public int getTotal() {
		return total;
	}
	
	public void setTotal(int total) {
		this.total = total;
	}
	
	public List<GlycanView> getRows() {
		return rows;
	}
	
	public void setRows(List<GlycanView> rows) {
		this.rows = rows;
	}
}
