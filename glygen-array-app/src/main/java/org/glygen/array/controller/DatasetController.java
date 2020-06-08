package org.glygen.array.controller;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.glygen.array.config.SesameTransactionConfig;
import org.glygen.array.exception.GlycanRepositoryException;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.UserRepository;
import org.glygen.array.persistence.rdf.data.ArrayDataset;
import org.glygen.array.persistence.rdf.data.ProcessedData;
import org.glygen.array.persistence.rdf.metadata.Description;
import org.glygen.array.persistence.rdf.metadata.Sample;
import org.glygen.array.persistence.rdf.template.DescriptorGroupTemplate;
import org.glygen.array.persistence.rdf.template.DescriptorTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplateType;
import org.glygen.array.persistence.rdf.template.Namespace;
import org.glygen.array.service.ArrayDatasetRepository;
import org.glygen.array.service.FeatureRepository;
import org.glygen.array.service.GlycanRepository;
import org.glygen.array.service.LayoutRepository;
import org.glygen.array.service.LinkerRepository;
import org.glygen.array.util.parser.ProcessedDataParser;
import org.glygen.array.util.parser.ProcessedResultConfiguration;
import org.glygen.array.view.ErrorCodes;
import org.glygen.array.view.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestBody;
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
@RequestMapping("/array")
public class DatasetController {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    @Autowired
    ArrayDatasetRepository datasetRepository;
    
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
    
    @Value("${spring.file.uploaddirectory}")
    String uploadDir;
    
    @Autowired
    ResourceLoader resourceLoader;
    
