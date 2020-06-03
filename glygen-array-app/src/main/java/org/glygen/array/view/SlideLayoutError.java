package org.glygen.array.view;

import org.glygen.array.persistence.rdf.SlideLayout;

public class SlideLayoutError {
    
    SlideLayout layout;
    ErrorMessage error;
    /**
     * @return the layout
     */
    public SlideLayout getLayout() {
        return layout;
    }
    /**
     * @param layout the layout to set
     */
    public void setLayout(SlideLayout layout) {
        this.layout = layout;
    }
    /**
     * @return the error
     */
    public ErrorMessage getError() {
        return error;
    }
    /**
     * @param error the error to set
     */
    public void setError(ErrorMessage error) {
        this.error = error;
    }

}
