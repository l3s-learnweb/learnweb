package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.validation.constraints.Size;

import org.apache.log4j.Logger;

import de.l3s.learnweb.ResourceMetadataField.MetadataType;
import de.l3s.learnweb.SearchFilters.SERVICE;

public class Organisation implements Serializable, Comparable<Organisation>
{
    private static final long serialVersionUID = -5187205229505825818L;
    private static Logger log = Logger.getLogger(Organisation.class);

    // add new options add the end , don't delete options !!!!!
    // if you add 64 options you have to add one options_field{x} column in lw_organisation
    public static enum Option implements Comparable<Option>
    {
        Resource_Hide_Star_rating,
        Resource_Hide_Thumb_rating,
        Groups_Hide_public_groups,
        Misc_Proxy_enabled
    }

    private int id;
    @Size(min = 1, max = 60)
    private String title;
    private String logo;
    private String welcomeMessage;
    private String welcomePage;
    private SearchFilters.SERVICE defaultSearchServiceText = SERVICE.bing;
    private SearchFilters.SERVICE defaultSearchServiceImage = SERVICE.flickr;
    private SearchFilters.SERVICE defaultSearchServiceVideo = SERVICE.youtube;
    private String defaultLanguage = null; // the language which is used after the user logged in
    private long[] options = new long[1];
    private List<ResourceMetadataField> metadataFields = new LinkedList<ResourceMetadataField>();

    /**
     * Constructs a temporary object. Can be persisted by OrganisationManager.save()
     * 
     */
    public Organisation()
    {
        this.id = -1;
    }

