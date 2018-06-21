package org.glygen.array.controller;

import org.glygen.array.GlygenArrayApplication;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.qos.logback.classic.Logger;

@SpringBootApplication
@RestController
@RequestMapping("/logger")
public class LoggingController {

	public static Logger logger=(Logger) LoggerFactory.getLogger(LoggingController.class);
	
	@GetMapping("/logInfo")
	public String logInfo() {
		logger.info("");
		return "Info logged";
	}
	
	@GetMapping("/logError")
	public String logError() {
		logger.error("");
		return "Error logger";
	}
	
}
