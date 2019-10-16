package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.Feature;

public class FeatureRepositoryImpl extends GlygenArrayRepositoryImpl implements FeatureRepository {

	@Override
	public String addFeature(Feature f, UserEntity u) throws SparqlException, SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Feature> getFeatureByUser(UserEntity user) throws SparqlException, SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Feature> getFeatureByUser(UserEntity user, int offset, int limit, String field, int order)
			throws SparqlException, SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getFeatureCountByUser(UserEntity user) throws SQLException, SparqlException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void deleteFeature(String featureId, UserEntity user) throws SparqlException, SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public Feature getFeatureFromURI(String featureURI, String graph) throws SparqlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateFeature(Feature g, UserEntity user) throws SparqlException, SQLException {
		// TODO Auto-generated method stub

	}

}
