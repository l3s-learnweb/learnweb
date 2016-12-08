package de.l3s.learnweb;

import java.sql.SQLException;

public interface GroupItem
{
    int getId();

    void setId(int id);

    String getTitle();

    void setTitle(String title);

    int getGroupId();

    void setGroupId(int groupId);

    Group getGroup() throws SQLException;

    int getUserId();

    void setUserId(int userId);

    User getUser() throws SQLException;

    void setUser(User user);

    GroupItem save() throws SQLException;

    void delete() throws SQLException;

    String getPath() throws SQLException;

    String getPrettyPath() throws SQLException;
}
