package de.l3s.test;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.h2.jdbcx.JdbcConnectionPool;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import de.l3s.learnweb.app.Learnweb;

public final class LearnwebExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(LearnwebExtension.class);
    private static final ReentrantLock lock = new ReentrantLock();

    private Handle handle;
    private LearnwebResource resource;

    private static LearnwebResource getResource(ExtensionContext context) {
        ExtensionContext.Store store = context.getRoot().getStore(NAMESPACE);
        LearnwebResource resource = store.get("res", LearnwebResource.class);

        if (resource == null) {
            lock.lock();
            try {
                resource = store.getOrComputeIfAbsent("res", s -> createResource(), LearnwebResource.class);
            } finally {
                lock.unlock();
            }
        }
        return resource;
    }

    private static LearnwebResource createResource() {
        JdbcConnectionPool dataSource = JdbcConnectionPool.create("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=MYSQL", "", "");
        return new LearnwebResource(dataSource);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        resource = getResource(context);

        handle = resource.getLearnweb().openJdbiHandle();
        handle.begin();
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        handle.savepoint("beforeEach");
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        handle.rollback();
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        handle.close();
    }

    public Learnweb getLearnweb() {
        return resource.getLearnweb();
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
