package de.l3s.learnweb.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.Course.Option;

public class ChangeSettingsOfAllCourses
{
    private static final Logger log = LogManager.getLogger(ChangeSettingsOfAllCourses.class);

    public static void main(String[] args) throws Exception
    {
        Learnweb learnweb = Learnweb.createInstance();

        for(Course course : learnweb.getCourseManager().getCoursesAll())
        {
            log.debug("Update " + course);
            course.setOption(Option.Groups_Forum_categories_enabled, false);
            course.save();
        }

        /*
        for(Organisation organisation : learnweb.getOrganisationManager().getOrganisationsAll())
        {
            log.debug("Update " + organisation);
            organisation.setOption(Organisation.Option.Privacy_Proxy_enabled, false);
            organisation.setOption(Organisation.Option.Privacy_Tracker_disabled, true);
            learnweb.getOrganisationManager().save(organisation);
        }*/

        learnweb.onDestroy();
    }

}
