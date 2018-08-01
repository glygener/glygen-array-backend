package org.glygen.array.logging.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class GlygenRequestAndResponseLoggingFilter extends OncePerRequestFilter  {

	Logger mylogger = LoggerFactory.getLogger("access-logger");
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (mylogger.isInfoEnabled()) {
            doLoggedFilterInternal(request, response, chain);
        } else {
            chain.doFilter(request, response);
        }
		
	}

	private void doLoggedFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		final ContentCachingRequestWrapper wrappedRequest = wrapRequest(request);
        final ContentCachingResponseWrapper wrappedResponse = wrapResponse(response);

        final boolean isLastExecution = !isAsyncStarted(request);

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
            wrappedResponse.copyBodyToResponse();
        } 
		 finally {
            if (isLastExecution) {
                logRequestAndResponse(wrappedRequest, wrappedResponse);
            }
        }
		
	}

	private void logRequestAndResponse(ContentCachingRequestWrapper request,
			ContentCachingResponseWrapper response) {
		
		StringBuilder RequestMsg = new StringBuilder();
        String uri = request.getRequestURI();
                
        RequestMsg.append("REQUEST:uri=").append(uri);
		
//		String payload = getRequestPayload(request);
//		if (payload != null) {
//			RequestMsg.append(";payload=").append(payload);
//		}
//		
//		StringBuilder ResponseMsg = new StringBuilder();
//		ResponseMsg.append("RESPONSE:payload=");
//		String responsePayload = getResponsePayload(response);
//		ResponseMsg.append(responsePayload);
		
        mylogger.info(RequestMsg.toString(),uri,request.getContentAsByteArray(),response.getContentAsByteArray());;
	}

	private ContentCachingRequestWrapper wrapRequest(final HttpServletRequest request) {

        final ContentCachingRequestWrapper wrappedRequest;
        if (request instanceof ContentCachingRequestWrapper) {
            wrappedRequest = (ContentCachingRequestWrapper) request;
        } else {
            wrappedRequest = new ContentCachingRequestWrapper(request);
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
	   
    @Nullable
	protected String getRequestPayload(ContentCachingRequestWrapper wrapper) {
		
		if (wrapper != null) {
			byte[] buf = wrapper.getContentAsByteArray();
			if (buf.length > 0) {
				int length = buf.length;
				try {
					return new String(buf, 0, length, wrapper.getCharacterEncoding());
				}
				catch (UnsupportedEncodingException ex) {
					return "[unknown]";
				}
			}
		}
		return null;
	}
    
    @Nullable
	protected String getResponsePayload(ContentCachingResponseWrapper wrapper) {
		
		if (wrapper != null) {
			byte[] buf = wrapper.getContentAsByteArray();
			if (buf.length > 0) {
				int length = buf.length;
				try {
					return new String(buf, 0, length, wrapper.getCharacterEncoding());
				}
				catch (UnsupportedEncodingException ex) {
					return "[unknown]";
				}
			}
		}
		return null;
	}
    
    
}
