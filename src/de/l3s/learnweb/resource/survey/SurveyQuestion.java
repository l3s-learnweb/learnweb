package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Rishita
 *
 */
public class SurveyQuestion implements Serializable
{
    private static final long serialVersionUID = -7698089608547415349L;

    public enum QuestionType // represents primeface input types
    {
        INPUT_TEXT, // options define the valid length (first entry = min length, second entry = max length)
        INPUT_TEXTAREA, // options define the valid length (first entry = min length, second entry = max length)
        AUTOCOMPLETE,
        ONE_MENU,
        ONE_MENU_EDITABLE,
        MULTIPLE_MENU,
        FULLWIDTH_HEADER(true),
        FULLWIDTH_DESCRIPTION(true),
        ONE_RADIO,
        MANY_CHECKBOX;

        private final boolean readonly;

        QuestionType()
        {
            this.readonly = false;
        }

        QuestionType(boolean readonly)
        {
            this.readonly = readonly;
        }

        public boolean isReadonly()
        {
            return readonly;
        }
    }

    private String label; // label on the website, is replaced by a translated term if available
    private String info; // an explanation, displayed as tooltip
    private QuestionType type;
    private int id; //question id
    private List<String> options = new LinkedList<String>(); // default options for some input types like OneMenu
    private boolean moderatorOnly = false; // only admins and moderators have write access
    private boolean required = false;
    private List<SelectItem> optionsList;
    private String extra; // if the options are rating or otherwise
    private List<String> answers; // predefined answers for question with options

    public SurveyQuestion(QuestionType type)
    {
        this.type = type;

        // set default length limits for text input fields
        if(type == QuestionType.INPUT_TEXT || type == QuestionType.INPUT_TEXTAREA)
        {
            options.add("0");
            options.add("6000");
        }
    }

    public QuestionType getType()
    {
        return type;
    }

    public void setType(QuestionType type)
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
        return null; // until now never used in a survey. But let's see
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
        this.info = StringUtils.defaultString(info);
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

    /**
     * Temp method for euMade4all delete after the project
     * 
     * @return
     */
    public List<String> getEUmadeAnswersWithout()
    {
        LinkedList<String> copy = new LinkedList<>(answers);
        copy.remove("Don't show");

        return copy;
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
