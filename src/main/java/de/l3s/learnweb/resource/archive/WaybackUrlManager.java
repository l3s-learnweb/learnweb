package de.l3s.learnweb.resource.archive;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.net.HttpCookie;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.SSLException;

import jakarta.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.l3s.util.URL;
import de.l3s.util.UrlHelper;

public final class WaybackUrlManager {
    private static final Logger log = LogManager.getLogger(WaybackUrlManager.class);

    private final LoadingCache<URL, UrlRecord> cache;
    private final CDXClient cdxClient;

    @Inject
    private WaybackUrlDao waybackUrlDao;

    public WaybackUrlManager() {
        cdxClient = new CDXClient();

        cache = Caffeine.newBuilder().maximumSize(3000000).build(url -> {
            Optional<UrlRecord> record = waybackUrlDao.findUrlRecordByUrl(url.toString());

            if (record.isEmpty()) {
                throw new RecordNotFoundException();
            }

            return record.get();
        });
    }

    /**
     * @param url UTF8 or ASCII encoded URL
     */
    public UrlRecord getUrlRecord(String url) throws URISyntaxException {
        URL asciiUrl = new URL(url);

        try {
            return cache.get(asciiUrl);
        } catch (Exception e) {
            if (e.getCause() instanceof RecordNotFoundException) {
                // create a new record for this url
                return new UrlRecord(asciiUrl);
            } else {
                log.error("fatal", e);
                throw new IllegalStateException(e);
            }
        }
    }

    public boolean hasToBeUpdated(UrlRecord urlRecord, Instant minAcceptableCrawlTime, Instant minAcceptableStatusTime) {
        if (minAcceptableCrawlTime != null && urlRecord.getCrawlDate().isBefore(minAcceptableCrawlTime)) {
            return true;
        }
        if (minAcceptableStatusTime != null && urlRecord.getStatusCodeDate().isBefore(minAcceptableStatusTime)) {
            return true;
        }
        return false;
    }

    public boolean updateRecord(UrlRecord record, Instant minAcceptableCrawlTime, Instant minAcceptableStatusTime) {
        boolean capturesUpdated = minAcceptableCrawlTime != null && updateRecordCaptures(record, minAcceptableCrawlTime);
        boolean statusUpdated = minAcceptableStatusTime != null && updateStatusCode(record, minAcceptableStatusTime);
        if (capturesUpdated || statusUpdated) {
            waybackUrlDao.saveUrlRecord(record);
            cache.put(record.getUrl(), record);
            return true;
        }
        return false;
    }

    private boolean updateStatusCode(UrlRecord urlRecord, Instant minAcceptableCrawlTime) {
        if (urlRecord.getStatusCodeDate().isAfter(minAcceptableCrawlTime)) {
            return false; // already up-to-date
        }

        //int statusCode = -1;

        try {
            //statusCode = getStatusCode(urlRecord.getUrl().toString());
            getStatusCode(urlRecord);
        } catch (Throwable t) {
            log.error("can't check url: {}", urlRecord.getUrl(), t);
        }

        // TODO @astappiev: insert content and status into wb_url_content

        //urlRecord.setStatusCode((short) statusCode);
        //urlRecord.setStatusCodeDate(new Date());
        return true;
    }

    private boolean updateRecordCaptures(UrlRecord urlRecord, Instant minAcceptableStatusTime) {
        if (urlRecord.getCrawlDate().isAfter(minAcceptableStatusTime)) {
            return false; // already up-to-date
        }

        String url = urlRecord.getUrl().toString();
        url = url.substring(url.indexOf("//") + 2); // remove protocol (http(s)://)

        try {
            LocalDateTime lastCapture = null;
            LocalDateTime firstCapture = cdxClient.getFirstCaptureDate(url);

            if (firstCapture != null) {
                lastCapture = cdxClient.getLastCaptureDate(url);
            }

            if (lastCapture != null) {
                urlRecord.setFirstCapture(firstCapture);
                urlRecord.setLastCapture(lastCapture);
            } else {
                urlRecord.setFirstCapture(null);
                urlRecord.setLastCapture(null);
            }

            urlRecord.setCrawlDate(Instant.now());
            return true;

        } catch (IOException e) {
            log.error("Wayback error for: {}", urlRecord.getUrl(), e);
        }

        return false;
    }

