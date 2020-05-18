package de.l3s.learnweb.resource;

import java.sql.SQLException;
import java.util.List;

public interface ResourceContainer {
    List<Folder> getSubFolders() throws SQLException;
}
