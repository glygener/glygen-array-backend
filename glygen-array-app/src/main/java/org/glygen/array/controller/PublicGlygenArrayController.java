package org.glygen.array.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.GlycanType;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.data.ArrayDataset;
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
import org.glygen.array.service.ArrayDatasetRepository;
import org.glygen.array.service.FeatureRepository;
import org.glygen.array.service.GlycanRepository;
import org.glygen.array.service.GlygenArrayRepository;
import org.glygen.array.service.GlygenArrayRepositoryImpl;
import org.glygen.array.service.LayoutRepository;
import org.glygen.array.service.LinkerRepository;
import org.glygen.array.service.MetadataRepository;
import org.glygen.array.service.QueryHelper;
import org.glygen.array.util.ExtendedGalFileParser;
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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

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
    
    @ApiOperation(value = "List all public glycans")
    @RequestMapping(value="/listGlycans", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Glycans retrieved successfully", response = GlycanListResultView.class), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public GlycanListResultView listGlycans (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of glycans to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
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
            throw new GlycanRepositoryException("Cannot retrieve glycans for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @ApiOperation(value = "Retrieve glycan with the given id")
    @RequestMapping(value="/getglycan/{glycanId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Glycan retrieved successfully"), 
            @ApiResponse(code=404, message="Gycan with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Glycan getGlycan (
            @ApiParam(required=true, value="id of the glycan to retrieve") 
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
    
    @ApiOperation(value = "Retrieve glycan with the given id")
    @RequestMapping(value="/getdatasetforglycan/{glycanId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Glycan retrieved successfully"), 
            @ApiResponse(code=404, message="Gycan with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public ArrayDatasetListView getDatasetsForGlycan (
            @ApiParam(required=true, value="id of the glycan to retrieve") 
            @PathVariable("glycanId") String glycanId,
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
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
            throw new GlycanRepositoryException("Cannot retrieve array datasets for user. Reason: " + e.getMessage());
        }
    }
    
    @ApiOperation(value = "Retrieve linker with the given id")
    @RequestMapping(value="/getlinker/{linkerId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Linker retrieved successfully"), 
            @ApiResponse(code=404, message="Linker with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Linker getLinker (
            @ApiParam(required=true, value="id of the linker to retrieve") 
            @PathVariable("linkerId") String linkerId) {
        try {
            Linker linker = linkerRepository.getLinkerById(linkerId.trim(), null);
            if (linker == null) {
                throw new EntityNotFoundException("Linker with id : " + linkerId + " does not exist in the repository");
            }
            return linker;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Linker cannot be retrieved for user ", e);
        }
        
    }
    
    @ApiOperation(value = "List all linkers for the user")
    @RequestMapping(value="/listLinkers", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Linkers retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public LinkerListResultView listLinkers (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of linkers to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
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
    
    @ApiOperation(value = "List all slide layouts for the user")
    @RequestMapping(value="/listSlidelayouts", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Slide layouts retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public SlideLayoutResultView listSlideLayouts (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of layouts to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam (required=false, defaultValue = "true", value="if false, do not load block details. Default is true (to load all)")
            @RequestParam(required=false, defaultValue = "true", value="loadAll") Boolean loadAll, 
            @ApiParam(required=false, value="a filter value to match") 
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
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve slide layouts for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @ApiOperation(value = "List all block layouts for the user")
    @RequestMapping(value="/listBlocklayouts", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Block layouts retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments", response = ErrorMessage.class),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public BlockLayoutResultView listBlockLayouts (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of layouts to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam (required=false, defaultValue = "true", value="if false, do not load spot details. Default is true (to load all)")
            @RequestParam(required=false, defaultValue = "true", value="loadAll") Boolean loadAll, 
            @ApiParam(required=false, value="a filter value to match") 
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
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve block layouts for user. Reason: " + e.getMessage());
        }
        
        return result;
    }

    @ApiOperation(value = "List all features for the user")
    @RequestMapping(value="/listFeatures", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Features retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public FeatureListResultView listFeature (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of features to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
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
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve linkers for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @ApiOperation(value = "List all public datasets")
    @RequestMapping(value="/listArrayDataset", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Array datasets retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public ArrayDatasetListView listArrayDataset (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="load rawdata and processed data details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="true") Boolean loadAll, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue) {
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
            
            int total = datasetRepository.getArrayDatasetCountByUser(null, searchValue);
            
            List<ArrayDataset> resultList = datasetRepository.getArrayDatasetByUser(null, offset, limit, field, order, searchValue, loadAll);
            result.setRows(resultList);
            result.setTotal(total);
            result.setFilteredTotal(resultList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve array datasets for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @ApiOperation(value = "List all public datasets submitted by the given user")
    @RequestMapping(value="/listArrayDatasetByUser", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Array datasets retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public ArrayDatasetListView listArrayDatasetByUser (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="load rawdata and processed data details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="false") Boolean loadAll, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue,
            @ApiParam(required=true, value="user name") 
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
    
    
    @ApiOperation(value = "List all public datasets submitted by the given user as a coowner")
    @RequestMapping(value="/listArrayDatasetByCoOwner", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Array datasets retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public ArrayDatasetListView listArrayDatasetByCoOwner (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="load rawdata and processed data details or not, default= true to load all the details") 
            @RequestParam(value="loadAll", required=false, defaultValue="false") Boolean loadAll, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue,
            @ApiParam(required=true, value="coowner name") 
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
    
    @ApiOperation(value = "List all printed slides for the user")
    @RequestMapping(value="/listPrintedSlide", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Printed slides retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public PrintedSlideListView listPrintedSlide (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
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
            throw new GlycanRepositoryException("Cannot retrieve array datasets for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @ApiOperation(value = "List all data processing software metadata for the user")
    @RequestMapping(value="/listDataProcessingSoftware", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Data processing software metadata list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listDataProcessingSoftware (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
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
            throw new GlycanRepositoryException("Cannot retrieve data processing software for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @ApiOperation(value = "List all image analysis software metadata for the user")
    @RequestMapping(value="/listImageAnalysisSoftware", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Image analysis software metadata list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listImageAnalysisSoftware (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
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
            throw new GlycanRepositoryException("Cannot retrieve image analysis software for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @ApiOperation(value = "List all printer metadata for the user")
    @RequestMapping(value="/listPrinters", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Printer list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listPrinters (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
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
            throw new GlycanRepositoryException("Cannot retrieve printers for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @ApiOperation(value = "List all samples for the user")
    @RequestMapping(value="/listSamples", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Samples retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listSamples (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
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
            throw new GlycanRepositoryException("Cannot retrieve samples for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @ApiOperation(value = "List all scanner metadata for the user")
    @RequestMapping(value="/listScanners", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Scanner list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listScanners (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
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
            throw new GlycanRepositoryException("Cannot retrieve scanners for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @ApiOperation(value = "List all slide metadata for the user")
    @RequestMapping(value="/listSlideMetadata", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Slide metadata list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listSlideMetadata (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
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
            throw new GlycanRepositoryException("Cannot retrieve slide metadata for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @ApiOperation(value = "List all assay metadata for the user")
    @RequestMapping(value="/listAssayMetadata", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Assay metadata list retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public MetadataListResultView listAssayMetadata (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
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
            throw new GlycanRepositoryException("Cannot retrieve assay metadata for user. Reason: " + e.getMessage());
        }
        
        return result;
    }
    
    @ApiOperation(value = "Retrieve block layout with the given id")
    @RequestMapping(value="/getblocklayout/{layoutId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Block Layout retrieved successfully"), 
            @ApiResponse(code=404, message="Block layout with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public BlockLayout getBlockLayout (
            @ApiParam(required=true, value="id of the block layout to retrieve") 
            @PathVariable("layoutId") String layoutId, 
            @ApiParam (required=false, defaultValue = "true", value="if false, do not load block details. Default is true (to load all)")
            @RequestParam(required=false, defaultValue = "true", value="loadAll") Boolean loadAll) {
        try {
            
            BlockLayout layout = layoutRepository.getBlockLayoutById(layoutId.trim(), null, loadAll);
            if (layout == null) {
                throw new EntityNotFoundException("Block layout with id : " + layoutId + " does not exist in the repository");
            }
            
            return layout;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Block Layout cannot be retrieved for user ", e);
        }
    }
    
    @ApiOperation(value = "Retrieve slide layout with the given id")
    @RequestMapping(value="/getslidelayout/{layoutId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Slide Layout retrieved successfully"), 
            @ApiResponse(code=404, message="Slide layout with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public SlideLayout getSlideLayout (
            @ApiParam(required=true, value="id of the slide layout to retrieve") 
            @PathVariable("layoutId") String layoutId, 
            @ApiParam (required=false, defaultValue = "true", value="if false, do not load slide details. Default is true (to load all)")
            @RequestParam(required=false, defaultValue = "true", value="loadAll") Boolean loadAll) {
        try {
            SlideLayout layout = layoutRepository.getSlideLayoutById(layoutId.trim(), null, loadAll);
            if (layout == null) {
                throw new EntityNotFoundException("Slide layout with id : " + layoutId + " does not exist in the repository");
            }
            
            return layout;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Slide Layout cannot be retrieved for user ", e);
        }
    }
    
    @ApiOperation(value = "Retrieve feature with the given id")
    @RequestMapping(value="/getfeature/{featureId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Feature retrieved successfully"), 
            @ApiResponse(code=404, message="Feature with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public org.glygen.array.persistence.rdf.Feature getFeature (
            @ApiParam(required=true, value="id of the feature to retrieve") 
            @PathVariable("featureId") String featureId) {
        try {
            org.glygen.array.persistence.rdf.Feature feature = featureRepository.getFeatureById(featureId.trim(), null);
            if (feature == null) {
                throw new EntityNotFoundException("Feature with id : " + featureId + " does not exist in the repository");
            }
            
            return feature;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Feature cannot be retrieved for user " , e);
        }
    }
    
    @ApiOperation(value = "Retrieve dataset with the given id")
    @RequestMapping(value="/getarraydataset/{datasetid}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Dataset retrieved successfully"), 
            @ApiResponse(code=404, message="Dataset with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public ArrayDataset getArrayDataset (
            @ApiParam(required=true, value="id of the array dataset to retrieve") 
            @PathVariable("datasetid") String id, 
            @ApiParam(required=false, value="load rawdata and processed data measurements or not, default= true to load all the details") 
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
    
    @ApiOperation(value = "Retrieve sample with the given id")
    @RequestMapping(value="/getsample/{sampleId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Sample retrieved successfully"), 
            @ApiResponse(code=404, message="Sample with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Sample getSample (
            @ApiParam(required=true, value="id of the sample to retrieve") 
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
    
    @ApiOperation(value = "Retrieve printer with the given id")
    @RequestMapping(value="/getPrinter/{printerId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Printer retrieved successfully"), 
            @ApiResponse(code=404, message="Printer with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Printer getPrinter (
            @ApiParam(required=true, value="id of the printer to retrieve") 
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

    @ApiOperation(value = "Retrieve printrun with the given id")
    @RequestMapping(value="/getPrintrun/{printrunId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Printrun retrieved successfully"), 
            @ApiResponse(code=404, message="Printer with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public PrintRun getPrintrun (
            @ApiParam(required=true, value="id of the printrun to retrieve") 
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
    
    @ApiOperation(value = "Retrieve scanner with the given id")
    @RequestMapping(value="/getScanner/{scannerId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Scanner retrieved successfully"), 
            @ApiResponse(code=404, message="ScannerMetadata with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public ScannerMetadata getScanner (
            @ApiParam(required=true, value="id of the ScannerMetadata to retrieve") 
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
    @ApiOperation(value = "Retrieve SlideMetadata with the given id")
    @RequestMapping(value="/getSlideMetadata/{slideId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="SlideMetadata retrieved successfully"), 
            @ApiResponse(code=404, message="Printer with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public SlideMetadata getSlideMetadata (
            @ApiParam(required=true, value="id of the SlideMetadata to retrieve") 
            @PathVariable("slideId") String id) {
        try {
            SlideMetadata metadata = metadataRepository.getSlideMetadataFromURI(GlygenArrayRepository.uriPrefixPublic + id.trim(), null);
            if (metadata == null) {
                throw new EntityNotFoundException("SlideMetadata with id : " + id + " does not exist in the repository");
            }
            return metadata;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("SlideMetadata cannot be retrieved" , e);
        }   
    }
    @ApiOperation(value = "Retrieve ImageAnalysisSoftware with the given id")
    @RequestMapping(value="/getImageAnalysisSoftware/{imagesoftwareId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="ImageAnalysisSoftware retrieved successfully"), 
            @ApiResponse(code=404, message="ImageAnalysisSoftware with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public ImageAnalysisSoftware getImageAnaylsisSoftware (
            @ApiParam(required=true, value="id of the ImageAnalysisSoftware to retrieve") 
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
    
    @ApiOperation(value = "Retrieve DataProcessingSoftware with the given id")
    @RequestMapping(value="/getDataProcessingSoftware/{dataprocessingId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="DataProcessingSoftware retrieved successfully"), 
            @ApiResponse(code=404, message="DataProcessingSoftware with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public DataProcessingSoftware getDataProcessingSoftware (
            @ApiParam(required=true, value="id of the DataProcessingSoftware to retrieve") 
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
    
    @ApiOperation(value = "Retrieve assay metadata with the given id")
    @RequestMapping(value="/getAssayMetadata/{assayId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Assay metadata retrieved successfully"), 
            @ApiResponse(code=404, message="Assay metadata with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public AssayMetadata getAssayMetadata (
            @ApiParam(required=true, value="id of the Assay metadata to retrieve") 
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
    
    
    @ApiOperation(value = "List all features with intensities for the given processed data")
    @RequestMapping(value="/listIntensityData", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Intensities retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public IntensityDataResultView listIntensityData (
            @ApiParam(required=true, value="offset for pagination, start from 0", example="0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of items to be retrieved", example="10") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1", example="0") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue,
            @ApiParam(required=true, value="id of the processed data from which the intensities should be retrieved") 
            @RequestParam(value="processedDataId", required=true)
            String processedDataId, 
            @RequestParam(value="datasetId", required=true)
            @ApiParam(required=true, value="id of the dataset for which the intensities should be retrieved")
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
    
    @ApiOperation(value = "Download the given file")
    @RequestMapping(value="/download", method = RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(code=200, message="File downloaded successfully"), 
            @ApiResponse(code=400, message="File not found, or not accessible publicly", response = ErrorMessage.class),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public ResponseEntity<Resource> downloadFile(
            @ApiParam(required=true, value="the folder of the file") 
            @RequestParam String fileFolder, 
            @ApiParam(required=true, value="the identifier of the file to be downloaded") 
            @RequestParam String fileIdentifier,
            @ApiParam(required=true, value="the original file name") 
            @RequestParam String originalName) {
        
        // check to see if the user can access this file
        fileFolder = fileFolder.trim();
        fileIdentifier = fileIdentifier.trim();
        originalName = originalName.trim();
        String datasetId = fileFolder.substring(fileFolder.lastIndexOf("/")+1);
        ErrorMessage errorMessage = new ErrorMessage("Invalid input");
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        try {
            String publicid = datasetRepository.getDatasetPublicId(datasetId);
            if (publicid == null) {
                errorMessage.addError(new ObjectError("fileWrapper", "This file is not public. Cannot be downloaded!"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            } 
        } catch (Exception e) {
            throw new GlycanRepositoryException("Array dataset cannot be loaded", e);
        }
        
        
        File file = new File(fileFolder, fileIdentifier);
        if (!file.exists()) {
            errorMessage.addError(new ObjectError("file", "NotFound"));
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
            return ResponseEntity.notFound().build();
            //throw new IllegalArgumentException ("File is not accessible", errorMessage);
        }
        
        return DatasetController.download(file, originalName);
  
        /*FileSystemResource r = new FileSystemResource(file);
        MediaType mediaType = MediaTypeFactory
                .getMediaType(r)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        
        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(mediaType);
        respHeaders.setContentLength(file.length());

        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(originalName)
                .build();

        respHeaders.set(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        respHeaders.set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,"Content-Disposition");
        
        return new ResponseEntity<Resource>(
                r, respHeaders, HttpStatus.OK
        );*/
    }
    
    
    @ApiOperation(value = "Export slide layout in extended GAL format")
    @RequestMapping(value = "/downloadSlideLayout", method=RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(code=200, message="File generated successfully"), 
            @ApiResponse(code=400, message="Invalid request, file cannot be found"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public ResponseEntity<Resource> exportSlideLayout (
            @ApiParam(required=true, value="id of the slide layout") 
            @RequestParam("slidelayoutid")
            String slidelayoutid,
            @ApiParam(required=true, value="the name for downloaded file") 
            @RequestParam("filename")
            String fileName) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        String uri = GlygenArrayRepositoryImpl.uriPrefix + slidelayoutid;
        File newFile = new File (uploadDir, "tmp" + fileName);
        
        try {
            SlideLayout layout = layoutRepository.getSlideLayoutFromURI(uri, true, null);
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
    
    @ApiOperation(value = "Export processed data in glygen array data file format")
    @RequestMapping(value = "/downloadProcessedData", method=RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(code=200, message="File generated successfully"), 
            @ApiResponse(code=400, message="Invalid request, file cannot be found"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public ResponseEntity<Resource> exportProcessedData (
            @ApiParam(required=true, value="id of the processed data") 
            @RequestParam("processeddataid")
            String processedDataId,
            @ApiParam(required=false, value="the name for downloaded file") 
            @RequestParam(value="filename", required=false)
            String fileName) {
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        String uri = GlygenArrayRepositoryImpl.uriPrefix + processedDataId;
        if (fileName == null || fileName.isEmpty()) {
            fileName = processedDataId + ".xlsx";
        }
        File newFile = new File (uploadDir, "tmp" + fileName);
        
        try {
            ProcessedData data = datasetRepository.getProcessedDataFromURI(uri, true, null);
            if (data == null) {
                // check if it is public
                data = datasetRepository.getProcessedDataFromURI(uri, true, null);
                if (data == null) {
                    errorMessage.addError(new ObjectError("processeddataid", "NotFound"));
                }
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
}
