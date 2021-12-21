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
	Integer group;
	String blockLayoutUri;
	String uri;
	String flag;
	
	SpotMetadata metadata;
	
	Map<String, Double> ratioMap = new HashMap<>(); // featureId to ratio in percentages
	Map<String, LevelUnit> concentrationMap = new HashMap<>(); // featureId to concentration
	Map<Feature, Double> featureRatioMap = new HashMap<Feature, Double>();
	Map<Feature, LevelUnit> featureConcentrationMap = new HashMap<Feature, LevelUnit>();
	
	@JsonIgnore
	public Map<Feature, Double> getFeatureRatioMap() {
        return featureRatioMap;
    }
	
	public void setFeatureRatioMap(Map<Feature, Double> featureRatioMap) {
        this.featureRatioMap = featureRatioMap;
    }
	
	//@JsonAnyGetter
	public Map<String, Double> getRatioMap() {
        return ratioMap;
    }
	
	public void setRatioMap(Map<String, Double> ratioMap) {
        this.ratioMap = ratioMap;
    }
	
	@JsonAnySetter
	public void setRatio (String key, Double value) {
	    this.ratioMap.put(key, value);
	}
	
	public Double getRatio (String featureId) {
	    return this.ratioMap.get(featureId);
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
	public Integer getGroup() {
		return group;
	}
	/**
	 * @param group the group to set
	 */
	public void setGroup(Integer group) {
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
     * @return the concentrationMap
     */
    public Map<String, LevelUnit> getConcentrationMap() {
        return concentrationMap;
    }

    /**
     * @param concentrationMap the concentrationMap to set
     */
    public void setConcentrationMap(Map<String, LevelUnit> concentrationMap) {
        this.concentrationMap = concentrationMap;
    }
    
    @JsonAnySetter
    public void setConcentration (String key, LevelUnit value) {
        this.concentrationMap.put(key, value);
    }
    
    public LevelUnit getConcentration (String featureId) {
        return this.concentrationMap.get(featureId);
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
}
