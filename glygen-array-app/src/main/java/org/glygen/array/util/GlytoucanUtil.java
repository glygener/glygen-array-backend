package org.glygen.array.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.eurocarbdb.MolecularFramework.io.SugarImporterException;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarExporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarImporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorException;
import org.glycoinfo.GlycanFormatconverter.Glycan.GlyContainer;
import org.glycoinfo.GlycanFormatconverter.io.GlycoCT.GlyContainerToSugar;
import org.glycoinfo.GlycanFormatconverter.util.exchange.WURCSGraphToGlyContainer.WURCSGraphToGlyContainer;
import org.glycoinfo.WURCSFramework.io.GlycoCT.WURCSExporterGlycoCT;
import org.glycoinfo.WURCSFramework.util.WURCSException;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.wurcs.graph.WURCSGraph;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.data.FilterExclusionInfo;
import org.glygen.array.persistence.rdf.data.FilterExclusionReasonType;
import org.glygen.array.persistence.rdf.data.ProcessedData;
import org.glygen.array.persistence.rdf.data.TechnicalExclusionInfo;
import org.glygen.array.persistence.rdf.data.TechnicalExclusionReasonType;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GlytoucanUtil {
	
	String apiKey;
	String userId;
	
	static String glycanURL = "https://sparqlist.glycosmos.org/sparqlist/api/gtc_wurcs_by_accession?accNum=";
	static String retrieveURL ="https://sparqlist.glyconavi.org/api/WURCS2GlyTouCan?WURCS=";
	static String registerURL = "https://api.glytoucan.org/glycan/register";
	
	private static RestTemplate restTemplate = new RestTemplate();
	
	final static Logger logger = LoggerFactory.getLogger("event-logger");
	
	// needs to be done to initialize static variables to parse glycan sequence
   /* static {
        BuilderWorkspace glycanWorkspace = new BuilderWorkspace(new GlycanRendererAWT());
        glycanWorkspace.initData();
    }*/
	
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
	
	public String registerGlycan (String wurcsSequence) {
	    
	    Sequence input = new Sequence();
	    input.setSequence(wurcsSequence);
	    
	    HttpEntity<Sequence> requestEntity = new HttpEntity<Sequence>(input, createHeaders(userId, apiKey));
		
		try {
			ResponseEntity<Response> response = restTemplate.exchange(registerURL, HttpMethod.POST, requestEntity, Response.class);
			return response.getBody().getMessage();
		} catch (HttpClientErrorException e) {
			logger.error("Client Error: Exception adding glycan " + ((HttpClientErrorException) e).getResponseBodyAsString());
			logger.info("Sequence: " + wurcsSequence);
		} catch (HttpServerErrorException e) {
			logger.error("Server Error: Exception adding glycan " + ((HttpServerErrorException) e).getResponseBodyAsString());
			logger.info("Sequence: " + wurcsSequence);
		} catch (Exception e) {
		    logger.error("General Error: Exception adding glycan " + e.getMessage());
            logger.info("Sequence: " + wurcsSequence);
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
				if (response.getBody()[0].message  != null) {
				    logger.info("Error retrieving glycan " + response.getBody()[0].message);
				}
				return response.getBody()[0].id;
			} catch (HttpClientErrorException e) {
				logger.info("Exception retrieving glycan " + ((HttpClientErrorException) e).getResponseBodyAsString());
			} catch (HttpServerErrorException e) {
				logger.info("Exception retrieving glycan " + ((HttpServerErrorException) e).getResponseBodyAsString());
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
			RetrieveResponse[] arr = response.getBody();
			if (arr.length > 0)
			    return response.getBody()[0].getWurcsLabel();
			else 
			    return null;
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
	
	public static Sugar getSugarFromWURCS (String wurcsSequence) throws IOException {
	    try {
            WURCSFactory wf = new WURCSFactory(wurcsSequence);
            WURCSGraph graph = wf.getGraph();

            // Exchange WURCSGraph to GlyContainer
            WURCSGraphToGlyContainer wg2gc = new WURCSGraphToGlyContainer();
            wg2gc.start(graph);
            GlyContainer t_gc = wg2gc.getGlycan();

            // Exchange GlyConatainer to Sugar
            GlyContainerToSugar t_export = new GlyContainerToSugar();
            t_export.start(t_gc);
            Sugar t_sugar = t_export.getConvertedSugar();
            return t_sugar;
	    } catch (Exception e) {
	        throw new IOException ("Cannot be converted to Sugar object. Reason: " + e.getMessage());
	    }
	}
	
	
	public static void main(String[] args) {
		
		//String sequence = GlytoucanUtil.getInstance().retrieveGlycan("G89311TM");
		//sequence = GlytoucanUtil.getInstance().retrieveGlycan("G69046CR");
		
		//String accessionNumber = GlytoucanUtil.getInstance().getAccessionNumber("WURCS=2.0/35_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5]/1-1-2-3-3/a4-b1_b4-c1_c3-d1_c6-e1");
		
		//String accessionNumber = GlytoucanUtil.getInstance().getAccessionNumber("WURCS=2.0/6,13,12/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5][a1221m-1a_1-5][a2112h-1b_1-5][Aad21122h-2a_2-6_5*NCC/3=O]/1-1-2-3-1-4-5-6-1-3-1-5-4/a4-b1_a6-m1_b4-c1_c3-d1_c4-i1_c6-j1_d2-e1_e3-f1_e4-g1_g3-h2_j2-k1_k4-l1");
		//String accessionNumber = GlytoucanUtil.getInstance().getAccessionNumber("WURCS=2.0/7,12,11/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5][a1221m-1a_1-5][a2112h-1b_1-5][Aad21122h-2a_2-6_5*NCC/3=O][a2112h-1b_1-5_2*NCC/3=O]/1-1-2-3-1-4-5-6-3-1-7-4/a4-b1_a6-l1_b4-c1_c3-d1_c6-i1_d2-e1_e3-f1_e4-g1_g3-h2_i2-j1_j4-k1");
		
		//System.out.println(sequence);
		//System.out.println(accessionNumber);
		
		GlytoucanUtil.getInstance().setApiKey("6d9fbfb1c0a52cbbffae7c113395a203ae0e3995a455c42ff3932862cbf7e62a");
        GlytoucanUtil.getInstance().setUserId("ff2dda587eb4597ab1dfb995b520e99b7ef68d7786af0f3ea626555e2c609c3d");
		
		String glyTouCanId = GlytoucanUtil.getInstance().registerGlycan("WURCS=2.0/6,13,12/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5][a1221m-1a_1-5][a2112h-1b_1-5][Aad21122h-2a_2-6_5*NCC/3=O]/1-1-2-3-1-4-5-6-1-3-1-5-4/a4-b1_a6-m1_b4-c1_c3-d1_c4-i1_c6-j1_d2-e1_e3-f1_e4-g1_g3-h2_j2-k1_k4-l1");
		System.out.println(glyTouCanId);
		
		/*
		GlytoucanUtil.getInstance().setApiKey("180accbf266f882f17b9e7067779872b5ed3360b7dc9f00a9ed58d5a6c77d6f7");
		GlytoucanUtil.getInstance().setUserId("ff2dda587eb4597ab1dfb995b520e99b7ef68d7786af0f3ea626555e2c609c3d");
		
		// add glytoucan ids for GRITS database
		String databaseFolder = "/Users/sena/Desktop/GRITS-databases";
		//String newDbFolder = databaseFolder + "/glytoucanAdded";
		
		File dbFolder = new File (databaseFolder);
		GlycanDatabase missingGlycansDatabase = new GlycanDatabase();
		try
        {
			JAXBContext jaxbContext = JAXBContext.newInstance(GlycanDatabase.class);
	        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
	        Marshaller jaxBMarshaller = jaxbContext.createMarshaller();
			for (File file: dbFolder.listFiles()) {
				if (file.isDirectory()) continue;
				if (file.getName().endsWith (".xml")) {
		            // see if we can use the database file using the JAXB annotations
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
				            	String glyTouCanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
				            	if (glyTouCanId != null && glyTouCanId.length() == 8) { // glytoucanId 
				            		str.setGlytoucanid(glyTouCanId);
				            	} else {
				            		glyTouCanId = GlytoucanUtil.getInstance().registerGlycan(wurcs);
				            		if (glyTouCanId != null && glyTouCanId.length() == 8) { // glytoucanId 
				            			str.setGlytoucanid(glyTouCanId);
				            		} else {
				            			glyTouCanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
						            	if (glyTouCanId != null && glyTouCanId.length() == 8) { // glytoucanId 
						            		str.setGlytoucanid(glyTouCanId);
						            	} else {
					            			missingGlycansDatabase.addStructure(str);
					            			System.out.println ("Cannot find glytoucanid for " + str.getId());
						            	}
				            		}
				            	}
			            	}
		            	}
		            	count++;
		            	if (count % 20 == 0) {
		            		jaxBMarshaller.marshal(missingGlycansDatabase, new File (databaseFolder + File.separator + "out/missingGlytoucanId" + file.getName()));
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
		 
		*/ 
		
		/*String glycoCTSeq = "RES\n" + 
		        "1b:b-dglc-HEX-1:5\n" + 
		        "2s:n-acetyl\n" + 
		        "3b:b-dglc-HEX-1:5\n" + 
		        "4s:n-acetyl\n" + 
		        "5b:b-dman-HEX-1:5\n" + 
		        "6b:a-dman-HEX-1:5\n" + 
		        "7b:a-dman-HEX-1:5\n" + 
		        "8b:a-dman-HEX-1:5\n" + 
		        "9b:a-dman-HEX-1:5\n" + 
		        "LIN\n" + 
		        "1:1d(2+1)2n\n" + 
		        "2:1o(4+1)3d\n" + 
		        "3:3d(2+1)4n\n" + 
		        "4:3o(4+1)5d\n" + 
		        "5:5o(3+1)6d\n" + 
		        "6:5o(6+1)7d\n" + 
		        "7:7o(3+1)8d\n" + 
		        "8:7o(6+1)9d\n" + 
		        "UND\n" + 
		        "UND1:100.0:100.0\n" + 
		        "ParentIDs:8|9\n" + 
		        "SubtreeLinkageID1:o(-1+1)d\n" + 
		        "RES\n" + 
		        "10b:x-dglc-HEX-1:5\n" + 
		        "11s:n-acetyl\n" + 
		        "LIN\n" + 
		        "9:10d(2+1)11n";*/
		
		/*String glycoCTSeq = "RES\n" + 
		        "1b:b-dglc-HEX-1:5\n" + 
		        "2s:n-acetyl\n" + 
		        "3s:R_CARBOXYETHYL\n" + 
		        "4b:b-dglc-HEX-1:5\n" + 
		        "5s:n-acetyl\n" + 
		        "LIN\n" + 
		        "1:1d(2+1)2n\n" + 
		        "2:1o(3+1)3n\n" + 
		        "3:1d(4+1)4o\n" + 
		        "4:4d(2+1)5n";*/
		
		String glycoCTSeq = "RES\n"
		        + "1b:b-dglc-HEX-1:5\n"
		        + "2s:n-acetyl\n"
		        + "3b:b-dglc-HEX-1:5\n"
		        + "4s:n-acetyl\n"
		        + "5b:b-dman-HEX-1:5\n"
		        + "6b:a-dman-HEX-1:5\n"
		        + "7b:b-dglc-HEX-1:5\n"
		        + "8s:n-acetyl\n"
		        + "9b:b-dgal-HEX-1:5\n"
		        + "10b:a-dgro-dgal-NON-2:6|1:a|2:keto|3:d\n"
		        + "11s:n-acetyl\n"
		        + "12b:a-dman-HEX-1:5\n"
		        + "13b:b-dglc-HEX-1:5\n"
		        + "14s:n-acetyl\n"
		        + "15b:b-dgal-HEX-1:5\n"
		        + "16b:a-dgro-dgal-NON-2:6|1:a|2:keto|3:d\n"
		        + "17s:n-acetyl\n"
		        + "18b:a-lgal-HEX-1:5|6:d\n"
		        + "LIN\n"
		        + "1:1d(2+1)2n\n"
		        + "2:1o(4+1)3d\n"
		        + "3:3d(2+1)4n\n"
		        + "4:3o(4+1)5d\n"
		        + "5:5o(3+1)6d\n"
		        + "6:6o(2+1)7d\n"
		        + "7:7d(2+1)8n\n"
		        + "8:7o(4+1)9d\n"
		        + "9:9o(3+2)10d\n"
		        + "10:10d(5+1)11n\n"
		        + "11:5o(6+1)12d\n"
		        + "12:12o(2+1)13d\n"
		        + "13:13d(2+1)14n\n"
		        + "14:13o(4+1)15d\n"
		        + "15:15o(3+2)16d\n"
		        + "16:16d(5+1)17n\n"
		        + "17:1o(6+1)18d";
        WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
        try {
            exporter.start(glycoCTSeq);
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
        String wurcs = exporter.getWURCS(); 
        System.out.println(wurcs);
        
        wurcs = "WURCS=2.0/6,15,14/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5][a2112h-1b_1-5][Aad21122h-2a_2-6_5*NCC/3=O][a1221m-1a_1-5]/1-1-2-3-1-4-5-1-4-5-3-1-4-5-6/a4-b1_a6-o1_b4-c1_d2-h1_e4-f1_f3-g2_h4-i1_i3-j2_k2-l1_l4-m1_m3-n2_c?-d1_c?-k1_d?-e1";
        System.out.println(wurcs);
        glyTouCanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
        System.out.println(glyTouCanId);
        
        wurcs = "WURCS=2.0/3,4,4/[a2112h-1a_1-5][a2122h-1b_1-5_2*NCC/3=O][a2211m-1b_1-5]/1-2-2-3/a6-b1_b4-d1_c1-b3%.6%_a1-d3~n";
        glyTouCanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
        System.out.println(glyTouCanId);
        try {
            SugarImporterGlycoCTCondensed importer = new SugarImporterGlycoCTCondensed();
            Sugar sugar = importer.parse(glycoCTSeq);
            SugarExporterGlycoCTCondensed exporter2 = new SugarExporterGlycoCTCondensed();
            exporter2.start(sugar);
            System.out.println(exporter2.getHashCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        
     // temporary code to generate exclusion info json
        
        ProcessedData dummyData = new ProcessedData();
        List<TechnicalExclusionInfo> technical = new ArrayList<TechnicalExclusionInfo>();
        List<FilterExclusionInfo> filter = new ArrayList<FilterExclusionInfo>();
        
        TechnicalExclusionInfo info1 = new TechnicalExclusionInfo();
        info1.setReason(TechnicalExclusionReasonType.Artifact);
        info1.setFeatures(new ArrayList<Feature>());
        Feature feature1 = new Feature();
        feature1.setId("F12342465");
        feature1.setName("feature1");
        feature1.setInternalId("feature InternalId 1");
        Feature feature2 = new Feature();
        feature2.setId("F12342466");
        feature2.setName("feature2");
        feature2.setInternalId("feature InternalId 2");
        Feature feature3 = new Feature();
        feature3.setId("F12342467");
        feature3.setName("feature3");
        feature3.setInternalId("feature InternalId 3");
        info1.getFeatures().add(feature1);
        info1.getFeatures().add(feature2);
        info1.getFeatures().add(feature3);
        technical.add(info1);
        
        TechnicalExclusionInfo info2 = new TechnicalExclusionInfo();
        info2.setReason(TechnicalExclusionReasonType.Missing_Spot);
        info2.setFeatures(new ArrayList<Feature>());
        Feature feature4 = new Feature();
        feature4.setId("F12342474");
        feature4.setName("feature4");
        feature4.setInternalId("feature InternalId 4");
        Feature feature5 = new Feature();
        feature5.setId("F12342475");
        feature5.setName("feature5");
        feature5.setInternalId("feature InternalId 5");
        Feature feature6 = new Feature();
        feature6.setId("F12342476");
        feature6.setName("feature6");
        feature6.setInternalId("feature InternalId 6");
        info2.getFeatures().add(feature4);
        info2.getFeatures().add(feature5);
        info2.getFeatures().add(feature6);
        technical.add(info2);
        
        dummyData.setTechnicalExclusions(technical);
        
        FilterExclusionInfo info3 = new FilterExclusionInfo();
        info3.setReason(FilterExclusionReasonType.Unrelated_feature);
        info3.setFeatures(new ArrayList<Feature>());
        Feature feature7 = new Feature();
        feature7.setId("F12342480");
        feature7.setName("feature7");
        feature7.setInternalId("feature InternalId 7");
        Feature feature8 = new Feature();
        feature8.setId("F12342481");
        feature8.setName("feature8");
        feature8.setInternalId("feature InternalId 8");
        Feature feature9 = new Feature();
        feature9.setId("F12342482");
        feature9.setName("feature9");
        feature9.setInternalId("feature InternalId 9");
        info3.getFeatures().add(feature7);
        info3.getFeatures().add(feature8);
        info3.getFeatures().add(feature9);
        filter.add(info3);
        
        FilterExclusionInfo info4 = new FilterExclusionInfo();
        info4.setOtherReason("filtered out from this");
        info4.setFeatures(new ArrayList<Feature>());
        Feature feature10 = new Feature();
        feature10.setId("F12342490");
        feature10.setName("feature10");
        feature10.setInternalId("feature InternalId 10");
        Feature feature11 = new Feature();
        feature11.setId("F12342491");
        feature11.setName("feature11");
        feature11.setInternalId("feature InternalId 11");
        Feature feature12 = new Feature();
        feature12.setId("F12342492");
        feature12.setName("feature12");
        feature12.setInternalId("feature InternalId 12");
        info4.getFeatures().add(feature10);
        info4.getFeatures().add(feature11);
        info4.getFeatures().add(feature12);
        filter.add(info4);
        
        dummyData.setFilteredDataList(filter);
        
     // serialize this and save it to a file
        try {
            String jsonValue = new ObjectMapper().writeValueAsString(dummyData);
            System.out.println(jsonValue);
        } catch (JsonProcessingException e) {
            logger.error("Could not serialize processed data exclusion info into JSON", e);
            throw new GlycanRepositoryException("Could not serialize processed data exclusion info into JSON", e);
        }
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
	String message; // in case of error
	
	@JsonProperty("GlyTouCan")
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
	
	public String getMessage() {
        return message;
    }
	
	public void setMessage(String message) {
        this.message = message;
    }
}
