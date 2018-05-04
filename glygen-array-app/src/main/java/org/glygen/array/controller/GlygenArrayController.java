package org.glygen.array.controller;

import org.glygen.array.persistence.GlycanBinding;
import org.glygen.array.view.Confirmation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/array")
public class GlygenArrayController {
	
	
	@RequestMapping(value = "/addbinding", method = RequestMethod.POST, 
    		consumes={"application/xml", "application/json"})
	public Confirmation addGlycanBinding (GlycanBinding glycan) {
		//TODO
		return new Confirmation("Binding added successfully", HttpStatus.CREATED.value());
	}

}
