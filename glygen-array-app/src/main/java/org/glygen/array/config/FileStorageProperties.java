package org.glygen.array.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {
	
	String imageDirectory;
	
	public String getImageDirectory() {
		return imageDirectory;
	}
	
	public void setImageDirectory(String imageDirectory) {
		this.imageDirectory = imageDirectory;
	}
}
