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

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.forum.ForumPostDao;
import de.l3s.learnweb.forum.ForumTopicDao;
import de.l3s.learnweb.group.FolderDao;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.logging.LogDao;
import de.l3s.learnweb.resource.CommentDao;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.resource.TagDao;
import de.l3s.learnweb.resource.archive.ArchiveUrlDao;
import de.l3s.learnweb.resource.archive.WaybackUrlDao;
import de.l3s.learnweb.resource.office.ResourceHistoryDao;
import de.l3s.learnweb.resource.submission.SubmissionDao;
import de.l3s.learnweb.resource.survey.SurveyDao;
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

    private ForumPostDao forumPostDao;
    private ForumTopicDao forumTopicDao;
    private RequestDao requestDao;
    private BanDao banDao;
    private GroupDao groupDao;
    private FolderDao folderDao;
    private CourseDao courseDao;
    private UserDao userDao;
    private MessageDao messageDao;
    private ResourceDao resourceDao;
    private CommentDao commentDao;
    private TagDao tagDao;
    private LogDao logDao;
    private OrganisationDao organisationDao;
    private ResourceHistoryDao resourceHistoryDao;
    private SubmissionDao submissionDao;
    private SurveyDao surveyDao;
    private ArchiveUrlDao archiveUrlDao;
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
        forumPostDao = jdbi.onDemand(ForumPostDao.class);
        forumTopicDao = jdbi.onDemand(ForumTopicDao.class);
        requestDao = jdbi.onDemand(RequestDao.class);
        banDao = jdbi.onDemand(BanDao.class);
        groupDao = jdbi.onDemand(GroupDao.class);
        folderDao = jdbi.onDemand(FolderDao.class);
        courseDao = jdbi.onDemand(CourseDao.class);
        userDao = jdbi.onDemand(UserDao.class);
        messageDao = jdbi.onDemand(MessageDao.class);
        resourceDao = jdbi.onDemand(ResourceDao.class);
        commentDao = jdbi.onDemand(CommentDao.class);
        tagDao = jdbi.onDemand(TagDao.class);
        logDao = jdbi.onDemand(LogDao.class);
        organisationDao = jdbi.onDemand(OrganisationDao.class);
        resourceHistoryDao = jdbi.onDemand(ResourceHistoryDao.class);
        submissionDao = jdbi.onDemand(SubmissionDao.class);
        surveyDao = jdbi.onDemand(SurveyDao.class);
        archiveUrlDao = jdbi.onDemand(ArchiveUrlDao.class);
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
    public Learnweb getLearnweb() {
        return learnweb;
    }

    @Produces
    public Jdbi getJdbi() {
        return jdbi;
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
    public RequestDao getRequestDao() {
        return requestDao;
    }

    @Produces
    public BanDao getBanDao() {
        return banDao;
    }

    @Produces
    public GroupDao getGroupDao() {
        return groupDao;
    }

    @Produces
    public FolderDao getFolderDao() {
        return folderDao;
    }

    @Produces
    public CourseDao getCourseDao() {
        return courseDao;
    }

    @Produces
    public UserDao getUserDao() {
        return userDao;
    }

    @Produces
    public MessageDao getMessageDao() {
        return messageDao;
    }

    @Produces
    public ResourceDao getResourceDao() {
        return resourceDao;
    }

    @Produces
    public CommentDao getCommentDao() {
        return commentDao;
    }

    @Produces
    public TagDao getTagDao() {
        return tagDao;
    }

    @Produces
    public LogDao getLogDao() {
        return logDao;
    }

    @Produces
    public OrganisationDao getOrganisationDao() {
        return organisationDao;
    }

    @Produces
    public ResourceHistoryDao getResourceHistoryDao() {
        return resourceHistoryDao;
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
    public ArchiveUrlDao getArchiveUrlDao() {
        return archiveUrlDao;
    }

    @Produces
    public WaybackUrlDao getWaybackUrlDao() {
        return waybackUrlDao;
    }
}
