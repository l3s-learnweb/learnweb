package de.l3s.learnweb.app;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import com.zaxxer.hikari.HikariDataSource;

import de.l3s.learnweb.AnnouncementDao;
import de.l3s.learnweb.forum.ForumPostDao;
import de.l3s.learnweb.forum.ForumTopicDao;
import de.l3s.learnweb.group.FolderDao;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.logging.LogDao;
import de.l3s.learnweb.resource.CommentDao;
import de.l3s.learnweb.resource.FileDao;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.resource.TagDao;
import de.l3s.learnweb.resource.archive.ArchiveUrlDao;
import de.l3s.learnweb.resource.archive.WaybackUrlDao;
import de.l3s.learnweb.resource.glossary.GlossaryDao;
import de.l3s.learnweb.resource.glossary.GlossaryEntryDao;
import de.l3s.learnweb.resource.glossary.GlossaryTermDao;
import de.l3s.learnweb.resource.office.ResourceHistoryDao;
import de.l3s.learnweb.resource.speechRepository.SpeechRepositoryDao;
import de.l3s.learnweb.resource.submission.SubmissionDao;
import de.l3s.learnweb.resource.survey.SurveyDao;
import de.l3s.learnweb.resource.ted.TedTranscriptDao;
import de.l3s.learnweb.searchhistory.SearchHistoryDao;
import de.l3s.learnweb.user.CourseDao;
import de.l3s.learnweb.user.MessageDao;
import de.l3s.learnweb.user.OrganisationDao;
import de.l3s.learnweb.user.UserDao;
import de.l3s.learnweb.web.BanDao;
import de.l3s.learnweb.web.BounceDao;
import de.l3s.learnweb.web.RequestDao;

/**
 * An important class with Bean providers, following Jakarta EE CDI specifications.
 *
 * <h3> Annotation used in this class: </h3>
 * <ul>
 * <li> @ApplicationScoped - something like a singleton, means that the class should be created once per application
 *          and always stored in memory, required to have @Produces methods in it
 *          https://docs.jboss.org/weld/reference/latest-3.1/en-US/html_single/#_built_in_scopes
 * <li> @Produces - is a method that acts as a source of bean instances,
 *          when @Inject is used, CDI is looking for any Bean or producer method that returns requested type
 *          https://docs.jboss.org/weld/reference/latest-3.1/en-US/html_single/#_producer_methods
 * </ul>
 */
@ApplicationScoped
@SuppressWarnings({"OverlyCoupledClass", "OverlyCoupledMethod"})
public class DaoProvider {
    private final DataSource dataSource;
    private final Jdbi jdbi;

    // private SpeechRepositoryDao speechRepositoryDao;
    // private TrackerDao trackerDao;
    private final AnnouncementDao announcementDao;
    private final ArchiveUrlDao archiveUrlDao;
    private final BanDao banDao;
    private final BounceDao bounceDao;
    private final CommentDao commentDao;
    private final CourseDao courseDao;
    private final FileDao fileDao;
    private final FolderDao folderDao;
    private final ForumPostDao forumPostDao;
    private final ForumTopicDao forumTopicDao;
    private final GlossaryDao glossaryDao;
    private final GlossaryEntryDao glossaryEntryDao;
    private final GlossaryTermDao glossaryTermDao;
    private final GroupDao groupDao;
    private final LogDao logDao;
    private final MessageDao messageDao;
    private final OrganisationDao organisationDao;
    private final RequestDao requestDao;
    private final ResourceDao resourceDao;
    private final ResourceHistoryDao resourceHistoryDao;
    private final SearchHistoryDao searchHistoryDao;
    private final SpeechRepositoryDao speechRepositoryDao;
    private final SubmissionDao submissionDao;
    private final SurveyDao surveyDao;
    private final TagDao tagDao;
    private final TedTranscriptDao tedTranscriptDao;
    private final UserDao userDao;
    private final WaybackUrlDao waybackUrlDao;

    @Inject
    public DaoProvider(final ConfigProvider configProvider) {
        this(configProvider, createDataSource(configProvider));

        migrateDatabase();
    }

    public DaoProvider(final ConfigProvider configProvider, final DataSource dataSource) {
        this.dataSource = dataSource;

        // add configuration and register mappers if needed http://jdbi.org/
        jdbi = Jdbi.create(dataSource)
            .installPlugin(new SqlObjectPlugin());

        if (configProvider.getPropertyBoolean("mysql_log_queries")) {
            jdbi.setSqlLogger(new LearnwebSqlLogger());
        }

        announcementDao = jdbi.onDemand(AnnouncementDao.class);
        archiveUrlDao = jdbi.onDemand(ArchiveUrlDao.class);
        banDao = jdbi.onDemand(BanDao.class);
        bounceDao = jdbi.onDemand(BounceDao.class);
        commentDao = jdbi.onDemand(CommentDao.class);
        courseDao = jdbi.onDemand(CourseDao.class);
        fileDao = jdbi.onDemand(FileDao.class);
        folderDao = jdbi.onDemand(FolderDao.class);
        forumPostDao = jdbi.onDemand(ForumPostDao.class);
        forumTopicDao = jdbi.onDemand(ForumTopicDao.class);
        glossaryDao = jdbi.onDemand(GlossaryDao.class);
        glossaryEntryDao = jdbi.onDemand(GlossaryEntryDao.class);
        glossaryTermDao = jdbi.onDemand(GlossaryTermDao.class);
        groupDao = jdbi.onDemand(GroupDao.class);
        logDao = jdbi.onDemand(LogDao.class);
        messageDao = jdbi.onDemand(MessageDao.class);
        organisationDao = jdbi.onDemand(OrganisationDao.class);
        requestDao = jdbi.onDemand(RequestDao.class);
        resourceDao = jdbi.onDemand(ResourceDao.class);
        resourceHistoryDao = jdbi.onDemand(ResourceHistoryDao.class);
        searchHistoryDao = jdbi.onDemand(SearchHistoryDao.class);
        speechRepositoryDao = jdbi.onDemand(SpeechRepositoryDao.class);
        submissionDao = jdbi.onDemand(SubmissionDao.class);
        surveyDao = jdbi.onDemand(SurveyDao.class);
        tagDao = jdbi.onDemand(TagDao.class);
        tedTranscriptDao = jdbi.onDemand(TedTranscriptDao.class);
        userDao = jdbi.onDemand(UserDao.class);
        waybackUrlDao = jdbi.onDemand(WaybackUrlDao.class);
    }

