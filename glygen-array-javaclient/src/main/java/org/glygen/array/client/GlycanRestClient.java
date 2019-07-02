package org.glygen.array.client;

import java.util.List;

import org.glygen.array.client.model.BlockLayout;
import org.glygen.array.client.model.Confirmation;
import org.glygen.array.client.model.GlycanView;
import org.glygen.array.client.model.LinkerView;
import org.glygen.array.client.model.SlideLayout;
import org.glygen.array.client.model.User;

public interface GlycanRestClient {
	Confirmation addGlycan (GlycanView glycan, User user);
	Confirmation addLinker (LinkerView linker, User user);
	Confirmation addBlockLayout (BlockLayout layout, User user);
	Confirmation addSlideLayout (SlideLayout layout, User user);
	public void setUsername(String username);
	public void setPassword(String password);
	public List<String> getDuplicates();
	public List<String> getEmpty();
}
