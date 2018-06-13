package de.l3s.util;

import java.util.HashMap;

import org.apache.log4j.Logger;

public class DefaultMap<K, V extends Cloneable> extends HashMap<K, V>
{
    private V defaultValue;

    public DefaultMap(V defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    @Override
    public V get(Object key)
    {
        V value = super.get(key);

        if(null == value)
            try
            {
                return (V) defaultValue.getClass().getMethod("clone").invoke(defaultValue);
            }
            catch(Exception e)
            {
                Logger.getLogger(DefaultMap.class).error("can't clone object", e);
                return defaultValue;
            }

        return value;
    }

}
