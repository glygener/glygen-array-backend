package org.glygen.array.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.template.MetadataTemplate;
import org.glygen.array.persistence.rdf.template.MetadataTemplateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(value="sesameTransactionManager") 
public class MetadataTemplateRepositoryImpl implements MetadataTemplateRepository {
    
    @Autowired
    QueryHelper queryHelper;
    
    @Autowired
    SesameSparqlDAO sparqlDAO;
    
    String prefix = GlygenArrayRepositoryImpl.prefix;

    public String getTemplateByName (String label) throws SparqlException, SQLException {
        List<SparqlEntity> results = queryHelper.retrieveByLabel(label, GlygenArrayRepository.ontPrefix + "SampleTemplate", GlygenArrayRepository.DEFAULT_GRAPH);
        if (results.isEmpty()) {
            return null;
        }
        String templateURI = results.get(0).getValue("s");
        return templateURI;
    }

    @Override
    public List<MetadataTemplate> getTemplateByType(String name, MetadataTemplateType type)
            throws SparqlException, SQLException {
        
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        queryBuf.append ("FROM <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ( " ?s rdf:type  <" + GlygenArrayRepository.ontPrefix + type.getLabel() + ">. \n}");
        
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

    public MetadataTemplate getTemplateFromURI(String templateURI) {
        // TODO Auto-generated method stub
        return null;
    }

}
