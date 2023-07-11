package org.glygen.array.drs;

import java.util.Calendar;
import java.util.Date;


public class ServiceInfo {
    
    //Unique ID of this service. Reverse domain name notation is recommended, though not required. 
    //The identifier should attempt to be globally unique so it can be used in downstream aggregator services e.g. Service Registry.
    String id = "org.glygen.array";          // required  example: org.ga4gh.myservice
    String name = "Glycan Array Repository DRS API";        // required
    ServiceType type = new ServiceType();   // required
    String description = "This service provides implementation of DRS specification for accessing files for the datasets within Glycan Array Repository.";
    Organization organization;   // required
    String contactUrl = "mailto:glygenarray.api@gmail.com";    //example: mailto:support@example.com
    //URL of the documentation of this service (RFC 3986 format).
    String documentationUrl;
    Date createdAt = new java.util.GregorianCalendar(2023, Calendar.JULY, 8).getTime();
    Date updatedAt = new java.util.GregorianCalendar(2023, Calendar.JULY, 8).getTime();
    String environment = "test";    // prod, test, dev, staging etc. not enforced
    String version = "1.0.0"; // required
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
     * @return the type
     */
    public ServiceType getType() {
        return type;
    }
    /**
     * @param type the type to set
     */
    public void setType(ServiceType type) {
        this.type = type;
    }
    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }
    /**
     * @return the organization
     */
    public Organization getOrganization() {
        return organization;
    }
    /**
     * @param organization the organization to set
     */
    public void setOrganization(Organization organization) {
        this.organization = organization;
    }
    /**
     * @return the contactUrl
     */
    public String getContactUrl() {
        return contactUrl;
    }
    /**
     * @param contactUrl the contactUr to set
     */
    public void setContactUrl(String contactUrl) {
        this.contactUrl = contactUrl;
    }
    /**
     * @return the documentationUrl
     */
    public String getDocumentationUrl() {
        return documentationUrl;
    }
    /**
     * @param documentationUrl the documentationUrl to set
     */
    public void setDocumentationUrl(String documentationUrl) {
        this.documentationUrl = documentationUrl;
    }
    /**
     * @return the createdAt
     */
    public Date getCreatedAt() {
        return createdAt;
    }
    /**
     * @param createdAt the createdAt to set
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    /**
     * @return the updatedAt
     */
    public Date getUpdatedAt() {
        return updatedAt;
    }
    /**
     * @param updatedAt the updatedAt to set
     */
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    /**
     * @return the environment
     */
    public String getEnvironment() {
        return environment;
    }
    /**
     * @param environment the environment to set
     */
    public void setEnvironment(String environment) {
        this.environment = environment;
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
