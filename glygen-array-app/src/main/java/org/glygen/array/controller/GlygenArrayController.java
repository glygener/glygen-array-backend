package org.glygen.array.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
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
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.service.GlygenArrayRepository;
import org.glygen.array.util.PubChemAPI;
import org.glygen.array.view.BatchGlycanUploadResult;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.glygen.array.view.GlycanListResultView;
import org.glygen.array.view.GlycanSequenceFormat;
import org.glygen.array.view.GlycanView;
import org.glygen.array.view.LinkerListResultView;
import org.glygen.array.view.LinkerView;
import org.grits.toolbox.glycanarray.library.om.layout.SlideLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.InputStreamResource;
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
	public Confirmation addSlideLayout (@RequestBody SlideLayout layout) {
		//TODO
		return new Confirmation("Slide Layout added successfully", HttpStatus.CREATED.value());
	}
	
	@RequestMapping(value = "/addBatchGlycan", method=RequestMethod.POST, consumes = {"multipart/form-data"}, produces={"application/json", "application/xml"})
	public BatchGlycanUploadResult addGlycanFromFile (@RequestBody MultipartFile file, Principal p) {
		BatchGlycanUploadResult result = new BatchGlycanUploadResult();
		UserEntity user = userRepository.findByUsername(p.getName());
		try {
			ByteArrayInputStream stream = new   ByteArrayInputStream(file.getBytes());
			Scanner scanner = new Scanner(stream);
			int count = 0;
			int countSuccess = 0;
			while (scanner.hasNext()) {
				String sequence = scanner.nextLine();
				count++;
				try {
					org.eurocarbdb.application.glycanbuilder.Glycan glycanObject = 
							org.eurocarbdb.application.glycanbuilder.Glycan.fromString(sequence);
					if (glycanObject == null) {
						// sequence is not valid, ignore and add to the list of failed glycans
						result.addWrongSequence(sequence);
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
							result.addDuplicateSequence(id);
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
					scanner.close();
					stream.close();
					throw new GlycanRepositoryException("Glycans cannot be added. Reason: " + e.getMessage());
				} catch (Exception e) {
					// sequence is not valid
					result.addWrongSequence(sequence);
				}
			}
			scanner.close();
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
		if (glycan.getSequence() == null || glycan.getSequence().isEmpty()) {
			ErrorMessage errorMessage = new ErrorMessage("Sequence cannot be empty");
			errorMessage.addError(new ObjectError("sequence", "Sequence cannot be empty"));
			throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
		}
		// validate first
		if (validator != null) {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
			errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
			
			if  (glycan.getName() != null) {
				Set<ConstraintViolation<GlycanView>> violations = validator.validateValue(GlycanView.class, "name", glycan.getName());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "exceeds length restrictions (max 100 characters"));
				}		
			}
			if (glycan.getComment() != null) {
				Set<ConstraintViolation<GlycanView>> violations = validator.validateValue(GlycanView.class, "comment", glycan.getComment());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("comment", "exceeeds length restrictions (max 250 characters)"));
				}		
			}
			if (glycan.getInternalId() != null) {
				Set<ConstraintViolation<GlycanView>> violations = validator.validateValue(GlycanView.class, "internalId", glycan.getInternalId());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("internalId", "exceeeds length restrictions"));
				}		
			}
			if (glycan.getGlytoucanId() != null) {
				Set<ConstraintViolation<GlycanView>> violations = validator.validateValue(GlycanView.class, "glytoucanId", glycan.getGlytoucanId());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("glytoucanId", "exceeeds length restrictions"));
				}		
			}
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
		
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		
		try {
			UserEntity user = userRepository.findByUsername(p.getName());
			Glycan g = new Glycan();
			g.setName(glycan.getName() != null ? glycan.getName().trim() : glycan.getName());
			g.setGlyTouCanId(glycan.getGlytoucanId() != null ? glycan.getGlytoucanId().trim() : glycan.getGlytoucanId());
			g.setInternalId(glycan.getInternalId() != null ? glycan.getInternalId().trim(): glycan.getInternalId());
			g.setComment(glycan.getComment() != null ? glycan.getComment().trim() : glycan.getComment());
			g.setSequence(glycan.getSequence().trim());
			g.setSequenceType(glycan.getSequenceFormat().getLabel());
			
			String existingURI = repository.getGlycanBySequence(glycan.getSequence());
			if (existingURI == null) {
				//TODO if there is a glytoucanId, check if it is valid
				try {
					if (glycan.getSequence() != null && !glycan.getSequence().isEmpty()) {
						//check if the given sequence is valid
						org.eurocarbdb.application.glycanbuilder.Glycan glycanObject= null;
						switch (glycan.getSequenceFormat()) {
							case GLYCOCT:
								glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromGlycoCTCondensed(glycan.getSequence().trim());
								break;
							case GWS:
								glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromString(glycan.getSequence().trim());
								break;
						}
						if (glycanObject != null) {
							g.setMass(glycanObject.computeMass(MassOptions.ISOTOPE_MONO));
							BufferedImage t_image = glycanWorkspace.getGlycanRenderer()
									.getImage(new Union<org.eurocarbdb.application.glycanbuilder.Glycan>(glycanObject), true, false, true, 0.5d);
							if (t_image != null) {
								String glycanURI = repository.addGlycan(g, user);
								String id = glycanURI.substring(glycanURI.lastIndexOf("/")+1);
								//save the image into a file
								logger.debug("Adding image to " + imageLocation);
								File imageFile = new File(imageLocation + File.separator + id + ".png");
								ImageIO.write(t_image, "png", imageFile);
							} else {
								logger.error("Glycan image is null");
								throw new GlycanRepositoryException("Glycan image cannot be generated");
							}
						} else {
							ErrorMessage errorMessage = new ErrorMessage("Sequence format is not valid for the given sequence");
							errorMessage.addError(new ObjectError("sequence", "Sequence cannot be parsed. Not valid"));
							throw new IllegalArgumentException("Sequence format is not valid for the given sequence", errorMessage);
						}
					} else {
						throw new GlycanRepositoryException("Cannot add a glycan without a sequence");
					}
				} catch (IOException e) {
					logger.error("Glycan image cannot be generated", e);
					throw new GlycanRepositoryException("Glycan image cannot be generated", e);
				} catch (Exception e) {
					logger.error("Glycan sequence is not valid", e);
					ErrorMessage errorMessage = new ErrorMessage(e.getMessage());
					errorMessage.addError(new ObjectError("sequence", "Sequence cannot be parsed. Not valid"));
					throw new IllegalArgumentException("Sequence format is not valid for the given sequence", errorMessage);
				}
				
			} else {
				// still add to the user's local repo
				// no need to generate the image again
				// check if it already exists in local repo as well (by sequence, by label, by internalId)
				existingURI = repository.getGlycanBySequence(glycan.getSequence().trim(), user);
				if (existingURI != null) {
					ErrorMessage errorMessage = new ErrorMessage("Cannot add duplicate glycans");
					errorMessage.addError(new ObjectError("sequence", "Duplicate"));
					throw new GlycanExistsException("A glycan with the same sequence already exists");
				}
				
				Glycan local = null;
				// check if internalid and label are unique
				if (glycan.getInternalId() != null) {
					local = repository.getGlycanByInternalId(glycan.getInternalId().trim(), user);
					if (local != null) {
						ErrorMessage errorMessage = new ErrorMessage("Cannot add duplicate glycans");
						errorMessage.addError(new ObjectError("internalId", "Duplicate"));
						throw new GlycanExistsException("A glycan with the same internal id already exists", errorMessage);
					}
				}
				if (glycan.getName() != null) {
					local = repository.getGlycanByLabel(glycan.getName().trim(), user);
					if (local != null) {
						ErrorMessage errorMessage = new ErrorMessage("Cannot add duplicate glycans");
						errorMessage.addError(new ObjectError("name", "Duplicate"));
						throw new GlycanExistsException("A glycan with the same label/name already exists", errorMessage);
					}
				} 
				repository.addGlycan(g, user);
			}
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Glycan cannot be added for user " + p.getName(), e);
		} 
		return new Confirmation("Glycan added successfully", HttpStatus.CREATED.value());
	}
	
	@RequestMapping(value="/getimage/{glycanId}", method = RequestMethod.GET, 
		produces = MediaType.IMAGE_PNG_VALUE )
	public @ResponseBody byte[] getImageForGlycan (
			@ApiParam(required=true, value="id of the glycan to retrieve the image for") 
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
	
	
	@RequestMapping(value = "/updateGlycan", method = RequestMethod.PUT)
	public void updateGlycan(@RequestBody GlycanView glycanView, Principal principal) throws SQLException {
		try {
			UserEntity user = userRepository.findByUsername(principal.getName());
			Glycan glycan= new Glycan();
			glycan.setUri(glycanView.getId());
			glycan.setInternalId(glycanView.getInternalId() != null ? glycanView.getInternalId().trim(): glycanView.getInternalId());
			glycan.setComment(glycanView.getComment() != null ? glycanView.getComment().trim() : glycanView.getComment());
			glycan.setName(glycanView.getName());			
			repository.updateGlycan(glycan, user);
			
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
		
		if (linker.getPubChemId() == null || linker.getPubChemId().isEmpty()) {
			ErrorMessage errorMessage = new ErrorMessage("PubChemId cannot be empty");
			errorMessage.addError(new ObjectError("pubChemId", "PubChemId cannot be empty"));
			throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage);
		}
		// validate first
		if (validator != null) {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
			errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
			
			if  (linker.getName() != null) {
				Set<ConstraintViolation<LinkerView>> violations = validator.validateValue(LinkerView.class, "name", linker.getName());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("name", "exceeds length restrictions (max 100 characters"));
				}		
			}
			if (linker.getComment() != null) {
				Set<ConstraintViolation<LinkerView>> violations = validator.validateValue(LinkerView.class, "name", linker.getComment());
				if (!violations.isEmpty()) {
					errorMessage.addError(new ObjectError("comment", "exceeeds length restrictions (max 250 characters)"));
				}		
			}
			
			if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
				throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
		
		} else {
			throw new RuntimeException("Validator cannot be found!");
		}
		
		UserEntity user = userRepository.findByUsername(p.getName());
		try {
			String linkerURI = repository.getLinkerByPubChemId(linker.getPubChemId().trim());
			if (linkerURI == null) {
				// get the linker details from pubChem
				Linker l = PubChemAPI.getLinkerDetailsFromPubChem(linker.getPubChemId().trim());
				if (l == null) {
					// could not get details from PubChem
					throw new EntityNotFoundException("A compound with the given pubChemId (" + linker.getPubChemId() + ") does not exist in PubChem");
					
				}
				if (linker.getName() != null) l.setName(linker.getName().trim());
				if (linker.getComment() != null) l.setComment(linker.getComment().trim());
			 
				repository.addLinker(l, user);
				
			} else {
				// still add to the user's local repo
				// check if it already exists in local repo as well (by pubChemId, by label)
				linkerURI = repository.getLinkerByPubChemId(linker.getPubChemId().trim(), user);
				if (linkerURI != null) {
					ErrorMessage errorMessage = new ErrorMessage("Cannot add duplicate linkers");
					errorMessage.addError(new ObjectError("pubChemId", "Duplicate"));
					throw new GlycanExistsException("A linker with the same pubChem Id already exists", errorMessage);
				}
				
				if (linker.getName() != null) {
					Linker local = repository.getLinkerByLabel(linker.getName().trim(), user);
					if (local != null) {
						ErrorMessage errorMessage = new ErrorMessage("Cannot add duplicate linkers");
						errorMessage.addError(new ObjectError("name", "Duplicate"));
						throw new GlycanExistsException("A linker with the same label/name already exists", errorMessage);
					}
				}
				
				// only add name and comment to the user's local repo
				// pubChemId is required for insertion
				Linker l = new Linker();
				l.setPubChemId(linker.getPubChemId().trim());
				if (linker.getName() != null) l.setName(linker.getName().trim());
				if (linker.getComment() != null) l.setComment(linker.getComment().trim());
			 
				repository.addLinker(l, user);
				
			}
			
		} catch (SparqlException | SQLException e) {
			throw new GlycanRepositoryException("Linker cannot be added for user " + p.getName(), e);
		} 
		
		return new Confirmation("Linker added successfully", HttpStatus.CREATED.value());
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
		g.setGlytoucanId(glycan.getGlyTouCanId());
		g.setDateModified(glycan.getDateModified());
		try {
			byte[] image = getCartoonForGlycan(g.getId());
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
