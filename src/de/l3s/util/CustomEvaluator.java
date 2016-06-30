package de.l3s.util;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.TriggeringEventEvaluator;

public class CustomEvaluator implements TriggeringEventEvaluator{

	private int cycleSeconds = 360000;  	// Sending a group of errors if the last check was more than an hour ago
	private long lastCheck;
	
	public boolean isTriggeringEvent(LoggingEvent event) { 
		long now = System.currentTimeMillis();
		if(now - lastCheck < cycleSeconds){
			return false;
		}
		else
		{
			lastCheck = now;
			return true;
		}
	}

}
