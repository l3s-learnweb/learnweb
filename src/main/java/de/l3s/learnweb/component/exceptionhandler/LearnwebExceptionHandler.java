package de.l3s.learnweb.component.exceptionhandler;

import java.util.Arrays;
import java.util.Set;

import jakarta.faces.application.ViewExpiredException;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.exceptionhandler.FullAjaxExceptionHandler;
import org.omnifaces.util.FacesLocal;
import org.omnifaces.util.Utils;

import de.l3s.learnweb.exceptions.BadRequestHttpException;
import de.l3s.learnweb.exceptions.ForbiddenHttpException;
import de.l3s.learnweb.exceptions.HttpException;
import de.l3s.learnweb.exceptions.UnauthorizedHttpException;

public class LearnwebExceptionHandler extends FullAjaxExceptionHandler {
    private static final Logger log = LogManager.getLogger(LearnwebExceptionHandler.class);
    // compared by name, because container classes are not available at compile time
    private static final Set<String> CLIENT_ABORT_EXCEPTIONS = Set.of("org.apache.catalina.connector.ClientAbortException", "org.eclipse.jetty.io.EofException");

    public LearnwebExceptionHandler(ExceptionHandler wrapped) {
        super(wrapped);
    }

    @Override
    protected void logException(final FacesContext context, final Throwable exception, final String location, final LogReason reason) {
        logException(exception, FacesLocal.getRequest(context));
    }

    protected static void logException(Throwable rootCause, HttpServletRequest request) {
        if (Utils.isOneInstanceOf(rootCause.getClass(),
            BadRequestHttpException.class, // happens when some part of url is missing, usually should provide a meaningful message to user
            UnauthorizedHttpException.class // Unauthorized access redirected to login page
        )) {
            return;
        }

        if (isMalformedRequest(rootCause)) {
            log.warn("Malformed request: {}", rootCause.getMessage()); // usually sent by vulnerability scanners or aborted uploads
            return;
        }

        if (rootCause instanceof HttpException httpException && httpException.isSilent()) {
            log.warn("Bean exception", rootCause);
            return;
        }

        if (rootCause instanceof ForbiddenHttpException) {
            log.warn("Illegal access", rootCause);
        } else if (rootCause instanceof BadRequestHttpException) {
            log.log(isBotUserAgent(request) ? Level.WARN : Level.ERROR, "Bad request", rootCause);
        } else if (rootCause instanceof ViewExpiredException) {
            log.debug("View expired", rootCause);
        } else if (CLIENT_ABORT_EXCEPTIONS.contains(rootCause.getClass().getName())) {
            log.debug("Client aborted request: {}", rootCause.getMessage());
        } else {
            log.error("Unhandled error", rootCause);
        }
    }

    /**
     * Returns true if the exception was caused by a malformed request, which is a client error:
     * Tomcat failing to parse the request parameters (e.g. invalid encoding, aborted multipart upload,
     * exceeded maxParameterCount, maxPartCount or maxPostSize, which valid requests rarely do)
     * or MyFaces failing to decode a malformed (e.g. tampered) {@code jakarta.faces.ViewState}.
     */
    protected static boolean isMalformedRequest(Throwable throwable) {
        // compared by name, because Tomcat classes are not available at compile time
        return "org.apache.tomcat.util.http.InvalidParameterException".equals(throwable.getClass().getName())
            || throwable instanceof IllegalArgumentException && Arrays.stream(throwable.getStackTrace())
            .anyMatch(element -> "org.apache.myfaces.application.viewstate.StateUtils".equals(element.getClassName()) && "decode".equals(element.getMethodName()));
    }

    /**
     * Returns true if this request was created by a crawler or bot.
     */
    private static boolean isBotUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");

        if (StringUtils.isEmpty(userAgent)) {
            return false; // can't be sure
        }

        return Strings.CS.containsAny(userAgent.toLowerCase(), "bot;", "bot/", "bot ", "java", "wget", "spider", "python-requests", "ltx71.com");
    }
}
