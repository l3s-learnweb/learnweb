package de.l3s.learnweb.beans;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;

/*
@ManagedBean
@ApplicationScoped
*/
public class ResourceMetaDataBean
{
    private static final Logger log = Logger.getLogger(ResourceMetaDataBean.class);
    private static final String[] LANGUAGES = { "af", "am", "ar", "arq", "as", "ast", "az", "be", "bg", "bi", "bn", "bo", "bs", "ca", "ceb", "cs", "da", "de", "el", "en", "eo", "es", "et", "eu", "fa", "fi", "fil", "fr", "fr-ca", "ga", "gl", "gu", "ha", "he", "hi", "hr", "ht",
            "hu", "hup", "hy", "id", "inh", "is", "it", "ja", "ka", "kk", "km", "kn", "ko", "ku", "ky", "la", "lb", "level", "lo", "lt", "ltg", "lv", "mg", "mk", "ml", "mn", "mr", "ms", "mt", "my", "nb", "ne", "nl", "nn", "oc", "pl", "pt", "pt-br", "ro", "ru", "rup", "sh", "si",
            "sk", "sl", "so", "sq", "sr", "srp", "sv", "sw", "szl", "ta", "te", "tg", "th", "tl", "tr", "tt", "ug", "uk", "ur", "uz", "vi", "zh", "zh-cn", "zh-tw" };

    private static final HashMap<String, List<SelectItem>> languageLists = new HashMap<String, List<SelectItem>>();
    private static final HashMap<Integer, List<String>> authorLists = new HashMap<>();

    public static List<SelectItem> getLanguageList()
    {
        String locale = UtilBean.getUserBean().getLocaleAsString();
        List<SelectItem> languageList;

        languageList = languageLists.get(locale);

        if(null == languageList)
        {
            synchronized(languageLists)
            {
                languageList = new ArrayList<>(LANGUAGES.length);

                for(String language : LANGUAGES)
                {
                    languageList.add(new SelectItem(language, UtilBean.getLocaleMessage("language_" + language)));
                }
                Collections.sort(languageList, TedTranscriptBean.languageComparator());

                languageLists.put(locale, languageList);
            }
        }

        return languageList;
    }

    /**
     * Very inefficient implementation
     * TODO improve
     * 
     * @param query
     * @return
     */
    public static List<String> completeAuthor(String query)
    {
        User user = UtilBean.getUserBean().getUser();
        if(user == null) // not logged in
            return null;

        int organisationId = user.getOrganisation().getId();

        List<String> authors = authorLists.get(organisationId);

        if(authors == null)
        {
            authors = loadAuthors(organisationId);
            //return null;
        }

        query = query.toLowerCase();
        List<String> suggestions = new LinkedList<>();
        for(String author : authors)
        {
            if(author.toLowerCase().startsWith(query))
                suggestions.add(author);
        }
        return suggestions;
    }

    private static List<String> loadAuthors(int organisationId)
    {
        try
        {
            HashSet<String> uniqueAuthors = new HashSet<>();

            Connection connection = Learnweb.getInstance().getConnection();
            PreparedStatement select = connection.prepareStatement("SELECT author FROM `lw_course` JOIN lw_group USING(course_id) JOIN lw_resource USING(group_id) WHERE `organisation_id` = ?");
            select.setInt(1, 480);
            ResultSet rs = select.executeQuery();
            while(rs.next())
            {
                String author = rs.getString(1);
                if(author == null || author.length() < 2)
                    continue;

                uniqueAuthors.add(author);
            }
            rs.close();
            select.close();

            ArrayList<String> authors = new ArrayList<>();
            authors.addAll(uniqueAuthors);

            authorLists.put(organisationId, authors);

            log.debug("Load " + authors.size() + "authors of organisation: " + organisationId);

            return authors;
        }
        catch(Exception e)
        {
            log.fatal("Can't complete author; organisation=" + organisationId, e);
        }
        return null;
    }
}
