package de.l3s.maintenance.organisations;

import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.Course.Option;
import de.l3s.learnweb.user.CourseDao;
import de.l3s.maintenance.MaintenanceTask;

public class CourseSetOption extends MaintenanceTask {

    @Override
    protected void init() {
        requireConfirmation = true;
    }

    @Override
    protected void run(final boolean dryRun) {
        final CourseDao courseDao = getLearnweb().getDaoProvider().getCourseDao();

        if (!dryRun) {
            for (Course course : courseDao.findAll()) {
                log.debug("Updating {}", course);
                course.setOption(Option.Groups_Forum_categories_enabled, false);
                courseDao.save(course);
            }
        }
    }

    public static void main(String[] args) {
        new CourseSetOption().start(args);
    }
}
