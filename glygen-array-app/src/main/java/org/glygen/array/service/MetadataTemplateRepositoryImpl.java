package org.glygen.array.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.glygen.array.persistence.rdf.data.StatisticalMethod;
import org.glygen.array.persistence.rdf.template.DescriptionTemplate;
import org.glygen.array.persistence.rdf.template.DescriptorGroupTemplate;
import org.glygen.array.persistence.rdf.template.DescriptorTemplate;
import org.glygen.array.persistence.rdf.template.MandateGroup;
import org.glygen.array.persistence.rdf.template.MetadataTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplateType;
import org.glygen.array.persistence.rdf.template.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(value="sesameTransactionManager") 
public class MetadataTemplateRepositoryImpl implements MetadataTemplateRepository {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    @Autowired
    QueryHelper queryHelper;
    
    @Autowired
    SesameSparqlDAO sparqlDAO;
    
    String prefix = GlygenArrayRepositoryImpl.prefix;
    
    @Override
    public List<StatisticalMethod> getAllStatisticalMethods () throws SparqlException, SQLException {
        List<StatisticalMethod> methods = new ArrayList<StatisticalMethod>();
        
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        queryBuf.append ("FROM <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/data/statistic_method>. \n}");
        
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI graphIRI = f.createIRI(GlygenArrayRepository.DEFAULT_GRAPH);
        
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        for (SparqlEntity sparqlEntity : results) {
            String methodURI = sparqlEntity.getValue("s");
            StatisticalMethod method = new StatisticalMethod();
            method.setUri(methodURI);
            RepositoryResult<Statement> statements = sparqlDAO.getStatements(f.createIRI(methodURI), null, null, graphIRI);
            while (statements.hasNext()) {
                Statement st = statements.next();
                if (st.getPredicate().equals(RDFS.LABEL)) {
                    method.setName(st.getObject().stringValue());
                }
            }
            methods.add(method);
        }
        
        return methods; 
    }

    @Override
    public String getTemplateByName (String label, MetadataTemplateType type) throws SparqlException, SQLException {
        if (label == null || type == null) {
            throw new SparqlException ("Label must be provided");
        }
        List<SparqlEntity> results = queryHelper.retrieveByLabel(label, templatePrefix + type.getLabel(), null);
        if (results.isEmpty()) {
            return null;
        }
        String templateURI = results.get(0).getValue("s");
        return templateURI;
    }

    @Override
    public List<MetadataTemplate> getTemplateByType (MetadataTemplateType type)
            throws SparqlException, SQLException {
        if ( type == null) {
            throw new SparqlException ("Type must be provided");
        }
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        queryBuf.append ("FROM <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ( " ?s rdf:type  <" + templatePrefix + type.getLabel() + ">. \n}");
        
        List<MetadataTemplate> templates = new ArrayList<MetadataTemplate>();
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        for (SparqlEntity sparqlEntity : results) {
            String templateURI = sparqlEntity.getValue("s");
            MetadataTemplate template= getTemplateFromURI(templateURI);
            if (template != null)
                templates.add(template);    
        }
        
        return templates;
    }

