package de.l3s.learnweb.sentry;

import java.util.Objects;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpServletRequest;

import io.sentry.Breadcrumb;
import io.sentry.Hint;
import io.sentry.HubAdapter;
import io.sentry.IHub;

@WebListener
public class SentryServletRequestListener implements ServletRequestListener {
    private final IHub hub;

    public SentryServletRequestListener() {
        this.hub = Objects.requireNonNull(HubAdapter.getInstance(), "hub is required");
    }

    @Override
    public void requestDestroyed(ServletRequestEvent servletRequestEvent) {
        this.hub.popScope();
    }

    @Override
    public void requestInitialized(ServletRequestEvent servletRequestEvent) {
        this.hub.pushScope();
        ServletRequest servletRequest = servletRequestEvent.getServletRequest();
        if (servletRequest instanceof HttpServletRequest httpRequest) {
            Hint hint = new Hint();
            hint.set("servlet:request", httpRequest);
            this.hub.clearBreadcrumbs();
            this.hub.addBreadcrumb(Breadcrumb.http(httpRequest.getRequestURI(), httpRequest.getMethod()), hint);
            this.hub.configureScope((scope) -> scope.addEventProcessor(new SentryRequestHttpServletRequestProcessor(httpRequest)));
        }
    }
}
