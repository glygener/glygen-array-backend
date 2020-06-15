package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.rdf.template.MetadataTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplateType;

public interface MetadataTemplateRepository {
    
    String getTemplateByName (String name) throws SparqlException, SQLException;
    List<MetadataTemplate> getTemplateByType (String name, MetadataTemplateType type) throws SparqlException, SQLException;

}
