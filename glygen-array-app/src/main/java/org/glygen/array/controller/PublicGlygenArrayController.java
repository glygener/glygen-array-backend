package org.glygen.array.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.persistence.EntityNotFoundException;
import javax.validation.Validator;

import org.glygen.array.config.SesameTransactionConfig;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.GPLinkedGlycoPeptide;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.GlycanInFeature;
import org.glygen.array.persistence.rdf.GlycanType;
import org.glygen.array.persistence.rdf.GlycoLipid;
import org.glygen.array.persistence.rdf.GlycoPeptide;
import org.glygen.array.persistence.rdf.GlycoProtein;
import org.glygen.array.persistence.rdf.LinkedGlycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.data.ArrayDataset;
import org.glygen.array.persistence.rdf.data.FileWrapper;
import org.glygen.array.persistence.rdf.data.Image;
import org.glygen.array.persistence.rdf.data.IntensityData;
import org.glygen.array.persistence.rdf.data.PrintedSlide;
import org.glygen.array.persistence.rdf.data.ProcessedData;
import org.glygen.array.persistence.rdf.data.RawData;
import org.glygen.array.persistence.rdf.data.Slide;
import org.glygen.array.persistence.rdf.metadata.AssayMetadata;
import org.glygen.array.persistence.rdf.metadata.DataProcessingSoftware;
import org.glygen.array.persistence.rdf.metadata.ImageAnalysisSoftware;
import org.glygen.array.persistence.rdf.metadata.MetadataCategory;
import org.glygen.array.persistence.rdf.metadata.PrintRun;
import org.glygen.array.persistence.rdf.metadata.Printer;
import org.glygen.array.persistence.rdf.metadata.Sample;
import org.glygen.array.persistence.rdf.metadata.ScannerMetadata;
import org.glygen.array.persistence.rdf.metadata.SlideMetadata;
import org.glygen.array.persistence.rdf.metadata.SpotMetadata;
import org.glygen.array.service.ArrayDatasetRepository;
import org.glygen.array.service.FeatureRepository;
import org.glygen.array.service.GlycanRepository;
import org.glygen.array.service.GlycanRepositoryImpl;
import org.glygen.array.service.GlygenArrayRepository;
import org.glygen.array.service.GlygenArrayRepositoryImpl;
import org.glygen.array.service.LayoutRepository;
import org.glygen.array.service.LinkerRepository;
import org.glygen.array.service.MetadataRepository;
import org.glygen.array.service.QueryHelper;
import org.glygen.array.util.ExtendedGalFileParser;
import org.glygen.array.util.MetadataImportExportUtil;
import org.glygen.array.util.parser.ProcessedDataParser;
import org.glygen.array.view.ArrayDatasetListView;
import org.glygen.array.view.BlockLayoutResultView;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.glygen.array.view.FeatureListResultView;
import org.glygen.array.view.GlycanListResultView;
import org.glygen.array.view.IntensityDataResultView;
import org.glygen.array.view.LinkerListResultView;
import org.glygen.array.view.MetadataListResultView;
import org.glygen.array.view.PrintedSlideListView;
import org.glygen.array.view.SlideLayoutResultView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Import(SesameTransactionConfig.class)
@RestController
@RequestMapping("/array/public")
public class PublicGlygenArrayController {
    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    @Autowired
    @Qualifier("glygenArrayRepositoryImpl")
    GlygenArrayRepository repository;
    
    @Autowired
    GlycanRepository glycanRepository;
    
    @Autowired
    LinkerRepository linkerRepository;
    
    @Autowired
    LayoutRepository layoutRepository;
    
    @Autowired
    FeatureRepository featureRepository;
    
    @Autowired
    UserRepository userRepository;
    
    @Autowired
    ArrayDatasetRepository datasetRepository;
    
    @Autowired
    MetadataRepository metadataRepository;
    
    @Autowired
    Validator validator;
    
    @Value("${spring.file.imagedirectory}")
    String imageLocation;
    
    @Autowired
    ResourceLoader resourceLoader;
    
    @Autowired
    QueryHelper queryHelper;
    
    @Value("${spring.file.uploaddirectory}")
    String uploadDir;
    
    @Autowired
    ExtendedGalFileParser galFileParser;
    
    @Value("${glygen.frontend.scheme}")
    String scheme;
    
    @Value("${glygen.frontend.host}")
    String host;
    
    @Value("${glygen.frontend.basePath}")
    String basePath;
    
