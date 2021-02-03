package de.l3s.maintenance;

import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;

@SuppressWarnings({"CallToSystemExit", "ProhibitedExceptionDeclared"})
public abstract class MaintenanceTask {
    protected final Logger log = LogManager.getLogger(getClass());

    private final Learnweb learnweb;

    /**
     * If set to true, then will run in dry-run mode (dryRun=true) when no argument given, and no dry-run (dryRun=false) when `--confirm` argument given.
     */
    protected boolean requireConfirmation = false;

    protected MaintenanceTask() {
        try {
            learnweb = Learnweb.createInstance("https://learnweb.l3s.uni-hannover.de");
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException("Unable to create Learnweb", e);
        }
    }

    protected final Learnweb getLearnweb() {
        return learnweb;
    }

    @SuppressWarnings("NoopMethodInAbstractClass")
    protected void init() throws Exception {
        // can be overridden to init managers
    }

    protected abstract void run(boolean dryRun) throws Exception;

    public final void start(String[] args) {
        try {
            init();

            if (!requireConfirmation || StringUtils.equalsAnyIgnoreCase("--confirm", args)) {
                run(false);
            } else {
                log.warn("You are running the command in \"Dry run\" mode (without actual changes)!");
                run(true);
                log.warn("To perform the changes, re-run the command with \"--confirm\" argument.");
            }
        } catch (Exception e) {
            log.error("An unhandled error occurred", e);
            System.exit(-1);
        } finally {
            learnweb.onDestroy();
        }
    }
}
