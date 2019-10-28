package org.glygen.array.client.model;

import java.util.List;

public class Publication {
	String uri;
	private List<String> authors;
    private Integer m_pubmedId = null;
    private String m_doiId = null;
    private String m_title = null;
    private String m_journal = null;
    private String m_startPage = null;
    private String m_endPage = null;
    private String m_volume = null;
    private Integer m_year = null;
    private String m_number = null;
    
    public String getUri() {
		return uri;
	}
    
    public void setUri(String uri) {
		this.uri = uri;
	}
    
	/**
	 * @return the authors
	 */
	public List<String> getAuthors() {
		return authors;
	}
	/**
	 * @param authors the authors to set
	 */
	public void setAuthors(List<String> authors) {
		this.authors = authors;
	}
	/**
	 * @return the m_pubmedId
	 */
	public Integer getM_pubmedId() {
		return m_pubmedId;
	}
	/**
	 * @param m_pubmedId the m_pubmedId to set
	 */
	public void setM_pubmedId(Integer m_pubmedId) {
		this.m_pubmedId = m_pubmedId;
	}
	/**
	 * @return the m_doiId
	 */
	public String getM_doiId() {
		return m_doiId;
	}
	/**
	 * @param m_doiId the m_doiId to set
	 */
	public void setM_doiId(String m_doiId) {
		this.m_doiId = m_doiId;
	}
	/**
	 * @return the m_title
	 */
	public String getM_title() {
		return m_title;
	}
	/**
	 * @param m_title the m_title to set
	 */
	public void setM_title(String m_title) {
		this.m_title = m_title;
	}
	/**
	 * @return the m_journal
	 */
	public String getM_journal() {
		return m_journal;
	}
	/**
	 * @param m_journal the m_journal to set
	 */
	public void setM_journal(String m_journal) {
		this.m_journal = m_journal;
	}
	/**
	 * @return the m_startPage
	 */
	public String getM_startPage() {
		return m_startPage;
	}
	/**
	 * @param m_startPage the m_startPage to set
	 */
	public void setM_startPage(String m_startPage) {
		this.m_startPage = m_startPage;
	}
	/**
	 * @return the m_endPage
	 */
	public String getM_endPage() {
		return m_endPage;
	}
	/**
	 * @param m_endPage the m_endPage to set
	 */
	public void setM_endPage(String m_endPage) {
		this.m_endPage = m_endPage;
	}
	/**
	 * @return the m_volume
	 */
	public String getM_volume() {
		return m_volume;
	}
	/**
	 * @param m_volume the m_volume to set
	 */
	public void setM_volume(String m_volume) {
		this.m_volume = m_volume;
	}
	/**
	 * @return the m_year
	 */
	public Integer getM_year() {
		return m_year;
	}
	/**
	 * @param m_year the m_year to set
	 */
	public void setM_year(Integer m_year) {
		this.m_year = m_year;
	}
	/**
	 * @return the m_number
	 */
	public String getM_number() {
		return m_number;
	}
	/**
	 * @param m_number the m_number to set
	 */
	public void setM_number(String m_number) {
		this.m_number = m_number;
	}

}
