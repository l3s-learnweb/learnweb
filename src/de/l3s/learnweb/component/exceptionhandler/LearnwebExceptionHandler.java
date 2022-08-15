package de.l3s.learnweb.component.exceptionhandler;

import jakarta.faces.application.ViewExpiredException;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
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
        if (Utils.isOneInstanceOf(rootCause.getClass(),
            BadRequestHttpException.class, // happens when some part of url is missing, usually should provide a meaningful message to user
            UnauthorizedHttpException.class // Unauthorized access redirected to login page
        )) {
            return;
        }

        String requestSummary = BeanHelper.getRequestSummary(request);

        if (rootCause instanceof HttpException && ((HttpException) rootCause).isSilent()) {
            log.warn("Bean exception {} ", requestSummary, rootCause);
            return;
        }

        if (rootCause instanceof ForbiddenHttpException) {
            log.error("Illegal access {} ", requestSummary, rootCause);
        } else if (rootCause instanceof BadRequestHttpException) {
            log.log(isBotUserAgent(request) ? Level.WARN : Level.ERROR, "Bad request {} ", requestSummary, rootCause);
        } else if (rootCause instanceof ViewExpiredException) {
            log.debug("View expired {}", requestSummary, rootCause);
        } else {
            log.error("Unhandled error on {}", requestSummary, rootCause);
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

        return StringUtils.containsAny(userAgent.toLowerCase(), "bot;", "bot/", "bot ", "java", "wget", "spider", "python-requests", "ltx71.com");
    }
}
