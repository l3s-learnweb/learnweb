package de.l3s.learnweb.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.faces.model.SelectItem;
import javax.validation.constraints.NotBlank;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.validator.constraints.Length;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.MetadataField;
import de.l3s.learnweb.resource.MetadataField.MetadataType;
import de.l3s.learnweb.resource.ResourceMetaDataBean;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.util.HasId;

public class Organisation implements HasId, Serializable, Comparable<Organisation> {
    private static final long serialVersionUID = -5187205229505825818L;
    private static final Logger log = LogManager.getLogger(Organisation.class);

    // add new options add the end , don't delete options !!!!!
    // if you add 64 options you have to add one options_field{x} column in lw_organisation
    public enum Option {
        Resource_Hide_Star_rating,
        Resource_Hide_Thumb_rating,
        Groups_Hide_public_groups,
        Privacy_Proxy_enabled,
        Privacy_Anonymize_usernames,
        Resource_Show_Content_Annotation_Field, // not used any more
        Privacy_Logging_disabled,
        Privacy_Tracker_disabled,
        Users_Hide_language_switch,
        Glossary_Add_Watermark,
        Glossary_Mandatory_Description,
        Glossary_Enable_Import // enables the glossary file import
    }

    private int id;
    @NotBlank
    @Length(min = 2, max = 60)
    private String title;
    private String welcomeMessage;
    private String welcomePage = "myhome/welcome.jsf"; // page to show after login
    private ResourceService defaultSearchServiceText = ResourceService.bing;
    private ResourceService defaultSearchServiceImage = ResourceService.flickr;
    private ResourceService defaultSearchServiceVideo = ResourceService.youtube;
    private String defaultLanguage; // the language which is used after the user logged in
    private String languageVariant; // optional variant that is added to the selected language
    private BitSet options = new BitSet(Option.values().length);
    private List<MetadataField> metadataFields = new LinkedList<>();
    private transient String bannerImage;
    private Integer bannerImageFileId;
    private String cssFile; // optional CSS file to load
    private List<Locale> glossaryLanguages = new ArrayList<>(4); // languages that can be used to construct a glossary

    public Organisation(int id) {
        this.id = id;

        // set default options. This is only relevant for new organisation. for existing organisations the options will be set from DB
        setOption(Option.Resource_Hide_Thumb_rating, true);
        setOption(Option.Glossary_Enable_Import, true);

        createMetadataFields();
    }

    /**
     * Create a new organisation with default values to avoid NPEs.
     */
    public Organisation(String title) {
        this(-1);

        setTitle(title);
        setLanguageVariant(null);
        setGlossaryLanguages(null);
    }

    /**
     * @return The userIds of all organization members
     */
    public List<Integer> getUserIds() {
        List<User> users = getUsers();
        List<Integer> userIds = new ArrayList<>(users.size());

        for (User user : users) {
            userIds.add(user.getId());
        }
        return userIds;
    }

    /**
     * @return 2 letter language code or NULL
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    /**
     * @param defaultLanguage Expect NULL or two letter language code
     */
    public void setDefaultLanguage(String defaultLanguage) {
        if (StringUtils.isEmpty(defaultLanguage)) {
            defaultLanguage = null;
        }

        if (defaultLanguage != null && defaultLanguage.length() != 2) {
            throw new IllegalArgumentException("Expect null or two letter language code; Given: " + defaultLanguage);
        }

        this.defaultLanguage = defaultLanguage;
    }

    public List<MetadataField> getMetadataFields() {
        return metadataFields;
    }

    public void setMetadataFields(List<MetadataField> metadataFields) {
        this.metadataFields = metadataFields;
    }

    public List<Course> getCourses() {
        return Learnweb.dao().getCourseDao().findByOrganisationId(id);
    }

    @Override
    public String toString() {
        return "Organisation [id=" + id + ", title=" + title + "]";
    }

    @Override
    public int compareTo(Organisation o) {
        if (null == o) {
            return -1;
        }

        return title.compareTo(o.getTitle());
    }

