package de.l3s.learnweb.beans.publicPages;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import de.l3s.learnweb.Announcement;
import de.l3s.learnweb.AnnouncementDao;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.CourseDao;

@Named
@RequestScoped
public class FrontpageBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 6447676611928379575L;

    private static final int MAX_ANNOUNCEMENTS = 5; // maximal number of announcements that will be shown

    // TODO: add some kind of cache
    private List<Announcement> announcements;
    private List<Course> publicCourses;
    private List<Course> specificCourses;

    @Inject
    private AnnouncementDao announcementDao;
    @Inject
    private CourseDao courseDao;

    /**
     * @return The {@code MAX_TOP_ANNOUNCEMENTS} newest announcements that are not hidden
     */
    public List<Announcement> getAnnouncements() {
        if (announcements == null) {
            announcements = announcementDao.findLastCreated(MAX_ANNOUNCEMENTS);
        }
        return announcements;
    }

    public List<Course> getPublicCourses() {
        if (publicCourses == null) {
            publicCourses = courseDao.findByRegistrationType(Course.RegistrationType.PUBLIC);
        }
        return publicCourses;
    }

    public List<Course> getSpecificCourses() {
        if (specificCourses == null) {
            specificCourses = courseDao.findByRegistrationType(Course.RegistrationType.SPECIFIC);
        }
        return specificCourses;
    }
}
