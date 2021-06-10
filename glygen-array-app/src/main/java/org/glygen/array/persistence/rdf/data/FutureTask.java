package org.glygen.array.persistence.rdf.data;

import java.util.Date;

import org.glygen.array.view.ErrorMessage;

public class FutureTask {
    
    FutureTaskStatus status = FutureTaskStatus.DONE;
    ErrorMessage error;
    Date startDate;
    
    /**
     * @return the error
     */
    public ErrorMessage getError() {
        return error;
    }
    /**
     * @param error the error to set
     */
    public void setError(ErrorMessage error) {
        this.error = error;
    }
    /**
     * @return the status
     */
    public FutureTaskStatus getStatus() {
        return status;
    }
    /**
     * @param status the status to set
     */
    public void setStatus(FutureTaskStatus status) {
        this.status = status;
    }
    /**
     * @return the startDate
     */
    public Date getStartDate() {
        return startDate;
    }
    /**
     * @param startDate the startDate to set
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

}