    /**
     * This constructor should only be called by OrganisationManager
     * 
     * @param rs
     * @throws SQLException
     */
    protected Organisation(ResultSet rs) throws SQLException
    {
        this.id = rs.getInt("organisation_id");
        this.title = rs.getString("title");
        this.logo = rs.getString("logo");
        this.welcomePage = rs.getString("welcome_page");
        this.welcomeMessage = rs.getString("welcome_message");
        this.defaultLanguage = rs.getString("default_language");

        setDefaultSearchServiceText(rs.getString("default_search_text"));
        setDefaultSearchServiceImage(rs.getString("default_search_image"));
        setDefaultSearchServiceVideo(rs.getString("default_search_video"));

        for(int i = 0; i < 1;)
            options[i] = rs.getInt("options_field" + (++i));

        ResourceMetadataField metadata;

        // define optional resource fields for some courses
        if(id == 893 || id == 480) // Admin and YELL 
        {
            metadataFields.add(new ResourceMetadataField("noname", "Topical", MetadataType.FULLWIDTH_HEADER));
            metadataFields.add(new ResourceMetadataField("noname", "Please tell us about the topic of this resource. Edit if necessary.", MetadataType.FULLWIDTH_DESCRIPTION));

            metadata = new ResourceMetadataField("title", "title", MetadataType.INPUT_TEXT);
            metadata.setRequired(true);
            metadataFields.add(metadata);

            metadata = new ResourceMetadataField("category", "category", MetadataType.INPUT_TEXT);
            metadata.setRequired(false);
            metadataFields.add(metadata);

            metadataFields.add(new ResourceMetadataField("noname", "Attributes", MetadataType.FULLWIDTH_HEADER));
            metadataFields.add(new ResourceMetadataField("noname", "Please tell us about the characteristics of this resource. Edit if necessary.", MetadataType.FULLWIDTH_DESCRIPTION));

            metadata = new ResourceMetadataField("Source", "Source", MetadataType.INPUT_TEXT);
            metadataFields.add(metadata);

            metadata = new ResourceMetadataField("author", "author", MetadataType.INPUT_TEXT);
            metadata.setInfo("Please, carefully acknowledge authors of resources. In case the author is not clear, use all the details you have: URL, book reference, etc");
            metadataFields.add(metadata);

            metadata = new ResourceMetadataField("yell_media_type", "Media Type", MetadataType.MULTIPLE_MENU);
            metadata.getOptions().add("Text");
            metadata.getOptions().add("Video");
            metadata.getOptions().add("Image");
            metadata.getOptions().add("Game");
            metadata.getOptions().add("App");
            metadata.setInfo("select all that apply");
            metadataFields.add(metadata);

            metadata = new ResourceMetadataField("language", "language", MetadataType.ONE_MENU_EDITABLE);
            metadata.getOptions().add("german");
            metadata.getOptions().add("english");
            metadata.getOptions().add("french");
            metadata.getOptions().add("greek");
            metadataFields.add(metadata);

            metadataFields.add(new ResourceMetadataField("noname", "Context", MetadataType.FULLWIDTH_HEADER));
            metadataFields.add(new ResourceMetadataField("noname", "Please tell us for what purpose you are using this resource.", MetadataType.FULLWIDTH_DESCRIPTION));

            metadata = new ResourceMetadataField("yell_purpose", "Purpose of use", MetadataType.MULTIPLE_MENU);
            metadata.getOptions().add("speaking skills");
            metadata.getOptions().add("listening skills");
            metadata.getOptions().add("reading skills");
            metadata.getOptions().add("writing skills");
            metadata.getOptions().add("class activities");
            metadata.getOptions().add("lesson plans");
            metadata.getOptions().add("cross-curricular resources / CLIL");
            metadata.getOptions().add("story telling");
            metadata.getOptions().add("plural-lingualism");
            metadata.getOptions().add("special learning needs");
            metadata.getOptions().add("assessment");
            metadata.getOptions().add("teacher education resources");
            metadata.getOptions().add("other");
            metadataFields.add(metadata);

            metadata = new ResourceMetadataField("language_level", "Language level", MetadataType.INPUT_TEXT);
            metadata.getOptions().add("C2");
            metadata.getOptions().add("C1");
            metadata.getOptions().add("B2");
            metadata.getOptions().add("B1");
            metadata.getOptions().add("A2");
            metadata.getOptions().add("A1");
            metadata.setInfo("");
            metadataFields.add(metadata);
            /*
            metadata = new ResourceMetadataField("resource_topic", "Topic of resource", MetadataType.INPUT_TEXT);
            metadata.setInfo("Main topic the resource deals with. Examples: water, food, ecology, human rights, etc.");
            metadataFields.add(metadata);
            
            metadata = new ResourceMetadataField("yell_resource_teaching_type", "Type of teaching/learning resource", MetadataType.INPUT_TEXT);
            metadata.setInfo("Examples: ready-to-use activities, lesson plans, teacher education materials, learning strategies, language skills (speaking, listening, reading, writing), CLIL, plurilingualism, inclusive classrooms, videos, songs, game, etc.");
            metadataFields.add(metadata);
            
            metadata = new ResourceMetadataField("yell_keywords", "Keywords", MetadataType.INPUT_TEXT);
            metadata.setInfo("Please, write representative words or expressions for the resource. Examples: plurilingual education, crosswords, documentary, learning styles, etc.");
            metadata.setRequired(true);
            metadataFields.add(metadata);
            */
            metadataFields.add(new ResourceMetadataField("description", "description", MetadataType.INPUT_TEXTAREA));

        }
        else if(id == 848) // Demo (archive course)
        {
            metadata = new ResourceMetadataField("title", "title", MetadataType.INPUT_TEXT);
            metadata.setRequired(true);
            metadataFields.add(metadata);

            metadataFields.add(new ResourceMetadataField("collector", MetadataType.INPUT_TEXT, true));
            metadataFields.add(new ResourceMetadataField("coverage", MetadataType.INPUT_TEXT, true));
            metadataFields.add(new ResourceMetadataField("publisher", MetadataType.INPUT_TEXT, true));

            metadataFields.add(new ResourceMetadataField("description", "description", MetadataType.INPUT_TEXTAREA));
        }
        else
        {
            metadata = new ResourceMetadataField("title", "title", MetadataType.INPUT_TEXT);
            metadata.setRequired(true);
            metadataFields.add(metadata);

            metadataFields.add(new ResourceMetadataField("description", "description", MetadataType.INPUT_TEXTAREA));
        }
    }

    public String getDefaultLanguage()
    {
        return defaultLanguage;
    }