    public UrlRecord getStatusCode(UrlRecord urlRecord) throws IOException {
        int responseCode = -1;
        String urlStr = urlRecord.getUrl().toString();
        urlStr = urlStr.replaceAll(" ", "%20"); // few urls from Bing do not encode space correctly

        String originalUrl = urlStr; // in case we get redirect we need to compare the urls
        int maxRedirects = 20;
        StringJoiner cookies = new StringJoiner(";");
        List<String> seenURLs = new LinkedList<>();

        while (maxRedirects > 0) {
            try {
                HttpClient client = HttpClient.newBuilder().sslContext(UrlHelper.getUnsafeSSLContext()).build();
                HttpRequest.Builder request = HttpRequest.newBuilder().uri(URI.create(urlStr)).GET()
                    .timeout(Duration.of(60, ChronoUnit.SECONDS))
                    .headers("User-Agent", UrlHelper.USER_AGENT)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "en,en-US;q=0.8,de;q=0.5,de-DE;q=0.3")
                    .headers("Accept-Encoding", "gzip, deflate")
                    .headers("Referer", "https://www.bing.com/")
                    .headers("Connection", "keep-alive");

                if (cookies.length() > 0) {
                    request.headers("Cookie", cookies.toString());
                }

                HttpResponse<InputStream> response = client.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());

                responseCode = response.statusCode();

                if (responseCode == -1) {
                    responseCode = 652;
                    break;
                }

                Optional<String> server = response.headers().firstValue("Server");
                if (responseCode == 403 && server.isPresent() && "cloudflare-nginx".equals(server.get())) {
                    responseCode = 606;
                    break;
                }

                List<String> cookiesHeader = response.headers().allValues("Set-Cookie");
                if (!cookiesHeader.isEmpty()) {
                    cookies = new StringJoiner(";");

                    for (String cookie : cookiesHeader) {
                        try {
                            cookies.add(HttpCookie.parse(cookie).get(0).toString());
                        } catch (IllegalArgumentException e) {
                            //log.debug("Invalid cookie: " + cookie);
                        }
                    }
                }

                if (responseCode >= 300 && responseCode < 400) {
                    maxRedirects--;
                    Optional<String> location = response.headers().firstValue("Location");

                    if (location.isEmpty()) {
                        responseCode = 607; // no location for redirect status code
                        break;
                    }

                    urlStr = getLocationUrl(location.get(), urlStr);

                    if (seenURLs.contains(urlStr)) {
                        responseCode = 604; // too many redirects
                        break;
                    }
                    seenURLs.add(urlStr);
                } else {
                    if (responseCode >= 200 && responseCode < 300) {
                        //download content if mime type does not start with "application/" (don't download PDFs)
                        Optional<String> contentType = response.headers().firstValue("Content-Type");
                        if (contentType.isPresent() && !contentType.get().startsWith("application/")) {
                            String content = IOUtils.toString(getDecodedInputStream(response), getCharset(contentType.get()));
                            urlRecord.setContent(content.trim());
                        }
                    }
                    break;
                }
            } catch (UnknownHostException e) {
                //log.warn("UnknownHostException: {}", urlStr);
                responseCode = 600;
                break;
            } catch (ProtocolException e) {
                log.warn("ProtocolException: {}; URL: {}", e.getMessage(), urlStr);
                responseCode = 601;
                break;
            } catch (SocketException e) {
                log.warn("SocketException: {}; URL: {}", e.getMessage(), urlStr);
                responseCode = 602;
                break;
            } catch (SocketTimeoutException e) {
                log.warn("SocketTimeoutException: {}; URL: {}", e.getMessage(), urlStr);
                responseCode = 603;
                break;
            } catch (SSLException e) {
                log.warn("SSLException: {}; URL: {}", e.getMessage(), urlStr);
                responseCode = 650;
                break;
            } catch (Exception e) {
                //this exception is thrown but not declared in the try block so we can't easily catch it
                if (GeneralSecurityException.class.isAssignableFrom(e.getClass())) {
                    log.warn("GeneralSecurityException: {}; URL: {}", e.getMessage(), urlStr);
                    responseCode = 651;
                    break;
                } else if (e.getCause() instanceof IllegalArgumentException) {
                    log.warn("Invalid redirect: {}; URL: {}", e.getCause().getMessage(), urlStr);
                    responseCode = 608; //redirect to invalid url
                    break;
                } else {
                    log.error("Can't check URL: {}", urlStr, e);
                    responseCode = 653;
                    break;
                    //throw e;
                }
            }
        }

        if (maxRedirects == 0) {
            responseCode = 604; // too many redirects
        }

        // check if a URL was redirected to the base URL. This can usually be handled as some kind of error 404
        if (!Objects.equals(originalUrl, urlStr)) { // got a redirect
            String pathOld = new java.net.URL(originalUrl).getPath();
            String pathNew = new java.net.URL(urlStr).getPath();

            if (responseCode < 300 && pathOld != null && pathOld.length() > 4 && (pathNew == null || pathNew.length() < 4)) {
                responseCode = 605; // redirect to main page
            }

            //log.debug("Redirect; status: {}; old {} ; new {}", responseCode, originalUrl, urlStr);
        }

        urlRecord.setStatusCode((short) responseCode);
        urlRecord.setStatusCodeDate(Instant.now());
        return urlRecord;
    }

    private static String getLocationUrl(String location, String baseUrl) {
        location = location.replaceAll(" ", "%20");
        //log.debug("Redirect {}; Location {}", responseCode, location);

        if (location.startsWith("/")) {
            int index = baseUrl.indexOf('/', baseUrl.indexOf("//") + 2);
            String domain = index > 0 ? baseUrl.substring(0, index) : baseUrl;
            return domain + location;
        } else if (!location.startsWith("http")) {
            return "http://" + location;
        } else {
            return location;
        }
    }

    public static InputStream getDecodedInputStream(HttpResponse<InputStream> response) throws IOException {
        // To handle gzip and deflate encodings
        Optional<String> encoding = response.headers().firstValue("Content-Encoding");

        if (encoding.isEmpty()) {
            return response.body();
        } else if ("gzip".equalsIgnoreCase(encoding.get())) {
            return new GZIPInputStream(response.body());
        } else if ("deflate".equalsIgnoreCase(encoding.get())) {
            return new InflaterInputStream(response.body(), new Inflater(true));
        } else {
            throw new UnsupportedOperationException("Unexpected Content-Encoding: " + encoding);
        }
    }

    public static Charset getCharset(String contentType) {
        try {
            int i = contentType.indexOf("charset=");
            if (i >= 0) {
                String charset = contentType.substring(i + 8);
                return Charset.forName(charset);
            }
        } catch (Throwable e) {
            // ignore anything
        }
        return StandardCharsets.UTF_8;
    }

    public UrlRecord getHtmlContent(String url) throws IOException, URISyntaxException {
        return getStatusCode(new UrlRecord(new URL(url)));
    }

    private static class RecordNotFoundException extends Exception {
        @Serial
        private static final long serialVersionUID = -1754500933829531955L;
    }
}
