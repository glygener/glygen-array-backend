package org.glygen.array.persistence.rdf.data;

public class Channel {
    
    String wavelength;
    ChannelUsageType usage = ChannelUsageType.DATA_AND_ALIGNMENT;
    /**
     * @return the wavelength
     */
    public String getWavelength() {
        return wavelength;
    }
    /**
     * @param wavelength the wavelength to set
     */
    public void setWavelength(String wavelength) {
        this.wavelength = wavelength;
    }
    /**
     * @return the usage
     */
    public ChannelUsageType getUsage() {
        return usage;
    }
    /**
     * @param usage the usage to set
     */
    public void setUsage(ChannelUsageType usage) {
        this.usage = usage;
    }

}