    /**
     * 
     * @param defaultLanguage Expect NULL or two letter language code
     */
    public void setDefaultLanguage(String defaultLanguage)
    {
        if(defaultLanguage != null && defaultLanguage.length() != 2)
            throw new IllegalArgumentException("Expect NULL or two letter language code");

        this.defaultLanguage = defaultLanguage;
    }

    public List<ResourceMetadataField> getMetadataFields()
    {
        return metadataFields;
    }

    public List<Course> getCourses() throws SQLException
    {
        return Learnweb.getInstance().getCourseManager().getCoursesByOrganisationId(id);
    }

    @Override
    public String toString()
    {
        return "Organisation [id=" + id + ", title=" + title + ", logo=" + logo + "]";
    }

    @Override
    public int compareTo(Organisation o)
    {
        if(null == o)
            return -1;

        return title.compareTo(o.getTitle());
    }

    /**
     * A negative id indicates, that this object is not stored at the database
     * 
     * @return
     */
    public int getId()
    {
        return id;
    }

    public String getTitle()
    {
        return title;
    }

    public String getLogo()
    {
        return logo;
    }

    public void setLogo(String logo)
    {
        this.logo = logo;
    }

    /**
     * This method should only be called by OrganisationManager
     * 
     * @param id
     */
    public void setId(int id)
    {
        this.id = id;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getWelcomeMessage()
    {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage)
    {
        this.welcomeMessage = welcomeMessage;
    }

    /**
     * The page that will be displayed after a user logs in
     * 
     * @return
     */
    public String getWelcomePage()
    {
        if(null == welcomePage || welcomePage.length() == 0)
            return "myhome/activity.jsf";

        return welcomePage;
    }

    public void setWelcomePage(String welcomePage)
    {
        this.welcomePage = welcomePage;
    }

    public boolean getOption(Option option)
    {
        int bit = option.ordinal();
        int field = bit >> 6;
        long bitMask = 1L << (bit % 64);

        return (options[field] & bitMask) == bitMask;
    }

    public void setOption(Option option, boolean value)
    {
        int bit = option.ordinal();
        int field = bit >> 6;
        long bitMask = 1L << (bit % 64);

        if(value) // is true set Bit to 1
        {
            options[field] |= bitMask;
        }
        else
        {
            options[field] &= ~bitMask;
        }
    }

    public long[] getOptions()
    {
        return options;
    }

    private static SearchFilters.SERVICE getServiceFromString(String name)
    {
        try
        {
            return SearchFilters.SERVICE.valueOf(name);
        }
        catch(Exception e)
        {
            log.fatal("Can't get service for " + name, e);
        }
        return null;
    }

    public void setDefaultSearchServiceText(String defaultSearchServiceText)
    {
        this.defaultSearchServiceText = getServiceFromString(defaultSearchServiceText);
    }

    public void setDefaultSearchServiceImage(String defaultSearchServiceImage)
    {
        this.defaultSearchServiceImage = getServiceFromString(defaultSearchServiceImage);
    }

    public void setDefaultSearchServiceVideo(String defaultSearchServiceVideo)
    {
        this.defaultSearchServiceVideo = getServiceFromString(defaultSearchServiceVideo);
    }

    public SearchFilters.SERVICE getDefaultSearchServiceText()
    {
        return defaultSearchServiceText;
    }

    public void setDefaultSearchServiceText(SearchFilters.SERVICE defaultSearchServiceText)
    {
        this.defaultSearchServiceText = defaultSearchServiceText;
    }

    public SearchFilters.SERVICE getDefaultSearchServiceImage()
    {
        return defaultSearchServiceImage;
    }

    public void setDefaultSearchServiceImage(SearchFilters.SERVICE defaultSearchServiceImage)
    {
        this.defaultSearchServiceImage = defaultSearchServiceImage;
    }

    public SearchFilters.SERVICE getDefaultSearchServiceVideo()
    {
        return defaultSearchServiceVideo;
    }

    public void setDefaultSearchServiceVideo(SearchFilters.SERVICE defaultSearchServiceVideo)
    {
        this.defaultSearchServiceVideo = defaultSearchServiceVideo;
    }
}
