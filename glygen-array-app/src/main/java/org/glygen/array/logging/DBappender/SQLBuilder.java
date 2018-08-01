package org.glygen.array.logging.DBappender;

import org.glygen.array.logging.DBappender.TableName;
import org.glygen.array.logging.DBappender.ColumnName;

import ch.qos.logback.classic.db.names.DBNameResolver;

public class SQLBuilder {

	static String buildInsertEventLoggingSQL (DBNameResolver dbNameResolver) {
		StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ");
			
		sqlBuilder.append(dbNameResolver.getTableName(TableName.LOGGING_EVENT)).append(" (");
		sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.TIMESTMP)).append(", ");
		sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.FORMATTED_MESSAGE)).append(", ");
		sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.LEVEL_STRING)).append(", ");
		sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.PARAMETERS)).append(", ");
		sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.CALLER_FILENAME)).append(", ");
		sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.CALLER_CLASS)).append(", ");
		sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.CALLER_METHOD)).append(", ");
		sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.CALLER_LINE)).append(", ");
		sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.CALLER_USER)).append(", ");
		sqlBuilder.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");		
			
		return sqlBuilder.toString();
	}
	
	static String buildInsertAccessLoggingSQL (DBNameResolver dbNameResolver) {
		StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ");
			
		sqlBuilder.append(dbNameResolver.getTableName(TableName.LOGGING_ACCESS)).append(" (");
		sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.TIMESTMP)).append(", ");
		sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.REQUEST_MESSAGE)).append(", ");
		sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.URI)).append(", ");
		sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.REQUEST_PAYLOAD)).append(", ");
		sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.RESPONSE_PAYLOAD)).append(", ");
		sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.CALLER_USER)).append(", ");
		sqlBuilder.append("VALUES (?, ?, ?, ?, ?, ?)");		
			
		return sqlBuilder.toString();
	}
	
	 static String buildInsertExceptionSQL(DBNameResolver dbNameResolver) {
	        StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ");
	        sqlBuilder.append(dbNameResolver.getTableName(TableName.LOGGING_EVENT_EXCEPTION)).append(" (");
	        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.EVENT_ID)).append(", ");
	        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.I)).append(", ");
	        sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.TRACE_LINE)).append(") ");
	        sqlBuilder.append("VALUES (?, ?, ?)");
	        return sqlBuilder.toString();
	    }
}








