package org.glygen.array.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.glygen.array.client.exception.CustomClientException;
import org.glygen.array.client.model.BlockLayout;
import org.glygen.array.client.model.Feature;
import org.glygen.array.client.model.FeatureType;
import org.glygen.array.client.model.Glycan;
import org.glygen.array.client.model.GlycanSequenceFormat;
import org.glygen.array.client.model.GlycanType;
import org.glygen.array.client.model.ImportGRITSLibraryResult;
import org.glygen.array.client.model.Linker;
import org.glygen.array.client.model.LinkerClassification;
import org.glygen.array.client.model.LoginRequest;
import org.glygen.array.client.model.SequenceDefinedGlycan;
import org.glygen.array.client.model.SlideLayout;
import org.glygen.array.client.model.SmallMoleculeLinker;
import org.glygen.array.client.model.UnknownGlycan;
import org.grits.toolbox.glycanarray.library.om.ArrayDesignLibrary;
import org.grits.toolbox.glycanarray.library.om.LibraryInterface;
import org.grits.toolbox.glycanarray.library.om.feature.GlycanProbe;
import org.grits.toolbox.glycanarray.library.om.feature.Ratio;
import org.grits.toolbox.glycanarray.library.om.layout.Block;
import org.grits.toolbox.glycanarray.library.om.layout.Spot;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


public class GlycanRestClientImpl implements GlycanRestClient {
	
	private RestTemplate restTemplate = new RestTemplate();
	
	String token=null;
	String username;
	String password;
	String url = "http://localhost:8080/";
	
	List<String> duplicates = new ArrayList<String>();
	List<String> empty = new ArrayList<String>();
	
	Map <String, Feature> featureCache = new HashMap<String, Feature>();
	
