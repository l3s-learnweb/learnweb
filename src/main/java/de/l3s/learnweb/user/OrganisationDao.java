package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import de.l3s.learnweb.exceptions.NotFoundHttpException;
import de.l3s.learnweb.resource.search.SearchMode;
import de.l3s.util.Cache;
import de.l3s.util.ICache;
import de.l3s.util.SqlHelper;
import de.l3s.util.StringHelper;

public interface OrganisationDao extends SqlObject, Serializable {
    ICache<Organisation> cache = new Cache<>(10000);

    default Optional<Organisation> findById(int organisationId) {
        return Optional.ofNullable(cache.get(organisationId))
            .or(() -> getHandle().select("SELECT * FROM lw_organisation WHERE organisation_id = ?", organisationId)
                .registerRowMapper(new OrganisationMapper())
                .mapTo(Organisation.class)
                .map(organisation -> {
                    organisation.setSettings(loadSettings(organisationId));
                    return organisation;
                }).findOne());
    }

    default Organisation findByIdOrElseThrow(int organisationId) {
        return findById(organisationId).orElseThrow(() -> new NotFoundHttpException("error_pages.not_found_group_description"));
    }

    default List<Organisation> findAll() {
        return getHandle().select("SELECT * FROM lw_organisation ORDER BY title")
            .registerRowMapper(new OrganisationMapper())
            .mapTo(Organisation.class)
            .map(organisation -> {
                if (organisation.getSettings() == null) {
                    organisation.setSettings(loadSettings(organisation.getId()));
                }
                return organisation;
            }).list();
    }

    @SqlQuery("SELECT COUNT(*) FROM lw_organisation WHERE title = ?")
    int countByTitle(String title);

    @SqlQuery("SELECT DISTINCT r.author FROM lw_course c JOIN lw_group g USING(course_id) JOIN lw_resource r ON r.group_id = g.group_id WHERE r.author IS NOT NULL AND c.organisation_id = ?")
    List<String> findAuthors(int organisationId);

    default OrganisationSettings loadSettings(int organisationId) {
        return getHandle().select("SELECT setting_key, setting_value FROM lw_organisation_settings WHERE organisation_id = ?", organisationId)
            .reduceResultSet(new OrganisationSettings(), (settings, rs, ctx) -> {
                Settings key = Settings.valueOf(rs.getString("setting_key"));
                String value = rs.getString("setting_value");

                if (key.getType() == Integer.class) {
                    settings.setValue(key, Integer.parseInt(value));
                } else if (key.getType() == Boolean.class) {
                    settings.setValue(key, Boolean.parseBoolean(value));
                } else {
                    settings.setValue(key, value);
                }
                return settings;
            });
    }

    default void saveSettings(int organisationId, OrganisationSettings settings) {
        PreparedBatch batch = getHandle().prepareBatch("INSERT INTO lw_organisation_settings (organisation_id, setting_key, setting_value) VALUES (?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)");

        for (Map.Entry<Settings, Object> entry : settings.getValues()) {
            batch.bind(0, organisationId).bind(1, entry.getKey().name()).bind(2, entry.getValue().toString()).add();
        }

        batch.execute();
    }

    default void save(Organisation organisation) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("organisation_id", SqlHelper.toNullable(organisation.getId()));
        params.put("title", organisation.getTitle());
        params.put("welcome_page", organisation.getWelcomePage());
        params.put("welcome_message", SqlHelper.toNullable(organisation.getWelcomeMessage()));
        params.put("terms_and_conditions", SqlHelper.toNullable(organisation.getTermsAndConditions()));
        params.put("theme", organisation.getTheme());
        params.put("default_search_mode", organisation.getDefaultSearchMode());
        params.put("default_search_text", organisation.getDefaultSearchServiceText());
        params.put("default_search_image", organisation.getDefaultSearchServiceImage());
        params.put("default_search_video", organisation.getDefaultSearchServiceVideo());
        params.put("default_language", organisation.getDefaultLanguage());
        params.put("language_variant", organisation.getLanguageVariant());
        params.put("banner_image_file_id", SqlHelper.toNullable(organisation.getBannerImageFileId()));
        params.put("tracker_api_key", SqlHelper.toNullable(organisation.getTrackerApiKey()));
        params.put("glossary_languages", StringHelper.join(organisation.getGlossaryLanguages()));
        SqlHelper.setBitSet(params, Organisation.Option.values().length, "options_field", organisation.getOptions());

        Optional<Integer> organisationId = SqlHelper.handleSave(getHandle(), "lw_organisation", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        saveSettings(organisationId.orElse(organisation.getId()), organisation.getSettings());

        if (organisationId.isPresent() && organisationId.get() != 0) {
            organisation.setId(organisationId.get());
            cache.put(organisation);
        }
    }

    class OrganisationMapper implements RowMapper<Organisation> {
        @Override
        public Organisation map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            Organisation organisation = cache.get(rs.getInt("organisation_id"));

            if (organisation == null) {
                organisation = new Organisation(rs.getInt("organisation_id"));
                organisation.setTitle(rs.getString("title"));
                organisation.setWelcomePage(rs.getString("welcome_page"));
                organisation.setWelcomeMessage(rs.getString("welcome_message"));
                organisation.setTermsAndConditions(rs.getString("terms_and_conditions"));
                organisation.setTheme(ColorTheme.valueOf(rs.getString("theme")));
                organisation.setDefaultSearchMode(SearchMode.valueOf(rs.getString("default_search_mode")));
                organisation.setDefaultSearchServiceText(rs.getString("default_search_text"));
                organisation.setDefaultSearchServiceImage(rs.getString("default_search_image"));
                organisation.setDefaultSearchServiceVideo(rs.getString("default_search_video"));
                organisation.setDefaultLanguage(rs.getString("default_language"));
                organisation.setLanguageVariant(rs.getString("language_variant"));
                organisation.setBannerImageFileId(rs.getInt("banner_image_file_id"));
                organisation.setTrackerApiKey(rs.getString("tracker_api_key"));
                organisation.setGlossaryLanguages(StringHelper.splitLocales(rs.getString("glossary_languages")));
                organisation.setOptions(SqlHelper.getBitSet(rs, Organisation.Option.values().length, "options_field"));
                cache.put(organisation);
            }
            return organisation;
        }
    }
}
