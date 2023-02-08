package de.l3s.learnweb.sentry;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Servlets;

import de.l3s.learnweb.app.Learnweb;
import io.sentry.EventProcessor;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.protocol.Request;
import io.sentry.protocol.User;

final class SentryRequestHttpServletRequestProcessor implements EventProcessor {
    private static final Logger log = LogManager.getLogger(SentryRequestHttpServletRequestProcessor.class);

    private static final List<String> SENSITIVE_HEADERS = Arrays.asList("X-FORWARDED-FOR", "AUTHORIZATION", "COOKIE");
    private static final List<String> SENSITIVE_PARAMS = Arrays.asList("password", "secret");
    private final HttpServletRequest httpRequest;

    SentryRequestHttpServletRequestProcessor(HttpServletRequest httpRequest) {
        this.httpRequest = Objects.requireNonNull(httpRequest, "httpRequest is required");
    }

    @Override
    public @NotNull SentryEvent process(@NotNull SentryEvent event, @NotNull Hint hint) {
        Request sentryRequest = new Request();
        sentryRequest.setMethod(this.httpRequest.getMethod());
        sentryRequest.setQueryString(this.httpRequest.getQueryString());
        sentryRequest.setUrl(this.httpRequest.getRequestURL().toString());
        sentryRequest.setHeaders(this.resolveHeadersMap(this.httpRequest));
        sentryRequest.setEnvs(this.resolveParametersMap(this.httpRequest.getParameterMap()));
        event.setRequest(sentryRequest);

        try {
            event.setEnvironment(Learnweb.config().getEnvironment());
            if (!"local".equals(event.getEnvironment())) {
                event.setRelease("learnweb@" + Learnweb.config().getVersion());
            }
        } catch (Exception e) {
            log.error("Failed to resolve environment and release", e);
        }

        try {
            User user = new User();
            user.setIpAddress(Servlets.getRemoteAddr(this.httpRequest));
            HttpSession session = this.httpRequest.getSession();
            if (session != null) {
                Object userId = session.getAttribute("UserId");
                if (userId != null) {
                    user.setId(userId.toString());
                }
                Object userName = session.getAttribute("UserName");
                if (userName != null) {
                    user.setUsername(userName.toString());
                }
            }
            event.setUser(user);
        } catch (Exception e) {
            log.error("Failed to resolve user", e);
        }

        try {
            event.setTag("locale", Faces.getLocale().getLanguage());
        } catch (Exception e) {
            log.error("Failed to resolve tags", e);
        }

        return event;
    }

    private @NotNull Map<String, String> resolveHeadersMap(@NotNull HttpServletRequest request) {
        Map<String, String> headersMap = new HashMap<>();

        for (final String headerName : Collections.list(request.getHeaderNames())) {
            if (SENSITIVE_HEADERS.contains(headerName.toUpperCase(Locale.ROOT))) {
                headersMap.put(headerName, "[Filtered]");
            } else {
                headersMap.put(headerName, toString(request.getHeaders(headerName)));
            }
        }

        return headersMap;
    }

    private @NotNull Map<String, String> resolveParametersMap(@NotNull Map<String, String[]> map) {
        Map<String, String> paramsMap = new HashMap<>();

        for (final Map.Entry<String, String[]> entry : map.entrySet()) {
            if (SENSITIVE_PARAMS.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                paramsMap.put(entry.getKey(), "[Filtered]");
            } else {
                paramsMap.put(entry.getKey(), String.join("; ", entry.getValue()));
            }
        }

        return paramsMap;
    }

    private static String toString(Enumeration<String> enumeration) {
        return enumeration != null ? String.join(",", Collections.list(enumeration)) : null;
    }
}
