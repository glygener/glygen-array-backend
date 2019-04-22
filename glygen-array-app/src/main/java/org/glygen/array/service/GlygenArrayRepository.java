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

	void addSlideLayout (SlideLayout s, UserEntity user, boolean isPrivate) throws SparqlException;
	void addSlideLayout (SlideLayout s, UserEntity user) throws SparqlException;
	
	void addBlockLayout (BlockLayout b, UserEntity user, boolean isPrivate) throws SparqlException;
	void addBlockLayout (BlockLayout b, UserEntity user) throws SparqlException;
	
	SlideLayout findSlideLayoutByName (String name) throws SparqlException;
	SlideLayout findSlideLayoutByName (String name, String username) throws SparqlException;
	
	BlockLayout findBlockLayoutByName (String name) throws SparqlException;
	BlockLayout findBlockLayoutByName (String name, String username) throws SparqlException;
	
	List<SlideLayout> findSlideLayoutByUser (String username) throws SparqlException;
	
	String addGlycan(Glycan g, UserEntity user) throws SparqlException;
	String addGlycan(Glycan g, UserEntity user, boolean isPrivate) throws SparqlException;
	
	Glycan getGlycan (String glytoucanId) throws SparqlException;
	Glycan getGlycan (String glytoucanId, UserEntity user, boolean isPrivate) throws SparqlException;
	Glycan getGlycanFromURI (String uri);
	
	Glycan getGlycanBySequence (String sequence) throws SparqlException;
	Glycan getGlycanBySequence (String sequence, UserEntity user, boolean isPrivate) throws SparqlException;
	
	List<Glycan> getGlycanByUser (UserEntity user) throws SparqlException;
	List<Glycan> getGlycanByUser (UserEntity user, int offset, int limit) throws SparqlException;
	
	String addPrivateGraphForUser(UserEntity uEntity) throws SQLException;
	String getGraphForUser(UserEntity user) throws SQLException;
	
	void deleteGlycan (String glycanId, UserEntity user) throws SparqlException;
	void updateGlycan (Glycan g, UserEntity user) throws SparqlException;
}
