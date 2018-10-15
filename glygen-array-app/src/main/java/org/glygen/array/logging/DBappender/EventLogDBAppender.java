package org.glygen.array.logging.DBappender;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import ch.qos.logback.classic.db.names.DBNameResolver;
import ch.qos.logback.classic.db.names.DefaultDBNameResolver;
import ch.qos.logback.classic.spi.*;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.db.DBAppenderBase;
import static ch.qos.logback.core.db.DBHelper.closeStatement;


public class EventLogDBAppender extends DBAppenderBase<ILoggingEvent>{
	
	static final Logger log = LoggerFactory.getLogger(EventLogDBAppender.class);

	protected String insertExceptionSQL;
    protected String insertSQL;
    protected static final Method GET_GENERATED_KEYS_METHOD;

    private DBNameResolver dbNameResolver;
	
    static final int TIMESTMP_INDEX = 1;
    static final int FORMATTED_MESSAGE_INDEX = 2;
    static final int LEVEL_STRING_INDEX = 3;
    static final int PARAMETERS_INDEX = 4;
    static final int CALLER_FILENAME_INDEX = 5;
    static final int CALLER_CLASS_INDEX = 6;
    static final int CALLER_METHOD_INDEX = 7;
    static final int CALLER_LINE_INDEX = 8;
    static final int CALLER_USER_INDEX = 9;
    
    static final StackTraceElement EMPTY_CALLER_DATA = CallerData.naInstance();

    
    static {
        // PreparedStatement.getGeneratedKeys() method was added in JDK 1.4
        Method getGeneratedKeysMethod;
        try {
            // the
            getGeneratedKeysMethod = PreparedStatement.class.getMethod("getGeneratedKeys", (Class[]) null);
        } catch (Exception ex) {
            getGeneratedKeysMethod = null;
            log.error("GetGeneratedKeys failed", ex);
        }
        GET_GENERATED_KEYS_METHOD = getGeneratedKeysMethod;
    }

    public void setDbNameResolver(DBNameResolver dbNameResolver) {
        this.dbNameResolver = dbNameResolver;
    }
        
    @Override
    public void start() {
        if (dbNameResolver == null)
            dbNameResolver = new DefaultDBNameResolver();
        insertSQL = SQLBuilder.buildInsertEventLoggingSQL(dbNameResolver);
        insertExceptionSQL = SQLBuilder.buildInsertExceptionSQL(dbNameResolver);
        
        super.start();
    }
    
    
    @Override
	protected void subAppend(ILoggingEvent event, Connection connection, PreparedStatement insertStatement) throws Throwable {
		
    	log.debug("Using event-logger");
    	 bindLoggingEventWithInsertStatement(insertStatement, event);
         bindLoggingEventArgumentsWithPreparedStatement(insertStatement, event.getArgumentArray());

         // This is expensive... should we do it every time?
         bindCallerDataWithPreparedStatement(insertStatement, event.getCallerData());
         
         int updateCount = insertStatement.executeUpdate();
         if (updateCount != 1) {
        	 log.error("Failed to insert loggingEvent");
             addWarn("Failed to insert loggingEvent");
         }
	}
    
    protected void secondarySubAppend(ILoggingEvent event, Connection connection, long eventId)
			throws Throwable {
		if (event.getThrowableProxy() != null) {
            insertThrowable(event.getThrowableProxy(), connection, eventId);
        }
    }
    
    private void bindLoggingEventWithInsertStatement(PreparedStatement stmt, ILoggingEvent event) throws SQLException {
    	 stmt.setObject(TIMESTMP_INDEX, LocalDateTime.now());
         stmt.setString(FORMATTED_MESSAGE_INDEX, event.getFormattedMessage());
         stmt.setString(LEVEL_STRING_INDEX, event.getLevel().toString());
         
	}
    
    private void bindLoggingEventArgumentsWithPreparedStatement(PreparedStatement stmt,
			Object[] argArray) throws SQLException {
		
    	int arrayLen = argArray != null ? argArray.length : 0;
    	
    	
		try {
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
	    	ObjectOutputStream so = new ObjectOutputStream(bo);
			for (int i = 0; i < arrayLen; i++) {
	    		 so.writeObject(argArray[i]);
	    	 }
			so.flush();
			ByteArrayInputStream bais = new ByteArrayInputStream(bo.toByteArray());
			stmt.setBinaryStream(PARAMETERS_INDEX, bais);
			so.close();
		} catch (IOException e) {
			log.error("Logging events to db failed: ", e);
		} 
		
	}
    
