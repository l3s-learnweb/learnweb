package de.l3s.learnweb.web;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Servlets;

import com.google.common.net.InetAddresses;

import de.l3s.learnweb.app.ConfigProvider;
import de.l3s.learnweb.exceptions.HttpException;
import de.l3s.learnweb.user.UserBean;
import de.l3s.util.bean.BeanHelper;

/**
 * Logs incoming requests by IPs.
 * Records IP, time and URL, then at the end of the day stores it into a log file.
 */
@SuppressWarnings("UnstableApiUsage")
@WebFilter(filterName = "RequestFilter", urlPatterns = "/*", asyncSupported = true)
public class RequestFilter extends HttpFilter {
    private static final long serialVersionUID = -6484981916986254209L;
    private static final Logger log = LogManager.getLogger(RequestFilter.class);

    @Inject
    private ConfigProvider configProvider;

    @Inject
    private RequestManager requestManager;

    @Inject
    private UserBean userBean;

    @Override
    protected void doFilter(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
        throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");

        // try to set server url
        if (configProvider.isServerUrlMissing()) {
            configProvider.setServerUrl(Servlets.getRequestBaseURL(request));
        }

        // validate ip address
        String ipAddr = Servlets.getRemoteAddr(request);
        if (!InetAddresses.isInetAddress(ipAddr)) {
            /*
             * This rule should ban threats like:
             * - Joomla Unserialize Vulnerability (https://blog.cloudflare.com/the-joomla-unserialize-vulnerability/)
             */
            log.error("Suspicious IP address restricted: {}. Request summary: {}", ipAddr, BeanHelper.getRequestSummary(request));

            // We can't ban them, because their IP address is not an address, but a string, likely long string...
            response.sendError(HttpException.FORBIDDEN, "error_pages.forbidden_blocked_description");
            return;
        }

        // validate request uri
        String requestUri = Servlets.getRequestURIWithQueryString(request);
        if (shouldBeValidated(requestUri)) {
            // check if not maintenance
            if (configProvider.isMaintenance() && !userBean.isAdmin()) {
                response.sendError(HttpException.UNAVAILABLE, request.getRequestURI());
                return;
            }

            if (StringUtils.endsWithAny(requestUri, "'", "'A=0")) {
                /*
                 * This rule should ban possible SQL injection
                 * https://stackoverflow.com/questions/33867813/strange-url-containing-a-0-or-0-a-in-web-server-logs
                 */
                requestManager.ban(ipAddr, "SQL injection");
            }

            requestManager.recordRequest(ipAddr, requestUri);
        }

        if (requestManager.isBanned(ipAddr)) {
            response.sendError(HttpException.FORBIDDEN, "error_pages.forbidden_blocked_description");
            return;
        }

        chain.doFilter(request, response);
    }

    private static boolean shouldBeValidated(final String requestUri) {
        if (requestUri.contains("/jakarta.faces.resource/")) {
            return false;
        }

        if ("/lw/status.jsf".equals(requestUri)) {
            return false;
        }

        return true;
    }
}
