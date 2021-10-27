package org.glygen.array.util.parser.cfg;

import java.util.List;

public class DataWithTitle {
    String title;
    List<Data> data;
    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }
    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }
    /**
     * @return the data
     */
    public List<Data> getData() {
        return data;
    }
    /**
     * @param data the data to set
     */
    public void setData(List<Data> data) {
        this.data = data;
    }
}
