package org.glygen.array.client.model.data;

import org.grits.toolbox.glycanarray.om.model.Coordinate;

public class Measurement {
    String id;
    String uri;
    
    Coordinate coordinates;
    
    Integer fPixels;
    Integer bPixels;
    
    Integer flags;
    
    Double mean;
    Double median;
    Double stdev;
    
    Double bMean;
    Double bMedian;
    Double bStDev;
    
    Double totoalIntensity;
    
    Double meanMinusB;
    Double medianMinusB;
    
    Double percentageOneSD;
    Double percentageTwoSD;
    Double percentageSaturated;
    
    Double snRatio;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @return the coordinates
     */
    public Coordinate getCoordinates() {
        return coordinates;
    }

    /**
     * @param coordinates the coordinates to set
     */
    public void setCoordinates(Coordinate coordinates) {
        this.coordinates = coordinates;
    }

    /**
     * @return the fPixels
     */
    public Integer getfPixels() {
        return fPixels;
    }

    /**
     * @param fPixels the fPixels to set
     */
    public void setfPixels(Integer fPixels) {
        this.fPixels = fPixels;
    }

    /**
     * @return the bPixels
     */
    public Integer getbPixels() {
        return bPixels;
    }

    /**
     * @param bPixels the bPixels to set
     */
    public void setbPixels(Integer bPixels) {
        this.bPixels = bPixels;
    }

    /**
     * @return the flags
     */
    public Integer getFlags() {
        return flags;
    }

    /**
     * @param flags the flags to set
     */
    public void setFlags(Integer flags) {
        this.flags = flags;
    }

    /**
     * @return the mean
     */
    public Double getMean() {
        return mean;
    }

    /**
     * @param mean the mean to set
     */
    public void setMean(Double mean) {
        this.mean = mean;
    }

    /**
     * @return the median
     */
    public Double getMedian() {
        return median;
    }

    /**
     * @param median the median to set
     */
    public void setMedian(Double median) {
        this.median = median;
    }

    /**
     * @return the stdev
     */
    public Double getStdev() {
        return stdev;
    }

    /**
     * @param stdev the stdev to set
     */
    public void setStdev(Double stdev) {
        this.stdev = stdev;
    }

    /**
     * @return the bMean
     */
    public Double getbMean() {
        return bMean;
    }

    /**
     * @param bMean the bMean to set
     */
    public void setbMean(Double bMean) {
        this.bMean = bMean;
    }

    /**
     * @return the bMedian
     */
    public Double getbMedian() {
        return bMedian;
    }

    /**
     * @param bMedian the bMedian to set
     */
    public void setbMedian(Double bMedian) {
        this.bMedian = bMedian;
    }

    /**
     * @return the bStDev
     */
    public Double getbStDev() {
        return bStDev;
    }

    /**
     * @param bStDev the bStDev to set
     */
    public void setbStDev(Double bStDev) {
        this.bStDev = bStDev;
    }

    /**
     * @return the totoalIntensity
     */
    public Double getTotoalIntensity() {
        return totoalIntensity;
    }

    /**
     * @param totoalIntensity the totoalIntensity to set
     */
    public void setTotoalIntensity(Double totoalIntensity) {
        this.totoalIntensity = totoalIntensity;
    }

    /**
     * @return the meanMinusB
     */
    public Double getMeanMinusB() {
        return meanMinusB;
    }

    /**
     * @param meanMinusB the meanMinusB to set
     */
    public void setMeanMinusB(Double meanMinusB) {
        this.meanMinusB = meanMinusB;
    }

    /**
     * @return the medianMinusB
     */
    public Double getMedianMinusB() {
        return medianMinusB;
    }

    /**
     * @param medianMinusB the medianMinusB to set
     */
    public void setMedianMinusB(Double medianMinusB) {
        this.medianMinusB = medianMinusB;
    }

    /**
     * @return the percentageOneSD
     */
    public Double getPercentageOneSD() {
        return percentageOneSD;
    }

    /**
     * @param percentageOneSD the percentageOneSD to set
     */
    public void setPercentageOneSD(Double percentageOneSD) {
        this.percentageOneSD = percentageOneSD;
    }

    /**
     * @return the percentageTwoSD
     */
    public Double getPercentageTwoSD() {
        return percentageTwoSD;
    }

    /**
     * @param percentageTwoSD the percentageTwoSD to set
     */
    public void setPercentageTwoSD(Double percentageTwoSD) {
        this.percentageTwoSD = percentageTwoSD;
    }

    /**
     * @return the percentageSaturated
     */
    public Double getPercentageSaturated() {
        return percentageSaturated;
    }

    /**
     * @param percentageSaturated the percentageSaturated to set
     */
    public void setPercentageSaturated(Double percentageSaturated) {
        this.percentageSaturated = percentageSaturated;
    }

    /**
     * @return the snRatio
     */
    public Double getSnRatio() {
        return snRatio;
    }

    /**
     * @param snRatio the snRatio to set
     */
    public void setSnRatio(Double snRatio) {
        this.snRatio = snRatio;
    }

}
