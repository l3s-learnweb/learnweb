package de.l3s.util;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.TriggeringEventEvaluator;

import de.l3s.learnweb.Learnweb;

public class CustomEvaluator implements TriggeringEventEvaluator
{
    private long cycleSeconds = 360000L; // Sending a group of errors if the last check was more than an hour ago
    private long lastCheck = 0L;
    private Boolean isRunningOnLocalhost = null;

    @Override
    public boolean isTriggeringEvent(LoggingEvent event)
    {
	if(isRunningOnLocalhost())
	    return false;

	long now = System.currentTimeMillis();
	if(now - lastCheck < cycleSeconds)
	{
	    return false;
	}
	else
	{
	    lastCheck = now;
	    return true;
	}
    }

    private boolean isRunningOnLocalhost()
    {
	if(isRunningOnLocalhost == null) // called the first time
	{
	    lastCheck = System.currentTimeMillis();

	    String contextUrl = Learnweb.getInstance().getContextUrl();

	    if(contextUrl.startsWith("http://localhost") || contextUrl.startsWith("http://learnweb.dev"))
		isRunningOnLocalhost = true;

	    return true; // true, to skip first error message
	}

	return isRunningOnLocalhost;
    }

}
