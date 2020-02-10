package org.glygen.array.client.model;

public class Publication {
	String uri;
	private String authors;
    private Integer pubmedId = null;
    private String doiId = null;
    private String title = null;
    private String journal = null;
    private String startPage = null;
    private String endPage = null;
    private String volume = null;
    private Integer year = null;
    private String number = null;
    
    public String getUri() {
		return uri;
	}
    
    public void setUri(String uri) {
		this.uri = uri;
	}
    
	/**
	 * @return the authors
	 */
	public String getAuthors() {
        return authors;
    }
	/**
	 * @param authors the authors to set
	 */
	public void setAuthors(String authors) {
        this.authors = authors;
    }

    /**
     * @return the pubmedId
     */
    public Integer getPubmedId() {
        return pubmedId;
    }

    /**
     * @param pubmedId the pubmedId to set
     */
    public void setPubmedId(Integer pubmedId) {
        this.pubmedId = pubmedId;
    }

    /**
     * @return the doiId
     */
    public String getDoiId() {
        return doiId;
    }

    /**
     * @param doiId the doiId to set
     */
    public void setDoiId(String doiId) {
        this.doiId = doiId;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the journal
     */
    public String getJournal() {
        return journal;
    }

    /**
     * @param journal the journal to set
     */
    public void setJournal(String journal) {
        this.journal = journal;
    }

    /**
     * @return the startPage
     */
    public String getStartPage() {
        return startPage;
    }

    /**
     * @param startPage the startPage to set
     */
    public void setStartPage(String startPage) {
        this.startPage = startPage;
    }

    /**
     * @return the endPage
     */
    public String getEndPage() {
        return endPage;
    }

    /**
     * @param endPage the endPage to set
     */
    public void setEndPage(String endPage) {
        this.endPage = endPage;
    }

    /**
     * @return the volume
     */
    public String getVolume() {
        return volume;
    }

    /**
     * @param volume the volume to set
     */
    public void setVolume(String volume) {
        this.volume = volume;
    }

    /**
     * @return the year
     */
    public Integer getYear() {
        return year;
    }

    /**
     * @param year the year to set
     */
    public void setYear(Integer year) {
        this.year = year;
    }

    /**
     * @return the number
     */
    public String getNumber() {
        return number;
    }

    /**
     * @param number the number to set
     */
    public void setNumber(String number) {
        this.number = number;
    }
	

}
