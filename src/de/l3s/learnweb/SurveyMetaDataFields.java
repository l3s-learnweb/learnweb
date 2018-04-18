package de.l3s.learnweb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.faces.model.SelectItem;

/**
 * @author Rishita
 *
 */
public class SurveyMetaDataFields implements Serializable
{
    private static final long serialVersionUID = -7698089608547415349L;

    public enum MetadataType
    { // represents primeface input types
        INPUT_TEXT,
        INPUT_TEXTAREA,
        AUTOCOMPLETE,
        ONE_MENU,
        ONE_MENU_EDITABLE,
        MULTIPLE_MENU,
        FULLWIDTH_HEADER,
        FULLWIDTH_DESCRIPTION,
        ONE_RADIO,
        MANY_CHECKBOX
    }

    // private String name; // the name of this field, will be used as SOLR column name
    private String label; // label on the website, is replaced by a translated term if available
    private String info; // an explanation, displayed as tooltip
    private MetadataType type;
    private int id; //question id
    private List<String> options = new LinkedList<String>(); // default options for some input types like OneMenu
    private boolean moderatorOnly = false; // only admins and moderators have write access
    private boolean required = false;
    private List<SelectItem> optionsList;
    private String extra; // if the options are rating or othherwise
    private List<String> answers; //answers for question with options

    public SurveyMetaDataFields(String label, MetadataType type)
    {
        super();
        //  this.name = name;
        this.label = label;
        this.type = type;
    }

    public SurveyMetaDataFields(String name, MetadataType type, boolean moderatorOnly)
    {
        super();
        //   this.name = name;
        this.label = name;
        this.type = type;
        this.moderatorOnly = moderatorOnly;
    }

    /* public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    */
    public MetadataType getType()
    {
        return type;
    }

    public void setType(MetadataType type)
    {
        this.type = type;
    }

    public List<String> getOptions()
    {
        return options;
    }

    public List<SelectItem> getOptionsList()
    {
        if(null == optionsList)
        {
            optionsList = new ArrayList<SelectItem>(options.size());

            for(String option : options)
            {
                optionsList.add(new SelectItem(option, option));
            }
        }
        return optionsList;
    }

    public List<String> completeText(String query)
    {
        return null;
    }

    public void setOptions(List<String> options)
    {
        this.options = options;
    }

    public boolean isModeratorOnly()
    {
        return moderatorOnly;
    }

    public void setModeratorOnly(boolean moderatorOnly)
    {
        this.moderatorOnly = moderatorOnly;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getInfo()
    {
        return info;
    }

    public void setInfo(String info)
    {
        this.info = info;
    }

    public boolean isRequired()
    {
        return required;
    }

    public void setRequired(boolean required)
    {
        this.required = required;
    }

    public String getExtra()
    {
        return extra;
    }

    public void setExtra(String extra)
    {
        this.extra = extra;
    }

    public List<String> getAnswers()
    {
        return answers;
    }

    public void setAnswers(List<String> answers)
    {
        this.answers = answers;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }
}
