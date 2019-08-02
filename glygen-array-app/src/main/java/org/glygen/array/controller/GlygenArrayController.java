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
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.glygen.array.config.SesameTransactionConfig;
import org.glygen.array.exception.GlycanExistsException;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.exception.UploadNotFinishedException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.service.GlygenArrayRepository;
import org.glygen.array.util.PubChemAPI;
import org.glygen.array.view.BatchGlycanUploadResult;
import org.glygen.array.view.BlockLayoutResultView;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.glygen.array.view.GlycanListResultView;
import org.glygen.array.view.GlycanSequenceFormat;
import org.glygen.array.view.GlycanView;
import org.glygen.array.view.ImportGRITSLibraryResult;
import org.glygen.array.view.LinkerListResultView;
import org.glygen.array.view.LinkerView;
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
import org.grits.toolbox.util.structure.glycan.util.FilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
	
	@Autowired
	SesameSparqlDAO sparqlDAO;
	
	@Autowired
	GlygenArrayRepository repository;
	
	@Autowired
	UserRepository userRepository;
	
	@Value("${spring.file.imagedirectory}")
	String imageLocation;
	
	@Value("${spring.file.uploaddirectory}")
	String uploadDir;
	
	@Autowired
	Validator validator;

	// needs to be done to initialize static variables to parse glycan sequence
	private static GlycanWorkspace glycanWorkspace = new GlycanWorkspace(null, false, new GlycanRendererAWT());
	
	List<Glycan> glycanCache = new ArrayList<Glycan>();
	List<Linker> linkerCache = new ArrayList<Linker>();
	
	static {
			// Set orientation of glycan: RL - right to left, LR - left to right, TB - top to bottom, BT - bottom to top
			glycanWorkspace.getGraphicOptions().ORIENTATION = GraphicOptions.RL;
			// Set flag to show information such as linkage positions and anomers
			glycanWorkspace.getGraphicOptions().SHOW_INFO = true;
			// Set flag to show mass
			glycanWorkspace.getGraphicOptions().SHOW_MASSES = false;
			// Set flag to show reducing end
			glycanWorkspace.getGraphicOptions().SHOW_REDEND = true;

//			glycanWorkspase.setDisplay(GraphicOptions.DISPLAY_NORMAL);
//			glycanWorkspase.setNotation(GraphicOptions.NOTATION_CFG);

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
	public Confirmation addSlideLayout (
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
			SlideLayout existing = repository.getSlideLayoutByName(layout.getName(), user);
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
			repository.addSlideLayout(layout, user);
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Slide layout cannot be added for user " + p.getName(), e);
		}
		
	
		return new Confirmation("Slide Layout added successfully", HttpStatus.CREATED.value());
	}
	
	@ApiOperation(value = "Checks whether the given slidelayout name is available to be used (returns true if available, false if alredy in use", response = Boolean.class)
	@RequestMapping(value = "/checkSlidelayoutName", method = RequestMethod.GET)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Check performed successfully"),
			@ApiResponse(code = 415, message = "Media type is not supported"),
			@ApiResponse(code = 500, message = "Internal Server Error") })
	public Boolean checkSlidelayoutName(@RequestParam("slidelayoutname") final String slidelayoutname, Principal principal) throws SparqlException, SQLException {

		UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
		
		SlideLayout existing = repository.getSlideLayoutByName(slidelayoutname, user);

		if (existing != null) {
			// duplicate
			ErrorMessage errorMessage = new ErrorMessage("Cannot add duplicate slide layout");
			errorMessage.addError(new ObjectError("slidelayoutname", "Duplicate"));
			throw new GlycanExistsException("A slide layout with the same name already exists", errorMessage);
		}

		return true;
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
	public BatchGlycanUploadResult addGlycanFromFile (@RequestBody MultipartFile file, Principal p) {
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
							Glycan g = new Glycan();
							g.setSequence(glycoCT);
							g.setSequenceType(GlycanSequenceFormat.GLYCOCT.getLabel());
							g.setMass(glycanObject.computeMass(MassOptions.ISOTOPE_MONO));
							String existing = repository.getGlycanBySequence(glycoCT, user);
							if (existing != null) {
								// duplicate, ignore
								String id = existing.substring(existing.lastIndexOf("/")+1);
								Glycan glycan = repository.getGlycanById(id, user);
								result.addDuplicateSequence(getGlycanView(glycan));
							} else {
								String added = repository.addGlycan(g, user);
								String id = added.substring(added.lastIndexOf("/")+1);
								BufferedImage t_image = glycanWorkspace.getGlycanRenderer()
										.getImage(new Union<org.eurocarbdb.application.glycanbuilder.Glycan>(glycanObject), true, false, true, 0.5d);
								if (t_image != null) {
									//save the image into a file
									logger.debug("Adding image to " + imageLocation);
									File imageFile = new File(imageLocation + File.separator + id + ".png");
									ImageIO.write(t_image, "png", imageFile);
								}
								Glycan addedGlycan = repository.getGlycanById(id, user);
								result.getAddedGlycans().add(getGlycanView(addedGlycan));
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
	
	@ApiOperation(value = "Add given glycan for the user")
	@RequestMapping(value="/addglycan", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Glycan added successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register glycans"),
			@ApiResponse(code=409, message="A glycan with the given sequence already exists!"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation addGlycan (@RequestBody GlycanView glycan, Principal p) {
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
				Set<ConstraintViolation<GlycanView>> violations = validator.validateValue(GlycanView.class, "name", glycan.getName());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (glycan.getComment() != null) {
				Set<ConstraintViolation<GlycanView>> violations = validator.validateValue(GlycanView.class, "comment", glycan.getComment());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("comment", "LengthExceeded"));
				}		
			}
			if (glycan.getInternalId() != null && !glycan.getInternalId().isEmpty()) {
				Set<ConstraintViolation<GlycanView>> violations = validator.validateValue(GlycanView.class, "internalId", glycan.getInternalId());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("internalId", "LengthExceeded"));
				}		
			}
			if (glycan.getGlytoucanId() != null && !glycan.getGlytoucanId().isEmpty()) {
				Set<ConstraintViolation<GlycanView>> violations = validator.validateValue(GlycanView.class, "glytoucanId", glycan.getGlytoucanId());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("glytoucanId", "LengthExceeded"));
				}		
			}
			
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		
		org.eurocarbdb.application.glycanbuilder.Glycan glycanObject= null;
		Glycan g = new Glycan();
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
					switch (glycan.getSequenceFormat()) {
						case GLYCOCT:
							glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromGlycoCTCondensed(glycan.getSequence().trim());
							break;
						case GWS:
							glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromString(glycan.getSequence().trim());
							glycoCT = glycanObject.toGlycoCTCondensed();
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
					String existingURI = repository.getGlycanBySequence(glycoCT, user);
					if (existingURI != null) {
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
				local = repository.getGlycanByInternalId(glycan.getInternalId().trim(), user);
				if (local != null) {
					errorMessage.addError(new ObjectError("internalId", "Duplicate"));
				}
			}
			if (glycan.getName() != null) {
				local = repository.getGlycanByLabel(glycan.getName().trim(), user);
				if (local != null) {
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
				g.setSequenceType(GlycanSequenceFormat.GLYCOCT.getLabel());
				String existingURI = repository.getGlycanBySequence(g.getSequence());
				if (existingURI == null) {
					BufferedImage t_image = glycanWorkspace.getGlycanRenderer()
							.getImage(new Union<org.eurocarbdb.application.glycanbuilder.Glycan>(glycanObject), true, false, true, 0.5d);
					if (t_image != null) {
						String glycanURI = repository.addGlycan(g, user);
						String id = glycanURI.substring(glycanURI.lastIndexOf("/")+1);
						Glycan added = repository.getGlycanById(id, user);
						if (added != null) {
							String filename = null;
							if (added.getGlytoucanId() == null || added.getGlytoucanId().isEmpty()) 
								filename = id + ".png";
							else
								filename = added.getGlytoucanId() + ".png";
							//save the image into a file
							logger.debug("Adding image to " + imageLocation);
							File imageFile = new File(imageLocation + File.separator + filename);
							ImageIO.write(t_image, "png", imageFile);
						} else {
							logger.error("Added glycan cannot be retrieved back");
							throw new GlycanRepositoryException("Glycan image cannot be generated");
						}
					} else {
						logger.error("Glycan image is null");
						throw new GlycanRepositoryException("Glycan image cannot be generated");
					}
				}
				else {
					// still add to the user's local repo
					// no need to generate the image again
					repository.addGlycan(g, user);
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
		
		return new Confirmation("Glycan added successfully", HttpStatus.CREATED.value());
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
			repository.deleteGlycan(glycanId, user);
			return new Confirmation("Glycan deleted successfully", HttpStatus.OK.value());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot delete glycan " + glycanId);
		} 
	}
	
	@ApiOperation(value = "Delete given linker from the user's list")
	@RequestMapping(value="/deleteLinker/{linkerId}", method = RequestMethod.DELETE, 
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
			repository.deleteLinker(linkerId, user);
			return new Confirmation("Linker deleted successfully", HttpStatus.OK.value());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot delete linker " + linkerId);
		} 
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
			repository.deleteBlockLayout(blockLayoutId, user);
			return new Confirmation("Block Layout deleted successfully", HttpStatus.OK.value());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot delete block layout " + blockLayoutId);
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
			repository.deleteSlideLayout(layoutId, user);
			return new Confirmation("Slide Layout deleted successfully", HttpStatus.OK.value());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot delete slide layout " + layoutId);
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
			@RequestBody GlycanView glycanView, Principal principal) throws SQLException {
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		// validate first
		if (validator != null) {
			if  (glycanView.getName() != null) {
				Set<ConstraintViolation<GlycanView>> violations = validator.validateValue(GlycanView.class, "name", glycanView.getName().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (glycanView.getComment() != null) {
				Set<ConstraintViolation<GlycanView>> violations = validator.validateValue(GlycanView.class, "comment", glycanView.getComment().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("comment", "LengthExceeded"));
				}		
			}
			if (glycanView.getInternalId() != null && !glycanView.getInternalId().isEmpty()) {
				Set<ConstraintViolation<GlycanView>> violations = validator.validateValue(GlycanView.class, "internalId", glycanView.getInternalId().trim());
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
				local = repository.getGlycanByInternalId(glycan.getInternalId().trim(), user);
				if (local != null && !local.getUri().equals(glycan.getUri())) {   // there is another with the same internal id
					errorMessage.addError(new ObjectError("internalId", "Duplicate"));
				}
			}
			if (glycan.getName() != null && !glycan.getName().isEmpty()) {
				local = repository.getGlycanByLabel(glycan.getName().trim(), user);
				if (local != null && !local.getUri().equals(glycan.getUri())) {   // there is another with the same name
					errorMessage.addError(new ObjectError("name", "Duplicate"));
				}
			} 
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
			
			repository.updateGlycan(glycan, user);
			return new Confirmation("Glycan updated successfully", HttpStatus.OK.value());
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Error updating glycan with id: " + glycanView.getId());
		}
	}
	
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
			repository.addAliasForGlycan(glycanId, alias, user);
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Error updating glycan with glycanId: " +glycanId);
		}
		return new Confirmation("Glycan updated successfully with new alias", HttpStatus.OK.value());
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
			String glycanURI = repository.getGlycanBySequence(seq.trim(), user);
			if (glycanURI != null)
				return glycanURI.substring(glycanURI.lastIndexOf("/") + 1);
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("error getting glycan: ", e);
		} catch (UnsupportedEncodingException e) {
			logger.info(e.getMessage());  // ignore, should not happen
		}
		return null;
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
		List<GlycanView> glycanList = new ArrayList<GlycanView>();
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
			
			int total = repository.getGlycanCountByUser (user);
			
			List<Glycan> glycans = repository.getGlycanByUser(user, offset, limit, field, order);
			for (Glycan glycan : glycans) {
				glycanList.add(getGlycanView(glycan));
			}
			
			result.setRows(glycanList);
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
    		@ApiResponse(code=500, message="Internal Server Error")})
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
		List<LinkerView> linkerList = new ArrayList<LinkerView>();
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
			
			int total = repository.getLinkerCountByUser (user);
			
			List<Linker> linkers = repository.getLinkerByUser(user, offset, limit, field, order);
			for (Linker linker : linkers) {
				linkerList.add(getLinkerView(linker));
			}
			
			result.setRows(linkerList);
			result.setTotal(total);
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot retrieve linkers for user. Reason: " + e.getMessage());
		}
		
		return result;
	}
	
	@ApiOperation(value = "List all block layouts for the user")
	@RequestMapping(value="/listBlocklayouts", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Block layouts retrieved successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error for arguments"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list block layouts"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
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
			
			int total = repository.getBlockLayoutCountByUser (user);
			List<BlockLayout> layouts = repository.getBlockLayoutByUser(user, offset, limit, field, loadAll, order);
			result.setRows(layouts);
			result.setTotal(total);
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot retrieve block layouts for user. Reason: " + e.getMessage());
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
			@ApiResponse(code=500, message="Internal Server Error")})
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
			
			int total = repository.getSlideLayoutCountByUser (user);
			List<SlideLayout> layouts = repository.getSlideLayoutByUser(user, offset, limit, field, loadAll, order);
			result.setRows(layouts);
			result.setTotal(total);
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot retrieve slide layouts for user. Reason: " + e.getMessage());
		}
		
		return result;
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
			BlockLayout layout = repository.getBlockLayoutById(layoutId, user, loadAll);
			if (layout == null) {
				throw new EntityNotFoundException("Block layout with id : " + layoutId + " does not exist in the repository");
			}
			
			return layout;
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Block Layout cannot be retrieved for user " + p.getName(), e);
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
			SlideLayout layout = repository.getSlideLayoutById(layoutId, user, loadAll);
			if (layout == null) {
				throw new EntityNotFoundException("Slide layout with id : " + layoutId + " does not exist in the repository");
			}
			
			return layout;
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Slide Layout cannot be retrieved for user " + p.getName(), e);
		}
	}
	
	private byte[] getCartoonForGlycan (String glycanId) {
		try {
			File imageFile = new File(imageLocation + File.separator + glycanId + ".png");
			InputStreamResource resource = new InputStreamResource(new FileInputStream(imageFile));
			return IOUtils.toByteArray(resource.getInputStream());
		} catch (IOException e) {
			logger.error("Image cannot be retrieved", e);
			throw new EntityNotFoundException("Image for glycan " + glycanId + " is not available");
		}
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
	public GlycanView getGlycan (
			@ApiParam(required=true, value="id of the glycan to retrieve") 
			@PathVariable("glycanId") String glycanId, Principal p) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
			Glycan glycan = repository.getGlycanById(glycanId, user);
			if (glycan == null) {
				throw new EntityNotFoundException("Glycan with id : " + glycanId + " does not exist in the repository");
			}
			
			return getGlycanView(glycan);
			
			
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Glycan cannot be retrieved for user " + p.getName(), e);
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
	public LinkerView getLinker (
			@ApiParam(required=true, value="id of the linker to retrieve") 
			@PathVariable("linkerId") String linkerId, Principal p) {
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
			Linker linker = repository.getLinkerById(linkerId, user);
			if (linker == null) {
				throw new EntityNotFoundException("Linker with id : " + linkerId + " does not exist in the repository");
			}
			
			return getLinkerView(linker);
			
			
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Linker cannot be retrieved for user " + p.getName(), e);
		}
		
	}
	
	@ApiOperation(value = "Add given linker for the user")
	@RequestMapping(value="/addlinker", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Linker added successfully"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register linkers"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation addLinker (
			@ApiParam(required=true, value="Linker to be added, only pubChemId is required, other fields are optional") 
			@RequestBody LinkerView linker, Principal p) {
		
		UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
		
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		
		if (linker.getPubChemId() == null) {
			errorMessage.addError(new ObjectError("pubChemId", "NoEmpty"));
		} 
	
		// validate first
		if (validator != null) {
			if (linker.getPubChemId() != null) {
				Set<ConstraintViolation<LinkerView>> violations = validator.validateValue(LinkerView.class, "pubChemId", linker.getPubChemId());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("pubChemId", "NotValid"));
				}		
			}
			
			if  (linker.getName() != null) {
				Set<ConstraintViolation<LinkerView>> violations = validator.validateValue(LinkerView.class, "name", linker.getName().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (linker.getComment() != null) {
				Set<ConstraintViolation<LinkerView>> violations = validator.validateValue(LinkerView.class, "comment", linker.getComment().trim());
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
			if (linker.getPubChemId() != null) 
				linkerURI = repository.getLinkerByPubChemId(linker.getPubChemId());
			if (linkerURI == null) {
				// get the linker details from pubChem
				try {
					l = PubChemAPI.getLinkerDetailsFromPubChem(linker.getPubChemId());
					if (l == null) {
						// could not get details from PubChem
						errorMessage.addError(new ObjectError("pubChemId", "NotValid"));
					} else {
						if (linker.getName() != null) l.setName(linker.getName().trim());
						if (linker.getComment() != null) l.setComment(linker.getComment().trim());
					}
				} catch (Exception e) {
					// could not get details from PubChem
					errorMessage.addError(new ObjectError("pubChemId", "NotValid"));
				}
				
				// check if it already exists in local repo as well (by pubChemId, by label)
				if (linker.getPubChemId() != null) {
					linkerURI = repository.getLinkerByPubChemId(linker.getPubChemId(), user);
					if (linkerURI != null) {
						errorMessage.addError(new ObjectError("pubChemId", "Duplicate"));
					}
				}
				
				if (linker.getName() != null) {
					Linker local = repository.getLinkerByLabel(linker.getName().trim(), user);
					if (local != null) {
						errorMessage.addError(new ObjectError("name", "Duplicate"));	
					}
				}
			} else {
				// check if it already exists in local repo as well (by pubChemId, by label)
				if (linker.getPubChemId() != null) {
					linkerURI = repository.getLinkerByPubChemId(linker.getPubChemId(), user);
					if (linkerURI != null) {
						errorMessage.addError(new ObjectError("pubChemId", "Duplicate"));
					}
				}
				
				if (linker.getName() != null) {
					Linker local = repository.getLinkerByLabel(linker.getName().trim(), user);
					if (local != null) {
						errorMessage.addError(new ObjectError("name", "Duplicate"));	
					}
				}
				// only add name and comment to the user's local repo
				// pubChemId is required for insertion
				l = new Linker();
				l.setPubChemId(linker.getPubChemId());
				if (linker.getName() != null) l.setName(linker.getName().trim());
				if (linker.getComment() != null) l.setComment(linker.getComment().trim());
			}
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage);
			
			if (l != null)
				repository.addLinker(l, user);
			
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Linker cannot be added for user " + p.getName(), e);
		} 
		
		return new Confirmation("Linker added successfully", HttpStatus.CREATED.value());
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
			@RequestBody LinkerView linkerView, Principal principal) throws SQLException {
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		// validate first
		if (validator != null) {
			if  (linkerView.getName() != null) {
				Set<ConstraintViolation<LinkerView>> violations = validator.validateValue(LinkerView.class, "name", linkerView.getName().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "LengthExceeded"));
				}		
			}
			if (linkerView.getComment() != null) {
				Set<ConstraintViolation<LinkerView>> violations = validator.validateValue(LinkerView.class, "comment", linkerView.getComment().trim());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("comment", "LengthExceeded"));
				}		
			}
		
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		try {
			UserEntity user = userRepository.findByUsernameIgnoreCase(principal.getName());
			Linker linker= new Linker();
			linker.setUri(GlygenArrayRepository.uriPrefix + linkerView.getId());
			linker.setComment(linkerView.getComment() != null ? linkerView.getComment().trim() : linkerView.getComment());
			linker.setName(linkerView.getName() != null ? linkerView.getName().trim() : null);		
			
			Linker local = null;
			// check if name is unique
			if (linker.getName() != null && !linker.getName().isEmpty()) {
				local = repository.getLinkerByLabel(linker.getName().trim(), user);
				if (local != null && !local.getUri().equals(linker.getUri())) {   // there is another with the same name
					errorMessage.addError(new ObjectError("name", "Duplicate"));
				}
			} 
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage);
			
			repository.updateLinker(linker, user);
			return new Confirmation("Linker updated successfully", HttpStatus.OK.value());
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Error updating linker with id: " + linkerView.getId());
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
				local = repository.getBlockLayoutByName(blockLayout.getName().trim(), user);
				if (local != null && !local.getUri().equals(blockLayout.getUri())) {   // there is another with the same name
					errorMessage.addError(new ObjectError("name", "Duplicate"));
				}
			} 
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid block layout information", errorMessage);
			
			repository.updateBlockLayout(blockLayout, user);
			return new Confirmation("Block Layout updated successfully", HttpStatus.OK.value());
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Error updating block layout with id: " + layout.getId());
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
				local = repository.getSlideLayoutByName(slideLayout.getName().trim(), user);
				if (local != null && !local.getUri().equals(slideLayout.getUri())) {   // there is another with the same name
					errorMessage.addError(new ObjectError("name", "Duplicate"));
				}
			} 
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid slide layout information", errorMessage);
			
			repository.updateSlideLayout(slideLayout, user);
			return new Confirmation("Slide Layout updated successfully", HttpStatus.OK.value());
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Error updating slide layout with id: " + layout.getId());
		}
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
	public Confirmation addBlockLayout (
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
			BlockLayout existing = repository.getBlockLayoutByName(layout.getName(), user);
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
			repository.addBlockLayout(layout, user);
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Block layout cannot be added for user " + p.getName(), e);
		}
		
		return new Confirmation("Block layout added successfully", HttpStatus.CREATED.value());
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
		        				Glycan myGlycan = new Glycan();
		        				myGlycan.setSequence(glycan.getSequence());  
		        				myGlycan.setName(glycan.getName());
		        				myGlycan.setComment(glycan.getComment());
		        				myGlycan.setGlytoucanId(glycan.getGlyTouCanId());
		        				myGlycan.setSequenceType(GlycanSequenceFormat.GLYCOCT.getLabel());
		        				myGlycan.setInternalId(glycan.getId() == null ? "" : glycan.getId().toString());
		        				myFeature.setGlycan(myGlycan);
        					}
		        			org.grits.toolbox.glycanarray.library.om.feature.Linker linker = LibraryInterface.getLinker(library, probe.getLinker());
		        			if (linker != null) {
		        				Linker myLinker = new Linker();
		        				if (linker.getPubChemId() != null) myLinker.setPubChemId(linker.getPubChemId().longValue());
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
							SlideLayout existing = repository.getSlideLayoutByName(searchName, user);
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
									BlockLayout existing = repository.getBlockLayoutByName(block.getBlockLayout().getName(), user);
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
															addLinker(getLinkerView(feature.getLinker()), p);
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
	 
	private GlycanView getGlycanView (Glycan glycan) {
		GlycanView g = new GlycanView();
		g.setName(glycan.getName());
		g.setComment(glycan.getComment());
		g.setMass(glycan.getMass());
		g.setSequence(glycan.getSequence());
		if (glycan.getSequenceType() == null || glycan.getSequenceType().equals(GlycanSequenceFormat.GLYCOCT.getLabel()))
			g.setSequenceFormat(GlycanSequenceFormat.GLYCOCT);
		else if (glycan.getSequenceType().equals(GlycanSequenceFormat.GWS.getLabel()))
			g.setSequenceFormat(GlycanSequenceFormat.GWS);
		g.setInternalId(glycan.getInternalId());
		if (glycan.getUri() != null)
			g.setId(glycan.getUri().substring(glycan.getUri().lastIndexOf("/")+1));
		g.setGlytoucanId(glycan.getGlytoucanId());
		g.setDateModified(glycan.getDateModified());
		g.setAliases(glycan.getAliases());
		try {
			byte[] image = null;
			if (g.getGlytoucanId() != null) {
				image = getCartoonForGlycan(g.getGlytoucanId());
			} else if (g.getId() != null){
				image = getCartoonForGlycan(g.getId());
			}
			g.setCartoon(image);
		} catch (Exception e) {
			logger.warn("Image cannot be retrieved", e);
		}
		return g;
	}
	
	private LinkerView getLinkerView(Linker linker) {
		LinkerView l = new LinkerView();
		l.setName(linker.getName());
		l.setComment(linker.getComment());
		l.setDateModified(linker.getDateModified());
		l.setPubChemId(linker.getPubChemId());
		if (linker.getUri() != null) 
			l.setId(linker.getUri().substring(linker.getUri().lastIndexOf("/")+1));
		l.setImageURL(linker.getImageURL());
		l.setInChiKey(linker.getInChiKey());
		l.setInChiSequence(linker.getInChiSequence());
		l.setIupacName(linker.getIupacName());
		l.setMolecularFormula(linker.getMolecularFormula());
		l.setMass(linker.getMass());
		l.setPubChemUrl(linker.getPubChemUrl());
		return l;
	}
}
