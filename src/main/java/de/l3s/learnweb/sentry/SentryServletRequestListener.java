package de.l3s.learnweb.sentry;

import java.util.Objects;

import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpServletRequest;

import io.sentry.Breadcrumb;
import io.sentry.Hint;
import io.sentry.IScopes;
import io.sentry.ISentryLifecycleToken;
import io.sentry.ScopesAdapter;

@WebListener
public class SentryServletRequestListener implements ServletRequestListener {
    private final IScopes hub;
    private ISentryLifecycleToken token;

    public SentryServletRequestListener() {
        this.hub = Objects.requireNonNull(ScopesAdapter.getInstance(), "hub is required");
    }

    @Override
    public void requestDestroyed(ServletRequestEvent servletRequestEvent) {
        if (token != null) {
            token.close();
        }
    }

    @Override
    public void requestInitialized(ServletRequestEvent servletRequestEvent) {
        token = this.hub.pushScope();

        if (servletRequestEvent.getServletRequest() instanceof HttpServletRequest httpRequest) {
            Hint hint = new Hint();
            hint.set("servlet:request", httpRequest);
            this.hub.clearBreadcrumbs();
            this.hub.addBreadcrumb(Breadcrumb.http(httpRequest.getRequestURI(), httpRequest.getMethod()), hint);
            this.hub.configureScope(scope -> scope.addEventProcessor(new SentryRequestHttpServletRequestProcessor(httpRequest)));
        }
    }
}
