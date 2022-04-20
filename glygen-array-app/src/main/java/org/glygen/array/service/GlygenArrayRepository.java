package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.data.ChangeLog;
import org.glygen.array.persistence.rdf.data.ChangeTrackable;
import org.glygen.array.persistence.rdf.data.FutureTask;
import org.glygen.array.view.AsyncBatchUploadResult;

public interface GlygenArrayRepository {
	
	public static final String DEFAULT_GRAPH = "http://glygen.org/glygenarray/public";
	public static final String PRIVATE_GRAPH = "http://glygen.org/glygenarray/private";
	public static final String uriPrefix = "http://glygen.org/glygenarray/";
	public static final String uriPrefixPublic = "http://glygen.org/glygenarray/public/";
	public static final String ontPrefix = "http://purl.org/gadr/data#";
	public static final String ontPrefix2 = "http://purl.org/gadr/data/";

	/**
	 * adds a new graph to the repository for the given user
	 * it also saves an entry in relational database's private graph table
	 * @param uEntity user to create a graph for
	 * @return the URI of the graph created for the user
	 * @throws SQLException if graph cannot be created 
	 */
	String addPrivateGraphForUser(UserEntity uEntity) throws SQLException;
	
	/**
	 * retrieves the graph uri for the given user. If it does not exist, it will try to create
	 * @param user the user to retrieve the graph for 
	 * @return the graph URI for the user
	 * @throws SQLException
	 */
	String getGraphForUser(UserEntity user) throws SQLException;
	
	List<String> getAllUserGraphs() throws SQLException;
	
	void resetRepository () throws SQLException;

    String saveChangeLog(ChangeLog change, String entryURI, String graph) throws SparqlException, SQLException;

    void retrieveChangeLog(ChangeTrackable entity, String entityUri, String graph) throws SparqlException, SQLException;

    void updateStatus(String uri, FutureTask task, UserEntity user) throws SparqlException, SQLException;
    String addBatchUpload(AsyncBatchUploadResult result, String type, UserEntity user)
            throws SparqlException, SQLException;

    String updateBatchUpload(AsyncBatchUploadResult result, UserEntity user) throws SparqlException, SQLException;
    
    AsyncBatchUploadResult getBatchUpload (String uri, UserEntity user) throws SparqlException, SQLException;

    List<AsyncBatchUploadResult> getActiveBatchUploadByType(String type, UserEntity user)
            throws SparqlException, SQLException;
}