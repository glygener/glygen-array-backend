package org.glygen.array.client.exception;

import org.springframework.http.HttpStatus;

@SuppressWarnings("serial")
public class CustomClientException extends RuntimeException{
	private HttpStatus statusCode;

    private String body;

    public CustomClientException(String msg) {
        super(msg);
    }

    public CustomClientException(HttpStatus statusCode, String body, String msg) {
        super(msg);
        this.statusCode = statusCode;
        this.body = body;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(HttpStatus statusCode) {
        this.statusCode = statusCode;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

}
