package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.SlideLayout;

public interface LayoutRepository {
	
	String addBlockLayout (BlockLayout b, UserEntity user) throws SparqlException;
	String addSlideLayout (SlideLayout s, UserEntity user) throws SparqlException, SQLException;
	
	void deleteBlockLayout(String blockLayoutId, UserEntity user) throws SparqlException, SQLException;
	void deleteSlideLayout(String slideLayoutId, UserEntity user) throws SparqlException, SQLException;
	
	BlockLayout getBlockLayoutById(String blockLayoutId, UserEntity user) throws SparqlException, SQLException;
	SlideLayout getSlideLayoutById(String slideLayoutId, UserEntity user) throws SparqlException, SQLException;
	
	BlockLayout getBlockLayoutById(String blockLayoutId, UserEntity user, boolean loadAll) throws SparqlException, SQLException;
	SlideLayout getSlideLayoutById(String slideLayoutId, UserEntity user, boolean loadAll) throws SparqlException, SQLException;
	
	List<BlockLayout> getBlockLayoutByUser(UserEntity user) throws SparqlException, SQLException;
	
	List<BlockLayout> getBlockLayoutByUser(UserEntity user, int offset, int limit, String field, int order)
			throws SparqlException, SQLException;
	int getBlockLayoutCountByUser(UserEntity user) throws SQLException, SparqlException;

	List<SlideLayout> getSlideLayoutByUser(UserEntity user) throws SparqlException, SQLException;


	BlockLayout getBlockLayoutByName(String name, UserEntity user) throws SparqlException, SQLException;
	SlideLayout getSlideLayoutByName(String name, UserEntity user) throws SparqlException, SQLException;

	List<SlideLayout> getSlideLayoutByUser(UserEntity user, int offset, int limit, String field, int order)
			throws SparqlException, SQLException;
	
	int getSlideLayoutCountByUser(UserEntity user) throws SQLException, SparqlException;

	List<BlockLayout> getBlockLayoutByUser(UserEntity user, Integer offset, Integer limit, String field,
			Boolean loadAll, Integer order) throws SparqlException, SQLException;

	List<SlideLayout> getSlideLayoutByUser(UserEntity user, Integer offset, Integer limit, String field,
			Boolean loadAll, Integer order) throws SparqlException, SQLException;

	void updateBlockLayout(BlockLayout layout, UserEntity user) throws SparqlException, SQLException;

	void updateSlideLayout(SlideLayout layout, UserEntity user) throws SparqlException, SQLException;

}