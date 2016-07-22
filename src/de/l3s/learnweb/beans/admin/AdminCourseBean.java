package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.event.ActionEvent;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

import de.l3s.learnweb.Course;
import de.l3s.learnweb.Course.Option;
import de.l3s.learnweb.File;
import de.l3s.learnweb.Organisation;
import de.l3s.learnweb.ResourcePreviewMaker;
import de.l3s.learnweb.solrClient.FileInspector.FileInfo;
import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@SessionScoped
public class AdminCourseBean extends ApplicationBean implements Serializable
{

    private static final long serialVersionUID = -1276599881084055950L;
    private Course course = null;
    private List<OptionWrapperGroup> optionGroups;
    private int courseId;
    private ArrayList<Organisation> organisations;

    public AdminCourseBean()
    {
	organisations = new ArrayList<Organisation>(getLearnweb().getOrganisationManager().getOrganisationsAll());
	Collections.sort(organisations);
    }

    public void loadCourse()
    {
	if(getUser() == null) // not logged in
	    return;

	if(courseId == 0)
	{
	    try
	    {
		courseId = Integer.parseInt(getFacesContext().getExternalContext().getRequestParameterMap().get("course_id"));
	    }
	    catch(Exception e)
	    {
		addGrowl(FacesMessage.SEVERITY_FATAL, "no course_id parameter");
		return;
	    }
	}

	course = getLearnweb().getCourseManager().getCourseById(courseId);

	if(null == course)
	{
	    addGrowl(FacesMessage.SEVERITY_FATAL, "invalid course_id parameter");
	    return;
	}

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
	    options.add(new OptionWrapper(option, course.getOption(option)));
	}
	optionGroups.add(new OptionWrapperGroup(oldOptionGroupName, options));
    }

    public void save(ActionEvent actionEvent)
    {
	if(course == null)
	    return;

	for(OptionWrapperGroup group : optionGroups)
	{
	    for(OptionWrapper optionWrapper : group.getOptions())
	    {
		course.setOption(optionWrapper.getOption(), optionWrapper.getValue());
	    }
	}
	try
	{
	    getLearnweb().getCourseManager().save(course);
	    addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    addGrowl(FacesMessage.SEVERITY_FATAL, "fatal_error");
	}
    }

    public List<OptionWrapperGroup> getOptionGroups()
    {
	return optionGroups;
    }

    public Course getCourse()
    {
	return course;
    }

    public void setCourseId(int courseId)
    {
	if(courseId != this.courseId)
	{
	    this.courseId = courseId;
	    loadCourse();
	}

    }

    public int getCourseId()
    {
	return courseId;
    }

    /**
     * Returns a list of all available organisations. For select box to select to which organisation a course belongs
     * 
     * @return
     */
    public ArrayList<Organisation> getOrganisations()
    {
	return organisations;
    }

    public void handleFileUpload(FileUploadEvent event)
    {

	UploadedFile uploadedFile = event.getFile();

	ResourcePreviewMaker rpm = getLearnweb().getResourcePreviewMaker();

	// get the mime type and extract text if possible
	FileInfo info;
	try
	{
	    info = rpm.getFileInfo(uploadedFile.getInputstream(), uploadedFile.getFileName());

	    File file = new File();
	    file.setName(info.getFileName());
	    file.setMimeType(info.getMimeType());

	    file = getLearnweb().getFileManager().save(file, uploadedFile.getInputstream());

	    course.setBannerImageFileId(file.getId());

	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    addGrowl(FacesMessage.SEVERITY_FATAL, "Could not store file");
	}
    }

    // only helper classes to display the options
    public class OptionWrapper implements Serializable
    {
	private static final long serialVersionUID = 2828959818690832148L;
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
