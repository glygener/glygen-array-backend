package org.glygen.array.util.parser;

import java.io.IOException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.data.ArrayDataset;
import org.grits.toolbox.glycanarray.om.model.FileWrapper;
import org.grits.toolbox.glycanarray.om.model.GlycanArrayExperiment;
import org.grits.toolbox.glycanarray.om.model.Slide;
import org.grits.toolbox.glycanarray.om.util.GlycanArrayParserUtils;

public class RawdataParser {
    
    public static ArrayDataset parse (String filePath, SlideLayout layout) {
        
        ArrayDataset arrayDataset = new ArrayDataset();
        
        GlycanArrayExperiment experiment = new GlycanArrayExperiment();
        Slide slide = new Slide();
        
        if (filePath.endsWith(".gpr") || filePath.endsWith(".txt")) {
            // process GenePix file
            FileWrapper fileWrapper = new FileWrapper (filePath, "GenePix");
            try {
                GlycanArrayParserUtils.processGenePixFile (fileWrapper, experiment, slide);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else if (filePath.endsWith(".xls") || filePath.endsWith(".xlsx")) {
            FileWrapper fileWrapper = new FileWrapper (filePath, "Proscan");
            try {
                GlycanArrayParserUtils.processProscanFile (fileWrapper, experiment, slide);
            } catch (InvalidFormatException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        
        }
        return arrayDataset;
    }
}
