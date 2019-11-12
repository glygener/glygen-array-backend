package org.glygen.array.client;

import java.util.List;

import org.glygen.array.client.model.BlockLayout;
import org.glygen.array.client.model.Confirmation;
import org.glygen.array.client.model.Glycan;
import org.glygen.array.client.model.Linker;
import org.glygen.array.client.model.LinkerClassification;
import org.glygen.array.client.model.SlideLayout;
import org.glygen.array.client.model.User;

public interface GlycanRestClient {
	
	public static final String uriPrefix = "http://glygen.org/glygenarray/";
	
	String addGlycan (Glycan glycan, User user);
	String addLinker (Linker linker, User user);
	Confirmation addBlockLayout (BlockLayout layout, User user);
	Confirmation addSlideLayout (SlideLayout layout, User user);
	public void setUsername(String username);
	public void setPassword(String password);
	public void setURL (String url);
	public List<String> getDuplicates();
	public List<String> getEmpty();
	List<LinkerClassification> getLinkerClassifications();
}
