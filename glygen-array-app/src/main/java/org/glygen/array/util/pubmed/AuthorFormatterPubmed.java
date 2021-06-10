package org.glygen.array.util.pubmed;

import java.util.Collections;
import java.util.List;

public class AuthorFormatterPubmed implements IAuthorFormatter
{

    public String format(List<DTOPublicationAuthor> mAuthors)
    {
        Collections.sort(mAuthors, new ComparatorPublicationAuthor());
        StringBuffer author = new StringBuffer();
        Boolean firstAuthor = true;
        for(DTOPublicationAuthor authorDTO : mAuthors)
        {
            if(firstAuthor)
            {
                firstAuthor = false;
            }
            else
            {
                author.append(", ");
            }
            author.append(authorDTO.getLastName());
            author.append(" ");
            author.append(this.firstCharacter(authorDTO.getFirstName()));
            author.append(this.firstCharacter(authorDTO.getMiddleName()));
        }
        author.append(".");
        return author.toString();
    }

    private String firstCharacter(String name)
    {
        if(name == null)
        {
            return "";
        }
        if(name.length() > 0)
        {
            return name.substring(0, 1);
        }
        return "";
    }

}
