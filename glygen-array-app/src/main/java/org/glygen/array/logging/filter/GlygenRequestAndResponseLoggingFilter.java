package org.glygen.array.logging.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

public class GlygenRequestAndResponseLoggingFilter extends OncePerRequestFilter  {
    Logger log = LoggerFactory.getLogger(GlygenRequestAndResponseLoggingFilter.class);
	Logger logger = LoggerFactory.getLogger("access-logger");
	private static final int DEFAULT_MAX_PAYLOAD_LENGTH = 10000;
	
	private int maxPayloadLength = DEFAULT_MAX_PAYLOAD_LENGTH;
	
	 public int getMaxPayloadLength() {
			return maxPayloadLength;
		}
	    
	    public void setMaxPayloadLength(int maxPayloadLength) {
			this.maxPayloadLength = maxPayloadLength;
		}
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (logger.isInfoEnabled()) {
            doLoggedFilterInternal(request, response, chain);
        } else {
            chain.doFilter(request, response);
        }
		
	}

	private void doLoggedFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		final ContentCachingRequestWrapper wrappedRequest = wrapRequest(request);
        final ContentCachingResponseWrapper wrappedResponse = wrapResponse(response);

        final boolean isLastExecution = !isAsyncStarted(request);

        String requestData=null;
        String responseData = null;
        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
            requestData = getRequestData(wrappedRequest);
            if (!wrappedRequest.getRequestURI().contains("download"))   // file download response should not be logged, it might be too large!
                responseData = getResponseData(wrappedResponse);
        } 
		 finally {
            if (isLastExecution) {
                logRequestAndResponse(wrappedRequest, wrappedResponse, requestData, responseData);
            }
        }
	}

	private void logRequestAndResponse(ContentCachingRequestWrapper request,
			ContentCachingResponseWrapper response, String requestData, String responseData) {
		
		StringBuilder RequestMsg = new StringBuilder();
        String uri = request.getRequestURI();
                
        RequestMsg.append("REQUEST:uri=").append(uri);
        try {
        	logger.info(RequestMsg.toString(),uri,requestData,responseData);
        } catch (Exception e) {
        	log.error("Error logging to database: ", e);
        }
        
	}

	private ContentCachingRequestWrapper wrapRequest(final HttpServletRequest request) {

        final ContentCachingRequestWrapper wrappedRequest;
        if (request instanceof ContentCachingRequestWrapper) {
            wrappedRequest = (ContentCachingRequestWrapper) request;
        } else {
            wrappedRequest = new ContentCachingRequestWrapper(request, getMaxPayloadLength());
        }
        return wrappedRequest;
    }

	private ContentCachingResponseWrapper wrapResponse(final HttpServletResponse response) {

        final ContentCachingResponseWrapper wrappedResponse;
        if (response instanceof ContentCachingResponseWrapper) {
            wrappedResponse = (ContentCachingResponseWrapper) response;
        } else {
            wrappedResponse = new ContentCachingResponseWrapper(response);
        }
        return wrappedResponse;
    }
    
    public static String getRequestData(final HttpServletRequest request) throws UnsupportedEncodingException {
        String payload = null;
        ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
        if (wrapper != null) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                payload = new String(buf, 0, buf.length, wrapper.getCharacterEncoding());
            }
        }
        return payload;
    }
    
    public static String getResponseData(final HttpServletResponse response) throws IOException {
        String payload = null;
        ContentCachingResponseWrapper wrapper =
            WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        if (wrapper != null) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                payload = new String(buf, 0, buf.length, wrapper.getCharacterEncoding());
                wrapper.copyBodyToResponse();
            }
        }
        return payload;
    }
}
