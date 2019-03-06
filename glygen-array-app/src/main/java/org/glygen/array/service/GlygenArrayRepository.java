package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.grits.toolbox.glycanarray.library.om.feature.Glycan;
import org.grits.toolbox.glycanarray.library.om.layout.BlockLayout;
import org.grits.toolbox.glycanarray.library.om.layout.SlideLayout;

public interface GlygenArrayRepository {
	
	public static final String DEFAULT_GRAPH = "http://glygen.org/glygenarray/public";
	public static final String PRIVATE_GRAPH = "http://glygen.org/glygenarray/private";

	void addSlideLayout (SlideLayout s, String username, boolean isPrivate) throws SparqlException;
	void addSlideLayout (SlideLayout s, String username) throws SparqlException;
	
	SlideLayout findSlideLayoutByName (String name) throws SparqlException;
	SlideLayout findSlideLayoutByName (String name, String username) throws SparqlException;
	BlockLayout findBlockLayoutByName (String name) throws SparqlException;
	BlockLayout findBlockLayoutByName (String name, String username) throws SparqlException;
	
	List<SlideLayout> findSlideLayoutByUser (String username) throws SparqlException;
	
	void addGlycan(Glycan g) throws SparqlException;
	void addGlycan(Glycan g, UserEntity user, boolean isPrivate) throws SparqlException;
	
	String addPrivateGraphForUser(UserEntity uEntity) throws SQLException;
}
