package de.l3s.learnweb.user;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.util.StringHelper;
import de.l3s.util.SqlHelper;

/**
 * DAO for the Organisation class.
 * Because there are only a few organisations we keep them all in memory
 *
 * @author Philipp Kemkes
 */
public class OrganisationManager {
    //private static final Logger log = LogManager.getLogger(OrganisationManager.class);
    private static final int FIELDS = 1;
    private static final String[] COLUMNS = {"organisation_id", "title", "logout_page", "welcome_page", "welcome_message",
        "options_field1", "default_search_text", "default_search_image", "default_search_video", "default_language",
        "language_variant", "banner_image_file_id", "glossary_languages", "css_file"};
    private static final String SAVE = SqlHelper.generateInsertQuery("lw_organisation", COLUMNS);

    private final Learnweb learnweb;
    private final Map<Integer, Organisation> cache;

    public OrganisationManager(Learnweb learnweb) throws SQLException {
        this.learnweb = learnweb;
        // TODO @astappiev: Cache is not updated when organization changed, also user stores organization which is separated from cache.
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(30));
        this.resetCache();
    }

    public void resetCache() throws SQLException {
        cache.clear();

        // load all organizations into cache
        try (Statement select = learnweb.getConnection().createStatement()) {
            ResultSet rs = select.executeQuery("SELECT * FROM lw_organisation ORDER BY title");
            while (rs.next()) {
                Organisation organisation = createOrganisation(rs);
                cache.put(rs.getInt("organisation_id"), organisation);
            }
        }
    }

    /**
     * @return number of cached objects
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Returns a list of all Organisations.
     *
     * @return The collection is unmodifiable
     */
    public Collection<Organisation> getOrganisationsAll() {
        return Collections.unmodifiableCollection(cache.values());
    }

    /**
     * Get an Organisation by her id.
     *
     * @return null if not found
     */
    public Organisation getOrganisationById(int id) {
        return cache.get(id);
    }

    /**
     * Get an Organisation by her title.
     *
     * @return null if not found
     */
    public Organisation getOrganisationByTitle(String title) {
        for (Organisation org : cache.values()) {
            if (org.getTitle().equalsIgnoreCase(title)) {
                return org;
            }
        }

        return null;
    }

    /**
     * Saves the organisation to the database.
     * If the organisation is not yet stored at the database, a new record will be created and the returned organisation contains the new id.
     */
    public Organisation save(Organisation organisation) throws SQLException {
        if (organisation.getId() < 0) { // the organisation is not yet stored at the database // we have to get a new id from the groupManager
            Group group = new Group();
            group.setTitle(organisation.getTitle());
            group.setDescription("Organisation");
            learnweb.getGroupManager().save(group);
            organisation.setId(group.getId());
            group.delete();

            cache.put(organisation.getId(), organisation);
        }

        try (PreparedStatement save = learnweb.getConnection().prepareStatement(SAVE)) {
            save.setInt(1, organisation.getId());
            save.setString(2, organisation.getTitle());
            save.setString(3, organisation.getLogoutPage());
            save.setString(4, organisation.getWelcomePage());
            save.setString(5, organisation.getWelcomeMessage());
            save.setLong(6, organisation.getOptions()[0]);
            save.setString(7, organisation.getDefaultSearchServiceText().name());
            save.setString(8, organisation.getDefaultSearchServiceImage().name());
            save.setString(9, organisation.getDefaultSearchServiceVideo().name());
            save.setString(10, organisation.getDefaultLanguage());
            save.setString(11, organisation.getLanguageVariant());
            save.setInt(12, organisation.getBannerImageFileId());
            save.setString(13, StringHelper.join(organisation.getGlossaryLanguages()));
            save.setString(14, organisation.getCssFile());
            save.executeUpdate();
        }

        return organisation;
    }

    private Organisation createOrganisation(ResultSet rs) throws SQLException {
        Organisation organisation = new Organisation(rs.getInt("organisation_id"));
        organisation.setTitle(rs.getString("title"));
        organisation.setLogoutPage(rs.getString("logout_page"));
        organisation.setWelcomePage(rs.getString("welcome_page"));
        organisation.setWelcomeMessage(rs.getString("welcome_message"));
        organisation.setDefaultSearchServiceText(rs.getString("default_search_text"));
        organisation.setDefaultSearchServiceImage(rs.getString("default_search_image"));
        organisation.setDefaultSearchServiceVideo(rs.getString("default_search_video"));
        organisation.setDefaultLanguage(rs.getString("default_language"));
        organisation.setLanguageVariant(rs.getString("language_variant"));
        organisation.setBannerImageFileId(rs.getInt("banner_image_file_id"));
        organisation.setGlossaryLanguages(StringHelper.splitLocales(rs.getString("glossary_languages")));

        long[] options = new long[FIELDS];
        for (int i = 0; i < FIELDS; i++) {
            options[i] = rs.getLong("options_field" + (i + 1));
        }
        organisation.setOptions(options);

        return organisation;
    }
}
