package de.l3s.util;

import java.net.HttpURLConnection;
import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class UrlHelper {
    private static final Logger log = LogManager.getLogger(UrlHelper.class);

    private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:70.0) Gecko/20100101 Firefox/70.0";

    /**
     * This function checks if a given String is a valid url.
     *
     * @return when the url leads to a redirect the function will return the target of the redirect.
     *     Returns {@code null} if the url is invalid or not reachable.
     */
    public static String validateUrl(String url) {
        try {
            if (!url.startsWith("http")) {
                url = "http://" + url;
            }
            url = toAscii(url.trim());

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", USER_AGENT);

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return url;
            } else if (responseCode >= 300 && responseCode < 400) {
                String location = connection.getHeaderField("Location");
                if (location.startsWith("/")) {
                    location = getOrigin(url) + location;
                }

                return location;
            }
        } catch (UnknownHostException e) {
            log.warn("unknown host: " + url, e);
        } catch (Exception e) {
            log.error("invalid url: " + url, e);
        }

        return null;
    }

    public static String getOrigin(final String input) {
        int index = input.indexOf('/', input.indexOf("//") + 2);
        return index > 0 ? input.substring(0, index) : input;
    }

    /**
     * Encodes the domain using punycode and the query using percent-encoding.
     */
    public static String toAscii(String url) throws URISyntaxException {
        if (url == null) {
            return null;
        }

        // Handle international domains by detecting non-ascii and converting them to punycode
        if (!StringHelper.isASCII(url)) {
            URI uri = new URI(url);

            // URI needs a scheme to work properly with authority parsing
            if (uri.getScheme() == null) {
                uri = new URI("http://" + url);
            }

            String scheme = uri.getScheme() == null ? null : (uri.getScheme() + "://");
            String authority = uri.getRawAuthority() == null ? "" : uri.getRawAuthority(); // includes domain and port
            String path = uri.getRawPath() == null ? "" : uri.getRawPath();
            String queryString = uri.getRawQuery() == null ? "" : ("?" + uri.getRawQuery());

            // Must convert domain to punycode separately from the path
            url = scheme + IDN.toASCII(authority) + path + queryString;

            // Convert path from unicode to ascii encoding
            url = new URI(url).toASCIIString();
        }
        return url;
    }
}
