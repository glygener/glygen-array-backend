package org.glygen.array.persistence.rdf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.Min;

import org.glygen.array.persistence.rdf.metadata.SpotMetadata;
import org.grits.toolbox.glycanarray.library.om.layout.LevelUnit;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class Spot {
	List<Feature> features;
	Integer row;
	Integer column;
	String group;
	String blockLayoutUri;
	String uri;
	String flag;
	
	SpotMetadata metadata;
	
	Map<String, RatioConcentration> ratioConcentrationMap = new HashMap<>(); // featureId to concentration
	Map<Feature, Double> featureRatioMap = new HashMap<Feature, Double>();
	Map<Feature, LevelUnit> featureConcentrationMap = new HashMap<Feature, LevelUnit>();
	
	@JsonIgnore
	public Map<Feature, Double> getFeatureRatioMap() {
        return featureRatioMap;
    }
	
	public void setFeatureRatioMap(Map<Feature, Double> featureRatioMap) {
        this.featureRatioMap = featureRatioMap;
    }
	
	
	@JsonAnySetter
	public void setRatioConcentration (String key, RatioConcentration value) {
	    this.ratioConcentrationMap.put(key, value);
	}
	
	public RatioConcentration getRatioConcentration (String featureId) {
	    return this.ratioConcentrationMap.get(featureId);
	}
	
	public void setFlag(String flag) {
		this.flag = flag;
	}
	
	public String getFlag() {
		return flag;
	}
	
	/**
	 * @return the feature
	 */
	public List<Feature> getFeatures() {
		return features;
	}
	/**
	 * @param feature the feature to set
	 */
	public void setFeatures(List<Feature> feature) {
		this.features = feature;
	}
	/**
	 * @return the row
	 */
	@Min(value=1, message = "row must be a positive integer")
	public Integer getRow() {
		return row;
	}
	/**
	 * @param row the row to set
	 */
	public void setRow(Integer row) {
		this.row = row;
	}
	/**
	 * @return the column
	 */
	@Min(value=1, message = "column must be a positive integer")
	public Integer getColumn() {
		return column;
	}
	/**
	 * @param column the column to set
	 */
	public void setColumn(Integer column) {
		this.column = column;
	}
	/**
	 * @return the group
	 */
	public String getGroup() {
		return group;
	}
	/**
	 * @param group the group to set
	 */
	public void setGroup(String group) {
		this.group = group;
	}
	
	public void setUri(String uri) {
		this.uri = uri;
	}
	
	public String getUri() {
		return uri;
	}

    /**
     * @return the blockLayoutUri
     */
    public String getBlockLayoutUri() {
        return blockLayoutUri;
    }

    /**
     * @param blockLayoutUri the blockLayoutUri to set
     */
    public void setBlockLayoutUri(String blockLayoutUri) {
        this.blockLayoutUri = blockLayoutUri;
    }

    /**
     * @return the metadata
     */
    public SpotMetadata getMetadata() {
        return metadata;
    }

    /**
     * @param metadata the metadata to set
     */
    public void setMetadata(SpotMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * @return the featureConcentrationMap
     */
    @JsonIgnore
    public Map<Feature, LevelUnit> getFeatureConcentrationMap() {
        return featureConcentrationMap;
    }

    /**
     * @param featureConcentrationMap the featureConcentrationMap to set
     */
    public void setFeatureConcentrationMap(Map<Feature, LevelUnit> featureConcentrationMap) {
        this.featureConcentrationMap = featureConcentrationMap;
    }

    /**
     * @return the ratioConcentrationMap
     */
    public Map<String, RatioConcentration> getRatioConcentrationMap() {
        return ratioConcentrationMap;
    }

    /**
     * @param ratioConcentrationMap the ratioConcentrationMap to set
     */
    public void setRatioConcentrationMap(Map<String, RatioConcentration> ratioConcentrationMap) {
        this.ratioConcentrationMap = ratioConcentrationMap;
    }
}
