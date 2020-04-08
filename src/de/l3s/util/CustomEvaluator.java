package de.l3s.util;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.TriggeringEventEvaluator;

import de.l3s.learnweb.Learnweb;

public class CustomEvaluator implements TriggeringEventEvaluator
{
    private long cycleSeconds = 60000L * 10L; // Sending a group of errors if the last check was more than an 10 minutes ago
    private long lastCheck = 0L;

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
}
