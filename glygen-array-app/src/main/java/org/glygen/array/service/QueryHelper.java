package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(value="sesameTransactionManager") 
public class QueryHelper {
    
    @Autowired
    SesameSparqlDAO sparqlDAO;
    
    String prefix = GlygenArrayRepositoryImpl.prefix;
    
    
    public List<SparqlEntity> retrieveGlycansByLabel (String label, String graph) throws SparqlException {
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        queryBuf.append ("FROM <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n");
        if (graph != null) queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ( " ?s gadr:has_date_addedtolibrary ?d . \n");
        queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/data#Glycan>. \n");
        queryBuf.append ( " {?s rdfs:label \"" + label + "\"^^xsd:string . \n }");
        queryBuf.append ( " UNION {?s gadr:has_alias \"" + label + "\"^^xsd:string . \n }");
        queryBuf.append ( "}\n");
        return sparqlDAO.query(queryBuf.toString());
    }
    
    public List<SparqlEntity> retrieveGlycanTypeByGlycan(String glycanURI, String graph) throws SparqlException{
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?t \n");
        queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ("<" +  glycanURI + "> gadr:has_type ?t . }");
    
        return sparqlDAO.query(queryBuf.toString());
    }
    
    public String getSearchPredicate (String searchValue) {
        String predicates = "";
        
        predicates += "?s rdfs:label ?value1 .\n";
        predicates += "OPTIONAL {?s gadr:has_internal_id ?value2} \n";
        predicates += "OPTIONAL {?s rdfs:comment ?value3} \n";
        predicates += "OPTIONAL {?s gadr:has_alias ?value4} \n";
        predicates += "OPTIONAL {?s gadr:has_glytoucan_id ?value5} \n";
        predicates += "OPTIONAL {?s gadr:has_mass ?value6} \n";
        
        String filterClause = "filter (";
        for (int i=1; i < 7; i++) {
            filterClause += "regex (str(?value" + i + "), '" + searchValue + "', 'i')";
            if (i + 1 < 7)
                filterClause += " || ";
        }
        filterClause += ")\n";
            
        predicates += filterClause;
        return predicates;
    }
    
    protected String getSortPredicate(String field) {
        if (field == null || field.equalsIgnoreCase("name")) 
            return "rdfs:label";
        else if (field.equalsIgnoreCase("comment")) 
            return "rdfs:comment";
        else if (field.equalsIgnoreCase("glytoucanId"))
            return "gadr:has_glytoucan_id";
        else if (field.equalsIgnoreCase("internalId"))
            return "gadr:has_internal_id";
        else if (field.equalsIgnoreCase("dateModified"))
            return "gadr:has_date_modified";
        else if (field.equalsIgnoreCase("mass"))
            return "gadr:has_mass";
        else if (field.equalsIgnoreCase("id"))
            return null;
        return null;
    }

    public List<SparqlEntity> retrieveGlycanByUser(int offset, int limit, String field, int order, String searchValue, String graph) throws SparqlException {
        String sortPredicate = getSortPredicate (field);
        
        String searchPredicate = "";
        if (searchValue != null)
            searchPredicate = getSearchPredicate(searchValue);
        
        String sortLine = "";
        if (sortPredicate != null)
            sortLine = "OPTIONAL {?s " + sortPredicate + " ?sortBy } .\n";  
        String orderByLine = " ORDER BY " + (order == 0 ? "DESC" : "ASC") + (sortPredicate == null ? "(?s)": "(?sortBy)");  
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        queryBuf.append ("FROM <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n");
        queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append (
                " ?s gadr:has_date_addedtolibrary ?d .\n" +
                " ?s rdf:type  <http://purl.org/gadr/data#Glycan>. \n" +
                        sortLine + searchPredicate + 
                "}\n" +
                 orderByLine + 
                ((limit == -1) ? " " : " LIMIT " + limit) +
                " OFFSET " + offset);
        
        return sparqlDAO.query(queryBuf.toString());
    }
    
    public List<SparqlEntity> retrieveGlycanByInternalId(String internalId, String graph) throws SparqlException {
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        queryBuf.append ("FROM <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n");
        queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ( " ?s gadr:has_date_addedtolibrary ?d . \n");
        queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/data#Glycan>. \n");
        queryBuf.append ( " ?s gadr:has_internal_id \"" + internalId + "\"^^xsd:string . \n"
                + "}\n");
        return sparqlDAO.query(queryBuf.toString());
    }
    
    public List<SparqlEntity> retrieveGlycanById(String glycanId, String graph) throws SparqlException, SQLException {
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?d \n");
        queryBuf.append ("FROM <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n");
        queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ( "<" +  GlygenArrayRepository.uriPrefix + glycanId + "> gadr:has_date_addedtolibrary ?d . }\n");
        return sparqlDAO.query(queryBuf.toString());
    }
    
   public List<SparqlEntity> findGlycanInGraphBySequence (String sequence, String graph) throws SparqlException {
        String fromString = "FROM <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n";
        String whereClause = "WHERE {";
        String where = " { " + 
                "                   ?s gadr:has_sequence ?o .\n" +
                "                    ?o gadr:has_sequence_value \"\"\"" + sequence + "\"\"\"^^xsd:string .\n";
        if (!graph.equals(GlygenArrayRepository.DEFAULT_GRAPH)) {
            // check if the user's private graph has this glycan
            fromString += "FROM <" + graph + ">\n";
            where += "              ?s gadr:has_date_addedtolibrary ?d .\n }";
            where += "  UNION { ?s gadr:has_date_addedtolibrary ?d .\n"
                    + " ?s gadr:has_public_uri ?p . \n" 
                    + " ?p gadr:has_sequence ?o . \n"
                    + " ?o gadr:has_sequence_value \"\"\"" + sequence + "\"\"\"^^xsd:string . \n}";
            
        } else {
            where += "}";
        }
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s ?o\n");
        queryBuf.append (fromString);
        queryBuf.append (whereClause + where + 
                "               }\n" + 
                "               LIMIT 10");
        return sparqlDAO.query(queryBuf.toString());
   }
   
   public List<SparqlEntity> canDeleteQuery (String glycanURI, String graph) throws SparqlException { 
       StringBuffer queryBuf = new StringBuffer();
       queryBuf.append (prefix + "\n");
       queryBuf.append ("SELECT DISTINCT ?s \n");
       queryBuf.append ("FROM <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n");
       queryBuf.append ("FROM <" + graph + ">\n");
       queryBuf.append ("WHERE {\n");
       queryBuf.append ("?s gadr:has_molecule  <" +  glycanURI + "> . } LIMIT 1");
       
       return sparqlDAO.query(queryBuf.toString());
   }
}