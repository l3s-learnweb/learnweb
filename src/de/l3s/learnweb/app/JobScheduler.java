package de.l3s.learnweb.app;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.l3s.learnweb.forum.ForumNotificator;
import de.l3s.learnweb.resource.speechRepository.SpeechRepositoryCrawler;
import de.l3s.learnweb.resource.ted.TedCrawlerSimple;
import de.l3s.learnweb.web.BounceFetcher;
import de.l3s.learnweb.web.ExpiredBansCleaner;
import de.l3s.learnweb.web.RequestsTaskHandler;
import it.sauronsoftware.cron4j.Scheduler;

/**
 * Description about Scheduling patterns.
 * http://www.sauronsoftware.it/projects/cron4j/manual.php#p02
 */
@ApplicationScoped
public class JobScheduler {

    private Scheduler scheduler;

    @Inject
    public JobScheduler(final ConfigProvider configProvider) {
        // If started in development (also when no servlet context) or other test instance, do not schedule any jobs.
        if (configProvider.isDevelopment() || !"https://learnweb.l3s.uni-hannover.de".equals(configProvider.getServerUrl())) {
            return;
        }

        scheduler = new Scheduler();

        // Cleans up expired bans once a week on Sunday at 3:00AM
        scheduler.schedule("0 3 * * Sun", new ExpiredBansCleaner());
        // Cleans up requests once an hour
        scheduler.schedule("0 * * * *", new RequestsTaskHandler());

        // Runs the TED crawler at 23:00 once a month to check for new/update TED videos
        scheduler.schedule("0 23 1 * *", new TedCrawlerSimple());
        // Runs the speech repository crawler at 22:00 once a month to check for new/update of videos
        scheduler.schedule("0 22 2 * *", new SpeechRepositoryCrawler());

        // Checks bounced mail every 5 minutes
        scheduler.schedule("0/5 * * * *", new BounceFetcher());

        // Runs the Forum Notificator at 8:00AM once a day to send summary emails
        scheduler.schedule("0 8 * * *", new ForumNotificator());
    }

    @PostConstruct
    public void startAllJobs() {
        if (scheduler != null && !scheduler.isStarted()) {
            scheduler.start();
        }
    }

    @PreDestroy
    public void stopAllJobs() {
        if (scheduler != null && scheduler.isStarted()) {
            scheduler.stop();
        }
    }
}
