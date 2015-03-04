package de.l3s.util;

import java.util.Properties;

public class PropertiesBundle extends Properties
{
    private static final long serialVersionUID = 6497606322559238165L;
    private Properties fallback;

    /**
     * 
     * @param fallback
     */
    public PropertiesBundle(Properties fallback)
    {
	this.fallback = fallback;
    }

    @Override
    public String getProperty(String key)
    {
	String value = super.getProperty(key);

	if(value != null)
	    return value;

	return fallback.getProperty(key);
    }
}
