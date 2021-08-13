package org.glygen.array.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.eurocarbdb.MolecularFramework.io.SugarImporterException;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarImporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.sugar.GlycoconjugateException;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.MolecularFramework.util.similiarity.SearchEngine.SearchEngine;
import org.eurocarbdb.MolecularFramework.util.similiarity.SearchEngine.SearchEngineException;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorException;
import org.glygen.array.config.SesameTransactionConfig;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.GlycanSearchResultEntity;
import org.glygen.array.persistence.dao.GlycanSearchResultRepository;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.GlycanSequenceFormat;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.service.ArrayDatasetRepository;
import org.glygen.array.service.GlycanRepository;
import org.glygen.array.service.GlygenArrayRepository;
import org.glygen.array.util.GlytoucanUtil;
import org.glygen.array.util.SequenceUtils;
import org.glygen.array.view.CompareByGlytoucanId;
import org.glygen.array.view.CompareByMass;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.glygen.array.view.GlycanSearchInput;
import org.glygen.array.view.GlycanSearchResult;
import org.glygen.array.view.GlycanSearchResultView;
import org.glygen.array.view.GlycanSearchType;
import org.glygen.array.view.Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Import(SesameTransactionConfig.class)
@RestController
@RequestMapping("/array/public/search")
public class SearchController {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    @Autowired
    @Qualifier("glygenArrayRepositoryImpl")
    GlygenArrayRepository repository;
    
    @Autowired
    GlycanRepository glycanRepository;
    
    @Autowired
    GlycanSearchResultRepository searchResultRepository;
    
    @Autowired
    ArrayDatasetRepository datasetRepository;
    
