package org.glygen.array.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

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
import org.glygen.array.view.LinkerListResultView;
import org.glygen.array.view.LinkerView;
import org.glygen.array.view.ResumableFileInfo;
import org.glygen.array.view.ResumableInfoStorage;
import org.glygen.array.view.SlideLayoutResultView;
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

	@RequestMapping(value="/addslidelayout", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	public Confirmation addSlideLayout (@RequestBody SlideLayout layout, Principal p) {
		
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
		
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
		
		UserEntity user = userRepository.findByUsername(p.getName());
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
			throw new IllegalArgumentException("Invalid Input: Not a valid block layout information", errorMessage);
		
		try {
			repository.addSlideLayout(layout, user);
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Slide layout cannot be added for user " + p.getName(), e);
		}
		
	
		return new Confirmation("Slide Layout added successfully", HttpStatus.CREATED.value());
	}
	
	@RequestMapping(value = "/addBatchGlycan", method=RequestMethod.POST, consumes = {"multipart/form-data"}, produces={"application/json", "application/xml"})
	public BatchGlycanUploadResult addGlycanFromFile (@RequestBody MultipartFile file, Principal p) {
		BatchGlycanUploadResult result = new BatchGlycanUploadResult();
		UserEntity user = userRepository.findByUsername(p.getName());
		try {
			ByteArrayInputStream stream = new   ByteArrayInputStream(file.getBytes());
			String fileAsString = IOUtils.toString(stream, StandardCharsets.UTF_8);
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
	
	@RequestMapping(value="/addglycan", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Glycan added successfully"), 
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
			user = userRepository.findByUsername(p.getName());
			
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
	
	@RequestMapping(value="/getimage/{glycanId}", method = RequestMethod.GET, 
		produces = MediaType.IMAGE_PNG_VALUE )
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
	
	@RequestMapping(value="/delete/{glycanId}", method = RequestMethod.DELETE, 
			produces={"application/json", "application/xml"})
	public Confirmation deleteGlycan (
			@ApiParam(required=true, value="id of the glycan to delete") 
			@PathVariable("glycanId") String glycanId, Principal principal) {
		try {
			UserEntity user = userRepository.findByUsername(principal.getName());
			repository.deleteGlycan(glycanId, user);
			return new Confirmation("Glycan deleted successfully", HttpStatus.OK.value());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot delete glycan " + glycanId);
		} 
	}
	
	@RequestMapping(value="/deleteLinker/{linkerId}", method = RequestMethod.DELETE, 
			produces={"application/json", "application/xml"})
	public Confirmation deleteLinker (
			@ApiParam(required=true, value="id of the linker to delete") 
			@PathVariable("linkerId") String linkerId, Principal principal) {
		try {
			UserEntity user = userRepository.findByUsername(principal.getName());
			repository.deleteLinker(linkerId, user);
			return new Confirmation("Linker deleted successfully", HttpStatus.OK.value());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot delete linker " + linkerId);
		} 
	}
	
	@RequestMapping(value="/deleteblocklayout/{layoutId}", method = RequestMethod.DELETE, 
			produces={"application/json", "application/xml"})
	public Confirmation deleteBlockLayout (
			@ApiParam(required=true, value="id of the block layout to delete") 
			@PathVariable("layoutId") String blockLayoutId, Principal principal) {
		try {
			UserEntity user = userRepository.findByUsername(principal.getName());
			repository.deleteBlockLayout(blockLayoutId, user);
			return new Confirmation("Block Layout deleted successfully", HttpStatus.OK.value());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot delete block layout " + blockLayoutId);
		} 
	}
	
	@RequestMapping(value="/deleteslidelayout/{layoutId}", method = RequestMethod.DELETE, 
			produces={"application/json", "application/xml"})
	public Confirmation deleteSlideLayout (
			@ApiParam(required=true, value="id of the block layout to delete") 
			@PathVariable("layoutId") String layoutId, Principal principal) {
		try {
			UserEntity user = userRepository.findByUsername(principal.getName());
			repository.deleteSlideLayout(layoutId, user);
			return new Confirmation("Slide Layout deleted successfully", HttpStatus.OK.value());
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot delete slide layout " + layoutId);
		} 
	}
	
	
	@RequestMapping(value = "/updateGlycan", method = RequestMethod.PUT)
	public Confirmation updateGlycan(@RequestBody GlycanView glycanView, Principal principal) throws SQLException {
		// validate first
		if (validator != null) {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
			errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
			
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
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
		
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		try {
			UserEntity user = userRepository.findByUsername(principal.getName());
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
					ErrorMessage errorMessage = new ErrorMessage("Cannot add duplicate glycans");
					errorMessage.addError(new ObjectError("internalId", "Duplicate"));
					throw new GlycanExistsException("A glycan with the same internal id already exists", errorMessage);
				}
			}
			if (glycan.getName() != null && !glycan.getName().isEmpty()) {
				local = repository.getGlycanByLabel(glycan.getName().trim(), user);
				if (local != null && !local.getUri().equals(glycan.getUri())) {   // there is another with the same name
					ErrorMessage errorMessage = new ErrorMessage("Cannot add duplicate glycans");
					errorMessage.addError(new ObjectError("name", "Duplicate"));
					throw new GlycanExistsException("A glycan with the same label/name already exists", errorMessage);
				}
			} 
			repository.updateGlycan(glycan, user);
			return new Confirmation("Glycan updated successfully", HttpStatus.OK.value());
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Error updating glycan with internalId: " + glycanView.getInternalId());
		}
	}
	
	@RequestMapping(value="/listGlycans", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Glycans retrieved successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list glycans"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public GlycanListResultView listGlycans (
			@ApiParam(required=true, value="offset for pagination, start from 0") 
			@RequestParam("offset") Integer offset,
			@ApiParam(required=true, value="limit of the number of glycans to be retrieved") 
			@RequestParam("limit") Integer limit, 
			@ApiParam(required=false, value="name of the sort field, defaults to id") 
			@RequestParam(value="sortBy", required=false) String field, 
			@ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
			@RequestParam(value="order", required=false) Integer order, Principal p) {
		GlycanListResultView result = new GlycanListResultView();
		List<GlycanView> glycanList = new ArrayList<GlycanView>();
		UserEntity user = userRepository.findByUsername(p.getName());
		try {
			if (offset == null)
				offset = 0;
			if (limit == null)
				limit = 20;
			if (field == null)
				field = "id";
			if (order == null)
				order = 0; // DESC
			
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
	
	@RequestMapping(value="/listLinkers", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Linkers retrieved successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list linkers"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public LinkerListResultView listLinkers (
			@ApiParam(required=true, value="offset for pagination, start from 0") 
			@RequestParam("offset") Integer offset,
			@ApiParam(required=true, value="limit of the number of linkers to be retrieved") 
			@RequestParam("limit") Integer limit, 
			@ApiParam(required=false, value="name of the sort field, defaults to id") 
			@RequestParam(value="sortBy", required=false) String field, 
			@ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
			@RequestParam(value="order", required=false) Integer order, Principal p) {
		LinkerListResultView result = new LinkerListResultView();
		List<LinkerView> linkerList = new ArrayList<LinkerView>();
		UserEntity user = userRepository.findByUsername(p.getName());
		try {
			if (offset == null)
				offset = 0;
			if (limit == null)
				limit = 20;
			if (field == null)
				field = "id";
			if (order == null)
				order = 0; // DESC
			
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
	
	@RequestMapping(value="/listBlocklayouts", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Block layouts retrieved successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list block layouts"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public BlockLayoutResultView listBlockLayouts (
			@ApiParam(required=true, value="offset for pagination, start from 0") 
			@RequestParam("offset") Integer offset,
			@ApiParam(required=true, value="limit of the number of layouts to be retrieved") 
			@RequestParam("limit") Integer limit, 
			@ApiParam(required=false, value="name of the sort field, defaults to id") 
			@RequestParam(value="sortBy", required=false) String field, 
			@ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
			@RequestParam(value="order", required=false) Integer order, 
			@ApiParam (required=false, defaultValue = "true", value="if false, do not load spot details. Default is true (to load all)")
			@RequestParam(required=false, defaultValue = "true", value="loadAll") Boolean loadAll, 
			Principal p) {
		BlockLayoutResultView result = new BlockLayoutResultView();
		UserEntity user = userRepository.findByUsername(p.getName());
		try {
			if (offset == null)
				offset = 0;
			if (limit == null)
				limit = 20;
			if (field == null)
				field = "id";
			if (order == null)
				order = 0; // DESC
			
			int total = repository.getBlockLayoutCountByUser (user);
			List<BlockLayout> layouts = repository.getBlockLayoutByUser(user, offset, limit, field, loadAll, order);
			result.setRows(layouts);
			result.setTotal(total);
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot retrieve block layouts for user. Reason: " + e.getMessage());
		}
		
		return result;
	}
	
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
			@PathVariable("layoutId") String layoutId, Principal p) {
		try {
			UserEntity user = userRepository.findByUsername(p.getName());
			BlockLayout layout = repository.getBlockLayoutById(layoutId, user);
			if (layout == null) {
				throw new EntityNotFoundException("Block layout with id : " + layoutId + " does not exist in the repository");
			}
			
			return layout;
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Block Layout cannot be retrieved for user " + p.getName(), e);
		}
	}
	
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
			@PathVariable("layoutId") String layoutId, Principal p) {
		try {
			UserEntity user = userRepository.findByUsername(p.getName());
			SlideLayout layout = repository.getSlideLayoutById(layoutId, user);
			if (layout == null) {
				throw new EntityNotFoundException("Slide layout with id : " + layoutId + " does not exist in the repository");
			}
			
			return layout;
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Slide Layout cannot be retrieved for user " + p.getName(), e);
		}
	}
	
	@RequestMapping(value="/listSlidelayouts", method = RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Slide layouts retrieved successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to list slide layouts"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public SlideLayoutResultView listSlideLayouts (
			@ApiParam(required=true, value="offset for pagination, start from 0") 
			@RequestParam("offset") Integer offset,
			@ApiParam(required=true, value="limit of the number of layouts to be retrieved") 
			@RequestParam("limit") Integer limit, 
			@ApiParam(required=false, value="name of the sort field, defaults to id") 
			@RequestParam(value="sortBy", required=false) String field, 
			@ApiParam(required=false, value="sort order, Descending = 0 (default), Ascending = 1") 
			@RequestParam(value="order", required=false) Integer order, Principal p) {
		SlideLayoutResultView result = new SlideLayoutResultView();
		UserEntity user = userRepository.findByUsername(p.getName());
		try {
			if (offset == null)
				offset = 0;
			if (limit == null)
				limit = 20;
			if (field == null)
				field = "id";
			if (order == null)
				order = 0; // DESC
			
			int total = repository.getSlideLayoutCountByUser (user);
			List<SlideLayout> layouts = repository.getSlideLayoutByUser(user, offset, limit, field, order);
			result.setRows(layouts);
			result.setTotal(total);
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Cannot retrieve slide layouts for user. Reason: " + e.getMessage());
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
			throw new EntityNotFoundException("Image for glycan " + glycanId + " is not available");
		}
	}
	
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
			UserEntity user = userRepository.findByUsername(p.getName());
			Glycan glycan = repository.getGlycanById(glycanId, user);
			if (glycan == null) {
				throw new EntityNotFoundException("Glycan with id : " + glycanId + " does not exist in the repository");
			}
			
			return getGlycanView(glycan);
			
			
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Glycan cannot be retrieved for user " + p.getName(), e);
		}
		
	}
	
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
			UserEntity user = userRepository.findByUsername(p.getName());
			Linker linker = repository.getLinkerById(linkerId, user);
			if (linker == null) {
				throw new EntityNotFoundException("Linker with id : " + linkerId + " does not exist in the repository");
			}
			
			return getLinkerView(linker);
			
			
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Linker cannot be retrieved for user " + p.getName(), e);
		}
		
	}
	
	@RequestMapping(value="/addlinker", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Linker added successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register linkers"),
			@ApiResponse(code=409, message="A linker with the given pubchemId already exists!"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation addLinker (@RequestBody LinkerView linker, Principal p) {
		
		UserEntity user = userRepository.findByUsername(p.getName());
		
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
			String linkerURI = repository.getLinkerByPubChemId(linker.getPubChemId());
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
				linkerURI = repository.getLinkerByPubChemId(linker.getPubChemId(), user);
				if (linkerURI != null) {
					errorMessage.addError(new ObjectError("pubChemId", "Duplicate"));
				}
				
				if (linker.getName() != null) {
					Linker local = repository.getLinkerByLabel(linker.getName().trim(), user);
					if (local != null) {
						errorMessage.addError(new ObjectError("name", "Duplicate"));	
					}
				}
			} else {
				// check if it already exists in local repo as well (by pubChemId, by label)
				linkerURI = repository.getLinkerByPubChemId(linker.getPubChemId(), user);
				if (linkerURI != null) {
					errorMessage.addError(new ObjectError("pubChemId", "Duplicate"));
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
	
	@RequestMapping(value = "/updateLinker", method = RequestMethod.PUT)
	public Confirmation updateLinker(@RequestBody LinkerView linkerView, Principal principal) throws SQLException {
		// validate first
		if (validator != null) {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
			errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
			
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
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage);
		
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		try {
			UserEntity user = userRepository.findByUsername(principal.getName());
			Linker linker= new Linker();
			linker.setUri(GlygenArrayRepository.uriPrefix + linkerView.getId());
			linker.setComment(linkerView.getComment() != null ? linkerView.getComment().trim() : linkerView.getComment());
			linker.setName(linkerView.getName() != null ? linkerView.getName().trim() : null);		
			
			Linker local = null;
			// check if internalid and label are unique
			if (linker.getName() != null && !linker.getName().isEmpty()) {
				local = repository.getLinkerByLabel(linker.getName().trim(), user);
				if (local != null && !local.getUri().equals(linker.getUri())) {   // there is another with the same name
					ErrorMessage errorMessage = new ErrorMessage("Cannot add duplicate linkers");
					errorMessage.addError(new ObjectError("name", "Duplicate"));
					throw new GlycanExistsException("A linker with the same label/name already exists", errorMessage);
				}
			} 
			repository.updateLinker(linker, user);
			return new Confirmation("Linker updated successfully", HttpStatus.OK.value());
		} catch (SparqlException e) {
			throw new GlycanRepositoryException("Error updating linker with id: " + linkerView.getId());
		}
	}
	
	@RequestMapping(value="/addblocklayout", method = RequestMethod.POST, 
			consumes={"application/json", "application/xml"},
			produces={"application/json", "application/xml"})
	@ApiResponses (value ={@ApiResponse(code=200, message="Block layout added successfully"), 
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Not enough privileges to register block layouts"),
			@ApiResponse(code=409, message="A block layout with the given name already exists!"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public Confirmation addBlockLayout (@RequestBody BlockLayout layout, Principal p) {
		if (layout.getName() == null || layout.getName().trim().isEmpty()) {
			ErrorMessage errorMessage = new ErrorMessage("Name cannot be empty");
			errorMessage.addError(new ObjectError("name", "Name cannot be empty"));
			throw new IllegalArgumentException("Invalid Input: Not a valid block layout information", errorMessage);
		}
		
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
		errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
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
		
		UserEntity user = userRepository.findByUsername(p.getName());
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
	
	@RequestMapping(value = "/upload", method=RequestMethod.POST, 
			produces={"application/json", "application/xml"})
	public Confirmation uploadFile(
			HttpEntity<byte[]> requestEntity,
            @RequestParam("resumableFilename") String resumableFilename,
            @RequestParam ("resumableRelativePath") String resumableRelativePath,
            @RequestParam ("resumableTotalChunks") String resumableTotalChunks,
            @RequestParam("resumableChunkSize") int resumableChunkSize,
            @RequestParam("resumableChunkNumber") int resumableChunkNumber,
            @RequestParam("resumableTotalSize") long resumableTotalSize,
            @RequestParam("resumableIdentifier") String resumableIdentifier
    ) throws IOException, InterruptedException {
        String resumableFilePath = new File(uploadDir, resumableFilename).getAbsolutePath() + ".temp";
        
        ResumableFileInfo info = ResumableInfoStorage.getInstance().get(resumableIdentifier);
        if (info == null) {
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
        
        if (info.checkIfUploadFinished()) { //Check if all chunks uploaded, and change filename
            ResumableInfoStorage.getInstance().remove(info);
            return new Confirmation ("All Finished", HttpStatus.OK.value());
        } else {
        	return new Confirmation ("Upload", HttpStatus.ACCEPTED.value());
        }
	}
	
	@RequestMapping(value = "/upload", method=RequestMethod.GET, 
			produces={"application/json", "application/xml"})
	public Confirmation resumeUpload (
			@RequestParam("resumableFilename") String resumableFilename,
			@RequestParam ("resumableRelativePath") String resumableRelativePath,
            @RequestParam ("resumableTotalChunks") String resumableTotalChunks,
            @RequestParam("resumableChunkSize") int resumableChunkSize,
            @RequestParam("resumableChunkNumber") int resumableChunkNumber,
            @RequestParam("resumableTotalSize") long resumableTotalSize,
            @RequestParam("resumableIdentifier") String resumableIdentifier) {

		
        ResumableFileInfo info = ResumableInfoStorage.getInstance().get(resumableIdentifier);
        if (info == null || !info.vaild()) {
        	if (info != null) ResumableInfoStorage.getInstance().remove(info);
        	throw new IllegalArgumentException("Chunk identifier is not valid");
        }
        if (info.uploadedChunks.contains(new ResumableFileInfo.ResumableChunkNumber(resumableChunkNumber))) {
        	return new Confirmation ("Upload", HttpStatus.OK.value()); //This Chunk has been Uploaded.
        } else {
            throw new UploadNotFinishedException("Not found");  // this will return HttpStatus no_content 204
        }
    }
	 
	private GlycanView getGlycanView (Glycan glycan) {
		GlycanView g = new GlycanView();
		g.setName(glycan.getName());
		g.setComment(glycan.getComment());
		g.setMass(glycan.getMass());
		g.setSequence(glycan.getSequence());
		if (glycan.getSequenceType().equals(GlycanSequenceFormat.GLYCOCT.getLabel()))
			g.setSequenceFormat(GlycanSequenceFormat.GLYCOCT);
		else if (glycan.getSequenceType().equals(GlycanSequenceFormat.GWS.getLabel()))
			g.setSequenceFormat(GlycanSequenceFormat.GWS);
		g.setInternalId(glycan.getInternalId());
		g.setId(glycan.getUri().substring(glycan.getUri().lastIndexOf("/")+1));
		g.setGlytoucanId(glycan.getGlytoucanId());
		g.setDateModified(glycan.getDateModified());
		try {
			byte[] image = null;
			if (g.getGlytoucanId() != null) {
				image = getCartoonForGlycan(g.getGlytoucanId());
			} else {
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
		l.setId(linker.getUri().substring(linker.getUri().lastIndexOf("/")+1));
		l.setImageURL(linker.getImageURL());
		l.setInChiKey(linker.getInChiKey());
		l.setInChiSequence(linker.getInChiSequence());
		l.setIupacName(linker.getIupacName());
		l.setMolecularFormula(linker.getMolecularFormula());
		l.setMass(linker.getMass());
		return l;
	}
}
