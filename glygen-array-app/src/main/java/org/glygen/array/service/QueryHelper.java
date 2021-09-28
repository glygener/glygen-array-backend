package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.SparqlEntity;
import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(value="sesameTransactionManager") 
public class QueryHelper {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    @Autowired
    SesameSparqlDAO sparqlDAO;
    
    String prefix = GlygenArrayRepositoryImpl.prefix;
    
    
    public List<SparqlEntity> retrieveByLabel (String label, String type, String graph) throws SparqlException {
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        queryBuf.append ("FROM <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n");
        if (graph != null) queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        if (graph != null) queryBuf.append ( " ?s gadr:has_date_addedtolibrary ?d . \n");
        queryBuf.append ( " ?s rdf:type  <" + type + ">. \n");
        queryBuf.append ( " {?s rdfs:label ?l FILTER (lcase(str(?l)) = \"\"\"" + label.toLowerCase() + "\"\"\") \n }");
        queryBuf.append ( " UNION {?s gadr:has_alias ?a FILTER (lcase(str(?a)) = \"\"\"" + label.toLowerCase() + "\"\"\") \n }");
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
    
    public String getSearchPredicate (String searchValue, String queryVariable) {
        String predicates = "";
        
        predicates += "OPTIONAL {" + queryVariable + " rdfs:label ?value1 }.\n";
        predicates += "OPTIONAL {" + queryVariable + " gadr:has_internal_id ?value2} \n";
        predicates += "OPTIONAL {" + queryVariable + " rdfs:comment ?value3} \n";
        predicates += "OPTIONAL {" + queryVariable + " gadr:has_alias ?value4} \n";
        predicates += "OPTIONAL {" + queryVariable + " gadr:has_glytoucan_id ?value5} \n";
        predicates += "OPTIONAL {" + queryVariable + " gadr:has_mass ?value6} \n";
       // predicates += "OPTIONAL {" + queryVariable + " gadr:has_type ?value7} \n";
        
        
        int numberOfValues = 7;
        String filterClause = "filter (";
        for (int i=1; i < numberOfValues; i++) {
            filterClause += "regex (str(?value" + i + "), '" + searchValue + "', 'i')";
            if (i + 1 < numberOfValues)
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
        else if (field.equalsIgnoreCase("type"))
            return "gadr:has_type";
        else if (field.equalsIgnoreCase("id"))
            return null;
        return null;
    }

    public List<SparqlEntity> retrieveGlycanByUser(int offset, int limit, String field, int order, String searchValue, String graph) throws SparqlException {
        String sortPredicate = getSortPredicate (field);
        
        String searchPredicate = "";
        String publicSearchPredicate = "";
        if (searchValue != null) {
            searchPredicate = getSearchPredicate(searchValue, "?s");
            publicSearchPredicate = getSearchPredicate(searchValue, "?public");
        }
        
        String sortLine = "";
        String publicSortLine = "";
        if (sortPredicate != null) {
            sortLine = "OPTIONAL {?s " + sortPredicate + " ?sortBy } .\n";  
            sortLine += "filter (bound (?sortBy) or !bound(?public) ) . \n";
            publicSortLine = "OPTIONAL {?public " + sortPredicate + " ?sortBy } .\n";  
        }
        
        
        String orderByLine = " ORDER BY " + (order == 0 ? "DESC" : "ASC") + (sortPredicate == null ? "(?s)": "(?sortBy)");  
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s");
        if (sortPredicate != null) {
            //queryBuf.append(", ?sortBy");
        }
        queryBuf.append ("\nFROM <" + graph + ">\n");
        if (!graph.equals(GlygenArrayRepository.DEFAULT_GRAPH))  {
            queryBuf.append ("FROM NAMED <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n");
        }
        queryBuf.append ("WHERE {\n {\n");
        queryBuf.append (
                " ?s gadr:has_date_addedtolibrary ?d .\n" +
                " ?s rdf:type  <http://purl.org/gadr/data#Glycan>. \n");
        if (!graph.equals(GlygenArrayRepository.DEFAULT_GRAPH))  {
            queryBuf.append("OPTIONAL {?s gadr:has_subtype ?subtype } .  \n");
            queryBuf.append("FILTER (!bound(?subtype) || str(?subtype) = \"BASE\") ");
        }
        queryBuf.append(
                " OPTIONAL {?s gadr:has_public_uri ?public  } .\n" + 
                        sortLine + searchPredicate + 
                "}\n" );
         if (!graph.equals(GlygenArrayRepository.DEFAULT_GRAPH))  {             
             queryBuf.append ("UNION {" +
                "?s gadr:has_public_uri ?public . \n" +
                "GRAPH <" + GlygenArrayRepository.DEFAULT_GRAPH + "> {\n" +
                " ?public rdf:type  <http://purl.org/gadr/data#Glycan>. \n" +
                "OPTIONAL {?public gadr:has_subtype ?subtype } .  \n" +
                "FILTER (!bound(?subtype) || str(?subtype) = \"BASE\") " +
                    publicSortLine + publicSearchPredicate + 
                "}}\n"); 
         }
         queryBuf.append ("}" + 
                 orderByLine + 
                ((limit == -1) ? " " : " LIMIT " + limit) +
                " OFFSET " + offset);
        
       // logger.info("Glycan query: " + queryBuf.toString());
        return sparqlDAO.query(queryBuf.toString());
    }
    
    public List<SparqlEntity> retrieveGlycanByInternalId(String internalId, String graph) throws SparqlException {
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?s \n");
        //queryBuf.append ("FROM <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n");
        queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ( " ?s gadr:has_date_addedtolibrary ?d . \n");
        queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/data#Glycan>. \n");
        queryBuf.append ( " ?s gadr:has_internal_id ?l FILTER (lcase(str(?l)) = \"\"\"" + internalId.toLowerCase() + "\"\"\") \n"
                + "}\n");
        
        return sparqlDAO.query(queryBuf.toString());
    }
    
    public List<SparqlEntity> retrieveById(String uri, String graph) throws SparqlException, SQLException {
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append (prefix + "\n");
        queryBuf.append ("SELECT DISTINCT ?d \n");
        //queryBuf.append ("FROM <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n");
        queryBuf.append ("FROM <" + graph + ">\n");
        queryBuf.append ("WHERE {\n");
        queryBuf.append ( "<" +  uri + "> gadr:has_date_addedtolibrary ?d . }\n");
        return sparqlDAO.query(queryBuf.toString());
    }
   
    
    public List<SparqlEntity> findGlycanInGraphBySequence (String sequence, String graph) throws SparqlException {
        String singleLineSequence = sequence.replace("\n", "\\n");
        // remove the \n at the end
        String singleLineSequence2 = singleLineSequence.substring(0, singleLineSequence.length()-2 );
        //System.out.println("seqeunce " + singleLineSequence);
        String fromString = "FROM <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n";
        String whereClause = "WHERE {";
        String where = " { " + 
                "                   ?s gadr:has_sequence ?o .\n" +
                "                    {?o gadr:has_sequence_value \"\"\"" + singleLineSequence + "\"\"\"^^xsd:string } "
                        + "UNION { ?o gadr:has_sequence_value \"\"\"" + singleLineSequence2 + "\"\"\"^^xsd:string } . \n";
        if (!graph.equals(GlygenArrayRepository.DEFAULT_GRAPH)) {
            // check if the user's private graph has this glycan
            fromString += "FROM <" + graph + ">\n";
            where += "              ?s gadr:has_date_addedtolibrary ?d .\n }";
            where += "  UNION { ?s gadr:has_date_addedtolibrary ?d .\n"
                    + " ?s gadr:has_public_uri ?p . \n" 
                    + " ?p gadr:has_sequence ?o . \n"
                    + " {?o gadr:has_sequence_value \"\"\"" + singleLineSequence + "\"\"\"^^xsd:string } "
                            + "UNION { ?o gadr:has_sequence_value \"\"\"" + singleLineSequence2 + "\"\"\"^^xsd:string } . \n}";
            
            
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
        
        //System.out.println("query " + queryBuf.toString());
        return sparqlDAO.query(queryBuf.toString());
   }
   
   public List<SparqlEntity> canDeleteQuery (String glycanURI, String graph) throws SparqlException { 
       StringBuffer queryBuf = new StringBuffer();
       queryBuf.append (prefix + "\n");
       queryBuf.append ("SELECT DISTINCT ?s \n");
       //queryBuf.append ("FROM <" + GlygenArrayRepository.DEFAULT_GRAPH + ">\n");
       queryBuf.append ("FROM <" + graph + ">\n");
       queryBuf.append ("WHERE {\n");
       queryBuf.append ("?s gadr:has_molecule  <" +  glycanURI + "> . } LIMIT 1");
       
       return sparqlDAO.query(queryBuf.toString());
   }
   
   
   public List<SparqlEntity> retrieveByListofGlytoucanIds (List<String> ids, int limit, int offset, String field, int order, String graph) throws SparqlException {
       String list = StringUtils.join(ids, "'^^xsd:string, '");
       list = "'" + list + "'^^xsd:string";
       String orderByLine = " ORDER BY " + (order == 0 ? "DESC" : "ASC") +  "(?s)";  
       String sortLine = "";
       String sortPredicate = getSortPredicate (field);
       if (sortPredicate != null) {
           sortLine = "OPTIONAL {?s " + sortPredicate + " ?sortBy } .\n";  
       }
       StringBuffer queryBuf = new StringBuffer();
       queryBuf.append (prefix + "\n");
       queryBuf.append ("SELECT DISTINCT ?s \n");
       queryBuf.append ("FROM <" + graph + ">\n");
       queryBuf.append ("WHERE {\n");
       queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/data#Glycan> . \n");
       queryBuf.append ( " {?s gadr:has_glytoucan_id ?gid FILTER (?gid IN (" + list + ")) } ");
        
       queryBuf.append (sortLine);
       queryBuf.append ("}" + 
               orderByLine + 
              ((limit == -1) ? " " : " LIMIT " + limit) +
              " OFFSET " + offset);
       
       return sparqlDAO.query(queryBuf.toString());
   }
   
   public List<SparqlEntity> retrieveByMassRange (double min, double max, int limit, int offset, String field, int order, String graph) throws SparqlException {
       String orderByLine = " ORDER BY " + (order == 0 ? "DESC" : "ASC") +  "(?s)";  
       
       String sortLine = "";
       String sortPredicate = getSortPredicate (field);
       if (sortPredicate != null) {
           sortLine = "OPTIONAL {?s " + sortPredicate + " ?sortBy } .\n";  
       }
       
       StringBuffer queryBuf = new StringBuffer();
       queryBuf.append (prefix + "\n");
       queryBuf.append ("SELECT DISTINCT ?s \n");
       queryBuf.append ("FROM <" + graph + ">\n");
       queryBuf.append ("WHERE {\n");
       queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/data#Glycan> . \n");
       queryBuf.append ( " {?s gadr:has_mass ?mass FILTER (?mass <" +  max  + " && ?mass > " + min + ") }");
       queryBuf.append (sortLine);
       queryBuf.append ("}" + 
               orderByLine + 
              ((limit == -1) ? " " : " LIMIT " + limit) +
              " OFFSET " + offset);
       
       return sparqlDAO.query(queryBuf.toString());
   }
   
   /**
    * retrieve datasets that uses the given printed slides
    * 
    * @param printedSlideName printed slide name or id to search for
    * @param graph graph to search for
    * @return the sparqlEntity list that contains the uris of the datasets matching the criteria
    * @throws SparqlException
    */
   public List<SparqlEntity> retrieveDatasetBySlideName(String printedSlideName, String graph) throws SparqlException {
       StringBuffer queryBuf = new StringBuffer();
       queryBuf.append (prefix + "\n");
       queryBuf.append ("SELECT DISTINCT ?s \n");
       queryBuf.append ("FROM <" + graph + ">\n");
       queryBuf.append ("WHERE {\n");
       queryBuf.append ( " ?s gadr:has_date_addedtolibrary ?d . \n");
       queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/data#array_dataset>. \n");
       queryBuf.append ( " ?s gadr:has_slide ?slide . ?slide gadr:has_printed_slide ?ps . ?ps rdfs:label ?l"
               + " FILTER (lcase(str(?l)) = \"\"\"" + printedSlideName.toLowerCase() + "\"\"\" "
                       + "|| contains(str(?ps),\""+ printedSlideName + "\") ) \n"
               + "}\n");
       
       return sparqlDAO.query(queryBuf.toString());
   }
   
   /**
    * retrieve datasets that has the given publication
    * 
    * @param printedSlideName printed slide name or id to search for
    * @param graph graph to search for
    * @return the sparqlEntity list that contains the uris of the datasets matching the criteria
    * @throws SparqlException
    */
   public List<SparqlEntity> retrieveDatasetByPublication(String pmid, String graph) throws SparqlException {
       StringBuffer queryBuf = new StringBuffer();
       queryBuf.append (prefix + "\n");
       queryBuf.append ("SELECT DISTINCT ?s \n");
       queryBuf.append ("FROM <" + graph + ">\n");
       queryBuf.append ("WHERE {\n");
       queryBuf.append ( " ?s gadr:has_date_addedtolibrary ?d . \n");
       queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/data#array_dataset>. \n");
       queryBuf.append ( " ?s gadr:has_publication ?pub . ?pub gadr:has_pubmed_id ?pmid . "
               + " FILTER (lcase(str(?pmid)) = \"\"\"" + pmid.toLowerCase() + "\"\"\") \n"
               + "}\n");
       
       return sparqlDAO.query(queryBuf.toString());
   }
   
   
   public List<SparqlEntity> retrieveDatasetByOwner(String username, String graph) throws SparqlException {
       StringBuffer queryBuf = new StringBuffer();
       queryBuf.append (prefix + "\n");
       queryBuf.append ("SELECT DISTINCT ?s \n");
       queryBuf.append ("FROM <" + graph + ">\n");
       queryBuf.append ("WHERE {\n");
       queryBuf.append ( " ?s gadr:has_date_addedtolibrary ?d . \n");
       queryBuf.append ( " ?s rdf:type  <http://purl.org/gadr/data#array_dataset>. \n");
       queryBuf.append ( " ?s gadr:created_by ?owner . "
               + " FILTER (lcase(str(?owner)) = \"\"\"" + username.toLowerCase() + "\"\"\") \n"
               + "}\n");
       
       return sparqlDAO.query(queryBuf.toString());
   }
}
