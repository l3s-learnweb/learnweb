package de.l3s.learnweb.component.exceptionhandler;

import static jakarta.servlet.RequestDispatcher.ERROR_EXCEPTION;
import static jakarta.servlet.RequestDispatcher.ERROR_MESSAGE;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serial;
import java.nio.charset.StandardCharsets;

import jakarta.faces.application.ViewExpiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.omnifaces.exceptionhandler.FullAjaxExceptionHandler;
import org.omnifaces.util.Exceptions;
import org.omnifaces.util.Servlets;
import org.omnifaces.util.Utils;

import de.l3s.learnweb.exceptions.HttpException;
import de.l3s.learnweb.exceptions.UnauthorizedHttpException;

/**
 * The filter uses the idea of {@link org.omnifaces.filter.FacesExceptionFilter}.
 */
@WebFilter(filterName = "LearnwebExceptionFilter", urlPatterns = "/*", asyncSupported = true)
public class LearnwebExceptionFilter extends HttpFilter {
    @Serial
    private static final long serialVersionUID = 3190219905269569699L;

    private static final String ERROR_REASON = "de.l3s.learnweb.error.reason";

    private Class<? extends Throwable>[] exceptionTypesToUnwrap;

    @Override
    public void init() {
        exceptionTypesToUnwrap = FullAjaxExceptionHandler.getExceptionTypesToUnwrap(getServletContext());
    }

    @Override
    protected void doFilter(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
        throws IOException, ServletException {

        try {
            chain.doFilter(request, response);
        } catch (FileNotFoundException exception) {
            // Ignoring thrown exception; this is a Faces quirk, and it should be interpreted as 404.
            response.sendError(HttpException.NOT_FOUND);
        } catch (Throwable exception) {
            // usually a ServletException thrown by Faces, but non-Faces servlets can throw e.g. an HttpException directly
            Throwable throwable = unwrap(exception);
            LearnwebExceptionHandler.logException(throwable, request);

            if (response.isCommitted()) {
                // too late to send an error page, let the container abort the response
                throw exception;
            }

            request.setAttribute(ERROR_MESSAGE, throwable.getMessage());
            request.setAttribute(ERROR_EXCEPTION, throwable);

            if (throwable instanceof ViewExpiredException) {
                response.sendError(HttpException.SESSION_EXPIRED);
            } else if (LearnwebExceptionHandler.isMalformedRequest(throwable)) {
                sendMalformedRequestError(response);
            } else if (throwable instanceof UnauthorizedHttpException) {
                // In case of unauthorized user, redirect to login page
                response.sendRedirect(prepareLoginURLWithRedirect(request));
            } else if (throwable instanceof HttpException httpException) {
                // Show an appropriate error page, these exceptions usually expected
                request.setAttribute(ERROR_REASON, httpException.getReason());
                response.sendError(httpException.getStatus(), httpException.getReason());
            } else {
                // An unexpected error, usually something went wrong
                throw exception;
            }
        } finally {
            // same workaround as in FullAjaxExceptionHandler
            request.removeAttribute(ERROR_EXCEPTION);
        }
    }

    /**
     * The error page is a Faces page, which often can't be rendered for a malformed request,
     * e.g. Tomcat throws the parse exception again on every getParameter() call. So a plain text error is sent instead.
     */
    private static void sendMalformedRequestError(final HttpServletResponse response) throws IOException {
        response.reset();
        response.setStatus(HttpException.BAD_REQUEST);
        response.setContentType("text/plain");
        response.setCharacterEncoding(StandardCharsets.UTF_8);
        response.getWriter().write("400 Bad Request");
    }

    private Throwable unwrap(final Throwable throwable) {
        Throwable cause = throwable instanceof ServletException exception && exception.getRootCause() != null ? exception.getRootCause() : throwable;
        return Exceptions.unwrap(cause, exceptionTypesToUnwrap);
    }

    private static String prepareLoginURLWithRedirect(HttpServletRequest request) {
        String requestURI = Servlets.getRequestRelativeURI(request);
        String queryString = Servlets.getRequestQueryString(request);
        String redirectToUrl = (queryString == null) ? requestURI : (requestURI + "?" + queryString);
        return request.getContextPath() + "/lw/user/login.jsf?redirect=" + Utils.encodeURL(redirectToUrl);
    }
}
