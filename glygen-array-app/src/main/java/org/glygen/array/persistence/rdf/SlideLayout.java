package org.glygen.array.persistence.rdf;

import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

public class SlideLayout {
	String name;
	String description;
	List <Block> blocks;
	Integer width;
	Integer height;
	/**
	 * @return the name
	 */
	@Size(max=50, message="Name cannot exceed 50 characters")
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the description
	 */
	@Size(max=250, message="description cannot exceed 250 characters")
	public String getDescription() {
		return description;
	}
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @return the blocks
	 */
	public List<Block> getBlocks() {
		return blocks;
	}
	/**
	 * @param blocks the blocks to set
	 */
	public void setBlocks(List<Block> blocks) {
		this.blocks = blocks;
	}
	/**
	 * @return the width
	 */
	@Min(value=1, message = "width must be a positive integer")
	public Integer getWidth() {
		return width;
	}
	/**
	 * @param width the width to set
	 */
	public void setWidth(Integer width) {
		this.width = width;
	}
	/**
	 * @return the height
	 */
	@Min(value=1, message = "height must be a positive integer")
	public Integer getHeight() {
		return height;
	}
	/**
	 * @param height the height to set
	 */
	public void setHeight(Integer height) {
		this.height = height;
	}

	
}
