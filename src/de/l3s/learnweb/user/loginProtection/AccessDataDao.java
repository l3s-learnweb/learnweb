package de.l3s.learnweb.user.loginProtection;

import java.util.List;

import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface AccessDataDao extends SqlObject {
    @SqlQuery("SELECT * FROM lw_bans")
    @RegisterBeanMapper(AccessData.class)
    List<AccessData> findAll();

    @SqlUpdate("INSERT INTO lw_bans (type, name, bandate, bannedon, attempts, reason) VALUES(:type, :name, :bannedUntil, :bannedOn, :attempts, :reason) ON DUPLICATE KEY UPDATE bandate = VALUES(bandate)")
    void create(@BindBean AccessData accessData);

    @SqlUpdate("DELETE FROM lw_bans WHERE name = ?")
    void delete(String name);

    @SqlUpdate("DELETE FROM lw_bans WHERE bandate <= CURDATE() - INTERVAL 7 DAY")
    void deleteOutdated();
}
