package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import de.l3s.learnweb.exceptions.NotFoundHttpException;
import de.l3s.learnweb.resource.search.SearchMode;
import de.l3s.util.Cache;
import de.l3s.util.ICache;
import de.l3s.util.SqlHelper;
import de.l3s.util.StringHelper;

@RegisterRowMapper(OrganisationDao.OrganisationMapper.class)
public interface OrganisationDao extends SqlObject, Serializable {
    ICache<Organisation> cache = new Cache<>(10000);

    default Optional<Organisation> findById(int organisationId) {
        return Optional.ofNullable(cache.get(organisationId))
            .or(() -> getHandle().select("SELECT * FROM lw_organisation WHERE organisation_id = ?", organisationId).mapTo(Organisation.class).findOne());
    }

    default Organisation findByIdOrElseThrow(int organisationId) {
        return findById(organisationId).orElseThrow(() -> new NotFoundHttpException("error_pages.not_found_group_description"));
    }

    @SqlQuery("SELECT * FROM lw_organisation ORDER BY title")
    List<Organisation> findAll();

    @SqlQuery("SELECT * FROM lw_organisation WHERE title = ?")
    Optional<Organisation> findByTitle(String title);

    @SqlQuery("SELECT DISTINCT r.author FROM lw_course c JOIN lw_group g USING(course_id) JOIN lw_resource r ON r.group_id = g.group_id WHERE r.author IS NOT NULL AND c.organisation_id = ?")
    List<String> findAuthors(int organisationId);

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
                organisation.setTheme(Theme.valueOf(rs.getString("theme")));
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
