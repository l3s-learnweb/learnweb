package de.l3s.learnweb.resource.archive;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.resource.Resource;

@ApplicationScoped
public class WaybackCapturesLogger {
    private static final Logger log = LogManager.getLogger(WaybackCapturesLogger.class);
    private static final Container LAST_ENTRY = new Container("", null, null); // this element indicates that the consumer thread should stop

    private final WaybackUrlDao waybackUrlDao;

    private final LinkedBlockingQueue<Container> queue;
    private final Thread consumerThread;
    private final ExecutorService cdxExecutorService;

    @Inject
    public WaybackCapturesLogger(final WaybackUrlDao waybackUrlDao) {
        this.waybackUrlDao = waybackUrlDao;

        this.queue = new LinkedBlockingQueue<>();
        this.consumerThread = new Thread(new Consumer());
        this.cdxExecutorService = Executors.newSingleThreadExecutor();
    }

    @PostConstruct
    public void start() {
        this.consumerThread.start();
    }

    public void logWaybackUrl(String url, LocalDateTime firstCapture, LocalDateTime lastCapture) {
        try {
            Container container = new Container(url, firstCapture, lastCapture);
            if (!queue.contains(container)) {
                queue.put(container);
            }
        } catch (InterruptedException e) {
            log.fatal("Couldn't log wayback url capture", e);
        }
    }

    public void logWaybackCaptures(Resource resource) {
        if (resource == null) {
            throw new IllegalStateException();
        }

        cdxExecutorService.submit(new CDXWorker(resource));
    }

    @PreDestroy
    public void onDestroy() {
        try {
            queue.put(LAST_ENTRY);
            consumerThread.join();

            cdxExecutorService.shutdown();
            //Wait for a while for currently executing tasks to terminate
            if (!cdxExecutorService.awaitTermination(50, TimeUnit.SECONDS)) {
                cdxExecutorService.shutdownNow(); //cancelling currently executing tasks
            }

            log.debug("Wayback captures executor service was stopped");
        } catch (InterruptedException e) {
            log.fatal("Couldn't stop wayback captures logger", e);
            // (Re-)Cancel if current thread also interrupted
            cdxExecutorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    private static class Container {
        final String url;
        final LocalDateTime firstCapture;
        final LocalDateTime lastCapture;

        Container(String url, LocalDateTime firstCapture, LocalDateTime lastCapture) {
            this.url = url;
            this.firstCapture = firstCapture;
            this.lastCapture = lastCapture;
        }

        @Override
        public String toString() {
            return "Container [url=" + url + ", firstCapture=" + firstCapture + ", lastCapture=" + lastCapture + "]";
        }

    }

    private class Consumer implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    Container container = queue.take();

                    if (container == LAST_ENTRY) { // stop method was called
                        break;
                    }

                    waybackUrlDao.insert(container.url, container.firstCapture, container.lastCapture);
                    //log.debug("Logged suggestion: " + container);
                }

                log.debug("Wayback Captures logger thread was stopped");
            } catch (InterruptedException e) {
                log.fatal("Wayback Captures logger crashed", e);
            }
        }
    }

    private class CDXWorker implements Callable<String> {
        final Resource resource;

        CDXWorker(Resource resource) {
            this.resource = resource;
        }

        @Override
        public String call() {
            CDXClient cdxClient = new CDXClient();
            List<LocalDateTime> timestamps = cdxClient.getCaptures(resource.getUrl());
            Optional<Integer> urlId = waybackUrlDao.findIdByUrl(resource.getUrl());
            if (urlId.isPresent()) {
                waybackUrlDao.updateMarkAllCapturesFetched(urlId.get());
                waybackUrlDao.insertCapture(urlId.get(), timestamps);
                log.debug("Logged the wayback captures in the database for: {}", resource.getUrl());
            }
            resource.addArchiveUrl(null);
            return null;
        }
    }
}
