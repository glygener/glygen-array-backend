package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.Linker;

public interface LinkerRepository {
	
	String addLinker (Linker l, UserEntity user) throws SparqlException, SQLException;
	void deleteLinker(String linkerId, UserEntity user) throws SQLException, SparqlException;
	Linker getLinkerById(String linkerId, UserEntity user) throws SparqlException, SQLException;
	Linker getLinkerByLabel(String trim, UserEntity user) throws SparqlException, SQLException;
	
	List<Linker> getLinkerByUser(UserEntity user) throws SQLException, SparqlException;
	List<Linker> getLinkerByUser(UserEntity user, int offset, int limit, String field, int order)
			throws SparqlException, SQLException;

	int getLinkerCountByUser(UserEntity user) throws SQLException, SparqlException;
	void updateLinker(Linker g, UserEntity user) throws SparqlException, SQLException;
	Linker getLinkerFromURI(String linkerURI, String graph) throws SparqlException;
	String getLinkerByField(String field, String predicate, String type) throws SparqlException;
	String getLinkerByField(String field, String predicate, String type, UserEntity user)
			throws SparqlException, SQLException;

}