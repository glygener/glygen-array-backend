package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.SlideLayout;

public interface GlygenArrayRepository {
	
	public static final String DEFAULT_GRAPH = "http://glygen.org/glygenarray/public";
	public static final String PRIVATE_GRAPH = "http://glygen.org/glygenarray/private";
	public static final String uriPrefix = "http://glygen.org/glygenarray/";
	public static final String ontPrefix = "http://purl.org/gadr/data#";

	String addBlockLayout (BlockLayout b, UserEntity user) throws SparqlException;
	
	String addGlycan(Glycan g, UserEntity user) throws SparqlException;
	
	String addLinker (Linker l, UserEntity user) throws SparqlException;
	
	String addPrivateGraphForUser(UserEntity uEntity) throws SQLException;
	
	void addSlideLayout (SlideLayout s, UserEntity user) throws SparqlException;
	
	void deleteBlockLayout(String blockLayoutId, UserEntity user) throws SparqlException, SQLException;
	
	//Glycan getGlycan (String glytoucanId) throws SparqlException, SQLException;
	//Glycan getGlycan (String glytoucanId, UserEntity user) throws SparqlException, SQLException;
	
	void deleteGlycan (String glycanId, UserEntity user) throws SparqlException, SQLException;
	
	void deleteLinker(String linkerId, UserEntity user) throws SQLException, SparqlException;
	
	BlockLayout findBlockLayoutByName (String name, String username) throws SparqlException;
	
	SlideLayout findSlideLayoutByName (String name, String username) throws SparqlException;
	
	BlockLayout getBlockLayoutById(String blockLayoutId, UserEntity user) throws SparqlException, SQLException;
	
	List<BlockLayout> getBlockLayoutByUser(UserEntity user) throws SparqlException, SQLException;
	
	List<BlockLayout> getBlockLayoutByUser(UserEntity user, int offset, int limit, String field, int order)
			throws SparqlException, SQLException;
	Glycan getGlycanById(String glycanId, UserEntity user) throws SparqlException, SQLException;
	
	Glycan getGlycanByInternalId(String glycanId, UserEntity user) throws SparqlException, SQLException;
	Glycan getGlycanByLabel(String glycanId, UserEntity user) throws SparqlException, SQLException;
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

	int getGlycanCountByUser(UserEntity user) throws SQLException, SparqlException;

	String getGraphForUser(UserEntity user) throws SQLException;

	Linker getLinkerById(String linkerId, UserEntity user) throws SparqlException, SQLException;

	Linker getLinkerByLabel(String trim, UserEntity user) throws SparqlException, SQLException;

	/**
	 * check if the linker exists in global graph (default-graph)
	 * @param pubChemId pubchem id
	 * @return the URI of the existing linker or null if it does not exist
	 * @throws SparqlException
	 */
	public String getLinkerByPubChemId(String pubChemId) throws SparqlException;

	/**
	 * check if the linker with the given pubchemId is already in user's private graph
	 * @param pubChemId pubchem id
	 * @param user user information
	 * @return the URI of the existing linker or null if it does not exist
	 * @throws SparqlException 
	 * @throws SQLException thrown if the user's graph cannot be accessed
	 */
	public String getLinkerByPubChemId (String pubChemId, UserEntity user) throws SparqlException, SQLException;

	List<Linker> getLinkerByUser(UserEntity user) throws SQLException, SparqlException;

	List<Linker> getLinkerByUser(UserEntity user, int offset, int limit, String field, int order)
			throws SparqlException, SQLException;

	int getLinkerCountByUser(UserEntity user) throws SQLException, SparqlException;

	List<SlideLayout> getSlideLayoutByUser(UserEntity user) throws SparqlException, SQLException;

	void updateGlycan (Glycan g, UserEntity user) throws SparqlException, SQLException;

	void updateLinker(Linker g, UserEntity user) throws SparqlException, SQLException;
	
}