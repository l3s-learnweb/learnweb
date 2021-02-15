package de.l3s.test;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.jupiter.api.extension.ExtensionContext;

import de.l3s.learnweb.app.ConfigProvider;
import de.l3s.learnweb.app.DaoProvider;
import de.l3s.learnweb.app.Learnweb;

class LearnwebResource implements ExtensionContext.Store.CloseableResource {

    private final Learnweb learnweb;

    LearnwebResource(final JdbcConnectionPool dataSource) {
        ConfigProvider configProvider = new ConfigProvider();
        configProvider.setServerUrl("https://learnweb.l3s.uni-hannover.de");

        DaoProvider daoProvider = new DaoProvider(dataSource);
        learnweb = new Learnweb(configProvider, daoProvider);

        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("db/migration", "db/test")
            .load();
        flyway.migrate();

        learnweb.init();
    }

    Learnweb getLearnweb() {
        return learnweb;
    }

    @Override
    public void close() {
        if (learnweb != null) {
            learnweb.destroy();
        }
    }
}
