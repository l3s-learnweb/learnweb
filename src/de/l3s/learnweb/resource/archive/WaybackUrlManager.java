package de.l3s.learnweb.resource.archive;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
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
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.HttpsURLConnection;
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

        enableRelaxedSSLconnection();
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

        urlStr = urlStr.replaceAll(" ", "%20"); //few urls from Bing do not encode space correctly
        String originalUrl = urlStr; // in case we get redirect we need to compare the urls
        int maxRedirects = 20;
        StringJoiner cookies = new StringJoiner(";");
        List<String> seenURLs = null;
        ///seenURLs.add(urlStr);

        while (maxRedirects > 0) {
            try {
                java.net.URL url = new java.net.URL(urlStr);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(60000);
                connection.setReadTimeout(60000);
                connection.setRequestProperty("User-Agent", UrlHelper.USER_AGENT);
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                connection.setRequestProperty("Accept-Language", "en,en-US;q=0.8,de;q=0.5,de-DE;q=0.3");
                connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
                connection.setRequestProperty("Referer", "https://www.bing.com/");
                connection.setRequestProperty("Connection", "keep-alive");
                if (cookies.length() > 0) {
                    connection.setRequestProperty("Cookie", cookies.toString());
                }

                responseCode = connection.getResponseCode();

                if (responseCode == -1) {
                    responseCode = 652;
                    break;
                }

                String server = connection.getHeaderField("Server");
                if (responseCode == 403 && server != null && server.equals("cloudflare-nginx")) {
                    responseCode = 606;
                    break;
                }

                List<String> cookiesHeader = connection.getHeaderFields().get("Set-Cookie");

                if (cookiesHeader != null) {
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
                    String location = connection.getHeaderField("Location");

                    if (location == null) {
                        responseCode = 607; //no location for redirect status code
                        break;
                    }

                    location = location.replaceAll(" ", "%20");

                    //log.debug("Redirect {}; Location {}", responseCode, location);

                    if (location.startsWith("/")) {
                        int index = urlStr.indexOf('/', urlStr.indexOf("//") + 2);
                        String domain = index > 0 ? urlStr.substring(0, index) : urlStr;
                        urlStr = domain + location;
                    } else if (!location.startsWith("http")) {
                        urlStr = "http://" + location;
                    } else {
                        urlStr = location;
                    }

                    connection.disconnect();

                    if (null == seenURLs) {
                        seenURLs = new LinkedList<>(); // init here to allow the two redirects to the initial url
                    }

                    if (seenURLs.contains(urlStr)) {
                        responseCode = 604; // too many redirects
                        break;
                    }
                    seenURLs.add(urlStr);
                } else {
                    if (responseCode >= 200 && responseCode < 300) {
                        //download content if mime type does not start with "application/" (don't download PDFs)
                        String contentType = connection.getContentType();
                        if (contentType != null && !contentType.startsWith("application/")) {
                            //To handle gzip and deflate encodings
                            String contentEncoding = connection.getContentEncoding();
                            InputStream inputStream;
                            if ("gzip".equalsIgnoreCase(contentEncoding)) {
                                inputStream = new GZIPInputStream(connection.getInputStream());
                            } else if ("deflate".equalsIgnoreCase(contentEncoding)) {
                                Inflater inf = new Inflater(true);
                                inputStream = new InflaterInputStream(connection.getInputStream(), inf);
                            } else {
                                inputStream = connection.getInputStream();
                            }

                            //To check webpage character encoding if present in header field: Content-Type
                            String[] contentTypeSplit = contentType.split("charset=");
                            String charsetStr = null;
                            if (contentTypeSplit.length == 2) {
                                charsetStr = contentTypeSplit[1];
                            }

                            Charset charset = null;
                            try {
                                if (charsetStr != null && !charsetStr.isEmpty()) {
                                    charset = Charset.forName(charsetStr);
                                }
                            } catch (IllegalCharsetNameException ignored) {
                                charset = StandardCharsets.UTF_8;
                            }

                            String content = IOUtils.toString(inputStream, charset);
                            urlRecord.setContent(content.trim());
                        }
                    }

                    connection.disconnect();
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
                logUrlInFile(urlStr);
                break;
            } catch (SocketTimeoutException e) {
                log.warn("SocketTimeoutException: {}; URL: {}", e.getMessage(), urlStr);
                responseCode = 603;
                break;
            } catch (SSLException e) {
                log.warn("SSLException: {}; URL: {}", e.getMessage(), urlStr);
                responseCode = getStatusCodeFromHttpClient(urlRecord);
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

    public UrlRecord getHtmlContent(String url) throws IOException, URISyntaxException {
        return getStatusCode(new UrlRecord(new URL(url)));
    }

    /**
     * This method is called only when there is a SSLHandshake failure from the previous method.
     */
    private int getStatusCodeFromHttpClient(UrlRecord urlRecord) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlRecord.getUrl().toString()))
                .header("User-Agent", UrlHelper.USER_AGENT)
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            urlRecord.setContent(response.body().trim());
            return 200;
        } catch (IOException | InterruptedException e) {
            log.warn("HttpClient method: SSLException: {}; URL: {}", e.getMessage(), urlRecord.getUrl());
            logUrlInFile(urlRecord.getUrl().toString());
            return 650;
        } catch (IllegalArgumentException e) {
            log.warn("HttpClient method: Invalid url: {}; URL: {}", e.getMessage(), urlRecord.getUrl());
            logUrlInFile(urlRecord.getUrl().toString());
            return 650;
        }
    }

    public void logUrlInFile(String url) {
        String filename = "/home/learnweb_user/searchlog_html_url_exceptions.txt";
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename, StandardCharsets.UTF_8, true)))) {
            out.println(url);
        } catch (IOException e) {
            log.error("Exception while writing url exception to file {}", e.getMessage());
        }
    }

    public static void enableRelaxedSSLconnection() {
        try {
            // Install relaxed TrustManager
            HttpsURLConnection.setDefaultSSLSocketFactory(UrlHelper.getUnsafeSSLContext().getSocketFactory());
            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static class RecordNotFoundException extends Exception {
        private static final long serialVersionUID = -1754500933829531955L;
    }
}
