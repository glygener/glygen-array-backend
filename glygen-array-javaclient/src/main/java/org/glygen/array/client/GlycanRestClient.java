package org.glygen.array.client;

import java.util.List;
import java.util.Map;

import org.glygen.array.client.model.BlockLayout;
import org.glygen.array.client.model.Feature;
import org.glygen.array.client.model.Glycan;
import org.glygen.array.client.model.ImportGRITSLibraryResult;
import org.glygen.array.client.model.Linker;
import org.glygen.array.client.model.LinkerClassification;
import org.glygen.array.client.model.SlideLayout;
import org.glygen.array.client.model.User;
import org.grits.toolbox.glycanarray.library.om.ArrayDesignLibrary;

public interface GlycanRestClient {
	
	public static final String uriPrefix = "http://glygen.org/glygenarray/";
	
	String addGlycan (Glycan glycan, User user);
	String addLinker (Linker linker, User user);
	String addFeature (Feature feature, User user);
	String addBlockLayout (BlockLayout layout, User user);
	String addSlideLayout (SlideLayout layout, User user);
	public void setUsername(String username);
	public void setPassword(String password);
	public void setURL (String url);
	public List<String> getDuplicates();
	public List<String> getEmpty();
	List<LinkerClassification> getLinkerClassifications();
	
	/** 
	 * 
	 * @param library library file to get slide layout
	 * @param linkerClassificationMap TODO
	 * @param layout layout name to import, if null, all slide layouts will be imported
	 * @param user user login
	 * @return a result object with the added slide layouts, errors and duplicates
	 */
	public ImportGRITSLibraryResult addFromLibrary (ArrayDesignLibrary library, Map<String, String> linkerClassificationMap, String layout, User user);
}
