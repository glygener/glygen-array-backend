package org.glygen.array.client.model;

public enum ErrorCodes {

	INTERNAL_ERROR(5000, "InternalError"),
	INVALID_URL(4001, "InvalidURL"),
	INVALID_INPUT (4002, "InvalidInput"),
	INVALID_INPUT_XML (4003, "InvalidInputXML"),
	INVALID_INPUT_JSON (4004, "InvalidInputJSON"),
	INVALID_STRUCTURE(4005, "InvalidStructure"),
	NOT_ALLOWED (4006, "NotAllowed"),
	UNSUPPORTED_MEDIATYPE (4007, "Unsupported"),
	UNSUPPORTED_ENCODING (4008, "Unsupported"),
	PARSE_ERROR (4009, "ParseError"),
	NOT_FOUND (4040, "NotFound"),
	UNAUTHORIZED(4010, "Unauthorized"), 
	ACCESS_DENIED (4030, "AccessDenied"),
	EXPIRED (4050, "Expired");
	
	private Integer code;
	private String error;
	
	private ErrorCodes( Integer c, String error)
    {
    	this.code =c;
    	this.error = error;
    }
	
	public static String forCode( Integer a_code ) throws Exception
    {
        for ( ErrorCodes e : ErrorCodes.values() )
        {
            if ( a_code == e.code ) 
            {
                return e.error;
            }
        }
        throw new Exception("Invalid code for ErrorCodes");
    }
}
