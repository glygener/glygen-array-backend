package org.glygen.array.typeahead;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.jline.utils.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sena Arpinar
 *
 */
public class NamespaceHandler {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");

    private static String resourceFolderName = "ontology" + File.separator + "namespace";
    
    protected static Map<String, String> namespaceFileMapping = new HashMap<>();
    protected static Map <String, PatriciaTrie<String>> namespaces = new HashMap<>();
    
    public static void loadNamespaces(){
        // check the folder and find all namespace files, 
        // load them into the namespaces hashmap
        long startingMemory = Runtime.getRuntime().freeMemory();
        logger.info("Memory before namespaces:" + startingMemory);
        File namespaceFolder = new File (resourceFolderName);
        String[] namespaceFiles = namespaceFolder.list();
        for (String filename: namespaceFiles) {
            String namespace = filename.substring(0, filename.indexOf("_"));
            namespaceFileMapping.put(namespace, filename);
            File namespaceFile = new File(namespaceFolder + File.separator + filename);
            if(namespaceFile.exists())
            {
                logger.info("Creating trie from namespace file : " + namespaceFile.getName());
                PatriciaTrie<String> trie = parseNamespaceFile(namespaceFile.getAbsolutePath());
                namespaces.put (namespace, trie);
            }
        }
        
        long endMemory = Runtime.getRuntime().freeMemory();
        logger.info("NamespaceHandler memory usage: " +  (endMemory - startingMemory)/1000000 + " MB" );
        logger.info("Memory after namespaces:" + endMemory);
    }
        
    /**
     * This is the default implementation which expects a file with two columns separated with a tab (\t) and 
     * each line corresponds to a new entry. First column should contain all the synonyms to be matched and the second 
     * column should have the actual value to be used
     * 
     * NOTE: Subclasses should override this method to parse their specific file formats
     *
     * @param filename containing the synonyms
     * @return a PatriciaTrie for searching
     */
    public static PatriciaTrie<String> parseNamespaceFile (String filename) {
        PatriciaTrie<String> trie = new PatriciaTrie<String>();
        
        long startTime = System.currentTimeMillis();
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(filename);
            logger.info("Reading namespaces from inputstream");
            BufferedReader names = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line=names.readLine())!=null) {
                String[] parts = line.split("\\t");
                String synonym = parts[0].trim();
                String name = parts[1].trim();
                
                trie.put (synonym.toLowerCase(), name); 
            }
            logger.info("Closing inputstream for namespace file");
            inputStream.close();
            names.close();
        } catch (FileNotFoundException e) {
            logger.error("Cannot find the namespace: " + filename, e);
        } catch (IOException e) {
            logger.error("Cannot read the namespace: " + filename, e);
        } catch (Exception | Error e) {
            logger.error("Cannot load the namespace: " + filename + "\n" + e.getMessage(), e);
            throw e;
        }
        logger.info("NamespaceHandler Took: " +  (System.currentTimeMillis() - startTime)/1000.0 + " seconds for file: " + filename);
        return trie;
    }
    
    /**
     * This implementation returns a PatriciaTrie using the strings in the given list as the synonyms and the actual values to show
     * for the type ahead.
     * 
     * @param items list of strings
     * @return patriciaTrie to be used in typeahead implementation
     */
    public static PatriciaTrie<String> createNamespaceFromList (List<String> items) {
        PatriciaTrie<String> trie = new PatriciaTrie<String>();
        for (String item: items) {
            trie.put(item.toLowerCase(), item);
        }
        return trie;
    }
    
    public static PatriciaTrie<String> getTrieForNamespace(String namespace) {
        return namespaces.get(namespace);
    }
}