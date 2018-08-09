package org.glygen.array.logging.DBappender;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import ch.qos.logback.classic.db.DBAppender;
import ch.qos.logback.classic.db.names.DBNameResolver;
import ch.qos.logback.classic.db.names.DefaultDBNameResolver;
import ch.qos.logback.classic.spi.*;
import ch.qos.logback.core.db.DBAppenderBase;

public class AccessLogDBAppender extends DBAppenderBase<ILoggingEvent> {

	protected String insertSQL;
    protected static final Method GET_GENERATED_KEYS_METHOD;

    private DBNameResolver dbNameResolver;
	
    static final int TIMESTMP_INDEX = 1;
    static final int RQUEST_MESSAGE_INDEX = 2;
    static final int URI_INDEX = 3;
    static final int REQUEST_PAYLOAD_INDEX = 4;
    static final int RESPONSE_PAYLOAD = 5;
    static final int CALLER_USER_INDEX = 6;
    
    static {
        // PreparedStatement.getGeneratedKeys() method was added in JDK 1.4
        Method getGeneratedKeysMethod;
        try {
            // the
            getGeneratedKeysMethod = PreparedStatement.class.getMethod("getGeneratedKeys", (Class[]) null);
        	
        } catch (Exception ex) {
            getGeneratedKeysMethod = null;
        }
        System.out.println(getGeneratedKeysMethod);
        GET_GENERATED_KEYS_METHOD = getGeneratedKeysMethod;
    }

    public void setDbNameResolver(DBNameResolver dbNameResolver) {
        this.dbNameResolver = dbNameResolver;
    }
        

    @Override
    public void start() {
        if (dbNameResolver == null)
            dbNameResolver = new DefaultDBNameResolver();
        insertSQL = SQLBuilder.buildInsertAccessLoggingSQL(dbNameResolver);
       
        super.start();
    }
    
    @Override
	protected void subAppend(ILoggingEvent event, Connection connection, PreparedStatement insertStatement) throws Throwable {
		
    	 this.bindLoggingEventWithInsertStatement(insertStatement, event);
         this.bindLoggingEventArgumentsWithPreparedStatement(insertStatement, event.getArgumentArray());

         // This is expensive... should we do it every time?
         this.bindCallerDataWithPreparedStatement(insertStatement, event.getCallerData());

         int updateCount = insertStatement.executeUpdate();
         if (updateCount != 1) {
             addWarn("Failed to insert loggingEvent");
         }
	}
    
    
    private void bindLoggingEventWithInsertStatement(PreparedStatement stmt, ILoggingEvent event) throws SQLException {
    	stmt.setObject(TIMESTMP_INDEX, LocalDateTime.now());
        stmt.setString(RQUEST_MESSAGE_INDEX, event.getFormattedMessage());		
	}
    
    
    private void bindLoggingEventArgumentsWithPreparedStatement(PreparedStatement stmt,
			Object[] argArray) throws SQLException {
    	
    	int arrayLen = argArray != null ? argArray.length : 0;
    	if(arrayLen!=0) {
    		stmt.setString(URI_INDEX, argArray[0].toString());
    		stmt.setString(REQUEST_PAYLOAD_INDEX, argArray[1] == null ? "" : argArray[1].toString());
    		stmt.setString(RESPONSE_PAYLOAD, argArray[2] == null ? "": argArray[2].toString());   
    	}
	}
    
	private void bindCallerDataWithPreparedStatement(PreparedStatement stmt,
			StackTraceElement[] callerData) throws SQLException {
		String currentUserName;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
             currentUserName = authentication.getName();
        }else {
        	currentUserName = "anonymous";
        }
        stmt.setString(CALLER_USER_INDEX, currentUserName);
		
	}

	@Override
	protected String getInsertSQL() {
		return insertSQL;
	}

	@Override
	protected void secondarySubAppend(ILoggingEvent arg0, Connection arg1, long arg2) throws Throwable {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected Method getGeneratedKeysMethod() {
		
		return GET_GENERATED_KEYS_METHOD;
	}

}
