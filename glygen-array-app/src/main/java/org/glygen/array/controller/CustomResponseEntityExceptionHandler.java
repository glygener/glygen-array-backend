package org.glygen.array.controller;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolation;

import org.glygen.array.exception.BindingNotFoundException;
import org.glygen.array.exception.EmailExistsException;
import org.glygen.array.exception.GlycanExistsException;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.LinkExpiredException;
import org.glygen.array.exception.UploadNotFinishedException;
import org.glygen.array.exception.UserNotFoundException;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.PropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailSendException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.xml.sax.SAXParseException;

import com.fasterxml.jackson.core.JsonParseException;


/**
 * This class handles exceptions and generates xml/json output for the client in case of exceptions
 *
 * @author sena
 *
 */
@ControllerAdvice
public class CustomResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

	final static Logger logger = LoggerFactory.getLogger("event-logger");
	
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        List<ObjectError> globalErrors = ex.getBindingResult().getGlobalErrors();
        List<ObjectError> errors = new ArrayList<ObjectError>(fieldErrors.size() + globalErrors.size());
        errors.addAll(fieldErrors);
        errors.addAll(globalErrors);
        ErrorMessage errorMessage = new ErrorMessage(errors);
        errorMessage.setStatus(status.value());
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        logger.error("Invalid Argument {}", errorMessage.toString());
        return new ResponseEntity<Object>(errorMessage, headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        String unsupported = "Unsupported content type: " + ex.getContentType();
        String supported = "Supported content types: " + MediaType.toString(ex.getSupportedMediaTypes());
        FieldError error = new FieldError(unsupported, supported, "Unsupported media type");
        ErrorMessage errorMessage = new ErrorMessage(error);
        errorMessage.setStatus(status.value());
        errorMessage.setErrorCode(ErrorCodes.UNSUPPORTED_MEDIATYPE);
        logger.error("MediaType Problem: {}", errorMessage.toString());
        return new ResponseEntity<Object>(errorMessage, headers, status);
    }
    
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        String unsupported = "Unsupported accept type: " + headers.getAccept();
        String supported = "Supported accept types: " + MediaType.toString(ex.getSupportedMediaTypes());
        FieldError error = new FieldError(unsupported, supported, "Unsupported accept media type");
        ErrorMessage errorMessage = new ErrorMessage(error);
        errorMessage.setStatus(status.value());
        errorMessage.setErrorCode(ErrorCodes.UNSUPPORTED_MEDIATYPE);
        logger.error("Accept MediaType Problem: {}", errorMessage.toString());
        return new ResponseEntity<Object>(errorMessage, headers, status);
    }
    
    @Override
    protected ResponseEntity<Object> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex,
            HttpHeaders headers, HttpStatus status, WebRequest webRequest) {
        ErrorMessage errorMessage = new ErrorMessage("Timed out. But it is still processing. Check back the results later!");
        errorMessage.setStatus(HttpStatus.REQUEST_TIMEOUT.value());
        errorMessage.setErrorCode(ErrorCodes.EXPIRED);
        logger.error("Asnychronous method timed out.", errorMessage.toString());
        return handleExceptionInternal(ex, errorMessage, headers, HttpStatus.REQUEST_TIMEOUT, webRequest);
    }
    

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        Throwable mostSpecificCause = ex.getMostSpecificCause();
        ErrorMessage errorMessage;
        if (mostSpecificCause != null) {     	
            String message = mostSpecificCause.getMessage();
            if (message != null)
            	errorMessage = new ErrorMessage(message);
            else
            	errorMessage = new ErrorMessage("Message not readable");
            if (mostSpecificCause.getClass().getPackage().getName().startsWith("com.fasterxml.jackson"))
            	errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT_JSON);
            else if (mostSpecificCause instanceof JsonParseException) {
        		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT_JSON);
        	}
            else if (mostSpecificCause instanceof SAXParseException) {
            	errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT_XML);
            }
            else {
            	errorMessage.setErrorCode(ErrorCodes.INVALID_URL);
            }
        } else {
            errorMessage = new ErrorMessage(ex.getMessage());
            errorMessage.setErrorCode(ErrorCodes.INVALID_URL);
        }
        errorMessage.setStatus(status.value());  
        logger.error("Message not readable:", ex);
        return new ResponseEntity<Object>(errorMessage, headers, status);
    }
    
    @ExceptionHandler(value={
    		ObjectNotFoundException.class,
    		EntityNotFoundException.class,
    		UploadNotFinishedException.class,
    		EntityExistsException.class,
    		DataIntegrityViolationException.class,
    		ConstraintViolationException.class,
    		LinkExpiredException.class,
    		EmailExistsException.class,
    		UserNotFoundException.class,
    		BindingNotFoundException.class,
    		GlycanRepositoryException.class,
    		GlycanExistsException.class,
    	//	GlycanNotFoundException.class,
    	//	MotifNotFoundException.class,
    	//	GlycanExistsException.class,
    		MailSendException.class,
    		IllegalArgumentException.class, 
    		UnsupportedEncodingException.class,
    		CompletionException.class,
    	//	GlycoVisitorException.class,
    	//	SugarImporterException.class,
    	//	SearchEngineException.class,
    	//	UserRoleViolationException.class,
    		javax.validation.ConstraintViolationException.class,
    		SAXParseException.class,
    		JsonParseException.class})
    public ResponseEntity<Object> handleCustomException(Exception ex, WebRequest request) {
    	HttpHeaders headers = new HttpHeaders();
        HttpStatus status;
        ErrorMessage errorMessage = null;
        
        if (ex instanceof ObjectNotFoundException || ex instanceof UserNotFoundException || ex instanceof BindingNotFoundException) {
        	//	|| ex instanceof MotifNotFoundException) {
        	 status = HttpStatus.NOT_FOUND;
        	
        	if (ex.getCause() != null && ex.getCause() instanceof ErrorMessage) {
    			errorMessage = (ErrorMessage) ex.getCause();
    			errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
    		} else {
    			errorMessage = new ErrorMessage (ex.getMessage());
    			errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
    		}
        } else if (ex instanceof UploadNotFinishedException) {
        	status = HttpStatus.PARTIAL_CONTENT;
        	errorMessage = new ErrorMessage (ex.getMessage());
            errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
        } else if (ex instanceof EntityNotFoundException) {
            status = HttpStatus.NOT_FOUND;
            errorMessage = new ErrorMessage (ex.getMessage());
            errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
        } else if (ex instanceof EntityExistsException || ex instanceof GlycanExistsException) {
            status = HttpStatus.CONFLICT;
            if (ex.getCause() != null && ex.getCause() instanceof ErrorMessage) {
    			errorMessage = (ErrorMessage) ex.getCause();
    			errorMessage.setErrorCode(ErrorCodes.DUPLICATE);
    		} else {
    			errorMessage = new ErrorMessage (ex.getMessage());
    			errorMessage.setErrorCode(ErrorCodes.DUPLICATE);
    		}
        } else if (ex instanceof EmailExistsException ) {
            status = HttpStatus.CONFLICT;
            //errorMessage = new ErrorMessage (ex.getMessage());
            errorMessage = (ErrorMessage) ex.getCause();
            errorMessage.setErrorCode(ErrorCodes.DUPLICATE);
//            errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
        } else if (ex instanceof IllegalArgumentException || (ex instanceof CompletionException && ex.getCause() !=null && ex.getCause() instanceof IllegalArgumentException)) { 
        	status = HttpStatus.BAD_REQUEST;
        	ErrorCodes code;
        	if (ex instanceof CompletionException) 
        	    ex = (Exception) ex.getCause();
        	if (ex instanceof IllegalArgumentException) {
        		// need to extract what kind of problem occurred and set the error code accordingly
        		String err = ((IllegalArgumentException)ex).getMessage();
        		if (err.toLowerCase().contains("invalid input")) {
        			code = ErrorCodes.INVALID_INPUT;
        		}
        		else if (err.toLowerCase().contains("parse") || err.toLowerCase().contains("parsing")) {
        			code = ErrorCodes.PARSE_ERROR;
        		} else if (err.toLowerCase().contains("valid")) {
        			code = ErrorCodes.INVALID_STRUCTURE;
        		} else if (err.toLowerCase().contains("export")) {
        			code = ErrorCodes.INTERNAL_ERROR;
        		} else if (err.toLowerCase().contains("support")) {
        			code = ErrorCodes.UNSUPPORTED_ENCODING;
        		} else if (err.toLowerCase().contains("expired")) {
        			code = ErrorCodes.EXPIRED;
        		} else {
        			code = ErrorCodes.INVALID_INPUT;
        		}
        		if (ex.getCause() != null && ex.getCause() instanceof ErrorMessage) {
        			errorMessage = (ErrorMessage) ex.getCause();
        			errorMessage.setErrorCode(((ErrorMessage) ex.getCause()).getErrorCode());
        			try {
        			    status = HttpStatus.valueOf(((ErrorMessage) ex.getCause()).getStatusCode());
        			} catch (Exception e) {
        			    // ignore
        			}
        		} else {
        			errorMessage = new ErrorMessage (ex.getMessage());
        			errorMessage.setErrorCode(code);
        		}
        	}
       /* 	else if (ex instanceof SugarImporterException) {
        		errorMessage = new ErrorMessage(((SugarImporterException)ex).getErrorText() + ":" +  ((SugarImporterException)ex).getPosition());
        		errorMessage.setErrorCode(ErrorCodes.PARSE_ERROR);
        	} else if (ex instanceof GlycoVisitorException) {
        		errorMessage = new ErrorMessage (ex.getMessage());
	        	errorMessage.setErrorCode(ErrorCodes.INVALID_STRUCTURE);
        	}*/
        	else {
	            logger.info("Bad Request {}", ex.getMessage());
	        	errorMessage = new ErrorMessage (ex.getMessage());
	        	errorMessage.setErrorCode(ErrorCodes.NOT_ALLOWED);
        	}
        } else if (ex instanceof DataIntegrityViolationException) {
        	Throwable cause = ((DataIntegrityViolationException)ex).getCause();
        	if (cause != null) {
        		List<ObjectError> errors = new ArrayList<ObjectError>();
        		if (cause instanceof ConstraintViolationException) {
        			Throwable mostSpecificCause = ((ConstraintViolationException)cause).getCause();
                    if (mostSpecificCause != null) {
                       // String exceptionName = mostSpecificCause.getClass().getName();
                        String message = mostSpecificCause.getMessage();
                        errorMessage = new ErrorMessage(message);
                       // errors.add(exceptionName);
                       // errors.add(message);
                    } else {
                    	errorMessage = new ErrorMessage(ex.getMessage());
                    }
                    errors.add(new ObjectError (((ConstraintViolationException)cause).getConstraintName(), "ConstraintViolated"));
                    status = HttpStatus.CONFLICT;
        		} else if (cause instanceof PropertyValueException) {
        			Throwable mostSpecificCause = ((PropertyValueException)cause).getCause();
                    if (mostSpecificCause != null) {
                     //   String exceptionName = mostSpecificCause.getClass().getName();
                        String message = mostSpecificCause.getMessage();
                     //   errors.add(exceptionName);
                        errorMessage = new ErrorMessage(message);
                    } else {
                    	errorMessage = new ErrorMessage(ex.getMessage());
                    }
                    errors.add(new FieldError (((PropertyValueException)ex).getEntityName(), ((PropertyValueException)ex).getPropertyName(), "PropertyValueError"));
        		} else {
		        	Throwable mostSpecificCause = ((DataIntegrityViolationException)ex).getMostSpecificCause();
		            if (mostSpecificCause != null) {
		             //   String exceptionName = mostSpecificCause.getClass().getName();
		                String message = mostSpecificCause.getMessage();
		                errorMessage = new ErrorMessage(message);
		            } else {
		                errorMessage = new ErrorMessage(ex.getMessage());
		            }
        		}
        	} else { // only put the message
        		errorMessage = new ErrorMessage(ex.getMessage());
        	}
        	errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            status = HttpStatus.CONFLICT;
        } else if (ex instanceof ConstraintViolationException) {
        	Throwable mostSpecificCause = ((ConstraintViolationException)ex).getCause();
        	
            if (mostSpecificCause != null) { 
                if (mostSpecificCause instanceof PSQLException) {
                	String detail = ((PSQLException)mostSpecificCause).getServerErrorMessage().getDetail();
                	errorMessage = new ErrorMessage(detail);
                	
                }
                else {
                	//String exceptionName = mostSpecificCause.getClass().getName();
                    String message = mostSpecificCause.getMessage();
                    errorMessage = new ErrorMessage(message);
                }
                
            } else {
            	errorMessage = new ErrorMessage(ex.getMessage());
            }
            errorMessage = new ErrorMessage(new ObjectError (((ConstraintViolationException)ex).getConstraintName(), "ConstraintViolated"));
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            status = HttpStatus.CONFLICT;
        } else if (ex instanceof javax.validation.ConstraintViolationException) {
        	Set<javax.validation.ConstraintViolation<?>> constaintViolations = ((javax.validation.ConstraintViolationException)ex).getConstraintViolations();
        	List<ObjectError> errors = new ArrayList<ObjectError>(constaintViolations.size());
        	for (Iterator iterator = constaintViolations.iterator(); iterator
					.hasNext();) {
				ConstraintViolation<?> constraintViolation = (ConstraintViolation<?>) iterator
						.next();
				ObjectError error = new ObjectError(constraintViolation.getPropertyPath().toString(), "ConstraintViolated");
				errors.add(error);
			}
        	errorMessage = new ErrorMessage(errors);
        	errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        	status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof SAXParseException) {
        	Throwable mostSpecificCause = ((SAXParseException)ex).getCause();
            if (mostSpecificCause != null) {
            	status = HttpStatus.BAD_REQUEST;
                errorMessage = new ErrorMessage (mostSpecificCause.getMessage());
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT_XML);
            }
            else {
            	status = HttpStatus.BAD_REQUEST;
                errorMessage = new ErrorMessage (ex.getMessage());
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT_XML);
            }
        } else if (ex instanceof JsonParseException) {
        	Throwable mostSpecificCause = ((SAXParseException)ex).getCause();
            if (mostSpecificCause != null) {
            	status = HttpStatus.BAD_REQUEST;
                errorMessage = new ErrorMessage (mostSpecificCause.getMessage());
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT_JSON);
            }
            else {
            	status = HttpStatus.BAD_REQUEST;
                errorMessage = new ErrorMessage (ex.getMessage());
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT_JSON);
            }
        } else if (ex instanceof UnsupportedEncodingException) {
        	status = HttpStatus.BAD_REQUEST;
        	errorMessage = new ErrorMessage (ex.getMessage());
        	errorMessage.setErrorCode(ErrorCodes.UNSUPPORTED_ENCODING);
        }
        else if (ex instanceof LinkExpiredException) {
        	status = HttpStatus.BAD_REQUEST;
        	errorMessage = new ErrorMessage (ex.getMessage());
        	errorMessage.setErrorCode(ErrorCodes.EXPIRED);
        } else {
            logger.warn("Unknown exception type: " + ex.getClass().getName());
            logger.error("Internal Server Error.", ex);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            ErrorMessage err = new ErrorMessage (ex.getClass().getName() + ": " + ex.getMessage());
            err.setStatus(status.value());
            err.setErrorCode(ErrorCodes.INTERNAL_ERROR);
            return handleExceptionInternal(ex, err, headers, status, request);
        }

        errorMessage.setStatus(status.value());
        logger.error("Error: {}", errorMessage.toString()); // reverting back to only summarizing the error instead of showing the stack trace
        return handleExceptionInternal(ex, errorMessage, headers, status, request);
    }
    
}
