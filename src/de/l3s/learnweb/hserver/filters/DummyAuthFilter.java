package de.l3s.learnweb.hserver.filters;

import java.security.Principal;
import java.sql.SQLException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;

@Provider
public class DummyAuthFilter implements ContainerRequestFilter {
    private static final Logger log = LogManager.getLogger(DummyAuthFilter.class);

    @Context
    SecurityContext context;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        UserManager userManager = Learnweb.getInstance().getUserManager();
        String authorization = requestContext.getHeaderString("Authorization");

        try {
            if (authorization != null) {
                String token = authorization.replace("Bearer ", "");
                User user = userManager.getUserByGrantToken(token);
                if (user != null) {
                    requestContext.setSecurityContext(new SecurityContext() {
                        @Override
                        public Principal getUserPrincipal() {
                            return user;
                        }

                        @Override
                        public boolean isUserInRole(final String role) {
                            return true;
                        }

                        @Override
                        public boolean isSecure() {
                            return context.isSecure();
                        }

                        @Override
                        public String getAuthenticationScheme() {
                            return context.getAuthenticationScheme();
                        }
                    });
                }
            }
        } catch (SQLException e) {
            log.catching(e);
        }
    }
}
