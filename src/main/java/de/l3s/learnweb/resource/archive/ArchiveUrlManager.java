package de.l3s.learnweb.resource.archive;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

    private final String archiveSaveURL;
    private final ArchiveUrlDao archiveUrlDao;

    private final ExecutorService executorService;
    private final ExecutorService cdxExecutorService;

    @Inject
    public ArchiveUrlManager(ConfigProvider configProvider, final ArchiveUrlDao archiveUrlDao) {
        this.archiveSaveURL = configProvider.getProperty("integration_archive_saveurl");
        this.archiveUrlDao = archiveUrlDao;

        executorService = Executors.newCachedThreadPool();
        cdxExecutorService = Executors.newSingleThreadExecutor(); // In order to sequentially poll the CDX server and not overload it
    }

    public Boolean addResourceToArchive(WebResource resource) throws IOException {
        try {
            Future<Boolean> executorResponse = executorService.submit(new ArchiveNowWorker(resource));
            return executorResponse.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error while retrieving response from a task that was interrupted by an exception for resource: {}", resource.getId(), e);
        }
        return false;
    }

    @PreDestroy
    public void onDestroy() {
        executorService.shutdown();
        cdxExecutorService.shutdown();
        try {
            //Wait for a while for currently executing tasks to terminate
            if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                executorService.shutdownNow(); //cancelling currently executing tasks
            }
        } catch (InterruptedException e) {
            // (Re-)Cancel if current thread also interrupted
            executorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        try {
            //Wait for a while for currently executing tasks to terminate
            if (!cdxExecutorService.awaitTermination(1, TimeUnit.SECONDS)) {
                cdxExecutorService.shutdownNow(); //cancelling currently executing tasks
            }
        } catch (InterruptedException e) {
            // (Re-)Cancel if current thread also interrupted
            cdxExecutorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }

    }

    class ArchiveNowWorker implements Callable<Boolean> {
        private final DateTimeFormatter responseDate = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

        final WebResource resource;

        ArchiveNowWorker(WebResource resource) {
            this.resource = resource;
        }

        @Override
        public Boolean call() throws InterruptedException, IOException, IllegalArgumentException {
            if (resource == null) {
                throw new IllegalArgumentException("resource was NULL");
            }

            if (resource.getArchiveUrls() != null) {
                int versions = resource.getArchiveUrls().size();
                if (versions > 0) {
                    boolean isArchivedRecently = resource.getArchiveUrls().getLast().timestamp().isAfter(LocalDateTime.now().minusMinutes(5));
                    if (isArchivedRecently) {
                        throw new IllegalArgumentException("resource was last archived less than 5 minutes ago");
                    }
                }
            }

            String archiveURL = null;
            String mementoDateString = null;
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(archiveSaveURL + resource.getUrl()))
                .header("Accept", "application/xml")
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                Optional<String> contentLocation = response.headers().firstValue("Content-Location");
                if (contentLocation.isPresent()) {
                    archiveURL = "http://web.archive.org" + contentLocation.get();
                } else {
                    log.debug("Content Location not found");
                }

                Optional<String> archiveOrigDate = response.headers().firstValue("X-Archive-Orig-Date");
                if (archiveOrigDate.isPresent()) {
                    mementoDateString = archiveOrigDate.get();
                } else {
                    log.debug("X-Archive-Orig-Date not found");
                }

                LocalDateTime archiveUrlDate = null;
                if (mementoDateString != null) {
                    archiveUrlDate = LocalDateTime.parse(mementoDateString, responseDate);
                }

                log.debug("Archived URL:{} Memento DateTime:{}", archiveURL, mementoDateString);
                archiveUrlDao.insertArchiveUrl(resource.getId(), archiveURL, archiveUrlDate);
                resource.setArchiveUrls(null);
            } else if (response.statusCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                Optional<String> livewebError = response.headers().firstValue("X-Archive-Wayback-Liveweb-Error");
                if (livewebError.isPresent()) {
                    if ("RobotAccessControlException: Blocked By Robots".equalsIgnoreCase(livewebError.get())) {
                        throw new IOException("Blocked by robots.txt");
                    }
                }

                log.error("Cannot archive URL because of an error other than robots.txt for resource: {}; Response: {}", resource.getId(), response.body());
                throw new InterruptedException("Cannot archive URL because of an error other than robots.txt");
            }

            return true;
        }

    }

}
