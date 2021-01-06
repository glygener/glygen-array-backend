package org.glygen.array.util.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.data.Measurement;
import org.grits.toolbox.glycanarray.library.om.layout.LevelUnit;
import org.grits.toolbox.glycanarray.om.model.Block;
import org.grits.toolbox.glycanarray.om.model.FileWrapper;
import org.grits.toolbox.glycanarray.om.model.GlycanArrayExperiment;
import org.grits.toolbox.glycanarray.om.model.Layout;
import org.grits.toolbox.glycanarray.om.model.MeasurementSet;
import org.grits.toolbox.glycanarray.om.model.PowerLevel;
import org.grits.toolbox.glycanarray.om.model.Slide;
import org.grits.toolbox.glycanarray.om.model.SpotData;
import org.grits.toolbox.glycanarray.om.model.Well;
import org.grits.toolbox.glycanarray.om.util.GlycanArrayParserUtils;

public class RawdataParser {
    
    public static Map<Measurement, Spot> parse (org.glygen.array.persistence.rdf.data.FileWrapper file, SlideLayout layout, Double powerLevel) throws Exception {
        
        Map<Measurement, Spot> dataMap = new HashMap<Measurement, Spot>();
        
        GlycanArrayExperiment experiment = new GlycanArrayExperiment();
        Slide slide = new Slide();
        //translate SlideLayout into Slide
        Layout l = new Layout();
        l.setName(layout.getName());
        slide.setLayout(l); 
        List<Block> blocks = new ArrayList<Block>();
        slide.setBlocks(blocks);
        List<Slide> slides = new ArrayList<Slide>();
        experiment.setSlides(slides);
        
        for (org.glygen.array.persistence.rdf.Block b: layout.getBlocks()) {
            Block block = new Block();
            Map<Well, SpotData> layoutData = new HashMap<Well, SpotData>();
            for (Spot spot: b.getBlockLayout().getSpots()) {
                int y = spot.getRow();
                int x = spot.getColumn();
                Well well = new Well(x, y);
                SpotData spotData = new SpotData();
                spotData.setGroup(spot.getGroup());
                spotData.setConcentration(spot.getConcentration().getConcentration());
                spotData.setProbeLevelUnit(spot.getConcentration().getLevelUnit());
                block.setPosition(new Well(b.getColumn(), b.getRow()));
                layoutData.put(well, spotData);
                int featureId = 1;
                if (spot.getFeatures() != null && !spot.getFeatures().isEmpty()) {
                    Feature feature = spot.getFeatures().get(0);
                    org.grits.toolbox.glycanarray.om.model.Feature f = new org.grits.toolbox.glycanarray.om.model.Feature();
                    f.setId(featureId ++);
                    f.setName(feature.getName());
                    spotData.setFeature(f);
                }
            }
            block.setLayoutData(layoutData);
            block.setName(b.getBlockLayout().getId());
            blocks.add(block);
        }
        
        // Check fileFormat to decide which parser to use
        if (file != null && file.getFileFormat() != null && file.getFileFormat().toLowerCase().contains("genepix")) {
            // process GenePix file
            FileWrapper fileWrapper = new FileWrapper (file.getFileFolder() + File.separator + file.getIdentifier(), "GenePix");
            GlycanArrayParserUtils.processGenePixFile (fileWrapper, experiment, slide);
        } else if (file != null && file.getFileFormat() != null && file.getFileFormat().toLowerCase().contains("proscan")) {
            FileWrapper fileWrapper = new FileWrapper (file.getFileFolder() + File.separator + file.getIdentifier(), "Proscan");
            GlycanArrayParserUtils.processProscanFile (fileWrapper, experiment, slide);
        } else if (file.getIdentifier().endsWith(".gpr") || file.getIdentifier().endsWith(".txt")) {
            FileWrapper fileWrapper = new FileWrapper (file.getFileFolder() + File.separator + file.getIdentifier(), "GenePix");
            GlycanArrayParserUtils.processGenePixFile (fileWrapper, experiment, slide);
        } else if (file.getIdentifier().endsWith(".xls") || file.getIdentifier().endsWith(".xlsx")) {
            FileWrapper fileWrapper = new FileWrapper (file.getFileFolder() + File.separator + file.getIdentifier(), "Proscan");
            GlycanArrayParserUtils.processProscanFile (fileWrapper, experiment, slide);
        } else {
            // format unknown
            throw new IOException ("file format is not supported: " + file.getFileFormat());
        }
        
        for (Block block: slide.getBlocks()) {
            if (block.getMeasurementSetMap() != null) {
                MeasurementSet set = null;
                for (PowerLevel key: block.getMeasurementSetMap().keySet()) {
                    if (key.getPowerLevel().equals(powerLevel)) 
                        set = block.getMeasurementSetMap().get(key);
                }
                if (set == null)
                    break;
                if (set.getMeasurementMap() == null) {
                    continue;
                }
                Collection<org.grits.toolbox.glycanarray.om.model.Measurement> measurements = set.getMeasurementMap().values();
                for (org.grits.toolbox.glycanarray.om.model.Measurement measurement: measurements) {
                    List<SpotData> dataList = measurement.getData();
                    for (SpotData spotData: dataList) {
                        Spot spot = new Spot();
                        spot.setRow(spotData.getPosition().getY());
                        spot.setColumn(spotData.getPosition().getX());
                        spot.setGroup(spotData.getGroup());
                        LevelUnit con = new LevelUnit();
                        con.setConcentration(spotData.getConcentration());
                        con.setLevelUnit(spotData.getProbeLevelUnit());
                        spot.setConcentration(con);
                        spot.setBlockLayoutId(block.getName());
                        Measurement m = new Measurement();
                        m.setbMean(spotData.getbMean());
                        m.setbMedian(spotData.getbMedian());
                        m.setbPixels(spotData.getbPixels());
                        m.setbStDev(spotData.getbStDev());
                        m.setCoordinates(spotData.getCoordinates());
                        m.setFlags(spotData.getFlags());
                        m.setfPixels(spotData.getfPixels());
                        m.setMean(spotData.getMean());
                        m.setMeanMinusB(spotData.getMeanMinusB());
                        m.setMedian(spotData.getMedian());
                        m.setMedianMinusB(spotData.getMedianMinusB());
                        m.setPercentageOneSD(spotData.getPercentageOneSD());
                        m.setPercentageSaturated(spotData.getPercentageSaturated());
                        m.setPercentageTwoSD(spotData.getPercentageTwoSD());
                        m.setSnRatio(spotData.getSnRatio());
                        m.setStdev(spotData.getStdev());
                        m.setTotoalIntensity(spotData.getTotalIntensity());
                        dataMap.put(m, spot);
                    }
                }
            }
        }
        
        return dataMap;
    }
}
