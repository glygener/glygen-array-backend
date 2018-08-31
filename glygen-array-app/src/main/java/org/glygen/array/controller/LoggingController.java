package org.glygen.array.controller;


import java.time.LocalDate;
import org.glygen.array.persistence.WebAccessLoggingEntity;
import org.glygen.array.persistence.WebEventLoggingEntity;
import org.glygen.array.persistence.dao.WebAccessLoggingDAO;
import org.glygen.array.persistence.dao.WebEventLoggingDAO;
import org.glygen.array.view.WebLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/weblogger")
public class LoggingController {

	final Logger log = LoggerFactory.getLogger("event-logger");
	
	@Autowired
	WebAccessLoggingDAO accessRepository;
	
	@Autowired
	WebEventLoggingDAO eventRepository;
	
	@RequestMapping(value = "/event", method = RequestMethod.POST, 
    		consumes={"application/xml", "application/json"})
	public void webEventLogger(@RequestBody(required=true) WebLogger logger) {
		try {
			WebEventLoggingEntity newEvent = new WebEventLoggingEntity();
			
			newEvent.setDate(LocalDate.now());
			newEvent.setLevelString(logger.getLevel());
			newEvent.setPage(logger.getPage());
			newEvent.setMessage(logger.getMessage());
			newEvent.setComment(logger.getComment());
			newEvent.setUser(logger.getUser());
			
			eventRepository.save(newEvent);
		}catch (Exception e)
		{
			log.error("Error in logging event to database" + e);
		}
				
	}
	
	@RequestMapping(value = "/access", method = RequestMethod.POST, 
    		consumes={"application/xml", "application/json"})
	public void webAccessLogger(@RequestBody(required=true) WebLogger logger) {
		
		try {
		WebAccessLoggingEntity newEvent = new WebAccessLoggingEntity();
		
		newEvent.setDate(LocalDate.now());
		newEvent.setLevelString(logger.getLevel());
		newEvent.setPage(logger.getPage());
		newEvent.setMessage(logger.getMessage());
		newEvent.setComment(logger.getComment());
		newEvent.setUser(logger.getUser());
		
		accessRepository.save(newEvent);
		}catch (Exception e)
		{
			log.error("Error in logging access to database" + e);
		}
	}
	
}
