package de.l3s.learnweb;

import de.l3s.learnweb.resource.speechRepository.SpeechRepositoryCrawlerSimple;
import org.apache.log4j.Logger;

import de.l3s.learnweb.resource.ted.TedCrawlerSimple;
import de.l3s.learnweb.user.loginProtection.ExpiredBansCleaner;
import de.l3s.learnweb.web.RequestsTaskHandler;
import de.l3s.util.email.BounceFetcher;
import it.sauronsoftware.cron4j.Scheduler;
import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;

public class JobScheduler
{
    private final Logger log = Logger.getLogger(JobScheduler.class);
    private Scheduler scheduler;
    private Learnweb learnweb;

    protected JobScheduler(Learnweb learnweb)
    {
        this.learnweb = learnweb;
        this.scheduler = new Scheduler();

        //description about Scheduling patterns : http://www.sauronsoftware.it/projects/cron4j/manual.php#p02

        //Schedules the task at 1:00 on 14th and 28th of every month
        // the loro crawler is not running at the moment
        //UpdateLoroResources loroTask = new UpdateLoroResources();
        //scheduler.schedule("0 1 14,28 * *", loroTask);

        //Schedules the task at 1:00 on 13th and 27th of every month
        //UpdateYovistoVideos yovistoTask = new UpdateYovistoVideos();
        //scheduler.schedule("0 1 13,27 * *", yovistoTask);

        if(learnweb.getService().equals(Learnweb.SERVICE.LEARNWEB))
        {
            //Runs the TED crawler at 23:00 once a week to check for new/update TED videos
            scheduler.schedule("0 23 * 1 *", new TedCrawlerSimple());
            //Runs the speech repository crawler at 22:00 once a week to check for new/update of videos
            scheduler.schedule("0 22 * * 1", new SpeechRepositoryCrawlerSimple());
        }

        //Cleans up expired bans once a week on Sunday at 3:00AM
        scheduler.schedule("0 3 * * Sun", new ExpiredBansCleaner());

        //Cleans up requests once an hour
        scheduler.schedule("0 * * * *", new RequestsTaskHandler());

        if(!Learnweb.isInDevelopmentMode() && !learnweb.getService().equals(Learnweb.SERVICE.AMA)) // don't run the fetcher for ama since it has no mail address set up
        {
            //Checks bounced mail every day at 3:00AM
            scheduler.schedule("0 3 * * *", new BounceFetcher());
        }
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

    @SuppressWarnings("unused")
    private class UpdateLoroResources extends Task
    {

        @Override
        public boolean canBePaused()
        {
            return true;
        }

        @Override
        public boolean canBeStopped()
        {
            return true;
        }

        @Override
        public boolean supportsCompletenessTracking()
        {
            return true;
        }

        @Override
        public boolean supportsStatusTracking()
        {
            return true;
        }

        @Override
        public void execute(TaskExecutionContext context)
        {

            try
            {
                learnweb.getLoroManager().saveLoroResource();
            }
            catch(Throwable t)
            {
                log.fatal("Can't update LORO resources to learnweb", t);
            }
        }
    }

    public static void main(String[] args)
    {
        Learnweb learnweb = Learnweb.getInstance();
        JobScheduler job = new JobScheduler(learnweb);
        job.startAllJobs();
        try
        {
            Thread.sleep(2L * 60L * 1000L);
        }
        catch(InterruptedException e)
        {
            ;
        }
        job.stopAllJobs();
    }
}
