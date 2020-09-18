package de.l3s.learnweb.hserver;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import de.l3s.util.StringHelper;

public final class HUtils {
    private static final Pattern USER_PATTERN = Pattern.compile("^acct:([^@]+)@(.*)$");
    private static final String[] PROXY_PREFIXS = {"https://via.hypothes.is/", "http://localhost:8080/my-web/record/",
        "http://waps.io/my-web/record/", "https://waps.io/my-web/record/"};
    private static final String[] URL_SCHEMES = {"http", "https"};

    public static Integer getUserIdFromUser(String user) {
        if (StringUtils.isNotBlank(user)) {
            Matcher matcher = USER_PATTERN.matcher(user);
            if (matcher.find()) {
                return StringHelper.parseInt(matcher.group(1), 0);
            }
        }
        return null;
    }

    public static Integer parseInt(String str) {
        if (StringUtils.isNotBlank(str)) {
            return StringHelper.parseInt(str, 0);
        }
        return null;
    }

    public static String getUserForUserId(Integer userId) {
        return "acct:" + userId + "@learnweb.l3s.uni-hannover.de";
    }

    /**
     * Translate the given URI into a normalized form.
     */
    public static String normalizeUri(String uriStr) {
        // Strip proxy prefix for proxied URLs
        for (String proxy : PROXY_PREFIXS) {
            for (String scheme : URL_SCHEMES) {
                if (uriStr.startsWith(proxy + scheme + ":")) {
                    uriStr = uriStr.substring(proxy.length());
                    break;
                }
            }
        }

        try {
            // Try to extract the scheme
            URI uri = new URI(uriStr);

            // If this isn't a URL, we don't perform any normalization
            if (Arrays.stream(URL_SCHEMES).noneMatch(uri.getScheme()::equalsIgnoreCase)) {
                return uriStr;
            }

            // Don't perform normalization on URLs with no hostname.
            if (uri.getHost() == null) {
                return uriStr;
            }

            // TODO: better normalization
            return uri.normalize().toASCIIString();
        } catch (URISyntaxException e) {
            // something wrong
            return null;
        }
    }
}
