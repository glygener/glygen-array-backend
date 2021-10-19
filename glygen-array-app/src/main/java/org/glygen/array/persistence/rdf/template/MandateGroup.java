package org.glygen.array.persistence.rdf.template;

public class MandateGroup {
    Integer id;
    Boolean xOrMandate = false;
    String name;
    /**
     * @return the id
     */
    public Integer getId() {
        return id;
    }
    /**
     * @param id the id to set
     */
    public void setId(Integer id) {
        this.id = id;
    }
    /**
     * @return the xOrMandate
     */
    public Boolean getxOrMandate() {
        return xOrMandate;
    }
    /**
     * @param xOrMandate the xOrMandate to set
     */
    public void setxOrMandate(Boolean xOrMandate) {
        this.xOrMandate = xOrMandate;
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
}
