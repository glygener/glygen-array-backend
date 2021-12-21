package org.glygen.array.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:extendedgalfile.properties")
public class ParserConfiguration {
    @Value("${blockColumn}")
    Integer blockColumn = -1;
    @Value("${coordinateColumnX}")
    Integer coordinateColumnX;
    @Value("${coordinateColumnY}")
    Integer coordinateColumnY;
    @Value("${idColumn}")
    Integer idColumn;
    @Value("${sequenceColumn}")
    Integer sequenceColumn;
    @Value("${nameColumn}")
    Integer nameColumn;
    @Value("${sequenceTypeColumn}")
    Integer sequenceTypeColumn;
    @Value("${concentrationColumn}")
    Integer concentrationColumn;
    @Value("${typeColumn}")
    Integer typeColumn;
    @Value("${mixtureColumn}")
    Integer mixtureColumn;
    @Value("${groupColumn}")
    Integer groupColumn;
    @Value("${glytoucanIdColumn}")
    Integer glytoucanIdColumn;
    @Value("${massColumn}")
    Integer massColumn;
    @Value("${bufferColumn}")
    Integer bufferColumn;
    @Value("${carrierColumn}")
    Integer carrierColumn;
    @Value("${methodColumn}")
    Integer methodColumn;
    @Value("${referenceColumn}")
    Integer referenceColumn;
    @Value("${volumeColumn}")
    Integer volumeColumn;
    @Value("${dispensesColumn}")
    Integer dispensesColumn;
    @Value("${ratioColumn}")
    Integer ratioColumn;
    @Value("${repoIdColumn}")
    Integer repoIdColumn;
    @Value("${commentColumn}")
    Integer commentColumn;
    @Value("${flagColumn}")
    Integer flagColumn;
    
    public Integer getBlockColumn() {
        return blockColumn;
    }
    public void setBlockColumn(Integer blockColumn) {
        this.blockColumn = blockColumn;
    }
    
    public void setNameColumn(Integer nameColumn) {
        this.nameColumn = nameColumn;
    }
    public Integer getNameColumn() {
        return nameColumn;
    }
    /**
     * @return the idColumn
     */
    public Integer getIdColumn() {
        return idColumn;
    }
    /**
     * @param idColumn the idColumn to set
     */
    public void setIdColumn(Integer idColumn) {
        this.idColumn = idColumn;
    }
    /**
     * @return the sequenceColumn
     */
    public Integer getSequenceColumn() {
        return sequenceColumn;
    }
    /**
     * @param sequenceColumn the sequenceColumn to set
     */
    public void setSequenceColumn(Integer sequenceColumn) {
        this.sequenceColumn = sequenceColumn;
    }
    /**
     * @return the sequenceTypeColumn
     */
    public Integer getSequenceTypeColumn() {
        return sequenceTypeColumn;
    }
    /**
     * @param sequenceTypeColumn the sequenceTypeColumn to set
     */
    public void setSequenceTypeColumn(Integer sequenceTypeColumn) {
        this.sequenceTypeColumn = sequenceTypeColumn;
    }
    /**
     * @return the concentrationColumn
     */
    public Integer getConcentrationColumn() {
        return concentrationColumn;
    }
    /**
     * @param concentrationColumn the concentrationColumn to set
     */
    public void setConcentrationColumn(Integer concentrationColumn) {
        this.concentrationColumn = concentrationColumn;
    }
    /**
     * @return the typeColumn
     */
    public Integer getTypeColumn() {
        return typeColumn;
    }
    /**
     * @param typeColumn the typeColumn to set
     */
    public void setTypeColumn(Integer typeColumn) {
        this.typeColumn = typeColumn;
    }
    /**
     * @return the mixtureColumn
     */
    public Integer getMixtureColumn() {
        return mixtureColumn;
    }
    /**
     * @param mixtureColumn the mixtureColumn to set
     */
    public void setMixtureColumn(Integer mixtureColumn) {
        this.mixtureColumn = mixtureColumn;
    }
    
    public void setGroupColumn(Integer groupColumn) {
        this.groupColumn = groupColumn;
    }
    
