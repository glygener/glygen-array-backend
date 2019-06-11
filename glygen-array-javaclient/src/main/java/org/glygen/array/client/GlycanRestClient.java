package org.glygen.array.client;

import org.glygen.array.client.model.BlockLayout;
import org.glygen.array.client.model.Confirmation;
import org.glygen.array.client.model.GlycanView;
import org.glygen.array.client.model.LinkerView;
import org.glygen.array.client.model.User;

public interface GlycanRestClient {
	Confirmation addGlycan (GlycanView glycan, User user);
	Confirmation addLinker (LinkerView linker, User user);
	Confirmation addBlockLayout (BlockLayout layout, User user);
	public void setUsername(String username);
	public void setPassword(String password);
}