    @Override
    public MetadataTemplate getTemplateFromURI(String templateURI) throws SparqlException {
        
        MetadataTemplate templateObject = null;
        
        String graph = GlygenArrayRepositoryImpl.DEFAULT_GRAPH;
        
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI template = f.createIRI(templateURI);
        IRI graphIRI = f.createIRI(graph);
        
        IRI hasDescriptionContext = f.createIRI(templatePrefix + "has_description_context");
        
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(template, null, null, graphIRI);
        if (statements.hasNext()) {
            templateObject = new MetadataTemplate();
            templateObject.setUri(templateURI);
            templateObject.setId(templateURI.substring(templateURI.lastIndexOf("#")+1));
            templateObject.setDescriptors(new ArrayList<DescriptionTemplate>());
        }
        
        while (statements.hasNext()) {
            Statement st = statements.next();
            if (st.getPredicate().equals(RDFS.LABEL)) {
                Value label = st.getObject();
                templateObject.setName(label.stringValue());
            } else if (st.getPredicate().equals(RDFS.COMMENT)) {
                Value comment = st.getObject();
                templateObject.setDescription(comment.stringValue());
            } else if (st.getPredicate().equals(hasDescriptionContext)) {
                Value uriValue = st.getObject();
                DescriptionTemplate description = getDescriptionFromURI(uriValue.stringValue());
                templateObject.getDescriptors().add(description);
            } else if (st.getPredicate().equals(RDF.TYPE)) {
                Value typeValue = st.getObject();
                String typeURI = typeValue.stringValue();
                String type = typeURI.substring(typeURI.indexOf("#")+1);
                templateObject.setType(MetadataTemplateType.forValue(type));
            }
        }
        return templateObject;
    }

