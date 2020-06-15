package de.l3s.learnweb.beans;

import javax.faces.application.ViewExpiredException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.exceptionhandler.FullAjaxExceptionHandler;
import org.omnifaces.util.FacesLocal;
import org.omnifaces.util.Utils;

import de.l3s.learnweb.beans.exceptions.BadRequestBeanException;
import de.l3s.learnweb.beans.exceptions.ForbiddenBeanException;
import de.l3s.learnweb.beans.exceptions.NotFoundBeanException;
import de.l3s.learnweb.beans.exceptions.UnauthorizedBeanException;
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

    static void logException(Throwable rootCause, HttpServletRequest request) {
        logException(rootCause, BeanHelper.getRequestSummary(request));
    }

    private static void logException(Throwable rootCause, String requestSummary) {
        // skip these types
        if (Utils.isOneInstanceOf(rootCause.getClass(), NotFoundBeanException.class)) {
            return;
        }

        // } else if (rootCause instanceof IllegalStateException && rootCause.getMessage().startsWith("Cannot create a session")) {
        //     log.warn(rootCause.getMessage() + "; Happens mostly because of error 404; On " + description);
        // } else if (rootCause instanceof IllegalArgumentException && rootCause.getMessage().startsWith("Illegal base64 character -54")) {
        //     log.warn(rootCause.getMessage() + "; This happens often due to ; On " + description);

        if (rootCause instanceof UnauthorizedBeanException) {
            log.info("Unauthorized access redirected to login page.");
        } else if (rootCause instanceof ForbiddenBeanException && rootCause.getMessage() == null) {
            log.error("Illegal access {} ", requestSummary, rootCause);
        } else if (rootCause instanceof BadRequestBeanException) {
            log.error("Bad request {} ", requestSummary, rootCause);
        } else if (rootCause instanceof ViewExpiredException) {
            log.error("View expired {}", requestSummary, rootCause);
        } else {
            log.fatal("Fatal unhandled error on {}", requestSummary, rootCause);
        }
    }
}
