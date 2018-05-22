package de.l3s.learnweb;

import org.apache.log4j.Logger;

import de.l3s.learnweb.user.loginProtection.ExpiredBansCleaner;
import de.l3s.learnweb.web.RequestsTaskHandler;
import de.l3s.ted.crawler.TedCrawlerSimple;
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
        UpdateLoroResources loroTask = new UpdateLoroResources();
        scheduler.schedule("0 1 14,28 * *", loroTask);

        //Schedules the task at 1:00 on 13th and 27th of every month
        //UpdateYovistoVideos yovistoTask = new UpdateYovistoVideos();
        //scheduler.schedule("0 1 13,27 * *", yovistoTask);

        //Schedules the task, at 1:00 everyday to check for new TED videos
        //scheduler.schedule("0 1 * * *", new CheckNewTedVideos());

        //Schedules the task at 1:00 on alternate days to update existing TED videos
        //scheduler.schedule("0 1 2-30/2 * *", new CheckUpdatedTedVideos());

        //Runs the TED crawler at 1:00 everyday to check for new/update TED videos
        scheduler.schedule("0 1 * * *", new TedCrawlerSimple());

        //Cleans up expired bans once a week on Sunday at 3:00AM
        scheduler.schedule("0 3 * * Sun", new ExpiredBansCleaner());

        //Cleans up requests once an hour
        scheduler.schedule("0 * * * *", new RequestsTaskHandler());
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
