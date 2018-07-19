package de.l3s.util;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.TriggeringEventEvaluator;

import de.l3s.learnweb.Learnweb;

public class CustomEvaluator implements TriggeringEventEvaluator
{

    private long cycleSeconds = 3600000L; // Sending a group of errors if the last check was more than an hour ago
    private long lastCheck = 0L;
    //private Boolean isRunningOnLocalhost = null;

    @Override
    public boolean isTriggeringEvent(LoggingEvent event)
    {
        if(Learnweb.isInDevelopmentMode())
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

    /*
    private boolean isRunningOnLocalhost()
    {
        if(isRunningOnLocalhost == null) // called the first time
        {
            lastCheck = System.currentTimeMillis();
            try
            {
                isRunningOnLocalhost = (Learnweb.getPropertiesFileName() != "learnweb");
    
                if(isRunningOnLocalhost)
                    Logger.getLogger(CustomEvaluator.class).info("Learnweb is running on Localhost, email logger disabled");
    
                return true; // true, to skip first error message
            }
            catch(Exception e)
            {
                isRunningOnLocalhost = true;
            }
        }
    
        return isRunningOnLocalhost;
    }*/

}