    public Integer getGroupColumn() {
        return groupColumn;
    }
    /**
     * @return the coordinateColumnX
     */
    public Integer getCoordinateColumnX() {
        return coordinateColumnX;
    }
    /**
     * @param coordinateColumnX the coordinateColumnX to set
     */
    public void setCoordinateColumnX(Integer coordinateColumnX) {
        this.coordinateColumnX = coordinateColumnX;
    }
    /**
     * @return the coordinateColumnY
     */
    public Integer getCoordinateColumnY() {
        return coordinateColumnY;
    }
    /**
     * @param coordinateColumnY the coordinateColumnY to set
     */
    public void setCoordinateColumnY(Integer coordinateColumnY) {
        this.coordinateColumnY = coordinateColumnY;
    }
    /**
     * @return the glytoucanIdColumn
     */
    public Integer getGlytoucanIdColumn() {
        return glytoucanIdColumn;
    }
    /**
     * @param glytoucanIdColumn the glytoucanIdColumn to set
     */
    public void setGlytoucanIdColumn(Integer glytoucanIdColumn) {
        this.glytoucanIdColumn = glytoucanIdColumn;
    }
    /**
     * @return the massColumn
     */
    public Integer getMassColumn() {
        return massColumn;
    }
    /**
     * @param massColumn the massColumn to set
     */
    public void setMassColumn(Integer massColumn) {
        this.massColumn = massColumn;
    }
    /**
     * @return the bufferColumn
     */
    public Integer getBufferColumn() {
        return bufferColumn;
    }
    /**
     * @param bufferColumn the bufferColumn to set
     */
    public void setBufferColumn(Integer bufferColumn) {
        this.bufferColumn = bufferColumn;
    }
    /**
     * @return the carrierColumn
     */
    public Integer getCarrierColumn() {
        return carrierColumn;
    }
    /**
     * @param carrierColumn the carrierColumn to set
     */
    public void setCarrierColumn(Integer carrierColumn) {
        this.carrierColumn = carrierColumn;
    }
    /**
     * @return the methodColumn
     */
    public Integer getMethodColumn() {
        return methodColumn;
    }
    /**
     * @param methodColumn the methodColumn to set
     */
    public void setMethodColumn(Integer methodColumn) {
        this.methodColumn = methodColumn;
    }
    /**
     * @return the referenceColumn
     */
    public Integer getReferenceColumn() {
        return referenceColumn;
    }
    /**
     * @param referenceColumn the referenceColumn to set
     */
    public void setReferenceColumn(Integer referenceColumn) {
        this.referenceColumn = referenceColumn;
    }
    /**
     * @return the volumeColumn
     */
    public Integer getVolumeColumn() {
        return volumeColumn;
    }
    /**
     * @param volumeColumn the volumeColumn to set
     */
    public void setVolumeColumn(Integer volumeColumn) {
        this.volumeColumn = volumeColumn;
    }
    /**
     * @return the dispensesColumn
     */
    public Integer getDispensesColumn() {
        return dispensesColumn;
    }
    /**
     * @param dispensesColumn the dispensesColumn to set
     */
    public void setDispensesColumn(Integer dispensesColumn) {
        this.dispensesColumn = dispensesColumn;
    }
    /**
     * @return the ratioColumn
     */
    public Integer getRatioColumn() {
        return ratioColumn;
    }
    /**
     * @param ratioColumn the ratioColumn to set
     */
    public void setRatioColumn(Integer ratioColumn) {
        this.ratioColumn = ratioColumn;
    }
    /**
     * @return the commentColumn
     */
    public Integer getCommentColumn() {
        return commentColumn;
    }
    /**
     * @param commentColumn the commentColumn to set
     */
    public void setCommentColumn(Integer commentColumn) {
        this.commentColumn = commentColumn;
    }
    /**
     * @return the repoIdColumn
     */
    public Integer getRepoIdColumn() {
        return repoIdColumn;
    }
    /**
     * @param repoIdColumn the repoIdColumn to set
     */
    public void setRepoIdColumn(Integer repoIdColumn) {
        this.repoIdColumn = repoIdColumn;
    }
    /**
     * @return the flagColumn
     */
    public Integer getFlagColumn() {
        return flagColumn;
    }
    /**
     * @param flagColumn the flagColumn to set
     */
    public void setFlagColumn(Integer flagColumn) {
        this.flagColumn = flagColumn;
    }
}