    private void createMetadataFields() {
        MetadataField metadata;

        // define optional resource fields for some courses
        if (id == 480) { // YELL
            metadataFields.add(new MetadataField("noname", "Topical", MetadataType.FULLWIDTH_HEADER));
            metadataFields.add(new MetadataField("noname", "Please tell us about the topic of this resource. Edit if necessary.", MetadataType.FULLWIDTH_DESCRIPTION));

            metadataFields.add(new MetadataField("title", "title", MetadataType.INPUT_TEXT, true));

            metadataFields.add(new MetadataField("description", "description", MetadataType.INPUT_TEXTAREA));

            metadataFields.add(new MetadataField("noname", "Attributes", MetadataType.FULLWIDTH_HEADER));
            metadataFields.add(new MetadataField("noname", "Please tell us about the characteristics of this resource. Edit if necessary.", MetadataType.FULLWIDTH_DESCRIPTION));

            metadata = new MetadataField("author", "author", MetadataType.AUTOCOMPLETE) {
                private static final long serialVersionUID = -2914974737800412242L;

                @Override
                public List<String> completeText(String query) {
                    return ResourceMetaDataBean.completeAuthor(query);
                    /*try
                    {
                        return Learnweb.getInstance().getSolrClient().getAutoCompletion("author", query);
                    }
                    catch(SolrServerException | IOException e)
                    {
                        log.error("Couldn't get auto completion for query=" + query, e);
                    }
                    return null;*/
                }
            };
            metadata.setInfo("Please, carefully acknowledge authors of resources. In case the author is not clear, use all the details you have: URL, book reference, etc.");
            metadataFields.add(metadata);

            metadata = new MetadataField("language", "language", MetadataType.MULTIPLE_MENU) {
                private static final long serialVersionUID = 1934886927426174254L;

                @Override
                public List<SelectItem> getOptionsList() {
                    return ResourceMetaDataBean.getLanguageList();
                }
            };
            metadata.setInfo("Select the language of the resource content");
            metadataFields.add(metadata);

            metadata = new MetadataField("language_level", "Language level", MetadataType.MULTIPLE_MENU);
            metadata.setInfo("Select all that apply");
            metadata.getOptions().add("C2"); // TODO @astappiev: retrieve from a database
            metadata.getOptions().add("C1");
            metadata.getOptions().add("B2");
            metadata.getOptions().add("B1");
            metadata.getOptions().add("A2");
            metadata.getOptions().add("A1");
            metadataFields.add(metadata);

            metadataFields.add(new MetadataField("noname", "Context", MetadataType.FULLWIDTH_HEADER));
            metadataFields.add(new MetadataField("noname", "Please tell us for what purpose you are using this resource.", MetadataType.FULLWIDTH_DESCRIPTION));

            metadata = new MetadataField("yell_purpose", "Purpose of use", MetadataType.AUTOCOMPLETE_MULTIPLE);
            metadata.setInfo("Select all that apply");
            metadata.getOptions().add("Speaking"); // TODO @astappiev: retrieve from a database
            metadata.getOptions().add("Listening");
            metadata.getOptions().add("Reading");
            metadata.getOptions().add("Writing");
            metadata.getOptions().add("Class activities");
            metadata.getOptions().add("Cross-curricular resources / CLIL");
            metadata.getOptions().add("Storytelling");
            metadata.getOptions().add("Plurilingualism");
            metadata.getOptions().add("Special learning needs");
            metadata.getOptions().add("Learning apps");
            metadata.getOptions().add("Assessment");
            metadata.getOptions().add("Teacher education resources");
            metadata.getOptions().add("Lesson plans");
            metadata.getOptions().add("Multiliteracy");
            metadata.getOptions().add("Bibliographical resources");
            metadataFields.add(metadata);

            metadata = new MetadataField("yell_target", "Target Learner", MetadataType.MULTIPLE_MENU);
            metadata.setInfo("Select all that apply");
            metadata.getOptions().add("Teachers"); // TODO @astappiev: retrieve from a database
            metadata.getOptions().add("Adult learners");
            metadata.getOptions().add("Teens");
            metadata.getOptions().add("Young learners");
            metadata.getOptions().add("Pre-school");
            metadataFields.add(metadata);
        } else {
            metadataFields.add(new MetadataField("title", "title", MetadataType.INPUT_TEXT, true));
            metadataFields.add(new MetadataField("description", "description", MetadataType.INPUT_TEXTAREA));
        }
    }

    /**
     * A negative id indicates, that this object is not stored at the database.
     */
    public int getId() {
        return id;
    }

    /**
     * This method should only be called by OrganisationManager.
     */
    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    /**
     * The page that will be displayed after a user logs in.
     */
    public String getWelcomePage() {
        return welcomePage;
    }

    public void setWelcomePage(String welcomePage) {
        this.welcomePage = welcomePage;
    }

