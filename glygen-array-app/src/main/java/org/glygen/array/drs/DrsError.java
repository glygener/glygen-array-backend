package org.glygen.array.drs;

public class DrsError {
    
    String msg;
    Integer status_code;
    
    public DrsError(String message, int statusCode) {
        this.msg = message;
        this.status_code = statusCode;
    }
    
    /**
     * @return the msg
     */
    public String getMsg() {
        return msg;
    }
    /**
     * @param msg the msg to set
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }
    /**
     * @return the status_code
     */
    public Integer getStatus_code() {
        return status_code;
    }
    /**
     * @param status_code the status_code to set
     */
    public void setStatus_code(Integer status_code) {
        this.status_code = status_code;
    }
    
    

}
