package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.Block;
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.Spot;

public interface LayoutRepository {
	
	String addBlockLayout (BlockLayout b, UserEntity user) throws SparqlException, SQLException;
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
	int getBlockLayoutCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException;

	List<SlideLayout> getSlideLayoutByUser(UserEntity user) throws SparqlException, SQLException;


	BlockLayout getBlockLayoutByName(String name, UserEntity user) throws SparqlException, SQLException;
	SlideLayout getSlideLayoutByName(String name, UserEntity user) throws SparqlException, SQLException;

	List<SlideLayout> getSlideLayoutByUser(UserEntity user, int offset, int limit, String field, int order)
			throws SparqlException, SQLException;
	
	
	int getSlideLayoutCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException;

	List<BlockLayout> getBlockLayoutByUser(UserEntity user, Integer offset, Integer limit, String field,
			Boolean loadAll, Integer order) throws SparqlException, SQLException;

	List<SlideLayout> getSlideLayoutByUser(UserEntity user, Integer offset, Integer limit, String field,
			Boolean loadAll, Integer order) throws SparqlException, SQLException;
	List<SlideLayout> getSlideLayoutByUser(UserEntity user, Integer offset, Integer limit, String field, 
	        Boolean loadAll, Integer order, String searchValue)
            throws SparqlException, SQLException;

	void updateBlockLayout(BlockLayout layout, UserEntity user) throws SparqlException, SQLException;

	void updateSlideLayout(SlideLayout layout, UserEntity user) throws SparqlException, SQLException;
	
	BlockLayout getBlockLayoutFromURI(String blockLayoutURI, Boolean loadAll, UserEntity user) throws SparqlException, SQLException;
    List<BlockLayout> getBlockLayoutByUser(UserEntity user, Integer offset, Integer limit, String field,
            Boolean loadAll, Integer order, String searchValue) throws SparqlException, SQLException;
    BlockLayout getBlockLayoutByName(String name, UserEntity user, boolean loadAll)
            throws SparqlException, SQLException;
    Spot getSpotFromURI(String spotURI, UserEntity user) throws SQLException, SparqlException;
    String makePublic(SlideLayout layout, UserEntity user, Map<String, String> spotIdMap)
            throws SparqlException, SQLException;
    SlideLayout getSlideLayoutFromURI(String slideLayoutURI, Boolean loadAll, UserEntity user)
            throws SparqlException, SQLException;
    String addSlideLayout(SlideLayout s, UserEntity user, Boolean layoutOnly) throws SparqlException, SQLException;
    String addBlocksToSlideLayout(SlideLayout s, UserEntity user) throws SparqlException, SQLException;
    Spot getSpotFromURI(String spotURI, Boolean loadAll, UserEntity user) throws SQLException, SparqlException;
    String getPublicBlockURI(String blockURI, String publicSlideLayoutURI, UserEntity user)
            throws SparqlException, SQLException;
    List<Spot> getSpotByFeatures(List<Feature> features, String slideLayoutURI, String groupId, UserEntity user)
            throws SparqlException, SQLException;
    String getSpotByPosition(String slideLayoutURI, int row, int column, UserEntity user)
            throws SparqlException, SQLException;
}
