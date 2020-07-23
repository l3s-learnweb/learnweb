package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.Learnweb;
import de.l3s.util.Deletable;

/**
 * @author Rishita
 */
public class SurveyQuestion implements Deletable, Serializable, Cloneable {
    private static final long serialVersionUID = -7698089608547415349L;

    public enum QuestionType { // represents primefaces input types
        INPUT_TEXT(false, false), // options define the valid length (first entry = min length, second entry = max length)
        INPUT_TEXTAREA(false, false), // options define the valid length (first entry = min length, second entry = max length)
        AUTOCOMPLETE(false, true),
        ONE_MENU(false, true),
        ONE_MENU_EDITABLE(false, true),
        MULTIPLE_MENU(false, true),
        ONE_RADIO(false, true),
        MANY_CHECKBOX(false, true),
        FULLWIDTH_HEADER(true, false),
        FULLWIDTH_DESCRIPTION(true, false);

        private final boolean readonly;
        private final boolean options;

        QuestionType() {
            this.readonly = false;
            this.options = false;
        }

        QuestionType(boolean readonly, boolean options) {
            this.readonly = readonly;
            this.options = options;
        }

        public boolean isReadonly() {
            return readonly;
        }

        public boolean isOptions() {
            return options;
        }
    }

    private String label; // label on the website, is replaced by a translated term if available
    private String info; // an explanation, displayed as tooltip
    private QuestionType type;
    private int id; //question id
    private int surveyId;
    private Map<String, Object> options = new HashMap<>(); // default options for some input types like OneMenu
    private boolean moderatorOnly = false; // only admins and moderators have write access
    private boolean required = false;
    private boolean deleted = false;
    private int order;
    private List<SurveyQuestionOption> answers = new ArrayList<>(); // predefined answers for types like ONE_MENU, ONE_RADIO, MANY_CHECKBOX ...

    public SurveyQuestion(QuestionType type) {
        this.type = type;
        // set default length limits for text input fields
        if (type == QuestionType.INPUT_TEXT || type == QuestionType.INPUT_TEXTAREA) {
            options.put("minLength", 0);
            options.put("maxLength", 6000);
        }
    }

    public SurveyQuestion(QuestionType type, String label) {
        this(type);
        setLabel(label);
    }

    public SurveyQuestion(QuestionType type, int surveyId) {
        this(type);
        setSurveyId(surveyId);
    }

    public SurveyQuestion(SurveyQuestion question) {
        setId(-1);
        setSurveyId(-1);
        setLabel(question.label);
        setInfo(question.info);
        setOptions(question.options);
        setModeratorOnly(question.moderatorOnly);
        setRequired(question.required);
        setDeleted(question.deleted);
        setOrder(question.order);
        for (SurveyQuestionOption answer : question.getAnswers()) {
            answer.setId(0);
            answers.add(answer);
        }
        setType(question.type);
    }

    public List<QuestionType> getQuestionTypes() {
        List<QuestionType> types = new ArrayList<>();
        Arrays.asList(QuestionType.values()).forEach(type -> {
            if (type != QuestionType.AUTOCOMPLETE && type != QuestionType.FULLWIDTH_HEADER) {
                types.add(type);
            }
        });
        return types;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
        if (type.options && this.getAnswers().isEmpty()) {
            this.getAnswers().add(new SurveyQuestionOption());
            this.getAnswers().add(new SurveyQuestionOption());
        }
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    public List<String> completeText(String query) {
        return null; // until now never used in a survey. But let's see
    }

    public boolean isModeratorOnly() {
        return moderatorOnly;
    }

    public void setModeratorOnly(boolean moderatorOnly) {
        this.moderatorOnly = moderatorOnly;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label.replaceAll("<p>", "").replaceAll("</p>", "");
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = StringUtils.defaultString(info);
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public List<SurveyQuestionOption> getAnswers() {
        return answers;
    }

    public void setAnswers(List<SurveyQuestionOption> answers) {
        this.answers = answers;
    }

    public int getId() {
        return id;
    }

    void setId(int id) {
        this.id = id;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public void save() throws SQLException {
        Learnweb.getInstance().getSurveyManager().saveQuestion(this);
    }

    public int getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(int surveyId) {
        this.surveyId = surveyId;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * Returns a copy of this Survey Question (Ids are set to default this the Object isn't persisted yet).
     */
    @Override
    public SurveyQuestion clone() {
        return new SurveyQuestion(this);
    }

}
