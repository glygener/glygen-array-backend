package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.Glycan;

public interface GlycanRepository {
	
	String addGlycan(Glycan g, UserEntity user) throws SparqlException, SQLException;
	String addGlycan(Glycan g, UserEntity user, boolean noGlytoucanRegistration) throws SparqlException, SQLException;
	
	void addAliasForGlycan (String glycanId, String alias, UserEntity user) throws SparqlException, SQLException;
	Glycan getGlycanById(String glycanId, UserEntity user) throws SparqlException, SQLException;
	
	Glycan getGlycanByInternalId(String glycanId, UserEntity user) throws SparqlException, SQLException;
	Glycan getGlycanByLabel(String label, UserEntity user) throws SparqlException, SQLException;
	/**
	 * check if the glycan exists in global graph (default-graph)
	 * @param sequence glycoCT sequence
	 * @return the URI of the existing glycan or null if it does not exist
	 * @throws SparqlException
	 */
	String getGlycanBySequence (String sequence) throws SparqlException;

	/**
	 * check if the glycan with the given sequence is already in user's private graph
	 * @param sequence glycoCT sequence
	 * @param user user information
	 * @return the URI of the existing glycan or null if it does not exist
	 * @throws SparqlException 
	 * @throws SQLException thrown if the user's graph cannot be accessed
	 */
	String getGlycanBySequence (String sequence, UserEntity user) throws SparqlException, SQLException;
	/**
	 * get all glycans for the given user in descending order of the URI
	 * @param user user information
	 * @return list of Glycan objects
	 * @throws SparqlException
	 * @throws SQLException
	 */
	List<Glycan> getGlycanByUser (UserEntity user) throws SparqlException, SQLException;
	/**
	 * get all glycans for the given user in the given sort order of the provided field
	 * 
	 * @param user user information
	 * @param offset start index
	 * @param limit total number to be returned
	 * @param field name of the sort field
	 * @param order 0 for descending, 1 for ascending sort order
	 * @return list of Glycans
	 * @throws SparqlException
	 * @throws SQLException
	 */
	List<Glycan> getGlycanByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
	
	List<Glycan> getSharedGlycansByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;

	int getGlycanCountByUser(UserEntity user) throws SQLException, SparqlException;
	void deleteGlycan (String glycanId, UserEntity user) throws SparqlException, SQLException;
	Glycan getGlycanFromURI(String glycanURI, UserEntity user) throws SparqlException, SQLException;
	void updateGlycan(Glycan g, UserEntity user) throws SparqlException, SQLException;
    List<Glycan> getGlycanByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    String getSearchPredicate(String searchValue);
}
