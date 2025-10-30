package de.l3s.test;

import java.sql.SQLException;
import java.util.TimeZone;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.Server;

import de.l3s.learnweb.app.ConfigProvider;
import de.l3s.learnweb.app.DaoProvider;
import de.l3s.learnweb.app.Learnweb;

class LearnwebResource implements AutoCloseable {
    private static final Logger log = LogManager.getLogger(LearnwebResource.class);

    private static final boolean startDbServer = false;
    private final Learnweb learnweb;

    LearnwebResource(final boolean useRealDatabase) {
        ConfigProvider configProvider = new ConfigProvider(false);
        configProvider.setServerUrl("https://learnweb.l3s.uni-hannover.de", "/");
        configProvider.setProperty("solr_server_url", "");
        configProvider.setProperty("file_manager_folder", FileUtils.getTempDirectory().getAbsolutePath());

        DaoProvider daoProvider;
        if (useRealDatabase) {
            daoProvider = new DaoProvider(configProvider);
        } else {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            String uuid = UUID.randomUUID().toString();
            String args = "DB_CLOSE_DELAY=-1;MODE=MYSQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE";
            DataSource dataSource = JdbcConnectionPool.create("jdbc:h2:mem:" + uuid + ";" + args, "", "");

            daoProvider = new DaoProvider(configProvider, dataSource);

            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("db/migration", "db/test")
                .load();
            flyway.migrate();

            if (startDbServer) {
                try {
                    Server server = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092", "-tcpDaemon").start();
                    log.info("Started embedded H2 server on port 9092");
                    log.info("Connect to the server with: jdbc:h2:{}/mem:{};{}", server.getURL(), uuid, args);
                } catch (SQLException e) {
                    log.error("Error starting embedded H2 server", e);
                }
            }
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
