package org.glygen.array.client.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME, 
		include = JsonTypeInfo.As.PROPERTY, 
		property = "type")
	@JsonSubTypes({ 
		@Type(value = SequenceDefinedGlycan.class, name = "SEQUENCE_DEFINED"), 
		@Type(value = MassOnlyGlycan.class, name = "MASS_ONLY"),
		@Type(value = UnknownGlycan.class, name = "UNKNOWN"),
		@Type(value = Glycan.class, name = "COMPOSITION_BASED"),
		@Type(value = Glycan.class, name = "CLASSIFICATION_BASED"),
		@Type(value = Glycan.class, name = "FRAGMENT_ONLY")
	})
public class Glycan {
	String id;
	String uri;
	String internalId;
	String name;
	String comment;
	Date dateModified;
	Date dateCreated;
	Date dateAddedToLibrary;
	List<String> aliases = new ArrayList<String>();
	GlycanType type;
	Creator owner;
	Boolean isPublic = false;
	byte[] cartoon;
	
	public Boolean getIsPublic() {
		return isPublic;
	}
	
	public void setIsPublic(Boolean isPublic) {
		this.isPublic = isPublic;
	}
	
	public Creator getUser() {
		return owner;
	}
	
	public void setUser(Creator owner) {
		this.owner = owner;
	}
	
	public byte[] getCartoon() {
		return cartoon;
	}
	
	public void setCartoon(byte[] cartoon) {
		this.cartoon = cartoon;
	}
	
	public String getId() {
		return id;
	}
	
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
	 * @return the internalId
	 */
	public String getInternalId() {
		return internalId;
	}
	/**
	 * @param internalId the internalId to set
	 */
	public void setInternalId(String internalId) {
		this.internalId = internalId;
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
	 * @return the comment
	 */
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
	
	public List<String> getAliases() {
		return aliases;
	}
	
	public void setAliases(List<String> aliases) {
		this.aliases = aliases;
	}
	
	public GlycanType getType() {
		return this.type;
	}

	public void setType(GlycanType t) {
		this.type = t;
	}	
	
	public void addAlias (String alias) {
		if (this.aliases == null)
			this.aliases = new ArrayList<String>();
		if (!this.aliases.contains(alias))
			this.aliases.add(alias);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Glycan))
			return false;
		if (uri != null && ((Glycan)obj).getUri()!= null)
			return  uri.equals(((Glycan)obj).getUri());
		
		return false;
	}
	
	@Override
	public int hashCode() {
		if (uri != null)
			return uri.hashCode();
		
		return super.hashCode();
	}
}
