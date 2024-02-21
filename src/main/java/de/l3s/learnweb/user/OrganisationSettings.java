package de.l3s.learnweb.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OrganisationSettings implements Serializable {
    @Serial
    private static final long serialVersionUID = -22791512601303567L;
    private static final Logger log = LogManager.getLogger(OrganisationSettings.class);

    private final EnumMap<Settings, Object> settings = new EnumMap<>(Settings.class);

    public Set<Map.Entry<Settings, Object>> getValues() {
        return settings.entrySet();
    }

    public void setValue(Settings key, Object value) {
        settings.put(key, value);
    }

    public String getValue(Settings key) {
        if (key.getType() == String.class) {
            return (String) settings.getOrDefault(key, key.getDefaultValue());
        }

        log.error("Requested value for key {} is not a String", key);
        return null;
    }

    public Boolean getBoolValue(Settings key) {
        if (key.getType() == Boolean.class) {
            return (Boolean) settings.getOrDefault(key, key.getDefaultValue());
        }

        log.error("Requested value for key {} is not a Boolean", key);
        return false;
    }

    public Integer getIntValue(Settings key) {
        if (key.getType() == Integer.class) {
            return (Integer) settings.getOrDefault(key, key.getDefaultValue());
        }

        log.error("Requested value for key {} is not a Integer", key);
        return null;
    }
}
