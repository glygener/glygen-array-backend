package org.glygen.array.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.imageio.ImageIO;
import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.eurocarbdb.MolecularFramework.io.SugarImporterException;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarExporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarImporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.sugar.GlycoconjugateException;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.MolecularFramework.util.analytical.mass.GlycoVisitorMass;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorException;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.IonCloud;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.glycoinfo.GlycanFormatconverter.io.GlycoCT.WURCSToGlycoCT;
import org.glycoinfo.WURCSFramework.io.GlycoCT.WURCSExporterGlycoCT;
import org.glycoinfo.WURCSFramework.util.WURCSException;
import org.glycoinfo.WURCSFramework.util.validation.WURCSValidator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glygen.array.controller.GlygenArrayController;
import org.glygen.array.controller.UtilityController;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.FeatureType;
import org.glygen.array.persistence.rdf.GPLinkedGlycoPeptide;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.GlycanInFeature;
import org.glygen.array.persistence.rdf.GlycanSequenceFormat;
import org.glygen.array.persistence.rdf.GlycanSubsumtionType;
import org.glygen.array.persistence.rdf.GlycoLipid;
import org.glygen.array.persistence.rdf.GlycoPeptide;
import org.glygen.array.persistence.rdf.GlycoProtein;
import org.glygen.array.persistence.rdf.LinkedGlycan;
import org.glygen.array.persistence.rdf.Linker;
import org.glygen.array.persistence.rdf.LinkerType;
import org.glygen.array.persistence.rdf.Lipid;
import org.glygen.array.persistence.rdf.MassOnlyGlycan;
import org.glygen.array.persistence.rdf.OtherGlycan;
import org.glygen.array.persistence.rdf.OtherLinker;
import org.glygen.array.persistence.rdf.PeptideLinker;
import org.glygen.array.persistence.rdf.ProteinLinker;
import org.glygen.array.persistence.rdf.Publication;
import org.glygen.array.persistence.rdf.SequenceBasedLinker;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.glygen.array.persistence.rdf.SmallMoleculeLinker;
import org.glygen.array.util.FixGlycoCtUtil;
import org.glygen.array.util.GlycanBaseTypeUtil;
import org.glygen.array.util.GlytoucanUtil;
import org.glygen.array.util.SequenceUtils;
import org.glygen.array.util.UniProtUtil;
import org.glygen.array.util.pubchem.PubChemAPI;
import org.glygen.array.util.pubmed.DTOPublication;
import org.glygen.array.util.pubmed.PubmedUtil;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.grits.toolbox.glycanarray.library.om.layout.LevelUnit;
import org.grits.toolbox.glycanarray.om.parser.cfg.CFGMasterListParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.ObjectError;

@Service
public class AddToRepositoryServiceImpl implements AddToRepositoryService {
    
final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    @Value("${spring.file.uploaddirectory}")
    String uploadDir;
    
    @Autowired
    ResourceLoader resourceLoader;
    
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

    @Override
    public String addLinker(Linker linker, Boolean unknown, UserEntity user) {
        switch (linker.getType()) {
        case SMALLMOLECULE:
        case LIPID:
            return addSmallMoleculeLinker((SmallMoleculeLinker)linker, unknown, user);
        case PEPTIDE:
            return addPeptideLinker ((PeptideLinker) linker, unknown, user);
        case PROTEIN:
            return addProteinLinker((ProteinLinker) linker, unknown, user);
        case OTHER:
        case UNKNOWN_OTHER:
            return addOtherLinker ((OtherLinker) linker, user);
        case UNKNOWN_PEPTIDE:
            return addPeptideLinker ((PeptideLinker) linker, true, user);
        case UNKNOWN_PROTEIN:
            return addProteinLinker((ProteinLinker) linker, true, user);
        case UNKNOWN_SMALLMOLECULE:
        case UNKNOWN_LIPID:
            return addSmallMoleculeLinker((SmallMoleculeLinker)linker, true, user);
        }
        throw new GlycanRepositoryException("Incorrect linker type");
    }
    
    private String addOtherLinker(OtherLinker linker, UserEntity user) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
       
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
        
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
        try {
            if (linker.getName() != null && !linker.getName().trim().isEmpty()) {
                Linker local = linkerRepository.getLinkerByLabel(linker.getName().trim(), linker.getType(), user);
                if (local != null && local.getType() == linker.getType()) {
                    linker.setId(local.getId());
                    String[] codes = {local.getId()};
                    errorMessage.addError(new ObjectError("name", codes, null, "Duplicate"));    
                }
            }
            
            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
                throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage);
            
