package org.glygen.array.view;

import java.util.List;

import org.glygen.array.persistence.rdf.Feature;

public class FeatureListResultView {
	int total;
	List<Feature> rows;
	
	public int getTotal() {
		return total;
	}
	
	public void setTotal(int total) {
		this.total = total;
	}
	
	public List<Feature> getRows() {
		return rows;
	}
	
	public void setRows(List<Feature> rows) {
		this.rows = rows;
	}
}
