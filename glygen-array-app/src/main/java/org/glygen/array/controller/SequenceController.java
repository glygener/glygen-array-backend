package org.glygen.array.controller;

import java.util.HashMap;

import javax.validation.Validator;

import org.eurocarbdb.MolecularFramework.io.SugarImporterException;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarExporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.io.namespace.GlycoVisitorToGlycoCT;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorException;
import org.eurocarbdb.application.glycanbuilder.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.GraphicOptions;
import org.eurocarbdb.application.glycoworkbench.GlycanWorkspace;
import org.eurocarbdb.resourcesdb.Config;
import org.eurocarbdb.resourcesdb.GlycanNamescheme;
import org.eurocarbdb.resourcesdb.io.MonosaccharideConverter;
import org.grits.toolbox.glycanarray.library.om.translation.GlycoVisitorNamespaceCfgArrayToCarbbank;
import org.grits.toolbox.glycanarray.library.om.translation.SugarImporterNCFG;
import org.grits.toolbox.glycanarray.om.parser.cfg.CFGMasterListParser;
import org.grits.toolbox.glycanarray.om.parser.cfg.CarbIdGlycoCTParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping("/glycan")
public class SequenceController {
	
	@Autowired
	Validator validator;

	// needs to be done to initialize static variables to parse glycan sequence
	private static GlycanWorkspace glycanWorkspace = new GlycanWorkspace(null, false, new GlycanRendererAWT());
	
	static {
			// Set orientation of glycan: RL - right to left, LR - left to right, TB - top to bottom, BT - bottom to top
			glycanWorkspace.getGraphicOptions().ORIENTATION = GraphicOptions.RL;
			// Set flag to show information such as linkage positions and anomers
			glycanWorkspace.getGraphicOptions().SHOW_INFO = true;
			// Set flag to show mass
			glycanWorkspace.getGraphicOptions().SHOW_MASSES = false;
			// Set flag to show reducing end
			glycanWorkspace.getGraphicOptions().SHOW_REDEND = true;

	}

	@ApiOperation(value = "Convert given glycan sequence (in NCFG format) into GlycoCT")
	@RequestMapping(value="/parseSequence", method = RequestMethod.GET)
	@ApiResponses (value ={@ApiResponse(code=200, message="Successfully converted into GlycoCT"), 
			@ApiResponse(code=400, message="Invalid request, validation error"),
    		@ApiResponse(code=415, message="Media type is not supported"),
    		@ApiResponse(code=500, message="Internal Server Error")})
	public String parseCFGNameString (
			@ApiParam(required=true, value="Sequence string (in NCFG format) to be parsed into GlycoCT. Please use a and b for alpha and beta respectively")
			@RequestParam String sequenceString) {
		
		CFGMasterListParser parser = new CFGMasterListParser();
		return parser.translateSequence(sequenceString);
	}
}
