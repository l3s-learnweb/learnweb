package de.l3s.learnweb.web;

import java.io.Serializable;
import java.time.Instant;
import java.util.Optional;

import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface BounceDao extends SqlObject, Serializable {

    @SqlQuery("SELECT MAX(timereceived) FROM lw_bounces")
    Optional<Instant> findLastBounceDate();

    @SqlUpdate("INSERT INTO lw_bounces (address, timereceived, code, description) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE timereceived = VALUES(timereceived), code = VALUES(code), description = VALUES(description)")
    void save(String originalRecipient, Instant date, String code, String description);

}
