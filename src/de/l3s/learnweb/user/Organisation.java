package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.validation.constraints.NotBlank;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.validator.constraints.Length;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.ResourceMetaDataBean;
import de.l3s.learnweb.resource.ResourceMetadataField;
import de.l3s.learnweb.resource.ResourceMetadataField.MetadataType;
import de.l3s.learnweb.resource.SERVICE;

public class Organisation implements Serializable, Comparable<Organisation>
{
    private static final long serialVersionUID = -5187205229505825818L;
    private static Logger log = Logger.getLogger(Organisation.class);

    // add new options add the end , don't delete options !!!!!
    // if you add 64 options you have to add one options_field{x} column in lw_organisation
    public enum Option implements Comparable<Option>
    {
        Resource_Hide_Star_rating,
        Resource_Hide_Thumb_rating,
        Groups_Hide_public_groups,
        Privacy_Proxy_enabled,
        Privacy_Anonymize_usernames,
        Resource_Show_Content_Annotation_Field,
        Privacy_Logging_disabled,
        Privacy_Tracker_disabled,
        Users_Hide_language_switch,
        Glossary_Add_Watermark,
        Glossary_Mandatory_Description
    }

    private int id;
    @NotBlank
    @Length(min = 2, max = 30)
    private String title;
    private String welcomeMessage;
    private String welcomePage = "/lw/myhome/welcome.jsf"; // page to show after login
    private String logoutPage; // page to show after logout
    private SERVICE defaultSearchServiceText = SERVICE.bing;
    private SERVICE defaultSearchServiceImage = SERVICE.flickr;
    private SERVICE defaultSearchServiceVideo = SERVICE.youtube;
    private String defaultLanguage; // the language which is used after the user logged in
    private String languageVariant; // optional variant that is added to the selected language
    private BitSet options = new BitSet(Option.values().length);
    private List<ResourceMetadataField> metadataFields = new LinkedList<>();
    private transient String bannerImage;
    private int bannerImageFileId;
    private String cssFile; // optional CSS file to load
    private List<Locale> glossaryLanguages = new ArrayList<>(4); // languages that can be used to construct a glossary

    public Organisation(int id)
    {
        this.id = id;

        createMetadataFields();
    }

    public List<User> getMembers() throws SQLException
    {
        return Learnweb.getInstance().getUserManager().getUsersByOrganisationId(id);
    }

    /**
     *
     * @return The userIds of all organization members
     * @throws SQLException
     */
    public List<Integer> getUserIds() throws SQLException
    {
        List<User> users = getMembers();
        List<Integer> userIds = new ArrayList<>(users.size());

        for(User user : users)
        {
            userIds.add(user.getId());
        }
        return userIds;
    }

    /**
     *
     * @return 2 letter language code or NULL
     */
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
        if(StringUtils.isEmpty(defaultLanguage))
            defaultLanguage = null;

