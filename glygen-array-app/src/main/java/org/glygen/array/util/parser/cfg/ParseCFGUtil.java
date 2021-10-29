package org.glygen.array.util.parser.cfg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ParseCFGUtil {
    
    long dataId = 1;
    long linkId = 1;
    
    Set<String> sampleColumns = new HashSet<String>();
    Set<String> experimentColumns = new HashSet<String>();
    Set<String> requestColumns = new HashSet<String>();
    
    
    public List<String> parse (String filename, String version) {
        Map<String, String> sampleColumnMap = new HashMap<String, String>();
        Map<String, String> experimentColumnMap = new HashMap<String, String>();
        Map<String, String> requestColumnMap = new HashMap<String, String>();
        
        ObjectMapper mapper = new ObjectMapper();
        List<String> sqlCommands = new ArrayList<String>();
        
        //if (version.contains("5.2") || version.contains("5.1") || version.contains("5.0")) {
            try {
                PrintStream columnFile = new PrintStream(new FileOutputStream("cfg" + version + "columns.txt" ));
                InputStream is = new FileInputStream(new File(filename));
                CFG52Model[] rows = mapper.readValue(is, CFG52Model[].class);
                //CFG52 model = mapper.readValue(is, new TypeReference<List<CFG52Model>>(){});
                String schema = "cfg_" + version.substring(0, 1) + "_" + version.substring(2);
                sqlCommands.add("CREATE SCHEMA IF NOT EXISTS "+ schema + ";\n");
                sqlCommands.add("CREATE TABLE IF NOT EXISTS \"" + schema + "\".experiment(\n");
                sqlCommands.add("id bigint primary key, \n " + 
                        "primscreen varchar(256),\n" + 
                        "sample_name varchar(256),\n" + 
                        "species  varchar(256),\n" + 
                        "protein_family varchar(256) ,\n" + 
                        "investigator  varchar(256), \n" +
                        "request varchar(256), \n" +
                        "date Date, \n" +
                        "sample_data_id bigint, \n"+
                        "experiment_data_id bigint, \n" +
                        "request_data_id bigint, \n" +
                        "filename varchar(256) );\n");
                
                sqlCommands.add("CREATE TABLE IF NOT EXISTS \"" + schema + "\".experiment_link(\n");
                sqlCommands.add("experiment_id bigint,\n" + 
                        "link_id bigint );\n");
                
                // go through data parts to determine the columns for the those data tables
                
                for (CFG52Model entry: rows) {
                    DataWithTitle sampleData = entry.getSampleData();
                    if (sampleData != null && sampleData.getData() != null) {
                        sampleColumns.addAll(findColumns (sampleData.getData(), sampleColumnMap));
                    }
                    DataWithTitle experimentData = entry.getExperimentData();
                    if (experimentData != null && experimentData.getData() != null) {
                        experimentColumns.addAll(findColumns (experimentData.getData(), experimentColumnMap));
                    }
                    DataWithTitle requestData = entry.getRequestData();
                    if (requestData != null && requestData.getData() != null) {
                        requestColumns.addAll(findColumns (requestData.getData(), requestColumnMap));
                    }
                }
                
                // print out the column maps
                columnFile.println("sampledata");
                for (String key: sampleColumnMap.keySet()) {
                    columnFile.println (key + "\t" + sampleColumnMap.get(key));
                }
                columnFile.println();
                
                columnFile.println("experimentdata");
                for (String key: experimentColumnMap.keySet()) {
                    columnFile.println (key + "\t" + experimentColumnMap.get(key));
                }
                columnFile.println();
                columnFile.println("requestdata");
                for (String key: requestColumnMap.keySet()) {
                    columnFile.println (key + "\t" + requestColumnMap.get(key));
                }
                
                columnFile.close();
                  
                sqlCommands.add("CREATE TABLE IF NOT EXISTS \"" + schema + "\".sampledata(\n");
                sqlCommands.add("id bigint primary key,\n");
                sqlCommands.add("title text,\n");
                int i=0;
                for (String col: sampleColumns) {
                    sqlCommands.add("\"" + col + "\" text");
                    if (i < sampleColumns.size()-1) 
                        sqlCommands.add(",\n");
                    i++;
                }
                sqlCommands.add("\n);\n");
                
                sqlCommands.add("CREATE TABLE IF NOT EXISTS \"" + schema + "\".experimentdata(\n");
                sqlCommands.add("id bigint primary key,\n");
                sqlCommands.add("title text,\n");
                i=0;
                for (String col: experimentColumns) {
                    sqlCommands.add("\"" + col + "\" text");
                    if (i < experimentColumns.size()-1) 
                        sqlCommands.add(",\n");
                    i++;
                }
                sqlCommands.add("\n);\n");
                
                sqlCommands.add("CREATE TABLE IF NOT EXISTS \"" + schema + "\".requestdata(\n");
                sqlCommands.add("id bigint primary key,\n");
                sqlCommands.add("title text,\n");
                i=0;
                for (String col: requestColumns) {
                    sqlCommands.add("\"" + col + "\" text");
                    if (i < requestColumns.size()-1) 
                        sqlCommands.add(",\n");
                    i++;
                }
                sqlCommands.add("\n);\n");
                
                sqlCommands.add("CREATE TABLE \"" + schema + "\".link(\n");
                sqlCommands.add("id bigint primary key, \n" +
                        "linkText varchar(256),\n" +
                        "href varchar(256),\n" +
                        "parentCell  INT);\n");
                
                
                // insert data
                long experimentId = 1;
                for (CFG52Model entry: rows) {
                    sqlCommands.addAll(addRow (entry, experimentId, schema));
                    experimentId ++;
                }
                
                
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }   
       //}
        
        return sqlCommands;
    }

    private List<String> addRow(CFG52Model entry, long experimentId, String schema) {
        List<String> inserts = new ArrayList<String>();
        inserts.add(addData(entry.sampleData, schema + ".sampledata"));
        inserts.add(addData(entry.experimentData, schema + ".experimentdata"));
        inserts.add(addData(entry.requestData, schema + ".requestdata"));
        for (Link l: entry.getLinks())
            inserts.add(addLink(l, experimentId, schema));
        
        String insert = "insert into \"" + schema + "\".experiment (id, primscreen, sample_name, "
                + "species, protein_family, investigator, request, date, sample_data_id, experiment_data_id, request_data_id, filename) values (";
        insert += experimentId + ", '" + entry.primscreen + "', E'" + entry.sampleName.replace("'", "\\'") + "', E'" + entry.species.replace("'", "\\'");
        insert += "', E'" + entry.proteinFamily.replace("'", "\\'") + "', E'" + entry.investigator.replace("'", "\\'") + "', E'" +
                entry.request.replace("'", "\\'") + "', '" + entry.date + "', " +
                this.dataId + ", " + this.dataId + ", " + this.dataId + ", E'" + entry.getFilename().replace("'", "\\'") + "');\n";
        inserts.add(insert + "\n\n");
        
        this.dataId ++;
        
        return inserts;
        
    }
    
    private String addData (DataWithTitle row, String tablename) {
        List<String> processedColumns = new ArrayList<String>();
        List<Integer> skipList = new ArrayList<Integer>();
        if (row.getData() != null) {
            String insert = "insert into " + tablename + " (id, title, ";
            int i=0;
            for (Data col: row.getData()) {
                String columnName = formatColumnName(col.getKey());
                if (!processedColumns.contains(columnName)) {
                    processedColumns.add(columnName);
                    insert += "\"" + columnName + "\"";
                    if (i < row.getData().size()-1) {
                        insert += ", ";
                    }
                    
                } else {
                    System.err.println ("Skipping " + columnName + " in table " + tablename);
                    skipList.add(i);
                }
                i++;
            }
            if (insert.endsWith(", ")) // remove the last comma
                insert = insert.substring(0, insert.length() - 2);
            insert += ") values (" + this.dataId + ", E'" + row.getTitle().replace("'", "\\'") + "', E'";
            i=0;
            for (Data col: row.getData()) {
                if (!skipList.contains(i)) {
                    String value = col.getValue().replace("'", "\\'");
                    insert += value;
                    if (i < row.getData().size()-1) {
                        insert += "', E'";
                    }
                    
                } 
                i++;
            }
            if (insert.endsWith(", ")) // remove the last comma
                insert = insert.substring(0, insert.length() - 2);
            insert += "');\n";
            
            
            return insert;
        } 
        return "";
    }
    
    private String addLink (Link row, long experimentId, String schema) {
        String insert = "insert into \"" + schema + "\".link (id, linkText, href, parentCell) values (";
        insert += linkId + ", E'" + row.linkText.replace("'", "\\'") + "', E'" + row.href.replace("'", "\\'") + "', " + row.parentCell + ");\n";
        insert += "insert into \"" + schema + "\".experiment_link(experiment_id, link_id) values (" +
                experimentId + ", " + this.linkId + ");\n";      
        this.linkId ++;
        return insert;
    }

    private Set<String> findColumns(List<Data> data, Map<String, String> columnMap) {
        Set<String> columnSet = new HashSet<String>();
        for (Data d: data) {
            String col = formatColumnName (d.getKey());
            columnSet.add(col);
            columnMap.put (col, d.getKey());
        }
        
        return columnSet;
    }
    
    private String formatColumnName (String columnName) {
        columnName = columnName.replace(" ", "_");
        columnName = columnName.replace("/", "_");
        if (columnName.length() > 20) {
            columnName = "" + columnName.hashCode();
        }
        return columnName;
    }
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("usage: filename version");
            System.exit(1);
        }
        String filename = args[0];
        String version = args[1].trim();
        try {
            PrintStream outputFile = new PrintStream(new FileOutputStream("cfg" + version + ".sql" ));
            List<String> lines = new ParseCFGUtil().parse(filename, version);
            for (String line: lines) {
                outputFile.print(line);
            }
            outputFile.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
