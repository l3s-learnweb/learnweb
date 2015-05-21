package de.l3s.learnweb;

import it.sauronsoftware.cron4j.Scheduler;
import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;

import org.apache.log4j.Logger;

public class JobScheduler
{
    private final Logger log = Logger.getLogger(JobScheduler.class);
    private Scheduler scheduler;
    private Learnweb learnweb;

    protected JobScheduler(Learnweb learnweb)
    {
	this.learnweb = learnweb;
	this.scheduler = new Scheduler();

	UpdateTedVideos task = new UpdateTedVideos();
	//Schedules the task, at 1:00 everyday
	//description about Scheduling patterns : http://www.sauronsoftware.it/projects/cron4j/manual.php#p02 
	scheduler.schedule("0 1 * * *", task);

	AddArchiveUrlToResource archiveTask = new AddArchiveUrlToResource();
	//Schedule the archiveTask every minute
	scheduler.schedule("* * * * *", archiveTask);
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

    private class UpdateTedVideos extends Task
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
		learnweb.getTedManager().saveTedResource();
	    }
	    catch(Throwable t)
	    {
		log.fatal("Can't update TED videos", t);
	    }
	}
    }

    private class AddArchiveUrlToResource extends Task
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
	public void execute(TaskExecutionContext context2)
	{
	    //log.debug("archive something");
	    try
	    {
		learnweb.getArchiveUrlManager().addArchiveUrlToResource();
	    }
	    catch(Throwable t)
	    {
		log.fatal("Can't archive certain resource links", t);
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
