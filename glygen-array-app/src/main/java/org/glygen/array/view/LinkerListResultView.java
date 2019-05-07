package org.glygen.array.view;

import java.util.List;

public class LinkerListResultView {
	int total;
	List<LinkerView> rows;
	
	public int getTotal() {
		return total;
	}
	
	public void setTotal(int total) {
		this.total = total;
	}
	
	public List<LinkerView> getRows() {
		return rows;
	}
	
	public void setRows(List<LinkerView> rows) {
		this.rows = rows;
	}
}
