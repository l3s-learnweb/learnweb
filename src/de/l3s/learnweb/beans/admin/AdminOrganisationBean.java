package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.primefaces.event.FileUploadEvent;

import de.l3s.learnweb.LanguageBundle;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.File.FileType;
import de.l3s.learnweb.resource.FileDao;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.Organisation.Option;
import de.l3s.learnweb.user.OrganisationDao;
import de.l3s.util.Image;

@Named
@ViewScoped
public class AdminOrganisationBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -4815509777068373043L;
    private static final Logger log = LogManager.getLogger(AdminOrganisationBean.class);

    private int organisationId;
    private Organisation organisation;
    private LinkedList<OptionWrapperGroup> optionGroups;
    private List<SelectItem> availableGlossaryLanguages;

    @Inject
    private FileDao fileDao;

    @Inject
    private OrganisationDao organisationDao;

    public void onLoad() {
        BeanAssert.authorized(isLoggedIn());

        if (organisationId != 0) {
            BeanAssert.hasPermission(getUser().isAdmin());

            setOrganisation(organisationDao.findByIdOrElseThrow(organisationId));
        } else {
            BeanAssert.hasPermission(getUser().isModerator());

            setOrganisation(getUser().getOrganisation()); // by default edit the users organisation
        }
    }

    public void handleFileUpload(FileUploadEvent event) {
        try {
            Image img = new Image(event.getFile().getInputStream());

            File file = new File(FileType.ORGANISATION_BANNER, "organization_banner.png", "image/png");
            Image thumbnail = img.getResized(324, 100);
            fileDao.save(file, thumbnail.getInputStream());
            thumbnail.dispose();

            organisation.getBannerImageFile().ifPresent(image -> fileDao.deleteHard(image)); // delete old image first
            organisation.setBannerImageFileId(file.getId());
            organisationDao.save(organisation);
        } catch (Exception e) {
            log.error("Could not handle uploaded banner image", e);
            addGrowl(FacesMessage.SEVERITY_FATAL, "Could not store file");
        }
    }

    public void removeBannerImage() {
        organisation.getBannerImageFile().ifPresent(image -> fileDao.deleteHard(image)); // delete old image first

        organisation.setBannerImageFileId(0);
        organisationDao.save(organisation);
    }

    /**
     * @return list of supported languages (codes) of this Learnweb instance
     */
    public List<String> getSupportedLanguages() {
        List<Locale> locales = LanguageBundle.getSupportedLocales();

        return locales.stream().map(Locale::getLanguage).distinct().collect(Collectors.toList());
    }

    /**
     * @return list of supported locale variants. At the time of writing: Archive
     */
    public List<String> getSupportedLanguageVariants() {
        List<Locale> locales = LanguageBundle.getSupportedLocales();

        return locales.stream().map(Locale::getVariant).filter(StringUtils::isNotEmpty).distinct().collect(Collectors.toList());
    }

    public void onSave() {
        for (OptionWrapperGroup group : optionGroups) {
            for (OptionWrapper optionWrapper : group.getOptions()) {
                organisation.setOption(optionWrapper.getOption(), optionWrapper.getValue());
            }
        }

        organisationDao.save(organisation);
        addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    public Organisation getSelectedOrganisation() {
        return organisation;
    }

    public List<OptionWrapperGroup> getOptionGroups() {
        return optionGroups;
    }

    private void setOrganisation(Organisation selectedOrganisation) {
        this.organisation = selectedOrganisation;

        // many string operations to display the options in a proper way
        optionGroups = new LinkedList<>();
        List<OptionWrapper> options = new LinkedList<>();
        String oldOptionGroupName = null;

        Option[] optionsEnum = Option.values();
        Arrays.sort(optionsEnum, new EnumComparator());

        for (Option option : optionsEnum) {
            // example: this gets "Services" from "Services_Allow_logout_from_Interweb"
            String newOptionGroupName = option.name().substring(0, option.name().indexOf('_'));

            if (oldOptionGroupName != null && !oldOptionGroupName.equalsIgnoreCase(newOptionGroupName)) {
                optionGroups.add(new OptionWrapperGroup(oldOptionGroupName, options));

                options = new LinkedList<>();
            }

            oldOptionGroupName = newOptionGroupName;
            options.add(new OptionWrapper(option, selectedOrganisation.getOption(option)));
        }
        optionGroups.add(new OptionWrapperGroup(oldOptionGroupName, options));
    }

    public List<SelectItem> getAvailableGlossaryLanguages() {
        if (null == availableGlossaryLanguages) {
            List<Locale> glossaryLanguages = Arrays.asList(new Locale("ar"), new Locale("de"), new Locale("el"),
                new Locale("en"), new Locale("es"), new Locale("fr"), new Locale("it"), new Locale("nl"),
                new Locale("pt"), new Locale("ru"), new Locale("sv"), new Locale("zh"));
            availableGlossaryLanguages = localesToSelectItems(glossaryLanguages);
        }
        return availableGlossaryLanguages;
    }

    public int getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(int organisationId) {
        this.organisationId = organisationId;
    }

    // only helper classes to display the options
    public static class OptionWrapper implements Serializable {
        private static final long serialVersionUID = 4028764135842666696L;
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
        private static final long serialVersionUID = -7136479116433806735L;
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
        private static final long serialVersionUID = 6363944568317854215L;

        @Override
        public int compare(Option o1, Option o2) {
            return o1.name().compareTo(o2.name());
        }
    }

}
