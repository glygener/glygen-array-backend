package org.glygen.array.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.glycoinfo.GlycanFormatconverter.io.GlycoCT.WURCSToGlycoCT;
import org.grits.toolbox.util.structure.glycan.database.GlycanDatabase;
import org.grits.toolbox.util.structure.glycan.database.GlycanStructure;

public class MakeMiniDatabaseFromFile{
    public static List<String> readExcel (String fileName) throws IOException {
        List<String> numberList = new ArrayList<>();
        File file = new File (fileName);
        if (!file.exists())
            throw new FileNotFoundException(fileName + " does not exist!");
        
        // Open the file
        FileInputStream fstream = new FileInputStream(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

        String strLine;

        //Read File Line By Line
        while ((strLine = br.readLine()) != null) {
            numberList.add(strLine.trim());
        }

        //Close the input stream
        fstream.close();
        
        return numberList;
    }
    
    public static void generateGRITSDatabase (List<String> glytoucanIds, File dbFile) throws JAXBException {
        GlycanDatabase miniGlycansDatabase = new GlycanDatabase();
        miniGlycansDatabase.setName("hsc1");
        miniGlycansDatabase.setDescription("Human Stem Cell #1");
        miniGlycansDatabase.setVersion("1.0");
        for (String glytoucan: glytoucanIds) {
            GlycanStructure structure = new GlycanStructure();
            structure.setGlytoucanid(glytoucan);
            // retrieve WURCS from glytoucan and convert it to GWS
            String wurcs = GlytoucanUtil.getInstance().retrieveGlycan(glytoucan);
            if (wurcs != null) {
                WURCSToGlycoCT wurcsConverter = new WURCSToGlycoCT();
                wurcsConverter.start(wurcs);
                String glycoCT = wurcsConverter.getGlycoCT();
                if (glycoCT != null) {
                    org.eurocarbdb.application.glycanbuilder.Glycan glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromGlycoCTCondensed(glycoCT);
                    structure.setGWBSequence(glycanObject.toString());
                    structure.setId(glytoucan);
                    miniGlycansDatabase.addStructure(structure);    
                }
                else {
                    System.out.println("Cannot get GWS for " + glytoucan);
                    // try to read from a file
                    try {
                        glycoCT = readGlycoCTFromFile(glytoucan);
                        if (glycoCT != null) {
                            System.out.println("Read glycoCT from file " + glycoCT);
                            org.eurocarbdb.application.glycanbuilder.Glycan glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromGlycoCTCondensed(glycoCT);
                            structure.setGWBSequence(glycanObject.toString());
                            structure.setId(glytoucan);
                            miniGlycansDatabase.addStructure(structure);
                        }
                        else {
                            System.out.println("Cannot get GWS for " + glytoucan + " from glycoCT");
                        }
                    } catch (IOException e) {
                        System.out.println("Cannot get GWS for " + glytoucan + " from glycoCT. Exception " + e.getMessage());
                    }
                    
                }
            } else {
                System.out.println ("Cannot get WURCS from Glytoucan for " + glytoucan);
            }
        }
        
        JAXBContext jaxbContext = JAXBContext.newInstance(GlycanDatabase.class);
        Marshaller jaxBMarshaller = jaxbContext.createMarshaller();
        jaxBMarshaller.marshal(miniGlycansDatabase, dbFile);
    }
    
    private static String readGlycoCTFromFile(String glytoucan) throws IOException {
        File file = new File ("/Users/sena/Desktop/minibase/" + glytoucan + ".txt");
        if (!file.exists()) {
            System.out.println ("Cannot find the file for " + glytoucan);
            return null;
        }
        StringBuffer glycoCT = new StringBuffer();
        
        // Open the file
        FileInputStream fstream = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

        String strLine;

        //Read File Line By Line
        while ((strLine = br.readLine()) != null) {
            glycoCT.append(strLine + "\n");
        }

        //Close the input stream
        fstream.close();
       
        return glycoCT.toString();
    }

    public static void main(String[] args) {
        try {
            List<String> ids = readExcel ("/Users/sena/Desktop/minibase/minibase1.txt");
            generateGRITSDatabase(ids, new File ("/Users/sena/Desktop/minibase/hsc1.xml"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
     
    
    

}
