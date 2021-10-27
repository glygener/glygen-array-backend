package org.glygen.array.util.parser.cfg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ParseCFGUtil {
    
    long dataId = 1;
    long linkId = 1;
    
    Set<String> sampleColumns = new HashSet<String>();
    Set<String> experimentColumns = new HashSet<String>();
    Set<String> requestColumns = new HashSet<String>();
    
    public List<String> parse (String filename, String version) {
        
        ObjectMapper mapper = new ObjectMapper();
        List<String> sqlCommands = new ArrayList<String>();
        
        if (version.contains("5.2")) {
            try {
                InputStream is = new FileInputStream(new File(filename));
                CFG52Model[] rows = mapper.readValue(is, CFG52Model[].class);
                //CFG52 model = mapper.readValue(is, new TypeReference<List<CFG52Model>>(){});
                sqlCommands.add("CREATE SCHEMA IF NOT EXISTS cfg_5_2;\n");
                sqlCommands.add("CREATE TABLE IF NOT EXISTS \"cfg_5_2\".experiment(\n");
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
                
                sqlCommands.add("CREATE TABLE IF NOT EXISTS \"cfg_5_2\".experiment_link(\n");
                sqlCommands.add("experiment_id bigint,\n" + 
                        "link_id bigint );\n");
                
                // go through data parts to determine the columns for the those data tables
                
                for (CFG52Model entry: rows) {
                    DataWithTitle sampleData = entry.getSampleData();
                    if (sampleData != null) {
                        sampleColumns.addAll(findColumns (sampleData.getData()));
                    }
                    DataWithTitle experimentData = entry.getExperimentData();
                    if (experimentData != null) {
                        experimentColumns.addAll(findColumns (experimentData.getData()));
                    }
                    DataWithTitle requestData = entry.getRequestData();
                    if (requestData != null) {
                        requestColumns.addAll(findColumns (requestData.getData()));
                    }
                }
                
                sqlCommands.add("CREATE TABLE IF NOT EXISTS \"cfg_5_2\".sampledata(\n");
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
                
                sqlCommands.add("CREATE TABLE IF NOT EXISTS \"cfg_5_2\".experimentdata(\n");
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
                
                sqlCommands.add("CREATE TABLE IF NOT EXISTS \"cfg_5_2\".requestdata(\n");
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
                
                sqlCommands.add("CREATE TABLE \"cfg_5_2\".link(\n");
                sqlCommands.add("id bigint primary key, \n" +
                        "linkText varchar(256),\n" +
                        "href varchar(256),\n" +
                        "parentCell  INT\n);");
                
                
                // insert data
                long experimentId = 1;
                for (CFG52Model entry: rows) {
                    sqlCommands.addAll(addRow (entry, experimentId));
                    experimentId ++;
                }
                
                
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }   
        }
        
        return sqlCommands;
    }

    private List<String> addRow(CFG52Model entry, long experimentId) {
        List<String> inserts = new ArrayList<String>();
        inserts.add(addSampleData(entry.sampleData));
        inserts.add(addExperimentData(entry.experimentData, experimentId));
        inserts.add(addRequestData(entry.requestData, experimentId));
        for (Link l: entry.getLinks())
            inserts.add(addLink(l, experimentId));
        
        String insert = "insert into \"cfg_5_2\".experiment (id, primscreen, sample_name, "
                + "species, protein_family, investigator, request, date, sample_data_id, experiment_data_id, request_data_id, filename) values (";
        insert += experimentId + ", '" + entry.primscreen + "', '" + entry.sampleName + "', '" + entry.species;
        insert += "', '" + entry.proteinFamily + "', '" + entry.investigator + "', '" + entry.request + "', '" + entry.date + "', '" +
               "', '" + this.dataId + "', '" + this.dataId + "', '" + this.dataId + "', '" + entry.getFilename() + "');\n";
        inserts.add(insert);
        
        this.dataId ++;
        
        return inserts;
        
    }
    
    private String addSampleData (DataWithTitle row) {
        String insert = "insert into \"cfg_5_2\".sampledata (id, title, ";
        int i=0;
        for (Data col: row.getData()) {
            insert += "\"" + formatColumnName(col.getKey()) + "\"";
            if (i < row.getData().size()-1) {
                insert += ", ";
            }
        }
        insert += ") values (" + this.dataId + ", '" + row.getTitle() + "', '";
        i=0;
        for (Data col: row.getData()) {
            insert += col.getValue();
            if (i < row.getData().size()-1) {
                insert += "', '";
            }
        }
        insert += "');\n";
        
        
        return insert;
    }

    private String addExperimentData (DataWithTitle row, long experimentId) {
        String insert = "insert into \"cfg_5_2\".experimentdata (id, title, ";
        int i=0;
        for (Data col: row.getData()) {
            insert += formatColumnName(col.getKey());
            if (i < row.getData().size()-1) {
                insert += ", ";
            }
        }
        insert += ") values (" + this.dataId + ", '" + row.getTitle() + "', '";
        i=0;
        for (Data col: row.getData()) {
            insert += col.getValue();
            if (i < row.getData().size()-1) {
                insert += "', '";
            }
        }
        insert += ");\n";
        
        
        return insert;
    }
    
    private String addRequestData (DataWithTitle row, long experimentId) {
        String insert = "insert into \"cfg_5_2\".requestdata (id, title, ";
        int i=0;
        for (Data col: row.getData()) {
            insert += formatColumnName(col.getKey());
            if (i < row.getData().size()-1) {
                insert += ", ";
            }
        }
        insert += ") values (" + this.dataId + ", '" + row.getTitle() + "', '";
        i=0;
        for (Data col: row.getData()) {
            insert += col.getValue();
            if (i < row.getData().size()-1) {
                insert += "', '";
            }
        }
        insert += ");\n";
        
        
        return insert;
    }
    
    private String addLink (Link row, long experimentId) {
        String insert = "insert into \"cfg_5_2\".link (id, linkText, href, parentCell) values (";
        insert += linkId + ", '" + row.linkText + "', '" + row.href + ", " + row.parentCell + ");\n";
        insert += "insert into \"cfg_5_2\".experiment_link(experiment_id, link_id) values (" +
                experimentId + ", " + this.linkId + ");\n";      
        this.linkId ++;
        return insert;
    }

    private Set<String> findColumns(List<Data> data) {
        Set<String> columnSet = new HashSet<String>();
        for (Data d: data) {
            columnSet.add(formatColumnName (d.getKey()));
        }
        
        return columnSet;
    }
    
    private String formatColumnName (String columnName) {
        columnName = columnName.replace(" ", "_");
        if (columnName.length() > 30) {
            columnName = columnName.substring(0, 30);
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
