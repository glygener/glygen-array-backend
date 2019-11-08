package org.glygen.array.util;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.codec.binary.Base64;
import org.eurocarbdb.MolecularFramework.io.SugarImporterException;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorException;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanRendererAWT;
import org.eurocarbdb.application.glycoworkbench.GlycanWorkspace;
import org.glycoinfo.GlycanFormatconverter.io.GlycoCT.WURCSExporterGlycoCT;
import org.glycoinfo.WURCSFramework.util.WURCSException;
import org.grits.toolbox.util.structure.glycan.database.GlycanDatabase;
import org.grits.toolbox.util.structure.glycan.database.GlycanStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GlytoucanUtil {
	
	String apiKey;
	String userId;
	
	static String glycanURL = "https://sparqlist.glycosmos.org/sparqlist/api/gtc_wurcs_by_accession?accNum=";
	static String retrieveURL ="https://api.glycosmos.org/glytoucan/sparql/wurcs2gtcids?wurcs=";
	static String registerURL = "https://api.glytoucan.org/glycan/register";
	
	private static RestTemplate restTemplate = new RestTemplate();
	
	final static Logger logger = LoggerFactory.getLogger("event-logger");
	
	// needs to be done to initialize static variables to parse glycan sequence
	private static GlycanWorkspace glycanWorkspace = new GlycanWorkspace(null, false, new GlycanRendererAWT());
	
	static GlytoucanUtil instance;
	
	private GlytoucanUtil() {
	}
	
	public static GlytoucanUtil getInstance () {
		if (instance == null)
			instance = new GlytoucanUtil();
		return instance;
	}
	
	
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String registerGlycan (String glycoCTSequence) {
	    
	    Sequence input = new Sequence();
	    input.setSequence(glycoCTSequence);
	    
	    HttpEntity<Sequence> requestEntity = new HttpEntity<Sequence>(input, createHeaders(userId, apiKey));
		
		try {
			ResponseEntity<Response> response = restTemplate.exchange(registerURL, HttpMethod.POST, requestEntity, Response.class);
			return response.getBody().getMessage();
		} catch (HttpClientErrorException e) {
			logger.error("Exception adding glycan " + ((HttpClientErrorException) e).getResponseBodyAsString());
		} catch (HttpServerErrorException e) {
			logger.error("Exception adding glycan " + ((HttpServerErrorException) e).getResponseBodyAsString());
		}
		
		return null;
	}
	
	public String getAccessionNumber (String wurcsSequence) {
		String accessionNumber = null;
		
		String url;
		//try {
			url = retrieveURL + wurcsSequence;
			HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(createHeaders(userId, apiKey));
			try {
				ResponseEntity<GlytoucanResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, GlytoucanResponse[].class);
				return response.getBody()[0].id;
			} catch (HttpClientErrorException e) {
				logger.info("Exception retrieving glycan " + ((HttpClientErrorException) e).getResponseBodyAsString());
			} catch (HttpServerErrorException e) {
				logger.info("Exception adding glycan " + ((HttpServerErrorException) e).getResponseBodyAsString());
			}
		//} catch (UnsupportedEncodingException e1) {
		//	logger.error("Could not encode wurcs sequence", e1);
		//}
		
		
		return accessionNumber;
	}
	
	/**
	 * calls Glytoucan API to retrieve the glycan with the given accession number
	 * 
	 * @param accessionNumber the glytoucan id to search
	 * @return WURCS sequence if the glycan is found, null otherwise
	 */
	public String retrieveGlycan (String accessionNumber) {
		String sequence = null;
		
		String url = glycanURL + accessionNumber;
		HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(createHeaders(userId, apiKey));
		try {
			ResponseEntity<RetrieveResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, RetrieveResponse[].class);
			return response.getBody()[0].getWurcsLabel();
			
		} catch (HttpClientErrorException e) {
			logger.info("Exception retrieving glycan " + ((HttpClientErrorException) e).getResponseBodyAsString());
		} catch (HttpServerErrorException e) {
			logger.info("Exception adding glycan " + ((HttpServerErrorException) e).getResponseBodyAsString());
		}
		
		return sequence;
	}
	
	static HttpHeaders createHeaders(String username, String password){
	   return new HttpHeaders() {{
	         String auth = username + ":" + password;
	         byte[] encodedAuth = Base64.encodeBase64( 
	            auth.getBytes(Charset.forName("US-ASCII")) );
	         String authHeader = "Basic " + new String( encodedAuth );
	         set( "Authorization", authHeader );
	         setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	      }};
	}
	
	
	public static void main(String[] args) {
		
		String sequence = GlytoucanUtil.getInstance().retrieveGlycan("G89311TM");
		sequence = GlytoucanUtil.getInstance().retrieveGlycan("G69046CR");
		
		String accessionNumber = GlytoucanUtil.getInstance().getAccessionNumber("WURCS=2.0/35_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5]/1-1-2-3-3/a4-b1_b4-c1_c3-d1_c6-e1");
		
		System.out.println(sequence);
		System.out.println(accessionNumber);

		GlytoucanUtil.getInstance().setApiKey("180accbf266f882f17b9e7067779872b5ed3360b7dc9f00a9ed58d5a6c77d6f7");
		GlytoucanUtil.getInstance().setUserId("ff2dda587eb4597ab1dfb995b520e99b7ef68d7786af0f3ea626555e2c609c3d");
		
		// add glytoucan ids for GRITS database
		String databaseFolder = "/Users/sena/Desktop/GRITS-databases";
		//String newDbFolder = databaseFolder + "/glytoucanAdded";
		
		File dbFolder = new File (databaseFolder);
		try
        {
			for (File file: dbFolder.listFiles()) {
				if (file.isDirectory()) continue;
				if (file.getName().endsWith (".xml")) {
		            // see if we can use the database file using the JAXB annotations
		            JAXBContext jaxbContext = JAXBContext.newInstance(GlycanDatabase.class);
		            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		            GlycanDatabase database = (GlycanDatabase) jaxbUnmarshaller.unmarshal(file);
		            int count = 0;
		            for (GlycanStructure str: database.getStructures()) {
		            	if (str.getGlytoucanid() == null) {
			            	String gwb = str.getGWBSequence();
							Glycan glycan = Glycan.fromString(gwb);
			            	if (glycan != null) {
				            	String glycoCT = glycan.toGlycoCTCondensed();
				            	WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
				            	exporter.start(glycoCT);
				            	String wurcs = exporter.getWURCS();	
				            	String glyTouCanId = GlytoucanUtil.getInstance().registerGlycan(wurcs);
				            	if (glyTouCanId != null && glyTouCanId.length() == 8) { // glytoucanId 
				            		str.setGlytoucanid(glyTouCanId);
				            	} else {
				            		System.out.println ("Cannot find glytoucanid for " + str.getId());
				            	}
			            	}
		            	}
		            	count++;
		            	if (count % 20 == 0) {
		            		Marshaller jaxBMarshaller = jaxbContext.createMarshaller();
				            jaxBMarshaller.marshal(database, new File (databaseFolder + File.separator + file.getName()));
		            	}
		            }
		            
				}
			}
        }
        catch (JAXBException e)
        {
           e.printStackTrace();
        } catch (SugarImporterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GlycoVisitorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (WURCSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		String glycoCTSequence = "RES\n" + 
				"1b:o-dman-HEX-0:0\n" + 
				"2s:anhydro\n" + 
				"LIN\n" + 
				"1:1d(2-1)2n\n" + 
				"2:1o(5-1)2n";
		
		//glycoCTSequence = "WURCS=2.0/4,5,5/[a2112h-1b_1-5_2*NCC/3=O][a2122A-1b_1-5][a2112h-1a_1-5][a2122h-1b_1-5]/1-2-1-3-4/a3-b1_b4-c1_c4-d1_d3-e1_a1-e4~n";
		glycoCTSequence = "WURCS=2.0/3,12,11/[a2122h-1x_1-5_2*NCC/3=O][a1122h-1x_1-5][a2112h-1x_1-5]/1-1-2-2-2-2-2-2-2-2-2-3/a4-b1_b4-c1_c?-d1_c?-i1_d?-e1_d?-g1_e?-f1_g?-h1_i?-j1_j?-k1_j?-l1";
		//System.out.println (GlytoucanUtil.registerGlycan(glycoCTSequence));
		 
	}
		
	
}

class Sequence {
	String sequence;
	
	public void setSequence (String s) {
		this.sequence = s;
	}
	
	public String getSequence() {
		return sequence;
	}
}

class Response {
	String timestamp;
	String status;
	String error;
	String message;
	String path;
	/**
	 * @return the timestamp
	 */
	public String getTimestamp() {
		return timestamp;
	}
	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}
	/**
	 * @return the error
	 */
	public String getError() {
		return error;
	}
	/**
	 * @param error the error to set
	 */
	public void setError(String error) {
		this.error = error;
	}
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}
	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}
	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}
}

