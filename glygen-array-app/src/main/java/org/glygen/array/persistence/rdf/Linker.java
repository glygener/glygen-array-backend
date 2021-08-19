package org.glygen.array.persistence.rdf;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.Size;

import org.glygen.array.config.ValidationConstants;
import org.glygen.array.persistence.rdf.data.ChangeLog;
import org.glygen.array.persistence.rdf.data.ChangeTrackable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME, 
		include = JsonTypeInfo.As.PROPERTY, 
		property = "type")
	@JsonSubTypes({ 
		@Type(value = SmallMoleculeLinker.class, name = "SMALLMOLECULE"), 
		@Type(value = PeptideLinker.class, name = "PEPTIDE"),
		@Type(value = ProteinLinker.class, name = "PROTEIN"),
		@Type(value = Lipid.class, name = "LIPID"),
		@Type(value = OtherLinker.class, name = "OTHER")
	})
public abstract class Linker implements ChangeTrackable {
	String id;
	String uri;
	String name;
	String comment;
	String description;
	Integer opensRing;    /* 0 does not open ring, 1 opens ring, 2 unknown */
	Date dateModified;
	Date dateCreated;
	Date dateAddedToLibrary;
	List<Publication> publications;
	List<String> urls;
	LinkerType type;
	Creator user;
    Boolean isPublic = false;
    
    Boolean inUse = false;
	
    List<ChangeLog> changes = new ArrayList<ChangeLog>();
    
    public Boolean getIsPublic() {
        return isPublic;
    }
    
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }
    
    public Creator getUser() {
        return user;
    }
    
    public void setUser(Creator owner) {
        this.user = owner;
    }
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * @return the name
	 */
	@Size(max=ValidationConstants.NAME_LIMIT, message="Name cannot exceed " + ValidationConstants.NAME_LIMIT + " characters")
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
	 * @return the comment
	 */
	@Size(max=ValidationConstants.DESCRIPTION_LIMIT, message="Description cannot exceed " + ValidationConstants.DESCRIPTION_LIMIT + " characters")
	public String getComment() {
		return comment;
	}
	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
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
	 * @return the dateModified
	 */
	public Date getDateModified() {
		return dateModified;
	}
	/**
	 * @param dateModified the dateModified to set
	 */
	public void setDateModified(Date dateModified) {
		this.dateModified = dateModified;
	}
	/**
	 * @return the dateCreated
	 */
	public Date getDateCreated() {
		return dateCreated;
	}
	/**
	 * @param dateCreated the dateCreated to set
	 */
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	/**
	 * @return the dateAddedToLibrary
	 */
	public Date getDateAddedToLibrary() {
		return dateAddedToLibrary;
	}
	/**
	 * @param dateAddedToLibrary the dateAddedToLibrary to set
	 */
	public void setDateAddedToLibrary(Date dateAddedToLibrary) {
		this.dateAddedToLibrary = dateAddedToLibrary;
	}
	
	public void setOpensRing(Integer opensRing) {
		this.opensRing = opensRing;
	}
	
	public Integer getOpensRing() {
		return opensRing;
	}
	
	public LinkerType getType() {
		return type;
	}
	
	public void setType(LinkerType type) {
		this.type = type;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Linker))
			return false;
		if (uri != null && ((Linker)obj).getUri() != null)
			return  uri.equals(((Linker)obj).getUri());
		
		return false;
	}
	
	@Override
	public int hashCode() {
		if (uri != null)
			return uri.hashCode();
		return super.hashCode();
	}
	/**
	 * @return the description
	 */
	@Size(max=250, message="descriptions cannot exceed 250 characters")
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
	 * @return the publications
	 */
	public List<Publication> getPublications() {
		return publications;
	}
	/**
	 * @param publications the publications to set
	 */
	public void setPublications(List<Publication> publications) {
		this.publications = publications;
	}
	/**
	 * @return the urls
	 */
	public List<String> getUrls() {
		return urls;
	}
	/**
	 * @param urls the urls to set
	 */
	public void setUrls(List<String> urls) {
		this.urls = urls;
	}

    public void addPublication(Publication publication) {
        if (publications == null) 
            publications = new ArrayList<Publication>();
        publications.add(publication);
    }
    
    @Override
    public List<ChangeLog> getChanges() {
        return this.changes;
    }

    @Override
    public void setChanges(List<ChangeLog> changes) {
        this.changes = changes;
    }

    @Override
    public void addChange(ChangeLog change) {
        this.changes.add(change);
    }

}
