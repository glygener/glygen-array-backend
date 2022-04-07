package org.glygen.array.persistence.rdf;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.Size;

import org.glygen.array.config.ValidationConstants;
import org.glygen.array.persistence.rdf.data.ChangeLog;
import org.glygen.array.persistence.rdf.data.ChangeTrackable;
import org.glygen.array.persistence.rdf.metadata.FeatureMetadata;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;


@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, 
        include = JsonTypeInfo.As.PROPERTY, 
        property = "type",
        visible = true)
    @JsonSubTypes({ 
        @Type(value = LinkedGlycan.class, name = "LINKEDGLYCAN"), 
        @Type(value = GlycoLipid.class, name = "GLYCOLIPID"),
        @Type(value = GlycoPeptide.class, name = "GLYCOPEPTIDE"),
        @Type(value = GlycoProtein.class, name = "GLYCOPROTEIN"),
        @Type(value = GPLinkedGlycoPeptide.class, name = "GPLINKEDGLYCOPEPTIDE"),
        @Type(value = LandingLight.class, name = "LANDING_LIGHT"),
        @Type(value = ControlFeature.class, name = "CONTROL"),
        @Type(value = NegControlFeature.class, name = "NEGATIVE_CONTROL"),
        @Type(value = CompoundFeature.class, name = "COMPOUND")
    })
public class Feature implements ChangeTrackable {
	String id;
	String uri;
	String name;
	String internalId;
	String description;
	Linker linker;
	FeatureMetadata metadata;
	
	Map<String, String> positionMap = new HashMap<>(); // position to glycanId map
	
	FeatureType type;
	
	Date dateModified;
	Date dateCreated;
	Date dateAddedToLibrary;
	
	List<ChangeLog> changes = new ArrayList<ChangeLog>();

	Boolean inUse = false;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	@Size(max=ValidationConstants.NAME_LIMIT, message="Name cannot exceed " + ValidationConstants.NAME_LIMIT + " characters")
	public String getName() {
        return name;
    }
	
	public void setName(String name) {
        this.name = name;
    }
	
	//@JsonAnyGetter
	public Map<String, String> getPositionMap() {
		return positionMap;
	}
	
	@JsonAnySetter
	public void setGlycan (String key, String value) {
	    this.positionMap.put(key, value);
	}
	
	public String getGlycan (String position) {
		return positionMap.get(position);
	}
	
	/**
	 * @return the linker
	 */
	public Linker getLinker() {
		return linker;
	}
	/**
	 * @param linker the linker to set
	 */
	public void setLinker(Linker linker) {
		this.linker = linker;
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
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Feature))
			return false;
		if (uri != null && ((Feature)obj).getUri() != null)
			return  uri.equals(((Feature)obj).getUri());
		return super.equals(obj);
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

	/**
	 * @param positionMap the positionMap to set
	 */
	public void setPositionMap(Map<String, String> positionMap) {
		this.positionMap = positionMap;
	}
	
	public void setType(FeatureType type) {
        this.type = type;
    }
	
	public FeatureType getType() {
        return type;
    }

    /**
     * @return the internalId
     */
	@Size(max=ValidationConstants.NAME_LIMIT, message="InternalId cannot exceed " + ValidationConstants.NAME_LIMIT + " characters")
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
     * @return the metadata
     */
    public FeatureMetadata getMetadata() {
        return metadata;
    }

    /**
     * @param metadata the metadata to set
     */
    public void setMetadata(FeatureMetadata metadata) {
        this.metadata = metadata;
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
    
    @Override
    public int hashCode() {
        if (uri != null)
            return uri.hashCode();
        return super.hashCode();
    }
}
