package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.Glycan;
import org.grits.toolbox.glycanarray.library.om.layout.BlockLayout;
import org.grits.toolbox.glycanarray.library.om.layout.SlideLayout;

public interface GlygenArrayRepository {
	
	public static final String DEFAULT_GRAPH = "http://glygen.org/glygenarray/public";
	public static final String PRIVATE_GRAPH = "http://glygen.org/glygenarray/private";

	void addSlideLayout (SlideLayout s, UserEntity user) throws SparqlException;
	
	void addBlockLayout (BlockLayout b, UserEntity user) throws SparqlException;
	
	SlideLayout findSlideLayoutByName (String name) throws SparqlException;
	SlideLayout findSlideLayoutByName (String name, String username) throws SparqlException;
	
	BlockLayout findBlockLayoutByName (String name) throws SparqlException;
	BlockLayout findBlockLayoutByName (String name, String username) throws SparqlException;
	
	List<SlideLayout> findSlideLayoutByUser (String username) throws SparqlException;
	
	String addGlycan(Glycan g, UserEntity user) throws SparqlException;
	
	//Glycan getGlycan (String glytoucanId) throws SparqlException, SQLException;
	//Glycan getGlycan (String glytoucanId, UserEntity user) throws SparqlException, SQLException;
	
	Glycan getGlycanBySequence (String sequence) throws SparqlException;
	Glycan getGlycanBySequence (String sequence, UserEntity user) throws SparqlException, SQLException;
	
	List<Glycan> getGlycanByUser (UserEntity user) throws SparqlException, SQLException;
	List<Glycan> getGlycanByUser (UserEntity user, int offset, int limit, String field, int order) throws SparqlException, SQLException;
	
	String addPrivateGraphForUser(UserEntity uEntity) throws SQLException;
	String getGraphForUser(UserEntity user) throws SQLException;
	
	void deleteGlycan (String glycanId, UserEntity user) throws SparqlException, SQLException;
	void updateGlycan (Glycan g, UserEntity user) throws SparqlException, SQLException;

	Glycan getGlycanById(String glycanId, UserEntity user) throws SparqlException, SQLException;

	int getGlycanCountByUser(UserEntity user) throws SQLException, SparqlException;
	
}