    @Override
    public DescriptionTemplate getDescriptionFromURI(String uri) throws SparqlException{
        List<DescriptionTemplate> descriptorList = new ArrayList<>();
        List<DescriptionTemplate> descriptorGroupList = new ArrayList<>();
        
        String graph = GlygenArrayRepositoryImpl.DEFAULT_GRAPH; 
        DescriptionTemplate description = null;
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI descriptionContext = f.createIRI(uri);
        IRI graphIRI = f.createIRI(graph);
        
        // first find the type, simple_description_context vs. complex_description_context
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(descriptionContext, RDF.TYPE, null, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next();
            Value obj = st.getObject();
            if (obj.stringValue().contains ("simple_description_context")) {
                description = new DescriptorTemplate();
                ((DescriptorTemplate) description).setUnits(new ArrayList<String>());
                description.setUri(uri);
                description.setId(uri.substring(uri.lastIndexOf("#")+1));
                break;
            } else if (obj.stringValue().contains ("complex_description_context")){
                description = new DescriptorGroupTemplate();
                description.setUri(uri);
                description.setId(uri.substring(uri.lastIndexOf("#")+1));
                break;
            }
        }
        if (description == null) { // there is a mismatch in the ontology
            return null;
        }
        
        IRI hasDescriptor = f.createIRI(templatePrefix + "has_descriptor");
        IRI hasDescriptionContext = f.createIRI(templatePrefix + "has_description_context");
        IRI cardinality = f.createIRI(templatePrefix + "cardinality");
        IRI isRequired = f.createIRI(templatePrefix + "is_required");
        IRI hasExample = f.createIRI(templatePrefix + "has_example");
        IRI hasUrl = f.createIRI(templatePrefix + "has_url");
        IRI hasNamespace = f.createIRI(templatePrefix + "has_namespace");
        IRI hasFile = f.createIRI(templatePrefix + "has_file");
        IRI hasSelection = f.createIRI(templatePrefix + "has_selection");
        IRI hasGroup = f.createIRI(templatePrefix + "has_mandate_group");
        IRI isMirage = f.createIRI(templatePrefix + "is_mirage");
        IRI isXor = f.createIRI(templatePrefix + "is_xor");
        IRI hasOrder = f.createIRI(templatePrefix + "has_order");
        IRI hasGroupId = f.createIRI(templatePrefix + "has_id");
        IRI allowNotRecorded = f.createIRI(templatePrefix + "allows_not_recorded");
        IRI allowNotApplicable = f.createIRI(templatePrefix + "allows_not_applicable");
        IRI allowReview = f.createIRI(templatePrefix + "allows_review");
        IRI hasUnit = f.createIRI(GlygenArrayRepository.ontPrefix2 + "has_unit_of_measurement");
        
        // get all statements
        statements = sparqlDAO.getStatements(descriptionContext, null, null, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next();
            MandateGroup group = new MandateGroup();
            if (st.getPredicate().equals(RDFS.LABEL)) {
                // complex description context label gives the name of the descriptor group
                description.setName(st.getObject().stringValue());
            } else if (st.getPredicate().equals(RDFS.COMMENT)) {
                description.setDescription(st.getObject().stringValue());
            } else if (st.getPredicate().equals(hasDescriptor)) {
                String descriptorURI = st.getObject().stringValue();
                // get details of the descriptor
                RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(f.createIRI(descriptorURI), null, null, graphIRI);
                while (statements2.hasNext()) {
                    Statement st2 = statements2.next();
                    if (st2.getPredicate().equals(RDFS.LABEL)) {
                        description.setName(st2.getObject().stringValue());
                    } else if (st2.getPredicate().equals(RDFS.COMMENT)) {
                        description.setDescription(st2.getObject().stringValue());
                    } else if (st2.getPredicate().equals(hasNamespace)) { 
                        Namespace namespace = new Namespace();
                        String namespaceURI = st2.getObject().stringValue();
                        List<String> selectionList = new ArrayList<String>();
                        if (namespaceURI.contains("dictionary") || namespaceURI.contains("selection")) {
                            namespace.setUri(namespaceURI);
                            // get details of namespace
                            RepositoryResult<Statement> statements3 = sparqlDAO.getStatements(f.createIRI(namespaceURI), null, null, graphIRI);
                            while (statements3.hasNext()) {
                                Statement st3 = statements3.next();
                                if (st3.getPredicate().equals(RDFS.LABEL)) {
                                    namespace.setName(st3.getObject().stringValue());
                                } else if (st3.getPredicate().equals(hasFile)) {
                                    namespace.setFilename(st3.getObject().stringValue());
                                } else if (st3.getPredicate().equals(hasSelection)) {
                                    String s = st3.getObject().stringValue();
                                    if (!selectionList.contains(s))
                                        selectionList.add(s);
                                }
                            }   
                        } else if (namespaceURI.contains("token")) {
                            namespace.setName("label");
                            namespace.setUri("http://www.w3.org/2001/XMLSchema#token");
                        } else if (namespaceURI.contains("string")) {
                            namespace.setName("text");
                            namespace.setUri("http://www.w3.org/2001/XMLSchema#string");
                        } else if (namespaceURI.contains("double")) {
                            namespace.setName("number");
                            namespace.setUri("http://www.w3.org/2001/XMLSchema#double");
                        } else if (namespaceURI.contains("date")) {
                            namespace.setName("date");
                            namespace.setUri("http://www.w3.org/2001/XMLSchema#date");
                        } else if (namespaceURI.contains("boolean")) {
                            namespace.setName("boolean");
                            namespace.setUri("http://www.w3.org/2001/XMLSchema#boolean");
                        }
                        if (description instanceof DescriptorTemplate) {
                            ((DescriptorTemplate) description).setNamespace(namespace);
                            if (!selectionList.isEmpty())
                                ((DescriptorTemplate) description).setSelectionList(selectionList);
                        }
                        else {
                            logger.warn("descriptor group should not have a namespace: " + description.getId());
                        }
                    }
                }               
            } else if (st.getPredicate().equals(hasDescriptionContext)) {
                String descriptionContextURI = st.getObject().stringValue();
                // get sub descriptions
                DescriptionTemplate child = getDescriptionFromURI(descriptionContextURI);
                if (child.isGroup()) {
                    descriptorGroupList.add(child);
                } else {
                    descriptorList.add(child);
                }
            } else if (st.getPredicate().equals(cardinality)) {
                String value = st.getObject().stringValue();
                if (value.equalsIgnoreCase("n")) {
                    description.setMaxOccurrence(Integer.MAX_VALUE);
                } else {
                    description.setMaxOccurrence(1);
                }
            } else if (st.getPredicate().equals(isRequired)) {
                String value = st.getObject().stringValue();
                description.setMandatory(value.equalsIgnoreCase("true"));
            } else if (st.getPredicate().equals(allowNotRecorded)) {
                String value = st.getObject().stringValue();
                description.setAllowNotRecorded(value.equalsIgnoreCase("true"));
            } else if (st.getPredicate().equals(allowNotApplicable)) {
                String value = st.getObject().stringValue();
                description.setAllowNotApplicable(value.equalsIgnoreCase("true"));
            } else if (st.getPredicate().equals(allowReview)) {
                String value = st.getObject().stringValue();
                description.setReview(value.equalsIgnoreCase("true"));
            } else if (st.getPredicate().equals(hasExample)) {
                description.setExample(st.getObject().stringValue());
            } else if (st.getPredicate().equals(hasUrl)) {
                description.setWikiLink(st.getObject().stringValue());
            } else if (st.getPredicate().equals(hasUnit)) {
                ((DescriptorTemplate) description).getUnits().add(st.getObject().stringValue());
            } else if (st.getPredicate().equals(isMirage)) {
                String value = st.getObject().stringValue();
                description.setMirage(value.equalsIgnoreCase("true"));
            } else if (st.getPredicate().equals(hasGroup)) {
                String value = st.getObject().stringValue();
                description.setMandateGroup(group);
                RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(f.createIRI(value), null, null, graphIRI);
                while (statements2.hasNext()) {
                    Statement st2 = statements2.next();
                    if (st2.getPredicate().equals(hasGroupId)) {
                        value = st2.getObject().stringValue();
                        group.setId(Integer.parseInt(value));
                    }
                    else if (st2.getPredicate().equals(RDFS.LABEL)) {
                        value = st2.getObject().stringValue();
                        group.setName(value);
                    }
                    else if (st2.getPredicate().equals(isXor)) {
                        value = st2.getObject().stringValue();
                        group.setxOrMandate(value.equalsIgnoreCase("true"));
                    }
                }
            } else if (st.getPredicate().equals(hasExample)) {
                description.setExample(st.getObject().stringValue());
            } else if (st.getPredicate().equals(hasOrder)) {
                String value = st.getObject().stringValue();
                try {
                    description.setOrder(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    logger.warn("order is invalid", e);
                }
            }
        }
        
        if (description.isGroup()) {
            ((DescriptorGroupTemplate) description).setDescriptors(new ArrayList<DescriptionTemplate>());
            ((DescriptorGroupTemplate) description).getDescriptors().addAll(descriptorList);
            ((DescriptorGroupTemplate) description).getDescriptors().addAll(descriptorGroupList);
        }
        
        return description;
    }

    @Override
    public void populateTemplateOntology() throws SparqlException {
        // load the model from "ontology/gadr-template-individuals.owl" and put the triples into the repository
        try {
            InputStream inputStream1 = new FileInputStream(new File("ontology/gadr-data.owl"));
            Model model = Rio.parse(inputStream1, "http://purl.org/gadr/gadr", RDFFormat.RDFXML);
            Iterator<Statement> itr = model.iterator();
            List<Statement> statements = new ArrayList<Statement>();
            while (itr.hasNext()) {
                Statement st = itr.next();
                statements.add(st);
            }
            InputStream inputStream = new FileInputStream(new File("ontology/gadr-template-individuals.owl"));
            model = Rio.parse(inputStream, "http://purl.org/gadr/template", RDFFormat.RDFXML);
            itr = model.iterator();
            while (itr.hasNext()) {
                Statement st = itr.next();
                statements.add(st);
            }
            ValueFactory f = sparqlDAO.getValueFactory();
            sparqlDAO.addStatements(statements, f.createIRI(GlygenArrayRepositoryImpl.DEFAULT_GRAPH) );
        } catch (IOException e) {
            throw new SparqlException ("Cannot read ontology file", e);
        }
        
    }
    
    public void deleteTemplate (String uri) throws SparqlException {
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI template = f.createIRI(uri);
        IRI graphIRI = f.createIRI(GlygenArrayRepository.DEFAULT_GRAPH);
        IRI hasDescriptionContext = f.createIRI(templatePrefix + "has_description_context");
        
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(template, hasDescriptionContext, null, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next();
            Value v = st.getObject();
            String contextURI = v.stringValue();
            deleteDescriptionContext(contextURI);
        }
        
        statements = sparqlDAO.getStatements(template, null, null, graphIRI);
        sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI);
    }
    
