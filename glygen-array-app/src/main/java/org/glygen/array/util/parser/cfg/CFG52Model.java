package org.glygen.array.util.parser.cfg;

import java.util.List;

public class CFG52Model {
    String primscreen;
    String sampleName;
    String species;
    String proteinFamily;
    String investigator;
    String experiment;
    String request;
    String date;
    List<Link> links;
    DataWithTitle sampleData;
    DataWithTitle experimentData;
    DataWithTitle requestData;
    String filename;
    /**
     * @return the primscreen
     */
    public String getPrimscreen() {
        return primscreen;
    }
    /**
     * @param primscreen the primscreen to set
     */
    public void setPrimscreen(String primscreen) {
        this.primscreen = primscreen;
    }
    /**
     * @return the sampleName
     */
    public String getSampleName() {
        return sampleName;
    }
    /**
     * @param sampleName the sampleName to set
     */
    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }
    /**
     * @return the species
     */
    public String getSpecies() {
        return species;
    }
    /**
     * @param species the species to set
     */
    public void setSpecies(String species) {
        this.species = species;
    }
    /**
     * @return the proteinFamily
     */
    public String getProteinFamily() {
        return proteinFamily;
    }
    /**
     * @param proteinFamily the proteinFamily to set
     */
    public void setProteinFamily(String proteinFamily) {
        this.proteinFamily = proteinFamily;
    }
    /**
     * @return the investigator
     */
    public String getInvestigator() {
        return investigator;
    }
    /**
     * @param investigator the investigator to set
     */
    public void setInvestigator(String investigator) {
        this.investigator = investigator;
    }
    /**
     * @return the experiment
     */
    public String getExperiment() {
        return experiment;
    }
    /**
     * @param experiment the experiment to set
     */
    public void setExperiment(String experiment) {
        this.experiment = experiment;
    }
    /**
     * @return the request
     */
    public String getRequest() {
        return request;
    }
    /**
     * @param request the request to set
     */
    public void setRequest(String request) {
        this.request = request;
    }
    /**
     * @return the date
     */
    public String getDate() {
        return date;
    }
    /**
     * @param date the date to set
     */
    public void setDate(String date) {
        this.date = date;
    }
    /**
     * @return the links
     */
    public List<Link> getLinks() {
        return links;
    }
    /**
     * @param links the links to set
     */
    public void setLinks(List<Link> links) {
        this.links = links;
    }
    /**
     * @return the sampleData
     */
    public DataWithTitle getSampleData() {
        return sampleData;
    }
    /**
     * @param sampleData the sampleData to set
     */
    public void setSampleData(DataWithTitle sampleData) {
        this.sampleData = sampleData;
    }
    /**
     * @return the experimentData
     */
    public DataWithTitle getExperimentData() {
        return experimentData;
    }
    /**
     * @param experimentData the experimentData to set
     */
    public void setExperimentData(DataWithTitle experimentData) {
        this.experimentData = experimentData;
    }
    /**
     * @return the requestData
     */
    public DataWithTitle getRequestData() {
        return requestData;
    }
    /**
     * @param requestData the requestData to set
     */
    public void setRequestData(DataWithTitle requestData) {
        this.requestData = requestData;
    }
    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }
    /**
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
}
