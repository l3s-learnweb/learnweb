package de.l3s.maintenance.resources;

import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogDao;
import de.l3s.maintenance.MaintenanceTask;

/**
 * Updates the lw_user_log_action table.
 */
public class RefillLogAction extends MaintenanceTask {

    @Override
    protected void init() {
        requireConfirmation = true;
    }

    @Override
    protected void run(final boolean dryRun) {
        LogDao logDao = getLearnweb().getDaoProvider().getLogDao();

        logDao.truncateUserLogAction();
        logDao.insertUserLogAction(Action.values());
    }

    public static void main(String[] args) {
        new RefillLogAction().start(args);
    }
}
