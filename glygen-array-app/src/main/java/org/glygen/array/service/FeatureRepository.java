package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.FeatureType;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.data.ChangeLog;

public interface FeatureRepository {
	String addFeature (Feature f, UserEntity u) throws SparqlException, SQLException;
	List<Feature> getFeatureByUser (UserEntity user) throws SparqlException, SQLException;
	List<Feature> getFeatureByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
	
	int getFeatureCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException;
	void deleteFeature (String featureId, UserEntity user) throws SparqlException, SQLException;
	Feature getFeatureFromURI(String featureURI, UserEntity user) throws SparqlException, SQLException;
	Feature getFeatureByLabel(String label, UserEntity user) throws SparqlException, SQLException;
    List<Feature> getFeatureByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    List<Feature> getFeatureByUser (UserEntity user, int offset, int limit, String field, int order, String searchValue, 
            FeatureType featureType) throws SparqlException, SQLException;
    List<Feature> getFeatureByUser (UserEntity user, int offset, int limit, String field, int order, String searchValue, 
            FeatureType featureType, boolean includePublic) throws SparqlException, SQLException;
    Feature getFeatureById(String featureId, UserEntity user) throws SparqlException, SQLException;
    String addPublicFeature(Feature feature, UserEntity user) throws SparqlException, SQLException;
    Feature getFeatureByGlycanLinker(Glycan glycan, Linker linker, String slideLayoutURI, String blockLayoutURI,
            UserEntity user) throws SparqlException, SQLException;
    Feature getFeatureByLabel(String label, String predicate, UserEntity user) throws SparqlException, SQLException;
    int getFeatureCountByUserByType(UserEntity user, FeatureType featureType, String searchValue)
            throws SQLException, SparqlException;
    int getFeatureCountByUser(UserEntity user, String searchValue, boolean includePublic)
            throws SQLException, SparqlException;
    int getFeatureCountByUserByType(UserEntity user, FeatureType featureType, String searchValue, boolean includePublic)
            throws SQLException, SparqlException;
    
    void updateFeature(Feature g, UserEntity user) throws SparqlException, SQLException;
    void updateFeature(Feature g, UserEntity user, ChangeLog change) throws SparqlException, SQLException;
    
}
