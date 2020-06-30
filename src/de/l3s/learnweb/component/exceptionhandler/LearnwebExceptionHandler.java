package de.l3s.learnweb.component.exceptionhandler;

import javax.faces.application.ViewExpiredException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.exceptionhandler.FullAjaxExceptionHandler;
import org.omnifaces.util.FacesLocal;
import org.omnifaces.util.Utils;

import de.l3s.learnweb.exceptions.BadRequestHttpException;
import de.l3s.learnweb.exceptions.ForbiddenHttpException;
import de.l3s.learnweb.exceptions.NotFoundHttpException;
import de.l3s.learnweb.exceptions.UnauthorizedHttpException;
import de.l3s.util.bean.BeanHelper;

public class LearnwebExceptionHandler extends FullAjaxExceptionHandler {
    private static final Logger log = LogManager.getLogger(LearnwebExceptionHandler.class);

    public LearnwebExceptionHandler(ExceptionHandler wrapped) {
        super(wrapped);
    }

    @Override
    protected void logException(final FacesContext context, final Throwable exception, final String location, final LogReason reason) {
        logException(exception, FacesLocal.getRequest(context));
    }

    protected static void logException(Throwable rootCause, HttpServletRequest request) {
        // skip these types
        if (Utils.isOneInstanceOf(rootCause.getClass(), NotFoundHttpException.class)) {
            return;
        }

        String requestSummary = BeanHelper.getRequestSummary(request);

        // } else if (rootCause instanceof IllegalStateException && rootCause.getMessage().startsWith("Cannot create a session")) {
        //     log.warn(rootCause.getMessage() + "; Happens mostly because of error 404; On " + description);
        // } else if (rootCause instanceof IllegalArgumentException && rootCause.getMessage().startsWith("Illegal base64 character -54")) {
        //     log.warn(rootCause.getMessage() + "; This happens often due to ; On " + description);

        if (rootCause instanceof UnauthorizedHttpException) {
            log.info("Unauthorized access redirected to login page.");
        } else if (rootCause instanceof ForbiddenHttpException && rootCause.getMessage() == null) {
            log.error("Illegal access {} ", requestSummary, rootCause);
        } else if (rootCause instanceof BadRequestHttpException) {
            if (isBotUserAgent(request)) {
                log.warn("Bad request {} ", requestSummary);
            } else {
                log.error("Bad request {} ", requestSummary, rootCause);
            }
        } else if (rootCause instanceof ViewExpiredException) {
            log.debug("View expired {}", requestSummary);
        } else if (rootCause instanceof ClientAbortException) { // happens when users press the abort button or navigate very fast
            log.debug("Client aborted a connection {}", requestSummary);
        } else {
            log.fatal("Fatal unhandled error on {}", requestSummary, rootCause);
        }
    }

    /**
     * Returns true if this request was created by a crawler or bot.
     */
    private static boolean isBotUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");

        if (StringUtils.isEmpty(userAgent)) {
            return false; // can't be sure
        }
        userAgent = userAgent.toLowerCase();

        return StringUtils.containsAny(userAgent.toLowerCase(), "bot;", "bot/", "bot ", "java", "wget", "spider", "python-requests", "ltx71.com");
    }
}
