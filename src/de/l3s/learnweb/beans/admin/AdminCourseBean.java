package de.l3s.learnweb.beans.admin;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.primefaces.event.FileUploadEvent;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.FileDao;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.Course.Option;
import de.l3s.learnweb.user.CourseDao;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.OrganisationDao;
import de.l3s.learnweb.user.User;
import de.l3s.util.Image;

@Named
@ViewScoped
public class AdminCourseBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -1276599881084055950L;
    private static final Logger log = LogManager.getLogger(AdminCourseBean.class);

    private Course course;
    private List<OptionWrapperGroup> optionGroups;
    private int courseId;
    private List<Organisation> organisations;

    @Inject
    private OrganisationDao organisationDao;

    @Inject
    private CourseDao courseDao;
    @Inject
    private FileDao fileDao;

    @PostConstruct
    public void init() {
        organisations = organisationDao.findAll();
        Collections.sort(organisations);
    }

    public void onLoad() {
        User user = getUser();
        BeanAssert.authorized(user);

        course = courseDao.findByIdOrElseThrow(courseId);
        BeanAssert.hasPermission(user.isAdmin() || user.isModerator() && course.isMember(user));

        // many string operations to display the options in a proper way
        optionGroups = new LinkedList<>();
        List<OptionWrapper> options = new LinkedList<>();
        String oldOptionGroupName = null;

        Option[] optionsEnum = Option.values();
        Arrays.sort(optionsEnum, new EnumComparator());

        for (Option option : optionsEnum) {
            // example: this gets "Services" from "Services_Allow_logout_from_Interweb"
            String newOptionGroupName = option.name().substring(0, option.name().indexOf('_'));

            if ("Unused".equals(newOptionGroupName)) {
                continue;
            }

            if (oldOptionGroupName != null && !oldOptionGroupName.equalsIgnoreCase(newOptionGroupName)) {
                optionGroups.add(new OptionWrapperGroup(oldOptionGroupName, options));

                options = new LinkedList<>();
            }

            oldOptionGroupName = newOptionGroupName;
            options.add(new OptionWrapper(option, course.getOption(option)));
        }
        optionGroups.add(new OptionWrapperGroup(oldOptionGroupName, options));
    }

    public void save(ActionEvent actionEvent) {
        for (OptionWrapperGroup group : optionGroups) {
            for (OptionWrapper optionWrapper : group.getOptions()) {
                course.setOption(optionWrapper.getOption(), optionWrapper.getValue());
            }
        }

        courseDao.save(course);
        addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    public List<OptionWrapperGroup> getOptionGroups() {
        return optionGroups;
    }

    public Course getCourse() {
        return course;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    /**
     * Returns a list of all available organisations. For select box to select to which organisation a course belongs
     */
    public List<Organisation> getOrganisations() {
        return organisations;
    }

    public Course.RegistrationType[] getRegistrationTypes() {
        return Course.RegistrationType.values();
    }

    public void handleFileUpload(FileUploadEvent event) {
        try {
            Image img = new Image(event.getFile().getInputStream());

            File file = new File(File.FileType.COURSE_PICTURE, "group_picture.png", "image/png");
            Image thumbnail = img.getResizedToSquare2(120, 0.0);
            fileDao.save(file, thumbnail.getInputStream());
            thumbnail.dispose();

            course.getRegistrationIconFile().ifPresent(image -> fileDao.deleteHard(image)); // delete old image
            course.setRegistrationIconFileId(file.getId());
            courseDao.save(course);
        } catch (Exception e) {
            log.error("Fatal error while processing a user image", e);
            addMessage(FacesMessage.SEVERITY_FATAL, "Fatal error while processing your image.");
        }
    }

    // only helper classes to display the options
    public static class OptionWrapper implements Serializable {
        @Serial
        private static final long serialVersionUID = 2828959818690832148L;
        private final Option option;
        private boolean value;

        public OptionWrapper(Option option, boolean value) {
            this.option = option;
            this.value = value;
        }

        public String getName() {
            return option.name().substring(option.name().indexOf('_')).replace("_", " ");
        }

        public boolean getValue() {
            return value;
        }

        public void setValue(boolean value) {
            this.value = value;
        }

        public Option getOption() {
            return option;
        }
    }

    public static class OptionWrapperGroup implements Serializable {
        @Serial
        private static final long serialVersionUID = -2323320446956640229L;
        private final String title;
        private final List<OptionWrapper> options;

        public OptionWrapperGroup(String title, List<OptionWrapper> options) {
            this.title = title;
            this.options = options;
        }

        public String getTitle() {
            return title;
        }

        public List<OptionWrapper> getOptions() {
            return options;
        }
    }

    private static class EnumComparator implements Comparator<Option>, Serializable {
        @Serial
        private static final long serialVersionUID = -6590111487348788376L;

        @Override
        public int compare(Option o1, Option o2) {
            return o1.name().compareTo(o2.name());
        }
    }
}
