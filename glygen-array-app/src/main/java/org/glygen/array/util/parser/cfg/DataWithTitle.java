package org.glygen.array.util.parser.cfg;

import java.util.List;

public class DataWithTitle {
    String title;
    List<Data> data;
    String error;
    String url;
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
    /**
     * @return the error
     */
    public String getError() {
        return error;
    }
    /**
     * @param error the error to set
     */
    public void setError(String error) {
        this.error = error;
    }
    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }
    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }
}
