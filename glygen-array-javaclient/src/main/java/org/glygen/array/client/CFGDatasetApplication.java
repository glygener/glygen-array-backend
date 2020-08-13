package org.glygen.array.client;

import java.io.File;
import java.util.Arrays;

import org.glygen.array.client.exception.CustomClientException;
import org.glygen.array.client.model.Glycan;
import org.glygen.array.client.model.LoginRequest;
import org.glygen.array.client.model.ProcessedResultConfiguration;
import org.glygen.array.client.model.User;
import org.glygen.array.client.model.metadata.Sample;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import ch.qos.logback.classic.Logger;

@SpringBootApplication
@Configuration
public class CFGDatasetApplication implements CommandLineRunner {

    private static final Logger log = (Logger) LoggerFactory.getLogger(Application.class);
    
    private RestTemplate restTemplate = new RestTemplate();
    
    @Bean
    @ConfigurationProperties("glygen")
    public GlygenSettings glygen() {
        return new GlygenSettings();
    }

    public static void main(String args[]) {
        new SpringApplicationBuilder(CFGDatasetApplication.class).run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        GlygenSettings settings = glygen();
        UserRestClient userClient = new UserRestClientImpl();
        userClient.setURL(settings.scheme + settings.host + settings.basePath);
        if (args == null || args.length < 3) {
            log.error("need to pass username and password and processed file folder as arguments");
            return;
        }
        userClient.login(args[0], args[1]);
        User user = userClient.getUser(args[0]);
        log.info("got user information:" + user.getEmail());
        // read all folders in the given folder
        File dataFolder = new File (args[2]);
        if (!dataFolder.isDirectory()) {
            log.error(args[2] + " is not a folder");
            return;
        }
        
        String url = settings.scheme + settings.host + settings.basePath;
        ProcessedResultConfiguration config = new ProcessedResultConfiguration();
        config.setCvColumnId(32);
        config.setFeatureColumnId(29);
        config.setResultFileType("cfg");
        config.setRfuColumnId(30);
        config.setSheetNumber(0);
        config.setStDevColumnId(31);
        config.setStartRow(1);
        
        
        String[] datasetFolders = dataFolder.list();
        for (String experimentName: datasetFolders) {
            File experimentFolder = new File (dataFolder + File.separator + experimentName);
            if (experimentFolder.isDirectory()) {
                String experimentFileName = experimentFolder.list()[0];   // there should be a single file
                String sampleName = experimentFileName.substring(0, experimentFileName.lastIndexOf(".xls"));
                Sample sample = new Sample();
                sample.setName(sampleName);
                sample.setTemplate("Protein Sample");
                addSample(url, userClient.getToken(), sample);
                // need to upload the file first!!!
                try {
                    String dataSetID = addDataset(url, userClient.getToken(), config, dataFolder.getName() + File.separator + experimentName + File.separator + experimentFileName, experimentName, sampleName);
                    log.debug("Added dataset " + dataSetID + " for " + experimentName);
                } catch (Exception e) {
                    System.out.println("Failed: " + dataFolder.getName() + File.separator + experimentName + File.separator + experimentFileName);
                }
            }
        }
    }
        
    private String addSample(String url, String token, Sample sample) {
        //set the header with token
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        HttpEntity<Sample> requestEntity = new HttpEntity<Sample>(sample, headers);
        url = url + "array/addSampleNoValidation?validate=false";
        System.out.println("URL: " + url);
        
        try {
            ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.out.println ("Error processing the file: " + e.getMessage());
            return null;
        }
    }

    String addDataset(String url, String token, ProcessedResultConfiguration config, String file, String experimentName, String sampleName) {
        //set the header with token
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        HttpEntity<ProcessedResultConfiguration> requestEntity = new HttpEntity<ProcessedResultConfiguration>(config, headers);
        url = url + "array/addDatasetFromExcel?file=" + file + "&name=" + experimentName + "&sampleName=" + sampleName;
        System.out.println("URL: " + url);
        
        try {
            ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.out.println ("Error processing the file: " + e.getMessage());
            return null;
        }
    }
    
    public class GlygenSettings {
        String host;
        String scheme;
        String basePath;
        /**
         * @return the host
         */
        public String getHost() {
            return host;
        }
        /**
         * @param host the host to set
         */
        public void setHost(String host) {
            this.host = host;
        }
        /**
         * @return the scheme
         */
        public String getScheme() {
            return scheme;
        }
        /**
         * @param scheme the scheme to set
         */
        public void setScheme(String scheme) {
            this.scheme = scheme;
        }
        /**
         * @return the basePath
         */
        public String getBasePath() {
            return basePath;
        }
        /**
         * @param basePath the basePath to set
         */
        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }
    }
}
