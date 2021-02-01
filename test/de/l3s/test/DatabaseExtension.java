package de.l3s.test;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DatabaseExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(DatabaseExtension.class);

    private static final ReentrantLock lock = new ReentrantLock();
    private static volatile JdbcConnectionPool dataSource = null;

    private Handle handle;
    private DatabaseResource resource;

    private static JdbcConnectionPool getDataSource() {
        if (dataSource == null) {
            lock.lock();
            try {
                if (dataSource == null) {
                    dataSource = JdbcConnectionPool.create("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=MYSQL", "", "");
                }
            } finally {
                lock.unlock();
            }
        }
        return dataSource;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        resource = context.getRoot().getStore(NAMESPACE).getOrComputeIfAbsent("res", s -> {
            JdbcConnectionPool dataSource = getDataSource();

            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("db/migration") // "db/test"
                .load();
            flyway.migrate();

            Jdbi jdbi = Jdbi.create(dataSource);
            jdbi.installPlugins();

            return new DatabaseResource(dataSource, flyway, jdbi);
        }, DatabaseResource.class);

        handle = resource.getJdbi().open();
        handle.begin();
    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        handle.close();
    }

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        handle.savepoint("beforeEach");
    }

    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
        handle.rollback();
    }

    /**
     * Get Jdbi, in case you want to open additional handles to the same data source.
     */
    public Jdbi getJdbi() {
        return resource.getJdbi();
    }

    /**
     * Get the single Handle instance opened for the duration of this test case.
     */
    public Handle getHandle() {
        return handle;
    }

    /**
     * Attach an extension (such as a SqlObject) to the managed handle.
     */
    public <T> T attach(final Class<T> extension) {
        return handle.attach(extension);
    }
}
