package org.glygen.array.client.model.metadata;

public class Sample extends MetadataCategory {
    
    public Sample() {
    }
    
    public Sample(MetadataCategory metadata) {
        this.id = metadata.id;
        this.uri = metadata.uri;
        this.dateAddedToLibrary = metadata.dateAddedToLibrary;
        this.dateCreated = metadata.dateCreated;
        this.dateModified = metadata.dateModified;
        this.description = metadata.description;
        this.descriptorGroups = metadata.descriptorGroups;
        this.descriptors = metadata.descriptors;
        this.isPublic = metadata.isPublic;
        this.name = metadata.name;
        this.template = metadata.template;
        this.user = metadata.user;
        this.templateType = metadata.templateType;
    }
    
}