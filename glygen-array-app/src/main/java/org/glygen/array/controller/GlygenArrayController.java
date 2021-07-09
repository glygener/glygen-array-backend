package org.glygen.array.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;

import javax.imageio.ImageIO;
import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarExporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarImporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.MolecularFramework.util.analytical.mass.GlycoVisitorMass;
import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.IonCloud;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.glycoinfo.GlycanFormatconverter.io.GlycoCT.WURCSToGlycoCT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glygen.array.config.SesameTransactionConfig;
import org.glygen.array.config.ValidationConstants;
import org.glygen.array.exception.GlycanExistsException;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.exception.UploadNotFinishedException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.FeatureType;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.GlycanSequenceFormat;
import org.glygen.array.persistence.rdf.GlycanType;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.LinkerType;
import org.glygen.array.persistence.rdf.MassOnlyGlycan;
import org.glygen.array.persistence.rdf.PeptideLinker;
import org.glygen.array.persistence.rdf.ProteinLinker;
import org.glygen.array.persistence.rdf.Publication;
import org.glygen.array.persistence.rdf.SequenceBasedLinker;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.SmallMoleculeLinker;
import org.glygen.array.persistence.rdf.UnknownGlycan;
import org.glygen.array.persistence.rdf.data.ChangeLog;
import org.glygen.array.persistence.rdf.data.ChangeType;
import org.glygen.array.persistence.rdf.data.FileWrapper;
import org.glygen.array.service.FeatureRepository;
import org.glygen.array.service.GlycanRepository;
import org.glygen.array.service.GlygenArrayRepository;
import org.glygen.array.service.LayoutRepository;
import org.glygen.array.service.LinkerRepository;
import org.glygen.array.util.ExtendedGalFileParser;
import org.glygen.array.util.GalFileImportResult;
import org.glygen.array.util.GlytoucanUtil;
import org.glygen.array.util.ParserConfiguration;
import org.glygen.array.util.UniProtUtil;
import org.glygen.array.util.pubchem.PubChemAPI;
import org.glygen.array.util.pubmed.DTOPublication;
import org.glygen.array.util.pubmed.PubmedUtil;
import org.glygen.array.view.BatchGlycanFileType;
import org.glygen.array.view.BatchGlycanUploadResult;
import org.glygen.array.view.BlockLayoutResultView;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.glygen.array.view.FeatureListResultView;
import org.glygen.array.view.GlycanListResultView;
import org.glygen.array.view.ImportGRITSLibraryResult;
import org.glygen.array.view.LinkerListResultView;
import org.glygen.array.view.ResumableFileInfo;
import org.glygen.array.view.ResumableInfoStorage;
import org.glygen.array.view.SlideLayoutError;
import org.glygen.array.view.SlideLayoutResultView;
import org.glygen.array.view.UploadResult;
import org.grits.toolbox.glycanarray.library.om.ArrayDesignLibrary;
import org.grits.toolbox.glycanarray.library.om.LibraryInterface;
import org.grits.toolbox.glycanarray.library.om.feature.Feature;
import org.grits.toolbox.glycanarray.library.om.feature.GlycanProbe;
import org.grits.toolbox.glycanarray.library.om.feature.Ratio;
import org.grits.toolbox.glycanarray.library.om.layout.Block;
import org.grits.toolbox.glycanarray.library.om.layout.Spot;
import org.grits.toolbox.glycanarray.om.parser.cfg.CFGMasterListParser;
import org.grits.toolbox.util.structure.glycan.util.FilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Import(SesameTransactionConfig.class)
@RestController
@RequestMapping("/array")
public class GlygenArrayController {
	final static Logger logger = LoggerFactory.getLogger("event-logger");
	
	static BuilderWorkspace glycanWorkspace = new BuilderWorkspace(new GlycanRendererAWT());
	static {       
	        glycanWorkspace.initData();
			// Set orientation of glycan: RL - right to left, LR - left to right, TB - top to bottom, BT - bottom to top
			glycanWorkspace.getGraphicOptions().ORIENTATION = GraphicOptions.RL;
			// Set flag to show information such as linkage positions and anomers
			glycanWorkspace.getGraphicOptions().SHOW_INFO = true;
			// Set flag to show mass
			glycanWorkspace.getGraphicOptions().SHOW_MASSES = false;
			// Set flag to show reducing end
			glycanWorkspace.getGraphicOptions().SHOW_REDEND = true;

			glycanWorkspace.setDisplay(GraphicOptions.DISPLAY_NORMALINFO);
			glycanWorkspace.setNotation(GraphicOptions.NOTATION_SNFG);
	}
	
	@Autowired
	SesameSparqlDAO sparqlDAO;
	
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
	
	@Value("${spring.file.imagedirectory}")
	String imageLocation;

	@Value("${spring.file.uploaddirectory}")
	String uploadDir;
	
	@Autowired
	Validator validator;
	
	List<Glycan> glycanCache = new ArrayList<Glycan>();
	
	List<Linker> linkerCache = new ArrayList<Linker>();
	
