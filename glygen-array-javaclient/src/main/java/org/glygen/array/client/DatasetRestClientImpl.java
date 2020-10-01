package org.glygen.array.client;

import java.util.Arrays;

import org.glygen.array.client.exception.CustomClientException;
import org.glygen.array.client.model.ArrayDatasetListView;
import org.glygen.array.client.model.LoginRequest;
import org.glygen.array.client.model.MetadataListResultView;
import org.glygen.array.client.model.data.ArrayDataset;
import org.glygen.array.client.model.data.FileWrapper;
import org.glygen.array.client.model.data.PrintedSlide;
import org.glygen.array.client.model.data.RawData;
import org.glygen.array.client.model.metadata.AssayMetadata;
import org.glygen.array.client.model.metadata.DataProcessingSoftware;
import org.glygen.array.client.model.metadata.ImageAnalysisSoftware;
import org.glygen.array.client.model.metadata.MetadataCategory;
import org.glygen.array.client.model.metadata.Printer;
import org.glygen.array.client.model.metadata.Sample;
import org.glygen.array.client.model.metadata.ScannerMetadata;
import org.glygen.array.client.model.metadata.SlideMetadata;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


public class DatasetRestClientImpl implements DatasetRestClient {
    
    private RestTemplate restTemplate = new RestTemplate();

    String token=null;
    String username;
    String password;
    String url = "http://localhost:8080/";

    @Override
    public void setUsername(String username) {
        this.username = username;

    }

    @Override
    public void setPassword(String password) {
       this.password = password;
    }

    @Override
    public void setURL(String url) {
       this.url = url;
    }

    @Override
    public String addDataset(String experimentName, Sample sample) {
        if (token == null) login (this.username, this.password);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        
        // first add the dataset
        ArrayDataset dataset = new ArrayDataset();
        dataset.setName(experimentName);
        dataset.setSample(sample);
        HttpEntity<ArrayDataset> requestEntity = new HttpEntity<ArrayDataset>(dataset, headers);
        String url = this.url + "array/addDataset";
        
        String datasetId = null;
        try {
            ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            datasetId = response.getBody();
        } catch (HttpClientErrorException e) {
            throw new CustomClientException(e.getStatusCode(), e.getResponseBodyAsString(), e.getMessage());
        }
        return datasetId;
    }

    @Override
    public String addProcessedDataFromExcel(FileWrapper file, String statisticalMethod, String datasetId,
            String metadataId) {
        if (token == null) login (this.username, this.password);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        
        HttpEntity<FileWrapper> requestEntity2 = new HttpEntity<FileWrapper>(file, headers);
        
        String url = this.url + "array/addDatasetFromExcel?file=" + file + 
                "&arraydatasetId=" + datasetId + "&methodName=" + statisticalMethod + "&metadataId=" + metadataId;
        
        try {
            ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity2, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new CustomClientException(e.getStatusCode(), e.getResponseBodyAsString(), e.getMessage());
        }
    }

    @Override
    public String addRawdataToDataset(RawData rawData, String datasetId) {
        if (token == null) login (this.username, this.password);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        
        HttpEntity<RawData> requestEntity = new HttpEntity<RawData>(rawData, headers);
        String url = this.url + "array/addRawData?=arraydatasetId=" + datasetId;
        
        try {
            ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new CustomClientException(e.getStatusCode(), e.getResponseBodyAsString(), e.getMessage());
        }
    }

    @Override
    public String addSample(Sample sample, boolean validate) {
        if (token == null) login (this.username, this.password);
        //set the header with token
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        HttpEntity<Sample> requestEntity = new HttpEntity<Sample>(sample, headers);
        String url = this.url + "array/addSample?validate=" + validate;
        
        try {
            ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new CustomClientException(e.getStatusCode(), e.getResponseBodyAsString(), e.getMessage());
        }
    }
    
    @Override
    public String addSample(Sample sample) {
        return addSample(sample, true);
    }

    @Override
    public String addPrinterMetadata(Printer printer) {
        return addPrinterMetadata(printer, true);
    }

    @Override
    public String addScannerMetadata(ScannerMetadata scanner) {
        return addScannerMetadata(scanner, true);
    }

    @Override
    public String addSlideMetadata(SlideMetadata slideMetadata) {
        return addSlideMetadata(slideMetadata, true);
    }

    @Override
    public String addImageAnalysisMetadata(ImageAnalysisSoftware metadata) {
        return addImageAnalysisMetadata(metadata, true);
    }

