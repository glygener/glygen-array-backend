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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;

public class ParseCFGUtil {
    
    long dataId = 1;
    long linkId = 1;
    
    Set<String> sampleColumns = new HashSet<String>();
    Set<String> experimentColumns = new HashSet<String>();
    Set<String> requestColumns = new HashSet<String>();
    Set<String> protocolColumns = new HashSet<String>();
    Map<String, String> protocolColumnMap = new HashMap<String, String>();
    Map<String, String> sampleColumnMap = new HashMap<String, String>();
    Map<String, String> experimentColumnMap = new HashMap<String, String>();
    Map<String, String> requestColumnMap = new HashMap<String, String>();
    
    public void determineColumns (String folderWithJson) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        File folder = new File (folderWithJson);
        if (folder.isDirectory()) {
            for (File jsonfile: folder.listFiles()) {
                if (jsonfile.getName().endsWith("json")) {
                    InputStream is = new FileInputStream(jsonfile);
                    System.out.println ("Processing file " + jsonfile.getName());
                    CFG52Model[] rows = mapper.readValue(is, CFG52Model[].class);
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
                }
            }
        }
    }
    
    void printColumns () throws FileNotFoundException {
        PrintStream columnFile = new PrintStream(new FileOutputStream("cfgcolumns.txt" ));
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
        
        columnFile.println();
        columnFile.println("protocoldata");
        for (String key: protocolColumnMap.keySet()) {
            columnFile.println (key + "\t" + protocolColumnMap.get(key));
        }
        columnFile.close();
    }
    
    
    public List<String> parse (String folderWithJson) {
        ObjectMapper mapper = new ObjectMapper();
        List<String> sqlCommands = new ArrayList<String>();
        
        String schema = "cfg";
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
        
        try {
            determineColumns(folderWithJson);
            File folder = new File (folderWithJson);
            if (folder.isDirectory()) {
                long experimentId = 1;
                for (File jsonfile: folder.listFiles()) {
                    if (jsonfile.getName().endsWith("json")) {
                        InputStream is = new FileInputStream(jsonfile);
                        CFG52Model[] rows = mapper.readValue(is, CFG52Model[].class);
            
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
                        
                        sqlCommands.add("CREATE TABLE IF NOT EXISTS \"" + schema + "\".link(\n");
                        sqlCommands.add("id bigint primary key, \n" +
                                "linkText varchar(256),\n" +
                                "href varchar(256),\n" +
                                "parentCell  INT);\n");
                        
                        int endTableIndex = sqlCommands.size()-2;
                        
                        // insert data
                        for (CFG52Model entry: rows) {
                            sqlCommands.addAll(addRow (entry, experimentId, schema));
                            experimentId ++;
                        }
                        
                        protocolColumns.add("Overview");
                        protocolColumnMap.put("Overview", "Overview");
                        StringBuffer tableStatement = new StringBuffer();
                        tableStatement.append("CREATE TABLE IF NOT EXISTS \"" + schema + "\".protocoldata(\n");
                        tableStatement.append("id bigint primary key,\n");
                        tableStatement.append("title text,\n");
                        i=0;
                        for (String col: protocolColumns) {
                            tableStatement.append("\"" + col + "\" text");
                            if (i < protocolColumns.size()-1) 
                                tableStatement.append(",\n");
                            i++;
                        }
                        tableStatement.append("\n);\n");
                        
                        sqlCommands.add(endTableIndex, tableStatement.toString());
                    }
                }
            }
            printColumns();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }   
        
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
                this.dataId + ", " + this.dataId + ", " + this.dataId + ", E'" + (entry.getFilename() != null ? entry.getFilename().replace("'", "\\'") : "") + "');\n";
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
                    System.err.println ("Found another " + columnName + " in table " + tablename);
                    insert += "\"" + columnName + "1\"";
                    if (i < row.getData().size()-1) {
                        insert += ", ";
                    }
                    skipList.add(i);
                }
                i++;
            }
            if (insert.endsWith(", ")) // remove the last comma
                insert = insert.substring(0, insert.length() - 2);
            insert += ") values (" + this.dataId + ", E'" + row.getTitle().replace("'", "\\'") + "', E'";
            i=0;
            for (Data col: row.getData()) {
                String value = col.getValue().replace("'", "\\'");
                insert += value;
                if (i < row.getData().size()-1) {
                    insert += "', E'";
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
    
    private String extractProtocolData(String primScreenURL, long experimentId, String schema) {
        String insert = "";
        // find the protocol page link and extract the data
        WebClient client = new WebClient();
        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);
        try {
            HtmlPage page = client.getPage(primScreenURL);
            if (page != null) {
                HtmlAnchor link = page.getFirstByXPath("//a[contains(@href, 'http://www.functionalglycomics.org:80/glycomics/ProtocolServlet')]");
                if (link != null) {
                    Link pdfLink = new Link();
                    DataWithTitle protocolData = extractProtocolData (client, link.getHrefAttribute(), pdfLink);
                    insert = addData(protocolData, schema + ".protocoldata");
                    if (pdfLink.getHref() != null && !pdfLink.getHref().isEmpty()) {
                        insert += addLink(pdfLink, experimentId, schema);
                    }
                }
            }
        
            client.close();
        } catch(Exception e){
            e.printStackTrace();
        }
        
        return insert;
    }

    @SuppressWarnings("unchecked")
    private DataWithTitle extractProtocolData(WebClient client, String href, Link link) {
        DataWithTitle protocolData = new DataWithTitle();
        String overview;
        List<Data> list = new ArrayList<Data>();
        String protocolURL = href.substring(href.indexOf("javascript:openWindow") + 23, href.length() - 2);
        try {
            HtmlPage page = client.getPage(protocolURL);
            if (page != null) {
                // get the title
                HtmlElement titleTable = (HtmlElement) page.getFirstByXPath("//div[@class='norm']/table[1]");
                HtmlElement titleH4 = titleTable.getFirstByXPath("//tr/td/h4");
                if (titleH4 != null) {
                    protocolData.setTitle(titleH4.getTextContent());
                }
                
                final HtmlTable table = (HtmlTable) page.getFirstByXPath("//table[@class='mainTable']");
                if (table != null) {
                    // get elements
                    List<HtmlElement> keys = (List<HtmlElement>) table.getByXPath("//tr/td[@class='webSiteBodyDark']");
                    if (keys != null) {
                        for (HtmlElement key: keys) {
                            Data data = new Data();
                            String keyString = key.getTextContent();
                            if (keyString != null) {
                                String col = keyString.substring(0, keyString.length()-1);
                                String formattedCol = formatColumnName(col);
                                if (!protocolColumns.contains(formattedCol)) {
                                    protocolColumns.add(formattedCol);
                                    protocolColumnMap.put(col, formattedCol);
                                }
                                data.setKey(col);
                                list.add(data);
                            }
                        }
                    }
                    int i=0;
                    List<HtmlElement> values = (List<HtmlElement>) table.getByXPath("//tr/td[@class='WebSiteBodyNormal']");
                    if (values != null) {
                        for (HtmlElement val: values) {
                            list.get(i).setValue(val.getTextContent());
                            i++;
                        }
                    }
                }
                
                HtmlTable overviewTable = (HtmlTable) page.getFirstByXPath("//td[@class='webSiteHeading']/strong/text()[. = 'Overview']/following::table[1]");
                if (overviewTable != null) {
                    for (DomNode tr: overviewTable.getChildren()) {
                        for (DomNode td: tr.getChildren()) {
                            overview = td.getTextContent();
                            Data overviewData = new Data();
                            overviewData.setKey("Overview");
                            overviewData.setValue(overview);
                            list.add(overviewData);
                        }
                    }
                }
                
                // find the pdf link
                HtmlTable pdfTable = (HtmlTable) page.getFirstByXPath("//td[@class='webSiteHeading']/strong/text()[. = 'Protocol Description (PDF)']/following::table[1]");
                if (pdfTable != null) {
                    HtmlAnchor pdfLink = pdfTable.getFirstByXPath("//td/a");
                    link.setLinkText("Protocol Description (PDF)");
                    String url = pdfLink.getAttribute("onClick");
                    if (url != null && !url.isEmpty()) {
                        url = url.substring(12, url.length()-2);
                        link.setHref("http://www.functionalglycomics.org/"+url);
                    }
                    
                }
            }
          
        }catch(Exception e){
            e.printStackTrace();
        }
        protocolData.setData(list);
        return protocolData;
    }

    private String addLink (Link row, long experimentId, String schema) {
        String insert = "";
        if (row.href != null && row.href.contains("HServlet")) {
            insert += extractProtocolData(row.href, experimentId, schema);
        }
        insert += "insert into \"" + schema + "\".link (id, linkText, href, parentCell) values (";
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
            if (columnSet.contains(col) && col.equalsIgnoreCase("Further_Info")) {
                columnSet.add(col + "1");
                columnMap.put (col + "1", d.getKey());
            } else {
                columnSet.add(col);
                columnMap.put (col, d.getKey());
            }
        }
        
        return columnSet;
    }
    
    private String formatColumnName (String columnName) {
        columnName = columnName.replace("[", "_");
        columnName = columnName.replace("]", "_");
        columnName = columnName.replace(" ", "_");
        columnName = columnName.replace("/", "_");
        
        if (columnName.length() > 20) {
            columnName = "" + columnName.hashCode();
        }
        return columnName;
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("usage: folder (containing json files)");
            System.exit(1);
        }
        String folderName = args[0];
        
        try {
            PrintStream outputFile = new PrintStream(new FileOutputStream("cfg.sql" ));
            List<String> lines = new ParseCFGUtil().parse(folderName);
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