    @Value("${spring.file.imagedirectory}")
    String imageLocation;
    
    
    @ApiOperation(value = "Perform search on glycans that match all of the given criteria")
    @RequestMapping(value="/searchGlycans", method = RequestMethod.POST)
    @ApiResponses (value ={@ApiResponse(code=200, message="The search id to be used to retrieve search results", response = String.class), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public String searchGlycans (
            @ApiParam(required=true, value="search terms") 
            @RequestBody GlycanSearchInput searchInput) {
        
        Map<String, List<String>> searchResultMap = new HashMap<String, List<String>>();
        try {
            if (searchInput.getGlytoucanIds() != null && !searchInput.getGlytoucanIds().isEmpty()) {
                List<String> matches = glycanRepository.getGlycanByGlytoucanIds(null, searchInput.getGlytoucanIds());
                List<String> matchedIds = new ArrayList<String>();
                for (String m: matches) {
                    matchedIds.add(m.substring(m.lastIndexOf("/")+1));
                }
                searchResultMap.put(searchInput.getGlytoucanIds().hashCode()+"glytoucan", matchedIds);
            }
            if (searchInput.getMinMass() != null && searchInput.getMaxMass() != null) {
                List<String> matches = glycanRepository.getGlycanByMass(null, searchInput.getMinMass(), searchInput.getMaxMass());
                List<String> matchedIds = new ArrayList<String>();
                for (String m: matches) {
                    matchedIds.add(m.substring(m.lastIndexOf("/")+1));
                }
                searchResultMap.put(searchInput.getMinMass()+"mass"+searchInput.getMaxMass(), matchedIds);
            }
            
            Set<String> finalMatches = new HashSet<String>();
            int i=0;
            String searchKey = "";
            for (String key: searchResultMap.keySet()) {
                searchKey += key;
                List<String> matches = searchResultMap.get(key);
                if (i == 0)
                    finalMatches.addAll(matches);
                else {
                    // get the intersection
                    finalMatches = matches.stream()
                            .distinct()
                            .filter(finalMatches::contains)
                            .collect(Collectors.toSet());
                    
                }
                i++;
            }
            
            if (!searchKey.isEmpty()) {
                GlycanSearchResultEntity searchResult = new GlycanSearchResultEntity();
                searchResult.setSequence(searchKey);
                searchResult.setIdList(String.join(",", finalMatches));
                searchResult.setSearchType(GlycanSearchType.COMBINED.name());
                try {
                    searchResult.setInput(new ObjectMapper().writeValueAsString(searchInput));
                } catch (JsonProcessingException e) {
                    logger.warn("could not serialize the search input" + e.getMessage());
                }
                searchResultRepository.save(searchResult);
                return searchResult.getSequence();
            } else {
                return null;
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve glycans for user. Reason: " + e.getMessage());
        } 
    }
    
    @ApiOperation(value = "Perform search on glycans that match one of the given glytoucan ids")
    @RequestMapping(value="/searchGlycansByGlytoucanIds", method = RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(code=200, message="The search id to be used to retrieve search results", response = String.class), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public String listGlycansByGlytoucanIds (
            @ApiParam(required=true, value="list of glytoucan ids to match") 
            @RequestParam(value="glytoucanids", required=true) List<String> ids) {
        try {
            List<String> matches = glycanRepository.getGlycanByGlytoucanIds(null, ids);
            List<String> matchedIds = new ArrayList<String>();
            for (String m: matches) {
                matchedIds.add(m.substring(m.lastIndexOf("/")+1));
            }
            try {
                GlycanSearchResultEntity searchResult = new GlycanSearchResultEntity();
                searchResult.setSequence(ids.hashCode()+"glytoucan");
                searchResult.setIdList(String.join(",", matchedIds));
                searchResult.setSearchType(GlycanSearchType.IDLIST.name());
                try {
                    GlycanSearchInput searchInput = new GlycanSearchInput();
                    searchInput.setGlytoucanIds(ids);
                    searchResult.setInput(new ObjectMapper().writeValueAsString(searchInput));
                } catch (JsonProcessingException e) {
                    logger.warn("could not serialize the search input" + e.getMessage());
                }
                searchResultRepository.save(searchResult);
                return searchResult.getSequence();
            } catch (Exception e) {
                logger.error("Cannot save the search result", e);
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve glycans for user. Reason: " + e.getMessage());
        }
        
        return null;
    }
    
    @ApiOperation(value = "Perform search on glycans that have masses in the given range")
    @RequestMapping(value="/searchGlycansByMass", method = RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(code=200, message="The search id to be used to retrieve search results", response = String.class), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public String listGlycansByMassRange (
            @ApiParam(required=true, value="minimum mass") 
            @RequestParam(value="min", required=true) Double min,
            @ApiParam(required=true, value="maximum mass") 
            @RequestParam(value="max", required=true) Double max) {
        try {
            List<String> matches = glycanRepository.getGlycanByMass(null, min, max);
            List<String> matchedIds = new ArrayList<String>();
            for (String m: matches) {
                matchedIds.add(m.substring(m.lastIndexOf("/")+1));
            }
            try {
                GlycanSearchResultEntity searchResult = new GlycanSearchResultEntity();
                searchResult.setSequence(min+"mass"+max);
                searchResult.setIdList(String.join(",", matchedIds));
                searchResult.setSearchType(GlycanSearchType.MASS.name());
                try {
                    GlycanSearchInput searchInput = new GlycanSearchInput();
                    searchInput.setMinMass(min);
                    searchInput.setMaxMass(max);
                    searchResult.setInput(new ObjectMapper().writeValueAsString(searchInput));
                } catch (JsonProcessingException e) {
                    logger.warn("could not serialize the search input" + e.getMessage());
                }
                searchResultRepository.save(searchResult);
                return searchResult.getSequence();
            } catch (Exception e) {
                logger.error("Cannot save the search result", e);
            }
            
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve glycans for user. Reason: " + e.getMessage());
        }
        
        return null;
    }
    
    @ApiOperation(value = "Perform search on glycans that match the given structure")
    @RequestMapping(value="/searchGlycansByStructure", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="The search id to be used to retrieve search results", response = String.class), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public String listGlycansByStructure (
            @ApiParam(required=true, value="structure to match") 
            @RequestBody String sequence,
            @ApiParam(required=true, value="sequence format", allowableValues="Wurcs, GlycoCT, IUPAC, GlycoWorkbench") 
            @RequestParam(value="sequenceFormat", required=true) String sequenceFormat) {
        
        try {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            
            String searchSequence = SequenceUtils.parseSequence(errorMessage, sequence, sequenceFormat);
            
            if (errorMessage != null && errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                throw new IllegalArgumentException("Error in the arguments", errorMessage);
            }
            
            String glycanURI = glycanRepository.getGlycanBySequence(searchSequence);  
            if (glycanURI != null) {
                try {
                    List<String> matches = new ArrayList<String>();
                    matches.add(glycanURI.substring(glycanURI.lastIndexOf("/")+1));
                    GlycanSearchResultEntity searchResult = new GlycanSearchResultEntity();
                    searchResult.setSequence(searchSequence.hashCode()+"structure");
                    searchResult.setIdList(String.join(",", matches));
                    searchResult.setSearchType(GlycanSearchType.STRUCTURE.name());
                    try {
                        GlycanSearchInput searchInput = new GlycanSearchInput();
                        Sequence s = new Sequence();
                        s.setSequence(sequence);
                        s.setFormat(GlycanSequenceFormat.forValue(sequenceFormat));
                        searchInput.setStructure(s);
                        searchResult.setInput(new ObjectMapper().writeValueAsString(searchInput));
                    } catch (JsonProcessingException e) {
                        logger.warn("could not serialize the search input" + e.getMessage());
                    }
                    searchResultRepository.save(searchResult);
                    return searchResult.getSequence();
                } catch (Exception e) {
                    logger.error("Cannot save the search result", e);
                }
            } 
        } catch (SparqlException e) {
            throw new GlycanRepositoryException("Cannot retrieve glycans for user. Reason: " + e.getMessage());
        }
        
        return null;
    }
    
    
    @ApiOperation(value = "Perform search on glycans that match the given substructure and return the search id")
    @RequestMapping(value="/searchGlycansBySubstructure", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="The search id to be used to retrieve search results", response = String.class), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public String listGlycanBySubstructure (
            @ApiParam(required=true, value="substructure to match") 
            @RequestBody String sequence,
            @ApiParam(required=true, value="sequence format", allowableValues="Wurcs, GlycoCT, IUPAC, GlycoWorkbench") 
            @RequestParam(value="sequenceFormat", required=true) String sequenceFormat, 
            @ApiParam(required=false, defaultValue = "false", value="restrict search to reducing end") 
            @RequestParam(value="reducingEnd", defaultValue = "false", required=false)
            Boolean reducingEnd) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        
        String searchSequence = SequenceUtils.parseSequence(errorMessage, sequence, sequenceFormat);
        
        if (errorMessage != null && errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
            throw new IllegalArgumentException("Error in the arguments", errorMessage);
        }
        
        try {
            List<SequenceDefinedGlycan> glycans = glycanRepository.getAllSequenceDefinedGlycans();
            List<String> matches = null;
            try {
                matches = subStructureSearch(searchSequence, glycans, reducingEnd);
                try {
                    GlycanSearchResultEntity searchResult = new GlycanSearchResultEntity();
                    searchResult.setSequence(searchSequence.hashCode()+"substructure");
                    searchResult.setIdList(String.join(",", matches));
                    searchResult.setSearchType(GlycanSearchType.SUBSTRUCTURE.name());
                    try {
                        GlycanSearchInput searchInput = new GlycanSearchInput();
                        Sequence s = new Sequence();
                        s.setSequence(sequence);
                        s.setFormat(GlycanSequenceFormat.forValue(sequenceFormat));
                        s.setReducingEnd(reducingEnd);
                        searchInput.setStructure(s);
                        searchResult.setInput(new ObjectMapper().writeValueAsString(searchInput));
                    } catch (JsonProcessingException e) {
                        logger.warn("could not serialize the search input" + e.getMessage());
                    }
                    searchResultRepository.save(searchResult);
                    return searchResult.getSequence();
                } catch (Exception e) {
                    logger.error("Cannot save the search result", e);
                }
            } catch (SugarImporterException | GlycoVisitorException | GlycoconjugateException
                        | SearchEngineException e1) {
                    errorMessage.addError(new ObjectError ("search", e1.getMessage()));
                    throw new IllegalArgumentException("Error during search", errorMessage);
    
            }
        } catch (SparqlException e) {
            throw new GlycanRepositoryException("Cannot retrieve glycans for user. Reason: " + e.getMessage());
        }
        return null;
    }
    
    
    @ApiOperation(value = "List glycans from the given search")
    @RequestMapping(value="/listGlycansForSearch", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Glycans retrieved successfully", response = GlycanSearchResultView.class), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public GlycanSearchResultView listGlycansForSearch (
            @ApiParam(required=true, value="offset for pagination, start from 0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of glycans to be retrieved") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=true, value="the search query id retrieved earlier by the corresponding search") 
            @RequestParam(value="searchId", required=true) String searchId) {
        GlycanSearchResultView result = new GlycanSearchResultView();
        try {
            if (offset == null)
                offset = 0;
            if (limit == null)
                limit = -1;
            if (field == null)
                field = "id";
            if (order == null)
                order = 0; // DESC
            
            if (order != 0 && order != 1) {
                ErrorMessage errorMessage = new ErrorMessage("Order should be 0 (Descending) or 1 (Ascending)");
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("order", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Order should be 0 or 1", errorMessage);
            }
            
            ErrorMessage errorMessage = new ErrorMessage("Retrieval of search results failed");
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            
            int total = glycanRepository.getGlycanCountByUser (null);
            
            List<GlycanSearchResult> searchGlycans = new ArrayList<>();
            List<String> matches = null;
            
            String idList = null;
            try {
                GlycanSearchResultEntity r = searchResultRepository.findBySequence(searchId);
                if (r != null) {
                    idList = r.getIdList();
                    result.setType(GlycanSearchType.valueOf(r.getSearchType()));
                    result.setInput(new ObjectMapper().readValue(r.getInput(), GlycanSearchInput.class));
                }
            } catch (Exception e) {
                logger.error("Cannot retrieve the search result", e);
            }
            if (idList != null) {
                matches = Arrays.asList(idList.split(","));  
            } else {
                errorMessage.addError(new ObjectError ("searchId", "NotFound"));
                throw new IllegalArgumentException("Search id should be obtained by a previous web service call", errorMessage);
            }
            
            if (matches != null) {
                List<Glycan> loadedGlycans = new ArrayList<Glycan>();
                for (String match: matches) {
                    Glycan glycan = glycanRepository.getGlycanById(match, null);
                    if (glycan != null)
                        loadedGlycans.add(glycan);
                }
                // sort the glycans by the given order
                if (field == null || field.equalsIgnoreCase("name")) {
                    if (order == 1)
                        loadedGlycans.sort(Comparator.comparing(Glycan::getName));
                    else 
                        loadedGlycans.sort(Comparator.comparing(Glycan::getName).reversed());
                } else if (field.equalsIgnoreCase("comment")) {
                    if (order == 1)
                        loadedGlycans.sort(Comparator.comparing(Glycan::getDescription));
                    else 
                        loadedGlycans.sort(Comparator.comparing(Glycan::getDescription).reversed());
                } else if (field.equalsIgnoreCase("glytoucanId")) {
                    if (order == 1)
                        loadedGlycans.sort(new CompareByGlytoucanId());
                    else 
                        loadedGlycans.sort(new CompareByGlytoucanId().reversed());
                } else if (field.equalsIgnoreCase("dateModified")) {
                    if (order == 1)
                        loadedGlycans.sort(Comparator.comparing(Glycan::getDateModified));
                    else 
                        loadedGlycans.sort(Comparator.comparing(Glycan::getDateModified).reversed());
                } else if (field.equalsIgnoreCase("mass")) {
                    if (order == 1)
                        loadedGlycans.sort(new CompareByMass());
                    else 
                        loadedGlycans.sort(new CompareByMass().reversed());
                } else if (field.equalsIgnoreCase("id")) {
                    if (order == 1)
                        loadedGlycans.sort(Comparator.comparing(Glycan::getId));
                    else 
                        loadedGlycans.sort(Comparator.comparing(Glycan::getId).reversed());
                }
                int i=0;
                int added = 0;
                for (Glycan glycan: loadedGlycans) {
                    i++;
                    if (i <= offset) continue;
                    int count = datasetRepository.getDatasetCountByGlycan(glycan.getId(), null);
                    GlycanSearchResult r = new GlycanSearchResult();
                    r.setDatasetCount(count);
                    r.setGlycan(glycan);
                    searchGlycans.add(r);
                    added ++;
                    if (glycan instanceof SequenceDefinedGlycan) {
                        byte[] image = GlygenArrayController.getCartoonForGlycan(glycan.getId(), imageLocation);
                        if (image == null && ((SequenceDefinedGlycan) glycan).getSequence() != null) {
                            BufferedImage t_image = GlygenArrayController.createImageForGlycan ((SequenceDefinedGlycan) glycan);
                            if (t_image != null) {
                                String filename = glycan.getId() + ".png";
                                //save the image into a file
                                logger.debug("Adding image to " + imageLocation);
                                File imageFile = new File(imageLocation + File.separator + filename);
                                try {
                                    ImageIO.write(t_image, "png", imageFile);
                                } catch (IOException e) {
                                    logger.error ("Glycan image cannot be written", e);
                                }
                            }
                            image = GlygenArrayController.getCartoonForGlycan(glycan.getId(), imageLocation);
                        }
                        glycan.setCartoon(image);
                    }
                    
                    if (added >= limit) break;
                    
                }
            }
            
            result.setRows(searchGlycans);
            result.setTotal(total);
            result.setFilteredTotal(searchGlycans.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve glycans for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    public List<String> subStructureSearch(String structure, List<SequenceDefinedGlycan> structures, 
            boolean reducingEnd)
            throws SugarImporterException, GlycoVisitorException, GlycoconjugateException, SearchEngineException {
        SugarImporterGlycoCTCondensed t_importer = new SugarImporterGlycoCTCondensed();
        Sugar t_sugarStructure = null;
        List<String> matches = new ArrayList<String>();
    
        SearchEngine search = new SearchEngine ();
        // parse the sequence
        t_sugarStructure = t_importer.parse(structure);
        search.setQueryStructure(t_sugarStructure);
        
        if (reducingEnd){
            search.restrictToReducingEnds();
        }
        
        // test for each structure
        for (SequenceDefinedGlycan s: structures) {
            Sugar existingStructure = null;
        
            if (s.getSequenceType() == GlycanSequenceFormat.GLYCOCT) {
                existingStructure = t_importer.parse(s.getSequence());
            } else if (s.getSequenceType() == GlycanSequenceFormat.WURCS) {
                try {
                    existingStructure = GlytoucanUtil.getSugarFromWURCS(s.getSequence());
                } catch (Exception e) {
                    logger.debug("cannot convert " + s.getId() + "'s sequence to GlycoCT");
                }
            }
            if (existingStructure != null) {
                search.setQueriedStructure(existingStructure);
                search.match();
                if (search.isExactMatch())
                {
                    // found a match, return
                    logger.debug("Found a match: {}", s.getId());
                    matches.add(s.getId());
                }
            }
        }
        
        return matches;
    }

}
