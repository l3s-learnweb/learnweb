package de.l3s.learnweb.resource.archive;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.app.ConfigProvider;
import de.l3s.learnweb.resource.web.WebResource;

@ApplicationScoped
public final class ArchiveUrlManager {
    private static final Logger log = LogManager.getLogger(ArchiveUrlManager.class);
    private static final Pattern SCHEME = Pattern.compile("^https?://", Pattern.CASE_INSENSITIVE);
    private static final Pattern SNAPSHOT_PATH = Pattern.compile("^/web/(\\d{14})[a-z_]*/");
    private static final DateTimeFormatter WAYBACK_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final String archiveSaveURL;
    private final ArchiveUrlDao archiveUrlDao;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @Inject
    public ArchiveUrlManager(ConfigProvider configProvider, final ArchiveUrlDao archiveUrlDao) {
        this.archiveSaveURL = configProvider.getProperty("integration_archive_saveurl");
        this.archiveUrlDao = archiveUrlDao;
    }

    /**
     * Asks the Wayback Machine to capture the resource and stores the resulting snapshot URL.
     *
     * @return false if the page is blocked by robots.txt
     */
    public boolean addResourceToArchive(WebResource resource) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(archiveSaveURL + resource.getUrl()))
            .header("Accept", "application/xml")
            .timeout(Duration.ofMinutes(3)) // capturing a page can take a while, but must not block the request thread forever
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() == HttpURLConnection.HTTP_FORBIDDEN && response.headers().firstValue("X-Archive-Wayback-Liveweb-Error")
            .filter("RobotAccessControlException: Blocked By Robots"::equalsIgnoreCase).isPresent()) {
            log.info("Cannot archive resource {}: blocked by robots.txt", resource.getId());
            return false;
        }

        // the snapshot is named in a header; redirects are not followed, since loading the snapshot page can fail even though the capture succeeded
        URI snapshot = response.headers().firstValue("Content-Location")
            .or(() -> response.headers().firstValue("Location"))
            .map(response.uri()::resolve)
            .orElse(response.uri());
        Matcher matcher = SNAPSHOT_PATH.matcher(snapshot.getPath());
        if (response.statusCode() >= 400 || !matcher.find()) {
            log.debug("Archive response for resource {}: {}", resource.getId(), response.body());
            throw new IOException("Cannot archive URL, unexpected response status: " + response.statusCode());
        }

        String archiveUrl = snapshot.toString();
        // Wayback may return an existing capture, older ones were stored with http://
        String archiveUrlWithoutScheme = stripScheme(archiveUrl);
        if (resource.getArchiveUrls().stream().noneMatch(url -> stripScheme(url.archiveUrl()).equals(archiveUrlWithoutScheme))) {
            // the snapshot timestamp is the capture time in UTC
            LocalDateTime timestamp = LocalDateTime.parse(matcher.group(1), WAYBACK_TIMESTAMP)
                .atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            archiveUrlDao.insertArchiveUrl(resource.getId(), archiveUrl, timestamp);
            resource.setArchiveUrls(null);
        }
        return true;
    }

    private static String stripScheme(String url) {
        return SCHEME.matcher(url).replaceFirst("");
    }

    @PreDestroy
    public void onDestroy() {
        httpClient.shutdownNow();
    }
}
