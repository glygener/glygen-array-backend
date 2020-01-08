package org.glygen.array.controller;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.eurocarbdb.application.glycanbuilder.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.GraphicOptions;
import org.eurocarbdb.application.glycanbuilder.MassOptions;
import org.eurocarbdb.application.glycanbuilder.Union;
import org.eurocarbdb.application.glycoworkbench.GlycanWorkspace;
import org.glycoinfo.GlycanFormatconverter.io.GlycoCT.WURCSToGlycoCT;
import org.glycoinfo.GlycanFormatconverter.io.WURCS.WURCSImporter;
import org.glygen.array.config.SesameTransactionConfig;
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
import org.glygen.array.persistence.rdf.LinkerClassification;
import org.glygen.array.persistence.rdf.LinkerType;
import org.glygen.array.persistence.rdf.MassOnlyGlycan;
import org.glygen.array.persistence.rdf.PeptideLinker;
import org.glygen.array.persistence.rdf.ProteinLinker;
import org.glygen.array.persistence.rdf.Publication;
import org.glygen.array.persistence.rdf.SequenceBasedLinker;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.SmallMoleculeLinker;
import org.glygen.array.service.FeatureRepository;
import org.glygen.array.service.GlycanRepository;
import org.glygen.array.service.GlygenArrayRepository;
import org.glygen.array.service.LayoutRepository;
import org.glygen.array.service.LinkerRepository;
import org.glygen.array.service.LinkerRepositoryImpl;
import org.glygen.array.util.GlytoucanUtil;
import org.glygen.array.util.UniProtUtil;
import org.glygen.array.util.pubchem.PubChemAPI;
import org.glygen.array.util.pubmed.DTOPublication;
import org.glygen.array.util.pubmed.PubmedUtil;
import org.glygen.array.view.BatchGlycanUploadResult;
import org.glygen.array.view.BlockLayoutResultView;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.glygen.array.view.FeatureListResultView;
import org.glygen.array.view.GlycanListResultView;
import org.glygen.array.view.LinkerListResultView;
import org.glygen.array.view.ResumableFileInfo;
import org.glygen.array.view.ResumableInfoStorage;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
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
	
	// needs to be done to initialize static variables to parse glycan sequence
	private static GlycanWorkspace glycanWorkspace = new GlycanWorkspace(null, false, new GlycanRendererAWT());
	
	static {
			// Set orientation of glycan: RL - right to left, LR - left to right, TB - top to bottom, BT - bottom to top
			glycanWorkspace.getGraphicOptions().ORIENTATION = GraphicOptions.RL;
			// Set flag to show information such as linkage positions and anomers
			glycanWorkspace.getGraphicOptions().SHOW_INFO = true;
			// Set flag to show mass
			glycanWorkspace.getGraphicOptions().SHOW_MASSES = false;
			// Set flag to show reducing end
			glycanWorkspace.getGraphicOptions().SHOW_REDEND = true;

			glycanWorkspace.setDisplay(GraphicOptions.DISPLAY_NORMAL);
			glycanWorkspace.setNotation(GraphicOptions.NOTATION_CFG);

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
			consumes={"application/json", "application/xml"},
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
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
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
			BlockLayout existing = layoutRepository.getBlockLayoutByName(layout.getName(), user);
			if (existing != null) {
				// duplicate
				errorMessage.addError(new ObjectError("name", "Duplicate"));
				throw new GlycanExistsException("A block layout with the same name already exists", errorMessage);
			} 
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Block layout cannot be added for user " + p.getName(), e);
		}
		
		if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
			throw new IllegalArgumentException("Invalid Input: Not a valid block layout information", errorMessage);
		
		try {
			return layoutRepository.addBlockLayout(layout, user);
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
	    if (feature.getType() == null || feature.getType() == FeatureType.NORMAL) {
    		if (feature.getLinker() == null || feature.getGlycans() == null || feature.getGlycans().isEmpty()) {
    			ErrorMessage errorMessage = new ErrorMessage();
    			errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
    			errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
    			if (feature.getLinker() == null)
    				errorMessage.addError(new ObjectError("linker", "NoEmpty"));
    			else 
    				errorMessage.addError(new ObjectError("glycan", "NoEmpty"));
    			throw new IllegalArgumentException("Invalid Input: Not a valid feature information", errorMessage);
    		}
	    }
		
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		
		if (feature.getName() != null && !feature.getName().trim().isEmpty()) {
		    try {
                org.glygen.array.persistence.rdf.Feature existing = featureRepository.getFeatureByLabel(feature.getName(), user);
                if (existing != null) {
                    ErrorMessage errorMessage = new ErrorMessage();
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    errorMessage.addError(new ObjectError("name", "Duplicate"));
                    throw new IllegalArgumentException("Invalid Input: Not a valid feature information", errorMessage);
                }
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Could not query existing features", e);
            }
		}
		try {
		    try {
    		    if (feature.getLinker().getUri() == null && feature.getLinker().getId() == null) {
        		    feature.getLinker().setId(addLinker(feature.getLinker(), p));
    		    }
		    } catch (Exception e) {
                logger.debug("Ignoring error: " + e.getMessage());
            }
		    // check its glycans
		    for (Glycan g: feature.getGlycans()) {
		        if (g.getUri() == null && g.getId() == null) {
		            try {
		                g.setId(addGlycan(g, p, true));
		            } catch (Exception e) {
		                logger.debug("Ignoring error: " + e.getMessage());
		            }
		        }
		    } 
			return featureRepository.addFeature(feature, user);
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
                    Map<Glycan, Integer>  positionMap = ((SequenceBasedLinker)feature.getLinker()).extractGlycans();
                    feature.setPositionMap(positionMap);
                    for (Glycan g: positionMap.keySet()) {
                        String seq = ((SequenceDefinedGlycan)g).getSequence();
                        if (seq != null) {
                            String existing = glycanRepository.getGlycanBySequence(((SequenceDefinedGlycan)g).getSequence(), user);
                            if (existing == null) {
                                // add the glycan
                                existing = addGlycan(g, p, true);
                            }
                            g.setUri(existing);
                            feature.addGlycan(g);
                        } else {
                            logger.error("Glycan in the feature with the following sequence cannot be located: " + seq);
                        }
                    }
                }
            }
			return featureRepository.addFeature(feature, user);
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
			if (glycan.getComment() != null) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "comment", glycan.getComment());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("comment", "LengthExceeded"));
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
			if (glycan.getInternalId() != null) {
				local = glycanRepository.getGlycanByInternalId(glycan.getInternalId().trim(), user);
				if (local != null) {
				    glycan.setId(local.getId());
					errorMessage.addError(new ObjectError("internalId", "Duplicate"));
				}
			}
			if (glycan.getName() != null) {
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
	public String addGlycan (@RequestBody Glycan glycan, Principal p, @RequestParam Boolean noGlytoucanRegistration) {
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
	
	@ApiOperation(value = "Register all glycans listed in a glycoworkbench file")
	@RequestMapping(value = "/addBatchGlycan", method=RequestMethod.POST, 
			consumes = {"multipart/form-data"}, produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Glycans processed successfully"), 
			@ApiResponse(code=400, message="Invalid request if file is not a text file"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register glycans"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public BatchGlycanUploadResult addGlycanFromFile (@RequestBody MultipartFile file, Principal p, @RequestParam Boolean noGlytoucanRegistration) {
		BatchGlycanUploadResult result = new BatchGlycanUploadResult();
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		try {
			ByteArrayInputStream stream = new   ByteArrayInputStream(file.getBytes());
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
							g.setMass(glycanObject.computeMass(MassOptions.ISOTOPE_MONO));
							String existing = glycanRepository.getGlycanBySequence(glycoCT, user);
							if (existing != null) {
								// duplicate, ignore
								String id = existing.substring(existing.lastIndexOf("/")+1);
								Glycan glycan = glycanRepository.getGlycanById(id, user);
								result.addDuplicateSequence(glycan);
							} else {
								String added = glycanRepository.addGlycan(g, user, noGlytoucanRegistration);
								String id = added.substring(added.lastIndexOf("/")+1);
								BufferedImage t_image = glycanWorkspace.getGlycanRenderer()
										.getImage(new Union<org.eurocarbdb.application.glycanbuilder.Glycan>(glycanObject), true, false, true, 0.5d);
								if (t_image != null) {
									//save the image into a file
									logger.debug("Adding image to " + imageLocation);
									File imageFile = new File(imageLocation + File.separator + id + ".png");
									ImageIO.write(t_image, "png", imageFile);
								}
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
			if (glycan.getComment() != null) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "comment", glycan.getComment());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("comment", "LengthExceeded"));
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
			if (glycan.getInternalId() != null) {
				local = glycanRepository.getGlycanByInternalId(glycan.getInternalId().trim(), user);
				if (local != null) {
				    glycan.setId(local.getId());
					errorMessage.addError(new ObjectError("internalId", "Duplicate"));
				}
			}
			if (glycan.getName() != null) {
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
				linkerURI = linkerRepository.getLinkerByField(linker.getSequence(), "has_sequence", "string", user);
				if (linkerURI != null) {
				    linker.setUri(linkerURI);
					errorMessage.addError(new ObjectError("sequence", "Duplicate"));
				}
			}
			
			
			l = linker;
			l.setUri(linkerURI);
			if (linker.getName() != null) {
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
			
			if (linker.getSequence() != null) {
				linkerURI = linkerRepository.getLinkerByField(linker.getSequence(), "has_sequence", "string", user);
				if (linkerURI != null) {
				    linker.setUri(linkerURI);
				    errorMessage.addError(new ObjectError("pubChemId", "Duplicate"));
				}
			}
			else if (linker.getUniProtId() != null) {
				linkerURI = linkerRepository.getLinkerByField(linker.getUniProtId(), "has_uniProtId", "string", user);
				if (linkerURI != null) {
				    linker.setUri(linkerURI);
				    errorMessage.addError(new ObjectError("uniProtId", "Duplicate"));
				}
			}
			
			l = linker;
			l.setUri(linkerURI);
			if (linker.getName() != null) {
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
			if (glycan.getComment() != null) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "comment", glycan.getComment());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("comment", "LengthExceeded"));
				}		
			}
			if (glycan.getInternalId() != null && !glycan.getInternalId().isEmpty()) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "internalId", glycan.getInternalId());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("internalId", "LengthExceeded"));
				}		
			}
			if (glycan.getGlytoucanId() != null && !glycan.getGlytoucanId().isEmpty()) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "glytoucanId", glycan.getGlytoucanId());
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
		g.setComment(glycan.getComment() != null ? glycan.getComment().trim() : glycan.getComment());
		//g.setSequence(glycan.getSequence().trim());
		//g.setSequenceType(glycan.getSequenceFormat().getLabel());
		
		String glycoCT = glycan.getSequence().trim();
		UserEntity user;
		try {
			user = userRepository.findByUsernameIgnoreCase(p.getName());
			
			if (glycan.getSequence() != null && !glycan.getSequence().trim().isEmpty()) {
				//check if the given sequence is valid
				
				boolean parseError = false;
				try {
					switch (glycan.getSequenceType()) {
					case GLYCOCT:
						glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromGlycoCTCondensed(glycan.getSequence().trim());
						glycoCT = glycan.getSequence().trim();
						break;
					case GWS:
						glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromString(glycan.getSequence().trim());
						glycoCT = glycanObject.toGlycoCTCondensed();
						break;
					case WURCS:
					    WURCSToGlycoCT wurcsConverter = new WURCSToGlycoCT();
					    wurcsConverter.start(glycan.getSequence().trim());
					    glycoCT = wurcsConverter.getGlycoCT();
					    if (glycoCT != null) 
					        glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromGlycoCTCondensed(glycoCT);
					    break;
                    case IUPAC:
                        CFGMasterListParser parser = new CFGMasterListParser();
                        glycoCT = parser.translateSequence(glycan.getSequence().trim());
                        if (glycoCT != null)
                            glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromGlycoCTCondensed(glycoCT);
                        break;
                    default:
                        break;
					}
				} catch (Exception e) {
					// parse error
					parseError = true;
				}
				// check for all possible errors 
				if (glycanObject == null) {
					parseError = true;
				} else {
					String existingURI = glycanRepository.getGlycanBySequence(glycoCT, user);
					if (existingURI != null) {
					    glycan.setId(existingURI.substring(existingURI.lastIndexOf("/")+1));
						errorMessage.addError(new ObjectError("sequence", "Duplicate"));
					}
				}
				if (parseError)
					errorMessage.addError(new ObjectError("sequence", "NotValid"));
			} else {
				errorMessage.addError(new ObjectError("sequence", "NoEmpty"));
			}
			Glycan local = null;
			// check if internalid and label are unique
			if (glycan.getInternalId() != null) {
				local = glycanRepository.getGlycanByInternalId(glycan.getInternalId().trim(), user);
				if (local != null) {
				    glycan.setId(local.getId());
					errorMessage.addError(new ObjectError("internalId", "Duplicate"));
				}
			}
			if (glycan.getName() != null) {
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
			if (glycanObject != null) {
				g.setMass(glycanObject.computeMass(MassOptions.ISOTOPE_MONO));
				g.setSequence(glycoCT);
				g.setSequenceType(GlycanSequenceFormat.GLYCOCT);
				BufferedImage t_image = glycanWorkspace.getGlycanRenderer()
						.getImage(new Union<org.eurocarbdb.application.glycanbuilder.Glycan>(glycanObject), true, false, true, 0.5d);
				if (t_image != null) {
					String glycanURI = glycanRepository.addGlycan(g, user, noGlytoucanRegistration);
					String id = glycanURI.substring(glycanURI.lastIndexOf("/")+1);
					Glycan added = glycanRepository.getGlycanById(id, user);
					if (added != null) {
						String filename = id + ".png";
						//save the image into a file
						logger.debug("Adding image to " + imageLocation);
						File imageFile = new File(imageLocation + File.separator + filename);
						ImageIO.write(t_image, "png", imageFile);
					} else {
						logger.error("Added glycan cannot be retrieved back");
						throw new GlycanRepositoryException("Glycan image cannot be generated");
					}
					return id;
				} else {
					logger.error("Glycan image is null");
					throw new GlycanRepositoryException("Glycan image cannot be generated");
				}
				
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
	
	@ApiOperation(value = "Add given slide layout for the user")
	@RequestMapping(value="/addslidelayout", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
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
			return layoutRepository.addSlideLayout(layout, user);
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
			else if (linker.getInChiKey() != null) {
				linkerURI = linkerRepository.getLinkerByField(linker.getInChiKey(), "has_inChI_key", "string", user);
				if (linkerURI != null) {
				    linker.setUri(linkerURI);
					errorMessage.addError(new ObjectError("inChiKey", "Duplicate"));	
				}
			}
			if (linkerURI == null) {
				// get the linker details from pubChem
				if (linker.getPubChemId() != null || linker.getInChiKey() != null) {
					try {
						if (linker.getPubChemId() != null) {
						    l = PubChemAPI.getLinkerDetailsFromPubChem(linker.getPubChemId());
						} else if (linker.getInChiKey() != null) {
						    l = PubChemAPI.getLinkerDetailsFromPubChemByInchiKey(linker.getInChiKey());
						}
						if (l == null) {
							// could not get details from PubChem
							errorMessage.addError(new ObjectError("pubChemId", "NotValid"));
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
						errorMessage.addError(new ObjectError("pubChemId", "NotValid"));
					}
				}
				else {
					l = linker;
					l.setUri(linkerURI);
				}
				
				if (linker.getName() != null) {
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
                                l.addPublication (getPublicationFrom(publication));
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
			throw new GlycanRepositoryException("Cannot delete slide layout " + layoutId);
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
				throw new EntityNotFoundException("Block layout with id : " + layoutId + " does not exist in the repository");
			}
			
			return layout;
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Block Layout cannot be retrieved for user " + p.getName(), e);
		}
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
	        for (org.grits.toolbox.glycanarray.library.om.layout.SlideLayout slideLayout : layoutList) {
	        	if (slideLayout.getName().equalsIgnoreCase(layout.getName())) {
	        		SlideLayout mySlideLayout = new SlideLayout();
	        		mySlideLayout.setName(slideLayout.getName());
	        		String desc = null;
	        		if (slideLayout.getDescription() != null) {
	        			desc = slideLayout.getDescription();
	        			if (desc.length() >= 250) {
	        				desc = desc.substring(0, 249);
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
	        			org.glygen.array.persistence.rdf.BlockLayout myLayout = new org.glygen.array.persistence.rdf.BlockLayout();
	        			String name = null;
	        			if (blockLayout.getName() != null) {
	        				name = blockLayout.getName();
	        				if (name.length() >= 100) {
	        					name = name.substring(0, 99);
	        				}
	        			}
	        			myLayout.setName(name);
	        			myLayout.setWidth(blockLayout.getColumnNum());
	        			myLayout.setHeight(blockLayout.getRowNum());
	        			String comment = null;
	        			if (blockLayout.getComment() != null) {
	        				comment = blockLayout.getComment().replaceAll("\\r", " ").replaceAll("\\n", " ");
	        				if (comment.length() >= 250) {
	        					comment = comment.substring(0, 249);
	        				}
	        			}
	        			myLayout.setDescription(comment);
	        			myBlock.setBlockLayout(myLayout);
	        			List<org.glygen.array.persistence.rdf.Spot> spots = getSpotsFromBlockLayout(library, blockLayout);
	        			myBlock.setSpots(spots);
	        			myLayout.setSpots(spots);
	        			blocks.add(myBlock);
	        		}
	        		
	        		mySlideLayout.setHeight(slideLayout.getHeight() == null ? height: slideLayout.getHeight());
	        		mySlideLayout.setWidth(slideLayout.getWidth() == null ? width: slideLayout.getWidth());
	        		mySlideLayout.setBlocks(blocks);
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
	/*
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
			@ApiParam(required=false, value="if new name is provided, it is assumed that the list (post data) contains a single slide layout"
					+ " and that layout is added to the repository with the given new name")
			@RequestParam(value="newname", required=false) String newName, Principal p) {
		if (newName != null) {
			try {
				newName = URLDecoder.decode(newName, StandardCharsets.UTF_8.name());
			} catch (UnsupportedEncodingException e) {
				// should not happen
				logger.debug("Cannot decode newName", e);
			}
		}
		if (uploadedFileName != null) {
			File libraryFile = new File(uploadDir, uploadedFileName);
			if (libraryFile.exists()) {
				if (slideLayouts == null || slideLayouts.isEmpty()) {
					ErrorMessage errorMessage = new ErrorMessage();
					errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
					errorMessage.addError(new ObjectError("slideLayouts", "NoEmpty"));
					throw new IllegalArgumentException("No slide layouts provided", errorMessage);
				}
				ImportGRITSLibraryResult result = new ImportGRITSLibraryResult();
				
				List<BlockLayout> addedLayouts = new ArrayList<BlockLayout>();
				
				for (SlideLayout slideLayout: slideLayouts) {
					// check if already exists before trying to import
					if (slideLayout.getName() != null) {
						try {
							UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
							String searchName = slideLayout.getName();
							if (newName != null && newName.length() > 0) {
								searchName = newName;
							}
							SlideLayout existing = layoutRepository.getSlideLayoutByName(searchName, user);
							if (existing != null) {
								result.getDuplicates().add(slideLayout);
								continue;
							}
						} catch (Exception e) {
							result.getErrors().add(slideLayout);
							continue;
						}
					}
					slideLayout = getFullLayoutFromLibrary (libraryFile, slideLayout);
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
										for (org.glygen.array.persistence.rdf.Spot spot: block.getSpots()) {
											for (org.glygen.array.persistence.rdf.Feature feature: spot.getFeatures()) {
												if (feature.getGlycan() != null) {
													if (!glycanCache.contains(feature.getGlycan())) {
														glycanCache.add(feature.getGlycan());
														try {	
															addGlycan(getGlycanView(feature.getGlycan()), p);
														} catch (Exception e) {
															if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
																ErrorMessage error = (ErrorMessage) e.getCause();
																for (ObjectError err: error.getErrors()) {
																	if (err.getObjectName().equalsIgnoreCase("sequence") && 
																			err.getDefaultMessage().equalsIgnoreCase("duplicate")) {
																		if (feature.getGlycan().getName() != null) {
																			// add name as an alias
																			String existingId = getGlycanBySequence(feature.getGlycan().getSequence(), p);
																			addAliasForGlycan(existingId, feature.getGlycan().getName(), p);
																		}
																		break;
																	}
																}
															} else {
																logger.info("Could not add glycan: ", e);
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
																	}
																}
																if (needAlias) {		
																	LinkerView linker = getLinkerView(feature.getLinker());
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
															}
														}
													}
												}
											}
										}
										addBlockLayout(block.getBlockLayout(), p);
									}
								} catch (Exception e) {
									logger.info("Cannot add block layout", e);
								}
							}
						}
						
						try {
							if (newName != null && newName.length() > 0) {
								slideLayout.setName(newName);
							}
							addSlideLayout(slideLayout, p);
							result.getAddedLayouts().add(slideLayout);
						} catch (Exception e) {
							if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
								ErrorMessage error = (ErrorMessage) e.getCause();
								for (ObjectError err: error.getErrors()) {
									if (err.getDefaultMessage().contains("Duplicate")) {
										result.getDuplicates().add(slideLayout);
									}
								}
							} else {
								logger.debug("Could not add slide layout", e);
								result.getErrors().add(slideLayout);
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
	*/
	
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
				throw new EntityNotFoundException("Glycan with id : " + glycanId + " does not exist in the repository");
			}
			byte[] cartoon = getCartoonForGlycan(glycanId);
			glycan.setCartoon(cartoon);
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
		try {
			String wurcsSequence = GlytoucanUtil.getInstance().retrieveGlycan(glytoucanId);
			if (wurcsSequence == null) {
				// cannot be found in Glytoucan
				throw new EntityNotFoundException("Glycan with accession number " + glytoucanId + " cannot be retrieved");
			} else {
				// convert sequence into GlycoCT and return
				WURCSToGlycoCT exporter = new WURCSToGlycoCT();
				exporter.start(wurcsSequence);
				if ( !exporter.getErrorMessages().isEmpty() )
					throw new GlycanRepositoryException(exporter.getErrorMessages());
				return exporter.getGlycoCT();
			}
			
		} catch (Exception e) {
			throw new GlycanRepositoryException("error getting glycan from Glytoucan. Reason: " + e.getMessage(), e);
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
			@ApiParam(required=true, value="GlyToucan id of the glycan to retrieve the image for") 
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
				throw new EntityNotFoundException("Linker with id : " + linkerId + " does not exist in the repository");
			}
			
			return linker;
			
			
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Linker cannot be retrieved for user " + p.getName(), e);
		}
		
	}
	
	@ApiOperation(value = "retrieves list of possible linker classifications", response = List.class)
	@RequestMapping(value = "/getLinkerClassifications", method = RequestMethod.GET)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "list returned successfully"),
			@ApiResponse(code = 415, message = "Media type is not supported"),
			@ApiResponse(code = 500, message = "Internal Server Error") })
	public List<LinkerClassification> getLinkerClassifications () throws SparqlException, SQLException {
		List<LinkerClassification> classificationList = new ArrayList<LinkerClassification>();
		
		try {
			Resource classificationNamespace = new ClassPathResource("linkerclassifications.csv");
			final InputStream inputStream = classificationNamespace.getInputStream();
			final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
	
			String line;
			while ((line = bufferedReader.readLine()) != null) {
			    String[] tokens = line.split(",");
			    if (tokens.length == 2) {
			    	Integer chebiID = Integer.parseInt(tokens[0]);
			    	String classicationValue = tokens[1];
			    	LinkerClassification classification = new LinkerClassification();
			    	classification.setChebiId(chebiID);
			    	classification.setClassification(classicationValue);
			    	classification.setUri(PubChemAPI.CHEBI_URI + chebiID);
			    	classificationList.add(classification);
			    }
			}
		} catch (Exception e) {
			logger.error("Cannot load linker classification", e);
		}
		
		return classificationList;
		
	}
	
	@ApiOperation(value = "Retrieve linker details from Pubchem with the given pubchem compound id or inchikey")
    @RequestMapping(value="/getlinkerFromPubChem/{pubchemid}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Linker retrieved successfully"), 
            @ApiResponse(code=404, message="Linker details with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Linker getLinkerDetailsFromPubChem (
            @ApiParam(required=true, value="pubchemid or the inchikey or the smiles of the linker to retrieve") 
            @PathVariable("pubchemid") String pubchemid) {
	    if (pubchemid == null || pubchemid.isEmpty()) {
	        ErrorMessage errorMessage = new ErrorMessage();
	        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
	        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
	        errorMessage.addError(new ObjectError("pubchemid", "NoEmpty"));
	        throw new IllegalArgumentException("pubchem id should be provided", errorMessage);
	    }
	    try {
	        Long pubChem = Long.parseLong(pubchemid);
	        Linker linker = PubChemAPI.getLinkerDetailsFromPubChem(pubChem);
	        if (linker == null) {
	            ErrorMessage errorMessage = new ErrorMessage();
	            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
	            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
	            errorMessage.addError(new ObjectError("pubchemid", "NotValid"));
	            throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage); 
	        }
	        return linker; 
	    } catch (NumberFormatException e) {
	        Linker linker = PubChemAPI.getLinkerDetailsFromPubChemByInchiKey(pubchemid);
	        if (linker == null) {
	            linker = PubChemAPI.getLinkerDetailsFromPubChemBySmiles(pubchemid);
	            if (linker == null) {
    	            ErrorMessage errorMessage = new ErrorMessage();
                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    errorMessage.addError(new ObjectError("pubchemid", "NotValid"));
                    throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage); 
	            }
	            return linker;
            }
            return linker; 
	    } 
    }
	
	@ApiOperation(value = "Retrieve publication details from Pubmed with the given pubmed id")
    @RequestMapping(value="/getPublicationFromPubmed/{pubmedid}", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Publication retrieved successfully"), 
            @ApiResponse(code=404, message="Publication with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public Publication getPublicationDetailsFromPubMed (
            @ApiParam(required=true, value="pubmed id for the publication") 
            @PathVariable("pubmedid") Integer pubmedid) {
	    if (pubmedid == null) {
	        ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("pubmedid", "NotValid"));
            throw new IllegalArgumentException("Invalid Input: Not a valid publication information", errorMessage);
	    }
	    PubmedUtil util = new PubmedUtil();
	    try {
            DTOPublication pub = util.createFromPubmedId(pubmedid);
            return getPublicationFrom(pub);
        } catch (Exception e) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("pubmedid", "NotValid"));
            throw new IllegalArgumentException("Invalid Input: Not a valid publication information", errorMessage);
        }
	}
	
	Publication getPublicationFrom (DTOPublication pub) {
	    Publication publication = new Publication ();
        publication.setAuthors(pub.getFormattedAuthor());
        publication.setDoiId(pub.getDoiId());
        publication.setEndPage(pub.getEndPage());
        publication.setJournal(pub.getJournal());
        publication.setNumber(pub.getNumber());
        publication.setPubmedId(pub.getPubmedId());
        publication.setStartPage(pub.getStartPage());
        publication.setTitle(pub.getTitle());
        publication.setVolume(pub.getVolume());
        publication.setYear(pub.getYear());
        
        return publication;
	}
	
	@ApiOperation(value = "Retrieve protein sequence from UniProt with the given uniprot id")
    @RequestMapping(value="/getSequenceFromUniprot/{uniprotid}", method = RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(code=200, message="Sequence retrieved successfully"), 
            @ApiResponse(code=404, message="Sequence with given id does not exist"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String getSequenceFromUniProt (
            @ApiParam(required=true, value="uniprotid such as P12345") 
            @PathVariable("uniprotid") String uniprotId) {
        if (uniprotId == null || uniprotId.isEmpty()) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("uniprotId", "NoEmpty"));
            throw new IllegalArgumentException("uniprotId should be provided", errorMessage);
        }
        try {
            String sequence = UniProtUtil.getSequenceFromUniProt(uniprotId);
            if (sequence == null) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                errorMessage.addError(new ObjectError("uniprotId", "NotValid"));
                throw new IllegalArgumentException("uniprotId does not exist", errorMessage);
            }
            return sequence;
        } catch (Exception e) {
            logger.error("Could not retrieve from uniprot", e);
            throw new GlycanRepositoryException("Failed to retieve from uniprot", e);
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
				throw new EntityNotFoundException("Slide layout with id : " + layoutId + " does not exist in the repository");
			}
			
			return layout;
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Slide Layout cannot be retrieved for user " + p.getName(), e);
		}
	}
	
	
	/**
	 * the library file should already be uploaded to "uploadDir" before calling this service.
	 * 
	 * @param uploadedFileName the name of the library file already uploaded
	 * @return list of slide layouts in the library
	 */
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
		        		//	Integer blockLayoutId = block.getLayoutId();
		        		//	org.grits.toolbox.glycanarray.library.om.layout.BlockLayout blockLayout = LibraryInterface.getBlockLayout(library, blockLayoutId);
		        		//	org.glygen.array.persistence.rdf.BlockLayout myLayout = new org.glygen.array.persistence.rdf.BlockLayout();
		        		//	myLayout.setName(blockLayout.getName());
		        		//	myBlock.setBlockLayout(myLayout);
		        		//	myBlock.setSpots(getSpotsFromBlockLayout(library, blockLayout));
		        		//	blocks.add(myBlock);
		        		}
		        		
		        		mySlideLayout.setHeight(slideLayout.getHeight() == null ? height: slideLayout.getHeight());
		        		mySlideLayout.setWidth(slideLayout.getWidth() == null ? width: slideLayout.getWidth());
		        		//mySlideLayout.setBlocks(blocks);
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
    	for (Spot spot: blockLayout.getSpot()) {
    		org.glygen.array.persistence.rdf.Spot s = new org.glygen.array.persistence.rdf.Spot();
    		s.setRow(spot.getY());
    		s.setColumn(spot.getX());
    		s.setGroup(spot.getGroup());
    		s.setConcentration(spot.getConcentration());
    		Feature feature = LibraryInterface.getFeature(library, spot.getFeatureId());
    		List<org.glygen.array.persistence.rdf.Feature> features = new ArrayList<>();
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
        					org.grits.toolbox.glycanarray.library.om.feature.Glycan glycan = LibraryInterface.getGlycan(library, r1.getItemId());
        					if (glycan != null) {
		        				SequenceDefinedGlycan myGlycan = new SequenceDefinedGlycan();
		        				myGlycan.setSequence(glycan.getSequence());  
		        				myGlycan.setName(glycan.getName());
		        				myGlycan.setComment(glycan.getComment());
		        				myGlycan.setGlytoucanId(glycan.getGlyTouCanId());
		        				myGlycan.setSequenceType(GlycanSequenceFormat.GLYCOCT);
		        				myGlycan.setInternalId(glycan.getId() == null ? "" : glycan.getId().toString());
		        				myFeature.getGlycans().add(myGlycan);
        					}
		        			org.grits.toolbox.glycanarray.library.om.feature.Linker linker = LibraryInterface.getLinker(library, probe.getLinker());
		        			if (linker != null) {
		        				Linker myLinker = new SmallMoleculeLinker();
		        				if (linker.getPubChemId() != null) ((SmallMoleculeLinker) myLinker).setPubChemId(linker.getPubChemId().longValue());
		        				myLinker.setName(linker.getName());
		        				myLinker.setComment(linker.getComment());
		        				myFeature.setLinker(myLinker);
		        			}
		        			myFeature.setRatio(r1.getItemRatio());
		        			features.add(myFeature);
        				}
        			}			
        		}
    		}
    		s.setFeatures(features);
    		spots.add(s);
    	}
    	
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
			List<BlockLayout> layouts = layoutRepository.getBlockLayoutByUser(user, offset, limit, field, loadAll, order);
			result.setRows(layouts);
			result.setTotal(total);
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
			@RequestParam(value="order", required=false) Integer order, Principal p) {
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
			
			List<org.glygen.array.persistence.rdf.Feature> features = featureRepository.getFeatureByUser(user, offset, limit, field, order);
			result.setRows(features);
			result.setTotal(total);
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot retrieve linkers for user. Reason: " + e.getMessage());
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
			@RequestParam(value="order", required=false) Integer order, Principal p) {
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
			
			List<Glycan> glycans = glycanRepository.getGlycanByUser(user, offset, limit, field, order);
			for (Glycan glycan : glycans) {
			    if (glycan.getType().equals(GlycanType.SEQUENCE_DEFINED)) {
			        glycan.setCartoon(getCartoonForGlycan(glycan.getId()));
			    }
			}
			
			result.setRows(glycans);
			result.setTotal(total);
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot retrieve glycans for user. Reason: " + e.getMessage());
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
			@RequestParam(value="order", required=false) Integer order, Principal p) {
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
			
			List<Linker> linkers = linkerRepository.getLinkerByUser(user, offset, limit, field, order);
			result.setRows(linkers);
			result.setTotal(total);
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
			List<SlideLayout> layouts = layoutRepository.getSlideLayoutByUser(user, offset, limit, field, loadAll, order);
			result.setRows(layouts);
			result.setTotal(total);
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot retrieve slide layouts for user. Reason: " + e.getMessage());
		}
		
		return result;
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
        	throw new IllegalArgumentException("Chunk identifier is not valid");
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
				local = layoutRepository.getBlockLayoutByName(blockLayout.getName().trim(), user);
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
			@RequestBody Glycan glycanView, Principal principal) throws SQLException {
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
			if (glycanView.getComment() != null) {
				Set<ConstraintViolation<Glycan>> violations = validator.validateValue(Glycan.class, "comment", glycanView.getComment().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("comment", "LengthExceeded"));
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
			glycan.setUri(GlygenArrayRepository.uriPrefix + glycanView.getId());
			glycan.setInternalId(glycanView.getInternalId() != null ? glycanView.getInternalId().trim(): glycanView.getInternalId());
			glycan.setComment(glycanView.getComment() != null ? glycanView.getComment().trim() : glycanView.getComment());
			glycan.setName(glycanView.getName() != null ? glycanView.getName().trim() : null);		
			
			Glycan local = null;
			// check if internalid and label are unique
			if (glycan.getInternalId() != null && !glycan.getInternalId().isEmpty()) {
				local = glycanRepository.getGlycanByInternalId(glycan.getInternalId().trim(), user);
				if (local != null && !local.getUri().equals(glycan.getUri())) {   // there is another with the same internal id
					errorMessage.addError(new ObjectError("internalId", "Duplicate"));
				}
			}
			if (glycan.getName() != null && !glycan.getName().isEmpty()) {
				local = glycanRepository.getGlycanByLabel(glycan.getName().trim(), user);
				if (local != null && !local.getUri().equals(glycan.getUri())) {   // there is another with the same name
					errorMessage.addError(new ObjectError("name", "Duplicate"));
				}
			} 
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
			
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
			@RequestBody Linker linkerView, Principal principal) throws SQLException {
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
		
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			Linker linker= new SmallMoleculeLinker();
			linker.setUri(GlygenArrayRepository.uriPrefix + linkerView.getId());
			linker.setComment(linkerView.getComment() != null ? linkerView.getComment().trim() : linkerView.getComment());
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
			produces={"application/json", "application/xml"})
	public UploadResult uploadFile(
			HttpEntity<byte[]> requestEntity,
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
        	String uniqueFileName = resumableFilename + System.currentTimeMillis();
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
        byte[] payload = requestEntity.getBody();
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
        result.setAssignedFileName(info.resumableFilePath.substring(info.resumableFilePath.lastIndexOf(File.separator) + 1));
        
        if (info.checkIfUploadFinished()) { //Check if all chunks uploaded, and change filename
            ResumableInfoStorage.getInstance().remove(info);
            result.setStatusCode(HttpStatus.OK.value());
            int index = info.resumableFilePath.indexOf(".temp") == -1 ? info.resumableFilePath.length() : info.resumableFilePath.indexOf(".temp");
            result.setAssignedFileName(info.resumableFilePath.substring(info.resumableFilePath.lastIndexOf(File.separator) + 1, index));
            return result;
        } else {
        	result.setStatusCode(HttpStatus.ACCEPTED.value());
        	return result;
        }
	}
}
