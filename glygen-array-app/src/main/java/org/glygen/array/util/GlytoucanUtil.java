package org.glygen.array.util;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

public class GlytoucanUtil {
	
	//TODO do not store it here, store encrypted in application.yml
	static String apiKey = "180accbf266f882f17b9e7067779872b5ed3360b7dc9f00a9ed58d5a6c77d6f7";
	static String URL = "https://api.glytoucan.org/glycan/register";
	static String userId = "ff2dda587eb4597ab1dfb995b520e99b7ef68d7786af0f3ea626555e2c609c3d";
	
	private static RestTemplate restTemplate = new RestTemplate();
	
	// needs to be done to initialize static variables to parse glycan sequence
	private static GlycanWorkspace glycanWorkspace = new GlycanWorkspace(null, false, new GlycanRendererAWT());
	
	
	public static String registerGlycan (String glycoCTSequence) {
	    
	    Sequence input = new Sequence();
	    input.setSequence(glycoCTSequence);
	    
	    HttpEntity<Sequence> requestEntity = new HttpEntity<Sequence>(input, createHeaders(userId, apiKey));
		
		try {
			ResponseEntity<Response> response = restTemplate.exchange(URL, HttpMethod.POST, requestEntity, Response.class);
			return response.getBody().getMessage();
		} catch (HttpClientErrorException e) {
			System.out.println("Exception adding glycan " + ((HttpClientErrorException) e).getResponseBodyAsString());
		} catch (HttpServerErrorException e) {
			System.out.println("Exception adding glycan " + ((HttpServerErrorException) e).getResponseBodyAsString());
		}
		
		return null;
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
				            	String glyTouCanId = registerGlycan(wurcs);
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
