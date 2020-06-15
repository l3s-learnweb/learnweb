package de.l3s.util.bean;

import java.util.Arrays;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Faces;

import de.l3s.learnweb.Learnweb;
import de.l3s.util.StringHelper;

public final class BeanHelper {
    private static final Logger log = LogManager.getLogger(BeanHelper.class);

    /**
     * @return some attributes of the current http request like url, referrer, ip etc.
     */
    public static String getRequestSummary() {
        return getRequestSummary(null);
    }

    /**
     * @return some attributes of a request like url, referrer, ip etc.
     */
    public static String getRequestSummary(HttpServletRequest request) {
        String url = null;
        String referrer = null;
        String ip = null;
        String ipForwardedForHeader = null;
        String userAgent = null;
        Integer userId = null;
        String user = null;
        Map<String, String[]> parameters = null;

        try {
            if (request == null) {
                request = Faces.getRequest();
            }

            parameters = request.getParameterMap();
            referrer = request.getHeader("referer");
            ip = request.getRemoteAddr();

            ipForwardedForHeader = request.getHeader("X-FORWARDED-FOR");

            userAgent = request.getHeader("User-Agent");
            url = request.getRequestURL().toString();
            if (request.getQueryString() != null) {
                url += '?' + request.getQueryString();
            }

            HttpSession session = request.getSession(false);
            if (session != null) {
                userId = (Integer) session.getAttribute("learnweb_user_id");
            }

            if (userId == null) {
                user = "not logged in";
            } else {
                Learnweb learnweb = Learnweb.getInstance();
                if (learnweb != null) {
                    user = learnweb.getUserManager().getUser(userId).toString();
                }
            }
        } catch (Throwable ignored) {
        }
        return "page: " + url + " ; user: " + user + "; ip: " + ip + "; ipHeader: " + ipForwardedForHeader + "; referrer: " + referrer + " ; userAgent: " + userAgent + "; parameters: " + printMap(parameters) + ";";
    }

    private static String printMap(Map<String, String[]> map) {
        if (null == map) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            sb.append(entry.getKey());
            sb.append("=[");
            if (entry.getKey().contains("password")) {
                sb.append("XXX replaced XXX");
            } else {
                sb.append(StringHelper.implode(Arrays.asList(entry.getValue()), "; "));
            }
            sb.append("], ");
        }
        if (map.size() > 1) { // remove last comma and whitespace
            sb.setLength(sb.length() - 2);
        }
        sb.append("}");
        return sb.toString();
    }
}
