package org.glygen.array.client;

import java.io.File;
import java.util.Arrays;

import org.glygen.array.client.model.ProcessedResultConfiguration;
import org.glygen.array.client.model.User;
import org.glygen.array.client.model.data.ArrayDataset;
import org.glygen.array.client.model.data.StatisticalMethod;
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
        
        StatisticalMethod method = new StatisticalMethod();
        method.setName("Eliminate");
        method.setUri("http://purl.org/gadr/data/eliminate");
        
        String[] datasetFolders = dataFolder.list();
        for (String experimentName: datasetFolders) {
            File experimentFolder = new File (dataFolder + File.separator + experimentName);
            if (experimentFolder.isDirectory()) {
                String experimentFileName = experimentFolder.list()[0];   // there should be a single file
                String sampleName = experimentFileName.substring(0, experimentFileName.lastIndexOf(".xls"));
                Sample sample = new Sample();
                sample.setName(sampleName);
                sample.setTemplate("Protein Sample");
                String sampleId = addSample(url, userClient.getToken(), sample);
                sample.setId(sampleId);
                // need to upload the file first!!!
                try {
                    String dataSetID = addDataset(url, userClient.getToken(), "CFG 5.2", method, dataFolder.getName() + File.separator + experimentName + File.separator + experimentFileName, experimentName, sample);
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

    String addDataset(String url, String token, String fileFormat, StatisticalMethod method, String file, String experimentName, Sample sample) {
        //set the header with token
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        
        // first add the dataset
        ArrayDataset dataset = new ArrayDataset();
        dataset.setName(experimentName);
        dataset.setSample(sample);
        HttpEntity<ArrayDataset> requestEntity = new HttpEntity<ArrayDataset>(dataset, headers);
        url = url + "array/addDataset";
        System.out.println("URL: " + url);
        
        String datasetId = null;
        try {
            ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            datasetId = response.getBody();
        } catch (HttpClientErrorException e) {
            System.out.println ("Error adding the array dataset: " + e.getMessage());
            return null;
        }
        
        // then add the processed data
        
        HttpEntity<StatisticalMethod> requestEntity2 = new HttpEntity<StatisticalMethod>(method, headers);
        
        url = url + "array/addDatasetFromExcel?file=" + file + "&arraydatasetId=" + datasetId + "&fileFormat=" + fileFormat;
        System.out.println("URL: " + url);
        
        try {
            ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity2, String.class);
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
