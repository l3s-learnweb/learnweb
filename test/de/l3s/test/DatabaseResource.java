package de.l3s.test;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.extension.ExtensionContext;

class DatabaseResource implements ExtensionContext.Store.CloseableResource {

    private final JdbcConnectionPool dataSource;
    private final Flyway flyway;
    private final Jdbi jdbi;

    DatabaseResource(final JdbcConnectionPool dataSource, Flyway flyway, Jdbi jdbi) {
        this.dataSource = dataSource;
        this.flyway = flyway;
        this.jdbi = jdbi;
    }

    @Override
    public void close() throws Throwable {
        if (flyway != null) {
            flyway.clean();
        }

        if (dataSource != null) {
            dataSource.dispose();
        }
    }

    public Jdbi getJdbi() {
        return jdbi;
    }

    public Flyway getFlyway() {
        return flyway;
    }
}
