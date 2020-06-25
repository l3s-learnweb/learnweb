package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.exceptions.BeanAsserts;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.Course.Option;
import de.l3s.learnweb.user.Organisation;

@Named
@ViewScoped
public class AdminCourseBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -1276599881084055950L;
    private static final Logger log = LogManager.getLogger(AdminCourseBean.class);

    private Course course;
    private List<OptionWrapperGroup> optionGroups;
    private int courseId;
    private final List<Organisation> organisations;

    public AdminCourseBean() {
        organisations = new ArrayList<>(getLearnweb().getOrganisationManager().getOrganisationsAll());
        Collections.sort(organisations);
    }

    public void onLoad() {
        BeanAsserts.authorized(isLoggedIn());
        BeanAsserts.hasPermission(getUser().isAdmin());

        course = getLearnweb().getCourseManager().getCourseById(courseId);
        BeanAsserts.validateNotNull(course);

        // many string operations to display the options in a proper way
        optionGroups = new LinkedList<>();
        List<OptionWrapper> options = new LinkedList<>();
        String oldOptionGroupName = null;

        Option[] optionsEnum = Option.values();

        EnumComparator c = new EnumComparator();
        java.util.Arrays.sort(optionsEnum, c);

        for (Option option : optionsEnum) {
            // example: this gets "Services" from "Services_Allow_logout_from_Interweb"
            String newOptionGroupName = option.name().substring(0, option.name().indexOf('_'));

            if (newOptionGroupName.equals("Unused")) {
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
        try {
            course.save();
            addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
        } catch (Exception e) {
            log.error("unhandled error", e);
            addErrorGrowl(e);
        }
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

    // only helper classes to display the options
    public static class OptionWrapper implements Serializable {
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
        private static final long serialVersionUID = -6590111487348788376L;

        @Override
        public int compare(Option o1, Option o2) {
            return o1.name().compareTo(o2.name());
        }
    }
}
