package org.glygen.array.client;

import java.util.List;

import org.glygen.array.client.model.data.StatisticalMethod;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PublicUtilityClientImpl implements PublicUtilitiyClient {
    
    private RestTemplate restTemplate = new RestTemplate();
    String url;

    @Override
    public List<StatisticalMethod> getAllStatisticalMethods() {
        
        String url = this.url + "util/statisticalmethods";
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            ResponseEntity<List> response = this.restTemplate.exchange(url, HttpMethod.GET, null, List.class);
            List list = response.getBody();
            List<StatisticalMethod> methods = mapper.convertValue(list, new TypeReference<List<StatisticalMethod>>() { });
            return methods;
        } catch (HttpClientErrorException e) {
            System.out.println ("Error gettting classification list: " + e.getMessage());
        }
        return null;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }

}
