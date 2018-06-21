package org.glygen.array;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(loader=MySpringBootContextLoader.class)
@SpringBootTest(classes = GlygenArrayApplication.class)
public class GlygenArrayApplicationTests {

	@Test
	public void contextLoads() {
	}

	@Test
	public void testLogging() {
		final Logger log = LoggerFactory.getLogger(org.glygen.array.GlygenArrayApplication.class);
		log.info("Info test");
		log.debug("Debug test");
		log.warn("Warning test");
		
	}
	
}
