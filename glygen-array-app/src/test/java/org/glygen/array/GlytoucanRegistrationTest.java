package org.glygen.array;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarExporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarImporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.glycoinfo.WURCSFramework.io.GlycoCT.GlycoVisitorValidationForWURCS;
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
		        System.out.println ("Processing " + st.getStructure_id());
		        String glycoCT = st.getGlyco_ct();
	            try {
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
                        /*GlycoVisitorValidationForWURCS t_validationWURCS = new GlycoVisitorValidationForWURCS();
                        t_validationWURCS.start(sugar);
                        List<String> errors = t_validationWURCS.getErrors();
                        if (errors != null && !errors.isEmpty()) {
                            System.out.println ("Validation errors, skipping " + st.getStructure_id() + ": ");
                            for (String e: errors) {
                                System.out.println (e);
                            }
                            continue;
                        }*/
                    }
	            }
                catch (Exception e) {
                    System.out.println ("Error parsing/recoding/validating " + st.getStructure_id() + " Reason:" + e.getMessage());
                    //e.printStackTrace();
                }
	            try {
	                WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
	                exporter.start(glycoCT);
	                String wurcs = exporter.getWURCS(); 
	                System.out.println ("Validating  " + st.getStructure_id());
	                // validate first to get errors if any
	                String errors = GlytoucanUtil.getInstance().validateGlycan(wurcs);
	                if (errors != null) {
	                    System.out.println ("Validation errors, skipping " + st.getStructure_id() + ": " + errors);
	                    continue;
	                }
	                System.out.println ("getting accession number " + st.getStructure_id());
	                String glytoucanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
	                if (glytoucanId == null) {
	                    System.out.println ("registering " + st.getStructure_id());
	                    glytoucanId = GlytoucanUtil.getInstance().registerGlycan(wurcs);
	                }
	                GlytoucanInfo g = new GlytoucanInfo();
	                g.setId(st.getStructure_id());
	                g.setGlytoucanId(glytoucanId);
	                glytoucanRepository.save(g);
	                System.out.println ("Done processing!");
	                assertTrue(true);
	            } catch (Exception e) {
	                System.out.println ("Error getting accession number for " + st.getStructure_id() + " Reason:" + e.getMessage());
	                //e.printStackTrace();
	            }
		    }
		    else {
		        if (existing.get().getGlytoucanId() == null || existing.get().getGlytoucanId().length() > 10) {
		            // hash
		            String glycoCT = st.getGlyco_ct();
	                try {
	                    System.out.println ("Processing previously registered " + st.getStructure_id());
	                    
	                    SugarImporterGlycoCTCondensed importer = new SugarImporterGlycoCTCondensed();
                        Sugar sugar = importer.parse(glycoCT.trim());
                        /*GlycoVisitorValidationForWURCS t_validationWURCS = new GlycoVisitorValidationForWURCS();
                        t_validationWURCS.start(sugar);
                        List<String> errors = t_validationWURCS.getErrors();
                        if (errors != null && !errors.isEmpty()) {
                            System.out.println ("Validation errors, skipping " + st.getStructure_id() + ": ");
                            for (String err: errors) {
                                System.out.println (err);
                            }
                            continue;
                        }*/
                        if (sugar != null) {
                            SugarExporterGlycoCTCondensed exporter = new SugarExporterGlycoCTCondensed();
                            exporter.start(sugar);
                            glycoCT = exporter.getHashCode();
                            glycoCT = fixGlycoCT.fixGlycoCT(glycoCT);
                            st.setGlyco_ct(glycoCT);
                            System.out.println("saving recoded glycoCT");
                            glycoCTRepository.save(st);
                        }
	                    WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
	                    exporter.start(glycoCT);
	                    String wurcs = exporter.getWURCS(); 
	                    System.out.println ("Validating previously registered " + st.getStructure_id());
	                    // validate first to get errors if any
	                    String errors = GlytoucanUtil.getInstance().validateGlycan(wurcs);
	                    if (errors != null) {
	                        System.out.println ("Validation errors, skipping " + st.getStructure_id() + ": " + errors);
	                        continue;
	                    }
	                    System.out.println ("getting accession number for previously registered " + st.getStructure_id());
	                    System.out.println ("WURCS: " + wurcs + " registration hash " + existing.get().getGlytoucanId());
	                    String glytoucanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
	                    System.out.println ("Got " + glytoucanId);
	                    if (glytoucanId == null) {
	                        // register again
                            System.out.println ("registering again " + st.getStructure_id());
                            glytoucanId = GlytoucanUtil.getInstance().registerGlycan(wurcs);
	                    } 
	                    GlytoucanInfo g = new GlytoucanInfo();
                        g.setId(st.getStructure_id());
                        g.setGlytoucanId(glytoucanId);
                        glytoucanRepository.save(g);
	                } catch (Exception e) {
	                    System.out.println ("error getting accession number for previously registered " + st.getStructure_id());
	                    System.out.println ("try recoding again!");
	                    try {
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
                               /* GlycoVisitorValidationForWURCS t_validationWURCS = new GlycoVisitorValidationForWURCS();
                                t_validationWURCS.start(sugar);
                                List<String> errors = t_validationWURCS.getErrors();
                                if (errors != null && !errors.isEmpty()) {
                                    System.out.println ("Validation errors, skipping " + st.getStructure_id() + ": ");
                                    for (String err: errors) {
                                        System.out.println (err);
                                    }
                                    continue;
                                }*/
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
	                        System.out.println ("Again, validating previously registered " + st.getStructure_id());
	                        // validate first to get errors if any
	                        String errors = GlytoucanUtil.getInstance().validateGlycan(wurcs);
	                        if (errors != null) {
	                            System.out.println ("Validation errors, skipping " + st.getStructure_id() + ": " + errors);
	                            continue;
	                        }
	                        System.out.println ("Again, getting accession number for previously registered " + st.getStructure_id());
	                        String glytoucanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
	                        System.out.println ("Got " + glytoucanId);
	                        if (glytoucanId != null) {
	                            GlytoucanInfo g = new GlytoucanInfo();
	                            g.setId(st.getStructure_id());
	                            g.setGlytoucanId(glytoucanId);
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
	
	@Test
    public void generateGlycomeDBIdForErrors() throws FileNotFoundException {
	    File hashFile = new File ("/Users/sena/Downloads/hashkeys.txt");
	    Scanner scanner = new Scanner(hashFile);
	    while (scanner.hasNextLine()) {
	        String hash = scanner.nextLine();
	        List<GlytoucanInfo> existing = glytoucanRepository.findByGlytoucanId(hash);
	        if (!existing.isEmpty()) {
	            System.out.println(existing.get(0).getId());
	        } else {
	            System.out.println ();
	        }
	    }
	    scanner.close();
	}
}
