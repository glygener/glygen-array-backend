package org.glygen.array.persistence.rdf;

import java.util.Date;
import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

import org.glygen.array.config.ValidationConstants;
import org.glygen.array.persistence.rdf.data.FileWrapper;
import org.glygen.array.persistence.rdf.data.FutureTask;

@XmlRootElement
public class SlideLayout extends FutureTask {
	String id;
	String uri;
	String name;
	String description;
	List <Block> blocks;
	Integer width;
	Integer height;
	Date dateCreated;
	Date dateModified;
	Date dateAddedToLibrary;
	Creator user;
	Boolean isPublic = false;
	
	Boolean inUse = false;
	
	FileWrapper file;
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
	 * @return the description
	 */
	@Size(max=ValidationConstants.DESCRIPTION_LIMIT, message="Description cannot exceed " + ValidationConstants.DESCRIPTION_LIMIT + " characters")
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
	 * @return the blocks
	 */
	public List<Block> getBlocks() {
		return blocks;
	}
	/**
	 * @param blocks the blocks to set
	 */
	public void setBlocks(List<Block> blocks) {
		this.blocks = blocks;
	}
	/**
	 * @return the width
	 */
	@Min(value=1, message = "width must be a positive integer")
	public Integer getWidth() {
		return width;
	}
	/**
	 * @param width the width to set
	 */
	public void setWidth(Integer width) {
		this.width = width;
	}
	/**
	 * @return the height
	 */
	@Min(value=1, message = "height must be a positive integer")
	public Integer getHeight() {
		return height;
	}
	/**
	 * @param height the height to set
	 */
	public void setHeight(Integer height) {
		this.height = height;
	}
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
		if (uri != null) 
			this.id = uri.substring(uri.lastIndexOf("/")+1);
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
	public Date getDateAddedToLibrary() {
		return dateAddedToLibrary;
	}
	public void setDateAddedToLibrary(Date dateAddedToLibrary) {
		this.dateAddedToLibrary = dateAddedToLibrary;
	}
	public Creator getUser() {
		return user;
	}
	public void setUser(Creator createdBy) {
		this.user = createdBy;
	}
	public Boolean getIsPublic() {
		return isPublic;
	}
	public void setIsPublic(Boolean isPublic) {
		this.isPublic = isPublic;
	}
    /**
     * @return the file
     */
    public FileWrapper getFile() {
        return file;
    }
    /**
     * @param file the file to set
     */
    public void setFile(FileWrapper file) {
        this.file = file;
    }

	
}
