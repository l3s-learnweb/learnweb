package de.l3s.learnweb.group;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.l3s.learnweb.logging.LogEntry;
import de.l3s.learnweb.resource.Resource;

public class SummaryOverview implements Serializable {
    @Serial
    private static final long serialVersionUID = 2180957883352434028L;

    private final LinkedList<LogEntry> addedResources = new LinkedList<>();
    private final LinkedList<LogEntry> deletedResources = new LinkedList<>();
    private final LinkedList<LogEntry> forumsInfo = new LinkedList<>();
    private final LinkedList<LogEntry> membersInfo = new LinkedList<>();
    private final HashMap<Resource, List<LogEntry>> updatedResources = new HashMap<>();

    public List<LogEntry> getAddedResources() {
        return addedResources;
    }

    public List<LogEntry> getDeletedResources() {
        return deletedResources;
    }

    public List<LogEntry> getForumsInfo() {
        return forumsInfo;
    }

    public List<LogEntry> getMembersInfo() {
        return membersInfo;
    }

    public boolean isEmpty() {
        return addedResources.isEmpty() && deletedResources.isEmpty() && forumsInfo.isEmpty() && membersInfo.isEmpty() && getUpdatedResources().isEmpty();
    }

    public Map<Resource, List<LogEntry>> getUpdatedResources() {
        return updatedResources;
    }

}
