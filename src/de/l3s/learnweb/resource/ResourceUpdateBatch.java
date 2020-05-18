package de.l3s.learnweb.resource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.GroupManager;

public class ResourceUpdateBatch {
    private static final Logger log = LogManager.getLogger(ResourceUpdateBatch.class);

    private final List<Resource> resources;
    private final List<Folder> folders;
    private int failed = 0;

    private int totalSize = 0;
    private int totalFailed = 0;

    public ResourceUpdateBatch(List<Resource> resources, List<Folder> folders) {
        this.resources = new ArrayList<>();
        this.folders = new ArrayList<>();

        if (resources != null && !resources.isEmpty()) {
            this.resources.addAll(resources);
        }
        if (folders != null && !folders.isEmpty()) {
            this.folders.addAll(folders);
        }
    }

    public ResourceUpdateBatch(String json) throws SQLException {
        resources = new ArrayList<>();
        folders = new ArrayList<>();

        GroupManager groupManager = Learnweb.getInstance().getGroupManager();
        ResourceManager resourceManager = Learnweb.getInstance().getResourceManager();

        JSONArray items = new JSONArray(json);
        for (int i = 0, len = items.length(); i < len; ++i) {
            JSONObject object = items.getJSONObject(i);
            String itemType = object.getString("itemType");
            int itemId = object.getInt("itemId");

            if ("resource".equals(itemType)) {
                Resource resource = resourceManager.getResource(itemId);
                if (resource != null) {
                    resources.add(resource);
                } else {
                    log.error("Can't find resource requested in update!");
                    failed++;
                }
            } else if ("folder".equals(itemType)) {
                Folder folder = groupManager.getFolder(itemId);
                if (folder != null) {
                    folders.add(folder);
                } else {
                    log.error("Can't find folder requested in update!");
                    failed++;
                }
            } else {
                log.error("Unsupported itemType: " + itemType);
                failed++;
            }
        }
    }

    public List<Resource> getResources() {
        return resources;
    }

    public List<Folder> getFolders() {
        return folders;
    }

    public int size() {
        return resources.size() + folders.size();
    }

    public int failed() {
        return failed;
    }

    public int getTotalSize() {
        return totalSize + this.size();
    }

    public void addTotalSize(final int totalSize) {
        this.totalSize += totalSize;
    }

    public int getTotalFailed() {
        return totalFailed + failed;
    }

    public void addTotalFailed(final int totalFailed) {
        this.totalFailed += totalFailed;
    }
}
