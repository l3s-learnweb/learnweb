package de.l3s.learnweb.solrClient;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.ResourceDecorator;

public class SolrCache
{
    private static final Logger log = Logger.getLogger(SolrCache.class);
    private static SolrCache instance = null;
    private SolrClient solrClient;
    private final BlockingQueue<ResourceDecorator> cacheQueue;
    private final ExecutorService service;

    private SolrCache()
    {
	solrClient = Learnweb.getInstance().getSolrClient();
	cacheQueue = new LinkedBlockingQueue<ResourceDecorator>();
	service = Executors.newSingleThreadExecutor();
	service.submit(new Indexer(cacheQueue, solrClient));
    }

    public static SolrCache getInstance()
    {
	if(null == instance)
	    instance = new SolrCache();

	return instance;
    }

    /**
     * The resources are added to an internal queue (the queue is thread safe).
     * 
     * A separate thread retrieves the resources from the queue and indexes them.
     * 
     */
    public void cacheResources(List<ResourceDecorator> resources)
    {
	if(resources.size() == 0)
	    return;

	log.debug("Added " + resources.size() + " to cache queue");

	for(ResourceDecorator decoratedResource : resources)
	{
	    try
	    {
		cacheQueue.put(decoratedResource);

		//log.debug("Debug: add Resource to cache Queue: " + decoratedResource.getTitle());
	    }
	    catch(InterruptedException e)
	    {
		e.printStackTrace();
		Thread.currentThread().interrupt();
	    }
	}
    }

    private static class Indexer implements Runnable
    {
	private final BlockingQueue<ResourceDecorator> workQueue;
	private SolrClient client;
	private final int maxResourcesIndexedOneTime = 1000;

	public Indexer(BlockingQueue<ResourceDecorator> workQueue, SolrClient solrClient)
	{
	    this.workQueue = workQueue;
	    this.client = solrClient;
	}

	@Override
	public void run()
	{
	    while(!Thread.currentThread().isInterrupted())
	    {
		try
		{
		    log.debug("wait");
		    List<ResourceDecorator> workList = new LinkedList<ResourceDecorator>();
		    workList.add(workQueue.take());
		    workQueue.drainTo(workList, maxResourcesIndexedOneTime - 1);
		    client.indexDecoratedResources(workList);
		}
		catch(InterruptedException ex)
		{
		    ex.printStackTrace();

		    Thread.currentThread().interrupt();
		    break;
		}
	    }
	}
    }
}
