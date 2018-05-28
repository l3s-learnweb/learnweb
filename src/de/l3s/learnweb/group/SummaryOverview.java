package de.l3s.learnweb.group;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.resource.Resource;

public class SummaryOverview
{
    private final List<LogEntry> addedResources = new LinkedList<>();
    private final List<LogEntry> deletedResources = new LinkedList<>();
    private final List<LogEntry> forumsInfo = new LinkedList<>();
    private final List<LogEntry> membersInfo = new LinkedList<>();
    private final Map<Resource, List<LogEntry>> updatedResources = new HashMap<>();

    public List<LogEntry> getAddedResources()
    {
        return addedResources;
    }

    public List<LogEntry> getDeletedResources()
    {
        return deletedResources;
    }

    public List<LogEntry> getForumsInfo()
    {
        return forumsInfo;
    }

    public List<LogEntry> getMembersInfo()
    {
        return membersInfo;
    }

    public boolean isEmpty()
    {
        return addedResources.isEmpty() && deletedResources.isEmpty() && forumsInfo.isEmpty() && membersInfo.isEmpty() && getUpdatedResources().isEmpty();
    }

    public Map<Resource, List<LogEntry>> getUpdatedResources()
    {
        return updatedResources;
    }

}
