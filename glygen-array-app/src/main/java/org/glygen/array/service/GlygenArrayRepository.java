package org.glygen.array.service;

import java.sql.SQLException;

import org.glygen.array.persistence.UserEntity;

public interface GlygenArrayRepository {
	
	public static final String DEFAULT_GRAPH = "http://glygen.org/glygenarray/public";
	public static final String PRIVATE_GRAPH = "http://glygen.org/glygenarray/private";
	public static final String uriPrefix = "http://glygen.org/glygenarray/";
	public static final String ontPrefix = "http://purl.org/gadr/data#";

	String addPrivateGraphForUser(UserEntity uEntity) throws SQLException;
	String getGraphForUser(UserEntity user) throws SQLException;
}