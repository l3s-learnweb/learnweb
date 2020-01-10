package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.Learnweb;

/**
 * @author Rishita
 *
 */
public class SurveyQuestion implements Serializable
{
    private static final long serialVersionUID = -7698089608547415349L;

    public enum QuestionType // represents primefaces input types
    {
        INPUT_TEXT (false, false), // options define the valid length (first entry = min length, second entry = max length)
        INPUT_TEXTAREA (false, false), // options define the valid length (first entry = min length, second entry = max length)
        AUTOCOMPLETE (false, true),
        ONE_MENU (false, true),
        ONE_MENU_EDITABLE (false, true),
        MULTIPLE_MENU (false, true),
        ONE_RADIO (false, true),
        MANY_CHECKBOX (false, true),
        FULLWIDTH_HEADER(true, false),
        FULLWIDTH_DESCRIPTION(true, false);

        private final boolean readonly;
        private final boolean options;

        QuestionType()
        {
            this.readonly = false;
            this.options = false;
        }

        QuestionType(boolean readonly, boolean options)
        {
            this.readonly = readonly;
            this.options = options;
        }

        public boolean isReadonly()
        {
            return readonly;
        }

        public boolean isOptions()
        {
            return options;
        }
    }

    public List<QuestionType> getQuestionTypes()
    {
        List<QuestionType> types = new ArrayList<>();
        Arrays.asList(QuestionType.values()).forEach(type -> {
            if(type != QuestionType.AUTOCOMPLETE && type != QuestionType.FULLWIDTH_HEADER)
                types.add(type);
        });
        return types;
    }

    private String label; // label on the website, is replaced by a translated term if available
    private String info; // an explanation, displayed as tooltip
    private QuestionType type;
    private int id; //question id
    private int surveyId;
    Map<String, Object> options = new HashMap<String, Object>(); // default options for some input types like OneMenu
    private boolean moderatorOnly = false; // only admins and moderators have write access
    private boolean required = false;
    private boolean deleted = false;
    private int order;
    private List<SelectItem> optionsList;
    private List<SurveyQuestionOption> answers = new ArrayList<>(); // predefined answers for types like ONE_MENU, ONE_RADIO, MANY_CHECKBOX ...

    public SurveyQuestion(QuestionType type)
    {
        this.type = type;
        // set default length limits for text input fields
        if(type == QuestionType.INPUT_TEXT || type == QuestionType.INPUT_TEXTAREA)
        {
            options.put("minLength", 0);
            options.put("maxLength", 6000);
        }
    }

    public SurveyQuestion(QuestionType type, String label)
    {
        this(type);
        setLabel(label);
    }

    public SurveyQuestion(QuestionType type, int surveyId)
    {
        this(type);
        setSurveyId(surveyId);

    }

    public QuestionType getType()
    {
        return type;
    }

    public void setType(QuestionType type)
    {
        this.type = type;
        if(type.options && this.getAnswers().size() == 0)
        {
            this.getAnswers().add(new SurveyQuestionOption());
            this.getAnswers().add(new SurveyQuestionOption());
        }
    }

    public Map<String, Object> getOptions()
    {
        return options;
    }

    public List<SelectItem> getOptionsList()
    {
        if(null == optionsList)
        {
            // TODO what is the purpose of this method?
            /* maybe Options and Answers were confused
             * answers shall anyway be renamed to Options
             *
             *
             */

            optionsList = new ArrayList<>(options.size());
            for(Map.Entry<String, Object> option : options.entrySet())
            {
                optionsList.add(new SelectItem(option.getValue(), option.getKey()));
            }
        }
        return optionsList;
    }

    public List<String> completeText(String query)
    {
        return null; // until now never used in a survey. But let's see
    }

    public void setOptions(Map<String, Object> options)
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

    public List<SurveyQuestionOption> getAnswers()
    {
        return answers;
    }

    public void setAnswers(List<SurveyQuestionOption> answers)
    {
        this.answers = answers;
    }

    public int getId()
    {
        return id;
    }

    void setId(int id)
    {
        this.id = id;
    }

    public boolean isDeleted()
    {
        return deleted;
    }

    public void setDeleted(final boolean deleted)
    {
        this.deleted = deleted;
    }

    public void save() throws SQLException
    {
        Learnweb.getInstance().getSurveyManager().saveQuestion(this);
    }

    public int getSurveyId()
    {
        return surveyId;
    }

    public void setSurveyId(int surveyId)
    {
        this.surveyId = surveyId;
    }

    public int getOrder()
    {
        return order;
    }

    public void setOrder(int order)
    {
        this.order = order;
    }

}
