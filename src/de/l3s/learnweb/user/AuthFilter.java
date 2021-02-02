package de.l3s.learnweb.user;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Servlets;

import de.l3s.learnweb.Learnweb;

/**
 * Checks if user is logged in or auth cookie is present and restores auth for new session.
 *
 * @author Oleh Astappiev
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = "/*")
public class AuthFilter extends HttpFilter {
    private static final long serialVersionUID = 5223280572456365126L;
    private static final Logger log = LogManager.getLogger(AuthFilter.class);

    @Inject
    private UserBean userBean;

    @Override
    protected void doFilter(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
        throws IOException, ServletException {

        // Inject user into session
        try {
            Optional<Learnweb> learnweb = Learnweb.getInstanceOptional();

            if (learnweb.isPresent() && !userBean.isLoggedIn()) {
                String authValue = Servlets.getRequestCookie(request, LoginBean.AUTH_COOKIE_NAME);

                if (StringUtils.isNotEmpty(authValue)) {
                    String[] auth = authValue.split(":", 2);
                    User user = learnweb.get().getUserManager().getUserByAuth(Long.parseLong(auth[0]), auth[1]);

                    if (user != null) {
                        userBean.setUser(user, request);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Unable to finish auth verification", e);
        }

        // Validate if session is not expired
        try {
            if (request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid()) {
                log.warn("Request attempt with invalid session {}", request.getRequestURI());
            }
        } catch (Exception e) {
            log.error("Unable detect session status", e);
        }

        chain.doFilter(request, response);
    }
}
