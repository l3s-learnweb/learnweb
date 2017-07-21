package de.l3s.util;

public class Misc
{
    public static void sleep(long millis)
    {
	try
	{
	    Thread.sleep(millis);
	}
	catch(InterruptedException e)
	{
	}
    }
}
