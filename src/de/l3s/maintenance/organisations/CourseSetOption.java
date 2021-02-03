package de.l3s.maintenance.organisations;

import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.Course.Option;
import de.l3s.maintenance.MaintenanceTask;

public class CourseSetOption extends MaintenanceTask {

    @Override
    protected void init() throws Exception {
        requireConfirmation = true;
    }

    @Override
    protected void run(final boolean dryRun) throws Exception {
        if (!dryRun) {
            for (Course course : getLearnweb().getCourseManager().getCoursesAll()) {
                log.debug("Updating {}", course);
                course.setOption(Option.Groups_Forum_categories_enabled, false);
                course.save();
            }
        }
    }

    public static void main(String[] args) {
        new CourseSetOption().start(args);
    }
}
