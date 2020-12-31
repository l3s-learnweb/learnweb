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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.app.ConfigProvider;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDecorator;

@ApplicationScoped
public final class ArchiveUrlManager {
    private static final Logger log = LogManager.getLogger(ArchiveUrlManager.class);

    private final String archiveSaveURL;
    private final ArchiveUrlDao archiveUrlDao;
    private final WaybackUrlDao waybackUrlDao;

    private final ExecutorService executorService;
    private final ExecutorService cdxExecutorService;

    @Inject
    public ArchiveUrlManager(ConfigProvider configProvider, final ArchiveUrlDao archiveUrlDao, final WaybackUrlDao waybackUrlDao) {
        this.archiveSaveURL = configProvider.getProperty("internet_archive_save_url");
        this.archiveUrlDao = archiveUrlDao;
        this.waybackUrlDao = waybackUrlDao;

        executorService = Executors.newCachedThreadPool();
        cdxExecutorService = Executors.newSingleThreadExecutor(); // In order to sequentially poll the CDX server and not overload it
    }

    public void checkWaybackCaptures(ResourceDecorator resource) {
        try {
            if (resource.getResource().getMetadataValue("first_timestamp") == null) {
                Optional<ImmutablePair<String, String>> firstAndLastCapture = waybackUrlDao.findFirstAndLastCapture(resource.getUrl());
                if (firstAndLastCapture.isPresent()) {
                    resource.getResource().setMetadataValue("first_timestamp", firstAndLastCapture.get().left);
                    resource.getResource().setMetadataValue("last_timestamp", firstAndLastCapture.get().right);
                } else {
                    cdxExecutorService.submit(new CDXWorker(resource));
                }
            }
        } catch (RejectedExecutionException e) {
            log.error("Checking if executor was shutdown: {}", cdxExecutorService.isShutdown());
            log.error("Executor exception while submitting new wayback capture request", e);
        }
    }

    public String addResourceToArchive(Resource resource) {
        String response = "";
        if (resource.isWebResource()) {
            Future<String> executorResponse = executorService.submit(new ArchiveNowWorker(resource));

            try {
                response = executorResponse.get();
                //log.debug(response);
            } catch (InterruptedException e) {
                log.error("Execution of the thread was interrupted on a task for resource: {}", resource.getId(), e);
            } catch (ExecutionException e) {
                log.error("Error while retrieving response from a task that was interrupted by an exception for resource: {}", resource.getId(), e);
            }
        }
        return response;
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

    private static final class CDXWorker implements Callable<String> {
        private final ResourceDecorator resource;

        private CDXWorker(ResourceDecorator resource) {
            this.resource = resource;
        }

        @Override
        public String call() throws IOException {
            CDXClient cdxClient = new CDXClient();
            cdxClient.isArchived(resource);
            return null;
        }
    }

    class ArchiveNowWorker implements Callable<String> {
        private final DateTimeFormatter responseDate = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

        final Resource resource;

        ArchiveNowWorker(Resource resource) {
            this.resource = resource;
        }

        @Override
        public String call() throws Exception {
            if (resource == null) {
                return "resource was NULL";
            }

            if (resource.getArchiveUrls() != null) {
                int versions = resource.getArchiveUrls().size();
                if (versions > 0) {
                    boolean isArchivedRecently = resource.getArchiveUrls().getLast().getTimestamp().isAfter(LocalDateTime.now().minusMinutes(5));
                    if (isArchivedRecently) {
                        return "resource was last archived less than 5 minutes ago";
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
                resource.addArchiveUrl(null);
            } else if (response.statusCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                Optional<String> livewebError = response.headers().firstValue("X-Archive-Wayback-Liveweb-Error");
                if (livewebError.isPresent()) {
                    if (livewebError.get().equalsIgnoreCase("RobotAccessControlException: Blocked By Robots")) {
                        return "ROBOTS_ERROR";
                    }
                }

                log.error("Cannot archive URL because of an error other than robots.txt for resource: {}; Response: {}", resource.getId(), response.body());
                return "GENERIC_ERROR";
            }

            return "ARCHIVE_SUCCESS";
        }

    }

}
