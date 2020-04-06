package de.l3s.util;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.TriggeringEventEvaluator;

import de.l3s.learnweb.Learnweb;

/**
 *
 * Used by Log4j to send mails in batches.
 *
 */
public class CustomEvaluator implements TriggeringEventEvaluator
{
    private long cycleSeconds = 3600000L; // Sending a group of errors if the last check was more than an hour ago
    private long lastCheck = 0L;

    @Override
    public boolean isTriggeringEvent(LoggingEvent event)
    {
        if(Learnweb.isDevelopmentStage())
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