    private void migrateDatabase() {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("db/migration")
            .baselineOnMigrate(true)
            .load();

        flyway.migrate();
    }

    public void destroy() {
        try {
            if (dataSource instanceof Closeable) {
                ((Closeable) dataSource).close();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static HikariDataSource createDataSource(final ConfigProvider configProvider) {
        HikariDataSource ds = new HikariDataSource();
        // Configuration docs https://github.com/brettwooldridge/HikariCP
        ds.setDriverClassName(configProvider.getProperty("mysql_driver"));
        ds.setJdbcUrl(configProvider.getProperty("mysql_url"));
        ds.setUsername(configProvider.getProperty("mysql_user"));
        ds.setPassword(configProvider.getProperty("mysql_password"));
        ds.setMaximumPoolSize(3);
        ds.setConnectionTimeout(60000); // 1 min
        return ds;
    }

    @Produces
    public Jdbi getJdbi() {
        return jdbi;
    }

    @Produces
    public AnnouncementDao getAnnouncementDao() {
        return announcementDao;
    }

    @Produces
    public ArchiveUrlDao getArchiveUrlDao() {
        return archiveUrlDao;
    }

    @Produces
    public BanDao getBanDao() {
        return banDao;
    }

    @Produces
    public BounceDao getBounceDao() {
        return bounceDao;
    }

    @Produces
    public CommentDao getCommentDao() {
        return commentDao;
    }

    @Produces
    public CourseDao getCourseDao() {
        return courseDao;
    }

    @Produces
    public FileDao getFileDao() {
        return fileDao;
    }

    @Produces
    public FolderDao getFolderDao() {
        return folderDao;
    }

    @Produces
    public ForumPostDao getForumPostDao() {
        return forumPostDao;
    }

    @Produces
    public ForumTopicDao getForumTopicDao() {
        return forumTopicDao;
    }

    @Produces
    public GlossaryDao getGlossaryDao() {
        return glossaryDao;
    }

    @Produces
    public GlossaryEntryDao getGlossaryEntryDao() {
        return glossaryEntryDao;
    }

    @Produces
    public GlossaryTermDao getGlossaryTermDao() {
        return glossaryTermDao;
    }

    @Produces
    public GroupDao getGroupDao() {
        return groupDao;
    }

    @Produces
    public LogDao getLogDao() {
        return logDao;
    }

    @Produces
    public MessageDao getMessageDao() {
        return messageDao;
    }

    @Produces
    public OrganisationDao getOrganisationDao() {
        return organisationDao;
    }

    @Produces
    public RequestDao getRequestDao() {
        return requestDao;
    }

    @Produces
    public ResourceDao getResourceDao() {
        return resourceDao;
    }

    @Produces
    public ResourceHistoryDao getResourceHistoryDao() {
        return resourceHistoryDao;
    }

    @Produces
    public SearchHistoryDao getSearchHistoryDao() {
        return searchHistoryDao;
    }

    @Produces
    public SpeechRepositoryDao getSpeechRepositoryDao() {
        return speechRepositoryDao;
    }

    @Produces
    public SubmissionDao getSubmissionDao() {
        return submissionDao;
    }

    @Produces
    public SurveyDao getSurveyDao() {
        return surveyDao;
    }

    @Produces
    public TagDao getTagDao() {
        return tagDao;
    }

    @Produces
    public TedTranscriptDao getTedTranscriptDao() {
        return tedTranscriptDao;
    }

    @Produces
    public UserDao getUserDao() {
        return userDao;
    }

    @Produces
    public WaybackUrlDao getWaybackUrlDao() {
        return waybackUrlDao;
    }

    private static class LearnwebSqlLogger implements SqlLogger {
        private static final Logger log = LogManager.getLogger(LearnwebSqlLogger.class);

        @Override
        public void logBeforeExecution(final StatementContext context) {
            log.debug("Executing query '{}' with params {}", context.getRenderedSql(), context.getBinding().toString());
        }

        @Override
        public void logAfterExecution(final StatementContext context) {
            log.debug("Query execution time - {} ms", context.getElapsedTime(ChronoUnit.NANOS) / 1000000.0);
        }

        @Override
        public void logException(final StatementContext context, final SQLException ex) {
            log.error("An SQL Error in query {}", context.getRenderedSql(), ex);
        }
    }
}