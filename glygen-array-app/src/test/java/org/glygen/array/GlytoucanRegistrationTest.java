package org.glygen.array;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarExporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarImporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.glycoinfo.WURCSFramework.io.GlycoCT.WURCSExporterGlycoCT;
import org.glygen.array.persistence.cfgdata.GlycoCTStructureRepository;
import org.glygen.array.persistence.cfgdata.GlytoucanInfo;
import org.glygen.array.persistence.cfgdata.GlytoucanInfoRepository;
import org.glygen.array.persistence.cfgdata.Structure;
import org.glygen.array.util.FixGlycoCtUtil;
import org.glygen.array.util.GlytoucanUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(loader=MySpringBootContextLoader.class)
@SpringBootTest(classes = GlygenArrayApplication.class)
public class GlytoucanRegistrationTest {
    
    /*
     * structureschema.sql and structure.sql (containing glycoct) should be imported into 
     * postgres database before running this application
     */
	
	@Autowired
	GlycoCTStructureRepository glycoCTRepository;
	
	@Autowired
	GlytoucanInfoRepository glytoucanRepository;
	
	@Test
	public void testRegistration () {
		//glytoucanRepository.deleteAll();
		List<Structure> structures = glycoCTRepository.findAll();
		FixGlycoCtUtil fixGlycoCT = new FixGlycoCtUtil();
		System.out.println ("Count :" + structures.size());
		for (Structure st: structures) {
		    Optional<GlytoucanInfo> existing = glytoucanRepository.findById(st.getStructure_id());
		    if (!existing.isPresent()) {
		        String glycoCT = st.getGlyco_ct();
	            try {
	                Glycan glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromGlycoCTCondensed(glycoCT.trim());
                    if (glycanObject != null) {
                        glycoCT = glycanObject.toGlycoCTCondensed(); // required to fix formatting errors like extra line break etc.
                        glycoCT = fixGlycoCT.fixGlycoCT(glycoCT);
                        System.out.println("saving recoded glycoCT");
                        st.setGlyco_ct(glycoCT);
                        glycoCTRepository.save(st);
                    } else {
                        SugarImporterGlycoCTCondensed importer = new SugarImporterGlycoCTCondensed();
                        Sugar sugar = importer.parse(glycoCT.trim());
                        if (sugar != null) {
                            SugarExporterGlycoCTCondensed exporter = new SugarExporterGlycoCTCondensed();
                            exporter.start(sugar);
                            glycoCT = exporter.getHashCode();
                            glycoCT = fixGlycoCT.fixGlycoCT(glycoCT);
                            st.setGlyco_ct(glycoCT);
                            System.out.println("saving recoded glycoCT");
                            glycoCTRepository.save(st);
                        }
                    }
	            }
                catch (Exception e) {
                    System.out.println ("Error parsing/recoding " + st.getStructure_id() + " Reason:" + e.getMessage());
                    //e.printStackTrace();
                }
	            try {
	                WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
	                exporter.start(glycoCT);
	                String wurcs = exporter.getWURCS(); 
	                System.out.println ("getting accession number " + st.getStructure_id());
	                String glytoucanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
	                if (glytoucanId == null) {
	                    System.out.println ("registering " + st.getStructure_id());
	                    glytoucanId = GlytoucanUtil.getInstance().registerGlycan(wurcs);
	                }
	                GlytoucanInfo g = new GlytoucanInfo();
	                g.setId(st.getStructure_id());
	                g.setGlytoucan_id(glytoucanId);
	                glytoucanRepository.save(g);
	                assertTrue(true);
	            } catch (Exception e) {
	                System.out.println ("Error getting accession number for " + st.getStructure_id() + " Reason:" + e.getMessage());
	                //e.printStackTrace();
	            }
		    }
		    else {
		        if (existing.get().getGlytoucan_id() == null || existing.get().getGlytoucan_id().length() > 10) {
		            // hash
		            String glycoCT = st.getGlyco_ct();
	                try {
	                    WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
	                    exporter.start(glycoCT);
	                    String wurcs = exporter.getWURCS(); 
	                    System.out.println ("getting accession number for previously registered" + st.getStructure_id());
	                    String glytoucanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
	                    System.out.println ("Got " + glytoucanId);
	                    if (glytoucanId != null) {
	                        GlytoucanInfo g = new GlytoucanInfo();
	                        g.setId(st.getStructure_id());
	                        g.setGlytoucan_id(glytoucanId);
	                        glytoucanRepository.save(g);
	                    }
	                } catch (Exception e) {
	                    System.out.println ("error getting accession number for previously registered " + st.getStructure_id());
	                    System.out.println ("try recoding again!");
	                    try {
	                        Glycan glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromGlycoCTCondensed(glycoCT.trim());
	                        if (glycanObject != null) {
	                            glycoCT = glycanObject.toGlycoCTCondensed(); // required to fix formatting errors like extra line break etc.
	                            glycoCT = fixGlycoCT.fixGlycoCT(glycoCT);
	                            System.out.println("saving recoded glycoCT");
	                            st.setGlyco_ct(glycoCT);
	                            glycoCTRepository.save(st);
	                        } else {
	                            SugarImporterGlycoCTCondensed importer = new SugarImporterGlycoCTCondensed();
	                            Sugar sugar = importer.parse(glycoCT.trim());
	                            if (sugar != null) {
	                                SugarExporterGlycoCTCondensed exporter = new SugarExporterGlycoCTCondensed();
	                                exporter.start(sugar);
	                                glycoCT = exporter.getHashCode();
	                                glycoCT = fixGlycoCT.fixGlycoCT(glycoCT);
	                                st.setGlyco_ct(glycoCT);
	                                System.out.println("saving recoded glycoCT");
	                                glycoCTRepository.save(st);
	                            }
	                        }
	                    }
                        catch (Exception e1) {
                            System.out.println ("Error parsing/recoding " + st.getStructure_id() + " Reason:" + e1.getMessage());
                            //e.printStackTrace();
                        }
	                    try {
	                        WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
	                        exporter.start(glycoCT);
	                        String wurcs = exporter.getWURCS(); 
	                        System.out.println ("Again, getting accession number for previously registered " + st.getStructure_id());
	                        String glytoucanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
	                        System.out.println ("Got " + glytoucanId);
	                        if (glytoucanId != null) {
	                            GlytoucanInfo g = new GlytoucanInfo();
	                            g.setId(st.getStructure_id());
	                            g.setGlytoucan_id(glytoucanId);
	                            glytoucanRepository.save(g);
	                        }
	                    } catch (Exception e2) {
	                        System.out.println ("Error getting accession number for previously registered after recoding " + st.getStructure_id() + " Reason: " + e2.getMessage());
    	                    //e.printStackTrace();
	                    }
	                }
		        }
		    }
		}
	}
}
