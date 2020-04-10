package org.glygen.array.util.pubmed;


import java.util.ArrayList;
import java.util.List;


public class DTOPublication
{
    private Integer m_id = null;
    private Integer m_pubmedId = null;
    private String m_doiId = null;
    private String m_title = null;
    private String m_journal = null;
    private String m_startPage = null;
    private String m_endPage = null;
    private String m_volume = null;
    private Integer m_year = null;
    private String m_number = null;
    private IAuthorFormatter formattedAuthor = new AuthorFormatterPubmed();
    private List<DTOPublicationAuthor> m_authors = new ArrayList<DTOPublicationAuthor>();
    private String type;

    public Integer getId()
    {
        return m_id;
    }

    public void setId(Integer a_id)
    {
        m_id = a_id;
    }

    public Integer getPubmedId()
    {
        return m_pubmedId;
    }

    public void setPubmedId(Integer a_pubmedId)
    {
        m_pubmedId = a_pubmedId;
    }

    public String getDoiId()
    {
        return m_doiId;
    }

    public void setDoiId(String a_doiId)
    {
        m_doiId = a_doiId;
    }

    public String getTitle()
    {
        return m_title;
    }

    public void setTitle(String a_title)
    {
        m_title = a_title;
    }

    public String getJournal()
    {
        return m_journal;
    }

    public void setJournal(String a_journal)
    {
        m_journal = a_journal;
    }

    public String getStartPage()
    {
        return m_startPage;
    }

    public void setStartPage(String a_startPage)
    {
        m_startPage = a_startPage;
    }

    public String getEndPage()
    {
        return m_endPage;
    }

    public void setEndPage(String a_endPage)
    {
        m_endPage = a_endPage;
    }

    public String getVolume()
    {
        return m_volume;
    }

    public void setVolume(String a_volume)
    {
        m_volume = a_volume;
    }

    public Integer getYear()
    {
        return m_year;
    }

    public void setYear(Integer a_year)
    {
        m_year = a_year;
    }

    public String getNumber()
    {
        return m_number;
    }

    public void setNumber(String a_number)
    {
        m_number = a_number;
    }

    public List<DTOPublicationAuthor> getAuthors()
    {
        return m_authors;
    }

    public void setAuthors(List<DTOPublicationAuthor> a_authors)
    {
        m_authors = a_authors;
    }

    public String getFormattedAuthor()
    {
        return formattedAuthor.format(this.m_authors);
    }

    @Deprecated
    public void setFormattedAuthor(String author)
    {
        
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
}
