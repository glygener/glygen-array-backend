package org.glygen.array.client.model;

public class Block {
	String uri;
	String id;
	Integer row;
	Integer column;
	BlockLayout blockLayout;
	//List<Spot> spots;
	/**
	 * @return the row
	 */
	public Integer getRow() {
		return row;
	}
	/**
	 * @param row the row to set
	 */
	public void setRow(Integer row) {
		this.row = row;
	}
	/**
	 * @return the column
	 */
	public Integer getColumn() {
		return column;
	}
	/**
	 * @param column the column to set
	 */
	public void setColumn(Integer column) {
		this.column = column;
	}
	/**
	 * @return the blockLayout
	 */
	public BlockLayout getBlockLayout() {
		return blockLayout;
	}
	/**
	 * @param blockLayout the blockLayout to set
	 */
	public void setBlockLayout(BlockLayout blockLayout) {
		this.blockLayout = blockLayout;
	}
	/**
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}
	/**
	 * @param uri the uri to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
		if (uri != null) 
			this.id = uri.substring(uri.lastIndexOf("/")+1);
	}
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @return the spots
	 */
	//public List<Spot> getSpots() {
	//	return spots;
	//}
	/**
	 * @param spots the spots to set
	 */
	//public void setSpots(List<Spot> spots) {
	//	this.spots = spots;
	//}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
}
