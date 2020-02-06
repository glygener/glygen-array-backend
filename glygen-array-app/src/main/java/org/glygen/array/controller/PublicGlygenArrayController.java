package org.glygen.array.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.persistence.EntityNotFoundException;
import javax.validation.Validator;

import org.apache.commons.io.IOUtils;
import org.glygen.array.config.SesameTransactionConfig;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.GlycanType;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.service.FeatureRepository;
import org.glygen.array.service.GlycanRepository;
import org.glygen.array.service.GlygenArrayRepository;
import org.glygen.array.service.LayoutRepository;
import org.glygen.array.service.LinkerRepository;
import org.glygen.array.view.BlockLayoutResultView;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.glygen.array.view.FeatureListResultView;
import org.glygen.array.view.GlycanListResultView;
import org.glygen.array.view.LinkerListResultView;
import org.glygen.array.view.SlideLayoutResultView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
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
    Validator validator;
    
    @Value("${spring.file.imagedirectory}")
    String imageLocation;
    
    @ApiOperation(value = "List all public glycans")
    @RequestMapping(value="/listGlycans", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Glycans retrieved successfully", response = GlycanListResultView.class), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public GlycanListResultView listGlycans (
            @ApiParam(required=true, value="offset for pagination, start from 0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of glycans to be retrieved") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
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
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("order", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Order should be 0 or 1", errorMessage);
            }
            
            int total = glycanRepository.getGlycanCountByUser (null);
            
            List<Glycan> glycans = glycanRepository.getGlycanByUser(null, offset, limit, field, order, searchValue);
            for (Glycan glycan : glycans) {
                if (glycan.getType().equals(GlycanType.SEQUENCE_DEFINED)) {
                    glycan.setCartoon(getCartoonForGlycan(glycan.getId()));
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
    
    private byte[] getCartoonForGlycan (String glycanId) {
        try {
            File imageFile = new File(imageLocation + File.separator + glycanId + ".png");
            InputStreamResource resource = new InputStreamResource(new FileInputStream(imageFile));
            return IOUtils.toByteArray(resource.getInputStream());
        } catch (IOException e) {
            logger.error("Image cannot be retrieved", e);
            return null;
        }
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
            
            Glycan glycan = glycanRepository.getGlycanById(glycanId, null);
            if (glycan == null) {
                throw new EntityNotFoundException("Glycan with id : " + glycanId + " does not exist in the repository");
            }
            byte[] cartoon = getCartoonForGlycan(glycanId);
            glycan.setCartoon(cartoon);
            return glycan;
            
            
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Glycan cannot be retrieved", e);
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
            Linker linker = linkerRepository.getLinkerById(linkerId, null);
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
            @ApiParam(required=true, value="offset for pagination, start from 0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of linkers to be retrieved") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
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
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("order", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Order should be 0 or 1", errorMessage);
            }
            
            int total = linkerRepository.getLinkerCountByUser (null);
            
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
            @ApiParam(required=true, value="offset for pagination, start from 0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of layouts to be retrieved") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
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
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("order", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Order should be 0 or 1", errorMessage);
            }
            
            int total = layoutRepository.getSlideLayoutCountByUser (null);
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
            @ApiParam(required=true, value="offset for pagination, start from 0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of layouts to be retrieved") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
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
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("order", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Order should be 0 or 1", errorMessage);
            }
            
            int total = layoutRepository.getBlockLayoutCountByUser (null);
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
            @ApiParam(required=true, value="offset for pagination, start from 0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of features to be retrieved") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
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
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("order", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Order should be 0 or 1", errorMessage);
            }
            
            int total = featureRepository.getFeatureCountByUser (null);
            
            List<org.glygen.array.persistence.rdf.Feature> features = featureRepository.getFeatureByUser(null, offset, limit, field, order, searchValue);
            result.setRows(features);
            result.setTotal(total);
            result.setFilteredTotal(features.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve linkers for user. Reason: " + e.getMessage());
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
            
            BlockLayout layout = layoutRepository.getBlockLayoutById(layoutId, null, loadAll);
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
            SlideLayout layout = layoutRepository.getSlideLayoutById(layoutId, null, loadAll);
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
            org.glygen.array.persistence.rdf.Feature feature = featureRepository.getFeatureById(featureId, null);
            if (feature == null) {
                throw new EntityNotFoundException("Feature with id : " + featureId + " does not exist in the repository");
            }
            
            return feature;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Feature cannot be retrieved for user " , e);
        }
    }

}
