package org.glygen.array.view;

public class EnumerationView {
    String name;
    String label;
    
    public EnumerationView() {
        // TODO Auto-generated constructor stub
    }
    
    public EnumerationView(String name, String label) {
        this.label = label;
        this.name = name;
    }
  
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }
    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

}
