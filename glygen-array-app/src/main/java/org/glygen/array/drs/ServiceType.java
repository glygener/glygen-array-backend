package org.glygen.array.drs;

public class ServiceType {
    String group = "org.ga4gh";   // required
    //Name of the API or GA4GH specification implemented. 
    String artifact = "drs";  //required example: beacon 
    //Version of the API or specification. GA4GH specifications use semantic versioning.
    String version = "1.0.0"; // required example: 1.0.0
    /**
     * @return the group
     */
    public String getGroup() {
        return group;
    }
    /**
     * @param group the group to set
     */
    public void setGroup(String group) {
        this.group = group;
    }
    /**
     * @return the artifact
     */
    public String getArtifact() {
        return artifact;
    }
    /**
     * @param artifact the artifact to set
     */
    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }
    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }
    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }
}