    public boolean getOption(Option option) {
        return options.get(option.ordinal());
    }

    public void setOption(Option option, boolean value) {
        options.set(option.ordinal(), value);
    }

    protected long[] getOptions() {
        long[] array = options.toLongArray();
        // if all values are false the array will be empty. But we need to return an array representing the values
        if (array.length == 0) {
            array = new long[1];
        }
        return array;
    }

    protected void setOptions(long[] optionValues) {
        this.options = BitSet.valueOf(optionValues);
    }

    public void setOptions(BitSet options) {
        this.options = options;
    }

    public ResourceService getDefaultSearchServiceText() {
        return defaultSearchServiceText;
    }

    public void setDefaultSearchServiceText(String defaultSearchServiceText) {
        this.defaultSearchServiceText = getServiceFromString(defaultSearchServiceText);
    }

    public void setDefaultSearchServiceText(ResourceService defaultSearchServiceText) {
        this.defaultSearchServiceText = defaultSearchServiceText;
    }

    public ResourceService getDefaultSearchServiceImage() {
        return defaultSearchServiceImage;
    }

    public void setDefaultSearchServiceImage(String defaultSearchServiceImage) {
        this.defaultSearchServiceImage = getServiceFromString(defaultSearchServiceImage);
    }

    public void setDefaultSearchServiceImage(ResourceService defaultSearchServiceImage) {
        this.defaultSearchServiceImage = defaultSearchServiceImage;
    }

    public ResourceService getDefaultSearchServiceVideo() {
        return defaultSearchServiceVideo;
    }

    public void setDefaultSearchServiceVideo(String defaultSearchServiceVideo) {
        this.defaultSearchServiceVideo = getServiceFromString(defaultSearchServiceVideo);
    }

    public void setDefaultSearchServiceVideo(ResourceService defaultSearchServiceVideo) {
        this.defaultSearchServiceVideo = defaultSearchServiceVideo;
    }

    public String getBannerImage() {
        if (id == 1249) {
            return "logos/logo_eumade4all.png";
        } else if (id == 1210) {
            return "logos/logo_lumsa.png";
        } else if (id == 480) {
            return "logos/logo_yell.png";
        }

        if (null == bannerImage) {
            if (bannerImageFileId == null) {
                return null;
            }

            File file = Learnweb.dao().getFileDao().findById(bannerImageFileId);

            if (file != null) {
                bannerImage = file.getUrl();
            } else {
                bannerImage = "";
            }
        }
        return bannerImage;
    }

    public Integer getBannerImageFileId() {
        return bannerImageFileId;
    }

    public void setBannerImageFileId(Integer bannerImageFileId) {
        this.bannerImageFileId = bannerImageFileId;
        this.bannerImage = null; // clear cache
    }

    public String getCssFile() {
        return cssFile;
    }

    public void setCssFile(String cssFile) {
        this.cssFile = cssFile;
    }

    public String getLanguageVariant() {
        return languageVariant;
    }

    public void setLanguageVariant(String languageVariant) {
        this.languageVariant = StringUtils.defaultString(languageVariant);
    }

    public List<Locale> getGlossaryLanguages() {
        return glossaryLanguages;
    }

    public void setGlossaryLanguages(List<Locale> glossaryLanguages) {
        if (CollectionUtils.isEmpty(glossaryLanguages)) { // Load default if not defined yet
            this.glossaryLanguages.clear();
            this.glossaryLanguages.add(new Locale("de"));
            this.glossaryLanguages.add(new Locale("it"));
            this.glossaryLanguages.add(new Locale("nl"));
            this.glossaryLanguages.add(new Locale("en"));
        } else {
            this.glossaryLanguages = glossaryLanguages;
        }
    }

    /**
     * @return All groups belonging to courses of this organization (sorted by title)
     */
    public List<Group> getGroups() {
        List<Group> groups = new LinkedList<>();
        for (Course course : getCourses()) {
            groups.addAll(course.getGroups());
        }

        Collections.sort(groups);
        return groups;
    }

    /**
     * @return all users that are registered to this organization
     */
    public List<User> getUsers() {
        return Learnweb.dao().getUserDao().findByOrganisationId(id);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Organisation that = (Organisation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    private static ResourceService getServiceFromString(String name) {
        try {
            return ResourceService.valueOf(name);
        } catch (Exception e) {
            log.fatal("Can't get service for {}", name, e);
        }
        return null;
    }
}
