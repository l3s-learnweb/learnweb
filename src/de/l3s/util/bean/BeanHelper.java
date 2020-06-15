package de.l3s.util.bean;

import java.util.Map;
import java.util.StringJoiner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Servlets;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User;

public final class BeanHelper {
    private static final Logger log = LogManager.getLogger(BeanHelper.class);

    /**
     * @return some attributes of the current http request like url, referrer, ip etc.
     */
    public static String getRequestSummary() {
        return getRequestSummary(Faces.getRequest());
    }

    /**
     * @return some attributes of a request like url, referrer, ip etc.
     */
    public static String getRequestSummary(HttpServletRequest request) {
        StringJoiner joiner = new StringJoiner("; ", "[", "]");

        try {
            joiner.add("page: " + Servlets.getRequestURLWithQueryString(request));
            joiner.add("referrer: " + request.getHeader("referer"));

            joiner.add("ip: " + request.getRemoteAddr());
            joiner.add("ipHeader: " + request.getHeader("X-FORWARDED-FOR"));
            joiner.add("userAgent: " + request.getHeader("User-Agent"));

            joiner.add("parameters: " + printMap(request.getParameterMap()));

            HttpSession session = request.getSession(false);
            if (session != null) {
                Learnweb learnweb = Learnweb.getInstance();
                Integer userId = (Integer) session.getAttribute("learnweb_user_id");

                if (learnweb != null && userId != null && userId != 0) {
                    User user = learnweb.getUserManager().getUser(userId);
                    if (user != null) {
                        joiner.add("user: " + user);
                    }
                }
            }
        } catch (Throwable e) {
            log.error("An error occurred during gathering request summary", e);
        }

        return joiner.toString();
    }

    private static String printMap(Map<String, String[]> map) {
        if (map == null) {
            return null;
        }

        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            String value = entry.getKey().contains("password") ? "XXX replaced XXX" : String.join("; ", entry.getValue());
            joiner.add(entry.getKey() + "=[" + value + "]");
        }
        return joiner.toString();
    }
}
