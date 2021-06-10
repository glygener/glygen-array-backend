package org.glygen.array.util.pubmed;

public class DTOPublicationAuthor
{
    private Integer m_id = null;
    private String m_firstName = null;
    private String m_middleName = null;
    private String m_lastName = null;
    private Integer m_order = 0;

    public Integer getId()
    {
        return m_id;
    }

    public void setId(Integer a_id)
    {
        m_id = a_id;
    }

    public String getFirstName()
    {
        return m_firstName;
    }

    public void setFirstName(String a_firstName)
    {
        m_firstName = a_firstName;
    }

    public String getMiddleName()
    {
        return m_middleName;
    }

    public void setMiddleName(String a_middleName)
    {
        m_middleName = a_middleName;
    }

    public String getLastName()
    {
        return m_lastName;
    }

    public void setLastName(String a_lastName)
    {
        m_lastName = a_lastName;
    }

    public Integer getOrder()
    {
        return m_order;
    }

    public void setOrder(Integer a_order)
    {
        m_order = a_order;
    }
}
