package de.l3s.learnweb.beans;

import static javax.servlet.RequestDispatcher.*;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.faces.application.ViewExpiredException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.omnifaces.util.Exceptions;
import org.omnifaces.util.Servlets;
import org.omnifaces.util.Utils;

import de.l3s.learnweb.beans.exceptions.BeanException;
import de.l3s.learnweb.beans.exceptions.UnauthorizedBeanException;

/**
 * The filter uses the idea of {@link org.omnifaces.filter.FacesExceptionFilter}.
 */
@WebFilter(filterName = "LearnwebExceptionFilter", urlPatterns = "/*")
public class LearnwebExceptionFilter extends HttpFilter {
    private static final long serialVersionUID = 3190219905269569699L;

    @Override
    protected void doFilter(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
        throws IOException, ServletException {

        try {
            chain.doFilter(request, response);
        } catch (FileNotFoundException exception) {
            // Ignoring thrown exception; this is a JSF quirk and it should be interpreted as 404.
            response.sendError(BeanException.NOT_FOUND, request.getRequestURI());
        } catch (ServletException exception) {
            // get unwrapped exception
            Throwable throwable = Exceptions.unwrap(exception.getRootCause());
            LearnwebExceptionHandler.logException(throwable, request);

            request.setAttribute(ERROR_MESSAGE, throwable.getMessage());
            request.setAttribute(ERROR_EXCEPTION, throwable);

            if (throwable instanceof ViewExpiredException) {
                response.sendError(BeanException.SESSION_EXPIRED, request.getRequestURI());
            } else if (throwable instanceof UnauthorizedBeanException) {
                // In case of unauthorized user, redirect to login page
                response.sendRedirect(prepareLoginURL(request));
            } else if (throwable instanceof BeanException) {
                // Show an appropriate error page, these exceptions usually expected
                response.sendError(((BeanException) throwable).getStatusCode(), throwable.getMessage());
            } else {
                // An unexpected error, usually something went wrong
                throw exception;
            }
        } catch (Throwable throwable) {
            // Theoretically should never happens, all errors should be of type ServletException
            LearnwebExceptionHandler.logException(throwable, request);
            throw new ServletException(throwable);
        } finally {
            // same workaround as in FullAjaxExceptionHandler
            request.removeAttribute(ERROR_EXCEPTION);
        }
    }

    private String prepareLoginURL(HttpServletRequest request) {
        String requestURI = Servlets.getRequestURI(request).substring(request.getContextPath().length());
        String queryString = Servlets.getRequestQueryString(request);
        String redirectToUrl = (queryString == null) ? requestURI : (requestURI + "?" + queryString);
        return request.getContextPath() + "/lw/user/login.jsf?redirect=" + Utils.encodeURL(redirectToUrl);
    }
}
