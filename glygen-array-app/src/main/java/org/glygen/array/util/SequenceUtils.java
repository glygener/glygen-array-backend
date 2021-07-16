package org.glygen.array.util;

import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarExporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarImporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.glycoinfo.GlycanFormatconverter.io.GlycoCT.WURCSToGlycoCT;
import org.glycoinfo.WURCSFramework.util.validation.WURCSValidator;
import org.glygen.array.persistence.rdf.GlycanSequenceFormat;
import org.glygen.array.view.ErrorMessage;
import org.grits.toolbox.glycanarray.om.parser.cfg.CFGMasterListParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.ObjectError;

public class SequenceUtils {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    public static String parseSequence (ErrorMessage errorMessage, String sequence, String sequenceFormat) {
        GlycanSequenceFormat format = GlycanSequenceFormat.forValue(sequenceFormat.trim());
        String searchSequence = null;
        switch (format) {
        case GLYCOCT:
            boolean gwbError = false;
            try {
                org.eurocarbdb.application.glycanbuilder.Glycan glycanObject = 
                        org.eurocarbdb.application.glycanbuilder.Glycan.fromGlycoCTCondensed(sequence.trim());
                if (glycanObject == null) 
                    gwbError = true;
                else 
                    searchSequence = glycanObject.toGlycoCTCondensed(); // required to fix formatting errors like extra line break etc.
            } catch (Exception e) {
                logger.error("Glycan builder parse error", e);
            }
            
            if (gwbError) {
                // check to make sure GlycoCT valid without using GWB
                SugarImporterGlycoCTCondensed importer = new SugarImporterGlycoCTCondensed();
                try {
                    Sugar sugar = importer.parse(sequence.trim());
                    if (sugar == null) {
                        logger.error("Cannot get Sugar object for sequence:" + sequence.trim());
                        errorMessage.addError(new ObjectError("sequence", "NotValid"));
                    } else {
                        SugarExporterGlycoCTCondensed exporter = new SugarExporterGlycoCTCondensed();
                        exporter.start(sugar);
                        searchSequence = exporter.getHashCode();
                    }
                } catch (Exception pe) {
                    logger.error("GlycoCT parsing failed", pe);
                    errorMessage.addError(new ObjectError("sequence", pe.getMessage()));
                }
            }
            break;
        case WURCS:
            WURCSToGlycoCT wurcsConverter = new WURCSToGlycoCT();
            wurcsConverter.start(sequence.trim());
            searchSequence = wurcsConverter.getGlycoCT();
            if (searchSequence == null) {
                // keep it as WURCS
                // validate and re-code
                WURCSValidator validator = new WURCSValidator();
                validator.start(sequence.trim());
                if (validator.getReport().hasError()) {
                    String [] codes = validator.getReport().getErrors().toArray(new String[0]);
                    errorMessage.addError(new ObjectError("sequence", codes, null, "NotValid"));
                    searchSequence = null;
                } else {
                    searchSequence = validator.getReport().getStandardString();
                }
            }
            break;
        case IUPAC:
            CFGMasterListParser parser = new CFGMasterListParser();
            searchSequence = parser.translateSequence(ExtendedGalFileParser.cleanupSequence(sequence.trim()));
            break;
        case GWS:
            org.eurocarbdb.application.glycanbuilder.Glycan glycanObject = 
                    org.eurocarbdb.application.glycanbuilder.Glycan.fromGlycoCTCondensed(sequence.trim());
            if (glycanObject != null) {
                searchSequence = glycanObject.toGlycoCTCondensed(); // required to fix formatting errors like extra line break etc.
            }
            break;
        }
        
        if (searchSequence == null && (errorMessage.getErrors() == null || errorMessage.getErrors().isEmpty())) {
            errorMessage.addError(new ObjectError("sequence", "NotValid"));
        }
        
        return searchSequence;
    }

}
