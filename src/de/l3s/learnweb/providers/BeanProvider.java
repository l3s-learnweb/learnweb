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
}
