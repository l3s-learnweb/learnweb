package de.l3s.learnweb.tasks;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.Course.Option;
import de.l3s.learnweb.user.Organisation;

public class ChangeSettingsOfAllCourses
{
    private final static Logger log = Logger.getLogger(ChangeSettingsOfAllCourses.class);

    public static void main(String[] args) throws Exception
    {
        Learnweb learnweb = Learnweb.createInstance(null);

        for(Course course : learnweb.getCourseManager().getCoursesAll())
        {
            log.debug("Update " + course);
            course.setOption(Option.Users_Require_mail_address, true);
            learnweb.getCourseManager().save(course);
        }

        for(Organisation organisation : learnweb.getOrganisationManager().getOrganisationsAll())
        {
            log.debug("Update " + organisation);
            organisation.setOption(Organisation.Option.Privacy_Proxy_enabled, false);
            organisation.setOption(Organisation.Option.Privacy_Tracker_disabled, true);
            learnweb.getOrganisationManager().save(organisation);
        }

        learnweb.onDestroy();
    }

}
