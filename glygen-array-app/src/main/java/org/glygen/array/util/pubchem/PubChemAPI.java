package org.glygen.array.util.pubchem;


import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.LinkerClassification;
import org.glygen.array.persistence.rdf.SmallMoleculeLinker;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class PubChemAPI {
	
	public final static String PUBCHEM_CID = "https://pubchem.ncbi.nlm.nih.gov/compound/";
	final static String url = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/";
	final static String propertyURL = "/property/MolecularFormula,MonoisotopicMass,InChIKey,InChI,IUPACName/JSON";
	final static String classificationURL ="/classification/JSON?classification_type=simple";
	public final static String CHEBI_URI = "http://purl.obolibrary.org/obo/CHEBI_";
	
	public static Linker getLinkerDetailsFromPubChem (Long pubChemId) {
		if (pubChemId == null)
			return null;
		RestTemplate restTemplate = new RestTemplate();
		String requestURL = url + pubChemId + propertyURL;
		ResponseEntity<PubChemResult> response = restTemplate.exchange(requestURL, HttpMethod.GET, null, PubChemResult.class);
		Linker linker = PubChemAPI.getLinkerFromResult (response.getBody(), pubChemId);
		try {
			requestURL = url + pubChemId + classificationURL;
			ResponseEntity<PubChemClassificationResult> response2 = restTemplate.exchange(requestURL, HttpMethod.GET, null, PubChemClassificationResult.class);
			LinkerClassification classification = PubChemAPI.getClassificationFromResult(response2.getBody());
			((SmallMoleculeLinker)linker).setClassification(classification);
		} catch (Exception e) {
			// do nothing
		}
		
		return linker;
	}

	private static LinkerClassification getClassificationFromResult(
			PubChemClassificationResult result) {
		if (result.getHierarchies() != null && result.getHierarchies().getHierarchy() != null) {
			for (Hierarchy hierarchy: result.getHierarchies().getHierarchy()) {
				if (hierarchy.getNode() != null) {
					for (Node node: hierarchy.getNode()) {
						if (node.getInformation() != null) {
							if (node.getInformation().getChildID() == null || node.getInformation().getChildID().isEmpty()) {
								// leaf node
								String url = node.getInformation().getURL();
								if (url.contains("chebiId=CHEBI:")) {
									LinkerClassification classification = new LinkerClassification();
									classification.setClassification(node.getInformation().getName());
									String chEBIId = url.substring(url.lastIndexOf(":")+1);
									try {
										classification.setChebiId(Integer.parseInt(chEBIId));
									} catch (NumberFormatException e) {
										// ignore
									}
									classification.setUri(CHEBI_URI + chEBIId);
									return classification;
								}
							}
						}
					}
				}
			}
		}
		
		return null;
	}

	public static Linker getLinkerFromResult(PubChemResult response, Long pubChemId) {
		if (response != null && response.getPropertyTable() != null && 
				response.getPropertyTable().getProperties() != null && !response.getPropertyTable().getProperties().isEmpty()) {
			PubChemProperty prop = response.getPropertyTable().getProperties().get(0);
			if (prop.getInChI() == null && prop.getInChIKey() == null && prop.getIUPACName() == null && prop.getMass() == null && prop.getMolecularFormula() == null)
				return null;   // invalid pubChem
			SmallMoleculeLinker linker = new SmallMoleculeLinker();
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
		System.out.println ("linker formula:"  + ((SmallMoleculeLinker)linker).getMolecularFormula());
		System.out.println ("linker image URL:"  + ((SmallMoleculeLinker)linker).getImageURL());
		System.out.println ("linker mass:"  + ((SmallMoleculeLinker)linker).getMass());
		
	}
	

}