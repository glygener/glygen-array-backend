package org.glygen.array;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.glycoinfo.WURCSFramework.io.GlycoCT.WURCSExporterGlycoCT;
import org.glygen.array.persistence.cfgdata.GlycoCTStructureRepository;
import org.glygen.array.persistence.cfgdata.GlytoucanInfo;
import org.glygen.array.persistence.cfgdata.GlytoucanInfoRepository;
import org.glygen.array.persistence.cfgdata.Structure;
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
		for (Structure st: structures) {
		    Optional<GlytoucanInfo> existing = glytoucanRepository.findById(st.getStructure_id());
		    if (!existing.isPresent() || existing.get().getGlytoucan_id().length() > 10) {
		        String glycoCT = st.getGlyco_ct();
	            try {
	                WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
	                exporter.start(glycoCT);
	                String wurcs = exporter.getWURCS(); 
	                String glytoucanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
	                if (glytoucanId == null) {
	                    glytoucanId = GlytoucanUtil.getInstance().registerGlycan(wurcs);
	                }
	                GlytoucanInfo g = new GlytoucanInfo();
	                g.setId(st.getStructure_id());
	                g.setGlytoucan_id(glytoucanId);
	                glytoucanRepository.save(g);
	                assertTrue(true);
	            } catch (Exception e) {
	                assertFalse(false);
	            }
		    }	
		}
	}
}
