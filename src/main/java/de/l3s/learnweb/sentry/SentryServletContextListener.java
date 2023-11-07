package de.l3s.learnweb.sentry;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import org.apache.logging.log4j.LogManager;

import de.l3s.learnweb.app.DaoProvider;

import io.sentry.HubAdapter;

@WebListener
public class SentryServletContextListener implements ServletContextListener {
    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        LogManager.getLogger(SentryServletContextListener.class).info("Servlet context is shutting down...");
        HubAdapter.getInstance().close();
    }
}
