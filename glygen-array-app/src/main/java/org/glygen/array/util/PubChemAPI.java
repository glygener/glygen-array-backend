package org.glygen.array.util;


import org.glygen.array.persistence.rdf.Linker;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class PubChemAPI {
	
	final static String url = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/";
	final static String propertyURL = "/property/MolecularFormula,MonoisotopicMass,InChIKey,InChI,IUPACName/JSON";
	
	public static Linker getLinkerDetailsFromPubChem (Long pubChemId) {
		RestTemplate restTemplate = new RestTemplate();
		String requestURL = url + pubChemId + propertyURL;
		ResponseEntity<PubChemResult> response = restTemplate.exchange(requestURL, HttpMethod.GET, null, PubChemResult.class);
		return PubChemAPI.getLinkerFromResult (response.getBody(), pubChemId);
	}

	public static Linker getLinkerFromResult(PubChemResult response, Long pubChemId) {
		if (response != null && response.getPropertyTable() != null && 
				response.getPropertyTable().getProperties() != null && !response.getPropertyTable().getProperties().isEmpty()) {
			PubChemProperty prop = response.getPropertyTable().getProperties().get(0);
			if (prop.getInChI() == null && prop.getInChIKey() == null && prop.getIUPACName() == null && prop.getMass() == null && prop.getMolecularFormula() == null)
				return null;   // invalid pubChem
			Linker linker = new Linker();
			linker.setInChiKey(prop.getInChIKey());
			linker.setInChiSequence(prop.getInChI());
			linker.setMass(prop.getMass());
			linker.setMolecularFormula(prop.getMolecularFormula());
			linker.setIupacName(prop.getIUPACName());
			linker.setImageURL(url + pubChemId + "/PNG");
			linker.setPubChemId(pubChemId);
			
			return linker;
		}
		return null;
	}
	
	public static void main(String[] args) {
		Linker linker = getLinkerDetailsFromPubChem (2444L);
		System.out.println ("linker formula:"  + linker.getMolecularFormula());
		System.out.println ("linker image URL:"  + linker.getImageURL());
		System.out.println ("linker mass:"  + linker.getMass());
		
	}
	

}
