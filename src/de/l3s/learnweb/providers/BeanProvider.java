package de.l3s.learnweb.providers;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.omnifaces.cdi.Eager;

import de.l3s.learnweb.AnnouncementDao;
import de.l3s.learnweb.Learnweb;
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
import de.l3s.learnweb.resource.glossary.GlossaryEntryDao;
import de.l3s.learnweb.resource.glossary.GlossaryTermDao;
import de.l3s.learnweb.resource.office.ResourceHistoryDao;
import de.l3s.learnweb.resource.submission.SubmissionDao;
import de.l3s.learnweb.resource.survey.SurveyDao;
import de.l3s.learnweb.resource.ted.TedTranscriptDao;
import de.l3s.learnweb.searchhistory.SearchHistoryDao;
import de.l3s.learnweb.user.CourseDao;
import de.l3s.learnweb.user.MessageDao;
import de.l3s.learnweb.user.OrganisationDao;
import de.l3s.learnweb.user.UserDao;
import de.l3s.learnweb.web.BanDao;
import de.l3s.learnweb.web.RequestDao;

/**
 * <p>
 * An important class with Bean providers, following Jakarta EE CDI specifications.
 * </p>
 *
 * <p>
 * We are using Weld implementation, so the best documentation you can find is here
 * https://docs.jboss.org/weld/reference/latest-3.1/en-US/html_single/
 * </p>
 *
 * <h3> Annotation used in this class: </h3>
 * <ul>
 * <li> @Eager - means the class should be eagerly instantiated, even before application is actually started
 *          https://showcase.omnifaces.org/cdi/Eager
 * <li> @ApplicationScoped - something like a singleton, means that the class should be created once per application
 *          and always stored in memory, required to have @Produces methods in it
 *          https://docs.jboss.org/weld/reference/latest-3.1/en-US/html_single/#_built_in_scopes
 * <li> @Produces - is a method that acts as a source of bean instances,
 *          when @Inject is used, CDI is looking for any Bean or producer method that returns requested type
 *          https://docs.jboss.org/weld/reference/latest-3.1/en-US/html_single/#_producer_methods
 * </ul>
 */
@Eager
@ApplicationScoped
public class BeanProvider {
    private static final Logger log = LogManager.getLogger(BeanProvider.class);

    private Learnweb learnweb;
    private Jdbi jdbi;

    private AnnouncementDao announcementDao;
    private ArchiveUrlDao archiveUrlDao;
    private BanDao banDao;
    private CommentDao commentDao;
    private CourseDao courseDao;
    private FileDao fileDao;
    private FolderDao folderDao;
    private ForumPostDao forumPostDao;
    private ForumTopicDao forumTopicDao;
    private GlossaryEntryDao glossaryEntryDao;
    private GlossaryTermDao glossaryTermDao;
    private GroupDao groupDao;
    private LogDao logDao;
    private MessageDao messageDao;
    private OrganisationDao organisationDao;
    private RequestDao requestDao;
    private ResourceDao resourceDao;
    private ResourceHistoryDao resourceHistoryDao;
    private SearchHistoryDao searchHistoryDao;
    private SubmissionDao submissionDao;
    private SurveyDao surveyDao;
    private TagDao tagDao;
    private TedTranscriptDao tedTranscriptDao;
    private UserDao userDao;
    private WaybackUrlDao waybackUrlDao;

    @Inject
    private ServletContext servletContext;

    /**
     * Because of @Eager, FacesContext is not available here.
     *
     * This method is the first entry point to the application in Servlet context (when running as web app).
     */
    @PostConstruct
    public void init() {
        try {
            var contextPath = servletContext.getContextPath();
            var serverInfo = servletContext.getServerInfo();
            var virtualServerName = servletContext.getVirtualServerName();
            var servletContextName = servletContext.getServletContextName();

            learnweb = Learnweb.createInstance();
            learnweb.setBeanProvider(this);
            migrateDatabase(learnweb.getDataSource());
        } catch (Exception e) {
            log.error("Application can't start", e);
            throw new DeploymentException(e);
        }

        jdbi = learnweb.getJdbi();
        announcementDao = jdbi.onDemand(AnnouncementDao.class);
        archiveUrlDao = jdbi.onDemand(ArchiveUrlDao.class);
        banDao = jdbi.onDemand(BanDao.class);
        commentDao = jdbi.onDemand(CommentDao.class);
        courseDao = jdbi.onDemand(CourseDao.class);
        fileDao = jdbi.onDemand(FileDao.class);
        folderDao = jdbi.onDemand(FolderDao.class);
        forumPostDao = jdbi.onDemand(ForumPostDao.class);
        forumTopicDao = jdbi.onDemand(ForumTopicDao.class);
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
        submissionDao = jdbi.onDemand(SubmissionDao.class);
        surveyDao = jdbi.onDemand(SurveyDao.class);
        tagDao = jdbi.onDemand(TagDao.class);
        tedTranscriptDao = jdbi.onDemand(TedTranscriptDao.class);
        userDao = jdbi.onDemand(UserDao.class);
        waybackUrlDao = jdbi.onDemand(WaybackUrlDao.class);
    }

    private void migrateDatabase(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("db/migration")
            .baselineOnMigrate(true)
            .load();

        flyway.migrate();
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
    public Jdbi getJdbi() {
        return jdbi;
    }

    @Produces
    public Learnweb getLearnweb() {
        return learnweb;
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
}
