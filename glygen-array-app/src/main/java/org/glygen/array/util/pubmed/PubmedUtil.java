package org.glygen.array.util.pubmed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class PubmedUtil
{
	private String m_strPubmedURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&retmode=xml&id=";
	private HashMap<String, Element> m_hashElements = new HashMap<String, Element>();
	private Document m_document = null;
	private Pattern m_patternYear = Pattern.compile(".*([0-9]{4}).*"); 
	private Pattern m_patternPages = Pattern.compile("^([0-9]+)(?:\\-([0-9]+))?$"); 
	
	/**
	 * Create an PublicationObject base on a PubmedID
	 * @param a_pubmedId ID in the Pubmed database for this publication
	 * @return A filled PublicationObject with the information provided by Pubmed or null
	 * @throws MalformedURLException
	 * @throws Exception
	 */
	public DTOPublication createFromPubmedId(Integer a_pubmedId) throws MalformedURLException, Exception
    {     
		this.clear();
        String t_strAnswer = this.makeHttpRequest(new URL(this.m_strPubmedURL + a_pubmedId.toString()));
        DTOPublication t_result = this.createFromPubmed(t_strAnswer);
        if ( t_result==null)
        {
        	return null;
        }
        if (!a_pubmedId.equals(t_result.getPubmedId())) 
        {
            return null;
        }        
        return t_result;
    }

	/**
	 * Reset member varialbes
	 */
    private void clear() 
    {
		this.m_document = null;
		this.m_hashElements.clear();
	}

    /**
     * Perform an HTTP request and return the answer content.
     * @param a_url URL of the HTTP request
     * @return Content of the answer (XML).
     * @throws Exception
     */
	private String makeHttpRequest(URL a_url) throws Exception 
    {                 
        // read result
        URLConnection t_connection = a_url.openConnection();
        t_connection.setUseCaches(false); 

        BufferedReader t_reader = new BufferedReader(new InputStreamReader(t_connection.getInputStream()));
        int t_count;
        StringBuilder t_result = new StringBuilder();
        while( (t_count = t_reader.read())!= -1 ) 
        {
            t_result.appendCodePoint(t_count);
        }
        return t_result.toString();
    }

	/**
	 * Create an DTOPublication base on a Pubmed XML string.
	 * @param a_strXML Pubmed XML for this publication.
	 * @return A filled DTOPublication with the information provided by Pubmed or null
	 * @throws JDOMException
	 * @throws IOException
	 */
    private DTOPublication createFromPubmed(String a_strXML) throws JDOMException, IOException 
    {
        if ( !this.readXML(a_strXML) ) 
        {
            return null;
        }
        DTOPublication t_result = new DTOPublication();

        // parse IDs
        t_result.setPubmedId( this.getPubmedId() );
        t_result.setDoiId( this.getPubmedItem("DOI") );
        if ( t_result.getDoiId() == null )
        {
        	t_result.setDoiId( this.getPubmedSubItem("ArticleIds", "doi") );
        }
        
        // parse type
        t_result.setType(this.getPubmedSubItem("PubTypeList", "PubType"));
        
        // title etc.
        t_result.setTitle( this.getPubmedItem("Title") );
        t_result.setJournal( this.getPubmedItem("FullJournalName") );
        t_result.setVolume( this.getPubmedItem("Volume") );
        t_result.setNumber( this.getPubmedItem("Issue") );
                    
        // parse year
        Matcher t_matcherYear = this.m_patternYear.matcher(getPubmedItem("PubDate"));
        if( t_matcherYear.matches() )
        {
            t_result.setYear(Integer.valueOf(t_matcherYear.group(1)));
        }
        
        // parse pages
        String t_strStart = null;
        String t_strEnd = null;
        Matcher pm = this.m_patternPages.matcher(getPubmedItem("Pages"));
        if( pm.matches() )
        {
            if( pm.group(1)!=null && pm.group(1).length()>0 ) 
            {
            	t_strStart = pm.group(1);
                if( pm.group(2)!=null && pm.group(2).length()>0 ) 
                {
                	t_strEnd = pm.group(2);
                    if ( t_strEnd.length() < t_strStart.length() )
                    {
                    	t_strEnd = t_strStart.substring(0, t_strStart.length() - t_strEnd.length() ) + t_strEnd;
                    }
                } 
            }
        }
        if ( t_strStart == null )
        {
        	t_result.setStartPage(null);
        }
        else
        {
        	t_result.setStartPage(t_strStart);
        }
        if ( t_strEnd == null )
        {
        	t_result.setEndPage(null);
        }
        else
        {
        	t_result.setEndPage(t_strEnd);
        }
        t_result.setAuthors( this.getAuthors() );
        return t_result;
    }

	@SuppressWarnings("unchecked")
	private List<DTOPublicationAuthor> getAuthors() 
	{
		ArrayList<DTOPublicationAuthor> t_result = new ArrayList<DTOPublicationAuthor>();
		Element t_elmentAuthorList = this.m_hashElements.get("AuthorList");
		if ( t_elmentAuthorList == null )
		{
			return t_result;
		}
		List<Element> t_childItem = t_elmentAuthorList.getChildren("Item");
		for (Element t_elementItem : t_childItem) 
		{
			String t_strValue = t_elementItem.getAttributeValue("Name");
			if ( t_strValue != null )
			{
				t_result.add(this.parseAuthorName(t_elementItem.getText()));
			}
		}
		return t_result;
	}

	/**
	 * Gets a Pubmed author name and fills a DTOPublicationAuthor object.
	 * @param a_strText Pubmed author name.
	 * @return Filled DTOPublicationAuthor object.
	 */
	private DTOPublicationAuthor parseAuthorName(String a_strText) 
	{
		DTOPublicationAuthor t_author = new DTOPublicationAuthor();
		if ( a_strText == null )
		{
			return t_author;
		}	
		String t_strText = a_strText.trim();
		int t_iPos = t_strText.lastIndexOf(" ");
		if ( t_iPos == -1 )
		{
			t_author.setLastName(t_strText);
		}
		else
		{
			t_author.setFirstName(t_strText.substring(t_iPos+1));
			t_author.setLastName(t_strText.substring(0,t_iPos));
		}
		return t_author;
	}

	/**
	 * Gives the text of an item sub tag within an item main tag.
	 * @param a_strMainItem Attribute value for Name of the main item tag.
	 * @param a_strSubItem Attribute value for Name of the sub item tag.
	 * @return Text of the sub tag or null if tag does not exist.
	 */
	@SuppressWarnings("unchecked")
	private String getPubmedSubItem(String a_strMainItem, String a_strSubItem) 
	{
		Element t_elementMain = this.m_hashElements.get(a_strMainItem);
		if ( t_elementMain == null )
		{
			return null;
		}
		List<Element> t_childItem = t_elementMain.getChildren("Item");
		for (Element t_elementItem : t_childItem) 
		{
			String t_strValue = t_elementItem.getAttributeValue("Name");
			if ( t_strValue != null )
			{
				if ( t_strValue.equals(a_strSubItem) )
				{
					return t_elementItem.getText();
				}
			}
		}
		return null;
	}

	/**
	 * Give the text of the Id tag in the Pubmed XML.
	 * @return Text of the tag or null.
	 */
	@SuppressWarnings("unchecked")
	private Integer getPubmedId() 
	{
		Element t_elementDocSum = this.m_hashElements.get("DocSum");
		if ( t_elementDocSum == null )
		{
			return null;
		}
		List<Element> t_childItem = t_elementDocSum.getChildren("Id");
		for (Element t_elementId : t_childItem)
		{
			try 
			{
				return Integer.parseInt(t_elementId.getText());
			} 
			catch (Exception e) 
			{
				return null;
			}			
		}
		return null;
	}

	/**
	 * Parse the xml and fill the Hashmap which contains each item tag with its name attribute as key.
	 * @param a_strXML XML string.
	 * @return True if the format was valid, false otherwise.
	 * @throws JDOMException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private boolean readXML(String a_strXML) throws JDOMException, IOException 
	{
		SAXBuilder t_builder = new SAXBuilder();
		this.m_document = t_builder.build(new StringReader(a_strXML));
		Element t_elmentRoot = this.m_document.getRootElement();
		if ( !t_elmentRoot.getName().equals("eSummaryResult") )
		{
			return false;
		}
		List<Element> t_childDocSum = t_elmentRoot.getChildren("DocSum");
		if ( t_childDocSum.size() != 1 )
		{
			return false;
		}
		for (Element t_elementDocSum : t_childDocSum) 
		{
			this.m_hashElements.put ("DocSum",t_elementDocSum);
			List<Element> t_childItem = t_elementDocSum.getChildren("Item");
			for (Element t_elementItem : t_childItem) 
			{
				String t_strValue = t_elementItem.getAttributeValue("Name");
				if ( t_strValue != null )
				{
					this.m_hashElements.put(t_strValue, t_elementItem);
				}
			}			
		}
		return true;
	}
    
	/**
	 * Return the text of an item tag base on his name.
	 * @param a_strName Name attribute of the tag.
	 * @return Text of the item tag
	 */
    private String getPubmedItem(String a_strName) 
    {
    	Element t_element = this.m_hashElements.get(a_strName);
    	if ( t_element == null )
    	{
    		return null;
    	}
    	return t_element.getText();
    }

}
