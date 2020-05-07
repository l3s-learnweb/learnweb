package de.l3s.util;

import java.util.Objects;
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

    public int getPropertyIntValue(String key)
    {
        return Integer.parseInt(getProperty(key));
    }

    @Override
    public boolean equals(final Object o)
    {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        if(!super.equals(o)) return false;
        final PropertiesBundle that = (PropertiesBundle) o;
        return Objects.equals(fallback, that.fallback);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), fallback);
    }
}