	@ApiOperation(value = "Add an alias to given glycan for the user")
	@RequestMapping(value = "/addAlias/{glycanId}", method = RequestMethod.POST, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Alias added to glycan successfully"), 
			@ApiResponse(code=400, message="Invalid request, alias cannot be empty"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to update glycans"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation addAliasForGlycan(
			@ApiParam(required=true, value="Id of the glycan to add alias for") 
			@PathVariable("glycanId") String glycanId, 
			@ApiParam(required=true, value="alias for the glycan") 
			@RequestBody String alias, Principal principal) throws SQLException {
		try {
			if (alias == null || alias.isEmpty()) {
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
				errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
				errorMessage.addError(new ObjectError("alias", "NoEmpty"));
				throw new IllegalArgumentException("Invalid Input: Not a valid alias", errorMessage);
			}
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			glycanRepository.addAliasForGlycan(glycanId, alias, user);
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Error updating glycan with glycanId: " +glycanId);
		}
		return new Confirmation("Glycan updated successfully with new alias", HttpStatus.OK.value());
	}
	
	@ApiOperation(value = "Add given block layout for the user")
	@RequestMapping(value="/addblocklayout", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Block layout added successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register block layouts"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public String addBlockLayout (
			@ApiParam(required=true, value="Block layout to be added, name, width, height, and spots are required")
			@RequestBody BlockLayout layout, Principal p) {
		
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		
		if (layout.getName() == null || layout.getName().trim().isEmpty()) {
			errorMessage.addError(new ObjectError("name", "NoEmpty"));
		} 
		if (layout.getWidth() == null || layout.getHeight() == null)
			errorMessage.addError(new ObjectError(layout.getWidth() == null ? "width" : "height", "NoEmpty"));
		if (layout.getSpots() == null)
			errorMessage.addError(new ObjectError("spots", "NoEmpty"));
		
		// validate first
		if (validator != null) {			
			if  (layout.getName() != null) {
				Set<ConstraintViolation<BlockLayout>> violations = validator.validateValue(BlockLayout.class, "name", layout.getName());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (layout.getDescription() != null) {
				Set<ConstraintViolation<BlockLayout>> violations = validator.validateValue(BlockLayout.class, "description", layout.getDescription());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("description", "LengthExceeded"));
				}		
			}
			if (layout.getWidth() != null) {
				Set<ConstraintViolation<BlockLayout>> violations = validator.validateValue(BlockLayout.class, "width", layout.getWidth());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("width", "PositiveOnly"));
				}		
			}
			if (layout.getHeight() != null) {
				Set<ConstraintViolation<BlockLayout>> violations = validator.validateValue(BlockLayout.class, "height", layout.getHeight());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("height", "PositiveOnly"));
				}		
			}
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		try {
			BlockLayout existing = layoutRepository.getBlockLayoutByName(layout.getName(), user, false);
			if (existing != null) {
				// duplicate
				errorMessage.addError(new ObjectError("name", "Duplicate"));
				throw new GlycanExistsException("A block layout with the same name already exists", errorMessage);
			} 
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Block layout cannot be added for user " + p.getName(), e);
		}
		
		// check if features exist
		List<org.glygen.array.persistence.rdf.Feature> checked = new ArrayList<>();
		if (layout.getSpots() != null) {
		    for (org.glygen.array.persistence.rdf.Spot s: layout.getSpots()) {
		        if (s.getRatioMap() != null) {
		            double sum = 0.0;
		            for (Double r: s.getRatioMap().values()) {
		                if (r != null) {
		                    sum += r;
		                }
		            }
		            if (sum > 0.0 && sum != 100.0) {
		                // ratios do not add up to 100
		                errorMessage.addError(new ObjectError("ratio", "NotValid"));
		            }
		        }
		        if (s.getFeatures() != null) {
		            for (org.glygen.array.persistence.rdf.Feature f: s.getFeatures()) {
		                try {
		                    if (!checked.contains(f)) {
                                org.glygen.array.persistence.rdf.Feature existing = featureRepository.getFeatureByLabel(f.getName(), user);
                                checked.add(f);
                                if (existing == null) {
                                    errorMessage.addError(new ObjectError("feature", f.getName() + " does not exist"));
                                }
		                    }
                        } catch (SparqlException | SQLException e) {
                            throw new GlycanRepositoryException("Block layout cannot be added for user " + p.getName(), e);
                        }
		            }
		        }
		    /*    if (s.getMetadata() == null) {
		            errorMessage.addError(new ObjectError("spot metadata", "NoEmpty"));
		        }*/ //TODO think about what to do with CFG data and include this check
		    }
		}
		
		if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid block layout information", errorMessage);
		
		try {
			String uri = layoutRepository.addBlockLayout(layout, user);
			String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Block layout cannot be added for user " + p.getName(), e);
		}
	}
	

	@ApiOperation(value = "Add given feature for the user")
	@RequestMapping(value="/addfeature", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added feature"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register features"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public String addFeature (
			@ApiParam(required=false, value="Feature to be added, a linker and an at least one glycan are mandatory") 
			@RequestBody(required=false) org.glygen.array.persistence.rdf.Feature feature, Principal p) {
	    
	    ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        // validate first
        if (validator != null) {
            if  (feature.getName() != null) {
                Set<ConstraintViolation<Feature>> violations = validator.validateValue(Feature.class, "name", feature.getName());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("name", "LengthExceeded"));
                }       
            }
            if  (feature.getInternalId() != null) {
                Set<ConstraintViolation<Feature>> violations = validator.validateValue(Feature.class, "internalId", feature.getInternalId());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("internalId", "LengthExceeded"));
                }       
            }
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
	    if (feature.getType() == null || feature.getType() == FeatureType.NORMAL) {
    		if (feature.getLinker() == null || feature.getGlycans() == null || feature.getGlycans().isEmpty()) {
    			if (feature.getLinker() == null)
    				errorMessage.addError(new ObjectError("linker", "NoEmpty"));
    			else 
    				errorMessage.addError(new ObjectError("glycan", "NoEmpty"));
    		}
	    } else {
	        // other types 
	        if (feature.getLinker() == null) {
                errorMessage.addError(new ObjectError("linker", "NoEmpty"));
                
	        }
	    }
	    
	    if (feature.getMetadata() == null) {
	        errorMessage.addError(new ObjectError("metadata", "NoEmpty"));
	    }
	   	
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		
		if (feature.getName() != null && !feature.getName().trim().isEmpty()) {
		    try {
                org.glygen.array.persistence.rdf.Feature existing = featureRepository.getFeatureByLabel(feature.getName(), user);
                if (existing != null) {
                    feature.setId(existing.getId());
                    errorMessage.addError(new ObjectError("name", "Duplicate"));
                }
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Could not query existing features", e);
            }
		}
		
		if (feature.getInternalId() != null && !feature.getInternalId().trim().isEmpty()) {
            try {
                org.glygen.array.persistence.rdf.Feature existing = featureRepository.getFeatureByLabel(feature.getInternalId(), 
                        "gadr:has_internal_id", user);
                if (existing != null) {
                    feature.setId(existing.getId());
                    errorMessage.addError(new ObjectError("internalId", "Duplicate"));
                }
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Could not query existing features", e);
            }
        }
		
		if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid feature information", errorMessage);
		
		try {
		    try {
    		    if (feature.getLinker().getUri() == null && feature.getLinker().getId() == null) {
        		    feature.getLinker().setId(addLinker(feature.getLinker(), p));
    		    } else {
    		        // check to make sure it is an existing linker
    		        String linkerId = feature.getLinker().getId();
    		        if (linkerId == null) {
    		            // get it from uri
    		            linkerId = feature.getLinker().getUri().substring(feature.getLinker().getUri().lastIndexOf("/")+1);
    		        }
    		        Linker existing = linkerRepository.getLinkerById(linkerId, user);
    		        if (existing == null) {
    		            errorMessage = new ErrorMessage();
                        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
    	                errorMessage.addError(new ObjectError("linker", "NotValid"));
    	                throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage);
    		        }
    		    }
		    } catch (Exception e) {
                logger.debug("Ignoring error: " + e.getMessage());
            }
		    // check its glycans
		    if (feature.getGlycans() != null) {
    		    
    		    for (Glycan g: feature.getGlycans()) {
    		        if (g.getUri() == null && g.getId() == null) {
    		            try {
    		                g.setId(addGlycan(g, p, true));
    		            } catch (Exception e) {
    		                logger.debug("Ignoring error: " + e.getMessage());
    		            }
    		        } else {
    		            // check to make sure it is an existing glycan
                        String glycanId = g.getId();
                        if (glycanId == null) {
                            // get it from uri
                            glycanId = g.getUri().substring(g.getUri().lastIndexOf("/")+1);
                        }
                        Glycan existing = glycanRepository.getGlycanById(glycanId, user);
                        if (existing == null) {
                            errorMessage = new ErrorMessage();
                            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                            errorMessage.addError(new ObjectError("glycan", "NotValid"));
                            throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
                        }
    		        }
    		    } 
		    }
		    
			String featureURI = featureRepository.addFeature(feature, user);
			String id = featureURI.substring(featureURI.lastIndexOf("/")+1);
			return id;
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Feature cannot be added for user " + p.getName(), e);
		}		
	}
	
	@ApiOperation(value = "Add given feature, provided only with sequence based linker for the user")
	@RequestMapping(value="/addfeatureFromSequence", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added feature"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register features"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public String addFeatureFromLinkerSequence (
			@ApiParam(required=false, value="Feature to be added, "
					+ "a linker is mandatory and should be one of PeptideLinker or a ProteinLinker with a valid sequence") 
			@RequestBody(required=false) org.glygen.array.persistence.rdf.Feature feature, Principal p) {
		if (feature.getLinker() == null || !(feature.getLinker() instanceof SequenceBasedLinker)) {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
			errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
			if (feature.getLinker() == null)
				errorMessage.addError(new ObjectError("linker", "NoEmpty"));
			else 
				errorMessage.addError(new ObjectError("linker", "InvalidSequence"));
			throw new IllegalArgumentException("Invalid Input: Not a valid feature information", errorMessage);
		}
		
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		try {
    		if (feature.getGlycans() == null || feature.getGlycans().isEmpty()) {
                // check if the linker is a sequence-based linker and if so, try to extract the glycans
                // from the linker sequence and populate positionMap
                if (feature.getLinker().getType() == LinkerType.PEPTIDE_LINKER || feature.getLinker().getType() == LinkerType.PROTEIN_LINKER) {
                    Map<Integer, Glycan>  positionMap = ((SequenceBasedLinker)feature.getLinker()).extractGlycans();
                    Map<String, String> positionMapWithId = new HashMap<>();
                    for (Integer position: positionMap.keySet()) {
                    	Glycan g = positionMap.get(position);
                        String seq = ((SequenceDefinedGlycan)g).getSequence();
                        if (seq != null) {
                            String existing = glycanRepository.getGlycanBySequence(seq.trim(), user);
                            if (existing == null) {
                                // add the glycan
                                existing = addGlycan(g, p, true);
                            }
                            g.setUri(existing);
                            feature.addGlycan(g);
                            positionMapWithId.put(position + "", existing);
                        } else {
                            logger.error("Glycan in the feature with the following sequence cannot be located: " + seq);
                        }
                        
                    }
                    
                    feature.setPositionMap(positionMapWithId);
                }
            }
			String featureURI = featureRepository.addFeature(feature, user);
			String id = featureURI.substring(featureURI.lastIndexOf("/")+1);
            return id;
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Feature cannot be added for user " + p.getName(), e);
		}		
	}
	
	
	private String addGenericGlycan(Glycan glycan, Principal p) {
		if (glycan.getName() == null || glycan.getName().trim().isEmpty()) {
			ErrorMessage errorMessage = new ErrorMessage("Name cannot be empty");
			errorMessage.addError(new ObjectError("name", "NoEmpty"));
			throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
		}
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		// validate first
		if (validator != null) {
			if  (glycan.getName() != null) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "name", glycan.getName());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (glycan.getDescription() != null) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "description", glycan.getDescription());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("description", "LengthExceeded"));
				}		
			}
			if (glycan.getInternalId() != null && !glycan.getInternalId().isEmpty()) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "internalId", glycan.getInternalId());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("internalId", "LengthExceeded"));
				}		
			}
			
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		
		UserEntity user = null;
		try {
			user = userRepository.findByUsernameIgnoreCase(p.getName());
			Glycan local = null;
			// check if internalid and label are unique
			if (glycan.getInternalId() != null && !glycan.getInternalId().trim().isEmpty()) {
				local = glycanRepository.getGlycanByInternalId(glycan.getInternalId().trim(), user);
				if (local != null) {
				    glycan.setId(local.getId());
					errorMessage.addError(new ObjectError("internalId", "Duplicate"));
				}
			}
			if (glycan.getName() != null && !glycan.getName().trim().isEmpty()) {
				local = glycanRepository.getGlycanByLabel(glycan.getName().trim(), user);
				if (local != null) {
				    glycan.setId(local.getId());
					errorMessage.addError(new ObjectError("name", "Duplicate"));
				}
			} 
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Glycan cannot be added for user " + p.getName(), e);
		}
				
		if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
		try {	
			// no errors add the glycan
			String glycanURI = glycanRepository.addGlycan(glycan, user);
			return glycanURI.substring(glycanURI.lastIndexOf("/")+1);
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Glycan cannot be added for user " + p.getName(), e);
		}
	}
	
	
	@ApiOperation(value = "Add given glycan for the user")
	@RequestMapping(value="/addglycan", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="id of the added glycan"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register glycans"),
			@ApiResponse(code=409, message="A glycan with the given sequence already exists!"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public String addGlycan (@RequestBody Glycan glycan, Principal p, @RequestParam("noGlytoucanRegistration") Boolean noGlytoucanRegistration) {
		if (glycan.getType() == null) {
			// assume sequenceDefinedGlycan
			glycan.setType(GlycanType.SEQUENCE_DEFINED);
		}
		if (noGlytoucanRegistration == null)
			noGlytoucanRegistration = false;
		switch (glycan.getType()) {
		case SEQUENCE_DEFINED: 
			return addSequenceDefinedGlycan((SequenceDefinedGlycan)glycan, p, noGlytoucanRegistration);
		case MASS_ONLY:
			return addMassOnlyGlycan ((MassOnlyGlycan) glycan, p);
		case UNKNOWN:
		default:
			return addGenericGlycan(glycan, p);
		}
	}
	
	@ApiOperation(value = "Register all glycans listed in a file")
	@RequestMapping(value = "/addBatchGlycan", method=RequestMethod.POST, 
			consumes = {"application/json", "application/xml"}, produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Glycans processed successfully"), 
			@ApiResponse(code=400, message="Invalid request if file is not a valid file"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register glycans"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public BatchGlycanUploadResult addGlycanFromFile (
	        @ApiParam(required=true, name="file", value="details of the uploded file") 
	        @RequestBody
	        FileWrapper fileWrapper, Principal p, 
	        @RequestParam Boolean noGlytoucanRegistration,
	        @ApiParam(required=true, name="filetype", value="type of the file", allowableValues="Tab separated, Library XML, GlycoWorkbench") 
	        @RequestParam(required=true, value="filetype") String fileType) {
	    BatchGlycanFileType type = BatchGlycanFileType.forValue(fileType);
	    
	    String fileFolder = uploadDir;
        if (fileWrapper.getFileFolder() != null && !fileWrapper.getFileFolder().isEmpty())
            fileFolder = fileWrapper.getFileFolder();
        File file = new File (fileFolder, fileWrapper.getIdentifier());
        if (!file.exists()) {
            ErrorMessage errorMessage = new ErrorMessage("file is not in the uploads folder");
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("file", "NotFound"));
            throw new IllegalArgumentException("File is not acceptable", errorMessage);
        }
        else {
            byte[] fileContent;
            try {
                fileContent = Files.readAllBytes(file.toPath());
                switch (type) {
                case GWS:
                    return addGlycanFromGWSFile(fileContent, noGlytoucanRegistration, p);
                case XML:
                    return addGlycanFromLibraryFile(fileContent, noGlytoucanRegistration, p);
                case TABSEPARATED:
                    return addGlycanFromCSVFile(fileContent, noGlytoucanRegistration, p);
                }
                ErrorMessage errorMessage = new ErrorMessage("filetype is not accepted");
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("filetype", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("File is not acceptable", errorMessage);
            } catch (IOException e) {
                ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("file", "NotValid"));
                throw new IllegalArgumentException("File cannot be read", errorMessage);
            }
    	    
        }
	}
	
	@SuppressWarnings("rawtypes")
    private BatchGlycanUploadResult addGlycanFromLibraryFile (byte[] contents, Boolean noGlytoucanRegistration, Principal p) {
	    UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
	    BatchGlycanUploadResult result = new BatchGlycanUploadResult();
	    try {
	        ByteArrayInputStream stream = new ByteArrayInputStream(contents);
            InputStreamReader reader2 = new InputStreamReader(stream, "UTF-8");
            List<Class> contextList = new ArrayList<Class>(Arrays.asList(FilterUtils.filterClassContext));
            contextList.add(ArrayDesignLibrary.class);
            JAXBContext context2 = JAXBContext.newInstance(contextList.toArray(new Class[contextList.size()]));
            Unmarshaller unmarshaller2 = context2.createUnmarshaller();
            ArrayDesignLibrary library = (ArrayDesignLibrary) unmarshaller2.unmarshal(reader2);
            List<org.grits.toolbox.glycanarray.library.om.feature.Glycan> glycanList = library.getFeatureLibrary().getGlycan();
            int count = 0;
            int countSuccess = 0;
            for (org.grits.toolbox.glycanarray.library.om.feature.Glycan glycan : glycanList) {
                count++;
                Glycan view = null;
                if (glycan.getSequence() == null) {
                    if (glycan.getOrigSequence() == null && (glycan.getClassification() == null || glycan.getClassification().isEmpty())) {
                        // this is not a glycan, it is either control or a flag
                        // do not create a glycan
                    } else {
                        view = new UnknownGlycan();
                    }
                } else {
                    view = new SequenceDefinedGlycan();
                    ((SequenceDefinedGlycan) view).setGlytoucanId(glycan.getGlyTouCanId());
                    ((SequenceDefinedGlycan) view).setSequence(glycan.getSequence().trim());
                    ((SequenceDefinedGlycan) view).setSequenceType(GlycanSequenceFormat.GLYCOCT);
                }
                try {
                    view.setInternalId(glycan.getId()+ "");
                    view.setName(glycan.getName());
                    view.setDescription(glycan.getComment());   
                    String id = addGlycan(view, p, noGlytoucanRegistration);
                    Glycan addedGlycan = glycanRepository.getGlycanById(id, user);
                    result.getAddedGlycans().add(addedGlycan);
                    countSuccess ++;
                } catch (Exception e) {
                    logger.error ("Exception adding the glycan: " + glycan.getName(), e);
                    if (e.getCause() instanceof ErrorMessage) {
                        if (((ErrorMessage)e.getCause()).toString().contains("Duplicate")) {
                            ErrorMessage error = (ErrorMessage)e.getCause();
                            if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                                ObjectError err = error.getErrors().get(0);
                                if (err.getCodes().length != 0) {
                                    Glycan duplicateGlycan = new Glycan();
                                    try {
                                        duplicateGlycan = glycanRepository.getGlycanById(err.getCodes()[0], user);
                                        result.addDuplicateSequence(duplicateGlycan);
                                    } catch (SparqlException | SQLException e1) {
                                        logger.error("Error retrieving duplicate glycan", e1);
                                        result.addWrongSequence(count + ":" + glycan.getName() + ". Reason: " + ((ErrorMessage)e.getCause()).toString());
                                    }
                                }
                            } else {
                                result.addWrongSequence(count + ":" + glycan.getName() + ". Reason: " + ((ErrorMessage)e.getCause()).toString());
                            }
                        } else {
                            result.addWrongSequence(count + ":" + glycan.getName() + ". Reason: " + ((ErrorMessage)e.getCause()).toString());
                        }
                    } else { 
                        result.addWrongSequence(count + ":" + glycan.getName());
                    }
                } 
            }
            stream.close();
            result.setSuccessMessage(countSuccess + " out of " + count + " glycans are added");
            return result;
	    } catch (Exception e) {
	        ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("file", "NotValid"));
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
	        throw new IllegalArgumentException("File is not valid.", errorMessage);
	    } 
	}
	
	private BatchGlycanUploadResult addGlycanFromCSVFile (byte[] contents, Boolean noGlytoucanRegistration, Principal p) {
	    ParserConfiguration config = new ParserConfiguration();
	    config.setNameColumn(0);
	    config.setIdColumn(1);
	    config.setGlytoucanIdColumn(2);
	    config.setSequenceColumn(3);
	    config.setSequenceTypeColumn(4);
	    config.setMassColumn(5);
	    config.setCommentColumn(6);
	    
	    UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        BatchGlycanUploadResult result = new BatchGlycanUploadResult();
        try {
            ByteArrayInputStream stream = new   ByteArrayInputStream(contents);
            String fileAsString = IOUtils.toString(stream, StandardCharsets.UTF_8);
            Scanner scan = new Scanner(fileAsString);
            int count = 0;
            int countSuccess = 0;
            while(scan.hasNext()){
                String curLine = scan.nextLine();
                String[] splitted = curLine.split("\t");
                if (splitted.length == 0)
                    continue;
                String firstColumn = splitted[0].trim();
                if (firstColumn.equalsIgnoreCase("name")) {
                    continue; // skip the header line
                }
                count++;
                String glycanName = splitted[config.getNameColumn()];
                String glytoucanId = splitted[config.getGlytoucanIdColumn()];
                String sequence = splitted[config.getSequenceColumn()];
                String sequenceType = splitted[config.getSequenceTypeColumn()];
                String mass = splitted[config.getMassColumn()];
                String internalId = splitted[config.getIdColumn()];
                String comments = splitted[config.getCommentColumn()];
                
                Glycan glycan = null;
                if ((glytoucanId == null || glytoucanId.isEmpty()) && (sequence == null || sequence.isEmpty())) {
                    // mass only glycan
                    if (mass != null && !mass.trim().isEmpty()) {
                        glycan = new MassOnlyGlycan(); 
                        try {
                            ((MassOnlyGlycan) glycan).setMass(Double.parseDouble(mass.trim()));
                        } catch (NumberFormatException e) {
                            // add to error list
                            result.addWrongSequence(count + ":" + mass);
                        }
                    } else {
                        // ERROR
                        result.addWrongSequence(count + ": No sequence and mass information found");
                    }
                } else {
                    // sequence defined glycan
                    glycan = new SequenceDefinedGlycan();
                    if (glytoucanId != null && !glytoucanId.trim().isEmpty()) {
                        // use glytoucanid to retrieve the sequence
                        String glycoCT = getSequenceFromGlytoucan(glytoucanId.trim());
                        if (glycoCT != null && glycoCT.startsWith("RES")) {
                            ((SequenceDefinedGlycan) glycan).setSequence(glycoCT.trim());
                            ((SequenceDefinedGlycan) glycan).setSequenceType(GlycanSequenceFormat.GLYCOCT);
                        } else if (glycoCT != null && glycoCT.startsWith("WURCS")){
                            ((SequenceDefinedGlycan) glycan).setSequence(glycoCT.trim());
                            ((SequenceDefinedGlycan) glycan).setSequenceType(GlycanSequenceFormat.WURCS);
                        }
                    } else if (sequence != null && !sequence.trim().isEmpty()) {
                        ((SequenceDefinedGlycan) glycan).setSequence(sequence.trim());
                        ((SequenceDefinedGlycan) glycan).setSequenceType(GlycanSequenceFormat.forValue(sequenceType));
                    }
                }
                if (internalId != null) glycan.setInternalId(internalId.trim());
                if (glycanName != null) glycan.setName(glycanName.trim());
                if (comments != null) glycan.setDescription(comments);
                try {  
                    String id = addGlycan(glycan, p, noGlytoucanRegistration);
                    Glycan addedGlycan = glycanRepository.getGlycanById(id, user);
                    result.getAddedGlycans().add(addedGlycan);
                    countSuccess ++;
                } catch (Exception e) {
                    logger.error ("Exception adding the glycan: " + glycan.getName(), e);
                    if (e.getCause() instanceof ErrorMessage) {
                        if (((ErrorMessage)e.getCause()).toString().contains("Duplicate")) {
                            ErrorMessage error = (ErrorMessage)e.getCause();
                            if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                                ObjectError err = error.getErrors().get(0);
                                if (err.getCodes().length != 0) {
                                    Glycan duplicateGlycan = new Glycan();
                                    try {
                                        duplicateGlycan = glycanRepository.getGlycanById(err.getCodes()[0], user);
                                        result.addDuplicateSequence(duplicateGlycan);
                                    } catch (SparqlException | SQLException e1) {
                                        logger.error("Error retrieving duplicate glycan", e1);
                                        result.addWrongSequence(count + ":" + glycan.getName() + ". Reason: " + ((ErrorMessage)e.getCause()).toString());
                                    }
                                }
                            } else {
                                result.addWrongSequence(count + ":" + glycan.getName() + ". Reason: " + ((ErrorMessage)e.getCause()).toString());
                            }
                        } else {
                            result.addWrongSequence(count + ":" + glycan.getName() + ". Reason: " + ((ErrorMessage)e.getCause()).toString());
                        }
                    } else { 
                        result.addWrongSequence(count + ":" + glycan.getName());
                    }
                } 
            }
            scan.close();
            stream.close();
            result.setSuccessMessage(countSuccess + " out of " + count + " glycans are added");
            return result;
        } catch (IOException e) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("file", "NotValid"));
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            throw new IllegalArgumentException("File is not valid.", errorMessage);
        }
    }
	
	private BatchGlycanUploadResult addGlycanFromGWSFile (byte[] contents, Boolean noGlytoucanRegistration, Principal p) {
	    UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
	    BatchGlycanUploadResult result = new BatchGlycanUploadResult();
        try {
            ByteArrayInputStream stream = new   ByteArrayInputStream(contents);
            String fileAsString = IOUtils.toString(stream, StandardCharsets.UTF_8);
            
            boolean isTextFile = Charset.forName("US-ASCII").newEncoder().canEncode(fileAsString);
            if (!isTextFile) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("file", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("File is not acceptable", errorMessage);
            }
            
            String[] structures = fileAsString.split(";");
            int count = 0;
            int countSuccess = 0;
            for (String sequence: structures) {
                count++;
                try {
                    org.eurocarbdb.application.glycanbuilder.Glycan glycanObject = 
                            org.eurocarbdb.application.glycanbuilder.Glycan.fromString(sequence);
                    if (glycanObject == null) {
                        // sequence is not valid, ignore and add to the list of failed glycans
                        result.addWrongSequence(count + ":" + sequence);
                    } else {
                        String glycoCT = glycanObject.toGlycoCTCondensed();
                        if (glycoCT == null || glycoCT.isEmpty()) {
                            result.addWrongSequence(count + ":" + sequence);
                        } else {
                            SequenceDefinedGlycan g = new SequenceDefinedGlycan();
                            g.setSequence(glycoCT);
                            g.setSequenceType(GlycanSequenceFormat.GLYCOCT);
                            g.setMass(computeMass(glycanObject));
                            String existing = glycanRepository.getGlycanBySequence(glycoCT, user);
                            if (existing != null) {
                                // duplicate, ignore
                                String id = existing.substring(existing.lastIndexOf("/")+1);
                                Glycan glycan = glycanRepository.getGlycanById(id, user);
                                result.addDuplicateSequence(glycan);
                            } else {
                                String id = addGlycan(g, p, noGlytoucanRegistration);
                                Glycan addedGlycan = glycanRepository.getGlycanById(id, user);
                                result.getAddedGlycans().add(addedGlycan);
                                countSuccess ++;
                            }
                        }
                    }
                }
                catch (SparqlException e) {
                    // cannot add glycan
                    stream.close();
                    throw new GlycanRepositoryException("Glycans cannot be added. Reason: " + e.getMessage());
                } catch (Exception e) {
                    logger.error ("Exception adding the sequence: " + sequence, e);
                    // sequence is not valid
                    result.addWrongSequence(count + ":" + sequence);
                }
            }
            stream.close();
            result.setSuccessMessage(countSuccess + " out of " + count + " glycans are added");
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException("File is not valid. Reason: " + e.getMessage());
        }
	}
	
	@ApiOperation(value = "Add given linker for the user")
	@RequestMapping(value="/addlinker", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added linker"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register linkers"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public String addLinker (
			@ApiParam(required=true, value="Linker to be added, only pubChemId is required, other fields are optional") 
			@RequestBody Linker linker, Principal p) {
		
		if (linker.getType() == null) {
			// assume sequenceDefinedGlycan
			linker.setType(LinkerType.SMALLMOLECULE_LINKER);
		}
		
		switch (linker.getType()) {
		case SMALLMOLECULE_LINKER: 
			return addSmallMoleculeLinker((SmallMoleculeLinker)linker, p);
		case PEPTIDE_LINKER:
			return addPeptideLinker ((PeptideLinker) linker, p);
		case PROTEIN_LINKER:
			return addProteinLinker((ProteinLinker) linker, p);
		}
		throw new GlycanRepositoryException("Incorrect linker type");
	}

	private String addMassOnlyGlycan(MassOnlyGlycan glycan, Principal p) {
		if (glycan.getMass() == null) {
			ErrorMessage errorMessage = new ErrorMessage("Mass cannot be empty");
			errorMessage.addError(new ObjectError("mass", "NoEmpty"));
			throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
		}
		
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		// validate first
		if (validator != null) {
			if  (glycan.getName() != null) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "name", glycan.getName());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (glycan.getDescription() != null) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "description", glycan.getDescription());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("description", "LengthExceeded"));
				}		
			}
			if (glycan.getInternalId() != null && !glycan.getInternalId().isEmpty()) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "internalId", glycan.getInternalId());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("internalId", "LengthExceeded"));
				}		
			}
			
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		
		UserEntity user = null;
		try {
			user = userRepository.findByUsernameIgnoreCase(p.getName());
			// TODO if name is null, check by mass to make sure there are no other glycans with the same mass and no name
			Glycan local = null;
			// check if internalid and label are unique
			if (glycan.getInternalId() != null && !glycan.getInternalId().trim().isEmpty()) {
				local = glycanRepository.getGlycanByInternalId(glycan.getInternalId().trim(), user);
				if (local != null) {
				    glycan.setId(local.getId());
					errorMessage.addError(new ObjectError("internalId", "Duplicate"));
				}
			}
			if (glycan.getName() != null && !glycan.getName().trim().isEmpty()) {
				local = glycanRepository.getGlycanByLabel(glycan.getName().trim(), user);
				if (local != null) {
				    glycan.setId(local.getId());
					errorMessage.addError(new ObjectError("name", "Duplicate"));
				}
			} 
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Glycan cannot be added for user " + p.getName(), e);
		}
				
		if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
		try {	
			// no errors add the glycan
			String glycanURI = glycanRepository.addGlycan(glycan, user);
			return glycanURI.substring(glycanURI.lastIndexOf("/")+1);
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Glycan cannot be added for user " + p.getName(), e);
		}
	}

	private String addPeptideLinker(PeptideLinker linker, Principal p) {
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		
		if (linker.getSequence() == null)  {
			errorMessage.addError(new ObjectError("sequence", "NoEmpty"));
		} 
		
		// validate first
		if (validator != null) {
			if (linker.getDescription() != null) {
				Set<ConstraintViolation<Linker>> violations = validator.validateValue(Linker.class, "description", linker.getDescription().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("description", "LengthExceeded"));
				}		
			}
			
			if  (linker.getName() != null) {
				Set<ConstraintViolation<Linker>> violations = validator.validateValue(Linker.class, "name", linker.getName().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (linker.getComment() != null) {
				Set<ConstraintViolation<Linker>> violations = validator.validateValue(Linker.class, "comment", linker.getComment().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("comment", "LengthExceeded"));
				}		
			}
		
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		
		try {
			Linker l = null;
			String linkerURI = null;
			if (linker.getSequence() != null) {
			    // modify the sequence to add position markers
			    try {
                    linker.setSequence(addPositionToSequence(linker.getSequence().trim()));
                } catch (Exception e) {
                    errorMessage.addError(new ObjectError("sequence", "NotValid"));
                }
				linkerURI = linkerRepository.getLinkerByField(linker.getSequence(), "has_sequence", "string", user);
				if (linkerURI != null) {
				    linker.setUri(linkerURI);
					errorMessage.addError(new ObjectError("sequence", "Duplicate"));
				}
			}
			
			
			l = linker;
			l.setUri(linkerURI);
			if (linker.getName() != null && !linker.getName().trim().isEmpty()) {
				Linker local = linkerRepository.getLinkerByLabel(linker.getName().trim(), user);
				if (local != null) {
				    linker.setId(local.getId());
					errorMessage.addError(new ObjectError("name", "Duplicate"));	
				}
			}
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage);
			
			
			String addedURI = linkerRepository.addLinker(l, user);
			return addedURI.substring(addedURI.lastIndexOf("/")+1);
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Linker cannot be added for user " + p.getName(), e);
		} 
	}

	private String addProteinLinker(ProteinLinker linker, Principal p) {
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		
		
		if (linker.getUniProtId() == null && linker.getSequence() == null)  { // at least one of them should be provided
			errorMessage.addError(new ObjectError("sequence", "NoEmpty"));
		} 
	
		// validate first
		if (validator != null) {
			if (linker.getDescription() != null) {
				Set<ConstraintViolation<Linker>> violations = validator.validateValue(Linker.class, "description", linker.getDescription().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("description", "LengthExceeded"));
				}		
			}
			
			if  (linker.getName() != null) {
				Set<ConstraintViolation<Linker>> violations = validator.validateValue(Linker.class, "name", linker.getName().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (linker.getComment() != null) {
				Set<ConstraintViolation<Linker>> violations = validator.validateValue(Linker.class, "comment", linker.getComment().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("comment", "LengthExceeded"));
				}		
			}
		
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		
		try {
			Linker l = null;
			String linkerURI = null;
			
			if (linker.getSequence() != null && !linker.getSequence().trim().isEmpty()) {
			    // modify the sequence to add position markers
                try {
                    String sequence = addPositionToSequence(linker.getSequence().trim());
                    linker.setSequence(sequence);
                } catch (Exception e) {
                    errorMessage.addError(new ObjectError("sequence", "NotValid"));
                }
				linkerURI = linkerRepository.getLinkerByField(linker.getSequence(), "has_sequence", "string", user);
				if (linkerURI != null) {
				    linker.setUri(linkerURI);
				    errorMessage.addError(new ObjectError("sequence", "Duplicate"));
				}
			}
			else if (linker.getUniProtId() != null && !linker.getUniProtId().trim().isEmpty()) {
				linkerURI = linkerRepository.getLinkerByField(linker.getUniProtId(), "has_uniProtId", "string", user);
				if (linkerURI != null) {
				    linker.setUri(linkerURI);
				    errorMessage.addError(new ObjectError("uniProtId", "Duplicate"));
				}
			}
			
			l = linker;
			l.setUri(linkerURI);
			if (linker.getName() != null && !linker.getName().trim().isEmpty()) {
				Linker local = linkerRepository.getLinkerByLabel(linker.getName().trim(), user);
				if (local != null) {
				    linker.setId(local.getId());
					errorMessage.addError(new ObjectError("name", "Duplicate"));	
				}
			}
			
			if (linker.getSequence() == null && linker.getUniProtId() != null) {
			    // try to retrieve sequence from Uniprot
			    String sequence = UniProtUtil.getSequenceFromUniProt(linker.getUniProtId());
			    if (sequence == null) {
			        errorMessage.addError(new ObjectError("uniProtId", "NotValid"));
			    } else {
			        ((SequenceBasedLinker)l).setSequence(sequence);
			    }
			} 
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage);
			
			String addedURI = linkerRepository.addLinker(l, user);
			return addedURI.substring(addedURI.lastIndexOf("/")+1);
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Linker cannot be added for user " + p.getName(), e);
		} 
	}
	
	private String addPositionToSequence(String sequence) throws Exception {
	    String newSequence = "";
        Stack<Character> stack = new Stack<Character>();
        int position = 1;
        int i=0;
        boolean begin = false;
        boolean end = false;
        boolean aminoAcid = false;
        while (i < sequence.length()) {
            if (sequence.charAt(i) == '{') {
                stack.push(new Character('{'));
                newSequence += "{" + position + "-";
                begin = true;
            } else if (sequence.charAt(i) == '}') {
                if (stack.isEmpty()) 
                    throw new Exception ("ParseError: no opening curly");
                stack.pop();
                position ++;
                newSequence += "}";
                end = true;
            } else {
                newSequence += sequence.charAt(i);
                if (begin && !end)
                	aminoAcid = true;
            }
            i++;
            if (begin && end && !aminoAcid) {
            	throw new Exception ("ParseError: no aminoacid between curly braces");
            } 
            if (begin && end && aminoAcid) {
            	// start over
            	begin = false;
            	end = false;
            	aminoAcid = false;
            }
        }
        if (!stack.isEmpty()) {
            throw new Exception ("ParseError: Curly braces error");
        }  
        return newSequence;
    }

    private String addSequenceDefinedGlycan (SequenceDefinedGlycan glycan, Principal p, Boolean noGlytoucanRegistration) {
		if (glycan.getSequence() == null || glycan.getSequence().trim().isEmpty()) {
			ErrorMessage errorMessage = new ErrorMessage("Sequence cannot be empty");
			errorMessage.addError(new ObjectError("sequence", "NoEmpty"));
			throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
		}
		
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		// validate first
		if (validator != null) {
			if  (glycan.getName() != null) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "name", glycan.getName());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (glycan.getDescription() != null) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "description", glycan.getDescription());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("description", "LengthExceeded"));
				}		
			}
			if (glycan.getInternalId() != null && !glycan.getInternalId().isEmpty()) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "internalId", glycan.getInternalId());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("internalId", "LengthExceeded"));
				}		
			}
			if (glycan.getGlytoucanId() != null && !glycan.getGlytoucanId().isEmpty()) {
				Set<ConstraintViolation<SequenceDefinedGlycan>> violations = validator.validateValue(SequenceDefinedGlycan.class, "glytoucanId", glycan.getGlytoucanId());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("glytoucanId", "LengthExceeded"));
				}		
			}
			
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		
		org.eurocarbdb.application.glycanbuilder.Glycan glycanObject= null;
		SequenceDefinedGlycan g = new SequenceDefinedGlycan();
		g.setName(glycan.getName() != null ? glycan.getName().trim() : glycan.getName());
		g.setGlytoucanId(glycan.getGlytoucanId() != null ? glycan.getGlytoucanId().trim() : glycan.getGlytoucanId());
		g.setInternalId(glycan.getInternalId() != null ? glycan.getInternalId().trim(): glycan.getInternalId());
		g.setDescription(glycan.getDescription() != null ? glycan.getDescription().trim() : glycan.getDescription());
		
		String glycoCT = glycan.getSequence().trim();
		UserEntity user;
		boolean gwbError = false;
		try {
			user = userRepository.findByUsernameIgnoreCase(p.getName());
			
			if (glycan.getSequence() != null && !glycan.getSequence().trim().isEmpty()) {
				//check if the given sequence is valid
				
				boolean parseError = false;
				
				try {
					switch (glycan.getSequenceType()) {
					case GLYCOCT:
					    try {
					        glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromGlycoCTCondensed(glycan.getSequence().trim());
					        if (glycanObject == null) 
	                            gwbError = true;
					        else 
					            glycoCT = glycanObject.toGlycoCTCondensed(); // required to fix formatting errors like extra line break etc.
					    } catch (Exception e) {
					        logger.error("Glycan builder parse error", e);
					        gwbError = true;
					    }
					    
					    if (gwbError) {
					        // check to make sure GlycoCT valid without using GWB
                            SugarImporterGlycoCTCondensed importer = new SugarImporterGlycoCTCondensed();
                            try {
                                Sugar sugar = importer.parse(glycan.getSequence().trim());
                                if (sugar == null) {
                                    logger.error("Cannot get Sugar object for sequence:" + glycan.getSequence().trim());
                                    parseError = true;
                                    gwbError = false;  
                                } else {
                                    SugarExporterGlycoCTCondensed exporter = new SugarExporterGlycoCTCondensed();
                                    exporter.start(sugar);
                                    glycoCT = exporter.getHashCode();
                                    // calculate mass
                                    GlycoVisitorMass massVisitor = new GlycoVisitorMass();
                                    massVisitor.start(sugar);
                                    g.setMass(massVisitor.getMass(GlycoVisitorMass.DERIVATISATION_NONE));
                                }
                            } catch (Exception pe) {
                                logger.error("GlycoCT parsing failed", pe);
                                parseError = true;
                                gwbError = false;
                            }
					    } else {
					        g.setMass(computeMass(glycanObject));
					    }
						g.setSequence(glycoCT);
						g.setSequenceType(GlycanSequenceFormat.GLYCOCT);
						break;
					case GWS:
						glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromString(glycan.getSequence().trim());
						glycoCT = glycanObject.toGlycoCTCondensed();
						g.setMass(computeMass(glycanObject));
						g.setSequence(glycoCT);
                        g.setSequenceType(GlycanSequenceFormat.GLYCOCT);
						break;
					case WURCS:
					    WURCSToGlycoCT wurcsConverter = new WURCSToGlycoCT();
					    wurcsConverter.start(glycan.getSequence().trim());
					    glycoCT = wurcsConverter.getGlycoCT();
					    if (glycoCT != null) {
					        glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromGlycoCTCondensed(glycoCT);
					        g.setMass(computeMass(glycanObject));
	                        g.setSequence(glycoCT);
	                        g.setSequenceType(GlycanSequenceFormat.GLYCOCT);
					    } else { // keep the sequence as WURCS
					        g.setSequence(glycan.getSequence().trim());
					        g.setSequenceType(GlycanSequenceFormat.WURCS);
					        WURCS2Parser t_wurcsparser = new WURCS2Parser();
					        MassOptions massOptions = new MassOptions();
				            massOptions.setDerivatization(MassOptions.NO_DERIVATIZATION);
				            massOptions.setIsotope(MassOptions.ISOTOPE_MONO);
				            massOptions.ION_CLOUD = new IonCloud();
				            massOptions.NEUTRAL_EXCHANGES = new IonCloud();
				            ResidueType m_residueFreeEnd = ResidueDictionary.findResidueType("freeEnd");
				            massOptions.setReducingEndType(m_residueFreeEnd);
                            glycanObject = t_wurcsparser.readGlycan(g.getSequence(), massOptions);
					        g.setMass(computeMass(glycanObject));
					    }
					    break;
                    case IUPAC:
                        CFGMasterListParser parser = new CFGMasterListParser();
                        glycoCT = parser.translateSequence(ExtendedGalFileParser.cleanupSequence(glycan.getSequence().trim()));
                        if (glycoCT != null) {
                            glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromGlycoCTCondensed(glycoCT);
                            g.setMass(computeMass(glycanObject));
                            g.setSequence(glycoCT);
                            g.setSequenceType(GlycanSequenceFormat.GLYCOCT);
                        }
                        break;
                    default:
                        break;
					}
				} catch (Exception e) {
					// parse error
					parseError = true;
					logger.error("Parse Error for sequence: " + glycan.getSequence());
					
				}
				// check for all possible errors 
				if (glycanObject == null && !gwbError) {
					parseError = true;
					logger.error("Parse Error for sequence: " + glycan.getSequence());
				} else {
					String existingURI = glycanRepository.getGlycanBySequence(g.getSequence(), user);
					if (existingURI != null && !existingURI.contains("public")) {
					    glycan.setId(existingURI.substring(existingURI.lastIndexOf("/")+1));
					    String[] codes = {existingURI.substring(existingURI.lastIndexOf("/")+1)};
						errorMessage.addError(new ObjectError("sequence", codes, null, "Duplicate"));
					}
				}
				if (parseError)
					errorMessage.addError(new ObjectError("sequence", "NotValid"));
			} else {
				errorMessage.addError(new ObjectError("sequence", "NoEmpty"));
			}
			Glycan local = null;
			// check if internalid and label are unique
			if (glycan.getInternalId() != null && !glycan.getInternalId().trim().isEmpty()) {
				local = glycanRepository.getGlycanByInternalId(glycan.getInternalId().trim(), user);
				if (local != null) {
				    glycan.setId(local.getId());
				    String[] codes = {local.getId()};
					errorMessage.addError(new ObjectError("internalId", codes, null, "Duplicate"));
				}
			}
			if (glycan.getName() != null && !glycan.getName().trim().isEmpty()) {
				local = glycanRepository.getGlycanByLabel(glycan.getName().trim(), user);
				if (local != null) {
				    glycan.setId(local.getId());
				    String[] codes = {local.getId()};
					errorMessage.addError(new ObjectError("name", codes, null, "Duplicate"));
				}
			} 
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Glycan cannot be added for user " + p.getName(), e);
		}
				
		if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
			throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
		
		try {	
			// no errors add the glycan
			if (glycanObject != null || gwbError) {
				//if (!gwbError) g.setMass(computeMass(glycanObject));
				//g.setSequence(glycoCT);
				//g.setSequenceType(GlycanSequenceFormat.GLYCOCT);
				
				String glycanURI = glycanRepository.addGlycan(g, user, noGlytoucanRegistration);
				String id = glycanURI.substring(glycanURI.lastIndexOf("/")+1);
				Glycan added = glycanRepository.getGlycanById(id, user);
				if (added != null) {
				    BufferedImage t_image = createImageForGlycan(g);
	                if (t_image != null) {
						String filename = id + ".png";
						//save the image into a file
						logger.debug("Adding image to " + imageLocation);
						File imageFile = new File(imageLocation + File.separator + filename);
						ImageIO.write(t_image, "png", imageFile);
	                } else {
	                    logger.warn ("Glycan image cannot be generated for glycan " + g.getName());
	                }
				} else {
					logger.error("Added glycan cannot be retrieved back");
					throw new GlycanRepositoryException("Glycan could not be added");
				}
				return id;
			}
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Glycan cannot be added for user " + p.getName(), e);
		} catch (IOException e) {
			logger.error("Glycan image cannot be generated", e);
			throw new GlycanRepositoryException("Glycan image cannot be generated", e);
		} catch (Exception e) {
			throw new GlycanRepositoryException("Glycan cannot be added for user " + p.getName(), e);
		}
		return null;
	}
    
    Double computeMass (org.eurocarbdb.application.glycanbuilder.Glycan glycanObject) {
        if (glycanObject != null) {
            MassOptions massOptions = new MassOptions();
            massOptions.setDerivatization(MassOptions.NO_DERIVATIZATION);
            massOptions.setIsotope(MassOptions.ISOTOPE_MONO);
            massOptions.ION_CLOUD = new IonCloud();
            massOptions.NEUTRAL_EXCHANGES = new IonCloud();
            ResidueType m_residueFreeEnd = ResidueDictionary.findResidueType("freeEnd");
            massOptions.setReducingEndType(m_residueFreeEnd);
            glycanObject.setMassOptions(massOptions);
            return glycanObject.computeMass();
        } 
        return null;
    }
	
	@ApiOperation(value = "Add given slide layout for the user")
	@RequestMapping(value="/addslidelayout", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Slide layout added successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register slide layouts"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public String addSlideLayout (
			@ApiParam(required=true, value="Slide Layout to be added, name, width, height and blocks are required")
			@RequestBody SlideLayout layout, Principal p) {
		
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		
		if (layout.getName() == null || layout.getName().trim().isEmpty()) {
			errorMessage.addError(new ObjectError("name", "NoEmpty"));
		} 
		if (layout.getWidth() == null || layout.getHeight() == null)
			errorMessage.addError(new ObjectError(layout.getWidth() == null ? "width" : "height", "NoEmpty"));
		if (layout.getBlocks() == null)
			errorMessage.addError(new ObjectError("blocks", "NoEmpty"));
		
		// validate first
		if (validator != null) {
			if  (layout.getName() != null) {
				Set<ConstraintViolation<BlockLayout>> violations = validator.validateValue(BlockLayout.class, "name", layout.getName());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (layout.getDescription() != null) {
				Set<ConstraintViolation<BlockLayout>> violations = validator.validateValue(BlockLayout.class, "description", layout.getDescription());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("description", "LengthExceeded"));
				}		
			}
			if (layout.getWidth() != null) {
				Set<ConstraintViolation<BlockLayout>> violations = validator.validateValue(BlockLayout.class, "width", layout.getWidth());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("width", "PositiveOnly"));
				}		
			}
			if (layout.getHeight() != null) {
				Set<ConstraintViolation<BlockLayout>> violations = validator.validateValue(BlockLayout.class, "height", layout.getHeight());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("height", "PositiveOnly"));
				}		
			}
			
			
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		try {
			SlideLayout existing = layoutRepository.getSlideLayoutByName(layout.getName(), user);
			if (existing != null) {
				// duplicate
				errorMessage.addError(new ObjectError("name", "Duplicate"));
			}
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Slide layout cannot be added for user " + p.getName(), e);
		}
		
		if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
			throw new IllegalArgumentException("Invalid Input: Not a valid slide layout information", errorMessage);
		
		try {
			String uri = layoutRepository.addSlideLayout(layout, user);
			String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Slide layout cannot be added for user " + p.getName(), e);
		}
	}
	
	private String addSmallMoleculeLinker(SmallMoleculeLinker linker, Principal p) {
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		
		if (linker.getClassification() == null && linker.getPubChemId() == null && linker.getInChiKey() == null) {   // at least one of them should be provided
			errorMessage.addError(new ObjectError("classification", "NoEmpty"));
		} 
	
		// validate first
		if (validator != null) {
			if (linker.getDescription() != null) {
				Set<ConstraintViolation<Linker>> violations = validator.validateValue(Linker.class, "description", linker.getDescription().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("description", "LengthExceeded"));
				}		
			}
			
			if  (linker.getName() != null) {
				Set<ConstraintViolation<Linker>> violations = validator.validateValue(Linker.class, "name", linker.getName().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (linker.getComment() != null) {
				Set<ConstraintViolation<Linker>> violations = validator.validateValue(Linker.class, "comment", linker.getComment().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("comment", "LengthExceeded"));
				}		
			}
		
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		
		try {
			Linker l = null;
			String linkerURI = null;
			if (linker.getPubChemId() != null) {
				linkerURI = linkerRepository.getLinkerByField(linker.getPubChemId().toString(), "has_pubchem_compound_id", "long", user);
				if (linkerURI != null) {
				    linker.setUri(linkerURI);
					errorMessage.addError(new ObjectError("pubChemId", "Duplicate"));
				}
			}
			else if (linker.getInChiKey() != null && !linker.getInChiKey().trim().isEmpty()) {
				linkerURI = linkerRepository.getLinkerByField(linker.getInChiKey(), "has_inChI_key", "string", user);
				if (linkerURI != null) {
				    linker.setUri(linkerURI);
					errorMessage.addError(new ObjectError("inChiKey", "Duplicate"));	
				}
			}
			if (linkerURI == null) {
				// get the linker details from pubChem
			    ObjectError err = new ObjectError("pubChemId", "NotValid");
				if (linker.getPubChemId() != null || (linker.getInChiKey() != null && !linker.getInChiKey().trim().isEmpty())) {
					try {
						if (linker.getPubChemId() != null) {
						    err = new ObjectError("pubChemId", "NotValid");
						    l = PubChemAPI.getLinkerDetailsFromPubChem(linker.getPubChemId());
						} else if (linker.getInChiKey() != null && !linker.getInChiKey().trim().isEmpty()) {
						    err = new ObjectError("inchiKey", "NotValid");
						    l = PubChemAPI.getLinkerDetailsFromPubChemByInchiKey(linker.getInChiKey());
						}
						if (l == null) {
							// could not get details from PubChem
							errorMessage.addError(err);
						} else {
							if (linker.getName() != null) l.setName(linker.getName().trim());
							if (linker.getComment() != null) l.setComment(linker.getComment().trim());
							if (linker.getDescription() != null) l.setDescription(linker.getDescription().trim());
							if (linker.getOpensRing() != null) l.setOpensRing(linker.getOpensRing());
							if (((SmallMoleculeLinker)l).getClassification() == null)
								((SmallMoleculeLinker)l).setClassification (linker.getClassification());
						}
					} catch (Exception e) {
						// could not get details from PubChem
						errorMessage.addError(err);
					}
				}
				else {
					l = linker;
					l.setUri(linkerURI);
				}
				
				if (linker.getName() != null && !linker.getName().trim().isEmpty()) {
					Linker local = linkerRepository.getLinkerByLabel(linker.getName().trim(), user);
					if (local != null) {
					    linker.setId(local.getId());
						errorMessage.addError(new ObjectError("name", "Duplicate"));	
					}
				}
				
				// retrieve publication details
				if (l != null && linker.getPublications() != null && !linker.getPublications().isEmpty()) {
				    PubmedUtil util = new PubmedUtil();
				    for (Publication pub: linker.getPublications()) {
				        if (pub.getPubmedId() != null) {
				            try {
                                DTOPublication publication = util.createFromPubmedId(pub.getPubmedId());
                                l.addPublication (UtilityController.getPublicationFrom(publication));
                            } catch (Exception e) {
                                logger.error("Cannot retrieve details from PubMed", e);
                                errorMessage.addError(new ObjectError("pubMedId", "NotValid"));
                            }
				        }
				    }
				}
			} 
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage);
			
			if (l != null) {
				String addedURI = linkerRepository.addLinker(l, user);
				return addedURI.substring(addedURI.lastIndexOf("/")+1);
			}
			
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Linker cannot be added for user " + p.getName(), e);
		} 
		
		return null;
	}
	
	@ApiOperation(value = "Checks whether the given slidelayout name is available to be used (returns true if available, false if alredy in use", response = Boolean.class)
	@RequestMapping(value = "/checkSlidelayoutName", method = RequestMethod.GET)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Check performed successfully"),
			@ApiResponse(code = 415, message = "Media type is not supported"),
			@ApiResponse(code = 500, message = "Internal Server Error") })
	public Boolean checkSlidelayoutName(@RequestParam("slidelayoutname") final String slidelayoutname, Principal principal) throws SparqlException, SQLException {

		UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
		
		SlideLayout existing = layoutRepository.getSlideLayoutByName(slidelayoutname, user);

		if (existing != null) {
			// duplicate
			ErrorMessage errorMessage = new ErrorMessage("Cannot add duplicate slide layout");
			errorMessage.addError(new ObjectError("slidelayoutname", "Duplicate"));
			throw new GlycanExistsException("A slide layout with the same name already exists", errorMessage);
		}

		return true;
	}
	
	@ApiOperation(value = "Delete given block layout")
	@RequestMapping(value="/deleteblocklayout/{layoutId}", method = RequestMethod.DELETE, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Block Layout deleted successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to delete block layouts"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation deleteBlockLayout (
			@ApiParam(required=true, value="id of the block layout to delete") 
			@PathVariable("layoutId") String blockLayoutId, Principal principal) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			layoutRepository.deleteBlockLayout(blockLayoutId, user);
			return new Confirmation("Block Layout deleted successfully", HttpStatus.OK.value());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot delete block layout " + blockLayoutId);
		} 
	}
	
	@ApiOperation(value = "Delete given feature from the user's list")
	@RequestMapping(value="/deletefeature/{featureId}", method = RequestMethod.DELETE, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Feature deleted successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to delete linkers"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation deleteFeature (
			@ApiParam(required=true, value="id of the feature to delete") 
			@PathVariable("featureId") String featureId, Principal principal) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			featureRepository.deleteFeature(featureId, user);
			return new Confirmation("Feature deleted successfully", HttpStatus.OK.value());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot delete feature " + featureId, e);
		} 
	}
	
	@ApiOperation(value = "Delete given glycan from the user's list")
	@RequestMapping(value="/delete/{glycanId}", method = RequestMethod.DELETE, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Glycan deleted successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to delete glycans"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation deleteGlycan (
			@ApiParam(required=true, value="id of the glycan to delete") 
			@PathVariable("glycanId") String glycanId, Principal principal) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			glycanRepository.deleteGlycan(glycanId, user);
			return new Confirmation("Glycan deleted successfully", HttpStatus.OK.value());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot delete glycan " + glycanId, e);
		} 
	}
	
	@ApiOperation(value = "Delete given linker from the user's list")
	@RequestMapping(value="/deletelinker/{linkerId}", method = RequestMethod.DELETE, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Linker deleted successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to delete linkers"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation deleteLinker (
			@ApiParam(required=true, value="id of the linker to delete") 
			@PathVariable("linkerId") String linkerId, Principal principal) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			linkerRepository.deleteLinker(linkerId, user);
			return new Confirmation("Linker deleted successfully", HttpStatus.OK.value());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot delete linker " + linkerId, e);
		} 
	}
	
	@ApiOperation(value = "Delete given slide layout")
	@RequestMapping(value="/deleteslidelayout/{layoutId}", method = RequestMethod.DELETE, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Slide Layout deleted successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to delete slide layouts"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation deleteSlideLayout (
			@ApiParam(required=true, value="id of the block layout to delete") 
			@PathVariable("layoutId") String layoutId, Principal principal) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			layoutRepository.deleteSlideLayout(layoutId, user);
			return new Confirmation("Slide Layout deleted successfully", HttpStatus.OK.value());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot delete slide layout " + layoutId, e);
		} 
	}
	
	@ApiOperation(value = "Retrieve block layout with the given id")
	@RequestMapping(value="/getblocklayout/{layoutId}", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Block Layout retrieved successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list block layouts"),
			@ApiResponse(code=404, message="Block layout with given id does not exist"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public BlockLayout getBlockLayout (
			@ApiParam(required=true, value="id of the block layout to retrieve") 
			@PathVariable("layoutId") String layoutId, 
			@ApiParam (required=false, defaultValue = "true", value="if false, do not load block details. Default is true (to load all)")
			@RequestParam(required=false, defaultValue = "true", value="loadAll") Boolean loadAll, 
			Principal p) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
			BlockLayout layout = layoutRepository.getBlockLayoutById(layoutId, user, loadAll);
			if (layout == null) {
			    // check if it is from public graph
			    layout = layoutRepository.getBlockLayoutById(layoutId, null, loadAll);
			    if (layout == null) 
			        throw new EntityNotFoundException("Block layout with id : " + layoutId + " does not exist in the repository");
			}
			
			if (loadAll && layout.getSpots() != null) {        
                for (org.glygen.array.persistence.rdf.Spot s: layout.getSpots()) {
                    if (s.getFeatures() == null) 
                        continue;
                    for (org.glygen.array.persistence.rdf.Feature f: s.getFeatures()) {
                        if (f.getGlycans()  != null) {
                            for (Glycan g: f.getGlycans()) {
                                if (g instanceof SequenceDefinedGlycan && g.getCartoon() == null ) {
                                    byte[] image = getCartoonForGlycan(g.getId());
                                    if (image == null && ((SequenceDefinedGlycan) g).getSequence() != null) {
                                        // try to create one
                                        BufferedImage t_image = createImageForGlycan ((SequenceDefinedGlycan) g);
                                        if (t_image != null) {
                                            String filename = g.getId() + ".png";
                                            //save the image into a file
                                            logger.debug("Adding image to " + imageLocation);
                                            File imageFile = new File(imageLocation + File.separator + filename);
                                            try {
                                                ImageIO.write(t_image, "png", imageFile);
                                            } catch (IOException e) {
                                                logger.error ("Glycan image cannot be written", e);
                                            }
                                        }
                                        image = getCartoonForGlycan(g.getId());
                                    }
                                    
                                    g.setCartoon(image);
                                }
                            }
                        }
                    }
                }
		    }
			return layout;
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Block Layout cannot be retrieved for user " + p.getName(), e);
		}
	}
	
	private byte[] getCartoonForGlycan (String glycanId) {
		try {
			File imageFile = new File(imageLocation + File.separator + glycanId + ".png");
			if (imageFile.exists()) {
			    InputStreamResource resource = new InputStreamResource(new FileInputStream(imageFile));
			    return IOUtils.toByteArray(resource.getInputStream());
			}
		} catch (Exception e) {
			logger.warn("Image cannot be retrieved for glycan " + glycanId, e);
			
		}
		return null;
	}
	
	@SuppressWarnings("rawtypes")
    SlideLayout getFullLayoutFromLibrary (File libraryFile, SlideLayout layout) {
		try {
			FileInputStream inputStream2 = new FileInputStream(libraryFile);
	        InputStreamReader reader2 = new InputStreamReader(inputStream2, "UTF-8");
	        List<Class> contextList = new ArrayList<Class>(Arrays.asList(FilterUtils.filterClassContext));
    		contextList.add(ArrayDesignLibrary.class);
	        JAXBContext context2 = JAXBContext.newInstance(contextList.toArray(new Class[contextList.size()]));
	        Unmarshaller unmarshaller2 = context2.createUnmarshaller();
	        ArrayDesignLibrary library = (ArrayDesignLibrary) unmarshaller2.unmarshal(reader2);
	        List<org.grits.toolbox.glycanarray.library.om.layout.SlideLayout> layoutList = 
	        		library.getLayoutLibrary().getSlideLayout();
	        ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
	        for (org.grits.toolbox.glycanarray.library.om.layout.SlideLayout slideLayout : layoutList) {
	        	if (slideLayout.getName().equalsIgnoreCase(layout.getName()) ||
	        	        layout.getId().equals(slideLayout.getId().toString())) {
	        		SlideLayout mySlideLayout = new SlideLayout();
	        		mySlideLayout.setName(slideLayout.getName());
	        		String desc = null;
	        		if (slideLayout.getDescription() != null) {
	        			desc = slideLayout.getDescription();
	        			if (desc.length() >= ValidationConstants.DESCRIPTION_LIMIT) {
	        				desc = desc.substring(0, ValidationConstants.DESCRIPTION_LIMIT-1);
	        			}
	        		}
	        		mySlideLayout.setDescription(desc);
	        		List<org.glygen.array.persistence.rdf.Block> blocks = new ArrayList<>();
	        		int width = 0;
	        		int height = 0;
	        		for (Block block: slideLayout.getBlock()) {
	        			org.glygen.array.persistence.rdf.Block myBlock = new org.glygen.array.persistence.rdf.Block();
	        			myBlock.setColumn(block.getColumn());
	        			myBlock.setRow(block.getRow());
	        			if (block.getColumn() > width)
	        				width = block.getColumn();
	        			if (block.getRow() > height)
	        				height = block.getRow();
	        			Integer blockLayoutId = block.getLayoutId();
	        			org.grits.toolbox.glycanarray.library.om.layout.BlockLayout blockLayout = LibraryInterface.getBlockLayout(library, blockLayoutId);
	        			if (blockLayout == null) {
	        			    // it should have been in the file
	        	            errorMessage.addError(new ObjectError("blockLayout:" + blockLayoutId, "NotFound"));
	        	            continue;
	        			}
	        			org.glygen.array.persistence.rdf.BlockLayout myLayout = new org.glygen.array.persistence.rdf.BlockLayout();
	        			String name = null;
	        			if (blockLayout.getName() != null) {
	        				name = blockLayout.getName();
	        				if (name.length() >= ValidationConstants.NAME_LIMIT) {
	        					name = name.substring(0, ValidationConstants.NAME_LIMIT-1);
	        				}
	        			}
	        			myLayout.setName(name);
	        			myLayout.setWidth(blockLayout.getColumnNum());
	        			myLayout.setHeight(blockLayout.getRowNum());
	        			String comment = null;
	        			if (blockLayout.getComment() != null) {
	        				comment = blockLayout.getComment().replaceAll("\\r", " ").replaceAll("\\n", " ");
	        				if (comment.length() >= ValidationConstants.DESCRIPTION_LIMIT) {
	        					comment = comment.substring(0, ValidationConstants.DESCRIPTION_LIMIT-1);
	        				}
	        			}
	        			myLayout.setDescription(comment);
	        			myBlock.setBlockLayout(myLayout); 
	        			try {
    	        			List<org.glygen.array.persistence.rdf.Spot> spots = getSpotsFromBlockLayout(library, blockLayout);
    	        			//myBlock.setSpots(spots);
    	        			myLayout.setSpots(spots);
    	        			blocks.add(myBlock);
	        			} catch (Exception e) {
	        			    if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
	                            for (ObjectError err: ((ErrorMessage) e.getCause()).getErrors()) {
	                                errorMessage.addError(err);
	                            }
	                        } else {
	                            errorMessage.addError(new ObjectError("internalError", e.getMessage()));
	                        }
	        			}
	        		}
	        		
	        		mySlideLayout.setHeight(slideLayout.getHeight() == null ? height: slideLayout.getHeight());
	        		mySlideLayout.setWidth(slideLayout.getWidth() == null ? width: slideLayout.getWidth());
	        		mySlideLayout.setBlocks(blocks);
	        		
	        		if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
	        		    throw new IllegalArgumentException("Not a valid array library!", errorMessage);
	        		}
	        		return mySlideLayout;
	        	}
	        }
		} catch (JAXBException e) {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
			errorMessage.addError(new ObjectError("file", "NotValid"));
			throw new IllegalArgumentException("Not a valid array library!", errorMessage);
		} catch (IOException e) {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
			errorMessage.addError(new ObjectError("file", "NotValid"));
			throw new IllegalArgumentException("File cannot be found!", errorMessage);
		}
		return null;
	}
	
	@ApiOperation(value = "Import slide layout from uploaded GAL file")
    @RequestMapping(value = "/addSlideLayoutFromGalFile", method=RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added slide layout"), 
            @ApiResponse(code=400, message="Invalid request, file cannot be found"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to register slide layouts"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
	public String addSlideLayoutFromGalFile (
	        @ApiParam(required=true, value="uploaded gal file with the slide layout") 
	        @RequestParam("file") String uploadedFileName, 
	        @ApiParam(required=true, value="name of the slide layout to be created") 
	        @RequestParam("name")
	        String slideLayoutName, 
	        @ApiParam(required=false, value="width of the slide layout") 
            @RequestParam(required=false, value="width")
	        Integer width,
	        @ApiParam(required=false, value="height of the slide layout") 
            @RequestParam(required=false, value="height")
	        Integer height,
	        Principal p) {
	    if (uploadedFileName != null) {
	        uploadedFileName = moveToTempFile (uploadedFileName);
            File galFile = new File(uploadDir, uploadedFileName);
            if (galFile.exists()) {
                // check if the name is available
                try {
                    UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
                    SlideLayout existing = layoutRepository.getSlideLayoutByName(slideLayoutName, user);
                    if (existing != null) {
                        ErrorMessage errorMessage = new ErrorMessage();
                        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                        errorMessage.addError(new ObjectError("name", "Duplicate"));
                        throw new IllegalArgumentException("There is already a slide layout with that name", errorMessage);
                    }
                    // need to retrieve full list of linkers first
                    LinkerListResultView result = listLinkers(0, null, null, null, null, p);
                    ExtendedGalFileParser parser = new ExtendedGalFileParser();
                    GalFileImportResult importResult = parser.parse(galFile.getAbsolutePath(), slideLayoutName, result.getRows());
                    
                    ErrorMessage errorMessage = new ErrorMessage();
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    if (!importResult.getErrors().isEmpty()) {
                        for (ErrorMessage error: importResult.getErrors()) {
                            for (ObjectError err: error.getErrors()) {
                                errorMessage.addError(err);
                            }
                        }
                    }
                    
                    if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                        throw new IllegalArgumentException("Errors processing the gal file", errorMessage);
                    }
                    
                    
                    SlideLayout layout = importResult.getLayout();
                    if (width != null && height != null) {
                        if (layout.getHeight() == 1 && layout.getWidth() == 1) { // only one block in the GAL file
                            if (layout.getBlocks() != null && layout.getBlocks().size() > 0) {
                                BlockLayout blockLayout = layout.getBlocks().get(0).getBlockLayout();
                                if (blockLayout == null) {
                                    // error
                                    errorMessage = new ErrorMessage();
                                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                                    errorMessage.addError(new ObjectError("blockLayout", "NotFound"));
                                    throw new IllegalArgumentException("Block layout cannot be extracted from the GAL file", errorMessage);
                                }
                                List<org.glygen.array.persistence.rdf.Block> blocks = new ArrayList<>();
                                // repeat the same block layout for all blocks
                                for (int i=0; i < width; i++) {
                                    for (int j=0; j < height; j++) {
                                        org.glygen.array.persistence.rdf.Block block = new org.glygen.array.persistence.rdf.Block();
                                        block.setRow(j);
                                        block.setColumn(i);
                                        block.setBlockLayout(blockLayout);
                                        blocks.add(block);
                                    }
                                }
                                layout.setBlocks(blocks);
                            } else {
                                // error
                                errorMessage = new ErrorMessage();
                                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                                errorMessage.addError(new ObjectError("blockLayout", "NotFound"));
                                throw new IllegalArgumentException("Block layout cannot be extracted from the GAL file", errorMessage);
                            }
                        } else if (layout.getWidth() != width || layout.getHeight() != height){
                            // error
                            errorMessage = new ErrorMessage();
                            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                            errorMessage.addError(new ObjectError("width", "NotValid"));
                            errorMessage.addError(new ObjectError("height", "NotValid"));
                            throw new IllegalArgumentException("Width and height specification does not match with GAL file", errorMessage);
                        }
                    }
                    
                    // add all new glycans and features and block layouts first
                    try {
                        for (Glycan g: importResult.getGlycanList()) {
                            addGlycan(g, p, true);
                        }
                        for (org.glygen.array.persistence.rdf.Feature f: importResult.getFeatureList()) {
                            addFeature(f, p);
                        }
                        for (BlockLayout b: importResult.getLayoutList()) {
                            addBlockLayout(b, p);
                        }
                    } catch (IllegalArgumentException e) {
                        // need to ignore duplicate errors
                        if (e.getCause() instanceof ErrorMessage && e.getCause().getMessage().contains("Duplicate") && e.getCause().getMessage().contains("name")) {
                            // do nothing
                        } else if (e.getCause() instanceof ErrorMessage){
                            for (ObjectError err: ((ErrorMessage) e.getCause()).getErrors()) {
                                errorMessage.addError(err);
                            }
                        } else {
                            throw e;
                        }
                    }
                    if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) {
                        throw new IllegalArgumentException("Errors processing the gal file", errorMessage);
                    }
                    
                    String slideURI = addSlideLayout(importResult.getLayout(), p);
                    String id = slideURI.substring(slideURI.lastIndexOf("/")+1);
                    return id;
                } catch (IllegalArgumentException e) {
                    throw e;
                } catch (IOException | SparqlException | SQLException e) {
                    throw new GlycanRepositoryException("SlideLayout could not be added", e);
                }
            } else {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("file", "NotValid"));
                throw new IllegalArgumentException("File cannot be found", errorMessage);
            }
	    } else {
	        ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("file", "NotValid"));
            throw new IllegalArgumentException("File cannot be found", errorMessage);
	    }
	}
	
	private String moveToTempFile(String uploadedFileName) {
        if (uploadedFileName.startsWith("tmp"))
            return uploadedFileName;
        File oldFile = new File (uploadDir, uploadedFileName);
        File newFile = new File (uploadDir, "tmp" + uploadedFileName);
        if (newFile.exists()) {
            // already moved to tmp
            return "tmp" + uploadedFileName;
        }
        boolean b = oldFile.renameTo(newFile);
        if (b) return "tmp" + uploadedFileName;
        
        return uploadedFileName;
    }

    @ApiOperation(value = "Import selected slide layouts from uploaded GRITS array library file")
	@RequestMapping(value = "/addSlideLayoutFromLibrary", method=RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Slide layouts imported into repository successfully"), 
			@ApiResponse(code=400, message="Invalid request, file cannot be found"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register slide layouts"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public ImportGRITSLibraryResult addSlideLayoutFromLibrary (
			@ApiParam(required=true, value="uploaded file with slide layouts")
			@RequestParam("file") String uploadedFileName,
			@ApiParam(required=true, value="list of slide layouts to be imported, only name is sufficient for a slide layout")
			@RequestBody List<SlideLayout> slideLayouts, 
			Principal p) {
		
		if (uploadedFileName != null) {
		    uploadedFileName = moveToTempFile (uploadedFileName);
			File libraryFile = new File(uploadDir, uploadedFileName);
			if (libraryFile.exists()) {
				if (slideLayouts == null || slideLayouts.isEmpty()) {
					ErrorMessage errorMessage = new ErrorMessage("No slide layouts provided");
					errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
					errorMessage.addError(new ObjectError("slideLayouts", "NoEmpty"));
					throw new IllegalArgumentException("No slide layouts provided", errorMessage);
				}
				ImportGRITSLibraryResult result = new ImportGRITSLibraryResult();
				
				List<BlockLayout> addedLayouts = new ArrayList<BlockLayout>();
				
				for (SlideLayout slideLayout: slideLayouts) {
				    ErrorMessage errorMessage = new ErrorMessage();
	                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
					// check if already exists before trying to import
				    String searchName = null;
					if (slideLayout.getName() != null) {
					    searchName = slideLayout.getName();
						try {
							UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
							SlideLayout existing = layoutRepository.getSlideLayoutByName(searchName, user);
							if (existing != null) {
								result.getDuplicates().add(createSlideLayoutView(slideLayout));
								continue;
							}
						} catch (Exception e) {
						    SlideLayoutError errorObject = new SlideLayoutError();
						    errorObject.setLayout(createSlideLayoutView(slideLayout));
							result.getErrors().add(errorObject);
							errorMessage.addError(new ObjectError("internalError", e.getMessage()));
							errorObject.setError(errorMessage);
							continue;
						}
					}
					try {
					    slideLayout = getFullLayoutFromLibrary (libraryFile, slideLayout);
					} catch (Exception e) {
					    logger.error("Error getting slide layout from the library file", e);
					    if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
					        for (ObjectError err: ((ErrorMessage) e.getCause()).getErrors()) {
					            errorMessage.addError(err);
					        }
					    } else {
					        errorMessage.addError(new ObjectError("internalError", e.getMessage()));
					    }
					    slideLayout = new SlideLayout();
					    slideLayout.setName(searchName);
					    SlideLayoutError errorObject = new SlideLayoutError();
					    errorObject.setError(errorMessage);
					    errorObject.setLayout(slideLayout);
					    result.getErrors().add(errorObject);
					    return result;
					}
					if (searchName != null) {
					    slideLayout.setName(searchName);
					}
					if (slideLayout != null) {
						// find all block layouts, glycans, linkers and add them first
						for (org.glygen.array.persistence.rdf.Block block: slideLayout.getBlocks()) {
							if (block.getBlockLayout() != null) { 
								try {
									UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
									BlockLayout existing = layoutRepository.getBlockLayoutByName(block.getBlockLayout().getName(), user);
									if (existing != null) { // already added no need to go through glycans/linkers
										continue;
									}
									if (!addedLayouts.contains(block.getBlockLayout())) {
										addedLayouts.add(block.getBlockLayout());
										for (org.glygen.array.persistence.rdf.Spot spot: block.getBlockLayout().getSpots()) {
											for (org.glygen.array.persistence.rdf.Feature feature: spot.getFeatures()) {
												if (feature.getGlycans() != null) {
												    for (Glycan g: feature.getGlycans()) {
    													if (!glycanCache.contains(g)) {
    														glycanCache.add(g);
    														try {	
    															addGlycan(g, p, true);   //don't want to register automatically!!!
    														} catch (Exception e) {
    															if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
    																ErrorMessage error = (ErrorMessage) e.getCause();
    																for (ObjectError err: error.getErrors()) {
    																	if (err.getObjectName().equalsIgnoreCase("sequence") && 
    																			err.getDefaultMessage().equalsIgnoreCase("duplicate")) {
    																		if (g.getName() != null) {
    																			// add name as an alias
    																		    if (g instanceof SequenceDefinedGlycan) {
        																			String existingId = getGlycanBySequence(
        																			     ((SequenceDefinedGlycan) g).getSequence(), p);
        																			addAliasForGlycan(existingId, g.getName(), p);
    																		    }
    																		}
    																		break;
    																	} else {
    																	    errorMessage.addError(err);
    																	}
    																}
    															} else {
    																logger.info("Could not add glycan: ", e);
    															}
    														}
    													}
												    }
												}
												if (feature.getLinker() != null) {
													if (!linkerCache.contains(feature.getLinker())) {
														linkerCache.add(feature.getLinker());
														try {
															addLinker(feature.getLinker(), p);
														} catch (Exception e) {
															if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
																ErrorMessage error = (ErrorMessage) e.getCause();
																boolean needAlias = false;
																for (ObjectError err: error.getErrors()) {
																	if (err.getDefaultMessage().contains("Duplicate")) {
																		needAlias = true;
																		if (err.getObjectName().contains("pubChemId")) {
																			needAlias = false;
																			break;
																		}
																	} else {
																	    errorMessage.addError(err);
																	}
																}
																if (needAlias) {		
																	Linker linker = feature.getLinker();
																	linker.setName(linker.getName()+"B");
																	try {
																		addLinker (linker, p);
																	} catch (IllegalArgumentException e1) {
																		// ignore, probably already added
																		logger.debug ("duplicate linker cannot be added", e1);
																	}
																}
															}
															else {
																logger.info("Could not add linker: ", e);
																errorMessage.addError(new ObjectError("internalError", e.getMessage()));
															}
														}
													}
												}
											}
										}
										addBlockLayout(block.getBlockLayout(), p);
									}
								} catch (Exception e) {
									logger.info("Could not add block layout", e);
									if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                                        ErrorMessage error = (ErrorMessage) e.getCause();
                                        for (ObjectError err: error.getErrors()) {
                                            errorMessage.addError(err);
                                        }
									} else {
									    errorMessage.addError(new ObjectError("internalError", e.getMessage()));
									}
								}
							}
						}
						
						try {
							String id = addSlideLayout(slideLayout, p);
							slideLayout.setId(id);
							result.getAddedLayouts().add(createSlideLayoutView(slideLayout));
						} catch (Exception e) {
							if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
								ErrorMessage error = (ErrorMessage) e.getCause();
								for (ObjectError err: error.getErrors()) {
									if (err.getDefaultMessage().contains("Duplicate")) {
										result.getDuplicates().add(createSlideLayoutView(slideLayout));
									} else {
									    errorMessage.addError(err);
									}
								}
							} else {
								logger.debug("Could not add slide layout", e);
								SlideLayoutError errorObject = new SlideLayoutError();
		                        errorObject.setError(errorMessage);
		                        errorObject.setLayout(slideLayout);
		                        result.getErrors().add(errorObject);
								errorMessage.addError(new ObjectError("internalError", e.getMessage()));
							}
						}
					}
				}
				return result;
			} else {
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
				errorMessage.addError(new ObjectError("file", "NotValid"));
				throw new IllegalArgumentException("File cannot be found", errorMessage);
			}
		} else {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
			errorMessage.addError(new ObjectError("file", "NotValid"));
			throw new IllegalArgumentException("File cannot be found", errorMessage);
		}	
	}
	
	SlideLayout createSlideLayoutView (SlideLayout layout) {
	    SlideLayout view = new SlideLayout();
	    view.setId(layout.getId());
	    view.setUri(layout.getUri());
	    view.setIsPublic(layout.getIsPublic());
	    view.setUser(layout.getUser());
	    view.setName(layout.getName());
	    view.setDescription(layout.getDescription());
	    view.setWidth(layout.getWidth());
	    view.setHeight(layout.getHeight());
	    view.setDateCreated(layout.getDateCreated());
	    view.setDateAddedToLibrary(layout.getDateAddedToLibrary());
	    view.setDateModified(layout.getDateModified());
	    
	    return view;
	}
	
	@ApiOperation(value = "Retrieve glycan with the given id")
	@RequestMapping(value="/getglycan/{glycanId}", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Glycan retrieved successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list glycans"),
			@ApiResponse(code=404, message="Gycan with given id does not exist"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Glycan getGlycan (
			@ApiParam(required=true, value="id of the glycan to retrieve") 
			@PathVariable("glycanId") String glycanId, Principal p) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
			Glycan glycan = glycanRepository.getGlycanById(glycanId, user);
			if (glycan == null) {
			    glycan = glycanRepository.getGlycanById(glycanId, null);
			    if (glycan == null) {
			        throw new EntityNotFoundException("Glycan with id : " + glycanId + " does not exist in the repository");
			    }
			}
			if (glycan instanceof SequenceDefinedGlycan) {
			    byte[] image = getCartoonForGlycan(glycanId);
                if (image == null && ((SequenceDefinedGlycan) glycan).getSequence() != null) {
                    // try to create one
                    BufferedImage t_image = createImageForGlycan ((SequenceDefinedGlycan) glycan);
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
                    image = getCartoonForGlycan(glycanId);
                }
                glycan.setCartoon(image);
			    
			}
			return glycan;
			
			
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Glycan cannot be retrieved for user " + p.getName(), e);
		}
		
	}
	
	@ApiOperation(value = "Retrieve id for a glycan given the sequence")
	@RequestMapping(value="/getGlycanBySequence", method = RequestMethod.GET)
	@ApiResponses (value ={@ApiResponse(code=200, message="Glycan id retrieved successfully", response = String.class), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list glycans"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public String getGlycanBySequence (
			@ApiParam(required=true, value="Sequence of the glycan to retrieve (in GlycoCT)") 
			@RequestParam String sequence, Principal principal) {
		if (sequence == null || sequence.isEmpty())
			return null;
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			String seq = URLDecoder.decode(sequence, StandardCharsets.UTF_8.name());
			String glycanURI = glycanRepository.getGlycanBySequence(seq.trim(), user);
			if (glycanURI != null)
				return glycanURI.substring(glycanURI.lastIndexOf("/") + 1);
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("error getting glycan: ", e);
		} catch (UnsupportedEncodingException e) {
			logger.info(e.getMessage());  // ignore, should not happen
		}
		return null;
	}
	
	@ApiOperation(value = "Retrieve sequence (in GlycoCT) from GlyToucan for the glycan with the given glytoucan id (accession number)")
	@RequestMapping(value="/getGlycanFromGlytoucan", method = RequestMethod.GET)
	@ApiResponses (value ={@ApiResponse(code=200, message="Glycan retrieved successfully", response = String.class), 
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public String getGlycanFromGlytoucan (
			@ApiParam(required=true, value="Accession number of the glycan to retrieve (from GlyToucan)") 
			@RequestParam String glytoucanId) {
		if (glytoucanId == null || glytoucanId.isEmpty())
			return null;
		return getSequenceFromGlytoucan(glytoucanId);
	}
	
	private String getSequenceFromGlytoucan (String glytoucanId) {
	    try {
            String wurcsSequence = GlytoucanUtil.getInstance().retrieveGlycan(glytoucanId.trim());
            if (wurcsSequence == null) {
                // cannot be found in Glytoucan
                throw new EntityNotFoundException("Glycan with accession number " + glytoucanId + " cannot be retrieved");
            } else {
                // convert sequence into GlycoCT and return
                WURCSToGlycoCT exporter = new WURCSToGlycoCT();
                exporter.start(wurcsSequence);
                if ( !exporter.getErrorMessages().isEmpty() ) {
                    //throw new GlycanRepositoryException(exporter.getErrorMessages());
                    logger.info("Cannot be exported in GlycoCT: " + wurcsSequence + " Reason: " + exporter.getErrorMessages());
                    return wurcsSequence;
                }
                return exporter.getGlycoCT();
            }           
        } catch (Exception e) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("glytoucanId", "NotValid"));
            throw new IllegalArgumentException("Invalid Input: Glytoucan is not valid" , errorMessage);
        }
	}
	
	@ApiOperation(value = "Retrieve image for given glycan")
	@RequestMapping(value="/getimage/{glycanId}", method = RequestMethod.GET, 
		produces = MediaType.IMAGE_PNG_VALUE )
	@ApiResponses (value ={@ApiResponse(code=200, message="Image retrieved successfully"), 
			@ApiResponse(code=404, message="Image for the given glycan is not available"),
			@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public @ResponseBody byte[] getImageForGlycan (
			@ApiParam(required=true, value="Id of the glycan to retrieve the image for") 
			@PathVariable("glycanId") String glycanId) {
		try {
			File imageFile = new File(imageLocation + File.separator + glycanId + ".png");
			InputStreamResource resource = new InputStreamResource(new FileInputStream(imageFile));
			return IOUtils.toByteArray(resource.getInputStream());
		} catch (IOException e) {
			logger.error("Image cannot be retrieved", e);
			throw new EntityNotFoundException("Image for glycan " + glycanId + " is not available");
		}
	}
	
	@ApiOperation(value = "Retrieve linker with the given id")
	@RequestMapping(value="/getlinker/{linkerId}", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Linker retrieved successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list glycans"),
			@ApiResponse(code=404, message="Linker with given id does not exist"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Linker getLinker (
			@ApiParam(required=true, value="id of the linker to retrieve") 
			@PathVariable("linkerId") String linkerId, Principal p) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
			Linker linker = linkerRepository.getLinkerById(linkerId, user);
			if (linker == null) {
			    linker = linkerRepository.getLinkerById(linkerId, null);
			    if (linker == null) {
			        throw new EntityNotFoundException("Linker with id : " + linkerId + " does not exist in the repository");
			    }
			}
			
			return linker;
			
			
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Linker cannot be retrieved for user " + p.getName(), e);
		}
		
	}
	
	@ApiOperation(value = "Retrieve slide layout with the given id")
	@RequestMapping(value="/getslidelayout/{layoutId}", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Slide Layout retrieved successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list slide layouts"),
			@ApiResponse(code=404, message="Slide layout with given id does not exist"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public SlideLayout getSlideLayout (
			@ApiParam(required=true, value="id of the slide layout to retrieve") 
			@PathVariable("layoutId") String layoutId, 
			@ApiParam (required=false, defaultValue = "true", value="if false, do not load slide details. Default is true (to load all)")
			@RequestParam(required=false, defaultValue = "true", value="loadAll") Boolean loadAll, 
			Principal p) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
			SlideLayout layout = layoutRepository.getSlideLayoutById(layoutId, user, loadAll);
			if (layout == null) {
			    // check the public repository
			    layout = layoutRepository.getSlideLayoutById(layoutId, null, loadAll);
			    if (layout == null) {
			        throw new EntityNotFoundException("Slide layout with id : " + layoutId + " does not exist in the repository");
			    }
			}
			
			if (loadAll && layout.getBlocks() != null) {
                for (org.glygen.array.persistence.rdf.Block block: layout.getBlocks()) {
                    if (block.getBlockLayout() != null) {
                        if (block.getBlockLayout().getSpots() == null)
                            continue;
                        for (org.glygen.array.persistence.rdf.Spot s: block.getBlockLayout().getSpots()) {
                            if (s.getFeatures() == null)
                                continue;
                            for (org.glygen.array.persistence.rdf.Feature f: s.getFeatures()) {
                                if (f.getGlycans()  != null) {
                                    for (Glycan g: f.getGlycans()) {
                                        if (g instanceof SequenceDefinedGlycan && g.getCartoon() == null) {
                                            byte[] image = getCartoonForGlycan(g.getId());
                                            if (image == null && ((SequenceDefinedGlycan) g).getSequence() != null) {
                                                // try to create one
                                                BufferedImage t_image = createImageForGlycan ((SequenceDefinedGlycan) g);
                                                if (t_image != null) {
                                                    String filename = g.getId() + ".png";
                                                    //save the image into a file
                                                    logger.debug("Adding image to " + imageLocation);
                                                    File imageFile = new File(imageLocation + File.separator + filename);
                                                    try {
                                                        ImageIO.write(t_image, "png", imageFile);
                                                    } catch (IOException e) {
                                                        logger.error ("Glycan image cannot be written", e);
                                                    }
                                                }
                                                image = getCartoonForGlycan(g.getId());
                                            }
                                            g.setCartoon(image);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
			return layout;
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Slide Layout cannot be retrieved for user " + p.getName(), e);
		}
	}
	
	@ApiOperation(value = "Retrieve feature with the given id")
    @RequestMapping(value="/getfeature/{featureId}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Feature retrieved successfully"), 
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to retrieve the feature"),
            @ApiResponse(code=404, message="Feature with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public org.glygen.array.persistence.rdf.Feature getFeature (
            @ApiParam(required=true, value="id of the feature to retrieve") 
            @PathVariable("featureId") String featureId, 
            Principal p) {
        try {
            UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
            org.glygen.array.persistence.rdf.Feature feature = featureRepository.getFeatureById(featureId, user);
            if (feature == null) {
                feature = featureRepository.getFeatureById(featureId, null);
                if (feature == null) {
                    throw new EntityNotFoundException("Feature with id : " + featureId + " does not exist in the repository");
                }
            }
            
            if (feature.getGlycans()  != null) {
                for (Glycan g: feature.getGlycans()) {
                    if (g instanceof SequenceDefinedGlycan && g.getCartoon() == null) {
                        byte[] image = getCartoonForGlycan(g.getId());
                        if (image == null && ((SequenceDefinedGlycan) g).getSequence() != null) {
                            // try to create one
                            BufferedImage t_image = createImageForGlycan ((SequenceDefinedGlycan) g);
                            if (t_image != null) {
                                String filename = g.getId() + ".png";
                                //save the image into a file
                                logger.debug("Adding image to " + imageLocation);
                                File imageFile = new File(imageLocation + File.separator + filename);
                                try {
                                    ImageIO.write(t_image, "png", imageFile);
                                } catch (IOException e) {
                                    logger.error ("Glycan image cannot be written", e);
                                }
                            }
                            image = getCartoonForGlycan(g.getId());
                        }
                        g.setCartoon(image);
                    }
                }
            }
            return feature;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Feature cannot be retrieved for user " + p.getName(), e);
        }
    }
	
	/**
	 * the library file should already be uploaded to "uploadDir" before calling this service.
	 * 
	 * @param uploadedFileName the name of the library file already uploaded
	 * @return list of slide layouts in the library
	 */
	@SuppressWarnings("rawtypes")
    @ApiOperation(value = "Retrieve slide layouts from uploaded GRITS array library file")
	@RequestMapping(value = "/getSlideLayoutFromLibrary", method=RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Slide layout retrieved from file successfully"), 
			@ApiResponse(code=400, message="Invalid request, file is not valid or cannot be found"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to retrieve slide layouts"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public List<SlideLayout> getSlideLayoutsFromLibrary(
			@ApiParam(required=true, value="uploaded file with slide layouts")
			@RequestParam("file") String uploadedFileName) {
		List<SlideLayout> layouts = new ArrayList<SlideLayout>();
		if (uploadedFileName != null) {
		    uploadedFileName = moveToTempFile (uploadedFileName);
			File libraryFile = new File(uploadDir, uploadedFileName);
			if (libraryFile.exists()) {
				try {
					FileInputStream inputStream2 = new FileInputStream(libraryFile);
			        InputStreamReader reader2 = new InputStreamReader(inputStream2, "UTF-8");
			        List<Class> contextList = new ArrayList<Class>(Arrays.asList(FilterUtils.filterClassContext));
		    		contextList.add(ArrayDesignLibrary.class);
			        JAXBContext context2 = JAXBContext.newInstance(contextList.toArray(new Class[contextList.size()]));
			        Unmarshaller unmarshaller2 = context2.createUnmarshaller();
			        ArrayDesignLibrary library = (ArrayDesignLibrary) unmarshaller2.unmarshal(reader2);
			        List<org.grits.toolbox.glycanarray.library.om.layout.SlideLayout> layoutList = 
			        		library.getLayoutLibrary().getSlideLayout();
			        for (org.grits.toolbox.glycanarray.library.om.layout.SlideLayout slideLayout : layoutList) {
		        		SlideLayout mySlideLayout = new SlideLayout();
		        		mySlideLayout.setName(slideLayout.getName());
		        		mySlideLayout.setDescription(slideLayout.getDescription());
		        		mySlideLayout.setId(slideLayout.getId().toString());
		        		int width = 0;
		        		int height = 0;
		        		for (Block block: slideLayout.getBlock()) {
		        			org.glygen.array.persistence.rdf.Block myBlock = new org.glygen.array.persistence.rdf.Block();
		        			myBlock.setColumn(block.getColumn());
		        			myBlock.setRow(block.getRow());
		        			if (block.getColumn() > width)
		        				width = block.getColumn();
		        			if (block.getRow() > height)
		        				height = block.getRow();
		        		}
		        		
		        		mySlideLayout.setHeight(slideLayout.getHeight() == null ? height: slideLayout.getHeight());
		        		mySlideLayout.setWidth(slideLayout.getWidth() == null ? width: slideLayout.getWidth());
		        		layouts.add(mySlideLayout);
					}
			        return layouts;
				} catch (JAXBException e) {
					ErrorMessage errorMessage = new ErrorMessage();
					errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
					errorMessage.addError(new ObjectError("file", "NotValid"));
					throw new IllegalArgumentException("Not a valid array library!", errorMessage);
				} catch (IOException e) {
					ErrorMessage errorMessage = new ErrorMessage();
					errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
					errorMessage.addError(new ObjectError("file", "NotValid"));
					throw new IllegalArgumentException("File cannot be found!", errorMessage);
				}
			}
		}
		
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		errorMessage.addError(new ObjectError("file", "NotValid"));
		throw new IllegalArgumentException("File is not valid", errorMessage);
	}
	
	
	List<org.glygen.array.persistence.rdf.Spot> getSpotsFromBlockLayout (ArrayDesignLibrary library, org.grits.toolbox.glycanarray.library.om.layout.BlockLayout blockLayout) {
		List<org.glygen.array.persistence.rdf.Spot> spots = new ArrayList<>();
		ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
    	for (Spot spot: blockLayout.getSpot()) {
    		org.glygen.array.persistence.rdf.Spot s = new org.glygen.array.persistence.rdf.Spot();
    		s.setRow(spot.getY());
    		s.setColumn(spot.getX());
    		s.setGroup(spot.getGroup());
    		s.setConcentration(spot.getConcentration());
    		Feature feature = LibraryInterface.getFeature(library, spot.getFeatureId());
    		List<org.glygen.array.persistence.rdf.Feature> features = new ArrayList<>();
    		Map<org.glygen.array.persistence.rdf.Feature, Double> ratioMap = new HashMap<>();
    		if (feature != null) {
        		for (Ratio r : feature.getRatio()) {
        			GlycanProbe probe = null;
        			for (GlycanProbe p : library.getFeatureLibrary().getGlycanProbe()) {
        				if (p.getId().equals(r.getItemId())) {
        					probe = p;
        					break;
        				}
        			}
        			if (probe != null) {
        				for (Ratio r1 : probe.getRatio()) {
        					org.glygen.array.persistence.rdf.Feature myFeature = new org.glygen.array.persistence.rdf.Feature();
        					myFeature.setGlycans(new ArrayList<Glycan>());
        					org.grits.toolbox.glycanarray.library.om.feature.Glycan glycan = LibraryInterface.getGlycan(library, r1.getItemId());
        					if (glycan != null) {
		        				SequenceDefinedGlycan myGlycan = new SequenceDefinedGlycan();
		        				myGlycan.setSequence(glycan.getSequence());  
		        				myGlycan.setName(glycan.getName());
		        				myGlycan.setDescription(glycan.getComment());
		        				myGlycan.setGlytoucanId(glycan.getGlyTouCanId());
		        				myGlycan.setSequenceType(GlycanSequenceFormat.GLYCOCT);
		        				myGlycan.setInternalId(glycan.getId() == null ? "" : glycan.getId().toString());
		        				myFeature.getGlycans().add(myGlycan);
        					} else {
        					    // should have been there
                                errorMessage.addError(new ObjectError("glycan:" + r1.getItemId(), "NotFound"));
        					}
		        			org.grits.toolbox.glycanarray.library.om.feature.Linker linker = LibraryInterface.getLinker(library, probe.getLinker());
		        			if (linker != null) {
		        				Linker myLinker = new SmallMoleculeLinker();
		        				if (linker.getPubChemId() != null) ((SmallMoleculeLinker) myLinker).setPubChemId(linker.getPubChemId().longValue());
		        				myLinker.setName(linker.getName());
		        				myLinker.setComment(linker.getComment());
		        				myFeature.setLinker(myLinker);
		        			}
		        			else {
		        			    // should have been there
                                errorMessage.addError(new ObjectError("linker:" + probe.getLinker(), "NotFound"));
                            }
		        			ratioMap.put(myFeature, r1.getItemRatio());
		        			features.add(myFeature);
        				}
        			} else {
        			    // should have been there
        			    errorMessage.addError(new ObjectError("probe:" + r.getItemId(), "NotFound"));
        			}
        		}
    		}
    		s.setFeatureRatioMap(ratioMap);
    		s.setFeatures(features);
    		spots.add(s);
    	}
    	
    	if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty())
    	    throw new IllegalArgumentException("Not a valid array library!", errorMessage);
    	return spots;
	}
	
	@ApiOperation(value = "List all block layouts for the user")
	@RequestMapping(value="/listBlocklayouts", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Block layouts retrieved successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error for arguments", response = ErrorMessage.class),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list block layouts"),
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
            @RequestParam(value="filter", required=false) String searchValue,
			Principal p) {
		BlockLayoutResultView result = new BlockLayoutResultView();
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
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
			
			int total = layoutRepository.getBlockLayoutCountByUser (user);
			List<BlockLayout> layouts = layoutRepository.getBlockLayoutByUser(user, offset, limit, field, loadAll, order, searchValue);
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
    			            if (f.getGlycans()  != null) {
    		                    for (Glycan g: f.getGlycans()) {
    		                        if (g instanceof SequenceDefinedGlycan && g.getCartoon() == null) {
    		                            byte[] image = getCartoonForGlycan(g.getId());
                                        if (image == null && ((SequenceDefinedGlycan) g).getSequence() != null) {
                                            // try to create one
                                            BufferedImage t_image = createImageForGlycan ((SequenceDefinedGlycan) g);
                                            if (t_image != null) {
                                                String filename = g.getId() + ".png";
                                                //save the image into a file
                                                logger.debug("Adding image to " + imageLocation);
                                                File imageFile = new File(imageLocation + File.separator + filename);
                                                try {
                                                    ImageIO.write(t_image, "png", imageFile);
                                                } catch (IOException e) {
                                                    logger.error ("Glycan image cannot be written", e);
                                                }
                                            }
                                            image = getCartoonForGlycan(g.getId());
                                        }
                                        
                                        g.setCartoon(image);
    		                        }
    		                    }
    		                }
    			        }
    			    }
    			}
			}
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
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list features"),
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
            @RequestParam(value="filter", required=false) String searchValue,
			Principal p) {
		FeatureListResultView result = new FeatureListResultView();
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
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
			
			int total = featureRepository.getFeatureCountByUser (user);
			
			List<org.glygen.array.persistence.rdf.Feature> features = featureRepository.getFeatureByUser(user, offset, limit, field, order, searchValue);
			result.setRows(features);
			result.setTotal(total);
			result.setFilteredTotal(features.size());
			
			// get cartoons for the glycans
			for (org.glygen.array.persistence.rdf.Feature f: features) {
			    if (f.getGlycans()  != null) {
			        for (Glycan g: f.getGlycans()) {
			            if (g instanceof SequenceDefinedGlycan && g.getCartoon() == null) {
			                byte[] image = getCartoonForGlycan(g.getId());
                            if (image == null && ((SequenceDefinedGlycan) g).getSequence() != null) {
                                // try to create one
                                BufferedImage t_image = createImageForGlycan ((SequenceDefinedGlycan) g);
                                if (t_image != null) {
                                    String filename = g.getId() + ".png";
                                    //save the image into a file
                                    logger.debug("Adding image to " + imageLocation);
                                    File imageFile = new File(imageLocation + File.separator + filename);
                                    try {
                                        ImageIO.write(t_image, "png", imageFile);
                                    } catch (IOException e) {
                                        logger.error ("Glycan image cannot be written", e);
                                    }
                                }
                                image = getCartoonForGlycan(g.getId());
                            }
                            
                            g.setCartoon(image);
			            }
			        }
			    }
			}
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot retrieve features for user. Reason: " + e.getMessage());
		}
		
		return result;
	}

	@ApiOperation(value = "List all glycans for the user")
	@RequestMapping(value="/listGlycans", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Glycans retrieved successfully", response = GlycanListResultView.class), 
			@ApiResponse(code=400, message="Invalid request, validation error for arguments"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list glycans"),
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
            @RequestParam(value="filter", required=false) String searchValue, Principal p) {
		GlycanListResultView result = new GlycanListResultView();
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
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
			
			int total = glycanRepository.getGlycanCountByUser (user);
			
			List<Glycan> glycans = glycanRepository.getGlycanByUser(user, offset, limit, field, order, searchValue);
			for (Glycan glycan : glycans) {
			    if (glycan.getType().equals(GlycanType.SEQUENCE_DEFINED)) {
			        byte[] image = getCartoonForGlycan(glycan.getId());
			        if (image == null && ((SequenceDefinedGlycan) glycan).getSequence() != null) {
                        BufferedImage t_image = createImageForGlycan ((SequenceDefinedGlycan) glycan);
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
                        image = getCartoonForGlycan(glycan.getId());
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
	
	

    @ApiOperation(value = "List all glycans for the user and the public ones")
    @RequestMapping(value="/listAllGlycans", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Glycans retrieved successfully", response = GlycanListResultView.class), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to list glycans"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
    public GlycanListResultView listAllGlycans (
            @ApiParam(required=true, value="offset for pagination, start from 0") 
            @RequestParam("offset") Integer offset,
            @ApiParam(required=false, value="limit of the number of glycans to be retrieved") 
            @RequestParam(value="limit", required=false) Integer limit, 
            @ApiParam(required=false, value="name of the sort field, defaults to id") 
            @RequestParam(value="sortBy", required=false) String field, 
            @ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
            @RequestParam(value="order", required=false) Integer order, 
            @ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, Principal p) {
        GlycanListResultView result = new GlycanListResultView();
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
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
            
            int total = glycanRepository.getGlycanCountByUser (user);
            List<Glycan> glycans = glycanRepository.getGlycanByUser(user, offset, limit, field, order, searchValue);
            List<Glycan> totalResultList = new ArrayList<>();
            totalResultList.addAll(glycans);
            
            int totalPublic = glycanRepository.getGlycanCountByUser (null);
            
            List<Glycan> publicResultList = glycanRepository.getGlycanByUser(null, offset, limit, field, order, searchValue);
            for (Glycan g1: publicResultList) {
                boolean duplicate = false;
                for (Glycan g2: glycans) {
                    if (g1.getName().equals(g2.getName())) {
                        duplicate = true;
                    }
                }
                if (!duplicate) {
                    totalResultList.add(g1);
                } 
            }
            
            for (Glycan glycan : totalResultList) {
                if (glycan.getType().equals(GlycanType.SEQUENCE_DEFINED)) {
                    byte[] image = getCartoonForGlycan(glycan.getId());
                    if (image == null && ((SequenceDefinedGlycan) glycan).getSequence() != null) {
                        // try to create one
                        BufferedImage t_image = createImageForGlycan ((SequenceDefinedGlycan) glycan);
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
                        image = getCartoonForGlycan(glycan.getId());
                    }
                    glycan.setCartoon(image);
                    //glycan.setCartoon(getCartoonForGlycan(glycan.getId(), ((SequenceDefinedGlycan) glycan).getSequence()));
                }
            }
            
            result.setRows(totalResultList);
            result.setTotal(total+totalPublic);
            result.setFilteredTotal(totalResultList.size());
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Cannot retrieve glycans. Reason: " + e.getMessage());
        }
        
        return result;
    }
	
	@ApiOperation(value = "List all linkers for the user and the public ones")
	@RequestMapping(value="/listAllLinkers", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Linkers retrieved successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error for arguments"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list linkers"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error", response = ErrorMessage.class)})
	public LinkerListResultView listAllLinkers (
			@ApiParam(required=true, value="offset for pagination, start from 0") 
			@RequestParam("offset") Integer offset,
			@ApiParam(required=false, value="limit of the number of linkers to be retrieved") 
			@RequestParam(value="limit", required=false) Integer limit, 
			@ApiParam(required=false, value="name of the sort field, defaults to id") 
			@RequestParam(value="sortBy", required=false) String field, 
			@ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
			@RequestParam(value="order", required=false) Integer order, 
			@ApiParam(required=false, value="a filter value to match") 
            @RequestParam(value="filter", required=false) String searchValue, Principal p) {
		LinkerListResultView result = new LinkerListResultView();
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
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
			
			int total = linkerRepository.getLinkerCountByUser (user);
			List<Linker> linkers = linkerRepository.getLinkerByUser(user, offset, limit, field, order, searchValue);
            List<Linker> totalResultList = new ArrayList<>();
            totalResultList.addAll(linkers);
            
            int totalPublic = linkerRepository.getLinkerCountByUser (null);
            
            List<Linker> publicResultList = linkerRepository.getLinkerByUser(null, offset, limit, field, order, searchValue);
            for (Linker g1: publicResultList) {
                boolean duplicate = false;
                for (Linker g2: linkers) {
                    if (g1.getName().equals(g2.getName())) {
                        duplicate = true;
                    }
                }
                if (!duplicate) {
                    totalResultList.add(g1);
                } 
            }
			
			result.setRows(totalResultList);
			result.setTotal(total+totalPublic);
			result.setFilteredTotal(totalResultList.size());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot retrieve linkers for user. Reason: " + e.getMessage());
		}
		
		return result;
	}
	
	@ApiOperation(value = "List all linkers for the user")
    @RequestMapping(value="/listLinkers", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Linkers retrieved successfully"), 
            @ApiResponse(code=400, message="Invalid request, validation error for arguments"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to list linkers"),
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
            @RequestParam(value="filter", required=false) String searchValue, Principal p) {
        LinkerListResultView result = new LinkerListResultView();
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
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
            
            int total = linkerRepository.getLinkerCountByUser (user);
            
            List<Linker> linkers = linkerRepository.getLinkerByUser(user, offset, limit, field, order, searchValue);
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
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list slide layouts"),
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
            @RequestParam(value="filter", required=false) String searchValue,
			Principal p) {
		SlideLayoutResultView result = new SlideLayoutResultView();
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
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
			
			int total = layoutRepository.getSlideLayoutCountByUser (user);
			List<SlideLayout> layouts = layoutRepository.getSlideLayoutByUser(user, offset, limit, field, loadAll, order, searchValue);
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
                                        if (f.getGlycans()  != null) {
                                            for (Glycan g: f.getGlycans()) {
                                                if (g instanceof SequenceDefinedGlycan && g.getCartoon() == null) {
                                                    byte[] image = getCartoonForGlycan(g.getId());
                                                    if (image == null && ((SequenceDefinedGlycan) g).getSequence() != null) {
                                                        // try to create one
                                                        BufferedImage t_image = createImageForGlycan ((SequenceDefinedGlycan) g);
                                                        if (t_image != null) {
                                                            String filename = g.getId() + ".png";
                                                            //save the image into a file
                                                            logger.debug("Adding image to " + imageLocation);
                                                            File imageFile = new File(imageLocation + File.separator + filename);
                                                            try {
                                                                ImageIO.write(t_image, "png", imageFile);
                                                            } catch (IOException e) {
                                                                logger.error ("Glycan image cannot be written", e);
                                                            }
                                                        }
                                                        image = getCartoonForGlycan(g.getId());
                                                    }
                                                    
                                                    g.setCartoon(image);
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
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot retrieve slide layouts for user. Reason: " + e.getMessage());
		}
		
		return result;
	}
	
	@ApiOperation(value = "make given glycan public")
    @RequestMapping(value="/makeglycanpublic/{glycanId}", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="id of the public glycan"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to modify glycans"),
            @ApiResponse(code=409, message="A glycan with the given name already exists in public repository!"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String makeGlycanPublic (
            @ApiParam(required=true, value="id of the glycan to retrieve") 
            @PathVariable("glycanId") String glycanId, Principal p) {
	    UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        try {
            Glycan glycan = glycanRepository.getGlycanById(glycanId, user);
            if (glycan == null) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("glycanId", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("There is no glycan with the given id in user's repository", errorMessage);
            }
            
            BufferedImage t_image = null;
            if (glycan.getType() == GlycanType.SEQUENCE_DEFINED) {
                t_image = createImageForGlycan((SequenceDefinedGlycan) glycan);
            }
            String glycanURI = glycanRepository.makePublic (glycan, user);
            if (glycanURI == null) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("name", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Cannot have glycans with the same name", errorMessage);
            }
            String id = glycanURI.substring(glycanURI.lastIndexOf("/")+1);
            if (t_image != null) {
                String filename = id + ".png";
                //save the image into a file
                logger.debug("Adding image to " + imageLocation);
                File imageFile = new File(imageLocation + File.separator + filename);
                ImageIO.write(t_image, "png", imageFile);
            }
            return id;
        } catch (SparqlException e) {
            throw new GlycanRepositoryException("Glycan cannot be made public for user " + p.getName(), e);
        } catch (SQLException e) {
            throw new GlycanRepositoryException("Glycan cannot be made public for user " + p.getName(), e);
        } catch (IOException e) {
            logger.error("Glycan image cannot be generated", e);
            throw new GlycanRepositoryException("Glycan image cannot be generated", e);
        }
	}
	
	@ApiOperation(value = "make given linker public")
    @RequestMapping(value="/makelinkerpublic/{linkerId}", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="id of the public linker"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to modify linker"),
            @ApiResponse(code=409, message="A linker with the given name already exists in public repository!"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String makeLinkerPublic (
            @ApiParam(required=true, value="id of the linker to retrieve") 
            @PathVariable("linkerId") String linkerId, Principal p) {
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        try {
            Linker linker = linkerRepository.getLinkerById(linkerId, user);
            if (linker == null) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("linkerId", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("There is no linker with the given id in user's repository", errorMessage); 
            }
            String linkerURI = linkerRepository.makePublic (linker, user);
            if (linkerURI == null) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("name", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("Cannot have linkers with the same name", errorMessage);
            }
            return linkerURI.substring(linkerURI.lastIndexOf("/")+1);
        } catch (SparqlException e) {
            throw new GlycanRepositoryException("Linker cannot be made public for user " + p.getName(), e);
        } catch (SQLException e) {
            throw new GlycanRepositoryException("Linker cannot be made public for user " + p.getName(), e);
        }
    }
	
	@ApiOperation(value = "make given slide layout public")
    @RequestMapping(value="/makeslidelayoutpublic/{layoutId}", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="id of the public slide layout"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to modify slide layout"),
            @ApiResponse(code=409, message="A slide layout with the given name already exists in public repository!"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String makeSlideLayoutPublic (
            @ApiParam(required=true, value="id of the slide layout to retrieve") 
            @PathVariable("layoutId") String layoutId, Principal p) {
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        try {
            SlideLayout layout = layoutRepository.getSlideLayoutById(layoutId, user);
            if (layout == null) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("linkerId", "NotValid"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("There is no slide layout with the given id in user's repository", errorMessage); 
            }
            if (layout.getIsPublic()) {
                // already been made public
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("layoutId", "This slide layout is already public"));
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                throw new IllegalArgumentException("This slide layout is already public!", errorMessage); 
            }
            String layoutURI = layoutRepository.makePublic (layout, user, new HashMap<String, String>()); 
            return layoutURI.substring(layoutURI.lastIndexOf("/")+1);
        } catch (GlycanExistsException e) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("name", "NotValid"));
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            throw new IllegalArgumentException("Cannot have slide layouts with the same name", errorMessage);
        } catch (SparqlException e) {
            throw new GlycanRepositoryException("Slide Layout cannot be made public for user " + p.getName(), e);
        } catch (SQLException e) {
            throw new GlycanRepositoryException("Slide Layout cannot be made public for user " + p.getName(), e);
        }
    }
	
	@ApiOperation(value = "Check status for upload file")
	@RequestMapping(value = "/upload", method=RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	public Confirmation resumeUpload (
			@RequestParam("resumableFilename") String resumableFilename,
			@RequestParam ("resumableRelativePath") String resumableRelativePath,
            @RequestParam ("resumableTotalChunks") String resumableTotalChunks,
            @RequestParam("resumableChunkSize") int resumableChunkSize,
            @RequestParam("resumableCurrentChunkSize") int resumableCurrentChunkSize,
            @RequestParam("resumableChunkNumber") int resumableChunkNumber,
            @RequestParam("resumableTotalSize") long resumableTotalSize,
            @RequestParam("resumableType") String resumableType,
            @RequestParam("resumableIdentifier") String resumableIdentifier) {

		
        ResumableFileInfo info = ResumableInfoStorage.getInstance().get(resumableIdentifier);
        if (info == null || !info.valid()) {
        	if (info != null) ResumableInfoStorage.getInstance().remove(info);
        	if (resumableChunkNumber != 1) {
        	    throw new IllegalArgumentException("file identifier is not valid");
        	} else {
        	    throw new UploadNotFinishedException("Not found");  // this will return HttpStatus no_content 204
        	}
        }
        if (info.uploadedChunks.contains(new ResumableFileInfo.ResumableChunkNumber(resumableChunkNumber))) {
        	return new Confirmation ("Upload", HttpStatus.OK.value()); //This Chunk has been Uploaded.
        } else {
            throw new UploadNotFinishedException("Not found");  // this will return HttpStatus no_content 204
        }
    }
	
	
	@ApiOperation(value = "Update given block layout for the user")
	@RequestMapping(value = "/updateBlockLayout", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Block layout updated successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to update block layout"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation updateBlockLayout(
			@ApiParam(required=true, value="Block layout to be updated, id is required, name and comment can be updated only") 
			@RequestBody BlockLayout layout, Principal principal) throws SQLException {
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		// validate first
		if (validator != null) {
			if (layout.getName() == null || layout.getName().isEmpty()) 
				errorMessage.addError(new ObjectError("name", "NoEmpty"));
			if  (layout.getName() != null) {
				Set<ConstraintViolation<BlockLayout>> violations = validator.validateValue(BlockLayout.class, "name", layout.getName().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (layout.getDescription() != null) {
				Set<ConstraintViolation<BlockLayout>> violations = validator.validateValue(BlockLayout.class, "description", layout.getDescription().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("description", "LengthExceeded"));
				}		
			}
		
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			BlockLayout blockLayout= new BlockLayout();
			blockLayout.setUri(GlygenArrayRepository.uriPrefix + layout.getId());
			blockLayout.setDescription(layout.getDescription() != null ? layout.getDescription().trim() : layout.getDescription());
			blockLayout.setName(layout.getName() != null ? layout.getName().trim() : null);		
			
			BlockLayout local = null;
			// check if name is unique
			if (blockLayout.getName() != null && !blockLayout.getName().isEmpty()) {
				local = layoutRepository.getBlockLayoutByName(blockLayout.getName().trim(), user, false);
				if (local != null && !local.getUri().equals(blockLayout.getUri())) {   // there is another with the same name
					errorMessage.addError(new ObjectError("name", "Duplicate"));
				}
			} 
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid block layout information", errorMessage);
			
			layoutRepository.updateBlockLayout(blockLayout, user);
			return new Confirmation("Block Layout updated successfully", HttpStatus.OK.value());
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Error updating block layout with id: " + layout.getId());
		}
	}
	
	@ApiOperation(value = "Update given glycan for the user")
	@RequestMapping(value = "/updateGlycan", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Glycan updated successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to update glycans"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation updateGlycan(
			@ApiParam(required=true, value="Glycan with updated fields") 
			@RequestBody Glycan glycanView, 
			@RequestParam(value="changeSummary", required=false)
            String changeSummary,
            @ApiParam(required=false, value="field that has changed, can provide multiple") 
            @RequestParam(value="changedField", required=false)
            List<String> changedFields,
			Principal principal) throws SQLException {
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		// validate first
		if (validator != null) {
			if  (glycanView.getName() != null) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "name", glycanView.getName().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (glycanView.getDescription() != null) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "description", glycanView.getDescription().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("description", "LengthExceeded"));
				}		
			}
			if (glycanView.getInternalId() != null && !glycanView.getInternalId().isEmpty()) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "internalId", glycanView.getInternalId().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("internalId", "LengthExceeded"));
				}		
			}
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			Glycan glycan= new Glycan();
			if (glycanView.getUri() != null && !glycanView.getUri().isEmpty()) {
			    glycan.setUri(glycanView.getUri());
			} else {
			    glycan.setUri(GlygenArrayRepository.uriPrefix + glycanView.getId());
			}
			glycan.setInternalId(glycanView.getInternalId() != null ? glycanView.getInternalId().trim(): glycanView.getInternalId());
			glycan.setDescription(glycanView.getDescription() != null ? glycanView.getDescription().trim() : glycanView.getDescription());
			glycan.setName(glycanView.getName() != null ? glycanView.getName().trim() : null);		
			
			Glycan local = null;
			// check if internalid and label are unique
			if (glycan.getInternalId() != null && !glycan.getInternalId().trim().isEmpty()) {
				local = glycanRepository.getGlycanByInternalId(glycan.getInternalId().trim(), user);
				if (local != null && !local.getUri().equals(glycan.getUri())) {   // there is another with the same internal id
					errorMessage.addError(new ObjectError("internalId", "Duplicate"));
				}
			}
			if (glycan.getName() != null && !glycan.getName().trim().isEmpty()) {
				local = glycanRepository.getGlycanByLabel(glycan.getName().trim(), user);
				if (local != null && !local.getUri().equals(glycan.getUri())) {   // there is another with the same name
					errorMessage.addError(new ObjectError("name", "Duplicate"));
				}
			} 
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
			
			ChangeLog changeLog = new ChangeLog();
            changeLog.setUser(principal.getName());
            changeLog.setChangeType(ChangeType.MINOR);
            changeLog.setDate(new Date());
            changeLog.setSummary(changeSummary);
            changeLog.setChangedFields(changedFields);
            glycan.addChange(changeLog);
			glycanRepository.updateGlycan(glycan, user);
			return new Confirmation("Glycan updated successfully", HttpStatus.OK.value());
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Error updating glycan with id: " + glycanView.getId());
		}
	}
	
	@ApiOperation(value = "Update given linker for the user")
	@RequestMapping(value = "/updateLinker", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Linker updated successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to update linkers"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation updateLinker(
			@ApiParam(required=true, value="Linker to be updated, id is required, name and comment can be updated only") 
			@RequestBody Linker linkerView, 
			@ApiParam(required=false, value="summary of the changes") 
            @RequestParam(value="changeSummary", required=false)
            String changeSummary,
            @ApiParam(required=false, value="field that has changed, can provide multiple") 
            @RequestParam(value="changedField", required=false)
            List<String> changedFields,
			Principal principal) throws SQLException {
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		// validate first
		if (validator != null) {
			if  (linkerView.getName() != null) {
				Set<ConstraintViolation<Linker>> violations = validator.validateValue(Linker.class, "name", linkerView.getName().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (linkerView.getComment() != null) {
				Set<ConstraintViolation<Linker>> violations = validator.validateValue(Linker.class, "comment", linkerView.getComment().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("comment", "LengthExceeded"));
				}		
			}
			if (linkerView.getDescription() != null) {
                Set<ConstraintViolation<Linker>> violations = validator.validateValue(Linker.class, "description", linkerView.getDescription().trim());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("description", "LengthExceeded"));
                }       
            }
		
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			Linker linker= new SmallMoleculeLinker();
			linker.setUri(GlygenArrayRepository.uriPrefix + linkerView.getId());
			linker.setComment(linkerView.getComment() != null ? linkerView.getComment().trim() : linkerView.getComment());
			linker.setDescription(linkerView.getDescription() != null ? linkerView.getDescription().trim() : linkerView.getDescription());
			linker.setName(linkerView.getName() != null ? linkerView.getName().trim() : null);	
			
			Linker local = null;
			// check if name is unique
			if (linker.getName() != null && !linker.getName().isEmpty()) {
				local = linkerRepository.getLinkerByLabel(linker.getName().trim(), user);
				if (local != null && !local.getUri().equals(linker.getUri())) {   // there is another with the same name
					errorMessage.addError(new ObjectError("name", "Duplicate"));
				}
			} 
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage);
			
			ChangeLog changeLog = new ChangeLog();
            changeLog.setUser(principal.getName());
            changeLog.setChangeType(ChangeType.MINOR);
            changeLog.setDate(new Date());
            changeLog.setSummary(changeSummary);
            changeLog.setChangedFields(changedFields);
            linker.addChange(changeLog);
			linkerRepository.updateLinker(linker, user);
			return new Confirmation("Linker updated successfully", HttpStatus.OK.value());
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Error updating linker with id: " + linkerView.getId());
		}
	}
	
	@ApiOperation(value = "Update given slide layout for the user")
	@RequestMapping(value = "/updateSlideLayout", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Slide layout updated successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to update slide layout"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation updateSlideLayout(
			@ApiParam(required=true, value="Slide layout to be updated, id is required, name and comment can be updated only") 
			@RequestBody SlideLayout layout, Principal principal) throws SQLException {
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		// validate first
		if (validator != null) {
			if (layout.getName() == null || layout.getName().isEmpty()) 
				errorMessage.addError(new ObjectError("name", "NoEmpty"));
			if  (layout.getName() != null) {
				Set<ConstraintViolation<SlideLayout>> violations = validator.validateValue(SlideLayout.class, "name", layout.getName().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (layout.getDescription() != null) {
				Set<ConstraintViolation<SlideLayout>> violations = validator.validateValue(SlideLayout.class, "description", layout.getDescription().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("description", "LengthExceeded"));
				}		
			}
		
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			SlideLayout slideLayout= new SlideLayout();
			slideLayout.setUri(GlygenArrayRepository.uriPrefix + layout.getId());
			slideLayout.setDescription(layout.getDescription() != null ? layout.getDescription().trim() : layout.getDescription());
			slideLayout.setName(layout.getName() != null ? layout.getName().trim() : null);		
			
			SlideLayout local = null;
			// check if name is unique
			if (slideLayout.getName() != null && !slideLayout.getName().isEmpty()) {
				local = layoutRepository.getSlideLayoutByName(slideLayout.getName().trim(), user);
				if (local != null && !local.getUri().equals(slideLayout.getUri())) {   // there is another with the same name
					errorMessage.addError(new ObjectError("name", "Duplicate"));
				}
			} 
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid slide layout information", errorMessage);
			
			layoutRepository.updateSlideLayout(slideLayout, user);
			return new Confirmation("Slide Layout updated successfully", HttpStatus.OK.value());
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Error updating slide layout with id: " + layout.getId());
		}
	}
	
	@ApiOperation(value = "Upload file")
	@RequestMapping(value = "/upload", method=RequestMethod.POST, 
	        consumes= {"application/octet-stream"},
			produces={"application/json", "application/xml"})
	public UploadResult uploadFile(
	        //@RequestParam("file") MultipartFile file,
	        HttpEntity<byte[]> requestBody,
            @RequestParam("resumableFilename") String resumableFilename,
            @RequestParam ("resumableRelativePath") String resumableRelativePath,
            @RequestParam ("resumableTotalChunks") String resumableTotalChunks,
            @RequestParam("resumableChunkSize") int resumableChunkSize,
            @RequestParam("resumableChunkNumber") int resumableChunkNumber,
            @RequestParam("resumableTotalSize") long resumableTotalSize,
            @RequestParam("resumableIdentifier") String resumableIdentifier
    ) throws IOException, InterruptedException {
		 
        ResumableFileInfo info = ResumableInfoStorage.getInstance().get(resumableIdentifier);
        if (info == null) {
            String extension = resumableFilename.substring(resumableFilename.lastIndexOf(".")+1);
        	String uniqueFileName = System.currentTimeMillis() + "." +  extension;
            String resumableFilePath = new File(uploadDir, uniqueFileName).getAbsolutePath() + ".temp";
        	info = new ResumableFileInfo();
            info.resumableChunkSize = resumableChunkSize;
            info.resumableIdentifier = resumableIdentifier;
            info.resumableTotalSize = resumableTotalSize;
            info.resumableFilename = resumableFilename;
            info.resumableRelativePath = resumableRelativePath;
            info.resumableFilePath = resumableFilePath;
        	ResumableInfoStorage.getInstance().add(info);
        } 
        
		RandomAccessFile raf = new RandomAccessFile(info.resumableFilePath, "rw");

        //Seek to position
        raf.seek((resumableChunkNumber - 1) * (long)resumableChunkSize);
        //byte[] payload = file.getBytes();
        byte[] payload = requestBody.getBody();
        InputStream is = new ByteArrayInputStream(payload);
        long content_length = payload.length;
        long read = 0;
        byte[] bytes = new byte[1024 * 100];
        while(read < content_length) {
            int r = is.read(bytes);
            if (r < 0)  {
                break;
            }
            raf.write(bytes, 0, r);
            read += r;
        }
        raf.close();
        
        info.uploadedChunks.add(new ResumableFileInfo.ResumableChunkNumber(resumableChunkNumber));
        
        UploadResult result = new UploadResult();
        FileWrapper file = new FileWrapper();
        file.setIdentifier(info.resumableFilePath.substring(info.resumableFilePath.lastIndexOf(File.separator) + 1));
        file.setOriginalName(resumableFilename);
        file.setFileFolder(uploadDir);
        file.setFileSize(read);
        result.setFile(file);
        
        if (info.checkIfUploadFinished()) { //Check if all chunks uploaded, and change filename
            ResumableInfoStorage.getInstance().remove(info);
            result.setStatusCode(HttpStatus.OK.value());
            int index = info.resumableFilePath.indexOf(".temp") == -1 ? info.resumableFilePath.length() : info.resumableFilePath.indexOf(".temp");
            file.setIdentifier(info.resumableFilePath.substring(info.resumableFilePath.lastIndexOf(File.separator) + 1, index));
            result.setFile(file);
            return result;
        } else {
        	result.setStatusCode(HttpStatus.ACCEPTED.value());
        	return result;
        }
	}
	
	public static BufferedImage createImageForGlycan(SequenceDefinedGlycan glycan) {
	     // try to create one
	        BufferedImage t_image = null;
	        org.eurocarbdb.application.glycanbuilder.Glycan glycanObject = null;
	        try {
	            if (glycan.getSequenceType() == GlycanSequenceFormat.GLYCOCT) {
	                glycanObject = 
	                        org.eurocarbdb.application.glycanbuilder.Glycan.
	                        fromGlycoCTCondensed(glycan.getSequence().trim());
	                if (glycanObject == null && glycan.getGlytoucanId() != null) {
	                    String seq = GlytoucanUtil.getInstance().retrieveGlycan(glycan.getGlytoucanId());
	                    if (seq != null) {
	                        try {
    	                        WURCS2Parser t_wurcsparser = new WURCS2Parser();
    	                        glycanObject = t_wurcsparser.readGlycan(seq, new MassOptions());
	                        } catch (Exception e) {
	                            logger.error ("Glycan image cannot be generated with WURCS sequence", e);
	                        }
	                    }
	                }
	                
	            } else if (glycan.getSequenceType() == GlycanSequenceFormat.WURCS) {
	                WURCS2Parser t_wurcsparser = new WURCS2Parser();
	                glycanObject = t_wurcsparser.readGlycan(glycan.getSequence().trim(), new MassOptions());
	            }
	            if (glycanObject != null) {
	                t_image = glycanWorkspace.getGlycanRenderer().getImage(glycanObject, true, false, true, 3.0D);
	            } 

	        } catch (Exception e) {
	            logger.error ("Glycan image cannot be generated", e);
	            // check if there is glytoucan id
	            if (glycan.getGlytoucanId() != null) {
	                String seq = GlytoucanUtil.getInstance().retrieveGlycan(glycan.getGlytoucanId());
	                if (seq != null) {
	                    WURCS2Parser t_wurcsparser = new WURCS2Parser();
	                    try {
                            glycanObject = t_wurcsparser.readGlycan(seq, new MassOptions());
                            if (glycanObject != null) {
                                t_image = glycanWorkspace.getGlycanRenderer().getImage(glycanObject, true, false, true, 3.0D);
                            }
                        } catch (Exception e1) {
                            logger.error ("Glycan image cannot be generated from WURCS", e);
                        }
	                }
	            }
	            
	        }
	        return t_image;
	    }
}