	private void bindCallerDataWithPreparedStatement(PreparedStatement stmt,
			StackTraceElement[] callerDataArray) throws SQLException {
		StackTraceElement caller = extractFirstCaller(callerDataArray);

        stmt.setString(CALLER_FILENAME_INDEX, caller.getFileName());
        stmt.setString(CALLER_CLASS_INDEX, caller.getClassName());
        stmt.setString(CALLER_METHOD_INDEX, caller.getMethodName());
        stmt.setString(CALLER_LINE_INDEX, Integer.toString(caller.getLineNumber()));
        String currentUserName;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication !=null && !(authentication instanceof AnonymousAuthenticationToken)) {
             currentUserName = authentication.getName();
        }else {
        	currentUserName = "anonymous";
        }
        stmt.setString(CALLER_USER_INDEX, currentUserName);
	}


	private StackTraceElement extractFirstCaller(StackTraceElement[] callerDataArray) {
		StackTraceElement caller = EMPTY_CALLER_DATA;
        if (hasAtLeastOneNonNullElement(callerDataArray))
            caller = callerDataArray[0];
        return caller;
	}

	private boolean hasAtLeastOneNonNullElement(StackTraceElement[] callerDataArray) {
        return callerDataArray != null && callerDataArray.length > 0 && callerDataArray[0] != null;
	}

	 @Override
	    protected Method getGeneratedKeysMethod() {
	        return GET_GENERATED_KEYS_METHOD;
	    }

	    @Override
	    protected String getInsertSQL() {
	        return insertSQL;
	    }
   
	    protected void insertThrowable(IThrowableProxy tp, Connection connection, long eventId) throws SQLException {

	        PreparedStatement exceptionStatement = null;
	        try {
	            exceptionStatement = connection.prepareStatement(insertExceptionSQL);

	            short baseIndex = 0;
	            while (tp != null) {
	                baseIndex = buildExceptionStatement(tp, baseIndex, exceptionStatement, eventId);
	                tp = tp.getCause();
	            }

	            if (cnxSupportsBatchUpdates) {
	                exceptionStatement.executeBatch();
	            }
	        } finally {
	            closeStatement(exceptionStatement);
	        }

	    }

	    short buildExceptionStatement(IThrowableProxy tp, short baseIndex, PreparedStatement insertExceptionStatement, long eventId) throws SQLException {

	        StringBuilder buf = new StringBuilder();
	        ThrowableProxyUtil.subjoinFirstLine(buf, tp);
	        updateExceptionStatement(insertExceptionStatement, buf.toString(), baseIndex++, eventId);

	        int commonFrames = tp.getCommonFrames();
	        StackTraceElementProxy[] stepArray = tp.getStackTraceElementProxyArray();
	        for (int i = 0; i < stepArray.length - commonFrames; i++) {
	            StringBuilder sb = new StringBuilder();
	            sb.append(CoreConstants.TAB);
	            ThrowableProxyUtil.subjoinSTEP(sb, stepArray[i]);
	            updateExceptionStatement(insertExceptionStatement, sb.toString(), baseIndex++, eventId);
	        }

	        if (commonFrames > 0) {
	            StringBuilder sb = new StringBuilder();
	            sb.append(CoreConstants.TAB).append("... ").append(commonFrames).append(" common frames omitted");
	            updateExceptionStatement(insertExceptionStatement, sb.toString(), baseIndex++, eventId);
	        }

	        return baseIndex;
	    }
	    
	    void updateExceptionStatement(PreparedStatement exceptionStatement, String txt, short i, long eventId) throws SQLException {
	        exceptionStatement.setLong(1, eventId);
	        exceptionStatement.setShort(2, i);
	        exceptionStatement.setString(3, txt);
	        if (cnxSupportsBatchUpdates) {
	            exceptionStatement.addBatch();
	        } else {
	            exceptionStatement.execute();
	        }
	    }
}
