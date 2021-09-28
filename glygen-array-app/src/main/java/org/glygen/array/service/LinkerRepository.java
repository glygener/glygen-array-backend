package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;

import javax.validation.constraints.Size;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.LinkerType;
import org.glygen.array.persistence.rdf.data.ChangeLog;

public interface LinkerRepository {
	
	String addLinker (Linker l, UserEntity user) throws SparqlException, SQLException;
	void deleteLinker(String linkerId, UserEntity user) throws SQLException, SparqlException;
	Linker getLinkerById(String linkerId, UserEntity user) throws SparqlException, SQLException;
	Linker getLinkerByLabel(String trim, UserEntity user) throws SparqlException, SQLException;
	
	List<Linker> getLinkerByUser(UserEntity user) throws SQLException, SparqlException;
	List<Linker> getLinkerByUser(UserEntity user, int offset, int limit, String field, int order)
			throws SparqlException, SQLException;

	void updateLinker(Linker g, UserEntity user) throws SparqlException, SQLException;
	void updateLinker(Linker g, UserEntity user, ChangeLog change) throws SparqlException, SQLException;
	Linker getLinkerFromURI(String linkerURI, UserEntity user) throws SparqlException, SQLException;
	String getLinkerByField(String field, String predicate, String type) throws SparqlException;
	String getLinkerByField(String field, String predicate, String type, UserEntity user)
			throws SparqlException, SQLException;

	List<Linker> getLinkerByUser(UserEntity user, int offset, int limit, String field, int order, String searchValue)
            throws SparqlException, SQLException;
    String makePublic(Linker linker, UserEntity user) throws SparqlException, SQLException;
    String getSearchPredicate(String searchValue, String queryLabel);
    List<Linker> getLinkerByUser(UserEntity user, Integer offset, Integer limit, String field, Integer order,
            String searchValue, LinkerType linkerType) throws SparqlException, SQLException;
    String getLinkerByField(String field, String predicate, String type, LinkerType linkerType, UserEntity user) throws SQLException, SparqlException;
    Linker getLinkerByLabel(String label, LinkerType type, UserEntity user) throws SparqlException, SQLException;
    int getLinkerCountByUser(UserEntity user, String searchValue) throws SQLException, SparqlException;
    int getLinkerCountByUserByType(UserEntity user, LinkerType linkerType, String searchValue)
            throws SparqlException, SQLException;

}
