package org.glygen.array.persistence.cfgdata;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Entity
@Table (name="sampledata", schema="cfg_5_2")
@XmlRootElement (name="sampledata")
@JsonSerialize
public class SampleData {  
    @Id
    Long id;
    @Column(name="title")
    String title;
    @Column(name="\"Swiss_Prot\"")
    String swissProt;
    @Column(name="\"-546303161\"")
    String species;  //-546303161
    @Column(name="\"-1790003555\"")
    String immunizationSex; // -1790003555
    @Column(name="\"Primary_Sequence\"")
    String primarySequence;
    @Column(name="\"27291108\"")
    String subtype; // 27291108
    @Column(name="\"Condition_of_storage\"")
    String storageCondition;
    @Column(name="\"-782475580\"")
    String antibodyName; // -782475580
    @Column(name="\"Name\"")
    String name;
    @Column(name="\"Organism_Cells\"")
    String organismCells;
    @Column(name="\"-861196869\"")
    String dateStored;   //-861196869
    @Column(name="\"Status\"")
    String status;
    @Column(name="\"-1051843856\"")
    String immunizationSchedule; //-1051843856
    @Column(name="\"790714772\"")
    String serumSpeciesInfo; //790714772
    @Column(name="\"-188920193\"")
    String diluent; //-188920193
    @Column(name="\"443812426\"")
    String experimentTitle; //443812426
    @Column(name="\"1943838091\"")
    String organismDescription; //1943838091
    @Column(name="\"Sub_Family\"")
    String subFamily;
    @Column(name="\"1281541380\"")
    String vendorCatalogForAntibody; //1281541380
    @Column(name="\"-100409677\"")
    String serumCollectionInfoType1; //-100409677
    @Column(name="\"953343693\"")
    String immunizationDoseAndRoute; //953343693
    @Column(name="\"65299273\"")
    String antibodyType; // 65299273
    @Column(name="\"Complete_Name\"")
    String completeName;
    @Column(name="\"Spec_Sci_Name\"")
    String specSciName;
    @Column(name="\"-227934913\"")
    String variantOrganismInfo; //-227934913
    @Column(name="\"Mutation_Chimera\"")
    String mutationChimera;
    @Column(name="\"-677687632\"")
    String epitopeInfo; //-677687632
    @Column(name="\"2102082671\"")
    String sampleNameForSerumSample; //2102082671
    @Column(name="\"-1020686480\"")
    String serumCollectionInfo; //-1020686480
    @Column(name="\"-116683742\"")
    String organismSpeciesInfo; //-116683742
    @Column(name="\"Gen_Bank\"")
    String genBank;
    @Column(name="\"1799653750\"")
    String ageOfSubject; //1799653750
    @Column(name="\"69234834\"")
    String source; //69234834
    @Column(name="\"Adjuvant\"")
    String adjuvant;
    @Column(name="\"207605457\"")
    String postCollectionInfo; //207605457
    @Column(name="\"1001016772\"")
    String entryDate; //1001016772
    @Column(name="\"-1801243311\"")
    String timeBloodAfterImmnunization; //-1801243311
    @Column(name="\"Estimated_purity\"")
    String estimatedPurity;
    @Column(name="\"987326654\"")
    String contaminants; //987326654
    @Column(name="\"75611650\"")
    String carrierProtein; //75611650
    @Column(name="\"Comments\"")
    String comments;
    @Column(name="\"-728300322\"")
    String methods; //-728300322
    @Column(name="\"-1958838658\"")
    String experimentDescription; //-1958838658
    @Column(name="\"-436221455\"")
    String expressionSites; //-436221455
    @Column(name="\"2119431185\"")
    String antibodyIsotope; //2119431185
    @Column(name="\"473641455\"")
    String expressionInfo; //473641455
    @Column(name="\"114605882\"")
    String reference; //114605882
    @Column(name="\"-1726439334\"")
    String testingMethod; //-1726439334
    @Column(name="\"-1585180357\"")
    String immunogen; //-1585180357
    @Column(name="\"Family\"")
    String family;
    @Column(name="\"Sample_Name\"")
    String sampleName;
    @Column(name="\"-1396494765\"")
    String immunizationProtocol; //-1396494765
    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }
    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }
    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }
    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }
    /**
     * @return the swissProt
     */
    public String getSwissProt() {
        return swissProt;
    }
    /**
     * @param swissProt the swissProt to set
     */
    public void setSwissProt(String swissProt) {
        this.swissProt = swissProt;
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
     * @return the immunizationSex
     */
    public String getImmunizationSex() {
        return immunizationSex;
    }
    /**
     * @param immunizationSex the immunizationSex to set
     */
    public void setImmunizationSex(String immunizationSex) {
        this.immunizationSex = immunizationSex;
    }
    /**
     * @return the primarySequence
     */
    public String getPrimarySequence() {
        return primarySequence;
    }
    /**
     * @param primarySequence the primarySequence to set
     */
    public void setPrimarySequence(String primarySequence) {
        this.primarySequence = primarySequence;
    }
    /**
     * @return the subtype
     */
    public String getSubtype() {
        return subtype;
    }
    /**
     * @param subtype the subtype to set
     */
    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }
    /**
     * @return the storageCondition
     */
    public String getStorageCondition() {
        return storageCondition;
    }
    /**
     * @param storageCondition the storageCondition to set
     */
    public void setStorageCondition(String storageCondition) {
        this.storageCondition = storageCondition;
    }
    /**
     * @return the antibodyName
     */
    public String getAntibodyName() {
        return antibodyName;
    }
    /**
     * @param antibodyName the antibodyName to set
     */
    public void setAntibodyName(String antibodyName) {
        this.antibodyName = antibodyName;
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
     * @return the organismCells
     */
    public String getOrganismCells() {
        return organismCells;
    }
    /**
     * @param organismCells the organismCells to set
     */
    public void setOrganismCells(String organismCells) {
        this.organismCells = organismCells;
    }
    /**
     * @return the dateStored
     */
    public String getDateStored() {
        return dateStored;
    }
    /**
     * @param dateStored the dateStored to set
     */
    public void setDateStored(String dateStored) {
        this.dateStored = dateStored;
    }
    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }
    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }
    /**
     * @return the immunizationSchedule
     */
    public String getImmunizationSchedule() {
        return immunizationSchedule;
    }
    /**
     * @param immunizationSchedule the immunizationSchedule to set
     */
    public void setImmunizationSchedule(String immunizationSchedule) {
        this.immunizationSchedule = immunizationSchedule;
    }
    /**
     * @return the serumSpeciesInfo
     */
    public String getSerumSpeciesInfo() {
        return serumSpeciesInfo;
    }
    /**
     * @param serumSpeciesInfo the serumSpeciesInfo to set
     */
    public void setSerumSpeciesInfo(String serumSpeciesInfo) {
        this.serumSpeciesInfo = serumSpeciesInfo;
    }
    /**
     * @return the diluent
     */
    public String getDiluent() {
        return diluent;
    }
    /**
     * @param diluent the diluent to set
     */
    public void setDiluent(String diluent) {
        this.diluent = diluent;
    }
    /**
     * @return the experimentTitle
     */
    public String getExperimentTitle() {
        return experimentTitle;
    }
    /**
     * @param experimentTitle the experimentTitle to set
     */
    public void setExperimentTitle(String experimentTitle) {
        this.experimentTitle = experimentTitle;
    }
    /**
     * @return the organismDescription
     */
    public String getOrganismDescription() {
        return organismDescription;
    }
    /**
     * @param organismDescription the organismDescription to set
     */
    public void setOrganismDescription(String organismDescription) {
        this.organismDescription = organismDescription;
    }
    /**
     * @return the subFamily
     */
    public String getSubFamily() {
        return subFamily;
    }
    /**
     * @param subFamily the subFamily to set
     */
    public void setSubFamily(String subFamily) {
        this.subFamily = subFamily;
    }
    /**
     * @return the vendorCatalogForAntibody
     */
    public String getVendorCatalogForAntibody() {
        return vendorCatalogForAntibody;
    }
    /**
     * @param vendorCatalogForAntibody the vendorCatalogForAntibody to set
     */
    public void setVendorCatalogForAntibody(String vendorCatalogForAntibody) {
        this.vendorCatalogForAntibody = vendorCatalogForAntibody;
    }
    /**
     * @return the serumCollectionInfoType1
     */
    public String getSerumCollectionInfoType1() {
        return serumCollectionInfoType1;
    }
    /**
     * @param serumCollectionInfoType1 the serumCollectionInfoType1 to set
     */
    public void setSerumCollectionInfoType1(String serumCollectionInfoType1) {
        this.serumCollectionInfoType1 = serumCollectionInfoType1;
    }
    /**
     * @return the immunizationDoseAndRoute
     */
    public String getImmunizationDoseAndRoute() {
        return immunizationDoseAndRoute;
    }
    /**
     * @param immunizationDoseAndRoute the immunizationDoseAndRoute to set
     */
    public void setImmunizationDoseAndRoute(String immunizationDoseAndRoute) {
        this.immunizationDoseAndRoute = immunizationDoseAndRoute;
    }
    /**
     * @return the antibodyType
     */
    public String getAntibodyType() {
        return antibodyType;
    }
    /**
     * @param antibodyType the antibodyType to set
     */
    public void setAntibodyType(String antibodyType) {
        this.antibodyType = antibodyType;
    }
    /**
     * @return the completeName
     */
    public String getCompleteName() {
        return completeName;
    }
    /**
     * @param completeName the completeName to set
     */
    public void setCompleteName(String completeName) {
        this.completeName = completeName;
    }
    /**
     * @return the specSciName
     */
    public String getSpecSciName() {
        return specSciName;
    }
    /**
     * @param specSciName the specSciName to set
     */
    public void setSpecSciName(String specSciName) {
        this.specSciName = specSciName;
    }
    /**
     * @return the variantOrganismInfo
     */
    public String getVariantOrganismInfo() {
        return variantOrganismInfo;
    }
    /**
     * @param variantOrganismInfo the variantOrganismInfo to set
     */
    public void setVariantOrganismInfo(String variantOrganismInfo) {
        this.variantOrganismInfo = variantOrganismInfo;
    }
    /**
     * @return the mutationChimera
     */
    public String getMutationChimera() {
        return mutationChimera;
    }
    /**
     * @param mutationChimera the mutationChimera to set
     */
    public void setMutationChimera(String mutationChimera) {
        this.mutationChimera = mutationChimera;
    }
    /**
     * @return the epitopeInfo
     */
    public String getEpitopeInfo() {
        return epitopeInfo;
    }
    /**
     * @param epitopeInfo the epitopeInfo to set
     */
    public void setEpitopeInfo(String epitopeInfo) {
        this.epitopeInfo = epitopeInfo;
    }
    /**
     * @return the sampleNameForSerumSample
     */
    public String getSampleNameForSerumSample() {
        return sampleNameForSerumSample;
    }
    /**
     * @param sampleNameForSerumSample the sampleNameForSerumSample to set
     */
    public void setSampleNameForSerumSample(String sampleNameForSerumSample) {
        this.sampleNameForSerumSample = sampleNameForSerumSample;
    }
    /**
     * @return the serumCollectionInfo
     */
    public String getSerumCollectionInfo() {
        return serumCollectionInfo;
    }
    /**
     * @param serumCollectionInfo the serumCollectionInfo to set
     */
    public void setSerumCollectionInfo(String serumCollectionInfo) {
        this.serumCollectionInfo = serumCollectionInfo;
    }
    /**
     * @return the organismSpeciesInfo
     */
    public String getOrganismSpeciesInfo() {
        return organismSpeciesInfo;
    }
    /**
     * @param organismSpeciesInfo the organismSpeciesInfo to set
     */
    public void setOrganismSpeciesInfo(String organismSpeciesInfo) {
        this.organismSpeciesInfo = organismSpeciesInfo;
    }
    /**
     * @return the genBank
     */
    public String getGenBank() {
        return genBank;
    }
    /**
     * @param genBank the genBank to set
     */
    public void setGenBank(String genBank) {
        this.genBank = genBank;
    }
    /**
     * @return the ageOfSubject
     */
    public String getAgeOfSubject() {
        return ageOfSubject;
    }
    /**
     * @param ageOfSubject the ageOfSubject to set
     */
    public void setAgeOfSubject(String ageOfSubject) {
        this.ageOfSubject = ageOfSubject;
    }
    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }
    /**
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }
    /**
     * @return the adjuvant
     */
    public String getAdjuvant() {
        return adjuvant;
    }
    /**
     * @param adjuvant the adjuvant to set
     */
    public void setAdjuvant(String adjuvant) {
        this.adjuvant = adjuvant;
    }
    /**
     * @return the postCollectionInfo
     */
    public String getPostCollectionInfo() {
        return postCollectionInfo;
    }
    /**
     * @param postCollectionInfo the postCollectionInfo to set
     */
    public void setPostCollectionInfo(String postCollectionInfo) {
        this.postCollectionInfo = postCollectionInfo;
    }
    /**
     * @return the entryDate
     */
    public String getEntryDate() {
        return entryDate;
    }
    /**
     * @param entryDate the entryDate to set
     */
    public void setEntryDate(String entryDate) {
        this.entryDate = entryDate;
    }
    /**
     * @return the timeBloodAfterImmnunization
     */
    public String getTimeBloodAfterImmnunization() {
        return timeBloodAfterImmnunization;
    }
    /**
     * @param timeBloodAfterImmnunization the timeBloodAfterImmnunization to set
     */
    public void setTimeBloodAfterImmnunization(String timeBloodAfterImmnunization) {
        this.timeBloodAfterImmnunization = timeBloodAfterImmnunization;
    }
    /**
     * @return the estimatedPurity
     */
    public String getEstimatedPurity() {
        return estimatedPurity;
    }
    /**
     * @param estimatedPurity the estimatedPurity to set
     */
    public void setEstimatedPurity(String estimatedPurity) {
        this.estimatedPurity = estimatedPurity;
    }
    /**
     * @return the contaminants
     */
    public String getContaminants() {
        return contaminants;
    }
    /**
     * @param contaminants the contaminants to set
     */
    public void setContaminants(String contaminants) {
        this.contaminants = contaminants;
    }
    /**
     * @return the carrierProtein
     */
    public String getCarrierProtein() {
        return carrierProtein;
    }
    /**
     * @param carrierProtein the carrierProtein to set
     */
    public void setCarrierProtein(String carrierProtein) {
        this.carrierProtein = carrierProtein;
    }
    /**
     * @return the comments
     */
    public String getComments() {
        return comments;
    }
    /**
     * @param comments the comments to set
     */
    public void setComments(String comments) {
        this.comments = comments;
    }
    /**
     * @return the methods
     */
    public String getMethods() {
        return methods;
    }
    /**
     * @param methods the methods to set
     */
    public void setMethods(String methods) {
        this.methods = methods;
    }
    /**
     * @return the experimentDescription
     */
    public String getExperimentDescription() {
        return experimentDescription;
    }
    /**
     * @param experimentDescription the experimentDescription to set
     */
    public void setExperimentDescription(String experimentDescription) {
        this.experimentDescription = experimentDescription;
    }
    /**
     * @return the expressionSites
     */
    public String getExpressionSites() {
        return expressionSites;
    }
    /**
     * @param expressionSites the expressionSites to set
     */
    public void setExpressionSites(String expressionSites) {
        this.expressionSites = expressionSites;
    }
    /**
     * @return the antibodyIsotope
     */
    public String getAntibodyIsotope() {
        return antibodyIsotope;
    }
    /**
     * @param antibodyIsotope the antibodyIsotope to set
     */
    public void setAntibodyIsotope(String antibodyIsotope) {
        this.antibodyIsotope = antibodyIsotope;
    }
    /**
     * @return the expressionInfo
     */
    public String getExpressionInfo() {
        return expressionInfo;
    }
    /**
     * @param expressionInfo the expressionInfo to set
     */
    public void setExpressionInfo(String expressionInfo) {
        this.expressionInfo = expressionInfo;
    }
    /**
     * @return the reference
     */
    public String getReference() {
        return reference;
    }
    /**
     * @param reference the reference to set
     */
    public void setReference(String reference) {
        this.reference = reference;
    }
    /**
     * @return the testingMethod
     */
    public String getTestingMethod() {
        return testingMethod;
    }
    /**
     * @param testingMethod the testingMethod to set
     */
    public void setTestingMethod(String testingMethod) {
        this.testingMethod = testingMethod;
    }
    /**
     * @return the immunogen
     */
    public String getImmunogen() {
        return immunogen;
    }
    /**
     * @param immunogen the immunogen to set
     */
    public void setImmunogen(String immunogen) {
        this.immunogen = immunogen;
    }
    /**
     * @return the family
     */
    public String getFamily() {
        return family;
    }
    /**
     * @param family the family to set
     */
    public void setFamily(String family) {
        this.family = family;
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
     * @return the immunizationProtocol
     */
    public String getImmunizationProtocol() {
        return immunizationProtocol;
    }
    /**
     * @param immunizationProtocol the immunizationProtocol to set
     */
    public void setImmunizationProtocol(String immunizationProtocol) {
        this.immunizationProtocol = immunizationProtocol;
    }

}
