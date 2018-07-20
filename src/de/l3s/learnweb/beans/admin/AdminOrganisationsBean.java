package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;

import org.apache.log4j.Logger;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.File.TYPE;
import de.l3s.learnweb.resource.search.solrClient.FileInspector;
import de.l3s.learnweb.resource.search.solrClient.FileInspector.FileInfo;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.Organisation.Option;

@Named
@SessionScoped
public class AdminOrganisationsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -4815509777068370043L;
    private static final Logger log = Logger.getLogger(AdminOrganisationsBean.class);
    private List<Organisation> organisations;
    private Organisation selectedOrganisation;
    private LinkedList<OptionWrapperGroup> optionGroups;

    public AdminOrganisationsBean() throws SQLException
    {
        log.debug("init AdminOrganisationsBean");
        organisations = new ArrayList<Organisation>(getLearnweb().getOrganisationManager().getOrganisationsAll());
        setSelectedOrganisation(getUser().getOrganisation()); // by default edit the users organization
    }

    public void handleFileUpload(FileUploadEvent event)
    {
        UploadedFile uploadedFile = event.getFile();

        FileInspector fileInspector = new FileInspector(getLearnweb());

        try
        {
            FileInfo fileInfo = fileInspector.inspect(uploadedFile.getInputstream(), uploadedFile.getFileName());

            File file = new File();
            file.setType(TYPE.SYSTEM_FILE);
            file.setName(fileInfo.getFileName());
            file.setMimeType(fileInfo.getMimeType());

            file = getLearnweb().getFileManager().save(file, uploadedFile.getInputstream());

            if(selectedOrganisation.getBannerImageFileId() > 0) // delete old image first
            {
                Learnweb.getInstance().getFileManager().delete(selectedOrganisation.getBannerImageFileId());
            }

            selectedOrganisation.setBannerImageFileId(file.getId());

        }
        catch(Exception e)
        {
            log.error("Could not handle uploaded banner image", e);
            addGrowl(FacesMessage.SEVERITY_FATAL, "Could not store file");
        }
    }

    public void onSave()
    {
        for(OptionWrapperGroup group : optionGroups)
        {
            for(OptionWrapper optionWrapper : group.getOptions())
            {
                selectedOrganisation.setOption(optionWrapper.getOption(), optionWrapper.getValue());
            }
        }

        try
        {
            getLearnweb().getOrganisationManager().save(selectedOrganisation);

            organisations = new ArrayList<Organisation>(getLearnweb().getOrganisationManager().getOrganisationsAll()); // reload

            addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

    public Organisation getSelectedOrganisation()
    {
        return selectedOrganisation;
    }

    public List<OptionWrapperGroup> getOptionGroups()
    {
        return optionGroups;
    }

    public void setSelectedOrganisation(Organisation selectedOrganisation)
    {
        log.debug("select organisation: " + selectedOrganisation);

        this.selectedOrganisation = selectedOrganisation;

        // many string operations to display the options in a proper way
        optionGroups = new LinkedList<OptionWrapperGroup>();
        List<OptionWrapper> options = new LinkedList<OptionWrapper>();
        String oldOptionGroupName = null;

        Option[] optionsEnum = Option.values();

        EnumComparator c = new EnumComparator();
        java.util.Arrays.sort(optionsEnum, c);

        for(Option option : optionsEnum)
        {
            // example: this gets "Services" from "Services_Allow_logout_from_Interweb"
            String newOptionGroupName = option.name().substring(0, option.name().indexOf("_"));

            if(oldOptionGroupName != null && !oldOptionGroupName.equalsIgnoreCase(newOptionGroupName))
            {
                optionGroups.add(new OptionWrapperGroup(oldOptionGroupName, options));

                options = new LinkedList<OptionWrapper>();
            }

            oldOptionGroupName = newOptionGroupName;
            options.add(new OptionWrapper(option, selectedOrganisation.getOption(option)));
        }
        optionGroups.add(new OptionWrapperGroup(oldOptionGroupName, options));
    }

    public List<Organisation> getOrganisations()
    {
        return organisations;
    }

    // only helper classes to display the options
    public class OptionWrapper implements Serializable
    {
        private static final long serialVersionUID = 4028764135842666696L;
        private Option option;
        private boolean value;

        public OptionWrapper(Option option, boolean value)
        {
            super();
            this.option = option;
            this.value = value;
        }

        public String getName()
        {
            return option.name().substring(option.name().indexOf("_")).replace("_", " ");
        }

        public boolean getValue()
        {
            return value;
        }

        public void setValue(boolean value)
        {
            this.value = value;
        }

        public Option getOption()
        {
            return option;
        }
    }

    public class OptionWrapperGroup implements Serializable
    {
        private static final long serialVersionUID = -7136479116433806735L;
        private String title;
        private List<OptionWrapper> options;

        public OptionWrapperGroup(String title, List<OptionWrapper> options)
        {
            super();
            this.title = title;
            this.options = options;
        }

        public String getTitle()
        {
            return title;
        }

        public List<OptionWrapper> getOptions()
        {
            return options;
        }
    }

    private class EnumComparator implements Comparator<Option>
    {
        @Override
        public int compare(Option o1, Option o2)
        {
            return o1.name().compareTo(o2.name());
        }
    }

}
