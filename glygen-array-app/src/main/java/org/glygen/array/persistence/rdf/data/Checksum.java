package org.glygen.array.persistence.rdf.data;

public class Checksum {

    String checksum;   // required
    String type;      // required
    /**
     * @return the checksum
     */
    public String getChecksum() {
        return checksum;
    }
    /**
     * @param checksum the checksum to set
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
    /**
     * @return the type
     */
    public String getType() {
        return type;
    }
    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }
}