        if(defaultLanguage != null && defaultLanguage.length() != 2)
            throw new IllegalArgumentException("Expect null or two letter language code; Given: " + defaultLanguage);

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
        return "Organisation [id=" + id + ", title=" + title + "]";
    }

    @Override
    public int compareTo(Organisation o)
    {
        if(null == o)
            return -1;

        return title.compareTo(o.getTitle());
    }

    private void createMetadataFields()
    {
        ResourceMetadataField metadata;

        // define optional resource fields for some courses
        if(id == 480) // YELL
        {
            metadata = new ResourceMetadataField("title", "title", MetadataType.INPUT_TEXT);
            metadata.setRequired(true);
            metadataFields.add(metadata);

            metadata = new ResourceMetadataField("author", "author", MetadataType.AUTOCOMPLETE)
            {
                private static final long serialVersionUID = -2914974737900412242L;

                @Override
                public List<String> completeText(String query)
                {
                    return ResourceMetaDataBean.completeAuthor(query);
                }
            };
            metadataFields.add(metadata);
            metadataFields.add(new ResourceMetadataField("description", "description", MetadataType.INPUT_TEXTAREA));
        }
        /*
        else if(id == 893 ) // Admin
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
        
            metadata = new ResourceMetadataField("author", "author", MetadataType.AUTOCOMPLETE)
            {
                private static final long serialVersionUID = -2914974737900412242L;
        
                @Override
                public List<String> completeText(String query)
                {
                    return ResourceMetaDataBean.completeAuthor(query);
                    /
                    try
                    {
                        return Learnweb.getInstance().getSolrClient().getAutoCompletion("author", query);
                    }
                    catch(SolrServerException | IOException e)
                    {
                        log.error("Couldn't get auto completion for query=" + query, e);
                    }
                    return null;
                    * /
                }
            };
            metadata.setInfo("Please, carefully acknowledge authors of resources. In case the author is not clear, use all the details you have: URL, book reference, etc");
            metadataFields.add(metadata);
        
            metadata = new ResourceMetadataField("yell_media_type", "Media Type", MetadataType.MULTIPLE_MENU);
            metadata.setInfo("Select all that apply");
            metadata.getOptions().add("Text");
            metadata.getOptions().add("Video");
            metadata.getOptions().add("Image");
            metadata.getOptions().add("Game");
            metadata.getOptions().add("App");
            metadataFields.add(metadata);
        
            metadata = new ResourceMetadataField("language", "language", MetadataType.MULTIPLE_MENU)
            {
                private static final long serialVersionUID = 1934886927426174254L;
        
                @Override
                public List<SelectItem> getOptionsList()
                {
                    return ResourceMetaDataBean.getLanguageList();
                }
            };
            metadata.setInfo("Select the language of the resource content");
            metadataFields.add(metadata);
        
            metadataFields.add(new ResourceMetadataField("noname", "Context", MetadataType.FULLWIDTH_HEADER));
            metadataFields.add(new ResourceMetadataField("noname", "Please tell us for what purpose you are using this resource.", MetadataType.FULLWIDTH_DESCRIPTION));
        
            metadata = new ResourceMetadataField("yell_purpose", "Purpose of use", MetadataType.MULTIPLE_MENU);
            metadata.setInfo("Select all that apply");
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
        
            metadata = new ResourceMetadataField("language_level", "Language level", MetadataType.MULTIPLE_MENU);
            metadata.setInfo("Select all that apply");
            metadata.getOptions().add("C2");
            metadata.getOptions().add("C1");
            metadata.getOptions().add("B2");
            metadata.getOptions().add("B1");
            metadata.getOptions().add("A2");
            metadata.getOptions().add("A1");
            metadata.setInfo("");
            metadataFields.add(metadata);
        
            metadataFields.add(new ResourceMetadataField("description", "description", MetadataType.INPUT_TEXTAREA));
        
        }*/
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
        return welcomePage;
    }

    public void setWelcomePage(String welcomePage)
    {
        this.welcomePage = welcomePage;
    }

    protected void setOptions(long[] optionValues)
    {
        this.options = BitSet.valueOf(optionValues);
    }

    public boolean getOption(Option option)
    {
        return options.get(option.ordinal());
    }

    public void setOption(Option option, boolean value)
    {
        options.set(option.ordinal(), value);
    }

    protected long[] getOptions()
    {
        long[] array = options.toLongArray();
        // if all values are false the array will be empty. But we need to return an array representing the values
        if(array.length == 0)
            array = new long[1];
        return array;
    }

    private static SERVICE getServiceFromString(String name)
    {
        try
        {
            return SERVICE.valueOf(name);
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

    public SERVICE getDefaultSearchServiceText()
    {
        return defaultSearchServiceText;
    }

    public void setDefaultSearchServiceText(SERVICE defaultSearchServiceText)
    {
        this.defaultSearchServiceText = defaultSearchServiceText;
    }

    public SERVICE getDefaultSearchServiceImage()
    {
        return defaultSearchServiceImage;
    }

    public void setDefaultSearchServiceImage(SERVICE defaultSearchServiceImage)
    {
        this.defaultSearchServiceImage = defaultSearchServiceImage;
    }

    public SERVICE getDefaultSearchServiceVideo()
    {
        return defaultSearchServiceVideo;
    }

    public void setDefaultSearchServiceVideo(SERVICE defaultSearchServiceVideo)
    {
        this.defaultSearchServiceVideo = defaultSearchServiceVideo;
    }

    public String getBannerImage() throws SQLException
    {
        if(null == bannerImage)
        {
            if(bannerImageFileId < 1)
                return null;

            // TODO temporal mapper for new layout to new image ids

            File file = Learnweb.getInstance().getFileManager().getFileById(bannerImageFileId);

            if(file != null)
                bannerImage = file.getUrl();
            else
                bannerImage = "";
        }
        return bannerImage;
    }

    public int getBannerImageFileId()
    {
        return bannerImageFileId;
    }

    public void setBannerImageFileId(int bannerImageFileId)
    {
        this.bannerImageFileId = bannerImageFileId;
        this.bannerImage = null; // clear cache
    }

    public String getCssFile()
    {
        return cssFile;
    }

    public void setCssFile(String cssFile)
    {
        this.cssFile = cssFile;
    }

    public String getLogoutPage()
    {
        return logoutPage;
    }

    public void setLogoutPage(String logoutPage)
    {
        if(StringUtils.isEmpty(logoutPage))
            this.logoutPage = "/lw/index.jsf";
        else
            this.logoutPage = logoutPage;
    }

    public String getLanguageVariant()
    {
        return languageVariant;
    }

    public void setLanguageVariant(String languageVariant)
    {
        this.languageVariant = StringUtils.defaultString(languageVariant);
    }

    public List<Locale> getGlossaryLanguages()
    {
        return glossaryLanguages;
    }

    public void setGlossaryLanguages(List<Locale> glossaryLanguages)
    {
        if(CollectionUtils.isEmpty(glossaryLanguages)) // Load default if not defined yet
        {
            this.glossaryLanguages.clear();
            this.glossaryLanguages.add(new Locale("de"));
            this.glossaryLanguages.add(new Locale("it"));
            this.glossaryLanguages.add(new Locale("nl"));
            this.glossaryLanguages.add(new Locale("en"));
        }
        else
        {
            this.glossaryLanguages = glossaryLanguages;
        }
    }

    public void setOptions(BitSet options)
    {
        this.options = options;
    }

    public void setMetadataFields(List<ResourceMetadataField> metadataFields)
    {
        this.metadataFields = metadataFields;
    }

    /**
     *
     * @return All groups belonging to courses of this organization (sorted by title)
     * @throws SQLException
     */
    public List<Group> getGroups() throws SQLException
    {
        List<Group> groups = new LinkedList<>();
        for(Course course : getCourses())
        {
            groups.addAll(course.getGroups());
        }

        Collections.sort(groups);
        return groups;
    }

    /**
     *
     * @return all users that are registered to this organization
     * @throws SQLException
     */
    public List<User> getUsers() throws SQLException
    {
        return Learnweb.getInstance().getUserManager().getUsersByOrganisationId(getId());
    }
}
