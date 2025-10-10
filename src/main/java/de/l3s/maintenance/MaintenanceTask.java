package de.l3s.maintenance;

import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.app.Learnweb;

@SuppressWarnings({"CallToSystemExit", "ProhibitedExceptionDeclared"})
public abstract class MaintenanceTask {
    protected final Logger log = LogManager.getLogger(getClass());

    private final Learnweb learnweb;

    /**
     * If set to true, then will run in dry-run mode (dryRun=true) when no argument given, and no dry-run (dryRun=false) when `--confirm` argument given.
     */
    protected boolean requireConfirmation = false;

    protected MaintenanceTask() {
        learnweb = Learnweb.createStatic();
        learnweb.init();
    }

    protected MaintenanceTask(Learnweb learnweb) {
        this.learnweb = learnweb;
    }

    protected final Learnweb getLearnweb() {
        return learnweb;
    }

    @SuppressWarnings("NoopMethodInAbstractClass")
    protected void init() {
        // can be overridden to init managers
    }

    protected abstract void run(boolean dryRun) throws Exception;

    public final void start(String[] args) {
        try {
            init();

            if (!requireConfirmation || Strings.CI.equalsAny("--confirm", args)) {
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
            learnweb.destroy();
        }
    }
}