	/**
     * {@inheritDoc}
     */
	@Override
	public String addGlycan(Glycan glycan) {
		if (token == null) login(this.username, this.password);
		//set the header with token
		HttpHeaders headers = new HttpHeaders();
	    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	    headers.add("Authorization", token);
	    HttpEntity<Glycan> requestEntity = new HttpEntity<Glycan>(glycan, headers);
	    String url = this.url + "array/addglycan";
	    url += "?noGlytoucanRegistration=true";
		System.out.println("URL: " + url);
		
		try {
			ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
			return response.getBody();
		} catch (HttpClientErrorException e) {
			System.out.println("Exception adding glycan " + ((HttpClientErrorException) e).getResponseBodyAsString());
			if (e.getResponseBodyAsString().contains("Duplicate") && e.getResponseBodyAsString().contains("sequence")) {
				duplicates.add(glycan.getName());
				if (glycan.getType() == GlycanType.SEQUENCE_DEFINED) {
					try {
						url = this.url + "array/getGlycanBySequence?sequence=" 
								+ URLEncoder.encode(((SequenceDefinedGlycan)glycan).getSequence(), StandardCharsets.UTF_8.name());
						HttpEntity<Map<String, String>> requestEntity1 = new HttpEntity<>(headers);
						ResponseEntity<String> glycanId = this.restTemplate.exchange(url, HttpMethod.GET, requestEntity1, String.class);
						if (glycanId.getBody() != null) {
						   // headers.setAccept(Arrays.asList(MediaType.TEXT_PLAIN));
							HttpEntity<String> requestEntity2 = new HttpEntity<String>(glycan.getName(), headers);
							// add as alias to the existing one
							url = this.url + "array/addAlias/" + glycanId.getBody();
							try {
								ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity2, String.class);
								System.out.println ("added alias " + glycan.getName());
								return response.getBody();
							} catch (HttpClientErrorException ex) {
								System.out.println ("Could not add alias for " + glycan.getName());
							}
						}
					} catch (UnsupportedEncodingException e1) {
						System.out.println ("Error encoding the sequence: " + e1.getMessage());
					}
				}
			}
			if (e.getResponseBodyAsString().contains("NoEmpty") && e.getResponseBodyAsString().contains("sequence")) {
				empty.add(glycan.getName());
			}
		}
		return null;
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
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}

	/**
     * {@inheritDoc}
     */
	@Override
	public String addBlockLayout(BlockLayout layout) {
		if (token == null) login(this.username, this.password);
		//set the header with token
		HttpHeaders headers = new HttpHeaders();
	    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	    headers.add("Authorization", token);
	    HttpEntity<BlockLayout> requestEntity = new HttpEntity<BlockLayout>(layout, headers);
		String url = this.url + "array/addblocklayout";
		System.out.println("URL: " + url);
		ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
		return response.getBody();
	}

	/**
     * {@inheritDoc}
     */
	@Override
	public String addLinker(Linker linker) {
		if (token == null) login(this.username, this.password);
		//set the header with token
		HttpHeaders headers = new HttpHeaders();
	    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	    headers.add("Authorization", token);
	    HttpEntity<Linker> requestEntity = new HttpEntity<Linker>(linker, headers);
		String url = this.url + "array/addlinker";
		System.out.println("URL: " + url);
		try {
			ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
			return response.getBody();
		} catch (HttpClientErrorException e) {
			String errorMessage = e.getResponseBodyAsString();
			if (errorMessage.contains("Duplicate") && !errorMessage.contains("pubChemId") 
					&& errorMessage.contains("name")) {
				linker.setName(linker.getName()+"B");
				requestEntity = new HttpEntity<Linker>(linker, headers);
				ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
				return response.getBody();
			}
		}
		return null;	
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
	public List<LinkerClassification> getLinkerClassifications() {
		String url = this.url + "util/getLinkerClassifications";
		System.out.println("URL: " + url);
		try {
			ObjectMapper mapper = new ObjectMapper();
			ResponseEntity<List> response = this.restTemplate.exchange(url, HttpMethod.GET, null, List.class);
			List list = response.getBody();
			List<LinkerClassification> classifications = mapper.convertValue(list, new TypeReference<List<LinkerClassification>>() { });
			return classifications;
		} catch (HttpClientErrorException e) {
			System.out.println ("Error gettting classification list: " + e.getMessage());
		}
		return null;	
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
	public String addSlideLayout(SlideLayout layout) {
		if (token == null) login(this.username, this.password);
		//set the header with token
		HttpHeaders headers = new HttpHeaders();
	    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	    headers.add("Authorization", token);
	    HttpEntity<SlideLayout> requestEntity = new HttpEntity<SlideLayout>(layout, headers);
		String url = this.url + "array/addslidelayout";
		System.out.println("URL: " + url);
		try {
			ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
			return response.getBody();
		} catch (HttpServerErrorException e) {
			String errorMessage = e.getResponseBodyAsString();
			if (errorMessage != null) {
				System.out.println("server error: " + errorMessage);
			}
		} catch (HttpClientErrorException e) {
			String errorMessage = e.getResponseBodyAsString();
			if (errorMessage != null) {
				System.out.println("client error: " + errorMessage);
			}
		}
		
		return null;
	}
	
	public List<String> getDuplicates() {
		return duplicates;
	}
	
	public List<String> getEmpty() {
		return empty;
	}

	/**
     * {@inheritDoc}
     */
	@Override
	public void setURL(String url) {
		this.url = url;
	}

	/**
     * {@inheritDoc}
     */
	@Override
	public String addFeature(Feature feature) {
	    System.out.println ("Trying to add " + feature.getName());
	    if (featureCache.get(feature.getName()) != null) {
	        System.out.println("Feature " + feature.getName() + " has already been added");
	        return featureCache.get(feature.getName()).getId();
	    }
		if (token == null) login(this.username, this.password);
		//set the header with token
		HttpHeaders headers = new HttpHeaders();
	    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	    headers.add("Authorization", token);
	    HttpEntity<Feature> requestEntity = new HttpEntity<Feature>(feature, headers);
		String url = this.url + "array/addfeature";
		System.out.println("URL: " + url);
		try {
			ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
			feature.setId(response.getBody());
			featureCache.put(feature.getName(), feature);
			return response.getBody();
		} catch (HttpServerErrorException e) {
			String errorMessage = e.getResponseBodyAsString();
			if (errorMessage != null) {
				System.out.println("server error: " + errorMessage);
				System.out.println("Cannot add feature:" + feature.getName());
			} 
		} catch (HttpClientErrorException e) {
			String errorMessage = e.getResponseBodyAsString();
			if (errorMessage != null) {
			    if (errorMessage.contains("Duplicate")) {
			        featureCache.put(feature.getName(), feature);
			    }
				System.out.println("client error: " + errorMessage);
				System.out.println("Cannot add feature:" + feature.getName());
			}
		}
		
		return null;
	}

	/**
     * {@inheritDoc}
     */
    @Override
    public ImportGRITSLibraryResult addFromLibrary(ArrayDesignLibrary library, Map<String, String> linkerClassificationMap, String layoutName) {
        ImportGRITSLibraryResult result = new ImportGRITSLibraryResult();
        // add Glycans
        List<org.grits.toolbox.glycanarray.library.om.feature.Glycan> glycanList = library.getFeatureLibrary().getGlycan();
        List<org.grits.toolbox.glycanarray.library.om.feature.Linker> linkerList = library.getFeatureLibrary().getLinker();
        List<org.grits.toolbox.glycanarray.library.om.feature.Feature> features = library.getFeatureLibrary().getFeature();
        List<org.grits.toolbox.glycanarray.library.om.layout.BlockLayout> blockLayouts = library.getLayoutLibrary().getBlockLayout();
        List<org.grits.toolbox.glycanarray.library.om.layout.SlideLayout> layouts = library.getLayoutLibrary().getSlideLayout();
        
        if (layoutName != null) {
        	for (org.grits.toolbox.glycanarray.library.om.layout.SlideLayout l: library.getLayoutLibrary().getSlideLayout()) {
        		if (l.getName().equalsIgnoreCase(layoutName)) {
        			layouts.clear();
        			layouts.add(l);
        			blockLayouts.clear();
        			glycanList.clear();
        			linkerList.clear();
        			features.clear();
        			for (Block b: l.getBlock()) {
        				String blockLayoutName = b.getBlockName();
        				for (org.grits.toolbox.glycanarray.library.om.layout.BlockLayout bl: 
        					library.getLayoutLibrary().getBlockLayout()) {
        					if (blockLayoutName.equals(bl.getName())) {
        						if (!blockLayouts.contains(bl)) {
        							blockLayouts.add(bl);
        							for (Spot s: bl.getSpot()) {
        								for (org.grits.toolbox.glycanarray.library.om.feature.Feature f: 
        									library.getFeatureLibrary().getFeature()) {
        									if (f.getId() == s.getFeatureId()) {
        										if (!features.contains(f)) {
        											features.add(f);
        											for (Ratio ratio : f.getRatio()) {
        						                        GlycanProbe probe = null;
        						                        for (GlycanProbe p : library.getFeatureLibrary().getGlycanProbe()) {
        						                            if (p.getId().equals(ratio.getItemId())) {
        						                                probe = p;
        						                                break;
        						                            }
        						                        }
        						                        if (probe != null) {
        						                            for (Ratio r1 : probe.getRatio()) {
        						                                org.grits.toolbox.glycanarray.library.om.feature.Glycan glycan = LibraryInterface.getGlycan(library, r1.getItemId());
        						                                if (!glycanList.contains(glycan)) {
        						                                	glycanList.add(glycan);
        						                                }
        						                                org.grits.toolbox.glycanarray.library.om.feature.Linker linker = LibraryInterface.getLinker(library, probe.getLinker());
        						                                if (!linkerList.contains(linker)) {
        						                                	linkerList.add(linker);
        						                                }
        						                            }
        						                        }
        											}
        										}
        									}
        								}
        							}
        						}
        					}
        				}
        			}
        		}
        	}
        }
        
        // add glycans
        for (org.grits.toolbox.glycanarray.library.om.feature.Glycan glycan : glycanList) {
            Glycan view = null;
            if (glycan.getSequence() == null) {
                if (glycan.getOrigSequence() == null && (glycan.getClassification() == null || glycan.getClassification().isEmpty())) {
                    // this is not a glycan, control or a flag
                } else {
                    view = new UnknownGlycan();
                }
            } else {
                view = new SequenceDefinedGlycan();
                ((SequenceDefinedGlycan) view).setGlytoucanId(glycan.getGlyTouCanId());
                ((SequenceDefinedGlycan) view).setSequence(glycan.getSequence());
                ((SequenceDefinedGlycan) view).setSequenceType(GlycanSequenceFormat.GLYCOCT);
            }
            if (view != null) {
                view.setInternalId(glycan.getId()+ "");
                view.setName(glycan.getName());
                view.setComment(glycan.getComment());
                try {
                    addGlycan(view);
                } catch (HttpClientErrorException e) {
                    System.out.println("Glycan " + glycan.getId() + " cannot be added. Reason: " + e.getMessage());
                }
            }
        }
        
        // add Linkers 
        List<LinkerClassification> classificationList = getLinkerClassifications();
        for (org.grits.toolbox.glycanarray.library.om.feature.Linker linker : linkerList) {
            SmallMoleculeLinker view = new SmallMoleculeLinker();
            if (linker.getPubChemId() != null) view.setPubChemId(linker.getPubChemId().longValue());
            view.setName(linker.getName());
            if (linker.getSequence() != null)
                view.setDescription(linker.getSequence());
            view.setComment(linker.getComment());
            if (linker.getPubChemId() == null) {
                String classification = linkerClassificationMap.get(linker.getName());
                if (classification != null) {
                    LinkerClassification lClass = new LinkerClassification();
                    lClass.setClassification(classification);
                    view.setClassification(lClass);
                }
                else {   
                    // assign a random classification
                    Collections.shuffle(classificationList);
                    view.setClassification((LinkerClassification) classificationList.get(0));
                }
            }
            try {
                addLinker(view);
            } catch (HttpClientErrorException e) {
                System.out.println ("Linker " + linker.getId() + " cannot be added. Reason:" + e.getMessage());
            }
        }
        
        // add Features   
        for (org.grits.toolbox.glycanarray.library.om.feature.Feature f: features) {
            Feature myFeature = new Feature();
            myFeature.setName(f.getName());
            List<Ratio> ratios = f.getRatio();
            for (Ratio ratio : ratios) {
                GlycanProbe probe = null;
                for (GlycanProbe p : library.getFeatureLibrary().getGlycanProbe()) {
                    if (p.getId().equals(ratio.getItemId())) {
                        probe = p;
                        break;
                    }
                }
                if (probe != null) {
                    boolean linkerOnly = true;
                    for (Ratio r1 : probe.getRatio()) {
                        org.grits.toolbox.glycanarray.library.om.feature.Glycan glycan = LibraryInterface.getGlycan(library, r1.getItemId());
                        if (glycan != null) {
                            org.glygen.array.client.model.Glycan myGlycan = null;
                            if (glycan.getSequence() == null) {
                                myGlycan = new UnknownGlycan();
                                myGlycan.setName(glycan.getName()); // name is sufficient to locate the unknown glycan
                            } else {
                                myGlycan = new org.glygen.array.client.model.SequenceDefinedGlycan();
                                ((SequenceDefinedGlycan) myGlycan).setSequence(glycan.getSequence());  // sequence is sufficient to locate this Glycan in the repository
                                ((SequenceDefinedGlycan) myGlycan).setSequenceType(GlycanSequenceFormat.GLYCOCT);
                            }
                            myFeature.addGlycan(myGlycan);
                            myFeature.setType(FeatureType.NORMAL);
                            linkerOnly = false;
                        }
                        
                    }
                    org.grits.toolbox.glycanarray.library.om.feature.Linker linker = LibraryInterface.getLinker(library, probe.getLinker());
                    if (linker != null) {
                        org.glygen.array.client.model.SmallMoleculeLinker myLinker = new org.glygen.array.client.model.SmallMoleculeLinker();
                        if (linker.getPubChemId() != null) myLinker.setPubChemId(linker.getPubChemId().longValue());  // pubChemId is sufficient to locate this Linker in the repository
                        else {
                            // need to set classification and name
                            myLinker.setName(linker.getName());
                            String classification = linkerClassificationMap.get(linker.getName());
                            if (classification != null) {
                                LinkerClassification lClass = new LinkerClassification();
                                lClass.setClassification(classification);
                                myLinker.setClassification(lClass);
                            }
                            else {   
                                // assign a random classification
                                Collections.shuffle(classificationList);
                                myLinker.setClassification((LinkerClassification) classificationList.get(0));
                            }
                        }
                        myFeature.setLinker(myLinker);
                        if (linkerOnly) myFeature.setType(FeatureType.CONTROL);
                    } 
                }
            }
            try {
                addFeature(myFeature);
            } catch (HttpClientErrorException e) {
                System.out.println("Feature " + f.getName() + " cannot be added. Reason: " + e.getMessage());
            }
        }
        
        // add Block Layouts
        for (org.grits.toolbox.glycanarray.library.om.layout.BlockLayout blockLayout : blockLayouts) {
            BlockLayout myLayout = new BlockLayout();
            myLayout.setName(blockLayout.getName());
            myLayout.setDescription(blockLayout.getComment());
            myLayout.setWidth(blockLayout.getColumnNum());
            myLayout.setHeight(blockLayout.getRowNum());
            myLayout.setSpots(getSpotsFromBlockLayout(library, blockLayout));
            
            try {
                addBlockLayout (myLayout);
            } catch (HttpClientErrorException e) {
                System.out.println("BlockLayout " + blockLayout.getId() + " cannot be added. Reason: " + e.getMessage());
            }
        }
        
        // add Slide Layouts
        for (org.grits.toolbox.glycanarray.library.om.layout.SlideLayout slideLayout : layouts) {
            SlideLayout mySlideLayout = new SlideLayout();
            mySlideLayout.setName(slideLayout.getName());
            mySlideLayout.setDescription(slideLayout.getDescription());
            
            List<org.glygen.array.client.model.Block> blocks = new ArrayList<org.glygen.array.client.model.Block>();
            int width = 0;
            int height = 0;
            for (Block block: slideLayout.getBlock()) {
                org.glygen.array.client.model.Block myBlock = new org.glygen.array.client.model.Block();
                myBlock.setColumn(block.getColumn());
                myBlock.setRow(block.getRow());
                if (block.getColumn() > width)
                    width = block.getColumn();
                if (block.getRow() > height)
                    height = block.getRow();
                Integer blockLayoutId = block.getLayoutId();
                org.grits.toolbox.glycanarray.library.om.layout.BlockLayout blockLayout = LibraryInterface.getBlockLayout(library, blockLayoutId);
                BlockLayout myLayout = new BlockLayout();
                myLayout.setName(blockLayout.getName());
                myBlock.setBlockLayout(myLayout);
                myLayout.setSpots(getSpotsFromBlockLayout(library, blockLayout));
                blocks.add(myBlock);
            }
            
            mySlideLayout.setHeight(slideLayout.getHeight() == null ? height: slideLayout.getHeight());
            mySlideLayout.setWidth(slideLayout.getWidth() == null ? width: slideLayout.getWidth());
            mySlideLayout.setBlocks(blocks);
            
            try {
                addSlideLayout (mySlideLayout);
                result.getAddedLayouts().add(mySlideLayout);
            } catch (HttpClientErrorException e) {
                String errorMessage = e.getResponseBodyAsString();
                if (errorMessage.contains("Duplicate")) {
                    result.getDuplicates().add(mySlideLayout);
                } else {
                    result.getErrors().add(mySlideLayout);
                }
            } catch (HttpServerErrorException e) {
                String errorMessage = e.getResponseBodyAsString();
                if (errorMessage.contains("Duplicate")) {
                    result.getDuplicates().add(mySlideLayout);
                } else {
                    result.getErrors().add(mySlideLayout);
                }
            }
        }
        
        return result;  
    }
    
    public static List<org.glygen.array.client.model.Spot> getSpotsFromBlockLayout (ArrayDesignLibrary library, 
            org.grits.toolbox.glycanarray.library.om.layout.BlockLayout blockLayout) {
        List<org.glygen.array.client.model.Spot> spots = new ArrayList<>();
        for (Spot spot: blockLayout.getSpot()) {
            org.glygen.array.client.model.Spot s = new org.glygen.array.client.model.Spot();
            s.setRow(spot.getY());
            s.setColumn(spot.getX());
            s.setGroup(spot.getGroup());
            s.setConcentration(spot.getConcentration());
            org.grits.toolbox.glycanarray.library.om.feature.Feature feature = LibraryInterface.getFeature(library, spot.getFeatureId());
            List<org.glygen.array.client.model.Feature> features = new ArrayList<>();
            if (feature != null) {
                List<Ratio> ratios = feature.getRatio();
                for (Ratio ratio : ratios) {
                    org.glygen.array.client.model.Feature myFeature = new org.glygen.array.client.model.Feature();
                    myFeature.setName(feature.getName());
                    GlycanProbe probe = null;
                    for (GlycanProbe p : library.getFeatureLibrary().getGlycanProbe()) {
                        if (p.getId().equals(ratio.getItemId())) {
                            probe = p;
                            break;
                        }
                    }
                    if (probe != null) {   
                        org.grits.toolbox.glycanarray.library.om.feature.Linker linker = LibraryInterface.getLinker(library, probe.getLinker());
                        if (linker != null) {
                            myFeature.setType(FeatureType.NORMAL);
                        } else {
                            myFeature.setType(FeatureType.CONTROL);
                        }
                    }
                    features.add(myFeature);
                }
            }
            s.setFeatures(features);
            if (!features.isEmpty())
                spots.add(s);
        }
        
        return spots;
    }
}