class RetrieveResponse {
	String accessionNumber;
	String hashKey;
	String wurcsLabel;
	
	/**
	 * @return the accessionNumber
	 */
	@JsonProperty("AccessionNumber")
	public String getAccessionNumber() {
		return accessionNumber;
	}
	/**
	 * @param accessionNumber the accessionNumber to set
	 */
	public void setAccessionNumber(String accessionNumber) {
		this.accessionNumber = accessionNumber;
	}
	/**
	 * @return the hashKey
	 */
	@JsonProperty("HashKey")
	public String getHashKey() {
		return hashKey;
	}
	/**
	 * @param hashKey the hashKey to set
	 */
	public void setHashKey(String hashKey) {
		this.hashKey = hashKey;
	}
	/**
	 * @return the wurcsLabel
	 */
	@JsonProperty("NormalizedWurcs")
	public String getWurcsLabel() {
		return wurcsLabel;
	}
	/**
	 * @param wurcsLabel the wurcsLabel to set
	 */
	public void setWurcsLabel(String wurcsLabel) {
		this.wurcsLabel = wurcsLabel;
	}	  
}

class GlytoucanResponse {
	String id;
	String wurcs;
	
	public String getId() {
		return id;
	}
	
	@JsonProperty("WURCS")
	public String getWurcs() {
		return wurcs;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setWurcs(String wurcs) {
		this.wurcs = wurcs;
	}
}
