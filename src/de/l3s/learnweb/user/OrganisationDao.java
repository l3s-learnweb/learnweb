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

import de.l3s.util.Cache;
import de.l3s.util.ICache;
import de.l3s.util.RsHelper;
import de.l3s.util.SqlHelper;
import de.l3s.util.StringHelper;

@RegisterRowMapper(OrganisationDao.OrganisationMapper.class)
public interface OrganisationDao extends SqlObject, Serializable {
    int FIELDS = 1; // number of options_fieldX fields, increase if Organisation.Options has more than 64 values
    ICache<Organisation> cache = new Cache<>(10000);

    default Organisation findById(int organisationId) {
        Organisation organisation = cache.get(organisationId);
        if (organisation != null) {
            return organisation;
        }

        return getHandle().select("SELECT * FROM lw_organisation WHERE organisation_id = ?", organisationId)
            .map(new OrganisationMapper()).findOne().orElse(null);
    }

    @SqlQuery("SELECT * FROM lw_organisation WHERE is_default = 1")
    Organisation findDefault();

    @SqlQuery("SELECT * FROM lw_organisation ORDER BY title")
    List<Organisation> findAll();

    @SqlQuery("SELECT * FROM lw_organisation WHERE title = ?")
    Optional<Organisation> findByTitle(String title);

    @SqlQuery("SELECT DISTINCT r.author FROM lw_course c JOIN lw_group g USING(course_id) JOIN lw_resource r ON r.group_id = g.group_id WHERE c.organisation_id = ?")
    List<String> findAuthors(int organisationId);

    default void save(Organisation organisation) {
        // TODO: why do we need this?
        // if (organisation.getId() < 0) { // the organisation is not yet stored at the database // we have to get a new id from the groupManager
        //     Group group = new Group();
        //     group.setTitle(organisation.getTitle());
        //     group.setDescription("Organisation");
        //     groupDao.save(group);
        //
        //     organisation.setId(group.getId());
        //     group.delete();
        // }

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("organisation_id", organisation.getId() < 1 ? null : organisation.getId());
        params.put("title", organisation.getTitle());
        params.put("welcome_page", organisation.getWelcomePage());
        params.put("welcome_message", organisation.getWelcomeMessage());
        params.put("options_field1", organisation.getOptions()[0]);
        params.put("default_search_text", organisation.getDefaultSearchServiceText());
        params.put("default_search_image", organisation.getDefaultSearchServiceImage());
        params.put("default_search_video", organisation.getDefaultSearchServiceVideo());
        params.put("default_language", organisation.getDefaultLanguage());
        params.put("language_variant", organisation.getLanguageVariant());
        params.put("banner_image_file_id", organisation.getBannerImageFileId());
        params.put("glossary_languages", StringHelper.join(organisation.getGlossaryLanguages()));
        params.put("css_file", organisation.getCssFile());

        Optional<Integer> organisationId = SqlHelper.handleSave(getHandle(), "lw_organisation", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        organisationId.ifPresent(organisation::setId);
        cache.put(organisation);
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
                organisation.setDefaultSearchServiceText(rs.getString("default_search_text"));
                organisation.setDefaultSearchServiceImage(rs.getString("default_search_image"));
                organisation.setDefaultSearchServiceVideo(rs.getString("default_search_video"));
                organisation.setDefaultLanguage(rs.getString("default_language"));
                organisation.setLanguageVariant(rs.getString("language_variant"));
                organisation.setBannerImageFileId(RsHelper.getInteger(rs, "banner_image_file_id"));
                organisation.setGlossaryLanguages(StringHelper.splitLocales(rs.getString("glossary_languages")));

                long[] options = new long[FIELDS];
                for (int i = 0; i < FIELDS; i++) {
                    options[i] = rs.getLong("options_field" + (i + 1));
                }
                organisation.setOptions(options);
                cache.put(organisation);
            }
            return organisation;
        }
    }
}
