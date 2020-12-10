package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.Linker;

public interface FeatureRepository {
	String addFeature (Feature f, UserEntity u) throws SparqlException, SQLException;
	List<Feature> getFeatureByUser (UserEntity user) throws SparqlException, SQLException;
	List<Feature> getFeatureByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
	int getFeatureCountByUser(UserEntity user) throws SQLException, SparqlException;
	void deleteFeature (String featureId, UserEntity user) throws SparqlException, SQLException;
	Feature getFeatureFromURI(String featureURI, UserEntity user) throws SparqlException, SQLException;
	Feature getFeatureByLabel(String label, UserEntity user) throws SparqlException, SQLException;
    List<Feature> getFeatureByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    Feature getFeatureById(String featureId, UserEntity user) throws SparqlException, SQLException;
	Feature getFeatureByGlycanLinker (Glycan glycan, Linker linker, UserEntity user) throws SparqlException, SQLException;
    String addPublicFeature(Feature feature, UserEntity user) throws SparqlException, SQLException;
    String getPublicFeatureId(String featureId, UserEntity user) throws SQLException, SparqlException;
}
