package org.glygen.array.util;

import java.util.ArrayList;
import java.util.List;

import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.view.ErrorMessage;

public class GalFileImportResult {
    List<Glycan> glycanList = new ArrayList<>();
    List<Feature> featureList = new ArrayList<>();
    List<BlockLayout> layoutList = new ArrayList<>();
    SlideLayout layout;
    List<ErrorMessage> errors = new ArrayList<>();
    /**
     * @return the glycanList
     */
    public List<Glycan> getGlycanList() {
        return glycanList;
    }
    /**
     * @param glycanList the glycanList to set
     */
    public void setGlycanList(List<Glycan> glycanList) {
        this.glycanList = glycanList;
    }
    /**
     * @return the featureList
     */
    public List<Feature> getFeatureList() {
        return featureList;
    }
    /**
     * @param featureList the featureList to set
     */
    public void setFeatureList(List<Feature> featureList) {
        this.featureList = featureList;
    }
    /**
     * @return the layoutList
     */
    public List<BlockLayout> getLayoutList() {
        return layoutList;
    }
    /**
     * @param layoutList the layoutList to set
     */
    public void setLayoutList(List<BlockLayout> layoutList) {
        this.layoutList = layoutList;
    }
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
    
    public List<ErrorMessage> getErrors() {
        return errors;
    }
    
    public void setErrors(List<ErrorMessage> errors) {
        this.errors = errors;
    }
    
    public void addError(ErrorMessage error) {
        this.errors.add(error);
    }
    
}
