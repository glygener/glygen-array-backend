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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/weblogger")
public class LoggingController {

	final Logger log = LoggerFactory.getLogger("event-logger");

	@Autowired
	WebAccessLoggingDAO accessRepository;

	@Autowired
	WebEventLoggingDAO eventRepository;

	@Operation(summary = "Add given log object into the event log")
	@RequestMapping(value = "/event", method = RequestMethod.POST, consumes = { "application/xml", "application/json" })
	public void webEventLogger(
			@Parameter(required = true, description = "object to be saved into the event log database") @RequestBody(required = true) WebLogger logger) {
		try {
			WebEventLoggingEntity newEventLog = new WebEventLoggingEntity();
			
			newEventLog.setLog_timestamp(LocalDate.now());
			newEventLog.setEvent_type(logger.getEvent_type());
			newEventLog.setPage(logger.getPage());
			newEventLog.setSession_id(logger.getSession_id());
			newEventLog.setComments(logger.getComments());
			newEventLog.setUser(logger.getUser());
			newEventLog.setInfo(logger.getInfo());
			newEventLog.setParams(logger.getParams());

			eventRepository.save(newEventLog);
		} catch (Exception e) {
			log.error("Error in logging event to database" + e);
		}

	}

	@Operation(summary = "Add given log object into the access log")
	@RequestMapping(value = "/access", method = RequestMethod.POST, consumes = { "application/xml",
			"application/json" })
	public void webAccessLogger(
			@Parameter(required = true, description = "object to be saved into the access log database") @RequestBody(required = true) WebLogger logger) {
		try {
			WebAccessLoggingEntity newAccessLog = new WebAccessLoggingEntity();
			
			newAccessLog.setLog_timestamp(LocalDate.now());
			newAccessLog.setPage(logger.getPage());
			newAccessLog.setSession_id(logger.getSession_id());
			newAccessLog.setUser(logger.getUser());

			accessRepository.save(newAccessLog);
		} catch (Exception e) {
			log.error("Error in logging access to database" + e);
		}
	}

}
