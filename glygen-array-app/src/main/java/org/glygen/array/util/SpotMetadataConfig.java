package org.glygen.array.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:spotmetatadaconfig.properties")
public class SpotMetadataConfig {
    
    @Value("${Buffer}")
    String formulationSolutionDescription;
    
    @Value("${Carrier}")
    String formulationCarrierDescription;
    
    @Value("${Method}")
    String formulationMethodDescription;
    
    @Value("${Reference}")
    String formulationReferenceDescription;
    
    @Value("${Volume}")
    String volumeDescription;
    
    @Value("${Dispenses}")
    String numberDispensesDescription;

    /**
     * @return the formulationSolutionDescription
     */
    public String getFormulationSolutionDescription() {
        return formulationSolutionDescription;
    }

    /**
     * @param formulationSolutionDescription the formulationSolutionDescription to set
     */
    public void setFormulationSolutionDescription(String formulationSolutionDescription) {
        this.formulationSolutionDescription = formulationSolutionDescription;
    }

    /**
     * @return the formulationCarrierDescription
     */
    public String getFormulationCarrierDescription() {
        return formulationCarrierDescription;
    }

    /**
     * @param formulationCarrierDescription the formulationCarrierDescription to set
     */
    public void setFormulationCarrierDescription(String formulationCarrierDescription) {
        this.formulationCarrierDescription = formulationCarrierDescription;
    }

    /**
     * @return the formulationMethodDescription
     */
    public String getFormulationMethodDescription() {
        return formulationMethodDescription;
    }

    /**
     * @param formulationMethodDescription the formulationMethodDescription to set
     */
    public void setFormulationMethodDescription(String formulationMethodDescription) {
        this.formulationMethodDescription = formulationMethodDescription;
    }

    /**
     * @return the formulationReferenceDescription
     */
    public String getFormulationReferenceDescription() {
        return formulationReferenceDescription;
    }

    /**
     * @param formulationReferenceDescription the formulationReferenceDescription to set
     */
    public void setFormulationReferenceDescription(String formulationReferenceDescription) {
        this.formulationReferenceDescription = formulationReferenceDescription;
    }

    /**
     * @return the volumeDescription
     */
    public String getVolumeDescription() {
        return volumeDescription;
    }

    /**
     * @param volumeDescription the volumeDescription to set
     */
    public void setVolumeDescription(String volumeDescription) {
        this.volumeDescription = volumeDescription;
    }

    /**
     * @return the numberDispensesDescription
     */
    public String getNumberDispensesDescription() {
        return numberDispensesDescription;
    }

    /**
     * @param numberDispensesDescription the numberDispensesDescription to set
     */
    public void setNumberDispensesDescription(String numberDispensesDescription) {
        this.numberDispensesDescription = numberDispensesDescription;
    }

}
