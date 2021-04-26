package de.l3s.test;

import java.util.TimeZone;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.jupiter.api.extension.ExtensionContext;

import de.l3s.learnweb.app.ConfigProvider;
import de.l3s.learnweb.app.DaoProvider;
import de.l3s.learnweb.app.Learnweb;

class LearnwebResource implements ExtensionContext.Store.CloseableResource {

    private final Learnweb learnweb;

    LearnwebResource(final boolean useRealDatabase) {
        ConfigProvider configProvider = new ConfigProvider(false);
        configProvider.setServerUrl("https://learnweb.l3s.uni-hannover.de");
        configProvider.setProperty("solr_server_url", "");
        configProvider.setProperty("file_manager_folder", FileUtils.getTempDirectory().getAbsolutePath());

        DaoProvider daoProvider;
        if (useRealDatabase) {
            daoProvider = new DaoProvider(configProvider);
        } else {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            DataSource dataSource = JdbcConnectionPool.create("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=MYSQL", "", "");

            daoProvider = new DaoProvider(configProvider, dataSource);

            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("db/migration", "db/test")
                .load();
            flyway.migrate();
        }

        learnweb = new Learnweb(configProvider, daoProvider);
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
