package org.glygen.array.client;

import org.glygen.array.client.model.Confirmation;
import org.glygen.array.client.model.GlycanView;
import org.glygen.array.client.model.User;

public interface GlycanRestClient {
	Confirmation addGlycan (GlycanView glycan, User user);
	public void setUsername(String username);
	public void setPassword(String password);
}
