package de.l3s.learnweb.resource;

import java.util.List;

@FunctionalInterface
public interface ResourceContainer {
    List<Folder> getSubFolders();
}
