package org.glygen.array.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.glygen.array.client.exception.CustomClientException;
import org.glygen.array.client.model.LoginRequest;
import org.glygen.array.client.model.UploadResult;
import org.glygen.array.client.model.data.FileWrapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class FileUploadClientImpl implements FileUploadClient {
    
    private RestTemplate restTemplate = new RestTemplate();

    String token=null;
    String username;
    String password;
    String url = "http://localhost:8080/";
    
    /**
     * {@inheritDoc}
     */
    @Override
    public FileWrapper uploadFile(String filePath) throws CustomClientException {
        if (token == null) login(this.username, this.password);
        
        File file = new File (filePath); 
        if (file.exists()) {
            int sizeOfFiles = 1024 * 1024;// 1MB
            byte[] buffer = new byte[sizeOfFiles];

            String fileName = file.getName();
            String identifier = System.currentTimeMillis() + "-" + fileName.replaceAll("[^0-9a-zA-Z_-]", "");
            
            int chunkNumber = 1;
            int totalChunks = (int) Math.ceil(((double)file.length()) / ((double)sizeOfFiles));
            
            FileWrapper resultFile = null;

            //try-with-resources to ensure closing stream
            try (FileInputStream fis = new FileInputStream(file);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                UploadResult result = null;
                int bytesRead = 0;
                while ((bytesRead = bis.read(buffer)) > 0) {
                    byte[] actual = new byte[bytesRead];
                    for (int i=0; i < bytesRead; i++) {
                        actual[i] = buffer[i];
                    }
                    result = uploadChunk (fileName, actual, chunkNumber, totalChunks, sizeOfFiles, file.length(), identifier);
                    chunkNumber++;         
                }
                if (result.getStatusCode() == HttpStatus.OK.value()) {
                    // finished uploading
                    resultFile = result.getFile();
                }
                return resultFile;
            } catch (IOException e) {
                throw new CustomClientException ("The file cannot be uploaded: " +  e.getMessage());
            }
        } else {
            throw new CustomClientException ("The file to be uploaded cannot be found: " + filePath);
        }
    }
    
    UploadResult uploadChunk (String filename, byte[] buffer, int chunkNumber, int totalChunks, long chunkSize, long totalSize, String identifier) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        HttpEntity<byte[]> requestEntity = new HttpEntity<>(buffer, headers);
        String url = this.url + "array/upload";
        url += "?resumableFilename=" + filename;
        url += "&resumableRelativePath=" + "";
        url += "&resumableTotalChunks=" + totalChunks;
        url += "&resumableChunkSize=" + chunkSize;
        url += "&resumableChunkNumber=" + chunkNumber;
        url += "&resumableTotalSize=" + totalSize;
        url += "&resumableIdentifier=" + identifier;
        
        try {
            ResponseEntity<UploadResult> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, UploadResult.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new CustomClientException (e.getStatusCode(), e.getResponseBodyAsString(), e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUsername(String username) {
       this.username = username;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setURL(String url) {
        this.url = url;
    }
    
    public void login(String username, String password) throws CustomClientException {
        // login to the system and set the token
        this.username = username;
        this.password = password;
        String url = this.url + "login";
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);
        HttpEntity<LoginRequest> requestEntity = new HttpEntity<LoginRequest>(loginRequest);
        HttpEntity<Void> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, Void.class);
        HttpHeaders header = response.getHeaders();
        this.token = header.getFirst("Authorization");
    }
    
    public static void main(String[] args) {
        FileUploadClient fileUploadTest = new FileUploadClientImpl();
        fileUploadTest.setURL("http://localhost:8080/");
        fileUploadTest.setUsername(args[0]);
        fileUploadTest.setPassword(args[1]);
        System.out.println(fileUploadTest.uploadFile("/Users/sena/Desktop/icl_lib.xml"));
    }

}
