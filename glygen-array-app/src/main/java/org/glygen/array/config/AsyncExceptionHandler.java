package org.glygen.array.config;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.stereotype.Component;

@Component
public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        logger.error("Asynchronous Exception message - " + ex.getMessage());
        logger.info("Method name - " + method.getName());
        for (Object param : params) {
            logger.info("Parameter value - " + param);
        }
    }

}