    @Override
    public String addDataProcessingMetadata(DataProcessingSoftware metadata) {
        return addDataProcessingMetadata(metadata, true);
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

    @Override
    public String addPrinterMetadata(Printer printer, boolean validate) {
        if (token == null) login (this.username, this.password);
        //set the header with token
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        HttpEntity<Printer> requestEntity = new HttpEntity<Printer>(printer, headers);
        String url = this.url + "array/addPrinter?validate=" + validate; 
        
        try {
            ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new CustomClientException(e.getStatusCode(), e.getResponseBodyAsString(), e.getMessage());
        }
    }

    @Override
    public String addScannerMetadata(ScannerMetadata scanner, boolean validate) {
        if (token == null) login (this.username, this.password);
        //set the header with token
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        HttpEntity<ScannerMetadata> requestEntity = new HttpEntity<ScannerMetadata>(scanner, headers);
        String url = this.url + "array/addScanner?validate=" + validate;  
        
        try {
            ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new CustomClientException(e.getStatusCode(), e.getResponseBodyAsString(), e.getMessage());
        }
    }

    @Override
    public String addSlideMetadata(SlideMetadata slideMetadata, boolean validate) {
        if (token == null) login (this.username, this.password);
        //set the header with token
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        HttpEntity<SlideMetadata> requestEntity = new HttpEntity<SlideMetadata>(slideMetadata, headers);
        String url = this.url + "array/addSlideMetadata?validate=" + validate;  
        
        try {
            ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new CustomClientException(e.getStatusCode(), e.getResponseBodyAsString(), e.getMessage());
        }
    }

    @Override
    public String addImageAnalysisMetadata(ImageAnalysisSoftware metadata, boolean validate) {
        if (token == null) login (this.username, this.password);
        //set the header with token
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        HttpEntity<ImageAnalysisSoftware> requestEntity = new HttpEntity<ImageAnalysisSoftware>(metadata, headers);
        String url = this.url + "array/addImageAnalysis?validate=" + validate;  
        
        try {
            ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new CustomClientException(e.getStatusCode(), e.getResponseBodyAsString(), e.getMessage());
        }
    }

    @Override
    public String addDataProcessingMetadata(DataProcessingSoftware metadata, boolean validate) {
        if (token == null) login (this.username, this.password);
        //set the header with token
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        HttpEntity<DataProcessingSoftware> requestEntity = new HttpEntity<DataProcessingSoftware>(metadata, headers);
        String url = this.url + "array/addDataProcessingSoftware?validate=" + validate; 
        
        try {
            ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new CustomClientException(e.getStatusCode(), e.getResponseBodyAsString(), e.getMessage());
        }
    }

    @Override
    public String adAssayMetadata(AssayMetadata metadata) {
        return addAssayMetadata(metadata, true);
    }

    @Override
    public String addAssayMetadata(AssayMetadata metadata, boolean validate) {
        if (token == null) login (this.username, this.password);
        //set the header with token
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        HttpEntity<AssayMetadata> requestEntity = new HttpEntity<AssayMetadata>(metadata, headers);
        String url = this.url + "array/addAssayMetadata?validate=" + validate; 
        
        try {
            ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new CustomClientException(e.getStatusCode(), e.getResponseBodyAsString(), e.getMessage());
        }
    }
    
    @Override
    public String addPrintedSlide(PrintedSlide printedSlide) {
        if (token == null) login (this.username, this.password);
        //set the header with token
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        HttpEntity<PrintedSlide> requestEntity = new HttpEntity<PrintedSlide>(printedSlide, headers);
        String url = this.url + "array/addPrintedSlide";
        
        try {
            ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new CustomClientException(e.getStatusCode(), e.getResponseBodyAsString(), e.getMessage());
        }
    }

    @Override
    public String getDataProcessingMetadataByLabel(String name) {
        if (token == null) login (this.username, this.password);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        HttpEntity<Void> requestEntity = new HttpEntity<Void>(null, headers);
        String url = this.url + "array/listDataProcessingSoftware?offset=0&filter=" + name;
        try {
            
            ResponseEntity<MetadataListResultView> response = this.restTemplate.exchange(url, HttpMethod.GET, requestEntity, MetadataListResultView.class);
            MetadataListResultView result = response.getBody();
            for (MetadataCategory metadata: result.getRows()) {
                if (metadata.getName().equalsIgnoreCase(name)) {
                    return metadata.getId();
                }
            }
        } catch (HttpClientErrorException e) {
            throw new CustomClientException(e.getStatusCode(), e.getResponseBodyAsString(), "Error gettting metadata list: " + e.getMessage());
        }
        return null;
    }

    @Override
    public String getAssayMetadataByLabel(String name) {
        if (token == null) login (this.username, this.password);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        HttpEntity<Void> requestEntity = new HttpEntity<Void>(null, headers);
        String url = this.url + "array/listAssayMetadata?offset=0&filter=" + name;
        try {
            
            ResponseEntity<MetadataListResultView> response = this.restTemplate.exchange(url, HttpMethod.GET, requestEntity, MetadataListResultView.class);
            MetadataListResultView result = response.getBody();
            for (MetadataCategory metadata: result.getRows()) {
                if (metadata.getName().equalsIgnoreCase(name)) {
                    return metadata.getId();
                }
            }
        } catch (HttpClientErrorException e) {
            throw new CustomClientException(e.getStatusCode(), e.getResponseBodyAsString(), "Error gettting metadata list: " + e.getMessage());
        }
        return null;
    }

    @Override
    public String getImageAnalysisMetadataByLabel(String name) {
        if (token == null) login (this.username, this.password);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        HttpEntity<Void> requestEntity = new HttpEntity<Void>(null, headers);
        String url = this.url + "array/listImageAnalysisSoftware?offset=0&filter=" + name;
        try {
            
            ResponseEntity<MetadataListResultView> response = this.restTemplate.exchange(url, HttpMethod.GET, requestEntity, MetadataListResultView.class);
            MetadataListResultView result = response.getBody();
            for (MetadataCategory metadata: result.getRows()) {
                if (metadata.getName().equalsIgnoreCase(name)) {
                    return metadata.getId();
                }
            }
        } catch (HttpClientErrorException e) {
            throw new CustomClientException(e.getStatusCode(), e.getResponseBodyAsString(), "Error gettting metadata list: " + e.getMessage());
        }
        return null;
    }

    @Override
    public String getSlideMetadataByLabel(String name) {
        if (token == null) login (this.username, this.password);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        HttpEntity<Void> requestEntity = new HttpEntity<Void>(null, headers);
        String url = this.url + "array/listSlideMetadata?offset=0&filter=" + name;
        try {
            
            ResponseEntity<MetadataListResultView> response = this.restTemplate.exchange(url, HttpMethod.GET, requestEntity, MetadataListResultView.class);
            MetadataListResultView result = response.getBody();
            for (MetadataCategory metadata: result.getRows()) {
                if (metadata.getName().equalsIgnoreCase(name)) {
                    return metadata.getId();
                }
            }
        } catch (HttpClientErrorException e) {
            throw new CustomClientException(e.getStatusCode(), e.getResponseBodyAsString(), "Error gettting metadata list: " + e.getMessage());
        }
        return null;
    }

    @Override
    public String getScannerByLabel(String name) {
        if (token == null) login (this.username, this.password);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        HttpEntity<Void> requestEntity = new HttpEntity<Void>(null, headers);
        String url = this.url + "array/listScanners?offset=0&filter=" + name;
        try {
            
            ResponseEntity<MetadataListResultView> response = this.restTemplate.exchange(url, HttpMethod.GET, requestEntity, MetadataListResultView.class);
            MetadataListResultView result = response.getBody();
            for (MetadataCategory metadata: result.getRows()) {
                if (metadata.getName().equalsIgnoreCase(name)) {
                    return metadata.getId();
                }
            }
        } catch (HttpClientErrorException e) {
            throw new CustomClientException(e.getStatusCode(), e.getResponseBodyAsString(), "Error gettting metadata list: " + e.getMessage());
        }
        return null;
    }

    @Override
    public String getPrinterByLabel(String name) {
        if (token == null) login (this.username, this.password);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        HttpEntity<Void> requestEntity = new HttpEntity<Void>(null, headers);
        String url = this.url + "array/listPrinters?offset=0&filter=" + name;
        try {
            
            ResponseEntity<MetadataListResultView> response = this.restTemplate.exchange(url, HttpMethod.GET, requestEntity, MetadataListResultView.class);
            MetadataListResultView result = response.getBody();
            for (MetadataCategory metadata: result.getRows()) {
                if (metadata.getName().equalsIgnoreCase(name)) {
                    return metadata.getId();
                }
            }
        } catch (HttpClientErrorException e) {
            throw new CustomClientException(e.getStatusCode(), e.getResponseBodyAsString(), "Error gettting metadata list: " + e.getMessage());
        }
        return null;
    }

    @Override
    public String getSampleByLabel(String name) {
        if (token == null) login (this.username, this.password);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        HttpEntity<Void> requestEntity = new HttpEntity<Void>(null, headers);
        String url = this.url + "array/listSamples?offset=0&filter=" + name;
        try {
            
            ResponseEntity<MetadataListResultView> response = this.restTemplate.exchange(url, HttpMethod.GET, requestEntity, MetadataListResultView.class);
            MetadataListResultView result = response.getBody();
            for (MetadataCategory metadata: result.getRows()) {
                if (metadata.getName().equalsIgnoreCase(name)) {
                    return metadata.getId();
                }
            }
        } catch (HttpClientErrorException e) {
            throw new CustomClientException(e.getStatusCode(), e.getResponseBodyAsString(), "Error gettting metadata list: " + e.getMessage());
        }
        return null;
    }

    @Override
    public ArrayDataset getDatasetByLabel(String name) {
        if (token == null) login (this.username, this.password);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", token);
        HttpEntity<Void> requestEntity = new HttpEntity<Void>(null, headers);
        String url = this.url + "array/listArrayDataset?offset=0&loadAll=false&filter=" + name;
        try {
            
            ResponseEntity<ArrayDatasetListView> response = this.restTemplate.exchange(url, HttpMethod.GET, requestEntity, ArrayDatasetListView.class);
            ArrayDatasetListView result = response.getBody();
            for (ArrayDataset dataset: result.getRows()) {
                if (dataset.getName().equalsIgnoreCase(name)) {
                    return dataset;
                }
            }
        } catch (HttpClientErrorException e) {
            throw new CustomClientException(e.getStatusCode(), e.getResponseBodyAsString(), "Error gettting array dataset list: " + e.getMessage());
        }
        return null;
    }

}
