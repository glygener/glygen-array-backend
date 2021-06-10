package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.rdf.data.StatisticalMethod;
import org.glygen.array.persistence.rdf.template.DescriptionTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplateType;

public interface MetadataTemplateRepository {
    
    public static String templatePrefix = "http://purl.org/gadr/template#";
    
    void populateTemplateOntology() throws SparqlException;
    
    String getTemplateByName (String name, MetadataTemplateType type) throws SparqlException, SQLException;
    List<MetadataTemplate> getTemplateByType (MetadataTemplateType type) throws SparqlException, SQLException;

    DescriptionTemplate getDescriptionFromURI(String uri) throws SparqlException;

    void deleteTemplates() throws SparqlException;

    MetadataTemplate getTemplateFromURI(String templateURI) throws SparqlException;

    List<StatisticalMethod> getAllStatisticalMethods() throws SparqlException, SQLException;

}
