package de.l3s.learnweb.resource;

import java.sql.SQLException;
import java.util.List;

public interface IResourceContainer
{
    public List<Folder> getSubFolders() throws SQLException;
}
