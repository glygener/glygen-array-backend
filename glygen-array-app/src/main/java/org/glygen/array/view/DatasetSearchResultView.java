package org.glygen.array.view;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.persistence.rdf.data.ArrayDataset;

@XmlRootElement
public class DatasetSearchResultView {
    DatasetSearchType type;
    DatasetSearchInput input;
    List<ArrayDataset> rows;
    int total;
    int filteredTotal;
    /**
     * @return the type
     */
    public DatasetSearchType getType() {
        return type;
    }
    /**
     * @param type the type to set
     */
    public void setType(DatasetSearchType type) {
        this.type = type;
    }
    /**
     * @return the input
     */
    public DatasetSearchInput getInput() {
        return input;
    }
    /**
     * @param input the input to set
     */
    public void setInput(DatasetSearchInput input) {
        this.input = input;
    }
    /**
     * @return the rows
     */
    public List<ArrayDataset> getRows() {
        return rows;
    }
    /**
     * @param rows the rows to set
     */
    public void setRows(List<ArrayDataset> rows) {
        this.rows = rows;
    }
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

}
