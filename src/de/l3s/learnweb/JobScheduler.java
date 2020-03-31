package de.l3s.learnweb;

import org.apache.log4j.Logger;

import de.l3s.learnweb.resource.speechRepository.SpeechRepositoryCrawlerSimple;
import de.l3s.learnweb.resource.ted.TedCrawlerSimple;
import de.l3s.learnweb.user.loginProtection.ExpiredBansCleaner;
import de.l3s.learnweb.web.RequestsTaskHandler;
import de.l3s.util.email.BounceFetcher;
import it.sauronsoftware.cron4j.Scheduler;

public class JobScheduler
{
    private final Logger log = Logger.getLogger(JobScheduler.class);
    private Scheduler scheduler;

    private Learnweb learnweb;

    /**
     * Description about Scheduling patterns
     * http://www.sauronsoftware.it/projects/cron4j/manual.php#p02
     */
    protected JobScheduler(Learnweb learnweb)
    {
        this.learnweb = learnweb;
        this.scheduler = new Scheduler();

        if(Learnweb.isInDevelopmentMode())
            return;

        // checks if url ends with .de, .com or .de/, if it ends with something like .de/1 than it is not the root instance
        //boolean isRootInstance = this.learnweb.getServerUrl().length() < this.learnweb.getServerUrl().lastIndexOf(".") + 3;

        boolean isRootInstance = this.learnweb.getServerUrl().endsWith("learnweb.l3s.uni-hannover.de/v3/"); // TODO need to revert when V3 becomes ROOT

        if(!isRootInstance)
        {
            log.warn("JobScheduler stopped because it seams that this instance is only for testing: " + this.learnweb.getServerUrl());
            return;
        }

        //Cleans up expired bans once a week on Sunday at 3:00AM
        scheduler.schedule("0 3 * * Sun", new ExpiredBansCleaner());

        //Cleans up requests once an hour
        scheduler.schedule("0 * * * *", new RequestsTaskHandler());

        //Runs the TED crawler at 23:00 once a month to check for new/update TED videos
        scheduler.schedule("0 23 1 * *", new TedCrawlerSimple());
        //Runs the speech repository crawler at 22:00 once a month to check for new/update of videos
        scheduler.schedule("0 22 2 * *", new SpeechRepositoryCrawlerSimple());

        //Checks bounced mail every 5 minutes
        scheduler.schedule("0/5 * * * *", new BounceFetcher());
    }

    public void startAllJobs()
    {
        if(!scheduler.isStarted())
            scheduler.start();
    }

    public void stopAllJobs()
    {
        if(scheduler.isStarted())
            scheduler.stop();
    }
}
