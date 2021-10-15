package de.l3s.learnweb.user;

import java.io.IOException;
import java.io.Serial;
import java.util.Optional;

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

import de.l3s.learnweb.web.RequestManager;
import de.l3s.util.HashHelper;

/**
 * Checks if user is logged in or auth cookie is present and restores auth for new session.
 *
 * @author Oleh Astappiev
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = "/*", asyncSupported = true)
public class AuthFilter extends HttpFilter {
    @Serial
    private static final long serialVersionUID = 5223280572456365126L;
    private static final Logger log = LogManager.getLogger(AuthFilter.class);

    @Inject
    private TokenDao tokenDao;

    @Inject
    private UserBean userBean;

    @Inject
    private RequestManager requestManager;

    @Override
    protected void doFilter(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
        throws IOException, ServletException {

        // Inject user into session
        try {
            if (!userBean.isLoggedIn()) {
                String authValue = Servlets.getRequestCookie(request, LoginBean.AUTH_COOKIE_NAME);

                if (StringUtils.isNotEmpty(authValue)) {
                    String[] auth = authValue.split(":", 2);
                    Optional<User> user = tokenDao.findUserByToken(Integer.parseInt(auth[0]), HashHelper.sha512(auth[1]));
                    if (user.isPresent()) {
                        userBean.setUser(user.get(), request);
                    } else {
                        requestManager.recordFailedAttempt(Servlets.getRemoteAddr(request), "auth:" + auth[0]);
                        Servlets.removeResponseCookie(request, response, LoginBean.AUTH_COOKIE_NAME, "/");
                    }
                }
            }
        } catch (NumberFormatException e) {
            // TODO: remove after some time, needed after migration from long id to int. Just remove old cookie and require login for that time.
            Servlets.removeResponseCookie(request, response, LoginBean.AUTH_COOKIE_NAME, "/");
        } catch (Exception e) {
            Servlets.removeResponseCookie(request, response, LoginBean.AUTH_COOKIE_NAME, "/");
            log.error("Unable to finish auth verification", e);
        }

        chain.doFilter(request, response);
    }
}
