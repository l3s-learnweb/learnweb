package de.l3s.learnweb.component.exceptionhandler;

import static jakarta.servlet.RequestDispatcher.ERROR_EXCEPTION;
import static jakarta.servlet.RequestDispatcher.ERROR_MESSAGE;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serial;
import java.lang.reflect.InvocationTargetException;

import jakarta.el.ELException;
import jakarta.faces.FacesException;
import jakarta.faces.application.ViewExpiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.weld.exceptions.WeldException;
import org.omnifaces.util.Exceptions;

import de.l3s.learnweb.exceptions.HttpException;
import de.l3s.learnweb.exceptions.UnauthorizedHttpException;
import de.l3s.learnweb.user.LoginBean;

/**
 * The filter uses the idea of {@link org.omnifaces.filter.FacesExceptionFilter}.
 */
@WebFilter(filterName = "LearnwebExceptionFilter", urlPatterns = "/*", asyncSupported = true)
public class LearnwebExceptionFilter extends HttpFilter {
    @Serial
    private static final long serialVersionUID = 3190219905269569699L;

    private static final String ERROR_REASON = "de.l3s.learnweb.error.reason";

    @Override
    protected void doFilter(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
        throws IOException, ServletException {

        try {
            chain.doFilter(request, response);
        } catch (FileNotFoundException exception) {
            // Ignoring thrown exception; this is a Faces quirk, and it should be interpreted as 404.
            response.sendError(HttpException.NOT_FOUND, request.getRequestURI());
        } catch (ServletException exception) {
            // get unwrapped exception
            Throwable throwable = Exceptions.unwrap(exception.getRootCause(), FacesException.class, ELException.class, WeldException.class, InvocationTargetException.class);
            LearnwebExceptionHandler.logException(throwable, request);

            request.setAttribute(ERROR_MESSAGE, throwable.getMessage());
            request.setAttribute(ERROR_EXCEPTION, throwable);

            if (throwable instanceof ViewExpiredException) {
                response.sendError(HttpException.SESSION_EXPIRED, request.getRequestURI());
            } else if (throwable instanceof UnauthorizedHttpException) {
                // In case of unauthorized user, redirect to login page
                response.sendRedirect(LoginBean.prepareLoginURL(request));
            } else if (throwable instanceof HttpException httpException) {
                // Show an appropriate error page, these exceptions usually expected
                request.setAttribute(ERROR_REASON, httpException.getReason());
                response.sendError(httpException.getStatus(), httpException.getReason());
            } else {
                // An unexpected error, usually something went wrong
                throw exception;
            }
        } catch (Throwable throwable) {
            // Theoretically should never happen, all errors should be of type ServletException
            LearnwebExceptionHandler.logException(throwable, request);
            throw new ServletException(throwable);
        } finally {
            // same workaround as in FullAjaxExceptionHandler
            request.removeAttribute(ERROR_EXCEPTION);
        }
    }
}
