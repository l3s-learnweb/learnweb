package de.l3s.test;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

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

    private final boolean useRealDatabase;

    private Handle handle;
    private LearnwebResource resource;
    private String savepointName;

    public LearnwebExtension() {
        this(false);
    }

    public LearnwebExtension(boolean useRealDatabase) {
        this.useRealDatabase = useRealDatabase;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        resource = getResource(context, useRealDatabase);

        handle = resource.getLearnweb().openJdbiHandle();
        handle.begin();
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        savepointName = UUID.randomUUID().toString();
        handle.savepoint(savepointName);
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        handle.rollbackToSavepoint(savepointName);
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        handle.rollback();
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

    private static LearnwebResource getResource(ExtensionContext context, boolean useRealDatabase) {
        ExtensionContext.Store store = context.getRoot().getStore(NAMESPACE);
        LearnwebResource resource = store.get("res", LearnwebResource.class);

        if (resource == null) {
            lock.lock();
            try {
                resource = store.getOrComputeIfAbsent("res", s -> new LearnwebResource(useRealDatabase), LearnwebResource.class);
            } finally {
                lock.unlock();
            }
        }
        return resource;
    }
}