    @Operation(summary = "List all public glycans")
    @RequestMapping(value="/listGlycans", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Glycans retrieved successfully", content = {
            @Content( schema = @Schema(implementation = GlycanListResultView.class))}), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public GlycanListResultView listGlycans (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of glycans to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue) {
        GlycanListResultView result = new GlycanListResultView();
        
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
            
            int total = glycanRepository.getGlycanCountByUser (null, searchValue);
            
            List<Glycan> glycans = glycanRepository.getGlycanByUser(null, offset, limit, field, order, searchValue);
            for (Glycan glycan : glycans) {
                if (glycan.getType().equals(GlycanType.SEQUENCE_DEFINED)) {
                    byte[] image = GlygenArrayController.getCartoonForGlycan(glycan.getId(), imageLocation);
                    if (image == null && ((SequenceDefinedGlycan) glycan).getSequence() != null) {
                        // try to create one
                        BufferedImage t_image = GlygenArrayController.createImageForGlycan((SequenceDefinedGlycan) glycan);
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
            }
            
            result.setRows(glycans);
            result.setTotal(total);
            result.setFilteredTotal(glycans.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve glycans. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "Retrieve glycan with the given id")
    @RequestMapping(value="/getglycan/{glycanId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Glycan retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Gycan with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Glycan getGlycan (
            @Parameter(required=true, description="id of the glycan to retrieve") 
            @PathVariable("glycanId") String glycanId) {
        try {
            
            Glycan glycan = glycanRepository.getGlycanById(glycanId.trim(), null);
            if (glycan == null) {
                throw new EntityNotFoundException("Glycan with id : " + glycanId + " does not exist in the repository");
            }
            byte[] cartoon = GlygenArrayController.getCartoonForGlycan(glycanId.trim(), imageLocation);
            glycan.setCartoon(cartoon);
            return glycan;
            
            
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Glycan cannot be retrieved", e);
        }
        
    }
    
    @Operation(summary = "Retrieve datasets that include the glycan with the given id")
    @RequestMapping(value="/getdatasetforglycan/{glycanId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Datasets retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Gycan with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ArrayDatasetListView getDatasetsForGlycan (
            @Parameter(required=true, description="id of the glycan to retrieve") 
            @PathVariable("glycanId") String glycanId,
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order) {
        ArrayDatasetListView result = new ArrayDatasetListView();
        
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
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("order", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Order should be 0 or 1", errorMessage);
            }
            int total = datasetRepository.getDatasetCountByGlycan(glycanId.trim(), null);
            
            List<ArrayDataset> resultList = datasetRepository.getDatasetByGlycan (glycanId.trim(), offset, limit, field, order, false, null);
            // need to clear rawdata, processed data etc.
            for (ArrayDataset dataset: resultList) {
                dataset.setSlides(null);
            }
            result.setRows(resultList); 
            result.setTotal(total);
            result.setFilteredTotal(resultList.size());
            return result;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve datasets. Reason: " + e.getMessage());
        }
    }
    
    @Operation(summary = "Retrieve linker with the given id")
    @RequestMapping(value="/getlinker/{linkerId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Linker retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Linker with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Linker getLinker (
            @Parameter(required=true, description="id of the linker to retrieve") 
            @PathVariable("linkerId") String linkerId) {
        try {
            Linker linker = linkerRepository.getLinkerById(linkerId.trim(), null);
            if (linker == null) {
                throw new EntityNotFoundException("Linker with id : " + linkerId + " does not exist in the repository");
            }
            return linker;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Linker cannot be retrieved", e);
        }
        
    }
    
    @Operation(summary = "List all public linkers")
    @RequestMapping(value="/listLinkers", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Linkers retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public LinkerListResultView listLinkers (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of linkers to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue) {
        LinkerListResultView result = new LinkerListResultView();
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
            
            int total = linkerRepository.getLinkerCountByUser (null, searchValue);
            
            List<Linker> linkers = linkerRepository.getLinkerByUser(null, offset, limit, field, order, searchValue);
            result.setRows(linkers);
            result.setTotal(total);
            result.setFilteredTotal(linkers.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve linkers for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all public slide layouts")
    @RequestMapping(value="/listSlidelayouts", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Slide layouts retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public SlideLayoutResultView listSlideLayouts (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of layouts to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter (required=false, schema = @Schema(type = "boolean", defaultValue="true"), description="if false, do not load block details. Default is true (to load all)")
            @RequestParam(required=false, defaultValue = "true", value="loadAll") Boolean loadAll, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue) {
        SlideLayoutResultView result = new SlideLayoutResultView();
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
            
            int total = layoutRepository.getSlideLayoutCountByUser (null, searchValue);
            List<SlideLayout> layouts = layoutRepository.getSlideLayoutByUser(null, offset, limit, field, loadAll, order, searchValue);
            result.setRows(layouts);
            result.setTotal(total);
            result.setFilteredTotal(layouts.size());
            
            if (loadAll) {
                // populate glycan images
                for (SlideLayout layout: layouts) {
                    if (layout.getBlocks() != null) {
                        for (org.glygen.array.persistence.rdf.Block block: layout.getBlocks()) {
                            if (block.getBlockLayout() != null) {
                                if (block.getBlockLayout().getSpots() == null)
                                    continue;
                                for (org.glygen.array.persistence.rdf.Spot s: block.getBlockLayout().getSpots()) {
                                    if (s.getFeatures() == null)
                                        continue;
                                    for (org.glygen.array.persistence.rdf.Feature f: s.getFeatures()) {
                                        GlygenArrayController.populateFeatureGlycanImages(f, imageLocation);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve slide layouts. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all public block layouts")
    @RequestMapping(value="/listBlocklayouts", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Block layouts retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))}),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public BlockLayoutResultView listBlockLayouts (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of layouts to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter (required=false, schema = @Schema(type = "boolean", defaultValue="true"), description="if false, do not load spot details. Default is true (to load all)")
            @RequestParam(required=false, defaultValue = "true", value="loadAll") Boolean loadAll, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue) {
        BlockLayoutResultView result = new BlockLayoutResultView();
        
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
            
            int total = layoutRepository.getBlockLayoutCountByUser (null, searchValue);
            List<BlockLayout> layouts = layoutRepository.getBlockLayoutByUser(null, offset, limit, field, loadAll, order, searchValue);
            result.setRows(layouts);
            result.setTotal(total);
            result.setFilteredTotal(layouts.size());
            
            if (loadAll) {
                // populate glycan images
                for (BlockLayout b: layouts) {
                    if (b.getSpots() == null)
                        continue;
                    for (org.glygen.array.persistence.rdf.Spot s: b.getSpots()) {
                        if (s.getFeatures() == null) 
                            continue;
                        for (org.glygen.array.persistence.rdf.Feature f: s.getFeatures()) {
                            GlygenArrayController.populateFeatureGlycanImages(f, imageLocation);
                        }
                    }
                }
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve block layouts. Reason: " + e.getMessage());
        }
        
        return result;
    }

    @Operation(summary = "List all public features")
    @RequestMapping(value="/listFeatures", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Features retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public FeatureListResultView listFeature (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of features to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue) {
        FeatureListResultView result = new FeatureListResultView();
       
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
            
            int total = featureRepository.getFeatureCountByUser (null, searchValue);
            
            List<org.glygen.array.persistence.rdf.Feature> features = featureRepository.getFeatureByUser(null, offset, limit, field, order, searchValue);
            result.setRows(features);
            result.setTotal(total);
            result.setFilteredTotal(features.size());
            
            // get cartoons for the glycans
            for (org.glygen.array.persistence.rdf.Feature f: features) {
                GlygenArrayController.populateFeatureGlycanImages(f, imageLocation);
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve features. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all public datasets")
    @RequestMapping(value="/listArrayDataset", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Array datasets retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public ArrayDatasetListView listArrayDataset (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="load rawdata and processed data details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue) {
        ArrayDatasetListView result = new ArrayDatasetListView();
        //logger.info("getting the list of array datasets");
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
            
            int total = datasetRepository.getArrayDatasetCountByUser(null, searchValue);
            
            List<ArrayDataset> resultList = datasetRepository.getArrayDatasetByUser(null, offset, limit, field, order, searchValue, loadAll);
            for (ArrayDataset d: resultList) {
                if (!loadAll) {
                    d.setSlides(null);
                }
            }
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(resultList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve array datasets. Reason: " + e.getMessage());
        }
        
        //logger.info("returning the list of array datasets");
        return result;
    }
    
    @Operation(summary = "List all public datasets submitted by the given user")
    @RequestMapping(value="/listArrayDatasetByUser", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Array datasets retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public ArrayDatasetListView listArrayDatasetByUser (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="load rawdata and processed data details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="false") Boolean loadAll, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue,
            @Parameter(required=true, description="user name") 
            @RequestParam("user") String username) {
        ArrayDatasetListView result = new ArrayDatasetListView();
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
            
            List<ArrayDataset> resultList = new ArrayList<ArrayDataset>();
            
            List<SparqlEntity> results = queryHelper.retrieveDatasetByOwner(username.trim(), GlygenArrayRepository.DEFAULT_GRAPH);
            int total = results.size();
            if (results != null) {
                for (SparqlEntity r: results) {
                    String m = r.getValue("s");
                    resultList.add(datasetRepository.getArrayDataset(m.substring(m.lastIndexOf("/")+1), loadAll, null));
                }
                // sort the datasets by the given order
                if (field == null || field.equalsIgnoreCase("name")) {
                    if (order == 1)
                        resultList.sort(Comparator.comparing(ArrayDataset::getName));
                    else 
                        resultList.sort(Comparator.comparing(ArrayDataset::getName).reversed());
                } else if (field.equalsIgnoreCase("comment")) {
                    if (order == 1)
                        resultList.sort(Comparator.comparing(ArrayDataset::getDescription));
                    else 
                        resultList.sort(Comparator.comparing(ArrayDataset::getDescription).reversed());
                } else if (field.equalsIgnoreCase("dateModified")) {
                    if (order == 1)
                        resultList.sort(Comparator.comparing(ArrayDataset::getDateModified));
                    else 
                        resultList.sort(Comparator.comparing(ArrayDataset::getDateModified).reversed());
                } else if (field.equalsIgnoreCase("id")) {
                    if (order == 1)
                        resultList.sort(Comparator.comparing(ArrayDataset::getId));
                    else 
                        resultList.sort(Comparator.comparing(ArrayDataset::getId).reversed());
                }
                
            }
            
            int i=0;
            int added = 0;
            List<ArrayDataset> searchDatasets = new ArrayList<ArrayDataset>();
            for (ArrayDataset dataset: resultList) {
                i++;
                if (i <= offset) continue;
                searchDatasets.add(dataset);
                added ++;
                
                if (limit != -1 && added >= limit) break;
                
            }
    
            result.setRows(searchDatasets);
            result.setTotal(total);
            result.setFilteredTotal(searchDatasets.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve array datasets for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    
    @Operation(summary = "List all public datasets submitted by the given user as a coowner")
    @RequestMapping(value="/listArrayDatasetByCoOwner", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Array datasets retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public ArrayDatasetListView listArrayDatasetByCoOwner (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="load rawdata and processed data details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="false") Boolean loadAll, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue,
            @Parameter(required=true, description="coowner name") 
            @RequestParam("user") String username) {
        ArrayDatasetListView result = new ArrayDatasetListView();
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
            
            List<ArrayDataset> resultList = new ArrayList<ArrayDataset>();
            UserEntity user = userRepository.findByUsernameIgnoreCase(username);
            // get datasets co-owned
            List<ArrayDataset> coowned = datasetRepository.getArrayDatasetByCoOwner(user, 0, -1, null, 0, null, false);
            for (ArrayDataset d: coowned) {
                if (d.getIsPublic()) {
                    resultList.add(datasetRepository.getArrayDataset(d.getPublicId(), loadAll, null));
                }
            }
            int total = resultList.size();
            
            // sort the datasets by the given order
            if (field == null || field.equalsIgnoreCase("name")) {
                if (order == 1)
                    resultList.sort(Comparator.comparing(ArrayDataset::getName));
                else 
                    resultList.sort(Comparator.comparing(ArrayDataset::getName).reversed());
            } else if (field.equalsIgnoreCase("comment")) {
                if (order == 1)
                    resultList.sort(Comparator.comparing(ArrayDataset::getDescription));
                else 
                    resultList.sort(Comparator.comparing(ArrayDataset::getDescription).reversed());
            } else if (field.equalsIgnoreCase("dateModified")) {
                if (order == 1)
                    resultList.sort(Comparator.comparing(ArrayDataset::getDateModified));
                else 
                    resultList.sort(Comparator.comparing(ArrayDataset::getDateModified).reversed());
            } else if (field.equalsIgnoreCase("id")) {
                if (order == 1)
                    resultList.sort(Comparator.comparing(ArrayDataset::getId));
                else 
                    resultList.sort(Comparator.comparing(ArrayDataset::getId).reversed());
            }
                
            int i=0;
            int added = 0;
            List<ArrayDataset> searchDatasets = new ArrayList<ArrayDataset>();
            for (ArrayDataset dataset: resultList) {
                i++;
                if (i <= offset) continue;
                searchDatasets.add(dataset);
                added ++;
                
                if (limit != -1 && added >= limit) break;
                
            }
    
            result.setRows(searchDatasets);
            result.setTotal(total);
            result.setFilteredTotal(searchDatasets.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve array datasets for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all public printed slides")
    @RequestMapping(value="/listPrintedSlide", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Printed slides retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public PrintedSlideListView listPrintedSlide (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue) {
        PrintedSlideListView result = new PrintedSlideListView();
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
            
            int total = datasetRepository.getPrintedSlideCountByUser(null, searchValue);
            
            List<PrintedSlide> resultList = datasetRepository.getPrintedSlideByUser(null, offset, limit, field, order, searchValue);
            // clear unnecessary fields before sending the results back
            for (PrintedSlide slide: resultList) {
                if (slide.getLayout() != null)
                    slide.getLayout().setBlocks(null);
                if (slide.getPrinter() != null) {
                    slide.getPrinter().setDescriptorGroups(null);
                    slide.getPrinter().setDescriptors(null);
                }
                if (slide.getMetadata() != null) {
                    slide.getMetadata().setDescriptorGroups(null);
                    slide.getMetadata().setDescriptors(null);
                }
            }
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(resultList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve array datasets. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all public data processing software metadata")
    @RequestMapping(value="/listDataProcessingSoftware", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Data processing software metadata list retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public MetadataListResultView listDataProcessingSoftware (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue) {
        MetadataListResultView result = new MetadataListResultView();
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
            
            int total = metadataRepository.getDataProcessingSoftwareCountByUser(null, searchValue);
            
            List<DataProcessingSoftware> metadataList = metadataRepository.getDataProcessingSoftwareByUser(null, offset, limit, field, order, searchValue);
            List<MetadataCategory> resultList = new ArrayList<MetadataCategory>();
            resultList.addAll(metadataList);
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(metadataList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve data processing software. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all public image analysis software metadata")
    @RequestMapping(value="/listImageAnalysisSoftware", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Image analysis software metadata list retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public MetadataListResultView listImageAnalysisSoftware (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue) {
        MetadataListResultView result = new MetadataListResultView();
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
            
            int total = metadataRepository.getImageAnalysisSoftwareCountByUser(null, searchValue);
            
            List<ImageAnalysisSoftware> metadataList = metadataRepository.getImageAnalysisSoftwareByUser(null, offset, limit, field, order, searchValue);
            List<MetadataCategory> resultList = new ArrayList<MetadataCategory>();
            resultList.addAll(metadataList);
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(metadataList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve image analysis software. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all public printer metadata")
    @RequestMapping(value="/listPrinters", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Printer list retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public MetadataListResultView listPrinters (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue) {
        MetadataListResultView result = new MetadataListResultView();
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
            
            int total = metadataRepository.getPrinterCountByUser(null, searchValue);
            
            List<Printer> metadataList = metadataRepository.getPrinterByUser(null, offset, limit, field, order, searchValue);
            List<MetadataCategory> resultList = new ArrayList<MetadataCategory>();
            resultList.addAll(metadataList);
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(metadataList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve printers. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all public samples")
    @RequestMapping(value="/listSamples", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Samples retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public MetadataListResultView listSamples (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue) {
        MetadataListResultView result = new MetadataListResultView();
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
            
            int total = metadataRepository.getSampleCountByUser (null, searchValue);
            
            List<Sample> metadataList = metadataRepository.getSampleByUser(null, offset, limit, field, order, searchValue);
            List<MetadataCategory> resultList = new ArrayList<MetadataCategory>();
            resultList.addAll(metadataList);
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(metadataList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve samples. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all public scanner metadata")
    @RequestMapping(value="/listScanners", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Scanner list retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public MetadataListResultView listScanners (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue) {
        MetadataListResultView result = new MetadataListResultView();
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
            
            int total = metadataRepository.getScannerMetadataCountByUser(null, searchValue);
            
            List<ScannerMetadata> metadataList = metadataRepository.getScannerMetadataByUser(null, offset, limit, field, order, searchValue);
            List<MetadataCategory> resultList = new ArrayList<MetadataCategory>();
            resultList.addAll(metadataList);
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(metadataList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve scanners. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all public slide metadata")
    @RequestMapping(value="/listSlideMetadata", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Slide metadata list retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public MetadataListResultView listSlideMetadata (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue) {
        MetadataListResultView result = new MetadataListResultView();
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
            
            int total = metadataRepository.getSlideMetadataCountByUser (null, searchValue);
            
            List<SlideMetadata> metadataList = metadataRepository.getSlideMetadataByUser(null, offset, limit, field, order, searchValue);
            List<MetadataCategory> resultList = new ArrayList<MetadataCategory>();
            resultList.addAll(metadataList);
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(metadataList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve slide metadata. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all public assay metadata")
    @RequestMapping(value="/listAssayMetadata", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Assay metadata list retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public MetadataListResultView listAssayMetadata (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue) {
        MetadataListResultView result = new MetadataListResultView();
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
            
            int total = metadataRepository.getAssayMetadataCountByUser(null, searchValue);
            
            List<AssayMetadata> metadataList = metadataRepository.getAssayMetadataByUser(null, offset, limit, field, order, searchValue);
            List<MetadataCategory> resultList = new ArrayList<MetadataCategory>();
            resultList.addAll(metadataList);
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(metadataList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve assay metadata. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "List all public spot metadata")
    @RequestMapping(value="/listSpotMetadata", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Spot metadata list retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public MetadataListResultView listSpotMetadata (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="load descriptor details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue) {
        MetadataListResultView result = new MetadataListResultView();
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
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("order", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Order should be 0 or 1", errorMessage);
            }
            
            int total = metadataRepository.getSpotMetadataCountByUser(null, searchValue);
            
            List<SpotMetadata> metadataList = metadataRepository.getSpotMetadataByUser(null, offset, limit, field, order, searchValue, loadAll);
            List<MetadataCategory> resultList = new ArrayList<MetadataCategory>();
            resultList.addAll(metadataList);
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(metadataList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve spot metadata. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @Operation(summary = "Retrieve block layout with the given id")
    @RequestMapping(value="/getblocklayout/{layoutId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Block Layout retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Block layout with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public BlockLayout getBlockLayout (
            @Parameter(required=true, description="id of the block layout to retrieve") 
            @PathVariable("layoutId") String layoutId, 
            @Parameter (required=false, schema = @Schema(type = "boolean", defaultValue="true"), description="if false, do not load block details. Default is true (to load all)")
            @RequestParam(required=false, defaultValue = "true", value="loadAll") Boolean loadAll) {
        try {
            
            BlockLayout layout = layoutRepository.getBlockLayoutById(layoutId.trim(), null, loadAll);
            if (layout == null) {
                throw new EntityNotFoundException("Block layout with id : " + layoutId + " does not exist in the repository");
            }
            
            if (loadAll && layout.getSpots() != null) {        
                for (org.glygen.array.persistence.rdf.Spot s: layout.getSpots()) {
                    if (s.getFeatures() == null) 
                        continue;
                    for (org.glygen.array.persistence.rdf.Feature f: s.getFeatures()) {
                        GlygenArrayController.populateFeatureGlycanImages(f, imageLocation);
                    }
                }
            }
            
            return layout;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Block Layout cannot be retrieved", e);
        }
    }
    
    @Operation(summary = "Retrieve slide layout with the given id")
    @RequestMapping(value="/getslidelayout/{layoutId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Slide Layout retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Slide layout with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public SlideLayout getSlideLayout (
            @Parameter(required=true, description="id of the slide layout to retrieve") 
            @PathVariable("layoutId") String layoutId, 
            @Parameter (required=false, schema = @Schema(type = "boolean", defaultValue="true"), description="if false, do not load slide details. Default is true (to load all)")
            @RequestParam(required=false, defaultValue = "true", value="loadAll") Boolean loadAll) {
        try {
            SlideLayout layout = layoutRepository.getSlideLayoutById(layoutId.trim(), null, loadAll);
            if (layout == null) {
                throw new EntityNotFoundException("Slide layout with id : " + layoutId + " does not exist in the repository");
            }
            
            return layout;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Slide Layout cannot be retrieved. ", e);
        }
    }
    
    @Operation(summary = "Retrieve printed slide with the given id")
    @RequestMapping(value="/getprintedslide/{slideId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Printed slide retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Printed slide with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public PrintedSlide getPrintedSlide (
            @Parameter(required=true, description="id of the printed slide to retrieve") 
            @PathVariable("slideId") String slideId, 
            @Parameter (required=false, schema = @Schema(type = "boolean", defaultValue="false"), description="if false, do not load slide details. Default is true (to load all)")
            @RequestParam(required=false, defaultValue = "false", value="loadAll") Boolean loadAll) {
        try {
            PrintedSlide layout = datasetRepository.getPrintedSlideFromURI(GlycanRepositoryImpl.uriPrefixPublic + slideId.trim(), loadAll, null);
            if (layout == null) {
                throw new EntityNotFoundException("Printed slide with id : " + slideId + " does not exist in the repository");
            }
            
            return layout;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Printed slide cannot be retrieved", e);
        }
    }
    
    @Operation(summary = "Retrieve feature with the given id")
    @RequestMapping(value="/getfeature/{featureId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Feature retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Feature with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public org.glygen.array.persistence.rdf.Feature getFeature (
            @Parameter(required=true, description="id of the feature to retrieve") 
            @PathVariable("featureId") String featureId) {
        try {
            org.glygen.array.persistence.rdf.Feature feature = featureRepository.getFeatureById(featureId.trim(), null);
            if (feature == null) {
                throw new EntityNotFoundException("Feature with id : " + featureId + " does not exist in the repository");
            }
            
            GlygenArrayController.populateFeatureGlycanImages(feature, imageLocation);
            return feature;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Feature cannot be retrieved for user " , e);
        }
    }
    
    @Operation(summary = "Retrieve dataset with the given id")
    @RequestMapping(value="/getarraydataset/{datasetid}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Dataset retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Dataset with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ArrayDataset getArrayDataset (
            @Parameter(required=true, description="id of the array dataset to retrieve") 
            @PathVariable("datasetid") String id, 
            @Parameter(required=false, description="load rawdata and processed data measurements or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll) {
        try {
            ArrayDataset dataset = datasetRepository.getArrayDataset(id.trim(), loadAll, null);
            if (dataset == null) {
                throw new EntityNotFoundException("Array dataset with id : " + id + " does not exist in the repository");
            }
            
            return dataset;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Array dataset with id " + id + " cannot be retrieved", e);
        }   
    }
    
    @Operation(summary = "Retrieve sample with the given id")
    @RequestMapping(value="/getsample/{sampleId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Sample retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Sample with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Sample getSample (
            @Parameter(required=true, description="id of the sample to retrieve") 
            @PathVariable("sampleId") String id) {
        try {
            Sample sample = metadataRepository.getSampleFromURI(GlygenArrayRepository.uriPrefixPublic + id.trim(), null);
            if (sample == null) {
                throw new EntityNotFoundException("Sample with id : " + id + " does not exist in the repository");
            }
            return sample;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Sample cannot be retrieved", e);
        }   
    }
    
    @Operation(summary = "Retrieve printer with the given id")
    @RequestMapping(value="/getPrinter/{printerId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Printer retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Printer with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Printer getPrinter (
            @Parameter(required=true, description="id of the printer to retrieve") 
            @PathVariable("printerId") String id) {
        try {
            Printer metadata = metadataRepository.getPrinterFromURI(GlygenArrayRepository.uriPrefixPublic + id.trim(), null);
            if (metadata == null) {
                throw new EntityNotFoundException("Printer with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Printer cannot be retrieved", e);
        }   
    }

    @Operation(summary = "Retrieve printrun with the given id")
    @RequestMapping(value="/getPrintrun/{printrunId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Printrun retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Prinrun with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public PrintRun getPrintrun (
            @Parameter(required=true, description="id of the printrun to retrieve") 
            @PathVariable("printrunId") String id) {
        try {
            PrintRun metadata = metadataRepository.getPrintRunFromURI(GlygenArrayRepository.uriPrefixPublic + id.trim(), null);
            if (metadata == null) {
                throw new EntityNotFoundException("Printrun with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Printrun cannot be retrieved", e);
        }   
    }
    
    @Operation(summary = "Retrieve scanner with the given id")
    @RequestMapping(value="/getScanner/{scannerId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Scanner retrieved successfully"), 
            @ApiResponse(responseCode="404", description="ScannerMetadata with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ScannerMetadata getScanner (
            @Parameter(required=true, description="id of the ScannerMetadata to retrieve") 
            @PathVariable("scannerId") String id) {
        try {
            ScannerMetadata metadata = metadataRepository.getScannerMetadataFromURI(GlygenArrayRepository.uriPrefixPublic + id.trim(), null);
            if (metadata == null) {
                throw new EntityNotFoundException("ScannerMetadata with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("ScannerMetadata cannot be retrieved", e);
        }   
    }
    @Operation(summary = "Retrieve SlideMetadata with the given id")
    @RequestMapping(value="/getSlideMetadata/{slideId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Slide Metadata retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Slide Metadata with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public SlideMetadata getSlideMetadata (
            @Parameter(required=true, description="id of the Slide Metadata to retrieve") 
            @PathVariable("slideId") String id) {
        try {
            SlideMetadata metadata = metadataRepository.getSlideMetadataFromURI(GlygenArrayRepository.uriPrefixPublic + id.trim(), null);
            if (metadata == null) {
                throw new EntityNotFoundException("Slide Metadata with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Slide Metadata cannot be retrieved" , e);
        }   
    }
    
    @Operation(summary = "Retrieve SpotMetadata with the given id")
    @RequestMapping(value="/getSpotMetadata/{spotMetadataId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Spot Metadata retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Spot Metadata with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public SpotMetadata getSpotMetadata (
            @Parameter(required=true, description="id of the Spot Metadata to retrieve") 
            @PathVariable("spotMetadataId") String id) {
        try {
            SpotMetadata metadata = metadataRepository.getSpotMetadataFromURI(GlygenArrayRepository.uriPrefixPublic + id.trim(), null);
            if (metadata == null) {
                throw new EntityNotFoundException("Spot Metadata with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Spot Metadata cannot be retrieved" , e);
        }   
    }
    
    @Operation(summary = "Retrieve ImageAnalysisSoftware with the given id")
    @RequestMapping(value="/getImageAnalysisSoftware/{imagesoftwareId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="ImageAnalysisSoftware retrieved successfully"), 
            @ApiResponse(responseCode="404", description="ImageAnalysisSoftware with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ImageAnalysisSoftware getImageAnaylsisSoftware (
            @Parameter(required=true, description="id of the ImageAnalysisSoftware to retrieve") 
            @PathVariable("imagesoftwareId") String id) {
        try {
            ImageAnalysisSoftware metadata = metadataRepository.getImageAnalysisSoftwareFromURI(GlygenArrayRepository.uriPrefixPublic 
                    + id.trim(), null);
            if (metadata == null) {
                throw new EntityNotFoundException("ImageAnalysisSoftware with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("ImageAnalysisSoftware cannot be retrieved", e);
        }   
    }
    
    @Operation(summary = "Retrieve DataProcessingSoftware with the given id")
    @RequestMapping(value="/getDataProcessingSoftware/{dataprocessingId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="DataProcessingSoftware retrieved successfully"), 
            @ApiResponse(responseCode="404", description="DataProcessingSoftware with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public DataProcessingSoftware getDataProcessingSoftware (
            @Parameter(required=true, description="id of the DataProcessingSoftware to retrieve") 
            @PathVariable("dataprocessingId") String id) {
        try {
            
            DataProcessingSoftware metadata = metadataRepository.getDataProcessingSoftwareFromURI(GlygenArrayRepository.uriPrefixPublic 
                    + id.trim(), null);
            if (metadata == null) {
                throw new EntityNotFoundException("DataProcessingSoftware with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("DataProcessingSoftware cannot be retrieved", e);
        }   
    }
    
    @Operation(summary = "Retrieve assay metadata with the given id")
    @RequestMapping(value="/getAssayMetadata/{assayId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Assay metadata retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Assay metadata with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public AssayMetadata getAssayMetadata (
            @Parameter(required=true, description="id of the Assay metadata to retrieve") 
            @PathVariable("assayId") String id) {
        try {
            
            AssayMetadata metadata = metadataRepository.getAssayMetadataFromURI(GlygenArrayRepository.uriPrefixPublic + id.trim(), null);
            if (metadata == null) {
                throw new EntityNotFoundException("Assay metadata with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Assay metadata cannot be retrieved", e);
        }   
    }
    
    
    @Operation(summary = "List all features with intensities for the given processed data")
    @RequestMapping(value="/listIntensityData", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Intensities retrieved successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))})})
    public IntensityDataResultView listIntensityData (
            @Parameter(required=true, description="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @Parameter(required=false, description="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @Parameter(required=false, description="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @Parameter(required=false, description="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @Parameter(required=false, description="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue,
            @Parameter(required=true, description="id of the processed data from which the intensities should be retrieved") 
            @RequestParam(value="processedDataId", required=true)
            String processedDataId, 
            @RequestParam(value="datasetId", required=true)
            @Parameter(required=true, description="id of the dataset for which the intensities should be retrieved")
            String datasetId) {
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
            
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            
            IntensityDataResultView result = new IntensityDataResultView();
            ArrayDataset dataset = datasetRepository.getArrayDataset(datasetId.trim(), false, null);
            if (dataset == null) {
                errorMessage.addError(new ObjectError("dataset", "NotFound"));
                errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
            }
            else {
                ProcessedData existing = null;
                for (Slide slide: dataset.getSlides()) {
                    for (Image image: slide.getImages()) {
                        if (image.getRawDataList() != null) {
                            for (RawData rawData: image.getRawDataList()) {
                                for (ProcessedData p: rawData.getProcessedDataList()) {
                                    if (p.getId().equals(processedDataId)) {
                                        existing = p;
                                    }
                                }
                            }
                        }
                    }
                }
                if (existing == null) {
                    errorMessage.addError(new ObjectError("processedData", "NotFound"));
                    errorMessage.setErrorCode(ErrorCodes.NOT_FOUND);
                }
                if (existing != null){      
                    int total = datasetRepository.getIntensityDataListCount(processedDataId.trim(), null, searchValue);
                    // need to retrieve the intensities
                    List<IntensityData> dataList = datasetRepository.getIntensityDataList(processedDataId.trim(), null, offset, limit, field, order, searchValue);
                    
                    // populate the cartoon images
                    for (IntensityData data: dataList) {
                        Feature feature = data.getFeature();
                        if (feature != null) {
                            GlygenArrayController.populateFeatureGlycanImages(feature, imageLocation);
                        }
                        if (feature instanceof LinkedGlycan) {
                        	if (((LinkedGlycan) feature).getGlycans() == null ||
                        			((LinkedGlycan) feature).getGlycans().isEmpty() ||
                        			((LinkedGlycan) feature).getGlycans().get(0).getGlycan() == null) {
                        		logger.info("feature "
                        				+ feature.getName() + " is missing its glycans");
                        	}
                        }
                    }
                    result.setRows(dataList);
                    result.setFilteredTotal(dataList.size());
                    result.setTotal(total);
                    return result;
                }
            }
            
            if (errorMessage != null && errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                throw new IllegalArgumentException("Error in the arguments", errorMessage);
            }
            
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("intensity data cannot be retrieved", e);
        }  
        
        return null;
        
    }
    
    @Operation(summary = "List of all glytoucanIDs in a given block layout")
    @RequestMapping(value="/listGlycoucanidsByblockLayout", method = RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Glycans retrieved sucessfully"), 
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public Set<String> listGlycansByBlockLayout (
            @Parameter(required=true, description="the id of the block layout") 
            @RequestParam String blockLayoutId) {
        Set<String> ids = new HashSet<String>();
        BlockLayout layout = getBlockLayout(blockLayoutId, true);
        for (Spot spot: layout.getSpots()) {
            if (spot == null) continue;
            for (Feature feature: spot.getFeatures()) {
                if (feature == null) continue;
                switch (feature.getType()) {
                case LINKEDGLYCAN:
                    for (GlycanInFeature gf: ((LinkedGlycan) feature).getGlycans()) {
                        if (gf.getGlycan() != null && gf.getGlycan().getType() == GlycanType.SEQUENCE_DEFINED) {
                            String glytoucanId = ((SequenceDefinedGlycan)gf.getGlycan()).getGlytoucanId();
                            if (glytoucanId != null && glytoucanId.length() <= 10) {
                                ids.add(glytoucanId);
                            }
                        }
                    }
                    break;
                case GLYCOLIPID:
                    for (LinkedGlycan lg: ((GlycoLipid) feature).getGlycans()) {
                        for (GlycanInFeature gf: lg.getGlycans()) {
                            if (gf.getGlycan() != null && gf.getGlycan().getType() == GlycanType.SEQUENCE_DEFINED) {
                                String glytoucanId = ((SequenceDefinedGlycan)gf.getGlycan()).getGlytoucanId();
                                if (glytoucanId != null && glytoucanId.length() <= 10) {
                                    ids.add(glytoucanId);
                                }
                            }
                        }
                    }
                    break;
                case GLYCOPEPTIDE:
                    for (LinkedGlycan lg: ((GlycoPeptide) feature).getGlycans()) {
                        for (GlycanInFeature gf: lg.getGlycans()) {
                            if (gf.getGlycan() != null && gf.getGlycan().getType() == GlycanType.SEQUENCE_DEFINED) {
                                String glytoucanId = ((SequenceDefinedGlycan)gf.getGlycan()).getGlytoucanId();
                                if (glytoucanId != null && glytoucanId.length() <= 10) {
                                    ids.add(glytoucanId);
                                }
                            }
                        }
                    }
                    break;
                case GLYCOPROTEIN:
                    for (LinkedGlycan lg: ((GlycoProtein) feature).getGlycans()) {
                        for (GlycanInFeature gf: lg.getGlycans()) {
                            if (gf.getGlycan() != null && gf.getGlycan().getType() == GlycanType.SEQUENCE_DEFINED) {
                                String glytoucanId = ((SequenceDefinedGlycan)gf.getGlycan()).getGlytoucanId();
                                if (glytoucanId != null && glytoucanId.length() <= 10) {
                                    ids.add(glytoucanId);
                                }
                            }
                        }
                    }
                    break;
                case GPLINKEDGLYCOPEPTIDE:
                    for (GlycoPeptide gp: ((GPLinkedGlycoPeptide) feature).getPeptides()) {
                        for (LinkedGlycan lg: gp.getGlycans()) {
                            for (GlycanInFeature gf: lg.getGlycans()) {
                                if (gf.getGlycan() != null && gf.getGlycan().getType() == GlycanType.SEQUENCE_DEFINED) {
                                    String glytoucanId = ((SequenceDefinedGlycan)gf.getGlycan()).getGlytoucanId();
                                    if (glytoucanId != null && glytoucanId.length() <= 10) {
                                        ids.add(glytoucanId);
                                    }
                                }
                            }
                        }
                    }
                    break;
                case LANDING_LIGHT:
                case NEGATIVE_CONTROL:
                case COMPOUND:
                case CONTROL:
                    break;
                }
                
            }
        }
        return ids;
    }
    
    @Operation(summary = "Download the given file")
    @RequestMapping(value="/download", method = RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="File downloaded successfully"), 
            @ApiResponse(responseCode="400", description="File not found, or not accessible publicly", content = {
                    @Content( schema = @Schema(implementation = ErrorMessage.class))}),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<Resource> downloadFile(
            @Parameter(required=true, description="the folder of the file") 
            @RequestParam String fileFolder, 
            @Parameter(required=true, description="the identifier of the file to be downloaded") 
            @RequestParam String fileIdentifier,
            @Parameter(required=false, description="filename to save the downloaded file as. If not provided, the original file name is used if available") 
            @RequestParam(value="filename", required=false)
            String originalName) {
        
        // check to see if the user can access this file
        fileFolder = fileFolder.trim();
        fileIdentifier = fileIdentifier.trim();
        if (originalName != null) originalName = originalName.trim();
        String datasetId = fileFolder.substring(fileFolder.lastIndexOf("/")+1);
        ErrorMessage errorMessage = new ErrorMessage("Invalid input");
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        try {
            String publicid = datasetRepository.getDatasetPublicId(datasetId);
            if (publicid == null) {
                errorMessage.addError(new ObjectError("fileIdentifier", "This file is not public. Cannot be downloaded!"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            } 
        } catch (Exception e) {
            throw new GlycanRepositoryException("Array dataset cannot be loaded", e);
        }
        
        
        File file = new File(fileFolder, fileIdentifier);
        if (!file.exists()) {
            errorMessage.addError(new ObjectError("fileIdentifier", "NotFound"));
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        if (originalName == null) {
            try {
                FileWrapper fw = repository.getFileByIdentifier(fileIdentifier, null);
                if (fw != null) {
                    originalName = fw.getOriginalName();
                }
            } catch (Exception e) {
                logger.warn ("error getting file details from the repository", e);
                
            }
            if (originalName == null)
                originalName = fileIdentifier;
        }
        return DatasetController.download(file, originalName);
    }
    
    
    @Operation(summary = "Export slide layout in extended GAL format")
    @RequestMapping(value = "/downloadSlideLayout", method=RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="File generated successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, file cannot be found"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<Resource> exportSlideLayout (
            @Parameter(required=true, description="id of the slide layout") 
            @RequestParam("slidelayoutid")
            String slidelayoutid,
            @Parameter(required=false, description="the name for downloaded file") 
            @RequestParam(value="filename", required=false)
            String fileName) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        if (fileName == null || fileName.isEmpty()) {
            fileName = slidelayoutid + ".gal";
        }
        File newFile = new File (uploadDir, "tmp" + fileName);
        
        try {
            SlideLayout layout = layoutRepository.getSlideLayoutById(slidelayoutid, null, true);
            if (layout == null) {    
                errorMessage.addError(new ObjectError("slidelayoutid", "NotFound"));
            }
            if (layout != null) {
                try {
                    galFileParser.exportToFile(layout, newFile.getAbsolutePath());
                    
                } catch (IOException e) {
                    errorMessage.addError(new ObjectError("file", "NotFound"));
                }
            }
            
            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return DatasetController.download (newFile, fileName);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve slide layout from the repository", e);
        }
    }
    
    @Operation(summary = "Export processed data in glygen array data file format")
    @RequestMapping(value = "/downloadProcessedData", method=RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="File generated successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, file cannot be found"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<Resource> exportProcessedData (
            @Parameter(required=true, description="id of the processed data") 
            @RequestParam("processeddataid")
            String processedDataId,
            @Parameter(required=false, description="the name for downloaded file") 
            @RequestParam(value="filename", required=false)
            String fileName) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        String uri = GlygenArrayRepositoryImpl.uriPrefixPublic + processedDataId;
        if (fileName == null || fileName.isEmpty()) {
            fileName = processedDataId + ".xlsx";
        }
        File newFile = new File (uploadDir, "tmp" + fileName);
        
        try {
            ProcessedData data = datasetRepository.getProcessedDataFromURI(uri, true, null);
            if (data == null) {
               errorMessage.addError(new ObjectError("processeddataid", "NotFound"));
            }
          
            if (data != null) {
                try {
                    ProcessedDataParser.exportToFile(data, newFile.getAbsolutePath());  
                } catch (IOException e) {
                    errorMessage.addError(new ObjectError("file", "NotFound"));
                }
            }
            
            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return DatasetController.download (newFile, fileName);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve processed data from the repository", e);
        }
    }
    
    @Operation(summary = "Export metadata into Excel")
    @RequestMapping(value = "/downloadMetadata", method=RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="File generated successfully"), 
            @ApiResponse(responseCode="400", description="Invalid request, file cannot be found"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<Resource> exportAllMetadata (
            @Parameter(required=true, description="id of the array dataset") 
            @RequestParam("datasetId")
            String datasetId,
            @Parameter(required=false, description="the name for downloaded file") 
            @RequestParam(value="filename", required=false)
            String fileName, 
            @Parameter(required=false, description="mirage metadata only") 
            @RequestParam(value="mirageOnly", required=false)
            Boolean mirageOnly,
            @Parameter(required=false, description="single sheet") 
            @RequestParam(value="singleSheet", required=false)
            Boolean singleSheet) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        if (fileName == null || fileName.isEmpty()) {
            fileName = datasetId + ".xlsx";
        }
        File newFile = new File (uploadDir, "tmp" + fileName);
        
        try {
            ArrayDataset data = datasetRepository.getArrayDataset(datasetId, true, false, null);
            if (data == null) {
              errorMessage.addError(new ObjectError("datasetId", "NotFound"));
            }
          
            if (data != null) {
                try {
                    UserEntity user = userRepository.findByUsernameIgnoreCase(data.getUser().getName());
                    if (user != null) {
                        // update user info
                        data.getUser().setFirstName(user.getFirstName());
                        data.getUser().setLastName(user.getLastName());
                    }
                    new MetadataImportExportUtil(scheme+host+basePath).exportIntoExcel(data, newFile.getAbsolutePath(), mirageOnly, singleSheet);  
                } catch (IOException e) {
                    errorMessage.addError(new ObjectError("file", "NotFound"));
                }
            }
            
            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return DatasetController.download (newFile, fileName);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve dataset from the repository", e);
        }
    }
}