            String addedURI = linkerRepository.addLinker(linker, user);
            return addedURI.substring(addedURI.lastIndexOf("/")+1);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Linker cannot be added for user " + user.getUsername(), e);
        } 
    }

    

    private String addPeptideLinker(PeptideLinker linker, boolean unknown, UserEntity user) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        if (linker.getSequence() == null)  {
            if (!unknown)
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
            
        
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
        try {
            Linker l = null;
            String linkerURI = null;
            if (linker.getSequence() != null && !linker.getSequence().trim().isEmpty()) {
                // modify the sequence to add position markers
                try {
                    linker.setSequence(addPositionToSequence(linker.getSequence().trim()));
                } catch (Exception e) {
                    errorMessage.addError(new ObjectError("sequence", "NotValid"));
                }
                String existing = linkerRepository.getLinkerByField(linker.getSequence().trim(), "has_sequence", "string", linker.getType(), user);
                if (existing != null && !existing.contains("public")) {
                    linker.setUri(existing);
                    linkerURI = existing;
                    String[] codes = {existing.substring(existing.lastIndexOf("/")+1)};
                    errorMessage.addError(new ObjectError("sequence", codes, null, "Duplicate"));
                }
            }
            
            
            l = linker;
            l.setUri(linkerURI);
            
            
            LinkerType otherType = null;
            if (linker.getType().name().startsWith("UNKNOWN")) {
                // add the regular type to the query
                otherType = LinkerType.valueOf(linker.getType().name().substring(linker.getType().name().lastIndexOf("UNKNOWN_")+8));
            } else if (!linker.getType().name().startsWith("UNKNOWN")) {
                otherType = LinkerType.valueOf("UNKNOWN_" + linker.getType().name());
               
            }
            
            if (linker.getName() != null && !linker.getName().trim().isEmpty()) {
                Linker local = linkerRepository.getLinkerByLabel(linker.getName().trim(), linker.getType(), user);
                if (local != null && (local.getType() == linker.getType() || local.getType() == otherType)) {
                    linker.setId(local.getId());
                    String[] codes = {local.getId()};
                    errorMessage.addError(new ObjectError("name", codes, null, "Duplicate"));    
                }
            }
            
            if (unknown) {
                if (!linker.getType().name().startsWith("UNKNOWN")) { 
                    LinkerType unknownType = LinkerType.valueOf("UNKNOWN_" + linker.getType().name());
                    l.setType(unknownType);
                } // else - already unknown
            }
            
            
            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
                throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage);
            
            
            String addedURI = linkerRepository.addLinker(l, user);
            return addedURI.substring(addedURI.lastIndexOf("/")+1);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Linker cannot be added for user " + user.getUsername(), e);
        } 
    }

    private String addProteinLinker(ProteinLinker linker, boolean unknown, UserEntity user) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        
        if (linker.getUniProtId() == null && linker.getSequence() == null)  { // at least one of them should be provided
            if (!unknown)
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
            
        
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
        try {
            Linker l = null;
            String linkerURI = null;
            
            if (!unknown) {
                if (linker.getSequence() != null && !linker.getSequence().trim().isEmpty()) {
                    // modify the sequence to add position markers
                    try {
                        String sequence = addPositionToSequence(linker.getSequence().trim());
                        linker.setSequence(sequence);
                    } catch (Exception e) {
                        errorMessage.addError(new ObjectError("sequence", "NotValid"));
                    }
                    String existing = linkerRepository.getLinkerByField(linker.getSequence().trim(), "has_sequence", "string", linker.getType(), user);
                    if (existing != null && !existing.contains("public")) {
                        linker.setUri(existing);
                        linkerURI = existing;
                        String[] codes = {existing.substring(existing.lastIndexOf("/")+1)};
                        errorMessage.addError(new ObjectError("sequence", codes, null, "Duplicate"));
                    }
                }
                else if (linker.getUniProtId() != null && !linker.getUniProtId().trim().isEmpty()) {
                    String existing = linkerRepository.getLinkerByField(linker.getUniProtId().trim(), "has_uniProtId", "string", linker.getType(), user);
                    if (existing != null && !existing.contains("public")) {
                        linker.setUri(existing);
                        linkerURI = existing;
                        errorMessage.addError(new ObjectError("uniProtId", "Duplicate"));
                    }
                }
            }
            
            l = linker;
            l.setUri(linkerURI);
            
            LinkerType otherType = null;
            if (linker.getType().name().startsWith("UNKNOWN")) {
                // add the regular type to the query
                otherType = LinkerType.valueOf(linker.getType().name().substring(linker.getType().name().lastIndexOf("UNKNOWN_")+8));
            } else if (!linker.getType().name().startsWith("UNKNOWN")) {
                otherType = LinkerType.valueOf("UNKNOWN_" + linker.getType().name());
               
            }
            
            if (linker.getName() != null && !linker.getName().trim().isEmpty()) {
                Linker local = linkerRepository.getLinkerByLabel(linker.getName().trim(), linker.getType(), user);
                if (local != null && (local.getType() == linker.getType() || local.getType() == otherType)) {
                    linker.setId(local.getId());
                    String[] codes = {local.getId()};
                    errorMessage.addError(new ObjectError("name", codes, null, "Duplicate"));    
                }
            }
            
            if (unknown) {
                if (!linker.getType().name().startsWith("UNKNOWN")) { 
                    LinkerType unknownType = LinkerType.valueOf("UNKNOWN_" + linker.getType().name());
                    l.setType(unknownType);
                } // else - already unknown
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
            throw new GlycanRepositoryException("Linker cannot be added for user " + user.getUsername(), e);
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
                stack.push(Character.valueOf('{'));
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

    @Override
    public String addGlycan(Glycan glycan, UserEntity user, Boolean noGlytoucanRegistration, Boolean bypassGlytoucanCheck) {
        if (noGlytoucanRegistration == null)
            noGlytoucanRegistration = false;
        switch (glycan.getType()) {
        case SEQUENCE_DEFINED: 
            return addSequenceDefinedGlycan((SequenceDefinedGlycan)glycan, user, noGlytoucanRegistration, bypassGlytoucanCheck);
        case MASS_ONLY:
            return addMassOnlyGlycan ((MassOnlyGlycan) glycan, user);
        case OTHER:
            return addOtherGlycan ((OtherGlycan) glycan, user);
        case UNKNOWN:
        default:
            return addGenericGlycan(glycan, user);
        }
    }
    
    private String addMassOnlyGlycan(MassOnlyGlycan glycan, UserEntity user) {
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
        
        try {
            // TODO if name is null, check by mass to make sure there are no other glycans with the same mass and no name
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
            throw new GlycanRepositoryException("Glycan cannot be added for user " + user.getUsername(), e);
        }
                
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
                throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
        try {   
            // no errors add the glycan
            String glycanURI = glycanRepository.addGlycan(glycan, user);
            return glycanURI.substring(glycanURI.lastIndexOf("/")+1);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Glycan cannot be added for user " + user.getUsername(), e);
        }
    }
    
    private String addGenericGlycan(Glycan glycan, UserEntity user) {
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
       
        try {   
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
            throw new GlycanRepositoryException("Glycan cannot be added for user " + user.getUsername(), e);
        }
                
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
                throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
        try {   
            // no errors add the glycan
            String glycanURI = glycanRepository.addGlycan(glycan, user);
            return glycanURI.substring(glycanURI.lastIndexOf("/")+1);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Glycan cannot be added for user " + user.getUsername(), e);
        }
    }
    
    /**
     * this adds the feature exported from the repository
     * 
     * @param feature
     * @param positions
     * @param user
     * @return
     */
    @Override
    public String importFeature (Feature feature, Map<Object, String> positions, UserEntity user) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        if (feature.getType() == null || 
                feature.getType() == FeatureType.LINKEDGLYCAN || 
                feature.getType() == FeatureType.GLYCOLIPID ||
                feature.getType() == FeatureType.GLYCOPEPTIDE || feature.getType() == FeatureType.GLYCOPROTEIN ||
                feature.getType() == FeatureType.GPLINKEDGLYCOPEPTIDE) {
            if (feature.getType() == FeatureType.LINKEDGLYCAN && feature.getLinker() == null)
                errorMessage.addError(new ObjectError("linker", "NoEmpty"));
            if (feature.getType() == FeatureType.LINKEDGLYCAN && ((LinkedGlycan) feature).getGlycans() == null) {
                errorMessage.addError(new ObjectError("glycan", "NoEmpty"));
            } else if (feature.getType() == FeatureType.GLYCOLIPID && ((GlycoLipid) feature).getGlycans() == null) {
                errorMessage.addError(new ObjectError("glycan", "NoEmpty"));
            } else if (feature.getType() == FeatureType.GLYCOLIPID && ((GlycoLipid) feature).getLipid() == null) {
                errorMessage.addError(new ObjectError("lipid", "NoEmpty"));
            } else if (feature.getType() == FeatureType.GLYCOPEPTIDE && ((GlycoPeptide) feature).getGlycans() == null) {
                errorMessage.addError(new ObjectError("glycan", "NoEmpty"));
            } else if (feature.getType() == FeatureType.GLYCOPEPTIDE && ((GlycoPeptide) feature).getPeptide() == null) {
                errorMessage.addError(new ObjectError("peptide", "NoEmpty"));
            } else if (feature.getType() == FeatureType.GLYCOPROTEIN && ((GlycoProtein) feature).getGlycans() == null) {
                errorMessage.addError(new ObjectError("glycan", "NoEmpty"));
            } else if (feature.getType() == FeatureType.GLYCOPROTEIN && ((GlycoProtein) feature).getProtein() == null) {
                errorMessage.addError(new ObjectError("protein", "NoEmpty"));
            } else if (feature.getType() == FeatureType.GPLINKEDGLYCOPEPTIDE && ((GPLinkedGlycoPeptide) feature).getPeptides() == null) {
                errorMessage.addError(new ObjectError("glycan", "NoEmpty"));
            } else if (feature.getType() == FeatureType.GPLINKEDGLYCOPEPTIDE && ((GPLinkedGlycoPeptide) feature).getProtein() == null) {
                errorMessage.addError(new ObjectError("protein", "NoEmpty"));
            }
        } else {
            // other types, i.e. controls
            if (feature.getLinker() == null && !feature.getType().equals(FeatureType.NEGATIVE_CONTROL)) {
                errorMessage.addError(new ObjectError("linker", "NoEmpty"));
            }
        }
        
        // if it was allowed to be saved in the repository without metadata, we can keep the same behavior while importing
       /* if (feature.getMetadata() == null) {
            errorMessage.addError(new ObjectError("metadata", "NoEmpty"));
        }*/
        
        if (feature.getName() != null && !feature.getName().trim().isEmpty()) {
            try {
                org.glygen.array.persistence.rdf.Feature existing = featureRepository.getFeatureByLabel(feature.getName(), user);
                if (existing != null) {
                    feature.setId(existing.getId());
                    String[] codes = {existing.getId()};
                    errorMessage.addError(new ObjectError("name", codes, null, "Duplicate"));
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
                    String[] codes = {existing.getId()};
                    errorMessage.addError(new ObjectError("internalId", codes, null, "Duplicate"));
                }
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Could not query existing features", e);
            }
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid feature information", errorMessage);
        
        try {
            switch (feature.getType()) {
            case LINKEDGLYCAN:
                return importLinkedGlycan((LinkedGlycan)feature, errorMessage, user);
            case GLYCOLIPID:
                return importGlycoLipid((GlycoLipid)feature, errorMessage, user);
            case GLYCOPEPTIDE:
                return importGlycoPeptide((GlycoPeptide)feature, errorMessage, positions, user);
            case GLYCOPROTEIN:
                return importGlycoProtein((GlycoProtein)feature, errorMessage, positions, user);
            case GPLINKEDGLYCOPEPTIDE:
                return importGPLinkedGlycoPeptide((GPLinkedGlycoPeptide)feature, errorMessage, positions, user);
            case LANDING_LIGHT:
            case NEGATIVE_CONTROL:
            case COMPOUND:
            case CONTROL:
            default:
                String featureURI = featureRepository.addFeature(feature, user);
                String id = featureURI.substring(featureURI.lastIndexOf("/")+1);
                return id;
            
            }  
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Feature cannot be added for user " + user.getUsername(), e);
        } 
    }
    
    private String importLinkedGlycan(LinkedGlycan feature, ErrorMessage errorMessage, UserEntity user) throws SparqlException, SQLException {
        if (feature.getLinker() != null) {
            if (feature.getLinker() instanceof Linker) {
                try {
                    String id = addLinker((Linker)feature.getLinker(), 
                        ((Linker)feature.getLinker()).getType().name().startsWith("UNKNOWN"), user);
                    ((Linker)feature.getLinker()).setId(id);
                } catch (Exception e) {
                    // we can ignore if it is duplicate
                    if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                        ErrorMessage error = (ErrorMessage) e.getCause();
                        for (ObjectError err: error.getErrors()) {
                            if (!err.getDefaultMessage().contains("Duplicate")) {
                                errorMessage.addError(err);
                            } else {
                                // need to get the duplicate linker
                                if (err.getCodes() != null && err.getCodes().length > 0)
                                    ((Linker)feature.getLinker()).setId(err.getCodes()[0]);
                                else {
                                    errorMessage.addError(new ObjectError("linker", err.getCodes(), null, "NotFound"));
                                }
                            }
                        }
                    } else {
                        errorMessage.addError(new ObjectError("linker", e.getMessage()));
                    }
                    logger.debug("linker not added " + feature.getLinker().getId() == null ? feature.getLinker().getUri() : feature.getLinker().getId());
                }
            } 
        }
        
        // check its glycans
        if (feature.getGlycans() != null) {
            for (GlycanInFeature gf: feature.getGlycans()) {
                Glycan g = gf.getGlycan();
                Glycan baseGlycan = gf.getBaseGlycan();
                try {
                    if (baseGlycan != null && baseGlycan instanceof SequenceDefinedGlycan) {
                        baseGlycan.setId (addGlycan(baseGlycan, user, true, false));
                        baseGlycan.setUri (GlygenArrayRepositoryImpl.uriPrefix + baseGlycan.getId());
                        // based on the reducing end configuration 
                        if (gf.getReducingEndConfiguration() != null) {
                            switch (gf.getReducingEndConfiguration().getType()) {
                            case ALPHA:
                                // get alpha version of the glycan
                                Glycan alpha = glycanRepository.retrieveOtherSubType(baseGlycan, GlycanSubsumtionType.ALPHA, user);
                                if (alpha != null) {
                                    gf.setGlycan(alpha);
                                }
                                break;
                            case BETA:
                                // get beta version of the glycan
                                Glycan beta = glycanRepository.retrieveOtherSubType(baseGlycan, GlycanSubsumtionType.BETA, user);
                                if (beta != null) {
                                    gf.setGlycan(beta);
                                }
                                break;
                            case OPENSRING:
                                // get alditol version of the glycan
                                Glycan open = glycanRepository.retrieveOtherSubType(baseGlycan, GlycanSubsumtionType.ALDITOL, user);
                                if (open != null) {
                                    gf.setGlycan(open);
                                }
                                break;
                            case EQUILIBRIUM:
                            case UNKNOWN:
                            default:
                                if (((SequenceDefinedGlycan) baseGlycan).getSubType() != GlycanSubsumtionType.BASE) {
                                    // error
                                    errorMessage = new ErrorMessage();
                                    errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
                                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                                    errorMessage.addError(new ObjectError("glycan", "NotBaseType"));
                                    throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
                                }
                                gf.setGlycan(baseGlycan);
                                break;
                            }
                        }
                        
                    } else {
                        g.setId(addGlycan(g, user, true, false));
                    }
                } catch (Exception e) {
                    // need to ignore duplicate check errors
                    if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                        ErrorMessage error = (ErrorMessage) e.getCause();
                        for (ObjectError err: error.getErrors()) {
                            if (!err.getDefaultMessage().contains("Duplicate")) {
                                errorMessage.addError(err);
                            } else {
                                // need to get the duplicate glycan
                                if (err.getCodes() != null && err.getCodes().length > 0)
                                    g.setId(err.getCodes()[0]);
                                else {
                                    errorMessage.addError(new ObjectError("glycan", err.getCodes(), null, "NotFound"));
                                }
                            }
                        }
                    } else {
                        errorMessage.addError(new ObjectError("glycan", e.getMessage()));
                    }
                }
            } 
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid feature information", errorMessage);
        
        String featureURI = featureRepository.addFeature(feature, user);
        String id = featureURI.substring(featureURI.lastIndexOf("/")+1);
        return id;   
    }
    
    private String importGlycoLipid(GlycoLipid feature, ErrorMessage errorMessage, UserEntity user) throws SparqlException, SQLException {
        if (feature.getLinker() != null) {
            if (feature.getLinker() instanceof Linker) {
                try {
                    String id = addLinker((Linker)feature.getLinker(), 
                        ((Linker)feature.getLinker()).getType().name().startsWith("UNKNOWN"), user);
                    ((Linker)feature.getLinker()).setId(id);
                } catch (Exception e) {
                    // we can ignore if it is duplicate
                    if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                        ErrorMessage error = (ErrorMessage) e.getCause();
                        for (ObjectError err: error.getErrors()) {
                            if (!err.getDefaultMessage().contains("Duplicate")) {
                                errorMessage.addError(err);
                            } else {
                                // need to get the duplicate linker
                                if (err.getCodes() != null && err.getCodes().length > 0)
                                    ((Linker)feature.getLinker()).setId(err.getCodes()[0]);
                                else {
                                    errorMessage.addError(new ObjectError("linker", err.getCodes(), null, "NotFound"));
                                }
                            }
                        }
                    } else {
                        errorMessage.addError(new ObjectError("linker", e.getMessage()));
                    }
                    logger.debug("linker not added " + feature.getLinker().getId() == null ? feature.getLinker().getUri() : feature.getLinker().getId());
                }
            } 
        }
        // check its glycans
        if (feature.getGlycans() != null) {
            for (LinkedGlycan g: feature.getGlycans()) {
                g.setId(importLinkedGlycan(g, errorMessage, user));
                g.setUri(GlygenArrayRepositoryImpl.uriPrefix + g.getId());
            }   
        }
        
        if (feature.getLipid() != null) {
            try {
                String id = addSmallMoleculeLinker(feature.getLipid(), feature.getLinker().getType().name().startsWith("UNKNOWN"), user);
                feature.getLipid().setId(id);
            } catch (Exception e) {
                // we can ignore if it is duplicate
                if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                    ErrorMessage error = (ErrorMessage) e.getCause();
                    for (ObjectError err: error.getErrors()) {
                        if (!err.getDefaultMessage().contains("Duplicate")) {
                            errorMessage.addError(err);
                        } else {
                            // need to get the duplicate linker
                            if (err.getCodes() != null && err.getCodes().length > 0)
                                feature.getLipid().setId(err.getCodes()[0]);
                            else {
                                errorMessage.addError(new ObjectError("lipid", err.getCodes(), null, "NotFound"));
                            }
                        }
                    }
                } else {
                    errorMessage.addError(new ObjectError("lipid", e.getMessage()));
                }
                logger.debug("peptide not added " + feature.getLipid().getId() == null ? feature.getLipid().getUri() : feature.getLipid().getId());
            }
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid feature information", errorMessage);
        
        
        String featureURI = featureRepository.addFeature(feature, user);
        String id = featureURI.substring(featureURI.lastIndexOf("/")+1);
        return id;
    }

    private String importGPLinkedGlycoPeptide(GPLinkedGlycoPeptide feature, ErrorMessage errorMessage,
            Map<Object, String> positions, UserEntity user) throws SparqlException, SQLException {
        if (feature.getLinker() != null) {
            if (feature.getLinker() instanceof Linker) {
                try {
                    String id = addLinker((Linker)feature.getLinker(), 
                        ((Linker)feature.getLinker()).getType().name().startsWith("UNKNOWN"), user);
                    ((Linker)feature.getLinker()).setId(id);
                } catch (Exception e) {
                    // we can ignore if it is duplicate
                    if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                        ErrorMessage error = (ErrorMessage) e.getCause();
                        for (ObjectError err: error.getErrors()) {
                            if (!err.getDefaultMessage().contains("Duplicate")) {
                                errorMessage.addError(err);
                            } else {
                                // need to get the duplicate linker
                                if (err.getCodes() != null && err.getCodes().length > 0)
                                    ((Linker)feature.getLinker()).setId(err.getCodes()[0]);
                                else {
                                    errorMessage.addError(new ObjectError("linker", err.getCodes(), null, "NotFound"));
                                }
                            }
                        }
                    } else {
                        errorMessage.addError(new ObjectError("linker", e.getMessage()));
                    }
                    logger.debug("linker not added " + feature.getLinker().getId() == null ? feature.getLinker().getUri() : feature.getLinker().getId());
                }
            } 
        }
        // check its glycans
        if (feature.getPeptides() != null) {
            for (GlycoPeptide g: feature.getPeptides()) {
                Map<Object, String> newPositions = AddToRepositoryServiceImpl.cleanFeature (g);
                g.setId(importGlycoPeptide(g, errorMessage, newPositions, user));
                g.setUri(GlygenArrayRepositoryImpl.uriPrefix + g.getId());   
            }
            
            // update the position map to include references to the newly created linked glycan
            if (feature.getPositionMap() != null && !feature.getPositionMap().isEmpty()) {
                for (Object o: positions.keySet()) {
                    String uri = null;
                    if (o instanceof Feature) {
                        uri = ((Feature) o).getUri();
                    } else if (o instanceof Glycan) {
                        uri = ((Glycan) o).getUri();
                    }
                    feature.getPositionMap().put(positions.get(o), uri);
                }
            }
        }
        
        if (feature.getProtein() != null) {
            try {
                String id = addProteinLinker(feature.getProtein(), 
                        feature.getProtein().getType().name().startsWith("UNKNOWN"), user);
                feature.getProtein().setId(id);
            } catch (Exception e) {
                // we can ignore if it is duplicate
                if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                    ErrorMessage error = (ErrorMessage) e.getCause();
                    for (ObjectError err: error.getErrors()) {
                        if (!err.getDefaultMessage().contains("Duplicate")) {
                            errorMessage.addError(err);
                        } else {
                            // need to get the duplicate linker
                            if (err.getCodes() != null && err.getCodes().length > 0)
                                feature.getProtein().setId(err.getCodes()[0]);
                            else {
                                errorMessage.addError(new ObjectError("protein", err.getCodes(), null, "NotFound"));
                            }
                        }
                    }
                } else {
                    errorMessage.addError(new ObjectError("protein", e.getMessage()));
                }
                logger.debug("peptide not added " + feature.getProtein().getId() == null ? feature.getProtein().getUri() : feature.getProtein().getId());
            }
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid feature information", errorMessage);
        
        
        String featureURI = featureRepository.addFeature(feature, user);
        String id = featureURI.substring(featureURI.lastIndexOf("/")+1);
        return id;
    }

    private String importGlycoProtein(GlycoProtein feature, ErrorMessage errorMessage, Map<Object, String> positions,
            UserEntity user) throws SparqlException, SQLException {
        if (feature.getLinker() != null) {
            if (feature.getLinker() instanceof Linker) {
                try {
                    String id = addLinker((Linker)feature.getLinker(), 
                        ((Linker)feature.getLinker()).getType().name().startsWith("UNKNOWN"), user);
                    ((Linker)feature.getLinker()).setId(id);
                } catch (Exception e) {
                    // we can ignore if it is duplicate
                    if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                        ErrorMessage error = (ErrorMessage) e.getCause();
                        for (ObjectError err: error.getErrors()) {
                            if (!err.getDefaultMessage().contains("Duplicate")) {
                                errorMessage.addError(err);
                            } else {
                                // need to get the duplicate linker
                                if (err.getCodes() != null && err.getCodes().length > 0)
                                    ((Linker)feature.getLinker()).setId(err.getCodes()[0]);
                                else {
                                    errorMessage.addError(new ObjectError("linker", err.getCodes(), null, "NotFound"));
                                }
                            }
                        }
                    } else {
                        errorMessage.addError(new ObjectError("linker", e.getMessage()));
                    }
                    logger.debug("linker not added " + feature.getLinker().getId() == null ? feature.getLinker().getUri() : feature.getLinker().getId());
                }
            } 
        }
        // check its glycans
        if (feature.getGlycans() != null) {
            for (LinkedGlycan g: feature.getGlycans()) { 
                g.setId(importLinkedGlycan(g, errorMessage, user));
                g.setUri(GlygenArrayRepositoryImpl.uriPrefix + g.getId());
            }
            
            // update the position map to include references to the newly created linked glycan
         // update the position map to include references to the newly created linked glycan
            if (feature.getPositionMap() != null && !feature.getPositionMap().isEmpty()) {
                for (Object o: positions.keySet()) {
                    String uri = null;
                    if (o instanceof Feature) {
                        uri = ((Feature) o).getUri();
                    } else if (o instanceof Glycan) {
                        uri = ((Glycan) o).getUri();
                    }
                    feature.getPositionMap().put(positions.get(o), uri);
                }
            }
        }
        
        if (feature.getProtein() != null) {
            try {
                String id = addProteinLinker(feature.getProtein(), 
                        feature.getProtein().getType().name().startsWith("UNKNOWN"), user);
                feature.getProtein().setId(id);
            } catch (Exception e) {
                // we can ignore if it is duplicate
                if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                    ErrorMessage error = (ErrorMessage) e.getCause();
                    for (ObjectError err: error.getErrors()) {
                        if (!err.getDefaultMessage().contains("Duplicate")) {
                            errorMessage.addError(err);
                        } else {
                            // need to get the duplicate linker
                            if (err.getCodes() != null && err.getCodes().length > 0)
                                feature.getProtein().setId(err.getCodes()[0]);
                            else {
                                errorMessage.addError(new ObjectError("protein", err.getCodes(), null, "NotFound"));
                            }
                        }
                    }
                } else {
                    errorMessage.addError(new ObjectError("protein", e.getMessage()));
                }
                logger.debug("peptide not added " + feature.getProtein().getId() == null ? feature.getProtein().getUri() : feature.getProtein().getId());
            }
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid feature information", errorMessage);

        String featureURI = featureRepository.addFeature(feature, user);
        String id = featureURI.substring(featureURI.lastIndexOf("/")+1);
        return id;
    }

    private String importGlycoPeptide(GlycoPeptide feature, ErrorMessage errorMessage, Map<Object, String> positions,
            UserEntity user) throws SparqlException, SQLException {
        if (feature.getLinker() != null) {
            if (feature.getLinker() instanceof Linker) {
                try {
                    String id = addLinker((Linker)feature.getLinker(), 
                        ((Linker)feature.getLinker()).getType().name().startsWith("UNKNOWN"), user);
                    ((Linker)feature.getLinker()).setId(id);
                } catch (Exception e) {
                    // we can ignore if it is duplicate
                    if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                        ErrorMessage error = (ErrorMessage) e.getCause();
                        for (ObjectError err: error.getErrors()) {
                            if (!err.getDefaultMessage().contains("Duplicate")) {
                                errorMessage.addError(err);
                            } else {
                                // need to get the duplicate linker
                                if (err.getCodes() != null && err.getCodes().length > 0)
                                    ((Linker)feature.getLinker()).setId(err.getCodes()[0]);
                                else {
                                    errorMessage.addError(new ObjectError("linker", err.getCodes(), null, "NotFound"));
                                }
                            }
                        }
                    } else {
                        errorMessage.addError(new ObjectError("linker", e.getMessage()));
                    }
                    logger.debug("linker not added " + feature.getLinker().getId() == null ? feature.getLinker().getUri() : feature.getLinker().getId());
                }
            } 
        }
        // check its glycans
        if (feature.getGlycans() != null) {    
            for (LinkedGlycan g: feature.getGlycans()) {
                g.setId(importLinkedGlycan(g, errorMessage, user));
                g.setUri(GlygenArrayRepositoryImpl.uriPrefix + g.getId());
            }
            
            // update the position map to include references to the newly created linked glycan
            if (feature.getPositionMap() != null && !feature.getPositionMap().isEmpty()) {
                for (Object o: positions.keySet()) {
                    String uri = null;
                    if (o instanceof Feature) {
                        uri = ((Feature) o).getUri();
                    } else if (o instanceof Glycan) {
                        uri = ((Glycan) o).getUri();
                    }
                    feature.getPositionMap().put(positions.get(o), uri);
                }
            }
        }
        
        if (feature.getPeptide() != null) {
            try {
                String id = addPeptideLinker(feature.getPeptide(), 
                        feature.getPeptide().getType().name().startsWith("UNKNOWN"), user);
                feature.getPeptide().setId(id);
            } catch (Exception e) {
                // we can ignore if it is duplicate
                if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                    ErrorMessage error = (ErrorMessage) e.getCause();
                    for (ObjectError err: error.getErrors()) {
                        if (!err.getDefaultMessage().contains("Duplicate")) {
                            errorMessage.addError(err);
                        } else {
                            // need to get the duplicate linker
                            if (err.getCodes() != null && err.getCodes().length > 0)
                                feature.getPeptide().setId(err.getCodes()[0]);
                            else {
                                errorMessage.addError(new ObjectError("peptide", err.getCodes(), null, "NotFound"));
                            }
                        }
                    }
                } else {
                    errorMessage.addError(new ObjectError("peptide", e.getMessage()));
                }
                logger.debug("peptide not added " + feature.getPeptide().getId() == null ? feature.getPeptide().getUri() : feature.getPeptide().getId());
            }
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid feature information", errorMessage);
        
        String featureURI = featureRepository.addFeature(feature, user);
        String id = featureURI.substring(featureURI.lastIndexOf("/")+1);
        return id;
    }

    @Override
    public String addFeature(Feature feature, UserEntity user) {
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
            if (feature.getDescription() != null) {
                Set<ConstraintViolation<Feature>> violations = validator.validateValue(Feature.class, "description", feature.getDescription().trim());
                if (!violations.isEmpty()) {
                    errorMessage.addError(new ObjectError("description", "LengthExceeded"));
                }       
            }
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
        if (feature.getType() == null || 
                feature.getType() == FeatureType.LINKEDGLYCAN || 
                feature.getType() == FeatureType.GLYCOLIPID ||
                feature.getType() == FeatureType.GLYCOPEPTIDE || feature.getType() == FeatureType.GLYCOPROTEIN ||
                feature.getType() == FeatureType.GPLINKEDGLYCOPEPTIDE) {
            if (feature.getType() == FeatureType.LINKEDGLYCAN && feature.getLinker() == null)
                errorMessage.addError(new ObjectError("linker", "NoEmpty"));
            if (feature.getType() == FeatureType.LINKEDGLYCAN && ((LinkedGlycan) feature).getGlycans() == null) {
                errorMessage.addError(new ObjectError("glycan", "NoEmpty"));
            } else if (feature.getType() == FeatureType.GLYCOLIPID && ((GlycoLipid) feature).getGlycans() == null) {
                errorMessage.addError(new ObjectError("glycan", "NoEmpty"));
            } else if (feature.getType() == FeatureType.GLYCOLIPID && ((GlycoLipid) feature).getLipid() == null) {
                errorMessage.addError(new ObjectError("lipid", "NoEmpty"));
            } else if (feature.getType() == FeatureType.GLYCOPEPTIDE && ((GlycoPeptide) feature).getGlycans() == null) {
                errorMessage.addError(new ObjectError("glycan", "NoEmpty"));
            } else if (feature.getType() == FeatureType.GLYCOPEPTIDE && ((GlycoPeptide) feature).getPeptide() == null) {
                errorMessage.addError(new ObjectError("peptide", "NoEmpty"));
            } else if (feature.getType() == FeatureType.GLYCOPROTEIN && ((GlycoProtein) feature).getGlycans() == null) {
                errorMessage.addError(new ObjectError("glycan", "NoEmpty"));
            } else if (feature.getType() == FeatureType.GLYCOPROTEIN && ((GlycoProtein) feature).getProtein() == null) {
                errorMessage.addError(new ObjectError("protein", "NoEmpty"));
            } else if (feature.getType() == FeatureType.GPLINKEDGLYCOPEPTIDE && ((GPLinkedGlycoPeptide) feature).getPeptides() == null) {
                errorMessage.addError(new ObjectError("glycan", "NoEmpty"));
            } else if (feature.getType() == FeatureType.GPLINKEDGLYCOPEPTIDE && ((GPLinkedGlycoPeptide) feature).getProtein() == null) {
                errorMessage.addError(new ObjectError("protein", "NoEmpty"));
            }
        } else {
            // other types, i.e. controls
            if (feature.getLinker() == null && !feature.getType().equals(FeatureType.NEGATIVE_CONTROL)) {
                errorMessage.addError(new ObjectError("linker", "NoEmpty"));
                
            }
        }
        
        if (feature.getMetadata() == null) {
            errorMessage.addError(new ObjectError("metadata", "NoEmpty"));
        }
        
        if (feature.getName() != null && !feature.getName().trim().isEmpty()) {
            try {
                org.glygen.array.persistence.rdf.Feature existing = featureRepository.getFeatureByLabel(feature.getName(), user);
                if (existing != null) {
                    feature.setId(existing.getId());
                    String[] codes = {existing.getId()};
                    errorMessage.addError(new ObjectError("name", codes, null, "Duplicate"));
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
                    String[] codes = {existing.getId()};
                    errorMessage.addError(new ObjectError("internalId", codes, null, "Duplicate"));
                }
            } catch (SparqlException | SQLException e) {
                throw new GlycanRepositoryException("Could not query existing features", e);
            }
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid feature information", errorMessage);
        
        try {
            switch (feature.getType()) {
            case LINKEDGLYCAN:
                return addLinkedGlycan((LinkedGlycan)feature, errorMessage, user);
            case GLYCOLIPID:
                return addGlycoLipid((GlycoLipid)feature, errorMessage, user);
            case GLYCOPEPTIDE:
                return addGlycoPeptide((GlycoPeptide)feature, errorMessage, user);
            case GLYCOPROTEIN:
                return addGlycoProtein((GlycoProtein)feature, errorMessage, user);
            case GPLINKEDGLYCOPEPTIDE:
                return addGPLinkedGlycoPeptide((GPLinkedGlycoPeptide)feature, errorMessage, user);
            case LANDING_LIGHT:
            case NEGATIVE_CONTROL:
            case COMPOUND:
            case CONTROL:
            default:
                String featureURI = featureRepository.addFeature(feature, user);
                String id = featureURI.substring(featureURI.lastIndexOf("/")+1);
                return id;
            
            } 
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Feature cannot be added for user " + user.getUsername(), e);
        }       
    }
    
    private String addGPLinkedGlycoPeptide(GPLinkedGlycoPeptide feature, ErrorMessage errorMessage, UserEntity user) throws SparqlException, SQLException {
        try {
            if (feature.getLinker() != null) {
                if (feature.getLinker().getUri() == null && feature.getLinker().getId() == null) {
                    if (feature.getLinker() instanceof Linker) {
                        try {
                            String id = addLinker((Linker)feature.getLinker(), 
                                ((Linker)feature.getLinker()).getType().name().startsWith("UNKNOWN"), user);
                            ((Linker)feature.getLinker()).setId(id);
                        } catch (Exception e) {
                            // we can ignore if it is duplicate
                            if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                                ErrorMessage error = (ErrorMessage) e.getCause();
                                for (ObjectError err: error.getErrors()) {
                                    if (!err.getDefaultMessage().contains("Duplicate")) {
                                        errorMessage.addError(err);
                                    }
                                }
                            } else {
                                errorMessage.addError(new ObjectError("linker", e.getMessage()));
                            }
                            logger.debug("linker not added " + feature.getLinker().getId() == null ? feature.getLinker().getUri() : feature.getLinker().getId());
                        }
                    } 
                } else {
                    // check to make sure it is an existing linker
                    String linkerId = feature.getLinker().getId();
                    if (linkerId == null) {
                        // get it from uri
                        linkerId = feature.getLinker().getUri().substring(feature.getLinker().getUri().lastIndexOf("/")+1);
                    }
                    Linker existing = linkerRepository.getLinkerById(linkerId, user);
                    if (existing == null) {
                        // check public linkers
                        existing = linkerRepository.getLinkerById(linkerId, null);
                        if (existing == null) {
                            errorMessage.addError(new ObjectError("linker", "NotFound"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            errorMessage.addError(new ObjectError("linker", e.getMessage()));
            logger.error("Linker cannot be added/found", e);
        }
        // check its glycans
        if (feature.getPeptides() != null) {
            
            for (GlycoPeptide g: feature.getPeptides()) {
                if (g.getUri() == null && g.getId() == null) {
                    g.setId(addGlycoPeptide(g, errorMessage, user));
                    g.setUri(GlygenArrayRepositoryImpl.uriPrefix + g.getId());
                    // update the position map to include references to the newly created linked glycan
                    if (feature.getPositionMap() != null && !feature.getPositionMap().isEmpty()) {
                        for (String position: feature.getPositionMap().keySet()) {
                            String glycoPeptideUri = feature.getPositionMap().get(position);
                            if (!g.getGlycans().isEmpty()) {
                                if (glycoPeptideUri.equals(g.getGlycans().get(0).getUri())) {
                                    feature.getPositionMap().put(position, g.getUri());
                                }
                            }
                        }
                    }
                } else {
                    // check to make sure it is an existing glycopeptide feature
                    String featureId = g.getId();
                    if (featureId == null) {
                        // get it from uri
                        featureId = g.getUri().substring(g.getUri().lastIndexOf("/")+1);
                    }
                    org.glygen.array.persistence.rdf.Feature existing = featureRepository.getFeatureById(featureId, user);
                    if (existing == null || existing.getType() != FeatureType.GLYCOPEPTIDE) {
                        if (existing == null) { // check if it is a public one
                            existing = featureRepository.getFeatureById(featureId, null);
                            if (existing == null || existing.getType() != FeatureType.GLYCOPEPTIDE) {
                                errorMessage.addError(new ObjectError("glycoPeptide", "NotFound"));
                                //throw new IllegalArgumentException("Invalid Input: Not a valid linked glycan information", errorMessage);
                            }
                        } else {
                            errorMessage.addError(new ObjectError("glycoPeptide", "NotValid"));
                            //throw new IllegalArgumentException("Invalid Input: Not a valid linked glycan information", errorMessage);
                        }
                    }
                }
            } 
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid feature information", errorMessage);
        
        String featureURI = featureRepository.addFeature(feature, user);
        String id = featureURI.substring(featureURI.lastIndexOf("/")+1);
        return id;
    }

    private String addGlycoProtein(GlycoProtein feature, ErrorMessage errorMessage, UserEntity user) throws SparqlException, SQLException {
        try {
            if (feature.getLinker() != null) {
                if (feature.getLinker().getUri() == null && feature.getLinker().getId() == null) {
                    if (feature.getLinker() instanceof Linker) {
                        try {
                            String id = addLinker((Linker)feature.getLinker(), 
                                ((Linker)feature.getLinker()).getType().name().startsWith("UNKNOWN"), user);
                            ((Linker)feature.getLinker()).setId(id);
                        } catch (Exception e) {
                            // we can ignore if it is duplicate
                            if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                                ErrorMessage error = (ErrorMessage) e.getCause();
                                for (ObjectError err: error.getErrors()) {
                                    if (!err.getDefaultMessage().contains("Duplicate")) {
                                        errorMessage.addError(err);
                                    }
                                }
                            } else {
                                errorMessage.addError(new ObjectError("linker", e.getMessage()));
                            }
                            logger.debug("linker not added " + feature.getLinker().getId() == null ? feature.getLinker().getUri() : feature.getLinker().getId());
                        }
                    } 
                } else {
                    // check to make sure it is an existing linker
                    String linkerId = feature.getLinker().getId();
                    if (linkerId == null) {
                        // get it from uri
                        linkerId = feature.getLinker().getUri().substring(feature.getLinker().getUri().lastIndexOf("/")+1);
                    }
                    Linker existing = linkerRepository.getLinkerById(linkerId, user);
                    if (existing == null) {
                        // check public linkers
                        existing = linkerRepository.getLinkerById(linkerId, null);
                        if (existing == null) {
                            errorMessage.addError(new ObjectError("linker", "NotFound"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            errorMessage.addError(new ObjectError("linker", e.getMessage()));
            logger.error("Linker cannot be added/found", e);
        }
        // check its glycans
        if (feature.getGlycans() != null) {
            for (LinkedGlycan g: feature.getGlycans()) {
                if (g.getUri() == null && g.getId() == null) {
                    Map <LinkedGlycan, Glycan> baseGlycanMap = new HashMap<>();
                    for (GlycanInFeature gf: g.getGlycans()) {
                        baseGlycanMap.put(g, gf.getGlycan());
                    }
                    g.setId(addLinkedGlycan(g, errorMessage, user));
                    g.setUri(GlygenArrayRepositoryImpl.uriPrefix + g.getId());
                    // update the position map to include references to the newly created linked glycan
                    if (feature.getPositionMap() != null && !feature.getPositionMap().isEmpty()) {
                        for (String position: feature.getPositionMap().keySet()) {
                            String glycanUri = feature.getPositionMap().get(position);
                            if (!g.getGlycans().isEmpty()) {
                                if (glycanUri.equals(g.getGlycans().get(0).getGlycan().getUri())) {
                                    feature.getPositionMap().put(position, g.getUri());
                                } else if (baseGlycanMap.get(g) != null && glycanUri.equals (baseGlycanMap.get(g).getUri())) {
                                    feature.getPositionMap().put(position, g.getUri());
                                } 
                            }
                        }
                    }
                } else {
                    // check to make sure it is an existing linkedglycan feature
                    String featureId = g.getId();
                    if (featureId == null) {
                        // get it from uri
                        featureId = g.getUri().substring(g.getUri().lastIndexOf("/")+1);
                    }
                    org.glygen.array.persistence.rdf.Feature existing = featureRepository.getFeatureById(featureId, user);
                    if (existing == null || existing.getType() != FeatureType.LINKEDGLYCAN) {
                        if (existing == null) { // check if it is a public one
                            existing = featureRepository.getFeatureById(featureId, null);
                            if (existing == null || existing.getType() != FeatureType.LINKEDGLYCAN) {
                                errorMessage.addError(new ObjectError("linkedGlycan", "NotFound"));
                                //throw new IllegalArgumentException("Invalid Input: Not a valid linked glycan information", errorMessage);
                            }
                        } else {
                            errorMessage.addError(new ObjectError("linkedGlycan", "NotValid"));
                            //throw new IllegalArgumentException("Invalid Input: Not a valid linked glycan information", errorMessage);
                        }
                    }
                }
            } 
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid feature information", errorMessage);
        
        String featureURI = featureRepository.addFeature(feature, user);
        String id = featureURI.substring(featureURI.lastIndexOf("/")+1);
        return id;
    }

    private String addGlycoPeptide(GlycoPeptide feature, ErrorMessage errorMessage, UserEntity user) throws SparqlException, SQLException {
        try {
            if (feature.getLinker() != null) {
                if (feature.getLinker().getUri() == null && feature.getLinker().getId() == null) {
                    if (feature.getLinker() instanceof Linker) {
                        try {
                            String id = addLinker((Linker)feature.getLinker(), 
                                ((Linker)feature.getLinker()).getType().name().startsWith("UNKNOWN"), user);
                            ((Linker)feature.getLinker()).setId(id);
                        } catch (Exception e) {
                            // we can ignore if it is duplicate
                            if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                                ErrorMessage error = (ErrorMessage) e.getCause();
                                for (ObjectError err: error.getErrors()) {
                                    if (!err.getDefaultMessage().contains("Duplicate")) {
                                        errorMessage.addError(err);
                                    }
                                }
                            } else {
                                errorMessage.addError(new ObjectError("linker", e.getMessage()));
                            }
                            logger.debug("linker not added " + feature.getLinker().getId() == null ? feature.getLinker().getUri() : feature.getLinker().getId());
                        }
                    } 
                } else {
                    // check to make sure it is an existing linker
                    String linkerId = feature.getLinker().getId();
                    if (linkerId == null) {
                        // get it from uri
                        linkerId = feature.getLinker().getUri().substring(feature.getLinker().getUri().lastIndexOf("/")+1);
                    }
                    Linker existing = linkerRepository.getLinkerById(linkerId, user);
                    if (existing == null) {
                        // check public linkers
                        existing = linkerRepository.getLinkerById(linkerId, null);
                        if (existing == null) {
                            errorMessage.addError(new ObjectError("linker", "NotFound"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            errorMessage.addError(new ObjectError("linker", e.getMessage()));
            logger.error("Linker cannot be added/found", e);
        }
        // check its glycans
        if (feature.getGlycans() != null) {    
            for (LinkedGlycan g: feature.getGlycans()) {
                if (g.getUri() == null && g.getId() == null) {
                    List<String> baseGlycanUris = new ArrayList<>();
                    for (GlycanInFeature gf: g.getGlycans()) {
                        baseGlycanUris.add(gf.getGlycan().getUri());
                    }
                    g.setId(addLinkedGlycan(g, errorMessage, user));
                    g.setUri(GlygenArrayRepositoryImpl.uriPrefix + g.getId());
                    // update the position map to include references to the newly created linked glycan
                    if (feature.getPositionMap() != null && !feature.getPositionMap().isEmpty()) {
                        for (String position: feature.getPositionMap().keySet()) {
                            String glycanUri = feature.getPositionMap().get(position);
                            if (!g.getGlycans().isEmpty()) {
                                if (glycanUri.equals(g.getGlycans().get(0).getGlycan().getUri())) {
                                    feature.getPositionMap().put(position, g.getUri());
                                } else if (baseGlycanUris != null && baseGlycanUris.contains(glycanUri)) {
                                    feature.getPositionMap().put(position, g.getUri());
                                }
                            }
                        }
                    }
                } else {
                    // check to make sure it is an existing linkedglycan feature
                    String featureId = g.getId();
                    if (featureId == null) {
                        // get it from uri
                        featureId = g.getUri().substring(g.getUri().lastIndexOf("/")+1);
                    }
                    org.glygen.array.persistence.rdf.Feature existing = featureRepository.getFeatureById(featureId, user);
                    if (existing == null || existing.getType() != FeatureType.LINKEDGLYCAN) {
                        if (existing == null) { // check if it is a public one
                            existing = featureRepository.getFeatureById(featureId, null);
                            if (existing == null || existing.getType() != FeatureType.LINKEDGLYCAN) {
                                errorMessage.addError(new ObjectError("linkedGlycan", "NotFound"));
                               // throw new IllegalArgumentException("Invalid Input: Not a valid linked glycan information", errorMessage);
                            }
                        } else {
                            errorMessage.addError(new ObjectError("linkedGlycan", "NotValid"));
                            //throw new IllegalArgumentException("Invalid Input: Not a valid linked glycan information", errorMessage);
                        }
                    }
                }
            } 
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid feature information", errorMessage);
        
        String featureURI = featureRepository.addFeature(feature, user);
        String id = featureURI.substring(featureURI.lastIndexOf("/")+1);
        return id;
    }

    private String addGlycoLipid(GlycoLipid feature, ErrorMessage errorMessage, UserEntity user) throws SparqlException, SQLException {
        try {
            if (feature.getLinker() != null) {
                if (feature.getLinker().getUri() == null && feature.getLinker().getId() == null) {
                    if (feature.getLinker() instanceof Linker) {
                        try {
                            String id = addLinker((Linker)feature.getLinker(), 
                                ((Linker)feature.getLinker()).getType().name().startsWith("UNKNOWN"), user);
                            ((Linker)feature.getLinker()).setId(id);
                        } catch (Exception e) {
                            // we can ignore if it is duplicate
                            if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                                ErrorMessage error = (ErrorMessage) e.getCause();
                                for (ObjectError err: error.getErrors()) {
                                    if (!err.getDefaultMessage().contains("Duplicate")) {
                                        errorMessage.addError(err);
                                    }
                                }
                            } else {
                                errorMessage.addError(new ObjectError("linker", e.getMessage()));
                            }
                            logger.debug("linker not added " + feature.getLinker().getId() == null ? feature.getLinker().getUri() : feature.getLinker().getId());
                        }
                    } 
                } else {
                    // check to make sure it is an existing linker
                    String linkerId = feature.getLinker().getId();
                    if (linkerId == null) {
                        // get it from uri
                        linkerId = feature.getLinker().getUri().substring(feature.getLinker().getUri().lastIndexOf("/")+1);
                    }
                    Linker existing = linkerRepository.getLinkerById(linkerId, user);
                    if (existing == null) {
                        // check public linkers
                        existing = linkerRepository.getLinkerById(linkerId, null);
                        if (existing == null) {
                            errorMessage.addError(new ObjectError("linker", "NotFound"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            errorMessage.addError(new ObjectError("linker", e.getMessage()));
            logger.error("Linker cannot be added/found", e);
        }
        // check its glycans
        if (feature.getGlycans() != null) {
            
            for (LinkedGlycan g: feature.getGlycans()) {
                if (g.getUri() == null && g.getId() == null) {
                    g.setId(addLinkedGlycan(g, errorMessage, user));
                    g.setUri(GlygenArrayRepositoryImpl.uriPrefix + g.getId());
                } else {
                    // check to make sure it is an existing linkedglycan feature
                    String featureId = g.getId();
                    if (featureId == null) {
                        // get it from uri
                        featureId = g.getUri().substring(g.getUri().lastIndexOf("/")+1);
                    }
                    org.glygen.array.persistence.rdf.Feature existing = featureRepository.getFeatureById(featureId, user);
                    if (existing == null || existing.getType() != FeatureType.LINKEDGLYCAN) {
                        if (existing == null) { // check if it is a public one
                            existing = featureRepository.getFeatureById(featureId, null);
                            if (existing == null || existing.getType() != FeatureType.LINKEDGLYCAN) {
                                errorMessage.addError(new ObjectError("linkedGlycan", "NotFound"));
                                //throw new IllegalArgumentException("Invalid Input: Not a valid linked glycan information", errorMessage);
                            }
                        } else {
                            errorMessage.addError(new ObjectError("linkedGlycan", "NotValid"));
                            //throw new IllegalArgumentException("Invalid Input: Not a valid linked glycan information", errorMessage);
                        }
                    }
                }
            } 
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid feature information", errorMessage);
        
        String featureURI = featureRepository.addFeature(feature, user);
        String id = featureURI.substring(featureURI.lastIndexOf("/")+1);
        return id;
    }
    
    private String addLinkedGlycan(LinkedGlycan feature, ErrorMessage errorMessage, UserEntity user) throws SparqlException, SQLException {
        try {
            if (feature.getLinker() != null) {
                if (feature.getLinker().getUri() == null && feature.getLinker().getId() == null) {
                    if (feature.getLinker() instanceof Linker) {
                        try {
                            String id = addLinker((Linker)feature.getLinker(), 
                                ((Linker)feature.getLinker()).getType().name().startsWith("UNKNOWN"), user);
                            ((Linker)feature.getLinker()).setId(id);
                        } catch (Exception e) {
                            // we can ignore if it is duplicate
                            if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                                ErrorMessage error = (ErrorMessage) e.getCause();
                                for (ObjectError err: error.getErrors()) {
                                    if (!err.getDefaultMessage().contains("Duplicate")) {
                                        errorMessage.addError(err);
                                    }
                                }
                            } else {
                                errorMessage.addError(new ObjectError("linker", e.getMessage()));
                            }
                            logger.debug("linker not added " + feature.getLinker().getId() == null ? feature.getLinker().getUri() : feature.getLinker().getId());
                        }
                    } 
                } else {
                    // check to make sure it is an existing linker
                    String linkerId = feature.getLinker().getId();
                    if (linkerId == null) {
                        // get it from uri
                        linkerId = feature.getLinker().getUri().substring(feature.getLinker().getUri().lastIndexOf("/")+1);
                    }
                    Linker existing = linkerRepository.getLinkerById(linkerId, user);
                    if (existing == null) {
                        // check public linkers
                        existing = linkerRepository.getLinkerById(linkerId, null);
                        if (existing == null) {
                            errorMessage.addError(new ObjectError("linker", "NotFound"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            errorMessage.addError(new ObjectError("linker", e.getMessage()));
            logger.error("Linker cannot be added/found", e);
        }
        // check its glycans
        if (feature.getGlycans() != null) {
            
            for (GlycanInFeature gf: feature.getGlycans()) {
                Glycan g = gf.getGlycan();
                if (g.getUri() == null && g.getId() == null) {
                    try {
                        g.setId(addGlycan(g, user, true, false));
                    } catch (Exception e) {
                        // need to ignore duplicate check errors
                        if (e.getCause() != null && e.getCause() instanceof ErrorMessage) {
                            ErrorMessage error = (ErrorMessage) e.getCause();
                            for (ObjectError err: error.getErrors()) {
                                if (!err.getDefaultMessage().contains("Duplicate")) {
                                    errorMessage.addError(err);
                                }
                            }
                        } else {
                            errorMessage.addError(new ObjectError("glycan", e.getMessage()));
                        }
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
                        existing = glycanRepository.getGlycanById(glycanId, null);
                        if (existing == null) {
                            
                            errorMessage.addError(new ObjectError("glycan", "NotValid"));
                            //throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
                        }
                    }
                }
                
                if (g instanceof SequenceDefinedGlycan) {
                    // based on the reducing end configuration 
                    if (gf.getReducingEndConfiguration() != null) {
                        switch (gf.getReducingEndConfiguration().getType()) {
                        case ALPHA:
                            // get alpha version of the glycan
                            Glycan alpha = glycanRepository.retrieveOtherSubType(g, GlycanSubsumtionType.ALPHA, user);
                            if (alpha != null) {
                                gf.setGlycan(alpha);
                            }
                            break;
                        case BETA:
                            // get beta version of the glycan
                            Glycan beta = glycanRepository.retrieveOtherSubType(g, GlycanSubsumtionType.BETA, user);
                            if (beta != null) {
                                gf.setGlycan(beta);
                            }
                            break;
                        case OPENSRING:
                            // get alditol version of the glycan
                            Glycan open = glycanRepository.retrieveOtherSubType(g, GlycanSubsumtionType.ALDITOL, user);
                            if (open != null) {
                                gf.setGlycan(open);
                            }
                            break;
                        case EQUILIBRIUM:
                        case UNKNOWN:
                        default:
                            if (((SequenceDefinedGlycan) g).getSubType() != GlycanSubsumtionType.BASE) {
                                // error
                                errorMessage.addError(new ObjectError("glycan", "NotBaseType"));
                                //throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
                            }
                            break;
                            
                        }
                    }
                }
            } 
        }
      
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid feature information. Reason: " + errorMessage.toString(), errorMessage);
        
        String featureURI = featureRepository.addFeature(feature, user);
        String id = featureURI.substring(featureURI.lastIndexOf("/")+1);
        return id;   
    }
    
    private String addSmallMoleculeLinker(SmallMoleculeLinker linker, boolean unknown, UserEntity user) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        
        if (linker.getClassification() == null && linker.getPubChemId() == null && linker.getInChiKey() == null) {   // at least one of them should be provided
            if (!unknown) 
                errorMessage.addError(new ObjectError("pubChemId", "NoEmpty"));
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
        
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
        try {
            Linker l = null;
            String linkerURI = null;
            if (linker.getPubChemId() != null) {
                String existing = linkerRepository.getLinkerByField(linker.getPubChemId().toString(), "has_pubchem_compound_id", "long", user);
                if (existing != null && !existing.contains("public")) {
                    linker.setUri(existing);
                    linkerURI = existing;
                    String[] codes = {existing.substring(existing.lastIndexOf("/")+1)};
                    errorMessage.addError(new ObjectError("pubchemid", codes, null, "Duplicate"));
                }
            }
            else if (linker.getInChiKey() != null && !linker.getInChiKey().trim().isEmpty()) {
                String existing = linkerRepository.getLinkerByField(linker.getInChiKey(), "has_inChI_key", "string", user);
                if (existing != null && !existing.contains("public")) {
                    linker.setUri(existing);
                    linkerURI = existing;
                    String[] codes = {existing.substring(existing.lastIndexOf("/")+1)};
                    errorMessage.addError(new ObjectError("inChiKey", codes, null, "Duplicate"));    
                }
            }
            if (linkerURI == null) {
                // get the linker details from pubChem
                ObjectError err = new ObjectError("pubchemid", "NotValid");
                if (linker.getPubChemId() != null || (linker.getInChiKey() != null && !linker.getInChiKey().trim().isEmpty())) {
                    try {
                        if (linker.getPubChemId() != null) {
                            err = new ObjectError("pubchemid", "NotValid");
                            l = PubChemAPI.getLinkerDetailsFromPubChem(linker.getPubChemId());
                            if (l != null && linker.getType() == LinkerType.LIPID) {
                                // need to create Lipid object
                                l = new Lipid ((SmallMoleculeLinker)l);
                            }
                        } else if (linker.getInChiKey() != null && !linker.getInChiKey().trim().isEmpty()) {
                            err = new ObjectError("inChiKey", "NotValid");
                            l = PubChemAPI.getLinkerDetailsFromPubChemByInchiKey(linker.getInChiKey().trim());
                            if (l != null && linker.getType() == LinkerType.LIPID) {
                                // need to create Lipid object
                                l = new Lipid ((SmallMoleculeLinker)l);
                            }
                        }
                        if (l == null) {
                            // could not get details from PubChem
                            errorMessage.addError(err);
                        } else {
                            if (linker.getName() != null) l.setName(linker.getName().trim());
                            if (linker.getDescription() != null) l.setDescription(linker.getDescription().trim());
                            if (((SmallMoleculeLinker)l).getClassification() == null)
                                ((SmallMoleculeLinker)l).setClassification (linker.getClassification());
                            l.setSource(linker.getSource());
                            l.setUrls(linker.getUrls());
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
                
                LinkerType otherType = null;
                if (linker.getType().name().startsWith("UNKNOWN")) {
                    // add the regular type to the query
                    otherType = LinkerType.valueOf(linker.getType().name().substring(linker.getType().name().lastIndexOf("UNKNOWN_")+8));
                } else if (!linker.getType().name().startsWith("UNKNOWN")) {
                    otherType = LinkerType.valueOf("UNKNOWN_" + linker.getType().name());
                   
                }
                
                if (linker.getName() != null && !linker.getName().trim().isEmpty()) {
                    Linker local = linkerRepository.getLinkerByLabel(linker.getName().trim(), linker.getType(), user);
                    if (local != null && (local.getType() == linker.getType() || local.getType() == otherType)) {
                        linker.setId(local.getId());
                        String[] codes = {local.getId()};
                        errorMessage.addError(new ObjectError("name", codes, null, "Duplicate"));    
                    }
                }
                
                if (unknown) {
                    if (!linker.getType().name().startsWith("UNKNOWN")) { 
                        LinkerType unknownType = LinkerType.valueOf("UNKNOWN_" + linker.getType().name());
                        l.setType(unknownType);
                    } // else - already unknown
                }
                
                // retrieve publication details
                if (l != null && linker.getPublications() != null && !linker.getPublications().isEmpty()) {
                    List<Publication> pubList = new ArrayList<Publication>();
                    PubmedUtil util = new PubmedUtil();
                    for (Publication pub: linker.getPublications()) {
                        if (pub.getPubmedId() != null) {
                            try {
                                DTOPublication publication = util.createFromPubmedId(pub.getPubmedId());
                                pubList.add (UtilityController.getPublicationFrom(publication));
                            } catch (Exception e) {
                                logger.error("Cannot retrieve details from PubMed", e);
                                errorMessage.addError(new ObjectError("pubMedId", "NotValid"));
                            }
                        }
                    }
                    l.setPublications(pubList);
                }
            } 
            
            if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
                throw new IllegalArgumentException("Invalid Input: Not a valid linker information", errorMessage);
            
            if (l != null) {
                String addedURI = linkerRepository.addLinker(l, user);
                return addedURI.substring(addedURI.lastIndexOf("/")+1);
            }
            
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Linker cannot be added for user " + user.getUsername(), e);
        } 
        
        return null;
    }
    
    private String addSequenceDefinedGlycan (SequenceDefinedGlycan glycan, UserEntity user, Boolean noGlytoucanRegistration, Boolean byPassGlytoucanCheck) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
        if (byPassGlytoucanCheck == null)
            byPassGlytoucanCheck = false;
        
        Boolean checkGlytoucan = false;
        if (glycan.getSequence() == null || glycan.getSequence().trim().isEmpty()) {
            // accept if there is glytoucanId
            if (glycan.getGlytoucanId() != null && !glycan.getGlytoucanId().isEmpty()) {
                String sequence = getSequenceFromGlytoucan(glycan.getGlytoucanId());
                if (sequence == null) {
                    errorMessage = new ErrorMessage("GlytoucanId is not valid");
                    errorMessage.addError(new ObjectError("glytoucanId", "NotValid"));
                    throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
                } else {
                    glycan.setSequence(sequence);
                }
            } else {
                errorMessage = new ErrorMessage("Sequence cannot be empty");
                errorMessage.addError(new ObjectError("sequence", "NoEmpty"));
                throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
            }
        } else {
            // check if both sequence and glytoucanId is provided
            // in such a case, we need to confirm they match
            if (glycan.getSequence() != null && !glycan.getSequence().trim().isEmpty() 
                    && glycan.getGlytoucanId() != null && !glycan.getGlytoucanId().trim().isEmpty()) {
                checkGlytoucan = true;
            }
        }
        
        
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
        FixGlycoCtUtil fixGlycoCT = new FixGlycoCtUtil();
        
        boolean gwbError = false;
        Sugar sugar = null;
        try {
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
                                glycoCT = fixGlycoCT.fixGlycoCT(glycoCT);
                        } catch (Exception e) {
                            logger.error("Glycan builder parse error", e.getMessage());
                            gwbError = true;
                        }
                        
                        if (gwbError) {
                            // check to make sure GlycoCT valid without using GWB
                            SugarImporterGlycoCTCondensed importer = new SugarImporterGlycoCTCondensed();
                            try {
                                sugar = importer.parse(glycan.getSequence().trim());
                                if (sugar == null) {
                                    logger.error("Cannot get Sugar object for sequence:" + glycan.getSequence().trim());
                                    parseError = true;
                                    gwbError = false;  
                                } else {
                                    SugarExporterGlycoCTCondensed exporter = new SugarExporterGlycoCTCondensed();
                                    exporter.start(sugar);
                                    glycoCT = exporter.getHashCode();
                                    glycoCT = fixGlycoCT.fixGlycoCT(glycoCT);
                                    // calculate mass
                                    GlycoVisitorMass massVisitor = new GlycoVisitorMass();
                                    massVisitor.start(sugar);
                                    g.setMass(massVisitor.getMass(GlycoVisitorMass.DERIVATISATION_NONE));
                                }
                            } catch (Exception pe) {
                                logger.error("GlycoCT parsing failed", pe.getMessage());
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
                        glycoCT = fixGlycoCT.fixGlycoCT(glycoCT);
                        g.setMass(computeMass(glycanObject));
                        g.setSequence(glycoCT);
                        g.setSequenceType(GlycanSequenceFormat.GLYCOCT);
                        break;
                    case WURCS:
                        WURCSToGlycoCT wurcsConverter = new WURCSToGlycoCT();
                        wurcsConverter.start(glycan.getSequence().trim());
                        glycoCT = wurcsConverter.getGlycoCT();
                        if (glycoCT != null) {
                            glycoCT = fixGlycoCT.fixGlycoCT(glycoCT);
                            glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromGlycoCTCondensed(glycoCT);
                            g.setMass(computeMass(glycanObject));
                            g.setSequence(glycoCT);
                            g.setSequenceType(GlycanSequenceFormat.GLYCOCT);
                        } else { // keep the sequence as WURCS
                            // recode the sequence
                            WURCSValidator validator = new WURCSValidator();
                            validator.start(glycan.getSequence().trim());
                            if (validator.getReport().hasError()) {
                                String [] codes = validator.getReport().getErrors().toArray(new String[0]);
                                errorMessage.addError(new ObjectError("sequence", codes, null, "NotValid"));
                            } else {
                                g.setSequence(validator.getReport().getStandardString());
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
                        }
                        break;
                    case IUPAC:
                        CFGMasterListParser parser = new CFGMasterListParser();
                        glycoCT = parser.translateSequence(SequenceUtils.cleanupSequence(glycan.getSequence().trim()));
                        if (glycoCT != null) {
                            glycoCT = fixGlycoCT.fixGlycoCT(glycoCT);
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
            throw new GlycanRepositoryException("Glycan cannot be added for user " + user.getUsername(), e);
        }
        
        try {   
            // no errors add the glycan
            if (glycanObject != null || gwbError) {
                boolean correctBase = false;
                if (gwbError) {
                    if (sugar != null) {
                        correctBase = GlycanBaseTypeUtil.isMakeBaseTypePossible(sugar);
                    }
                } else {
                    SugarImporterGlycoCTCondensed importer = new SugarImporterGlycoCTCondensed();
                    if (g.getSequenceType() == GlycanSequenceFormat.GLYCOCT) {
                        sugar = importer.parse(g.getSequence());
                        if (sugar != null) {
                            correctBase = GlycanBaseTypeUtil.isMakeBaseTypePossible(sugar);
                        }
                    } 
                }
                
                if (g.getSequenceType() == GlycanSequenceFormat.GLYCOCT && !correctBase) {
                    // error
                    errorMessage.addError(new ObjectError("sequence", null, null, "NotBaseType"));
                }
                
                if (g.getSequenceType() == GlycanSequenceFormat.WURCS) {
                    // cannot get the Sugar object since it cannot be converted to GlycoCT
                    // for now, only add the base version
                    // TODO figure out how to calculate the other versions
                    if (checkGlytoucan) {
                        // need to check if the sequence and the given glytoucan match
                        String glyToucanId = GlytoucanUtil.getInstance().getAccessionNumber(glycan.getSequence().trim());
                        if (glyToucanId == null || !glyToucanId.equals(glycan.getGlytoucanId().trim())) {
                            // error
                            errorMessage.addError(new ObjectError("glytoucanId", null, null, "NotValid"));
                        }
                    }
                    if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
                        throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
                    return addGlycan(g, null, user, noGlytoucanRegistration);        
                } else {
                    if (checkGlytoucan && !byPassGlytoucanCheck) {
                        try {
                            // need to check if the sequence and the given glytoucan match
                            WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
                            exporter.start(glycan.getSequence().trim());
                            String wurcs = exporter.getWURCS();
                            String glyToucanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
                            if (glyToucanId == null || !glyToucanId.equals(glycan.getGlytoucanId().trim())) {
                                // error
                                errorMessage.addError(new ObjectError("glytoucanId", null, null, "NotValid"));
                            }
                        } catch (WURCSException | SugarImporterException | GlycoVisitorException e) {
                            logger.warn ("cannot convert sequence into Wurcs to check glytoucan", e);
                        }
                    }
                    if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
                        throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
                    return addAllConfigurations (g, sugar, noGlytoucanRegistration, user);
                }
            }
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Glycan cannot be added for user " + user.getUsername(), e);
        } catch (IOException e) {
            logger.error("Glycan image cannot be generated", e);
            throw new GlycanRepositoryException("Glycan image cannot be generated", e);
        } catch (GlycoconjugateException | GlycoVisitorException e) {
            // should not happen
            throw new GlycanRepositoryException("Glycan cannot be added for user " + user.getUsername(), e);
        } catch (SugarImporterException e) {
            // should not happen
            throw new GlycanRepositoryException("Glycan cannot be added for user " + user.getUsername(), e);
        }
        return null;
    }
    
    @Override
    public String getSequenceFromGlytoucan (String glytoucanId) {
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
    
    private String addAllConfigurations(SequenceDefinedGlycan baseGlycan, 
            Sugar sugar, Boolean noGlytoucanRegistration, UserEntity user) 
                    throws GlycoVisitorException, GlycoconjugateException, SparqlException, SQLException, IOException {
        SugarExporterGlycoCTCondensed t_exporter = new SugarExporterGlycoCTCondensed();
        
        // make basetype (unknown anomer)
        GlycanBaseTypeUtil.makeBaseType(sugar);
        t_exporter.start(sugar);
        SequenceDefinedGlycan glycan1 = new SequenceDefinedGlycan();
        glycan1.setName(baseGlycan.getName());
        glycan1.setDescription(baseGlycan.getDescription());
        glycan1.setInternalId(baseGlycan.getInternalId());
        glycan1.setSequence(t_exporter.getHashCode());
        glycan1.setSequenceType(GlycanSequenceFormat.GLYCOCT);
        glycan1.setSubType(GlycanSubsumtionType.BASE);
        // calculate mass
        try {
            GlycoVisitorMass massVisitor = new GlycoVisitorMass();
            massVisitor.start(sugar);
            glycan1.setMass(massVisitor.getMass(GlycoVisitorMass.DERIVATISATION_NONE));
        } catch (Exception e) {
            logger.warn("Mass cannot be calculated", e);
        }
        String baseId = addGlycan(glycan1, null, user, noGlytoucanRegistration);
        
        // make alpha version
        GlycanBaseTypeUtil.makeAlpha(sugar);
        t_exporter.start(sugar);
        SequenceDefinedGlycan glycan2 = new SequenceDefinedGlycan();
        glycan2.setSequence(t_exporter.getHashCode());
        glycan2.setSequenceType(GlycanSequenceFormat.GLYCOCT);
        glycan2.setSubType(GlycanSubsumtionType.ALPHA);
        glycan2.setName(baseGlycan.getName()+" (Alpha version)");
        // calculate mass
     // calculate mass
        try {
            GlycoVisitorMass massVisitor = new GlycoVisitorMass();
            massVisitor.start(sugar);
            glycan2.setMass(massVisitor.getMass(GlycoVisitorMass.DERIVATISATION_NONE));
        } catch (Exception e) {
            logger.warn("Mass cannot be calculated", e);
        }
        addGlycan(glycan2, glycan1, user, noGlytoucanRegistration);
     
        // make beta version
        GlycanBaseTypeUtil.makeBeta(sugar);
        t_exporter.start(sugar);
        SequenceDefinedGlycan glycan3 = new SequenceDefinedGlycan();
        glycan3.setSequence(t_exporter.getHashCode());
        glycan3.setSequenceType(GlycanSequenceFormat.GLYCOCT);
        glycan3.setSubType(GlycanSubsumtionType.BETA);
        glycan3.setName(baseGlycan.getName()+" (Beta version)");
        // calculate mass
     // calculate mass
        try {
            GlycoVisitorMass massVisitor = new GlycoVisitorMass();
            massVisitor.start(sugar);
            glycan3.setMass(massVisitor.getMass(GlycoVisitorMass.DERIVATISATION_NONE));
        } catch (Exception e) {
            logger.warn("Mass cannot be calculated", e);
        }
        addGlycan(glycan3, glycan1, user, noGlytoucanRegistration);
      
        // make alditol version
        GlycanBaseTypeUtil.makeAlditol(sugar);
        t_exporter.start(sugar);
        SequenceDefinedGlycan glycan4 = new SequenceDefinedGlycan();
        glycan4.setSequence(t_exporter.getHashCode());
        glycan4.setSequenceType(GlycanSequenceFormat.GLYCOCT);
        glycan4.setSubType(GlycanSubsumtionType.ALDITOL);
        glycan4.setName(baseGlycan.getName()+" (Alditol version)");
        // calculate mass
     // calculate mass
        try {
            GlycoVisitorMass massVisitor = new GlycoVisitorMass();
            massVisitor.start(sugar);
            glycan4.setMass(massVisitor.getMass(GlycoVisitorMass.DERIVATISATION_NONE));
        } catch (Exception e) {
            logger.warn("Mass cannot be calculated", e);
        }
        addGlycan(glycan4, glycan1, user, noGlytoucanRegistration);
        
        return baseId;
    }
        
    private String addGlycan (SequenceDefinedGlycan g, SequenceDefinedGlycan baseGlycan, UserEntity user, Boolean noGlytoucanRegistration) throws SparqlException, SQLException, IOException {
        String id = null;
        if (baseGlycan == null) {
            String glycanURI = glycanRepository.addGlycan(g, user, noGlytoucanRegistration);
            id = glycanURI.substring(glycanURI.lastIndexOf("/")+1);
            g.setId(id);
        } else {
            String glycanURI = glycanRepository.addSequenceDefinedGlycan(g, baseGlycan, user, noGlytoucanRegistration);
            id = glycanURI.substring(glycanURI.lastIndexOf("/")+1);
            g.setId(id);
        }
        
        Glycan added = glycanRepository.getGlycanById(id, user);
        if (added != null) {
            BufferedImage t_image = GlygenArrayController.createImageForGlycan(g);
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
    
    private String addOtherGlycan(OtherGlycan glycan, UserEntity user) {
        if (glycan.getSequence() == null) {
            ErrorMessage errorMessage = new ErrorMessage("sequence cannot be empty");
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
            
        } else {
            throw new RuntimeException("Validator cannot be found!");
        }
        
        try {
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
            throw new GlycanRepositoryException("Glycan cannot be added for user " + user.getUsername(), e);
        }
                
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
                throw new IllegalArgumentException("Invalid Input: Not a valid glycan information", errorMessage);
        try {   
            // no errors add the glycan
            String glycanURI = glycanRepository.addGlycan(glycan, user);
            return glycanURI.substring(glycanURI.lastIndexOf("/")+1);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Glycan cannot be added for user " + user.getUsername(), e);
        }
    }

    @Override
    public String addBlockLayout(BlockLayout layout, Boolean noFeatureCheck, UserEntity user) {
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
        
        try {
            BlockLayout existing = layoutRepository.getBlockLayoutByName(layout.getName(), user, false);
            if (existing != null) {
                // duplicate
                String[] codes = {existing.getId()};
                errorMessage.addError(new ObjectError("name", codes, null, "Duplicate"));
                throw new IllegalArgumentException("A block layout with the same name already exists", errorMessage);
            } 
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Block layout cannot be added for user " + user.getUsername(), e);
        }
        
        if (noFeatureCheck != null && !noFeatureCheck) {
            // check if features exist
            Map<String, org.glygen.array.persistence.rdf.Feature> checkedMap = new HashMap<>();
            if (layout.getSpots() != null) {
                for (org.glygen.array.persistence.rdf.Spot s: layout.getSpots()) {
                   /* if (s.getRatioMap() != null) {
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
                    }*/
                    if (s.getFeatures() != null) {
                        List<org.glygen.array.persistence.rdf.Feature> newList = new ArrayList<org.glygen.array.persistence.rdf.Feature>();
                        for (org.glygen.array.persistence.rdf.Feature f: s.getFeatures()) {
                            try {
                                String key = f.getInternalId();
                                if (checkedMap.get(f.getInternalId()) == null && checkedMap.get(f.getId()) == null) {
                                    org.glygen.array.persistence.rdf.Feature existing = featureRepository.getFeatureByLabel(f.getInternalId(), "gadr:has_internal_id", user);
                                    if (existing == null) {
                                        // check by uri
                                        if (f.getUri() != null) {
                                            existing = featureRepository.getFeatureFromURI(f.getUri(), user);
                                            key = f.getUri().substring(f.getUri().lastIndexOf("/")+1);
                                        } else if (f.getId() != null) {
                                            existing = featureRepository.getFeatureFromURI(GlygenArrayRepositoryImpl.uriPrefix + f.getId(), user);
                                            key = f.getId();
                                        }
                                        if (existing == null) {
                                            // check the public one
                                            existing = featureRepository.getFeatureByLabel(f.getInternalId(), "gadr:has_internal_id", null);
                                            if (existing == null) {
                                                errorMessage.addError(new ObjectError("feature", 
                                                        f.getInternalId() == null ? f.getName(): f.getInternalId() + " does not exist in the repository"));
                                            }
                                        } 
                                    } else {
                                        newList.add(existing);
                                        if (s.getFeatureConcentrationMap().get(f) != null) {
                                            LevelUnit con = s.getFeatureConcentrationMap().get(f);
                                            s.getFeatureConcentrationMap().put(existing, con);
                                            s.getFeatureConcentrationMap().remove(f);
                                        }
                                        
                                        if (s.getFeatureRatioMap().get(f) != null) {
                                            Double ratio = s.getFeatureRatioMap().get(f);
                                            s.getFeatureRatioMap().put(existing, ratio);
                                            s.getFeatureRatioMap().remove(f);
                                        }
                                    }
                                    checkedMap.put(key, existing);
                                } else {
                                    org.glygen.array.persistence.rdf.Feature feat = checkedMap.get(f.getInternalId());
                                    if (feat == null) {
                                        feat = checkedMap.get(f.getId());
                                        key = f.getId();
                                    }
                                    if (feat != null) {
                                        newList.add(feat);
                                        if (s.getFeatureConcentrationMap().get(f) != null) {
                                            LevelUnit con = s.getFeatureConcentrationMap().get(f);
                                            s.getFeatureConcentrationMap().put(checkedMap.get(key), con);
                                            s.getFeatureConcentrationMap().remove(f);
                                        }
                                        if (s.getFeatureRatioMap().get(f) != null) {
                                            Double ratio = s.getFeatureRatioMap().get(f);
                                            s.getFeatureRatioMap().put(checkedMap.get(key), ratio);
                                            s.getFeatureRatioMap().remove(f);
                                        }
                                    } else {
                                        errorMessage.addError(new ObjectError("feature", f.getInternalId() == null ? f.getName(): f.getInternalId() + " does not exist in the repository"));
                                    }
                                }
                            } catch (SparqlException | SQLException e) {
                                throw new GlycanRepositoryException("Block layout cannot be added for user " + user.getUsername(), e);
                            }
                        }
                        s.setFeatures(newList);
                        if (s.getMetadata() == null) {
                            errorMessage.addError(new ObjectError("metadata", "NoEmpty"));
                        }
                    }   
                }
            }
        }
        
        if (errorMessage.getErrors() != null && !errorMessage.getErrors().isEmpty()) 
            throw new IllegalArgumentException("Invalid Input: Not a valid block layout information", errorMessage);
        
        try {
            String uri = layoutRepository.addBlockLayout(layout, user);
            String id = uri.substring(uri.lastIndexOf("/")+1);
            return id;
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Block layout cannot be added for user " + user.getUsername(), e);
        }
    }
    
    public static Map<Object, String> cleanFeature (Feature feature) {
        // need to fix positionMap if it exists
        Map<Object, String> newPositions = new HashMap<Object, String>();
        if (feature.getPositionMap() != null && !feature.getPositionMap().isEmpty()) {
            for (String position: feature.getPositionMap().keySet()) { 
                String uri = feature.getPositionMap().get(position);
                switch (feature.getType()) {
                case GLYCOPEPTIDE:
                    if (((GlycoPeptide)feature).getGlycans() != null) {
                        boolean located = false;
                        for (LinkedGlycan g: ((GlycoPeptide)feature).getGlycans()) {
                            if (g.getUri() != null && g.getUri().equals(uri)) {
                                newPositions.put(g, position);
                                located = true;
                            }
                        }
                        if (!located) {
                            // check the individual glycans
                            for (LinkedGlycan g: ((GlycoPeptide)feature).getGlycans()) {
                                for (GlycanInFeature gf: g.getGlycans()) {
                                    if (gf.getGlycan().getUri().equals(uri)) {
                                        newPositions.put(gf.getGlycan(), position);
                                    }
                                }
                            }
                        }
                    }
                break;
                case GLYCOPROTEIN:
                    if (((GlycoProtein)feature).getGlycans() != null) {
                        boolean located = false;
                        for (LinkedGlycan g: ((GlycoProtein)feature).getGlycans()) {
                            if (g.getUri() != null && g.getUri().equals(uri)) {
                                newPositions.put(g, position);
                                located = true;
                            }
                        }
                        
                        if (!located) {
                            // check the individual glycans
                            for (LinkedGlycan g: ((GlycoPeptide)feature).getGlycans()) {
                                for (GlycanInFeature gf: g.getGlycans()) {
                                    if (gf.getGlycan().getUri().equals(uri)) {
                                        newPositions.put(gf.getGlycan(), position);
                                    }
                                }
                            }
                        }
                    }
                    break;
                case GPLINKEDGLYCOPEPTIDE:
                    if (((GPLinkedGlycoPeptide)feature).getPeptides() != null) {
                        for (GlycoPeptide g: ((GPLinkedGlycoPeptide)feature).getPeptides()) {
                            if (g.getUri() != null && g.getUri().equals(uri)) {
                                newPositions.put(g, position);
                            }  
                        }
                    }
                    break;
                default:
                    break;
                }
            }
        }
        
        if (feature.getLinker() != null) {
            feature.getLinker().setUri(null);
            feature.getLinker().setId(null);
        }
        
        if (feature.getMetadata() != null) {
            feature.getMetadata().setUri(null);
            feature.getMetadata().setId(null);
        }
        
        switch (feature.getType()) {
        case LINKEDGLYCAN:
            if (((LinkedGlycan) feature).getGlycans() != null) {
                for (GlycanInFeature gf: ((LinkedGlycan) feature).getGlycans()) {
                    Glycan g = gf.getGlycan();
                    g.setUri(null);
                    g.setId(null);
                }
            }
            break;
        case GLYCOLIPID:
            if (((GlycoLipid)feature).getGlycans() != null) {
                for (LinkedGlycan g: ((GlycoLipid)feature).getGlycans()) {
                    g.setUri(null);
                    g.setId(null);
                    if (g.getLinker() != null) {
                        g.getLinker().setUri(null);
                        g.getLinker().setId(null);
                    }
                    for (GlycanInFeature gf: g.getGlycans()) {
                        Glycan glycan = gf.getGlycan();
                        glycan.setUri(null);
                        glycan.setId(null);
                    }
                }
            }
            Lipid lipid = ((GlycoLipid)feature).getLipid();
            lipid.setUri(null);
            lipid.setId(null);
                
            break;
        case GLYCOPEPTIDE:
            if (((GlycoPeptide)feature).getGlycans() != null) {
                for (LinkedGlycan g: ((GlycoPeptide)feature).getGlycans()) {
                    g.setUri(null);
                    g.setId(null);
                    if (g.getLinker() != null) {
                        g.getLinker().setUri(null);
                        g.getLinker().setId(null);
                    }
                    for (GlycanInFeature gf: g.getGlycans()) {
                        Glycan glycan = gf.getGlycan();
                        glycan.setUri(null);
                        glycan.setId(null);
                    }
                }
            }
            PeptideLinker pl = ((GlycoPeptide)feature).getPeptide();
            pl.setUri(null);
            pl.setId(null); 
            break;
        case GLYCOPROTEIN:
            if (((GlycoProtein)feature).getGlycans() != null) {
                for (LinkedGlycan g: ((GlycoProtein)feature).getGlycans()) {
                    g.setUri(null);
                    g.setId(null);
                    if (g.getLinker() != null) {
                        g.getLinker().setUri(null);
                        g.getLinker().setId(null);
                    }
                    for (GlycanInFeature gf: g.getGlycans()) {
                        Glycan glycan = gf.getGlycan();
                        glycan.setUri(null);
                        glycan.setId(null);
                    }
                }
            }
            ProteinLinker protein = ((GlycoProtein)feature).getProtein();
            protein.setUri(null);
            protein.setId(null);     
            break;
        case GPLINKEDGLYCOPEPTIDE:
            if (((GPLinkedGlycoPeptide)feature).getPeptides() != null) {
                for (GlycoPeptide g: ((GPLinkedGlycoPeptide)feature).getPeptides()) {
                    g.setUri(null);
                    g.setId(null);
                    for (LinkedGlycan lg: ((GlycoPeptide)g).getGlycans()) {
                        lg.setUri(null);
                        lg.setId(null);
                        if (lg.getLinker() != null) {
                            lg.getLinker().setUri(null);
                            lg.getLinker().setId(null);
                        }
                        for (GlycanInFeature gf: lg.getGlycans()) {
                            Glycan glycan = gf.getGlycan();
                            glycan.setUri(null);
                            glycan.setId(null);
                        }
                    }
                    PeptideLinker pep = ((GlycoPeptide)g).getPeptide();
                    pep.setUri(null);
                    pep.setId(null);
                }
            }
            
            ProteinLinker prot = ((GPLinkedGlycoPeptide)feature).getProtein();
            prot.setUri(null);
            prot.setId(null);
            break;
        
        default:
            break;
        }
        
        return newPositions;
    }

}
