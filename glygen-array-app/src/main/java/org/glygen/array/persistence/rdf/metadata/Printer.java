package org.glygen.array.persistence.rdf.metadata;

public class Printer extends MetadataCategory {
    
    public Printer() {
        // TODO Auto-generated constructor stub
    }

    public Printer(MetadataCategory metadata) {
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
    }
}
