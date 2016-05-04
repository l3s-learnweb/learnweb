package de.l3s.learnweb;

import org.apache.log4j.Logger;

import de.l3s.tedapi.crawler.CheckNewTedVideos;
import de.l3s.tedapi.crawler.CheckUpdatedTedVideos;
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
	UpdateYovistoVideos yovistoTask = new UpdateYovistoVideos();
	scheduler.schedule("0 1 13,27 * *", yovistoTask);

	//Schedules the task, at 1:00 everyday to check for new TED videos
	scheduler.schedule("0 1 * * *", new CheckNewTedVideos());

	//Schedules the task at 1:00 on alternate days to update existing TED videos
	scheduler.schedule("0 1 2-30/2 * *", new CheckUpdatedTedVideos());

	//scheduler.schedule("0 1 * * *", new TedCrawlController());
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

    private class UpdateYovistoVideos extends Task
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
		learnweb.getYovistoManager().saveYovistoResource();
	    }
	    catch(Throwable t)
	    {
		log.fatal("Can't update Yovisto videos to learnweb", t);
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
