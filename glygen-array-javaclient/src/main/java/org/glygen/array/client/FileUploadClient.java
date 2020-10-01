package org.glygen.array.client;

import org.glygen.array.client.exception.CustomClientException;
import org.glygen.array.client.model.data.FileWrapper;

public interface FileUploadClient {
    /**
     * uploads the given file to the server
     * @param filePath full path of the file to be uploaded
     * @return the FileWrapper of the uploaded file at the server, 
     * the file should be accessed with this object in other web service calls
     * @throws CustomClientException if upload fails
     */
    FileWrapper uploadFile (String filePath) throws CustomClientException;
    
    /**
     * sets the user name of the user
     * @param username name of the user
     */
    void setUsername(String username);
    
    /**
     * sets the password to be used for the user
     * @param password password of the user
     */
    void setPassword(String password);
    
    /**
     * sets the url for the web services, e.g https://glygen.ccrc.uga.edu/ggarray/api/
     * @param url url to be used to access the web services (with the trailing /)
     */
    void setURL (String url);
}