    @ApiOperation(value = "Import experiment results from uploaded excel file")
    @RequestMapping(value = "/addDatasetFromExcel", method=RequestMethod.POST, 
            consumes={"application/json", "application/xml"},
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added array dataset"), 
            @ApiResponse(code=400, message="Invalid request, file cannot be found"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to add array datasets"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addProcessedDataFromExcel (
            @ApiParam(required=true, value="uploaded Excel with the experiment results") 
            @RequestParam("file") String uploadedFileName, 
            @ApiParam(required=true, value="name of the array dataset (must already be in the repository) to add the processed data") 
            @RequestParam("name")
            String datasetName, 
            @ApiParam(required=true, value="(internal) name of the sample used in the experiment, must already be in the repository") 
            @RequestParam("sampleName")
            String sample,
            @ApiParam(required=true, value="configuration information related to excel file") 
            @RequestBody
            ProcessedResultConfiguration config,
            Principal p) {
        if (uploadedFileName != null) {
            File excelFile = new File(uploadDir, uploadedFileName);
            if (excelFile.exists()) {
                UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
                ProcessedDataParser parser = new ProcessedDataParser(featureRepository, glycanRepository, linkerRepository);
                try {
                    Resource resource = resourceLoader.getResource("classpath:sequenceMap.txt");
                    if (!resource.exists()) {
                        ErrorMessage errorMessage = new ErrorMessage();
                        errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                        errorMessage.addError(new ObjectError("mapFile", "NotValid"));
                        throw new IllegalArgumentException("Mapping file cannot be found in resources", errorMessage);
                    }
                    ProcessedData processedData = parser.parse(excelFile.getAbsolutePath(), resource.getFile().getAbsolutePath(), config, user);
                    ArrayDataset dataset = new ArrayDataset();
                    dataset.setName(datasetName);
                    // TODO retrieve dataset from repository and update (add processed data)
                    // TODO get Sample from repository
                    //dataset.setSample(sample);
                    dataset.setProcessedData(processedData);
                    return datasetRepository.addArrayDataset(dataset, user);   
                } catch (InvalidFormatException | IOException | SparqlException | SQLException e)  {
                    ErrorMessage errorMessage = new ErrorMessage();
                    errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
                    errorMessage.addError(new ObjectError("file", "NotValid"));
                    throw new IllegalArgumentException("File cannot be parsed", errorMessage);
                } catch (IllegalArgumentException e) {
                    throw e;
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
    
    @ApiOperation(value = "Add given array dataset for the user")
    @RequestMapping(value="/addDataset", method = RequestMethod.POST, 
            consumes={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="return id for the newly added dataset"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to register datasets"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public String addArrayDataset (
            @ApiParam(required=true, value="Array dataset to be added") 
            @RequestBody ArrayDataset dataset, Principal p) {
        
        UserEntity user = userRepository.findByUsernameIgnoreCase(p.getName());
        //TODO do validation on other fields such as Sample, rawdata, processeddata
        if (dataset.getName() == null || dataset.getName().isEmpty()) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setErrorCode(ErrorCodes.INVALID_INPUT);
            errorMessage.setStatus(HttpStatus.BAD_REQUEST.value());
            errorMessage.addError(new ObjectError("name", "NoEmpty"));
            throw new IllegalArgumentException("Invalid Input: Not a valid array dataset information", errorMessage);
        }
        
        try {
            return datasetRepository.addArrayDataset(dataset, user);
        } catch (SparqlException | SQLException e) {
            throw new GlycanRepositoryException("Array dataset cannot be added for user " + p.getName(), e);
        }
        
    }
    
    @ApiOperation(value = "Retrieve list of templates for the given type")
    @RequestMapping(value="/listTemplates", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(code=200, message="Return a list of metadata templates"), 
            @ApiResponse(code=400, message="Invalid request, validation error"),
            @ApiResponse(code=401, message="Unauthorized"),
            @ApiResponse(code=403, message="Not enough privileges to retrieve templates"),
            @ApiResponse(code=415, message="Media type is not supported"),
            @ApiResponse(code=500, message="Internal Server Error")})
    public List<MetadataTemplate> getAllTemplatesByType (
            @ApiParam(required=true, value="Type of the metadatatemplate") 
            @RequestParam("type")
            MetadataTemplateType type, Principal p) {
        
        List<MetadataTemplate> templates = new ArrayList<MetadataTemplate>();
        
        //TODO retrieve them from the repository
        MetadataTemplate sampleTemplate = new MetadataTemplate();
        sampleTemplate.setId("1234567");
        sampleTemplate.setName("Protein Sample Template");
        sampleTemplate.setType(MetadataTemplateType.SAMPLE);
        List<Description> descriptors = new ArrayList<>();
        DescriptorTemplate descriptor = new DescriptorTemplate();
        descriptor.setName ("AA Sequence");
        descriptor.setDescription("Amino acid sequence in fasta format");
        descriptor.setMandatory(true);
        descriptor.setMaxOccurrence(1);
        Namespace namespace = new Namespace();
        namespace.setName("text");
        namespace.setUri("http://www.w3.org/2001/XMLSchema#string");
        descriptor.setNamespace(namespace);
        descriptors.add(descriptor);
        
        DescriptorGroupTemplate descriptorGroup = new DescriptorGroupTemplate();
        descriptorGroup.setName("Database entry");
        descriptorGroup.setMandatory(true);
        descriptorGroup.setMaxOccurrence(Integer.MAX_VALUE);
        descriptorGroup.setDescription("Entry of the protein in a reference database (e.g. Uniprot)");
        List<Description> groupDescriptors = new ArrayList<Description>();
        descriptor = new DescriptorTemplate();
        descriptor.setName ("Database");
        descriptor.setDescription("Name of the database");
        descriptor.setMandatory(true);
        descriptor.setMaxOccurrence(1);
        descriptor.setNamespace(namespace);
        groupDescriptors.add(descriptor);
        descriptor = new DescriptorTemplate();
        descriptor.setName ("Database URL");
        descriptor.setDescription("Web link of the database");
        descriptor.setMandatory(true);
        descriptor.setMaxOccurrence(1);
        descriptor.setNamespace(namespace);
        groupDescriptors.add(descriptor);
        descriptor = new DescriptorTemplate();
        descriptor.setName ("Id");
        descriptor.setDescription("Identifier of the protein in the database");
        descriptor.setMandatory(true);
        descriptor.setMaxOccurrence(1);
        descriptor.setNamespace(namespace);
        groupDescriptors.add(descriptor);
        descriptor = new DescriptorTemplate();
        descriptor.setName ("URL");
        descriptor.setDescription("Web link of the protein in the database");
        descriptor.setMandatory(false);
        descriptor.setMaxOccurrence(1);
        descriptor.setNamespace(namespace);
        groupDescriptors.add(descriptor);
        descriptorGroup.setDescriptors(groupDescriptors);
        
        descriptors.add(descriptorGroup);
        sampleTemplate.setDescriptors(descriptors);
        templates.add(sampleTemplate);
        
        return templates;
    }
}