    public void deleteDescriptionContext (String uri) throws RepositoryException, SparqlException {
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI graphIRI = f.createIRI(GlygenArrayRepository.DEFAULT_GRAPH);
        IRI context = f.createIRI(uri);
        IRI hasDescriptor = f.createIRI(templatePrefix + "has_descriptor");
        IRI hasNamespace = f.createIRI(templatePrefix + "has_namespace");
        IRI hasDescriptionContext = f.createIRI(templatePrefix + "has_description_context");
        RepositoryResult<Statement> statements = sparqlDAO.getStatements(context, hasDescriptionContext, null, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next();
            Value v = st.getObject();
            String contextURI = v.stringValue();
            deleteDescriptionContext (contextURI);
        }
        statements = sparqlDAO.getStatements(context, hasDescriptor, null, graphIRI);
        while (statements.hasNext()) {
            Statement st = statements.next();
            Value v = st.getObject();
            String descriptorURI = v.stringValue();
            IRI descriptor = f.createIRI(descriptorURI);
            RepositoryResult<Statement> statements2 = sparqlDAO.getStatements(descriptor, hasNamespace, null, graphIRI);
            while (statements2.hasNext()) {
                Statement st2 = statements2.next();
                Value v2 = st2.getObject();
                IRI namespace = f.createIRI(v2.stringValue());
                RepositoryResult<Statement> statements3 = sparqlDAO.getStatements(namespace, null, null, graphIRI);
                sparqlDAO.removeStatements(Iterations.asList(statements3), graphIRI);
            }
            statements2 = sparqlDAO.getStatements(descriptor, null, null, graphIRI);
            sparqlDAO.removeStatements(Iterations.asList(statements2), graphIRI); 
        }
        statements = sparqlDAO.getStatements(context, null, null, graphIRI);
        sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI); 
        
    }

    @Override
    public void deleteTemplates() throws SparqlException {    
        for (MetadataTemplateType type: MetadataTemplateType.values()) {
            StringBuffer queryBuf = new StringBuffer();
            queryBuf.append (prefix + "\n");
            queryBuf.append ("SELECT DISTINCT ?s \n");
            queryBuf.append ("FROM <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n");
            queryBuf.append ("WHERE {\n");
            queryBuf.append ( " ?s rdf:type  <" + templatePrefix  + type.getLabel() + ">. \n}");
            
            List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
            for (SparqlEntity sparqlEntity : results) {
                String templateURI = sparqlEntity.getValue("s");
                deleteTemplate(templateURI);
            }   
        }
        
        // to clean up left over predicates
        // get all description context objects and delete their statements
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        queryBuf.append ("FROM <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ( " ?s <" +templatePrefix + "is_required> ?p. \n}");
        ValueFactory f = sparqlDAO.getValueFactory();
        IRI graphIRI = f.createIRI(GlygenArrayRepository.DEFAULT_GRAPH);
        List<SparqlEntity> results = sparqlDAO.query(queryBuf.toString());
        for (SparqlEntity sparqlEntity : results) {
            String uri = sparqlEntity.getValue("s");
            RepositoryResult<Statement> statements = sparqlDAO.getStatements(f.createIRI(uri), null, null, graphIRI);
           /* while (statements.hasNext()) {
                Statement st = statements.next();
                logger.info("in delete templates: statement -" + st.getPredicate());
            }*/
            sparqlDAO.removeStatements(Iterations.asList(statements), graphIRI); 
        }     
    }

}
