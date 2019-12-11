package org.glygen.array.util;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class UniProtUtil {

    static String url = "https://www.uniprot.org/uniprot/";
    static String type = ".fasta";
    
    public static String getSequenceFromUniProt(String uniProtId) {
        RestTemplate restTemplate = new RestTemplate();
        String requestURL = url + uniProtId + type;
        try {
            ResponseEntity<String> response = restTemplate.exchange(requestURL, HttpMethod.GET, null, String.class);
            String fasta = response.getBody();
            if (fasta == null)
                return null;
            return fasta.substring(fasta.indexOf("\n")+1);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static void main(String[] args) {
        System.out.println (UniProtUtil.getSequenceFromUniProt("P12345"));
    }
}
