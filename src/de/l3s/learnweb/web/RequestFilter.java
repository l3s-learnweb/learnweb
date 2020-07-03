package de.l3s.learnweb.web;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Servlets;

import com.google.common.net.InetAddresses;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.exceptions.ForbiddenHttpException;
import de.l3s.learnweb.user.loginProtection.ProtectionManager;
import de.l3s.util.bean.BeanHelper;

/**
 * Logs incoming requests by IPs. Records IP, time and URL, then at the end of the day stores it into a log file.
 *
 * @author Kate
 */
@WebFilter(filterName = "RequestFilter", urlPatterns = "/*")
public class RequestFilter extends HttpFilter {
    private static final long serialVersionUID = -6484981916986254209L;
    private static final Logger log = LogManager.getLogger(RequestFilter.class);

    private transient RequestManager requestManager;
    private transient ProtectionManager protectionManager;

    public void init(HttpServletRequest request) {
        try {
            String serverUrl = Servlets.getRequestBaseURL(request);
            Learnweb learnweb = Learnweb.createInstance(serverUrl);

            requestManager = learnweb.getRequestManager();
            protectionManager = learnweb.getProtectionManager();
        } catch (Exception e) {
            log.error("Request filter not initialized", e);
        }
    }

    @Override
    protected void doFilter(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
        throws IOException, ServletException {

        request.setCharacterEncoding("UTF-8");

        if (requestManager == null) {
            init(request);
        }

        if (requestManager != null && protectionManager != null) {
            String ipAddr = Servlets.getRemoteAddr(request);
            String requestUrl = Servlets.getRequestURL(request);

            if (!InetAddresses.isInetAddress(ipAddr)) {
                log.error("Suspicious request: {}", BeanHelper.getRequestSummary(request));

                /*
                 * Joomla Unserialize Vulnerability
                 * https://blog.cloudflare.com/the-joomla-unserialize-vulnerability/
                 */
                if (ipAddr.contains("JDatabaseDriverMysqli")) {
                    protectionManager.ban(ipAddr, "Suspicious IP address");
                }
            }

            /*
             * Possible SQL injection probation
             * https://stackoverflow.com/questions/33867813/strange-url-containing-a-0-or-0-a-in-web-server-logs
             */
            if (requestUrl.endsWith("'A=0")) {
                protectionManager.ban(ipAddr, "SQL injection");
            }

            requestManager.recordRequest(ipAddr, requestUrl);
            if (protectionManager.isBanned(ipAddr)) {
                throw new ForbiddenHttpException("error_pages.forbidden_blocked_description");
            }
        }

        chain.doFilter(request, response);
    }
}
